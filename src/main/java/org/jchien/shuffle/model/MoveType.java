package org.jchien.shuffle.model;

/**
 * need a better name for this but "stage type" is already in use to different EB vs competition vs normal stage
 * @author jchien
 */
public enum MoveType {
    MOVES("%d moves left"),
    TIME("%d seconds left");

    private final String resultFormat;

    MoveType(String resultFormat) {
        this.resultFormat = resultFormat;
    }

    public String format(int remaining) {
        return String.format(resultFormat, remaining);
    }
}
