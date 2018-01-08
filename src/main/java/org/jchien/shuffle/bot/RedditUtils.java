package org.jchien.shuffle.bot;

/**
 * @author jchien
 */
public class RedditUtils {
    public static String getCommentPermalink(String submissionUrl, String commentId) {
        return submissionUrl + commentId;
    }
}
