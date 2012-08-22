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

/**This package is imported to be able to launch the "Blackberry SImulator Output Console"
 * when a Rim Simulator with a debug mode has been invoked from this class.
 **/

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.core.IRIMMarker;
import net.rim.ejde.internal.launching.MDSCSChecker.MDSCSCheckResult;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.preferences.SignatureToolPreferences;
import net.rim.ejde.internal.packaging.PackagingJob;
import net.rim.ejde.internal.packaging.PackagingJobWrapper;
import net.rim.ejde.internal.ui.launchers.LaunchUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ejde.internal.util.VMToolsUtils;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ide.OSUtils;
import net.rim.ide.RIA;
import net.rim.ide.SimulatorProfiles;
import net.rim.ide.core.IDEProperties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * @author dmeng
 *
 */
public class FledgeLaunchConfigurationDelegate extends AbstractLaunchConfigurationDelegate implements IFledgeLaunchConstants {

    private static Logger _logger = Logger.getLogger( FledgeLaunchConfigurationDelegate.class );
    private ILaunch _launch;
    private LaunchParams _launchParams;
    private boolean _terminatePreviousLaunch;
    private Object _notifier = new Object();
    private static final Pattern FLEDGE_COMMAND_PATTERN = Pattern.compile( "(.+)\\s/handheld=(\\S+)(.*)" );

    /**
     * Default constructor.
     */
    public FledgeLaunchConfigurationDelegate() {
        // do nothing
    }

    public static class LaunchParams {

        private String _commandLine;
        private String _workingDir;
        private String _MDSDir;
        private boolean _launchMDSCS;
        private ILaunchConfiguration _configuration;
        private IVMInstall _vm;
        private String _deviceName;
        private SimulatorProfiles _simulatorProfiles;

        public LaunchParams( ILaunchConfiguration configuration ) {
            _configuration = configuration;
        }

        public IVMInstall getVM() {
            if( _vm == null ) {
                _vm = LaunchUtils.getVMFromConfiguration( _configuration );
            }
            return _vm;
        }

        public SimulatorProfiles getSimulatorProfiles() {
            if( _simulatorProfiles == null ) {
                IVMInstall vm = getVM();
                RIA ria = ContextManager.PLUGIN.getRIA( vm.getInstallLocation().getAbsolutePath() );
                _simulatorProfiles = ria.getSimulatorProfiles();
            }
            return _simulatorProfiles;
        }

        public String getCustomizedCommandLine() {
            return LaunchUtils.getStringAttribute( _configuration, ATTR_CUSTOMIZED_COMMAND_LINE, StringUtils.EMPTY );
        }

        public String getCustomizedMDSDirectory() {
            return LaunchUtils.getStringAttribute( _configuration, ATTR_CUSTOMIZED_MDS_DIRECTORY, StringUtils.EMPTY );
        }

        public String getCustomizedWorkingDirectory() {
            return LaunchUtils.getStringAttribute( _configuration, ATTR_CUSTOMIZED_WORKING_DIRECTORY, StringUtils.EMPTY );
        }

        public boolean useCustomizedCommandOptions() {
            return LaunchUtils.getBooleanAttribute( _configuration, ATTR_USE_CUSTOMIZED_COMMAND_LINE, false );
        }

        public String getDefaultCommandLine() {
            if( _commandLine == null ) {
                _commandLine = buildCommandLine( _configuration );
            }
            return _commandLine;
        }

        public String getDefaultWorkingdir() {
            if( _workingDir == null ) {
                IVMInstall vm = getVM();
                if( vm != null ) {
                    RIA ria = ContextManager.PLUGIN.getRIA( vm.getInstallLocation().getAbsolutePath() );
                    if( ria != null ) {
                        String bundleName = getBundleName();
                        if( bundleName.equals( IFledgeLaunchConstants.DEFAULT_SIMULATOR_BUNDLE_NAME ) ) {
                            _workingDir = LaunchUtils.getSimualtorPath( vm );
                        } else {
                            _workingDir = LaunchUtils
                                    .getStringAttribute( _configuration, ATTR_GENERAL_SIM_DIR, StringUtils.EMPTY );
                        }
                    }
                }
            }
            return _workingDir;
        }

        public boolean isLaunchMDSCS() {
            try {
                _launchMDSCS = _configuration.getAttribute( ATTR_GENERAL_LAUNCH_MDSCS, false );
            } catch( CoreException e ) {
                _launchMDSCS = false;
                _logger.error( "", e );
            }
            return _launchMDSCS;
        }

        public String getDefaultMDSPath() {
            if( _MDSDir == null ) {
                IVMInstall vm = getVM();
                if( vm != null ) {
                    if( VMUtils.isInternal( vm ) ) {
                        IPath mdsPath = new Path( vm.getInstallLocation().getAbsolutePath() ).append( ".." ).append(
                                "IPProxyProject" );
                        _MDSDir = mdsPath.toOSString();
                    } else {
                        _MDSDir = vm.getInstallLocation().toString() + File.separator + "MDS";
                    }
                }
            }
            return _MDSDir;
        }

        public String getCommandLine() throws CoreException {
            if( useCustomizedCommandOptions() ) {
                String cmdLine = getCustomizedCommandLine();
                // parse the command line for device name
                Matcher matcher = FLEDGE_COMMAND_PATTERN.matcher( cmdLine );
                if( matcher.matches() ) {
                    _deviceName = matcher.group( 2 );
                } else {
                    throw new CoreException( StatusFactory.createErrorStatus( Messages.Launch_Error_DeviceNotFoundInCommnandLine ) );
                }
                return cmdLine;
            } else {
                return getDefaultCommandLine();
            }
        }

        public String getWorkingDirectory() {
            if( useCustomizedCommandOptions() ) {
                return getCustomizedWorkingDirectory();
            } else {
                return getDefaultWorkingdir();
            }
        }

        public String getMDSPath() {
            if( useCustomizedCommandOptions() ) {
                return getCustomizedMDSDirectory();
            } else {
                return getDefaultMDSPath();
            }
        }

        public String getBundleName() {
            return LaunchUtils.getStringAttribute( _configuration, ATTR_GENERAL_BUNDLE, StringUtils.EMPTY );
        }

        public String getDeviceName() {
            return _deviceName;
        }

        @SuppressWarnings("unchecked")
        private String buildCommandLine( ILaunchConfiguration configuration ) {
            StringBuffer sb = new StringBuffer();
            boolean booleanAttr;
            String stringAttr;
            List< String > listAttr;
            try {
                List< IProject > projects = configuration.getAttribute( ATTR_DEPLOYED_PROJECTS, Collections.EMPTY_LIST );
                // There is no open projects in the launch configuration
                if( projects.isEmpty() ) {
                    throw new CoreException( StatusFactory.createErrorStatus( Messages.Launch_Error_ProjectNotFound ) );
                }

                IVMInstall vm = getVM();
                // no VM available to launch
                if( vm == null ) {
                    throw new CoreException( StatusFactory.createErrorStatus( Messages.Launch_Error_JRENotFound ) );
                }

                List< DeviceInfo > devices = LaunchUtils.getDevicesInfo( vm );
                if( devices.isEmpty() ) {
                    throw new CoreException( StatusFactory.createErrorStatus( NLS.bind( Messages.Launch_Error_DeviceNotFound,
                            vm.getId() ) ) );
                }

                // get device to be launched
                DeviceInfo di = LaunchUtils.getDeviceToLaunch( configuration );
                String simDir = di.getDirectory();
                String deviceName = di.getDeviceName();
                String configFileName = di.getConfigName();
                String fledgePath = IConstants.EMPTY_STRING;
                if( VMUtils.isInternal( vm ) ) {
                    if( simDir.equals( LaunchUtils.getSimualtorPath( vm ) ) ) {
                        // it is internal simulator
                        fledgePath = "\"" + vm.getInstallLocation().getPath() + File.separator + "fledge" + File.separator
                                + "bin" + File.separator + "fledge.exe" + "\" ";
                    } else {
                        // it is external simulator
                        fledgePath = "\"" + simDir + File.separator + "fledge.exe" + "\" ";
                    }
                } else {
                    fledgePath = "\"" + simDir + File.separator + "fledge.exe" + "\" ";
                }
                sb.append( fledgePath );
                sb.append( "/handheld=" + deviceName + " " );
                _deviceName = deviceName;
                // config file
                sb.append( "/app-param=JvmAlxConfigFile:" + configFileName + ".xml " );
                // PIN
                String pin = configuration.getAttribute( ATTR_GENERAL_PIN, StringUtils.EMPTY );
                sb.append( "/pin=" + pin + " " );
                // data port
                sb.append( "/data-port=0x4d44 /data-port=0x4d4e " );
                // session name
                sb.append( "/session=" + deviceName + " " );
                // application to be launched
                String appName = configuration.getAttribute( ATTR_GENERAL_LAUNCH_APP_ON_STARTUP, StringUtils.EMPTY );
                if( appName.length() > 0 ) {
                    sb.append( "/app-param=launch=" + appName + " " );
                }
                // jvm library
                if( VMUtils.isInternal( vm ) ) {
                    if( simDir.equals( LaunchUtils.getSimualtorPath( vm ) ) ) {
                        // it is internal simulator
                        sb.append( "/app=JvmFledgeSimulator.dll " );
                    } else {
                        // it is external simulator
                        String dllPath = "/app=" + "\"" + simDir + File.separator + "Jvm.dll" + "\" ";
                        sb.append( dllPath );
                    }
                } else {
                    String dllPath = "/app=" + "\"" + simDir + File.separator + "Jvm.dll" + "\" ";
                    sb.append( dllPath );
                }
                // automatically use default value for all prompts
                booleanAttr = configuration.getAttribute( ATTR_GENERAL_AUTOMATICALLY_USE_DEFAULT_VALUE, false );
                if( booleanAttr ) {
                    sb.append( "/automate=true " );
                    // numer of seconds to wait before automated response is selected
                    stringAttr = configuration.getAttribute( ATTR_GENERAL_NUMBER_OF_SECONDS_WAIT_BEFORE_RESPONSE,
                            StringUtils.EMPTY );
                    if( !StringUtils.isEmpty( stringAttr ) ) {
                        sb.append( "/automate-timeout=" + stringAttr + " " );
                    }
                }
                // esn
                stringAttr = configuration.getAttribute( ATTR_GENERAL_ESN, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/esn=" + stringAttr + " " );
                }
                // meid
                stringAttr = configuration.getAttribute( ATTR_GENERAL_MEID, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/meid=" + stringAttr + " " );
                }
                // enable device security
                booleanAttr = configuration.getAttribute( ATTR_GENERAL_ENABLE_DEVICE_SECURITY, false );
                if( booleanAttr ) {
                    sb.append( "/secure=true " );
                }
                // system locale
                stringAttr = configuration.getAttribute( ATTR_GENERAL_SYSTEM_LOCALE, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/locale=" + stringAttr + " " );
                }
                // keyboard locale
                stringAttr = configuration.getAttribute( ATTR_GENERAL_KEYBOARD_LOCALE, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/keypad-locale=" + stringAttr + " " );
                }
                // interrupt debug on potential deadlock
                booleanAttr = configuration.getAttribute( ATTR_DEBUG_INTERRUPT_DEBUGGER_ON_DEADLOCK, false );
                if( booleanAttr ) {
                    sb.append( "/JvmDebugLocks " );
                }
                // do not stop execution when exception is caught
                booleanAttr = configuration.getAttribute( ATTR_DEBUG_DO_NOT_STOP_EXECUTION, false );
                if( booleanAttr ) {
                    sb.append( "/JvmNoBreakOnThrowable " );
                }
                // application heapsize
                stringAttr = configuration.getAttribute( ATTR_MEMORY_APPLICATION_HEAP_SIZE, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/heap-size=" + stringAttr + " " );
                }
                // branding data
                stringAttr = configuration.getAttribute( ATTR_MEMORY_BRANDING_DATA, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/branding-data=\"" + stringAttr + "\" " );
                }
                // reset file system on startup
                booleanAttr = configuration.getAttribute( ATTR_MEMORY_RESET_FILE_SYSTEM_ON_STARTUP, false );
                if( booleanAttr ) {
                    sb.append( "/reset-filesystem=true " );
                }
                // reset NVRAM on startup
                booleanAttr = configuration.getAttribute( ATTR_MEMORY_RESET_NVRAM_ON_STARTUP, false );
                if( booleanAttr ) {
                    sb.append( "/reset-nvram=true " );
                }
                // file system size
                stringAttr = configuration.getAttribute( ATTR_MEMORY_FILE_SYSTEM_SIZE, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/filesystem-size=" + stringAttr + " " );
                }
                // do not save flash data on simulator exit
                booleanAttr = configuration.getAttribute( ATTR_MEMORY_NOT_SAVE_FLASH_ON_EXIT, false );
                if( booleanAttr ) {
                    sb.append( "/save-flash=false " );
                }
                // do not compact file system on exit
                booleanAttr = configuration.getAttribute( ATTR_MEMORY_NOT_COMPACT_FILE_SYSTEM_ON_EXIT, false );
                if( booleanAttr ) {
                    sb.append( "/compact-filesystem=false " );
                }
                // destroy existing sdcard image
                booleanAttr = configuration.getAttribute( ATTR_MEMORY_DESTROY_EXISTING_SDCARD_IMAGE, false );
                if( booleanAttr ) {
                    sb.append( "/clear-sdcard=true " );
                }
                // simulate sdcard insert
                booleanAttr = configuration.getAttribute( ATTR_MEMORY_SIMULATE_SDCARD_INSERTED, false );
                if( booleanAttr ) {
                    sb.append( "/sdcard-inserted=true " );
                    // sd card image
                    stringAttr = configuration.getAttribute( ATTR_MEMORY_SDCARD_IMAGE, StringUtils.EMPTY );
                    if( !StringUtils.isEmpty( stringAttr ) ) {
                        sb.append( "/sdcard=" + "\"" + stringAttr + "\" " );
                    }
                    // sd card size
                    stringAttr = configuration.getAttribute( ATTR_MEMORY_SDCARD_SIZE, StringUtils.EMPTY );
                    if( !StringUtils.isEmpty( stringAttr ) ) {
                        sb.append( "/sdcard-size=" + stringAttr + " " );
                    }
                }
                // use PC filesystem for SD card files
                booleanAttr = configuration.getAttribute( ATTR_MEMORY_USE_PC_FILESYSTEM_FOR_SDCARD_FILES, false );
                if( booleanAttr ) {
                    sb.append( "/fs-sdcard=true " );
                    // pc file system path
                    stringAttr = configuration.getAttribute( ATTR_MEMORY_PC_FILESYSTEM_PATH, StringUtils.EMPTY );
                    // remove the last "\" character if there is one
                    if( stringAttr.endsWith( "\\" ) ) {
                        stringAttr = stringAttr.substring( 0, stringAttr.length() - 1 );
                    }
                    sb.append( "/fs-sdcard-root=\"" + stringAttr + "\" " );
                }
                // disable registration
                booleanAttr = configuration.getAttribute( ATTR_NETWORK_DISABLE_REGISTRATION, false );
                if( booleanAttr ) {
                    sb.append( "/app-param=DisableRegistration " );
                }
                // networks
                listAttr = configuration.getAttribute( ATTR_NETWORK_NETWORKS, Collections.EMPTY_LIST );
                if( !listAttr.isEmpty() ) {
                    for( String network : listAttr ) {
                        sb.append( "/network=" + network + " " );
                    }
                }
                // start with radio off
                booleanAttr = configuration.getAttribute( ATTR_NETWORK_START_WITH_RADIO_OFF, false );
                if( booleanAttr ) {
                    sb.append( "/radio-on-at-startup=false " );
                }
                // phone numbers
                listAttr = configuration.getAttribute( ATTR_NETWORK_PHONE_NUMBERS, Collections.EMPTY_LIST );
                if( !listAttr.isEmpty() ) {
                    for( String network : listAttr ) {
                        sb.append( "/phone-number=" + network + " " );
                    }
                }
                // automatically answer outgoing calls
                booleanAttr = configuration.getAttribute( ATTR_NETWORK_AUTO_ANSWER_OUTGOING_CALL, false );
                if( booleanAttr ) {
                    sb.append( "/auto-answer-calls=true " );
                }
                // IMEI
                stringAttr = configuration.getAttribute( ATTR_NETWORK_IMEI, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/imei=" + stringAttr + " " );
                }
                // ICCID
                stringAttr = configuration.getAttribute( ATTR_NETWORK_ICCID, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/iccid=" + stringAttr + " " );
                }
                // IMSI
                stringAttr = configuration.getAttribute( ATTR_NETWORK_IMSI, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/imsi=" + stringAttr + " " );
                }
                // simulate SIM not present
                booleanAttr = configuration.getAttribute( ATTR_NETWORK_SIMULATE_SIM_NOT_PRESENT, false );
                if( booleanAttr ) {
                    sb.append( "/sim-present=false " );
                }
                // IP address
                stringAttr = configuration.getAttribute( ATTR_NETWORK_IP_ADDRESS, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/ip-address=" + stringAttr + " " );
                }
                // ignore UDP port conflict
                booleanAttr = configuration.getAttribute( ATTR_NETWORK_IGNORE_UDP_PORT_CONFLICT, false );
                if( booleanAttr ) {
                    sb.append( "/ignore-data-port-conflicts=true " );
                }
                // SMS source port
                stringAttr = configuration.getAttribute( ATTR_NETWORK_SMS_SOURCE_PORT, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/sms-source-port=" + stringAttr + " " );
                }
                // SMS destination port
                stringAttr = configuration.getAttribute( ATTR_NETWORK_SMS_DESTINATION_PORT, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/sms-destination-port=" + stringAttr + " " );
                }
                // PDE port
                stringAttr = configuration.getAttribute( ATTR_NETWORK_PDE_PORT, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/pde-port=" + stringAttr + " " );
                }
                // USB cable connected
                booleanAttr = configuration.getAttribute( ATTR_PORTS_USB_CONNECTED, false );
                if( booleanAttr ) {
                    sb.append( "/comm-cable-connected=true " );
                }
                // bluetooth test board port
                stringAttr = configuration.getAttribute( ATTR_PORTS_BLUETOOTH_PORT, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/bluetooth-port=" + stringAttr + " " );
                }
                // disable automatic backlight shutoff
                booleanAttr = configuration.getAttribute( ATTR_VIEW_DISABLE_AUTO_BACKLIGHT_SHUTOFF, false );
                if( booleanAttr ) {
                    sb.append( "/JvmDisableBacklightTimeout " );
                }
                // hide network specific information
                booleanAttr = configuration.getAttribute( ATTR_VIEW_HIDE_NETWORK_INFORMATION, false );
                if( booleanAttr ) {
                    sb.append( "/JvmHideNetworkInfo " );
                }
                // display LCD only
                booleanAttr = configuration.getAttribute( ATTR_VIEW_DISPLAY_LCD_ONLY, false );
                if( booleanAttr ) {
                    sb.append( "/show-plastics=false " );
                }
                // full or relative path to a Config-Pack
                stringAttr = configuration.getAttribute( ATTR_VIEW_PATH_TO_CONFIG_PACK, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/config-pack=" + stringAttr + " " );
                }
                // LCD zoom level
                stringAttr = configuration.getAttribute( ATTR_VIEW_LCD_ZOOM, StringUtils.EMPTY );
                if( !StringUtils.isEmpty( stringAttr ) ) {
                    sb.append( "/zoom=" + stringAttr + " " );
                }
                // do not show help for key mapping
                booleanAttr = configuration.getAttribute( ATTR_VIEW_NOT_SHOW_HELP_FOR_KEY_MAPPING, false );
                if( booleanAttr ) {
                    sb.append( "/show-key-help=false " );
                }
                // do not simulate using authentic RIM battery
                booleanAttr = configuration.getAttribute( ATTR_ADVANCED_NOT_SIMULATE_RIM_BATTERY, false );
                if( booleanAttr ) {
                    sb.append( "/authentic-battery=false " );
                }
                // do not use PC numpad as trackball
                booleanAttr = configuration.getAttribute( ATTR_ADVANCED_NOT_USE_PC_NUMPAD_FOR_TRACKBALL, false );
                if( booleanAttr ) {
                    sb.append( "/numpad-as-trackball=false " );
                }
            } catch( CoreException e ) {
                _logger.error( "", e );
            }
            return sb.toString().trim();
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org. eclipse.debug.core.ILaunchConfiguration,
     * java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void launch( ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor )
            throws CoreException {

        try {
            _launchParams = new LaunchParams( configuration );
            RIA ria = RIA.getCurrentDebugger();
            String commandLine = _launchParams.getCommandLine();
            String workingDir = _launchParams.getWorkingDirectory();
            String MDSDir = _launchParams.getMDSPath();
            boolean launchIPProxy = _launchParams.isLaunchMDSCS();
            _logger.info( "Simulator launch command:" + commandLine );
            _logger.info( "Working directory:" + workingDir );
            _logger.info( "Launch MDS-CS:" + launchIPProxy );
            _logger.info( "MDS-CS directory:" + MDSDir );
            IDEProperties ideProperties = ria.getProperties();
            ideProperties.putStringProperty( "SimulatorDirectory", workingDir );
            ideProperties.putStringProperty( "SimulatorCommand", commandLine );
            ideProperties.putBoolProperty( "MinimizeOnSimulatorLaunch", false );
            ideProperties.putBoolProperty( "SimulatorLaunch", true );
            ideProperties.putBoolProperty( "SimulatorReuse", true );
            ideProperties.putBoolProperty( "IPProxyLaunch", launchIPProxy );
            if( launchIPProxy ) {
                ria.setMDSSimulatorPath( new File( MDSDir ) );
                // check if MDS-CS can be launched
                MDSCSCheckResult result = MDSCSChecker.checkMDSCS( ria.getMDSSimulatorPath() );
                if( result == MDSCSCheckResult.CANCEL ) {
                    // user doesn't want to continue.
                    _logger.warn( NLS.bind( Messages.FledgeLaunchConfigurationDelegate_launchMsg, configuration.getName() )
                            + Messages.FledgeLaunchConfigurationDelegate_noLaunchMDSCSMsg );
                    return;
                }
                if( result == MDSCSCheckResult.DISABLE_MDSCS ) {
                    ideProperties.putBoolProperty( "IPProxyLaunch", false );
                }
            }
            // start launching
            if( mode.equals( "run" ) ) { //$NON-NLS-1$
                run( launch );
            } else if( mode.equals( "debug" ) ) { //$NON-NLS-1$
                debug( configuration, launch, monitor );
            }
            File fledgeHookExe = ( VMToolsUtils.getVMToolsFolderPath()
                    .append( IPath.SEPARATOR + IConstants.FLEDGE_HOOK_FILE_NAME ) ).toFile();
            // we only run fledgehook, if CP is <=5 on win7
            if( OSUtils.isWindows7() && !VMToolsUtils.is6OrLater( VMUtils.getVMVersion( _launchParams.getVM() ) )
                    && fledgeHookExe.exists() && fledgeHookExe.isFile() ) {
                String command = fledgeHookExe.getCanonicalPath();
                _logger.debug( "FledgeHook:" + fledgeHookExe.getPath() );
                _logger.debug( "FledgeHook command:" + command );
                Runtime.getRuntime().exec( command, null, fledgeHookExe.getParentFile() );
            }
        } catch( OperationCanceledException e ) {
            // dispose
        } catch( IOException e ) {
            // dispose
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate2#buildForLaunch
     * (org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    public boolean buildForLaunch( ILaunchConfiguration configuration, String mode, IProgressMonitor monitor )
            throws CoreException {
        // Package projects
        packageProjects( configuration );
        return false;
    }

    /**
     * Package projects.
     *
     * @param configuration
     *            The launch configuration
     * @throws CoreException
     */
    protected void packageProjects( ILaunchConfiguration configuration ) throws CoreException, OperationCanceledException {
        String jobName = "Packaging " + configuration.getName(); //$NON-NLS-1$
        _logger.debug( jobName );
        Set< IProject > iProjects = LaunchUtils.getProjectsFromConfiguration( configuration );
        Set< BlackBerryProject > bbProjects = ProjectUtils.getBlackBerryProjects( iProjects );
        boolean secureSim = LaunchUtils.getBooleanAttribute( configuration,
                IFledgeLaunchConstants.ATTR_GENERAL_ENABLE_DEVICE_SECURITY, false );
        boolean needSign = SignatureToolPreferences.getRunSignatureToolAutomatically();
        PackagingJobWrapper packagingJob = new PackagingJobWrapper( jobName, bbProjects,
                secureSim && needSign ? PackagingJob.SIGN_IF_PROTECTED_API_USED : PackagingJob.SIGN_NO );
        packagingJob.setUser( true );
        packagingJob.schedule();
        try {
            packagingJob.join();
        } catch( InterruptedException e ) {
            _logger.error( e );
        }
    }

    void run( final ILaunch launch ) throws CoreException {
        _launch = launch;
        final RIA debugServer = RIA.getCurrentDebugger();
        debugServer.setSimulatorCloseListener( new Runnable() {
            public void run() {
                debugServer.stopSimulator();
                // Fix for DPI213776
                if( DebugPlugin.getDefault().getLaunchManager() != null ) {
                    DebugPlugin.getDefault().getLaunchManager().removeLaunch( _launch );
                }
            }
        } );
        debugServer.startSimulator();
    }

    @Override
    protected String getDebugDestination( ILaunchConfiguration configuration ) throws CoreException {
        final RIA debugServer = RIA.getCurrentDebugger();
        if( debugServer.simulatorSupportsHotSwap() ) {
            _logger.debug( "Attaching reuse simulator..." );
            return RIA.getDebugAttachReuseSimulator();
        }
        _logger.debug( "Attaching regular simulator..." );
        return RIA.getDebugAttachSimulator();
    }

    /**
     * Override parent method to return list of projects to deploy This is done to ensure user gets prompted (if desired) when
     * there are compile problems in list of deployed projects
     */
    @Override
    protected IProject[] getProjectsForProblemSearch( ILaunchConfiguration configuration, String mode ) throws CoreException {
        IProject[] projects = getDeployedProjects( configuration );
        int projectNum = projects == null ? 0 : projects.length;
        IProject[] projectsForProblemSearch = new IProject[ projectNum ];

        if( projectNum != 0 )
            System.arraycopy( projects, 0, projectsForProblemSearch, 0, projects.length );

        return projectsForProblemSearch;
    }

    /**
     * Override parent to
     */
    @Override
    protected IProject[] getBuildOrder( ILaunchConfiguration configuration, String mode ) throws CoreException {
        return getDeployedProjects( configuration );
    }

    @SuppressWarnings("unchecked")
    private IProject[] getDeployedProjects( ILaunchConfiguration configuration ) throws CoreException {
        List< String > projectNames = configuration.getAttribute( ATTR_DEPLOYED_PROJECTS, Collections.EMPTY_LIST );
        List< IProject > projects = new ArrayList< IProject >();
        for( String projectName : projectNames ) {
            IProject project = ProjectUtils.getProject( projectName );
            if( project != null ) {
                projects.add( project );
            }
        }
        return projects.toArray( new IProject[ 0 ] );
    }

    /**
     * Override parent method because it only searches for java model problems, where EJDE marks problems with general "Problem"
     * category
     */
    @Override
    protected boolean isLaunchProblem( IMarker problemMarker ) throws CoreException {
        return ( super.isLaunchProblem( problemMarker ) || problemMarker.isSubtypeOf( IRIMMarker.PREPROCESSING_PROBLEM_MARKER )
                || problemMarker.isSubtypeOf( IRIMMarker.RESOURCE_BUILD_PROBLEM_MARKER )
                || problemMarker.isSubtypeOf( IRIMMarker.CODE_SIGN_PROBLEM_MARKER ) || problemMarker
                    .isSubtypeOf( IRIMMarker.MODEL_PROBLEM ) )
                && problemMarker.getAttribute( IMarker.SEVERITY, 0 ) == IMarker.SEVERITY_ERROR;
    }

    @Override
    public boolean preLaunchCheck( ILaunchConfiguration configuration, String mode, IProgressMonitor monitor )
            throws CoreException {

        ILaunch launch = LaunchUtils.getRunningBBLaunch();
        if( launch != null ) {
            // don't allow launch if the running launch is not a simulator launch
            ILaunchConfigurationType launchType = launch.getLaunchConfiguration().getType();
            if( !launchType.getIdentifier().equals( IFledgeLaunchConstants.LAUNCH_CONFIG_ID ) ) {
                throw new CoreException(
                        StatusFactory.createErrorStatus( Messages.AbstractLaunchConfigurationDelegate_debuggerActiveMsg ) );
            }
            // Simulator is running
            boolean restartSimulator = true;
            String message = Messages.Launch_Close_Simulator_Dialog_Message;
            IVMInstall vm = _launchParams.getVM();
            RIA ria = ContextManager.PLUGIN.getRIA( vm.getInstallLocation().getAbsolutePath() );
            if( launchType.getIdentifier().equals( IFledgeLaunchConstants.LAUNCH_CONFIG_ID ) ) {
                // The new launch and existing launch must use the same JRE/device
                String newVM = LaunchUtils.getVMFromConfiguration( configuration ).getId();
                String existingVM = _launchParams.getVM().getId();
                if( newVM.equals( existingVM ) ) {
                    DeviceInfo di = LaunchUtils.getDeviceInfo( configuration );
                    String newBundle = di.getBundleName();
                    String newDevice = di.getDeviceName();
                    String existingBundle = _launchParams.getBundleName();
                    String existingDevice = _launchParams.getDeviceName();
                    if( newBundle.equals( existingBundle ) && newDevice.equals( existingDevice ) ) {
                        if( ria.simulatorSupportsHotSwap() ) {
                            restartSimulator = false;
                        } else {
                            // simulator does not support hot-swap, restart is required
                            message = Messages.Launch_Error_HotswapNotSupport;
                        }
                    } else {
                        // device is changed, restart simulator is required
                        message = NLS.bind( Messages.FledgeLaunchConfigurationDelegate_differentDevice, newBundle + "-"
                                + newDevice, existingBundle + "-" + existingDevice );
                    }
                } else {
                    // JRE is changed, restart simulator is required
                    message = NLS.bind( Messages.FledgeLaunchConfigurationDelegate_differentJRE, existingVM, newVM );
                }
            }
            if( restartSimulator ) {
                final String dialogMessage = message;
                // ask users if they want to terminate current launch
                Display.getDefault().syncExec( new Runnable() {
                    public void run() {
                        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                        _terminatePreviousLaunch = MessageDialog.openQuestion( shell,
                                Messages.Launch_Close_Simulator_Dialog_Title, dialogMessage );
                    }
                } );
                if( _terminatePreviousLaunch ) {
                    if( ria.isSimulatorRunning() ) {
                        ria.setSimulatorCloseListener( new Runnable() {
                            public void run() {
                                synchronized( _notifier ) {
                                    _notifier.notifyAll();
                                }
                            }
                        } );
                        ria.stopSimulator();
                        // wait until the simulator is closed before launching the new one
                        while( ria.isSimulatorRunning() ) {
                            synchronized( _notifier ) {
                                try {
                                    _notifier.wait( 3000 );
                                } catch( InterruptedException e ) {
                                    // do nothing
                                }
                            }
                        }
                    }
                    DebugPlugin.getDefault().getLaunchManager().removeLaunch( launch );
                } else {
                    // give up, don't launch
                    return false;
                }
            }
        }
        return super.preLaunchCheck( configuration, mode, monitor );
    }

    /**
     * Deploy projects in the launch configuration into simulator folder.
     *
     * @param configuration
     *            The launch configuration
     * @param monitor
     *            The progress monitor
     * @return The deployment status
     * @throws CoreException
     * @throws OperationCanceledException
     */
    protected IStatus deployProjects( ILaunchConfiguration configuration, IProgressMonitor monitor ) throws CoreException,
            OperationCanceledException {
        String taskName = "Deploying " + configuration.getName(); //$NON-NLS-1$
        Set< IProject > iProjects = LaunchUtils.getProjectsFromConfiguration( configuration );
        Set< BlackBerryProject > bbProjects = ProjectUtils.getBlackBerryProjects( iProjects );
        DeploymentTask deployTask = new DeploymentTask( taskName, bbProjects, configuration );
        return deployTask.run( monitor );
    }
}
