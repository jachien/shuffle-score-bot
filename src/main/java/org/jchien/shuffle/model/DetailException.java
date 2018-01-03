package org.jchien.shuffle.model;

/**
 * @author jchien
 */
public class DetailException extends Exception {
    public DetailException() {
    }

    public DetailException(String message) {
        super(message);
    }

    public DetailException(String message, Throwable cause) {
        super(message, cause);
    }

    public DetailException(Throwable cause) {
        super(cause);
    }

    public DetailException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
