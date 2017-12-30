package org.jchien.shuffle.parser;

import java.util.List;
import java.util.Objects;

/**
 * @author jchien
 */
public class RawRunDetails {
    private List<RawPokemon> team;
    private List<String> items;
    private String score;
    private String stage;
    private String movesLeft;

    public RawRunDetails(List<RawPokemon> team, List<String> items, String score, String stage, String movesLeft) {
        this.team = team;
        this.items = items;
        this.score = score;
        this.stage = stage;
        this.movesLeft = movesLeft;
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

    @Override
    public String toString() {
        return "RawRunDetails{" +
                "team=" + team +
                ", items=" + items +
                ", score='" + score + '\'' +
                ", stage='" + stage + '\'' +
                ", movesLeft='" + movesLeft + '\'' +
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
                Objects.equals(movesLeft, that.movesLeft);
    }

    @Override
    public int hashCode() {

        return Objects.hash(team, items, score, stage, movesLeft);
    }
}
