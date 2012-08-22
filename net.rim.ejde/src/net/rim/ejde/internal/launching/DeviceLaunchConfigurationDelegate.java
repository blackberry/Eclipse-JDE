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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.preferences.SignatureToolPreferences;
import net.rim.ejde.internal.packaging.PackagingJob;
import net.rim.ejde.internal.packaging.PackagingJobWrapper;
import net.rim.ejde.internal.ui.launchers.LaunchUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ide.RIA;
import net.rim.ide.core.Util;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.osgi.util.NLS;

/**
 * config delegate for BB device launch
 *
 * @author bchabot
 *
 */
public class DeviceLaunchConfigurationDelegate extends AbstractDebugLaunchConfigurationDelegate implements IDeviceLaunchConstants {

    Logger _logger = Logger.getLogger( DeviceLaunchConfigurationDelegate.class );

    Timer _refreshTimer;

    /*
     * A Timer class to constantly scan the device USB connection. If the device to which the debugger was attached is
     * disconnected the class disconnects the debugging session The way we check if the USB device has been disconnected is we get
     * a list of devices which are connected to the computer via USB and if the device to which the debugger is attached is not
     * one of the devices in the list we disconnect the debugging session.
     */
    class RefreshTask extends TimerTask {

        @Override
        public void run() {

            // get RIA
            RIA debugServer = RIA.getCurrentDebugger();

            // if RIA is null return
            if( debugServer == null ) {
                return;
            }

            // get a list of degub devices which are attached to the computer
            String[] deviceList = debugServer.getDebugDeviceList();

            // get the machine to which the debugger is attached to (it can be a
            // USB device or a Simulator)
            String debugAttachedTo = debugServer.getDebugAttachTo();

            // boolean flag to keep track if the device to which the debugger is
            // attached is still in the list of connected devices
            boolean containsTarget = false;

            // if debugger is attached to nothing or the debugger is not
            // attached to a USB or if the debugger is not attached
            // return. When we disconnect to a device and disconnect it the
            // "debugAttachedTo" would still not be null but the
            // "isDebuggerAttached" would show the correct status. Thats why I
            // put in the 3rd condition also in the if condition.
            if( debugAttachedTo == null || !( debugAttachedTo.contains( "USB" ) ) //$NON-NLS-1$
                    || !debugServer.isDebuggerAttached() ) {

                return;
            }

            // System.out.println(debugServer.isDebuggerAttached());
            // System.out.println(debugAttachedTo);

            /*
             * go through the device list to see if the device to which the debugger is attached is still in the list of devices.
             */
            for( int i = 0; i < deviceList.length; i++ ) {

                if( debugAttachedTo.equals( deviceList[ i ] ) ) {

                    containsTarget = true; // if the device to which the
                    // debugger is attached is in the
                    // list of devices mark the
                    // containsTarget
                    // flag as true.
                }
            }

            if( !containsTarget ) {

                _logger.debug( "Device unplugged" );

                // if the deviceList is of length 0 or if it does not contain
                // the target get the launches and from the launches
                // get the debugTarget and disconnect the debug Target.
                try {
                    ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
                    if( launches.length != 0 ) {
                        _logger.debug( "Terminating launch: " + launches[ 0 ].getLaunchConfiguration().getName() );
                        _refreshTimer.cancel();
                        LaunchUtils.closeLaunch( launches[ 0 ] );
                        _logger.debug( "Launch terminated." );
                    }
                } catch( DebugException e ) {
                    _logger.error( e );
                }
            }
        }

    }

    /**
     * Deploy projects in the launch configuration into simulator folder. It packages the projects if necessary.
     *
     * @param configuration
     *            The launch configuration
     * @throws CoreException
     * @throws OperationCanceledException
     */
    protected void packageProjects( ILaunchConfiguration configuration ) throws CoreException, OperationCanceledException {
        String jobName = "Packaging " + configuration.getName(); //$NON-NLS-1$
        _logger.debug( jobName );
        Set< IProject > iProjects = LaunchUtils.getProjectsFromConfiguration( configuration );
        Set< BlackBerryProject > bbProjects = ProjectUtils.getBlackBerryProjects( iProjects );
        // for device debugging, we always want to sign the cod files
        boolean needSign = SignatureToolPreferences.getRunSignatureToolAutomatically();
        PackagingJobWrapper packagingJob = new PackagingJobWrapper( jobName, bbProjects,
                needSign ? PackagingJob.SIGN_IF_PROTECTED_API_USED : PackagingJob.SIGN_NO );
        packagingJob.setUser( true );
        packagingJob.schedule();
        try {
            packagingJob.join();
        } catch( InterruptedException e ) {
            _logger.error( e );
        }
    }

    /**
     * Default constructor.
     */
    public DeviceLaunchConfigurationDelegate() {
        // do nothing
    }

    @Override
    protected String getDebugDestination( ILaunchConfiguration configuration ) throws CoreException {
        // fixed DPI221813, we should set the default return value as
        // ANY_DEVICE, so that a user can
        // start the debugger by clicking the run->debug as-> BB device without
        // creating a device debug
        // launch configuration first.
        return configuration.getAttribute( IDeviceLaunchConstants.DEVICE, IDeviceLaunchConstants.ANY_DEVICE );
    }

    /**
     * Override parent method to check for "connect to any device" mode
     *
     * @param device
     * @param debugServer
     * @return
     */
    @Override
    protected void setDebugAttach( final RIA debugServer, String debugDest ) throws CoreException {
        _logger.debug( "Set debugger attach:" + debugDest );
        if( debugDest.equals( IDeviceLaunchConstants.ANY_DEVICE ) ) {
            for( ;; ) {
                String[] attachedDevices = debugServer.getDebugDeviceList();
                if( attachedDevices.length > 0 ) {
                    debugDest = attachedDevices[ 0 ];
                    break;
                }
                Util.sleep( 5000 );
            }
        }
        super.setDebugAttach( debugServer, debugDest );
    }

    @Override
    protected void doConnect( IVMConnector connector, Map< String, String > argMap, IProgressMonitor monitor, ILaunch launch )
            throws CoreException {
        for( ;; ) {
            try {
                _logger.debug( "Connecting to VM connector..." );
                connector.connect( argMap, monitor, launch );
                _logger.debug( "Connected" );
                break;
            } catch( CoreException e ) {
                _logger.debug( "Connection failed, retry" );
                // debug target is not available, wait a while
                Util.sleep( 5000 );
            }
        }
    }

    @Override
    protected void debug( ILaunchConfiguration configuration, final ILaunch launch, IProgressMonitor monitor )
            throws CoreException {
        super.debug( configuration, launch, monitor );
        // create a new refresh timer and task
        _refreshTimer = new Timer( "Device refresh timer" ); //$NON-NLS-1$
        _refreshTimer.scheduleAtFixedRate( new RefreshTask(), REFRESH_TIME, REFRESH_TIME );
    }

    @Override
    public boolean preLaunchCheck( ILaunchConfiguration configuration, String mode, IProgressMonitor monitor )
            throws CoreException {

        IVMInstall vm = LaunchUtils.getVMFromConfiguration( configuration );
        RIA ria = ContextManager.PLUGIN.getRIA( vm.getInstallLocation().getAbsolutePath() );
        if( ria == null ) {
            throw new CoreException( StatusFactory.createErrorStatus( NLS.bind( Messages.RIA_NO_RIA_INSTANCE_ERROR_MSG,
                    vm.getName() ) ) );
        }

        // Fix 788345 (checks if device is still connected before launching)
        String currentDevice = configuration.getAttribute( DEVICE, ANY_DEVICE );
        if( !currentDevice.equals( IDeviceLaunchConstants.ANY_DEVICE )
                && !Arrays.asList( ria.getDebugDeviceList() ).contains( currentDevice ) ) {
            currentDevice = currentDevice.substring( currentDevice.indexOf( '(' ) + 1, currentDevice.indexOf( ')' ) ).trim();
            throw new CoreException( StatusFactory.createErrorStatus( NLS.bind( "USB Device not found. (PIN: " + currentDevice
                    + ')', vm.getId() ) ) );
        }

        ILaunch launch = LaunchUtils.getRunningBBLaunch();
        if( launch != null ) {
            ILaunchConfigurationType launchType = launch.getLaunchConfiguration().getType();
            if( !launchType.getIdentifier().equals( IDeviceLaunchConstants.LAUNCH_CONFIG_ID ) ) {
                throw new CoreException(
                        StatusFactory.createErrorStatus( Messages.AbstractLaunchConfigurationDelegate_debuggerActiveMsg ) );
            }
        }
        return super.preLaunchCheck( configuration, mode, monitor );
    }

}
