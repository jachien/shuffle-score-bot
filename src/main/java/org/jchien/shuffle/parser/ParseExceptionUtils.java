package org.jchien.shuffle.parser;

import org.jchien.shuffle.parser.exception.FormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @author jchien
 */
public class ParseExceptionUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ParseExceptionUtils.class);

    private static final String[] TOKEN_IMAGE = initTokenImage();

    private static String[] initTokenImage() {
        return Arrays.stream(RunParserConstants.tokenImage)
                .map(s -> {
                    if (s.startsWith("\"") && s.endsWith("\"")) {
                        return s.substring(1, s.length() - 1);
                    }

                    // convert these token names to a more friendly string
                    if ("<MOVES_LEFT_HEADER>".equals(s)) {
                        return "MOVES LEFT:";
                    } else if ("<TIME_LEFT_HEADER>".equals(s)) {
                        return "TIME LEFT:";
                    }

                    return s;
                })
                .toArray(String[]::new);
    }

    /**
     * @param comment       string containing exactly the run details (and nothing more, i.e. possibly not the full post)
     * @param lineOffset    0-indexed line offset of run details in full comment
     * @param colOffset     0-indexed column offset of run details in full comment
     * @param e             parse exception from javacc
     * @return              exception with error message for end user consumption
     */
    public static FormatException getFormatException(String comment, int lineOffset, int colOffset, ParseException e) {
        // modified from ParseException#initialise

        Token currentToken = e.currentToken;
        int[][] expectedTokenSequences = e.expectedTokenSequences;

        StringBuilder message = new StringBuilder();

        StringBuilder expected = new StringBuilder();
        int maxSize = 0;
        for (int i = 0; i < expectedTokenSequences.length; i++) {
            if (maxSize < expectedTokenSequences[i].length) {
                maxSize = expectedTokenSequences[i].length;
            }
            for (int j = 0; j < expectedTokenSequences[i].length; j++) {
                expected.append(TOKEN_IMAGE[expectedTokenSequences[i][j]]).append(' ');
            }
            if (expectedTokenSequences[i][expectedTokenSequences[i].length - 1] != 0) {
                expected.append("...");
            }
            expected.append("\n    ");
        }
        message.append("Encountered `");
        Token tok = currentToken.next;
        for (int i = 0; i < maxSize; i++) {
            if (i != 0) message.append(" ");
            if (tok.kind == 0) {
                message.append(TOKEN_IMAGE[0]);
                break;
            }
            // todo escape reddit syntax?
            message.append(ParseException.add_escapes(tok.image));
            tok = tok.next;
        }
        final int errorLine = currentToken.next.beginLine; // this is 1-indexed
        final int errorColumn = currentToken.next.beginColumn; // this is 1-indexed

        final int adjustedErrorLine = errorLine + lineOffset;
        final int adjustedErrorColumn = errorColumn + colOffset;

        message.append("` at line ").append(adjustedErrorLine).append(", column ").append(adjustedErrorColumn);
        message.append(".\n\n");

        try {
            String context = getContextSnippet(comment, errorLine - 1, errorColumn - 1);
            message.append(context).append("\n\n");
        } catch (Exception ctxException) {
            LOG.error("error generating context snippet: ", ctxException);
        }

        if (expectedTokenSequences.length == 0) {
            // Nothing to add here
        } else {
            if (expectedTokenSequences.length == 1) {
                message.append("Was expecting:\n\n    ");
            } else {
                message.append("Was expecting one of:\n\n    ");
            }
            message.append(expected.toString());
        }

        return new FormatException(message.toString(), e);
    }

    private static final int MIN_SNIPPET_SIZE = 40;
    private static final String INDENT = "    ";

    /**
     * @param comment
     * @param lineNum 0-indexed
     * @param columnNum 0-indexed
     * @return
     */
    private static String getContextSnippet(String comment, int lineNum, int columnNum) {
        StringBuilder sb = new StringBuilder();

        String[] lines = comment.split("\\n");
        String errLine = lines[lineNum];

        int halfSize = MIN_SNIPPET_SIZE / 2;
        int end = getSnippetEnd(errLine, columnNum, halfSize);
        int addlLen = Math.max(0, halfSize - (end - columnNum));
        int start = getSnippetStart(errLine, columnNum, halfSize + addlLen);

        int snippetLen = end - start;
        int remLen = MIN_SNIPPET_SIZE - snippetLen;
        appendPreErrorSnippet(sb, lines, lineNum, remLen);

        appendMainErrorSnippet(sb, errLine, columnNum, start, end);

        return sb.toString();
    }

    private static int getSnippetStart(String line, int columnNum, int minOffset) {
        int start = Math.max(0, columnNum - minOffset);
        while (start > 0 && !Character.isWhitespace(line.codePointAt(start))) {
            start = line.offsetByCodePoints(start, -1);
        }
        return start;
    }

    private static int getSnippetEnd(String line, int columnNum, int minOffset) {
        int end = Math.min(line.length(), columnNum + minOffset);
        while (end < line.length() && !Character.isWhitespace(line.codePointAt(end))) {
            end = line.offsetByCodePoints(end, 1);
        }
        return end;
    }

    private static void appendPreErrorSnippet(StringBuilder sb, String[] lines, int lineNum, int remLen) {
        if (remLen <= 0) {
            return;
        }

        int ctxLineNum = lineNum - 1;
        if (ctxLineNum < 0) {
            return;
        }

        String ctxLine = lines[ctxLineNum];
        while (remLen > 0 && ctxLineNum > 0 && ctxLine.length() < remLen) {
            remLen -= ctxLine.length();
            ctxLineNum--;
            ctxLine = lines[ctxLineNum];
        }

        if (remLen > 0) {
            int ctxStart = getSnippetStart(ctxLine, ctxLine.length(), remLen);
            sb.append(INDENT);
            if (ctxStart > 0) {
                sb.append("... ");
            }
            sb.append(ctxLine, ctxStart, ctxLine.length()).append("\n");
        }
        for (int i=ctxLineNum+1; i < lineNum; i++) {
            sb.append(INDENT).append(lines[i]).append("\n");
        }
    }

    private static void appendMainErrorSnippet(StringBuilder sb, String errLine, int columnNum, int start, int end) {
        int errorOffset = 0;
        sb.append(INDENT);
        if (start > 0) {
            String ellipses = "... ";
            sb.append(ellipses);
            errorOffset += ellipses.length();
        }
        sb.append(errLine, start, end);
        if (end < errLine.length()) {
            sb.append(" ...");
        }

        sb.append("\n").append(INDENT);
        for (int i=0; i < errorOffset; i++) {
            sb.append(' ');
        }
        for (int i=start; i < columnNum; i++) {
            sb.append(' ');
        }
        sb.append('^');
    }

    public static void main(String[] args) {
        String comment = "!roster\n" +
                "SMCX (Lv15, SL2, 15/15),  Tapu-Koko (Lv16, TC SL5), Meganium (Lv26, BS SL5), Tapu-Bulu (Lv16, TC SL5), Diancie-S (Lv20, SL5, 5/5), Shiftry (Lv15, SC SL5), AngryChu (Lv20, SL5), Rowlet (lv15, UP SL5), Tyranitar (lv10, Ejec SL1), Shaymin-Land (Lv15, SL5). !end\n";
        System.out.println(getContextSnippet(comment, 1, 252));

        System.out.println("--------");

        comment = "!roster W-Glalie (Lv10, SL1, 20/20), Salazzle (Lv25, Shot Out SL5) Rapidash (Lv15, Shot Out SL5), Noivern (Lv20, Shot Out SL5).\n" +
                "!end\n";
        System.out.println(getContextSnippet(comment, 0, 67));

        System.out.println("--------");

        comment = "!comp\n" +
                "\n" +
                "Team: Gengar (Lv15), Lunala (Lv17, SL1), Mimikyu (Lv15, SL5)\n" +
                "\n" +
                "Items: M+5, MS, APU, C-1\n" +
                "\n" +
                "Scored: 808,472\n" +
                "\n" +
                "!end";
        System.out.println(getContextSnippet(comment, 6, 0));

        System.out.println("--------");

        comment = "!comp\n" +
                "\n" +
                "Team: Gengar (Lv15\n" +
                "\n" +
                "Items: M+5, MS, APU, C-1\n" +
                "\n" +
                "Score: 808,472\n" +
                "\n" +
                "!end";
        System.out.println(getContextSnippet(comment, 4, 0));

        System.out.println("--------");

        comment = "!comp\n" +
                "\n" +
                "[shuffle score bot docs](https://jachien.github.io/shuffle-score-bot/)\n" +
                "\n" +
                "/r/PokemonShuffle\n" +
                "\n" +
                "!end";
        System.out.println(getContextSnippet(comment, 2, 1));
    }
}
