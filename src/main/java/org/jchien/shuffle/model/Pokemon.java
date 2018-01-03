package org.jchien.shuffle;

/**
 * @author jchien
 */
public class Pokemon {
    private String name;

    private int msus;

    private int level;

    private int skillLevel;

    private String skill;

    public Pokemon(String name, int msus, int level, int skillLevel, String skill) {
        this.name = name;
        this.msus = msus;
        this.level = level;
        this.skillLevel = skillLevel;
        this.skill = skill;
    }

    public String getName() {
        return name;
    }

    public int getMsus() {
        return msus;
    }

    public int getLevel() {
        return level;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public String getSkill() {
        return skill;
    }
}
