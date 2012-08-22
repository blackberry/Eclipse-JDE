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
package net.rim.ejde.internal.signing;

import java.io.File;
import java.io.IOException;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMToolsUtils;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class RemoveSignKeysAction implements IWorkbenchWindowActionDelegate {

    private static Logger _log = Logger.getLogger( RemoveSignKeysAction.class );

    /**
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action .IAction,
     *      org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged( IAction action, ISelection selection ) {

    }

    @Override
    public void run( IAction action ) {
        if( MessageDialog.openQuestion( ContextManager.getActiveWorkbenchShell(),
                Messages.CodeSigningPrefsPage_MessageDialogTitle3, Messages.CodeSigningPrefsPage_MessageDialogMsg4
                        + Messages.CodeSigningPrefsPage_MessageDialogMsg6 ) ) {
            removeKeys();
        }
    }

    private void removeKeys() {
        try {
            File cskFile = new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.CSK_FILE_NAME );
            File dbFile = new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.DB_FILE_NAME );
            if( ( !cskFile.exists() ) && ( !dbFile.exists() ) ) {
                MessageDialog dialog = new MessageDialog( ContextManager.getActiveWorkbenchShell(),
                        Messages.CodeSigningPrefsPage_MessageDialogTitle3, null, Messages.CodeSigningPrefsPage_MessageDialogMsg3,
                        MessageDialog.WARNING, new String[] { IDialogConstants.OK_LABEL }, 0 );
                dialog.open();
                return;
            }
            if( cskFile.exists() ) {
                cskFile.renameTo( new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.CSK_FILE_NAME
                        + IConstants.UNDERSCORE_STRING + System.currentTimeMillis() ) );
            }
            if( dbFile.exists() ) {
                dbFile.renameTo( new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.DB_FILE_NAME
                        + IConstants.UNDERSCORE_STRING + System.currentTimeMillis() ) );
            }
            if( ( !cskFile.exists() ) && ( !dbFile.exists() ) ) {
                MessageDialog dialog = new MessageDialog( ContextManager.getActiveWorkbenchShell(),
                        Messages.CodeSigningPrefsPage_MessageDialogTitle3, null, Messages.CodeSigningPrefsPage_MessageDialogMsg5
                                + Messages.CodeSigningPrefsPage_MessageDialogMsg6, MessageDialog.INFORMATION,
                        new String[] { IDialogConstants.OK_LABEL }, 0 );
                dialog.open();
                _log.info( Messages.CodeSigningPrefsPage_MessageDialogMsg7 );
            }
        } catch( IOException e ) {
            _log.error( e.getMessage() );
        }
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

    @Override
    public void init( IWorkbenchWindow window ) {
        // TODO Auto-generated method stub

    }
}
