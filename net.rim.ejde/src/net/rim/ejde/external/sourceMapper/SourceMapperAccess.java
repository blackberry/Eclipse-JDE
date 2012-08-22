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
package net.rim.ejde.external.sourceMapper;

import net.rim.ejde.internal.builders.PreprocessingBuilder;

import org.eclipse.core.resources.IFile;

/**
 * Provides a static registration location for the currently registered {@link ISourceMapper} instance.
 */
public class SourceMapperAccess {
    // The currently registered source mapper or null if none
    // is currently registered.
    private static ISourceMapper sourceMapper;

    /**
     * Return a mapped source file for the specified file or <code>null</code> if the file is not mapped. This method will check
     * for a currently registered {@link ISourceMapper} instance and use that for the mapping if it has been set.
     *
     * @param file
     * @return
     */
    public static IFile getMappedSourceFile( IFile file ) {
        if( !PreprocessingBuilder.shouldBuiltByJavaBuilder( file ) ) {
            return null;
        }
        IFile mapped = null;
        if( sourceMapper != null ) {
            mapped = sourceMapper.getMappedResource( file );
        }

        return mapped;
    }

    /**
     * {@link ISourceMapper#getContent(IFile)}
     *
     * @param file
     * @return
     */
    public static char[] getContent( IFile file ) {
        char[] content = new char[ 0 ];

        if( sourceMapper != null ) {
            content = sourceMapper.getContent( file );
        }

        return content;
    }

    /**
     * Return a boolean indicating whether the hook code was properly installed.
     *
     * @return
     */
    public static boolean isHookCodeInstalled() {
        // NOTE: Don't change this in the source code.
        // The hook implementation will rewrite this
        // class to return true to this call. This is
        // necessary since the hook is loaded from a different
        // classloader, thus static variables and methods
        // are not shared with the callers.
        return false;
    }

    /**
     * Set the current {@link ISourceMapper} instance used to map SourceFile {@link IFile} instances to a different IFile
     * instance.
     *
     * @param newSourceMapper
     */
    public static void setSourceMapper( ISourceMapper newSourceMapper ) {
        sourceMapper = newSourceMapper;
    }

    // Private constructor for static access
    private SourceMapperAccess() {
    }
}
