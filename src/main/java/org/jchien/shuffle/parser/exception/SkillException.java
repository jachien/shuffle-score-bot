package org.jchien.shuffle.parser.exception;

/**
 * @author jchien
 */
public class SkillException extends FormatException {
    public SkillException() {
    }

    public SkillException(String message) {
        super(message);
    }

    public SkillException(String message, Throwable cause) {
        super(message, cause);
    }

    public SkillException(Throwable cause) {
        super(cause);
    }

    public SkillException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
