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

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Helper class to advance an IProgressMonitor in equal percentages
 *
 * @author mcacenco
 */
public class Progress {
    /** current actual progress */
    private float _curr = 0;
    /** current integer progress reported */
    private int _icur;
    private float _step;
    IProgressMonitor _monitor;

    /**
     * New instance with 100 total steps
     *
     * @param monitor
     * @param nsteps
     */
    public Progress( IProgressMonitor monitor, int nsteps ) {
        this( monitor, nsteps, 100 );
    }

    /**
     * New instance with specified total steps
     *
     * @param monitor
     * @param nsteps
     * @param totsteps
     */
    public Progress( IProgressMonitor monitor, int nsteps, int totsteps ) {
        _monitor = monitor;
        _step = ( (float) totsteps ) / ( nsteps > 0 ? nsteps : 1 );
    }

    /**
     * works the progres with the calculated increment
     *
     */
    public void worked() {
        _curr += _step;
        int istep = (int) ( _curr - _icur );
        if( istep > 0 ) {
            _icur += istep;
        }
        _monitor.worked( istep );
    }

}
