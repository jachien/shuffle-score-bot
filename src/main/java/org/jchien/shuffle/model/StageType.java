package org.jchien.shuffle.model;

/**
 * @author jchien
 */
public enum StageType {
    COMPETITION("!comp"),
    ESCALATION_BATTLE("!eb %s"),
    NORMAL("!run %s"),
    ROSTER("!roster"); // this doesn't really belong here

    private final String headerFormat;

    StageType(String headerFormat) {
        this.headerFormat = headerFormat;
    }

    public String getHeader(String normalizedStage) {
        if (normalizedStage == null) {
            normalizedStage = "";
        }
        // seems like it doesn't matter if I give more arguments than formatRuns specifiers
        return String.format(headerFormat, normalizedStage).trim();
    }
}
