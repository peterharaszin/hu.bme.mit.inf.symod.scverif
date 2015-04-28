package hu.bme.mit.remo.scverif.processing.sct;

/**
 * Forbidden element
 * @author Pete
 *
 */
public class ForbiddenElement {
    String message;
    
    public ForbiddenElement(String message) {
        super();
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
