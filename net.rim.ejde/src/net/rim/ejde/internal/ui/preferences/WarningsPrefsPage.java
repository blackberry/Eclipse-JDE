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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.core.IRIMMarker;
import net.rim.ejde.internal.model.preferences.WarningsPreferences;
import net.rim.ejde.internal.signing.BBSigningKeys;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class WarningsPrefsPage extends BasicPrefsPage {

    public static final String ID = "net.rim.ejde.internal.ui.preferences.DebugPrefsPage";

    static private final Logger _log = Logger.getLogger( WarningsPrefsPage.class );
    private Button _promptForDebugFileButton;
    private Button _promptForMissingDependenciesFileButton;
    private Map< String, List< String >> _keyTable;
    private Tree _checkTree;
    private Map< String, Boolean > _statusTable;

    @Override
    protected Control createContents( Composite parent ) {
        Composite main = new Composite( parent, SWT.NONE );
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.numColumns = 1;
        main.setLayout( layout );
        main.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        Label warnLabel = new Label( main, SWT.NONE );
        warnLabel.setText( Messages.CodeSigningPrefsPage_SigningStatusLabel );

        GridData gridData = new GridData( SWT.FILL, SWT.FILL, true, true );

        _keyTable = populateKeyTable();

        _checkTree = new Tree( main, SWT.CHECK );
        _checkTree.setRedraw( false );
        _checkTree.setBackground( main.getBackground() );
        _checkTree.setLayoutData( gridData );

        for( String root : _keyTable.keySet() ) {
            TreeItem item = new TreeItem( _checkTree, SWT.NONE );
            item.setText( root );
            for( String vmName : _keyTable.get( root ) ) {
                TreeItem child = new TreeItem( item, SWT.NONE );
                child.setText( vmName );
            }
        }

        // Turn drawing back on!
        _checkTree.setRedraw( true );
        _checkTree.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                checkAllChildrens( _checkTree );
            }
        } );
        gridData = new GridData( SWT.FILL, SWT.CENTER, true, false );
        // gridData.verticalIndent = 10;

        _promptForDebugFileButton = new Button( main, SWT.CHECK );
        _promptForDebugFileButton.setText( Messages.DebugPrefsPage_WarnForDebugMsg );
        _promptForDebugFileButton.setToolTipText( Messages.DebugPrefsPage_WarnForDebugMsg );
        _promptForDebugFileButton.setLayoutData( gridData );

        _promptForMissingDependenciesFileButton = new Button( main, SWT.CHECK );
        _promptForMissingDependenciesFileButton.setText( Messages.SDKPrefsPage_WarnForMissingDependenciesMsg );
        _promptForMissingDependenciesFileButton.setToolTipText( Messages.SDKPrefsPage_WarnForMissingDependenciesMsg );
        _promptForMissingDependenciesFileButton.setLayoutData( gridData );

        initValues();

        return parent;
    }

    private Map< String, List< String >> populateKeyTable() {
        String id = null;
        _keyTable = new HashMap< String, List< String >>();

        Map< String, BBSigningKeys > signKeysTable = VMUtils.getSignKeysCache();
        for( IVMInstall vm : VMUtils.getInstalledBBVMs() ) {
            VMUtils.addSignKeysToCache( vm );
        }
        signKeysTable = VMUtils.getSignKeysCache();
        for( String vmName : signKeysTable.keySet() ) {
            BBSigningKeys keyObj = signKeysTable.get( vmName );
            for( int key : keyObj.getKeys() ) {
                id = VMUtils.convertKeyToPreferenceLabel( Integer.valueOf( key ), vmName );
                if( _keyTable.containsKey( id ) ) {
                    _keyTable.get( id ).add( vmName );
                } else {
                    _keyTable.put( id, new ArrayList< String >() );
                    _keyTable.get( id ).add( vmName );
                }
            }
        }
        return _keyTable;
    }

    /**
     * This recursive method takes the Tree/TreeItem object as a parameter and check/uncheck all of its descendant children's
     * check boxes responds to their parent's checked status.
     *
     * @param item
     *            Object -- Tree/TreeItem
     */
    void checkAllChildrens( Object obj ) {
        TreeItem[] children;
        if( obj instanceof Tree ) {
            children = ( (Tree) obj ).getItems();
        } else if( obj instanceof TreeItem ) {
            children = ( (TreeItem) obj ).getItems();
        } else {
            return;
        }
        for( TreeItem element : children ) {
            TreeItem item = element;
            if( item.getParentItem() != null ) {
                item.setChecked( item.getParentItem().getChecked() );
            }
            checkAllChildrens( element );
        }
    }

    /**
     * Sets the selection state for the given key
     *
     * @param preferenceLabel
     *            the preference label as created by VMUtils
     * @param state
     *            the checked state
     */
    public void setSelection( String preferenceLabel, boolean state ) {
        for( TreeItem item : _checkTree.getItems() ) {
            if( item.getText().equals( preferenceLabel ) ) {
                item.setChecked( state );
                return;
            }
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
        checkAllChildrens( _checkTree );
        super.performDefaults();
    }

    private void initValues() {
        _statusTable = new HashMap< String, Boolean >();
        for( TreeItem item : _checkTree.getItems() ) {
            _statusTable.put( item.getText(), Boolean.valueOf( item.getChecked() ) );
        }
        WarningsPreferences.getCodeSignWarnStatus( _statusTable );
        boolean status = false;
        for( TreeItem item : _checkTree.getItems() ) {
            status = _statusTable.get( item.getText() ).booleanValue();
            item.setChecked( status );
        }
        _promptForDebugFileButton.setSelection( WarningsPreferences.getPromptForMissingDebugFiles() );
        _promptForMissingDependenciesFileButton.setSelection( WarningsPreferences.getPromptForMissingDependenciesFiles() );
    }

    private void initializeDefaults() {
        Map< String, Boolean > statusTable = new HashMap< String, Boolean >();
        boolean status = false;
        for( TreeItem item : _checkTree.getItems() ) {
            statusTable.put( item.getText(), Boolean.valueOf( item.getChecked() ) );
        }
        WarningsPreferences.setDefaultCodeSignWarnStatus( statusTable );
        for( TreeItem item : _checkTree.getItems() ) {
            status = statusTable.get( item.getText() ).booleanValue();
            item.setChecked( status );
        }
        _promptForDebugFileButton.setSelection( WarningsPreferences.getDefaultPromptForMissingDebugFiles() );
        _promptForMissingDependenciesFileButton.setSelection( WarningsPreferences.getDefaultPromptForMissingDependenciesFiles() );
    }

    private void storePrefValues() {
        Map< String, Boolean > currentStatusTable = new HashMap< String, Boolean >();
        boolean needBuild = false, givePrompt = ContextManager.getDefault().getPreferenceStore()
                .getBoolean( IConstants.PROMPT_FOR_BUILD_KEY );
        List< IMarker > problems = null;
        for( TreeItem item : _checkTree.getItems() ) {
            Integer itemKey = VMUtils.convertPreferenceLabelToKey( item.getText() );
            currentStatusTable.put( item.getText(), Boolean.valueOf( item.getChecked() ) );
            boolean oldCheckedValue = WarningsPreferences.getWarnStatus( item.getText() );
            if( item.getChecked() && !oldCheckedValue ) {
                if( problems == null ) {
                    try {
                        problems = Arrays.asList( ResourcesPlugin.getWorkspace().getRoot()
                                .findMarkers( IRIMMarker.CODE_SIGN_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE ) );
                    } catch( CoreException e ) {
                        _log.error( "Error Finding Workspace Markers", e );
                    }
                    if( problems == null ) {
                        break;
                    }
                }
                List< IMarker > retainedProblems = new ArrayList< IMarker >();
                for( IMarker marker : problems ) {
                    try {
                        Object key = marker.getAttribute( IRIMMarker.KEY );
                        if( key != null && key.equals( itemKey ) ) {
                            marker.delete();
                        } else {
                            retainedProblems.add( marker );
                        }
                    } catch( CoreException e ) {
                        _log.error( "Error Retrieving Key from marker", e );
                    }
                }
                problems = retainedProblems;
            } else if( !item.getChecked() && oldCheckedValue ) {
                needBuild = true;
            }
        }

        WarningsPreferences.setCodeSignWarnStatus( currentStatusTable );
        WarningsPreferences.setPromptForMissingDebugFiles( _promptForDebugFileButton.getSelection() );
        WarningsPreferences.setPromptForMissingDependenciesFiles( _promptForMissingDependenciesFileButton.getSelection() );

        if( needBuild && givePrompt ) {
            MessageDialog dialog = new MessageDialog( getShell(), Messages.CodeSigningPrefsPage_MessageDialogTitle2, null,
                    Messages.CodeSigningPrefsPage_MessageDialogMsg2, MessageDialog.QUESTION, new String[] {
                            IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 2 );
            int res = dialog.open();
            if( res == 0 ) {
                CoreUtility.getBuildJob( null ).schedule();
            }
        }
    }
}
