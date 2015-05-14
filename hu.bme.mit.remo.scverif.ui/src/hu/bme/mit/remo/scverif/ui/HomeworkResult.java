package hu.bme.mit.remo.scverif.ui;

import java.util.ArrayList;
import java.util.LinkedList;

import org.junit.runner.Result;

import hu.bme.mit.remo.scverif.processing.sct.ForbiddenElement;
import hu.bme.mit.remo.scverif.processing.sct.MissingEObject;

/**
 * Container class for storing the result of a homework test
 * 
 * @author Peter Haraszin
 *
 */
public class HomeworkResult {
    String tasks;
    String neptunCode;
    org.junit.runner.Result testResult;
    LinkedList<ForbiddenElement> staticAnalysisResultForbiddenElementsInModel;
    ArrayList<MissingEObject> staticAnalysisResultMissingElementsInInterface;
    boolean isSctUploaded;
    Exception exceptionThrown;
    
    public HomeworkResult(String neptunCode, boolean isSctUploaded, LinkedList<ForbiddenElement> staticAnalysisResult, ArrayList<MissingEObject> missingElementsInInterface, Result testResult, String tasks, Exception exceptionThrown) {
        this.tasks = tasks;
        this.neptunCode = neptunCode;
        this.testResult = testResult;
        this.staticAnalysisResultForbiddenElementsInModel = staticAnalysisResult;
        this.staticAnalysisResultMissingElementsInInterface = missingElementsInInterface;
        this.isSctUploaded = isSctUploaded;
        this.exceptionThrown = exceptionThrown;
    }

    public String getTasks() {
        return tasks;
    }
    
    public org.junit.runner.Result getTestResult() {
        return testResult;
    }
    
    public LinkedList<ForbiddenElement> getForbiddenElementsInModel() {
        return staticAnalysisResultForbiddenElementsInModel;
    }
    
    public ArrayList<MissingEObject> getMissingElementsInInterface() {
        return staticAnalysisResultMissingElementsInInterface;
    }
    
    public boolean isSctUploaded() {
        return isSctUploaded;
    }

    public String getNeptunCode() {
        return neptunCode;
    }  
    
    public Exception getExceptionThrown() {
        return exceptionThrown;
    }

    @Override
    public String toString() {
        String returnString = "HomeworkResult [neptunCode=" + neptunCode + ", tasks=" + tasks + ", testResult failure count=" + testResult.getFailureCount() + ". Result of static analysis: forbidden elements: ";
        for (ForbiddenElement forbiddenElement : staticAnalysisResultForbiddenElementsInModel) {
            returnString += " " + forbiddenElement.getMessage() + ", ";
        }
        
        returnString += " missing elements in the interface: ";
        
        for (MissingEObject missingEObject : staticAnalysisResultMissingElementsInInterface) {
            returnString += " " + missingEObject.getMessage() + ", ";
        }
        
        return returnString;
    }
}
