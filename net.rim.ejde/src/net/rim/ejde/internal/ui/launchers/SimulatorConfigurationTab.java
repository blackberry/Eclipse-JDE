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

import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;

import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Tab to configure the simulator. The class may be replaced by the internal plug-in fragment (if present).
 */
@InternalFragmentReplaceable
public class SimulatorConfigurationTab extends SimulatorConfigurationTabBase {

    public void initializeFrom( ILaunchConfiguration configuration ) {
        _initializingForm = true;
        super.initializeFrom( configuration );
        _initializingForm = false;
    }

}
