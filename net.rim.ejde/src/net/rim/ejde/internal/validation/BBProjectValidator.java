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

import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;
import net.rim.ejde.internal.util.Messages;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.osgi.util.NLS;

/**
 * Validate a given BlackBerry project. It validates the schema first, if it passes, then it validates the the project properties.
 *
 * @author dmeng
 *
 */
public class BBProjectValidator implements IBBComponentValidator {
    static private final Logger log = Logger.getLogger( BBProjectValidator.class );

    /**
     * Validates the given object (IProject)
     */
    public BBDiagnostic validate( Object validateThis ) {
        // check if the meta file exists
        BBDiagnostic diag = AbstractDiagnosticFactory.createChainedDiagnostic();
        IProject iproject = (IProject) validateThis;
        try {
            if( iproject.hasNature( BlackBerryProjectCoreNature.NATURE_ID )
                    && !iproject.getFile( BlackBerryProject.METAFILE ).exists() ) {
                diag.merge( new BBDiagnostic( Diagnostic.ERROR, DiagnosticFactory.DIAGNOSTIC_SOURCE, -1, NLS.bind(
                        Messages.BBProjectValidator_MISSING_PROJECT_DESCRIPTION_ERROR, iproject.getName() ), null ) );
            }
        } catch( CoreException e ) {
            log.error( e.getMessage() );
        }
        return diag;
    }
}
