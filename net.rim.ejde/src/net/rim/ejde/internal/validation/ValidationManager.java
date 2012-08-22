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
package net.rim.ejde.internal.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import net.rim.ejde.internal.core.IRIMMarker;
import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.ResourceBuilderUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.jface.text.BadLocationException;

/**
 * Singleton class that controls validation for all BB projects
 *
 * Listens for resource changes and triggers validation when any occur
 *
 * @author bchabot
 */
public class ValidationManager {

    private static final Logger _logger = Logger.getLogger( ValidationManager.class );

    private static final int NO_SEVERITY = -1;

    private static ValidationManager _instance;

    /** Map of IProject to ProjectValidationManager */
    private Map< IProject, ProjectValidationManager > _projectModelValidators;

    private DiagnosticManager _diagnosticManager;

    /**
     * private constructor, cannot be instantiated
     */
    private ValidationManager() {
        _projectModelValidators = new Hashtable< IProject, ProjectValidationManager >();
        _diagnosticManager = new DiagnosticManager();
        _diagnosticManager.addChangeListener( new MarkerSynchronizer() );
        ResourcesPlugin.getWorkspace().addResourceChangeListener( new MyResourceChangeListener(),
                IResourceChangeEvent.POST_CHANGE );
    }

    /**
     * Returns the singleton instance.
     *
     * @return <code>ValidationManager</code>
     */
    public static ValidationManager getInstance() {
        if( _instance == null ) {
            _instance = new ValidationManager();
        }
        return _instance;
    }

    /**
     * Removes diagnostics for given deleted model object
     *
     * @param iproject
     * @param iProject
     * @param modelObject
     */
    public void handleDeletedResource( IProject iproject, IProject iProject, Object modelObject ) {
        getProjectValidator( iproject ).removeAllDiagnostics( iProject, modelObject );
    }

    /**
     * Clean-up for closed projects
     *
     * @param closedProjects
     */
    public void handleClosedProjects( Collection< IProject > closedProjects ) {
        synchronized( _projectModelValidators ) {
            for( IProject iproject : closedProjects ) {
                ProjectValidationManager projValidator = _projectModelValidators.remove( iproject );
                if( projValidator != null ) {
                    getDiagnosticManager().removeProjectRegistry( projValidator.getIProject() );
                }
            }
        }
    }

    private ProjectValidationManager getProjectValidator( IProject iproject ) {
        synchronized( _projectModelValidators ) {
            ProjectValidationManager projectValidator = _projectModelValidators.get( iproject );
            if( projectValidator == null ) {
                projectValidator = new ProjectValidationManager( getDiagnosticManager(), iproject );
                _projectModelValidators.put( iproject, projectValidator );
            }
            return projectValidator;
        }
    }

    /**
     * Validates the given project.
     *
     * @param iproject
     *            The project to be validated
     * @param monitor
     *            The progress monitor or null
     */
    public void validateProject( IProject iproject, final IProgressMonitor monitor ) {
        final ProjectValidationManager projValidator = getProjectValidator( iproject );
        if( projValidator != null ) {
            projValidator.validateProject( monitor );
        }
    }

    /**
     * Validates the given projectgs.
     *
     * @param changedProjects
     * @param monitor
     */
    public void validateProjects( Collection< IProject > changedProjects, IProgressMonitor monitor ) {
        if( monitor == null ) {
            monitor = new NullProgressMonitor();
        }
        monitor.beginTask( "Validating projects", changedProjects.size() );
        for( IProject iproject : changedProjects ) {
            // do not validate non-BB project
            try {
                if( !iproject.hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                    continue;
                }
            } catch( CoreException e ) {
                _logger.error( e.getMessage() );
                continue;
            }
            String msg = "Validating project " + iproject.getName();
            _logger.debug( msg );
            monitor.subTask( msg );
            validateProject( iproject, monitor );
            monitor.worked( 1 );
        }
        monitor.done();
    }

    /**
     * Retrieves the DiagnosticManager
     *
     * @return
     */
    private DiagnosticManager getDiagnosticManager() {
        return _diagnosticManager;
    }

    /**
     * Cleans the respective target diags from the diagnosticManager registry
     *
     * @param project
     * @param target
     */
    public void cleanObjectDiags( IProject project, Object target ) {
        _diagnosticManager.cleanObjectDiags( project, target );
    }

    /**
     * Determine if given project has any validation errors
     *
     * @param refreshValidation
     *            - true if validation should be rerun for project before returning result
     * @param iRes
     * @return true if project has one or more errors, false otherwise
     */
    public boolean hasValidationErrors( boolean refreshValidation, IResource iRes, IProgressMonitor monitor ) {
        if( refreshValidation ) {
            if( iRes instanceof IProject ) {
                validateProject( (IProject) iRes, monitor );
            }
        }
        try {
            if( iRes.findMaxProblemSeverity( IRIMMarker.MODEL_PROBLEM, true, IResource.DEPTH_INFINITE ) == IMarker.SEVERITY_ERROR ) {
                return true;
            }
            return false;
        } catch( CoreException e ) {
            _logger.error( e );
            return true;
        }
    }

    /**
     * Listens to BBDiagnostic change events, and creates, deletes markers for them
     *
     * @author bchabot
     *
     */
    private class MarkerSynchronizer implements IDiagnosticManagerChangeListener {

        public MarkerSynchronizer() {
        }

        public void addedDiagnostic( IProject project, Object target, BBDiagnostic diagnostic ) {
            IResource ires = getIResource( target );
            if( ires != null ) {
                try {
                    clearValidationMarkers( ires, IResource.DEPTH_ZERO );
                } catch( CoreException e ) {
                    _logger.error( e.getMessage() );
                }
                for( Diagnostic child : diagnostic.getChildren() ) {
                    if( child.getSeverity() != Diagnostic.OK ) {
                        createValidationMarker( ires, child.getMessage(), child.getCode(),
                                getMarkerSeverity( child.getSeverity() ) );
                    }
                }
            }
        }

        public void removedDiagnostic( IProject project, Object target ) {

            try {
                IResource ires = getIResource( target );
                if( ires != null ) {
                    clearValidationMarkers( ires, IResource.DEPTH_ZERO );
                }
            } catch( CoreException e ) {
                _logger.error( e );
            }
        }

        public void removedProject( IProject project ) {
            // eclipse should delete markers when project deleted, not needed
        }

        private IResource getIResource( Object target ) {
            if( ( target instanceof IProject ) || ( target instanceof IFile ) ) {
                return (IResource) target;
            }
            return null;
        }
    }

    private static int getMarkerSeverity( int diagSeverity ) {
        int markerSeverity = NO_SEVERITY;
        switch( diagSeverity ) {
            case Diagnostic.ERROR:
                markerSeverity = IMarker.SEVERITY_ERROR;
                break;

            case Diagnostic.WARNING:
                markerSeverity = IMarker.SEVERITY_WARNING;
                break;

            case Diagnostic.INFO:
                markerSeverity = IMarker.SEVERITY_INFO;
                break;
        }
        return markerSeverity;
    }

    /**
     * Returns if the given resource has any problem.
     *
     * @param resource
     *            The given resource
     * @return true if yes; otherwise false
     */
    public static boolean hasProblems( IResource resource ) {
        if( !resource.isAccessible() ) {
            return false;
        }
        try {
            return ( resource.findMarkers( IRIMMarker.MODEL_PROBLEM, true, IResource.DEPTH_ZERO ).length > 0 );
        } catch( CoreException e ) {
            _logger.error( e );
        }
        return false;
    }

    /**
     * Find if resource has any general problems of the specific id
     *
     * @param resource
     * @param id
     * @return
     */
    public boolean hasProblems( IResource resource, int id ) {
        return hasProblems( resource, IRIMMarker.MODEL_PROBLEM, id );
    }

    /**
     * Find if resource has any problems of the specific type and id
     *
     * @param resource
     * @param type
     * @param id
     * @return
     */
    public boolean hasProblems( IResource resource, String type, int id ) {
        if( !resource.isAccessible() ) {
            return false;
        }
        try {
            IMarker[] markers = resource.findMarkers( type, true, IResource.DEPTH_ZERO );
            for( IMarker marker : markers ) {
                if( marker.getAttribute( IRIMMarker.ID, -1 ) == id ) {
                    return true;
                }
            }

        } catch( CoreException e ) {
            _logger.error( e );
        }
        return false;
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
    private static void createValidationMarker( IResource resource, String message, int lineNumber, int severity ) {
        try {
            ResourceBuilderUtils.createProblemMarker( resource, IRIMMarker.MODEL_PROBLEM, message, lineNumber, severity );
        } catch( CoreException e ) {
            _logger.error( e );
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
    private static void clearValidationMarkers( IResource resource, int depth ) throws CoreException {
        ResourceBuilderUtils.cleanProblemMarkers( resource, new String[] { IRIMMarker.MODEL_PROBLEM }, depth );
    }

    class MyResourceChangeListener implements IResourceChangeListener {

        @Override
        public void resourceChanged( IResourceChangeEvent event ) {
            if( event.getType() == IResourceChangeEvent.POST_CHANGE ) {
                IResourceDelta rootDelta = event.getDelta();
                ResDeltaVisitor visitor = new ResDeltaVisitor();
                try {
                    rootDelta.accept( visitor );
                } catch( CoreException e ) {
                    _logger.error( "", e );
                }
                Set< IProject > allChangedProjects = ProjectUtils.getAllReferencingProjects( visitor.changedProjects
                        .toArray( new IProject[ visitor.changedProjects.size() ] ) );
                validateProjects( allChangedProjects, new NullProgressMonitor() );
                handleClosedProjects( visitor.closedOrDeletedProjects );
            }
        }
    }

    /**
     * The Class ResDeltaVisitor.
     */
    private static class ResDeltaVisitor implements IResourceDeltaVisitor {

        public Collection< IProject > changedProjects;
        public Collection< IProject > closedOrDeletedProjects;

        /**
         * Instantiates a new res delta visitor.
         */
        public ResDeltaVisitor() {
            super();
            changedProjects = new ArrayList< IProject >();
            closedOrDeletedProjects = new ArrayList< IProject >();
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
         */
        public boolean visit( IResourceDelta delta ) {

            IResource iresource = delta.getResource();
            if( iresource.isDerived() ) {
                return false;
            }
            IProject iproject = iresource.getProject();
            if( iproject != null && ( !iproject.isOpen() || !iproject.exists() ) ) {
                closedOrDeletedProjects.add( iproject );
                return false;
            }
            if( delta.getKind() == IResourceDelta.CHANGED ) {
                // only react to content changes, otherwise this could just simply be a marker change, etc
                if( ( delta.getFlags() & IResourceDelta.CONTENT ) != 0 && ValidationUtils.needToBeValidated( iresource )
                        && !changedProjects.contains( iresource.getProject() ) ) {
                    changedProjects.add( iresource.getProject() );
                }
                return true; // visit the children
            } else if( delta.getKind() == IResourceDelta.ADDED && !changedProjects.contains( iresource.getProject() ) ) {
                changedProjects.add( iresource.getProject() );
                return false;
            } else if( delta.getKind() == IResourceDelta.REMOVED && !changedProjects.contains( iresource.getProject() ) ) {
                changedProjects.add( iresource.getProject() );
                return false;
            }
            return true;
        }
    }
}
