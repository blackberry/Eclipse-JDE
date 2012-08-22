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

import net.rim.ejde.internal.launching.DeviceInfo;

/**
 * CleanSimulatorTreeItemExternal
 *
 * @author bkurz
 *
 */
public class CleanSimulatorTreeItemExternal extends CleanSimulatorTreeItem {
    private DeviceInfo _deviceProfile;

    /**
     * Creates an instance of CleanSimulatorTreeItemInternal with the given parameters
     *
     * @param vmInstall
     * @param itemID
     * @param itemName
     */
    public CleanSimulatorTreeItemExternal( DeviceInfo deviceProfile, ItemId itemID, String itemName ) {
        super( ItemType.EXTERNAL_BUNDLE, itemID, itemName );
        _deviceProfile = deviceProfile;
    }

    /**
     * Creates an instance of CleanSimulatorTreeItemInternal with the given parameters
     *
     * @param vmInstall
     * @param itemID
     * @param itemName
     * @param childItems
     */
    public CleanSimulatorTreeItemExternal( DeviceInfo deviceProfile, ItemId itemID, String itemName,
            CleanSimulatorTreeItem[] childItems ) {
        super( ItemType.EXTERNAL_BUNDLE, itemID, itemName, childItems );
        _deviceProfile = deviceProfile;
    }

    /**
     * Returns the BlackBerry SDK associated with this clean simulator tree item
     *
     * @return BlackBerry SDK
     */
    public DeviceInfo getDeviceProfile() {
        return this._deviceProfile;
    }

    /**
     * Sets the BlackBerry SDK associated with this clean simulator tree item
     *
     * @param vmInstall
     */
    public void setDeviceProfile( DeviceInfo deviceProfile ) {
        this._deviceProfile = deviceProfile;
    }
}
