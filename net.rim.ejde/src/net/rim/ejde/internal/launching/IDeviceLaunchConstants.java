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
package net.rim.ejde.internal.launching;

/**
 * Interface that defines common attribute constants for EJDE launch configurations
 *
 * @author bchabot
 *
 */
public interface IDeviceLaunchConstants {

    /** attribute for defining which device to attach to */
    public static final String DEVICE = "Device"; //$NON-NLS-1$

    /** constant value - if DEVICE attribute is set to this, will connect to first device it finds */
    public static final String ANY_DEVICE = "AnyDevice"; //$NON-NLS-1$

    /** The id of a RIM aka BlackBerry Device launch config type */
    public static final String LAUNCH_CONFIG_ID = "net.rim.ejde.launching.DeviceLaunchConfiguration"; //$NON-NLS-1$

    /** The refresh interval, the refresh task is used to scan USB port for device unplug */
    public static final int REFRESH_TIME = 5000;

    /** attribute for deploying projects to device */
    public static final String DEPLOY_PROJECT_TO_DEVICE = "deployProjectsToDevice";
}
