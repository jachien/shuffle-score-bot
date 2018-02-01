package org.jchien.shuffle.model;

/**
 * @author jchien
 */
public class ItemException extends FormatException {
    public ItemException() {
    }

    public ItemException(String message) {
        super(message);
    }

    public ItemException(String message, Throwable cause) {
        super(message, cause);
    }

    public ItemException(Throwable cause) {
        super(cause);
    }

    public ItemException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
