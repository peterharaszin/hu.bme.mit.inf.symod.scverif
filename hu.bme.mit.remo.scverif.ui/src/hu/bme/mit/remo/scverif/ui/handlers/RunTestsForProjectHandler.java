package hu.bme.mit.remo.scverif.ui.handlers;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Logger;
//import org.slf4j.Logger;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import hu.bme.mit.remo.scverif.ui.jobs.DoStatechartProcessing;

/**
 * Handler for running the homework tests on the selected project(s)
 * 
 * @author Peter Haraszin
 *
 */
public class RunTestsForProjectHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Logger logger = DoStatechartProcessing.logger;

        logger.info("RunTestsForProjectHandler::execute(), at: '"
                + RunTestsForProjectHandler.class.getProtectionDomain().getCodeSource().getLocation() + "'.");

        final Shell shell = HandlerUtil.getActiveShell(event);

        // https://eclipse.org/articles/Article-Concurrency/jobs-api.html
        final Job processHomeworksJob = new Job("Processing homeworks...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                // // Generate instances
                // SubProgressMonitor subProgressMonitor = null;                

                try {

                    IStructuredSelection selection = (IStructuredSelection) HandlerUtil.getActiveMenuSelection(event);
                    Object[] selectedObjects = selection.toArray();

                    ArrayList<IProject> iProjectsList = new ArrayList<IProject>();

                    for (Object selectedObject : selectedObjects) {
                        logger.info("object.getClass().getName(): " + selectedObject.getClass().getName());
                        if (selectedObject instanceof IJavaProject) {// Package Explorerből megnyitva
                            IJavaProject javaProject = (IJavaProject) selectedObject;
                            logger.info("javaProject.getElementName(): " + javaProject.getElementName());
                            IPath iPath = javaProject.getPath();
                            File file = iPath.toFile();
                            logger.info("file.getAbsolutePath(): " + file.getAbsolutePath());
                            logger.info("file.toURI(): " + file.toURI());
                            IProject iProject = javaProject.getProject();
                            logger.info("iProject.getRawLocationURI(): " + iProject.getRawLocationURI());
                            logger.info("iProject.getLocationURI(): " + iProject.getLocationURI());
                            // add the project to the list
                            iProjectsList.add(iProject);
                        } else if (selectedObject instanceof IProject) {// Project Explorerből megnyitva
                            IProject iProject = (IProject) selectedObject;
                            logger.info("iProject.getRawLocationURI(): " + iProject.getRawLocationURI());
                            logger.info("iProject.getLocationURI(): " + iProject.getLocationURI());
                            // add the project to the list
                            iProjectsList.add(iProject);
                        }
                    }

                    TreeMap<String, IProject> matchingProjects = DoStatechartProcessing.getMatchingProjects(iProjectsList);

                    logger.info("matchingProjects.size(): " + matchingProjects.size());

                    DoStatechartProcessing doRemoJobs = new DoStatechartProcessing(shell);
                    Path summaryFilePath = doRemoJobs.runTestsOnProjects(matchingProjects);

//                    Display.getDefault().asyncExec(new Runnable() {
//                        public void run() {
//                            MessageDialog.openInformation(shell, "Job done",
//                                    "OK, running tests went fine for " + matchingProjects.size() + " projects."
//                                            + " You can find the summary file at '" + summaryFilePath.toUri() + "'.");
//                        }
//                    });
                    // http://eclipsesource.com/blogs/2014/03/24/how-to-use-swt-with-java-8/
                    Display.getDefault().asyncExec(() -> {
                        MessageDialog.openInformation(shell, "Job done",
                            "OK, running tests went fine for " + matchingProjects.size() + " projects."
                                    + " You can find the summary file at '" + summaryFilePath.toUri() + "'.");
                        }
                );
                    

                } catch (OperationCanceledException e) {
                    String cancelMessage = "Running the tests has been canceled! ("+e.getMessage()+")";
                    logger.severe(cancelMessage);
                    // http://eclipsesource.com/blogs/2014/03/24/how-to-use-swt-with-java-8/
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageDialog.openInformation(shell, "Job canceled", cancelMessage);
                        }
                    });                    
                    return Status.CANCEL_STATUS;
                } catch (Exception e) {
                    StackTraceElement[] stackTrace = e.getStackTrace();
                    StackTraceElement firstStackTraceElement = stackTrace[0];

                    String exceptionMessage = "Something went wrong when executing homework analyzation (exception type: '"
                            + e.getClass().getName() + "'): " + e.getMessage() + " (file: '"
                            + firstStackTraceElement.getFileName() + "', line: "
                            + firstStackTraceElement.getLineNumber() + ", method name: "
                            + firstStackTraceElement.getMethodName() + ")";
                    logger.severe(exceptionMessage);
                    e.printStackTrace();

                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            MessageDialog.openError(shell, "Job finished with errors", exceptionMessage);
                        }
                    });

                    return Status.CANCEL_STATUS;
                }

                return Status.OK_STATUS;
            }
        };

        // "the user will be shown a progress dialog but will be given the option to run the job in the background by clicking a button in the dialog"
        processHomeworksJob.setUser(true);
        processHomeworksJob.setPriority(Job.LONG);

        processHomeworksJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                if (event.getResult().isOK()) {
                    DoStatechartProcessing.logger.info("Job called '" + processHomeworksJob.getName() + "' completed successfully");
                } else {
                    DoStatechartProcessing.logger.severe("Job called '" + processHomeworksJob.getName()
                    //                        .error("Job called '" + processHomeworksJob.getName()
                            + "' did not complete successfully");
                }

                processHomeworksJob.removeJobChangeListener(this);// we don't want to listen to this job anymore
            }
        });
        processHomeworksJob.schedule();

        return null;// return the result of the execution - reserved for future use, must be null!!
    }
}
