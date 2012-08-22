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
package net.rim.ejde.internal.builders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.core.IRIMMarker;
import net.rim.ejde.internal.util.ImportUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PackageUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.ResourceBuilderUtils;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.sdk.CompilerException;
import net.rim.sdk.rc.ConvertUtil;
import net.rim.sdk.rc.ResourceCompiler;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

/**
 * This class is used to create interface java files for resource files.
 */
public class ResourceBuilder extends IncrementalProjectBuilder {
    static private Logger log = Logger.getLogger( ResourceBuilder.class );
    public static final String RESOURCE_BUILD_TMP_FOLDER_HEAD = "resourceBuilder_rc_";
    public static final String BUILDER_ID = "net.rim.ejde.internal.builder.BlackBerryResourcesBuilder"; //$NON-NLS-1$
    public static final String LOCALE_INTERFACES_FOLDER_NAME = "project_locale_interfaces_folder"; //$NON-NLS-1$
    static private String _tmpOutputFolder;
    private Hashtable< String, String > _resourceBuilderOptions = null;
    private Vector< IFile > _filesNeedToReBuild = new Vector< IFile >();

    /**
     * (non-javadoc)
     *
     * @see IncrementalProjectBuilder#build(int, Map, IProgressMonitor)
     */
    protected IProject[] build( int kind, Map args, IProgressMonitor monitor ) throws CoreException {
        log.trace( "Entering ResourcesBuilder build();kind=" + kind ); //$NON-NLS-1$
        IProject project = getProject();
        IResourceDelta delta = getDelta( getProject() );
        IResourceDelta classpathFile = null;
        if( delta != null ) {
            classpathFile = delta.findMember( new Path( IConstants.CLASSPATH_FILE_NAME ) );
        }
        if( kind == IncrementalProjectBuilder.FULL_BUILD || classpathFile != null ) {
            ResourceDeltaVisitor resourceVisitor = new ResourceDeltaVisitor( monitor );
            project.accept( resourceVisitor );
        } else {
            _filesNeedToReBuild = getFilesNeedRebuild();
            if( delta != null ) {
                ResourceDeltaVisitor deltaVisitor = new ResourceDeltaVisitor( monitor );
                delta.accept( deltaVisitor );
            }
            if( _filesNeedToReBuild.size() > 0 ) {
                for( IFile file : _filesNeedToReBuild ) {
                    compile( file, monitor );
                }
            }
        }
        log.trace( "Leaving ResourcesBuilder build()" ); //$NON-NLS-1$
        return null;
    }

    static public void cleanTmpDir() {
        // synchronizlly delete the tmp folder and files
        Display.getDefault().syncExec( new Runnable() {

            @Override
            public void run() {
                cleanupTempSubdir( _tmpOutputFolder );
            }

        } );
    }

    /**
     * Get rrh/rrc files which need to be rebuilt.
     *
     * @return
     */
    private Vector< IFile > getFilesNeedRebuild() {
        Vector< IFile > vector = new Vector< IFile >();
        IMarker[] markers;
        try {
            markers = getProject().findMarkers( IRIMMarker.RESOURCE_BUILD_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE );
            for( int i = 0; i < markers.length; i++ ) {
                IResource resource = markers[ i ].getResource();
                if( resource instanceof IFile
                        && ( PackageUtils.hasRRCExtension( resource.getName() ) || PackageUtils.hasRRHExtension( resource
                                .getName() ) ) ) {
                    vector.add( (IFile) resource );
                }
            }
        } catch( CoreException e ) {
            log.error( e.getMessage() );
        }
        return vector;
    }

    protected void clean( IProgressMonitor monitor ) throws CoreException {
        removeResourceBuildMarkers( getProject(), IResource.DEPTH_INFINITE );
        IFolder tmpFolder = getProject().getFolder( ImportUtils.getImportPref( ResourceBuilder.LOCALE_INTERFACES_FOLDER_NAME ) );
        if( !tmpFolder.exists() )
            return;
        IResource[] members = tmpFolder.members();
        for( int i = 0; i < members.length; i++ ) {
            if( members[ i ] instanceof IFile ) {
                members[ i ].delete( true, monitor );
            } else if( members[ i ] instanceof IFolder )
                ( (IFolder) members[ i ] ).delete( true, false, monitor );
            else
                throw new CoreException( StatusFactory.createErrorStatus( NLS.bind( "", members[ i ].getName() ) ) ); //$NON-NLS-1$
        }
    }

    /**
     * Compiles given <code>iFile</code>. <code>iFIle</code> should be either of rrc or rrh file.
     *
     * @param resource
     * @param monitor
     * @throws CoreException
     */
    void compile( IResource iFile, IProgressMonitor monitor ) throws CoreException {
        log.trace( "ResourcesBuilder compile(IResource); Resource: " //$NON-NLS-1$
                + iFile.getLocation().toOSString() );
        // remove the old markers
        removeResourceBuildMarkers( iFile, IResource.DEPTH_ONE );
        File resourceFile = null;
        // get the absolute path of the file
        resourceFile = ResourceBuilderUtils.getFile( iFile );
        if( resourceFile == null )
            log.error( NLS.bind( "", //$NON-NLS-1$
                    iFile.getLocationURI() ) );

        // get the parent directory of the resource file
        Vector< String > fileList = new Vector< String >();
        try {
            ResourceCompiler.compile( resourceFile.getPath(), null, getResourceBuilderOptions(), fileList );
        } catch( CompilerException e ) {
            log.error( NLS.bind( Messages.RIMResourcesBuilder_COMPILE_FILE_ERROR_MSG, new String[] { e.getMessage() } ) );
            createResourceMarker( iFile,
                    NLS.bind( Messages.RIMResourcesBuilder_COMPILE_FILE_ERROR_MSG, new String[] { e.getMessage() } ), 0,
                    IMarker.SEVERITY_ERROR );
            return;
        } catch( IOException e ) {
            log.error( NLS.bind( Messages.RIMResourcesBuilder_COMPILE_FILE_ERROR_MSG, new String[] { e.getMessage() } ) );
            createResourceMarker( iFile,
                    NLS.bind( Messages.RIMResourcesBuilder_COMPILE_FILE_ERROR_MSG, new String[] { e.getMessage() } ), 0,
                    IMarker.SEVERITY_ERROR );
            return;
        }
        // parse the result file list
        for( String string : fileList ) {
            File processedFile = new File( string );
            IFolder tmpParentFolder = getParentFolder( iFile.getRawLocation().toFile(), processedFile, iFile.getProject(),
                    monitor );
            // get the name of the compiled resource file
            String fileName = processedFile.getName();
            // get the IFile handle of the compiled resource file
            IFile file = tmpParentFolder.getFile( fileName );
            if( file.exists() )
                file.delete( IResource.DERIVED | IResource.FORCE, monitor );
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream( processedFile );
                file.create( fileInputStream, IResource.DERIVED | IResource.FORCE, monitor );
            } catch( FileNotFoundException e ) {
                log.error( e );
                continue;
            } finally {
                try {
                    if( fileInputStream != null ) {
                        fileInputStream.close();
                    }
                } catch( IOException e ) {
                    log.error( e.getMessage() );
                }
            }
        }
    }

    /**
     * Gets the parent IFolder of the given <code>compildedFile</code>.
     * <p>
     * If the <code>compiledFile</code> is a java file, we return the folder which is .locale_interfaces + package;
     * </p>
     * <p>
     * If the <code>compiledFile</code> is a crb file, we return the folder which is .locale_interfaces because crb files are
     * supposed to be put in the root of the jar file.
     *
     * @param compiledFile
     * @param originalFile
     * @param project
     * @param monitor
     * @return
     * @throws CoreException
     */
    private IFolder getParentFolder( File compiledFile, File originalFile, IProject project, IProgressMonitor monitor )
            throws CoreException {
        // get the resource builder output root folder
        IFolder localeInterfaceFolderRoot = project.getFolder( ImportUtils
                .getImportPref( ResourceBuilder.LOCALE_INTERFACES_FOLDER_NAME ) );
        // create the .locale_interface folder if it does not exist
        ResourceBuilderUtils.createResourcesOutputRoot( project, monitor );
        if( PackageUtils.hasRRCExtension( originalFile.getName() ) ) {
            return localeInterfaceFolderRoot;
        }
        // get the package info of the file
        String packageName = null;
        packageName = PackageUtils.getFilePackageString( originalFile, null );
        IFolder localeInterfaceParentFolder;
        if( StringUtils.isBlank( packageName ) )
            localeInterfaceParentFolder = localeInterfaceFolderRoot;
        else {
            IPath parentFolderPath = new Path( packageName );
            localeInterfaceParentFolder = localeInterfaceFolderRoot.getFolder( parentFolderPath );
        }
        if( !localeInterfaceParentFolder.exists() )
            ImportUtils.createFolders( project, localeInterfaceParentFolder.getProjectRelativePath(), IResource.DERIVED );
        if( !localeInterfaceParentFolder.exists() ) {
            log.error( NLS.bind( Messages.RIMResourcesBuilder_ResourceInterfaceFolderMissingMessage,
                    localeInterfaceParentFolder.getProjectRelativePath() ) );
            return null;
        }
        return localeInterfaceParentFolder;
    }

    /**
     * Create a new marker in the specified resource.
     *
     * @param resource
     * @param message
     * @param lineNumber
     * @param severity
     * @throws CoreException
     * @throws BadLocationException
     */
    void createResourceMarker( IResource resource, String message, int lineNumber, int severity ) {
        try {
            ResourceBuilderUtils.createProblemMarker( resource, IRIMMarker.RESOURCE_BUILD_PROBLEM_MARKER, message, lineNumber,
                    severity );
        } catch( CoreException e ) {
            log.error( e );
        }
    }

    /**
     * Clears previous preprocessor markers from the specified resource.
     *
     * @param resource
     * @param depth
     *
     * @throws CoreException
     */
    void removeResourceBuildMarkers( IResource resource, int depth ) throws CoreException {
        ResourceBuilderUtils.cleanProblemMarkers( resource, new String[] { IRIMMarker.RESOURCE_BUILD_PROBLEM_MARKER }, depth );
    }

    private static int removeFiles( File dir ) throws IOException {
        int count = 0;
        String[] inDir = dir.list();
        if( inDir != null ) {
            for( int j = 0; j < inDir.length; ++j ) {
                File f = new File( dir.getPath(), inDir[ j ] );
                if( f.isDirectory() ) {
                    count += removeFiles( f );
                } else {
                    count += (int) f.length();
                    if( !f.delete() ) {
                        throw new IOException();
                    }
                }
            }
        }
        if( !dir.delete() ) {
            throw new IOException();
        }
        return count;
    }

    private Hashtable< String, String > getResourceBuilderOptions() {
        if( _resourceBuilderOptions == null ) {
            _resourceBuilderOptions = new Hashtable< String, String >();
            _resourceBuilderOptions.put( ResourceCompiler.OPT_OUTPUT_FOLDER, getTempOutputFolder() );
            // For BB OS older than 6.0.0, UTF-16BE doesn't work well in localizing strings for Asian simulators based on that OS
        }
        String vmver = ProjectUtils.getVMVersionForProject( getProject() );
        _resourceBuilderOptions.put( ResourceCompiler.OPT_USE_UTF8, vmver.compareTo( "6.0.0" ) < 0 ? ResourceCompiler.OPT_TRUE
                : ResourceCompiler.OPT_FALSE );
        return _resourceBuilderOptions;
    }

    static private String getTempOutputFolder() {
        if( StringUtils.isBlank( _tmpOutputFolder ) ) {
            _tmpOutputFolder = getTemporaryWorkingDir();
        }
        return _tmpOutputFolder;
    }

    private static synchronized String getTemporaryWorkingDir() {
        String tmpDir = System.getProperty( "java.io.tmpdir" );
        if( tmpDir == null ) {
            // Shouldn't happen
            tmpDir = ".";
        }

        File subDir = null;

        for( int retry = 0; retry < 10; retry++ ) {
            subDir = new File( tmpDir, RESOURCE_BUILD_TMP_FOLDER_HEAD + System.currentTimeMillis() + ConvertUtil.ext_dir );
            if( !subDir.exists() && subDir.mkdir() ) {
                return subDir.toString();
            }
        }

        throw new RuntimeException( "unable to create temporary subdirectory: " + subDir.getPath() );
    }

    private static void cleanupTempSubdir( String name ) {
        if( name == null )
            return;
        try {
            File dir = new File( name );
            String[] inDir = dir.list();
            if( inDir != null ) {
                for( int i = 0; i < inDir.length; ++i ) {
                    File subDir = new File( dir, inDir[ i ] );
                    if( subDir.isDirectory() ) {
                        removeFiles( subDir );
                    } else {
                        subDir.delete();
                    }
                }
            }
            dir.delete();
        } catch( IOException ioe ) {
            log.error( ioe );
        }
    }

    void removeResourceInterface( IFile rrhFile ) throws CoreException {
        if( rrhFile == null )
            return;
        // get the resource builder output root folder
        IProject project = rrhFile.getProject();
        IFolder tmpRoot = project.getFolder( ImportUtils.getImportPref( ResourceBuilder.LOCALE_INTERFACES_FOLDER_NAME ) );
        IPath interfacePath = getPackagePath( rrhFile );
        String rrhFileName = rrhFile.getName();
        String interfaceFileName = rrhFileName.substring( 0, rrhFileName.length() - 4 ) + "Resource.java"; //$NON-NLS-1$
        interfacePath = interfacePath.append( interfaceFileName );
        IFile iFile = tmpRoot.getFile( interfacePath );
        if( iFile.exists() ) {
            iFile.refreshLocal( IResource.DEPTH_ONE, new NullProgressMonitor() );
            iFile.delete( true, new NullProgressMonitor() );
        }
    }

    /**
     * Get the package path of the given <code>file</code>, e.g. net/rim/api The given <code>file</code> should be a file in a
     * source folder.
     *
     * @param file
     * @return
     */
    IPath getPackagePath( IFile file ) {
        IContainer sourceFolder = PackageUtils.getSrcFolder( file );
        IPath sourceFolderPath = sourceFolder.getProjectRelativePath();
        IPath filePath = file.getProjectRelativePath();
        IPath packagePath = filePath.removeFirstSegments( sourceFolderPath.segmentCount() );
        return packagePath.removeLastSegments( 1 );
    }

    void removeCorrespondingCRBFile( IFile rrcFile ) throws CoreException {
        if( rrcFile == null )
            return;

        IFile crbFile = getCorrespondingCRBFile( rrcFile );

        if( crbFile != null && crbFile.exists() ) {
            crbFile.delete( true, new NullProgressMonitor() );
        }
    }

    IFile getCorrespondingCRBFile( IFile rrcFile ) {
        if( rrcFile == null )
            return null;
        // get the resource builder output root folder
        IProject project = rrcFile.getProject();
        IFolder tmpRoot = project.getFolder( ImportUtils.getImportPref( ResourceBuilder.LOCALE_INTERFACES_FOLDER_NAME ) );
        IPath packagePath = getPackagePath( rrcFile );
        String fileName = rrcFile.getName().replace( IConstants.RRC_FILE_EXTENSION_WITH_DOT, IConstants.EMPTY_STRING );
        String packageID = PackageUtils.convertPkgStringToID( packagePath.toString() );
        packageID = packageID.replace( File.separator, IConstants.DOT_MARK );
        String crbFileName = PackageUtils.getCRBFileName( fileName, packageID, false );
        IFile crbFile = tmpRoot.getFile( crbFileName );
        String crbAltFileName;
        if( !crbFile.exists() ) {
            if( !( crbAltFileName = PackageUtils.getCRBFileName( fileName, packageID, true ) ).equals( crbFileName ) ) {
                crbFile = tmpRoot.getFile( crbAltFileName );
                if( !crbFile.exists() ) {
                    return null;
                }
            } else {
                return null;
            }
        }
        return crbFile;
    }

    /**
     * Checks if the given <code>file</code> is in the right package structure.
     *
     * @param file
     * @return
     */
    private boolean hasValidPackage( IFile file ) {
        try {
            String packageID = PackageUtils.getFilePackageString( file.getLocation().toFile(), null );
            IPath packagePath = new Path( packageID );
            IPath currentPackagePath = getPackagePath( file );
            if( currentPackagePath.equals( packagePath ) ) {
                return true;
            }
            String message = Messages.ResourceBuilder_WRONG_PACKAGE_MSG;
            createResourceMarker( file, NLS.bind( message, file.getName() ), -1, IMarker.SEVERITY_ERROR );
            return false;
        } catch( CoreException e ) {
            log.error( e.getMessage() );
            createResourceMarker( file, e.getMessage(), -1, IMarker.SEVERITY_ERROR );
            return false;
        }
    }

    /**
     * Delta visitor for visiting changed resource files for preprocessing.
     */
    private class ResourceDeltaVisitor extends BasicBuilderResourceDeltaVisitor {

        public ResourceDeltaVisitor( IProgressMonitor monitor ) {
            super( monitor );
        }

        protected void buildResource( IResource resource, IProgressMonitor monitor ) throws CoreException {
            if( PackageUtils.hasRRHExtension( resource.getName() ) ) {
                removeResourceInterface( (IFile) resource );
            } else {
                removeCorrespondingCRBFile( (IFile) resource );
            }
            compile( resource, monitor );
            // remove the file from the need rebuild file list
            if( _filesNeedToReBuild != null ) {
                _filesNeedToReBuild.remove( resource );
            }
        }

        @Override
        protected void removeResource( IResource resource, IProgressMonitor monitor ) throws CoreException {
            if( PackageUtils.hasRRHExtension( resource.getName() ) ) {
                removeResourceInterface( (IFile) resource );
            } else if( PackageUtils.hasRRCExtension( resource.getName() ) ) {
                removeCorrespondingCRBFile( (IFile) resource );
            }
            // remove the file from the need rebuild file list
            if( _filesNeedToReBuild != null ) {
                _filesNeedToReBuild.remove( resource );
            }
        }

        @Override
        protected boolean needBuild( IResource resource ) {
            if( resource == null ) {
                return false;
            }
            if( !resource.exists() ) {
                return false;
            }
            if( !PackageUtils.hasRRHExtension( resource.getName() ) && !PackageUtils.hasRRCExtension( resource.getName() ) ) {
                return false;
            }
            if( !hasValidPackage( (IFile) resource ) ) {
                return false;
            }
            // we do not process a resource which is not on the classpath
            if( !JavaCore.create( resource.getProject() ).isOnClasspath( resource ) ) {
                try {
                    if( PackageUtils.hasRRHExtension( resource.getName() ) ) {
                        removeResourceInterface( (IFile) resource );
                    } else {
                        removeCorrespondingCRBFile( (IFile) resource );
                    }

                } catch( CoreException e ) {
                    log.error( "needBuild Error: " + e.getMessage() );
                }
                return false;
            }
            return true;
        }
    }
}
