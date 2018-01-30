package org.jchien.shuffle.model;

import java.util.List;

public class RunDetailsBuilder {
    private List<Pokemon> team;
    private List<Item> items;
    private String stage;
    private Integer score;
    private Integer movesLeft;
    private Integer timeLeft;
    private StageType stageType;
    private MoveType moveType;
    private List<Throwable> throwables;

    public RunDetailsBuilder setTeam(List<Pokemon> team) {
        this.team = team;
        return this;
    }

    public RunDetailsBuilder setItems(List<Item> items) {
        this.items = items;
        return this;
    }

    public RunDetailsBuilder setStage(String stage) {
        this.stage = stage;
        return this;
    }

    public RunDetailsBuilder setScore(Integer score) {
        this.score = score;
        return this;
    }

    public RunDetailsBuilder setMovesLeft(Integer movesLeft) {
        this.movesLeft = movesLeft;
        return this;
    }

    public RunDetailsBuilder setTimeLeft(Integer timeLeft) {
        this.timeLeft = timeLeft;
        return this;
    }

    public RunDetailsBuilder setStageType(StageType stageType) {
        this.stageType = stageType;
        return this;
    }

    public RunDetailsBuilder setMoveType(MoveType moveType) {
        this.moveType = moveType;
        return this;
    }

    public RunDetailsBuilder setThrowables(List<Throwable> throwables) {
        this.throwables = throwables;
        return this;
    }

    public RunDetails build() {
        return new RunDetails(team, items, stage, score, movesLeft, timeLeft, stageType, moveType, throwables);
    }
}
