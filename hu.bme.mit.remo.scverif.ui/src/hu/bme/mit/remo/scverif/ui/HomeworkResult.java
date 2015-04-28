package hu.bme.mit.remo.scverif.ui;

import java.util.LinkedList;

import org.junit.runner.Result;

import hu.bme.mit.remo.scverif.processing.sct.ForbiddenElement;

public class HomeworkResult {
    String tasks;
    String neptunCode;
    org.junit.runner.Result testResult;
    LinkedList<ForbiddenElement> staticAnalysisResult;
    
    public HomeworkResult(String neptunCode, LinkedList<ForbiddenElement> staticAnalysisResult, Result testResult, String tasks) {
        this.tasks = tasks;
        this.neptunCode = neptunCode;
        this.testResult = testResult;
        this.staticAnalysisResult = staticAnalysisResult;
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

    @Override
    public String toString() {
        String returnString = "HomeworkResult [neptunCode=" + neptunCode + ", tasks=" + tasks + ", testResult failure count=" + testResult.getFailureCount() + ". Result of static analysis: ";
        for (ForbiddenElement forbiddenElement : staticAnalysisResult) {
            returnString += " " + forbiddenElement.getMessage() + ", ";
        }
        
        return returnString;
    }
}
