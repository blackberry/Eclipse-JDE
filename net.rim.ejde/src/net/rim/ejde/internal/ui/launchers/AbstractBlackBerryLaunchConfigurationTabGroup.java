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

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;

/**
 * Parent class for all BlackBerry launch configuration tab groups.
 *
 * @author bchabot
 *
 */
abstract public class AbstractBlackBerryLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTabGroup#performApply(ILaunchConfigurationWorkingCopy configuration)
     */
    public void performApply( ILaunchConfigurationWorkingCopy config ) {
        super.performApply( config );
        // try {
        // if( config.isDirty() && config.isLocal() ) {
        // ILaunchConfiguration origconf = config.getOriginal();
        // if( origconf != null && !origconf.getName().equals( config.getName() ) ) {
        // origconf.delete();
        // config.doSave();
        // }
        // }
        // } catch( CoreException e ) {
        // _log.error( e );
        // }

    }

}
