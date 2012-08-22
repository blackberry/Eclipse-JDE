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
package net.rim.ejde.internal.core;

import java.util.List;
import java.util.TreeMap;

import net.rim.ejde.internal.builders.ResourceBuilder;
import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.packaging.PackagingManager;
import net.rim.ejde.internal.util.ImportUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.ResourceBuilderUtils;
import net.rim.ejde.internal.validation.ValidationManager;
import net.rim.ide.Project;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

abstract public class BasicClasspathElementChangedListener implements IElementChangedListener {
    static private Logger _log = Logger.getLogger( BasicClasspathElementChangedListener.class );
    private TreeMap< String, Boolean > _changedBuildClasspath = new TreeMap< String, Boolean >();

    public BasicClasspathElementChangedListener() {
    }

    public void elementChanged( ElementChangedEvent event ) {
        IJavaElementDelta[] children = event.getDelta().getChangedChildren(); // children = IProjects
        for( IJavaElementDelta child : children ) {
            IProject project = child.getElement().getJavaProject().getProject();
            int size = child.getAffectedChildren().length; // .getChangedElement() = JavaProject
            if( size == 1 ) {
                IJavaElementDelta elementDelta = child.getAffectedChildren()[ 0 ]; // if it is only 1, name is ".tmp"
                // and elementDelta.kind = 4
                // (CHANGED)
                IJavaElement changedElement = elementDelta.getElement();
                if( changedElement.getElementName().equals(
                        ImportUtils.getImportPref( ResourceBuilder.LOCALE_INTERFACES_FOLDER_NAME ) ) ) {
                    _changedBuildClasspath.put( project.getName(), Boolean.FALSE );
                    break;
                }
            }
            if( isClasspathChange( child ) ) {// adding classpath entries might induce reordering the classpath entries
                _changedBuildClasspath.put( project.getName(), Boolean.TRUE );
                // notify the listeners
                EJDEEventNotifier.getInstance()
                        .notifyClassPathChanged( child.getElement().getJavaProject(), hasCPRemoved( child ) );
                // validate the project
                ValidationManager.getInstance()
                        .validateProjects(
                                ProjectUtils.getAllReferencingProjects( new IProject[] { child.getElement().getJavaProject()
                                        .getProject() } ), null );
            }
            if( ( child.getFlags() & IJavaElementDelta.F_CLASSPATH_CHANGED ) != 0
                    || ( child.getFlags() & IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED ) != 0 ) {
                IJavaElement javaElement = child.getElement();
                final IJavaProject javaProject = javaElement.getJavaProject();
                classPathChanged( javaProject, child );
            }
            checkSourceAttachement( child.getAffectedChildren() );
        }

        for( final IJavaElementDelta addedElemDelta : event.getDelta().getAddedChildren() ) {
            final IJavaProject javaProject = addedElemDelta.getElement().getJavaProject();
            try {
                if( javaProject.getProject().hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                    if( addedElemDelta.getAffectedChildren().length == 0 ) {
                        final IJavaElement addedElement = addedElemDelta.getElement();
                        if( addedElement instanceof IJavaProject ) {
                            final IJavaProject addedJavaProj = (IJavaProject) addedElement;
                            if( addedJavaProj.equals( javaProject ) ) {
                                projectCreated( javaProject );
                            }
                        }
                    }
                }
            } catch( final CoreException ce ) {
                _log.error( "", ce );
            }
        }
    }

    public void classPathChanged( final IJavaProject javaProj, final IJavaElementDelta childDelta ) {
        _log.trace( "Entered classPathChanged(); project: " + javaProj.getProject().getName() );
        // check project options
        checkProjectOptions( javaProj );
        // check project dependency
        hasProjectDependencyProblem( javaProj );
    }

    private void checkProjectOptions( IJavaProject javaProj ) {
        _log.trace( "Entered checkProjectOptions(); project: " + javaProj.getProject().getName() );
        try {
            // we only check BB projects and Java projects which are depended by any BB projects
            if( !javaProj.getProject().hasNature( BlackBerryProjectCoreNature.NATURE_ID )
                    && !ProjectUtils.isDependedByBBProject( javaProj.getProject() ) )
                return;
            // get all referred projects
            List< IJavaProject > referedProjects = ProjectUtils.getAllReferencedJavaProjects( javaProj );
            List< IJavaProject > nonBBProjects = ProjectUtils.getNonBBJavaProjects( referedProjects );
            // get non-bb projects which has JDK compatibility problem
            List< IJavaProject > projectsWithProblem = ProjectUtils
                    .getJavaProjectsContainJDKCompatibilityProblem( referedProjects );
            if( nonBBProjects.size() > 0 ) {
                setJDKCompatibilitySettings( javaProj, nonBBProjects, projectsWithProblem );
            }
        } catch( JavaModelException e ) {
            _log.error( e.getMessage() );
        } catch( CoreException e ) {
            _log.error( e.getMessage() );
        }
    }

    /**
     * The flags indicate a classpath chhange and its type
     *
     * @param flags
     *            the flags to inspect
     * @return true if the flag flags a classpath change
     */
    static public boolean isClasspathChangeFlag( int flags ) {
        if( ( flags & IJavaElementDelta.F_RESOLVED_CLASSPATH_CHANGED ) != 0 )
            return true;

        if( ( flags & IJavaElementDelta.F_ADDED_TO_CLASSPATH ) != 0 )
            return true;

        if( ( flags & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH ) != 0 )
            return true;

        if( ( flags & IJavaElementDelta.F_ARCHIVE_CONTENT_CHANGED ) != 0 )
            return true;

        return false;
    }

    private boolean hasCPRemoved( IJavaElementDelta delta ) {
        for( IJavaElementDelta cpDelta : delta.getAffectedChildren() ) {
            if( ( cpDelta.getFlags() & IJavaElementDelta.F_REMOVED_FROM_CLASSPATH ) != 0
                    && cpDelta.getElement().getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT
                    && ( (IPackageFragmentRoot) cpDelta.getElement() ).getPath().lastSegment().equals( IConstants.RIM_API_JAR ) )
                return true;
        }
        return false;
    }

    private void setJDKCompatibilitySettings( final IJavaProject mainProject, final List< IJavaProject > nonBBProjects,
            final List< IJavaProject > projectsWithProblem ) {
        _log.trace( "Entered setJDKCompatibilitySettings()" );
        if( projectsWithProblem.size() > 0 ) {
            StringBuffer projectList = new StringBuffer();
            for( IJavaProject javaProject : projectsWithProblem ) {
                projectList.append( javaProject.getProject().getName() + "\n" );
            }
            final String projectNames = projectList.toString();
            Display.getDefault().syncExec( new Runnable() {
                public void run() {
                    Shell shell = ContextManager.getActiveWorkbenchWindow().getShell();
                    StringBuffer messageBuffer = new StringBuffer( Messages.ClasspathChangeManager_settingJDKComplianceMsg1 );
                    messageBuffer.append( "\n" );
                    messageBuffer.append( NLS.bind( Messages.ClasspathChangeManager_settingJDKComplianceMsg2, mainProject
                            .getProject().getName() ) );
                    messageBuffer.append( "\n" );
                    messageBuffer.append( projectNames );
                    messageBuffer.append( "\n" );
                    messageBuffer.append( Messages.ClasspathChangeManager_settingJDKComplianceMsg3 );
                    MessageDialog complianceDialog = new MessageDialog( shell, Messages.ClasspathChangeManager_DialogTitle, null,
                            messageBuffer.toString(), MessageDialog.INFORMATION, new String[] {
                                    Messages.IConstants_OK_BUTTON_TITLE, Messages.IConstants_CANCEL_BUTTON_TITLE }, 0 );

                    int buttonEventCode = complianceDialog.open();

                    switch( buttonEventCode ) {
                        case Window.OK: {
                            processNonBBProjects( nonBBProjects, projectsWithProblem, true );
                            break;
                        }
                        case Window.CANCEL: {
                            processNonBBProjects( nonBBProjects, projectsWithProblem, false );
                            break;
                        }
                        default:
                            throw new IllegalArgumentException( "Unsupported dialog button event!" );
                    }
                    complianceDialog.close();
                }
            } );
        } else {
            processNonBBProjects( nonBBProjects, projectsWithProblem, false );
        }
    }

    private void processNonBBProjects( final List< IJavaProject > nonBBProjects, final List< IJavaProject > projectsWithProblem,
            final boolean fix ) {
        for( IJavaProject javaProject : nonBBProjects ) {
            if( fix && projectsWithProblem.contains( javaProject ) ) {
                ImportUtils.initializeProjectOptions( javaProject );
            }
        }
        if( fix ) {
            Shell shell = ContextManager.getActiveWorkbenchWindow().getShell();
            String message = Messages.ClasspathChangeManager_rebuildProjectMsg1;
            StringBuffer projectList = new StringBuffer();
            for( IJavaProject javaProject : projectsWithProblem ) {
                projectList.append( javaProject.getProject().getName() + "\n" );
            }
            message += projectList.toString();
            message += Messages.ClasspathChangeManager_rebuildProjectMsg2;
            MessageDialog dialog = new MessageDialog( shell, Messages.ClasspathChangeManager_RebuildProjectDialogTitle, null,
                    message, MessageDialog.INFORMATION, new String[] { Messages.IConstants_OK_BUTTON_TITLE,
                            Messages.IConstants_CANCEL_BUTTON_TITLE }, 0 );
            int buttonEventCode = dialog.open();

            switch( buttonEventCode ) {
                case Window.OK: {
                    buildProjects( projectsWithProblem );
                    break;
                }
                case Window.CANCEL: {
                    break;
                }
                default:
                    throw new IllegalArgumentException( "Unsupported dialog button event!" );
            }
            dialog.close();
        }
    }

    private void buildProjects( final List< IJavaProject > projects ) {
        BuildTask task = new BuildTask( projects );
        task.setRule( ResourcesPlugin.getWorkspace().getRoot() );
        task.schedule();
    }

    /**
     * This methods determines whether an IProject has its build classpath changed by an explicit user action (from the Java Build
     * Path dialog)
     *
     * @param project
     * @return
     */
    public boolean hasClasspathChanged( IProject project ) {
        Boolean projectChanged = _changedBuildClasspath.get( project.getName() );
        if( projectChanged == null || !projectChanged.booleanValue() )
            return false;
        return true;
    }

    /**
     * This method marks an Eclipse project as not having the class path changed by an explicit user action Its callers indicate
     * one build classpath entry has already been processed for a given project
     *
     * @param project
     *            the Eclipse Project
     */
    public void markUnchangedClasspath( IProject project ) {
        _changedBuildClasspath.put( project.getName(), Boolean.FALSE );
    }

    static public boolean hasProjectDependencyProblem( IJavaProject javaProject ) {
        IProject project = javaProject.getProject();
        try {
            ResourceBuilderUtils.cleanProblemMarkers( project, new String[] { IRIMMarker.PROJECT_DEPENDENCY_PROBLEM_MARKER },
                    IResource.DEPTH_ONE );
        } catch( CoreException e ) {
            _log.error( e );
        }
        IClasspathEntry[] classpathEntries = null;
        try {
            classpathEntries = javaProject.getRawClasspath();
        } catch( JavaModelException e ) {
            _log.error( e );
            return true;
        }
        IProject dependentProject = null;
        String projectName = null;
        boolean hasDependencyError = false;
        for( IClasspathEntry entry : classpathEntries ) {
            if( entry.getEntryKind() == IClasspathEntry.CPE_PROJECT ) {
                projectName = entry.getPath().lastSegment();
                dependentProject = ResourcesPlugin.getWorkspace().getRoot().getProject( projectName );
                if( !isValidDependency( javaProject.getProject(), dependentProject ) && !hasDependencyError ) {
                    hasDependencyError = true;
                }
            }
        }
        return hasDependencyError;
    }

    static private boolean isValidDependency( final IProject mainProject, IProject dependentProject ) {
        try {
            // we allow a BB project depending on any other non-BB projects
            if( !dependentProject.hasNature( BlackBerryProjectCoreNature.NATURE_ID ) )
                return true;
        } catch( CoreException e ) {
            _log.error( e.getMessage() );
            String message = NLS.bind( Messages.ClasspathChangeManager_PROJECT_NATURE_ERROR, dependentProject.getName() );
            try {
                ResourceBuilderUtils.createProblemMarker( mainProject, IRIMMarker.PROJECT_DEPENDENCY_PROBLEM_MARKER, message, -1,
                        IMarker.SEVERITY_ERROR );
            } catch( CoreException e1 ) {
                _log.error( e1.getMessage() );
            }
        }
        BlackBerryProperties dependentProperties = ContextManager.PLUGIN.getBBProperties( dependentProject.getName(), false );
        if( dependentProperties == null )
            return false;
        if( PackagingManager.getProjectTypeID( dependentProperties._application.getType() ) != Project.LIBRARY ) {
            final String message = NLS.bind( Messages.ClasspathChangeManager_WrongProjectDependencyMessage, new String[] {
                    mainProject.getName(), dependentProject.getName() } );
            try {
                _log.error( message );
                ResourceBuilderUtils.createProblemMarker( mainProject, IRIMMarker.PROJECT_DEPENDENCY_PROBLEM_MARKER, message, -1,
                        IMarker.SEVERITY_ERROR );
            } catch( CoreException e ) {
                _log.error( e );
            }
            return false;
        }
        return true;
    }

    private void projectCreated( final IJavaProject javaProj ) {
        try {
            if( javaProj.getProject().hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                checkSourceFolders( javaProj );
            }
        } catch( CoreException e ) {
            _log.error( "Error Changing Project Output Folder", e );
        }
        EJDEEventNotifier.getInstance().notifyNewProjectCreated( javaProj );
    }

    /**
     * Checks the source attachment. If the <code>changedElements</code> contains jars/variables which are added to the classpath,
     * this method will try to search for the source jar and attach the source jar to the binary jar if it is found.
     *
     * @param changedElements
     */
    abstract protected void checkSourceAttachement( IJavaElementDelta[] changedElements );

    /**
     * Checks if all source files are existing. If not, create them.
     *
     * @param javaProj
     */
    private void checkSourceFolders( final IJavaProject javaProj ) {
        if( javaProj == null )
            return;
        if( javaProj.exists() ) {
            try {
                if( !javaProj.isOpen() ) {
                    javaProj.open( new NullProgressMonitor() );
                }
                IClasspathEntry[] entries = javaProj.getRawClasspath();
                for( IClasspathEntry entry : entries ) {
                    if( IClasspathEntry.CPE_SOURCE == entry.getEntryKind() ) {
                        IPath path = entry.getPath();
                        final IPath folderPath = path.removeFirstSegments( 1 );
                        if( !folderPath.isEmpty() ) {
                            Display.getDefault().asyncExec( new Runnable() {
                                public void run() {
                                    try {
                                        ImportUtils.createFolders( javaProj.getProject(), folderPath, IResource.FORCE );
                                    } catch( CoreException e ) {
                                        _log.error( e.getMessage() );
                                    }
                                }
                            } );
                        }
                    }
                }
            } catch( JavaModelException e ) {
                _log.error( "findProjectSources: Could not retrieve project sources:", e ); //$NON-NLS-1$
            }
        }
    }

    /**
     * Does the delta indicate a classpath change?
     *
     * @param delta
     *            the delta to inspect
     * @return true if classpath has changed
     */
    private boolean isClasspathChange( IJavaElementDelta delta ) {
        int flags = delta.getFlags();
        if( isClasspathChangeFlag( flags ) )
            return true;

        if( ( flags & IJavaElementDelta.F_CHILDREN ) != 0 ) {
            IJavaElementDelta[] children = delta.getAffectedChildren();
            for( IJavaElementDelta element : children ) {
                if( isClasspathChangeFlag( element.getFlags() ) )
                    return true;
            }
        }

        return false;
    }

    protected class BuildTask extends WorkspaceJob {
        List< IJavaProject > projectsNeedToBuild;

        public BuildTask( List< IJavaProject > javaProjects ) {
            super( "" );
            projectsNeedToBuild = javaProjects;
        }

        @Override
        public IStatus runInWorkspace( IProgressMonitor monitor ) throws CoreException {
            monitor.beginTask( "Building...", projectsNeedToBuild.size() );
            for( IJavaProject javaProjects : projectsNeedToBuild ) {
                try {
                    javaProjects.getProject().build( IncrementalProjectBuilder.FULL_BUILD, new SubProgressMonitor( monitor, 1 ) );
                } catch( CoreException e ) {
                    _log.error( e.getMessage() );
                }
            }
            return Status.OK_STATUS;
        }
    }

}
