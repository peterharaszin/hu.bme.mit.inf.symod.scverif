package hu.bme.mit.inf.symod.scverif.processing.sct;

import org.eclipse.emf.ecore.EObject;

/**
 * Class to instantiate when a model element is missing in the statechart
 * provided by the student when comparing it to a reference statechart.
 * 
 * It contains the name of the missing element and its EObject instance.
 * 
 * @author Peter Haraszin
 *
 */
public class MissingEObject {
    String message;
    EObject eObject;
       
    public MissingEObject(String message, EObject eObject) {
        super();
        this.message = message;
        this.eObject = eObject;
    }

    public MissingEObject(String message) {
        super();
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }

    public EObject getEObject() {
        return eObject;
    }

    @Override
    public String toString() {
        return message;
    }
}
