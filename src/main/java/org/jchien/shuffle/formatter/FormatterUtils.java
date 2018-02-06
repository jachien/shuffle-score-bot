package org.jchien.shuffle.formatter;

/**
 * @author jchien
 */
public class FormatterUtils {
    private static final int DASH_CODEPOINT = (int)'-';

    public static void appendCapitalizedWords(StringBuilder sb, String str) {
        boolean capitalize = true;

        for (int i=0; i < str.length();) {
            int codePoint = str.codePointAt(i);

            if (capitalize) {
                sb.appendCodePoint(Character.toUpperCase(codePoint));
            } else {
                // don't lowercase stuff so we still handle things like "MMY" nicely
                sb.appendCodePoint(codePoint);
            }

            if (Character.isWhitespace(codePoint) || DASH_CODEPOINT == codePoint) {
                capitalize = true;
            } else {
                capitalize = false;
            }

            i += Character.charCount(codePoint);
        }
    }

    public static String getCommentPermalink(String submissionUrl, String commentId) {
        return submissionUrl + commentId;
    }
}
