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
package net.rim.ejde.internal.ui.editors.model.factories;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.rim.ejde.internal.model.BasicBlackBerryProperties.Icon;
import net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.Action;
import net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.DirtyListener;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 *
 * @author jkeshavarzi, bkurz
 *
 */
public class ControlFactory {
    private static final Integer DEFAULT_TEXT_WIDTH_HINT = Integer.valueOf( 100 );
    private static final String CONTROL_TITLE_KEY = "TITLE"; //$NON-NLS-1$
    private static final String TABLE_TEXT_INDEX_KEY = "TEXT_COLUMN"; //$NON-NLS-1$

    public static enum ControlType {
        LABEL, TEXT, COMBO, BUTTON, TEXT_WITH_LABEL, TEXT_WITH_LABEL_WITH_BUTTON, TABLE
    }

    /**
     * Builds a text control with a label and a button onto the given composite body
     *
     * @param labelValue
     * @param textValue
     * @param textTooltip
     * @param body
     * @param toolkit
     * @param dirtyListener
     * @param validators
     * @param buttonAction
     * @param buttonListener
     * @return
     */
    public static final Map< ControlType, Control > buildTextWithLabelAndButtonControl( Composite body, FormToolkit toolkit,
            String labelValue, String textValue, String textTooltip, DirtyListener dirtyListener, VerifyListener[] validators,
            Action buttonAction, SelectionListener buttonListener ) {
        Map< ControlType, Control > controlList = new HashMap< ControlType, Control >( 2 );

        // Create text control
        Label lbl = buildLabelControl( body, toolkit, ControlType.TEXT_WITH_LABEL, labelValue, 1 );

        // Create label control
        Text textInputField = buildTextControl( body, toolkit, textValue, textTooltip, 1, dirtyListener, validators );

        // Create button controls
        Map< Action, SelectionListener > buttonMap = new HashMap< Action, SelectionListener >();
        buttonMap.put( buttonAction, buttonListener );
        Map< Action, Button > button = buildButtonControls( buttonMap, body, toolkit );

        controlList.put( ControlType.LABEL, lbl );
        controlList.put( ControlType.TEXT, textInputField );
        controlList.put( ControlType.BUTTON, button.get( buttonAction ) );

        return controlList;
    }

    /**
     * Builds a text control with a label onto the given composite body
     *
     * @param body
     * @param toolkit
     * @param labelValue
     * @param textValue
     * @param textTooltip
     * @param dirtyListener
     * @param validators
     * @return
     */
    public static final Map< ControlType, Control > buildTextWithLabelControl( Composite body, FormToolkit toolkit,
            String labelValue, String textValue, String textTooltip, DirtyListener dirtyListener, VerifyListener[] validators ) {
        Map< ControlType, Control > controlList = new HashMap< ControlType, Control >( 2 );

        // Create label control
        Label lbl = buildLabelControl( body, toolkit, ControlType.TEXT_WITH_LABEL, labelValue, 1 );

        // Create text control
        Text textInputField = buildTextControl( body, toolkit, textValue, textTooltip, 2, dirtyListener, validators );
        textInputField.setData( CONTROL_TITLE_KEY, labelValue );

        controlList.put( ControlType.LABEL, lbl );
        controlList.put( ControlType.TEXT, textInputField );

        return controlList;
    }

    /**
     * Builds a label control onto the given composite body
     *
     * @param body
     * @param toolkit
     * @param controlType
     * @param label
     * @param columns
     * @return
     */
    public static final Label buildLabelControl( Composite body, FormToolkit toolkit, ControlType controlType, String label,
            Integer columns ) {
        Label lbl = toolkit.createLabel( body, label );
        lbl.setForeground( toolkit.getColors().getColor( IFormColors.TITLE ) );
        lbl.setBackground( body.getBackground() );

        initializeControlLayout( body, lbl, controlType, columns );
        return lbl;
    }

    /**
     * Builds a text control onto the given composite body
     *
     * @param body
     * @param toolkit
     * @param value
     * @param tooltip
     * @param colSpan
     * @param dirtyListener
     * @param validators
     * @return
     */
    public static final Text buildTextControl( Composite body, FormToolkit toolkit, String value, String tooltip,
            Integer colSpan, DirtyListener dirtyListener, VerifyListener[] validators ) {
        Text textInputField = toolkit.createText( body, value == null ? "" : value, SWT.SINGLE ); //$NON-NLS-1$
        textInputField.setEditable( true );
        textInputField.setEnabled( true );
        textInputField.setToolTipText( tooltip );
        textInputField.addModifyListener( dirtyListener );

        initializeControlLayout( body, textInputField, ControlType.TEXT, colSpan );

        if( ( null != validators ) && ( 0 < validators.length ) ) {
            for( VerifyListener verifyListener : validators ) {
                if( null != verifyListener ) {
                    textInputField.addVerifyListener( verifyListener );
                }
            }
        }
        return textInputField;
    }

    /**
     * Builds a button composite onto the given composite body. Builds a button for each Action in the passed in actionListeners
     * map
     *
     * @param actionListeners
     * @param body
     * @param toolkit
     * @return
     */
    public static final Map< Action, Button > buildButtonControls( Map< Action, SelectionListener > actionListeners,
            Composite body, FormToolkit toolkit ) {
        Object bodyLayout = body.getLayout();
        Map< Action, Button > actionButtons = new HashMap< Action, Button >( actionListeners.size() );

        Composite fButtonContainer = toolkit.createComposite( body );
        GridData gd = new GridData( GridData.VERTICAL_ALIGN_FILL );
        fButtonContainer.setLayoutData( gd );
        fButtonContainer.setLayout( createButtonsLayout() );
        fButtonContainer.setBackground( body.getBackground() );

        Button button;
        SelectionListener selectionListener;

        for( Action action : Action.values() ) {
            if( actionListeners.containsKey( action ) ) {
                button = toolkit.createButton( fButtonContainer, action.getButtonLabel(), SWT.PUSH );
                if( bodyLayout instanceof TableWrapLayout ) {
                    TableWrapData td = new TableWrapData();
                    button.setLayoutData( td );
                } else if( bodyLayout instanceof GridLayout ) {
                    GridData gld = new GridData( GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING );
                    button.setLayoutData( gld );
                }

                selectionListener = actionListeners.get( action );
                if( null != selectionListener ) {
                    button.addSelectionListener( selectionListener );
                }
                actionButtons.put( action, button );
            }
        }

        if( actionButtons.get( Action.EDIT ) != null ) {
            actionButtons.get( Action.EDIT ).setEnabled( false );
        }
        if( actionButtons.get( Action.REMOVE ) != null ) {
            actionButtons.get( Action.REMOVE ).setEnabled( false );
        }
        if( actionButtons.get( Action.SELECT_ALL ) != null ) {
            actionButtons.get( Action.SELECT_ALL ).setEnabled( false );
        }
        if( actionButtons.get( Action.SELECT_NONE ) != null ) {
            actionButtons.get( Action.SELECT_NONE ).setEnabled( false );
        }
        if( actionButtons.get( Action.MOVE_UP ) != null ) {
            actionButtons.get( Action.MOVE_UP ).setEnabled( false );
        }
        if( actionButtons.get( Action.MOVE_DOWN ) != null ) {
            actionButtons.get( Action.MOVE_DOWN ).setEnabled( false );
        }

        return actionButtons;
    }

    /**
     * Builds a check box control onto the given composite body
     *
     * @param booleanInputFieldLabel
     * @param selected
     * @param body
     * @param toolkit
     * @return
     */
    public static final Button buildCheckBoxControl( Composite body, FormToolkit toolkit, String booleanInputFieldLabel,
            String toolTipText, Boolean selected, DirtyListener dirtyListener ) {
        Button button = toolkit.createButton( body, booleanInputFieldLabel, SWT.CHECK );
        initializeControlLayout( body, button, ControlType.BUTTON, 3 );

        if( dirtyListener != null ) {
            button.addSelectionListener( dirtyListener );
        }
        button.setBackground( body.getBackground() );
        button.setSelection( selected == null ? false : selected );
        button.setToolTipText( toolTipText );
        return button;
    }

    /**
     * Builds a combo box control onto the given composite body
     *
     * @param inputChoiceList
     * @param selectedText
     * @param label
     * @param body
     * @param toolkit
     * @param selectionAdapter
     * @return
     */
    public static final Map< ControlType, Control > buildComboBoxControl( Composite body, FormToolkit toolkit,
            final String[] inputChoiceList, String selectedText, String label, String toolTipText,
            SelectionAdapter selectionAdapter, DirtyListener dirtyListener ) {
        Map< ControlType, Control > controlList = new HashMap< ControlType, Control >( 2 );

        // Create label control
        Label lbl = buildLabelControl( body, toolkit, ControlType.TEXT_WITH_LABEL, label, 1 );

        // Create combo box control
        final Combo _comboBox = new Combo( body, SWT.SINGLE | SWT.BORDER );
        toolkit.adapt( _comboBox, true, false );

        _comboBox.setItems( inputChoiceList == null ? new String[] {} : inputChoiceList );
        _comboBox.setText( selectedText == null ? "" : selectedText ); //$NON-NLS-1$
        _comboBox.setData( CONTROL_TITLE_KEY, label );
        _comboBox.setToolTipText( toolTipText );
        /* _comboBox.addListener( SWT.KeyDown, new Listener() {
            @Override
            public void handleEvent( Event event ) {
				//event.text = "";
            }
        } ); */
        // Initialize combo box control layout
        initializeControlLayout( body, _comboBox, ControlType.COMBO, 2 );

        if( selectionAdapter != null ) {
            _comboBox.addSelectionListener( selectionAdapter );
        }

        if( dirtyListener != null ) {
            _comboBox.addModifyListener( dirtyListener );
        }

        controlList.put( ControlType.LABEL, lbl );
        controlList.put( ControlType.COMBO, _comboBox );

        return controlList;
    }

    /**
     * Builds a table control onto the given composite body
     *
     * @param label
     * @param style
     * @param columns
     * @param columnProperties
     * @param body
     * @param toolkit
     * @param cProvider
     * @param lProvider
     * @param input
     * @return
     */
    public static final Viewer buildTableControl( Composite body, FormToolkit toolkit, String label, String toolTipText,
            Integer style, Integer columns, String columnProperties[], IStructuredContentProvider contentProvider,
            ITableLabelProvider labelProvider, Object input ) {
        // Create label control
        if( label != null ) {
            buildLabelControl( body, toolkit, ControlFactory.ControlType.LABEL, label, 3 );
        }

        Integer tableStyle = SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.BORDER;

        if( style != null ) {
            tableStyle = tableStyle | style;
        }
        if( ( tableStyle & SWT.MULTI ) == 0 ) {
            tableStyle = tableStyle + SWT.SINGLE;
        }

        // Create table control
        Table table = toolkit.createTable( body, tableStyle );
        table.setToolTipText( toolTipText );

        CellEditor[] editors = new CellEditor[ 1 ];
        TextCellEditor textEditor = new TextCellEditor( table );
        textEditor.setStyle( SWT.READ_ONLY );
        editors[ 0 ] = textEditor;

        // Create table viewer control
        TableViewer tableViewer;
        if( ( tableStyle & SWT.CHECK ) != 0 ) {
            tableViewer = new CheckboxTableViewer( table );
        } else {
            tableViewer = new TableViewer( table );
        }
        tableViewer.setContentProvider( contentProvider == null ? new DefaultContentProvider() : contentProvider );
        tableViewer.setLabelProvider( labelProvider == null ? new DefaultLabelProvider() : labelProvider );
        tableViewer.setUseHashlookup( true );
        tableViewer.setColumnProperties( columnProperties );
        tableViewer.setCellEditors( editors );
        tableViewer.setInput( input );

        initializeControlLayout( body, table, ControlType.TABLE, columns );
        return tableViewer;
    }

    /**
     * Builds an icon table control onto the given composite body
     *
     * @param body
     * @param toolkit
     * @param label
     * @param toolTipText
     * @param style
     * @param columns
     * @param columnProperties
     * @param input
     * @param contentProvider
     * @param labelProvider
     * @param checkStateProvider
     * @param selectionListener
     * @return
     */
    public static final Viewer buildIconTableControl( Composite body, FormToolkit toolkit, String label, String toolTipText,
            Integer style, Integer columns, String columnProperties[], Set< Icon > input,
            IStructuredContentProvider contentProvider, ITableLabelProvider labelProvider,
            ICheckStateProvider checkStateProvider, SelectionListener selectionListener ) {
        CheckboxTableViewer viewer = (CheckboxTableViewer) buildTableControl( body, toolkit, label, toolTipText, style, columns,
                columnProperties, contentProvider, labelProvider, input );

        // Add icon specific table properties
        if( checkStateProvider != null ) {
            viewer.setCheckStateProvider( checkStateProvider );
        }

        Table table = viewer.getTable();
        table.setData( TABLE_TEXT_INDEX_KEY, 2 );
        table.setData( CONTROL_TITLE_KEY, "Icons" );

        if( selectionListener != null ) {
            table.addSelectionListener( selectionListener );
        }

        GridData gridData = ( (GridData) table.getLayoutData() );
        gridData.heightHint = 55;
        table.setHeaderVisible( true );

        TableColumn focusColumn = new TableColumn( table, SWT.NONE );
        focusColumn.setText( Messages.BlackBerryProjectPropertiesPage_Table_RolloverIcon_Column_Label );
        focusColumn.pack();

        TableColumn iconColumn = new TableColumn( table, SWT.NONE );
        iconColumn.setText( Messages.BlackBerryProjectPropertiesPage_Table_Icon_Column_Label );
        iconColumn.pack();

        TableColumn pathColumn = new TableColumn( table, SWT.NONE );
        pathColumn.setText( Messages.BlackBerryProjectPropertiesPage_Table_File_Column_Label );
        pathColumn.setWidth( 300 );

        return viewer;
    }

    /**
     * Inserts a line onto the given composite body
     *
     * @param body
     * @param toolkit
     */
    public static final void insertLine( Composite body, FormToolkit toolkit ) {
        buildLabelControl( body, toolkit, ControlFactory.ControlType.LABEL, "\n", 3 ); //$NON-NLS-1$
    }

    private static GridLayout createButtonsLayout() {
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        return layout;
    }

    private static void initializeControlLayout( Composite body, Control c, ControlType controlType, Integer columns ) {
        Layout bodyLayout = body.getLayout();
        if( bodyLayout instanceof TableWrapLayout ) {
            if( controlType.equals( ControlType.LABEL ) || controlType.equals( ControlType.BUTTON )
                    || controlType.equals( ControlType.COMBO ) || controlType.equals( ControlType.TABLE ) ) {
                c.setLayoutData( createTableWrapData( TableWrapData.FILL, TableWrapData.MIDDLE, 1, columns, 0, true ) );
            } else if( controlType.equals( ControlType.TEXT ) ) {
                TableWrapData data = createTableWrapData( TableWrapData.FILL, TableWrapData.MIDDLE, 1, columns, 0, true );
                data.maxWidth = DEFAULT_TEXT_WIDTH_HINT;
                c.setLayoutData( data );
            } else if( controlType.equals( ControlType.TEXT_WITH_LABEL ) ) {
                c.setLayoutData( createTableWrapData( TableWrapData.LEFT, TableWrapData.MIDDLE, 1, columns, 0, false ) );
            }
        } else if( bodyLayout instanceof GridLayout ) {
            if( controlType.equals( ControlType.LABEL ) || controlType.equals( ControlType.BUTTON ) ) {
                c.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, true, false, columns, 0 ) );
            } else if( controlType.equals( ControlType.COMBO ) ) {
                c.setLayoutData( new GridData( SWT.FILL, SWT.CENTER, true, false, columns, 0 ) );
            } else if( controlType.equals( ControlType.TEXT ) ) {
                GridData data = new GridData( SWT.FILL, SWT.CENTER, true, false, columns, 0 );
                data.widthHint = DEFAULT_TEXT_WIDTH_HINT;
                c.setLayoutData( data );
            } else if( controlType.equals( ControlType.TEXT_WITH_LABEL ) ) {
                c.setLayoutData( new GridData( SWT.LEFT, SWT.CENTER, false, false, columns, 0 ) );
            } else if( controlType.equals( ControlType.TABLE ) ) {
                GridData gd = new GridData( SWT.FILL, SWT.CENTER, true, false, columns, 0 );
                gd.widthHint = 150;
                gd.heightHint = 140;
                c.setLayoutData( gd );
            }
        }
    }

    private static TableWrapData createTableWrapData( Integer align, Integer valign, Integer rowspan, Integer colspan,
            Integer indent, Boolean grabHorizontal ) {
        TableWrapData td = new TableWrapData( align, valign, rowspan, colspan );
        td.indent = indent;
        td.grabHorizontal = grabHorizontal;
        return td;
    }

    /**
     * The default content provider used in the editor if one cannot be provided
     *
     * @author jkeshavarzi
     *
     */
    private static class DefaultContentProvider implements IStructuredContentProvider {
        @Override
        public Object[] getElements( Object inputElement ) {
            Object[] result = new Object[ 0 ];

            if( ( null != inputElement ) && ( inputElement instanceof String[] ) ) {
                result = (String[]) inputElement;
            }
            return result;
        }

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
        }
    }

    /**
     * The default label provider used in the editor if one cannot be provided
     *
     * @author jkeshavarzi
     *
     */
    private static class DefaultLabelProvider implements ITableLabelProvider {
        public Image getColumnImage( Object element, int columnIndex ) {
            return null;
        }

        public String getColumnText( Object element, int columnIndex ) {
            if( ( null != element ) && String.class.equals( element.getClass() ) && ( columnIndex >= 0 ) ) {
                String item = (String) element;
                if( columnIndex == 0 ) {
                    return item;
                }
            }
            return ""; //$NON-NLS-1$
        }

        @Override
        public void addListener( ILabelProviderListener listener ) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public boolean isLabelProperty( Object element, String property ) {
            return false;
        }

        @Override
        public void removeListener( ILabelProviderListener listener ) {
        }
    }
}
