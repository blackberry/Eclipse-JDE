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

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PromptDialog;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class SignatureToolPasswordPromptDialog extends PromptDialog {

    public SignatureToolPasswordPromptDialog( Shell parentShell, String initialAnswer ) {
        super( parentShell, Messages.SignatureToolPasswordPromptDialog_DialogTitleMsg, initialAnswer, true );
    }

    @Override
    protected void buttonPressed( int buttonId ) {
        if( buttonId == IDialogConstants.OK_ID ) {
            if( _promptText.getText().trim().length() < 8 ) {
                ErrorDialog.openError( getParentShell(), Messages.SignatureToolPasswordPromptDialog_ErrorDialogTitleMsg, null,
                        new Status( IStatus.ERROR, ContextManager.PLUGIN_ID,
                                Messages.SignatureToolPasswordPromptDialog_ErrorDialogMsg ) );
                return;
            }
        }
        super.buttonPressed( buttonId );
    }

    @Override
    protected void configureShell( Shell shell ) {
        shell.setSize( 500, 180 );
        super.configureShell( shell );
    }

    @Override
    protected Control createCustomArea( Composite parent ) {
        Composite directParent = (Composite) super.createCustomArea( parent );
        return directParent;
    }
}
