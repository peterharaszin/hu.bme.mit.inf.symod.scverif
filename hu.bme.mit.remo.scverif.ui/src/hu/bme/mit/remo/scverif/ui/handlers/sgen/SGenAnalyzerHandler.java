package hu.bme.mit.remo.scverif.ui.handlers.sgen;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.yakindu.sct.generator.core.GeneratorExecutor;

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
		
		System.out.println("Trying to generate code...");
		GeneratorExecutor generatorExecutor = new GeneratorExecutor();
		generatorExecutor.executeGenerator(file);
		
//		// eredeti helyett a módosított leszármazott osztállyal:
//		YakinduGeneratorExecutorModified generatorExecutorModified = new YakinduGeneratorExecutorModified();
//		boolean executionSuccessful = generatorExecutorModified.executeGeneratorWithoutIProgressMonitor(file);
//		
//		if(executionSuccessful){
//			System.out.println("Code generation was successful!");
//		}
//		else {
//			System.out.println("Code generation has failed!");
//		}
		
		MessageDialog.openInformation(shell, "Info (Pete)",
				"(TEMP MESSAGE) Code has been generated!");
		
		return null;
	}	
}
