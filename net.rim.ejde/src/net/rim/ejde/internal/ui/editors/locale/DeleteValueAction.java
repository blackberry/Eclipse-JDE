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
package net.rim.ejde.internal.ui.editors.locale;

import net.rim.sdk.resourceutil.RIMResourceElement;
import net.rim.sdk.resourceutil.RIMResourceLocale;
import net.rim.sdk.resourceutil.ResourceCollection;
import net.rim.sdk.resourceutil.ResourceElement;
import net.rim.sdk.resourceutil.ResourceLocale;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * The action that deletes a key value
 *
 * @author jkeshavarzi
 *
 */
class DeleteValueAction extends Action {
    private ResourceElement _element;
    private Shell _shell;

    DeleteValueAction( ResourceElement element, Shell shell ) {
        super( "Delete Value" );
        _element = element;
        _shell = shell;
        if( element.isEmpty() ) {
            setEnabled( false );
        }
    }

    public void run() {
        if( confirm() ) {
            ResourceLocale locale = _element.getLocale();

            if( locale.getResourceElement( _element.getKey() ).isMulti() ) {
                locale.deleteValue( _element.getKey() );
                ResourceCollection collection = _element.getLocale().getCollection();
                collection.convertToMultipleValues( _element.getKey() );
            } else {
                locale.deleteValue( _element.getKey() );
            }

            ResourceEditorOptionsDialog.updateVersioningAfterResourceElementEdited( _element );
        }
    }

    private boolean confirm() {
        return checkFileReadOnly();
    }

    private boolean fileWritableOk() {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "Are you sure you want to delete the value for the key '" );
        buffer.append( _element.getKey() );
        buffer.append( "'?" );
        return MessageDialog.openQuestion( _shell, "Resource Editor", buffer.toString() );
    }

    private boolean checkFileReadOnly() {
        if( _element instanceof RIMResourceElement ) {
            RIMResourceLocale locale = (RIMResourceLocale) _element.getLocale();
            if( locale.isRrcFileWritable() == false ) {
                if( MessageDialog.openQuestion( null, "Resource Editor", locale.getRrcFilename()
                        + " is read-only.\nDo you want to mark it read-write?" ) ) {
                    locale.setRrcFileWritable();
                    locale.getCollection().setHeaderFileWritable();
                    return fileWritableOk();
                } else {
                    MessageDialog.openInformation( null, "Resource Editor", "You will not be able to save changes to "
                            + locale.getRrcFilename() + "." );
                    return false;
                }
            } else {
                return fileWritableOk();
            }
        } else {
            return fileWritableOk();
        }
    }

}
