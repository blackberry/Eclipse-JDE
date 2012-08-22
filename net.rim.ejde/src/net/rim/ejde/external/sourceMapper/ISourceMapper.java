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

import org.eclipse.core.resources.IFile;

/**
 * Client should implement this interface to map a source file resource to another source file resource.
 */
public interface ISourceMapper {
    /**
     * Return a mapped resource or <code>null</code> if the original resource should be used.
     *
     * @param sourceResource
     * @return
     */
    IFile getMappedResource( IFile sourceFile );

    /**
     * Return the content of the given <code>soruceFile</code>.
     * <p>
     * <b>Currently this method is only called when the {@link ISourceMapper#getMappedResource()} return null. We provide a change
     * to return an empty char array to eclipse compiler to skip compiling the source file.</b>
     *
     * @param sourceFile
     * @return
     */
    char[] getContent( IFile sourceFile );
}
