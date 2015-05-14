package hu.bme.mit.remo.scverif.ui.handlers;

import java.nio.file.Path;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import hu.bme.mit.remo.scverif.ui.jobs.DoRemoJobs;

/**
 * Handler for doing the tests for all the potential projects in the workspace
 * 
 * @author Peter Haraszin
 *
 */
public class StatechartProcessingHandler extends AbstractHandler {

    /**
     * 
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final Shell shell = HandlerUtil.getActiveShell(event);
        Logger logger = DoRemoJobs.logger;
        
        // https://eclipse.org/articles/Article-Concurrency/jobs-api.html
        final Job processHomeworksJob = new Job("Processing homeworks...") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                // // Generate instances
                // SubProgressMonitor subProgressMonitor = null;
                
                try {
                    DoRemoJobs doRemoJobs = new DoRemoJobs(shell);
                    TreeMap<String, IProject> matchingProjectsInWorkspace = DoRemoJobs.getMatchingProjectsInWorkspace();
                    
                    // subProgressMonitor = new SubProgressMonitor(monitor, 1);
                    // setProperty(key, value);

                    logger.info("StatechartProcessingHandler::execute(), at: '"
                            + StatechartProcessingHandler.class.getProtectionDomain().getCodeSource().getLocation()
                            + "'.");
                    
//                    doRemoJobs.cleanAndFullBuildAllProjectsInWorkspace(null);
                    //                  doRemoJobs.waitForBuildWithJobChangeAdapter();
                    // doRemoJobs.waitForAutoAndManualBuild();
                    doRemoJobs.requestAutoBuildAndWaitForIt();

                    logger.info("============ OK, WAITING FOR AUTOBUILD DONE ==============");
                    Path summaryFilePath = doRemoJobs.runTestsOnProjects(matchingProjectsInWorkspace, monitor);

                    // Display.getDefault().syncExec(new Runnable() {
                    Display.getDefault().asyncExec(new Runnable() {
                        public void run() {
                            MessageDialog.openInformation(shell, "Job done", "OK, everything went fine for " + matchingProjectsInWorkspace.size() + " projects. "
                                    + "You can find the summary file at '"+summaryFilePath.toUri()+"'.");
                        }
                    });
                } catch (final Exception e) {
                    logger.info("Job ended with errors. (StatechartProcessingHandler::execute()). "
                            + "Something went wrong when executing homework analyzation (exception type: '"
                            + e.getClass().getName() + "'): " + e.getMessage());
                    e.printStackTrace();
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            MessageDialog.openError(shell, "Job finished with errors", e.getMessage());
                        }
                    });
                } finally {
                    monitor.done();
                    logger.info("End of job. (StatechartProcessingHandler::execute())");
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
                    logger.info("Job called '" + processHomeworksJob.getName() + "' completed successfully");
                } else {
                    logger.severe(
//                    logger.error(
                            "Job called '" + processHomeworksJob.getName()
                            + "' did not complete successfully");
                }

                processHomeworksJob.removeJobChangeListener(this); // is it really needed?
            }
        });
        processHomeworksJob.schedule();

        return null; // return the result of the execution - reserved for future use, must be null!!
    }

    /**
     * 
     * @return
     * @see http://stackoverflow.com/questions/11335491/how-to-programmatically-change-the-selection-within-package-explorer/20282030#20282030
     */
    @SuppressWarnings("unused")
    private IWorkbenchPart getActivePart() {
        final IWorkbench workbench = PlatformUI.getWorkbench();
        final IWorkbenchWindow activeWindow = workbench.getActiveWorkbenchWindow();
        if (activeWindow != null) {
            final IWorkbenchPage activePage = activeWindow.getActivePage();
            if (activePage != null) {
                return activePage.getActivePart();
            }
        }
        return null;
    }
}
