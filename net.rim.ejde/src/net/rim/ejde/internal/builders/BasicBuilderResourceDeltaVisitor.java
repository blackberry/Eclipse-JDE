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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This is the basic resource delta visitor used by BB builders, e.g. resource builder and preprocessor. This class is supposed to
 * be sub-classed.
 *
 */
abstract public class BasicBuilderResourceDeltaVisitor extends BasicBuilderResourceVistor implements IResourceDeltaVisitor {

    public BasicBuilderResourceDeltaVisitor( IProgressMonitor monitor ) {
        super( monitor );
    }

    public boolean visit( IResourceDelta delta ) throws CoreException {
        IResource resource = delta.getResource();
        if( !needVisit( resource ) ) {
            return false;
        }
        int kind = delta.getKind();
        switch( kind ) {
            case IResourceDelta.ADDED:
            case IResourceDelta.CHANGED: {
                if( needBuild( resource ) ) {
                    buildResource( resource, _monitor );
                }
                break;
            }
            case IResourceDelta.REMOVED: {
                removeResource( resource, _monitor );
                break;
            }
        }
        return true;
    }

    abstract protected void removeResource( IResource resource, IProgressMonitor monitor ) throws CoreException;
}
