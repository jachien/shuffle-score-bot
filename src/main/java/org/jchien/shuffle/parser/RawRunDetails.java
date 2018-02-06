package org.jchien.shuffle.parser;

import org.jchien.shuffle.model.MoveType;
import org.jchien.shuffle.model.StageType;

import java.util.List;
import java.util.Objects;

/**
 * @author jchien
 */
public class RawRunDetails {
    private final List<RawPokemon> team;
    private final List<String> items;
    private final String score;
    private final String stage;
    private final String movesLeft;
    private final String timeLeft;
    private final StageType stageType;
    private final MoveType moveType;
    private final String notes;

    public RawRunDetails(List<RawPokemon> team,
                         List<String> items,
                         String score,
                         String stage,
                         String movesLeft,
                         String timeLeft,
                         StageType stageType,
                         MoveType moveType,
                         String notes) {
        this.team = team;
        this.items = items;
        this.score = score;
        this.stage = stage;
        this.movesLeft = movesLeft;
        this.timeLeft = timeLeft;
        this.stageType = stageType;
        this.moveType = moveType;
        this.notes = notes;
    }

    public List<RawPokemon> getTeam() {
        return team;
    }

    public List<String> getItems() {
        return items;
    }

    public String getScore() {
        return score;
    }

    public String getStage() {
        return stage;
    }

    public String getMovesLeft() {
        return movesLeft;
    }

    public String getTimeLeft() {
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

    @Override
    public String toString() {
        return "RawRunDetails{" +
                "team=" + team +
                ", items=" + items +
                ", score='" + score + '\'' +
                ", stage='" + stage + '\'' +
                ", movesLeft='" + movesLeft + '\'' +
                ", timeLeft='" + timeLeft + '\'' +
                ", stageType=" + stageType +
                ", moveType=" + moveType +
                ", notes='" + notes + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawRunDetails that = (RawRunDetails) o;
        return Objects.equals(team, that.team) &&
                Objects.equals(items, that.items) &&
                Objects.equals(score, that.score) &&
                Objects.equals(stage, that.stage) &&
                Objects.equals(movesLeft, that.movesLeft) &&
                Objects.equals(timeLeft, that.timeLeft) &&
                stageType == that.stageType &&
                moveType == that.moveType &&
                Objects.equals(notes, that.notes);
    }

    @Override
    public int hashCode() {

        return Objects.hash(team, items, score, stage, movesLeft, timeLeft, stageType, moveType, notes);
    }
}
