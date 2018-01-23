package org.jchien.shuffle.model;

/**
 * @author jchien
 */
public class UserRunDetails {
    private String user;

    private String commentId;

    private RunDetails runDetails;

    public UserRunDetails(String user, String commentId, RunDetails runDetails) {
        this.user = user;
        this.commentId = commentId;
        this.runDetails = runDetails;
    }

    public String getUser() {
        return user;
    }

    public String getCommentId() {
        return commentId;
    }

    public RunDetails getRunDetails() {
        return runDetails;
    }

    @Override
    public String toString() {
        return "UserRunDetails{" +
                "user='" + user + '\'' +
                ", commentId='" + commentId + '\'' +
                ", runDetails=" + runDetails +
                '}';
    }
}
