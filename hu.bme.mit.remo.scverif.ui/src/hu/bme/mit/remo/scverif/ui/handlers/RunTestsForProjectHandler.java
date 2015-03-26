package hu.bme.mit.remo.scverif.ui.handlers;

import hu.bme.mit.remo.scverif.ui.jobs.DoRemoJobs;

import java.awt.List;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

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
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class RunTestsForProjectHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        System.out.println("RunTestsForProjectHandler::execute(), at: '"+RunTestsForProjectHandler.class.getProtectionDomain().getCodeSource().getLocation()+"'.");
        
        try {
        
        Shell shell = HandlerUtil.getActiveShell(event);
        
        IStructuredSelection selection = (IStructuredSelection) HandlerUtil
                .getActiveMenuSelection(event);
        Object firstElement = selection.getFirstElement();
                
        Object[] selectedObjects = selection.toArray();
        
        ArrayList<IProject> iProjectsList = new ArrayList<IProject>();
                
        for (Object selectedObject : selectedObjects) {
            System.out.println("object.getClass().getName(): "+selectedObject.getClass().getName());
            if(selectedObject instanceof IJavaProject){
                IJavaProject javaProject = (IJavaProject) selectedObject;
                System.out.println("javaProject.getElementName(): "+javaProject.getElementName());
                IPath iPath = javaProject.getPath();
                File file = iPath.toFile();
                System.out.println("file.getAbsolutePath(): "+file.getAbsolutePath());
                System.out.println("file.toURI(): "+file.toURI());
                IProject iProject = javaProject.getProject();
                System.out.println("iProject.getRawLocationURI(): "+iProject.getRawLocationURI());
                // add the project to the list
                iProjectsList.add(iProject);
            }
        }
        
        TreeMap<String,IProject> matchingProjects = DoRemoJobs.getMatchingProjects(iProjectsList);      
        
//            DoRemoJobs doRemoJobs = new DoRemoJobs(shell);
//            doRemoJobs.doRemoJobs();
        } catch (Exception e) {
            System.err.println("Something went wrong when executing homework analyzation (exception type: '"+e.getClass().getName()+"'): "+e.getMessage());
            e.printStackTrace();
        }

        return null; // return the result of the execution - reserved for future use, must be null!!
    }
}
