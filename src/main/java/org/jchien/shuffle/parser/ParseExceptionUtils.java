package org.jchien.shuffle.parser;

import org.jchien.shuffle.model.FormatException;

import java.util.Arrays;

/**
 * @author jchien
 */
public class ParseExceptionUtils {
    private static final String[] TOKEN_IMAGE = initTokenImage();

    private static String[] initTokenImage() {
        return Arrays.stream(RunParserConstants.tokenImage)
                .map(s -> {
                    if (s.startsWith("\"") && s.endsWith("\"")) {
                        return s.substring(1, s.length() - 1);
                    }
                    return s;
                })
                .toArray(String[]::new);
    }

    public static FormatException getFormatException(ParseException e) {
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
        message.append("Encountered \"");
        Token tok = currentToken.next;
        for (int i = 0; i < maxSize; i++) {
            if (i != 0) message.append(" ");
            if (tok.kind == 0) {
                message.append(TOKEN_IMAGE[0]);
                break;
            }
            message.append(ParseException.add_escapes(tok.image));
            tok = tok.next;
        }
        message.append("\" at line ").append(currentToken.next.beginLine).append(", column ").append(currentToken.next.beginColumn);
        message.append(".\n");


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
}
