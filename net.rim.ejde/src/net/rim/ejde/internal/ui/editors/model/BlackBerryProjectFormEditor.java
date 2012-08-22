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
package net.rim.ejde.internal.ui.editors.model;

import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;

import org.eclipse.ui.PartInitException;

@InternalFragmentReplaceable
public class BlackBerryProjectFormEditor extends BlackBerryProjectFormEditorBase {

    protected void doAddPages() {
        try {
            // Set application icon to be used by all pages
            _applicationImage = setApplicationImage();

            _applicationPage = new BlackBerryProjectApplicationPage( this );
            addPage( _applicationPage );

            _buildPage = new BlackBerryProjectBuildPage( this );
            addPage( _buildPage );

            _aepPage = new BlackBerryProjectAlternateEntryPointPage( this );
            addPage( _aepPage );

        } catch( PartInitException e ) {
            _log.error( "Error constructing pages", e ); //$NON-NLS-1$
        }
    }

}
