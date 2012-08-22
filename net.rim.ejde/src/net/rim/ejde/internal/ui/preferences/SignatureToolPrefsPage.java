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
package net.rim.ejde.internal.ui.preferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.preferences.SignatureToolPreferences;
import net.rim.ejde.internal.signing.ImportCSIFilesAction;
import net.rim.ejde.internal.ui.widgets.dialog.SigningSearchDialog;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMToolsUtils;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;

public class SignatureToolPrefsPage extends BasicPrefsPage {

    public static final String ID = "net.rim.ejde.internal.ui.preferences.CodeSigningPrefsPage"; //$NON-NLS-1$

    static private final Logger _log = Logger.getLogger( SignatureToolPrefsPage.class );
    private Link _searchKeyLink, _removeKeyLink;
    private Button _runSignToolSilently;
    private Button _runSignToolAutomatically;

    @Override
    protected Control createContents( Composite parent ) {

        Composite main = new Composite( parent, SWT.NONE );
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        main.setLayout( layout );
        main.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        Label linkLabel = new Label( main, SWT.NONE );
        linkLabel.setText( Messages.CodeSigningPrefsPage_ClickHereLabel );

        Link keyLink = new Link( main, SWT.NONE );
        keyLink.setText( Messages.CodeSigningPrefsPage_AddNewKeyLabel );
        keyLink.setToolTipText( Messages.CodeSigningPrefsPage_AddNewKeyToolTip );

        keyLink.addListener( SWT.Selection, new Listener() {
            public void handleEvent( Event event ) {
                ImportCSIFilesAction action = new ImportCSIFilesAction();
                action.run( null );
            }
        } );

        _searchKeyLink = new Link( main, SWT.NONE );
        _searchKeyLink.setText( Messages.CodeSigningPrefsPage_AddOldKeyLabel );
        _searchKeyLink.setToolTipText( Messages.CodeSigningPrefsPage_AddOldKeyToolTip );

        _removeKeyLink = new Link( main, SWT.NONE );
        _removeKeyLink.setText( Messages.CodeSigningPrefsPage_RemoveCurrentKeyLabel );
        _removeKeyLink.setToolTipText( Messages.CodeSigningPrefsPage_RemoveCurrentKeyToolTip );

        File cskFile;
        File dbFile;
        try {
            cskFile = new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.CSK_FILE_NAME );
            dbFile = new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.DB_FILE_NAME );
            if( ( cskFile.exists() ) && ( dbFile.exists() ) ) {
                _searchKeyLink.setEnabled( false );
                _removeKeyLink.setEnabled( true );
            } else {
                _searchKeyLink.setEnabled( true );
                _removeKeyLink.setEnabled( false );
            }
        } catch( IOException io ) {
            _log.error( io.getMessage() );
        }

        _searchKeyLink.addListener( SWT.Selection, new Listener() {
            public void handleEvent( Event event ) {
                // Open file dialog to allow user select the parent folder of *.csk and *.db files
                SigningSearchDialog oldKeyDialog = new SigningSearchDialog( getShell() );
                try {
                    ArrayList< File > oldKeyFiles = oldKeyDialog.search();
                    if( oldKeyFiles != null ) {
                        oldKeyDialog.copyFileIntoSignToolDir( oldKeyFiles );
                        MessageDialog dialog = new MessageDialog( getShell(), Messages.CodeSigningPrefsPage_MessageDialogTitle1,
                                null, Messages.CodeSigningPrefsPage_MessageDialogMsg1, MessageDialog.INFORMATION,
                                new String[] { IDialogConstants.OK_LABEL }, 0 );
                        dialog.open();
                        _searchKeyLink.setEnabled( false );
                        _removeKeyLink.setEnabled( true );
                        _log.info( Messages.CodeSigningPrefsPage_MessageDialogMsg9 );
                    }
                } catch( IllegalArgumentException ex ) {
                    MessageDialog dialog = new MessageDialog( getShell(), Messages.CodeSigningPrefsPage_MessageDialogTitle1,
                            null, ex.getMessage(), MessageDialog.WARNING, new String[] { IDialogConstants.OK_LABEL }, 0 );
                    dialog.open();
                }
            }
        } );

        _removeKeyLink.addListener( SWT.Selection, new Listener() {
            public void handleEvent( Event event ) {
                if( MessageDialog.openQuestion( getShell(), Messages.CodeSigningPrefsPage_MessageDialogTitle3,
                        Messages.CodeSigningPrefsPage_MessageDialogMsg4 + Messages.CodeSigningPrefsPage_MessageDialogMsg6 ) ) {
                    removeKeys();
                }
            }
        } );

        GridData gridData = new GridData( GridData.FILL, GridData.CENTER, true, false );
        gridData.verticalIndent = 15;

        _runSignToolAutomatically = new Button( main, SWT.CHECK );
        _runSignToolAutomatically.setText( Messages.SignatureToolPrefsPage_AutomaticallySigningBtnMsg );
        _runSignToolAutomatically.setToolTipText( Messages.SignatureToolPrefsPage_AutomaticallySigningBtnTooltipMsg );
        _runSignToolAutomatically.setLayoutData( gridData );

        _runSignToolSilently = new Button( main, SWT.CHECK );
        _runSignToolSilently.setText( Messages.SignatureToolPrefsPage_SilentToolBtnMsg );
        _runSignToolSilently.setToolTipText( Messages.SignatureToolPrefsPage_SilentToolBtnTooltipMsg );
        _runSignToolSilently.setLayoutData( gridData );

        initValues();

        return parent;
    }

    private void removeKeys() {
        try {
            File cskFile = new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.CSK_FILE_NAME );
            File dbFile = new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.DB_FILE_NAME );
            if( ( !cskFile.exists() ) && ( !dbFile.exists() ) ) {
                MessageDialog dialog = new MessageDialog( getShell(), Messages.CodeSigningPrefsPage_MessageDialogTitle3, null,
                        Messages.CodeSigningPrefsPage_MessageDialogMsg3, MessageDialog.WARNING,
                        new String[] { IDialogConstants.OK_LABEL }, 0 );
                dialog.open();
                return;
            }
            if( cskFile.exists() ) {
                cskFile.renameTo( new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.CSK_FILE_NAME
                        + IConstants.UNDERSCORE_STRING + System.currentTimeMillis() ) );
            }
            if( dbFile.exists() ) {
                dbFile.renameTo( new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.DB_FILE_NAME
                        + IConstants.UNDERSCORE_STRING + System.currentTimeMillis() ) );
            }
            if( ( !cskFile.exists() ) && ( !dbFile.exists() ) ) {
                MessageDialog dialog = new MessageDialog( getShell(), Messages.CodeSigningPrefsPage_MessageDialogTitle3, null,
                        Messages.CodeSigningPrefsPage_MessageDialogMsg5 + Messages.CodeSigningPrefsPage_MessageDialogMsg6,
                        MessageDialog.INFORMATION, new String[] { IDialogConstants.OK_LABEL }, 0 );
                dialog.open();
                _searchKeyLink.setEnabled( true );
                _removeKeyLink.setEnabled( false );
                _log.info( Messages.CodeSigningPrefsPage_MessageDialogMsg7 );
            }
        } catch( IOException e ) {
            _log.error( e.getMessage() );
        }
    }

    @Override
    public boolean performOk() {
        storePrefValues();
        return true;
    }

    @Override
    protected void performDefaults() {
        initializeDefaults();
        super.performDefaults();
    }

    private void initValues() {
        if( _runSignToolSilently != null ) {
            _runSignToolSilently.setSelection( SignatureToolPreferences.getRunSignatureToolSilently() );
        }

        if( _runSignToolAutomatically != null ) {
            _runSignToolAutomatically.setSelection( SignatureToolPreferences.getRunSignatureToolAutomatically() );
        }
    }

    private void initializeDefaults() {
        if( _runSignToolSilently != null ) {
            _runSignToolSilently.setSelection( SignatureToolPreferences.getDefaultRunSignatureToolSilently() );
        }
        if( _runSignToolAutomatically != null ) {
            _runSignToolAutomatically.setSelection( SignatureToolPreferences.getDefaultRunSignatureToolAutomatically() );
        }
    }

    private void storePrefValues() {
        if( _runSignToolSilently != null ) {
            SignatureToolPreferences.setRunSignatureToolSilently( _runSignToolSilently.getSelection() );
        }
        if( _runSignToolAutomatically != null ) {
            SignatureToolPreferences.setRunSignatureToolAutomatically( _runSignToolAutomatically.getSelection() );
        }
    }
}
