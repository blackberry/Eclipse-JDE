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
import net.rim.sdk.resourceutil.ResourceElement;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;

class PropertiesAction extends Action {
    private ResourceCellEditorUIFactory _uiFactory;
    private ResourceElement _element;

    PropertiesAction( ResourceCellEditorUIFactory uiFactory, ResourceElement element ) {
        super( "Properties" );
        _uiFactory = uiFactory;
        _element = element;
    }

    public void run() {
        if( _element instanceof RIMResourceElement ) {
            RIMResourceLocale locale = (RIMResourceLocale) _element.getLocale();
            if( locale.isRrcFileWritable() == false ) {
                if( MessageDialog.openQuestion( null, "Resource Editor", locale.getRrcFilename()
                        + " is read-only.\nDo you want to mark it read-write?" ) ) {
                    locale.setRrcFileWritable();
                    locale.getCollection().setHeaderFileWritable();
                    _uiFactory.createCellEditorDialog( _element ).display();
                } else {
                    MessageDialog.openInformation( null, "Resource Editor", "You will not be able to save changes to "
                            + locale.getRrcFilename() + "." );
                    return;
                }
            } else {
                _uiFactory.createCellEditorDialog( _element ).display();
            }
        } else {
            _uiFactory.createCellEditorDialog( _element ).display();
        }
    }
}
