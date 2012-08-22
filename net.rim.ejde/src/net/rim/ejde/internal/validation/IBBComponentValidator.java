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

public interface IBBComponentValidator {
    /**
     * Validates a single validation unit represented by an Object
     *
     * @param validateThis
     *            -- the validation unit to validated.
     * @return a diagnostic chain. If severity is not OK, then the chain will contain one or more WicaDiagnostic children. Each
     *         child should represent exactly one error, warning or informational item for this validation unit.
     */
    BBDiagnostic validate( Object validateThis );
}
