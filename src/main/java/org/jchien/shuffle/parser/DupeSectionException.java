package org.jchien.shuffle.parser;

/**
 * @author jchien
 */
public class DupeSectionException extends Exception {
    private final Section dupeSection;

    public DupeSectionException(Section dupeSection) {
        this.dupeSection = dupeSection;
    }

    public DupeSectionException(Section dupeSection, String message) {
        super(message);
        this.dupeSection = dupeSection;
    }

    public Section getDupeSection() {
        return dupeSection;
    }
}
