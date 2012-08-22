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
package net.rim.ejde.internal.ui.widgets.dialog;

/**
 * Change listener used by <code>DialogField</code>
 */
public interface IDialogFieldListener {

    /**
     * The dialog field has changed.
     *
     * @param field
     *            the dialog field that changed
     */
    void dialogFieldChanged( DialogField field );

}
