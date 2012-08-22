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
package net.rim.ejde.internal;

import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public class BlackBerryAdapterFactory implements IAdapterFactory {
    static private final Logger _log = Logger.getLogger( BlackBerryAdapterFactory.class );

    public Object getAdapter( Object adaptableObject, Class adapterType ) {
        IJavaProject javaProj;
        IProject iproj;
        try {
            if( adaptableObject instanceof IProject && ( iproj = (IProject) adaptableObject ).isOpen() ) {
                if( iproj.hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                    javaProj = JavaCore.create( iproj );
                    return new BlackBerryProject( javaProj );
                }
            } else if( adaptableObject instanceof IJavaProject
                    && ( javaProj = (IJavaProject) adaptableObject ).getProject().isOpen() ) {
                if( javaProj.getProject().hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                    return new BlackBerryProject( javaProj );
                }
            }
        } catch( CoreException e ) {
            _log.error( "getAdapter: ", e );
        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[] { IProject.class, IJavaProject.class };
    }
}
