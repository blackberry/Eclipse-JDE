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
package net.rim.ejde.internal.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.Diagnostic;

/**
 * @author cbateman
 */
public class DiagnosticManager {
    /** The logger. */
    private static final Logger _logger = Logger.getLogger( DiagnosticManager.class );
    private final Map< IProject, Map< Object, BBDiagnostic >> _registry = new HashMap< IProject, Map< Object, BBDiagnostic >>();
    private final List< IDiagnosticManagerChangeListener > _listeners = new ArrayList< IDiagnosticManagerChangeListener >();
    private boolean _firingEvents = false;

    // TODO: review this way of doing it
    // listeners that get added during event firing
    // they are only added/removed *after* the existing listeners
    // are all processed
    private final List< IDiagnosticManagerChangeListener > _deferredAddListeners = new ArrayList< IDiagnosticManagerChangeListener >();
    private final List< IDiagnosticManagerChangeListener > _deferredRemoveListeners = new ArrayList< IDiagnosticManagerChangeListener >();

    protected synchronized void put( IProject project, Object forThisObject, BBDiagnostic diagnostic ) {
        // This is extra logging to watch for null projects
        if( project == null ) {
            _logger.error( "Null Project has added a diagnostic: " + diagnostic.getMessage() + " for:" + forThisObject );
        }

        Map< Object, BBDiagnostic > projectRegistry = getProjectRegistry( project );

        if( diagnostic.getSeverity() != Diagnostic.OK ) {
            BBDiagnostic oldDiagnostic = projectRegistry.get( forThisObject );
            if( oldDiagnostic == null || !oldDiagnostic.compare( diagnostic ) ) {
                projectRegistry.put( forThisObject, diagnostic );
                fireAddedEvent( project, forThisObject, diagnostic );
            }
        } else {
            projectRegistry.remove( forThisObject );
            fireRemovedEvent( project, forThisObject );
        }
    }

    public BBDiagnostic get( IProject project, Object forThisObject ) {
        BBDiagnostic wdiag = null;
        Map< Object, BBDiagnostic > projectRegistry = getProjectRegistry( project );
        wdiag = projectRegistry.get( forThisObject );
        return wdiag;
    }

    private Map< Object, BBDiagnostic > getProjectRegistry( IProject project ) {
        Map< Object, BBDiagnostic > registry = _registry.get( project );

        if( registry == null ) {
            registry = new HashMap< Object, BBDiagnostic >();
            _registry.put( project, registry );
        }

        return registry;
    }

    protected void removeProjectRegistry( IProject project ) {
        _registry.remove( project );
        fireRemovedProject( project );
    }

    protected void cleanObjectDiags( IProject project, Object obj ) {
        Map< Object, BBDiagnostic > omap = _registry.get( project );
        if( omap != null && omap.size() > 0 ) {
            omap.remove( obj );
        }
    }

    public void addChangeListener( IDiagnosticManagerChangeListener listener ) {
        // if this method is not being called nested in a event firing
        if( !_firingEvents ) {
            if( !_listeners.contains( listener ) ) {
                _listeners.add( listener );
            }
        } else {
            if( !_deferredAddListeners.contains( listener ) ) {
                _deferredAddListeners.add( listener );
            }
        }
    }

    public void removeChangeListener( IDiagnosticManagerChangeListener listener ) {
        if( !_firingEvents ) {
            _listeners.remove( listener );
        } else {
            _deferredRemoveListeners.add( listener );
        }
    }

    protected void fireAddedEvent( IProject project, Object target, BBDiagnostic diagnostic ) {
        _firingEvents = true;
        for( Iterator< IDiagnosticManagerChangeListener > it = _listeners.iterator(); it.hasNext(); ) {
            IDiagnosticManagerChangeListener listener = it.next();
            listener.addedDiagnostic( project, target, diagnostic );
        }
        processDeferredEvents();
    }

    protected void fireRemovedEvent( IProject project, Object target ) {
        _firingEvents = true;
        for( Iterator< IDiagnosticManagerChangeListener > it = _listeners.iterator(); it.hasNext(); ) {
            IDiagnosticManagerChangeListener listener = it.next();
            listener.removedDiagnostic( project, target );
        }
        processDeferredEvents();
    }

    protected void fireRemovedProject( IProject project ) {
        _firingEvents = true;
        for( Iterator< IDiagnosticManagerChangeListener > it = _listeners.iterator(); it.hasNext(); ) {
            IDiagnosticManagerChangeListener listener = it.next();
            listener.removedProject( project );
        }
        processDeferredEvents();
    }

    protected void processDeferredEvents() {
        // removes take precendence
        _firingEvents = false;
        processDeferredAddEvents();
        processDeferredRemoveEvents();
    }

    protected void processDeferredRemoveEvents() {
        _firingEvents = false;
        for( Iterator< IDiagnosticManagerChangeListener > it = _deferredRemoveListeners.iterator(); it.hasNext(); ) {
            IDiagnosticManagerChangeListener listener = it.next();
            removeChangeListener( listener );
            it.remove();
        }
    }

    protected void processDeferredAddEvents() {
        for( Iterator< IDiagnosticManagerChangeListener > it = _deferredAddListeners.iterator(); it.hasNext(); ) {
            IDiagnosticManagerChangeListener listener = it.next();
            addChangeListener( listener );
            it.remove();
        }
    }

}
