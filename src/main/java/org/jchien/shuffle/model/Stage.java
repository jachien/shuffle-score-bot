package org.jchien.shuffle.model;

/**
 * @author jchien
 */
public class Stage {
    private final StageType stageType;
    private final String stageId;

    public Stage(StageType stageType, String stageId) {
        this.stageType = stageType;
        this.stageId = stageId;
    }

    public StageType getStageType() {
        return stageType;
    }

    public String getStageId() {
        return stageId;
    }
}
