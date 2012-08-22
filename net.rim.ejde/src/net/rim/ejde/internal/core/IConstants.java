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
package net.rim.ejde.internal.core;

import net.rim.ide.OSUtils;

public interface IConstants {

    final static String VERSION_MF_ATTRIBUTE_STRING = "Specification-Version"; //$NON-NLS-1$

    // Display levels
    final static int PROFILE_DATA_LEVEL = 5;

    final static int PROFILE_MODULE_LEVEL = 4;

    final static int PROFILE_METHOD_LEVEL = 3;

    final static int PROFILE_LINE_LEVEL = 2;

    final static int PROFILE_BYTECODE_LEVEL = 1;

    final static int UNREGOINZED_LEVEL = 0;

    // Preferences.ini constants
    final static String PROJECT_BUILD_EXTRA_STEPS = "project_build_extra_steps"; //$NON-NLS-1$

    final static String RIM_PREF_PAGE_ACCESSED = "rim.pref.page.accessed"; //$NON-NLS-1$

    final static String PROJECT_OUTPUT_FOLDER_PREFIX_KEY = "project_output_folder_prefix";

    final static String GENERATE_ALX_FILE_KEY = "default_generate_alx_file";

    final static String BB_MODEL_VERSION = "default_model_version";

    final static String PROJECT_OUTPUT_FILE_REPLACE_CHAR = "project_output_file_replace_char";

    final static String PROJECT_STARTUP_TIER_KEY = "default_startup_tier";

    final static String VMTOOLS_LOCATION_KEY = "bundle_vmTools_location";

    final static String BUNDLE_DATA_LOCATION_KEY = "bundle_data_location";

    final static String JRE_DIRECTIVE_PREFIX_KEY = "JRE_directive_prefix";

    final static String PROMPT_FOR_BUILD_KEY = "prompt_for_build";

    final static String VISIBLE_HOMESCREENPOSITION_KEY = "default_visible_homeScreenPosition";

    final static String VISIBLE_CLEAN_SIMULATOR_KEY = "default_visible_cleanSimulator";

    final static String PACKAGE_EXPORTED_JAR = "package_exported_jar";

    final static String DEFAULT_DEBUG_FILE_SERVER_URL = "https://developer.blackberry.com/BBDebugFiles/rest/debug";

    // File Extensions
    final static String CRB_EXTENSION_WITH_DOT = ".crb"; //$NON-NLS-1$

    final static String CRB_EXTENSION = "crb"; //$NON-NLS-1$

    final static String CSV_EXTENSION = "csv"; //$NON-NLS-1$

    final static String JDW_EXTENSION = "jdw"; //$NON-NLS-1$

    final static String JDP_EXTENSION = "jdp"; //$NON-NLS-1$

    final static String JDP_EXTENSION_WITH_DOT = ".jdp"; //$NON-NLS-1$

    final static String JAR_EXTENSION = "jar"; //$NON-NLS-1$

    final static String JAR_EXTENSION_WITH_DOT = ".jar";

    final static String NEED_CLEAN_WORKSPACE_KEY = "need_clean_workspace";

    final static String JAVA_EXTENSION = "java"; //$NON-NLS-1$

    final static String JAVA_EXTENSION_WITH_DOT = ".java"; //$NON-NLS-1$

    final static String RRH_FILE_EXTENSION = "rrh"; //$NON-NLS-1$

    final static String RRH_FILE_EXTENSION_WITH_DOT = ".rrh"; //$NON-NLS-1$

    final static String RRC_FILE_EXTENSION = "rrc"; //$NON-NLS-1$

    final static String RRC_FILE_EXTENSION_WITH_DOT = ".rrc"; //$NON-NLS-1$

    final static String RAPC_FILE_EXTENSION = "rapc"; //$NON-NLS-1$

    final static String RAPC_FILE_EXTENSION_WITH_DOT = ".rapc"; //$NON-NLS-1$

    final static String COD_FILE_EXTENSION = "cod"; //$NON-NLS-1$

    final static String CSO_FILE_EXTENSION = "cso"; //$NON-NLS-1$

    final static String CSL_FILE_EXTENSION = "csl"; //$NON-NLS-1$

    final static String CSO_FILE_EXTENSION_WITH_DOT = ".cso"; //$NON-NLS-1$

    final static String DEBUG_FILE_EXTENSION = "debug"; //$NON-NLS-1$

    final static String DEBUG_FILE_EXTENSION_WITH_DOT = ".debug"; //$NON-NLS-1$

    final static String LST_FILE_EXTENSION = "lst"; //$NON-NLS-1$

    final static String WTS_FILE_EXTENSION = "wts"; //$NON-NLS-1$

    final static String JAD_FILE_EXTENSION = "jad"; //$NON-NLS-1$

    final static String JAD_FILE_EXTENSION_WITH_DOT = ".jad"; //$NON-NLS-1$

    final static String ERROR_FILE_EXTENSION = "err"; //$NON-NLS-1$

    final static String CLASS_FILE_EXTENSION = "class"; //$NON-NLS-1$

    final static String CLASS_FILE_EXTENSION_WITH_DOT = ".class"; //$NON-NLS-1$

    final static String ALX_FILE_EXTENSION = "alx"; //$NON-NLS-1$

    final static String ALX_FILE_EXTENSION_WITH_DOT = ".alx"; //$NON-NLS-1$

    final static String COD_FILE_EXTENSION_WITH_DOT = ".cod"; //$NON-NLS-1$

    final static String CSL_FILE_EXTENSION_WITH_DOT = ".csl"; //$NON-NLS-1$

    final static String KEY_FILE_EXTENSION = "key"; //$NON-NLS-1$

    // File Names
    final static String JDWP_FILE_NAME = "JDWP.jar"; //$NON-NLS-1$

    final static String RESOURCE_INTERFACE_SUFFIX = "Resource.java"; //$NON-NLS-1$

    final static String CLASSPATH_FILE_NAME = ".classpath"; //$NON-NLS-1$

    final static String PROJECT_FILE_NAME = ".project"; //$NON-NLS-1$

    final static String RIM_API_JAR = "net_rim_api.jar"; //$NON-NLS-1$

    final static String DEFAULT_BUILD_FILE_NAME = "DefaultBuild.rc"; //$NON-NLS-1$

    final static String PREVERIFY_FILE_NAME = OSUtils.isWindows() ? "preverify.exe" : "preverify"; //$NON-NLS-1$

    final static String RAPC_FILE_NAME = OSUtils.isWindows() ? "rapc.exe" : "rapc.jar"; //$NON-NLS-1$

    final static String RUNTIME_FILE_NAME = "Runtime.rc"; //$NON-NLS-1$

    final static String SIGNATURE_TOOL_FILE_NAME = "SignatureTool.jar"; //$NON-NLS-1$

    final static String JAVA_LOADER_FILE_NAME = OSUtils.isWindows() ? "JavaLoader.exe" : "javaloader"; //$NON-NLS-1$

    final static String FLEDGE_FILE_NAME = OSUtils.isWindows() ? "fledge.exe" : "fledge"; //$NON-NLS-1$

    final static String FLEDGE_HOOK_FILE_NAME = OSUtils.isWindows() ? "FledgeHook.exe" : ""; //$NON-NLS-1$

    final static String FLEDGE_HOOK_DLL_FILE_NAME = OSUtils.isWindows() ? "FledgeHook.dll" : ""; //$NON-NLS-1$

    final static String CP_DLL_1_FILE_NAME = OSUtils.isWindows() ? "RIMIDEWin32Util.dll" : ""; //$NON-NLS-1$

    final static String CP_DLL_2_FILE_NAME = OSUtils.isWindows() ? "RimUsbJni.dll" : ""; //$NON-NLS-1$

    final static String CP_JNI_LIB_2_FILE_NAME = "libRIMUsbJni.jnilib"; //$NON-NLS-1$

    final static String CSK_FILE_NAME = "sigtool.csk"; //$NON-NLS-1$

    final static String DB_FILE_NAME = "sigtool.db"; //$NON-NLS-1$

    // Folder Names
    final static String BIN_FOLD_NAME = "bin"; //$NON-NLS-1$

    final static String LIB_FOLDER_NAME = "lib"; //$NON-NLS-1$

    final static String SETTING_FOLD_NAME = ".settings"; //$NON-NLS-1$

    // VM and CP constants
    final static String BLACKBERRY_JRE_PREFIX = "BlackBerry JRE"; //$NON-NLS-1$

    final static String BLACKBERRY_EXECUTION_ENV = "BlackBerry Execution Environment"; //$NON-NLS-1$

    final static String BB_VM_ID = "net.rim.ejde.BlackBerryVMInstallType"; //$NON-NLS-1$

    final static String BB_VM_NAME = "BlackBerry Execution Environment VM"; //$NON-NLS-1$

    final static String EE_FILE_NAME = "BlackBerry.ee"; //$NON-NLS-1$

    final static String EE_FILE_LOCATION = "components"; //$NON-NLS-1$

    final static String CP_EXTENSION_POINT_ID = "net.rim.ejde.componentpack"; //$NON-NLS-1$

    // Signature Tool Parameters

    final static String JAR_CMD = "-jar"; //$NON-NLS-1$

    final static String LOOK_AND_FEEL_CMD = "-Dswing.defaultlaf="; //$NON-NLS-1$

    final static String SIGTOOL_PASSWORD = "-p";

    final static String SIGTOLL_AUTOMATIC = "-a";

    final static String SIGTOLL_AUTO_CLOSE = "-c";

    // Other strings
    final static String QUOTE_MARK = "'"; //$NON-NLS-1$

    final static String EQUAL_MARK = "="; //$NON-NLS-1$

    final static String NEWLINE = System.getProperty( "line.separator" ); //$NON-NLS-1$

    final static String EMPTY_STRING = ""; //$NON-NLS-1$

    final static String ONE_BLANK_STRING = " "; //$NON-NLS-1$

    final static String WILDCAST_MARK = "*"; //$NON-NLS-1$

    final static String DOT_MARK = "."; //$NON-NLS-1$

    final static String UNDERSCORE_STRING = "_"; //$NON-NLS-1$

    final static char START_CHAR = '*';

    final static char DOT_CHAR = '.';

    final static char FORWARD_SLASH_CHAR = '/';

    final static char BACK_SLASH_CHAR = '/';

    final static String COMMA_MARK = ","; //$NON-NLS-1$

    final static String WILD_CAST_STRING = ".*"; //$NON-NLS-1$

    final static String SEMICOLON_MARK = ";"; //$NON-NLS-1$

    final static String COLON_MARK = ":"; //$NON-NLS-1$

    final static String ADRESS_MARK = "@"; //$NON-NLS-1$

    final static String DOLLAR_MARK = "$"; //$NON-NLS-1$

    final static String DOUBLE_DOTS = ".."; //$NON-NLS-1$

    final static String DOUBLE_QUOTE = "\""; //$NON-NLS-1$

    final static String FORWARD_SLASH_MARK = "/"; //$NON-NLS-1$

    final static String BACK_SLASH_MARK = "/"; //$NON-NLS-1$

    final static String DOT_QUOTE_STRING = "\\."; //$NON-NLS-1$

    final static int DEFAULT_DISPLAY_NUMBER = 1000;

    final static String PATH_SEPARATE_MARK = OSUtils.isWindows() ? "\\" : "/"; //$NON-NLS-1$

    final static String NONE_CLASSPATH_STRING = "<none>"; //$NON-NLS-1$

    final static String CHANG_LINE_STRING = "\r\n"; //$NON-NLS-1$

    final static String PP_VALIDATION_REG_EX = "($\\{)?[a-zA-Z_]{1,63}[-a-zA-Z_0-9.]{0,63}[\\}]?"; //$NON-NLS-1$

    final static String JAVA_HOME_PROPERTY = "java.home"; //$NON-NLS-1$

    final static String JAVA_CMD = OSUtils.isWindows() ? "java.exe" : "java"; //$NON-NLS-1$

    final static String LC_LIBRARIES = "Libraries"; //$NON-NLS-1$

    final static String LC_MULTIPROJECTS = "MultiProjects"; //$NON-NLS-1$

    static final String START_UP_PAGE = "/intro/startup/index.html"; //$NON-NLS-1$

    static final String START_UP_FOLDER = "/intro/startup"; //$NON-NLS-1$

    static final String BROWSER_ID = "startupPage"; //$NON-NLS-1$

    //static final String DOC_PLUGIN_ID = OSUtils.isWindows() ? "net.rim.ejde.doc" : "net.rim.ejde.doc.mac"; //$NON-NLS-1$ //$NON-NLS-2$

    static final String PREPROCESSING_HOOK_FRGMENT_ID = "net.rim.ejde.preprocessing.hook"; //$NON-NLS-1$

    static final String HTML_PAGE_FOLDER = "/html"; //$NON-NLS-1$

    final static String VM_VERSION = "version";

    final static String VERSION_PROPERTY_FILE_NAME = "version.properties";

    final static String DEFAULT_VM_VERSION = "0.0.0.0";

    final static String DEFAULT_VM_OUTPUT_FOLDER_NAME = "0.0.0";

    static final String SIMULATOR_FOLDER_NAME = "simulator";

    static final String SIMULATOR_MANIFEST_FILE_NAME = "_manifest";

    static final public String HEADVER_VM_VERSION = "999.999.999";

    static final public String HEADVER_VM_OUTPUTFOLDER = "headver";

    static final String SIMULATOR_DMP_FILE_EXTENSION = "dmp";

    static final String SDK_FIVE_VERSION = "5.0.0.0";

    static final String SDK_SIX_VERSION = "6.0.0.0";

    static final String OLD_VMTOOLS_LOCATION = "signTool";

    static final String LC_FILTERING_SET_KEY = "LC_filtering_set";

    static final String FULL_JAD_FILE_SUFFIX = "_full";
}
