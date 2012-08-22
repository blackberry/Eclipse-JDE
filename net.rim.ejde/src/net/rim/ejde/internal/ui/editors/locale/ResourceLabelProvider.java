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

import net.rim.sdk.resourceutil.ResourceElement;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * The label provider used in the resource editor
 *
 * @author jkeshavarzi
 *
 */
public class ResourceLabelProvider extends LabelProvider implements ITableLabelProvider {
    public Image getColumnImage( Object element, int columnIndex ) {
        return null;
    }

    public String getColumnText( Object element, int columnIndex ) {
        if( element instanceof ResourceElement ) {
            switch( columnIndex ) {
                case ResourceEditorPage.KEY_COLUMN_INDEX:
                    return ( (ResourceElement) element ).getKey();
                case ResourceEditorPage.VALUE_COLUMN_INDEX:
                    return ( (ResourceElement) element ).getValueAsString();
            }
        }
        return null;
    }
}
