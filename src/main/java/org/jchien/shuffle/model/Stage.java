package org.jchien.shuffle.model;

import java.util.Objects;

/**
 * @author jchien
 */
public class Stage {
    private final StageType stageType;
    private final String stageId;
    private boolean isNumericStageId;

    public Stage(StageType stageType, String stageId) {
        this.stageType = stageType;
        this.stageId = stageId;
        this.isNumericStageId = initIsNumericStageId(stageId);
    }

    private static boolean initIsNumericStageId(String stageId) {
        if (stageId == null) {
            return false;
        }

        try {
            Integer.parseInt(stageId);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public StageType getStageType() {
        return stageType;
    }

    public String getStageId() {
        return stageId;
    }

    public static String normalizeStageId(String stage) {
        if (stage == null) {
            return null;
        }
        return stage.replaceAll("\\s+", "").toLowerCase().trim();
    }

    public boolean isNumericStageId() {
        return isNumericStageId;
    }

    @Override
    public String toString() {
        return "Stage{" +
                "stageType=" + stageType +
                ", stageId='" + stageId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stage stage = (Stage) o;
        return stageType == stage.stageType &&
                Objects.equals(stageId, stage.stageId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(stageType, stageId);
    }
}
