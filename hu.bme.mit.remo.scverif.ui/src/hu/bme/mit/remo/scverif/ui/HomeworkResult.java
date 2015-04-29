package hu.bme.mit.remo.scverif.ui;

import java.util.LinkedList;

import org.junit.runner.Result;

import hu.bme.mit.remo.scverif.processing.sct.ForbiddenElement;

public class HomeworkResult {
    String tasks;
    String neptunCode;
    org.junit.runner.Result testResult;
    LinkedList<ForbiddenElement> staticAnalysisResult;
    boolean isSctUploaded;
    Exception exceptionThrown;
    
    public HomeworkResult(String neptunCode, boolean isSctUploaded, LinkedList<ForbiddenElement> staticAnalysisResult, Result testResult, String tasks, Exception exceptionThrown) {
        this.tasks = tasks;
        this.neptunCode = neptunCode;
        this.testResult = testResult;
        this.staticAnalysisResult = staticAnalysisResult;
        this.isSctUploaded = isSctUploaded;
        this.exceptionThrown = exceptionThrown;
    }

    public String getTasks() {
        return tasks;
    }
    
    public org.junit.runner.Result getTestResult() {
        return testResult;
    }
    
    public LinkedList<ForbiddenElement> getStaticAnalysisResult() {
        return staticAnalysisResult;
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
        String returnString = "HomeworkResult [neptunCode=" + neptunCode + ", tasks=" + tasks + ", testResult failure count=" + testResult.getFailureCount() + ". Result of static analysis: ";
        for (ForbiddenElement forbiddenElement : staticAnalysisResult) {
            returnString += " " + forbiddenElement.getMessage() + ", ";
        }
        
        return returnString;
    }
}
