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
 * The action that converts single keys to multi value keys
 *
 * @author jkeshavarzi
 *
 */
class ConvertToMultipleValuesAction extends Action {
    private ResourceElement _element;
    private Shell _shell;

    ConvertToMultipleValuesAction( ResourceElement element, Shell shell ) {
        super( "Convert to Multiple Values" );
        _element = element;
        _shell = shell;
        if( element.isMulti() ) {
            setEnabled( false );
        }
    }

    public void run() {
        if( confirm() ) {
            ResourceCollection collection = _element.getLocale().getCollection();
            collection.convertToMultipleValues( _element.getKey() );

            ResourceEditorOptionsDialog.updateVersioningAfterResourceElementEdited( _element );
        }
    }

    private boolean confirm() {
        return checkFileReadOnly();
    }

    private boolean fileWritableOk() {
        StringBuffer buffer = new StringBuffer();
        buffer.append( "This will convert single values to a list of multiple values for the key '" );
        buffer.append( _element.getKey() );
        buffer.append( "' in all applicable resources.\n\n" );
        buffer.append( "Are you sure?" );
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
