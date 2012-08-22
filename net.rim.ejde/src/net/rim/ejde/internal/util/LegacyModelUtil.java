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
import net.rim.ejde.internal.core.IConstants;
import net.rim.ide.Project;
import net.rim.ide.Workspace;
import net.rim.ide.core.IDEError;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

/**
 * @author cmalinescu, mcacenco
 *
 */
public final class LegacyModelUtil {
    static private final Logger log = Logger.getLogger( LegacyModelUtil.class );
    static public final String BB_VIRTUAL_PROJECT_USERDATA = "VIRTUAL_PROJECT";
    static public final String BLANK_STRING = " ";
    static public final String EMPTY_STRING = "";
    static public final char DELIM_SECTION = '|';
    static public final char DELIM_ITEM = ';';

    /**
     *
     */
    private LegacyModelUtil() {
    }

    /**
     * Ads a project to the workspace preventing duplicates
     *
     * @param wksp
     * @param proj
     * @throws IDEError
     */
    public static void addProjectToWorkspaceNonDup( Workspace wksp, Project proj ) {
        int np = wksp.getNumProjects();
        Project oldproj;
        File pfile = proj.getFile();
        boolean isact;

        for( int i = 0; i < np; i++ ) {
            oldproj = wksp.getProject( i );

            if( oldproj.getFile().equals( pfile ) ) {
                if( proj != oldproj ) {
                    isact = wksp.isActiveProject( oldproj );
                    wksp.removeProject( oldproj );

                    try {
                        wksp.addProject( proj );
                    } catch( IDEError e ) {
                        log.error( e.getMessage(), e );
                        return;
                    }

                    if( isact )
                        wksp.setActiveProject( proj, "Release" );

                    return;
                }

                return;
            }
        }

        try {
            wksp.addProject( proj );
        } catch( IDEError e ) {
            log.error( e.getMessage(), e );
        }
    }

    static public void setSource( Project proj, IJavaProject eclipseJavaProject, String source ) {
        if( null == proj )// Don't process for a non existing legacy project
            return;

        if( StringUtils.isBlank( source ) )// Don't process for a non existing
            // source folder
            return;

        if( null == eclipseJavaProject )// Don't process for a non existing
            // Eclipse equivalent
            return;

        try {
            IClasspathEntry[] classpathEntries = eclipseJavaProject.getRawClasspath();

            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceRoot workspaceRoot = workspace.getRoot();

            IPath classpathEntryPath;
            String classpathEntryLastSegment;
            IFolder folder;

            for( IClasspathEntry classpathEntry : classpathEntries ) {
                if( IClasspathEntry.CPE_SOURCE == classpathEntry.getEntryKind() ) {
                    classpathEntryPath = classpathEntry.getPath();
                    classpathEntryLastSegment = classpathEntryPath.lastSegment();

                    if( source.equalsIgnoreCase( classpathEntryLastSegment ) ) {// if
                        // the
                        // string
                        // can't
                        // be
                        // matched
                        // to
                        // an
                        // existing
                        // classpath
                        // entry
                        // why
                        // should
                        // we
                        // add
                        // it
                        // to
                        // the
                        // legacy
                        // metadata?!
                        if( ImportUtils.getImportPref( ResourceBuilder.LOCALE_INTERFACES_FOLDER_NAME ).equalsIgnoreCase(
                                classpathEntryLastSegment ) ) {
                            return;
                        }
                        if( !classpathEntryPath.toOSString().equals( IConstants.EMPTY_STRING ) ) {

                            folder = workspaceRoot.getFolder( classpathEntryPath );

                            if( folder.isDerived() )// Don't process for
                                // Eclipse
                                // derived directories
                                return;
                        }

                    }
                }
            }
        } catch( JavaModelException e ) {
            log.error( e.getMessage(), e );
        }

        String udata = proj.getUserData();

        if( StringUtils.isNotBlank( udata ) ) {
            int idx1 = udata.indexOf( DELIM_SECTION );
            if( idx1 >= 0 ) {
                int idx2 = udata.indexOf( DELIM_SECTION, idx1 + 1 );
                String udata_new = ( idx1 > 0 ? udata.substring( 0, idx1 ) : EMPTY_STRING ) + DELIM_SECTION + source
                        + ( idx2 > idx1 ? udata.substring( idx2 ) : EMPTY_STRING );
                if( !udata.equals( udata_new ) ) {
                    proj.setUserData( udata_new );
                }
            }
        } else {
            proj.setUserData( DELIM_SECTION + source );
        }
    }

    static public void syncSources( Project proj, IJavaProject eclipseJavaProject ) {
        if( null == proj )// Don't process for a non existing legacy project
            return;

        if( null == eclipseJavaProject )// Don't process for a non existing
            // Eclipse equivalent
            return;

        String sources = "";
        StringBuffer buf = new StringBuffer();

        try {
            IClasspathEntry[] classpathEntries = eclipseJavaProject.getRawClasspath();

            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceRoot workspaceRoot = workspace.getRoot();

            IPath classpathEntryPath;
            String classpathEntryLastSegment;
            IFolder folder;

            for( IClasspathEntry classpathEntry : classpathEntries ) {
                if( IClasspathEntry.CPE_SOURCE == classpathEntry.getEntryKind() ) {
                    classpathEntryPath = classpathEntry.getPath();
                    classpathEntryLastSegment = classpathEntryPath.lastSegment();

                    if( ImportUtils.getImportPref( ResourceBuilder.LOCALE_INTERFACES_FOLDER_NAME ).equalsIgnoreCase(
                            classpathEntryLastSegment ) ) {
                        continue;
                    }

                    if( classpathEntryPath.toOSString().equals( IConstants.EMPTY_STRING ) ) {
                        continue;
                    }

                    folder = workspaceRoot.getFolder( classpathEntryPath );

                    if( folder.isDerived() ) {// Don't process for Eclipse
                        // derived directories
                        continue;
                    }

                    buf.append( DELIM_SECTION + classpathEntryLastSegment );
                }
            }
        } catch( JavaModelException e ) {
            log.error( e.getMessage(), e );
        }

        sources = buf.toString();

        String udata = proj.getUserData();

        if( StringUtils.isNotBlank( udata ) ) {

            int idx1 = udata.indexOf( DELIM_SECTION );

            if( idx1 >= 0 ) {
                int idx2 = udata.indexOf( DELIM_SECTION, idx1 + 1 );
                String udata_new = ( idx1 > 0 ? udata.substring( 0, idx1 ) : EMPTY_STRING ) + DELIM_SECTION + sources
                        + ( idx2 > idx1 ? udata.substring( idx2 ) : EMPTY_STRING );
                if( !udata.equals( udata_new ) ) {
                    proj.setUserData( udata_new );
                }
            }
        } else {
            proj.setUserData( sources );
        }
    }
}
