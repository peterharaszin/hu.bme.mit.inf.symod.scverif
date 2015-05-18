package hu.bme.mit.remo.scverif.ui.handlers.sct;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.yakindu.sct.model.sgraph.Statechart;
import org.yakindu.sct.model.stext.stext.InterfaceScope;

import hu.bme.mit.remo.scverif.processing.sct.StatechartAnalyzer;
import hu.bme.mit.remo.scverif.ui.jobs.DoStatechartVerification;

/**
 * Handler for analyzing the selected SCT file
 * 
 * @author Peter Haraszin
 *
 */
public class SCTAnalyzeHandler extends AbstractHandler {

    /**
     * Execute the analyzations
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        // Return the active shell. Is not necessarily the active workbench
        // window shell; we pass the execution event that contains the
        // application context
        Shell shell = HandlerUtil.getActiveShell(event);

        IFile selectedSctFile = getSelectedSctFile(event);

        if (selectedSctFile == null) {
            MessageDialog.openError(shell, "ReMo info", "Please select an SCT file!");
            return null;
        }

        analyzeSctFile(selectedSctFile, shell);

        return null;
    }

    public void analyzeSctFile(IFile file, Shell shell) {

        // Loads the resource
        ResourceSet resourceSet = new ResourceSetImpl();
        URI fileURI = URI.createPlatformResourceURI(file.getFullPath().toString(), false);
        Resource res = resourceSet.getResource(fileURI, true);

        // Process SCT model
        for (EObject content : res.getContents()) {
            // EObject content = res.getContents().get(0);
            // if it's an implementation of the model object 'Statechart'.
            if (content instanceof Statechart) {
                Statechart statechart = (Statechart) content;
                // TreeIterator<EObject> eAllContents =
                // statechart.eAllContents();
                StatechartAnalyzer stateChartAnalyzer = new StatechartAnalyzer(statechart);
                stateChartAnalyzer.processStatechart();

                Logger logger = DoStatechartVerification.logger;

                logger.info("getting interfaces... (dirty test)");

                ArrayList<InterfaceScope> interfaces = stateChartAnalyzer.getInterfaces();
                if (interfaces == null) {
                    logger.info("There is no interface in the model");
                } else {
                    for (InterfaceScope interfaceScope : interfaces) {
                        logger.info("current interface's name: '" + interfaceScope.getName() + "'");
                    }
                }

//                // TODO: it was just a test!
//                String anotherSpecification = "/*************************\n" + "   Interfaces of the chess clock\n"
//                        + "   Do not modify!\n" + " *************************/\n" + "interface Buttons:\n"
//                        + "in event button\n" + "\n" + "interface Display:\n" + "var text:string = \"Initial text\"\n"
//                        + "\n" + " internal:\n" + "/*************************\n"
//                        + "   Insert additional variables here:\n" + " *************************/\n" + " \n"
//                        + "// var myExampleInteger: integer\n" + "// var myExampleText: string\n";
//                boolean interfaceSpecificationEquals = stateChartAnalyzer
//                        .interfaceSpecificationEqualsTo(anotherSpecification);
//                System.out.println("interfaceSpecificationEquals: " + interfaceSpecificationEquals);
            }
            // if (content instanceof org.eclipse.gmf.runtime.notation.impl.DiagramImpl) {
            //     // ez nem fog kelleni...
            // }
        }

        MessageDialog.openInformation(shell, "Info (Pete)",
                "(TEMP MESSAGE) Please look at the console for inspecting the iteration of the statechart...");
    }

    /**
     * Returns the selected SCT file or null if no SCT file has been selected
     * 
     * TODO: fix the ugly methods 
     * 
     * @param event
     * @return
     */
    public IFile getSelectedSctFile(ExecutionEvent event) {

        // we'll try 2 methods:
        // 1. when we click on a given file with the right mouse button and try to process the file
        // 2. when we click on the toolbar icon and have to look for the selected file

        // 1st try:

        // Return the active menu selection. The active menu is a registered context menu; 
        // we pass the execution event that contains the application context		
        ISelection currentSelection = HandlerUtil.getActiveMenuSelection(event);
        IStructuredSelection currentSelectionStructured = (IStructuredSelection) currentSelection;
        // Returns the first element in this selection, or null if the selection is empty.
        Object firstElement = (currentSelectionStructured != null) ? currentSelectionStructured.getFirstElement()
                : null;

        //        if (firstElement != null && firstElement.getClass().getTypeName() == "org.eclipse.core.internal.resources.File") {
        if (firstElement != null && firstElement instanceof IFile) {
            IFile file = (IFile) firstElement;
            String fileExtension = file.getFileExtension();
            return (DoStatechartVerification.SCT_FILE_EXTENSION.equals(fileExtension) ? file : null);
        } else {
            return null;
        }

        // TODO: create 2nd try
        //		// 2nd try (e.g. when clicking the icon on the toolbar)
        //		currentSelection = HandlerUtil.getCurrentSelection(event);
        //		if (currentSelectionStructured == null) {
        //			return null;
        //		}
        //
        //		firstElement = currentSelectionStructured.getFirstElement();
        //
        //		if (firstElement != null
        //				&& firstElement.getClass().getTypeName() == fileTypeClass) {
        //			IFile file = (IFile) firstElement;
        //			String fileExtension = file.getFileExtension();			
        //			return (DoStatechartVerification.SCT_FILE_EXTENSION.equals(fileExtension) ? file
        //					: null);
        //		} else {
        //			return null;
        //		}
    }
}
