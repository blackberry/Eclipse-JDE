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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

import com.sun.jdi.VMDisconnectedException;

public class RIMSourceLookupParticipant extends JavaSourceLookupParticipant {

    /**
     *
     * Returns the source name associated with the given object, or <code>null</code> if none.
     *
     * @param object
     *            an object with an <code>IJavaStackFrame</code> adapter, an IJavaValue or an IJavaType
     * @return the source name associated with the given object, or <code>null</code> if none
     * @exception CoreException
     *                if unable to retrieve the source name
     * @see org.eclipse.jdt.internal.debug.core.JavaDebugUtils#getSourceName(Object)
     */
    public String getSourceName( Object object ) throws CoreException {
        if( object instanceof String ) {
            // assume it's a file name
            return (String) object;
        }
        IJavaStackFrame frame = null;
        if( object instanceof IAdaptable ) {
            frame = (IJavaStackFrame) ( (IAdaptable) object ).getAdapter( IJavaStackFrame.class );
        }
        String typeName = null;
        try {
            if( frame != null ) {
                if( frame.isObsolete() ) {
                    return null;
                }
                String sourceName = frame.getSourcePath();
                // TODO: this may break fix to bug 21518
                if( sourceName == null ) {
                    // no debug attributes, guess at source name
                    typeName = frame.getDeclaringTypeName();
                } else {
                    return sourceName;
                }
            } else {
                if( object instanceof IJavaValue ) {
                    // look at its type
                    object = ( (IJavaValue) object ).getJavaType();
                }
                if( object instanceof IJavaReferenceType ) {
                    IJavaReferenceType refType = (IJavaReferenceType) object;
                    String[] sourcePaths = refType.getSourcePaths( null );
                    if( sourcePaths != null && sourcePaths.length > 0 ) {
                        return sourcePaths[ 0 ];
                    }
                }
                if( object instanceof IJavaType ) {
                    typeName = ( (IJavaType) object ).getName();
                }
            }
        } catch( DebugException e ) {
            int code = e.getStatus().getCode();
            if( code == IJavaThread.ERR_THREAD_NOT_SUSPENDED || code == IJavaStackFrame.ERR_INVALID_STACK_FRAME
                    || e.getStatus().getException() instanceof VMDisconnectedException ) {
                return null;
            }
            throw e;
        }
        if( typeName != null ) {
            return generateSourceName( typeName );
        }
        return null;
    }

    /**
     *
     * Generates and returns a source file path based on a qualified type name. For example, when <code>java.lang.String</code> is
     * provided, the returned source name is <code>java/lang/String.java</code>.
     *
     * @param qualifiedTypeName
     *            fully qualified type name that may contain inner types denoted with <code>$</code> character
     * @return a source file path corresponding to the type name
     * @see org.eclipse.jdt.internal.debug.core.JavaDebugUtils#generateSourceName(String)
     */
    public static String generateSourceName( String qualifiedTypeName ) {
        int index = qualifiedTypeName.lastIndexOf( '.' );
        if( index < 0 ) {
            index = 0;
        }
        qualifiedTypeName = qualifiedTypeName.replace( '.', File.separatorChar );
        index = qualifiedTypeName.indexOf( '$' );
        if( index >= 0 ) {
            qualifiedTypeName = qualifiedTypeName.substring( 0, index );
        }
        if( qualifiedTypeName.length() == 0 ) {
            // likely a proxy class (see bug 40815)
            qualifiedTypeName = null;
        } else {
            qualifiedTypeName = qualifiedTypeName + ".java"; //$NON-NLS-1$
        }
        return qualifiedTypeName;
    }

}
