package org.jchien.shuffle.parser.exception;

import org.jchien.shuffle.parser.Section;

/**
 * @author jchien
 */
public class DupeSectionException extends FormatException {
    private final Section dupeSection;

    public DupeSectionException(Section dupeSection) {
        super("Duplicate section found: " + dupeSection.toString().toLowerCase());
        this.dupeSection = dupeSection;
    }

    public Section getDupeSection() {
        return dupeSection;
    }
}
