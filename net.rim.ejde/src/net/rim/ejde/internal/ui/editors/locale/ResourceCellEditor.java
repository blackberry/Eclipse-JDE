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

import java.util.Vector;

import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICellEditorValidator;

class ResourceCellEditor implements IResourceCellEditor {
    private String _errorMessage;
    private Vector< ICellEditorListener > _listeners;
    private ICellEditorValidator _validator;
    private Object _value;

    ResourceCellEditor( Object value ) {
        _value = value;
        _listeners = new Vector< ICellEditorListener >();
    }

    public void addListener( ICellEditorListener listener ) {
        if( listener == null ) {
            throw new NullPointerException();
        }

        if( !_listeners.contains( listener ) ) {
            _listeners.add( listener );
        }
    }

    public String getErrorMessage() {
        return _errorMessage;
    }

    public void apply() {
        fireApplyEvent();
    }

    public void cancel() {
        fireCancelEvent();
    }

    public void changeValue( Object value ) {
        boolean oldValidState = isValueValid();
        boolean newValidState = true;
        _value = value;
        _errorMessage = null;
        if( _validator != null ) {
            _errorMessage = _validator.isValid( value );
            newValidState = isValueValid();
        }
        fireValueChangedEvent( oldValidState, newValidState );
    }

    public Object getValue() {
        if( isValueValid() ) {
            return _value;
        }
        return null;
    }

    public void removeListener( ICellEditorListener listener ) {
        _listeners.remove( listener );
    }

    public void setValidator( ICellEditorValidator validator ) {
        _validator = validator;
    }

    private void fireApplyEvent() {
        for( ICellEditorListener listener : _listeners ) {
            listener.applyEditorValue();
        }
    }

    private void fireCancelEvent() {
        for( ICellEditorListener listener : _listeners ) {
            listener.cancelEditor();
        }
    }

    private void fireValueChangedEvent( boolean oldValidState, boolean newValidState ) {
        for( ICellEditorListener listener : _listeners ) {
            listener.editorValueChanged( oldValidState, newValidState );
        }
    }

    private boolean isValueValid() {
        return _errorMessage == null;
    }
}
