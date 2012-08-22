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
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;

import org.eclipse.jface.preference.IPreferenceStore;

public class RootPreferences {
    static IPreferenceStore store = ContextManager.getDefault().getPreferenceStore();

    public static String getProjectVersion() {
        return store.getString( PreferenceConstants.DEFAULT_PROJECT_VERSION );
    }

    public static String getDefaultProjectVersion() {
        return store.getDefaultString( PreferenceConstants.DEFAULT_PROJECT_VERSION );
    }

    public static void setProjectVersion( String projectVersion ) {
        store.setValue( PreferenceConstants.DEFAULT_PROJECT_VERSION, projectVersion );
    }

    public static String getProjectVendor() {
        return store.getString( PreferenceConstants.DEFAULT_PROJECT_VENDOR );
    }

    public static String getDefaultProjectVendor() {
        return store.getDefaultString( PreferenceConstants.DEFAULT_PROJECT_VENDOR );
    }

    public static void setProjectVendor( String projectVendor ) {
        store.setValue( PreferenceConstants.DEFAULT_PROJECT_VENDOR, projectVendor );
    }

    public static boolean getOpenStartupOnNew() {
        return store.getBoolean( PreferenceConstants.OPEN_STARTUP_PAGE_ON_NEW_PROJECT );
    }

    public static boolean getDefaultOpenStartupOnNew() {
        return store.getDefaultBoolean( PreferenceConstants.OPEN_STARTUP_PAGE_ON_NEW_PROJECT );
    }

    public static void setOpenStartupOnNew( boolean openStartupOnNew ) {
        store.setValue( PreferenceConstants.OPEN_STARTUP_PAGE_ON_NEW_PROJECT, openStartupOnNew );
    }

    public static boolean getOpenAppDescriptorOnNew() {
        return store.getBoolean( PreferenceConstants.OPEN_APP_DESCRIPTOR_ON_NEW_PROJECT );
    }

    public static boolean getDefaultOpenAppDescriptorOnNew() {
        return store.getDefaultBoolean( PreferenceConstants.OPEN_APP_DESCRIPTOR_ON_NEW_PROJECT );
    }

    public static boolean getDefaultUpdateNotify() {
        return store.getDefaultBoolean( PreferenceConstants.UPDATE_NOTIFY );
    }

    public static boolean getUpdateNotify() {
        return store.getBoolean( PreferenceConstants.UPDATE_NOTIFY );
    }

    public static void setUpdateNotify( boolean notifyUpdate ) {
        store.setValue( PreferenceConstants.UPDATE_NOTIFY, notifyUpdate );
    }

    public static void setOpenAppDescriptorOnNew( boolean openAppDescriptorOnNew ) {
        store.setValue( PreferenceConstants.OPEN_APP_DESCRIPTOR_ON_NEW_PROJECT, openAppDescriptorOnNew );
    }

    public static boolean getOpenStartupPageOnEclipseStart() {
        return store.getBoolean( PreferenceConstants.OPEN_STARTUP_PAGE_ON_ECLPSE_FIRST_START );
    }

    public static boolean getDefaultOpenStartupPageOnEclipseStart() {
        return store.getDefaultBoolean( PreferenceConstants.OPEN_STARTUP_PAGE_ON_ECLPSE_FIRST_START );
    }

    public static void setOpenStartupPageOnEclipseStart( boolean openStartupPage ) {
        store.setValue( PreferenceConstants.OPEN_STARTUP_PAGE_ON_ECLPSE_FIRST_START, openStartupPage );
    }

    public static boolean getAppendConsoleLogToFile() {
        return store.getBoolean( PreferenceConstants.APPEND_CONSOLE_LOG_TO_FILE );
    }

    public static boolean getDefaultAppendConsoleLogToFile() {
        return store.getDefaultBoolean( PreferenceConstants.APPEND_CONSOLE_LOG_TO_FILE );
    }

    public static void setAppendConsoleLogToFile( boolean appendLog ) {
        store.setValue( PreferenceConstants.APPEND_CONSOLE_LOG_TO_FILE, appendLog );
    }

    public static String getConsoleLogFile() {
        return store.getString( PreferenceConstants.CONSOLE_LOG_FILE );
    }

    public static String getDefaultConsoleLogFile() {
        return store.getDefaultString( PreferenceConstants.CONSOLE_LOG_FILE );
    }

    public static void setConsoleLogFile( String logFile ) {
        store.setValue( PreferenceConstants.CONSOLE_LOG_FILE, logFile );
    }

    public static int getDownloadDebugFilesOption() {
        return store.getInt( PreferenceConstants.DOWNLOAD_DEBUG_FILES );
    }

    public static void setDownloadDebugFilesOption( int option ) {
        store.setValue( PreferenceConstants.DOWNLOAD_DEBUG_FILES, option );
    }

    public static int getDefaultDownloadDebugFilesOption() {
        return store.getDefaultInt( PreferenceConstants.DOWNLOAD_DEBUG_FILES );
    }
}
