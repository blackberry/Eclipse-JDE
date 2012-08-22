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

import net.rim.sdk.resourceutil.ResourceCollection;
import net.rim.sdk.resourceutil.ResourceElement;
import net.rim.sdk.resourceutil.ResourceLocale;

import org.eclipse.jface.viewers.ICellEditorListener;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.swt.widgets.Table;

class ResourceCellEditorUIFactory {
    private Table _table;

    ResourceCellEditorUIFactory( Table table ) {
        _table = table;
    }

    IDisplayable createCellEditorUI( ResourceElement element, int columnIndex ) {
        if( element.isMulti() ) {
            return createCellEditorDialog( element );
        }
        return createCellEditorTextBox( element, columnIndex );
    }

    IDisplayable createCellEditorDialog( ResourceElement element ) {
        IResourceCellEditor keyEditor = createKeyEditor( element );
        IResourceCellEditor valueEditor = createValueEditor( element );
        IResourceCellEditor commentEditor = createCommentEditor( element );

        ResourceCollection collection = element.getLocale().getCollection();
        String originalLocaleName = collection.getOriginalLocaleName();
        Object originalLocaleValue = null;
        if( originalLocaleName != null ) {
            ResourceLocale originalLocale = collection.getOriginalLocale();
            ResourceElement originalLocaleElement = originalLocale.getResourceElement( element.getKey() );
            originalLocaleValue = originalLocaleElement.getValue();
        }

        return new ResourceCellEditorDialog( _table.getShell(), keyEditor, valueEditor, commentEditor, originalLocaleName,
                originalLocaleValue );
    }

    private IDisplayable createCellEditorTextBox( ResourceElement element, int columnIndex ) {
        switch( columnIndex ) {
            case ResourceEditorPage.KEY_COLUMN_INDEX:
                IResourceCellEditor keyEditor = createKeyEditor( element );
                return new ResourceCellEditorTextBox( _table, columnIndex, keyEditor );
            case ResourceEditorPage.VALUE_COLUMN_INDEX:
                IResourceCellEditor valueEditor = createValueEditor( element );
                return new ResourceCellEditorTextBox( _table, columnIndex, valueEditor );
            default:
                return null;
        }
    }

    private IResourceCellEditor createCommentEditor( ResourceElement element ) {
        String comment = element.getLocale().getCollection().getComment( element.getKey() );
        IResourceCellEditor commentEditor = new ResourceCellEditor( comment );
        ICellEditorListener commentListener = createCommentListener( commentEditor, element );
        commentEditor.addListener( commentListener );

        return commentEditor;
    }

    private ICellEditorListener createCommentListener( final IResourceCellEditor commentEditor, final ResourceElement element ) {
        final ICellModifier commentModifier = new ResourceCommentModifier( _table.getShell(), commentEditor );

        return new ICellEditorListener() {
            public void applyEditorValue() {
                if( commentModifier.canModify( element, null ) ) {
                    Object value = commentEditor.getValue();
                    commentModifier.modify( element, null, value );
                }
            }

            public void cancelEditor() {
            }

            public void editorValueChanged( boolean oldValidState, boolean newValidState ) {
            }
        };
    }

    private IResourceCellEditor createKeyEditor( ResourceElement element ) {
        IResourceCellEditor keyEditor = new ResourceCellEditor( element.getKey() );
        ICellEditorListener keyListener = createKeyListener( keyEditor, element );
        ICellEditorValidator keyValidator = createKeyValidator( element );
        keyEditor.addListener( keyListener );
        keyEditor.setValidator( keyValidator );

        return keyEditor;
    }

    private ICellEditorListener createKeyListener( final IResourceCellEditor keyEditor, final ResourceElement element ) {
        final ICellModifier keyModifier = new ResourceKeyModifier( _table.getShell(), keyEditor );

        return new ICellEditorListener() {
            public void applyEditorValue() {
                if( keyModifier.canModify( element, ResourceEditorPage.KEY_COLUMN_ID ) ) {
                    Object value = keyEditor.getValue();
                    keyModifier.modify( element, ResourceEditorPage.KEY_COLUMN_ID, value );
                }
            }

            public void cancelEditor() {
            }

            public void editorValueChanged( boolean oldValidState, boolean newValidState ) {
            }
        };
    }

    private ICellEditorValidator createKeyValidator( final ResourceElement element ) {
        return new ResourceKeyValidator( element.getLocale().getCollection(), true, element.getKey() );
    }

    private IResourceCellEditor createValueEditor( ResourceElement element ) {
        IResourceCellEditor valueEditor = new ResourceCellEditor( element.getValue() );
        ICellEditorListener valueListener = createValueListener( valueEditor, element );
        ICellEditorValidator valueValidator = createValueValidator();
        valueEditor.addListener( valueListener );
        valueEditor.setValidator( valueValidator );

        return valueEditor;
    }

    private ICellEditorListener createValueListener( final IResourceCellEditor valueEditor, final ResourceElement element ) {
        final ICellModifier valueModifier = new ResourceValueModifier( _table.getShell(), valueEditor );

        return new ICellEditorListener() {
            public void applyEditorValue() {
                if( valueModifier.canModify( element, ResourceEditorPage.VALUE_COLUMN_ID ) ) {
                    Object value = valueEditor.getValue();
                    valueModifier.modify( element, ResourceEditorPage.VALUE_COLUMN_ID, value );

                    ResourceEditorOptionsDialog.updateVersioningAfterResourceElementEdited( element );
                }
            }

            public void cancelEditor() {
            }

            public void editorValueChanged( boolean oldValidState, boolean newValidState ) {
            }
        };
    }

    private ICellEditorValidator createValueValidator() {
        return new ICellEditorValidator() {
            public String isValid( Object value ) {
                return null;
            }
        };
    }
}
