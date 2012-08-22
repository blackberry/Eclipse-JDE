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

import net.rim.ejde.internal.core.ContextManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

public class ProblemFactory {

    public static CoreException create_VM_MISSING_exception( String projectName ) {
        return new CoreException( new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, NLS.bind(
                Messages.ProblemFactory_VM_missing_err_msg, projectName ) ) );
    }
}
