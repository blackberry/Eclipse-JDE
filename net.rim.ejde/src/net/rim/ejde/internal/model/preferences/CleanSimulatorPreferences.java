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
package net.rim.ejde.internal.model.preferences;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.ui.dialogs.CleanSimulatorTreeItem;
import net.rim.ejde.internal.ui.dialogs.CleanSimulatorTreeItem.ItemType;
import net.rim.ejde.internal.ui.dialogs.CleanSimulatorTreeItemExternal;
import net.rim.ejde.internal.ui.dialogs.CleanSimulatorTreeItemInternal;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 *
 * @author bkurz
 *
 */
public class CleanSimulatorPreferences {
    static IPreferenceStore store = ContextManager.getDefault().getPreferenceStore();

    public static List< CleanSimulatorTreeItem > getCleanSimulatorPreferences( List< CleanSimulatorTreeItem > inputItems ) {
        for( CleanSimulatorTreeItem item : inputItems ) {
            if( item.hasChildItems() ) {
                getCleanSimulatorPreferences( Arrays.asList( item.getChildItems() ) );
            }

            item.setChecked( store.getBoolean( PreferenceConstants.CLEAN_SIMULATOR_PREFERENCE_PREFIX + getSDKName( item ) + "-"
                    + item.getItemID() ) );
        }
        return inputItems;
    }

    /**
     *
     * @param items
     */
    public static void setCleanSimulatorPreferences( List< CleanSimulatorTreeItem > items ) {
        Map< String, Boolean > preferenceTable = new HashMap< String, Boolean >();
        preferenceTable = createCleanSimulatorPreferenceTable( items, preferenceTable );
        for( String key : preferenceTable.keySet() ) {
            store.setValue( PreferenceConstants.CLEAN_SIMULATOR_PREFERENCE_PREFIX + key, preferenceTable.get( key ) );
        }
    }

    private static Map< String, Boolean > createCleanSimulatorPreferenceTable( List< CleanSimulatorTreeItem > items,
            Map< String, Boolean > preferenceTable ) {
        for( CleanSimulatorTreeItem item : items ) {
            if( item.hasChildItems() ) {
                createCleanSimulatorPreferenceTable( Arrays.asList( item.getChildItems() ), preferenceTable );
            }
            preferenceTable.put( getSDKName( item ) + "-" + item.getItemID(), item.isChecked() );
        }
        return preferenceTable;
    }

    private static String getSDKName( CleanSimulatorTreeItem item ) {
        if( item.getItemType().equals( ItemType.INTERNAL_BUNDLE ) ) {
            CleanSimulatorTreeItemInternal internalItem = (CleanSimulatorTreeItemInternal) item;
            return internalItem.getVMInstall().getName();
        } else if( item.getItemType().equals( ItemType.EXTERNAL_BUNDLE ) ) {
            CleanSimulatorTreeItemExternal externalItem = (CleanSimulatorTreeItemExternal) item;
            return externalItem.getDeviceProfile().getBundleName();
        }
        return IConstants.EMPTY_STRING;
    }
}
