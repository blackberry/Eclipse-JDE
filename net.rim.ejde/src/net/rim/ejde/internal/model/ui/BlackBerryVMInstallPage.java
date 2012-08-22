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
package net.rim.ejde.internal.model.ui;

import java.io.File;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BlackBerryVMInstallType;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.jdt.debug.ui.launchConfigurations.AbstractVMInstallPage;
import org.eclipse.jdt.internal.debug.ui.IJavaDebugHelpContextIds;
import org.eclipse.jdt.internal.debug.ui.StatusInfo;
import org.eclipse.jdt.internal.debug.ui.jres.JREMessages;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * @author cmalinescu, jheifetz
 *
 */
public class BlackBerryVMInstallPage extends AbstractVMInstallPage {

    // VM being edited or created
    private VMStandin _vmStandin;
    private Text _vmName;
    private Text _vmArgs;
    public Text _eeFile;
    private VMLibraryBlock _libraryBlock;
    private IStatus[] _fieldStatus = new IStatus[ 1 ];
    private boolean _ignoreCallbacks = false;
    final Image BB_IMAGE = AbstractUIPlugin.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons" //$NON-NLS-1$
            + File.separator + Messages.BlackBerryVMInstallPage_BBVMInstallPageIcon ).createImage();

    static final private Logger _log = Logger.getLogger( BlackBerryVMInstallPage.class );

    /**
	 *
	 */
    public BlackBerryVMInstallPage() {
        super( Messages.BlackBerryVMInstallPage_BBVMInstallPageTitle );
        for( int i = 0; i < _fieldStatus.length; i++ ) {
            _fieldStatus[ i ] = Status.OK_STATUS;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.IDialogPage#getImage()
     */
    @Override
    public Image getImage() {
        return BB_IMAGE;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl( Composite p ) {
        // create a composite with standard margins and spacing
        Composite composite = new Composite( p, SWT.NONE );
        GridLayout layout = new GridLayout();
        layout.numColumns = 3;
        composite.setLayout( layout );
        composite.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        // VM location
        SWTFactory.createLabel( composite, JREMessages.EEVMPage_1, 1 );
        _eeFile = SWTFactory.createSingleText( composite, 1 );
        Button folders = SWTFactory.createPushButton( composite, JREMessages.EEVMPage_2, null );
        GridData data = (GridData) folders.getLayoutData();
        data.horizontalAlignment = GridData.END;
        // VM name
        SWTFactory.createLabel( composite, JREMessages.addVMDialog_jreName, 1 );
        _vmName = SWTFactory.createSingleText( composite, 2 );
        // VM arguments
        Label label = SWTFactory.createLabel( composite, JREMessages.AddVMDialog_23, 2 );
        GridData gd = (GridData) label.getLayoutData();
        gd.verticalAlignment = SWT.BEGINNING;
        _vmArgs = SWTFactory.createText( composite, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP, 3, "" ); //$NON-NLS-1$
        gd = (GridData) _vmArgs.getLayoutData();
        gd.widthHint = 200;
        gd.heightHint = 75;
        // VM libraries block
        SWTFactory.createLabel( composite, JREMessages.AddVMDialog_JRE_system_libraries__1, 3 );
        _libraryBlock = new VMLibraryBlock();
        _libraryBlock.setWizard( getWizard() );
        _libraryBlock.createControl( composite );
        Control libControl = _libraryBlock.getControl();
        gd = new GridData( GridData.FILL_BOTH );
        gd.horizontalSpan = 3;
        libControl.setLayoutData( gd );

        initializeFields();
        // add the listeners now to prevent them from monkeying with initialized settings
        _vmName.addModifyListener( new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
                if( !_ignoreCallbacks ) {
                    validateVMName();
                }
            }
        } );
        _eeFile.addModifyListener( new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
                if( !_ignoreCallbacks ) {
                    if( validateDefinitionFile().isOK() ) {
                        reloadDefinitionFile();
                    }
                }
            }
        } );
        folders.addSelectionListener( new SelectionListener() {
            public void widgetDefaultSelected( SelectionEvent e ) {
            }

            public void widgetSelected( SelectionEvent e ) {
                FileDialog dialog = new FileDialog( getShell() );
                dialog.setFilterExtensions( new String[] { "*.ee" } ); //$NON-NLS-1$
                File file = getDefinitionFile();
                String text = _eeFile.getText();
                if( ( file != null ) && file.isFile() ) {
                    text = file.getParentFile().getAbsolutePath();
                }
                dialog.setFileName( text );
                String newPath = dialog.open();
                if( newPath != null ) {
                    _eeFile.setText( newPath );
                }
            }
        } );
        Dialog.applyDialogFont( composite );
        setControl( composite );
        PlatformUI.getWorkbench().getHelpSystem().setHelp( getControl(), IJavaDebugHelpContextIds.EDIT_JRE_EE_FILE_WIZARD_PAGE );
    }

    /**
     * Validates the JRE location
     *
     * @return the status after validating the JRE location
     */
    private IStatus validateDefinitionFile() {
        String locationName = _eeFile.getText();
        IStatus s = null;
        File file = null;
        if( locationName.length() == 0 ) {
            s = new StatusInfo( IStatus.WARNING, JREMessages.EEVMPage_4 );
        } else {
            file = new File( locationName );
            if( !file.exists() ) {
                s = new StatusInfo( IStatus.ERROR, JREMessages.EEVMPage_5 );
            } else {
                final IStatus[] temp = new IStatus[ 1 ];
                final VMStandin[] vm = new VMStandin[ 1 ];
                final File tempFile = file;
                Runnable r = new Runnable() {
                    public void run() {
                        try {
                            String name = _vmStandin.getName();
                            if( name == IConstants.EMPTY_STRING ) {
                                vm[ 0 ] = VMUtils.createVMFromDefinitionFile( tempFile, true );
                            } else {
                                vm[ 0 ] = VMUtils.createVMFromDefinitionFile( tempFile, name, _vmStandin.getId() );
                            }

                            IStatus status = vm[ 0 ].getVMInstallType().validateInstallLocation( vm[ 0 ].getInstallLocation() );

                            if( status.getSeverity() != IStatus.ERROR ) {
                                temp[ 0 ] = Status.OK_STATUS;
                            } else {
                                temp[ 0 ] = status;
                            }
                        } catch( CoreException e ) {
                            temp[ 0 ] = e.getStatus();
                            _log.error( "BlackBerry VM Install Page VM Reload Error", e );
                        }
                    }
                };
                BusyIndicator.showWhile( getShell().getDisplay(), r );
                s = temp[ 0 ];
            }
        }
        setDefinitionFileStatus( s );
        updatePageStatus();
        return s;
    }

    /**
     * Initializes the JRE attributes from the definition file
     */
    private void reloadDefinitionFile() {
        IStatus s = Status.OK_STATUS;
        File file = getDefinitionFile();
        if( ( file != null ) && file.exists() ) {
            final IStatus[] temp = new IStatus[ 1 ];
            final VMStandin[] vm = new VMStandin[ 1 ];
            final File tempFile = file;
            Runnable r = new Runnable() {
                public void run() {
                    try {
                        String name = _vmStandin.getName();
                        if( name == IConstants.EMPTY_STRING ) {
                            vm[ 0 ] = VMUtils.createVMFromDefinitionFile( tempFile, true );
                        } else {
                            vm[ 0 ] = VMUtils.createVMFromDefinitionFile( tempFile, name, _vmStandin.getId() );
                        }
                        temp[ 0 ] = Status.OK_STATUS;
                    } catch( CoreException e ) {
                        temp[ 0 ] = e.getStatus();
                        _log.error( "BlackBerry VM Install Page VM Reload Error", e );

                    }
                }
            };
            BusyIndicator.showWhile( getShell().getDisplay(), r );
            s = temp[ 0 ];
            if( s.isOK() ) {
                _vmStandin = vm[ 0 ];
            }
        }
        if( s.isOK() && ( file != null ) ) {
            initializeFields();
        }
        setDefinitionFileStatus( s );
    }

    /**
     * Validates the entered name of the VM
     *
     * @return the status of the name validation
     */
    private void validateVMName() {
        nameChanged( _vmName.getText() );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.debug.ui.launchConfigurations.AbstractVMInstallPage#finish()
     */
    @Override
    public boolean finish() {
        setFieldValuesToVM( _vmStandin );
        _libraryBlock.finish();
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.debug.ui.launchConfigurations.AbstractVMInstallPage#getSelection()
     */
    @Override
    public VMStandin getSelection() {
        return _vmStandin;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.debug.ui.launchConfigurations.AbstractVMInstallPage#setSelection(org.eclipse.jdt.launching.VMStandin)
     */
    @Override
    public void setSelection( VMStandin vm ) {
        super.setSelection( vm );
        _vmStandin = vm;
        setTitle( JREMessages.EEVMPage_6 );
        setDescription( JREMessages.EEVMPage_7 );
    }

    /**
     * initialize fields to the specified VM
     *
     * @param vm
     *            the VM to initialize from
     */
    protected void setFieldValuesToVM( VMStandin vm ) {
        vm.setName( _vmName.getText() );
        String argString = _vmArgs.getText().trim();
        if( ( argString != null ) && ( argString.length() > 0 ) ) {
            vm.setVMArgs( argString );
        } else {
            vm.setVMArgs( null );
        }
    }

    /**
     * Returns the definition file from the text control or <code>null</code> if none.
     *
     * @return definition file or <code>null</code>
     */
    private File getDefinitionFile() {
        String path = _eeFile.getText().trim();
        if( path.length() > 0 ) {
            return new File( path );
        }
        return null;

    }

    /**
     * Initialize the dialogs fields
     */
    private void initializeFields() {
        try {
            _ignoreCallbacks = true;
            _libraryBlock.setSelection( _vmStandin );
            _vmName.setText( _vmStandin.getName() );
            String eePath = _vmStandin.getAttribute( BlackBerryVMInstallType.ATTR_DEFINITION_FILE );
            if( eePath != null ) {
                _eeFile.setText( eePath );
            }
            String vmArgs = _vmStandin.getVMArgs();
            if( vmArgs != null ) {
                _vmArgs.setText( vmArgs );
            }
            validateVMName();
            validateDefinitionFile();
        } finally {
            _ignoreCallbacks = false;
        }
    }

    /**
     * Sets the status of the definition file.
     *
     * @param status
     *            definition file status
     */
    private void setDefinitionFileStatus( IStatus status ) {
        _fieldStatus[ 0 ] = status;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.DialogPage#getErrorMessage()
     */
    @Override
    public String getErrorMessage() {
        String message = super.getErrorMessage();
        if( message == null ) {
            return _libraryBlock.getErrorMessage();
        }
        return message;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
     */
    @Override
    public boolean isPageComplete() {
        boolean complete = super.isPageComplete();
        if( complete ) {
            return _libraryBlock.isPageComplete();
        }
        return complete;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.debug.ui.launchConfigurations.AbstractVMInstallPage#getVMStatus()
     */
    @Override
    protected IStatus[] getVMStatus() {
        return _fieldStatus;
    }

}
