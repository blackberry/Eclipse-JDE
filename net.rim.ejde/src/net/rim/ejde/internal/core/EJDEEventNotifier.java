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

import java.util.ArrayList;
import java.util.List;

import net.rim.ejde.internal.model.BlackBerrySDKInstall;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IVMInstall;

/**
 * The Class EJDEEventNotifier.
 */
public class EJDEEventNotifier {

    private static class EJDEEventNotifierHolder {
        public static EJDEEventNotifier INSTANCE = new EJDEEventNotifier();
    }

    private Logger _logger = Logger.getLogger( EJDEEventNotifier.class );

    private List< IEJDEEventListener > _listeners;

    private EJDEEventNotifier() {
        _listeners = new ArrayList< IEJDEEventListener >();
    }

    /**
     * Gets the single instance of EJDEEventNotifier.
     *
     * @return single instance of EJDEEventNotifier
     */
    public static EJDEEventNotifier getInstance() {
        return EJDEEventNotifierHolder.INSTANCE;
    }

    /**
     * Adds the bridge event listener.
     *
     * @param listener
     *            the listener
     */
    public synchronized void addEJDEEventListener( IEJDEEventListener listener ) {
        if( !_listeners.contains( listener ) ) {
            _listeners.add( listener );
        }
    }

    /**
     * Removes the bridge event listener.
     *
     * @param listener
     *            the listener
     *
     * @return true, if successful
     */
    public synchronized boolean removeEJDEEventListener( IEJDEEventListener listener ) {
        return _listeners.remove( listener );
    }

    /**
     * Notify workspace pre processor tag change.
     *
     * @param previous
     *            the previous
     * @param current
     *            the current
     */
    public synchronized void notifyWorkspaceJREChange( final IVMInstall previous, final IVMInstall current ) {
        for( final IEJDEEventListener listener : _listeners ) {
            SafeRunner.run( new ISafeRunnable() {
                public void handleException( Throwable exception ) {
                    _logger.error( exception );
                }

                public void run() throws Exception {
                    // The listener can somehow be disposed
                    if( listener != null ) {
                        listener.workspaceJREChanged( previous, current );
                    }
                }
            } );
        }
    }

    /**
     * Notify project pre processor tag change.
     *
     * @param sourceVM
     *            the source vm
     */
    public synchronized void notifyJREDefinitionChanged( final BlackBerrySDKInstall sourceVM ) {
        for( final IEJDEEventListener listener : _listeners ) {
            SafeRunner.run( new ISafeRunnable() {
                public void handleException( Throwable exception ) {
                    _logger.error( exception );
                }

                public void run() throws Exception {
                    // The listener can somehow be disposed
                    if( listener != null ) {
                        listener.jreDefinitionChanged( sourceVM );
                    }
                }
            } );
        }
    }

    /**
     * Notify project pre processor tag change.
     *
     * @param project
     *            the project
     */
    public synchronized void notifyNewProjectCreated( final IJavaProject project ) {
        for( final IEJDEEventListener listener : _listeners ) {
            SafeRunner.run( new ISafeRunnable() {
                public void handleException( Throwable exception ) {
                    _logger.error( exception );
                }

                public void run() throws Exception {
                    // The listener can somehow be disposed
                    if( listener != null ) {
                        listener.newProjectCreated( project );
                    }
                }
            } );
        }
    }

    /**
     * Notify project pre processor tag change.
     *
     * @param project
     *            the project
     * @param isProjectJREChange
     *            the is project jre change
     */
    public synchronized void notifyClassPathChanged( final IJavaProject project, final boolean isProjectJREChange ) {
        for( final IEJDEEventListener listener : _listeners ) {
            SafeRunner.run( new ISafeRunnable() {
                public void handleException( Throwable exception ) {
                    _logger.error( exception );
                }

                public void run() throws Exception {
                    // The listener can somehow be disposed
                    if( listener != null ) {
                        listener.classPathChanged( project, isProjectJREChange );
                    }
                }
            } );
        }
    }

    /**
     * Notify project pre processor tag change.
     */
    public synchronized void notifyWorkspacePreprocessorTagsChanged() {
        for( final IEJDEEventListener listener : _listeners ) {
            SafeRunner.run( new ISafeRunnable() {
                public void handleException( Throwable exception ) {
                    _logger.error( exception );
                }

                public void run() throws Exception {
                    // The listener can somehow be disposed
                    if( listener != null ) {
                        listener.workspacePreprocessorTagsChanged();
                    }
                }
            } );
        }
    }

    /**
     * Notify project pre processor tag change.
     *
     * @param project
     *            the project
     */
    public synchronized void notifyProjectPropertiesChanged( final IProject project ) {
        for( final IEJDEEventListener listener : _listeners ) {
            SafeRunner.run( new ISafeRunnable() {
                public void handleException( Throwable exception ) {
                    _logger.error( exception );
                }

                public void run() throws Exception {
                    // The listener can somehow be disposed
                    if( listener != null ) {
                        listener.projectPreprocessorTagChanged( project );
                    }
                }
            } );
        }
    }
}
