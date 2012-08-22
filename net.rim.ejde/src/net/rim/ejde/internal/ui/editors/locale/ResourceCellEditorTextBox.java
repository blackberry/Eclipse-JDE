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

import net.rim.sdk.resourceutil.ResourceUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

class ResourceCellEditorTextBox implements IDisplayable {
    private int _columnIndex;
    private IResourceCellEditor _editor;
    private FocusListener _focusListener;
    private boolean _isDisposed;
    private Table _table;
    private TableEditor _tableEditor;
    private Text _text;

    // TODO: add TextActionHandler

    ResourceCellEditorTextBox( Table table, int columnIndex, IResourceCellEditor editor ) {
        _table = table;
        _columnIndex = columnIndex;
        _editor = editor;
        _text = createText();
        _tableEditor = createTableEditor();
    }

    public void display() {
        if( _isDisposed ) {
            return;
        }

        TableItem item = _table.getSelection()[ 0 ];
        _tableEditor.setEditor( _text, item, _columnIndex );

        String escapedValue = ResourceUtil.unicodeToEscaped( (String) _editor.getValue() );
        _text.setText( escapedValue );
        _text.selectAll();
        _text.setFocus();
        _text.setVisible( true );
    }

    private void applyValueAndDisposeEditor() {
        // remove FocusListener in case firing the apply event causes
        // focus to be lost, otherwise applyValueAndDisposeEditor()
        // could get called again as a result
        _text.removeFocusListener( _focusListener );
        _editor.apply();
        disposeEditor();
    }

    private void cancelAndDisposeEditor() {
        // remove FocusListener in case firing the cancel event causes
        // focus to be lost, otherwise applyValueAndDisposeEditor()
        // could get called as a result
        _text.removeFocusListener( _focusListener );
        _editor.cancel();
        disposeEditor();
    }

    private TableEditor createTableEditor() {
        TableEditor tableEditor = new TableEditor( _table );
        tableEditor.horizontalAlignment = SWT.LEFT;
        tableEditor.grabHorizontal = true;

        return tableEditor;
    }

    private Text createText() {
        final Text text = new Text( _table, SWT.NONE );

        text.addListener( SWT.Traverse, new Listener() {
            public void handleEvent( Event event ) {
                switch( event.detail ) {
                    case SWT.TRAVERSE_ESCAPE:
                        cancelAndDisposeEditor();
                        event.doit = true;
                        event.detail = SWT.TRAVERSE_NONE;
                        break;
                    case SWT.TRAVERSE_RETURN:
                        applyValueAndDisposeEditor();
                        event.doit = true;
                        event.detail = SWT.TRAVERSE_NONE;
                        break;
                }
            }
        } );

        text.addModifyListener( new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
                _editor.changeValue( text.getText() );
            }
        } );

        _focusListener = new FocusAdapter() {
            public void focusLost( FocusEvent e ) {
                applyValueAndDisposeEditor();
            }
        };
        text.addFocusListener( _focusListener );

        text.setVisible( false );

        return text;
    }

    private void disposeEditor() {
        if( _isDisposed ) {
            return;
        }

        _tableEditor.setEditor( null, null, _columnIndex );
        _tableEditor.dispose();
        _tableEditor = null;

        _text.setVisible( false );
        _text.dispose();
        _text = null;

        _isDisposed = true;
    }
}
