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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PromptDialog extends MessageDialog {
    protected Text _promptText;
    private boolean _obscure = false;
    private String _response;
    private String _initialAnswer;

    public PromptDialog( Shell parentShell, String title, String initialAnswer, boolean obscure ) {
        super( parentShell, title, null, title + ":", QUESTION, new String[] { IDialogConstants.OK_LABEL,
                IDialogConstants.CANCEL_LABEL }, 0 );
        _obscure = obscure;
        _response = null;
        _initialAnswer = initialAnswer;
    }

    /**
     * Overridden to create the password textboxes on the given parent composite to enable the user to supply credentials.
     *
     * @see org.eclipse.jface.dialogs.MessageDialog#createCustomArea(org.eclipse.swt.widgets.Composite)
     */
    protected Control createCustomArea( Composite parent ) {
        GridLayout gridLayout = (GridLayout) parent.getLayout();
        gridLayout.numColumns = 1;

        // Label lblPassword = new Label( parent, SWT.NONE );
        // lblPassword.setText( _promptLabel );

        int style = SWT.BORDER;
        if( _obscure ) {
            style |= SWT.PASSWORD;
        }
        _promptText = new Text( parent, style );
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace = true;
        _promptText.setLayoutData( data );

        _promptText.setText( _initialAnswer );
        _promptText.setFocus();

        return parent;

    }

    @Override
    protected void buttonPressed( int buttonId ) {
        if( buttonId == IDialogConstants.OK_ID ) {
            _response = _promptText.getText();
        }
        super.buttonPressed( buttonId );
    }

    public String getResponse() {
        return _response;
    }

}
