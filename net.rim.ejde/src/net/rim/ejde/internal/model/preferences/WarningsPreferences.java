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

import java.util.Map;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;

import org.eclipse.jface.preference.IPreferenceStore;

public class WarningsPreferences {
    static IPreferenceStore store = ContextManager.getDefault().getPreferenceStore();

    public static boolean getPromptForMissingDebugFiles() {
        return store.getBoolean( PreferenceConstants.PROMPT_FOR_MISSING_DEBUG_FILES );
    }

    public static void setPromptForMissingDebugFiles( boolean prompt ) {
        store.setValue( PreferenceConstants.PROMPT_FOR_MISSING_DEBUG_FILES, prompt );
    }

    public static boolean getDefaultPromptForMissingDebugFiles() {
        return store.getDefaultBoolean( PreferenceConstants.PROMPT_FOR_MISSING_DEBUG_FILES );
    }

    public static boolean getPromptForMissingDependenciesFiles() {
        return store.getBoolean( PreferenceConstants.POP_FOR_MISSING_VC );
    }

    public static void setPromptForMissingDependenciesFiles( boolean prompt ) {
        store.setValue( PreferenceConstants.POP_FOR_MISSING_VC, prompt );
    }

    public static boolean getDefaultPromptForMissingDependenciesFiles() {
        return store.getDefaultBoolean( PreferenceConstants.POP_FOR_MISSING_VC );
    }

    public static Map< String, Boolean > getCodeSignWarnStatus( Map< String, Boolean > statusTable ) {
        boolean keyStatus = false;
        for( String key : statusTable.keySet() ) {
            keyStatus = store.getBoolean( PreferenceConstants.WARN_ABOUT_CODESIGN_MSG + key );
            statusTable.put( key, Boolean.valueOf( keyStatus ) );
        }
        return statusTable;
    }

    public static void setCodeSignWarnStatus( Map< String, Boolean > statusTable ) {
        for( String key : statusTable.keySet() ) {
            store.setValue( PreferenceConstants.WARN_ABOUT_CODESIGN_MSG + key, statusTable.get( key ).booleanValue() );
        }
    }

    public static void setDefaultCodeSignWarnStatus( Map< String, Boolean > statusTable ) {
        boolean defaultValue = store.getDefaultBoolean( PreferenceConstants.WARN_ABOUT_CODESIGN_MSG );
        for( String key : statusTable.keySet() ) {
            statusTable.put( key, Boolean.valueOf( defaultValue ) );
        }
    }

    public static boolean getWarnStatus( String keyLabel ) {
        return store.getBoolean( PreferenceConstants.WARN_ABOUT_CODESIGN_MSG + keyLabel );
    }

    public static void setWarnStatus( String keyLabel, boolean status ) {
        store.setValue( PreferenceConstants.WARN_ABOUT_CODESIGN_MSG + keyLabel, status );
    }
}
