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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * The action that deletes a key value from all
 *
 * @author jkeshavarzi
 *
 */
class DeleteValueFromAllAction extends Action {
    private ResourceElement _element;
    private Shell _shell;

    DeleteValueFromAllAction( ResourceElement element, Shell shell ) {
        super( "Delete Value from All" );
        _element = element;
        _shell = shell;
    }

    public void run() {
        if( confirm() ) {
            ResourceCollection collection = _element.getLocale().getCollection();
            collection.deleteKey( _element.getKey() );
        }
    }

    private boolean confirm() {
        return checkFileReadOnly();
    }

    private boolean fileWritableOk() {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "Are you sure you want to delete the key '" );
        buffer.append( _element.getKey() );
        buffer.append( "' and its values from ALL resources?" );
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
