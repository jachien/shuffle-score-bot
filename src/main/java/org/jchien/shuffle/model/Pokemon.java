package org.jchien.shuffle.model;

/**
 * @author jchien
 */
public class Pokemon {
    private String name;

    private Integer level;

    private Integer skillLevel;

    private String skillName;

    private Integer msus;

    private Integer maxMsus;

    public Pokemon(String name, Integer level, Integer skillLevel, String skillName, Integer msus, Integer maxMsus) {
        this.name = name;
        this.level = level;
        this.skillLevel = skillLevel;
        this.skillName = skillName;
        this.msus = msus;
        this.maxMsus = maxMsus;
    }

    public String getName() {
        return name;
    }

    public Integer getLevel() {
        return level;
    }

    public Integer getSkillLevel() {
        return skillLevel;
    }

    public String getSkillName() {
        return skillName;
    }

    public Integer getMsus() {
        return msus;
    }

    public Integer getMaxMsus() {
        return maxMsus;
    }

    @Override
    public String toString() {
        return "Pokemon{" +
                "name='" + name + '\'' +
                ", level=" + level +
                ", skillLevel=" + skillLevel +
                ", skillName='" + skillName + '\'' +
                ", msus=" + msus +
                ", maxMsus=" + maxMsus +
                '}';
    }
}
