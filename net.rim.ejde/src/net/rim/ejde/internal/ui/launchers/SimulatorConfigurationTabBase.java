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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.core.RimIDEUtil;
import net.rim.ejde.internal.launching.DeviceInfo;
import net.rim.ejde.internal.launching.FledgeLaunchConfigurationDelegate.LaunchParams;
import net.rim.ejde.internal.launching.IFledgeLaunchConstants;
import net.rim.ejde.internal.ui.CompositeFactory;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.validation.ValidationUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Tab to configure the simulator. Overridden by both the external eJDE and the internal eJDE plug-in fragment to add extended
 * behavior.
 */
public abstract class SimulatorConfigurationTabBase extends AbstractLaunchConfigurationTab implements IFledgeLaunchConstants,
        PropertyChangeListener {

    protected static final Logger _logger = Logger.getLogger( SimulatorConfigurationTab.class );

    protected TabFolder _tabFolder;
    private GeneralTab _generalTab;
    private DebuggingTab _debuggingTab;
    private AdvancedTab _advancedTab;
    private MemoryTab _memoryTab;
    private NetworkTab _networkTab;
    private PortsTab _portsTab;
    private ViewTab _viewTab;
    protected boolean _initializingForm;
    private ILaunchConfiguration _configuration;
    static SimulatorConfigurationTabBase _thisInst; 

    /**
     * Default constructor.
     */
    public SimulatorConfigurationTabBase() {
        _thisInst = this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse .swt.widgets.Composite)
     */
    public void createControl( Composite parent ) {
        Composite mainComposite = new Composite( parent, SWT.NONE );
        mainComposite.setLayout( new GridLayout() );
        GridData fillData = new GridData( GridData.FILL_BOTH );
        mainComposite.setLayoutData( fillData );
        buildProfileUI( mainComposite );
        setControl( mainComposite );
    }

    /**
     * Build the simulator profile UI from RIA
     *
     * @param profileName
     */
    protected void buildProfileUI( Composite parent ) {

        _tabFolder = new TabFolder( parent, SWT.NONE );
        _tabFolder.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        // general tab
        _generalTab = new GeneralTab();
        Control generalControl = _generalTab.createControl( _tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
        GridData data = new GridData( GridData.FILL_BOTH );
        generalControl.setLayoutData( data );
        TabItem tab1 = new TabItem( _tabFolder, SWT.BORDER );
        tab1.setText( Messages.SimulatorConfigurationTab_generalTabLabel );
        tab1.setControl( generalControl );

        // debugging tab
        _debuggingTab = new DebuggingTab();
        Control debugControl = _debuggingTab.createControl( _tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
        debugControl.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        TabItem tab2 = new TabItem( _tabFolder, SWT.BORDER );
        tab2.setText( Messages.SimulatorConfigurationTab_debuggingTabLabel );
        tab2.setControl( debugControl );

        // memory tab
        _memoryTab = new MemoryTab();
        Control memoryControl = _memoryTab.createControl( _tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
        memoryControl.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        TabItem tab3 = new TabItem( _tabFolder, SWT.BORDER );
        tab3.setText( Messages.SimulatorConfigurationTab_memoryTabLabel );
        tab3.setControl( memoryControl );

        // network tab
        _networkTab = new NetworkTab();
        Control networkControl = _networkTab.createControl( _tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
        networkControl.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        TabItem tab4 = new TabItem( _tabFolder, SWT.BORDER );
        tab4.setText( Messages.SimulatorConfigurationTab_networkTabLabel );
        tab4.setControl( networkControl );

        // ports tab
        _portsTab = new PortsTab();
        Control portsControl = _portsTab.createControl( _tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
        portsControl.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        TabItem tab5 = new TabItem( _tabFolder, SWT.BORDER );
        tab5.setText( Messages.SimulatorConfigurationTab_portsTabLabel );
        tab5.setControl( portsControl );

        // view tab
        _viewTab = new ViewTab();
        Control viewControl = _viewTab.createControl( _tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
        viewControl.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        TabItem tab6 = new TabItem( _tabFolder, SWT.BORDER );
        tab6.setText( Messages.SimulatorConfigurationTab_viewTabLabel );
        tab6.setControl( viewControl );

        // advanced tab
        _advancedTab = new AdvancedTab();
        Control advancedControl = _advancedTab.createControl( _tabFolder, SWT.V_SCROLL | SWT.H_SCROLL );
        advancedControl.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        final TabItem advancedTabItem = new TabItem( _tabFolder, SWT.BORDER );
        advancedTabItem.setText( Messages.SimulatorConfigurationTab_advancedTabLabel );
        advancedTabItem.setControl( advancedControl );

        _tabFolder.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                if( e.item == advancedTabItem ) {
                    LaunchParams param = new LaunchParams( _configuration );
                    _advancedTab.setDefaultCommandLine( param.getDefaultCommandLine() );
                    _advancedTab.setDefaultWorkingDirectory( param.getDefaultWorkingdir() );
                    _advancedTab.setDefaultMDSDirectory( param.getDefaultMDSPath() );
                }
            }
        } );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return Messages.SimulatorConfigurationTab_tabName;
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
     */
    public Image getImage() {
        ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons"
                + File.separator + "obj16" + File.separator + "fledge.gif" );
        final Image image = imageDescriptor.createImage();
        return image;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse .debug.core.ILaunchConfiguration)
     */
    public void initializeFrom( ILaunchConfiguration configuration ) {

        _configuration = configuration;

        if( _generalTab != null ) {
            _generalTab.initialize( configuration );
        }
        if( _debuggingTab != null ) {
            _debuggingTab.initialize( configuration );
        }
        if( _memoryTab != null ) {
            _memoryTab.initialize( configuration );
        }
        if( _networkTab != null ) {
            _networkTab.initialize( configuration );
        }
        if( _portsTab != null ) {
            _portsTab.initialize( configuration );
        }
        if( _viewTab != null ) {
            _viewTab.initialize( configuration );
        }
        if( _advancedTab != null ) {
            _advancedTab.initialize( configuration );
        }
    }

    /*
     * Note: this method is called whenever a modification is made to the run-time copy of the current launch configuration. It is
     * not just called when Apply is clicked. (non-Javadoc)
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse .debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply( ILaunchConfigurationWorkingCopy configuration ) {
        _generalTab.performApply( configuration );
        _debuggingTab.performApply( configuration );
        _memoryTab.performApply( configuration );
        _networkTab.performApply( configuration );
        _portsTab.performApply( configuration );
        _viewTab.performApply( configuration );
        _advancedTab.performApply( configuration );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse. debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults( ILaunchConfigurationWorkingCopy configuration ) {
        if( _generalTab != null ) {
            _generalTab.initialize( configuration );
        }
        if( _debuggingTab != null ) {
            _debuggingTab.initialize( configuration );
        }
        if( _memoryTab != null ) {
            _memoryTab.initialize( configuration );
        }
        if( _advancedTab != null ) {
            _advancedTab.initialize( configuration );
        }
    }

    public void propertyChange( PropertyChangeEvent evt ) {
        if( !_initializingForm ) {
            _generalTab.propertyChanged();
            _memoryTab.propertyChanged();
            updateLaunchConfigurationDialog();
        }
    }

    @Override
    public boolean canSave() {
        return isValid( _configuration );
    }

    @Override
    public boolean isValid( ILaunchConfiguration configuration ) {
        try {
            // time to wait before automatic response is selected
            boolean useDefaultValue = configuration.getAttribute(
                    IFledgeLaunchConstants.ATTR_GENERAL_AUTOMATICALLY_USE_DEFAULT_VALUE, false );
            if( useDefaultValue ) {
                String timeToWait = _configuration.getAttribute(
                        IFledgeLaunchConstants.ATTR_GENERAL_NUMBER_OF_SECONDS_WAIT_BEFORE_RESPONSE, StringUtils.EMPTY );
                if( !ValidationUtils.isPostiveInteger( timeToWait ) ) {
                    setErrorMessage( NLS.bind( Messages.SimulatorConfigurationTab_invalidValue,
                            Messages.SimulatorConfigurationTab_generalTabLabel,
                            Messages.SimulatorConfigurationTab_General_timeToWait ) );
                    return false;
                }
            }
            // heap size
            String heapSize = configuration.getAttribute( IFledgeLaunchConstants.ATTR_MEMORY_APPLICATION_HEAP_SIZE,
                    StringUtils.EMPTY );
            if( heapSize.length() > 0 && !ValidationUtils.isPostiveInteger( heapSize ) ) {
                setErrorMessage( NLS.bind( Messages.SimulatorConfigurationTab_invalidValue,
                        Messages.SimulatorConfigurationTab_memoryTabLabel, Messages.SimulatorConfigurationTab_Memory_HeapSize ) );
                return false;
            }
            // file system size
            String fileSystemSize = configuration.getAttribute( IFledgeLaunchConstants.ATTR_MEMORY_FILE_SYSTEM_SIZE,
                    StringUtils.EMPTY );
            if( fileSystemSize.length() > 0 && !ValidationUtils.isPostiveInteger( fileSystemSize ) ) {
                setErrorMessage( NLS.bind( Messages.SimulatorConfigurationTab_invalidValue,
                        Messages.SimulatorConfigurationTab_memoryTabLabel,
                        Messages.SimulatorConfigurationTab_Memory_fileSystemSize ) );
                return false;
            }
            // SD card size
            String sdCardSize = configuration.getAttribute( IFledgeLaunchConstants.ATTR_MEMORY_SDCARD_SIZE, StringUtils.EMPTY );
            if( sdCardSize.length() > 0 && !ValidationUtils.isPostiveInteger( sdCardSize ) ) {
                setErrorMessage( NLS.bind( Messages.SimulatorConfigurationTab_invalidValue,
                        Messages.SimulatorConfigurationTab_memoryTabLabel, Messages.SimulatorConfigurationTab_Memory_sdSize ) );
                return false;
            }
            // SMS source port
            String smsSrcPort = configuration.getAttribute( IFledgeLaunchConstants.ATTR_NETWORK_SMS_SOURCE_PORT,
                    StringUtils.EMPTY );
            if( smsSrcPort.length() > 0 && !ValidationUtils.isPostiveInteger( smsSrcPort ) ) {
                setErrorMessage( NLS.bind( Messages.SimulatorConfigurationTab_invalidValue,
                        Messages.SimulatorConfigurationTab_networkTabLabel, Messages.SimulatorConfigurationTab_Network_smsSource ) );
                return false;
            }
            // SMS destination port
            String smsDstPort = configuration.getAttribute( IFledgeLaunchConstants.ATTR_NETWORK_SMS_DESTINATION_PORT,
                    StringUtils.EMPTY );
            if( smsDstPort.length() > 0 && !ValidationUtils.isPostiveInteger( smsDstPort ) ) {
                setErrorMessage( NLS.bind( Messages.SimulatorConfigurationTab_invalidValue,
                        Messages.SimulatorConfigurationTab_networkTabLabel,
                        Messages.SimulatorConfigurationTab_Network_smsDestination ) );
                return false;
            }
            // PDE port
            String pdePort = configuration.getAttribute( IFledgeLaunchConstants.ATTR_NETWORK_PDE_PORT, StringUtils.EMPTY );
            if( pdePort.length() > 0 && !ValidationUtils.isPostiveInteger( pdePort ) ) {
                setErrorMessage( NLS.bind( Messages.SimulatorConfigurationTab_invalidValue,
                        Messages.SimulatorConfigurationTab_networkTabLabel, Messages.SimulatorConfigurationTab_Network_pde ) );
                return false;
            }
            // Blue tooth test board port
            String blueToothPort = configuration.getAttribute( IFledgeLaunchConstants.ATTR_PORTS_BLUETOOTH_PORT,
                    StringUtils.EMPTY );
            if( blueToothPort.length() > 0 && !ValidationUtils.isPostiveInteger( blueToothPort ) ) {
                setErrorMessage( NLS.bind( Messages.SimulatorConfigurationTab_invalidValue,
                        Messages.SimulatorConfigurationTab_portsTabLabel, Messages.SimulatorConfigurationTab_Port_bluetoothPort ) );
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

    private class GeneralTab {

        private Button _launchMDSCSCheckbox;
        private Text _launchAppOnStartupText;
        private ComboViewer _deviceCombo;
        private Button _autoDefaultCheckbox;
        private Text _timeToWaitText;
        private Text _pinText;
        private Text _esnText;
        private Text _meidText;
        private Button _enableSecurityCheckbox;
        private Text _systemLocaleText;
        private Text _keypadLocaleText;
        private IVMInstall _currentVM;

        public void initialize( ILaunchConfiguration config ) {
            try {
                _launchMDSCSCheckbox.setSelection( config.getAttribute( ATTR_GENERAL_LAUNCH_MDSCS, false ) );
                _launchAppOnStartupText.setText( config.getAttribute( ATTR_GENERAL_LAUNCH_APP_ON_STARTUP, "" ) );
                _autoDefaultCheckbox.setSelection( config.getAttribute( ATTR_GENERAL_AUTOMATICALLY_USE_DEFAULT_VALUE, false ) );
                _timeToWaitText.setText( config.getAttribute( ATTR_GENERAL_NUMBER_OF_SECONDS_WAIT_BEFORE_RESPONSE, "0" ) );
                _timeToWaitText.setEnabled( _autoDefaultCheckbox.getSelection() );
                _pinText.setText( config.getAttribute( ATTR_GENERAL_PIN, IFledgeLaunchConstants.DEFAULT_PIN_NUMBER ) );
                _esnText.setText( config.getAttribute( ATTR_GENERAL_ESN, StringUtils.EMPTY ) );
                _meidText.setText( config.getAttribute( ATTR_GENERAL_MEID, StringUtils.EMPTY ) );
                _enableSecurityCheckbox.setSelection( config.getAttribute( ATTR_GENERAL_ENABLE_DEVICE_SECURITY, false ) );
                _systemLocaleText.setText( config.getAttribute( ATTR_GENERAL_SYSTEM_LOCALE, StringUtils.EMPTY ) );
                _keypadLocaleText.setText( config.getAttribute( ATTR_GENERAL_KEYBOARD_LOCALE, StringUtils.EMPTY ) );

                IVMInstall newVM = LaunchUtils.getVMFromConfiguration( config );
                if( newVM != null ) {
                    List< DeviceInfo > devices = LaunchUtils.getDevicesInfo( newVM );
                    _deviceCombo.setInput( devices );
                    if( devices.size() > 0 ) {
                        if( _currentVM == null || newVM == _currentVM ) {
                            String simDir = config.getAttribute( ATTR_GENERAL_SIM_DIR, StringUtils.EMPTY );
                            String bundle = config.getAttribute( ATTR_GENERAL_BUNDLE, StringUtils.EMPTY );
                            String device = config.getAttribute( ATTR_GENERAL_DEVICE, StringUtils.EMPTY );
                            String configFileName = config.getAttribute( ATTR_GENERAL_CONFIG_FILE, StringUtils.EMPTY );
                            DeviceInfo di = new DeviceInfo( bundle, device, simDir, configFileName );
                            if( devices.contains( di ) ) {
                                _deviceCombo.setSelection( new StructuredSelection( di ) );
                            } else {
                                _deviceCombo.setSelection( new StructuredSelection( LaunchUtils.getDefaultDeviceInfo( newVM ) ) );
                            }
                        } else {
                            _deviceCombo.setSelection( new StructuredSelection( LaunchUtils.getDefaultDeviceInfo( newVM ) ) );
                        }
                    }
                }
                _currentVM = newVM;

            } catch( CoreException e ) {
                _logger.error( "", e );
            }
        }

        /**
         * Creates the control.
         *
         * @param parent
         *            The parent composite
         * @param stule
         *            The style
         */
        public Control createControl( Composite parent, int style ) {

            ScrolledComposite mainComposite = new ScrolledComposite( parent, style );
            mainComposite.setExpandVertical( true );
            mainComposite.setExpandHorizontal( true );
            Composite scrollable = CompositeFactory.gridComposite( mainComposite, 1, 5, 5 );

            // launch MDS-CS checkbox
            _launchMDSCSCheckbox = createCheckbox( scrollable, SWT.CHECK, Messages.SimulatorConfigurationTab_General_launchMDSCS,
                    null, SimulatorConfigurationTabBase.this );

            // launch app or URL on startup
            _launchAppOnStartupText = createText( scrollable, Messages.SimulatorConfigurationTab_General_launchApp, null,
                    SimulatorConfigurationTabBase.this );

            // device
            final Label label = new Label( scrollable, SWT.NONE | SWT.WRAP );
            label.setText( Messages.SimulatorConfigurationTab_General_device );
            _deviceCombo = new ComboViewer( scrollable );
            GridData gridData = new GridData( GridData.FILL_HORIZONTAL );
            _deviceCombo.getCombo().setLayoutData( gridData );
            _deviceCombo.setContentProvider( new DeviceComboContentProvider() );
            _deviceCombo.setLabelProvider( new DeviceComboLabelProvider() );
            _deviceCombo.addSelectionChangedListener( new ISelectionChangedListener() {
                public void selectionChanged( SelectionChangedEvent event ) {
                    if( !_initializingForm ) {
                        _generalTab.propertyChanged();
                        _memoryTab.propertyChanged();
                        updateLaunchConfigurationDialog();
                    }
                }
            } );

            // automatically use default value for all prompts
            _autoDefaultCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_General_autoUseDefaultValue, null, SimulatorConfigurationTabBase.this );

            // Specify the number of seconds to wait before the automated response is selected.
            _timeToWaitText = createText( scrollable, Messages.SimulatorConfigurationTab_General_timeToWait, null,
                    SimulatorConfigurationTabBase.this );

            // PIN
            _pinText = createText( scrollable, Messages.SimulatorConfigurationTab_General_pin, null,
                    SimulatorConfigurationTabBase.this );

            // ESN
            _esnText = createText( scrollable, Messages.SimulatorConfigurationTab_General_esn, null,
                    SimulatorConfigurationTabBase.this );

            // MEID
            _meidText = createText( scrollable, Messages.SimulatorConfigurationTab_General_meid,
                    Messages.SimulatorConfigurationTab_General_meidTooltip, SimulatorConfigurationTabBase.this );

            // Enable device security
            _enableSecurityCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_General_enableDeviceSecurity,
                    Messages.SimulatorConfigurationTab_General_enableDeviceSecurityTooltip, SimulatorConfigurationTabBase.this );

            // System locale
            _systemLocaleText = createText( scrollable, Messages.SimulatorConfigurationTab_General_systemLocale,
                    Messages.SimulatorConfigurationTab_General_systemLocaleTooltip, SimulatorConfigurationTabBase.this );

            // Keypad locale
            _keypadLocaleText = createText( scrollable, Messages.SimulatorConfigurationTab_General_keyboardLocale,
                    Messages.SimulatorConfigurationTab_General_keyboardLocaleTooltip, SimulatorConfigurationTabBase.this );

            mainComposite.setContent( scrollable );
            mainComposite.setMinSize( scrollable.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );

            return mainComposite;
        }

        public void performApply( ILaunchConfigurationWorkingCopy configuration ) {
            IVMInstall newVM = LaunchUtils.getVMFromConfiguration( configuration );
            if( newVM == _currentVM ) {
                final IStructuredSelection selection = (IStructuredSelection) _deviceCombo.getSelection();
                DeviceInfo di = (DeviceInfo) selection.getFirstElement();
                if( di != null ) {
                    configuration.setAttribute( ATTR_GENERAL_SIM_DIR, di.getDirectory() );
                    configuration.setAttribute( ATTR_GENERAL_BUNDLE, di.getBundleName() );
                    configuration.setAttribute( ATTR_GENERAL_DEVICE, di.getDeviceName() );
                    configuration.setAttribute( ATTR_GENERAL_CONFIG_FILE, di.getConfigName() );
                }
            } else {
                configuration.setAttribute( ATTR_GENERAL_BUNDLE, IFledgeLaunchConstants.DEFAULT_SIMULATOR_BUNDLE_NAME );
                String defaultDeviceName = LaunchUtils.getDefaultDeviceInfo( newVM ).getDeviceName();
                configuration.setAttribute( ATTR_GENERAL_DEVICE, defaultDeviceName );
                configuration.setAttribute( ATTR_GENERAL_CONFIG_FILE, defaultDeviceName );
                configuration.setAttribute( ATTR_GENERAL_SIM_DIR, StringUtils.EMPTY );
                _currentVM = newVM;
            }
            configuration.setAttribute( ATTR_GENERAL_LAUNCH_MDSCS, _launchMDSCSCheckbox.getSelection() );
            configuration.setAttribute( ATTR_GENERAL_LAUNCH_APP_ON_STARTUP, _launchAppOnStartupText.getText() );
            configuration.setAttribute( ATTR_GENERAL_AUTOMATICALLY_USE_DEFAULT_VALUE, _autoDefaultCheckbox.getSelection() );
            configuration.setAttribute( ATTR_GENERAL_NUMBER_OF_SECONDS_WAIT_BEFORE_RESPONSE, _timeToWaitText.getText() );
            configuration.setAttribute( ATTR_GENERAL_PIN, _pinText.getText() );
            configuration.setAttribute( ATTR_GENERAL_ESN, _esnText.getText() );
            configuration.setAttribute( ATTR_GENERAL_MEID, _meidText.getText() );
            configuration.setAttribute( ATTR_GENERAL_ENABLE_DEVICE_SECURITY, _enableSecurityCheckbox.getSelection() );
            configuration.setAttribute( ATTR_GENERAL_SYSTEM_LOCALE, _systemLocaleText.getText() );
            configuration.setAttribute( ATTR_GENERAL_KEYBOARD_LOCALE, _keypadLocaleText.getText() );
        }

        /**
         * Callback metod indicating some properties are changed
         */
        public void propertyChanged() {
            _timeToWaitText.setEnabled( _autoDefaultCheckbox.getSelection() );
        }
    }

    private class DebuggingTab {

        private Button _interruptDebuggerCheckbox;
        private Button _notStopExecutionCheckbox;

        /**
         * Creates the control.
         *
         * @param parent
         *            The parent composite
         * @param stule
         *            The style
         */
        public Control createControl( Composite parent, int style ) {

            ScrolledComposite mainComposite = new ScrolledComposite( parent, style );
            mainComposite.setExpandVertical( true );
            mainComposite.setExpandHorizontal( true );
            Composite scrollable = CompositeFactory.gridComposite( mainComposite, 1, 5, 5 );

            _interruptDebuggerCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Debugging_interrupt, null, SimulatorConfigurationTabBase.this );

            _notStopExecutionCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Debugging_nonStopExecution, null, SimulatorConfigurationTabBase.this );

            mainComposite.setContent( scrollable );
            mainComposite.setMinSize( scrollable.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );
            return mainComposite;
        }

        public void initialize( ILaunchConfiguration config ) {
            try {
                _interruptDebuggerCheckbox.setSelection( config.getAttribute( ATTR_DEBUG_INTERRUPT_DEBUGGER_ON_DEADLOCK, false ) );
                _notStopExecutionCheckbox.setSelection( config.getAttribute( ATTR_DEBUG_DO_NOT_STOP_EXECUTION, false ) );
            } catch( CoreException e ) {
                _logger.error( "", e );
            }
        }

        public void performApply( ILaunchConfigurationWorkingCopy configuration ) {
            configuration.setAttribute( ATTR_DEBUG_INTERRUPT_DEBUGGER_ON_DEADLOCK, _interruptDebuggerCheckbox.getSelection() );
            configuration.setAttribute( ATTR_DEBUG_DO_NOT_STOP_EXECUTION, _notStopExecutionCheckbox.getSelection() );
        }
    }

    private class MemoryTab {

        private Text _heapSizeText;
        private Text _brandingDataText;
        private Button _resetFileSystemCheckbox;
        private Button _resetNVRAMCheckbox;
        private Text _fileSystemSizeText;
        private Button _notSaveFlashCheckbox;
        private Button _notCompactFSCheckbox;
        private Button _simSDInsertCheckbox;
        private Button _destroyExistingSDImageCheckbox;
        private Text _sdCardSizeText;
        private Text _sdCardImageText;
        private Button _sdCardImageButton;
        private Button _usePCForSDCardCheckbox;
        private Text _pcFileSystemPathText;
        private Button _pcFileSystemPathButton;

        /**
         * Creates the control.
         *
         * @param parent
         *            The parent composite
         * @param stule
         *            The style
         */
        public Control createControl( Composite parent, int style ) {

            ScrolledComposite mainComposite = new ScrolledComposite( parent, style );
            mainComposite.setExpandVertical( true );
            mainComposite.setExpandHorizontal( true );
            Composite scrollable = CompositeFactory.gridComposite( mainComposite, 1, 5, 5 );

            // application heap size
            _heapSizeText = createText( scrollable, Messages.SimulatorConfigurationTab_Memory_HeapSize, null,
                    SimulatorConfigurationTabBase.this );

            // branding data
            TextButtonWrapper wrapper = createTextBrowse( scrollable, Messages.SimulatorConfigurationTab_Memory_brandingData,
                    null, SimulatorConfigurationTabBase.this, BROWSE_DIALOG_TYPE_OPEN_FILE );
            _brandingDataText = wrapper._text;

            // reset file system on startup
            _resetFileSystemCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Memory_resetFileSystem,
                    Messages.SimulatorConfigurationTab_Memory_resetFileSystemTooltip, SimulatorConfigurationTabBase.this );

            // reset NVRAM on startup
            _resetNVRAMCheckbox = createCheckbox( scrollable, SWT.CHECK, Messages.SimulatorConfigurationTab_Memory_resetNVRam,
                    Messages.SimulatorConfigurationTab_Memory_resetNVRamTooltip, SimulatorConfigurationTabBase.this );

            // filesystem size
            _fileSystemSizeText = createText( scrollable, Messages.SimulatorConfigurationTab_Memory_fileSystemSize,
                    Messages.SimulatorConfigurationTab_Memory_fileSystemSizeTooltip, SimulatorConfigurationTabBase.this );

            // Do not save flash data on simulator exit
            _notSaveFlashCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Memory_notSaveFlash, null, SimulatorConfigurationTabBase.this );

            // Do not compact filesystem on exit
            _notCompactFSCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Memory_notCompactFS, null, SimulatorConfigurationTabBase.this );

            // Destroy existing SD Card image
            _destroyExistingSDImageCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Memory_destroyExistingSD,
                    Messages.SimulatorConfigurationTab_Memory_destroyExistingSDTooltip, SimulatorConfigurationTabBase.this );

            // Simulate SD Card inserted
            _simSDInsertCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Memory_simulate_SD_Inserted, null, SimulatorConfigurationTabBase.this );
            _simSDInsertCheckbox.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected( SelectionEvent e ) {
                    if( _simSDInsertCheckbox.getSelection() ) {
                        _usePCForSDCardCheckbox.setSelection( false );
                        _pcFileSystemPathText.setEnabled( false );
                        _pcFileSystemPathButton.setEnabled( false );
                    }
                }
            } );

            // SD Card image
            wrapper = createTextBrowse( scrollable, Messages.SimulatorConfigurationTab_Memory_sdImage, null,
                    SimulatorConfigurationTabBase.this, BROWSE_DIALOG_TYPE_OPEN_FILE );
            _sdCardImageText = wrapper._text;
            _sdCardImageButton = wrapper._button;

            // SD Card size
            _sdCardSizeText = createText( scrollable, Messages.SimulatorConfigurationTab_Memory_sdSize,
                    Messages.SimulatorConfigurationTab_Memory_sdSizeTooltip, SimulatorConfigurationTabBase.this );

            // use PC filesystem for SD Card files
            _usePCForSDCardCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Memory_usePCForSD, null, SimulatorConfigurationTabBase.this );
            _usePCForSDCardCheckbox.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected( SelectionEvent e ) {
                    if( _usePCForSDCardCheckbox.getSelection() ) {
                        _simSDInsertCheckbox.setSelection( false );
                        _sdCardSizeText.setEnabled( false );
                        _sdCardImageText.setEnabled( false );
                        _sdCardImageButton.setEnabled( false );
                    }
                }
            } );

            // PC filesystem path
            wrapper = createTextBrowse( scrollable, Messages.SimulatorConfigurationTab_Memory_pcFileSystem,
                    Messages.SimulatorConfigurationTab_Memory_pcFileSystemTooltip, SimulatorConfigurationTabBase.this,
                    BROWSE_DIALOG_TYPE_SELECT_DIR );
            _pcFileSystemPathText = wrapper._text;
            _pcFileSystemPathButton = wrapper._button;

            mainComposite.setContent( scrollable );
            mainComposite.setMinSize( scrollable.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );
            return mainComposite;
        }

        public void initialize( ILaunchConfiguration config ) {
            try {
                _heapSizeText.setText( config.getAttribute( ATTR_MEMORY_APPLICATION_HEAP_SIZE, IConstants.EMPTY_STRING ) );
                _brandingDataText.setText( config.getAttribute( ATTR_MEMORY_BRANDING_DATA, IConstants.EMPTY_STRING ) );
                _resetFileSystemCheckbox.setSelection( config.getAttribute( ATTR_MEMORY_RESET_FILE_SYSTEM_ON_STARTUP, false ) );
                _resetNVRAMCheckbox.setSelection( config.getAttribute( ATTR_MEMORY_RESET_NVRAM_ON_STARTUP, false ) );
                _fileSystemSizeText.setText( config.getAttribute( ATTR_MEMORY_FILE_SYSTEM_SIZE, IConstants.EMPTY_STRING ) );
                _notSaveFlashCheckbox.setSelection( config.getAttribute( ATTR_MEMORY_NOT_SAVE_FLASH_ON_EXIT, false ) );
                _notCompactFSCheckbox.setSelection( config.getAttribute( ATTR_MEMORY_NOT_COMPACT_FILE_SYSTEM_ON_EXIT, false ) );
                _destroyExistingSDImageCheckbox.setSelection( config.getAttribute( ATTR_MEMORY_DESTROY_EXISTING_SDCARD_IMAGE,
                        false ) );
                _simSDInsertCheckbox.setSelection( config.getAttribute( ATTR_MEMORY_SIMULATE_SDCARD_INSERTED, false ) );
                _sdCardSizeText.setText( config.getAttribute( ATTR_MEMORY_SDCARD_SIZE, IConstants.EMPTY_STRING ) );
                _sdCardSizeText.setEnabled( _simSDInsertCheckbox.getSelection() );
                _sdCardImageText.setText( config.getAttribute( ATTR_MEMORY_SDCARD_IMAGE, IConstants.EMPTY_STRING ) );
                _sdCardImageText.setEnabled( _simSDInsertCheckbox.getSelection() );
                _sdCardImageButton.setEnabled( _simSDInsertCheckbox.getSelection() );
                _usePCForSDCardCheckbox
                        .setSelection( config.getAttribute( ATTR_MEMORY_USE_PC_FILESYSTEM_FOR_SDCARD_FILES, false ) );
                _pcFileSystemPathText.setText( config.getAttribute( ATTR_MEMORY_PC_FILESYSTEM_PATH, IConstants.EMPTY_STRING ) );
                _pcFileSystemPathText.setEnabled( _usePCForSDCardCheckbox.getSelection() );
                _pcFileSystemPathButton.setEnabled( _usePCForSDCardCheckbox.getSelection() );
            } catch( CoreException e ) {
                _logger.error( e );
            }
        }

        public void performApply( ILaunchConfigurationWorkingCopy configuration ) {
            configuration.setAttribute( ATTR_MEMORY_APPLICATION_HEAP_SIZE, _heapSizeText.getText() );
            configuration.setAttribute( ATTR_MEMORY_BRANDING_DATA, _brandingDataText.getText() );
            configuration.setAttribute( ATTR_MEMORY_RESET_FILE_SYSTEM_ON_STARTUP, _resetFileSystemCheckbox.getSelection() );
            configuration.setAttribute( ATTR_MEMORY_RESET_NVRAM_ON_STARTUP, _resetNVRAMCheckbox.getSelection() );
            configuration.setAttribute( ATTR_MEMORY_FILE_SYSTEM_SIZE, _fileSystemSizeText.getText() );
            configuration.setAttribute( ATTR_MEMORY_NOT_SAVE_FLASH_ON_EXIT, _notSaveFlashCheckbox.getSelection() );
            configuration.setAttribute( ATTR_MEMORY_NOT_COMPACT_FILE_SYSTEM_ON_EXIT, _notCompactFSCheckbox.getSelection() );
            configuration.setAttribute( ATTR_MEMORY_SIMULATE_SDCARD_INSERTED, _simSDInsertCheckbox.getSelection() );
            configuration
                    .setAttribute( ATTR_MEMORY_DESTROY_EXISTING_SDCARD_IMAGE, _destroyExistingSDImageCheckbox.getSelection() );
            configuration.setAttribute( ATTR_MEMORY_SDCARD_SIZE, _sdCardSizeText.getText() );
            configuration.setAttribute( ATTR_MEMORY_SDCARD_IMAGE, _sdCardImageText.getText() );
            configuration.setAttribute( ATTR_MEMORY_USE_PC_FILESYSTEM_FOR_SDCARD_FILES, _usePCForSDCardCheckbox.getSelection() );
            configuration.setAttribute( ATTR_MEMORY_PC_FILESYSTEM_PATH, _pcFileSystemPathText.getText().trim() );
        }

        /**
         * Callback metod indicating some properties are changed
         */
        public void propertyChanged() {
            _pcFileSystemPathText.setEnabled( _usePCForSDCardCheckbox.getSelection() );
            _pcFileSystemPathButton.setEnabled( _usePCForSDCardCheckbox.getSelection() );
            _sdCardSizeText.setEnabled( _simSDInsertCheckbox.getSelection() );
            _sdCardImageText.setEnabled( _simSDInsertCheckbox.getSelection() );
            _sdCardImageButton.setEnabled( _simSDInsertCheckbox.getSelection() );
        }
    }

    private class NetworkTab {

        private Button _disableRegistrationCheckbox;
        private org.eclipse.swt.widgets.List _networkList;
        private Button _startWithRadioOffCheckbox;
        private org.eclipse.swt.widgets.List _phoneNumberList;
        private Button _autoAnswerOutgoingCallCheckbox;
        private Text _imeiText;
        private Text _iccidText;
        private Text _imsiText;
        private Button _simSIMNotPresentCheckbox;
        private Text _ipAddressText;
        private Button _ignoreUDPPortConflictCheckbox;
        private Text _smsSrcPortText;
        private Text _smsDestPortText;
        private Text _pdePortText;

        /**
         * Creates the control.
         *
         * @param parent
         *            The parent composite
         * @param stule
         *            The style
         */
        public Control createControl( Composite parent, int style ) {

            ScrolledComposite mainComposite = new ScrolledComposite( parent, style );
            mainComposite.setExpandVertical( true );
            mainComposite.setExpandHorizontal( true );
            Composite scrollable = CompositeFactory.gridComposite( mainComposite, 1, 5, 5 );

            // disable registration
            _disableRegistrationCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Network_disableRegistration, null, SimulatorConfigurationTabBase.this );

            // network
            _networkList = createList( scrollable, Messages.SimulatorConfigurationTab_Network_networks,
                    Messages.SimulatorConfigurationTab_Network_networksTooltip );

            // start with radio off
            _startWithRadioOffCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Network_startWithRadioOff, null, SimulatorConfigurationTabBase.this );

            // phone number
            _phoneNumberList = createList( scrollable, Messages.SimulatorConfigurationTab_Network_phoneNumbers, "" );

            // automatically answer outgoing calls
            _autoAnswerOutgoingCallCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Network_autoAnswerOutgoingCall, null, SimulatorConfigurationTabBase.this );

            // IMEI
            _imeiText = createText( scrollable, Messages.SimulatorConfigurationTab_Network_imei, null,
                    SimulatorConfigurationTabBase.this );

            // ICCID (GPRS)
            _iccidText = createText( scrollable, Messages.SimulatorConfigurationTab_Network_iccid, null,
                    SimulatorConfigurationTabBase.this );

            // IMSI (GPRS)
            _imsiText = createText( scrollable, Messages.SimulatorConfigurationTab_Network_imsi, null,
                    SimulatorConfigurationTabBase.this );

            // simulate SIM not present
            _simSIMNotPresentCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Network_simulateSIMNotPresent, null, SimulatorConfigurationTabBase.this );

            // IP address
            _ipAddressText = createText( scrollable, Messages.SimulatorConfigurationTab_Network_ipAddress,
                    Messages.SimulatorConfigurationTab_Network_ipAddressTooltip, SimulatorConfigurationTabBase.this );

            // ignore UDP port conflicts
            _ignoreUDPPortConflictCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Network_ignoreUDPConflict,
                    Messages.SimulatorConfigurationTab_Network_ignoreUDPConflictTooltip, SimulatorConfigurationTabBase.this );

            // SMS source port
            _smsSrcPortText = createText( scrollable, Messages.SimulatorConfigurationTab_Network_smsSource,
                    Messages.SimulatorConfigurationTab_Network_smsSourceTooltip, SimulatorConfigurationTabBase.this );

            // SMS destination port
            _smsDestPortText = createText( scrollable, Messages.SimulatorConfigurationTab_Network_smsDestination,
                    Messages.SimulatorConfigurationTab_Network_smsDestinationTooltip, SimulatorConfigurationTabBase.this );

            // PDE port
            _pdePortText = createText( scrollable, Messages.SimulatorConfigurationTab_Network_pde,
                    Messages.SimulatorConfigurationTab_Network_pdeTooltip, SimulatorConfigurationTabBase.this );

            mainComposite.setContent( scrollable );
            mainComposite.setMinSize( scrollable.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );
            return mainComposite;
        }

        @SuppressWarnings("unchecked")
        public void initialize( ILaunchConfiguration config ) {
            try {
                _disableRegistrationCheckbox.setSelection( config.getAttribute( ATTR_NETWORK_DISABLE_REGISTRATION, true ) );
                List< String > networks = config.getAttribute( ATTR_NETWORK_NETWORKS, Collections.EMPTY_LIST );
                _networkList.setItems( networks.toArray( new String[ 0 ] ) );
                _startWithRadioOffCheckbox.setSelection( config.getAttribute( ATTR_NETWORK_START_WITH_RADIO_OFF, false ) );
                List< String > phones = config.getAttribute( ATTR_NETWORK_PHONE_NUMBERS, Collections.EMPTY_LIST );
                _phoneNumberList.setItems( phones.toArray( new String[ 0 ] ) );
                _autoAnswerOutgoingCallCheckbox
                        .setSelection( config.getAttribute( ATTR_NETWORK_AUTO_ANSWER_OUTGOING_CALL, false ) );
                _imeiText.setText( config.getAttribute( ATTR_NETWORK_IMEI, "" ) );
                _iccidText.setText( config.getAttribute( ATTR_NETWORK_ICCID, "" ) );
                _imsiText.setText( config.getAttribute( ATTR_NETWORK_IMSI, "" ) );
                _simSIMNotPresentCheckbox.setSelection( config.getAttribute( ATTR_NETWORK_SIMULATE_SIM_NOT_PRESENT, false ) );
                _ipAddressText.setText( config.getAttribute( ATTR_NETWORK_IP_ADDRESS, "" ) );
                _ignoreUDPPortConflictCheckbox.setSelection( config.getAttribute( ATTR_NETWORK_IGNORE_UDP_PORT_CONFLICT, false ) );
                _smsSrcPortText.setText( config.getAttribute( ATTR_NETWORK_SMS_SOURCE_PORT, "" ) );
                _smsDestPortText.setText( config.getAttribute( ATTR_NETWORK_SMS_DESTINATION_PORT, "" ) );
                _pdePortText.setText( config.getAttribute( ATTR_NETWORK_PDE_PORT, "" ) );

            } catch( CoreException e ) {
                _logger.error( "", e );
            }
        }

        public void performApply( ILaunchConfigurationWorkingCopy configuration ) {
            configuration.setAttribute( ATTR_NETWORK_DISABLE_REGISTRATION, _disableRegistrationCheckbox.getSelection() );
            configuration.setAttribute( ATTR_NETWORK_NETWORKS, Arrays.asList( _networkList.getItems() ) );
            configuration.setAttribute( ATTR_NETWORK_START_WITH_RADIO_OFF, _startWithRadioOffCheckbox.getSelection() );
            configuration.setAttribute( ATTR_NETWORK_PHONE_NUMBERS, Arrays.asList( _phoneNumberList.getItems() ) );
            configuration.setAttribute( ATTR_NETWORK_AUTO_ANSWER_OUTGOING_CALL, _autoAnswerOutgoingCallCheckbox.getSelection() );
            configuration.setAttribute( ATTR_NETWORK_IMEI, _imeiText.getText() );
            configuration.setAttribute( ATTR_NETWORK_ICCID, _iccidText.getText() );
            configuration.setAttribute( ATTR_NETWORK_IMSI, _imsiText.getText() );
            configuration.setAttribute( ATTR_NETWORK_SIMULATE_SIM_NOT_PRESENT, _simSIMNotPresentCheckbox.getSelection() );
            configuration.setAttribute( ATTR_NETWORK_IP_ADDRESS, _ipAddressText.getText() );
            configuration.setAttribute( ATTR_NETWORK_IGNORE_UDP_PORT_CONFLICT, _ignoreUDPPortConflictCheckbox.getSelection() );
            configuration.setAttribute( ATTR_NETWORK_SMS_SOURCE_PORT, _smsSrcPortText.getText() );
            configuration.setAttribute( ATTR_NETWORK_SMS_DESTINATION_PORT, _smsDestPortText.getText() );
            configuration.setAttribute( ATTR_NETWORK_PDE_PORT, _pdePortText.getText() );
        }

    }

    private class PortsTab {

        private Button _usbCableConnectedCheckbox;
        private Text _bluetoothPortText;

        /**
         * Creates the control.
         *
         * @param parent
         *            The parent composite
         * @param stule
         *            The style
         */
        public Control createControl( Composite parent, int style ) {

            ScrolledComposite mainComposite = new ScrolledComposite( parent, style );
            mainComposite.setExpandVertical( true );
            mainComposite.setExpandHorizontal( true );
            Composite scrollable = CompositeFactory.gridComposite( mainComposite, 1, 5, 5 );

            // USB cable connected
            _usbCableConnectedCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Port_usbConnected,
                    Messages.SimulatorConfigurationTab_Port_usbConnectedTooltip, SimulatorConfigurationTabBase.this );

            // Bluetooth test board port
            _bluetoothPortText = createText( scrollable, Messages.SimulatorConfigurationTab_Port_bluetoothPort,
                    Messages.SimulatorConfigurationTab_Port_bluetoothPortTooltip, SimulatorConfigurationTabBase.this );

            mainComposite.setContent( scrollable );
            mainComposite.setMinSize( scrollable.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );
            return mainComposite;
        }

        public void initialize( ILaunchConfiguration config ) {
            try {
                _usbCableConnectedCheckbox.setSelection( config.getAttribute( ATTR_PORTS_USB_CONNECTED, false ) );
                _bluetoothPortText.setText( config.getAttribute( ATTR_PORTS_BLUETOOTH_PORT, "" ) );
            } catch( CoreException e ) {
                _logger.error( "", e );
            }
        }

        public void performApply( ILaunchConfigurationWorkingCopy configuration ) {
            configuration.setAttribute( ATTR_PORTS_USB_CONNECTED, _usbCableConnectedCheckbox.getSelection() );
            configuration.setAttribute( ATTR_PORTS_BLUETOOTH_PORT, _bluetoothPortText.getText() );
        }
    }

    private class ViewTab {

        private Button _disableBacklightAutoOffCheckbox;
        private Button _hideNetworkInfoCheckbox;
        private Button _lcdOnlyCheckbox;
        private Combo _lcdZoomCombo;
        private Button _keyMappingCheckbox;

        /**
         * Creates the control.
         *
         * @param parent
         *            The parent composite
         * @param stule
         *            The style
         */
        public Control createControl( Composite parent, int style ) {

            ScrolledComposite mainComposite = new ScrolledComposite( parent, style );
            mainComposite.setExpandVertical( true );
            mainComposite.setExpandHorizontal( true );
            Composite scrollable = CompositeFactory.gridComposite( mainComposite, 1, 5, 5 );

            // disable automatic backlight shutoff
            _disableBacklightAutoOffCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_View_disableBacklightOff,
                    Messages.SimulatorConfigurationTab_View_disableBacklightOffTooltip, SimulatorConfigurationTabBase.this );

            // hide network-specific information
            _hideNetworkInfoCheckbox = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_View_hideNetworkInfo,
                    Messages.SimulatorConfigurationTab_View_hideNetworkInfoTooltip, SimulatorConfigurationTabBase.this );

            // display LCD only
            _lcdOnlyCheckbox = createCheckbox( scrollable, SWT.CHECK, Messages.SimulatorConfigurationTab_View_lcdOnly, null,
                    SimulatorConfigurationTabBase.this );

            // LCD zoom
            String choices[] = { "Default", "0.5", "1", "2", "3", "4", "" };
            _lcdZoomCombo = createCombo( scrollable, Messages.SimulatorConfigurationTab_View_lcdZoom,
                    Messages.SimulatorConfigurationTab_View_lcdZoomTooltip, SimulatorConfigurationTabBase.this );
            _lcdZoomCombo.setItems( choices );

            // do not show help for key mapping
            _keyMappingCheckbox = createCheckbox( scrollable, SWT.CHECK, Messages.SimulatorConfigurationTab_View_keyMapping,
                    Messages.SimulatorConfigurationTab_View_keyMappingTooltip, SimulatorConfigurationTabBase.this );

            mainComposite.setContent( scrollable );
            mainComposite.setMinSize( scrollable.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );
            return mainComposite;
        }

        public void initialize( ILaunchConfiguration config ) {
            try {
                _disableBacklightAutoOffCheckbox.setSelection( config.getAttribute( ATTR_VIEW_DISABLE_AUTO_BACKLIGHT_SHUTOFF,
                        false ) );
                _hideNetworkInfoCheckbox.setSelection( config.getAttribute( ATTR_VIEW_HIDE_NETWORK_INFORMATION, false ) );
                _lcdOnlyCheckbox.setSelection( config.getAttribute( ATTR_VIEW_DISPLAY_LCD_ONLY, false ) );
                _lcdZoomCombo.setText( config.getAttribute( ATTR_VIEW_LCD_ZOOM, "" ) );
                _keyMappingCheckbox.setSelection( config.getAttribute( ATTR_VIEW_NOT_SHOW_HELP_FOR_KEY_MAPPING, false ) );
            } catch( CoreException e ) {
                _logger.error( "", e );
            }
        }

        public void performApply( ILaunchConfigurationWorkingCopy configuration ) {
            configuration
                    .setAttribute( ATTR_VIEW_DISABLE_AUTO_BACKLIGHT_SHUTOFF, _disableBacklightAutoOffCheckbox.getSelection() );
            configuration.setAttribute( ATTR_VIEW_HIDE_NETWORK_INFORMATION, _hideNetworkInfoCheckbox.getSelection() );
            configuration.setAttribute( ATTR_VIEW_DISPLAY_LCD_ONLY, _lcdOnlyCheckbox.getSelection() );
            configuration.setAttribute( ATTR_VIEW_LCD_ZOOM, _lcdZoomCombo.getText() );
            configuration.setAttribute( ATTR_VIEW_NOT_SHOW_HELP_FOR_KEY_MAPPING, _keyMappingCheckbox.getSelection() );
        }
    }

    private class AdvancedTab {

        private Button _simBatteryButton;
        private Button _numPadButton;
        private Text _defaultCommandLineText;
        private Text _defaultWorkingDirectoryText;
        private Text _defaultMDSDirectoryText;
        private Button _customizedCommandCheckButton;
        private Text _customizedCommandLineText;
        private TextButtonWrapper _customizedWorkingDirectoryText;
        private TextButtonWrapper _customizedMDSDirectoryText;

        public void initialize( ILaunchConfiguration config ) {
            try {
                _simBatteryButton.setSelection( config.getAttribute( ATTR_ADVANCED_NOT_SIMULATE_RIM_BATTERY, false ) );
                _numPadButton.setSelection( config.getAttribute( ATTR_ADVANCED_NOT_USE_PC_NUMPAD_FOR_TRACKBALL, false ) );
                enableCustomizedOptions( config.getAttribute( ATTR_USE_CUSTOMIZED_COMMAND_LINE, false ) );
                enableDefaultOptions( !config.getAttribute( ATTR_USE_CUSTOMIZED_COMMAND_LINE, false ) );
                _customizedCommandLineText.setText( config.getAttribute( ATTR_CUSTOMIZED_COMMAND_LINE, StringUtils.EMPTY ) );
                _customizedWorkingDirectoryText._text.setText( config.getAttribute( ATTR_CUSTOMIZED_WORKING_DIRECTORY,
                        StringUtils.EMPTY ) );
                _customizedMDSDirectoryText._text
                        .setText( config.getAttribute( ATTR_CUSTOMIZED_MDS_DIRECTORY, StringUtils.EMPTY ) );
            } catch( CoreException e ) {
                _logger.error( "", e );
            }
        }

        private void enableCustomizedOptions( boolean enabled ) {
            _customizedCommandCheckButton.setSelection( enabled );
            _customizedCommandLineText.setEnabled( enabled );
            _customizedWorkingDirectoryText._text.setEnabled( enabled );
            _customizedWorkingDirectoryText._button.setEnabled( enabled );
            _customizedMDSDirectoryText._text.setEnabled( enabled );
            _customizedMDSDirectoryText._button.setEnabled( enabled );
        }

        private void enableDefaultOptions( boolean enabled ) {
            _defaultCommandLineText.setEnabled( enabled );
            _defaultWorkingDirectoryText.setEnabled( enabled );
            _defaultMDSDirectoryText.setEnabled( enabled );
        }

        /**
         * Creates the control.
         *
         * @param parent
         *            The parent composite
         * @param stule
         *            The style
         */
        public Control createControl( Composite parent, int style ) {
            ScrolledComposite mainComposite = new ScrolledComposite( parent, style );
            mainComposite.setExpandVertical( true );
            mainComposite.setExpandHorizontal( true );
            Composite scrollable = CompositeFactory.gridComposite( mainComposite, 1, 5, 5 );

            _simBatteryButton = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Advanced_simulateBattery, null, SimulatorConfigurationTabBase.this );
            _simBatteryButton.addSelectionListener( new SelectionAdapter() {
                @Override
                public void widgetSelected( SelectionEvent e ) {
                    updateCommandLine();
                }
            } );

            _numPadButton = createCheckbox( scrollable, SWT.CHECK, Messages.SimulatorConfigurationTab_Advanced_numPad,
                    Messages.SimulatorConfigurationTab_Advanced_numPadTooltip, SimulatorConfigurationTabBase.this );
            _numPadButton.addSelectionListener( new SelectionAdapter() {
                @Override
                public void widgetSelected( SelectionEvent e ) {
                    updateCommandLine();
                }
            } );

            // default command line
            _defaultCommandLineText = createText( scrollable, Messages.SimulatorConfigurationTab_Advanced_default_commandLine,
                    null, SimulatorConfigurationTabBase.this );
            _defaultCommandLineText.setEditable( false );
            _defaultWorkingDirectoryText = createText( scrollable,
                    Messages.SimulatorConfigurationTab_Advanced_default_workingdirectory, null,
                    SimulatorConfigurationTabBase.this );
            _defaultWorkingDirectoryText.setEditable( false );
            _defaultMDSDirectoryText = createText( scrollable, Messages.SimulatorConfigurationTab_Advanced_default_MDSdirectory,
                    null, SimulatorConfigurationTabBase.this );
            _defaultMDSDirectoryText.setEditable( false );
            // customized command line
            _customizedCommandCheckButton = createCheckbox( scrollable, SWT.CHECK,
                    Messages.SimulatorConfigurationTab_Advanced_customized_options, null, SimulatorConfigurationTabBase.this );
            _customizedCommandCheckButton.addSelectionListener( new SelectionAdapter() {
                @Override
                public void widgetSelected( SelectionEvent e ) {
                    if( e.getSource() != _customizedCommandCheckButton ) {
                        return;
                    }
                    enableCustomizedOptions( _customizedCommandCheckButton.getSelection() );
                    enableDefaultOptions( !_customizedCommandCheckButton.getSelection() );
                    if( _customizedCommandCheckButton.getSelection() ) {
                        // if the customized command or working directory are empty, set the default command and working directory
                        // as their default value
                        if( StringUtils.isEmpty( _customizedCommandLineText.getText() ) ) {
                            _customizedCommandLineText.setText( _defaultCommandLineText.getText() );
                        }
                        if( StringUtils.isEmpty( _customizedWorkingDirectoryText._text.getText() ) ) {
                            _customizedWorkingDirectoryText._text.setText( _defaultWorkingDirectoryText.getText() );
                        }
                        if( StringUtils.isEmpty( _customizedMDSDirectoryText._text.getText() ) ) {
                            _customizedMDSDirectoryText._text.setText( _defaultMDSDirectoryText.getText() );
                        }
                    }
                }
            } );
            _customizedCommandLineText = createText( scrollable,
                    Messages.SimulatorConfigurationTab_Advanced_customized_commandLine, null, SimulatorConfigurationTabBase.this );
            _customizedWorkingDirectoryText = createTextBrowse( scrollable,
                    Messages.SimulatorConfigurationTab_Advanced_customized_workingdirectory, null,
                    SimulatorConfigurationTabBase.this, BROWSE_DIALOG_TYPE_SELECT_DIR );
            _customizedMDSDirectoryText = createTextBrowse( scrollable,
                    Messages.SimulatorConfigurationTab_Advanced_customized_MDSdirectory, null,
                    SimulatorConfigurationTabBase.this, BROWSE_DIALOG_TYPE_SELECT_DIR );
            mainComposite.setContent( scrollable );
            mainComposite.setMinSize( scrollable.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );
            return mainComposite;
        }

        public void performApply( ILaunchConfigurationWorkingCopy configuration ) {
            configuration.setAttribute( ATTR_ADVANCED_NOT_SIMULATE_RIM_BATTERY, _simBatteryButton.getSelection() );
            configuration.setAttribute( ATTR_ADVANCED_NOT_USE_PC_NUMPAD_FOR_TRACKBALL, _numPadButton.getSelection() );
            configuration.setAttribute( ATTR_USE_CUSTOMIZED_COMMAND_LINE, _customizedCommandCheckButton.getSelection() );
            configuration.setAttribute( ATTR_CUSTOMIZED_COMMAND_LINE, _customizedCommandLineText.getText() );
            configuration.setAttribute( ATTR_CUSTOMIZED_WORKING_DIRECTORY, _customizedWorkingDirectoryText._text.getText() );
            configuration.setAttribute( ATTR_CUSTOMIZED_MDS_DIRECTORY, _customizedMDSDirectoryText._text.getText() );
        }

        public void setDefaultCommandLine( String commandLine ) {
            _defaultCommandLineText.setText( commandLine );
        }

        public void setDefaultWorkingDirectory( String workingDirectory ) {
            _defaultWorkingDirectoryText.setText( workingDirectory );
        }

        public void setDefaultMDSDirectory( String MDSDirectory ) {
            _defaultMDSDirectoryText.setText( MDSDirectory );
        }

        private void updateCommandLine() {
            LaunchParams param = new LaunchParams( _configuration );
            _advancedTab.setDefaultCommandLine( param.getDefaultCommandLine() );
        }
    }

    /**
     * Creates a text control.
     *
     * @param parent
     *            The parent composite
     * @param labelText
     *            The label
     * @param tooltip
     *            The tooltip
     * @param listener
     *            The property change listener
     * @return The text control
     */
    private static Text createText( final Composite parent, final String labelText, final String tooltip,
            final PropertyChangeListener listener ) {
        if( !StringUtils.isBlank( labelText ) ) {
            final Label label = new Label( parent, SWT.NONE | SWT.WRAP );
            label.setText( labelText );
        }
        Text text = new Text( parent, SWT.LEFT | SWT.SINGLE | SWT.BORDER | SWT.WRAP );
        GridData gridData = new GridData( SWT.FILL, SWT.CENTER, true, false );
        text.setLayoutData( gridData );
        if( tooltip != null ) {
            text.setToolTipText( tooltip );
        }
        text.addModifyListener( new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
                listener.propertyChange( null );
            }
        } );
        return text;
    }

    private static class TextButtonWrapper {
        Text _text;
        Button _button;

        public TextButtonWrapper( Text text, Button button ) {
            _text = text;
            _button = button;
        }
    }

    /**
     * Create a text control with a browse button besides.
     *
     * @param parent
     *            The parent composite
     * @param labelText
     *            The label
     * @param tooltip
     *            The tooltip
     * @param listener
     *            The property change listener
     * @param dialogType
     *            Browse for file or directory
     * @return The text and button wrapper
     */
    private static TextButtonWrapper createTextBrowse( final Composite parent, final String labelText, final String tooltip,
            final PropertyChangeListener listener, final int dialogType ) {
        final Label label = new Label( parent, SWT.NONE | SWT.WRAP );
        label.setText( labelText );
        Composite comp1 = CompositeFactory.gridComposite( parent, 2 );
        final Text text = new Text( comp1, SWT.LEFT | SWT.SINGLE | SWT.BORDER | SWT.WRAP );
        GridData gridData = new GridData( SWT.FILL, SWT.CENTER, true, false );
        text.setLayoutData( gridData );
        if( tooltip != null ) {
            text.setToolTipText( tooltip );
        }
        text.addModifyListener( new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
                listener.propertyChange( null );
            }
        } );
        final Button browseButton = new Button( comp1, SWT.NONE );
        browseButton.setText( Messages.IConstants_BROWSE_BUTTON_TITLE );
        gridData = new GridData();
        gridData.widthHint = 75;
        browseButton.setLayoutData( gridData );
        browseButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                File file;
                if( dialogType == BROWSE_DIALOG_TYPE_OPEN_FILE ) {
                    file = RimIDEUtil.openFile( browseButton.getShell(), Messages.SimulatorConfigurationTab_openFile, null,
                            SWT.OPEN, new String[] { Messages.SimulatorConfigurationTab_dmpFile } );
                } else {
                    file = RimIDEUtil.openDirecotryDialog( browseButton.getShell(), Messages.SimulatorTab_DIR_DIALOG_TITLE );
                }
                if( file == null ) {
                    return;
                }
                text.setText( file.getPath() );
            }
        } );
        return new TextButtonWrapper( text, browseButton );
    }

    /**
     * Creates a checkbox control.
     *
     * @param parent
     *            The parent composite
     * @param label
     *            The label
     * @param tooltip
     *            The tooltip
     * @param listener
     *            The property change listener
     * @return The checkbox control
     */
    protected static Button createCheckbox( final Composite parent, final int style, final String label, final String tooltip,
            final PropertyChangeListener listener ) {
        Button checkboxButton = new Button( parent, style );
        checkboxButton.setText( label );
        if( tooltip != null ) {
            checkboxButton.setToolTipText( tooltip );
        }
        checkboxButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                listener.propertyChange( null );
            }
        } );
        return checkboxButton;
    }

    /**
     * Creates a combo control.
     *
     * @param parent
     *            The parent composite
     * @param labelText
     *            The label
     * @param tooltip
     *            The tooltip
     * @param choices
     *            The choices
     * @param listener
     *            The property change listener
     * @return The combo control
     */
    private static Combo createCombo( final Composite parent, final String labelText, final String tooltip,
            final PropertyChangeListener listener ) {
        final Label label = new Label( parent, SWT.NONE | SWT.WRAP );
        label.setText( labelText );
        Combo combo = new Combo( parent, SWT.LEFT | SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY );
        GridData gridData = new GridData( SWT.FILL, SWT.FILL, true, false );
        combo.setLayoutData( gridData );
        if( tooltip != null ) {
            combo.setToolTipText( tooltip );
        }
        combo.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                listener.propertyChange( null );
            }
        } );
        return combo;
    }

    /**
     * Creates a list with add,delete,edit button underneath.
     *
     * @param parent
     *            The parent composite
     * @param labelText
     *            The label text
     * @param tooltip
     *            The tooltip text
     * @return The list control
     */
    private static org.eclipse.swt.widgets.List createList( final Composite parent, final String labelText, final String tooltip ) {
        final Label networkLabel = new Label( parent, SWT.NONE | SWT.WRAP );
        networkLabel.setText( labelText );
        final org.eclipse.swt.widgets.List list = new org.eclipse.swt.widgets.List( parent, SWT.NONE | SWT.V_SCROLL
                | SWT.H_SCROLL | SWT.BORDER );
        GridData gridData = new GridData( GridData.FILL_HORIZONTAL );
        gridData.heightHint = 50;
        list.setLayoutData( gridData );
        list.setToolTipText( WordUtils.wrap( tooltip, 100 ) );
        // create a Composite instance for button group
        Composite buttonComposite = CompositeFactory.gridComposite( parent, tooltip.isEmpty() ? 3 : 4);
        // add "Add" button
        final Button addButton = new Button( buttonComposite, SWT.PUSH );
        addButton.setText( Messages.IConstants_ADD_BUTTON_TITLE );
        setDialogConfirmButtonLayoutData( addButton );
        addButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                try {
                    // create an input dialog
                    InputDialog dialog = new InputDialog( addButton.getShell(), "Add new", labelText, null, null );
                    // display the dialog
                    int result = dialog.open();
                    String input;
                    if( result == Window.OK )
                        input = dialog.getValue();
                    else
                        input = null;
                    if( input == null )
                        return;
                    list.add( input );
                    _thisInst.updateLaunchConfigurationDialog();
                } catch( Exception ex ) {
                    _logger.error( ex.getMessage() );
                }
            }
        } );
        // add "Delete" button
        final Button deleteButton = new Button( buttonComposite, SWT.PUSH );
        deleteButton.setText( Messages.IConstants_DELETE_BUTTON_TITLE );
        setDialogConfirmButtonLayoutData( deleteButton );
        // add listener to listen to the event when "Delete" button is pressed
        deleteButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                int index = list.getSelectionIndex();
                list.remove( index );
                _thisInst.updateLaunchConfigurationDialog();
            }
        } );
        // add "Edit" button
        final Button editButton = new Button( buttonComposite, SWT.PUSH );
        editButton.setText( Messages.IConstants_EDIT_BUTTON_TITLE );
        setDialogConfirmButtonLayoutData( editButton );
        editButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                // create an input dialog
                SimpleVectorInputDialog dialog = new SimpleVectorInputDialog( labelText, Arrays.asList( list.getItems() ),
                        editButton.getShell() );
                List< String > l = dialog.open();
                if( l == null ) {
                    return;
                }
                list.setItems( l.toArray( new String[ 0 ] ) );
                _thisInst.updateLaunchConfigurationDialog();
            }
        } );

        if(!tooltip.isEmpty()) {
        	// Info button to display steady the tooltip text
            final Button hlpButton = new Button(buttonComposite, SWT.PUSH);
            hlpButton.setImage(ContextManager.PLUGIN.getImageFromPlugin( ContextManager.PLUGIN_ID, "icons/obj16/help_icon.png" ));
            hlpButton.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected( SelectionEvent e ) {
                	InfoDialog id = SimulatorConfigurationTabBase._thisInst.new InfoDialog(hlpButton, tooltip, labelText);
                	id.open();
                }
            } );
        }
        return list;
    }

    /**
     * Sets the layout of the given <code>button</code>.
     *
     * @param button
     */
    private static void setDialogConfirmButtonLayoutData( Button button ) {
        GridData data = new GridData( GridData.HORIZONTAL_ALIGN_FILL );
        Point minSize = button.computeSize( SWT.DEFAULT, SWT.DEFAULT, true );
        data.widthHint = Math.max( IDialogConstants.BUTTON_WIDTH, minSize.x );
        button.setLayoutData( data );
    }

    private static class DeviceComboContentProvider implements IStructuredContentProvider {
        public Object[] getElements( Object inputElement ) {
            if( inputElement instanceof List ) {
                return ( (List) inputElement ).toArray();
            }
            return new Object[ 0 ];
        }

        public void dispose() {
            // do nothing
        }

        public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
            // do nothing
        }
    }

    private static class DeviceComboLabelProvider extends LabelProvider {
        public String getText( Object element ) {
            if( element instanceof DeviceInfo ) {
                return ( (DeviceInfo) element ).toString();
            }
            return "";
        }
    }
    
    /**
     * Dialog used to display more pregnant tooltip info.
     */
    private class InfoDialog extends Dialog {

        private Text _txt;
        private String _msg, _title;
        private Point _parp;	// origin of parent for reference to position this dialog

        /**
         * Constructs an instance of InfoDialog.
         * @param parent -the parent shell
         * @param msg - the message to be displayed
         * @param title - the title of the dialog
         * 
         */
        InfoDialog( Button parent, String msg, String title) {
            super( parent.getShell());
            _msg = msg;
            _title = title;
            _parp = getAbsPoint(parent);
        }
        
        private Point getAbsPoint(Control ctrl) {
        	Composite par = ctrl.getParent();
        	Point parp;
        	if(par==null) {
        		Rectangle shr = ctrl.getShell().getBounds();
        		parp = new Point (-shr.x, -shr.y);
        	}else {
        		parp = getAbsPoint(par);
        	}
        	
        	Rectangle ctrlr = ctrl.getBounds();
        	return new Point(parp.x + ctrlr.x, parp.y + ctrlr.y);
        }

        /**
         * Opens the dialog.
         */
        public void open() {
            // Create the dialog window
            Shell shell = new Shell( getParent(), SWT.CLOSE | SWT.APPLICATION_MODAL | SWT.SCROLL_PAGE);
            shell.setText( _title );
            createContents( shell );
            shell.setBounds(_parp.x, _parp.y, 354, 84);
            shell.pack();
            shell.open();
            Display display = getParent().getDisplay();
            while( !shell.isDisposed() ) {
                if( !display.readAndDispatch() ) {
                    display.sleep();
                }
            }
        }

        private void createContents( final Shell parent ) {
        	GridLayout layout = new GridLayout( 1, false );
            layout.marginHeight = 2;
            layout.marginWidth = 2;
            parent.setLayout( layout );
            
            GridData data2 = new GridData( GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
            data2.widthHint = 350;
            data2.heightHint = 80;
            
            _txt = new Text(parent, SWT.READ_ONLY | SWT.WRAP);
            _txt.setText(_msg);
            _txt.setLayoutData(data2);
        }
    }

}
