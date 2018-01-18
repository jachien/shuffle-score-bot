package org.jchien.shuffle.model;

/**
 * @author jchien
 */
public class BotComment {
    private final String commentId;

    private final String content;

    public BotComment(String commentId, String content) {
        this.commentId = commentId;
        this.content = content;
    }

    public String getCommentId() {
        return commentId;
    }

    public String getContent() {
        return content;
    }
}
