package org.jchien.shuffle.model;

import java.util.List;
import java.util.Objects;

/**
 * @author jchien
 */
public class RunDetails {
    private final List<Pokemon> team;
    private final List<Item> items;
    private final String stage;
    private final Integer score;
    private final Integer movesLeft;
    private final StageType stageType;
    private final MoveType moveType;

    private final Exception exception;

    public RunDetails(List<Pokemon> team, List<Item> items, String stage, Integer score, Integer movesLeft, StageType stageType, MoveType moveType, Exception exception) {
        this.team = team;
        this.items = items;
        this.stage = stage;
        this.score = score;
        this.movesLeft = movesLeft;
        this.stageType = stageType;
        this.moveType = moveType;
        this.exception = exception;
    }

    public RunDetails(List<Pokemon> team, List<Item> items, String stage, Integer score, Integer movesLeft, StageType stageType, MoveType moveType) {
        this(team, items, stage, score, movesLeft, stageType, moveType, null);
    }

    public RunDetails(Exception exception) {
        this(null, null, null, null, null, null, null, exception);
    }


    public List<Pokemon> getTeam() {
        return team;
    }

    public List<Item> getItems() {
        return items;
    }

    public Integer getItemsCost() {
        // could cache this but it should be fast to compute so whatever

        if (items == null) {
            // don't know what items were used
            return null;
        }

        int cost = 0;
        for (Item i : items) {
            cost += i.getCost();
        }

        return cost;
    }

    public String getStage() {
        return stage;
    }

    public Integer getScore() {
        return score;
    }

    public Integer getMovesLeft() {
        return movesLeft;
    }

    public StageType getStageType() {
        return stageType;
    }

    public MoveType getMoveType() {
        return moveType;
    }

    public Exception getException() {
        return exception;
    }

    public boolean hasException() {
        return exception != null;
    }

    @Override
    public String toString() {
        return "RunDetails{" +
                "team=" + team +
                ", items=" + items +
                ", stage='" + stage + '\'' +
                ", score=" + score +
                ", movesLeft=" + movesLeft +
                ", stageType=" + stageType +
                ", moveType=" + moveType +
                ", exception=" + exception +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RunDetails that = (RunDetails) o;
        return Objects.equals(team, that.team) &&
                Objects.equals(items, that.items) &&
                Objects.equals(stage, that.stage) &&
                Objects.equals(score, that.score) &&
                Objects.equals(movesLeft, that.movesLeft) &&
                stageType == that.stageType &&
                moveType == that.moveType &&
                Objects.equals(exception, that.exception);
    }

    @Override
    public int hashCode() {

        return Objects.hash(team, items, stage, score, movesLeft, stageType, moveType, exception);
    }
}
