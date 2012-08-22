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
package net.rim.ejde.internal.sourceprovider;

import java.util.HashMap;
import java.util.Map;

import net.rim.ejde.internal.ui.launchers.LaunchUtils;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;
import org.eclipse.ui.services.IServiceLocator;

/**
 *
 * @author bkurz
 *
 */
public class CleanSimulatorSourceProvider extends AbstractSourceProvider {
    private static final String COMMANDSTATE = "net.rim.ejde.internal.cleanSimulator.enableState";
    private static final String ENABLED = "enabled";
    private static final String DISABLED = "disabled";

    private boolean _currentState;

    @Override
    public void initialize( final IServiceLocator locator ) {
        _currentState = LaunchUtils.getRunningBBLaunch() == null;
        DebugPlugin.getDefault().getLaunchManager().addLaunchListener( new ILaunchesListener2() {

            @Override
            public void launchesRemoved( ILaunch[] launches ) {
                setEnableState( LaunchUtils.getRunningBBLaunch() == null );

            }

            @Override
            public void launchesAdded( ILaunch[] launches ) {
                setEnableState( LaunchUtils.getRunningBBLaunch() == null );
            }

            @Override
            public void launchesChanged( ILaunch[] launches ) {
                // Do nothing
            }

            @Override
            public void launchesTerminated( ILaunch[] launches ) {
                setEnableState( LaunchUtils.getRunningBBLaunch() == null );
            }
        }

        );

    }

    @Override
    public void dispose() {
    }

    @Override
    public Map< String, String > getCurrentState() {
        Map< String, String > map = new HashMap< String, String >();
        map.put( COMMANDSTATE, _currentState ? ENABLED : DISABLED );
        return map;
    }

    @Override
    public String[] getProvidedSourceNames() {
        return new String[] { COMMANDSTATE };
    }

    private void setEnableState( boolean value ) {
        _currentState = value;

        // Run asnchronously to avoid SWT exception when projects have been closed.
        Display.getDefault().asyncExec( new Runnable() {
            @Override
            public void run() {
                fireSourceChanged( ISources.WORKBENCH, COMMANDSTATE, _currentState ? ENABLED : DISABLED );
            }
        } );
    }

}
