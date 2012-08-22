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

import org.eclipse.jdt.launching.IVMInstall;

/**
 * @author cmalinescu
 *
 */
public class BlackBerryDebuggerRunner extends BlackBerrySimulatorRunner {

    /**
     * @since 3.3 OSX environment variable specifying JRE to use
     */
    protected static final String JAVA_JVM_VERSION = "JAVA_JVM_VERSION"; //$NON-NLS-1$

    /**
     * Jre path segment descriptor
     *
     * String equals the word: <code>jre</code>
     *
     * @since 3.3.1
     */
    protected static final String JRE = "jre"; //$NON-NLS-1$

    /**
     * Bin path segment descriptor
     *
     * String equals the word: <code>bin</code>
     *
     * @since 3.3.1
     */
    protected static final String BIN = "bin"; //$NON-NLS-1$

    /**
     *
     */
    public BlackBerryDebuggerRunner() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Creates a new launcher
     */
    public BlackBerryDebuggerRunner( IVMInstall vmInstance ) {
        super( vmInstance );
    }

}
