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
package net.rim.ejde.internal.ui.views.process;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

public class ProcessViewOptionsDialog extends MessageDialog {
    Button _liveUpdateButton;
    boolean _liveUpdateEnabled;

    public ProcessViewOptionsDialog( Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage,
            int dialogImageType, String[] dialogButtonLabels, int defaultIndex ) {
        super( parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultIndex );
    }

    protected Control createDialogArea( Composite parent ) {
        Composite mainComp = new Composite( parent, SWT.NONE );
        GridLayout mainCompLayout = new GridLayout();
        mainCompLayout.numColumns = 1;
        mainComp.setLayout( mainCompLayout );
        mainComp.setLayoutData( new GridData( GridData.FILL_VERTICAL | GridData.FILL_HORIZONTAL) );

        Group optionGroup = new Group(mainComp, SWT.NONE);
        optionGroup.setText( "Options" );
        optionGroup.setLayout( mainCompLayout  );
        optionGroup.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        IPreferenceStore ps = ContextManager.PLUGIN.getPreferenceStore();
        _liveUpdateButton = new Button( optionGroup, SWT.None | SWT.CHECK );
        _liveUpdateButton.setText( "Live update" );
        _liveUpdateEnabled = ps.getBoolean( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_LIVE_UPDATE );
        _liveUpdateButton.setSelection( _liveUpdateEnabled );
        _liveUpdateButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                _liveUpdateEnabled = _liveUpdateButton.getSelection();
            }
        } );
        return mainComp;
    }

    protected Control createButtonBar( Composite parent ) {
        Control control = super.createButtonBar( parent );
        Button okButton = getButton( 0 );
        okButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                IPreferenceStore ps = ContextManager.PLUGIN.getPreferenceStore();
                ps.setValue( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_LIVE_UPDATE, _liveUpdateEnabled );
            }
        } );
        return control;
    }

}
