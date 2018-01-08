package org.jchien.shuffle.cache;

import org.jchien.shuffle.model.UserRunDetails;

import java.util.List;

/**
 * @author jchien
 */
public class CommentEntry {
    private String commentId;

    private long lastModifiedTime;

    private List<UserRunDetails> runs;

    public CommentEntry(String commentId, long lastModifiedTime, List<UserRunDetails> runs) {
        this.commentId = commentId;
        this.lastModifiedTime = lastModifiedTime;
        this.runs = runs;
    }

    public String getCommentId() {
        return commentId;
    }

    public long getLastModifiedTime() {
        return lastModifiedTime;
    }

    public List<UserRunDetails> getRuns() {
        return runs;
    }
}
