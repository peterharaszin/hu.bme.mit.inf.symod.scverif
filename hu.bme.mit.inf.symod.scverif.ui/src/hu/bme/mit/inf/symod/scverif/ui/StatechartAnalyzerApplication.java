package hu.bme.mit.inf.symod.scverif.ui;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class StatechartAnalyzerApplication implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		System.out.println("Statechart analyzer application has started");
		return null;
	}

	@Override
	public void stop() {
		System.out.println("Statechart analyzer application has stopped");
	}

}
