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

import net.rim.ejde.internal.model.preferences.RootPreferences;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class RootPrefsPage extends BasicPrefsPage {

    public static final String ID = "net.rim.ejde.internal.ui.preferences"; //$NON-NLS-1$

    private Text _versionText;
    private Text _vendorText;
    private Button _openAppDescriptorCheckbox;
    private Button _openStartupCheckbox;
    private Button _checkForNewVersion;
    private Button _appendLogToFile;
    private Text   _logFile;
    private Button _downloadDebugFiles;
    private Button _notDownloadDebugFiles;
    private Button _promptDownloadDebugFiles;


    /**
     * Creates and returns the SWT control for the customized body of this property page under the given parent composite.
     *
     * @param parent
     *            the parent composite
     * @return the new control
     */
    @Override
    protected Control createContents( Composite parent ) {

        Composite main = new Composite( parent, SWT.NONE );
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        main.setLayout( gridLayout );
        main.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        Label versionLabel = new Label( main, SWT.NONE );
        versionLabel.setText( Messages.WorkspacePrefsPage_ProjectVersion );

        GridData gridData = new GridData( SWT.FILL, SWT.CENTER, false, false );
        gridData.widthHint = 100;

        _versionText = new Text( main, SWT.LEFT | SWT.SINGLE | SWT.BORDER | SWT.WRAP );
        _versionText.setLayoutData( gridData );

        gridData = new GridData( SWT.FILL, SWT.CENTER, true, false );

        Label vendorLabel = new Label( main, SWT.NONE );
        vendorLabel.setText( Messages.WorkspacePrefsPage_ProjectVendor );

        _vendorText = new Text( main, SWT.LEFT | SWT.SINGLE | SWT.BORDER | SWT.WRAP );
        _vendorText.setLayoutData( gridData );

        gridData = new GridData( SWT.FILL, SWT.CENTER, false, false );
        gridData.verticalIndent = 15;

        _openAppDescriptorCheckbox = new Button( main, SWT.CHECK );
        _openAppDescriptorCheckbox.setText( Messages.WorkspacePrefsPage_OpenAppDescriptorOnNew );
        _openAppDescriptorCheckbox.setLayoutData( gridData );

        _openStartupCheckbox = new Button( main, SWT.CHECK );
        _openStartupCheckbox.setText( Messages.WorkspacePrefsPage_OpenStartupOnNew );
        _openStartupCheckbox.setLayoutData( gridData );

        _checkForNewVersion = new Button( main, SWT.CHECK );
        _checkForNewVersion.setText( Messages.WorkspacePrefsPage_CheckForNewVersion );
        _checkForNewVersion.setLayoutData( gridData );

        _appendLogToFile = new Button( main, SWT.CHECK );
        _appendLogToFile.setText( Messages.WorkspacePrefsPage_AppendLogToFile );
        _appendLogToFile.setLayoutData( gridData );

        gridData = new GridData( SWT.FILL, SWT.CENTER, true, false );

        _logFile = new Text( main, SWT.LEFT | SWT.SINGLE | SWT.BORDER | SWT.WRAP );
        _logFile.setLayoutData( gridData );

        Group group = new Group( main, SWT.NONE );
        group.setText("Download debug files");
        gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        group.setLayout( gridLayout );
        group.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        _promptDownloadDebugFiles = new Button( group, SWT.RADIO );
        _promptDownloadDebugFiles.setText( "Prompt" );
        _downloadDebugFiles = new Button( group, SWT.RADIO );
        _downloadDebugFiles.setText( "Yes" );
        _notDownloadDebugFiles = new Button( group, SWT.RADIO );
        _notDownloadDebugFiles.setText( "No" );

        initValues();

        return parent;
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
        _versionText.setText( RootPreferences.getProjectVersion() );
        _vendorText.setText( RootPreferences.getProjectVendor() );
        _openAppDescriptorCheckbox.setSelection( RootPreferences.getOpenAppDescriptorOnNew() );
        _openStartupCheckbox.setSelection( RootPreferences.getOpenStartupOnNew() );
        _checkForNewVersion.setSelection( RootPreferences.getUpdateNotify() );
        _appendLogToFile.setSelection( RootPreferences.getAppendConsoleLogToFile() );
        _logFile.setText( RootPreferences.getConsoleLogFile() );
        int option = RootPreferences.getDownloadDebugFilesOption();
        _promptDownloadDebugFiles.setSelection( option == PreferenceConstants.DOWNLOAD_DEBUG_FILES_PROMPT );
        _downloadDebugFiles.setSelection( option == PreferenceConstants.DOWNLOAD_DEBUG_FILES_YES );
        _notDownloadDebugFiles.setSelection( option == PreferenceConstants.DOWNLOAD_DEBUG_FILES_NO );
    }

    private void initializeDefaults() {
        _versionText.setText( RootPreferences.getDefaultProjectVersion() );
        _vendorText.setText( RootPreferences.getDefaultProjectVendor() );
        _openAppDescriptorCheckbox.setSelection( RootPreferences.getDefaultOpenAppDescriptorOnNew() );
        _openStartupCheckbox.setSelection( RootPreferences.getDefaultOpenStartupOnNew() );
        _checkForNewVersion.setSelection( RootPreferences.getDefaultUpdateNotify() );
        _appendLogToFile.setSelection( RootPreferences.getDefaultAppendConsoleLogToFile() );
        _logFile.setText( RootPreferences.getDefaultConsoleLogFile() );
        int option = RootPreferences.getDefaultDownloadDebugFilesOption();
        _promptDownloadDebugFiles.setSelection( option == PreferenceConstants.DOWNLOAD_DEBUG_FILES_PROMPT );
        _downloadDebugFiles.setSelection( option == PreferenceConstants.DOWNLOAD_DEBUG_FILES_YES );
        _notDownloadDebugFiles.setSelection( option == PreferenceConstants.DOWNLOAD_DEBUG_FILES_NO );
    }

    private void storePrefValues() {
        RootPreferences.setProjectVersion( _versionText.getText() );
        RootPreferences.setProjectVendor( _vendorText.getText() );
        RootPreferences.setOpenAppDescriptorOnNew( _openAppDescriptorCheckbox.getSelection() );
        RootPreferences.setOpenStartupOnNew( _openStartupCheckbox.getSelection() );
        RootPreferences.setUpdateNotify( _checkForNewVersion.getSelection() );
        RootPreferences.setAppendConsoleLogToFile( _appendLogToFile.getSelection() );
        RootPreferences.setConsoleLogFile( _logFile.getText() );
        if( _promptDownloadDebugFiles.getSelection() ) {
            RootPreferences.setDownloadDebugFilesOption( PreferenceConstants.DOWNLOAD_DEBUG_FILES_PROMPT );
        } else if( _downloadDebugFiles.getSelection() ) {
            RootPreferences.setDownloadDebugFilesOption( PreferenceConstants.DOWNLOAD_DEBUG_FILES_YES );
        } else {
            RootPreferences.setDownloadDebugFilesOption( PreferenceConstants.DOWNLOAD_DEBUG_FILES_NO );
        }
    }
}
