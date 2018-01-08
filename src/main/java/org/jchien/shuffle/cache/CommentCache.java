package org.jchien.shuffle.cache;

import org.jchien.shuffle.model.UserRunDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jchien
 */
public class CommentCache {
    private final String submissionId;

    private long lastProcessedTime = 0;

    // comment id -> last edit time, run*
    private Map<String, CommentEntry> commentMap = new HashMap<>();

    public CommentCache(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public long getLastProcessedTime() {
        return lastProcessedTime;
    }

    public void setLastProcessedTime(long lastProcessedTime) {
        this.lastProcessedTime = lastProcessedTime;
    }

    public void setCommentData(String commentId, long commentLastModifiedTime, List<UserRunDetails> runs) {
        commentMap.put(commentId, new CommentEntry(commentId, commentLastModifiedTime, runs));
    }

    public Map<String, List<UserRunDetails>> getStageRunsMap() {
        Map<String, List<UserRunDetails>> stageRunsMap = new HashMap<>();
        for (CommentEntry entry : commentMap.values()) {
            for (UserRunDetails run : entry.getRuns()) {
                addRun(stageRunsMap, run);
            }
        }
        return stageRunsMap;
    }

    private void addRun(Map<String, List<UserRunDetails>> stageRunsMap, UserRunDetails run) {
        String stage = run.getRunDetails().getStage();
        if (stage != null) {
            stage = stage.toLowerCase();
        }

        List<UserRunDetails> runs = stageRunsMap.get(stage);
        if (runs == null) {
            runs = new ArrayList<>();
            stageRunsMap.put(stage, runs);
        }
        runs.add(run);
    }
}
