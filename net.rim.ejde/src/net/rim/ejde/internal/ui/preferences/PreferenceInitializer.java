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
package net.rim.ejde.internal.ui.preferences;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.ui.views.profiler.ProfilingViewOptionsDialog;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer implements PreferenceConstants {

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
     */
    public void initializeDefaultPreferences() {
        IPreferenceStore store = ContextManager.getDefault().getPreferenceStore();
        // store.setDefault( DEFAULT_PROJECT_VENDOR, "RIM");
        // store.setDefault( DEFAULT_PROJECT_VERSION, "1.0.0");
        // store.setDefault(PREPROCESSOR_DEFINE_LIST, "RIM_TAG1|false,RIM_TAG2|false");
        store.setDefault( PREPROCESSOR_DEFINE_LIST, "" );
        store.setDefault( POP_FOR_PREPROCESS_HOOK_MISSING, true );
        store.setDefault( PROMPT_FOR_MISSING_DEBUG_FILES, true );
        store.setDefault( WARN_ABOUT_CODESIGN_MSG, false );
        store.setDefault( RUN_SIGNATURE_TOOL_SILENTLY, false );
        store.setDefault( OPEN_APP_DESCRIPTOR_ON_NEW_PROJECT, true );
        store.setDefault( OPEN_STARTUP_PAGE_ON_NEW_PROJECT, true );
        store.setDefault( UPDATE_NOTIFY, true );
        // signature tool
        store.setDefault( RUN_SIGNATURE_TOOL_SILENTLY, true );
        store.setDefault( RUN_SIGNATURE_TOOL_AUTOMATICALLY, true );
        store.setDefault( NET_RIM_EJDE_UI_VIEWS_WHATTOPROFILE, ProfilingViewOptionsDialog.PROFILE_TYPE_SAMPLE );
        store.setDefault( POP_FOR_MISSING_VC, true );
        store.setDefault( OPEN_STARTUP_PAGE_ON_ECLPSE_FIRST_START, false );

        store.setDefault( DOWNLOAD_DEBUG_FILES, PreferenceConstants.DOWNLOAD_DEBUG_FILES_PROMPT );
        store.setDefault( DEBUG_FILE_SERVER_URL, IConstants.DEFAULT_DEBUG_FILE_SERVER_URL );
    }
}
