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

import java.io.File;
import java.io.IOException;

import net.rim.ejde.internal.util.ComponentPackUtils;
import net.rim.ejde.internal.util.VMToolsUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

public final class BundleListenerHandler implements SynchronousBundleListener {
    static final private Logger _log = Logger.getLogger( BundleListenerHandler.class );

    static private BundleListenerHandler INSTANCE;

    static synchronized BundleListenerHandler getInstance( BundleContext context ) {
        if( null == INSTANCE ) {
            INSTANCE = new BundleListenerHandler();
            context.addBundleListener( INSTANCE );
        }
        return INSTANCE;
    }

    static void removeInstance( BundleContext context ) {
        context.removeBundleListener( INSTANCE );
        INSTANCE = null;
    }

    public void bundleChanged( BundleEvent event ) {
        Bundle bundle = event.getBundle();
        String bundleSymbolicName = bundle.getSymbolicName();

        if( bundleSymbolicName.equals( ContextManager.PLUGIN_ID ) ) {
            switch( event.getType() ) {
                case BundleEvent.STARTED: {
                    synchronized( INSTANCE ) {
                        try {
                            IPath vmToolsDirPath = VMToolsUtils.getVMToolsFolderPath();
                            File vmToolsDir = vmToolsDirPath.toFile();
                            if( !vmToolsDir.exists() ) {
                                _log.debug( "Attempting To Create vmTools Directory: " + vmToolsDirPath.toString() );
                                boolean dirCreationStatus = vmToolsDir.mkdirs();
                                if( !dirCreationStatus ) {
                                    _log.error( "Failed To Create vmTools Directory" );
                                }
                            }
                            vmToolsDir.setWritable( true );

                            // Install all VMs
                            ComponentPackUtils.initialLoad();

                        } catch( IOException ioe ) {
                            _log.error( ioe );
                        }
                    }
                }
                default: {
                    logChange( event );
                }
            }
        } else if( bundleSymbolicName.contains( IConstants.CP_EXTENSION_POINT_ID ) ) {
            switch( event.getType() ) {
                default: {
                    logChange( event );
                }
            }
        }
    }

    private void logChange( BundleEvent event ) {
        String msg = "Unknown";

        switch( event.getType() ) {
            case BundleEvent.STARTING: {
                msg = "Starting";
                break;
            }
            case BundleEvent.STARTED: {
                msg = "Started";
                break;
            }
            case BundleEvent.INSTALLED: {
                msg = "Installed";
                break;
            }
            case BundleEvent.UNINSTALLED: {
                msg = "UnInstalled";
                break;
            }
            case BundleEvent.RESOLVED: {
                msg = "Resolved";
                break;
            }
            case BundleEvent.UNRESOLVED: {
                msg = "UnResolved";
                break;
            }

            case BundleEvent.STOPPED: {
                msg = "Stopped";
                break;
            }
            case BundleEvent.STOPPING: {
                msg = "Stopping";
                break;
            }
            case BundleEvent.LAZY_ACTIVATION: {
                msg = "Lazy Activation";
                break;
            }
            case BundleEvent.UPDATED: {
                msg = "Updated";
                break;
            }
        }
        _log.debug( "Bundle [" + event.getBundle().getSymbolicName() + "] is changing state to " + msg + "." );
    }
} // end BundleListenerHandler
