package org.jchien.shuffle.parser;

/**
 * @author jchien
 */
public class RawPokemon {
    private String name;
    private String level;
    private String skillLevel;
    private String skillName;
    private String msus;

    public RawPokemon(String name, String level, String skillLevel, String skillName, String msus) {
        this.name = name;
        this.level = level;
        this.skillLevel = skillLevel;
        this.skillName = skillName;
        this.msus = msus;
    }

    public String getName() {
        return name;
    }

    public String getLevel() {
        return level;
    }

    public String getSkillLevel() {
        return skillLevel;
    }

    public String getSkillName() {
        return skillName;
    }

    public String getMsus() {
        return msus;
    }

    @Override
    public String toString() {
        return "RawPokemon{" +
                "name='" + name + '\'' +
                ", level='" + level + '\'' +
                ", skillLevel='" + skillLevel + '\'' +
                ", skillName='" + skillName + '\'' +
                ", msus='" + msus + '\'' +
                '}';
    }
}
