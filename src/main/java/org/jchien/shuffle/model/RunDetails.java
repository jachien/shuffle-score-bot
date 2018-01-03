package org.jchien.shuffle.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author jchien
 */
public class RunDetails {
    private List<Pokemon> team;
    private Set<Item> items;
    private Integer score;
    private Integer stage;
    private Integer movesLeft;

    private Exception exception;

    public RunDetails(List<Pokemon> team, Set<Item> items, Integer score, Integer stage, Integer movesLeft) {
        this.team = team;
        this.items = items;
        this.score = score;
        this.stage = stage;
        this.movesLeft = movesLeft;
    }

    public RunDetails(Exception exception) {
        this.exception = exception;
    }

    public List<Pokemon> getTeam() {
        return team;
    }

    public Set<Item> getItems() {
        return items;
    }

    public Integer getScore() {
        return score;
    }

    public Integer getStage() {
        return stage;
    }

    public Integer getMovesLeft() {
        return movesLeft;
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
                ", score='" + score + '\'' +
                ", stage='" + stage + '\'' +
                ", movesLeft='" + movesLeft + '\'' +
                ", exception='" + exception + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RunDetails that = (RunDetails) o;
        return Objects.equals(team, that.team) &&
                Objects.equals(items, that.items) &&
                Objects.equals(score, that.score) &&
                Objects.equals(stage, that.stage) &&
                Objects.equals(movesLeft, that.movesLeft) &&
                Objects.equals(exception, that.exception);
    }

    @Override
    public int hashCode() {

        return Objects.hash(team, items, score, stage, movesLeft, exception);
    }
}
