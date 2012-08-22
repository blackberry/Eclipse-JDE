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

import org.eclipse.jdt.launching.IVMInstall;

/**
 * CleanSimulatorTreeItemInternal
 *
 * @author bkurz
 *
 */
public class CleanSimulatorTreeItemInternal extends CleanSimulatorTreeItem {
    private IVMInstall _vmInstall;

    /**
     * Creates an instance of CleanSimulatorTreeItemInternal with the given parameters
     *
     * @param vmInstall
     * @param itemID
     * @param itemName
     */
    public CleanSimulatorTreeItemInternal( IVMInstall vmInstall, ItemId itemID, String itemName ) {
        super( ItemType.INTERNAL_BUNDLE, itemID, itemName );
        _vmInstall = vmInstall;
    }

    /**
     * Creates an instance of CleanSimulatorTreeItemInternal with the given parameters
     *
     * @param vmInstall
     * @param itemID
     * @param itemName
     * @param childItems
     */
    public CleanSimulatorTreeItemInternal( IVMInstall vmInstall, ItemId itemID, String itemName,
            CleanSimulatorTreeItem[] childItems ) {
        super( ItemType.INTERNAL_BUNDLE, itemID, itemName, childItems );
        _vmInstall = vmInstall;
    }

    /**
     * Returns the BlackBerry SDK associated with this clean simulator tree item
     *
     * @return BlackBerry SDK
     */
    public IVMInstall getVMInstall() {
        return this._vmInstall;
    }

    /**
     * Sets the BlackBerry SDK associated with this clean simulator tree item
     *
     * @param vmInstall
     */
    public void setVMInstall( IVMInstall vmInstall ) {
        this._vmInstall = vmInstall;
    }
}
