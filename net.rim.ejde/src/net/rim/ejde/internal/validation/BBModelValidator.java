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

import org.eclipse.emf.common.util.Diagnostic;

/**
 * Validate a given BlackBerry project. It validates the schema first, if it passes, then it validates the the project properties.
 *
 * @author dmeng
 *
 */
public class BBModelValidator implements IBBComponentValidator {

    /**
     * Validates the given object (IProject)
     */
    public BBDiagnostic validate( Object validateThis ) {
        BBDiagnostic diag = AbstractDiagnosticFactory.createChainedDiagnostic();
        BBDiagnostic schemaDiagnostic = new BBSchemaValidator().validate( validateThis );
        if( schemaDiagnostic.getSeverity() == Diagnostic.OK ) {
            BBDiagnostic propertiesDiagnostic = new BBPropertiesValidator().validate( validateThis );
            diag.merge( propertiesDiagnostic );
        } else {
            diag.merge( schemaDiagnostic );
        }
        return diag;
    }
}
