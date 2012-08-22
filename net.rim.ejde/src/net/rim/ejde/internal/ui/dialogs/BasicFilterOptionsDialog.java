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

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * The basic filter and options dialog use in RIM IDE plug-in.
 */
abstract public class BasicFilterOptionsDialog extends TrayDialog {

    /**
     * Constructs an instance of BasicFilterOptionsDialog.
     *
     * @param shell
     */
    public BasicFilterOptionsDialog( Shell shell ) {
        super( shell );
    }

    protected Control createDialogArea( Composite parent ) {
        Composite composite = (Composite) super.createDialogArea( parent );
        createPartControl( parent );
        return composite;
    }

    public boolean isHelpAvailable() {
        return false;
    }

    /**
     * Customize the UI controls of the dialog. This method is supposed to be overridden by the sub-classes instead of overriding
     * the <code>createDialogArea(Composite)</code> method.
     *
     * @param parent
     */
    abstract public void createPartControl( Composite parent );
}
