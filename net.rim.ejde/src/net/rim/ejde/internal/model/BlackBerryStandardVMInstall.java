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

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.internal.launching.StandardVMDebugger;
import org.eclipse.jdt.internal.launching.StandardVMRunner;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.IVMRunner;

public class BlackBerryStandardVMInstall extends AbstractVMInstall {

    /**
     * If a StandardVM returns a string for #getDebugArgs(), the string may contain the variable ${port}. This will be replaced
     * with the port that the vm is using when launching.
     */
    public static final String VAR_PORT = "${port}"; //$NON-NLS-1$

    public BlackBerryStandardVMInstall( IVMInstallType type, String id ) {
        super( type, id );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.IVMInstall#getVMRunner(java.lang.String)
     */
    public IVMRunner getVMRunner_Org( String mode ) {
        if( ILaunchManager.RUN_MODE.equals( mode ) ) {
            return new StandardVMRunner( this );
        } else if( ILaunchManager.DEBUG_MODE.equals( mode ) ) {
            return new StandardVMDebugger( this );
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.IVMInstall#getVMRunner(java.lang.String)
     */
    public IVMRunner getVMRunner( String mode ) {
        if( ILaunchManager.RUN_MODE.equals( mode ) ) {
            return new BlackBerrySimulatorRunner( this );
        } else if( ILaunchManager.DEBUG_MODE.equals( mode ) ) {
            return new BlackBerryDebuggerRunner( this );
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.IVMInstall#getJavaVersion()
     */
    public String getJavaVersion() {
        BlackBerryVMStandardInstallType installType = (BlackBerryVMStandardInstallType) getVMInstallType();
        File installLocation = getInstallLocation();
        if( installLocation != null ) {
            File executable = getJavaExecutable();
            if( executable != null ) {
                String vmVersion = installType.getVMVersion( installLocation, executable );
                // strip off extra info
                StringBuffer version = new StringBuffer();
                for( int i = 0; i < vmVersion.length(); i++ ) {
                    char ch = vmVersion.charAt( i );
                    if( Character.isDigit( ch ) || ch == '.' ) {
                        version.append( ch );
                    } else {
                        break;
                    }
                }
                if( version.length() > 0 ) {
                    return version.toString();
                }
            }
        }
        return null;
    }

    /**
     * Returns the java executable for this VM or <code>null</code> if cannot be found
     *
     * @return executable for this VM or <code>null</code> if none
     */
    File getJavaExecutable() {
        File installLocation = getInstallLocation();
        if( installLocation != null ) {
            return StandardVMType.findJavaExecutable( installLocation );
        }
        return null;
    }

    /**
     * Returns arguments used to start this VM in debug mode or <code>null</code> if default arguments should be used.
     *
     * @return arguments used to start this VM in debug mode or <code>null</code> if default arguments should be used
     */
    public String getDebugArgs() {
        return null;
    }

}
