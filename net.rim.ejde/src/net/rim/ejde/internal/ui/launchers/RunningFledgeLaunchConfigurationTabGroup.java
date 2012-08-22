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
 * Running simulator launch configuration
 *
 * @author bchabot
 *
 */
public class RunningFledgeLaunchConfigurationTabGroup extends AbstractBlackBerryLaunchConfigurationTabGroup {

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#createTabs(org.eclipse.debug.ui.ILaunchConfigurationDialog,
     *      java.lang.String)
     */
    public void createTabs( ILaunchConfigurationDialog dialog, String mode ) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { new ProjectsTab( this ), new SourceLookupTab(),
                new BBCommonTab() };

        setTabs( tabs );
    }
}
