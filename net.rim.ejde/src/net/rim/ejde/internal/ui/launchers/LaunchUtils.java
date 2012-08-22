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
package net.rim.ejde.internal.ui.launchers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.launching.DeviceInfo;
import net.rim.ejde.internal.launching.DeviceProfileManager;
import net.rim.ejde.internal.launching.IDeviceLaunchConstants;
import net.rim.ejde.internal.launching.IFledgeLaunchConstants;
import net.rim.ejde.internal.launching.IRunningFledgeLaunchConstants;
import net.rim.ejde.internal.model.BlackBerryVMInstallType;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.NatureUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ide.SettableProperty;
import net.rim.ide.SettablePropertyGroup;
import net.rim.ide.SimulatorProfiles;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;

/**
 * Utility class used by launch configuration.
 *
 * @author dmeng
 *
 */
public class LaunchUtils {

    private static Logger _logger = Logger.getLogger( LaunchUtils.class );

    /**
     * Returns device info for the given VM.
     *
     * @param vm
     *            The BlackBerry VM
     * @return List of <code>DeviceInfo</code>
     */
    public static List< DeviceInfo > getDevicesInfo( IVMInstall vm ) {
        List< DeviceInfo > deviceList = DeviceProfileManager.getInstance().getDeviceProfiles( vm );
        return deviceList;
    }

    /**
     * Returns the default device info in the given VM.
     *
     * @param vm
     *            The VM
     * @return The default device info
     */
    public static DeviceInfo getDefaultDeviceInfo( IVMInstall vm ) {
        DeviceInfo defaultDevice = DeviceProfileManager.getInstance().getDefaultDevice( vm );
        return defaultDevice;
    }

    /**
     * Returns the device info for the given simulator profile.
     *
     * @param profiles
     *            The <code>SimulatorProfiles</code>
     * @param profileName
     *            The profile name
     * @return The device info in the given profile
     */
    public static List< DeviceInfo > getDevicesInfo( SimulatorProfiles profiles, String profileName ) {
        List< DeviceInfo > deviceNames = new ArrayList< DeviceInfo >();
        SettablePropertyGroup[] propGroups = profiles.getSettableProperties( profileName );
        for( int groupIndex = 0; groupIndex < propGroups.length; groupIndex++ ) {
            SettableProperty[] properties = propGroups[ groupIndex ].getProperties();
            for( int propIndex = 0; propIndex < properties.length; propIndex++ ) {
                if( properties[ propIndex ].getLabel().equals( IFledgeLaunchConstants.SETTABLE_PROPERTY_DEVICE ) ) {
                    String[] choices = properties[ propIndex ].getChoices();
                    // filter out Default and blank
                    for( int k = 0; k < choices.length; k++ ) {
                        if( !choices[ k ].equals( IConstants.EMPTY_STRING )
                                && !choices[ k ].equals( IFledgeLaunchConstants.SETTABLE_PROPERTY_DEVICE_DEFAULT ) ) {
                            // for default simulators, don't need set directory and config file name
                            deviceNames.add( new DeviceInfo( IFledgeLaunchConstants.DEFAULT_SIMULATOR_BUNDLE_NAME, choices[ k ],
                                    "", choices[ k ] ) );
                        }
                    }
                    return deviceNames;
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Returns BlackBerry VM used to launch the given projects.
     *
     * @param projects
     *            The list of <code>IProject</code>
     * @return The <code>IVMInstall</code> or <code>null</code> if not found.
     */
    public static IVMInstall getDefaultLaunchVM( Collection< IProject > projects ) {
        IVMInstall targetVM = null;
        for( IProject project : projects ) {
            IVMInstall vm = ProjectUtils.getVMForProject( JavaCore.create( project ) );
            if( vm != null ) {
                if( targetVM != null ) {
                    // use the highest version of VM
                    if( vm.getId().compareTo( targetVM.getId() ) > 0 ) {
                        targetVM = vm;
                    }
                } else {
                    targetVM = vm;
                }
            }
        }
        return targetVM;
    }

    /**
     * Get the projects defined in the given launch configuration.
     *
     * @param configuration
     *            The launch configuration
     * @return The collection of projects
     */
    @SuppressWarnings("unchecked")
    public static Set< IProject > getProjectsFromConfiguration( ILaunchConfiguration configuration ) {
        List< String > checkedProjectNames;
        try {
            checkedProjectNames = configuration.getAttribute( IFledgeLaunchConstants.ATTR_DEPLOYED_PROJECTS,
                    Collections.EMPTY_LIST );
        } catch( CoreException e ) {
            _logger.error( e );
            return Collections.emptySet();
        }
        Set< IProject > checkedProjects = new HashSet< IProject >();
        for( String name : checkedProjectNames ) {
            IResource ires = ResourcesPlugin.getWorkspace().getRoot().findMember( name );
            if( ires != null && ires instanceof IProject ) {
                IProject project = (IProject) ires;
                // this also filters out closed projects
                if( project.isOpen() ) {
                    checkedProjects.add( project );
                }
            }
        }
        return checkedProjects;
    }

    /**
     * Get the highest VM in the given projects.
     *
     * @param projects
     *            The projects
     * @return The highest version of VM or <code>null</code> if all VMs are uninstalled
     */
    public static IVMInstall getHighestVMInProjects( Collection< IProject > projects ) {
        IVMInstall targetVM = null;
        for( IProject project : projects ) {
            IVMInstall vm = ProjectUtils.getVMForProject( JavaCore.create( project ) );
            // The VM must be available
            if( vm != null ) {
                if( targetVM != null ) {
                    if( vm.getId().compareTo( targetVM.getId() ) > 0 ) {
                        targetVM = vm;
                    }
                } else {
                    targetVM = vm;
                }
            }
        }
        return targetVM;
    }

    /**
     * Returns current running BlackBerry launch, could be Simulator, Device or Running Simulator launch.
     *
     * @return The BlackBerry launch or <code>null</code> if running BB launch does not exist.
     */
    public static ILaunch getRunningBBLaunch() {
        ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
        for( int i = 0; i < launches.length; i++ ) {
            if( !launches[ i ].isTerminated() ) {
                try {
                    ILaunchConfiguration configuration = launches[ i ].getLaunchConfiguration();
                    if( configuration != null ) {
                        ILaunchConfigurationType launchType = configuration.getType();
                        if( launchType.getIdentifier().equals( IFledgeLaunchConstants.LAUNCH_CONFIG_ID )
                                || launchType.getIdentifier().equals( IRunningFledgeLaunchConstants.LAUNCH_CONFIG_ID )
                                || launchType.getIdentifier().equals( IDeviceLaunchConstants.LAUNCH_CONFIG_ID ) ) {
                            return launches[ i ];
                        }
                    }
                } catch( CoreException e ) {
                    _logger.error( e );
                }
            }
        }
        return null;
    }

    /**
     * Get list of <code>IProject</code> from the selection.
     *
     * @param selection
     * @return The list of <code>IProject</code>
     */
    public static List< IProject > getSelectedProjects( StructuredSelection selection ) {
        Object[] items = selection.toArray();
        List< IProject > projects = new ArrayList< IProject >();
        IProject iproject;
        for( int i = 0; i < items.length; i++ ) {
            iproject = null;
            if( items[ i ] instanceof IAdaptable ) {
                Object ires = ( (IAdaptable) items[ i ] ).getAdapter( IResource.class );
                if( ires != null ) {
                    iproject = ( (IResource) ires ).getProject();
                }
            }
            if( iproject != null ) {
                if( NatureUtils.hasBBNature( iproject ) && !projects.contains( iproject ) ) {
                    projects.add( iproject );
                }
            }

        }
        return projects;
    }

    /**
     * Returns the VM in the given launch configuration.
     *
     * @param configuration
     *            The <code>ILaunchConfiguration</code>
     * @return The <code>IVMInstall</code> or <code>null</code> if BB-VM is not found.
     */
    public static IVMInstall getVMFromConfiguration( ILaunchConfiguration configuration ) {
        IVMInstall vm = null;
        try {
            int vmType = configuration.getAttribute( IFledgeLaunchConstants.ATTR_JRE_TYPE,
                    IFledgeLaunchConstants.DEFAULT_JRE_TYPE );
            if( vmType == IFledgeLaunchConstants.JRE_TYPE_PROJECT ) {
                Set< IProject > projects = getProjectsFromConfiguration( configuration );
                vm = getDefaultLaunchVM( projects );
            } else if( vmType == IFledgeLaunchConstants.JRE_TYPE_ALTERNATE ) {
                String vmId = configuration.getAttribute( IFledgeLaunchConstants.ATTR_JRE_ID, StringUtils.EMPTY );
                if( !vmId.equals( StringUtils.EMPTY ) ) {
                    vm = VMUtils.findVMById( vmId );
                }
            }
        } catch( CoreException e ) {
            _logger.error( e );
        }
        return vm;
    }

    /**
     * Returns the device name associated with the given launch configuration.
     *
     * @param configuration
     *            The launch configuration
     * @return The device name or empty string if any error occurs
     */
    public static DeviceInfo getDeviceInfo( ILaunchConfiguration configuration ) {
        try {
            String simDir = configuration.getAttribute( IFledgeLaunchConstants.ATTR_GENERAL_SIM_DIR, StringUtils.EMPTY );
            String bundleName = configuration.getAttribute( IFledgeLaunchConstants.ATTR_GENERAL_BUNDLE, StringUtils.EMPTY );
            String deviceName = configuration.getAttribute( IFledgeLaunchConstants.ATTR_GENERAL_DEVICE, StringUtils.EMPTY );
            String configFile = configuration.getAttribute( IFledgeLaunchConstants.ATTR_GENERAL_CONFIG_FILE, StringUtils.EMPTY );
            IVMInstall vm = LaunchUtils.getVMFromConfiguration( configuration );
            List< DeviceInfo > devices = LaunchUtils.getDevicesInfo( vm );
            DeviceInfo di = new DeviceInfo( bundleName, deviceName, simDir, configFile );
            if( !devices.contains( di ) ) {
                return LaunchUtils.getDefaultDeviceInfo( vm );
            }
            return di;
        } catch( CoreException e ) {
            _logger.error( e );
        }
        return null;
    }

    /**
     * Returns the String attribute value stored in the launch configuration.
     *
     * @param configuration
     *            The launch configuration
     * @param attribute
     *            The attribute
     * @return The attribute value
     */
    public static String getStringAttribute( ILaunchConfiguration configuration, String attribute, String defaultValue ) {
        String ret = defaultValue;
        try {
            ret = configuration.getAttribute( attribute, defaultValue );
        } catch( CoreException e ) {
            _logger.error( e.getMessage() );
        }
        return ret;
    }

    /**
     * Returns the boolean attribute value stored in the launch configuration.
     *
     * @param configuration
     *            The launch configuration
     * @param attribute
     *            The attribute
     * @return The attribute value
     */
    public static boolean getBooleanAttribute( ILaunchConfiguration configuration, String attribute, boolean defaultValue ) {
        boolean ret = defaultValue;
        try {
            ret = configuration.getAttribute( attribute, defaultValue );
        } catch( CoreException e ) {
            _logger.error( e.getMessage() );
        }
        return ret;
    }

    /**
     * Returns the simulator path for the given VM.
     *
     * @param vm
     *            The VM
     * @return The simulator path
     */
    public static String getSimualtorPath( IVMInstall vm ) {
        String simulatorPath;
        String isInternal = ( (AbstractVMInstall) vm ).getAttribute( BlackBerryVMInstallType.ATTR_INTERNAL );
        if( isInternal != null && Integer.parseInt( isInternal ) == 1 ) {
            simulatorPath = vm.getInstallLocation().getPath() + File.separator + "debug";
        } else {
            simulatorPath = vm.getInstallLocation().getPath() + File.separator + "simulator";
        }
        return simulatorPath;
    }

    /**
     * Returns the fledge.exe path for the given VM.
     *
     * @param vm
     *            The VM
     * @return The fledge.exe path
     */
    public static String getFledgeExePath( IVMInstall vm ) {
        String fledgePath;
        String isInternal = ( (AbstractVMInstall) vm ).getAttribute( BlackBerryVMInstallType.ATTR_INTERNAL );
        if( isInternal != null && Integer.parseInt( isInternal ) == 1 ) {
            fledgePath = vm.getInstallLocation().getPath() + File.separator + "fledge" + File.separator + "bin";
        } else {
            fledgePath = vm.getInstallLocation().getPath() + File.separator + "simulator";
        }
        return fledgePath;
    }

    public static DeviceInfo getDeviceToLaunch( ILaunchConfiguration configuration ) throws CoreException {
        String bundleName = configuration.getAttribute( IFledgeLaunchConstants.ATTR_GENERAL_BUNDLE, StringUtils.EMPTY );
        String simDir = configuration.getAttribute( IFledgeLaunchConstants.ATTR_GENERAL_SIM_DIR, StringUtils.EMPTY );
        String deviceName = configuration.getAttribute( IFledgeLaunchConstants.ATTR_GENERAL_DEVICE, StringUtils.EMPTY );
        String configFileName = configuration.getAttribute( IFledgeLaunchConstants.ATTR_GENERAL_CONFIG_FILE, StringUtils.EMPTY );
        IVMInstall vm = LaunchUtils.getVMFromConfiguration( configuration );
        List< DeviceInfo > devices = LaunchUtils.getDevicesInfo( vm );
        if( devices.isEmpty() ) {
            throw new CoreException(
                    StatusFactory.createErrorStatus( NLS.bind( Messages.Launch_Error_DeviceNotFound, vm.getId() ) ) );
        }
        if( !devices.contains( new DeviceInfo( bundleName, deviceName, simDir, configFileName ) ) ) {
            // JRE is changed, choose the default device instead
            DeviceInfo di = LaunchUtils.getDefaultDeviceInfo( vm );
            if( di != null ) {
                bundleName = di.getBundleName();
                simDir = di.getDirectory();
                deviceName = di.getDeviceName();
                configFileName = di.getConfigName();
                // save the changes
                ILaunchConfigurationWorkingCopy workingCopy = configuration.getWorkingCopy();
                workingCopy.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_BUNDLE, bundleName );
                workingCopy.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_SIM_DIR, simDir );
                workingCopy.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_DEVICE, deviceName );
                workingCopy.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_CONFIG_FILE, configFileName );
                workingCopy.doSave();
            } else {
                throw new CoreException( StatusFactory.createErrorStatus( NLS.bind( Messages.Launch_Error_DefaultDeviceNotFound,
                        vm.getId() ) ) );
            }
        }
        return new DeviceInfo( bundleName, deviceName, simDir, configFileName );
    }

    /**
     * Close the given launch, it terminates the launch first (if supported) and remove it from launch manager.
     *
     * @param launch
     *            The launch to be closed
     * @throws DebugException
     */
    public static void closeLaunch( ILaunch launch ) throws DebugException {
        if( launch != null ) {
            if( launch.canTerminate() ) {
                launch.terminate();
            }
            DebugPlugin.getDefault().getLaunchManager().removeLaunch( launch );
        }
    }

    /**
     * Returns the VM name used in the given launch configuration.
     *
     * @param configuration
     *            The launch configuration
     * @return The VM name or "undefined" if the vm has been removed
     */
    public static String getVMNameFromConfiguration( ILaunchConfiguration configuration ) {
        String vmName = "undefined";
        try {
            int vmType = configuration.getAttribute( IFledgeLaunchConstants.ATTR_JRE_TYPE,
                    IFledgeLaunchConstants.DEFAULT_JRE_TYPE );
            if( vmType == IFledgeLaunchConstants.JRE_TYPE_PROJECT ) {
                Set< IProject > projects = getProjectsFromConfiguration( configuration );
                IVMInstall vm = getDefaultLaunchVM( projects );
                if( vm != null ) {
                    vmName = vm.getName();
                }
            } else if( vmType == IFledgeLaunchConstants.JRE_TYPE_ALTERNATE ) {
                vmName = configuration.getAttribute( IFledgeLaunchConstants.ATTR_JRE_ID, StringUtils.EMPTY );
            }
        } catch( CoreException e ) {
            _logger.error( e );
        }
        return vmName;
    }
}
