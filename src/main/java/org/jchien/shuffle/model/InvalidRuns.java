package org.jchien.shuffle.model;

import java.time.Instant;
import java.util.List;

/**
 * @author jchien
 */
public class InvalidRuns {
    private final String commentId;

    private final Instant lastModifiedDate;

    private final List<UserRunDetails> runs;

    public InvalidRuns(String commentId, Instant lastModifiedDate, List<UserRunDetails> runs) {
        this.commentId = commentId;
        this.lastModifiedDate = lastModifiedDate;
        this.runs = runs;
    }

    public String getCommentId() {
        return commentId;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public List<UserRunDetails> getRuns() {
        return runs;
    }

    @Override
    public String toString() {
        return "InvalidRuns{" +
                "commentId='" + commentId + '\'' +
                ", lastModifiedDate=" + lastModifiedDate +
                ", runs=" + runs +
                '}';
    }
}
