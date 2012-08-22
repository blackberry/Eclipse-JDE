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
package net.rim.ejde.internal.builders;

import net.rim.ejde.internal.util.PackageUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This is the basic resource visitor used by BB builders, e.g. resource builder and preprocessor. This class is supposed to be
 * sub-classed.
 *
 */
abstract public class BasicBuilderResourceVistor implements IResourceVisitor {
    IProgressMonitor _monitor;

    public BasicBuilderResourceVistor( IProgressMonitor monitor ) {
        _monitor = monitor;
    }

    public boolean visit( IResource resource ) throws CoreException {
        if( !needVisit( resource ) ) {
            return false;
        }
        if( needBuild( resource ) ) {
            buildResource( resource, getProgressMonitor() );
        }
        return true;
    }

    protected IProgressMonitor getProgressMonitor() {
        return _monitor;
    }

    protected boolean needVisit( IResource resource ) {
        if( resource instanceof IProject ) {
            return true;
        }
        // we do not process a derived resource
        if( resource.isDerived() ) {
            return false;
        }
        // we do not process a resource which is not in a source folder
        if( !PackageUtils.isUnderSrcFolder( resource ) ) {
            return false;
        }

        return true;
    }

    abstract protected boolean needBuild( IResource resource );

    abstract protected void buildResource( IResource resource, IProgressMonitor monitor ) throws CoreException;
}
