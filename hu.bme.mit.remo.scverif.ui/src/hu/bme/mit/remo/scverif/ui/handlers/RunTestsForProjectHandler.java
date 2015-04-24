package hu.bme.mit.remo.scverif.ui.handlers;

import hu.bme.mit.remo.scverif.ui.jobs.DoRemoJobs;

import java.awt.List;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
//import org.slf4j.Logger;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.FileInfoMatcherDescription;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceFilterDescription;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IPluginDescriptor;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class RunTestsForProjectHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Logger logger = DoRemoJobs.logger;
        
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
                        if (selectedObject instanceof IJavaProject) { // Package Explorerből megnyitva
                            IJavaProject javaProject = (IJavaProject) selectedObject;
                            logger.info("javaProject.getElementName(): " + javaProject.getElementName());
                            IPath iPath = javaProject.getPath();
                            File file = iPath.toFile();
                            logger.info("file.getAbsolutePath(): " + file.getAbsolutePath());
                            logger.info("file.toURI(): " + file.toURI());
                            IProject iProject = javaProject.getProject();
                            logger.info("iProject.getRawLocationURI(): " + iProject.getRawLocationURI());
                            // add the project to the list
                            iProjectsList.add(iProject);
                        } else if(selectedObject instanceof IProject){ // Project Explorerből megnyitva
                            IProject iProject = (IProject) selectedObject;
                            logger.info("iProject.getRawLocationURI(): " + iProject.getRawLocationURI());
                            // add the project to the list
                            iProjectsList.add(iProject);                    
                        }
                    }

                    TreeMap<String, IProject> matchingProjects = DoRemoJobs.getMatchingProjects(iProjectsList);

                    logger.info("matchingProjects.size(): "+matchingProjects.size());
                    
                    DoRemoJobs doRemoJobs = new DoRemoJobs(shell);                        
                    doRemoJobs.runTestsOnProjects(matchingProjects);
                } catch (OperationCanceledException e){
                    logger.severe("Operation has been cancelled! "+e.getMessage());
//                    logger.error("Operation has been cancelled! "+e.getMessage());
                    return Status.CANCEL_STATUS;
                } catch (Exception e) {
                    logger.severe(
//                    logger.error(
                            "Something went wrong when executing homework analyzation (exception type: '"
                            + e.getClass().getName() + "'): " + e.getMessage());
                    e.printStackTrace();
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
                    DoRemoJobs.logger.info("Job called '" + processHomeworksJob.getName() + "' completed successfully");
                } else {
                    DoRemoJobs.logger
                        .severe("Job called '" + processHomeworksJob.getName()
//                        .error("Job called '" + processHomeworksJob.getName()
                            + "' did not complete successfully");
                }

                processHomeworksJob.removeJobChangeListener(this); // we don't want to listen to this job anymore
            }
        });
        processHomeworksJob.schedule();        

        return null; // return the result of the execution - reserved for future use, must be null!!
    }
}
