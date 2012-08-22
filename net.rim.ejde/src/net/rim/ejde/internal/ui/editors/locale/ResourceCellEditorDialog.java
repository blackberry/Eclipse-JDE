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

import net.rim.sdk.resourceutil.ResourceUtil;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

class ResourceCellEditorDialog extends Dialog implements IDisplayable {
    private IResourceCellEditor _keyEditor;
    private IResourceCellEditor _valueEditor;
    private IResourceCellEditor _commentEditor;
    private String _originalLocaleName;
    private Object _originalLocaleValue;

    private static final int NUM_CHARS_COMMENT_TEXT = 40;
    private static final int NUM_LINES_COMMENT_TEXT = 5;

    private static final int NUM_CHARS_VALUE_TEXT = 30;
    private static final int NUM_LINES_VALUE_LIST = 5;

    public ResourceCellEditorDialog( Shell parentShell, IResourceCellEditor keyEditor, IResourceCellEditor valueEditor,
            IResourceCellEditor commentEditor, String originalLocaleName, Object originalLocaleValue ) {
        super( parentShell );
        setShellStyle( getShellStyle() | SWT.RESIZE );
        _keyEditor = keyEditor;
        _valueEditor = valueEditor;
        _commentEditor = commentEditor;
        _originalLocaleName = originalLocaleName;
        _originalLocaleValue = originalLocaleValue;
    }

    public final void display() {
        open();
    }

    protected void configureShell( Shell newShell ) {
        super.configureShell( newShell );
        newShell.setText( "Edit Resource Line" );
    }

    protected Control createDialogArea( Composite parent ) {
        Composite container = (Composite) super.createDialogArea( parent );
        container.setLayout( new GridLayout( 1, false ) );

        createKeyArea( container );
        createCommentArea( container );
        createValueArea( container );
        createOriginalLocaleArea( container );

        return container;
    }

    protected void okPressed() {
        _keyEditor.apply();
        _valueEditor.apply();
        _commentEditor.apply();
        super.okPressed();
    }

    protected void cancelPressed() {
        _keyEditor.cancel();
        _valueEditor.cancel();
        _commentEditor.cancel();
        super.cancelPressed();
    }

    private void createCommentArea( Composite container ) {
        GridData data = new GridData();
        data.verticalIndent = 5;
        Label commentLabel = new Label( container, SWT.NONE );
        commentLabel.setText( "Comment:" );
        commentLabel.setLayoutData( data );

        data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = SWT.FILL;
        data.heightHint = getTextHeight( container ) * NUM_LINES_COMMENT_TEXT;
        data.widthHint = getTextWidth( container ) * NUM_CHARS_COMMENT_TEXT;
        final Text commentText = new Text( container, SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL );
        commentText.setText( (String) _commentEditor.getValue() );
        commentText.setLayoutData( data );
        commentText.addModifyListener( new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
                commentTextChanged( commentText.getText() );
            }
        } );
    }

    private void createKeyArea( Composite container ) {
        Label keyLabel = new Label( container, SWT.NONE );
        keyLabel.setText( "Key:" );

        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = SWT.FILL;
        final Text keyText = new Text( container, SWT.BORDER );
        keyText.setText( (String) _keyEditor.getValue() );
        keyText.setLayoutData( data );
        keyText.addModifyListener( new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
                keyTextChanged( keyText.getText() );
            }
        } );
    }

    private void createOriginalLocaleArea( Composite container ) {
        if( _originalLocaleName != null && _originalLocaleValue != null ) {
            GridData data = new GridData();
            data.verticalIndent = 5;
            Label originalLocaleLabel = new Label( container, SWT.NONE );
            originalLocaleLabel.setText( "Original Locale Value (" + _originalLocaleName + ")" );
            originalLocaleLabel.setLayoutData( data );

            String[] valuesArray;
            if( _originalLocaleValue instanceof String ) {
                valuesArray = new String[] { (String) _originalLocaleValue };
            } else if( _originalLocaleValue instanceof Vector ) {
                Vector< String > originalLocaleValues = (Vector< String >) _originalLocaleValue;
                valuesArray = originalLocaleValues.toArray( new String[ originalLocaleValues.size() ] );
            } else {
                throw new IllegalStateException( "Original locale value is neither String nor Vector<String>" );
            }

            data = new GridData();
            data.grabExcessHorizontalSpace = true;
            data.horizontalAlignment = SWT.FILL;
            List originalLocaleValueList = new List( container, SWT.NONE );
            originalLocaleValueList.setItems( valuesArray );
            originalLocaleValueList.setLayoutData( data );
            originalLocaleValueList.setEnabled( false );
        }
    }

    private void createValueArea( Composite container ) {
        GridData data = new GridData();
        data.verticalIndent = 5;
        Label valueLabel = new Label( container, SWT.NONE );
        valueLabel.setText( "Value:" );
        valueLabel.setLayoutData( data );

        data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = SWT.FILL;

        Object value = _valueEditor.getValue();
        if( value instanceof String ) {
            final Text valueText = new Text( container, SWT.BORDER );
            String escapedValue = ResourceUtil.unicodeToEscaped( (String) value );
            valueText.setText( escapedValue );
            valueText.setLayoutData( data );
            valueText.addModifyListener( new ModifyListener() {
                public void modifyText( ModifyEvent e ) {
                    valueTextChanged( valueText.getText() );
                }
            } );
        } else if( value instanceof Vector ) {
            // Create components
            final List valueList = new List( container, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL );
            Composite composite = new Composite( container, SWT.NONE );
            final Text valueText = new Text( composite, SWT.BORDER );
            final Button addButton = new Button( composite, SWT.PUSH | SWT.CENTER );
            final Button editButton = new Button( composite, SWT.PUSH | SWT.CENTER );
            final Button removeButton = new Button( composite, SWT.PUSH | SWT.CENTER );

            // Lay out and set up components
            data.heightHint = getTextHeight( container ) * NUM_LINES_VALUE_LIST;
            valueList.setLayoutData( data );
            Vector< String > values = (Vector< String >) value;
            String[] valuesArray = values.toArray( new String[ values.size() ] );
            valueList.setItems( valuesArray );
            valueList.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected( SelectionEvent e ) {
                    setValueTextFromValueListSelection( valueList, valueText );
                    setEditButtonEnabled( editButton, valueList );
                    setRemoveButtonEnabled( removeButton, valueList );
                }
            } );

            data = new GridData();
            data.horizontalAlignment = SWT.CENTER;
            composite.setLayoutData( data );
            composite.setLayout( new GridLayout( 4, false ) );

            data = new GridData();
            data.grabExcessHorizontalSpace = true;
            data.widthHint = getTextWidth( composite ) * NUM_CHARS_VALUE_TEXT;
            valueText.setLayoutData( data );

            addButton.setText( "Add" );
            addButton.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected( SelectionEvent e ) {
                    String text = valueText.getText();
                    int index = valueList.getSelectionIndex();
                    if( index == -1 ) {
                        index = valueList.getItemCount();
                    } else {
                        ++index;
                    }
                    addToValueList( valueList, index, text );
                    setEditButtonEnabled( editButton, valueList );
                    setRemoveButtonEnabled( removeButton, valueList );
                    keyValuesChanged( valueList );
                }
            } );

            editButton.setText( "Edit" );
            editButton.setEnabled( false );
            editButton.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected( SelectionEvent e ) {
                    setValueListSelectionFromValueText( valueList, valueText );
                    keyValuesChanged( valueList );
                }
            } );

            removeButton.setText( "Remove" );
            removeButton.setEnabled( false );
            removeButton.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected( SelectionEvent e ) {
                    int index = valueList.getSelectionIndex();
                    valueList.remove( index );
                    if( index == valueList.getItemCount() ) {
                        --index;
                    }
                    valueList.setSelection( index );
                    setValueTextFromValueListSelection( valueList, valueText );
                    setEditButtonEnabled( editButton, valueList );
                    setRemoveButtonEnabled( removeButton, valueList );
                    keyValuesChanged( valueList );
                }
            } );
        } else {
            throw new IllegalStateException( "Resource value is neither String nor Vector<String>" );
        }
    }

    private void commentTextChanged( String newText ) {
        _commentEditor.changeValue( newText );
    }

    private static int getTextHeight( Control control ) {
        GC gc = new GC( control );
        gc.setFont( control.getFont() );
        FontMetrics fontMetrics = gc.getFontMetrics();
        gc.dispose();
        return fontMetrics.getHeight();
    }

    private static int getTextWidth( Control control ) {
        GC gc = new GC( control );
        gc.setFont( control.getFont() );
        FontMetrics fontMetrics = gc.getFontMetrics();
        gc.dispose();
        return fontMetrics.getAverageCharWidth();
    }

    private void keyTextChanged( String newText ) {
        _keyEditor.changeValue( newText );
    }

    private void keyValuesChanged( List valueList ) {
        String[] valueStrings = valueList.getItems();
        Vector< String > values = new Vector< String >( valueStrings.length );
        for( int i = 0; i < valueStrings.length; ++i ) {
            values.add( i, valueStrings[ i ] );
        }
        _valueEditor.changeValue( values );
    }

    private static void setEditButtonEnabled( Button editButton, List valueList ) {
        boolean enabled = valueList.getSelectionIndex() != -1;
        editButton.setEnabled( enabled );
    }

    private static void setRemoveButtonEnabled( Button removeButton, List valueList ) {
        boolean enabled = valueList.getSelectionIndex() != -1;
        removeButton.setEnabled( enabled );
    }

    private static void setValueListSelectionFromValueText( List valueList, Text valueText ) {
        int index = valueList.getSelectionIndex();
        if( index != -1 ) {
            String text = valueText.getText();
            text = ResourceUtil.escapedToUnicode( text );
            valueList.setItem( index, text );
        }
    }

    private static void setValueTextFromValueListSelection( List valueList, Text valueText ) {
        int index = valueList.getSelectionIndex();
        if( index != -1 ) {
            String text = valueList.getItem( index );
            text = ResourceUtil.unicodeToEscaped( text );
            valueText.setText( text );
        } else {
            valueText.setText( "" );
        }
    }

    private static void addToValueList( List valueList, int index, String text ) {
        text = ResourceUtil.escapedToUnicode( text );
        valueList.add( text, index );
        valueList.setSelection( index );
    }

    private void valueTextChanged( String newText ) {
        _valueEditor.changeValue( newText );
    }
}
