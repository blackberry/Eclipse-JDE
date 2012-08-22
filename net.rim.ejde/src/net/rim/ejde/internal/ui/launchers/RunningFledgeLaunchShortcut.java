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

import net.rim.ejde.internal.launching.IRunningFledgeLaunchConstants;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;

/**
 * @author dmeng
 *
 */
public class RunningFledgeLaunchShortcut extends AbstractLaunchShortcut {

    /*
     * (non-Javadoc)
     *
     * @seeorg.eclipse.jdt.internal.debug.ui.launcher.JavaLaunchShortcut# getConfigurationType()
     */
    protected ILaunchConfigurationType getConfigurationType() {
        ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
        return lm.getLaunchConfigurationType( IRunningFledgeLaunchConstants.LAUNCH_CONFIG_ID );
    }
}
