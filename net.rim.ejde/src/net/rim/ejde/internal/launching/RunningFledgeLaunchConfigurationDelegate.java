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

import java.util.Map;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.ui.launchers.LaunchUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ide.RIA;
import net.rim.ide.core.Util;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.osgi.util.NLS;

/**
 * Launch delegate for attaching debugger to a currently running BB simulator
 *
 * @author bchabot
 */
public class RunningFledgeLaunchConfigurationDelegate extends AbstractDebugLaunchConfigurationDelegate {

    Logger log = Logger.getLogger( RunningFledgeLaunchConfigurationDelegate.class );

    public RunningFledgeLaunchConfigurationDelegate() {
        // do nothing
    }

    @Override
    protected String getDebugDestination( ILaunchConfiguration configuration ) throws CoreException {
        return RIA.getDebugAttachRunningSimulator();
    }

    @Override
    protected void doConnect( IVMConnector connector, Map< String, String > argMap, IProgressMonitor monitor, ILaunch launch )
            throws CoreException {
        ConnectionThread connThread = new ConnectionThread( connector, argMap, monitor, launch );
        connThread.start();
        while( connThread.isRunning() && !monitor.isCanceled() ) {
            Util.sleep( 2000 );
        }
        if( monitor.isCanceled() ) {
            // cancel the current launch
            RIA ria = RIA.getCurrentDebugger();
            ria.getBaseDebugAPI().fini();
            return;
        }
        if( !connThread.isConnected() ) {
            // cancel the current launch
            RIA ria = RIA.getCurrentDebugger();
            ria.transportConnectDialogCancel( false );
            throw new CoreException( StatusFactory.createErrorStatus( "Failed to connect to running simulator." ) ); //$NON-NLS-1$
        }
    }

    private class ConnectionThread extends Thread {
        private IVMConnector _connector;
        private Map< String, String > _argMap;
        private IProgressMonitor _monitor;
        private ILaunch _launch;
        private boolean _running = true;
        private boolean _connected = false;

        public ConnectionThread( IVMConnector connector, Map< String, String > argMap, IProgressMonitor monitor, ILaunch launch ) {
            _connector = connector;
            _argMap = argMap;
            _monitor = monitor;
            _launch = launch;
        }

        @Override
        public void run() {
            try {
                _connector.connect( _argMap, _monitor, _launch );
                _connected = true;
            } catch( CoreException e ) {
                log.error( e );
            }
            _running = false;
        }

        public boolean isConnected() {
            return _connected;
        }

        public boolean isRunning() {
            return _running;
        }
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

        // Don't allow relaunch if there is another running BB launch
        ILaunch launch = LaunchUtils.getRunningBBLaunch();
        if( launch != null ) {
            throw new CoreException(
                    StatusFactory.createErrorStatus( Messages.AbstractLaunchConfigurationDelegate_debuggerActiveMsg ) );
        }
        return super.preLaunchCheck( configuration, mode, monitor );
    }

}
