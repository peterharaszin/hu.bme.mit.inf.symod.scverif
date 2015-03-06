package hu.bme.mit.remo.scverif.processing.sgen;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.yakindu.sct.generator.core.GeneratorExecutor;
import org.yakindu.sct.generator.core.ISCTGenerator;
import org.yakindu.sct.generator.core.extensions.GeneratorExtensions;
import org.yakindu.sct.generator.core.extensions.GeneratorExtensions.GeneratorDescriptor;
import org.yakindu.sct.generator.genmodel.SGenStandaloneSetup;
import org.yakindu.sct.model.sgen.GeneratorEntry;
import org.yakindu.sct.model.sgen.GeneratorModel;

import com.google.inject.Injector;

/**
 * Yakindu generator without an IProgressMonitor
 * 
 * @author Pete
 *
 */
public class YakinduGeneratorExecutorModified extends GeneratorExecutor {

	public void executeGeneratorWithInjector(IFile file){
		Resource resource = loadResourceWithInjector(file);
		if (resource == null || resource.getContents().size() == 0
				|| resource.getErrors().size() > 0)
			return;
		GeneratorModel model = (GeneratorModel) resource.getContents().get(0);

		String generatorId = model.getGeneratorId();
		GeneratorDescriptor description = GeneratorExtensions
				.getGeneratorDescriptorForId(generatorId);
		if (description == null){
			return;
		}
		final ISCTGenerator generator = description.createGenerator();
		final EList<GeneratorEntry> entries = model.getEntries();
		Job generatorJob = new Job("Execute SCT Genmodel " + file.getName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				for (GeneratorEntry generatorEntry : entries) {
					if (monitor.isCanceled()) {
						break;
					}
					generator.generate(generatorEntry);
				}
				return Status.OK_STATUS;
			}
		};
		generatorJob.setRule(file.getProject().getWorkspace().getRuleFactory()
				.buildRule());
		generatorJob.schedule();		
	}
	
	public boolean executeGeneratorWithoutIProgressMonitor(IFile file) {
		Resource resource = loadResourceWithInjector(file);
		if (resource == null || resource.getContents().size() == 0
				|| resource.getErrors().size() > 0) {
			return false;
		}

		GeneratorModel model = (GeneratorModel) resource.getContents().get(0);
		
		// mod by Pete
		executeGenerator(model);
		return true;
	}
	
	protected Resource loadResourceWithInjector(IFile file) {
		Injector injector = new SGenStandaloneSetup().createInjectorAndDoEMFRegistration();
		ResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		
		Resource resource = null;
		URI uri = URI.createPlatformResourceURI(file.getFullPath().toString(),
				true);
		resource = resourceSet.getResource(uri, true);
		return resource;
	}

}
