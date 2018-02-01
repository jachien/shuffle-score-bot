package org.jchien.shuffle.model;

import java.util.List;

/**
 * @author jchien
 */
public class ParsedComment {

    private final List<UserRunDetails> validRuns;

    private final List<UserRunDetails> invalidRuns;

    public ParsedComment(List<UserRunDetails> validRuns, List<UserRunDetails> invalidRuns) {
        this.validRuns = validRuns;
        this.invalidRuns = invalidRuns;
    }

    public List<UserRunDetails> getValidRuns() {
        return validRuns;
    }

    public List<UserRunDetails> getInvalidRuns() {
        return invalidRuns;
    }
}
