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

/**
 * Constant definitions for plug-in preferences
 */
public interface PreferenceConstants {

    // Code Signing preferences
    public static final String WARN_ABOUT_CODESIGN_MSG = "codeSign-";

    public static final String RUN_SIGNATURE_TOOL_SILENTLY = "RunSignatureToolSilently";

    public static final String RUN_SIGNATURE_TOOL_AUTOMATICALLY = "RunSignatureToolAutomatically";
    // Debug preferences
    public static final String PROMPT_FOR_MISSING_DEBUG_FILES = "promptForMissingDebugFiles";
    public static final String DOWNLOAD_DEBUG_FILES = "downloadDebugFiles";
    public static final int DOWNLOAD_DEBUG_FILES_PROMPT = 0;
    public static final int DOWNLOAD_DEBUG_FILES_YES = 1;
    public static final int DOWNLOAD_DEBUG_FILES_NO = 2;

    // General preferences
    public static final String DEFAULT_PROJECT_VERSION = "default_projectVersion";

    public static final String DEFAULT_PROJECT_VENDOR = "default_projectVendor";

    // Open App descriptor on new BlackBerry project
    public static final String OPEN_APP_DESCRIPTOR_ON_NEW_PROJECT = "openAppDescriptorOnNew";

    // Open startup page on new or imported BlackBerry project
    public static final String OPEN_STARTUP_PAGE_ON_NEW_PROJECT = "openStartupOnNew";

    // Notify for new update version
    public static final String UPDATE_NOTIFY = "updateNotify";

    // Notify for new update version url
    public static final String UPDATE_NOTIFY_URL = "updateNotifyURL";

    // Pre-processor preferences
    public static final String PREPROCESSOR_DEFINE_LIST = "preprocessorDefines";

    // Pop for preprocess hook missing
    public static final String POP_FOR_PREPROCESS_HOOK_MISSING = "popForPreprocessHookMissing";

    // Pop for Microsoft VC missing
    public static final String POP_FOR_MISSING_VC = "popForMissingVC";

    // BlackBerry Runtime Preferences
    public static final String BB_DEFAULT_RUNTIME_ID = "defaultRuntimeID";

    // profiler view preferences
    public static final String NET_RIM_EJDE_UI_VIEWS_METHOD_TIME_TYPE = "net.rim.ejde.ui.views.methodattribution";

    public static final String NET_RIM_EJDE_UI_VIEWS_WHATTOPROFILE = "net.rim.ejde.ui.views.whattoprofile";

    public static final String NET_RIM_EJDE_UI_VIEWS_SHOW_GROUP_MEMBER = "net.rim.ejde.ui.views.show.group.member";

    public static final String NET_RIM_EJDE_UI_VIEWS_SHOW_RECURSIVE_SIZE = "net.rim.ejde.ui.views.show.recursive.size";

    // process view preferences
    public static final String NET_RIM_EJDE_UI_VIEWS_LIVE_UPDATE = "net.rim.ejde.ui.views.liveupdate";

    public static final String CLEAN_SIMULATOR_PREFERENCE_PREFIX = "cleanSimulator-";

    // Open Startup page upon first-time Eclipse launch
    public static final String OPEN_STARTUP_PAGE_ON_ECLPSE_FIRST_START = "openStartupOnEclpseFirstStart";

    public static final String APPEND_CONSOLE_LOG_TO_FILE = "appendConsoleLogToFile";

    public static final String CONSOLE_LOG_FILE = "consoleLogFile";

    public static final String DEBUG_FILE_SERVER_URL = "debugFileServerUrl";
}
