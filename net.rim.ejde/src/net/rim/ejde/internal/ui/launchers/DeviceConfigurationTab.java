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
package net.rim.ejde.internal.ui.launchers;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.launching.IDeviceLaunchConstants;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ide.RIA;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 * Tab to configure the device to debug.
 */
class DeviceConfigurationTab extends AbstractLaunchConfigurationTab implements IDeviceLaunchConstants {

    private Logger _logger = Logger.getLogger( DeviceConfigurationTab.class );
    private static final String SELECT_A_DEVICE = "SELECT A DEVICE";

    private Button _anyDeviceButton;
    private Button _specificDeviceButton;
    private Button _refreshButton;

    private Combo _deviceAttach;
    private String[] _deviceList;
    private RIA _ria;
    private boolean _firstTimeDisplay;
    private ILaunchConfiguration _configuration;

    /**
     * Default constructor.
     */
    public DeviceConfigurationTab() {
        _ria = null;
        _firstTimeDisplay = true;
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse .swt.widgets.Composite)
     */
    public void createControl( Composite parent ) {
        Composite comp = new Composite( parent, SWT.NONE );
        comp.setLayout( new GridLayout() );
        setControl( comp );

        createDeviceGroup( comp );
    }

    private void createDeviceGroup( Composite parent ) {
        Group group = new Group( parent, SWT.NONE );
        group.setText( Messages.DeviceConfigurationTab_groupLabel );
        GridLayout layout = new GridLayout();
        group.setLayout( layout );
        group.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

        GridData data = new GridData();
        _anyDeviceButton = new Button( group, SWT.RADIO );
        _anyDeviceButton.setText( "Attach to any connected device" );
        _anyDeviceButton.setToolTipText( "Attach to the first USB-connected BlackBerry device found" );
        _anyDeviceButton.setLayoutData( data );
        _anyDeviceButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                enableDeviceList( false );
                updateLaunchConfigurationDialog();
            }
        } );

        _specificDeviceButton = new Button( group, SWT.RADIO );
        _specificDeviceButton.setText( "Attach to specific device" );
        _specificDeviceButton.setToolTipText( "Attach to a specific device listed below" );
        _specificDeviceButton.setLayoutData( data );
        _specificDeviceButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                enableDeviceList( true );
                _deviceList = _ria.getDebugDeviceList();
                updateDeviceList();
            }
        } );

        Composite composite = new Composite( group, SWT.NONE );
        layout = new GridLayout( 3, false );
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        composite.setLayout( layout );
        GridData gridData = new GridData( GridData.FILL_HORIZONTAL );
        composite.setLayoutData( gridData );
        Label deviceLabel = new Label( composite, SWT.NONE );
        deviceLabel.setText( Messages.DeviceConfigurationTab_attachLabel );
        deviceLabel.setToolTipText( Messages.DeviceConfigurationTab_attachToolTip );
        _deviceAttach = new Combo( composite, SWT.DROP_DOWN | SWT.READ_ONLY );
        GridData comboData = new GridData( GridData.FILL_HORIZONTAL );
        _deviceAttach.setLayoutData( comboData );
        _deviceAttach.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                updateLaunchConfigurationDialog();
            }
        } );
        _refreshButton = new Button( composite, SWT.PUSH );
        _refreshButton.setText( Messages.IConstants_IConstants_REFRESH_BUTTON_TITLE );
        _refreshButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                _deviceList = _ria.getDebugDeviceList();
                updateDeviceList();
            }
        } );
    }

    protected void enableDeviceList( boolean enable ) {
        _deviceAttach.setEnabled( enable );
        _refreshButton.setEnabled( enable );
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return Messages.DeviceConfigurationTab_tabName;
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse .debug.core.ILaunchConfiguration)
     */
    public void initializeFrom( ILaunchConfiguration configuration ) {
        _configuration = configuration;
        try {
            // use default BB-VM to initialize RIA
            _ria = ContextManager.PLUGIN.getRIA( VMUtils.getDefaultBBVM().getInstallLocation().getPath() );
            if( _ria == null ) {
                return;
            }
            // only refresh the device list when it is the first
            // time the device list is displayed.
            // Don't refresh device list of user switches from other tabs
            if( _firstTimeDisplay ) {
                _deviceList = _ria.getDebugDeviceList();
                _firstTimeDisplay = false;
            }
            final String device = configuration.getAttribute( DEVICE, ANY_DEVICE );
            if( device.equals( ANY_DEVICE ) ) {
                populateDeviceList();
                _anyDeviceButton.setSelection( true );
                _specificDeviceButton.setSelection( false );
                enableDeviceList( false );
            } else {
                _anyDeviceButton.setSelection( false );
                _specificDeviceButton.setSelection( true );
                populateDeviceList();
                enableDeviceList( true ); // SDR168524
            }
        } catch( CoreException e ) {
            _logger.error( e );
        }
    }

    private void populateDeviceList() {
        _deviceAttach.setItems( _deviceList );
        _deviceAttach.add( SELECT_A_DEVICE, 0 );
        String device = LaunchUtils.getStringAttribute( _configuration, IDeviceLaunchConstants.DEVICE, StringUtils.EMPTY );
        int index = _deviceAttach.indexOf( device );
        if( index != -1 ) {
            _deviceAttach.select( index );
        } else {
            _deviceAttach.select( 0 );
        }
        updateLaunchConfigurationDialog();
    }

    private void updateDeviceList() {
        int index = _deviceAttach.getSelectionIndex();
        String currentSelection = _deviceAttach.getItem( index );
        _deviceAttach.setItems( _deviceList );
        _deviceAttach.add( SELECT_A_DEVICE, 0 );
        index = _deviceAttach.indexOf( currentSelection );
        // device is disconnected, select the first item: "SELECT A DEIVE"
        if( index == -1 ) {
            index = 0;
        } else if( index == 0 ) {
            // automatically select the first available device
            if( _deviceList.length > 0 ) {
                index = 1;
            }
        }
        _deviceAttach.select( index );
        updateLaunchConfigurationDialog();
    }

    @Override
    public boolean canSave() {
        return isValid( _configuration );
    }

    @Override
    public boolean isValid( ILaunchConfiguration configuration ) {
        try {
            // device selected
            String device = configuration.getAttribute( IDeviceLaunchConstants.DEVICE, StringUtils.EMPTY );
            if( StringUtils.isBlank( device ) || device.equals( SELECT_A_DEVICE ) ) {
                setErrorMessage( Messages.DeviceConfigurationTab_noDeviceMsg );
                return false;
            }
        } catch( CoreException e ) {
            _logger.error( e );
            setErrorMessage( e.getMessage() );
            return false;
        }
        setMessage( null );
        setErrorMessage( null );
        return true;
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse .debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply( ILaunchConfigurationWorkingCopy configuration ) {
        if( _specificDeviceButton.getSelection() ) {
            int index = _deviceAttach.getSelectionIndex();
            if( index != -1 ) {
                String device = _deviceAttach.getItem( index );
                configuration.setAttribute( DEVICE, device );
            }
        } else {
            configuration.setAttribute( DEVICE, ANY_DEVICE );
        }
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse. debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults( ILaunchConfigurationWorkingCopy configuration ) {
        // do nothing
    }

}
