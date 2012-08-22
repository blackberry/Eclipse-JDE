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

import java.io.File;
import java.util.Vector;

import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;
import net.rim.ide.Workspace;
import net.rim.ide.core.IDEError;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;

@InternalFragmentReplaceable
public class InternalWorkspaceDependencyUtils extends WorkspaceDependencyUtils {
    static private Logger _log = Logger.getLogger( InternalWorkspaceDependencyUtils.class );

    /**
     * Store the workspace dependency information, e.g. imported jars, as a user library.
     *
     * @throws CoreException
     *
     */
    static public void storeDependenciesAsUserLibrary( Workspace workspace, String userLibrary ) throws CoreException {
        if( null == workspace || null == userLibrary )
            throw new IllegalArgumentException( new NullPointerException() );

        Vector< File > importJars = null;
        try {
            importJars = workspace.getImportJars();
        } catch( IDEError e ) {
            _log.error( e );
        }

        if( null == importJars )
            throw new IllegalArgumentException( new NullPointerException() );

        File[] jarFiles = importJars.toArray( new File[ importJars.size() ] );
        storeDependenciesAsUserLibrary( jarFiles, userLibrary );
    }
}
