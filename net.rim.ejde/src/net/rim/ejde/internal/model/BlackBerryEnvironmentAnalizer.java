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

import net.rim.ejde.internal.core.IConstants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.CompatibleEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentAnalyzerDelegate;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;

/**
 * Execution environments (EEs) are symbolic representations of JREs. For example, rather than talking about a specific JRE, with
 * a specific name at a specific location on your disk, you can talk about the J2SE-1.4 execution environment. The system can then
 * be configured to use a specific JRE to implement that execution environment. Execution environments are relevant both to
 * development (compile) time and runtime.
 *
 * The EE analyzer delegate analyzes VM installs for compatibility with execution environments.
 */

public class BlackBerryEnvironmentAnalizer implements IExecutionEnvironmentAnalyzerDelegate {

    public BlackBerryEnvironmentAnalizer() {
    }

    /**
     * For now it retrieves a hard-coded predefined EE as in plugin.xml; Next version it can get more sophisticated.
     *
     * @param vm
     *            - likely a BB vm
     * @param monitor
     * @return - for BlackBerry vm(s) return the predefined BB EE
     */
    public CompatibleEnvironment[] analyze( IVMInstall vm, IProgressMonitor monitor ) throws CoreException {
        if( vm.getId().contains( IConstants.BLACKBERRY_JRE_PREFIX ) ) {
            IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
            IExecutionEnvironment env = manager.getEnvironment( IConstants.BLACKBERRY_EXECUTION_ENV );
            if( env != null )
                return new CompatibleEnvironment[] { new CompatibleEnvironment( env, true ) };
        }
        return new CompatibleEnvironment[ 0 ];
    }
}
