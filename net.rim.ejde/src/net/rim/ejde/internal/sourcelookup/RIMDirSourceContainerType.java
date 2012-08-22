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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainerTypeDelegate;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A folder in the local file system.
 *
 */
public class RIMDirSourceContainerType extends AbstractSourceContainerTypeDelegate {

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainerType#createSourceContainer(java.lang.String)
     */
    public ISourceContainer createSourceContainer( String memento ) throws CoreException {
        Node node = parseDocument( memento );
        if( node.getNodeType() == Node.ELEMENT_NODE ) {
            Element element = (Element) node;
            if( "directory".equals( element.getNodeName() ) ) { //$NON-NLS-1$
                String string = element.getAttribute( "path" ); //$NON-NLS-1$
                if( string == null || string.length() == 0 ) {
                    abort( SourceLookupMessages.DirectorySourceContainerType_10, null );
                }
                String nest = element.getAttribute( "nest" ); //$NON-NLS-1$
                boolean nested = "true".equals( nest ); //$NON-NLS-1$
                return new RIMDirSourceContainer( new Path( string ), nested );
            }
            abort( SourceLookupMessages.DirectorySourceContainerType_11, null );
        }
        abort( SourceLookupMessages.DirectorySourceContainerType_12, null );
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.debug.internal.core.sourcelookup.ISourceContainerType#getMemento(org.eclipse.debug.internal.core.sourcelookup
     * .ISourceContainer)
     */
    public String getMemento( ISourceContainer container ) throws CoreException {
        RIMDirSourceContainer folder = (RIMDirSourceContainer) container;
        Document document = newDocument();
        Element element = document.createElement( "directory" ); //$NON-NLS-1$
        element.setAttribute( "path", folder.getDirectory().getAbsolutePath() ); //$NON-NLS-1$
        String nest = "false"; //$NON-NLS-1$
        if( folder.isComposite() ) {
            nest = "true"; //$NON-NLS-1$
        }
        element.setAttribute( "nest", nest ); //$NON-NLS-1$
        document.appendChild( element );
        return serializeDocument( document );
    }

}
