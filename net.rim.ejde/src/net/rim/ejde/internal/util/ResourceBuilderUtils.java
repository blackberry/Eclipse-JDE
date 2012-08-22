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

import net.rim.ejde.internal.builders.ResourceBuilder;
import net.rim.ejde.internal.validation.ValidationManager;
import net.rim.ide.Project;
import net.rim.ide.WorkspaceFile;

import org.apache.log4j.Logger;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.widgets.Display;

public class ResourceBuilderUtils {
    static private final Logger _log = Logger.getLogger( ResourceBuilderUtils.class );

    /**
     * Check if the given <code>file</code> needs to be built.
     *
     * @param file
     * @param project
     * @return
     */
    public static boolean needBuild( File file, Project project ) {
        if( ( file == null ) || ( project == null ) )
            return false;
        for( int i = 0; i < project.getNumFiles(); i++ ) {
            WorkspaceFile workspaceFile = project.getSourceFile( i );
            if( workspaceFile.getFile().equals( file ) )
                return !workspaceFile.getDontBuild();
        }
        return false;
    }

    /**
     * Check if the give <code>resource</code> is in the <b>.tmp</b> folder.
     *
     * @param resource
     * @return
     */
    public static boolean isInTmpFolder( IResource resource ) {
        IPath resourcePath = resource.getProjectRelativePath();
        IPath firstSegment = resourcePath.removeLastSegments( resourcePath.segmentCount() - 1 );
        String firstSegmentString = firstSegment.toString();
        if( firstSegmentString.equals( ImportUtils.getImportPref( ResourceBuilder.LOCALE_INTERFACES_FOLDER_NAME ) ) ) {
            return true;
        }
        return false;
    }

    /**
     * Creates problem marker on the given <code>resource</code>.
     *
     * @param resource
     * @param type
     * @param message
     * @param lineNumber
     * @param severity
     * @throws CoreException
     */
    static public void createProblemMarker( final IResource resource, final String type, final String message,
            final int lineNumber, final int severity ) throws CoreException {
        if( resource.getWorkspace().isTreeLocked() ) {
            Display.getDefault().asyncExec( new Runnable() {
                public void run() {
                    try {
                        internalCreateProblemMarker( resource, type, message, lineNumber, severity );
                    } catch( CoreException e ) {
                        _log.error( "createProblemMarkers: ", e );
                    }
                }
            } );
        } else {
            internalCreateProblemMarker( resource, type, message, lineNumber, severity );
        }
    }

    static private void internalCreateProblemMarker( IResource resource, String type, String message, int lineNumber, int severity )
            throws CoreException {
        IMarker marker = resource.createMarker( type );
        marker.setAttribute( IMarker.MESSAGE, message );
        marker.setAttribute( IMarker.SEVERITY, severity );
        if( lineNumber > 0 ) {
            marker.setAttribute( IMarker.LINE_NUMBER, lineNumber );
        }
    }

    /**
     * Removes the given <code>type</code> of markers from the given <code>resource</code>.
     *
     * @param resource
     * @param types
     * @param depth
     * @throws CoreException
     */
    static public void cleanProblemMarkers( final IResource resource, final String[] types, final int depth )
            throws CoreException {
        if( resource == null || !resource.exists() ) {
            return;
        }
        if( resource.getWorkspace().isTreeLocked() ) {
            Display.getDefault().asyncExec( new Runnable() {
                public void run() {
                    try {
                        internalCleanProblemMarkers( resource, types, depth );
                    } catch( CoreException e ) {
                        _log.error( "cleanProblemMarkers:", e );
                    }
                }
            } );
        } else {
            internalCleanProblemMarkers( resource, types, depth );
        }
    }

    static private void internalCleanProblemMarkers( IResource resource, String[] types, int depth ) throws CoreException {
        if( resource != null ) {
            if( resource.getProject() != null && !resource.getProject().isOpen() ) {
                return;
            }
            IMarker[] markers;
            for( int j = 0; j < types.length; j++ ) {
                markers = resource.findMarkers( types[ j ], true, depth );
                for( int i = 0; i < markers.length; i++ ) {
                    markers[ i ].delete();
                }
            }
            if( 0 == depth && resource instanceof IFile ) {
                // purge ValidationManager problems registry
                ValidationManager.getInstance().cleanObjectDiags( resource.getProject(), resource );
            }
        }
    }

    /**
     * Creates the resource output root folder if it is not there.
     *
     * @param project
     * @param monitor
     * @throws CoreException
     *
     */

    public static IFolder createResourcesOutputRoot( IProject project, IProgressMonitor monitor ) throws CoreException {

        IContainer sourceContainer = project
                .getFolder( ImportUtils.getImportPref( ResourceBuilder.LOCALE_INTERFACES_FOLDER_NAME ) );
        if( !sourceContainer.exists() ) {
            // if the folder does not exist, create it
            ( (IFolder) sourceContainer ).create( IResource.DERIVED, true, monitor );
        }
        IJavaProject javaProject = JavaCore.create( project );
        // check if folder is on classpath already (can occur if project is on classpath
        if( !javaProject.isOnClasspath( sourceContainer ) ) {
            IClasspathEntry tmpSourceRootEntry = JavaCore.newSourceEntry( sourceContainer.getFullPath() );
            // Set raw classpath
            IClasspathEntry[] classpathEntries = javaProject.getRawClasspath();
            IClasspathEntry[] newClasspathEntries = new IClasspathEntry[ classpathEntries.length + 1 ];
            System.arraycopy( classpathEntries, 0, newClasspathEntries, 0, classpathEntries.length );
            newClasspathEntries[ classpathEntries.length ] = tmpSourceRootEntry;
            javaProject.setRawClasspath( newClasspathEntries, monitor );
        }
        return (IFolder) sourceContainer;
    }

    /**
     * Get the actual file represented by the given <code>resource</code>.
     *
     * @param resource
     * @return
     * @throws CoreException
     */
    static public File getFile( IResource resource ) throws CoreException {
        IFileStore store = EFS.getStore( resource.getLocationURI() );

        return store.toLocalFile( EFS.NONE, null );
    }
}
