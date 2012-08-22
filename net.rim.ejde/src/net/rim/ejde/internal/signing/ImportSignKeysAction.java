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
import java.util.ArrayList;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.ui.widgets.dialog.SigningSearchDialog;
import net.rim.ejde.internal.util.Messages;

import org.apache.log4j.Logger;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public class ImportSignKeysAction implements IWorkbenchWindowActionDelegate {

    private static Logger _log = Logger.getLogger( ImportSignKeysAction.class );

    /**
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action .IAction,
     *      org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged( IAction action, ISelection selection ) {

    }

    @Override
    public void run( IAction action ) {
        // Open file dialog to allow user select the parent folder of *.csk and *.db files
        SigningSearchDialog oldKeyDialog = new SigningSearchDialog( ContextManager.getActiveWorkbenchShell() );
        try {
            ArrayList< File > oldKeyFiles = oldKeyDialog.search();
            if( oldKeyFiles != null ) {
                oldKeyDialog.copyFileIntoSignToolDir( oldKeyFiles );
                MessageDialog dialog = new MessageDialog( ContextManager.getActiveWorkbenchShell(),
                        Messages.CodeSigningPrefsPage_MessageDialogTitle1, null, Messages.CodeSigningPrefsPage_MessageDialogMsg1,
                        MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0 );
                dialog.open();
                _log.info( Messages.CodeSigningPrefsPage_MessageDialogMsg9 );
            }
        } catch( IllegalArgumentException ex ) {
            MessageDialog dialog = new MessageDialog( ContextManager.getActiveWorkbenchShell(),
                    Messages.CodeSigningPrefsPage_MessageDialogTitle1, null, ex.getMessage(), MessageDialog.WARNING,
                    new String[] { IDialogConstants.OK_LABEL }, 0 );
            dialog.open();
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
