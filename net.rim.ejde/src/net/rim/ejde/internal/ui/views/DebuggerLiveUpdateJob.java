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
package net.rim.ejde.internal.ui.views;

import java.util.HashSet;
import java.util.Set;

import net.rim.ejde.internal.util.DebugUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.swt.widgets.Display;

public class DebuggerLiveUpdateJob extends Job implements IDebugEventSetListener {
    private static final Logger _log = Logger.getLogger( DebuggerLiveUpdateJob.class );
    private VarContentDebugView _view;
    private boolean _isLiveUpdateCanceled = false;
    private boolean _skipUpdate = false;
    private Set< IJavaThread > debugThreadList = new HashSet< IJavaThread >();

    public DebuggerLiveUpdateJob( String name, VarContentDebugView view ) {
        super( name );
        DebugPlugin.getDefault().addDebugEventListener( this );
        _view = view;
        _log.trace( "Debugger live update job was created" );
    }

    public void cancelUpdate() {
        _isLiveUpdateCanceled = true;
    }

    public boolean isUpdateCanceled() {
        return _isLiveUpdateCanceled;
    }

    @Override
    protected IStatus run( IProgressMonitor monitor ) {
        _log.trace( "Debugger live update job started" );
        _isLiveUpdateCanceled = false;
        while( !_isLiveUpdateCanceled && !monitor.isCanceled() ) {
            try {
                Thread.sleep( 2000 );
            } catch( InterruptedException e ) {
                _log.error( e );
            }
            try {
                updateUI();
            } catch( CoreException e ) {
                _log.error( e );
            }
        }
        if( monitor.isCanceled() ) {
            _isLiveUpdateCanceled = true;
        }
        return Status.OK_STATUS;
    }

    private void updateUI() throws CoreException {
        if( _isLiveUpdateCanceled ) {
            return;
        }
        if( _skipUpdate ) {
            return;
        }
        ILaunch launch = DebugUtils.getRIMLaunch();
        if( launch == null ) {
            return;
        }
        IDebugTarget debugTarget = launch.getDebugTarget();
        if( debugTarget == null ) {
            return;
        }
        if( hasThreadSuspended( debugTarget ) ) {
            return;
        }
        if( !debugTarget.isSuspended() && debugTarget.canSuspend() ) {
            debugTarget.suspend();
            try {
                Thread.sleep( 1000 );
            } catch( InterruptedException e ) {
                _log.error( e );
            }
            Display.getDefault().syncExec( new Runnable() {

                @Override
                public void run() {
                    try {
                        _log.trace( "Updating debugger view" );
                        if( !_view.getTableView().getTable().isDisposed() ) {
                            _view.refresh();
                        }
                    } catch( CoreException e ) {
                        _log.error( e );
                    }
                }

            } );
            if( debugTarget.canResume() ) {
                resumeDebugger( debugTarget );
            }
        }
    }

    private void resumeDebugger( IDebugTarget debugTarget ) {
        IThread[] threads;
        try {
            threads = debugTarget.getThreads();
        } catch( DebugException e ) {
            _log.error( e );
            return;
        }
        for( int i = 0; i < threads.length; i++ ) {
            if( !debugThreadList.contains( threads[ i ] ) ) {
                try {
                    if( threads[ i ].canResume() ) {
                        threads[ i ].resume();
                    }
                } catch( DebugException e ) {
                    _log.error( e );
                }
            }
        }
    }

    private boolean hasThreadSuspended( IDebugTarget debugTarget ) {
        IThread[] threads;
        try {
            threads = debugTarget.getThreads();
        } catch( DebugException e ) {
            _log.error( e );
            return false;
        }
        for( int i = 0; i < threads.length; i++ ) {
            if( threads[ i ].isSuspended() ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void handleDebugEvents( DebugEvent[] events ) {
        for( int i = 0; i < events.length; i++ ) {
            if( events[ i ].getKind() == DebugEvent.SUSPEND && events[ i ].getDetail() == DebugEvent.BREAKPOINT ) {
                _skipUpdate = true;
                if( events[ i ].getSource() != null && ( events[ i ].getSource() instanceof IJavaThread ) ) {
                    debugThreadList.add( (IJavaThread) events[ i ].getSource() );
                }
            }
            if( events[ i ].getKind() == DebugEvent.RESUME && events[ i ].getDetail() == DebugEvent.CLIENT_REQUEST ) {
                _skipUpdate = false;
                if( events[ i ].getSource() != null && ( events[ i ].getSource() instanceof IJavaThread ) ) {
                    debugThreadList.remove( events[ i ].getSource() );
                }
            }
        }

    }

}
