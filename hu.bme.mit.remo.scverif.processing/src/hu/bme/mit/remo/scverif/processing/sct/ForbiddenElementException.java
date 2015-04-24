package hu.bme.mit.remo.scverif.processing.sct;

/**
 * Exception to throw when a forbidden element is used in the model
 * @author Pete
 *
 */
@SuppressWarnings("serial")
public class ForbiddenElementException extends Exception {

    public ForbiddenElementException() {
        super();
    }

    public ForbiddenElementException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ForbiddenElementException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForbiddenElementException(String message) {
        super(message);
    }

    public ForbiddenElementException(Throwable cause) {
        super(cause);
    }

    
}
