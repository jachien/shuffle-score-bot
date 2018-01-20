package org.jchien.shuffle.model;

import java.time.Instant;

/**
 * @author jchien
 */
public class UserRunDetails {
    private String user;

    private String commentId;

    private Instant lastModifiedTime;

    private RunDetails runDetails;

    public UserRunDetails(String user, String commentId, Instant lastModifiedTime, RunDetails runDetails) {
        this.user = user;
        this.commentId = commentId;
        this.lastModifiedTime = lastModifiedTime;
        this.runDetails = runDetails;
    }

    public String getUser() {
        return user;
    }

    public String getCommentId() {
        return commentId;
    }

    public Instant getLastModifiedTime() {
        return lastModifiedTime;
    }

    public RunDetails getRunDetails() {
        return runDetails;
    }

    @Override
    public String toString() {
        return "UserRunDetails{" +
                "user='" + user + '\'' +
                ", commentId='" + commentId + '\'' +
                ", lastModifiedTime=" + lastModifiedTime +
                ", runDetails=" + runDetails +
                '}';
    }
}
