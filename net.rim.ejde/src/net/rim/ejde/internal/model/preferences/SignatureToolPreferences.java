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

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;

import org.eclipse.jface.preference.IPreferenceStore;

public class SignatureToolPreferences {

    private static IPreferenceStore store = ContextManager.getDefault().getPreferenceStore();
    private static String _storedPassword = IConstants.EMPTY_STRING;

    public static boolean getRunSignatureToolSilently() {
        return store.getBoolean( PreferenceConstants.RUN_SIGNATURE_TOOL_SILENTLY );
    }

    public static void setRunSignatureToolSilently( boolean runSilently ) {
        store.setValue( PreferenceConstants.RUN_SIGNATURE_TOOL_SILENTLY, runSilently );
    }

    public static boolean getDefaultRunSignatureToolSilently() {
        return store.getDefaultBoolean( PreferenceConstants.RUN_SIGNATURE_TOOL_SILENTLY );
    }

    public static boolean getRunSignatureToolAutomatically() {
        return store.getBoolean( PreferenceConstants.RUN_SIGNATURE_TOOL_AUTOMATICALLY );
    }

    public static void setRunSignatureToolAutomatically( boolean runAutomatically ) {
        store.setValue( PreferenceConstants.RUN_SIGNATURE_TOOL_AUTOMATICALLY, runAutomatically );
    }

    public static boolean getDefaultRunSignatureToolAutomatically() {
        return store.getDefaultBoolean( PreferenceConstants.RUN_SIGNATURE_TOOL_AUTOMATICALLY );
    }

    public static String getCachedPassword() {
        return _storedPassword;
    }

    public static boolean isPasswordCached() {
        return !_storedPassword.equals( IConstants.EMPTY_STRING );
    }

    public static void clearCachedPassword() {
        _storedPassword = IConstants.EMPTY_STRING;
    }

    public static void setCachedPassword( String newPassword ) {
        _storedPassword = newPassword;
    }
}
