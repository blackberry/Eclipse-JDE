/*
* Copyright (c) 2010-2012 Research In Motion Limited. All rights reserved.
*
* This program and the accompanying materials are made available
* under the terms of the Eclipse Public License, Version 1.0,
* which accompanies this distribution and is available at
*
* http://www.eclipse.org/legal/epl-v10.html
*
*/
package net.rim.ejde.internal.packaging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.rim.ejde.internal.builders.ALXBuilder;
import net.rim.ejde.internal.builders.ResourceBuilder;
import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.core.IRIMMarker;
import net.rim.ejde.internal.launching.DeploymentHelper;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.model.BlackBerryPropertiesFactory;
import net.rim.ejde.internal.model.BlackBerrySDKInstall;
import net.rim.ejde.internal.model.BlackBerryVMInstallType;
import net.rim.ejde.internal.packaging.JadFile.CodEntry;
import net.rim.ejde.internal.ui.consoles.PackagingConsole;
import net.rim.ejde.internal.util.ImportUtils;
import net.rim.ejde.internal.util.InternalPackagingUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PackageUtils;
import net.rim.ejde.internal.util.PackagingUtils;
import net.rim.ejde.internal.util.ProblemFactory;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ejde.internal.validation.DiagnosticFactory;
import net.rim.ide.OSUtils;
import net.rim.ide.Project;
import net.rim.ide.core.Util;
import net.rim.sdk.resourceutil.ResourceParseException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.core.util.IMethodInfo;
import org.eclipse.jdt.core.util.IModifierConstants;
import org.eclipse.jdt.internal.launching.JREContainer;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * This class provides methods to package a BlackBerry project.
 *
 * Usually a project has two deployment folders:
 * <p>
 * /deliverables/Standard
 * <p>
 * and
 * <p>
 * /deliverables/Web
 * <p>
 * The project is packaged into the <code>/deliverables/Standard</code> folder and all generated rapc artifacts are copied to the
 * <code>/deliverables/Web</code> folder. However, if the cod file is a zipped parent cod file, we un-zip it and copy all the
 * sibling cod files in the the <code>/deliverables/Web</code> folder.
 */
public class PackagingManager {
    static final Logger _log = Logger.getLogger( PackagingManager.class );
    static final int MAX_COMMAND_ELEMENTS = 30;
    public static final String COMPRESS_RESOURCE_OPTION = "-cr";
    public static final String CONVERT_PNG_RAPC_OPTION = "-convertpng";
    public static final String VERBOSE_RAPC_OPTION = "-verbose";
    public static final int MIDLET_JAR = 0x0001;
    public static final int EVISCERATED_JAR = 0x0002;
    private Vector< String > _compileOptions;
    private BlackBerryProject _bbProject;
    private List< String > _rapcCommandsHead;
    private List< String > _rapcCommands;
    private Vector< String > _sourceRoots;
    private Vector< String > _protectionOptions;
    private Vector< ImportedJar > _imports;
    private Vector< String > _otherFiles;
    private Vector< String > _outputFolders;
    private MessageConsoleStream _consoleOutputStream;
    private boolean writeToFile;

    private PackagingManager( BlackBerryProject bbProject ) {
        _bbProject = bbProject;
        _compileOptions = new Vector< String >();
        _sourceRoots = new Vector< String >();
        _imports = new Vector< ImportedJar >();
        _otherFiles = new Vector< String >();
        _protectionOptions = new Vector< String >();
        _outputFolders = new Vector< String >();
        _rapcCommandsHead = new ArrayList< String >();
        _rapcCommands = new ArrayList< String >();
        // Grab and activate a console to redirect build output to.
        PackagingConsole packagingConsole = PackagingConsole.getInstance();
        ConsolePlugin.getDefault().getConsoleManager().addConsoles( new IConsole[] { packagingConsole } );
        _consoleOutputStream = packagingConsole.newMessageStream();
    }

    /**
     * This is a help method to package a BlackBerry project.
     *
     * @param project
     *            A BlackBerry project
     *
     * @throws CoreException
     */
    static public void packageProject( BlackBerryProject project ) throws CoreException {
        PackagingManager packagingManage = new PackagingManager( project );
        packagingManage.internalPackageProject();
    }

    /**
     * This is a help method to package a BlackBerry project.
     *
     * @param project
     *            A BlackBerry project
     *
     * @throws CoreException
     */
    static public void generateALXForProject( BlackBerryProject project ) throws CoreException {
        PackagingManager packagingManage = new PackagingManager( project );
        // if the deliverable folder does not exist, create it
        IProject eclipseProject = project.getProject();
        IPath outputFolderPath = new Path( PackagingUtils.getRelativeStandardOutputFolder( project ) );
        try {
            ImportUtils.createFolders( eclipseProject, outputFolderPath, IResource.DERIVED );
        } catch( CoreException e ) {
            throw new ResourceException( DiagnosticFactory.CREATE_FOLDER_ERR_ID, project.getMetaFileHandler()
                    .getProjectRelativePath(), NLS.bind( Messages.PackagingManager_PACKAGING_CANNOT_CREATE_FOLDER_MSG,
                    outputFolderPath ), e );
        }
        packagingManage.internalGenerateALX();
    }

    private void internalGenerateALX() throws CoreException {
        generateALX();
        IResource outputFolder = _bbProject.getProject().findMember(
                new Path( PackagingUtils.getRelativeAlxFileOutputFolder( _bbProject ) ) );
        outputFolder.refreshLocal( IResource.DEPTH_ONE, new NullProgressMonitor() );
    }

    private void internalPackageProject() throws CoreException {
        final BlackBerrySDKInstall bbVM = PackagingUtils.getBBSDKInstall( _bbProject.getJavaProject() );
        if( bbVM == null ) {
            String msg = NLS.bind( Messages.PackagingManager_PACKAGING_NO_BB_JRE_MSG, _bbProject.getProject().getName() );
            reportProblem( _bbProject.getProject(), -1, 0, 0, msg, Problem.ERROR );
            _log.error( msg );
            return;
        }
        // clean the deployment folders
        try {
            PackagingUtils.cleanProjectOutputFolder( _bbProject );
        } catch( CoreException e ) {
            _log.error( e );
        }
        IProject eclipseProject = _bbProject.getProject();
        // if the deliverable folder does not exist, create it
        IPath outputFolderPath = new Path( PackagingUtils.getRelativeStandardOutputFolder( _bbProject ) );
        try {
            ImportUtils.createFolders( eclipseProject, outputFolderPath, IResource.DERIVED );
        } catch( CoreException e ) {
            throw new ResourceException( DiagnosticFactory.CREATE_FOLDER_ERR_ID, _bbProject.getMetaFileHandler()
                    .getProjectRelativePath(), NLS.bind( Messages.PackagingManager_PACKAGING_CANNOT_CREATE_FOLDER_MSG,
                    outputFolderPath ), e );
        }

        // calculate rapc commands
        calculateRAPCCommand();
        // check if the project should be packaged
        if( !shouldPackage() ) {
            return;
        }
        // run rapc command
        runRapcCommand();
        // post packaging steps
        postPackagingProcess( _bbProject );
    }

    private void postPackagingProcess( BlackBerryProject BBProject ) throws CoreException {
        IPath outputFolderPath = new Path( PackagingUtils.getRelativeStandardOutputFolder( _bbProject ) );
        IResource outputFolder = BBProject.getProject().findMember( outputFolderPath );
        // copy the cod files of dependency projects to the deployment folders
        copyDependencyDeploymentFiles();
        // Refresh to show new resources
        outputFolder.refreshLocal( IResource.DEPTH_INFINITE, new NullProgressMonitor() );
        // Generate ALX File
        if( BBProject.getProperties()._packaging.getGenerateALXFile().booleanValue() ) {
            generateALX();
        }
    }

    /**
     * Copies the deployment files of the dependent projects to the corresponding deployment folders.
     *
     * @throws CoreException
     */
    private void copyDependencyDeploymentFiles() throws CoreException {
        IPath standardSrcFolderPath, standardDstFolderPath;
        String outputFileName;
        IPath absoluteStandardSrcFolderPath, absoluteStandardDstFolderPath;
        List< CodEntry > codEntries = new ArrayList< CodEntry >();
        String mainVersion = PackagingUtils.getVMOutputFolderName( _bbProject );
        for( BlackBerryProject dependentProj : ProjectUtils.getAllReferencedProjects( _bbProject ) ) {
            JadFile jadFile = null;
            try {
                jadFile = new JadFile( getJadFilePath( dependentProj ).toFile() );
                jadFile.parseJadFile();
                String dependencyVersion = PackagingUtils.getVMOutputFolderName( dependentProj );
                if( !mainVersion.equals( dependencyVersion ) ) {
                    // if the BB JRE of the main project is different than the dependency projects, need to calculate the cod
                    // relative paths
                    for( CodEntry entry : jadFile.getCodeEntries() ) {
                        entry.setUrl( ".." + File.separator + dependencyVersion + File.separator + entry.getUrl() );
                    }
                }
                codEntries.addAll( jadFile.getCodeEntries() );
            } catch( IOException e ) {
                _log.error( e );
            }
            // get the standard deployment folder of the dependent project
            standardDstFolderPath = new Path( PackagingUtils.getRelativeStandardOutputFolder( dependentProj ) );
            // get the standard corresponding deployment folder of the main project
            absoluteStandardDstFolderPath = _bbProject.getProject().getLocation().append( standardDstFolderPath );
            // make sure the destination folder is created
            try {
                ImportUtils.createFolders( _bbProject.getProject(), standardDstFolderPath, IResource.DERIVED );
            } catch( CoreException e ) {
                throw new ResourceException( DiagnosticFactory.CREATE_FOLDER_ERR_ID, _bbProject.getMetaFileHandler()
                        .getProjectRelativePath(), NLS.bind( Messages.PackagingManager_PACKAGING_CANNOT_CREATE_FOLDER_MSG,
                        standardDstFolderPath ), e );
            }
            // copy deployment files in standard folder
            standardSrcFolderPath = new Path( PackagingUtils.getRelativeStandardOutputFolder( dependentProj ) );
            absoluteStandardSrcFolderPath = dependentProj.getProject().getLocation().append( standardSrcFolderPath );
            outputFileName = dependentProj.getProperties()._packaging.getOutputFileName();
            File[] outputFiles = absoluteStandardSrcFolderPath.toFile()
                    .listFiles( new DeploymentFileNameFilter( outputFileName ) );
            _log.trace( "Dependent project " + dependentProj.getElementName() + " copy folder: " + standardSrcFolderPath
                    + " --> " + _bbProject.getElementName() + " / " + standardDstFolderPath );
            for( File file : outputFiles ) {
                if( file.exists() ) {
                    DeploymentHelper.executeCopy( file, absoluteStandardDstFolderPath.append( file.getName() ).toFile() );
                }
            }
            // refresh the standard folder
            IFolder folder = _bbProject.getProject().getFolder( standardDstFolderPath );
            folder.refreshLocal( IResource.DEPTH_ONE, new NullProgressMonitor() );
        }
        if( codEntries.size() != 0 ) {
            writeCodEntry( codEntries );
        }
    }

    private void writeCodEntry( List< CodEntry > codEntries ) {
        File file = getJadFilePath( _bbProject ).toFile();
        BufferedWriter writer = null;
        List< String > list = new ArrayList< String >();
        try {
            JadFile jadFile = new JadFile( file );
            jadFile.parseJadFile();
            // add dependency cod entries
            jadFile.addCodEntries( codEntries );
            list = jadFile.getOtherProperties();
            for( CodEntry entry : jadFile.getCodeEntries() ) {
                list.add( entry.getCodURLPropertyLine() );
                list.add( entry.getCodSizePropertyLine() );
                list.add( entry.getCodCreationgTimePropertyLine() );
                list.add( entry.getCodShaPropertyLine() );
            }
            IPath destFilePath = getJadFilePath( _bbProject );
            destFilePath = destFilePath.removeLastSegments( 1 );
            destFilePath = destFilePath.append( _bbProject.getProperties().getPackaging().getOutputFileName()
                    + IConstants.FULL_JAD_FILE_SUFFIX + IConstants.JAD_FILE_EXTENSION_WITH_DOT );
            writer = new BufferedWriter( new FileWriter( destFilePath.toFile() ) );
            for( int i = 0; i < list.size(); i++ ) {
                writer.write( list.get( i ) + "\r\n" );
            }
        } catch( Exception e ) {
            _log.error( e );
        } finally {
            try {
                if( writer != null ) {
                    writer.close();
                }
            } catch( IOException e ) {
                _log.error( e );
            }
        }
    }

    public static final void copyInputStream( InputStream in, OutputStream out ) throws IOException {
        byte[] buffer = new byte[ 1024 ];
        int len;

        while( ( len = in.read( buffer ) ) >= 0 )
            out.write( buffer, 0, len );

        in.close();
        out.close();
    }

    class DeploymentFileNameFilter implements FilenameFilter {
        String _outputFileName;
        boolean _excludeCodFile;

        public DeploymentFileNameFilter( String outputFileName ) {
            _outputFileName = outputFileName;
        }

        public DeploymentFileNameFilter( String outputFileName, boolean excludeCod ) {
            _outputFileName = outputFileName;
            _excludeCodFile = excludeCod;
        }

        @Override
        public boolean accept( File dir, String name ) {
            if( name.startsWith( _outputFileName ) ) {
                if( _excludeCodFile ) {
                    if( !name.endsWith( IConstants.COD_FILE_EXTENSION_WITH_DOT ) ) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
            return false;
        }
    }

    private void generateALX() throws CoreException {
        BlackBerryProperties properties = _bbProject.getProperties();
        String targetDir = PackagingUtils.getRelativeAlxFileOutputFolder( _bbProject );
        String targetFolderLocation;
        if( targetDir == null || targetDir.equals( IConstants.EMPTY_STRING ) ) {
            targetFolderLocation = _bbProject.getProject().getLocation().toOSString();
        } else {
            targetFolderLocation = _bbProject.getProject().getFolder( new Path( targetDir ) ).getLocation().toOSString();
        }
        String filename = properties._packaging.getOutputFileName();
        String alxName = targetFolderLocation + File.separator + filename;
        try {
            ALXBuilder.Alx alx = ALXBuilder.generateAlx( null, _bbProject );
            ALXBuilder.write( alxName, alx );
        } catch( ResourceParseException rpe ) {
            throw new CoreException( new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, rpe.getMessage() ) );
        } catch( FileNotFoundException fnfe ) {
            throw new CoreException( new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, fnfe.getMessage() ) );
        }
        // refresh the alx file
        IFile alxFile = _bbProject.getProject().getFolder( new Path( targetDir ) )
                .getFile( filename + IConstants.ALX_FILE_EXTENSION_WITH_DOT );
        alxFile.refreshLocal( IResource.DEPTH_INFINITE, new NullProgressMonitor() );
    }

    private void runRapcCommand() throws CoreException {
        try {
            File workDir = _bbProject.getProject().getLocation().toFile();
            if( writeToFile ) {
                File outputFile = null;
                String outputFileName = _bbProject.getProject().getName() + ".files";
                outputFile = new File( workDir, outputFileName );
                _rapcCommandsHead.add( "@" + outputFileName );
                flushToFile( outputFile );
            } else {
                _rapcCommandsHead.addAll( _rapcCommands );
            }
            String command = getStringCommand( _rapcCommandsHead );
            _log.trace( "Execute rapc command: " + command + "; Working Directory: " + workDir.getPath() );
            ProcessBuilder rapcBuilder = new ProcessBuilder( _rapcCommandsHead );

            String javaHome = System.getenv( "JAVA_HOME" );
            if( javaHome != null ) {
                Map< String, String > env = rapcBuilder.environment();
                String pathName = "Path";
                for( String s : env.keySet() ) {
                    if( s.equalsIgnoreCase( "Path" ) )
                        pathName = s;
                }
                String path = env.get( pathName );
                path = path == null ? javaHome : ( path + File.pathSeparator + javaHome );
                path = path + File.pathSeparator + javaHome + File.separator + "bin";
                env.put( pathName, path );
                _log.trace( "PATH=" + path );
            }

            rapcBuilder.directory( workDir );
            rapcBuilder.redirectErrorStream( true );
            long startTime = System.currentTimeMillis();
            _consoleOutputStream.println( NLS.bind( Messages.PackagingManager_PACKAGING_PROJECT_MSG, _bbProject.getProject()
                    .getName() ) );
            _consoleOutputStream.println( command );
            Process process = rapcBuilder.start();
            InputStream inStream = process.getInputStream();
            InputStreamHandler inputHandler = new InputStreamHandler( _bbProject.getProject(), _consoleOutputStream, inStream );
            inputHandler.start();
            int result = process.waitFor();
            inputHandler.join();
            float spendTime = ( (float) ( System.currentTimeMillis() - startTime ) ) / 1000;
            if( result == 0 ) {

                _consoleOutputStream.println( NLS.bind( Messages.PackagingManager_PACKAGING_SUCCEED_MSG, new String[] {
                        _bbProject.getProject().getName(), String.valueOf( spendTime ) } ) );
            } else {
                _consoleOutputStream.println( NLS.bind( Messages.PackagingManager_PACKAGING_FAILED_MSG, new String[] {
                        _bbProject.getProject().getName(), String.valueOf( spendTime ) } ) );
            }
        } catch( IOException e ) {
            throw new CoreException( StatusFactory.createErrorStatus( e.getMessage() ) );
        } catch( InterruptedException e ) {
            throw new CoreException( StatusFactory.createErrorStatus( e.getMessage() ) );
        }
    }

    private void flushToFile( File file ) throws IOException {
        FileOutputStream fout = null;
        PrintStream indirect = null;
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            indirect = new PrintStream( bout, false, "UTF-8" );
            for( int i = 0; i < _rapcCommands.size(); i++ ) {
                indirect.println( _rapcCommands.get( i ) );
            }
            indirect.close();
            byte[] newBytes = bout.toByteArray();

            // either old file doesn't exist or it isn't the same
            // write out the new data
            fout = new FileOutputStream( file );
            fout.write( newBytes );
            fout.close();
        } finally {
            if( indirect != null ) {
                indirect.close();
            }
            if( fout != null ) {
                fout.close();
            }
        }
    }

    private String getStringCommand( List< String > commands ) {
        StringBuffer command = new StringBuffer();
        for( int i = 0; i < commands.size(); i++ ) {
            if( i != 0 ) {
                command.append( IConstants.ONE_BLANK_STRING );
            }
            System.out.println( commands.get( i ) );
            command.append( commands.get( i ) );
        }
        return command.toString();
    }

    private void calculateRAPCCommand() throws CoreException {
        // calculate rapc commands
        SourcerootVisitor visitor = new SourcerootVisitor();
        _bbProject.getProject().accept( visitor );
        // get customized jad and rapc files
        String custjad = PackagingUtils.getCustomJadFile( _bbProject);
        if(custjad!=null) {
        	_otherFiles.add( custjad );
        }
        String custrapc = PackagingUtils.getCustomRapcFile( _bbProject);
        if(custrapc!=null) {
        	_otherFiles.add( custrapc );
        }
        // get imported jars (project level and workspace level)
        _imports = getCompileImports( _bbProject );
        // check if there is any exported midlet jar
        // TODO: Try invoking rapc.jar in Windows instead of rapc.exe
        if( OSUtils.isWindows() ) {
            // add path of rapc.exe
            _rapcCommandsHead.add( getRAPCPath() );
        } else {
            _rapcCommandsHead.add( "java" );
            _rapcCommandsHead.add( "-jar" );
            // add path of rapc.jar
            _rapcCommandsHead.add( getRAPCPath() );
        }

        // get compile options
        _compileOptions = getCompileOptions();
        // add compile options
        if( _compileOptions.size() > 0 ) {
            _rapcCommandsHead.addAll( _compileOptions );
        }
        // get source roots
        _sourceRoots = visitor.getSourceRoots( _bbProject.getProject() );
        // add source roots
        // TODO: Added for preverifier and do more investigation and
        // see whether we can eliminate this option or not.
        if( !OSUtils.isWindows() ) {
            String binDir = getRAPCPath().replace( "rapc.jar", "" );
            String exepath = "-exepath=" + binDir;
            _rapcCommandsHead.add( exepath );
        }
        StringBuffer rapcComandBuffer = new StringBuffer();
        if( _sourceRoots.size() > 0 ) {
            rapcComandBuffer.append( "-sourceroot=" );
            rapcComandBuffer.append( composeString( _sourceRoots, File.pathSeparator ) );
            _rapcCommandsHead.add( rapcComandBuffer.toString() );
        }
        // get protection options
        _protectionOptions = getProtectionOptions( false );
        // add imports
        rapcComandBuffer = new StringBuffer();
        writeToFile = ( _imports.size() + _protectionOptions.size() ) > MAX_COMMAND_ELEMENTS;
        if( _imports.size() > 0 ) {
            if( writeToFile ) {
                for( int i = 0; i < _imports.size(); i++ ) {
                    _rapcCommands.add( "-import=" + _imports.get( i ).toString() );
                }
            } else {
                rapcComandBuffer.append( "-import=" );
                rapcComandBuffer.append( composeImportsString( _imports, File.pathSeparator, false ) );
                _rapcCommands.add( rapcComandBuffer.toString() );
            }
        }
        // add protection options
        if( _protectionOptions.size() > 0 ) {
            _rapcCommands.addAll( _protectionOptions );
        }
        // add exported jar files
        for( int i = 0; i < _imports.size(); i++ ) {
            if( _imports.get( i ).isExported() ) {
                _rapcCommands.add( _imports.get( i ).getPath() );
            }
        }

        prepareDescriptor();

        // add other files
        _rapcCommands.addAll( _otherFiles );
        // get output folders
        getOutputFolder();
        // add output folders
        if( _outputFolders.size() > 0 ) {
            _rapcCommands.addAll( _outputFolders );
        }
    }

    private void prepareDescriptor() throws CoreException {
        boolean found = false;
        IPath outputFilePath = PackagingUtils.getAbsoluteStandardOutputFilePath( _bbProject ).removeLastSegments( 1 );
        int i;
        for( i = 0; i < _otherFiles.size(); i++ ) {
            String sl = _otherFiles.get( i ).toLowerCase();
            if( sl.endsWith( IConstants.RAPC_FILE_EXTENSION_WITH_DOT ) || sl.endsWith( IConstants.JAD_FILE_EXTENSION_WITH_DOT ) ) {
                File f = new File( _otherFiles.get( i ) );
                String distFileName = _bbProject.getProperties().getPackaging().getOutputFileName();
                if( sl.endsWith( IConstants.RAPC_FILE_EXTENSION_WITH_DOT ) ) {
                    distFileName += IConstants.RAPC_FILE_EXTENSION_WITH_DOT;
                    found = true;
                } else {
                    distFileName += IConstants.JAD_FILE_EXTENSION_WITH_DOT;
                }
                File f2 = outputFilePath.append( distFileName ).toFile();
                DeploymentHelper.executeCopy( f, f2 );
                _otherFiles.set( i, f2.getAbsolutePath() );
            }
        }
        if( !found ) {
            // generate rapc file
            RAPCFile rapcFile = new RAPCFile( _bbProject );
            rapcFile.loadContent();
            rapcFile.flushToFile();
            _otherFiles.addElement( RAPCFile.getRelativeRapcFilePath( _bbProject ).toOSString() );
        }
    }

    private IPath getJadFilePath( BlackBerryProject bbProject ) {
        IPath outputFilePath = PackagingUtils.getAbsoluteStandardOutputFilePath( bbProject ).removeLastSegments( 1 );
        String jadFileName = bbProject.getProperties().getPackaging().getOutputFileName();
        jadFileName += IConstants.JAD_FILE_EXTENSION_WITH_DOT;
        return outputFilePath.append( jadFileName );
    }

    /**
     * Checks if the project should be packaged.
     *
     * @return
     */
    private boolean shouldPackage() {
        int MidletJarNumber = 0;
        for( ImportedJar jar : _imports ) {
            if( jar.isExported() ) {
                if( jar.isMidletJar() ) {
                    MidletJarNumber++;
                    if( MidletJarNumber != 1 ) {
                        reportProblem( _bbProject.getProject(), 0, 0, 0, Messages.PackagingManager_MIDLET_JAR_ERROR_MSG1,
                                Problem.ERROR );
                        return false;
                    }
                    Vector< String > jadFiles = getJadFiles( _otherFiles );
                    if( jadFiles.size() == 0 ) {
                        reportProblem( _bbProject.getProject(), 0, 0, 0, Messages.PackagingManager_MIDLET_JAR_ERROR_MSG2,
                                Problem.WARNING );
                    } else if( jadFiles.size() > 1 ) {
                        reportProblem( _bbProject.getProject(), 0, 0, 0, Messages.PackagingManager_MIDLET_JAR_ERROR_MSG3,
                                Problem.ERROR );
                        return false;
                    }
                } else if( ( jar.getType() & EVISCERATED_JAR ) > 0 ) {
                    reportProblem( _bbProject.getProject(), 0, 0, 0,
                            NLS.bind( Messages.PackagingManager_MIDLET_JAR_ERROR_MSG4, jar._path ), Problem.ERROR );
                    return false;
                }
            }
        }
        return true;
    }

    private Vector< String > getJadFiles( Vector< String > files ) {
        Vector< String > jadFiles = new Vector< String >();
        for( String file : files ) {
            if( file.endsWith( IConstants.JAD_FILE_EXTENSION_WITH_DOT ) ) {
                jadFiles.add( file );
            }
        }
        return jadFiles;
    }

    private void getOutputFolder() {
        Set< IPath > outputFolderSet = ImportUtils.getOutputPathSet( _bbProject );
        IPath javaOutRelPath;
        for( IPath path : outputFolderSet ) {
            // get rid of the first project segment
            javaOutRelPath = path.removeFirstSegments( 1 ).makeRelative();
            IFolder folder = _bbProject.getProject().getFolder( javaOutRelPath );
            if( folder == null || !folder.exists() ) {
                continue;
            }
            if( folder.isLinked() ) {
                _outputFolders.add( folder.getLocation().toOSString() );
            } else {
                _outputFolders.add( _bbProject.getProject().getLocation().append( javaOutRelPath ).toOSString() );
            }
        }
    }

    private String composeImportsString( Vector< ImportedJar > vector, String separator, boolean exported ) {
        StringBuffer buffer = new StringBuffer();
        if( vector == null ) {
            return buffer.toString();
        }
        boolean first = true;
        for( int i = 0; i < vector.size(); i++ ) {
            if( vector.get( i ).isExported() != exported ) {
                continue;
            }
            if( first ) {
                buffer.append( vector.get( i ).toString() );
                first = false;
            } else {
                buffer.append( separator );
                buffer.append( vector.get( i ).toString() );
            }
        }
        return buffer.toString();
    }

    private String composeString( Vector< String > vector, String separator ) {
        StringBuffer buffer = new StringBuffer();
        if( vector == null ) {
            return buffer.toString();
        }
        for( int i = 0; i < vector.size(); i++ ) {
            if( i == 0 ) {
                buffer.append( vector.get( i ).toString() );
            } else {
                buffer.append( separator );
                buffer.append( vector.get( i ).toString() );
            }
        }
        return buffer.toString();
    }

    static private boolean existingJar( Vector< ImportedJar > vec, ImportedJar jar ) {
        if( jar == null ) {
            return false;
        }
        for( int i = 0; i < vec.size(); i++ ) {
            if( vec.get( i ).getPath().equalsIgnoreCase( jar.getPath() ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets paths of all imported jars (project level and workspace level) and jars of dependency projects.
     *
     * @return
     * @throws CoreException
     */
    static public Vector< ImportedJar > getCompileImports( IJavaProject jProject ) throws CoreException {
        Vector< ImportedJar > vector = new Vector< ImportedJar >();
        IClasspathEntry[] entries = jProject.getRawClasspath();
        if( entries != null && entries.length > 0 ) {
            getCompileImportsRecusively( entries, jProject, vector, true );
        }
        return vector;
    }

    static private void getCompileImportsRecusively( IClasspathEntry[] entries, IJavaProject jProject,
            Vector< ImportedJar > imports, boolean isMainProject ) throws CoreException {
        if( imports == null ) {
            imports = new Vector< ImportedJar >();
        }
        // Workspace imports; if there aren't any specified, default to
        // using the runtime libraries.
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        // String jarPathString;
        try {
            BlackBerryProperties properties = null;
            boolean needAddBBJar = false;
            IPath jarPath = null;
            ImportedJar importedJar = null;
            for( IClasspathEntry entry : entries ) {
                switch( entry.getEntryKind() ) {
                    case IClasspathEntry.CPE_CONTAINER: {
                        // libraries
                        IClasspathContainer container = JavaCore.getClasspathContainer( entry.getPath(),
                                jProject.getJavaProject() );
                        if( container == null ) {
                            continue;
                        }

                        IVMInstall containerVM;
                        if( !( container instanceof JREContainer ) ) {
                            // We need to verify the type of the container because the path of Maven container only has one
                            // segment and JavaRuntime.getVMInstall(IPath) return the default VM install if the entry path has one
                            // segment.
                            containerVM = null;
                        } else {
                            containerVM = JavaRuntime.getVMInstall( entry.getPath() );
                        }

                        try {
                            if( containerVM != null ) {
                                if( containerVM.getVMInstallType().getId().equals( BlackBerryVMInstallType.VM_ID ) ) {
                                    if( isMainProject ) {
                                        // Add jars to a list
                                        IClasspathEntry[] classpathEntries = container.getClasspathEntries();
                                        if( classpathEntries != null && classpathEntries.length > 0 ) {
                                            getCompileImportsRecusively( classpathEntries, jProject, imports, false );
                                        }
                                    }
                                } else {
                                    if( !jProject.getProject().hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                                        needAddBBJar = true;
                                        continue;
                                    }
                                }
                            } else {
                                // Add jars to a list
                                IClasspathEntry[] classpathEntries = container.getClasspathEntries();
                                if( classpathEntries != null && classpathEntries.length > 0 ) {
                                    getCompileImportsRecusively( classpathEntries, jProject, imports, false );
                                }
                            }
                        } catch( CoreException e ) {
                            _log.error( e.getMessage() );
                            continue;
                        }
                        break;
                    }
                    case IClasspathEntry.CPE_LIBRARY: {
                        // imported jars
                        jarPath = PackageUtils.getAbsoluteEntryPath( entry );
                        // the jar path can be null if the jar file does not exist
                        if( jarPath == null ) {
                            throw new CoreException( StatusFactory.createErrorStatus( NLS.bind(
                                    Messages.PackagingManager_Entry_Not_Found_MSG, entry.getPath() ) ) );
                        }
                        if( jarPath.lastSegment().equals( IConstants.RIM_API_JAR ) && needAddBBJar ) {
                            needAddBBJar = false;
                        }

                        importedJar = null;
                        if( PackagingUtils.getPackagExportedJar() ) {
                            if( entry.isExported() ) {
                                if( isMainProject ) {
                                    // if the exported jar is not in the main project but a dependent project, the classes it
                                    // contains are packaged into the dependent project jar. We don't add it to classpath.
                                    importedJar = new ImportedJar( jarPath.toOSString(), true, getJarFileType( jarPath.toFile() ) );
                                }
                            } else {
                                importedJar = new ImportedJar( jarPath.toOSString(), false, getJarFileType( jarPath.toFile() ) );
                            }
                        } else {
                            importedJar = new ImportedJar( jarPath.toOSString(), false, getJarFileType( jarPath.toFile() ) );
                        }
                        if( importedJar != null && !existingJar( imports, importedJar ) ) {
                            imports.add( importedJar );
                        }
                        break;
                    }
                    case IClasspathEntry.CPE_PROJECT: {
                        // dependency projects
                        IProject project = workspaceRoot.getProject( entry.getPath().toString() );
                        IJavaProject javaProject = JavaCore.create( project );
                        try {
                            if( project.hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                                properties = ContextManager.PLUGIN.getBBProperties( javaProject.getProject().getName(), false );
                                if( properties == null ) {
                                    _log.error( "BlackBerry properties is null" );
                                    break;
                                }
                            } else {
                                properties = BlackBerryPropertiesFactory.createBlackBerryProperties( javaProject );
                            }
                        } catch( CoreException e ) {
                            _log.error( e.getMessage() );
                            continue;
                        }
                        if( PackagingManager.getProjectTypeID( properties._application.getType() ) == Project.LIBRARY ) {
                            IPath absoluteJarPath = PackagingUtils.getAbsoluteStandardOutputFilePath( new BlackBerryProject(
                                    javaProject, properties ) );
                            File jarFile = new File( absoluteJarPath.toOSString() + IConstants.DOT_MARK
                                    + IConstants.JAR_EXTENSION );
                            importedJar = new ImportedJar( jarFile.getAbsolutePath(), false, getJarFileType( jarFile ) );
                            if( !existingJar( imports, importedJar ) ) {
                                imports.add( importedJar );
                            }
                            IClasspathEntry[] subEntries = javaProject.getRawClasspath();
                            if( subEntries != null && subEntries.length > 0 ) {
                                getCompileImportsRecusively( subEntries, javaProject, imports, false );
                            }
                        }
                        break;
                    }
                    case IClasspathEntry.CPE_VARIABLE: {
                        // variables
                        String e = entry.getPath().toString();
                        int index = e.indexOf( '/' );
                        if( index == -1 ) {
                            index = e.indexOf( '\\' );
                        }
                        String variable = e;
                        IPath cpvar = JavaCore.getClasspathVariable( variable );
                        if( cpvar == null ) {
                            String msg = NLS.bind( Messages.PackagingManager_Variable_Not_Defined_MSG, variable );
                            throw new CoreException( StatusFactory.createErrorStatus( msg ) );
                        }
                        if( cpvar.lastSegment().equals( IConstants.RIM_API_JAR ) && needAddBBJar ) {
                            needAddBBJar = false;
                        }
                        // TODO RAPC does not support a class folder. We may support it later on
                        if( cpvar.lastSegment().endsWith( "." + IConstants.JAR_EXTENSION ) ) {
                            importedJar = new ImportedJar( cpvar.toOSString(), false, getJarFileType( cpvar.toFile() ) );
                            if( !existingJar( imports, importedJar ) ) {
                                imports.add( importedJar );
                            }
                        }
                        break;
                    }
                }
            }
            if( needAddBBJar && isMainProject ) {
                // insert the default BB jre lib if needed
                IVMInstall bbVM = VMUtils.getDefaultBBVM();
                if( bbVM != null ) {
                    LibraryLocation[] libLocations = bbVM.getLibraryLocations();
                    if( libLocations != null ) {
                        for( LibraryLocation location : libLocations ) {
                            importedJar = new ImportedJar( location.getSystemLibraryPath().toOSString(), false,
                                    getJarFileType( location.getSystemLibraryPath().toFile() ) );
                            if( !existingJar( imports, importedJar ) ) {
                                imports.add( importedJar );
                            }
                        }
                    }
                }
            }
        } catch( JavaModelException e ) {
            _log.error( e.getMessage() );
        }
    }

    public static String quoteFile( String fileName ) {
        return IConstants.DOUBLE_QUOTE + fileName + IConstants.DOUBLE_QUOTE;
    }

    public static int getProjectTypeID( String type ) {
        if( type.trim().equalsIgnoreCase( BlackBerryProject.CLDC_APPLICATION ) ) {
            return Project.CLDC_APPLICATION;
        }
        if( type.trim().equalsIgnoreCase( BlackBerryProject.MIDLET ) ) {
            return Project.MIDLET;
        }
        if( type.trim().equalsIgnoreCase( BlackBerryProject.LIBRARY ) ) {
            return Project.LIBRARY;
        }
        return -1;
    }

    private String getRAPCPath() {
        String rapcPath = IConstants.EMPTY_STRING;
        try {
            IVMInstall vm = null;
            if( _bbProject.getProject().hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                vm = JavaRuntime.getVMInstall( _bbProject );
            } else {
                // for java proejct, we use the default BB jre
                vm = VMUtils.getDefaultBBVM();
            }
            if( vm != null ) {
                File vmLocation = vm.getInstallLocation();
                IPath vmPath = new Path( vmLocation.getPath() );
                vmPath = vmPath.append( "bin" );
                if( OSUtils.isWindows() ) {
                    vmPath = vmPath.append( "rapc.exe" );
                } else {
                    // Make sure preverify is in executable state
                    File f = null;
                    if( ( f = new File( vmPath + File.separator + IConstants.PREVERIFY_FILE_NAME ) ).exists() ) {
                        if( !f.canExecute() ) {
                            f.setExecutable( true );
                        }
                    }
                    // invoke rapc.jar instead of rapc.exe
                    vmPath = vmPath.append( "rapc.jar" );
                }
                rapcPath = vmPath.toOSString();
            } else {
                throw ProblemFactory.create_VM_MISSING_exception( _bbProject.getElementName() );
            }
        } catch( CoreException e ) {
            _log.error( "getRapcPath: " + e.getMessage() );
        }
        return rapcPath;
    }

    private Vector< String > getCompileOptions() {
        Vector< String > options = new Vector< String >();
        BlackBerryProperties properties = _bbProject.getProperties();
        // TODO java home
        // get options from the compile section
        if( properties._compile.getCompressResources().booleanValue() ) {
            options.add( COMPRESS_RESOURCE_OPTION );
        }
        if( properties._compile.getConvertImages().booleanValue() ) {
            options.add( CONVERT_PNG_RAPC_OPTION );
        }
        if( !properties._compile.getCreateWarningForNoExportedRoutine().booleanValue()
                || !properties._application.getType().equals( BlackBerryProject.CLDC_APPLICATION ) ) {
            options.add( Project.NO_MAIN_RAPC_OPTION );
        }
        if( !properties._compile.getOutputCompilerMessages().booleanValue() ) {
            options.add( Project.QUIET_RAPC_OPTION );
        }
        if( !StringUtils.isBlank( properties._compile.getAliasList() )
                && properties._application.getType().equals( BlackBerryProject.LIBRARY ) ) {
            options.add( Project.ALIAS_RAPC_OPTION + properties._compile.getAliasList() );
        }
        // add other options
        InternalPackagingUtils.addOtherOptions( options, properties );
        // add codename
        int projectType = getProjectTypeID( properties._application.getType() );
        String outputFileName = PackagingUtils.getRelativeStandardOutputFilePath( _bbProject ).toOSString();
        if( projectType == Project.LIBRARY ) {
            options.addElement( "library=" + outputFileName );
        } else {
            options.addElement( "codename=" + outputFileName );
        }

        if( projectType == Project.MIDLET ) {
            options.addElement( "-midlet" );
        }
        return options;
    }

    /**
     * Checks if a jar file is a MidletJar created by rapc.
     *
     * @param f
     * @return
     */
    static public int getJarFileType( File f ) {
        int type = 0x0;
        if( !f.exists() ) {
            return type;
        }
        java.util.jar.JarFile jar = null;
        try {
            jar = new java.util.jar.JarFile( f, false );
            java.util.jar.Manifest manifest = jar.getManifest();
            if( manifest != null ) {
                java.util.jar.Attributes attributes = manifest.getMainAttributes();
                String profile = attributes.getValue( "MicroEdition-Profile" );
                if( profile != null ) {
                    if( "MIDP-1.0".equals( profile ) || "MIDP-2.0".equals( profile ) ) {
                        type = type | MIDLET_JAR;
                    }
                }
            }
            Enumeration< JarEntry > entries = jar.entries();
            JarEntry entry;
            String entryName;
            InputStream is = null;
            IClassFileReader classFileReader = null;
            // check the attribute of the class files in the jar file
            for( ; entries.hasMoreElements(); ) {
                entry = entries.nextElement();
                entryName = entry.getName();
                if( entryName.endsWith( IConstants.CLASS_FILE_EXTENSION_WITH_DOT ) ) {
                    is = jar.getInputStream( entry );
                    classFileReader = ToolFactory.createDefaultClassFileReader( is, IClassFileReader.ALL );
                    if( isEvisceratedClass( classFileReader ) ) {
                        type = type | EVISCERATED_JAR;
                        break;
                    }
                }
            }
        } catch( IOException e ) {
            _log.error( e.getMessage() );
        } finally {
            try {
                if( jar != null ) {
                    jar.close();
                }
            } catch( IOException e ) {
                _log.error( e.getMessage() );
            }
        }
        return type;
    }

    /**
     * Verify if the given <code>classFileReader</code> has code attributes.
     *
     * @param classFileReader
     * @return
     */
    static private boolean isEvisceratedClass( IClassFileReader classFileReader ) {
        // ignore interface classes
        if( !classFileReader.isClass() ) {
            return false;
        }
        IMethodInfo[] methodInfos = classFileReader.getMethodInfos();
        if( methodInfos == null ) {
            return false;
        }
        for( int i = 0; i < methodInfos.length; i++ ) {
            // Ignore <init>, <clinit> and abstract methods
            if( "<init>".equalsIgnoreCase( String.valueOf( methodInfos[ i ].getName() ) ) || methodInfos[ i ].isClinit()
                    || ( methodInfos[ i ].getAccessFlags() & IModifierConstants.ACC_ABSTRACT ) != 0 ) {
                continue;
            }
            if( methodInfos[ i ].getCodeAttribute() == null ) {
                return true;
            }
        }
        return false;
    }

    public static void reportProblem( IResource resource, int line, int start, int end, String msg, int level ) {
        reportProblem( resource, IRIMMarker.PACKAGING_PROBLEM, line, start, end, msg, level );
    }

    public static void reportProblem( IResource resource, String type, int line, int start, int end, String msg, int level ) {

        try {
            IMarker m = resource.createMarker( type );
            m.setAttribute( IMarker.LINE_NUMBER, line );
            m.setAttribute( IMarker.MESSAGE, msg );
            m.setAttribute( IMarker.CHAR_START, start );
            m.setAttribute( IMarker.CHAR_END, end );
            switch( level ) {
                case Problem.ERROR:
                    m.setAttribute( IMarker.PRIORITY, IMarker.PRIORITY_HIGH );
                    m.setAttribute( IMarker.SEVERITY, IMarker.SEVERITY_ERROR );
                    break;
                case Problem.WARNING:
                    m.setAttribute( IMarker.PRIORITY, IMarker.PRIORITY_NORMAL );
                    m.setAttribute( IMarker.SEVERITY, IMarker.SEVERITY_WARNING );
                    break;
                case Problem.INFO:
                default:
                    m.setAttribute( IMarker.PRIORITY, IMarker.PRIORITY_LOW );
                    m.setAttribute( IMarker.SEVERITY, IMarker.SEVERITY_INFO );
                    break;
            }
        } catch( CoreException e ) {
            _log.error( e.getMessage(), e );
        }
    }

    private Vector< String > getProtectionOptions( boolean forMakefile ) {
        Vector< String > v = new Vector< String >();
        Hashtable< String, String > classProtection, packageProtection;
        classProtection = _bbProject.getProperties()._hiddenProperties.getClassProtection();
        packageProtection = _bbProject.getProperties()._hiddenProperties.getPackageProtection();
        Object keys[] = packageProtection.keySet().toArray();
        for( int i = 0; i < keys.length; ++i ) {
            v.addElement( "package:" + Util.doubleDollar( keys[ i ].toString() ) + "="
                    + stripPath( packageProtection.get( keys[ i ] ) ) );
        }
        keys = classProtection.keySet().toArray();
        for( int i = 0; i < keys.length; ++i ) {
            if( forMakefile ) {
                v.addElement( "class:" + Util.doubleDollar( keys[ i ].toString() ) + "="
                        + stripPath( classProtection.get( keys[ i ] ) ) );
            } else {
                // When we are not creating a makefile don't add double $$ to rapc cmd line
                v.addElement( "class:" + keys[ i ].toString() + "=" + stripPath( classProtection.get( keys[ i ] ) ) );
            }
        }
        return v;
    }

    private String stripPath( Object f ) {
        return new File( f.toString() ).getName();
    }

    private class SourcerootVisitor implements IResourceVisitor {
        Set< String > _resourceRootSet;

        public SourcerootVisitor() {
            _resourceRootSet = new HashSet< String >();
        }

        public boolean visit( IResource resource ) throws CoreException {
            if( !( resource instanceof IFile ) ) {
                return shouldBeSourceRoot( resource );
            }
            String extension = resource.getFileExtension();
            if( extension != null ) {
                if( extension.equalsIgnoreCase( IConstants.JAVA_EXTENSION ) ) {
                    String sourceRoot = calculateSourceRoot( resource.getLocation().toFile() );
                    if( sourceRoot != null ) {
                        _resourceRootSet.add( sourceRoot );
                    }
                }
            }
            return false;
        }

        private String calculateSourceRoot( File javaFile ) {
            String packagename = PackageUtils.getJavaFilePackageID( javaFile );
            packagename = packagename.replace( IConstants.DOT_CHAR, IConstants.BACK_SLASH_CHAR );
            IPath absolutePath = new Path( javaFile.getPath() );
            IPath packageAndFileNamPath = null;
            if( packagename.trim().equals( IConstants.EMPTY_STRING ) ) {
                packageAndFileNamPath = new Path( javaFile.getName() );
            } else {
                packageAndFileNamPath = new Path( packagename );
                packageAndFileNamPath = packageAndFileNamPath.append( new Path( javaFile.getName() ) );
            }
            int differentSegNumber = absolutePath.segmentCount() - packageAndFileNamPath.segmentCount();
            if( differentSegNumber >= 0 ) {
                IPath actualPackageAndFileNamPath = absolutePath.removeFirstSegments( differentSegNumber );
                if( actualPackageAndFileNamPath.matchingFirstSegments( packageAndFileNamPath ) == packageAndFileNamPath
                        .segmentCount() ) {
                    return absolutePath.removeLastSegments( packageAndFileNamPath.segmentCount() ).toOSString();
                }
            }
            _log.debug( "Java file [" + javaFile.getPath() + "] is not in the right package." ); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }

        public Vector< String > getSourceRoots( IProject proj ) {
            Vector< String > roots = new Vector< String >();
            roots.addAll( _resourceRootSet );
            try {
                if( proj.hasNature( JavaCore.NATURE_ID ) ) {
                    IJavaProject jproj = JavaCore.create( proj );
                    IClasspathEntry[] clpentrs = jproj.getResolvedClasspath( true );
                    IWorkspaceRoot workspaceRoot = proj.getWorkspace().getRoot();
                    for( IClasspathEntry clpentry : clpentrs ) {
                        if( ( clpentry.getEntryKind() == IClasspathEntry.CPE_SOURCE )
                                && !ImportUtils.getImportPref( ResourceBuilder.LOCALE_INTERFACES_FOLDER_NAME ).equals(
                                        clpentry.getPath().lastSegment() ) ) {
                            // Try to resolve the source container
                            IResource resource = workspaceRoot.findMember( clpentry.getPath() );
                            boolean found = false;
                            if( resource instanceof IContainer ) {
                                for( String resRoot : _resourceRootSet ) {
                                    if( resource.getLocation().equals( new Path( resRoot ) ) ) {
                                        found = true;
                                        break;
                                    }
                                }
                                if( !found ) {
                                    roots.add( resource.getLocation().toOSString() );
                                }
                            }
                        }
                    }
                }
            } catch( CoreException e ) {
                _log.error( e );
            }
            return roots;
        }

        private boolean shouldBeSourceRoot( IResource resource ) {
            if( resource instanceof IProject ) {
                return true;
            }
            if( PackageUtils.isUnderSrcFolder( resource ) ) {
                return true;
            }
            return false;
        }
    }

    /**
     * This class is a handler for executing reading an InputStream of a process in a StringBuffer
     *
     *
     */
    static public class InputStreamHandler extends Thread {
        private InputStream _stream;
        private MessageConsoleStream _consoleStream;
        private IProject _project;

        /**
         * Regular expression to match an error line pattern. These lines are usually in the form file:line:msg
         */
        private static final Pattern javaErrorStartPattern = Pattern.compile( ".*\\.java:\\d+:\\s\\S.*" );
        private static final Pattern rapcErrorStartPattern = Pattern.compile( "[Ee]rror.*" );
        private static final Pattern rapcErrorStartPattern2 = Pattern.compile( ".*[Ee]rror!.*" );
        /**
         * Regular expression to match a symbol error. This typically looks like symbol : <missing symbol>
         */
        private static final Pattern errorSymbolPattern = Pattern.compile( "^symbol\\s*:\\s\\S.*" );

        private static final Pattern errorLocationPattern = Pattern.compile( "^location\\s*:\\s\\S.*$" );

        private static final Pattern errorSourcePattern = Pattern.compile( "^\\s+\\S.*$" );

        private static final Pattern errorSourceLocationPattern = Pattern.compile( "^\\s*\\^\\s*$" );

        /**
         * Regular expression to match a warning line pattern. These lines are usually in the form file:line: Warning!: msg
         */
        private static final Pattern javaWarningStartPattern = Pattern.compile( ".*\\.java:\\d+:\\s[Ww]arning.*" );

        private static final Pattern errorLineNumberPattern = Pattern.compile( ":\\d+:" );

        private static final Pattern rapcWarningStartPattern = Pattern.compile( "[Ww]arning.*" );
        private static final Pattern rapcWarningStartPattern2 = Pattern.compile( ".*[Ww]arning!.*" );

        /**
         * @param captureBuffer
         * @param stream
         */
        public InputStreamHandler( IProject project, MessageConsoleStream consoleOutputStream, InputStream inputStream ) {
            _stream = inputStream;
            _consoleStream = consoleOutputStream;
            _project = project;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Thread#run()
         */
        @Override
        public void run() {
            try {
                /*
                 * List of queued problems to submit. This is a stack, because it is useful to get the problem just added to add
                 * additional info to it.
                 */
                Stack< Problem > problems = new Stack< Problem >();
                InputStreamReader isr = new InputStreamReader( _stream );
                BufferedReader br = new BufferedReader( isr );
                while( true ) {
                    String line = br.readLine();
                    if( line == null ) {
                        break;
                    }
                    try {
                        parse( line, problems );
                    } catch( RuntimeException e ) {
                        _log.error( e.getMessage(), e );
                    }
                    _consoleStream.println( line );
                }
                _stream.close();
                reportProblems( problems );
            } catch( IOException ioe ) {
                _log.error( ioe );
            }
        }

        private void reportProblems( Stack< Problem > problems ) {
            for( Problem problem : problems ) {
                reportProblem( problem );
            }
            problems.clear();
        }

        private void reportProblem( Problem problem ) {
            _log.trace( "reporting problem " + problem.file + " at " + problem.line + " about " + problem.msg );
            IResource resource = null;
            if( problem.file != null ) {
                IPath problemFileLocation = new Path( problem.file );
                resource = ProjectUtils.getResource( _project, problemFileLocation.toFile() );
            } else {
                resource = _project;
            }

            if( resource != null ) {
                PackagingManager.reportProblem( resource, problem.line, problem.start, problem.end, problem.msg, problem.level );
            }
        }

        /**
         * Takes a string and delegates it based on the pattern of it.
         *
         * @param s
         */
        protected void parse( String s, Stack< Problem > problems ) {
            if( javaWarningStartPattern.matcher( s ).matches() ) {
                parseJavaWarningStart( s, problems );
            } else if( rapcWarningStartPattern.matcher( s ).matches() || rapcWarningStartPattern2.matcher( s ).matches() ) {
                parseRAPCWarningStart( s, problems );
            } else if( javaErrorStartPattern.matcher( s ).matches() ) {
                parseJavaErrorString( s, problems );
            } else if( rapcErrorStartPattern.matcher( s ).matches() || rapcErrorStartPattern2.matcher( s ).matches() ) {
                parseRAPCErrorStart( s, problems );
            }
        }

        /**
         * Parses an error generated by RAPC.
         */
        private void parseRAPCErrorStart( String s, Stack< Problem > problems ) {
            // For example:
            // Error!: Error: java compiler failed:
            // C:\Java\jdk1.6.0_01\bin\javac.exe -source 1.3
            // -target 1.1 -g -O -d C:\DOCUME~1\zqiu\LOCALS~1\Temp\r ...

            Matcher m = rapcErrorStartPattern.matcher( s );
            if( !m.find() ) {
                m = rapcErrorStartPattern2.matcher( s );
                if( !m.find() ) {
                    _log.error( "Failed to parse RAPC error message: " + s );
                    return;
                }
            }

            Problem problem = new Problem();
            problem.msg = s;
            problem.level = Problem.ERROR;

            problems.add( problem );
        }

        /**
         * Parses an error start. An error start has the file name, line number, and message. Subsequent lines will hold more
         * information about the specific error.
         */
        private void parseRAPCWarningStart( String s, Stack< Problem > problems ) {
            // For example:
            // Warning!: Reference to class: net.rim.device.api.io.File requires
            // signing
            // with key: RIM Runtime API

            Matcher m = rapcWarningStartPattern.matcher( s );
            if( !m.find() ) {
                m = rapcWarningStartPattern2.matcher( s );
                if( !m.find() ) {
                    _log.error( "Failed to parse RAPC warning message: " + s );
                    return;
                }
            }

            Problem problem = new Problem();
            problem.msg = s;
            problem.level = Problem.WARNING;

            problems.add( problem );
        }

        /**
         * Parses an error start. An error start has the file name, line number, and message. Subsequent lines will hold more
         * information about the specific error.
         */
        private void parseJavaWarningStart( String s, Stack< Problem > problems ) {
            // For example:
            // C:\samples\com\rim\samples\device\tictactoe\TTTService.java:10:
            // package net.rim.blackberry.api.blackberrymessenger does not exist

            int firstColon, secondColon;

            Matcher m = errorLineNumberPattern.matcher( s );
            if( m.find() ) {
                firstColon = m.start();
                secondColon = m.end() - 1;
            } else {
                _log.warn( "Faling back to old way of doing things..." );
                secondColon = s.lastIndexOf( ':' );
                firstColon = s.lastIndexOf( ':', secondColon - 1 );
            }

            Problem problem = new Problem();
            problem.file = s.substring( 0, firstColon ).trim();
            problem.line = Integer.parseInt( s.substring( firstColon + 1, secondColon ) );
            problem.msg = s.substring( secondColon + 1 ).trim();
            problem.level = Problem.WARNING;

            problems.add( problem );
        }

        /* Keep track of state for parse() automaton */
        private int parseState;

        /**
         * Takes a string and delegates it based on the pattern of it.
         *
         * @param s
         */
        private void parseJavaErrorString( String s, Stack< Problem > problems ) {
            _log.trace( "Parsing " + s );
            if( javaErrorStartPattern.matcher( s ).matches() ) {
                // Some warnings appear on stderr for some reason...
                // For example:
                // C:\samples\com\rim\samples\device\syncdemo\SyncDemo.java:288:
                // warning: [deprecation] getScreenWidth() in
                // net.rim.device.api.ui.Graphics has been deprecated
                if( javaWarningStartPattern.matcher( s ).matches() ) {
                    parseJavaWarningStart( s, problems );
                } else {
                    parseErrorStart( s, problems );
                }
                parseState = 0;
            } else if( ( parseState == 0 ) && errorSymbolPattern.matcher( s ).matches() ) {
                parseErrorSymbol( s, problems );
                parseState = 1;
            } else if( ( parseState == 1 ) && errorLocationPattern.matcher( s ).matches() ) {
                // Optional "location:*" line. Only occurs for stuff in inner
                // classes or methods.
            } else if( ( parseState == 1 ) && errorSourcePattern.matcher( s ).matches() ) {
                parseErrorSource( s, problems );
                parseState = 2;
            } else if( ( parseState == 2 ) && errorSourceLocationPattern.matcher( s ).matches() ) {
                parseErrorLocation( s, problems );
                parseState = 3;
            }
        }

        /**
         * Parses an error start. An error start has the file name, line number, and message. Subsequent lines will hold more
         * information about the specific error.
         */
        private void parseErrorStart( String s, Stack< Problem > problems ) {
            // For example:
            // C:\samples\com\rim\samples\device\tictactoe\TTTService.java:10:
            // package net.rim.blackberry.api.blackberrymessenger does not exist

            int firstColon, secondColon;

            Matcher m = errorLineNumberPattern.matcher( s );
            if( m.find() ) {
                firstColon = m.start();
                secondColon = m.end() - 1;
            } else {
                _log.warn( "Faling back to old way of doing things..." );
                secondColon = s.lastIndexOf( ':' );
                firstColon = s.lastIndexOf( ':', secondColon - 1 );
            }

            Problem problem = new Problem();
            problem.file = s.substring( 0, firstColon ).trim();
            problem.line = Integer.parseInt( s.substring( firstColon + 1, secondColon ) );
            problem.msg = s.substring( secondColon + 1 ).trim();
            problem.level = Problem.ERROR;

            problems.add( problem );
        }

        /**
         * This parses the line associated with a "cannot find symbol" error. Following such an error, rapc will output the undef
         * symbol.
         *
         * @param s
         */
        private void parseErrorSymbol( String s, Stack< Problem > problems ) {
            // For example:
            // C:\samples\com\rim\samples\device\tictactoe\TTTRequestListener.java
            // :28: cannot find symbol
            // symbol : class Session

            try {
                int colon = s.indexOf( ":" );
                Problem problem = problems.peek();
                if( ( problem.msg != null ) && problem.msg.equals( "cannot find symbol" ) ) {
                    problem.msg += " " + s.substring( colon ).trim();
                }
            } catch( EmptyStackException e ) {
                _log.error( e.getMessage(), e );
            }
        }

        private void parseErrorLocation( String s, Stack< Problem > problems ) {
            try {
                Problem problem = problems.peek();
                if( problem != null ) {
                    problem.start = s.indexOf( '^' );

                    String source = problem.source;
                    int sourceLength = source.length();
                    for( int i = problem.start; i < sourceLength; i++ ) {
                        char c = source.charAt( i );

                        // All valid characters that can go into java
                        // identifiers.
                        if( ( ( c > 96 ) && ( c < 123 ) ) || ( ( c > 64 ) && ( c < 91 ) ) || ( ( c > 47 ) && ( c < 58 ) )
                                || ( c == 36 ) || ( c == 95 ) ) {
                            // Do Nothing
                        } else {
                            problem.end = i + 1;
                            break;
                        }
                    }
                }
            } catch( EmptyStackException e ) {
                _log.error( e.getMessage(), e );
            }
        }

        private void parseErrorSource( String s, Stack< Problem > problems ) {
            try {
                Problem problem = problems.peek();
                if( problem != null ) {
                    problem.source = s;
                }
            } catch( EmptyStackException e ) {
                _log.error( e.getMessage(), e );
            }
        }
    }

    /**
     * Problem declaration
     */
    static public class Problem {
        public static final int ERROR = 1;
        public static final int WARNING = 2;
        public static final int INFO = 4;

        String file;
        int line;
        String msg;
        int level;

        String source;
        int start = -1;
        int end = -1;

        @Override
        public String toString() {
            return "Problem[file=" + file + ";line=" + line + ";msg=" + msg + ";level=" + level + ";]";
        }

    }

    /**
     * ImportedJar class is used to differentiate if a jar is exported or not.
     *
     *
     */
    public static class ImportedJar {
        String _path;
        boolean _exported;
        int _type;

        public ImportedJar( String path, boolean exported, int jarType ) {
            _path = path;
            _exported = exported;
            _type = jarType;
        }

        public boolean isExported() {
            return _exported;
        }

        public void setExported( boolean exported ) {
            _exported = exported;
        }

        public String getPath() {
            return _path;
        }

        public void setPath( String path ) {
            _path = path;
        }

        public int getType() {
            return _type;
        }

        public void setType( int type ) {
            _type = type;
        }

        public String toString() {
            return _path;
        }

        public boolean isMidletJar() {
            return ( _type & MIDLET_JAR ) > 0;
        }
    }
}
