package hu.bme.mit.remo.scverif.ui.handlers.sct;

import java.util.List;
import java.util.TreeMap;
import java.util.logging.Logger;
//import org.slf4j.Logger;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import hu.bme.mit.remo.scverif.ui.jobs.DoStatechartVerification;

/**
 * Handler for copying the SCT file to the appropriate project (if any) in the workspace
 *  
 * @author Peter Haraszin
 *
 */
public class SctCopyToIProjects extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Logger logger = DoStatechartVerification.logger;
        logger.info("Copying SCT files (SctCopyToIProjects::execute)...");

        // Return the active shell. Is not necessarily the active workbench
        // window shell; we pass the execution event that contains the
        // application context
        // org.eclipse.swt.widgets.Shell shell = HandlerUtil.getActiveShell(event);
        ISelection activeMenuSelection = HandlerUtil.getActiveMenuSelection(event);
        IStructuredSelection activeMenuStructuredSelection = (IStructuredSelection) activeMenuSelection;

        TreeMap<String, IProject> matchingProjectsInWorkspace = DoStatechartVerification.getMatchingProjectsInWorkspace();

        List<?> selectionList = activeMenuStructuredSelection.toList();
        for (Object currentSelection : selectionList) {
            IFile currentSelectionAsIFile = (IFile) currentSelection;
            String fileExtension = currentSelectionAsIFile.getFileExtension();
            
            if (DoStatechartVerification.SCT_FILE_EXTENSION.equals(fileExtension)) {
                String filename = currentSelectionAsIFile.getName();
                String filenameWithoutExtension = filename.substring(0, (filename.length() - fileExtension.length() - 1)); // -1 because of the dot (e.g. TEST.txt --> TEST and not "TEST.")
                logger.info("filename: " + filename);
                IProject iProject = matchingProjectsInWorkspace.get(filenameWithoutExtension);
                // if there is a project matching what we found
                if(iProject != null && iProject.exists()){
                    // then copy...
                    // logger.info("OK, there is a matching project for '"+filename+"': "+iProject.getName()+" at '"+iProject.getRawLocationURI()+"'.");
                    logger.info("OK, there is a matching project for '"+filename+"': "+iProject.getName()+" at '"+iProject.getLocationURI()+"'.");
                }
            }
        }

        return null;
    }
}
