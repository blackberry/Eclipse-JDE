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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.WorkspaceSourceContainer;

public class RIMSourceLookupDirector extends AbstractSourceLookupDirector {
    private static Set fFilteredTypes;

    static {
        fFilteredTypes = new HashSet();
        fFilteredTypes.add( ProjectSourceContainer.TYPE_ID );
        fFilteredTypes.add( WorkspaceSourceContainer.TYPE_ID );
        // can't reference UI constant
        fFilteredTypes.add( "org.eclipse.debug.ui.containerType.workingSet" ); //$NON-NLS-1$
        fFilteredTypes.add( RIMDirSourceContainer.TYPE_ID );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.internal.core.sourcelookup.ISourceLookupDirector#initializeParticipants()
     */
    public void initializeParticipants() {
        addParticipants( new ISourceLookupParticipant[] { new RIMSourceLookupParticipant() } );
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.debug.internal.core.sourcelookup.ISourceLookupDirector#supportsSourceContainerType(org.eclipse.debug.internal
     * .core.sourcelookup.ISourceContainerType)
     */
    public boolean supportsSourceContainerType( ISourceContainerType type ) {
        return !fFilteredTypes.contains( type.getId() );
    }

}
