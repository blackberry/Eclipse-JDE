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
package net.rim.ejde.internal.ui.editors.key;

import net.rim.ejde.internal.core.IConstants;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorMatchingStrategy;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;

public class KeyEditorMatchingStrategy implements IEditorMatchingStrategy {

    private static final Logger _logger = Logger.getLogger( KeyEditorMatchingStrategy.class );

    @Override
    public boolean matches( IEditorReference editorRef, IEditorInput input ) {
        // don't open a new editor if there is already one opened in the same project
        if( editorRef.getEditor( false ) instanceof PrivateKeyEditor ) {
            IFile newFile = (IFile) input.getAdapter( IFile.class );
            if( newFile.getFileExtension().equalsIgnoreCase( IConstants.KEY_FILE_EXTENSION ) ) {
                IFile existingFile = null;
                try {
                    existingFile = (IFile) ( editorRef.getEditorInput().getAdapter( IFile.class ) );
                } catch( PartInitException e ) {
                    _logger.error( e );
                }
                if( existingFile != null && newFile.getProject() == existingFile.getProject() ) {
                    PrivateKeyEditor editor = (PrivateKeyEditor) editorRef.getEditor( false );
                    String keyFile = newFile.getProjectRelativePath().toOSString();
                    editor.switchKey( keyFile );
                    return true;
                }
            }
        }
        return false;
    }
}
