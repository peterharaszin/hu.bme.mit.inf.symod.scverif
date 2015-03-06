package hu.bme.mit.remo.scverif.ui.jobs;

import hu.bme.mit.remo.scverif.processing.sct.StatechartAnalyzer;
import hu.bme.mit.remo.scverif.processing.sgen.YakinduGeneratorExecutorModified;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.progress.IProgressConstants;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
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
	private static final String sgenFilePathInBundle = "model/CallHandling.sgen";
	private static final String sctFilePathInBundle = "model/CallHandling.sct";
	private static final String testFilePathInBundle = "src/MyTest.java";
	private static final Logger logger = Logger.getLogger("RemoLog");
	private static DoRemoJobs.MyConsoleHandler myConsoleHandler = new MyConsoleHandler();
	static {
		java.util.logging.LogManager.getLogManager().reset();
		logger.addHandler(myConsoleHandler);
		logger.setLevel(java.util.logging.Level.ALL);
	}

	/**
	 * We don't want ALL the logging stuffs to be printed out red (e.g. in
	 * Eclipse) - for example, the texts printed out with logger.info("...")
	 * should remain black.
	 * 
	 * @see http 
	 *      ://stackoverflow.com/questions/9794516/change-appengine-console-red
	 *      -color-in-eclipse/16229664#16229664
	 * @author Pete
	 *
	 */
	public static class MyConsoleHandler extends
			java.util.logging.StreamHandler {
		private java.util.logging.Formatter formatter = new SimpleFormatter();

		public void publish(java.util.logging.LogRecord record) {
			if (record.getLevel().intValue() < java.util.logging.Level.WARNING
					.intValue()) {
				System.out.println(formatter.formatMessage(record));
			} else {
				System.err.println(formatter.format(record));
			}
		}
	}

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
	 * 
	 * @return
	 */
	public boolean copySctFile() {
		// ...
		logger.info("Copying SCT file...");
		return true;
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
		logger.info("Executing statechart analyzation job (ReMo)...");
		// http://stackoverflow.com/questions/4480334/how-to-call-a-method-stored-in-a-hashmap-java/4480360#4480360
		// Create a hash map
		LinkedHashMap<String, Callable<Boolean>> remoTasks = new LinkedHashMap<String, Callable<Boolean>>();

		// Put elements to the map
		remoTasks.put("Copying the necessary SCT file...", () -> {
			return copySctFile();
		});

		remoTasks.put("Indicating a clean request...", () -> {
			return cleanProject();
		});

		remoTasks.put("Deleting the content of the 'src-gen' directory...",
				() -> {
					return deleteGeneratedDirectoryContents();
				});

		// remoTasks.put("Generating code from the SCT file...", () -> {
		// return generateCodeFromSctFile();
		// });

		remoTasks.put("Building project...", () -> {
			return buildProject(remoProject);
		});

		remoTasks.put("Analyzing SCT file...", () -> {
			return analyzeSctFile();
		});

		remoTasks.put("Running JUnit test...", () -> {
			return testStatechart();
		});

		// https://eclipse.org/articles/Article-Concurrency/jobs-api.html
		Job remoJob = new Job("Executing ReMo tasks") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				// // Generate instances
				// SubProgressMonitor subProgressMonitor = null;

				try {
					// subProgressMonitor = new SubProgressMonitor(monitor, 1);
					// setProperty(key, value);

					Set<Entry<String, Callable<Boolean>>> entrySet = remoTasks
							.entrySet();
					monitor.beginTask(
							"Starting to prepare analyzing statecharts...",
							entrySet.size());

					for (Entry<String, Callable<Boolean>> remoTask : entrySet) {
						if (monitor.isCanceled()) {
							throw new OperationCanceledException();
							// return Status.CANCEL_STATUS;
						}

						String taskDescription = remoTask.getKey();
						Callable<Boolean> taskMethodToCall = remoTask
								.getValue();
						monitor.subTask(taskDescription);
						Boolean success = taskMethodToCall.call();
						logger.info("check if this job ran successfully: "
								+ success);
						monitor.worked(1);

					}
					logger.info("OK, everything went fine");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					monitor.done();
					logger.info("End of statechart analyzation.");
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

		// "the user will be shown a progress dialog but will be given the option to run the job in the background by clicking a button in the dialog"
		remoJob.setUser(true);
		remoJob.schedule();
	}

	/**
	 * "The user may decide to wait while the reservation is being made or decide to run it in the background. When the job completes the reservation, it checks to see what the user chose to do. [... ] This method checks the IProgressConstants.PROPERTY_IN_DIALOG to see if the job is being run in a dialog. Basically, if the property exists and is the Boolean value true, then the user decided to wait for the results. The results can be shown immediately to the user (by invoking showResults)."
	 * 
	 * @see https://eclipse.org/articles/Article-Concurrency/jobs-api.html
	 * @param job
	 * @return
	 */
	public boolean isModal(Job job) {
		Boolean isModal = (Boolean) job
				.getProperty(IProgressConstants.PROPERTY_IN_DIALOG);
		if (isModal == null)
			return false;
		return isModal.booleanValue();
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
			logger.info("There are no projects in this workspace!");
		} else {

			logger.info("Iterating through projects in current workspace...");

			for (IProject currentProject : projects) {
				logger.info("currentProject.getName(): "
						+ currentProject.getName());
			}
		}
	}

	/**
	 * Indicates a clean request.
	 * 
	 * @return
	 * 
	 * @throws CoreException
	 */
	public boolean cleanProject() throws CoreException {
		logger.info("Indicating a clean request...");
		remoProject.build(IncrementalProjectBuilder.CLEAN_BUILD, null);
		return true;
	}

	/**
	 * Deleting the content of the "src-gen" directory recursively. TODO: lehet,
	 * hogy felesleges külön, a generáltatás megcsinálja
	 * 
	 * @return
	 * 
	 * @throws IOException
	 */
	public boolean deleteGeneratedDirectoryContents() throws IOException {
		logger.info("Deleting the content of the 'src-gen' directory...");

		IFolder srcGenFolder = remoProject.getFolder("src-gen");
		Path srcGenDirectory = Paths.get(srcGenFolder.getRawLocationURI());

		logger.info("directory.toString(): " + srcGenDirectory.toString());
		logger.info("directory exists: " + Files.exists(srcGenDirectory));

		// Path directory = Paths.get("./src-gen");

		if (Files.exists(srcGenDirectory)) {
			Files.walkFileTree(srcGenDirectory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file,
						java.nio.file.attribute.BasicFileAttributes attrs)
						throws IOException {
					logger.info("file visited: " + file.toString());
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir,
						IOException exc) throws IOException {
					logger.info("dir (postVisitDirectory): " + dir.toString());

					// we do NOT want to delete the src-gen directory itself
					// (because in this case, the sgen file indicates a
					// frustrating warning because of the missing directory)
					if (dir != srcGenDirectory) {
						Files.delete(dir);
					}
					return FileVisitResult.CONTINUE;
				}

			});
		}

		return true;
	}

	/**
	 * Build Java project.
	 * 
	 * @param project
	 * @throws Exception
	 */
	public boolean buildProject(IProject project) throws Exception {
		logger.info("Building project '" + project.getName() + "'...");
		// open if necessary
		if (!project.isOpen()) {
			project.open(null);
		}

		// projects with the Java nature
		boolean isJavaNatureEnabled = project.isNatureEnabled(JDT_NATURE);

		logger.info("isJavaNatureEnabled: " + isJavaNatureEnabled);

		IMarker[] problemMarkers = project.findMarkers(IMarker.PROBLEM, true,
				IResource.DEPTH_INFINITE);
		if (problemMarkers.length == 0) {
			logger.info("No problem markers...");
		} else {
			for (IMarker problemMarker : problemMarkers) {
				logger.info("problemMarker: " + problemMarker);
			}
		}
		// project.getWorkspace().

		logger.info("Building project...");
		// nem szeretnénk monitort, ezért null a második paraméter
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);
		// project.build(IncrementalProjectBuilder.AUTO_BUILD, null);
		// project.build(IncrementalProjectBuilder.FULL_BUILD,
		// "org.eclipse.jdt.core.javabuilder", null, null);

		logger.info("OK, building the project is ready!");
		return true;
	}

	/**
	 * Generate statechart implementation code from an *.sgen file (code
	 * generator model)
	 * 
	 * @return
	 * @throws Exception
	 */
	public boolean generateCodeFromSctFile() throws Exception {
		// getting IFile
		IFile sgenFile = remoProject.getFile(sgenFilePathInBundle);
		boolean sgenFileExists = sgenFile.exists();

		logger.info("Generating code from file at '"
				+ sgenFile.getRawLocationURI() + "' ('"
				+ sgenFile.getFullPath().toString() + "')...");

		if (!sgenFileExists) {
			throw new Exception("The '" + sgenFile.getRawLocationURI()
					+ "' file does not exist!");
		}

		logger.info("Trying to generate code...");

		// GeneratorExecutor generatorExecutor = new GeneratorExecutor();
		// generatorExecutor.executeGenerator(sgenFile);

		YakinduGeneratorExecutorModified yakinduGeneratorExecutorModified = new YakinduGeneratorExecutorModified();
		yakinduGeneratorExecutorModified
				.executeGeneratorWithoutIProgressMonitor(sgenFile);
		// yakinduGeneratorExecutorModified.executeGeneratorWithInjector(sgenFile);
		logger.info("Code generation was successful!");

		return true;
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

	public boolean analyzeSctFile() {
		return true;
	}

	public boolean testStatechart() throws Exception {
		// Statechart statechartFromIFile = getStatechartFromBundle();

		IFile jUnitTestFile = remoProject.getFile(testFilePathInBundle);
		boolean testFileExists = jUnitTestFile.exists();
		URI testFileRawLocationURI = jUnitTestFile.getRawLocationURI();

		logger.info("Executing test from file at '" + testFileRawLocationURI
				+ "' ('" + jUnitTestFile.getFullPath().toString() + "')...");
		logger.info("testFileExists: " + testFileExists);

		if (!testFileExists) {
			throw new Exception("The test file at '" + testFileRawLocationURI
					+ "' does not exist!");
		}

		logger.info("testFileRawLocationURI.getRawPath(): "
				+ testFileRawLocationURI.getRawPath());
		logger.info("testFileRawLocationURI.getPath(): "
				+ testFileRawLocationURI.getPath());
		logger.info("testFileRawLocationURI.toString(): "
				+ testFileRawLocationURI.toString());
		logger.info("testFileRawLocationURI.toURL().getPath(): "
				+ testFileRawLocationURI.toURL().getPath());
		logger.info("testFileRawLocationURI.resolve('..').resolve('..'): "
				+ testFileRawLocationURI.resolve("..").resolve("..").toString());
		
		JavaCompiler systemJavaCompiler = ToolProvider.getSystemJavaCompiler();
		if (systemJavaCompiler == null) {
			String javaHomeProperty = System.getProperty("java.home");
			// http://stackoverflow.com/questions/2543439/null-pointer-exception-while-using-java-compiler-api/10944833#10944833
			System.err
					.println("You are using JRE instead of JDK. Your JAVA_HOME is currently set to '"
							+ javaHomeProperty + "'");
			throw new Exception(
					"You are using JRE instead of JDK. Your JAVA_HOME is currently set to '"
							+ javaHomeProperty
							+ "'. Please set the JRE System Library execution environment in Eclipse's settings to the path of the JDK for the testing to work.");
		}

		IFolder binFolder = remoProject.getFolder("bin");
		String testFileLocation = jUnitTestFile.getRawLocationURI().getPath();
		String binDirectory = binFolder.getRawLocationURI().getPath();
		
		// List<String> options = Arrays.asList("-d", "./bin/", testFileLocation);
		// http://stackoverflow.com/questions/13075370/dynamically-retrieving-junit-class
		List<String> arguments = Arrays.asList("-d", binDirectory,
				testFileLocation);

		logger.info("binDirectory: " + binDirectory);
		logger.info("options: " + arguments);

		// int compilationResult = systemJavaCompiler.run(null, null, null, arguments.toArray(new String[arguments.size()]));

		// if (compilationResult == 0) {
		// logger.info("Compilation was successful");

		String testClassName = "MyTest";

		// bin-könyvtár elérési útja
		File file = new File(binDirectory);

		URL url = null;
		URLClassLoader myURLClassLoader = null;
		try {
			url = file.toURI().toURL();
			logger.info("url: " + url);
			logger.info("url.getPath(): " + url.getPath());
			
			System.out.flush();
			
			URL[] urls = { url };
			// majd átnézni:
			// http://www.coderanch.com/t/633874/java/java/Unload-Reload-Dymnically-Loaded-Class
			myURLClassLoader = new URLClassLoader(urls,
					JUnitCore.class.getClassLoader());

			// Class<?> classForName = Class.forName(testClassName, true,
			// myURLClassLoader);

			Class<?> loadClassResult = myURLClassLoader.loadClass(
			// "com.example.tests.blabla"
					testClassName);

//			 System.out.println(loadClassResult.getDeclaredMethods().length);
//			 Object o = loadClassResult.newInstance();

			org.junit.runner.Result result = JUnitCore
					.runClasses(loadClassResult);

			for (Failure failure : result.getFailures()) {
				logger.warning(failure.toString());
			}

		} catch (MalformedURLException e) {
			logger.severe("Error with file location (URL)");
			// e.printStackTrace();
			throw e;
		} catch (ClassNotFoundException e) {
			logger.severe("Couldn't find test class to load");
			// e.printStackTrace();
			throw e;
		} finally {
			if (myURLClassLoader != null) {
				myURLClassLoader.close();
			}
		}

		// } else {
		// throw new Exception("Compilation failed!");
		// }

		// org.junit.runner.JUnitCore junit1 = new org.junit.runner.JUnitCore();
		// org.junit.runner.Result result1 = junit1.run(SCTTest.class);

		// TreeIterator<EObject> eAllContents =
		// statechart.eAllContents();
		// StatechartAnalyzer stateChartAnalyzer = new StatechartAnalyzer(
		// statechartFromIFile);
		// stateChartAnalyzer.processStatechart();
		// org.junit.runner.JUnitCore.main("junitfaq.SimpleTest");
		// .
		// http://stackoverflow.com/questions/2543912/how-do-i-run-junit-tests-from-inside-my-java-application/2562575#2562575
		// org.junit.runner.JUnitCore junit = new org.junit.runner.JUnitCore();
		// org.junit.runner.Result result = junit.run(SCTTest.class);
		// // org.junit.runner.Result result =
		// JUnitCore.runClasses(SCTTest.class);
		// boolean wasSuccessful = result.wasSuccessful();
		//
		// int failureCount = result.getFailureCount();
		// int runCount = result.getRunCount();
		// logger.info("wasSuccessful: " + wasSuccessful);
		// logger.info("failureCount: " + failureCount);
		// logger.info("runCount: " + runCount);
		// logger.info("result.getFailures().toString(): "
		// + result.getFailures().toString());
		//
		// if (failureCount > 0) {
		// String problems = "";
		// for (Failure failure : result.getFailures()) {
		// problems.concat(failure.getMessage() + " (descr.: "
		// + failure.getDescription() + "). failure.toString(): "
		// + failure.toString());
		// }
		//
		// throw new Exception(
		// "The SCT file has not passed the test! Problems while analyzing the file: \n"
		// + problems);
		// }
		//
		// return (failureCount == 0);
		return true;
	}
}
