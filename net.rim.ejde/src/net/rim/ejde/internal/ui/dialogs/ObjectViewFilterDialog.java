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
package net.rim.ejde.internal.ui.dialogs;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;
import net.rim.ejde.internal.ui.views.objects.ObjectsView;
import net.rim.ejde.internal.util.Messages;
import net.rim.ide.core.ObjectsContentsHelper;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog used to display and set up filters and options of object view.
 */
public class ObjectViewFilterDialog extends Dialog {

    private ObjectsView _objectsView;
    boolean _showGroupMember;
    boolean _showRecursiveSize;
    // UI controls
    private Combo _comboSnapShotFilter;
    private Text _txtType;
    private Text _txtProcess;
    private Combo _comboLocation;
    private Button _buttonIncludeAllInstance;
    private boolean _okButtonClicked;

    /**
     * Constructs an instance of ObjectViewFilterDialog.
     *
     * @param shell
     * @param callback
     */
    public ObjectViewFilterDialog( Shell shell, ObjectsView callback ) {
        // Pass the default styles here
        this( shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL, callback );
    }

    /**
     * Constructs an instance of OptionsDialog (customized style).
     *
     * @param parent
     *            the parent
     * @param style
     *            the style
     */
    public ObjectViewFilterDialog( Shell parent, int style, ObjectsView callback ) {
        // Let users override the default styles
        super( parent, style );
        setText( "Objects View Options" );
        _objectsView = callback;
    }

    /**
     * Opens the dialog.
     *
     * @return String
     */
    public void open() {
        // Create the dialog window
        Shell shell = new Shell( getParent(), getStyle() );
        shell.setMinimumSize( 300, 200 );
        shell.setText( getText() );
        createContents( shell );
        shell.pack();
        shell.open();
        Display display = getParent().getDisplay();
        while( !shell.isDisposed() ) {
            if( !display.readAndDispatch() ) {
                display.sleep();
            }
        }
    }

    /**
     * (no java doc)
     *
     * @see BasicFilterDialog#createPartControl(Composite)
     */
    private void createContents( final Shell parent ) {
        parent.setLayout( new GridLayout( 2, false ) );
        GridData gridData = new GridData( GridData.FILL_HORIZONTAL );
        Label labelFilter = new Label( parent, SWT.NONE );
        labelFilter.setText( "Snapshot Filter: " );
        _comboSnapShotFilter = new Combo( parent, SWT.READ_ONLY | SWT.DROP_DOWN );
        _comboSnapShotFilter.setLayoutData( gridData );
        _comboSnapShotFilter.setItems( ObjectsContentsHelper.getSnapshotFilterStrings() );
        Label labelType = new Label( parent, SWT.NONE );
        labelType.setText( "Type: " );
        _txtType = new Text( parent, SWT.BORDER );
        _txtType.setLayoutData( gridData );
        Label labelProcess = new Label( parent, SWT.NONE );
        labelProcess.setText( "Process: " );
        _txtProcess = new Text( parent, SWT.BORDER );
        _txtProcess.setLayoutData( gridData );
        Label labelLocation = new Label( parent, SWT.NONE );
        labelLocation.setText( "Location: " );
        _comboLocation = new Combo( parent, SWT.READ_ONLY | SWT.DROP_DOWN );
        _comboLocation.setLayoutData( gridData );
        _comboLocation.setItems( ObjectsContentsHelper.getLocationFilterStrings() );
        _buttonIncludeAllInstance = new Button( parent, SWT.CHECK );
        _buttonIncludeAllInstance.setText( "Include All Instance" );
        Label blankLabel = new Label( parent, SWT.NONE );
        blankLabel.setLayoutData( gridData );
        // set initial value of the filters
        if( _objectsView == null ) {
            _comboSnapShotFilter.select( 0 );
            _comboLocation.select( 0 );
        } else {
            _txtProcess.setText( _objectsView.getProcessFilterText() );
            _txtType.setText( _objectsView.getTypeFilterText() );
            _comboSnapShotFilter.select( _objectsView.getSnapshotFilterIndex() );
            _comboLocation.select( _objectsView.getLocationFilterIndex() );
            _buttonIncludeAllInstance.setSelection( _objectsView.getIncludeAllInstances() );
        }

        Composite buttonComposite = new Composite( parent, SWT.NONE );
        GridLayout layout = new GridLayout( 2, true );
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttonComposite.setLayout( layout );
        gridData = new GridData( GridData.FILL_HORIZONTAL );
        gridData.horizontalSpan = 2;
        buttonComposite.setLayoutData( gridData );

        // Create the OK button and add a handler
        // so that pressing it will set input
        // to the entered value
        Button ok = new Button( buttonComposite, SWT.PUSH );
        ok.setText( Messages.IConstants_OK_BUTTON_TITLE );
        gridData = new GridData( GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END );
        gridData.widthHint = 60;
        ok.setLayoutData( gridData );
        ok.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent event ) {
                _okButtonClicked = true;
                saveOptions();
                parent.close();
            }
        } );

        // Create the cancel button and add a handler
        Button cancel = new Button( buttonComposite, SWT.PUSH );
        cancel.setText( Messages.IConstants_CANCEL_BUTTON_TITLE );
        gridData = new GridData( GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING );
        gridData.widthHint = 60;
        cancel.setLayoutData( gridData );
        cancel.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent event ) {
                _okButtonClicked = false;
                parent.close();
            }
        } );

        // Set the cancel button as the default, so
        // user can type input and press Enter
        // to dismiss
        parent.setDefaultButton( cancel );

    }

    /**
     * Saves options to preference store.
     */
    void saveOptions() {
        // set filter options to callback
        if( _objectsView != null ) {
            _objectsView.setSnapshotFilterIndex( _comboSnapShotFilter.getSelectionIndex() );
            _objectsView.setTypeFilterText( _txtType.getText() );
            _objectsView.setProcessFilterText( _txtProcess.getText() );
            _objectsView.setLocationFilterIndex( _comboLocation.getSelectionIndex() );
            _objectsView.setShowGroupMember( _showGroupMember );
            _objectsView.setShowRecursiveSize( _showRecursiveSize );
            _objectsView.setIncludeAllInstances( _buttonIncludeAllInstance.getSelection() );
        }
        // save options to preferences
        IPreferenceStore ps = ContextManager.PLUGIN.getPreferenceStore();
        ps.setValue( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_SHOW_GROUP_MEMBER, _showGroupMember );
        ps.setValue( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_SHOW_RECURSIVE_SIZE, _showRecursiveSize );
    }

    /**
     * Checks if group member need to be shown.
     *
     * @return <code>true</code> group member need to be shown, <code>false</code> otherwise.
     */
    public boolean getShowGroupMember() {
        return _showGroupMember;
    }

    /**
     * Sets if group member need to be shown.
     *
     * @param show
     *            <code>true</code> group member need to be shown, <code>false</code> otherwise.
     */
    public void setShowGroupMember( boolean show ) {
        _showGroupMember = show;
    }

    /**
     * Checks if recursive size need to be shown.
     *
     * @return <code>true</code> recursive size need to be shown, <code>false</code> otherwise.
     */
    public boolean getShowRecursiveSize() {
        return _showRecursiveSize;
    }

    /**
     * Sets if recursive size need to be shown.
     *
     * @param show
     *            <code>true</code> recursive size need to be shown, <code>false</code> otherwise.
     */
    public void setShowRecursiveSize( boolean show ) {
        _showRecursiveSize = show;
    }

    /**
     * Returns if the OK button is clicked.
     *
     * @return <code>true</code> if yes; <code>false</code> otherwise.
     */
    public boolean isOkButtonClicked() {
        return _okButtonClicked;
    }
}
