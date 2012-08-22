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
package net.rim.ejde.internal.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;

import net.rim.ejde.internal.builders.ResourceBuilder;
import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.imports.LegacyImportHelper;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.PreprocessorTag;
import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;
import net.rim.ejde.internal.model.BlackBerryProjectPreprocessingNature;
import net.rim.ejde.internal.model.IModelConstants.IArtifacts;
import net.rim.ide.OSUtils;
import net.rim.ide.Project;
import net.rim.ide.WorkspaceFile;
import net.rim.ide.core.Util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.UserLibrary;
import org.eclipse.jdt.ui.PreferenceConstants;

public class ImportUtils {
    static private final Logger _log = Logger.getLogger( ImportUtils.class );
    static private final int HAS_RES_FILE = 1;
    static private final int HAS_LOCALE_FILE = 2;
    static public final int PROJECT_SRC_FOLDE_INDEX = 0;
    static public final int PROJECT_RES_FOLDE_INDEX = 1;
    static public final int PROJECT_LOCALE_FOLDE_INDEX = 2;

    static private final Pattern validPPTagPattern = Pattern.compile( IConstants.PP_VALIDATION_REG_EX );

    static public String[] POTENTIAL_SOURCE_FOLDERS;
    static {
        POTENTIAL_SOURCE_FOLDERS = new String[ 3 ];
        String folderName = ImportUtils.getImportPref( LegacyImportHelper.PROJECT_SRC_FOLDER_NAME_KEY );
        if( StringUtils.isBlank( folderName ) ) {
            POTENTIAL_SOURCE_FOLDERS[ PROJECT_SRC_FOLDE_INDEX ] = "src";
        } else {
            POTENTIAL_SOURCE_FOLDERS[ PROJECT_SRC_FOLDE_INDEX ] = folderName;
        }
        folderName = ImportUtils.getImportPref( LegacyImportHelper.PROJECT_RES_FOLDER_NAME_KEY );
        if( StringUtils.isBlank( folderName ) ) {
            POTENTIAL_SOURCE_FOLDERS[ PROJECT_RES_FOLDE_INDEX ] = "res";
        } else {
            POTENTIAL_SOURCE_FOLDERS[ PROJECT_RES_FOLDE_INDEX ] = folderName;
        }
        folderName = ImportUtils.getImportPref( LegacyImportHelper.PROJECT_IMPORT_LOCALE_FOLDER_NAME_KEY );
        if( StringUtils.isBlank( folderName ) ) {
            POTENTIAL_SOURCE_FOLDERS[ PROJECT_LOCALE_FOLDE_INDEX ] = "src";
        } else {
            POTENTIAL_SOURCE_FOLDERS[ PROJECT_LOCALE_FOLDE_INDEX ] = folderName;
        }
    }

    /**
     * Creates a IProject instance with its natures and builders initialized.
     *
     * @param name
     * @return An IProject instance or <code>null</null> if any error occurred.
     */
    static public IProject createEclipseProjectHandler( String name, IPath location ) {
        if( StringUtils.isEmpty( name ) )
            throw new IllegalArgumentException( "Can't create Eclipse project for undefined name!" );
        if( location != null ) {
            File projectFile = location.append( ".project" ).toFile();
            // delete the .project if it exists. A new .project will be created.
            if( projectFile.exists() ) {
                projectFile.delete();
            }
        }
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot workspaceRoot = workspace.getRoot();

        IProject eclipseProjectHandler = workspaceRoot.getProject( name );

        if( eclipseProjectHandler.exists() ) {
            // this should not happen because we filter out the existing project
            // on the import wizard
            return eclipseProjectHandler;
        }
        try {
            IProjectDescription projectDescription = createEclipseProjectDescription( name, location );
            eclipseProjectHandler.create( projectDescription, new NullProgressMonitor() );
            if( !eclipseProjectHandler.isOpen() ) {
                eclipseProjectHandler.open( new NullProgressMonitor() );
            }
            // add natures
            initiateProjectNature( eclipseProjectHandler );
            // add builders
            InternalImportUtils.initiateBuilders( eclipseProjectHandler );
        } catch( Throwable t ) {
            _log.error( t.getMessage(), t );
            return null;
        }
        return eclipseProjectHandler;
    }

    static protected IClasspathEntry[] createSourceFolders( IProject eclipseProject, IPath[] sourceFolders ) {

        IContainer projectContainer = eclipseProject.getProject(), sourceContainer = null;

        IClasspathEntry[] sourceClasspathEntries = new IClasspathEntry[ sourceFolders.length ];
        int i = 0;

        for( IPath path : sourceFolders ) {
            if( path.segmentCount() > 0 ) {
                sourceContainer = projectContainer.getFolder( path );

                if( !sourceContainer.exists() ) {
                    try {
                        ( (IFolder) sourceContainer ).create( false, true, new NullProgressMonitor() );
                    } catch( CoreException e ) {
                        _log.error( e.getMessage(), e );
                    }
                }
                sourceClasspathEntries[ i++ ] = JavaCore.newSourceEntry( sourceContainer.getFullPath() );
            }
        }

        return sourceClasspathEntries;
    }

    static public IProjectDescription createEclipseProjectDescription( String projectName, IPath projectLocation ) {
        IProjectDescription projectDescription = ResourcesPlugin.getWorkspace().newProjectDescription( projectName );

        // If it is in the platform workspace, then, we don't need to set this,
        // since by _default_, the project will be created in the platform
        // workspace. If the user has specifically customized this field when
        // creating the project, then we set it here.
        if( ( projectLocation != null ) )
            projectDescription.setLocation( projectLocation.makeAbsolute() );

        return projectDescription;
    }

    /**
     * Creates a java project for the given <code>legacyProject</code>.
     *
     * @param legacyProject
     * @param importType
     * @param userLib
     * @param REPath
     * @param monitor
     * @return
     * @throws CoreException
     */
    static public IJavaProject createJavaProject( Project legacyProject, int importType, String userLib, IPath REPath,
            IProgressMonitor monitor ) throws CoreException {
        IProject project = null;
        if( importType == LegacyImportHelper.COPY_IMPORT ) {
            project = createEclipseProjectHandler( legacyProject.getDisplayName(), null );
        } else {
            String name = legacyProject.getDisplayName();
            IPath legacyProjectPath = getLocationPath( legacyProject );
            IPath eclipseProjectPath = getLocationForEclipseProject( legacyProject, legacyProjectPath, true );
            project = createEclipseProjectHandler( name, eclipseProjectPath );
        }
        return internalCreateJavaProject( legacyProject, project, importType, userLib, REPath, monitor );
    }

    /**
     * Get the location of the Eclipse project. Returns the best possible location for the .project file to be created if one does
     * not already exist.
     *
     * @param sourceProject
     * @return
     */
    public static IPath getLocationPath( Project sourceProject ) {
        IPath folderPath;
        IPath jdpPath = new Path( sourceProject.getFile().getAbsolutePath() );
        // check if this is a multiple jdp scenario
        // get an appropriate (closest to original) location to create the
        // .project file.

        folderPath = getLocationForEclipseProject( sourceProject, jdpPath.removeLastSegments( 1 ), true );

        return folderPath;
    }

    /**
     * Get the Location of .project file associated with this sourceProject or return the best possible path for the .project file
     * to be created.
     *
     * @param sourceProject
     * @param folderPath
     * @param projectName
     * @return
     */
    static public IPath getLocationForEclipseProject( Project legacyProject, IPath folderPath, boolean safe ) {
        String projectName = legacyProject.getDisplayName();

        boolean folderExists = ( new File( folderPath.toOSString() ) ).exists();

        if( folderExists ) {
            if( safe ) {
                boolean go = isMultipleJDP_Ws( folderPath ) || !isProjectFileValid( legacyProject, folderPath );

                if( go ) {
                    // append the projectName as a new folder and see if it has
                    // MultipleJDPs and is not a valid .project file.

                    return getLocationForEclipseProject( legacyProject, folderPath.append( projectName ), safe );
                }
            }
        }

        return folderPath;
    }

    /**
     * Determines if there are multiple JDPs .
     *
     * @param Folder
     *            - RIA project.
     * @return - True if folder contains multiple JDPs or JDWs, false otherwise.
     *
     */
    static public boolean isMultipleJDP_Ws( IPath folderPath ) {

        File folder = new File( folderPath.toOSString() );

        if( !folder.exists() ) {// if the folder doesn't exist we know there
            // can't be multiple JDPs in it.
            return false;
        }

        File[] listOfFiles = folder.listFiles();
        boolean isFirstJDP_W = true;// we expect to find one JDP.

        String fileExtension;
        File file;
        for( int i = 0; i < listOfFiles.length; i++ ) {
            file = listOfFiles[ i ];
            if( file.isFile() ) {
                fileExtension = ( new Path( file.getAbsolutePath() ) ).getFileExtension();

                if( "jdp".equalsIgnoreCase( fileExtension ) || "jdw".equalsIgnoreCase( fileExtension ) ) {
                    if( isFirstJDP_W ) {
                        isFirstJDP_W = false; // the first JDP is found.
                    } else {
                        return true;// second JDP is found.
                    }
                }
            } else if( listOfFiles[ i ].isDirectory() ) {
                // ignore this
            }
        }
        return false;// no second JDP was found.
    }

    /**
     * Validate if the .project file is in fact associated with sourceProject.
     *
     * @param sourceProject
     * @param folder
     *            - Where the jdp for the sourceProject is located.
     * @return True if the .project file is valid or not there. If there is no project file existing in the same directory, it is
     *         safe to assume that the new .project file created by the temporary project will be valid.
     */
    static public boolean isProjectFileValid( Project sourceProject, IPath folder ) {
        // check if validation is needed i.e. if there are other .project files
        // in the same location
        boolean projectFileExists = projectFileExists( folder );

        if( !projectFileExists ) {
            return true;
        }

        // validate .project file. It is possible that .project file here is not
        // associated with this sourceProject

        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        IProjectDescription projectDescription = null;
        try {
            // simply loading the project description from the file is
            // sufficient in this case.
            // We do not need to create a temporary project for this.
            projectDescription = workspace.loadProjectDescription( new Path( getProjectFile( folder ).getAbsolutePath() ) );
        } catch( CoreException e ) {
            _log.error( "Error occured while reading the Project description", e );
        }

        if( sourceProject.getDisplayName().equals( projectDescription.getName() ) ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check to see if the .project file exists.
     *
     * @param folderPath
     * @return
     */
    static public boolean projectFileExists( IPath folderPath ) {
        File folder = new File( folderPath.toOSString() );

        if( !folder.exists() ) {// if the folder doesn't exist we know there
            // can't be a .project file in it.
            return false;
        }

        File[] listOfFiles = folder.listFiles();

        String fileExtension;

        for( int i = 0; i < listOfFiles.length; i++ ) {
            if( listOfFiles[ i ].isFile() ) {
                fileExtension = ( new Path( listOfFiles[ i ].getAbsolutePath() ) ).getFileExtension();

                if( "project".equalsIgnoreCase( fileExtension ) ) {
                    return true;// .project file found.

                }
            } else if( listOfFiles[ i ].isDirectory() ) {
                // ignore this
            }
        }
        return false;// no project file exists.
    }

    /**
     * Get the .project file located in the folderPath.
     *
     * @param folderPath
     * @return - the .project file. null if the folderPath or file does not exist.
     */
    static public File getProjectFile( IPath folderPath ) {

        File folder = new File( folderPath.toOSString() );
        if( !folder.exists() ) {// if the folder doesn't exist we know there
            // can't be a .project file in it.
            return null;
        }
        File[] listOfFiles = folder.listFiles();
        String fileExtension;

        for( int i = 0; i < listOfFiles.length; i++ ) {
            if( listOfFiles[ i ].isFile() ) {
                fileExtension = ( new Path( listOfFiles[ i ].getAbsolutePath() ) ).getFileExtension();
                if( "project".equalsIgnoreCase( fileExtension ) ) {

                    return listOfFiles[ i ];// .project file found.

                }
            } else if( listOfFiles[ i ].isDirectory() ) {
                // ignore this
            }
        }
        return null;// no project file exists.
    }

    static protected IJavaProject internalCreateJavaProject( Project legacyProject, IProject project, int importType,
            String userLib, IPath REPath, IProgressMonitor monitor ) throws CoreException {
        try {
            if( legacyProject == null )
                throw new IllegalArgumentException( "Can't create Eclipse Java Project from undefind legacy project!" );

            _log.debug( "Create IJavaProject [" + legacyProject.getDisplayName() + "]" );
            monitor.beginTask( "Importing project " + legacyProject.getDisplayName(), 10 );
            monitor.worked( 1 );
            if( project == null )
                return null;
            // create a IJavaProject
            IJavaProject eclipseJavaProject = JavaCore.create( project );
            // set project options
            initializeProjectOptions( eclipseJavaProject );
            monitor.worked( 2 );
            // get source folders, e.g. src and res
            IPath[] defaultSources = getCustomSourceFolders( legacyProject );
            // create classpath entries for source folders
            IClasspathEntry[] sourceFolderEntries = createSourceFolders( project, defaultSources );
            IClasspathEntry[] importedLibraries = resolveProjectImports( legacyProject, project );
            String[] dependencies = getDependsOnProjectNames( legacyProject );
            IClasspathEntry[] dependentProjects = resolveDependentProjects( dependencies );
            IClasspathEntry[] jreEntries = getREClasspathEntries( REPath );
            List< IClasspathEntry > classpathEntries = new ArrayList< IClasspathEntry >();
            addEntryToList( classpathEntries, sourceFolderEntries );
            addEntryToList( classpathEntries, importedLibraries );
            addEntryToList( classpathEntries, dependentProjects );
            addEntryToList( classpathEntries, jreEntries );
            classpathEntries = applyExclusionPatterns( project, legacyProject, classpathEntries );
            // add workspace imports as a user library
            if( !StringUtils.isBlank( userLib ) ) {
                UserLibrary library = JavaModelManager.getUserLibraryManager().getUserLibrary( userLib );
                if( null != library ) {
                    IPath path = new Path( JavaCore.USER_LIBRARY_CONTAINER_ID ).append( userLib );
                    IClasspathEntry userLibEntry = JavaCore.newContainerEntry( path );
                    if( !classpathEntries.contains( userLibEntry ) ) {
                        classpathEntries.add( userLibEntry );
                    }
                }
            }
            if( !eclipseJavaProject.isOpen() ) {
                try {
                    eclipseJavaProject.open( new NullProgressMonitor() );
                } catch( JavaModelException e ) {
                    _log.error( e.getMessage() );
                    throw new CoreException( StatusFactory.createErrorStatus( e.getMessage() ) );
                }
            }
            setRawClassPath( eclipseJavaProject, classpathEntries.toArray( new IClasspathEntry[ classpathEntries.size() ] ) );
            monitor.worked( 2 );
            // link or copy files
            if( importType == LegacyImportHelper.COPY_IMPORT ) {
                copySourceFiles( legacyProject, eclipseJavaProject.getProject() );

                int AEPNumber = legacyProject.getNumEntries();

                // Copy AEP source files as well.
                for( int i = 0; i < AEPNumber; i++ ) {
                    copySourceFiles( legacyProject.getEntry( i ), eclipseJavaProject.getProject() );
                }
            } else {
                /**
                 * String: the package id, the container key. Set<File>: the files belonging to the package
                 * */
                ResourcesBuffer resourcesBuffer;
                resourcesBuffer = createProjectResourcesBuffer( legacyProject );
                createBulkLinks( eclipseJavaProject, resourcesBuffer, legacyProject );
                int AEPNumber = legacyProject.getNumEntries();
                // link AEP source files as well.
                for( int i = 0; i < AEPNumber; i++ ) {
                    resourcesBuffer = createProjectResourcesBuffer( legacyProject.getEntry( i ) );
                    createBulkLinks( eclipseJavaProject, resourcesBuffer, legacyProject.getEntry( i ) );
                }
            }
            monitor.worked( 6 );
            return eclipseJavaProject;
        } finally {
            monitor.done();
        }
    }

    /**
     *
     * @param IJavaProject
     * @param ResourcesBuffer
     * @param project
     *
     * @throws IllegalArgumentException
     *             , NullPointerException for bounds and missing src directory
     * */
    static public void createBulkLinks( IJavaProject iJavaProject, ResourcesBuffer buffer, Project project ) {
        if( null == iJavaProject )
            throw new IllegalArgumentException( new NullPointerException( "Undefined java project!" ) );

        if( null == buffer )
            throw new IllegalArgumentException( new NullPointerException( "Undefined package container!" ) );

        /**
         *
         * Processing all files
         *
         * */
        List< LinkBuffer > linkBuffers = generateLinks( iJavaProject, buffer, project );

        long start, end, total;

        _log.debug( "Start creating links." );
        start = System.currentTimeMillis();
        for( LinkBuffer linkBuffer : linkBuffers ) {
            linkBuffer.create();
        }
        end = System.currentTimeMillis();

        total = end - start;

        _log.debug( "Finished creating links in [" + total / 1000 + "] seconds." );
    }

    /**
     * resources already in a source folder don't need a link, otherwise they'll be duplicated in bin/! jars will be handled by
     * LIB type classpath
     *
     * @param packageResource
     *            : the Eclipse resource representing the package root
     * @return boolean
     */
    static private boolean isResourceTargetFolder( IResource packageResource ) {
        return null != packageResource
                && packageResource.exists()
                && !ImportUtils.getImportPref( ResourceBuilder.LOCALE_INTERFACES_FOLDER_NAME ).equalsIgnoreCase(
                        packageResource.getName() );
    }

    /**
     * resources already in a source folder don't need a link, otherwise they'll be duplicated in bin/! jars will be handled by
     * LIB type classpath
     *
     * @param filePth
     *            : path to the file
     * @param packageResource
     *            : the Eclipse resource representing the package segment
     * @return boolean
     */
    static private boolean canIgnoreFile( IPath filePath, IJavaProject eclipseJavaProject ) {
        if( null == filePath ) {
            throw new IllegalArgumentException( "Can't evaluate undefined path!" );
        }

        if( null == eclipseJavaProject || !eclipseJavaProject.exists() ) {
            throw new IllegalArgumentException( "Can't evaluate undefined Eclipse JDT project!" );
        }

        if( IConstants.JAR_EXTENSION.equalsIgnoreCase( filePath.getFileExtension() ) )
            return true;

        return false;
    }

    static public IPath assureFolderPath( IFolder packageFolder, IPath rpath ) {
        if( null == packageFolder )
            throw new IllegalArgumentException( "Can't process undefined folder!" );

        if( null == rpath )
            throw new IllegalArgumentException( "Can't process undefined path!" );

        IPath rfpath = rpath.removeLastSegments( 1 );

        if( !packageFolder.exists( rfpath ) ) {
            int segmentCount = rfpath.segmentCount();
            boolean alternate = false;
            String lfseg;
            IResource[] members = null;
            IFolder pfolder;
            IPath rfpath_;

            for( int i = 0; i < segmentCount; i++ ) {
                rfpath_ = i < segmentCount - 1 ? rfpath.removeLastSegments( segmentCount - 1 - i ) : rfpath;

                if( !packageFolder.exists( rfpath_ ) ) {
                    pfolder = packageFolder.getFolder( rfpath_ );

                    try {
                        pfolder.create( true, true, new NullProgressMonitor() );
                    } catch( CoreException e ) {
                        _log.error( e );
                        return null;
                    }

                    pfolder = packageFolder.getFolder( rfpath_.removeLastSegments( 1 ) );

                    try {
                        members = pfolder.members();
                    } catch( Throwable t ) {
                        _log.error( t.getMessage(), t );
                        return null;
                    }

                    for( IResource member : members ) {
                        if( IResource.FOLDER == member.getType()
                                && ( lfseg = member.getFullPath().lastSegment() ).equalsIgnoreCase( rfpath_.lastSegment() ) ) {
                            alternate = true;
                            // substitute the case-mismatch segment
                            rfpath_ = rfpath.removeLastSegments( segmentCount - i ).append( lfseg );

                            if( i < segmentCount - 1 ) {
                                rfpath = rfpath_.append( rfpath.removeFirstSegments( i + 1 ) );
                            } else {
                                rfpath = rfpath_;
                            }
                        }
                    }
                }
            }

            if( alternate ) {
                rpath = rfpath.append( rpath.lastSegment() );
            }
        }

        return rpath;
    }

    static public IFile createFileHandle( IFolder parent, String name ) {
        if( null == parent )
            throw new IllegalArgumentException( "Can't create handle for undefined parent!" );

        if( StringUtils.isBlank( name ) )
            throw new IllegalArgumentException( "Can't create handle for undefined name!" );

        IWorkspaceRoot workspaceRoot = parent.getWorkspace().getRoot();
        IPath fullPath = parent.getFullPath().append( name );
        return workspaceRoot.getFile( fullPath );
    }

    static private List< LinkBuffer > generateLinks( IJavaProject eclipseJavaProject, ResourcesBuffer buffer,
            Project legacyProject ) {
        Map< String, Set< File >> javaArtifacts = buffer.getJavaContener();
        Map< String, Set< File >> localeArtifacts = buffer.getlocaleContener();
        Set< File > nonPackageableFiles = buffer.getNonPackageContener();

        IPath drfpath = null, filePath = null;

        IFile eclipseFileHandle = null, fileHandle = null;

        IProject eclipseProject = eclipseJavaProject.getProject();
        IWorkspaceRoot workspaceRoot = eclipseProject.getWorkspace().getRoot();

        List< String > sources = net.rim.ejde.internal.legacy.Util.getSources( legacyProject );
        IPackageFragmentRoot[] packageFragmentRoots;
        IPackageFragment packageFragment;
        IFolder packageFolder;
        IResource resource, packageDirectory;
        List< LinkBuffer > linkBuffers = Collections.emptyList();
        try {
            // packageFragmentRoots =
            // eclipseJavaProject.getPackageFragmentRoots(); //!WARNING: it
            // seems this is buggy!!!!
            packageFragmentRoots = eclipseJavaProject.getAllPackageFragmentRoots();
            linkBuffers = new ArrayList< LinkBuffer >();
            String srcFolder = POTENTIAL_SOURCE_FOLDERS[ PROJECT_SRC_FOLDE_INDEX ];
            String resFolder = POTENTIAL_SOURCE_FOLDERS[ PROJECT_RES_FOLDE_INDEX ];
            String localeFolder = POTENTIAL_SOURCE_FOLDERS[ PROJECT_LOCALE_FOLDE_INDEX ];
            IJavaProject javaProject = null;
            for( IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots ) {
                javaProject = packageFragmentRoot.getParent().getJavaProject();
                if( javaProject == null || !javaProject.equals( eclipseJavaProject ) ) {
                    // fixed DPI225325, we only care source folders in the
                    // current project
                    continue;
                }
                if( IPackageFragmentRoot.K_SOURCE == packageFragmentRoot.getKind() ) {
                    packageDirectory = packageFragmentRoot.getResource();

                    if( null != packageDirectory ) {
                        if( isResourceTargetFolder( packageDirectory ) ) {
                            if( IResource.FOLDER == packageDirectory.getType() ) {
                                // handle resource files which are not java, rrh
                                // and rrc
                                if( resFolder.equalsIgnoreCase( packageDirectory.getName() ) ) {
                                    packageFragment = packageFragmentRoot.createPackageFragment( StringUtils.EMPTY, true,
                                            new NullProgressMonitor() );
                                    packageFolder = (IFolder) packageFragment.getResource();

                                    for( File file : nonPackageableFiles ) {
                                        filePath = new Path( file.getAbsolutePath() );

                                        if( canIgnoreFile( filePath, eclipseJavaProject ) ) {
                                            continue;
                                        }

                                        // drfpath = PackageUtils.resolvePathForFile( filePath, legacyProjectPath,
                                        // legacyWorkspacePath ); // DPI222295
                                        try {
                                            drfpath = new Path( PackageUtils.getFilePackageString( filePath.toFile(),
                                                    legacyProject ) ).append( filePath.lastSegment() );
                                        } catch( CoreException e ) {
                                            _log.error( e.getMessage() );
                                            drfpath = new Path( IConstants.EMPTY_STRING );
                                        }

                                        if( drfpath.segmentCount() > 1 ) {
                                            if( sources.contains( drfpath.segment( 0 ) ) ) {
                                                drfpath = drfpath.removeFirstSegments( 1 );
                                            }

                                            drfpath = assureFolderPath( packageFolder, drfpath );
                                        }

                                        fileHandle = createFileHandle( packageFolder, drfpath.toOSString() );

                                        resource = eclipseProject.findMember( PackageUtils.deResolve( filePath,
                                                eclipseProject.getLocation() ) );

                                        if( resource != null )
                                            eclipseFileHandle = workspaceRoot.getFile( resource.getFullPath() );
                                        else
                                            eclipseFileHandle = workspaceRoot.getFile( eclipseProject.getFullPath().append(
                                                    drfpath ) );

                                        if( !fileHandle.equals( eclipseFileHandle ) ) {
                                            linkBuffers.add( new LinkBuffer( fileHandle, filePath ) );
                                        }
                                    }
                                }
                                if( srcFolder.equalsIgnoreCase( packageDirectory.getName() )
                                        || srcFolder.equalsIgnoreCase( packageDirectory.getName() ) ) { // All
                                    linkPackagableFiles( javaProject, packageFragmentRoot, javaArtifacts, linkBuffers );
                                }
                                if( localeFolder.equalsIgnoreCase( packageDirectory.getName() )
                                        || localeFolder.equalsIgnoreCase( packageDirectory.getName() ) ) {
                                    linkPackagableFiles( javaProject, packageFragmentRoot, localeArtifacts, linkBuffers );
                                }
                            } else {
                                continue;
                            }
                        }
                    }
                }
            }
        } catch( JavaModelException e1 ) {
            _log.error( e1.getMessage(), e1 );
        }

        return linkBuffers;
    }

    static private void linkPackagableFiles( IJavaProject javaProject, IPackageFragmentRoot packageFragmentRoot,
            Map< String, Set< File >> packagableContainer, List< LinkBuffer > linkBuffers ) throws JavaModelException {
        IResource resource;
        IFolder packageFolder;
        IPath filePath = null;
        IPath drfpath = null;
        IFile eclipseFileHandle = null, fileHandle = null;
        StringTokenizer tokenizer;
        IPackageFragment packageFragment;
        Set< File > packageableFiles;
        Set< String > packageIds = packagableContainer.keySet();
        for( String packageId : packageIds ) {
            packageableFiles = packagableContainer.get( packageId );

            if( StringUtils.isBlank( packageId ) ) {
                packageId = "";
            }

            packageFragment = packageFragmentRoot.createPackageFragment( packageId, true, new NullProgressMonitor() );

            if( IResource.FOLDER == packageFragment.getResource().getType() ) {
                packageFolder = (IFolder) packageFragment.getResource();

                for( File file : packageableFiles ) {
                    filePath = new Path( file.getAbsolutePath() );

                    if( canIgnoreFile( filePath, javaProject ) ) {
                        continue;
                    }

                    tokenizer = new StringTokenizer( packageId, "." );

                    drfpath = new Path( "" );

                    while( tokenizer.hasMoreElements() ) {
                        drfpath.append( tokenizer.nextToken() );
                    }

                    assureFolderPath( packageFolder, drfpath );

                    fileHandle = createFileHandle( packageFolder, file.getName() );

                    resource = javaProject.getProject().findMember(
                            PackageUtils.deResolve( filePath, javaProject.getProject().getLocation() ) );

                    if( resource != null )
                        eclipseFileHandle = javaProject.getProject().getWorkspace().getRoot().getFile( resource.getFullPath() );
                    else
                        eclipseFileHandle = null;

                    if( !fileHandle.equals( eclipseFileHandle ) ) {
                        linkBuffers.add( new LinkBuffer( fileHandle, filePath ) );
                    }
                }
            }
        }
    }

    /**
     * Creates a eclipse file link to the rim Project JDP file
     *
     * @param eclipseProject
     * @param legacyProject
     */

    static public void createFileLink( IProject eclipseProject, Project legacyProject ) {
        IFile file = eclipseProject.getFile( legacyProject.getFile().getName() );
        IPath absolutePath = new Path( legacyProject.getFile().getAbsolutePath() );

        if( !file.exists() )
            try {
                file.createLink( absolutePath, IResource.NONE, new NullProgressMonitor() );
            } catch( CoreException e ) {
                _log.error( e.getMessage(), e );
            }
    }

    static public ResourcesBuffer createProjectResourcesBuffer( Project project ) {
        if( null == project )
            throw new IllegalArgumentException( new NullPointerException( "Project can't be null!" ) );

        int fileCount = project.getNumFiles();

        Set< File > javaArtifacts = new TreeSet< File >();
        Set< File > localeArtifacts = new TreeSet< File >();
        Set< File > nonPackageableArtifacts = new TreeSet< File >();
        List< String > phantomArtifacts = new ArrayList< String >();

        WorkspaceFile workspaceFile;

        File file;
        String phantomArtifact = null;

        for( int i = 0; i < fileCount; i++ ) {
            workspaceFile = project.getSourceFile( i );

            file = new File( OSUtils.replaceFileSeperator( workspaceFile.getFile().getAbsolutePath() ) );

            if( !file.exists() ) {
                try {
                    phantomArtifact = file.getCanonicalPath();
                } catch( IOException e ) {
                    _log.error( e.getMessage(), e );
                }

                phantomArtifacts.add( "Missing file returned by legacy model [" + phantomArtifact + "]." );
                continue;
            }

            if( workspaceFile.getIsJava() ) {
                javaArtifacts.add( file );
                continue;
            } else if( workspaceFile.getIsResourceHeader() || workspaceFile.getIsLanguageResource() ) {
                localeArtifacts.add( file );
                continue;
            }

            nonPackageableArtifacts.add( file );
        }

        Map< String, Set< File >> javaContainer = buildPackageContainer( javaArtifacts );
        Map< String, Set< File >> localeContainer = buildPackageContainer( localeArtifacts );

        ResourcesBuffer resourcesBuffer;

        resourcesBuffer = new ResourcesBuffer( javaContainer, localeContainer, nonPackageableArtifacts );

        return resourcesBuffer;
    }

    /**
     * Process Java packages and files declared by the corresponding *.jdp metadata of the legacy Project entity
     */
    static public Map< String, Set< File >> buildPackageContainer( Set< File > legacyProjectFiles ) {
        Map< String, Set< File >> container = new HashMap< String, Set< File >>();

        Set< File > files;

        String fileName = "", rrhFileName, packageId;

        Map< RRHFile, Set< File >> rrcForRrhFiles = new HashMap< RRHFile, Set< File >>();
        Map< String, String > rrhForPackageId = new HashMap< String, String >();

        boolean isJava;
        File directory;
        File rrhFile;
        RRHFile rrhFileTuple;

        for( File file : legacyProjectFiles ) {
            packageId = null;

            if( null != file && file.exists() ) {
                fileName = file.getName();
                packageId = IConstants.EMPTY_STRING;
                try {
                    /*
                     * .java,.rrh and.rrc files should be treated differently than any other files because.java file can have
                     * package declaration and.rrh file must have package declaration(requirement)and we treat rrc files based on
                     * rrh files package name.
                     *
                     * If the file is not.java or.rrh or.rrc then the package name will be empty string.
                     */
                    if( PackageUtils.hasSourceExtension( fileName ) ) {
                        if( PackageUtils.hasRRCExtension( fileName ) ) {
                            if( fileName.contains( "_" ) ) {
                                rrhFileName = fileName.substring( 0, fileName.indexOf( '_' ) );
                            } else {
                                rrhFileName = fileName.substring( 0, fileName.indexOf( '.' ) );
                            }

                            directory = file.getParentFile();

                            if( directory.exists() && directory.isDirectory() ) {
                                rrhFile = new File( directory.getCanonicalPath() + File.separator + rrhFileName + ".rrh" );

                                if( rrhFile.exists() && rrhFile.isFile() ) {
                                    try {
                                        packageId = PackageUtils.getFilePackageString( rrhFile, null );
                                        packageId = PackageUtils.convertPkgStringToID( packageId );
                                    } catch( CoreException e ) {
                                        _log.error( e.getMessage() );
                                        packageId = IConstants.EMPTY_STRING;
                                    }
                                    rrhFileTuple = new RRHFile( rrhFileName, packageId );

                                    files = rrcForRrhFiles.get( rrhFileTuple );

                                    if( null == files ) {
                                        files = new HashSet< File >();
                                        rrcForRrhFiles.put( rrhFileTuple, files );
                                    }

                                    files.add( file );
                                }
                            }

                            continue;
                        }

                        isJava = PackageUtils.hasJavaExtension( fileName );

                        if( isJava || PackageUtils.hasRRHExtension( fileName ) ) {
                            try {
                                packageId = PackageUtils.getFilePackageString( file, null );
                            } catch( CoreException e ) {
                                _log.error( e.getMessage() );
                            }
                            packageId = PackageUtils.convertPkgStringToID( packageId );
                            // Is default package?
                            if( StringUtils.isBlank( packageId ) ) {
                                packageId = IConstants.EMPTY_STRING;
                            }

                            if( !isJava ) {
                                rrhForPackageId
                                        .put( fileName.replaceAll( IConstants.RRH_FILE_EXTENSION_WITH_DOT, "" ), packageId );
                            }

                            files = container.get( packageId );

                            if( null == files ) {
                                files = new HashSet< File >();
                                container.put( packageId, files );
                            }

                            files.add( file );
                        }
                    } else {
                        // Is not a package-able artifact *.java/*.rrh/*.rrc
                        files = container.get( "" );

                        if( null == files ) {
                            files = new HashSet< File >();
                            container.put( "", files );
                        }

                        files.add( file );

                        continue;
                    }
                } catch( Throwable e ) {
                    _log.error( e.getMessage(), e );
                }
            }
        }// for

        Set< String > rrhFileNames = rrhForPackageId.keySet();
        Set< File > rrcFiles;

        for( String rrhFileKey : rrhFileNames ) {
            packageId = rrhForPackageId.get( rrhFileKey );
            rrcFiles = rrcForRrhFiles.remove( rrhFileKey );

            if( null == rrcFiles )
                continue;

            for( File rrcFile : rrcFiles ) {
                files = container.get( packageId );
                files.add( rrcFile );
            }
        }

        if( !rrcForRrhFiles.isEmpty() ) {
            Set< RRHFile > rrhFiles = rrcForRrhFiles.keySet();
            // No package found then use the default package
            // packageId = "";
            rrhFile = null;

            for( RRHFile rrhFileCursor : rrhFiles ) {
                rrcFiles = rrcForRrhFiles.get( rrhFileCursor );

                // create eg:- Theme.rrh
                /*
                 * rrhFileName = rrhFileCursor._fileName + IConstants.RRH_FILE_EXTENSION_WITH_DOT;
                 *
                 * rrhPathStr = ""; rrcFileName = "";
                 *
                 * for( File extraFile : rrcFiles ) { rrhPathStr = extraFile.getAbsolutePath(); rrcFileName = extraFile.getName();
                 *
                 * break; }
                 *
                 * rrhPathStr = rrhPathStr.replace( rrcFileName, rrhFileName );
                 *
                 * / if( !StringUtils.isBlank( rrhPathStr ) ) { rrhFile = new File( rrhPathStr ); // This algorithm might fail on
                 * non-Windows system // because of possible case sensitivity issues. if( rrhFile.exists() ) { packageId =
                 * parseFileForPackageId( rrhFile ); } }
                 */

                files = container.get( rrhFileCursor._packageId );

                if( null == files ) {
                    files = new HashSet< File >();
                    container.put( rrhFileCursor._packageId, files );
                }

                for( File extraFile : rrcFiles ) {
                    files.add( extraFile );
                }
            }
        }

        return container;
    }

    static public int hasResource( Project legacyProject ) {
        int result = 0;
        int nsf = legacyProject.getNumFiles();
        WorkspaceFile wsFile;
        for( int i = 0; i < nsf; i++ ) {
            wsFile = legacyProject.getSourceFile( i );
            if( wsFile.getIsJava() ) {
                continue;
            }
            if( wsFile.getIsResourceHeader() || wsFile.getIsLanguageResource() ) {
                if( ( result & HAS_LOCALE_FILE ) == 0 ) {
                    result = result | HAS_LOCALE_FILE;
                    if( ( result & HAS_RES_FILE ) != 0 ) {
                        return result;
                    }
                }
                continue;
            }
            if( ( result & HAS_RES_FILE ) == 0 ) {
                result = result | HAS_RES_FILE;
                if( ( result & HAS_LOCALE_FILE ) != 0 ) {
                    return result;
                }
            }
        }
        return result;
    }

    static protected IPath[] getCustomSourceFolders( Project legacyProject ) {
        Set< IPath > sourcePathSet = new LinkedHashSet< IPath >();
        sourcePathSet.add( new Path( POTENTIAL_SOURCE_FOLDERS[ PROJECT_SRC_FOLDE_INDEX ] ) );

        String resFolderName = POTENTIAL_SOURCE_FOLDERS[ PROJECT_RES_FOLDE_INDEX ];
        if( StringUtils.isNotBlank( resFolderName ) ) {
            sourcePathSet.add( new Path( ImportUtils.getImportPref( LegacyImportHelper.PROJECT_RES_FOLDER_NAME_KEY ) ) );
        }

        String localeFolderName = POTENTIAL_SOURCE_FOLDERS[ PROJECT_LOCALE_FOLDE_INDEX ];
        if( StringUtils.isNotBlank( localeFolderName ) ) {
            sourcePathSet.add( new Path( ImportUtils.getImportPref( LegacyImportHelper.PROJECT_IMPORT_LOCALE_FOLDER_NAME_KEY ) ) );
        }

        return sourcePathSet.toArray( new IPath[ sourcePathSet.size() ] );
    }

    /**
     * Get the IClasspathEntry for the give runtime environment path <code>REPath</code>.
     *
     * @param REPath
     * @return
     */
    static public IClasspathEntry[] getREClasspathEntries( IPath REPath ) {
        if( REPath != null ) {
            return new IClasspathEntry[] { JavaCore.newContainerEntry( REPath ) };
        }
        return PreferenceConstants.getDefaultJRELibrary();
    }

    static protected void addEntryToList( List< IClasspathEntry > entryList, IClasspathEntry[] entries ) {
        for( int i = 0; i < entries.length; i++ ) {
            if( entries[ i ] != null ) {
                entryList.add( entries[ i ] );
            }
        }
    }

    static protected IClasspathEntry[] resolveDependentProjects( String[] dependencies ) {
        IClasspathEntry[] sourceClasspathEntries = new IClasspathEntry[ dependencies.length ];
        int i = 0;
        for( String project : dependencies ) {
            sourceClasspathEntries[ i++ ] = JavaCore.newProjectEntry( new Path( IConstants.BACK_SLASH_MARK + project ), true );
        }
        return sourceClasspathEntries;
    }

    /**
     * Sets the given <code>classpathEntries</code> to the <code>eclipseJavaProject</code>.
     *
     * @param eclipseJavaProject
     * @param classpathEntries
     */
    static public void setRawClassPath( IJavaProject eclipseJavaProject, IClasspathEntry[] classpathEntries ) {
        if( null == eclipseJavaProject )
            throw new IllegalArgumentException( "Can't add classpath entries to undefined project!" );
        if( null == classpathEntries )
            throw new IllegalArgumentException( "Can't add undefined classpath entries to the project!" );
        if( 0 == classpathEntries.length )
            return;
        try {
            eclipseJavaProject.setRawClasspath( classpathEntries, new NullProgressMonitor() );
        } catch( JavaModelException e ) {
            _log.error( e.getMessage(), e );
        }

    }

    /**
     * Sets the RIM and Java project natures on the given project.
     *
     * @param project
     *            The Eclipse project to set the nature on
     * @param monitor
     *            The workspace progress monitor
     * @throws CoreException
     */
    static public void initiateProjectNature( IProject project ) throws CoreException {
        // Add BB core nature to the new BB Project if it is BB project
        // This nature must be first so that the project will be properly decorated
        if( !project.hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
            addNatureToProject( project, BlackBerryProjectCoreNature.NATURE_ID, true );
        }
        // Add Preprocessing nature
        if( !project.hasNature( BlackBerryProjectPreprocessingNature.NATURE_ID ) ) {
            addNatureToProject( project, BlackBerryProjectPreprocessingNature.NATURE_ID, false );
        }
        // Add Java Nature
        if( !project.hasNature( JavaCore.NATURE_ID ) ) {
            addNatureToProject( project, JavaCore.NATURE_ID, false );
        }
    }

    /**
     * Helper method that installs a Project Nature onto a project.
     *
     * @param proj
     *            the Eclipse project
     * @param natureId
     *            the ID of the project nature
     * @param mustBeFirst
     *            whether the nature must be the first in the list. An icon must be first if the appropriate decorator is to be
     *            shown.
     *
     * @throws CoreException
     *             the core exception
     */
    static public void addNatureToProject( IProject proj, String natureId, boolean mustBeFirst ) throws CoreException {
        IProjectDescription description = proj.getDescription();

        String[] prevNatures = description.getNatureIds();
        String[] newNatures = new String[ prevNatures.length + 1 ];
        if( mustBeFirst ) {
            System.arraycopy( prevNatures, 0, newNatures, 1, prevNatures.length );
            newNatures[ 0 ] = natureId;
        } else {
            System.arraycopy( prevNatures, 0, newNatures, 0, prevNatures.length );
            newNatures[ prevNatures.length ] = natureId;
        }
        description.setNatureIds( newNatures );

        proj.setDescription( description, new NullProgressMonitor() );
    }

    /**
     * Sets a whole bunch of project specific settings.
     * <p>
     * http://help.eclipse.org/help31/topic/org.eclipse.jdt.doc.isv/reference/
     * api/org/eclipse/jdt/core/JavaCore.html#getDefaultOptions()
     *
     * @param project
     */
    @SuppressWarnings("unchecked")
    static public void initializeProjectOptions( IJavaProject javaProject ) {
        if( null == javaProject )
            throw new IllegalArgumentException();

        final Map map = javaProject.getOptions( false );

        if( map.size() > 0 ) {
            map.remove( JavaCore.COMPILER_COMPLIANCE );
            map.remove( JavaCore.COMPILER_SOURCE );
            map.remove( JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM );
        }

        map.put( JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3 );
        // DPI 221069 --> Bugzilla id=250185
        map.put( JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_4 );
        map.put( JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_2 );

        javaProject.setOptions( map );
    }

    /**
     * Gets the names of the projects which are depended by the <code>legacyProject</code>.
     *
     * @param legacyProject
     * @return
     */
    static public String[] getDependsOnProjectNames( Project legacyProject ) {
        int dependsDirectlyOnCont = legacyProject.getNumDependsDirectlyOn();
        String[] dependsOnProjectNames = new String[ dependsDirectlyOnCont ];

        try {// for watherver reason - legayc API crashes etc, we don't want to
             // create a case for NPE in the caller
            String projectName;

            for( int count = 0; count < dependsDirectlyOnCont; count++ ) {
                projectName = legacyProject.getDependsDirectlyOn( count ).getDisplayName();

                if( StringUtils.isNotBlank( projectName ) )
                    dependsOnProjectNames[ count ] = projectName;
                else
                    _log.error( "Can't add an undefined project as dependency!" );
            }
        } catch( Throwable t ) {
            _log.error( t.getMessage(), t );
        }

        return dependsOnProjectNames;
    }

    /**
     * Gets the path of the potential source jar for the given library.
     *
     * @param libPath
     *            Path of a library jar
     * @return The potential source jar for the given library
     */
    public static IPath getSourceJarPath( IPath libPath ) {
        if( libPath == null || libPath.isEmpty() ) {
            return null;
        }
        IPath sourcePath;
        String libraryName, sourceJarName;
        libraryName = libPath.lastSegment();
        sourceJarName = libraryName.substring( 0, libraryName.length() - 4 );
        sourceJarName += "-sources.jar";
        sourcePath = libPath.removeLastSegments( 1 ).append( sourceJarName );
        return sourcePath;
    }

    /**
     * Extracts a list of jar files that a RIM project needs on its build path. It does not actually assign the build path entries
     * to the Eclipse project - it is up to the caller to do this.
     *
     * @param iProject
     *            the Eclipse project
     * @param project
     *            the RIM project.
     * @return a list of classpath entries representing import JAR files.
     */
    @SuppressWarnings("unchecked")
    static public IClasspathEntry[] resolveProjectImports( Project _project, IProject eclipseProject ) {
        Vector< File > importJars = _project.getImports();

        List< IClasspathEntry > jars = new ArrayList< IClasspathEntry >();

        /**
         * The file list can contain environment variables. Also, it is correct only to use file.getPath(). Using absolute paths
         * with environment variables is a recipe for disaster.
         */
        for( File importJar : importJars ) {
            jars.add( addClassPathEntry( eclipseProject, new File( OSUtils.replaceFileSeperator( importJar.getPath() ) ), false ) );// Mac
                                                                                                                                    // support
        }

        // also get [Files] .jar entries
        File file;
        String path;
        int idx;

        int numfiles = _project.getNumFiles();

        for( int i = 0; i < numfiles; i++ ) {
            file = _project.getSourceFile( i ).getFile();
            path = file.getPath();

            if( ( idx = path.lastIndexOf( '.' ) ) > 0 && ".jar".equalsIgnoreCase( path.substring( idx ) ) ) {
                jars.add( addClassPathEntry( eclipseProject, file, true ) );
            }
        }

        return jars.toArray( new IClasspathEntry[ jars.size() ] );
    }

    static public IClasspathEntry addClassPathEntry( IProject project, File importJar, boolean export ) {
        String importJarPath = Util.replaceEnvVars( importJar.getPath() );

        IPath importJarLocation = new Path( importJarPath );
        int nms;

        if( ( nms = project.getLocation().matchingFirstSegments( importJarLocation ) ) == project.getLocation().segmentCount() ) {
            importJarLocation = project.getFile( importJarLocation.removeFirstSegments( nms ) ).getFullPath();
        }

        // check to ensure path is resolved
        if( !importJarLocation.isAbsolute() ) {
            if( "BBMProtocolTest".equalsIgnoreCase( project.getName() ) ) {
                _log.debug( "found zombie BBMProtocolTest project!" );
            }

            String resolvedPath = EnvVarUtils.resolveVarToString( importJarLocation.toString() );

            importJarLocation = EnvVarUtils.resolveVarToPath( resolvedPath );

            _log.warn( "Can't add not absolute library path [" + importJarLocation + "]to the project classpath!" );
        }

        IPath sourceJarPath = getSourceJarPath( importJarLocation );
        IClasspathEntry entry;
        if( sourceJarPath == null || sourceJarPath.isEmpty() || !sourceJarPath.toFile().exists() ) {
            entry = JavaCore.newLibraryEntry( importJarLocation, null, null, export );
        } else {
            entry = JavaCore.newLibraryEntry( importJarLocation, sourceJarPath, null, export );
        }

        return entry;
    }

    /**
     * Returns the array of all referenced Projects for a given Project
     *
     * @param project
     *            the given Project
     * @return the set of referenced Projects, calculated recursively
     * @throws CoreException
     *             if an error occurs while computing referenced projects
     */
    public static Set< Project > getAllReferencedProjects( Project project ) throws CoreException {
        Set< Project > referencedProjects = new HashSet< Project >();
        addReferencedProjects( project, referencedProjects );
        return referencedProjects;
    }

    private static void addReferencedProjects( Project project, Set< Project > references ) throws CoreException {
        Project refProject;
        for( int i = 0; i < project.getNumDependsDirectlyOn(); i++ ) {
            refProject = project.getDependsDirectlyOn( i );
            if( !references.contains( refProject ) ) {
                references.add( refProject );
                addReferencedProjects( refProject, references );
            }
        }
    }

    /**
     * Copies files form the <code>legacyProject</code> to the eclipse project <code>iproject</code>.
     *
     * @param legacyProject
     * @param iproject
     */
    static public void copySourceFiles( Project legacyProject, IProject iproject ) {
        IPath srcFolderPath, resFolderPath, localeFolderPath;
        File srcFile, headerFile;

        WorkspaceFile wsFile;

        int numSrcFiles = legacyProject.getNumFiles();
        boolean isAbsolute;
        String fileName;

        File[] exportedLibs = null, libsBuffer = null;
        IPath fileFolderPath, relpath = null;
        srcFolderPath = new Path( POTENTIAL_SOURCE_FOLDERS[ PROJECT_SRC_FOLDE_INDEX ] );
        resFolderPath = new Path( POTENTIAL_SOURCE_FOLDERS[ PROJECT_RES_FOLDE_INDEX ] );
        localeFolderPath = new Path( POTENTIAL_SOURCE_FOLDERS[ PROJECT_LOCALE_FOLDE_INDEX ] );

        for( int i = 0; i < numSrcFiles; i++ ) {
            fileFolderPath = srcFolderPath;
            wsFile = legacyProject.getSourceFile( i );
            srcFile = new File( OSUtils.replaceFileSeperator( wsFile.getFile().getAbsolutePath() ) );// Mac support

            fileName = srcFile.getName();

            if( fileName.endsWith( ".jar" ) )/**
             * Legacy model hack when an exported lib resides in the Files[...] section.
             */
            {
                _log.debug( "Project [" + legacyProject.getDisplayName() + "] has an exported library [" + fileName + "]." );

                if( null == exportedLibs ) {
                    exportedLibs = new File[ 1 ];
                    exportedLibs[ 0 ] = srcFile;
                } else {
                    libsBuffer = new File[ exportedLibs.length + 1 ];
                    System.arraycopy( exportedLibs, 0, libsBuffer, 0, exportedLibs.length );
                    libsBuffer[ exportedLibs.length ] = srcFile;
                    exportedLibs = libsBuffer;
                }
                continue;
            }

            isAbsolute = false;
            // copy java and language files to their package declaration
            // location
            if( srcFile.exists() ) {
                if( wsFile.getIsJava() ) {
                    // handle java files
                    fileFolderPath = getPackagePath( srcFolderPath, srcFile );
                } else if( wsFile.getIsResourceHeader() ) {
                    // handle rrh files
                    fileFolderPath = getPackagePath( localeFolderPath, srcFile );
                } else if( wsFile.getIsLanguageResource() ) {
                    // handle rrc files
                    headerFile = getResourceHeader( wsFile );

                    if( headerFile == null ) {
                        String err = "Can not find resource header file for "
                                + OSUtils.replaceFileSeperator( wsFile.getFile().getAbsolutePath() );
                        _log.warn( err );
                        continue;
                    }

                    fileFolderPath = getPackagePath( localeFolderPath, headerFile ).removeLastSegments( 1 );
                    fileFolderPath = fileFolderPath.append( srcFile.getName() );
                } else {
                    // handle other resource files which do not contain package information
                    if( isLinked( iproject, wsFile ) )
                        continue;

                    relpath = new Path( OSUtils.replaceFileSeperator( wsFile.toString() ) );

                    isAbsolute = relpath.isAbsolute();

                    if( !isAbsolute && 0 < relpath.segmentCount() ) {
                        try {
                            relpath = new Path( PackageUtils.getFilePackageString( srcFile, legacyProject ) ).append( relpath
                                    .lastSegment() );
                        } catch( CoreException e ) {
                            _log.error( e.getMessage() );
                            relpath = new Path( IConstants.EMPTY_STRING );
                        }
                    }

                    // put key file in the source folder
                    if( wsFile.getIsKey() ) {
                        fileFolderPath = srcFolderPath.append( relpath );
                    } else {
                        if( StringUtils.isNotEmpty( POTENTIAL_SOURCE_FOLDERS[ PROJECT_RES_FOLDE_INDEX ] ) && !isAbsolute ) {
                            fileFolderPath = resFolderPath.append( relpath );
                        } else {
                            fileFolderPath = relpath;
                        }
                    }
                }
            } else {
                String msg = "Can't parse undefined file or directory [" + srcFile.getAbsolutePath() + "]";
                _log.error( msg );
            }

            if( !isAbsolute ) {
                if( iproject.isOpen() )
                    createFolders( iproject.getLocation().toOSString(), fileFolderPath.removeLastSegments( 1 ) );

                copyFile( iproject, srcFile, fileFolderPath );
            }

            if( ( wsFile.getIsResourceHeader() || wsFile.getIsLanguageResource() ) && !isLinked( iproject, wsFile ) )
                // need to copy all associated rrc and rrh files, some of which
                // may not be explicitly included in jdp
                copyLocaleFiles( iproject, legacyProject, srcFile, fileFolderPath.removeLastSegments( 1 ) );
        }
    }

    static private IPath getPackagePath( IPath newFilePath, File srcFile ) {
        String packageId = IConstants.EMPTY_STRING;
        ;
        try {
            packageId = PackageUtils.getFilePackageString( srcFile, null );
        } catch( CoreException e ) {
            _log.error( e.getMessage() );
        }
        packageId = PackageUtils.convertPkgStringToID( packageId );
        if( !StringUtils.isBlank( packageId ) ) {
            packageId = packageId.replace( '.', IPath.SEPARATOR );
            newFilePath = newFilePath.append( packageId );
        }
        newFilePath = newFilePath.append( srcFile.getName() );
        return newFilePath;
    }

    static public boolean isLinked( IProject iProject, WorkspaceFile workspaceFile ) {
        String projectName = iProject.getName();
        String fileName = OSUtils.replaceFileSeperator( workspaceFile.getFile().getAbsolutePath() );

        IFile file = getProjectBasedFileFromOSBasedFile( projectName, fileName );// Mac support

        if( file == null ) // if the files is not found for the project, it is
            // not linked
            return false;

        return file.isLinked();
    }

    /***
     * Find file/linked file under a Eclipse project.
     *
     * @param projectName
     *            the name of project. If null, don't care which project this file belongs to
     * @param osFilePathStr
     *            the file path string
     * @return corresponding IFile object, or null if such file is not under any Eclipse project.
     */
    public static IFile getProjectBasedFileFromOSBasedFile( String projectName, String osFilePathStr ) {
        if( StringUtils.isEmpty( projectName ) ) {
            return getProjectBasedFileFromOSBasedFile( osFilePathStr );
        }

        IFile result = null;
        IPath location = Path.fromOSString( osFilePathStr );
        IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation( location );

        for( int i = 0; i < files.length; i++ ) {
            String currentProjectName = files[ i ].getProject().getName();
            if( currentProjectName.equals( projectName ) ) {
                result = files[ i ];
                break;
            }
        }
        return result;
    }

    /***
     * Find file/linked file under a Eclipse workspace.
     *
     * @param osFilePathStr
     *            the file path string
     * @return corresponding IFile object, or null if such file is not under any Eclipse project.
     */
    public static IFile getProjectBasedFileFromOSBasedFile( String osFilePathStr ) {
        IFile result = null;
        IPath location = Path.fromOSString( osFilePathStr );
        IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation( location );

        if( files.length > 0 ) {
            for( int i = 0; i < files.length; i++ ) {
                if( files[ i ].getName().equals( location.lastSegment() ) ) {
                    result = files[ i ];
                    break;
                }
            }
            if( result == null ) {
                result = files[ 0 ]; // retrieve the first found file
            }

        }
        return result;
    }

    static public File getResourceHeader( WorkspaceFile languageFile ) {
        File[] resourceFiles = getResourceFiles( languageFile.getFile(), languageFile.getFile().getName() );
        if( resourceFiles != null ) {
            for( File file : resourceFiles )
                if( file.getName().endsWith( IArtifacts.ILegacy.IDs.rrh.name() ) )
                    return file;
        }
        return null;
    }

    static public File[] getResourceFiles( final File origFile, String fileName ) {
        final File srcDirectory = origFile.getParentFile();
        final String fileNamePattern = getResourceFileNamePattern( fileName );

        File[] resourceFiles = srcDirectory.listFiles( new FilenameFilter() {
            public boolean accept( File dir, String name ) {
                return !name.equals( origFile.getName() )
                        && name.startsWith( fileNamePattern )
                        && ( name.endsWith( IArtifacts.ILegacy.IDs.rrc.name() ) || name.endsWith( IArtifacts.ILegacy.IDs.rrh
                                .name() ) );
            }
        } );
        return resourceFiles;
    }

    static public String getResourceFileNamePattern( String fileName ) {
        int index = fileName.indexOf( '_' );
        if( index == -1 )
            return removeExtension( fileName );
        else
            return removeExtension( fileName.substring( 0, index ) );
    }

    static public String removeExtension( String fileName ) {
        int index = fileName.lastIndexOf( '.' );
        if( index == -1 )
            return fileName;
        else
            return fileName.substring( 0, index );
    }

    /*
     * Copies all rrc files associated with given rrh file to destFilePath
     *
     * @param iproject
     *
     * @param rrhFile
     *
     * @param destPath
     */
    static public void copyLocaleFiles( IProject iproject, Project legacyProject, File origFile, IPath destPath ) {
        String fileName = origFile.getName();

        File[] resourceFiles = getResourceFiles( origFile, fileName );

        if( resourceFiles != null ) {
            for( int i = 0; i < resourceFiles.length; i++ )
                createFolders( iproject.getLocation().toOSString(), destPath );

            for( File resourceFile : resourceFiles )
                copyFile( iproject, resourceFile, destPath.append( resourceFile.getName() ) );
        }
    }

    static public void copyFile( IProject iproject, File srcFile, IPath newFilePath ) {
        if( null == srcFile || !srcFile.exists() || srcFile.isDirectory() ) {
            String msg = null == srcFile ? "" : srcFile.getPath();
            _log.error( "Can't copy undefined file or directory [" + msg + "]" );
            return;
        }

        if( null == iproject || !iproject.exists() ) {
            String msg = null == iproject ? "" : iproject.getName();
            _log.error( "Can't copy file [" + srcFile.getPath() + "] to undefined project [" + msg + "]" );
            return;
        }

        if( null == newFilePath || newFilePath.isEmpty() ) {
            String msg = null == newFilePath ? "" : newFilePath.toOSString();
            _log.error( "Can't copy the file [" + srcFile.getPath() + "] to undefined path [" + msg + "]" );
            return;
        }
        IFile iFile = iproject.getFile( newFilePath );
        // if the file is there, do not copy
        if( iFile.exists() ) {
            return;
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream( srcFile );
            IPath parent = newFilePath.removeLastSegments( 1 );
            if( !parent.isEmpty() ) {
                IFolder parentFolder = iproject.getFolder( parent );
                if( !parentFolder.exists() ) {
                    ImportUtils.createFolders( iproject, parent, IResource.FORCE );
                }
            }
            iFile.create( inputStream, IResource.FORCE, new NullProgressMonitor() );
            iFile.refreshLocal( IResource.DEPTH_INFINITE, new NullProgressMonitor() );
        } catch( Throwable e ) {
            _log.error( e.getMessage(), e );
        } finally {// just in case the Eclipse API doesn't close it for whatever
            // reason.
            try {
                if( null != inputStream ) {
                    inputStream.close();
                }
            } catch( Throwable e ) {
                ;
                _log.error( e.getMessage(), e );
            }
        }
    }

    static public void createFolders( String projectLocation, IPath newFilePath ) {
        if( StringUtils.isBlank( projectLocation ) )
            return;

        File projectDirectory = new File( projectLocation ), newDirectory;

        if( projectDirectory.exists() && projectDirectory.isDirectory() ) {
            newDirectory = new File( projectDirectory.getAbsolutePath() + File.separator + newFilePath.toOSString() );

            if( !newDirectory.exists() )
                try {
                    newDirectory.mkdirs();
                } catch( Throwable e ) {
                    _log.error( e.getMessage(), e );
                }
        }
    }

    static public IClasspathEntry createSourceEntry( IContainer sourceContainer ) {
        IClasspathEntry entry = JavaCore.newSourceEntry( sourceContainer.getFullPath() );

        return entry;
    }

    public static void createFolders( IProject project, IPath newFilePath, int updateFlags ) throws CoreException {
        int numSegments = newFilePath.segmentCount();
        for( int i = numSegments - 1; i >= 0; i-- ) {
            IFolder folder = project.getFolder( newFilePath.removeLastSegments( i ) );
            if( project.isOpen() && !folder.exists() ) {
                folder.create( updateFlags, true, new NullProgressMonitor() );
            }
        }
    }

    public static void createFolders( IFolder parentFolder, IPath newFilePath, int updateFlags ) throws CoreException {
        int numSegments = newFilePath.segmentCount();
        for( int i = numSegments - 1; i >= 0; i-- ) {
            IFolder folder = parentFolder.getFolder( newFilePath.removeLastSegments( i ) );
            if( !folder.exists() ) {
                folder.create( updateFlags, true, new NullProgressMonitor() );
            }
        }
    }

    public static IFolder createFolder( IFolder parent, IPath newFolder, int updateFlags ) throws CoreException {
        if( newFolder == null || newFolder.toOSString().equals( IConstants.EMPTY_STRING ) ) {
            return null;
        }
        IFolder folder = parent.getFolder( newFolder );
        if( !folder.exists() ) {
            folder.create( updateFlags, true, new NullProgressMonitor() );
        }
        return folder;
    }

    /**
     * Get the preferences.ini key value
     *
     * @param key
     * @return
     */
    public static String getImportPref( String key ) {
        return ContextManager.getDefault().getPreferenceStore().getString( key );
    }

    /**
     * Get the prefereces.ini setting for res
     *
     * @return
     */
    public static String getProjectResFolderName() {
        return getImportPref( LegacyImportHelper.PROJECT_RES_FOLDER_NAME_KEY );
    }

    /**
     * Get the preferences.ini key value as an int
     *
     * @param key
     * @return
     */
    public static int getIntImportPref( String key ) {
        int npref = 0;
        try {
            npref = Integer.parseInt( getImportPref( key ) );
        } catch( NumberFormatException e ) {
            _log.warn( key + " preference is not initialized" );
        }
        return npref;
    }

    /**
     * Get the set of output paths of the given <code>IProject</code>.
     *
     * @param project
     * @return
     * @throws JavaModelException
     */
    static public Set< IPath > getOutputPathSet( IProject project ) throws JavaModelException {
        return getOutputPathSet( JavaCore.create( project ) );
    }

    /**
     * Get the set of output paths of the given <code>IJavaProject</code>.
     *
     * @param javaProject
     * @return
     * @throws JavaModelException
     */
    static public Set< IPath > getOutputPathSet( IJavaProject javaProject ) {
        HashSet< IPath > outputPathSet = new HashSet< IPath >();

        try {
            // get the output folder path of the project
            IPath outputFolderPath = javaProject.getOutputLocation();
            if( outputFolderPath != null ) {
                outputPathSet.add( outputFolderPath );
            }

            IClasspathEntry[] _classPathEntries = javaProject.getRawClasspath();

            IClasspathEntry entry;

            for( int i = 0; i < _classPathEntries.length; i++ ) {
                entry = _classPathEntries[ i ];
                if( IClasspathEntry.CPE_SOURCE == entry.getEntryKind() ) {
                    // get the output folder of the entry
                    outputFolderPath = entry.getOutputLocation();
                    if( outputFolderPath != null ) {
                        outputPathSet.add( outputFolderPath );
                    }
                }
            }
        } catch( JavaModelException e ) {
            _log.debug( e.getMessage(), e );
        }

        return outputPathSet;
    }

    /**
     * Get the set of output Files of the given <code>IJavaProject</code>.
     *
     * @param javaProject
     * @return
     */
    static public Set< File > getOutputFolderSet( IJavaProject javaProject ) {
        Set< File > outputFolderFiles = new HashSet< File >();
        Set< IPath > outputFolderSet = getOutputPathSet( javaProject );
        IPath projectRelativePath;
        for( IPath path : outputFolderSet ) {
            // get rid of the first project segment
            projectRelativePath = path.removeFirstSegments( 1 ).makeRelative();
            outputFolderFiles.add( javaProject.getProject().getLocation().append( projectRelativePath ).toFile() );
        }
        return outputFolderFiles;
    }

    /**
     * Check if the given <code>resource</code> is in one of the output folders of its project.
     *
     * @param resource
     * @return
     * @throws JavaModelException
     */
    static public boolean isInOutputFolder( IResource resource, Set< IPath > outputPathSet ) throws JavaModelException {
        for( Iterator< IPath > iterator = outputPathSet.iterator(); iterator.hasNext(); ) {
            IPath path = iterator.next();
            if( resource.getFullPath().matchingFirstSegments( path ) == path.segmentCount() )
                return true;
        }
        return false;
    }

    static public final class ResourcesBuffer {
        private final Map< String, Set< File >> _javaContainer;
        private final Map< String, Set< File >> _localeContainer;
        private final Set< File > _nonPackageContainer;

        public ResourcesBuffer( Map< String, Set< File >> javaContainer, Map< String, Set< File >> localeContainer,
                Set< File > nonPackageContainer ) {
            if( null == javaContainer )
                _javaContainer = Collections.emptyMap();
            else
                _javaContainer = javaContainer;

            if( null == javaContainer )
                _localeContainer = Collections.emptyMap();
            else
                _localeContainer = localeContainer;

            if( null == nonPackageContainer )
                _nonPackageContainer = Collections.emptySet();
            else
                _nonPackageContainer = nonPackageContainer;
        }

        public Map< String, Set< File >> getJavaContener() {
            return _javaContainer;
        }

        public Map< String, Set< File >> getlocaleContener() {
            return _localeContainer;
        }

        public Set< File > getNonPackageContener() {
            return _nonPackageContainer;
        }
    }

    static private class LinkBuffer {
        IFile _fileHandle;
        IPath _filePath;

        LinkBuffer( IFile fileHandle, IPath filePath ) {
            _fileHandle = fileHandle;
            _filePath = filePath;
        }

        void create() {
            try {
                _fileHandle.createLink( _filePath, IResource.BACKGROUND_REFRESH | IResource.REPLACE
                        | IResource.ALLOW_MISSING_LOCAL, new NullProgressMonitor() );
            } catch( CoreException e ) {
                _log.error( e.getMessage(), e );
            }
        }
    }

    static final class RRHFile {
        final String _fileName, _packageId;

        RRHFile( String fileName, String packageId ) {
            if( StringUtils.isEmpty( fileName ) )
                throw new IllegalArgumentException( "" );

            _fileName = fileName;

            if( StringUtils.isEmpty( packageId ) )
                _packageId = "";
            else
                _packageId = packageId;
        }

        @Override
        public String toString() {
            return "<" + RRHFile.class.getSimpleName() + "[_fileName=[" + _fileName + "], _packageId=[" + _packageId + "]>";
        }

        @Override
        public boolean equals( Object other ) {
            if( null == other )
                return false;

            if( RRHFile.class.equals( other.getClass() ) ) {
                RRHFile theOther = (RRHFile) other;

                return toString().equals( theOther.toString() );
            }

            return false;
        }

        @Override
        public int hashCode() {
            return ( _fileName + _packageId ).hashCode();
        }
    }

    /**
     * Applies the Java Path exclusion patterns to a given project and returns the list of filtered IClasspathEntry
     *
     * @param eclipseProject
     * @param legacyProject
     * @param originalClasspathEntries
     * @return
     */
    static public List< IClasspathEntry > applyExclusionPatterns( IProject eclipseProject, Project legacyProject,
            List< IClasspathEntry > originalClasspathEntries ) {
        if( null == eclipseProject ) {
            throw new IllegalArgumentException( "Can't process undefined Eclipse project!" );
        }

        if( null == legacyProject ) {
            throw new IllegalArgumentException( "Can't process undefined legacy project!" );
        }

        if( null == originalClasspathEntries ) {
            throw new IllegalArgumentException( "Can't process undefined Eclipse classpath entries!" );
        }

        // TODO: call this when importing projects, rather than from the
        // Compilation Participant
        List< WorkspaceFile > excludedWorkspaceFiles = getFilesToBeExcluded( legacyProject );

        if( excludedWorkspaceFiles.isEmpty() && originalClasspathEntries.isEmpty() ) {
            return originalClasspathEntries;
        }

        List< IClasspathEntry > excludedClasspathEntries = new ArrayList< IClasspathEntry >();
        HashMap< IPath, IClasspathEntry > filterMap = new HashMap< IPath, IClasspathEntry >();
        String projectNamePattern = IPath.SEPARATOR + eclipseProject.getName() + IPath.SEPARATOR;
        List< IPath > exclusionPatterns;
        IPath classpathEntryPath, exclusionPatternPath;
        boolean forProject;
        String lastSegment;
        IFolder folder;
        IPath srcLocation;
        IClasspathEntry newEntry;
        IPath[] excludedPaths;
        File file;
        String workspaceFilePath;
        String packageId;

        for( IClasspathEntry entry : originalClasspathEntries ) {
            exclusionPatterns = new ArrayList< IPath >();

            classpathEntryPath = entry.getPath();

            if( IClasspathEntry.CPE_SOURCE == entry.getEntryKind() ) {
                lastSegment = classpathEntryPath.lastSegment();

                if( lastSegment.equalsIgnoreCase( ImportUtils.getImportPref( ResourceBuilder.LOCALE_INTERFACES_FOLDER_NAME ) ) ) {
                    continue;
                }

                folder = eclipseProject.getFolder( lastSegment );

                if( folder.isDerived() || !folder.exists() ) {
                    continue;
                }

                forProject = classpathEntryPath.toString().startsWith( projectNamePattern );

                if( forProject ) {
                    srcLocation = folder.getLocation();

                    if( srcLocation == null || srcLocation.isEmpty() ) {
                        return originalClasspathEntries;
                    }

                    for( WorkspaceFile workspaceFile : excludedWorkspaceFiles ) {
                        workspaceFilePath = workspaceFile.toString();
                        file = workspaceFile.getFile();

                        if( null != file && file.exists() && file.isFile() ) {
                            // Fix for IDT 149988 - Check type of source folder and file type to prevent duplication for exclusion
                            // patterns
                            if( lastSegment.equalsIgnoreCase( ImportUtils
                                    .getImportPref( LegacyImportHelper.PROJECT_SRC_FOLDER_NAME_KEY ) ) ) {
                                if( !workspaceFile.getIsJava() ) {
                                    continue;
                                }
                            } else {
                                if( workspaceFile.getIsJava() ) {
                                    continue;
                                }
                            }

                            if( workspaceFile.getIsJava() || workspaceFile.getIsResourceHeader() || workspaceFile.getIsResource() ) {
                                packageId = IConstants.EMPTY_STRING;
                                try {
                                    packageId = PackageUtils.getFilePackageString( file, legacyProject );
                                } catch( CoreException e ) {
                                    _log.error( e.getMessage() );
                                    packageId = IConstants.EMPTY_STRING;
                                }
                                workspaceFilePath = File.separator + packageId + File.separator + workspaceFilePath;
                            }
                            exclusionPatternPath = getExclusionPattern( workspaceFile, lastSegment, eclipseProject, legacyProject );

                            if( !exclusionPatternPath.isEmpty() ) {
                                exclusionPatterns.add( exclusionPatternPath );
                            }
                        }
                    }
                }

                if( exclusionPatterns.isEmpty() ) {
                    excludedPaths = new IPath[] {};
                } else {
                    excludedPaths = exclusionPatterns.toArray( new IPath[ exclusionPatterns.size() ] );
                }

                newEntry = JavaCore.newSourceEntry( classpathEntryPath, entry.getInclusionPatterns(), excludedPaths,
                        entry.getOutputLocation(), entry.getExtraAttributes() );
                filterMap.put( classpathEntryPath, newEntry );
            } else {// IClasspathEntry of type other than CPE_SOURCE
                filterMap.put( classpathEntryPath, entry );
            }
        }

        IPath elementPath;

        for( IClasspathEntry element : originalClasspathEntries ) {
            elementPath = element.getPath();
            newEntry = filterMap.get( elementPath );
            if( null != newEntry ) {
                excludedClasspathEntries.add( newEntry );
            }
        }

        return excludedClasspathEntries;
    }

    static private IPath getExclusionPattern( WorkspaceFile workspaceFile, String lastSegment, IProject project,
            Project rimProject ) {
        String file_str = IConstants.EMPTY_STRING;
        String pkg_name = IConstants.EMPTY_STRING;
        file_str = workspaceFile.toString();

        File file;

        if( workspaceFile.getIsJava() || workspaceFile.getIsResourceHeader() ) {
            try {
                pkg_name = PackageUtils.getFilePackageString( workspaceFile.getFile(), rimProject );
            } catch( CoreException e ) {
                _log.error( e.getMessage() );
            }
            if( StringUtils.isBlank( pkg_name ) ) {
                file_str = workspaceFile.getFile().getName();
            } else {
                file_str = pkg_name + IConstants.FORWARD_SLASH_MARK + workspaceFile.getFile().getName();
            }

            file_str = trimFirstSlash( file_str );
        } else if( workspaceFile.getIsLanguageResource() ) {// handle rrc files
            String fileName = workspaceFile.getFile().getName();
            String rrhFileName = IConstants.EMPTY_STRING;
            String rrh_path = IConstants.EMPTY_STRING;

            if( fileName.contains( "_" ) ) {
                /*
                 * eg : - Theme_en. rrc
                 */
                rrhFileName = fileName.substring( 0, fileName.indexOf( '_' ) );
            } else {
                /*
                 * eg : - Theme . rrc
                 */
                rrhFileName = fileName.substring( 0, fileName.indexOf( '.' ) );
            }

            rrhFileName = rrhFileName + "." + IConstants.RRH_FILE_EXTENSION;
            /*
             * eg : - Theme . rrh
             */
            rrh_path = workspaceFile.getFile().getAbsolutePath();
            rrh_path = rrh_path.replace( fileName, rrhFileName );

            if( rrh_path != null ) {
                file = new File( rrh_path );

                if( file.exists() && file.isFile() ) {
                    try {
                        pkg_name = PackageUtils.getFilePackageString( file, rimProject );
                    } catch( CoreException e ) {
                        _log.error( e.getMessage() );
                    }
                }
            }
            if( StringUtils.isBlank( pkg_name ) ) {
                file_str = workspaceFile.getFile().getName();
            } else {
                file_str = pkg_name + IConstants.FORWARD_SLASH_MARK + workspaceFile.getFile().getName();
            }
            file_str = trimFirstSlash( file_str ); // eg:- com/rim/test/A.java
        } else {
            IPath bbwkspath = ( new Path( rimProject.getWorkspace().getFile().toString() ) ).removeLastSegments( 1 );
            IPath bbprjpath = ( new Path( rimProject.getFile().toString() ) ).removeLastSegments( 1 );
            IPath resolvedFilePath = PackageUtils.resolvePathForFile( new Path( workspaceFile.getFile().getAbsolutePath() ),
                    bbprjpath, bbwkspath );

            List< String > sources = getSources( rimProject );

            String firstSegment = resolvedFilePath.segment( 0 );

            if( sources.contains( firstSegment ) ) {
                resolvedFilePath = resolvedFilePath.removeFirstSegments( 1 );
            }

            return resolvedFilePath;
        }

        return new Path( file_str );
    }

    static private String trimFirstSlash( String file_str ) {
        if( ( file_str.startsWith( IConstants.PATH_SEPARATE_MARK ) ) || ( file_str.startsWith( IConstants.FORWARD_SLASH_MARK ) ) ) {
            file_str = file_str.substring( 1 );
        }
        return file_str;
    }

    static public List< WorkspaceFile > getFilesToBeExcluded( Project rimProject ) {
        List< WorkspaceFile > files = new ArrayList< WorkspaceFile >();

        WorkspaceFile f;

        for( int i = 0; i < rimProject.getNumFiles(); i++ ) {
            f = rimProject.getSourceFile( i );

            if( f.getDontBuild() )
                files.add( f );
        }

        return files;
    }

    /**
     * Returns a <code>List</code> of [UserData] sources
     *
     * @param proj
     * @return
     */
    static public List< String > getSources( Project proj ) {
        List< String > sources = new ArrayList< String >();
        String udata = proj.getUserData();

        StringTokenizer st = new StringTokenizer( udata, "|" );
        String token;

        while( st.hasMoreElements() ) {
            token = st.nextToken();
            if( StringUtils.isNotBlank( token ) ) {
                sources.add( token );
            }
        }

        return sources;
    }

    /**
     * Returns a boolean when ppList already contains the passed tag otherwise returns false
     *
     * @param ArrayList
     *            < PreprocessorTag > ppList
     * @param String
     *            tag
     * @return boolean
     */
    static public boolean isPPtagsExists( ArrayList< PreprocessorTag > ppList, String tag ) {
        for( PreprocessorTag ppTag : ppList ) {
            if( ppTag.getPreprocessorDefine().equals( tag ) ) {
                return true;
            }
        }
        return false;
    }

    static public boolean isValidPPtag( String tag ) {
        return validPPTagPattern.matcher( tag ).matches();
    }
}
