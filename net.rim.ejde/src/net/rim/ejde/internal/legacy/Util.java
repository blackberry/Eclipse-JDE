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
package net.rim.ejde.internal.legacy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.IModelConstants;
import net.rim.ejde.internal.util.FileUtils;
import net.rim.ide.Project;
import net.rim.ide.Workspace;
import net.rim.ide.core.IDEError;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public final class Util implements IModelConstants.IArtifacts {
    static private final Logger log = Logger.getLogger( Util.class );

    private Util() {
    }

    static public Workspace getDefaultLegacyWorkspace() {
        Workspace workspace = null;

        File file = ILegacy.Workspace.getMetaFile();

        try {
            if( !file.exists() ) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            workspace = new Workspace( file );

            save( workspace, true );
        } catch( Throwable t ) {
            log.error( t.getMessage(), t );
        }

        return workspace;
    }

    static public IStatus save( Workspace workspace, boolean prompt ) {
        if( workspace == null ) {
            log.error( "Workspace could not be saved. NULL workspace." );
            return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, 1, "", null );
        }
        if( !workspace.getDirty() ) {
            log.warn( "Tried to save an unchanged workspace" );
            return Status.OK_STATUS;
        }

        File jdwFile = workspace.getFile();

        if( jdwFile == null || !jdwFile.exists() ) {
            log.error( "Workspace could not be saved. JDW file does not exist or is null" );
            return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, 1, "", null );
        }

        boolean canChange = jdwFile.canWrite();

        if( !canChange && prompt ) {

            IStatus lStatus = FileUtils.canChange( jdwFile );
            if( lStatus.isOK() ) {
                try {
                    workspace.save();
                } catch( IDEError e ) {
                    log.error( "Workspace could not be saved.", e );
                    return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, 1, "", e );
                }
            } else {
                log.error( "Workspace could not be saved. User disallowed operation." );
                return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, 1, "", null );
            }
        } else {
            try {
                workspace.save();
            } catch( IDEError e ) {
                log.error( "Workspace could not be saved.", e );
                return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, 1, "", e );
            }
        }

        return Status.OK_STATUS;
    }

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

    public Project getLegacyProject( Workspace legacyWorkspace, BlackBerryProject iproject ) {
        return getLegacyProject( legacyWorkspace, iproject.getProject() );
    }

    public Project getLegacyProject( Workspace legacyWorkspace, IProject eclipseProject ) {
        return getLegacyProject( legacyWorkspace, eclipseProject.getName() );
    }

    public Project getLegacyProject( Workspace legacyWorkspace, String eclipseProjectName ) {
        if( null == legacyWorkspace ) {
            return null;
        }

        if( StringUtils.isBlank( eclipseProjectName ) ) {
            return null;
        }

        int count = legacyWorkspace.getNumProjects();

        Project project = null;

        for( int i = 0; i < count; i++ ) {
            project = legacyWorkspace.getProject( i );

            if( null != project && eclipseProjectName.equals( project.getDisplayName() ) ) {
                return project;
            }
        }

        return null;
    }

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
}
