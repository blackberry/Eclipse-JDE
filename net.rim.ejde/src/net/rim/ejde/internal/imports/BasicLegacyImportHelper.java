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
package net.rim.ejde.internal.imports;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.PropertyChangeListenerImp;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.PreprocessorTag;
import net.rim.ejde.internal.model.preferences.PreprocessorPreferences;
import net.rim.ejde.internal.util.ImportUtils;
import net.rim.ejde.internal.util.InternalWorkspaceDependencyUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ejde.internal.util.WorkspaceDependencyUtils;
import net.rim.ide.Project;
import net.rim.ide.Workspace;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.osgi.util.NLS;

/**
 * This is the main class which imports a legacy BB workspace to the current eclipse workspace.
 */
public abstract class BasicLegacyImportHelper implements IWorkspaceRunnable {

    final public static String PROJECT_SRC_FOLDER_NAME_KEY = "project_src_folder";
    final public static String PROJECT_RES_FOLDER_NAME_KEY = "project_res_folder";
    final public static String PROJECT_IMPORT_LOCALE_FOLDER_NAME_KEY = "project_import_locale_file_folder";
    static private final Logger _log = Logger.getLogger( BasicLegacyImportHelper.class );

    /**
     * Link import type. The legacy workspace/projects will be imported using the link approach.
     */
    final public static int LINK_IMPORT = 0;

    /**
     * Copy import type. The legacy workspace/projects will be imported using the copy approach.
     */
    final public static int COPY_IMPORT = 1;

    protected Workspace _legacyWorkspace;
    protected Set< Project > _legacyProjects;
    protected int _importType = LINK_IMPORT;
    protected IPath _REPath;
    protected IStatus _status;
    protected String _bbLibName;

    /**
     * Creates a BasicLegacyImportHelper instance.
     *
     * @param legacyProjects
     */
    public BasicLegacyImportHelper( Set< Project > legacyProjects ) {
        this( legacyProjects, LINK_IMPORT, null );
    }

    /**
     * Creates a BasicLegacyImportHelper instance.
     *
     * @param legacyProjects
     * @param importType
     */
    public BasicLegacyImportHelper( Set< Project > legacyProjects, int importType ) {
        this( legacyProjects, importType, null );
    }

    /**
     * Creates a BasicLegacyImportHelper instance.
     *
     * @param legacyProjects
     * @param importType
     * @param REPath
     */
    public BasicLegacyImportHelper( Set< Project > legacyProjects, int importType, IPath REPath ) {
        _importType = importType;
        _REPath = REPath;
        _legacyProjects = legacyProjects;
        if( legacyProjects.size() > 0 ) {
            _legacyWorkspace = legacyProjects.iterator().next().getWorkspace();
        }
    }

    protected boolean isValidImportType( int type ) {
        if( type == LINK_IMPORT || type == COPY_IMPORT ) {
            return true;
        }
        return false;
    }

    /**
     * Sets the BlackBerry runtime environment path which will be attached to the imported projects.
     *
     * @param type
     */
    public void setBBREPath( IPath REPath ) {
        _REPath = REPath;
    }

    /**
     * Gets the BlackBerry runtime environment path which will be attached to the imported projects.
     *
     * @return
     */
    public IPath getBBRE() {
        return _REPath;
    }

    private IStatus importProjects( IProgressMonitor monitor ) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot workspaceRoot = workspace.getRoot();
        MultiStatus resultStatus = StatusFactory.createMultiStatus( Messages.LegacyImportOperation_ERROR_INDICATION_MSG );
        try {
            initialize();
        } catch( CoreException e ) {
            resultStatus.merge( StatusFactory.createErrorStatus( e.getMessage() ) );
            return resultStatus;
        }
        monitor.beginTask( "Importing legacy projects.", _legacyProjects.size() * 2 );
        for( Project legacyProject : _legacyProjects ) {// first loop for import
            try {
                importProject( legacyProject, new SubProgressMonitor( monitor, 1 ) );
                IProject project = workspaceRoot.getProject( legacyProject.getDisplayName() );
                project.refreshLocal( IProject.DEPTH_INFINITE, monitor );
            } catch( CoreException e ) {
                _log.error( e );
                // if the java project is not created successfully, roll back to delete the project
                IProject project = workspaceRoot.getProject( legacyProject.getDisplayName() );
                if( project.exists() ) {
                    try {
                        project.delete( true, new NullProgressMonitor() );
                    } catch( CoreException e1 ) {
                        _log.error( e1 );
                    }
                }
                // remove the BB properties in case it is there
                ContextManager.PLUGIN.removeBBProperties( legacyProject.getDisplayName() );
                resultStatus.merge( StatusFactory.createErrorStatus(
                        NLS.bind( Messages.LegacyImportOperation_PROJECT_IMPORT_FAIL_MSG, legacyProject.getDisplayName() ), e ) );
                monitor.worked( 1 );
            }
            if( monitor.isCanceled() ) {
                break;
            }
        }
        monitor.done();
        return resultStatus;
    }

    private void initialize() throws CoreException {
        importWspPreprocessTags();
        if( _legacyProjects != null ) {
            _bbLibName = WorkspaceDependencyUtils.generateBBLibName( _legacyWorkspace.getDisplayName() );
            if( !StringUtils.isBlank( _bbLibName ) ) {
                InternalWorkspaceDependencyUtils.storeDependenciesAsUserLibrary( _legacyWorkspace, _bbLibName );
            }
        }
    }

    private void importWspPreprocessTags() {
        List< String > wspPreProcessTags = _legacyWorkspace.getDefines();
        ArrayList< PreprocessorTag > wspPpList = (ArrayList< PreprocessorTag >) PreprocessorPreferences.getPreprocessDefines();

        for( String tag : wspPreProcessTags ) {
            PreprocessorTag tagObj = new PreprocessorTag( tag, true );
            // check old pp tag is valid and does not exists in the list
            if( ( ImportUtils.isValidPPtag( tag ) ) && ( !ImportUtils.isPPtagsExists( wspPpList, tag ) ) ) {
                wspPpList.add( tagObj );
            }
        }
        // disable the property change listener before adding workspace
        // level preprocessor tags
        PropertyChangeListenerImp.removeListener();
        PreprocessorPreferences.setPreprocessDefines( wspPpList );
        // enable the property change listener after adding workspace
        // level preprocessor tags
        PropertyChangeListenerImp.addListener();
    }

    /**
     * This method is implemented differently in the internal fragment's LegacyImportHelper from the external version.
     */
    abstract protected IJavaProject importProject( Project legacyProject, IProgressMonitor monitor ) throws CoreException;

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime .IProgressMonitor)
     */
    @Override
    public void run( IProgressMonitor monitor ) throws CoreException {
        // check the import type before starting
        if( !isValidImportType( _importType ) ) {
            throw new CoreException( StatusFactory.createErrorStatus( NLS.bind(
                    Messages.LegacyImportOperation_WRONG_IMPORT_TYPE_MSG, _importType ) ) );
        }
        _status = importProjects( monitor );
    }

    /**
     * Get the status which represents the result of the import. This method should be called after the
     * {@link BasicLegacyImportHelper#run(IProgressMonitor)} is executed.
     *
     * @return
     */
    public IStatus getStatus() {
        return _status;
    }
}
