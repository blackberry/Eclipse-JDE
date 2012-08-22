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
package net.rim.ejde.internal.ui.views.profiler;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;
import net.rim.ejde.internal.util.Messages;
import net.rim.ide.RIA;
import net.rim.ide.RIA.ProfileType;

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

/**
 * This class demonstrates how to create your own dialog classes. It allows users to input a String
 */
public class ProfilingViewOptionsDialog extends Dialog implements IConstants {

    public static final int PROFILE_TYPE_SAMPLE = 11;
    public static final int PROFILE_TYPE_SAMPLE_NO_BREAK = 12;
    public static final int PROFILE_TYPE_NOTHING = 9;

    private ProfileType[] types;
    private boolean _okButtonClicked;

    /**
     * Constructs an instance of OptionsDialog (default style).
     *
     * @param parent
     *            the parent
     */
    public ProfilingViewOptionsDialog( Shell parent ) {
        // Pass the default styles here
        this( parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL );
    }

    /**
     * Constructs an instance of OptionsDialog (customized style).
     *
     * @param parent
     *            the parent
     * @param style
     *            the style
     */
    public ProfilingViewOptionsDialog( Shell parent, int style ) {
        // Let users override the default styles
        super( parent, style );

        RIA ria = RIA.getCurrentDebugger();
        if( ria == null ) {
            types = new ProfileType[] {};
        }else{//MKS 2486071
            String debugAttachedTo = ria.getDebugAttachTo();
            if(debugAttachedTo == null || debugAttachedTo.isEmpty()){
            	 types = new ProfileType[] {};
            }else {
                types = getProfileTypes( ria );
            }
        }
        setText( Messages.OptionsDialog_OPTIONS_DIALOG_TITLE );
    }

    /**
     * Opens the dialog.
     *
     * @return String
     */
    public void open() {
        // Create the dialog window
        Shell shell = new Shell( getParent(), getStyle() );
        shell.setMinimumSize( 300, 100 );
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
     * Creates the dialog's contents
     *
     * @param shell
     *            the dialog window
     */
    private void createContents( final Shell shell ) {
        IPreferenceStore ps = ContextManager.PLUGIN.getPreferenceStore();
        shell.setLayout( new GridLayout( 2, false ) );
        GridData data;
        data = new GridData( GridData.FILL_HORIZONTAL );
        // Display the "method attribution" combo
        Label label1 = new Label( shell, SWT.NONE );
        label1.setText( Messages.OptionsDialog_METHOD_ATTRIBUTION_LABEL_TITLE );
        final Combo comMethodAtt = new Combo( shell, SWT.READ_ONLY );
        comMethodAtt.setLayoutData( data );
        comMethodAtt
                .setItems( new String[] { RIA.getString( Messages.OptionsDialog_1 ), RIA.getString( Messages.OptionsDialog_2 ) } );
        comMethodAtt.select( ps.getBoolean( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_METHOD_TIME_TYPE ) ? 0 : 1 );

        // Display the "what to profile" combo
        Label label2 = new Label( shell, SWT.NONE );
        label2.setText( Messages.OptionsDialog_WHATTOPROFILE_LABEL_TITLE );
        final Combo comWhatToProfile = new Combo( shell, SWT.READ_ONLY );
        comWhatToProfile.setLayoutData( data );
        comWhatToProfile.setItems( getProfileTypeNames() );
        comWhatToProfile
                .select( indexOfProfileType( types, ps.getInt( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_WHATTOPROFILE ) ) );

        Composite buttonComposite = new Composite( shell, SWT.NONE );
        GridLayout layout = new GridLayout( 2, true );
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        buttonComposite.setLayout( layout );
        data = new GridData( GridData.FILL_HORIZONTAL );
        data.horizontalSpan = 2;
        buttonComposite.setLayoutData( data );

        // Create the OK button and add a handler
        // so that pressing it will set input
        // to the entered value
        Button ok = new Button( buttonComposite, SWT.PUSH );
        ok.setText( Messages.IConstants_OK_BUTTON_TITLE );
        data = new GridData( GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_END );
        data.widthHint = 60;
        ok.setLayoutData( data );
        ok.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent event ) {
                _okButtonClicked = true;
                IPreferenceStore preferenceStore = ContextManager.PLUGIN.getPreferenceStore();
                preferenceStore.setValue( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_WHATTOPROFILE,
                        types[ comWhatToProfile.getSelectionIndex() ].getId() );
                preferenceStore.setValue( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_METHOD_TIME_TYPE,
                        comMethodAtt.getSelectionIndex() == 0 ? true : false );
                shell.close();
            }
        } );

        // Create the cancel button and add a handler
        Button cancel = new Button( buttonComposite, SWT.PUSH );
        cancel.setText( Messages.IConstants_CANCEL_BUTTON_TITLE );
        data = new GridData( GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING );
        data.widthHint = 60;
        cancel.setLayoutData( data );
        cancel.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent event ) {
                _okButtonClicked = false;
                shell.close();
            }
        } );

        // Set the OK button as the default, so
        // user can type input and press Enter
        // to dismiss
        shell.setDefaultButton( ok );
    }

    /**
     * Gets names of all profile types.
     *
     * @return
     */
    private String[] getProfileTypeNames() {
        String[] names = new String[ types.length ];
        for( int i = 0; i < types.length; i++ )
            names[ i ] = types[ i ].getDescription();
        return names;
    }

    /**
     * Get all profile options.
     *
     * @param ria
     *            The debugger
     * @return all profile options
     */
    public static ProfileType[] getProfileTypes( RIA ria ) {
        String debugAttachedTo = ria.getDebugAttachTo();
        ProfileType[] types;
        if( debugAttachedTo.contains( "USB" ) ) {
            types = ria.profileGetTypes( RIA.PROFILE_TYPE_DEVICE );
        } else {
            types = ria.profileGetTypes( RIA.PROFILE_TYPE_SIMULATOR );
        }
        return types;
    }

    private static int indexOfProfileType( ProfileType[] types, int id ) {
        for( int i = 0; i < types.length; i++ ) {
            if( types[ i ].getId() == id ) {
                return i;
            }
        }
        return types.length > 0 ? 0 : -1;
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
