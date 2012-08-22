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
package net.rim.ejde.internal.launching;

/**
 * Interface that defines common attribute constants for EJDE launch configurations
 *
 * @author bchabot
 *
 */
public interface IFledgeLaunchConstants {

    /**
     * Name of launch attribute that stores list of deployable projects in the launch configuration
     */
    public static final String PROJECTS = "Projects"; //$NON-NLS-1$

    /** The id of a RIM aka BlackBerry Simulator launch config type */
    public static final String LAUNCH_CONFIG_ID = "net.rim.ejde.launching.FledgeLaunchConfiguration"; //$NON-NLS-1$

    /** The profile used to launch */
    public static final String EJDE_PROFILE_NAME = "ejdeprofile"; //$NON-NLS-1$
    public static final String JDE_PROFILE_NAME = "-JDE"; //$NON-NLS-1$
    public static final String DEFAULT_SIMULATOR_BUNDLE_NAME = "BlackBerry-SDK"; //$NON-NLS-1$

    /** JRE type: project specific or alternate */
    public static final int JRE_TYPE_PROJECT = 0;
    public static final int JRE_TYPE_ALTERNATE = 1;
    public static final int JRE_TYPE_SIMPKG = 2;
    public static int DEFAULT_JRE_TYPE = JRE_TYPE_PROJECT;

    public static String FLEDGE_EXECUTABLE = "fledge.exe";

    // Configuration attributes
    public static final String ATTR_DEPLOYED_PROJECTS = "attr.projects.deployedProjects"; //$NON-NLS-1$
    public static final String ATTR_JRE_ID = "attr.jre.id"; //$NON-NLS-1$
    public static final String ATTR_JRE_TYPE = "attr.jre.type"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_LAUNCH_MDSCS = "attr.general.launchMDSCS"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_LAUNCH_APP_ON_STARTUP = "attr.general.launchAppOnStartup"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_SIM_DIR = "attr.general.simulatorDir"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_CONFIG_FILE = "attr.general.configFile"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_BUNDLE = "attr.general.bundle"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_DEVICE = "attr.general.device"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_AUTOMATICALLY_USE_DEFAULT_VALUE = "attr.general.automaticallyUseDefaultValue"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_NUMBER_OF_SECONDS_WAIT_BEFORE_RESPONSE = "attr.general.numberOfSecondsToWaitBeforeResponse"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_PIN = "attr.general.pin"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_ESN = "attr.general.esn"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_MEID = "attr.general.meid"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_ENABLE_DEVICE_SECURITY = "attr.general.enableDeviceSecurity"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_SYSTEM_LOCALE = "attr.general.deviceLocale"; //$NON-NLS-1$
    public static final String ATTR_GENERAL_KEYBOARD_LOCALE = "attr.general.keyboardLocale"; //$NON-NLS-1$
    public static final String ATTR_DEBUG_INTERRUPT_DEBUGGER_ON_DEADLOCK = "attr.debug.interruptDebuggerOnDeadlock"; //$NON-NLS-1$
    public static final String ATTR_DEBUG_DO_NOT_STOP_EXECUTION = "attr.debug.doNotStopExecution"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_APPLICATION_HEAP_SIZE = "attr.memory.appHeapSize"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_BRANDING_DATA = "attr.memory.brandingData"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_RESET_FILE_SYSTEM_ON_STARTUP = "attr.memory.resetFileSystemOnStartup"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_RESET_NVRAM_ON_STARTUP = "attr.memory.resetNVRAMOnStartup"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_FILE_SYSTEM_SIZE = "attr.memory.fileSystemSize"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_NOT_SAVE_FLASH_ON_EXIT = "attr.memory.notSaveFlashDataOnExit"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_NOT_COMPACT_FILE_SYSTEM_ON_EXIT = "attr.memory.notCompactFSOnExit"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_SIMULATE_SDCARD_INSERTED = "attr.memory.simNoSDInserted"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_FORMAT_SDCARD_ON_STARTUP = "attr.memory.formatSDOnStartup"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_DESTROY_EXISTING_SDCARD_IMAGE = "attr.memory.destroyExistingSDImage"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_SDCARD_SIZE = "attr.memory.sdCardSize"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_SDCARD_IMAGE = "attr.memory.sdCardImage"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_USE_PC_FILESYSTEM_FOR_SDCARD_FILES = "attr.memory.usePCFileForSDFiles"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_PC_FILESYSTEM_PATH = "attr.memory.pcFileSystemPath"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_NOT_SUPPORT_MULTIMEDIA_CARD_SIMULATION = "attr.memory.noMultimediaCardSimulation"; //$NON-NLS-1$
    public static final String ATTR_MEMORY_NOT_SPLIT_MMC_PARTITION_INTO_DIFFERENT_FILES = "attr.memory.noSplitMMCPartitionIntoDifferentFiles"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_DISABLE_REGISTRATION = "attr.network.disableRegistration"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_NETWORKS = "attr.network.networks"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_START_WITH_RADIO_OFF = "attr.network.startWithRadioOff"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_PHONE_NUMBERS = "attr.network.phoneNumbers"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_AUTO_ANSWER_OUTGOING_CALL = "attr.network.autoAnswerOutgoingCall"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_IMEI = "attr.network.imei"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_ICCID = "attr.network.iccid"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_IMSI = "attr.network.imsi"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_SIMULATE_SIM_NOT_PRESENT = "attr.network.simulateSimNotPresent"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_SAVE_SIM_CARD_DATA = "attr.network.saveSimCardData"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_IP_ADDRESS = "attr.network.ip"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_IGNORE_UDP_PORT_CONFLICT = "attr.network.ignoreUDPPortConflict"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_SMS_SOURCE_PORT = "attr.network.smsSourcePort"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_SMS_DESTINATION_PORT = "attr.network.smsDestinationPort"; //$NON-NLS-1$
    public static final String ATTR_NETWORK_PDE_PORT = "attr.network.pdePort"; //$NON-NLS-1$
    public static final String ATTR_PORTS_USB_CONNECTED = "attr.ports.usbCableConnected"; //$NON-NLS-1$
    public static final String ATTR_PORTS_BLUETOOTH_PORT = "attr.ports.bluetooth.port"; //$NON-NLS-1$
    public static final String ATTR_VIEW_DISABLE_AUTO_BACKLIGHT_SHUTOFF = "attr.view.disableAutoBacklightShutOff"; //$NON-NLS-1$
    public static final String ATTR_VIEW_HIDE_NETWORK_INFORMATION = "attr.view.hideNetworkInformation"; //$NON-NLS-1$
    public static final String ATTR_VIEW_DISPLAY_LCD_ONLY = "attr.view.displayLCDOnly"; //$NON-NLS-1$
    public static final String ATTR_VIEW_PATH_TO_CONFIG_PACK = "attr.view.pathToConfigPack"; //$NON-NLS-1$
    public static final String ATTR_VIEW_LCD_ZOOM = "attr.view.lcdZoom"; //$NON-NLS-1$
    public static final String ATTR_VIEW_NOT_SHOW_HELP_FOR_KEY_MAPPING = "attr.view.notShowHelpForKeyMapping"; //$NON-NLS-1$
    public static final String ATTR_ADVANCED_NOT_SIMULATE_RIM_BATTERY = "notSimulateAuthenticRIMBattery"; //$NON-NLS-1$
    public static final String ATTR_ADVANCED_NOT_USE_PC_NUMPAD_FOR_TRACKBALL = "notUsePcNumpadForTrackball"; //$NON-NLS-1$
    public static final String ATTR_USE_CUSTOMIZED_COMMAND_LINE = "useCustomizedCommandLine"; //$NON-NLS-1$
    public static final String ATTR_CUSTOMIZED_COMMAND_LINE = "customizedCommandLine"; //$NON-NLS-1$
    public static final String ATTR_CUSTOMIZED_WORKING_DIRECTORY = "customizedWorkingDirectory"; //$NON-NLS-1$
    public static final String ATTR_CUSTOMIZED_MDS_DIRECTORY = "customizedMDSDirectory"; //$NON-NLS-1$

    public static final int BROWSE_DIALOG_TYPE_OPEN_FILE = 0;
    public static final int BROWSE_DIALOG_TYPE_SELECT_DIR = 1;
    public static final String DEFAULT_PIN_NUMBER = "0x2100000A"; //$NON-NLS-1$
    public static final String SETTABLE_PROPERTY_DEVICE = "Device:"; //$NON-NLS-1$
    public static final String SETTABLE_PROPERTY_DEVICE_DEFAULT = "Default"; //$NON-NLS-1$
}
