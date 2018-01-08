package org.jchien.shuffle.model;

/**
 * @author jchien
 */
public enum StageType {
    COMPETITION("!comp"),
    ESCALATION_BATTLE("!eb %s"),
    NORMAL("!run %s");

    private final String headerFormat;

    StageType(String headerFormat) {
        this.headerFormat = headerFormat;
    }

    public String getHeader(String normalizedStage) {
        return String.format(headerFormat, normalizedStage);
    }
}
