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
import net.rim.ejde.internal.model.preferences.PreprocessorPreferences;
import net.rim.ejde.internal.util.Messages;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class PreprocessHookInstallDialog extends MessageDialog {
    private static final Logger _log = Logger.getLogger( PreprocessHookInstallDialog.class );
    private static boolean _isDialogOn = false;
    private Button _checkBoxButton;

    /**
     * Creates a new dialog
     *
     * @see MessageDialog#MessageDialog(org.eclipse.swt.widgets.Shell, java.lang.String, org.eclipse.swt.graphics.Image,
     *      java.lang.String, int, java.lang.String[], int)
     */
    public PreprocessHookInstallDialog( String title, String message ) {
        super( ContextManager.getActiveWorkbenchShell(), title, null, message, QUESTION, new String[] {
                Messages.PreprocessHookInstallDialogButtonLabel, IDialogConstants.CANCEL_LABEL }, 0 ); // yes is the default
    }

    /**
     * Convenience method to open dialog.
     */
    public static synchronized int openQuestion( String title, String message ) {
        _log.info( "Preprocess hook setting dialog is open" );
        PreprocessHookInstallDialog dialog = new PreprocessHookInstallDialog( title, message );
        int result = dialog.open();
        _log.info( "Preprocess hook setting dialog is closed" );
        return result;
    }

    /**
     * Sets if the dialog is on.
     *
     * @param on
     */
    public static void setIsDialogOn( boolean on ) {
        _isDialogOn = on;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.MessageDialog#createCustomArea(org.eclipse.swt.widgets.Composite)
     */
    protected Control createCustomArea( Composite parent ) {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        _checkBoxButton = new Button( parent, SWT.CHECK );
        _checkBoxButton.setText( Messages.DontAskMeAgainMsg );
        _checkBoxButton.setSelection( !PreprocessorPreferences.getPopForPreprocessHookMissing() );
        return composite;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
     */
    protected void buttonPressed( int buttonId ) {
        PreprocessorPreferences.setPopForPreprocessHookMissing( !_checkBoxButton.getSelection() );
        super.buttonPressed( buttonId );
    }

    /**
     * Checks if the dialog is on or has ever been on. Since it always forces a reset we never need to open more than one.
     *
     * @return
     */
    public static boolean isDialogOn() {
        return _isDialogOn;
    }
}
