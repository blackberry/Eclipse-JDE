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
 * A class used for storing information on each simulator device.
 *
 * @author dmateescu
 * @since September 2007
 *
 */
public class DeviceInfo {

    private String _bundleName;
    private String _deviceName;
    private String _directory;
    private String _configName;

    /**
     * Constructor
     *
     * @param bundleName
     *            The simulator bundle name
     * @param deviceName
     *            The device name
     * @param directory
     *            The simulator installation directory
     */
    public DeviceInfo( String bundleName, String deviceName, String directory, String configName ) {
        _bundleName = bundleName;
        _deviceName = deviceName;
        _directory = directory;
        _configName = configName;
    }

    /**
     * @return
     */
    public String getDirectory() {
        return _directory;
    }

    /**
     * Getter method for the device name
     *
     * @return
     */
    public String getDeviceName() {
        return _deviceName;
    }

    /**
     * Getter method for the bundle name
     *
     * @return
     */
    public String getBundleName() {
        return _bundleName;
    }

    /**
     * Getter method for the config name
     *
     * @return
     */
    public String getConfigName() {
        return _configName;
    }

    @Override
    public boolean equals( Object obj ) {
        if( obj instanceof DeviceInfo ) {
            return ( (DeviceInfo) obj ).getDirectory().equals( _directory )
                    && ( (DeviceInfo) obj ).getBundleName().equals( _bundleName )
                    && ( (DeviceInfo) obj ).getDeviceName().equals( _deviceName );
        }
        return false;
    }

    @Override
    public String toString() {
        return _bundleName + " - " + _deviceName;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

}
