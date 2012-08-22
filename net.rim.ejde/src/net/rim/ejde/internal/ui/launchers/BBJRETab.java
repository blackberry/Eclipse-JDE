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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.rim.ejde.internal.launching.IFledgeLaunchConstants;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.preferences.CompliancePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class BBJRETab extends AbstractLaunchConfigurationTab implements IFledgeLaunchConstants {

    private static final Logger _logger = Logger.getLogger( BBJRETab.class );
    private Button _projectJREButton;
    private Button _alternateJREButton;
    private Combo _jreCombo;
    private Button _installedJREButton;
    private ProjectsTab _projectsTab;

    /**
     * Constructor
     *
     * @param simulatorTab
     *            SimulatorConfigurationTab
     */
    public BBJRETab( ProjectsTab projectsTab ) {
        _projectsTab = projectsTab;
    }

    /*
     * Returns false if there is no project stored in launch configuration; it disables Run/Debug button.
     *
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse .debug.core.ILaunchConfiguration)
     */
    @Override
    public boolean isValid( ILaunchConfiguration configuration ) {
        Set< IProject > projects = LaunchUtils.getProjectsFromConfiguration( configuration );
        if( projects.isEmpty() ) {
            setErrorMessage( "Please select at least one project." );
            return false;
        }
        IVMInstall vm = LaunchUtils.getVMFromConfiguration( configuration );
        // VM is removed
        if( vm == null ) {
            String vmId = LaunchUtils.getVMNameFromConfiguration( configuration );
            setErrorMessage( "Unable to resolve JRE: " + vmId );
            return false;
        }
        // VM is not a BlackBerry VM
        if( !VMUtils.isBlackBerryVM( vm ) ) {
            String vmId = LaunchUtils.getVMNameFromConfiguration( configuration );
            setErrorMessage( "Not a BlackBerry JRE: " + vmId );
            return false;
        }
        setMessage( null );
        setErrorMessage( null );
        return true;
    }

    /**
     * Creates the UI for this tab
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl( Composite parent ) {
        Composite mainComposite = new Composite( parent, SWT.NONE );
        mainComposite.setLayout( new GridLayout() );
        GridData layoutData = new GridData( GridData.FILL_BOTH );
        mainComposite.setLayoutData( layoutData );

        Group group = createJRESelectionGroup( mainComposite );
        layoutData = new GridData( GridData.FILL_HORIZONTAL );
        group.setLayoutData( layoutData );
        setControl( mainComposite );
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return Messages.JRETab_title;
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
     */
    public Image getImage() {
        return JavaUI.getSharedImages().getImage( ISharedImages.IMG_OBJS_LIBRARY );

    }

    /**
     * Initialize current UI selection to data given by configuration parameter
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public void initializeFrom( ILaunchConfiguration configuration ) {
        try {
            Collection< IProject > projects = _projectsTab.getSelectedProjects();
            IVMInstall vm = LaunchUtils.getDefaultLaunchVM( projects );
            if( vm != null ) {
                _projectJREButton.setText( Messages.JRETab_projectJRE + " (" + vm.getName() + ")" );
            } else {
                _projectJREButton.setText( Messages.JRETab_projectJRE + " (undefined)" );
            }
            int jreType = configuration.getAttribute( ATTR_JRE_TYPE, DEFAULT_JRE_TYPE );
            if( jreType == JRE_TYPE_PROJECT ) {
                _projectJREButton.setSelection( true );
                _alternateJREButton.setSelection( false );
                _jreCombo.setEnabled( false );
            } else if( jreType == JRE_TYPE_ALTERNATE ) {
                _projectJREButton.setSelection( false );
                _alternateJREButton.setSelection( true );
                initJREComboSelection( configuration );
            }
        } catch( CoreException e ) {
            setErrorMessage( Messages.ProjectsTab_noProjectSelected );
            _logger.error( "", e );
        }
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply( ILaunchConfigurationWorkingCopy configuration ) {
        if( _projectJREButton.getSelection() ) {
            configuration.setAttribute( ATTR_JRE_TYPE, JRE_TYPE_PROJECT );
        } else {
            configuration.setAttribute( ATTR_JRE_TYPE, JRE_TYPE_ALTERNATE );
            IVMInstall vm = VMUtils.findVMByName( _jreCombo.getText() );
            if( vm != null ) {
                configuration.setAttribute( ATTR_JRE_ID, vm.getId() );
            }
        }
    }

    /**
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#deactivated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void deactivated( ILaunchConfigurationWorkingCopy workingCopy ) {
        // do nothing
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults( ILaunchConfigurationWorkingCopy configuration ) {
    }

    /**
     * Sets the layout of the given <code>button</code>.
     *
     * @param button
     */
    public static void setDialogConfirmButtonLayoutData( Button button ) {
        GridData data = new GridData( GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING );
        Point minSize = button.computeSize( SWT.DEFAULT, SWT.DEFAULT, true );
        data.widthHint = Math.max( IDialogConstants.BUTTON_WIDTH, minSize.x );
        button.setLayoutData( data );
    }

    public void propertyChange( PropertyChangeEvent evt ) {
        updateLaunchConfigurationDialog();
    }

    private Group createJRESelectionGroup( Composite parent ) {
        Group group = new Group( parent, SWT.NONE );
        group.setText( Messages.JRETab_groupTitle );
        GridLayout layout = new GridLayout( 3, false );
        group.setLayout( layout );

        _projectJREButton = new Button( group, SWT.RADIO );
        GridData layoutData = new GridData( GridData.FILL_HORIZONTAL );
        layoutData.horizontalSpan = 3;
        _projectJREButton.setLayoutData( layoutData );
        _projectJREButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                if( _projectJREButton.getSelection() ) {
                    _jreCombo.setEnabled( false );
                    updateLaunchConfigurationDialog();
                }
            }
        } );

        _alternateJREButton = new Button( group, SWT.RADIO );
        _alternateJREButton.setText( Messages.JRETab_alternateJRE );
        _alternateJREButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                if( _alternateJREButton.getSelection() ) {
                    _jreCombo.setEnabled( true );
                    int index = _jreCombo.getSelectionIndex();
                    if( index == -1 && _jreCombo.getItemCount() > 0 ) {
                        _jreCombo.select( 0 );
                    }
                    updateLaunchConfigurationDialog();
                }
            }
        } );

        _jreCombo = new Combo( group, SWT.DROP_DOWN | SWT.READ_ONLY );
        List< IVMInstall > installedVMs = VMUtils.getInstalledBBVMs();
        List< String > vmNames = new ArrayList< String >();
        for( IVMInstall vm : installedVMs ) {
            vmNames.add( vm.getName() );
        }
        _jreCombo.setItems( vmNames.toArray( new String[ vmNames.size() ] ) );
        _jreCombo.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        _jreCombo.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                updateLaunchConfigurationDialog();
            }
        } );

        _installedJREButton = new Button( group, SWT.NONE );
        _installedJREButton.setText( Messages.JRETab_installedJREs );
        _installedJREButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                String jreID = BuildPathSupport.JRE_PREF_PAGE_ID;
                String eeID = BuildPathSupport.EE_PREF_PAGE_ID;
                String complianceId = CompliancePreferencePage.PREF_ID;
                Map data = new HashMap();
                data.put( PropertyAndPreferencePage.DATA_NO_LINK, Boolean.TRUE );
                PreferencesUtil.createPreferenceDialogOn( e.display.getActiveShell(), jreID,
                        new String[] { jreID, complianceId, eeID }, data ).open();
                fillInstalledJREs();
                updateProjectJRE();
                updateLaunchConfigurationDialog();
            }
        } );
        return group;
    }

    private void initJREComboSelection( ILaunchConfiguration configuration ) {
        try {
            String jreId = configuration.getAttribute( ATTR_JRE_ID, StringUtils.EMPTY );
            List< IVMInstall > installedVMs = VMUtils.getInstalledBBVMs();
            int selectedIndex = 0;
            for( IVMInstall vm : installedVMs ) {
                if( vm.getId().equals( jreId ) ) {
                    break;
                }
                selectedIndex++;
            }
            _jreCombo.select( selectedIndex );
        } catch( CoreException e ) {
            _logger.error( "", e );
        }
    }

    /**
     * Populate installed JRE ComboBox.Preserve the current selection.
     */
    private void fillInstalledJREs() {
        String selectedItem = "";
        int selectionIndex = -1;
        selectionIndex = _jreCombo.getSelectionIndex();
        if( selectionIndex != -1 ) {// paranoia
            selectedItem = _jreCombo.getItems()[ selectionIndex ];
        }
        List< IVMInstall > standins = VMUtils.getInstalledBBVMs();
        IVMInstall[] installedVMs = ( standins.toArray( new IVMInstall[ standins.size() ] ) );

        selectionIndex = -1;// find new index
        String[] jreLabels = new String[ installedVMs.length ];
        String[] RECompliance = new String[ installedVMs.length ];
        for( int i = 0; i < installedVMs.length; i++ ) {
            jreLabels[ i ] = installedVMs[ i ].getName();
            if( selectedItem != null && jreLabels[ i ].equals( selectedItem ) ) {
                selectionIndex = i;
            }
            if( installedVMs[ i ] instanceof IVMInstall2 ) {
                RECompliance[ i ] = JavaModelUtil.getCompilerCompliance( (IVMInstall2) installedVMs[ i ], JavaCore.VERSION_1_4 );
            } else {
                RECompliance[ i ] = JavaCore.VERSION_1_4;
            }
        }
        _jreCombo.setItems( jreLabels );
        if( selectionIndex == -1 ) {
            _jreCombo.setText( getDefaultJREName() );
        } else {
            _jreCombo.select( selectionIndex );
        }
    }

    /**
     * Update project JRE.
     */
    private void updateProjectJRE() {
        Collection< IProject > projects = _projectsTab.getSelectedProjects();
        IVMInstall vm = LaunchUtils.getDefaultLaunchVM( projects );
        if( vm != null ) {
            _projectJREButton.setText( Messages.JRETab_projectJRE + " (" + vm.getName() + ")" );
        }
    }

    private String getDefaultJREName() {
        IVMInstall vm = VMUtils.getDefaultBBVM();
        return vm != null ? vm.getName() : StringUtils.EMPTY;
    }
}
