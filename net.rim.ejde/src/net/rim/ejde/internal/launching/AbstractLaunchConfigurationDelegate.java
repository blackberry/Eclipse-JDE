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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IRIMMarker;
import net.rim.ejde.internal.ui.consoles.SimulatorOutputConsoleFactory;
import net.rim.ejde.internal.ui.launchers.LaunchUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ide.RIA;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.debug.core.JDIDebugModel;
import org.eclipse.jdt.internal.debug.ui.IJDIPreferencesConstants;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMConnector;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.util.NLS;

public abstract class AbstractLaunchConfigurationDelegate extends AbstractJavaLaunchConfigurationDelegate {

    private static final Logger log = Logger.getLogger( AbstractLaunchConfigurationDelegate.class );

    public AbstractLaunchConfigurationDelegate() {
        super();

        // See package org.eclipse.jdt.internal.debug.ui.JavaDebugPreferencePage
        // for more info

        // Debugger timeout
        Preferences corePreferences = JDIDebugModel.getPreferences();
        corePreferences.setValue( JDIDebugModel.PREF_REQUEST_TIMEOUT, 240000 );

        // Launch timeout
        Preferences runtimePreferences = JavaRuntime.getPreferences();
        runtimePreferences.setValue( JavaRuntime.PREF_CONNECT_TIMEOUT, 240000 );

        // don't alert for hot code replace failure
        JDIDebugUIPlugin.getDefault().getPreferenceStore().setValue( IJDIPreferencesConstants.PREF_ALERT_HCR_FAILED, false );
        JDIDebugUIPlugin.getDefault().getPreferenceStore()
                .setValue( IJDIPreferencesConstants.PREF_ALERT_HCR_NOT_SUPPORTED, false );
    }

    @SuppressWarnings({ "unchecked" })//$NON-NLS-1$ //$NON-NLS-2$
    protected void debug( final ILaunchConfiguration configuration, final ILaunch launch, IProgressMonitor monitor )
            throws CoreException {

        try {
            // opens the Simulator Output Console
            SimulatorOutputConsoleFactory launchC = new SimulatorOutputConsoleFactory();
            launchC.openConsole();
        } catch( Exception exception ) {
            log.error( "SimulatorOutputConsoleFactory failed to deploy: " + exception.getMessage() );
        }

        log.debug( "Entering AbstractLaunchConfigurationDelegate debug for" + configuration.getName() ); //$NON-NLS-1$

        if( monitor == null ) {
            monitor = new NullProgressMonitor();
        }

        RIA debugServer = RIA.getCurrentDebugger();
        if( debugServer == null ) {
            return;
        }

        if( debugServer.isDebuggerAttached() ) {
            throw new CoreException(
                    StatusFactory.createErrorStatus( Messages.AbstractLaunchConfigurationDelegate_debuggerActiveMsg ) );
        }

        final String debugDest = getDebugDestination( configuration );
        setDebugAttach( debugServer, debugDest );

        monitor.beginTask( "" //$NON-NLS-1$
                + configuration.getName(), 3 );
        // check for cancellation
        if( monitor.isCanceled() ) {
            return;
        }

        monitor.subTask( "" ); //$NON-NLS-1$

        log.debug( "Configuring jvm connector" ); //$NON-NLS-1$

        String connectorId = IJavaLaunchConfigurationConstants.ID_SOCKET_ATTACH_VM_CONNECTOR;
        IVMConnector connector = JavaRuntime.getVMConnector( connectorId );
        if( connector == null ) {
            abort( "", //$NON-NLS-1$
                    null, IJavaLaunchConfigurationConstants.ERR_CONNECTOR_NOT_AVAILABLE );
        }

        Map< String, String > argMap = configuration.getAttribute(
                IJavaLaunchConfigurationConstants.ATTR_CONNECT_MAP, (Map) null );

        int connectTimeout = JavaRuntime.getPreferences().getInt( JavaRuntime.PREF_CONNECT_TIMEOUT );
        if( argMap == null ) {
            argMap = new HashMap< String, String >();
            argMap.put( "hostname", "localhost" ); //$NON-NLS-1$ //$NON-NLS-2$
            argMap.put( "port", String.valueOf( debugServer.getJDWPPort() ) ); //$NON-NLS-1$
        }
        argMap.put( "timeout", String.valueOf( connectTimeout ) ); //$NON-NLS-1$

        monitor.subTask( "" ); //$NON-NLS-1$

        // check for cancellation\
        if( monitor.isCanceled() ) {
            return;
        }

        monitor.worked( 1 );

        String logMessage = "Connecting to JVM: \n" + "    hostname: [" + argMap.get( "hostname" ) + "]\n" + "    port    : ["
                + argMap.get( "port" ) + "]\n" + "    launch  : \n" + "        Configuration: ["
                + launch.getLaunchConfiguration().getName() + "]\n" + "        Locator      : ["
                + launch.getSourceLocator().getClass().getName() + "]\n";
        log.debug( logMessage );

        // connect to remote VM
        doConnect( connector, argMap, monitor, launch );

        log.debug( "Connected to JVM." );

        // check for cancellation
        if( monitor.isCanceled() ) {
            IDebugTarget[] debugTargets = launch.getDebugTargets();
            for( int i = 0; i < debugTargets.length; i++ ) {
                IDebugTarget target = debugTargets[ i ];
                if( target.canDisconnect() ) {
                    target.disconnect();
                }
            }
            return;
        }

        monitor.done();

        log.debug( "Leaving AbstractLaunchConfigurationDelegate debug()" ); //$NON-NLS-1$
    }

    protected void doConnect( IVMConnector connector, Map< String, String > argMap, IProgressMonitor monitor, ILaunch launch )
            throws CoreException {
        connector.connect( argMap, monitor, launch );
    }

    protected void setDebugAttach( final RIA debugServer, final String debugDest ) throws CoreException {
        log.debug( "debug destination: " + debugDest ); //$NON-NLS-1$
        if( !debugServer.setDebugAttachTo( debugDest ) ) {
            final String errorMsg = NLS.bind( Messages.AbstractLaunchConfigurationDelegate_invalidDebugDestMsg, debugDest );
            throw new CoreException( StatusFactory.createErrorStatus( errorMsg ) );
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.debug.core.model.ILaunchConfigurationDelegate2#finalLaunchCheck(org.eclipse.debug.core.ILaunchConfiguration,
     * java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    public boolean finalLaunchCheck( ILaunchConfiguration configuration, String mode, IProgressMonitor monitor )
            throws CoreException {
        boolean isOk = super.finalLaunchCheck( configuration, mode, monitor );
        if( isOk ) {
            // initialize current debugger
            initializeDebugger( configuration );

            // we need to check code signing errors and packaging errors
            String[] types = new String[] { IRIMMarker.SIGNATURE_TOOL_PROBLEM_MARKER };
            if( ProjectUtils.hasError( ResourcesPlugin.getWorkspace().getRoot(), types ) ) {
                return false;
            }
            types = new String[] { IRIMMarker.PACKAGING_PROBLEM };
            Set< IProject > projects = LaunchUtils.getProjectsFromConfiguration( configuration );
            for( IProject project : projects ) {
                if( ProjectUtils.hasError( project, types ) ) {
                    return false;
                }
            }
            // Deploy projects
            IStatus status = deployProjects( configuration, monitor );
            if( !status.isOK() ) {
                throw new CoreException( status );
            }
        }
        return isOk;
    }

    /**
     * Deploy projects.
     *
     * @param configuration
     * @param monitor
     * @return
     * @throws CoreException
     * @throws OperationCanceledException
     */
    abstract protected IStatus deployProjects( ILaunchConfiguration configuration, IProgressMonitor monitor )
            throws CoreException, OperationCanceledException;

    /**
     * Returns one of RIA.getDebugAttachList
     *
     * @return
     */
    protected abstract String getDebugDestination( ILaunchConfiguration configuration ) throws CoreException;

    public boolean preLaunchCheck( ILaunchConfiguration configuration, String mode, IProgressMonitor monitor )
            throws CoreException {
        Set< IProject > projects = LaunchUtils.getProjectsFromConfiguration( configuration );
        if( projects.isEmpty() ) {
            return false;
        }

        return super.preLaunchCheck( configuration, mode, monitor );
    }

    private void initializeDebugger( ILaunchConfiguration configuration ) throws CoreException {
        RIA ria = ContextManager.PLUGIN.getRIA( LaunchUtils.getVMFromConfiguration( configuration ).getInstallLocation()
                .getPath() );
        if( ria == null ) {
            throw new CoreException( StatusFactory.createErrorStatus( NLS.bind( Messages.RIA_NO_RIA_INSTANCE_ERROR_MSG, VMUtils
                    .getDefaultBBVM().getName() ) ) );
        }
        RIA currentRIA = RIA.getCurrentDebugger();
        if( !currentRIA.equals( ria ) ) {
            RIA.setCurrentDebugger( ria );
        }
    }
}
