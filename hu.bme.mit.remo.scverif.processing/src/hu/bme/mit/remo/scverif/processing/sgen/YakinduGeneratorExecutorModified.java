package hu.bme.mit.remo.scverif.processing.sgen;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
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

/**
 * Yakindu generator without an IProgressMonitor
 * 
 * @author Pete
 *
 */
public class YakinduGeneratorExecutorModified extends GeneratorExecutor {

	/**
	 * 
	 * @param file
	 * @throws Exception
	 */
	public void executeGeneratorWithInjector(IFile file) throws Exception{
		Resource resource = loadResourceWithInjector(file);
		if (resource == null || resource.getContents().size() == 0
				|| resource.getErrors().size() > 0)
			throw new Exception("Resource could not be loaded with the injector from IFile at '"+file.getRawLocationURI()+"' (does it exist in the current workspace? --> "+file.exists()+")");
		GeneratorModel model = (GeneratorModel) resource.getContents().get(0);

		String generatorId = model.getGeneratorId();
		GeneratorDescriptor description = GeneratorExtensions
				.getGeneratorDescriptorForId(generatorId);
		if (description == null){
			throw new Exception("Could not get the Generator Descriptor for the given generator id (it was null, because the id is unknown)!");
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
		ISchedulingRule buildRule = file.getProject().getWorkspace().getRuleFactory().buildRule();
		generatorJob.setRule(buildRule);
		generatorJob.schedule();		
	}
	
	/**
	 * Execute the generator without using an IProgressMonitor
	 * 
	 * @param file
	 * @return
	 * @throws Exception 
	 */
	public boolean executeGeneratorWithoutIProgressMonitor(IFile file) throws Exception {
		Resource resource = loadResourceWithInjector(file);
		if (resource == null || resource.getContents().size() == 0
				|| resource.getErrors().size() > 0) {
			throw new Exception("Resource could not be loaded with the injector from IFile at '"+file.getRawLocationURI()+"' (does it exist in the current workspace? --> "+file.exists()+")");
		}

		GeneratorModel model = (GeneratorModel) resource.getContents().get(0);
		
		// mod by Pete
		executeGenerator(model);
		return true;
	}
	
	public Resource loadResourceWithInjector(IFile file) {
		// Initialization support for running Xtext languages without equinox extension registry
		com.google.inject.Injector injector = new SGenStandaloneSetup().createInjectorAndDoEMFRegistration();
		ResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		
		Resource resource = null;
		IPath fullPath = file.getFullPath();
		URI uri = URI.createPlatformResourceURI(fullPath.toString(),
				true);
		resource = resourceSet.getResource(uri, true);
		return resource;
	}

}
