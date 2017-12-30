package org.jchien.shuffle.parser;

import java.util.Objects;

/**
 * @author jchien
 */
public class RawPokemon {
    private String name = null;
    private String level = null;
    private String skill = null;
    private String msus = null;
    private boolean perfect = false;

    public RawPokemon(String name, String level, String skill, String msus, boolean perfect) {
        this.name = name;
        this.level = level;
        this.skill = skill;
        this.msus = msus;
        this.perfect = perfect;
    }

    public String getName() {
        return name;
    }

    public String getLevel() {
        return level;
    }

    public String getSkill() {
        return skill;
    }

    public String getMsus() {
        return msus;
    }

    public boolean isPerfect() {
        return perfect;
    }

    @Override
    public String toString() {
        return "RawPokemon{" +
                "name='" + name + '\'' +
                ", level='" + level + '\'' +
                ", skill='" + skill + '\'' +
                ", msus='" + msus + '\'' +
                ", perfect=" + perfect +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RawPokemon that = (RawPokemon) o;
        return perfect == that.perfect &&
                Objects.equals(name, that.name) &&
                Objects.equals(level, that.level) &&
                Objects.equals(skill, that.skill) &&
                Objects.equals(msus, that.msus);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, level, skill, msus, perfect);
    }
}
