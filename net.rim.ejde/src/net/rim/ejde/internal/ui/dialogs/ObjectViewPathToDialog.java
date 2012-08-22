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

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.ui.CompositeFactory;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ObjectViewPathToDialog extends BasicFilterOptionsDialog implements IConstants {
    private int _path;
    private String _title;
    private Text _txtPath;

    public ObjectViewPathToDialog( Shell shell ) {
        this( shell, EMPTY_STRING );
    }

    public ObjectViewPathToDialog( Shell shell, String title ) {
        super( shell );
        setShellStyle( SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL );
        _title = title;
    }

    public void createPartControl( Composite parent ) {
        getShell().setText( _title );
        Composite composite = CompositeFactory.gridComposite( parent, 2, 3 );
        Label label = new Label( composite, SWT.NONE );
        label.setText( Messages.ObjectViewPathToDialog_FROM_LABEL_TITLE );
        _txtPath = new Text( composite, SWT.BORDER );
        _txtPath.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
    }

    /**
     * Saves path when OK button is pressed.
     *
     * @see Dialog#okPressed()
     */
    protected void okPressed() {
        if( !isInputAvailable() ) {
            MessageDialog.openError( getShell(), _title,
                    NLS.bind( Messages.ObjectViewPathToDialog_INVALID_INPUT_MESSAGE, _txtPath.getText() ) );
            return;
        }
        // call super's method
        super.okPressed();
    }

    private boolean isInputAvailable() {
        String input = _txtPath.getText();
        if( input.trim().startsWith( ADRESS_MARK ) ) {
            // path is something like "@02AC4000", we are going to get ride of
            // the "@"
            input = input.trim().substring( 1 );
        }
        try {
            _path = Integer.parseInt( input, 16 );
            return true;
        } catch( NumberFormatException e ) {
            _path = 0;
            return false;
        }
    }

    public int getPath() {
        return _path;
    }
}
