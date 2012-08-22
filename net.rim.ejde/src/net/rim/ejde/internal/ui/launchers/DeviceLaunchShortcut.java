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

import net.rim.ejde.internal.launching.IDeviceLaunchConstants;

import org.apache.log4j.Logger;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;

/**
 * @author bchabot
 *
 */
public class DeviceLaunchShortcut extends AbstractLaunchShortcut {

    Logger log = Logger.getLogger( DeviceLaunchShortcut.class );

    /*
     * (non-Javadoc)
     *
     * @seeorg.eclipse.jdt.internal.debug.ui.launcher.JavaLaunchShortcut# getConfigurationType()
     */
    protected ILaunchConfigurationType getConfigurationType() {
        ILaunchManager lm = DebugPlugin.getDefault().getLaunchManager();
        return lm.getLaunchConfigurationType( IDeviceLaunchConstants.LAUNCH_CONFIG_ID );
    }
}
