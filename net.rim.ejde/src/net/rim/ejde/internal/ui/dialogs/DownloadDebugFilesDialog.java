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
package net.rim.ejde.internal.ui.dialogs;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.model.preferences.RootPreferences;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class DownloadDebugFilesDialog extends MessageDialog {

    private Button _dontAskButton;

    public DownloadDebugFilesDialog( String title, String message ) {
        super( ContextManager.getActiveWorkbenchShell(), title, null, message, QUESTION, new String[] {
                IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0 ); // yes is the default
    }

    /**
     * Convenience method to open dialog.
     */
    public static int openQuestion( String title, String message ) {
        DownloadDebugFilesDialog dialog = new DownloadDebugFilesDialog( title, message );
        int result = dialog.open();
        if( result == IDialogConstants.OK_ID ) {
            return PreferenceConstants.DOWNLOAD_DEBUG_FILES_YES;
        }
        return PreferenceConstants.DOWNLOAD_DEBUG_FILES_NO;
    }

    @Override
    protected Control createCustomArea( Composite parent ) {
        GridLayout gridLayout = (GridLayout) parent.getLayout();
        gridLayout.numColumns = 1;
        gridLayout.marginHeight = 10;

        _dontAskButton = new Button( parent, SWT.CHECK );
        _dontAskButton.setText( Messages.DontAskMeAgainMsg );
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace = true;
        _dontAskButton.setLayoutData( data );
        return parent;

    }

    @Override
    protected void buttonPressed( int buttonId ) {
        int option;
        if( _dontAskButton.getSelection() ) {
            if( buttonId == IDialogConstants.OK_ID ) {
                option = PreferenceConstants.DOWNLOAD_DEBUG_FILES_YES;
            } else {
                option = PreferenceConstants.DOWNLOAD_DEBUG_FILES_NO;
            }
        } else {
            option = PreferenceConstants.DOWNLOAD_DEBUG_FILES_PROMPT;
        }
        RootPreferences.setDownloadDebugFilesOption( option );
        super.buttonPressed( buttonId );
    }
}
