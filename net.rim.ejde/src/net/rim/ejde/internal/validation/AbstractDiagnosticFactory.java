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

/**
 * @author cbateman
 */
public abstract class AbstractDiagnosticFactory {
    public static BBDiagnostic getOK() {
        return BBDiagnostic.OK_INSTANCE;
    }

    public static BBDiagnostic createChainedDiagnostic() {
        return new BBDiagnostic( "", -1, "", null );
    }
}
