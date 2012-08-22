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

import net.rim.ejde.internal.launching.IDeviceLaunchConstants;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Projects tab for device launch configuration.
 *
 * @author dmeng
 *
 */
public class DeviceLCProjectsTab extends ProjectsTab {
    private Button _deployProjectsButton;

    /**
     * Constructs the tab.
     */
    public DeviceLCProjectsTab( AbstractBlackBerryLaunchConfigurationTabGroup tabGroup ) {
        super( tabGroup );
    }

    @Override
    public void createControl( Composite parent ) {
        Composite mainComposite = new Composite( parent, SWT.NONE );
        mainComposite.setLayout( new GridLayout() );
        GridData layoutData = new GridData( SWT.FILL, SWT.FILL, true, true );
        mainComposite.setLayoutData( layoutData );

        super.createControl( mainComposite );
        _deployProjectsButton = new Button( mainComposite, SWT.CHECK );
        _deployProjectsButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                propertyChange( new PropertyChangeEvent( _deployProjectsButton, "", null, null ) );
            }
        } );
        _deployProjectsButton.setText( Messages.DeviceLCProjectsTab_deployProjects );

        setControl( mainComposite );
    }

    @Override
    public void initializeFrom( ILaunchConfiguration configuration ) {
        super.initializeFrom( configuration );
        boolean deployProjects = LaunchUtils.getBooleanAttribute( configuration, IDeviceLaunchConstants.DEPLOY_PROJECT_TO_DEVICE,
                true );
        _deployProjectsButton.setSelection( deployProjects );
    }

    @Override
    public void performApply( ILaunchConfigurationWorkingCopy configuration ) {
        super.performApply( configuration );
        configuration.setAttribute( IDeviceLaunchConstants.DEPLOY_PROJECT_TO_DEVICE, _deployProjectsButton.getSelection() );
    }

}
