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

import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICellEditorValidator;

/**
 * IResourceCellEditor interface
 *
 * @author jkeshavarzi
 *
 */
interface IResourceCellEditor {
    public void addListener( ICellEditorListener listener );

    public String getErrorMessage();

    public Object getValue();

    public void apply();

    public void cancel();

    public void changeValue( Object value );

    public void removeListener( ICellEditorListener listener );

    public void setValidator( ICellEditorValidator validator );
}
