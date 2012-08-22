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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.rim.ejde.internal.util.Progress;

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author dmeng
 */
@SuppressWarnings("restriction")
public class ProjectValidationManager {

    /** Map of validation objection to validator **/
    private Map< Class, IBBComponentValidator > _validators;

    private DiagnosticManager _diagnostics;

    private IProject _iProject;

    private IValidationProvider _validationProvider;

    /**
     * Constructor.
     *
     * @param diagnosticRegistry
     *            the diagnostic registry
     * @param project
     *            the IProject
     */
    public ProjectValidationManager( DiagnosticManager diagnosticRegistry, IProject project ) {
        _validators = new HashMap< Class, IBBComponentValidator >();
        _validationProvider = new ValidationProvider();
        _diagnostics = diagnosticRegistry;
        _iProject = project;
        initValidators();
    }

    protected void initValidators() {
        putValidator( Project.class, new BBProjectValidator() );
        putValidator( File.class, new BBModelValidator() );
    }

    protected void putValidator( Class validationType, IBBComponentValidator validator ) {
        _validators.put( validationType, validator );
    }

    public IBBComponentValidator getValidator( Class validationType ) {
        return _validators.get( validationType );
    }

    /**
     * Validates the given object (if a validator exists for it), stores the result in diagnostics registry (overwriting if
     * something already there), and returns the result.
     *
     * @param validateThis
     *            The object to be validated
     * @return
     */
    public BBDiagnostic validate( Object validateThis ) {
        IBBComponentValidator validator = getValidator( validateThis.getClass() );
        BBDiagnostic result = null;
        if( validator != null ) {
            result = validator.validate( validateThis );
            _diagnostics.put( _iProject, validateThis, result );
        }
        return result;
    }

    /**
     * Validates the given object and its children.
     *
     * @param validateThis
     *            The object to be validated
     * @param monitor
     *            The progress monitor
     */
    private void validateAll( Object validateThis, IProgressMonitor monitor ) {
        validate( validateThis );

        List< Object > validationChildren = _validationProvider.getValidationChildren( validateThis );
        int nv = validationChildren.size();
        Progress prog = null;
        if( monitor != null ) {
            prog = new Progress( monitor, nv );
        }

        for( Object obj : validationChildren ) {
            validateAll( obj, monitor );
            if( prog != null ) {
                prog.worked();
            }
        }
    }

    /**
     * Validates the project
     *
     * @param monitor
     *            The progress monitor or <code>null</code> if no progress monitor.
     */
    public void validateProject( IProgressMonitor monitor ) {
        validateAll( _iProject, monitor );
    }

    public void removeAllDiagnostics( IProject project, Object object ) {
        // adding a diagnostic with error level OK, effectively removes
        // any existing diagnostic for this eObject
        _diagnostics.put( project, object, BBDiagnostic.OK_INSTANCE );
    }

    /**
     * Removes the given project from projects registry and notify listeners.
     *
     * @param project
     *            The project to be removed
     */
    public void removeAllForProject( IProject project ) {
        _diagnostics.removeProjectRegistry( project );
    }

    /**
     * @return The IProject
     */
    public IProject getIProject() {
        return _iProject;
    }
}
