package hu.bme.mit.remo.scverif.ui.handlers;

import hu.bme.mit.remo.scverif.ui.jobs.DoRemoJobs;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class StatechartProcessingHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			DoRemoJobs doRemoJobs = new DoRemoJobs();
			doRemoJobs.doRemoJobs();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null; // return the result of the execution - reserved for future use, must be null!!
	}

	/**
	 * 
	 * @return
	 * @see http
	 *      ://stackoverflow.com/questions/11335491/how-to-programmatically-change
	 *      -the-selection-within-package-explorer/20282030#20282030
	 */
	private IWorkbenchPart getActivePart() {
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final IWorkbenchWindow activeWindow = workbench
				.getActiveWorkbenchWindow();
		if (activeWindow != null) {
			final IWorkbenchPage activePage = activeWindow.getActivePage();
			if (activePage != null) {
				return activePage.getActivePart();
			}
		}
		return null;
	}
}
