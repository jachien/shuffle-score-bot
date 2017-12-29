package org.jchien.shuffle.parser;

import java.util.List;

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
}
