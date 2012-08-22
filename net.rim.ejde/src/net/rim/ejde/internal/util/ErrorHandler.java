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

import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;

public class ErrorHandler {
    private static final Logger _logger = Logger.getLogger( ErrorHandler.class );

    public static void handleOperationException( Shell shell, String dialogMsg, Throwable e ) {
        if( e instanceof InvocationTargetException ) {
            e = ( (InvocationTargetException) e ).getTargetException();
        }

        IStatus status = null;
        if( e instanceof CoreException ) {
            status = ( (CoreException) e ).getStatus();
        } else {
            String message = e.getMessage();
            if( message == null ) {
                message = e.toString();
            }
            status = StatusFactory.createErrorStatus( message, null );
        }
        ErrorDialog.openError( shell, Messages.ErrorHandler_DIALOG_TITLE, dialogMsg, status );
        // MessageDialog.openError(getShell(), "Error", err);
        _logger.error( e );

    }
}
