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
    private final Integer timeLeft;
    private final StageType stageType;
    private final MoveType moveType;
    private final String notes;

    private final List<Throwable> throwables;

    RunDetails(List<Pokemon> team,
               List<Item> items,
               String stage,
               Integer score,
               Integer movesLeft,
               Integer timeLeft,
               StageType stageType,
               MoveType moveType,
               String notes,
               List<Throwable> throwables) {
        this.team = team;
        this.items = items;
        this.stage = stage;
        this.score = score;
        this.movesLeft = movesLeft;
        this.timeLeft = timeLeft;
        this.stageType = stageType;
        this.moveType = moveType;
        this.notes = notes;
        this.throwables = throwables;
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

    public Integer getTimeLeft() {
        return timeLeft;
    }

    public StageType getStageType() {
        return stageType;
    }

    public MoveType getMoveType() {
        return moveType;
    }

    public String getNotes() {
        return notes;
    }

    public List<Throwable> getThrowables() {
        return throwables;
    }

    public boolean hasThrowable() {
        return throwables != null && !throwables.isEmpty();
    }

    @Override
    public String toString() {
        return "RunDetails{" +
                "team=" + team +
                ", items=" + items +
                ", stage='" + stage + '\'' +
                ", score=" + score +
                ", movesLeft=" + movesLeft +
                ", timeLeft=" + timeLeft +
                ", stageType=" + stageType +
                ", moveType=" + moveType +
                ", notes='" + notes + '\'' +
                ", throwables=" + throwables +
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
                Objects.equals(timeLeft, that.timeLeft) &&
                stageType == that.stageType &&
                moveType == that.moveType &&
                Objects.equals(notes, that.notes) &&
                Objects.equals(throwables, that.throwables);
    }

    @Override
    public int hashCode() {

        return Objects.hash(team, items, stage, score, movesLeft, timeLeft, stageType, moveType, notes, throwables);
    }
}
