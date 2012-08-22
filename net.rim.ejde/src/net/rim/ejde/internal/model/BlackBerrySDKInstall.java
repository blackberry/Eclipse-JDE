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

import java.io.File;

import net.rim.ejde.internal.core.IConstants;

import org.eclipse.jdt.launching.IVMInstallType;

/**
 * @author cmalinescu
 *
 */
public class BlackBerrySDKInstall extends BlackBerryStandardVMInstall {

    /**
     * Attribute key for Java version property
     */
    public static final String ATTR_JAVA_VERSION = "ATTR_JAVA_VERSION"; //$NON-NLS-1$

    /**
     * Attribute key for supported execution environment by this runtime
     */
    public static final String ATTR_EXECUTION_ENVIRONMENT_ID = "ATTR_EXECUTION_ENVIRONMENT_ID"; //$NON-NLS-1$

    /**
     * Attribute key for Java executable used by this VM
     */
    public static final String ATTR_JAVA_EXE = "ATTR_JAVA_EXE"; //$NON-NLS-1$

    /**
     * Attribute key for VM debug arguments
     */
    public static final String ATTR_DEBUG_ARGS = "ATTR_DEBUG_ARGS"; //$NON-NLS-1$

    /**
     * Path to file used to define the JRE
     */
    public static final String ATTR_DEFINITION_FILE = "ATTR_DEFINITION_FILE"; //$NON-NLS-1$

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.internal.launching.StandardVM#getJavaVersion()
     */
    public String getJavaVersion() {
        return getAttribute( ATTR_JAVA_VERSION );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.internal.launching.StandardVM#getJavaExecutable()
     */
    File getJavaExecutable() {
        String exe = getAttribute( ATTR_JAVA_EXE );
        if( exe != null ) {
            return new File( exe );
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.internal.launching.StandardVM#getDebugArgs()
     */
    public String getDebugArgs() {
        return getAttribute( ATTR_DEBUG_ARGS );
    }

    /**
     *
     */
    public BlackBerrySDKInstall( IVMInstallType type, String id ) {
        super( type, id );
    }

    /**
     * Get the version of this BlackBerry JRE.
     *
     * @return
     */
    public String getVMVersion() {
        String version = getAttribute( BlackBerryVMInstallType.ATTR_RAPC_OUTPUT_FOLDER );
        if( version.equalsIgnoreCase( IConstants.HEADVER_VM_OUTPUTFOLDER ) ) {
            version = IConstants.HEADVER_VM_VERSION;
        }
        return version;
    }
}
