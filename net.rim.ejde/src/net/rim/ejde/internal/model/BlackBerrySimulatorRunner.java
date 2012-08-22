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
package net.rim.ejde.internal.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.launching.AbstractVMRunner;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

/**
 * @author cmalinescu
 *
 */
public class BlackBerrySimulatorRunner extends AbstractVMRunner {
    /**
     * The VM install instance
     */
    protected IVMInstall _VMInstance;

    /**
     *
     */
    public BlackBerrySimulatorRunner() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Creates a new launcher
     */
    public BlackBerrySimulatorRunner( IVMInstall vmInstance ) {
        _VMInstance = vmInstance;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.AbstractVMRunner#getPluginIdentifier()
     */
    @Override
    protected String getPluginIdentifier() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void run( VMRunnerConfiguration configuration, ILaunch launch, IProgressMonitor monitor ) throws CoreException {
        // TODO Auto-generated method stub

    }
}
