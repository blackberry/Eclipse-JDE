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

import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

/**
 * @author thungyao
 *
 */
public class FledgeLaunchConfigurationTabGroup extends AbstractBlackBerryLaunchConfigurationTabGroup {
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse .debug.ui.ILaunchConfigurationDialog,
     * java.lang.String)
     */
    public void createTabs( ILaunchConfigurationDialog dialog, String mode ) {
        ProjectsTab projectsTab = new ProjectsTab( this );
        SimulatorConfigurationTab simulatorTab = new SimulatorConfigurationTab();
        BBJRETab jreTab = new BBJRETab( projectsTab );
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { projectsTab, jreTab, simulatorTab,
                new SourceLookupTab(), new BBCommonTab() };
        setTabs( tabs );
    }
}
