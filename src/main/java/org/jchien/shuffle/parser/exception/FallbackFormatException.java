package org.jchien.shuffle.parser.exception;

/**
 * @author jchien
 */
public class FallbackFormatException extends FormatException {
    public FallbackFormatException(Throwable cause) {
        super("An error occurred while generating the error message for this." +
                      " Embarrassing. Please report this to /u/jcrixus.", cause);
    }
}
