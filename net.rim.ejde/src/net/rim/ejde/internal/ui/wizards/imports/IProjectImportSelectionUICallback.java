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
package net.rim.ejde.internal.ui.wizards.imports;

import org.eclipse.jdt.launching.IVMInstall;

/**
 * An implementation of this callback is used by {@link ProjectImportSelectionUI} to communicate with the class which uses
 * {@link ProjectImportSelectionUI}.
 *
 *
 */
public interface IProjectImportSelectionUICallback {

    /**
     * Set message.
     *
     * @param message
     * @param type
     */
    public void setMessage( String message, int type );

    /**
     * Sets whether the ProjceImportSelectionUI is complete (no error).
     *
     * @param complete
     *            <code>true</code> if this UI is complete without any error, and and <code>false</code> otherwise
     */
    public void setComplete( boolean complete );

    /**
     * Gets the installed JVM.
     *
     * @return
     */
    public IVMInstall getSelectedJVM();
}
