package org.jchien.shuffle.parser;

public class RawPokemonBuilder {
    private String name;
    private String level;
    private String skill;
    private String msus;
    private boolean perfect;

    public RawPokemonBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public RawPokemonBuilder setLevel(String level) {
        this.level = level;
        return this;
    }

    public RawPokemonBuilder setSkill(String skill) {
        this.skill = skill;
        return this;
    }

    public RawPokemonBuilder setMsus(String msus) {
        this.msus = msus;
        return this;
    }

    public RawPokemonBuilder setPerfect(boolean perfect) {
        this.perfect = perfect;
        return this;
    }

    public RawPokemon build() {
        return new RawPokemon(name, level, skill, msus, perfect);
    }
}
