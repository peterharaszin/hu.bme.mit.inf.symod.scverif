package hu.bme.mit.remo.scverif.processing.sct;

/**
 * Class to instantiate when a forbidden element was found in the statechart.
 * It contains a simple informative message about the found forbidden element.
 * 
 * @author Peter Haraszin
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
