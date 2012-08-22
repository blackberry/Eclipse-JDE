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
package net.rim.ejde.internal.util;

import java.util.ArrayList;
import java.util.List;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ide.RIA;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IProcess;

public class DebugUtils {
    static private final Logger log = Logger.getLogger( DebugUtils.class );

    /**
     * Returns the running BlackBerry ILaunch.
     * <p>
     * <b>There should be only one running BlackBerry ILaunch at one time</b>
     *
     * @return
     */
    public static ILaunch getRIMLaunch() throws CoreException {
        DebugPlugin debugPlugin = DebugPlugin.getDefault();
        if( debugPlugin == null ) {
            return null;
        }
        ILaunchManager lm = debugPlugin.getLaunchManager();
        if( lm == null ) {
            return null;
        }
        ILaunch[] launches = lm.getLaunches();
        for( ILaunch launch : launches ) {
            if( launch.getLaunchMode().equals( ILaunchManager.DEBUG_MODE ) && !launch.isTerminated() && isRIMLaunch( launch ) ) {
                return checkLaunch( launch );

            }
        }
        return null;
    }

    /**
     * Subclasses may override to check extra conditions of enablement
     *
     * @throws CoreException
     */
    public static ILaunch checkLaunch( ILaunch launch ) throws CoreException {
        return launch;

    }

    public static boolean hasRIMLaunch( ILaunch[] launches ) {
        for( ILaunch launch : launches ) {
            try {
                if( DebugUtils.isRIMLaunch( launch ) ) {
                    return true;
                }
            } catch( CoreException e ) {
                log.error( e );
            }
        }
        return false;
    }

    public static List< ILaunch > getRIMLaunches( ILaunch[] launches ) {
        List< ILaunch > rimLaunches = new ArrayList< ILaunch >();
        if( launches == null ) {
            return rimLaunches;
        }
        for( int i = 0; i < launches.length; i++ ) {
            try {
                if( DebugUtils.isRIMLaunch( launches[ i ] ) ) {
                    rimLaunches.add( launches[ i ] );
                }
            } catch( CoreException e ) {
                log.error( e );
            }
        }
        return rimLaunches;
    }

    /**
     * Checks if the given debug event is from a RIM launch.
     *
     * @param event
     * @return
     */
    public static boolean isFromRIMLaunch( DebugEvent event ) {
        Object source = event.getSource();
        try {
            if( source instanceof IDebugElement ) {
                IDebugElement debugElement = (IDebugElement) source;
                if( isRIMLaunch( debugElement.getLaunch() ) ) {
                    return true;
                }
            } else if( source instanceof IProcess ) {
                IProcess process = (IProcess) source;
                if( isRIMLaunch( process.getLaunch() ) ) {
                    return true;
                }
            }
            return false;
        } catch( CoreException e ) {
            log.error( e );
            return false;
        }

    }

    public static boolean isRIMLaunch( ILaunch launch ) throws CoreException {
        if( launch.getLaunchConfiguration() == null ) {
            return false;
        }
        String launchTypeID = launch.getLaunchConfiguration().getType().getIdentifier();
        // TODO: is it safe to assume all launch types begin with this
        return launchTypeID.startsWith( getLaunchTypeIDPattern() );
    }

    public static String getLaunchTypeIDPattern() {
        return ContextManager.PLUGIN_ID;
    }

    /**
     * Checks if a RIM debugger is running.
     *
     * @return
     */
    public static boolean isRIMDebuggerRunning() {
        RIA ria = RIA.getCurrentDebugger();
        if( ria == null ) {
            return false;
        }
        if ( ria.getBaseDebugAPI() == null ){
            return false;
        }
        ILaunch launch;
        try {
            launch = DebugUtils.getRIMLaunch();
            if( launch == null ) {
                return false;
            }
        } catch( CoreException e ) {
            log.error( e );
            return false;
        }
        return true;
    }
}
