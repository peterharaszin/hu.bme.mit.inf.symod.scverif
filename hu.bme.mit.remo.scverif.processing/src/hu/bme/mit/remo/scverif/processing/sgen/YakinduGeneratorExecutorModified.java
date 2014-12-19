package hu.bme.mit.remo.scverif.processing.sgen;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.resource.Resource;
import org.yakindu.sct.generator.core.GeneratorExecutor;
import org.yakindu.sct.model.sgen.GeneratorModel;

/**
 * Yakindu generator without an IProgressMonitor
 * 
 * @author Pete
 *
 */
public class YakinduGeneratorExecutorModified extends GeneratorExecutor {

	public boolean executeGeneratorWithoutIProgressMonitor(IFile file) {
		Resource resource = loadResource(file);
		if (resource == null || resource.getContents().size() == 0
				|| resource.getErrors().size() > 0) {
			return false;
		}

		GeneratorModel model = (GeneratorModel) resource.getContents().get(0);

		// mod by Pete
		executeGenerator(model);
		return true;
	}
}
