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
package net.rim.ejde.internal.sourcelookup;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.containers.DirectorySourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;

public class RIMDirSourceContainer extends DirectorySourceContainer {
    public static final String TYPE_ID = "net.rim.ejde.launching.RIMDirSourceContainer"; //$NON-NLS-1$

    public RIMDirSourceContainer( File dir, boolean subfolders ) {
        super( dir, subfolders );
        // TODO Auto-generated constructor stub
    }

    public RIMDirSourceContainer( IPath dirPath, boolean subfolders ) {
        super( dirPath.toFile(), subfolders );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.core.sourcelookup.ISourceContainer#findSourceElements(java.lang.String)
     */
    public Object[] findSourceElements( String name ) throws CoreException {
        ArrayList sources = new ArrayList();
        File directory = getDirectory();
        File file = new File( directory, name );
        if( file.exists() && file.isFile() ) {
            sources.add( new LocalFileStorage( file ) );
        }

        if( sources.isEmpty() )
            return EMPTY;
        return sources.toArray();
    }
}
