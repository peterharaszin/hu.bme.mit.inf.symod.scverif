package hu.bme.mit.remo.scverif.ui.jobs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.ui.progress.IProgressService;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.yakindu.sct.model.sgen.GeneratorEntry;
import org.yakindu.sct.model.sgen.GeneratorModel;
import org.yakindu.sct.model.sgraph.Statechart;

import hu.bme.mit.remo.scverif.processing.sct.ForbiddenElement;
import hu.bme.mit.remo.scverif.processing.sct.StatechartAnalyzer;
import hu.bme.mit.remo.scverif.processing.sgen.YakinduGeneratorExecutorModified;
import hu.bme.mit.remo.scverif.ui.BuildError;
import hu.bme.mit.remo.scverif.ui.HomeworkResult;
import hu.bme.mit.remo.scverif.ui.YakinduSCTFileNotFoundException;
import hu.bme.mit.remo.scverif.ui.YakinduSGenFileNotFoundException;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 * 1. sct-fájl átmásolása X helyről
 * 2. sct-fájl bejárása
 * 3. kód legenerálása az sct-fájl+sgen-fájl segítségével
 * 4. buildelés
 * 
 * @author Peter Haraszin
 */
public class DoRemoJobs {
    public static final String SCT_FILE_EXTENSION = "sct";
    public static final String SGEN_FILE_EXTENSION = "sgen";
    public static final String yakindu_BUILDER_ID = "org.yakindu.sct.builder.SCTBuilder";

    // the relative path of the sgen file in the IProject
    private static final String sgenFilePathInBundle = "yakindu/homework2java." + SGEN_FILE_EXTENSION;
    // the package name that contains the test file in every homework projects
    private static final String packageNameContainingTestCases = "hu.bme.mit.inf.symod.homework.generic.tests";
    private static final String testClassName = "TestCases";
    // the full class name of the test file (containing the package name) for dynamic class loading
    private static final String testFullClassNameWithPackage = packageNameContainingTestCases + "." + testClassName;

    private static final String testCompiledClassFolderPathInIProject = "bin/"
            + packageNameContainingTestCases.replace(".", "/") + "/";
    private static final String testCompiledClassFileFullPathInIProject = testCompiledClassFolderPathInIProject
            + testClassName + ".class";

    private Shell parentActiveShell;
    private IWorkspaceRoot workspaceRoot;
    private IWorkspace workspace;

    // Delimiter used in CSV file
    private static final String CSV_COMMA_DELIMITER = ";";
    // new line separator
    private static final String NEW_LINE = "\n";
    private static final String CSV_targetFilenamePrefix = "TestResults";
    //    private static final String CSV_targetFilename;
    private static final String dateFormatPattern = "yyyy-MM-dd HH-mm-ss";
    private static final SimpleDateFormat csvSimpleDateFormatForColumn = new SimpleDateFormat(dateFormatPattern);
    private static final SimpleDateFormat csvSimpleDateFormatForFilename = new SimpleDateFormat(
            dateFormatPattern.replace(' ', '_'));

    public static final Logger logger = Logger.getLogger("System Modeling Log");
    //    public static final Logger logger = LoggerFactory.getLogger(DoRemoJobs.class);
    private static DoRemoJobs.MyConsoleHandler myConsoleHandler = new MyConsoleHandler();
    private static final String projectRegex = "hu\\.bme\\.mit\\.inf\\.symod\\.(\\w{6})\\.homework";;
    private static final Pattern patternCompiled = Pattern.compile(projectRegex, Pattern.CASE_INSENSITIVE);

    /**
     * Setting the Logger
     */
    static {
        java.util.logging.LogManager.getLogManager().reset();
        logger.addHandler(myConsoleHandler);
        logger.setLevel(java.util.logging.Level.ALL);
    }

    /**
     * We don't want ALL the logging stuffs to be printed out red (e.g. in Eclipse) 
     * - for example, the texts printed out with logger.info("...") should remain black.
     * 
     * @see http://stackoverflow.com/questions/9794516/change-appengine-console-red-color-in-eclipse/16229664#16229664
     * @author Pete
     */
    public static class MyConsoleHandler extends java.util.logging.StreamHandler {
        private java.util.logging.Formatter formatter = new java.util.logging.SimpleFormatter();

        public void publish(java.util.logging.LogRecord record) {
            if (record.getLevel().intValue() < java.util.logging.Level.WARNING.intValue()) {
                System.out.println(formatter.formatMessage(record));
                // flushing the stream, because the error output stream is not buffered, and this way the messages can mix:
                // (@see http://stackoverflow.com/questions/9146257/why-do-system-err-statements-get-printed-first-sometimes)
                System.out.flush();
            } else {
                System.out.flush();// flush the outstream here too - not to get mixed
                System.err.println(formatter.format(record));
                System.err.flush();
            }
        }
    }

    /**
     * Constructor
     * @param shell
     * @throws Exception
     */
    public DoRemoJobs(Shell shell) throws Exception {
        this.parentActiveShell = shell;
        // setLogger();

        workspace = ResourcesPlugin.getWorkspace();
        workspaceRoot = workspace.getRoot();
    }

    /**
     * Get the root directory based on the project passed as a parameter
     * - we assume that ALL the to-be-checked projects are in the very same directory!
     * 
     * @param iProject
     * @return
     */
    public Path getProjectRootDirectoryFromIProject(IProject iProject) {
        // using rawLocation is not OK: http://stackoverflow.com/questions/20493654/why-does-iresource-getrawlocation-return-null-for-iproject/20494107#20494107
        // IPath location = iProject.getLocation();
        URI locationURI = iProject.getLocationURI();
        // going up one directory - don't know why yet, but ".." goes up TWO directories
        URI projectsRootDirectoryURI = locationURI.resolve(".");
        logger.info("projectRootDirectoryURI: " + projectsRootDirectoryURI);
        return Paths.get(projectsRootDirectoryURI);
    }

    /**
     * Get the first IProject that matches the regular expression (for example for determining the root directory for all the other projects (we assume that the projects to process are in the same directory)) 
     * 
     * @return
     */
    public IProject getFirstMatchingProjectInWorkspace() {
        IWorkspaceRoot iWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] iProjects = iWorkspaceRoot.getProjects();
        Matcher matcher = null;

        for (IProject iProject : iProjects) {
            String projectName = iProject.getName();
            matcher = patternCompiled.matcher(projectName);
            logger.info("projectName: '" + projectName + "'");
            if (matcher.find()) {
                //                 String neptunCode = matcher.group(1);
                return iProject;
            }
        }
        return null;
    }

    public static TreeMap<String, IProject> getMatchingProjectsInWorkspace() {
        return getMatchingProjects(Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()));
    }

    /**
     * Get projects matching the regular expression.
     * 
     * @param projects An array of IProject objects.
     * @return A TreeMap of Neptun-code - IProject pairs.
     */
    public static TreeMap<String, IProject> getMatchingProjects(List<IProject> projects) {
        TreeMap<String, IProject> matchingIProjects = new TreeMap<String, IProject>();

        Matcher matcher = null;

        for (IProject iProject : projects) {
            String projectName = iProject.getName();
            matcher = patternCompiled.matcher(projectName);
            logger.info("projectName: '" + projectName + "'");
            if (matcher.find()) {
                String neptunCode = matcher.group(1);
                matchingIProjects.put(neptunCode, iProject);
            } else {
                // logger.info("Ignoring project called '" + projectName + "' at '" + iProject.getRawLocationURI()
                logger.info("Ignoring project called '" + projectName + "' at '" + iProject.getLocationURI()
                        + "', because it does not match the following regular expression: " + projectRegex);
            }
        }

        return matchingIProjects;
    }

    /**
     * Returns true if .sgen file exists in the project
     * 
     * @param project
     * @return
     */
    public boolean sGenFileExistsInIProject(IProject project) {
        return project.getFile(sgenFilePathInBundle).exists();
    }

    public void checkExistenceOfSGenFileInIProject(IProject project) throws YakinduSGenFileNotFoundException {
        logger.info("Checking if the sgen file exists in the project called '" + project.getName() + "' at '"
        // + project.getRawLocationURI() + "'...");
                + project.getLocationURI() + "'...");
        IFile sgenFile = project.getFile(sgenFilePathInBundle);
        if (!sgenFile.exists()) {
            throw new YakinduSGenFileNotFoundException(
                    // "Yakindu generator file has not been found at the following path: '" + sgenFile.getRawLocationURI()
                    "Yakindu generator file has not been found at the following path: '" + sgenFile.getLocationURI()
                            + "' (relative path: '" + sgenFilePathInBundle + "') in the following project: '"
                            + project.getName() + "'");
        }
        logger.info("OK, the sgen file exists in '" + project.getName() + "'.");
    }

    public void checkExistenceOfSCTFileInRootDirectory(Path projectsRootDirectoryPath, String sctFileName)
            throws YakinduSCTFileNotFoundException {
        Path sctFilePathInRootDirectory = projectsRootDirectoryPath.resolve(sctFileName);

        logger.info("Checking if '" + sctFileName + "' exists in '" + sctFilePathInRootDirectory.toUri() + "'...");

        if (!Files.exists(sctFilePathInRootDirectory)) {
            throw new YakinduSCTFileNotFoundException("Yakindu statechart file ('" + sctFileName
                    + "') has not been found at the following path: '" + sctFilePathInRootDirectory.toUri() + "'!");
        }
        logger.info("OK, the sct file '" + sctFileName + "' exists.");
    }

    /**
     * Returns true if SCT file exists in the root directory
     * 
     * @param projectsRootDirectoryPath
     * @param sctFilename
     * @return
     */
    public boolean sctFileExistsInRootDirectory(Path projectsRootDirectoryPath, String sctFilename) {
        Path sctFilePathInRootDirectory = projectsRootDirectoryPath.resolve(sctFilename);
        logger.info("Checking if '" + sctFilename + "' exists in '" + sctFilePathInRootDirectory.toUri() + "'...");
        return Files.exists(sctFilePathInRootDirectory);
    }

    /**
     * Copy the SCT file from one place to another
     * 
     * @param currentEntry
     *            <Neptun-code, IProject> pair
     * @return
     * @throws IOException
     */
    public boolean copySctFile(Path sourcePath, String sctFileNameInRootDirectory, IProject iProject)
            throws IOException {
        // String neptunCode = currentEntry.getKey();
        // IProject iProject = currentEntry.getValue();

        logger.info("Copying SCT file (project name: '" + iProject.getName() + "')...");

        Path statechartOriginalPath = sourcePath.resolve(sctFileNameInRootDirectory);

        if (!sctFileExistsInRootDirectory(sourcePath, sctFileNameInRootDirectory)) {
            return false;
        }

        String statechartTargetFilename = "homework" + sctFileNameInRootDirectory;// e.g.
        // "homeworkA3BC1G.sct"
        // Path iProjectPath = Paths.get(iProject.getRawLocationURI());
        Path iProjectPath = Paths.get(iProject.getLocationURI());
        Path statechartTargetPath = iProjectPath.resolve(statechartTargetFilename);

        // we create a backup
        if (Files.exists(statechartTargetPath)) {
            String statechartTargetBackupFilename = statechartTargetFilename + "."
                    + (new SimpleDateFormat("yyyy-MM-dd_hh-mm-ss").format(new Date())) + ".BAK";
            logger.info("Statechart file already existed at '" + statechartTargetPath.toUri() + "', renaming it to '"
                    + statechartTargetBackupFilename + "'.");
            Files.move(statechartTargetPath, statechartTargetPath.resolveSibling(statechartTargetBackupFilename));
        }

        Path copyPath = Files.copy(statechartOriginalPath, statechartTargetPath);
        logger.info("Copying the statechart from '" + statechartOriginalPath.toUri() + "' to '"
                + statechartTargetPath.toUri() + "' " + (Files.exists(copyPath) ? "was successful" : "failed") + ".");

        return true;
    }

    /**
     * Get the project we will continue to work on.
     * 
     * @param bundleName
     * @return
     * @throws Exception
     */
    public IProject getProject(String bundleName) throws Exception {
        IProject project = workspaceRoot.getProject(bundleName);
        if (!project.exists()) {
            throw new Exception("ERROR: Project called '" + bundleName + "' does not exist!");
        }

        return project;
    }

    //    @SuppressWarnings("unused")
    //    private void setLogger() throws SecurityException, IOException {
    //        // get current working directory
    //        String currentWorkingDirectory = java.nio.file.Paths.get(".").toAbsolutePath().normalize().toString();
    //        // configuring the logger with the handler and formatter 
    //        logFilehandler = new FileHandler(currentWorkingDirectory + "/RemoLogFile.log");
    //        logger.addHandler(logFilehandler);
    //        SimpleFormatter formatter = new SimpleFormatter();
    //        logFilehandler.setFormatter(formatter);
    //    }

    /**
     * Run all the tests on a given project.
     * 
     * TODO: fix waiting for full build, enabling cleaning request again, making it work even without automatic build ticked...
     * 
     * @param projectsRootDirectoryPath
     * @param neptunCode
     * @param currentIProject
     * @return
     * @throws Exception
     */
    public HomeworkResult runTestsOnProject(Path projectsRootDirectoryPath, String neptunCode,
            IProject currentIProject) {

        // TODO: feladatokat valahonnan beállítani
        String tasks = "";
        LinkedList<ForbiddenElement> forbiddenElementsInStatechart = null;
        Result result = null;
        boolean sctFileExistsInRootDirectory = false;
        Exception exceptionThrown = null;

        try {
            NullProgressMonitor nullProgressMonitor = new NullProgressMonitor();

            logger.info("Neptun: " + neptunCode + "; project name: " + currentIProject.getName() + ", location URI: "
            // + currentIProject.getRawLocationURI()
                    + currentIProject.getLocationURI());

            String sctFileNameInRootDirectory = neptunCode + "." + SCT_FILE_EXTENSION;// e.g. "A3BC1G.sct"

            checkExistenceOfSGenFileInIProject(currentIProject);

            sctFileExistsInRootDirectory = sctFileExistsInRootDirectory(projectsRootDirectoryPath,
                    sctFileNameInRootDirectory);

            if (sctFileExistsInRootDirectory) {
                // checkExistenceOfSCTFileInRootDirectory(projectsRootDirectoryPath, sctFileNameInRootDirectory);

                currentIProject.open(nullProgressMonitor);
                // refreshing in case e.g. an sgen file has been added since the last
                // build (so this way the new files get "recorded" in the workspace)
                currentIProject.refreshLocal(IResource.DEPTH_INFINITE, nullProgressMonitor);

                //        deleteYakinduTargetFolderContents(currentIProject);

                // cleaning does NOT seem to work synchronously even if some sources found on the internet say so... so this has been commented out (and it's not necessarily needed). 
                // cleanProject(currentIProject, nullProgressMonitor);

                copySctFile(projectsRootDirectoryPath, sctFileNameInRootDirectory, currentIProject);

                checkBuildErrors(currentIProject);

                buildProject(currentIProject, nullProgressMonitor);

                forbiddenElementsInStatechart = getForbiddenElementsInStatechart(currentIProject, neptunCode);

                result = testStatechart(currentIProject);
            }
        } catch (final Exception e) {
            exceptionThrown = e;

            logger.severe("Problems occurred while trying to process the project called '" + currentIProject.getName()
            // + "' at '" + currentIProject.getRawLocationURI() + "'. Message: " + e.getMessage());
                    + "' at '" + currentIProject.getLocationURI() + "'. Message: " + e.getMessage());
        }

        HomeworkResult homeworkResult = new HomeworkResult(neptunCode, sctFileExistsInRootDirectory,
                forbiddenElementsInStatechart, result, tasks, exceptionThrown);
        return homeworkResult;
    }

    /**
     * Check for build errors in the project - throws an exception if it has any.
     * 
     * @param project
     * @throws CoreException
     * @throws BuildError
     */
    public void checkBuildErrors(IProject project) throws CoreException, BuildError {
        logger.info("Checking build errors...");

        if (hasBuildErrors(project)) {
            String buildErrorsAsUserFriendlyMessage = getBuildErrorsAsUserFriendlyMessageInProject(project);
            throw new BuildError("Build error: "
                    // "A build error occurred in the project called '" + project.getName() + "'"
                    // + " (at '" + project.getLocationURI() + "')"
                    + buildErrorsAsUserFriendlyMessage);
        }
    }

    /**
     * Get problem markers' messages as user friendly messages.
     * 
     * @param project
     * @return
     * @throws CoreException
     */
    public String getBuildErrorsAsUserFriendlyMessageInProject(IProject project) throws CoreException {
        IMarker[] problemMarkers = getProblemMarkers(project);

        if (problemMarkers == null || problemMarkers.length == 0) {
            return null;
        }

        String sgenFileProblemMarkerType = "org.yakindu.sct.generator.genmodel.ui.sgen.check.fast";
        String sctFileProblemMarkerType = "org.yakindu.sct.ui.editor.diagnostic";

        StringBuilder buildErrors = new StringBuilder("");

        for (IMarker marker : problemMarkers) {
            String logMsg = "";

            boolean isJavaCodeRelatedProblem = "JDT".equals(marker.getAttribute(IMarker.SOURCE_ID));
            IResource markerResource = marker.getResource();
            IPath projectRelativePath = markerResource.getProjectRelativePath();
            String markerType = marker.getType();

            if (sgenFileProblemMarkerType.equals(markerType)) {
                buildErrors.append("SGEN file problem: ");
            } else if (sctFileProblemMarkerType.equals(markerType)) {
                buildErrors.append("SCT file problem: ");
            }

            buildErrors.append("(file: '" + projectRelativePath.toString() + "', line nr.: "
                    + marker.getAttribute(IMarker.LINE_NUMBER) + "): " + marker.getAttribute(IMarker.MESSAGE) + " ");

            logMsg += 
                    "Problem marker message: '" + marker.getAttribute(IMarker.MESSAGE) 
                    + "', severity: '" + marker.getAttribute(IMarker.SEVERITY, Integer.MAX_VALUE)
                    + "', line number: '" + marker.getAttribute(IMarker.LINE_NUMBER) 
                    + "', source id: '" + marker.getAttribute(IMarker.SOURCE_ID) 
                    + "', resource's name: '" + markerResource.getName()
                    + "', resource's project relative path: '" + projectRelativePath.toString()
                    + "', resource's URI: '" + markerResource.getLocationURI() 
                    + "', resource's RAW URI: '" + markerResource.getRawLocationURI() + "', type: " + marker.getType();
            
            if (!isJavaCodeRelatedProblem) {// if the problem is not Java code specific, emphasize it
                logger.severe(logMsg);
            } else {
                logger.info(logMsg);
            }

        }

        //        IMarker[] javaProblemMarkers = project.findMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
        //        for (IMarker marker : javaProblemMarkers) {
        //            int severity = marker.getAttribute(IMarker.SEVERITY, Integer.MAX_VALUE);
        //            
        ////            Object[] attributes = marker.getAttributes(new String[]{IMarker.MESSAGE} );
        ////            for (Object object : attributes) {
        ////                logger.info("attr: "+object);
        ////            }
        //            
        //            String errorMessage = (String)marker.getAttribute(IMarker.MESSAGE);
        //            
        //            int lineNumber = (Integer)marker.getAttribute(IMarker.LINE_NUMBER);
        //            IResource resource = marker.getResource();
        //            String type = marker.getType();
        //            
        //            logger.info("message: "+errorMessage +"severity: "+severity+", line number: "+lineNumber+", source id: "+marker.getAttribute(IMarker.SOURCE_ID)+", resource URI: '"+resource.getRawLocationURI()+"', type: "+type);
        //            
        //            //for (Object name : marker.getAttributes().keySet()) { 
        //            //    logger.severe("   "+name+": "+marker.getAttribute((String)name, "<nothing>")+"\n"); 
        //            //}             
        //        }

        //        logger.info("generalProblemMarkers: "+generalProblemMarkers.length+", javaProblemMarkers: "+javaProblemMarkers.length);        

        return buildErrors.toString();

    }

    /**
     * Get problem markers in the project
     * 
     * @param project
     * @return
     * @throws CoreException
     */
    public IMarker[] getProblemMarkers(IProject project) throws CoreException {
        return project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
    }

    /**
     * Returns true if the project has any kind of build errors
     * 
     * @param project
     * @return
     * @throws CoreException
     */
    public boolean hasBuildErrors(IProject project) throws CoreException {
        int maxProblemSeverity = project.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);

        // logger.info("maxProblemSeverity: " + maxProblemSeverity);

        if (maxProblemSeverity == IMarker.SEVERITY_ERROR) {
            return true;
        }
        return false;
    }

    /**
     * Run all the tests on the projects passed as a parameter (the key in the TreeMap is the student's Neptun-code)
     * 
     * @param remoIProjects
     * @throws Exception
     */
    public void runTestsOnProjects(TreeMap<String, IProject> remoIProjects) throws Exception {
        runTestsOnProjects(remoIProjects, new NullProgressMonitor());
    }

    /**
     * Run all the tests on the projects passed as a parameter (the key in the TreeMap is the student's Neptun-code)
     * 
     * @param remoIProjects
     * @throws Exception
     */
    public void runTestsOnProjects(TreeMap<String, IProject> remoIProjects, IProgressMonitor monitor) throws Exception {
        logger.info("Executing statechart analyzation job (ReMo)...");

        if (remoIProjects.isEmpty()) {
            throw new Exception(
                    "No projects could be found in the workspace at '" + workspaceRoot.getLocationURI() + "'!");
        }

        final IProject firstProject = (remoIProjects.firstEntry()).getValue();
        final Path projectsRootDirectoryPath = getProjectRootDirectoryFromIProject(firstProject);

        final Set<Entry<String, IProject>> iProjectsEntrySet = remoIProjects.entrySet();

        // filename containing the actual date
        final String CSV_targetFilename = CSV_targetFilenamePrefix + "."
                + csvSimpleDateFormatForFilename.format(new Date()) + ".csv";

        final Path CSV_targetFilePath = projectsRootDirectoryPath.resolve(CSV_targetFilename);
        logger.info("CSV_targetFilePath.toUri(): " + CSV_targetFilePath.toUri());
        logger.info("Files.exists(CSV_targetFilePath): " + Files.exists(CSV_targetFilePath));
        logger.info("projectsRootDirectoryPath.toUri(): " + projectsRootDirectoryPath.toUri());

        String charsetNameForExcel = "windows-1250";// see http://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html
        // or here's another approach (writing to a file with the character encoding 'UTF-8 with BOM' (not without)): http://stackoverflow.com/questions/4192186/setting-a-utf-8-in-java-and-csv-file/4192897#4192897
        Charset charset = Charset.isSupported(charsetNameForExcel) ? Charset.forName(charsetNameForExcel)
                : StandardCharsets.UTF_8;

        try (BufferedWriter csvWriter = Files.newBufferedWriter(CSV_targetFilePath, charset)) {
            monitor.beginTask("Starting to process projects...", iProjectsEntrySet.size());

            // Dátum;Neptun-kód;Összegzés (siker/hiba);Beadott;Tiltott elemek;Teszthibák;Hibás tesztesetek száma;Összes teszteset száma;Exception dobódott
            csvWriter.append("Dátum");
            csvWriter.append(CSV_COMMA_DELIMITER);
            csvWriter.append("Neptun-kód");
            csvWriter.append(CSV_COMMA_DELIMITER);
            csvWriter.append("Összegzés (siker/hiba)");
            csvWriter.append(CSV_COMMA_DELIMITER);
            csvWriter.append("Beadott");
            csvWriter.append(CSV_COMMA_DELIMITER);
            csvWriter.append("Tiltott elemek");
            csvWriter.append(CSV_COMMA_DELIMITER);
            csvWriter.append("Teszthibák");
            csvWriter.append(CSV_COMMA_DELIMITER);
            csvWriter.append("Hibás tesztesetek száma");
            csvWriter.append(CSV_COMMA_DELIMITER);
            csvWriter.append("Összes teszteset száma");
            csvWriter.append(CSV_COMMA_DELIMITER);
            csvWriter.append("Exception dobódott");
            csvWriter.append(CSV_COMMA_DELIMITER);
            csvWriter.append(NEW_LINE);

            try {
                for (Entry<String, IProject> currentEntry : iProjectsEntrySet) {
                    String neptunCode = currentEntry.getKey();
                    IProject currentIProject = currentEntry.getValue();

                    // see if cancellation has been requested
                    if (monitor.isCanceled()) {
                        throw new OperationCanceledException(
                                "Operation has been cancelled before processing the project called '"
                                        + currentIProject.getName() + "'.");
                        // return Status.CANCEL_STATUS;
                    }
                    //                monitor.subTask("Clean+build...");               

                    boolean wasSuccessful = false;
                    // the number of tests that failed during the run
                    int failureCount = 0;
                    // the number of tests run
                    int runCount = 0;
                    HomeworkResult homeworkResult = runTestsOnProject(projectsRootDirectoryPath, neptunCode,
                            currentIProject);

                    monitor.worked(1);

                    logger.info("End of statechart analyzation.");
                    String dateFormatColumn = csvSimpleDateFormatForColumn.format(new Date());

                    String exceptionText = "";

                    Exception exceptionThrown = homeworkResult.getExceptionThrown();

                    if (exceptionThrown == null) {
                        exceptionText = "-";
                    } else {
                        if (exceptionThrown instanceof YakinduSGenFileNotFoundException) {
                            exceptionText = "No SGEN file has been found! ";
                        } else if (exceptionThrown instanceof org.xml.sax.SAXParseException) {
                            exceptionText = "Invalid SCT file! ";
                        } else {
                            exceptionThrown.printStackTrace();
                        }

                        exceptionText += exceptionThrown.getMessage().replace(CSV_COMMA_DELIMITER, " == ")
                                .replace(NEW_LINE, " == ").replace("\\r", " == ");
                    }

                    String testFailureText = "-";
                    String forbiddenElementsText = "-";

                    Result testStatechartResult = homeworkResult.getTestResult();

                    if (testStatechartResult != null) {
                        String testFailureMessages = "";
                        for (Failure failure : testStatechartResult.getFailures()) {
                            testFailureMessages += failure.getMessage();
                        }
                        testFailureText = (testFailureMessages.length() > 0
                                ? testFailureMessages.replace(NEW_LINE, " == ").replace("\\r", " == ") : "-");
                        
                        // true if all tests succeeded
                        wasSuccessful = testStatechartResult.wasSuccessful();
                        // the number of tests that failed during the run
                        failureCount = testStatechartResult.getFailureCount();
                        // // the number of tests ignored during the run
                        // ignoreCount = testStatechartResult.getIgnoreCount();
                        // the number of tests run
                        runCount = testStatechartResult.getRunCount();
                    }

                    LinkedList<ForbiddenElement> staticAnalysisResult = homeworkResult.getStaticAnalysisResult();
                    if (staticAnalysisResult != null) {
                        forbiddenElementsText = "";
                        for (ForbiddenElement forbiddenElement : staticAnalysisResult) {
                            if (!"".equals(forbiddenElementsText)) {// ha van előző üzenet is, elválasztjuk
                                forbiddenElementsText += " | ";
                            }
                            forbiddenElementsText += forbiddenElement.getMessage();
                        }
                    }

                    String uploadedSctFileMessage = (homeworkResult.isSctUploaded() ? "Igen" : "Nem");

                    // Dátum;Neptun-kód;Összegzés (siker/hiba);Beadott;Tiltott elemek;Teszthibák;Hibás tesztesetek száma;Összes teszteset száma;Exception dobódott
                    csvWriter.append(dateFormatColumn);
                    csvWriter.append(CSV_COMMA_DELIMITER);
                    csvWriter.append(neptunCode);
                    csvWriter.append(CSV_COMMA_DELIMITER);
                    csvWriter.append((wasSuccessful && exceptionThrown == null) ? "Siker" : "Hiba");
                    csvWriter.append(CSV_COMMA_DELIMITER);
                    csvWriter.append(uploadedSctFileMessage);
                    csvWriter.append(CSV_COMMA_DELIMITER);
                    csvWriter.append(forbiddenElementsText);
                    csvWriter.append(CSV_COMMA_DELIMITER);
                    csvWriter.append(testFailureText);
                    csvWriter.append(CSV_COMMA_DELIMITER);
                    csvWriter.append("" + failureCount);
                    csvWriter.append(CSV_COMMA_DELIMITER);
                    csvWriter.append("" + runCount);
                    csvWriter.append(CSV_COMMA_DELIMITER);
                    csvWriter.append(exceptionText);
                    csvWriter.append(CSV_COMMA_DELIMITER);

                    // and finally, add a new line
                    csvWriter.append(NEW_LINE);

                }

            } finally {
                // flush the stream even if OperationCanceledException gets raised (so the user cancels the process)
                csvWriter.flush();
            }

        } catch (IOException ioex) {
            System.err.println(
                    "A problem occurred while trying to write to the CSV file at '" + CSV_targetFilePath.toUri() + "'");
            ioex.printStackTrace();
        } finally {
            monitor.done();
        }

        logger.info("(System Modeling) End of processing.");

    }

    @SuppressWarnings("unused")
    private void backuporiginalstuffsfromdomethod() throws Exception {
        // http://stackoverflow.com/questions/4480334/how-to-call-a-method-stored-in-a-hashmap-java/4480360#4480360
        // Create a hash map
        LinkedHashMap<String, Callable<Boolean>> remoTasks = new LinkedHashMap<String, Callable<Boolean>>();

        IProject remoProject = getProject("hu.bme.mit.remo.scverif.test.callhandling");
        // open if necessary (no action is taken if the project is already open)
        remoProject.open(null);

        // Put elements to the map
        remoTasks.put("Copying the necessary SCT file...", () -> {
            // IProjectDescription projectDescription = remoProject
            // .getDescription();
            // ICommand[] buildSpec = projectDescription
            // .getBuildSpec();
            // for (ICommand iCommand : buildSpec) {
            // logger.info("iCommand.getBuilderName(): "
            // + iCommand.getBuilderName());
            // }

            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceDescription workspaceDescription = workspace.getDescription();
            logger.info("workspaceDescription: " + workspaceDescription.toString());
            String[] buildOrders = workspaceDescription.getBuildOrder();
            // workspaceDescription.
            if (buildOrders != null) {
                for (String buildOrder : buildOrders) {
                    logger.info("buildOrder: " + buildOrder);
                }
            }

            copySctFile(getProjectRootDirectoryFromIProject(remoProject), "ASDASD", remoProject);

            // Path binPath = Paths.get(remoProject.getFolder(
            // "bin").getRawLocationURI());

            // logger.info("CLEAN - bin content BEFORE");
            // listFilesInDirectory(binPath);

            cleanProject(remoProject, new org.eclipse.core.runtime.NullProgressMonitor());

            // logger.info("CLEAN - bin content AFTER");
            // listFilesInDirectory(binPath);

            deleteYakinduTargetFolderContents(remoProject);

            buildProject(remoProject, new org.eclipse.core.runtime.NullProgressMonitor());

            // logger.info("BUILD - bin content AFTER");
            // listFilesInDirectory(binPath);

            LinkedList<ForbiddenElement> checkForbiddenElementsInStatechart = getForbiddenElementsInStatechart(
                    remoProject, "ASDASD");

            // logger.info("TESTING - bin content BEFORE");
            // listFilesInDirectory(binPath);

            testStatechart(remoProject);

            return true;
            // return copySctFile();
        } );
        //
        // remoTasks.put("Indicating a clean request...", () -> {
        // return cleanProject(remoProject, new
        // org.eclipse.core.runtime.NullProgressMonitor());
        // });
        //
        // remoTasks.put("Deleting the content of the Yakindu target folder...",
        // () -> {
        // return deleteGeneratedDirectoryContents();
        // });
        //
        // // remoTasks.put("Generating code from the SCT file...", () -> {
        // // return generateCodeFromSgenFile(sgenFilePathInBundle);
        // // });
        //
        // remoTasks.put("Building project...", () -> {
        // return buildProject(remoProject, new
        // org.eclipse.core.runtime.NullProgressMonitor());
        // });
        //
        // remoTasks.put("Analyzing SCT file...", () -> {
        // return analyzeSctFile();
        // });
        //
        // remoTasks.put("Running JUnit test...", () -> {
        // return testStatechart();
        // });

        // https://eclipse.org/articles/Article-Concurrency/jobs-api.html
        Job remoJob = new Job("Executing ReMo tasks") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                // // Generate instances
                // SubProgressMonitor subProgressMonitor = null;

                try {
                    // subProgressMonitor = new SubProgressMonitor(monitor, 1);
                    // setProperty(key, value);

                    ExecutorService singleThreadExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

                    Set<Entry<String, Callable<Boolean>>> entrySet = remoTasks.entrySet();
                    monitor.beginTask("Starting to prepare analyzing statecharts...", entrySet.size());

                    for (Entry<String, Callable<Boolean>> remoTask : entrySet) {
                        if (monitor.isCanceled()) {
                            throw new OperationCanceledException();
                            // return Status.CANCEL_STATUS;
                        }

                        String taskDescription = remoTask.getKey();
                        Callable<Boolean> taskMethodToCall = remoTask.getValue();
                        monitor.subTask(taskDescription);
                        Boolean success = taskMethodToCall.call();

                        // //
                        // "If you would like to immediately block waiting for a task, you can use constructions of the form result = exec.submit(aCallable).get();"
                        // //
                        // http://stackoverflow.com/questions/12621041/how-to-make-callable-wait-till-execution
                        // Boolean success = singleThreadExecutor.submit(
                        // taskMethodToCall).get(); //
                        // "Waits if necessary for the computation to complete, and then retrieves its result."

                        logger.info("check if this job ran successfully ('" + taskDescription + "'): " + success);
                        monitor.worked(1);

                    }
                    logger.info("OK, everything went fine");

                    if (parentActiveShell != null) {
                        Display.getDefault().syncExec(new Runnable() {
                            public void run() {
                                MessageDialog.openInformation(parentActiveShell, "Job done",
                                        "OK, everything went fine");
                            }
                        });
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                    if (parentActiveShell != null) {
                        Display.getDefault().syncExec(new Runnable() {
                            @Override
                            public void run() {
                                MessageDialog.openError(parentActiveShell, "Job finished with errors", e.getMessage());
                            }
                        });
                    }
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
        remoJob.setPriority(Job.LONG);
        // http://www.eclipse.org/articles/Article-Concurrency/jobs-api.html
        // "reserves exclusive write access to the resources contained in the workspace by associating a scheduling rule with the job. The job will not be run while other threads hold a conflicting rule. [...] it locks the entire workspace while only touching files in a single project. This means that no other code, job or otherwise, that modifies any resource in the workspace can run concurrently with our job"
        // remoJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
        remoJob.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                if (event.getResult().isOK()) {
                    logger.info("Job completed successfully");
                } else {
                    logger.severe("Job did not complete successfully");
                    //                    logger.error("Job did not complete successfully");
                }
            }
        });
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
        Boolean isModal = (Boolean) job.getProperty(IProgressConstants.PROPERTY_IN_DIALOG);
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
                logger.info("currentProject.getName(): " + currentProject.getName());
            }
        }
    }

    /**
     * Indicates a clean request.
     * 
     * @return
     * @throws CoreException
     */
    public boolean cleanProject(IProject project, IProgressMonitor monitor) throws CoreException {
        logger.info("Indicating a clean request...");
        project.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
        return true;
    }

    /**
     * Indicates a clean request for ALL the projects in the current workspace.
     * 
     * @return
     * @throws CoreException
     */
    public boolean cleanAllProjectsInWorkspace(IProgressMonitor monitor) throws CoreException {
        logger.info("Indicating a clean request for ALL the projects in the workspace...");
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
        return true;
    }

    /**
     * @see http
     *      ://help.eclipse.org/luna/index.jsp?topic=%2Forg.eclipse.platform.
     *      doc.
     *      isv%2Freference%2Fapi%2Forg%2Feclipse%2Fcore%2Fresources%2FIProject
     *      .html&anchor=build(int,%20org.eclipse.core.runtime.IProgressMonitor)
     * @param project
     * @param monitor
     * @return
     * @throws Exception
     */
    public boolean cleanAndFullBuildProject(IProject project, IProgressMonitor monitor) throws Exception {
        cleanProject(project, monitor);
        buildProject(project, monitor);

        return true;
    }

    /**
     * Indicates a clean request and then a full build request for ALL the projects in the current workspace.
     * 
     * @return
     * @throws CoreException
     */
    public boolean cleanAndFullBuildAllProjectsInWorkspace(IProgressMonitor monitor) throws CoreException {
        logger.info(
                "Indicating a clean request for ALL the projects in the workspace AND indicating a Full build request...");
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        logger.info("Cleaning ALL projects...");
        workspace.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
        logger.info("Cleaning ALL projects DONE!");
        logger.info("FULL building all projects...");
        workspace.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        logger.info("FULL building all projects DONE!");
        logger.info("Cleaning ALL projects DONE!");
        logger.info("AUTO building all projects...");
        workspace.build(IncrementalProjectBuilder.AUTO_BUILD, monitor);
        logger.info("AUTO building all projects DONE!");
        return true;
    }

    /**
     * Get 
     * 
     * @param project
     * @return
     * @throws FileNotFoundException
     */
    public Resource getYakinduSgenFileAsLoadedResource(IProject project) throws FileNotFoundException {
        IFile sgenFile = project.getFile(sgenFilePathInBundle);
        if (!sgenFile.exists()) {
            throw new YakinduSGenFileNotFoundException(
                    // "Yakindu generator file has not been found at the following path: '" + sgenFile.getRawLocationURI()
                    "Yakindu generator file has not been found at the following path: '" + sgenFile.getLocationURI()
                            + "' (relative path: '" + sgenFilePathInBundle + "') in the following project: '"
                            + project.getName() + "'");
        }

        // org.yakindu.sct.generator.core.features.ICoreFeatureConstants.OUTLET_FEATURE_TARGET_FOLDER;
        // ezt kéne meghívni:
        // org.yakindu.sct.generator.core.util.GeneratorUtils.getTargetFolder(GeneratorEntry
        // entry)
        YakinduGeneratorExecutorModified yakinduGeneratorExecutorModified = new YakinduGeneratorExecutorModified();
        return yakinduGeneratorExecutorModified.loadResourceWithInjector(sgenFile);
    }

    public GeneratorModel getYakinduSgenFileAsGeneratorModel(IProject project) throws Exception {
        Resource resource = getYakinduSgenFileAsLoadedResource(project);

        if (resource == null || resource.getContents().size() == 0 || resource.getErrors().size() > 0) {
            throw new Exception("Yakindu sgen file could not be loaded as a Resource instance!");
        }
        return (GeneratorModel) resource.getContents().get(0);
    }

    public File getYakinduTargetFolderAsFile(IProject project) throws Exception {
        GeneratorModel model = getYakinduSgenFileAsGeneratorModel(project);
        final EList<GeneratorEntry> entries = model.getEntries();
        // for (GeneratorEntry generatorEntry : entries) {
        // File targetFolder =
        // org.yakindu.sct.generator.core.util.GeneratorUtils.getTargetFolder(generatorEntry);
        // logger.info("sgen targetFolder: "+targetFolder);
        // }
        return org.yakindu.sct.generator.core.util.GeneratorUtils.getTargetFolder(entries.get(0));
    }

    /**
     * Deleting the content of the Yakindu targetfolder (the folder to store the
     * generated artifacts) recursively.
     * TODO: lehet, hogy felesleges külön, a generáltatás (vagy a teljes
     * buildelés) megcsinálja?
     * 
     * @return
     * @throws Exception
     */
    public boolean deleteYakinduTargetFolderContents(IProject project) throws Exception {
        logger.info(
                "Deleting the content of the Yakindu target folder (the folder to store the generated artifacts)...");

        // File yakinduTargetFolder = getYakinduTargetFolderAsFile(project);
        //
        // boolean yakinduTargetFolderExists = yakinduTargetFolder.exists();
        //
        // logger.info("yakinduTargetFolder.toString(): " +
        // yakinduTargetFolder.toString());
        // logger.info("yakinduTargetFolderExists: " +
        // yakinduTargetFolderExists);
        //
        // if (!yakinduTargetFolderExists) {
        // // // if this directory doesn't exist, let's create it
        // // srcGenDirectory.toFile().mkdirs();
        // return true;
        // }

        // bedrótozott megoldás
        IFolder folder = project.getFolder("yakindu");

        // Path yakinduTargetFolderAsPath = yakinduTargetFolder.toPath();
        // Path yakinduTargetFolderAsPath = Paths.get(folder.getRawLocationURI());
        Path yakinduTargetFolderAsPath = Paths.get(folder.getLocationURI());
        // File sgenFile = new File(yakinduTargetFolder, "homework2java.sgen");
        IFile sgenFile = project.getFile(sgenFilePathInBundle);
        // Path sgenFilePath = Paths.get(sgenFile.getRawLocationURI());
        Path sgenFilePath = Paths.get(sgenFile.getLocationURI());

        Files.walkFileTree(yakinduTargetFolderAsPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs)
                    throws IOException {
                // logger.info("file visited: " + file.toString());

                // do not delete the Yakindu sgen file (it's also in the target
                // folder)
                if (!file.equals(sgenFilePath)) {
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                // logger.info("dir (postVisitDirectory): " + dir.toString());

                // we do NOT want to delete the Yakindu target folder itself
                // (because in this case, the sgen file indicates a
                // frustrating warning because of the missing directory)
                if (!dir.equals(yakinduTargetFolderAsPath)) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }

        });

        return true;
    }

    /**
     * Build Java project.
     * 
     * @param project
     * @throws Exception
     */
    public boolean buildProject(IProject project, IProgressMonitor monitor) throws Exception {
        org.yakindu.sct.model.sgraph.SGraphPackage.eINSTANCE.getClass();

        logger.info("Building project '" + project.getName() + "'...");
        // open if necessary
        if (!project.isOpen()) {
            project.open(monitor);
        }

        // IProjectDescription description = project.getDescription();
        // ICommand[] buildSpec = description.getBuildSpec();
        // for (ICommand iCommand : buildSpec) {
        // logger.info("builder: " + iCommand.getBuilderName());
        // logger.info("iCommand.isBuilding(IncrementalProjectBuilder.AUTO_BUILD): "
        // + iCommand.isBuilding(IncrementalProjectBuilder.AUTO_BUILD));
        // }

        //
        // IMarker[] problemMarkers = project.findMarkers(IMarker.PROBLEM, true,
        // IResource.DEPTH_INFINITE);
        // if (problemMarkers.length == 0) {
        // logger.info("No problem markers...");
        // } else {
        // for (IMarker problemMarker : problemMarkers) {
        // logger.info("problemMarker: " + problemMarker);
        // }
        // }
        // project.getWorkspace().

        logger.info("Building project (full build)...");
        // BOTH are needed! (IncrementalProjectBuilder.AUTO_BUILD and
        // IncrementalProjectBuilder.FULL_BUILD, with only one running, the
        // build is not complete... For some reason,
        // IncrementalProjectBuilder.AUTO_BUILD AND
        // IncrementalProjectBuilder.INCREMENTAL_BUILD was not enough...)
        project.build(IncrementalProjectBuilder.AUTO_BUILD, monitor);
        // http://www.eclipse.org/articles/Article-Builders/builders.html#2b
        IJobManager jobManager = Job.getJobManager();
        jobManager.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                Job job = event.getJob();
                String jobName = job.getName();
                String yakinduJobPrefix = "Execute SCT Genmodel ";
                boolean isExecuteSCTGenmodelJob = jobName.startsWith(yakinduJobPrefix);
                // logger.info(Boolean.toString(isExecuteSCTGenmodelJob));
                if (isExecuteSCTGenmodelJob) {
                    // logger.info("The job called '" + jobName+"' has finished! Removing JobChangeListener...");
                    // org.eclipse.core.runtime.jobs.ISchedulingRule
                    // schedulingRule = job.getRule();
                    // logger.info("schedulingRule: " + schedulingRule);
                    job.removeJobChangeListener(this);
                }
            }
        });

        InterruptedException e = null;

        do {
            try {
                jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
                e = null;
            } catch (InterruptedException ex) {
                e = ex;

                // TODO: temp...
                logger.info("building projects... InterruptedException: " + ex.toString());
            }
        } while (e != null);

        // project.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        // Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
        // project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

        // waitForAutoBuild();
        // waitForFullBuild();

        logger.info("OK, the project building process has finished!");
        return true;
    }

    /**
     * Check if all the required natures are enabled on the project
     * 
     * @param project
     * @return
     * @throws CoreException
     */
    public static boolean hasRequiredProjectNatures(IProject project) throws CoreException {
        String[] requiredNatures = { org.eclipse.jdt.core.JavaCore.NATURE_ID, // Java nature
                org.yakindu.sct.builder.nature.SCTNature.NATURE_ID// Yakindu nature
        };
        // projects with the Java nature
        //         boolean hasJavaNature = project.hasNature(org.eclipse.jdt.core.JavaCore.NATURE_ID);
        //         boolean isJavaNatureEnabled = project.isNatureEnabled(org.eclipse.jdt.core.JavaCore.NATURE_ID); // http://help.eclipse.org/juno/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fjdt%2Fcore%2FJavaCore.html&anchor=NATURE_ID

        for (String natureId : requiredNatures) {
            if (!project.isNatureEnabled(natureId)) {
                logger.warning("The project nature called '" + natureId + "' is not enabled!");
                //                logger.warn("The project nature called '" + natureId + "' is not enabled!");
                return false;
            }
        }

        return true;
    }

    /**
     * List all the files in the given directory
     * 
     * @param path
     * @throws IOException
     */
    public static void listFilesInDirectory(Path path) throws IOException {
        if(!Files.isDirectory(path)) {
            return;
        }
        
        try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                if (Files.isDirectory(entry)) {
                    listFilesInDirectory(entry);
                }
                logger.info("entry: " + entry.toString());
            }
        }
    }

    /**
     * Indicate a full build request and wait for it synchronously
     * 
     * @see https://github.com/ckulla/org.junit.contrib.eclipse/blob/master/ui/src/org/junit/contrib/eclipse/ui/WorkspaceUtil.java
     * @throws CoreException
     */
    public void doFullBuildAndWaitForIt() throws CoreException {
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
        boolean wasInterrupted = false;
        do {
            try {
                Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
                wasInterrupted = false;
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                wasInterrupted = true;
            }
        } while (wasInterrupted);
    }

    /**
     * TODO: this method is unfinished, doesn't work yet
     */
    public void waitForBuildWithJobChangeAdapter() {
        logger.info("Waiting for full build with JobChangeAdapter...");

        Job.getJobManager().addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                Job job = event.getJob();
                String jobName = job.getName();
                String yakinduJobPrefix = "Execute SCT Genmodel ";
                boolean isExecuteSCTGenmodelJob = jobName.startsWith(yakinduJobPrefix);
                // logger.info(Boolean.toString(isExecuteSCTGenmodelJob));
                if (isExecuteSCTGenmodelJob) {
                    // logger.info("The job called '" + jobName+"' has finished! Removing JobChangeListener...");
                    // org.eclipse.core.runtime.jobs.ISchedulingRule
                    // schedulingRule = job.getRule();
                    // logger.info("schedulingRule: " + schedulingRule);
                    job.removeJobChangeListener(this);
                } else {
                    logger.info("The job called '" + jobName + "' has finished!");
                }
            }
        });

        logger.info("After JobChangeAdapter...");
    }

    /**
     * Request an automatic build and wait for its completion
     * @throws CoreException
     */
    public void requestAutoBuildAndWaitForIt() throws CoreException {
        logger.info("Indicating an automatic build request");
        ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.AUTO_BUILD, new NullProgressMonitor());
        waitForAutoAndManualBuild();
    }

    /**
     * Wait synchronously for automatic and manual build requests 
     * 
     * @see https://github.com/ckulla/org.junit.contrib.eclipse/blob/master/ui/src/org/junit/contrib/eclipse/ui/WorkspaceUtil.java
     */
    public void waitForAutoAndManualBuild() {
        logger.info("Waiting for autobuild (waitForAutoBuild())...");

        boolean wasInterrupted = false;
        do {
            try {
                Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
                Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
                wasInterrupted = false;
            } catch (OperationCanceledException e) {
                logger.info("waitForAutoBuild() -- OperationCanceledException: " + e.toString());

                e.printStackTrace();
            } catch (InterruptedException e) {
                logger.info("waitForAutoBuild() -- InterruptedException: " + e.toString());

                wasInterrupted = true;
            }
        } while (wasInterrupted);

        logger.info("Waiting for autobuild (waitForAutoBuild()) DONE...");
    }

    /**
     * http://eclipse.1072660.n5.nabble.com/Wait-for-build-to-complete-and-check-if-errors-td57716.html#a57717
     * 
     * @return
     */
    public static boolean buildAndWaitForEnd() {
        boolean temp = true;

        IWorkbench workbench = PlatformUI.getWorkbench();
        IProgressService progressService = workbench.getProgressService();
        final IRunnableWithProgress runnable = new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor) throws InvocationTargetException {
                logger.info("===== Starting build process and waiting for the end =====");

                IJobManager jobManager = Job.getJobManager();
                // IWorkbench workbench = PlatformUI.getWorkbench();
                try {
                    ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
                } catch (CoreException e) {
                    throw new InvocationTargetException(e);
                }
                if (!monitor.isCanceled()) {
                    try {

                        jobManager.join(ResourcesPlugin.FAMILY_MANUAL_BUILD, monitor);

                        jobManager.join(ResourcesPlugin.FAMILY_AUTO_BUILD, monitor);
                    } catch (InterruptedException e) {
                        // continue

                        if (temp)
                            logger.info("BUILD PROCESS - InterruptedException: " + e.toString());
                    }
                }
            }
        };

        try {
            progressService.busyCursorWhile(runnable);
            return true;
        } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();

            if (temp)
                logger.info("BUILD PROCESS - InterruptedException (progressService.busyCursorWhile): " + e.toString());
        }

        return false;
    }

    /**
     * Generate statechart implementation code from an *.sgen file (code
     * generator model)
     * 
     * @return
     * @throws Exception
     */
    public boolean generateCodeFromSgenFile(IProject project, String _sgenFilePathInBundle) throws Exception {
        // getting IFile
        IFile sgenFile = project.getFile(_sgenFilePathInBundle);
        boolean sgenFileExists = sgenFile.exists();

        // logger.info("Generating code from file at '" + sgenFile.getRawLocationURI() + "' ('"
        logger.info("Generating code from file at '" + sgenFile.getLocationURI() + "' ('"
                + sgenFile.getFullPath().toString() + "')...");

        if (!sgenFileExists) {
            // throw new Exception("The '" + sgenFile.getRawLocationURI()
            throw new Exception(
                    "The '" + sgenFile.getLocationURI() + "' file does not exist, so code generation is not possible!");
        }

        logger.info("Trying to generate code...");

        // GeneratorExecutor generatorExecutor = new GeneratorExecutor();
        // generatorExecutor.executeGenerator(sgenFile);

        YakinduGeneratorExecutorModified yakinduGeneratorExecutorModified = new YakinduGeneratorExecutorModified();
        yakinduGeneratorExecutorModified.executeGeneratorWithoutIProgressMonitor(sgenFile);
        // yakinduGeneratorExecutorModified.executeGeneratorWithInjector(sgenFile);
        logger.info("Code generation was successful!");

        return true;
    }

    /**
     * Get the SCT file (the statechart model) from the project
     * 
     * @param bundleName
     * @param sctFilePathInBundle
     * @return
     * @throws Exception
     */
    public Statechart getStatechartFromBundle(String bundleName, String sctFilePathInBundle) throws Exception {
        IProject project = getProject(bundleName);
        IFile file = project.getFile(sctFilePathInBundle);
        Statechart statechartFromIFile = StatechartAnalyzer.getStatechartFromIFile(file);

        if (statechartFromIFile == null) {
            throw new Exception("Statechart could not be parsed from the file at path '"
                    // + file.getRawLocationURI().getPath() + "'!");
                    + file.getLocationURI().getPath() + "'!");
        }
        return statechartFromIFile;
    }

    /**
     * Get the SCT file (the statechart model) from the project
     * 
     * @param project
     * @param sctFilePathInIProject
     * @return
     * @throws Exception
     */
    public Statechart getStatechartFromIProject(IProject project, String sctFilePathInIProject) throws Exception {
        IFile file = project.getFile(sctFilePathInIProject);
        Statechart statechartFromIFile = StatechartAnalyzer.getStatechartFromIFile(file);

        if (statechartFromIFile == null) {
            throw new Exception("Statechart could not be parsed from the file at path '"
                    // + file.getRawLocationURI().getPath() + "'!");
                    + file.getLocationURI().getPath() + "'!");
        }
        return statechartFromIFile;
    }

    /**
     * Check all potential forbidden elements in the SCT file
     * 
     * @param currentIProject
     * @param neptunCode
     * @return
     * @throws Exception
     */
    public LinkedList<ForbiddenElement> getForbiddenElementsInStatechart(IProject currentIProject, String neptunCode)
            throws Exception {
        Statechart statechartFromIProject = getStatechartFromIProject(currentIProject,
                "./homework" + neptunCode + "." + SCT_FILE_EXTENSION);// example: homeworkABC123.sct
        StatechartAnalyzer sta = new StatechartAnalyzer();
        sta.setStatechart(statechartFromIProject);
        return sta.getForbiddenElements();
    }

    /**
     * Run tests for checking the statechart
     * 
     * @param currentIProject
     * @return
     * @throws Exception
     */
    public org.junit.runner.Result testStatechart(IProject currentIProject) throws Exception {
        logger.info("Executing tests");

        IFolder binFolder = currentIProject.getFolder("bin");
        // String binDirectory = binFolder.getRawLocationURI().getPath();
        String binDirectory = binFolder.getLocationURI().getPath();

        // bin-könyvtár elérési útja
        File binDirectoryAsFile = new File(binDirectory);
        boolean binDirectoryExists = binDirectoryAsFile.exists();
        logger.info("binDirectoryAsFile.exists(): " + binDirectoryExists);

        IFolder currentIFolder = currentIProject.getFolder(testCompiledClassFolderPathInIProject);

        // Path currentIFolderAsPath = Paths.get(currentIFolder.getRawLocationURI());
        Path currentIFolderAsPath = Paths.get(currentIFolder.getLocationURI());
        Path testCompiledClassToLoadPath = currentIFolderAsPath.resolve(testClassName + ".class");

        boolean classFileExists_1 = Files.exists(testCompiledClassToLoadPath);
        boolean classFileExists_2 = new File(binDirectoryAsFile,
                testFullClassNameWithPackage.replace(".", "/") + ".class").exists();

        logger.info("does the " + testCompiledClassFileFullPathInIProject + " exist?: " + classFileExists_2);

        if (!classFileExists_1) {
            throw new ClassNotFoundException(
                    "Class file for testing at '" + testCompiledClassToLoadPath.toUri() + "' does not exist!");
        }

        if (!classFileExists_2) {
            throw new ClassNotFoundException(
                    "Class file for testing at '" + testCompiledClassToLoadPath.toUri() + "' does not exist!");
        }

        URL url = null;

        try {
            url = binDirectoryAsFile.toURI().toURL();
        } catch (MalformedURLException e) {
            logger.severe("Error with file location (URL) (binDirectoryAsFile: " + binDirectoryAsFile.getAbsolutePath()
                    + ")");
            throw e;
        }

        URL[] urls = { url };

        org.junit.runner.Result result = null;

        // majd átnézni:
        // http://www.coderanch.com/t/633874/java/java/Unload-Reload-Dymnically-Loaded-Class
        try (URLClassLoader myURLClassLoader = new URLClassLoader(urls, JUnitCore.class.getClassLoader())) {

            logger.info("myURLClassLoader urls:");
            for (URL myURLClassLoaderUrl : myURLClassLoader.getURLs()) {
                logger.info(myURLClassLoaderUrl.getFile());
            }
            logger.info("=======");

            // logger.info("Waiting 2 secs for the classloader to work correctly...");
            // java.util.concurrent.TimeUnit.SECONDS.sleep(2);
            // logger.info("continuing...");

            Class<?> loadClassResult = myURLClassLoader.loadClass(testFullClassNameWithPackage);
            result = JUnitCore.runClasses(loadClassResult);

            // http://junit.sourceforge.net/javadoc/org/junit/runner/Result.html#getRunCount()
            int failureCount = result.getFailureCount();
            int runCount = result.getRunCount();
            int ignoreCount = result.getIgnoreCount();

            logger.info("failureCount: " + failureCount + ", runCount: " + runCount + ", ignoreCount: " + ignoreCount);

            for (Failure failure : result.getFailures()) {
                logger.warning(failure.toString());
                //                logger.warn(failure.toString());
            }

        } catch (ClassNotFoundException e) {
            logger.severe("Couldn't find test class to load");
            throw e;
        }

        return result;
    }
}
