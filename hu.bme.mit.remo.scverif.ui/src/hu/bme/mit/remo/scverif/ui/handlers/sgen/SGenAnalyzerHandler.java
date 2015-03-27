package hu.bme.mit.remo.scverif.ui.handlers.sgen;

import hu.bme.mit.remo.scverif.processing.sgen.YakinduGeneratorExecutorModified;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

public class SGenAnalyzerHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// get the active shell, which is not necessarily the active workbench
		// window shell; we pass the execution event that contains the
		// application context
		Shell shell = HandlerUtil.getActiveShell(event);
		// get the active menu selection. The active menu is a registered
		// context menu
		ISelection sel = HandlerUtil.getActiveMenuSelection(event);
		IStructuredSelection selection = (IStructuredSelection) sel;

		// átmenetileg, amíg a toolbar ikonját normálisan le nem kezelem
		if (selection == null) {
			MessageDialog
					.openInformation(shell, "Info (Pete)",
							"No elements have been selected (OR temp: toolbar icon is not working yet!)");
			return null;
		}

		// Returns the first element in this selection, or null if the selection
		// is empty.
		Object firstElement = selection.getFirstElement();
		
		// átmenetileg, amíg a toolbar ikonra való kattintást normálisan le nem
		// kezelem
		if (firstElement == null) {
			MessageDialog
					.openInformation(shell, "Info (Pete)",
							"No elements have been selected (OR temp: toolbar icon is not working yet!)");
			return null;
		}
		
		// the filter is set in the command declaration - no need for type
		// checking
		IFile file = (IFile) firstElement;
		
		try {
			System.out.println("Trying to generate code...");
			
//			GeneratorExecutor generatorExecutor = new GeneratorExecutor();
//			generatorExecutor.executeGenerator(file);
			
//			// eredeti helyett a módosított leszármazott osztállyal:
			YakinduGeneratorExecutorModified generatorExecutorModified = new YakinduGeneratorExecutorModified();
			boolean executionSuccessful = generatorExecutorModified.executeGeneratorWithoutIProgressMonitor(file);
		
			MessageDialog.openInformation(shell, "System Modeling - code generation is ready", "Code has been generated from the file at '"+file.getRawLocationURI()+"'! Result: "+executionSuccessful);
		} catch (Exception e) {
			System.out.println("Code generation failed:");
			e.printStackTrace();
			MessageDialog.openError(shell, "System Modeling - code generation failed", "An error occurred while trying to generate code from the file at '"+file.getRawLocationURI()+"': "+e.getMessage());
		} finally {
			System.out.println("End of code generation.");
		}
		
		return null;
	}	
}