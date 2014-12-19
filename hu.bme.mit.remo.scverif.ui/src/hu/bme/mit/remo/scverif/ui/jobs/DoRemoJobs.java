package hu.bme.mit.remo.scverif.ui.jobs;

import hu.bme.mit.remo.scverif.processing.sct.StatechartAnalyzer;
import hu.bme.mit.remo.scverif.ui.handlers.sct.SCTTest;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.yakindu.sct.generator.core.GeneratorExecutor;
import org.yakindu.sct.model.sgraph.Statechart;

/**
 * 1. sct-fájl átmásolása X helyről 2. sct-fájl bejárása 3. kód legenerálása az
 * sct-fájl+sgen-fájl segítségével 4. buildelés
 * 
 * @author Pete
 *
 */
public class DoRemoJobs {
	private static final String JDT_NATURE = "org.eclipse.jdt.core.javanature";
	private static final String myBundleName = "hu.bme.mit.remo.scverif.test.callhandling";
	// private static final String myBundleName =
	// "hu.peterharaszin.quicktest.java";
	private static final String sgenFilePathInProject = "model/CallHandling.sgen";
	private final static String sctFilePathInBundle = "model/CallHandling.sct";
	private Logger logger = Logger.getLogger("RemoLog");
	private FileHandler logFilehandler;
	//
	private IProject remoProject;

	public DoRemoJobs() throws Exception {
		setLogger();
		remoProject = getProject(myBundleName);

		// open if necessary (no action is taken if the project is already open)
		remoProject.open(null);
	}

	/**
	 * Copy the SCT file from one place to another
	 * 
	 * TODO
	 */
	public void copySctFile() {
		// ...
		System.out.println("Copying SCT file...");
	}

	/**
	 * Get the project we will continue to work on.
	 * 
	 * @param bundleName
	 * 
	 * @return
	 * @throws Exception
	 */
	public IProject getProject(String bundleName) throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot workspaceRoot = workspace.getRoot();
		IProject project = workspaceRoot.getProject(myBundleName);
		if (!project.exists()) {
			throw new Exception("ERROR: Project called '" + myBundleName
					+ "' does not exist!");
		}

		return project;
	}

	private void setLogger() throws SecurityException, IOException {
		// get current working directory ()
		String currentWorkingDirectory = java.nio.file.Paths.get(".")
				.toAbsolutePath().normalize().toString();
		// configuring the logger with the handler and formatter
		logFilehandler = new FileHandler(currentWorkingDirectory
				+ "/RemoLogFile.log");
		logger.addHandler(logFilehandler);
		SimpleFormatter formatter = new SimpleFormatter();
		logFilehandler.setFormatter(formatter);
	}

	public void doRemoJobs() {
		Job remoJob = new Job("Executing ReMo tasks") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (monitor.isCanceled()) {
						// ...
					}
					int numberOfTasks = 6;
					monitor.beginTask("Copying the necessary SCT file...",
							numberOfTasks);
					TimeUnit.SECONDS.sleep(3);
					copySctFile();
					monitor.worked(1);

					monitor.setTaskName("Indicating a clean request...");
					TimeUnit.SECONDS.sleep(3);
					cleanProject();
					monitor.worked(1);

					monitor.setTaskName("Deleting the content of the 'src-gen' directory...");
					TimeUnit.SECONDS.sleep(3);
					deleteGeneratedDirectoryContents();
					monitor.worked(1);

					monitor.setTaskName("Generating code from the SCT file...");
					TimeUnit.SECONDS.sleep(3);
					generateCodeFromSctFile();
					monitor.worked(1);

					monitor.setTaskName("Building project...");
					TimeUnit.SECONDS.sleep(1);
					buildProject(remoProject);
					monitor.worked(1);

					monitor.setTaskName("Analyzing SCT file...");
					TimeUnit.SECONDS.sleep(1);
					analyzeSctFile();
					monitor.worked(1);

					monitor.setTaskName("Running JUnit test...");
					TimeUnit.SECONDS.sleep(1);
					testStatechart();
					monitor.worked(1);

					System.out.println("OK, everything went fine");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return Status.CANCEL_STATUS;
				} finally {
					monitor.done();
					System.out.println("Ready.");
				}
				return Status.OK_STATUS;
			}
		};

		// get the scheduling rule that is required for building a project or
		// the entire workspace
		// ISchedulingRule buildRule =
		// remoProject.getWorkspace().getRuleFactory()
		// .buildRule();
		// remoJob.setRule(buildRule);
		remoJob.schedule();
	}

	/**
	 * List all the projects in the currently available workspace (which you can
	 * override with the -data argument)
	 */
	public void listProjectsInWorkspace() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot workspaceRoot = workspace.getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		if (projects.length == 0) {
			System.out.println("There are no projects in this workspace!");
		} else {

			System.out
					.println("Iterating through projects in current workspace...");

			for (IProject currentProject : projects) {
				System.out.println("currentProject.getName(): "
						+ currentProject.getName());
			}
		}
	}

	/**
	 * Indicates a clean request.
	 * 
	 * @throws CoreException
	 */
	public void cleanProject() throws CoreException {
		System.out.println("Indicating a clean request...");
		remoProject.build(IncrementalProjectBuilder.CLEAN_BUILD, null);
	}

	/**
	 * Deleting the content of the "src-gen" directory recursively. TODO: lehet,
	 * hogy felesleges külön, a generáltatás megcsinálja
	 * 
	 * @throws IOException
	 */
	public void deleteGeneratedDirectoryContents() throws IOException {
		System.out.println("Deleting src-gen...");

		IFolder srcGenFolder = remoProject.getFolder("src-gen");
		Path directory = Paths.get(srcGenFolder.getRawLocationURI());

		System.out.println("directory.toString(): " + directory.toString());

		// Path directory = Paths.get("./src-gen");

		Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file,
					java.nio.file.attribute.BasicFileAttributes attrs)
					throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc)
					throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

		});
	}

	/**
	 * Build Java project.
	 * 
	 * @param project
	 * @throws Exception
	 */
	public void buildProject(IProject project) throws Exception {
		System.out.println("Building project '" + project.getName() + "'...");
		// open if necessary
		if (!project.isOpen()) {
			project.open(null);
		}

		// projects with the Java nature
		boolean isJavaNatureEnabled = project.isNatureEnabled(JDT_NATURE);

		System.out.println("isJavaNatureEnabled: " + isJavaNatureEnabled);

		IMarker[] problemMarkers = project.findMarkers(IMarker.PROBLEM, true,
				IResource.DEPTH_INFINITE);
		if (problemMarkers.length == 0) {
			System.out.println("No problem markers...");
		} else {
			for (IMarker problemMarker : problemMarkers) {
				System.out.println("problemMarker: " + problemMarker);
			}
		}

		System.out.println("Building project...");
		// nem szeretnénk monitort, ezért null a második paraméter
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		// project.build(IncrementalProjectBuilder.AUTO_BUILD, null);
		// project.build(IncrementalProjectBuilder.FULL_BUILD,
		// "org.eclipse.jdt.core.javabuilder", null, null);

		System.out.println("OK, building the project is ready!");
	}

	public void generateCodeFromSctFile() throws Exception {
		// getting IFile
		IFile sgenFile = remoProject.getFile(sgenFilePathInProject);
		boolean sgenFileExists = sgenFile.exists();

		System.out.println("Generating code from file at '"
				+ sgenFile.getRawLocationURI() + "' ('"
				+ sgenFile.getFullPath().toString() + "')...");

		if (!sgenFileExists) {
			throw new Exception("The " + sgenFile.getRawLocationURI()
					+ " file does not exist!");
		}

		System.out.println("Trying to generate code...");
		GeneratorExecutor generatorExecutor = new GeneratorExecutor();
		generatorExecutor.executeGenerator(sgenFile);
		System.out.println("OK, generating the code went successful");
	}

	public Statechart getStatechartFromBundle(String bundleName,
			String sctFilePathInBundle) throws Exception {
		IProject project = getProject(bundleName);
		IFile file = project.getFile(sctFilePathInBundle);
		Statechart statechartFromIFile = StatechartAnalyzer
				.getStatechartFromIFile(file);

		if (statechartFromIFile == null) {
			throw new Exception(
					"Statechart could not be parsed from the file at path '"
							+ file.getRawLocationURI().getPath() + "'!");
		}
		return statechartFromIFile;
	}

	public Statechart getStatechartFromBundle() {
		IFile sctFile = remoProject.getFile(sctFilePathInBundle);
		return StatechartAnalyzer.getStatechartFromIFile(sctFile);
	}

	public void analyzeSctFile() throws Exception {
//		Statechart statechartFromIFile = getStatechartFromBundle();

		// TreeIterator<EObject> eAllContents =
		// statechart.eAllContents();
//		StatechartAnalyzer stateChartAnalyzer = new StatechartAnalyzer(
//				statechartFromIFile);
		// stateChartAnalyzer.processStatechart();
		// org.junit.runner.JUnitCore.main("junitfaq.SimpleTest");
		// http://stackoverflow.com/questions/2543912/how-do-i-run-junit-tests-from-inside-my-java-application/2562575#2562575
		org.junit.runner.JUnitCore junit = new org.junit.runner.JUnitCore();
		org.junit.runner.Result result = junit.run(SCTTest.class);
		boolean wasSuccessful = result.wasSuccessful();
		int failureCount = result.getFailureCount();
		int runCount = result.getRunCount();
		System.out.println("wasSuccessful: " + wasSuccessful);
		System.out.println("failureCount: " + failureCount);
		System.out.println("runCount: " + runCount);
		
	}

	public void testStatechart() {
		// ...
		
	}

}
