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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.launching.DeviceInfo;
import net.rim.ejde.internal.launching.IFledgeLaunchConstants;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.sourcelookup.RIMSourcePathProvider;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.NatureUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ide.RIA;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

public abstract class AbstractLaunchShortcut implements ILaunchShortcut {

    private static final Logger _logger = Logger.getLogger( AbstractLaunchShortcut.class );

    public AbstractLaunchShortcut() {
        super();
    }

    public void launch( ISelection selection, String mode ) {
        Set< IProject > allProjects = new HashSet< IProject >();
        List< IProject > selectedProjects = LaunchUtils.getSelectedProjects( (StructuredSelection) selection );
        Set< IProject > referencedProjects;
        try {
            referencedProjects = ProjectUtils.getAllReferencedProjects( selectedProjects );
        } catch( CoreException e ) {
            _logger.error( "", e );
            referencedProjects = new HashSet< IProject >();
        }
        allProjects.addAll( selectedProjects );
        allProjects.addAll( referencedProjects );
        launch( allProjects, mode );
    }

    public void launch( IEditorPart editor, String mode ) {
        IEditorInput input = editor.getEditorInput();
        if( input instanceof IFileEditorInput ) {
            IFileEditorInput fileInput = (IFileEditorInput) input;
            IFile ifile = fileInput.getFile();
            IProject iproject = ifile.getProject();
            if( NatureUtils.hasBBNature( iproject ) ) {
                Set< IProject > projects = new HashSet< IProject >();
                projects.add( iproject );
                Set< IProject > referencedProjects;
                try {
                    referencedProjects = ProjectUtils.getAllReferencedProjects( iproject );
                } catch( CoreException e ) {
                    _logger.error( "", e );
                    referencedProjects = new HashSet< IProject >();
                }
                projects.addAll( referencedProjects );
                launch( projects, mode );
            }
        }
    }

    public void openLaunchConfiguration( ISelection selection, String mode ) {
        Set< IProject > allProjects = new HashSet< IProject >();
        List< IProject > selectedProjects = LaunchUtils.getSelectedProjects( (StructuredSelection) selection );
        Set< IProject > referencedProjects;
        try {
            referencedProjects = ProjectUtils.getAllReferencedProjects( selectedProjects );
        } catch( CoreException e ) {
            _logger.error( "", e );
            referencedProjects = new HashSet< IProject >();
        }
        allProjects.addAll( selectedProjects );
        allProjects.addAll( referencedProjects );
        final ILaunchConfiguration config = findLaunchConfiguration( allProjects, getConfigurationType() );
        if( config != null ) {
            DebugUITools.openLaunchConfigurationDialog( getShell(), new StructuredSelection( new Object[] { config } ), mode );
        }
    }

    protected void launch( final Set< IProject > projects, final String mode ) {
        final ILaunchConfiguration config = findLaunchConfiguration( projects, getConfigurationType() );
        if( config != null ) {
            DebugUITools.launch( config, mode );
        }
    }

    protected abstract ILaunchConfigurationType getConfigurationType();

    /**
     * Search launch configurations in given type by projects and VM. If one could not be found, creates one; if multiple
     * configurations are found, prompt a dialog for user selection.
     *
     * @param selectedProjects
     *            The selected projects
     * @param configType
     *            The launch configuration type
     * @return The launch configuration
     */
    private ILaunchConfiguration findLaunchConfiguration( Set< IProject > selectedProjects, ILaunchConfigurationType configType ) {
        List< ILaunchConfiguration > candidateConfigs = new ArrayList< ILaunchConfiguration >();
        try {
            ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations( configType );
            for( int i = 0; i < configs.length; i++ ) {
                // Compare projects and VM in the launch configuration and selected projects
                Collection< IProject > projects = LaunchUtils.getProjectsFromConfiguration( configs[ i ] );
                if( projects.equals( selectedProjects ) ) {
                    candidateConfigs.add( configs[ i ] );
                }
            }
        } catch( CoreException e ) {
            _logger.error( e.getMessage(), e );
        }

        // If there are no existing configs associated with the IType, create
        // one.
        // If there is exactly one config associated with the IType, return it.
        // Otherwise, if there is more than one config associated with the
        // IType, prompt the user to choose one.
        int candidateCount = candidateConfigs.size();
        ILaunchConfiguration ret;
        if( candidateCount < 1 ) {
            ret = createConfiguration( selectedProjects );
        } else if( candidateCount == 1 ) {
            ret = candidateConfigs.get( 0 );
        } else {
            // Prompt the user to choose a config. A null result means the user
            // canceled the dialog, in which case this method returns null,
            // since canceling the dialog should also cancel launching anything.
            ret = chooseConfiguration( candidateConfigs );
        }
        return ret;
    }

    protected ILaunchConfiguration createConfiguration( Set< IProject > projects ) {
        ILaunchConfiguration config = null;
        try {
            IVMInstall defaultVM = LaunchUtils.getDefaultLaunchVM( projects );
            ILaunchConfigurationType configType = getConfigurationType();
            List< String > projectNames = new ArrayList< String >();
            int napps = 0;
            BlackBerryProperties bbprops;
            String lcName = IConstants.UNDERSCORE_STRING;
            for( IProject project : projects ) {
                projectNames.add( project.getName() );
                bbprops = ContextManager.PLUGIN.getBBProperties( project.getName(), false );
                // when determine the launch configuration name, skip library or non-BB projects
                if( bbprops != null && !BlackBerryProject.LIBRARY.equals( bbprops._application.getType() )
                        && NatureUtils.hasBBNature( project ) ) {
                    napps++;
                    lcName = project.getName();
                }
            }
            // if use selects only one project, name the launch configuration as project name
            // otherwise name it "MultiProjects[n]"
            if( napps == 0 ) {
                lcName = IConstants.LC_LIBRARIES;
            } else if( napps > 1 ) {
                lcName = IConstants.LC_MULTIPROJECTS;
            }
            ILaunchConfigurationWorkingCopy wc = configType.newInstance( null, DebugPlugin.getDefault().getLaunchManager()
                    .generateUniqueLaunchConfigurationNameFrom( lcName ) );
            wc.setAttribute( IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
                    RIMSourcePathProvider.RIM_SOURCEPATH_PROVIDER_ID );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_DEPLOYED_PROJECTS, projectNames );
            RIA ria = ContextManager.PLUGIN.getRIA( defaultVM.getInstallLocation().getPath() );
            if( ria == null ) {
                _logger.error( NLS.bind( Messages.RIA_NO_RIA_INSTANCE_ERROR_MSG, defaultVM.getName() ) );
                return null;
            }
            wc.setAttribute( IFledgeLaunchConstants.ATTR_JRE_TYPE, IFledgeLaunchConstants.DEFAULT_JRE_TYPE );
            // wc.setAttribute( IFledgeLaunchConstants.ATTR_JRE_ID, defaultVM.getId() );
            DeviceInfo defaultDevice = LaunchUtils.getDefaultDeviceInfo( defaultVM );
            if( defaultDevice != null ) {
                wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_SIM_DIR, defaultDevice.getDirectory() );
                wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_BUNDLE, defaultDevice.getBundleName() );
                wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_DEVICE, defaultDevice.getDeviceName() );
                wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_CONFIG_FILE, defaultDevice.getConfigName() );
            } else {
                MessageDialog.openError( getShell(), Messages.Launch_Error_Title, Messages.Launch_Error_SimulatorNotAvailable );
                return null;
            }
            wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_PIN, IFledgeLaunchConstants.DEFAULT_PIN_NUMBER );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_LAUNCH_MDSCS, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_LAUNCH_APP_ON_STARTUP, napps == 1 ? lcName : StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_AUTOMATICALLY_USE_DEFAULT_VALUE, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_NUMBER_OF_SECONDS_WAIT_BEFORE_RESPONSE, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_ESN, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_MEID, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_ENABLE_DEVICE_SECURITY, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_SYSTEM_LOCALE, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_GENERAL_KEYBOARD_LOCALE, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_DEBUG_INTERRUPT_DEBUGGER_ON_DEADLOCK, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_DEBUG_DO_NOT_STOP_EXECUTION, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_APPLICATION_HEAP_SIZE, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_BRANDING_DATA, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_RESET_FILE_SYSTEM_ON_STARTUP, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_RESET_NVRAM_ON_STARTUP, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_FILE_SYSTEM_SIZE, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_NOT_SAVE_FLASH_ON_EXIT, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_NOT_COMPACT_FILE_SYSTEM_ON_EXIT, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_SIMULATE_SDCARD_INSERTED, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_FORMAT_SDCARD_ON_STARTUP, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_DESTROY_EXISTING_SDCARD_IMAGE, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_SDCARD_SIZE, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_SDCARD_IMAGE, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_USE_PC_FILESYSTEM_FOR_SDCARD_FILES, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_PC_FILESYSTEM_PATH, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_NOT_SUPPORT_MULTIMEDIA_CARD_SIMULATION, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_MEMORY_NOT_SPLIT_MMC_PARTITION_INTO_DIFFERENT_FILES, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_DISABLE_REGISTRATION, true );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_NETWORKS, Collections.EMPTY_LIST );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_START_WITH_RADIO_OFF, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_PHONE_NUMBERS, Collections.EMPTY_LIST );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_AUTO_ANSWER_OUTGOING_CALL, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_IMEI, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_ICCID, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_IMSI, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_SIMULATE_SIM_NOT_PRESENT, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_SAVE_SIM_CARD_DATA, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_IP_ADDRESS, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_IGNORE_UDP_PORT_CONFLICT, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_SMS_SOURCE_PORT, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_SMS_DESTINATION_PORT, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_NETWORK_PDE_PORT, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_PORTS_USB_CONNECTED, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_PORTS_BLUETOOTH_PORT, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_VIEW_DISABLE_AUTO_BACKLIGHT_SHUTOFF, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_VIEW_HIDE_NETWORK_INFORMATION, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_VIEW_DISPLAY_LCD_ONLY, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_VIEW_PATH_TO_CONFIG_PACK, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_VIEW_LCD_ZOOM, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_VIEW_NOT_SHOW_HELP_FOR_KEY_MAPPING, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_ADVANCED_NOT_SIMULATE_RIM_BATTERY, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_ADVANCED_NOT_USE_PC_NUMPAD_FOR_TRACKBALL, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_USE_CUSTOMIZED_COMMAND_LINE, false );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_CUSTOMIZED_COMMAND_LINE, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_CUSTOMIZED_WORKING_DIRECTORY, StringUtils.EMPTY );
            wc.setAttribute( IFledgeLaunchConstants.ATTR_CUSTOMIZED_MDS_DIRECTORY, StringUtils.EMPTY );

            /** Fix IDT345420 */
            wc.setMappedResources( projects.toArray( new IResource[ projects.size() ] ) );

            config = wc.doSave();
        } catch( CoreException ce ) {
            _logger.error( ce.getMessage(), ce );
        }
        return config;
    }

    /**
     * Show a selection dialog that allows the user to choose one of the specified launch configurations. Return the chosen
     * config, or <code>null</code> if the user canceled the dialog.
     */
    protected ILaunchConfiguration chooseConfiguration( List< ILaunchConfiguration > configList ) {
        IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
        ElementListSelectionDialog dialog = new ElementListSelectionDialog( getShell(), labelProvider );
        dialog.setElements( configList.toArray() );
        dialog.setTitle( "Choose a launch configuration" );
        dialog.setMessage( LauncherMessages.JavaLaunchShortcut_2 );
        dialog.setMultipleSelection( false );
        int result = dialog.open();
        labelProvider.dispose();
        if( result == Window.OK ) {
            return (ILaunchConfiguration) dialog.getFirstResult();
        }
        return null;
    }

    /**
     * Convenience method to get the window that owns this action's Shell.
     */
    protected Shell getShell() {
        return JDIDebugUIPlugin.getActiveWorkbenchShell();
    }

}
