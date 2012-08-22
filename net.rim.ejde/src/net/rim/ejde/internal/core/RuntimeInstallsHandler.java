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
package net.rim.ejde.internal.core;

import java.io.IOException;

import net.rim.ejde.internal.launching.DeviceProfileManager;
import net.rim.ejde.internal.model.BlackBerrySDKInstall;
import net.rim.ejde.internal.model.BlackBerryVMInstallType;
import net.rim.ejde.internal.util.VMToolsUtils;
import net.rim.ejde.internal.util.VMUtils;

import org.apache.log4j.Logger;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallChangedListener;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.PropertyChangeEvent;

/**
 * The Class RuntimeInstallsHandler.
 *
 * @author cmalinescu, jheifetz
 */
public final class RuntimeInstallsHandler implements IVMInstallChangedListener {

    static private final Logger _log = Logger.getLogger( RuntimeInstallsHandler.class );

    static private RuntimeInstallsHandler INSTANCE;

    /**
     * Gets the single instance of RuntimeInstallsHandler.
     *
     * @return single instance of RuntimeInstallsHandler
     */
    static synchronized RuntimeInstallsHandler getInstance() {
        if( null == INSTANCE ) {
            INSTANCE = new RuntimeInstallsHandler();
            JavaRuntime.addVMInstallChangedListener( INSTANCE );
        }

        return INSTANCE;
    }

    /**
     * Removes the instance.
     */
    static public void removeInstance() {
        JavaRuntime.removeVMInstallChangedListener( INSTANCE );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.IVMInstallChangedListener#defaultVMInstallChanged (org.eclipse.jdt.launching.IVMInstall,
     * org.eclipse.jdt.launching.IVMInstall)
     */
    @Override
    public void defaultVMInstallChanged( final IVMInstall previous, final IVMInstall current ) {
        _log.debug( "VM: *SWITCH* from [" + ( previous == null ? "NULL" : previous.getId() + "$" + previous.getName() )
                + "] to [" + ( current == null ? "NULL" : current.getId() + "$" + current.getName() ) + "]" );
        EJDEEventNotifier.getInstance().notifyWorkspaceJREChange( previous, current );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.IVMInstallChangedListener#vmAdded(org.eclipse .jdt.launching.IVMInstall)
     */
    @Override
    public void vmAdded( final IVMInstall vm ) {
        _log.debug( "VM: *ADDED* [" + vm.getId() + "$" + vm.getInstallLocation() + "]" );
        try {
            VMToolsUtils.addVMTools( vm );
        } catch( IOException ioe ) {
            _log.error( "Error Updating Signature Tool", ioe );
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.IVMInstallChangedListener#vmChanged(org.eclipse .jdt.launching.PropertyChangeEvent)
     */
    @Override
    public void vmChanged( final PropertyChangeEvent event ) {
        String property = event.getProperty();
        Object oldValue = event.getOldValue(), newValue = event.getNewValue();

        String oldValueStrRepr, newValueStrRepr;

        oldValueStrRepr = null != oldValue ? oldValue.toString() : "";
        newValueStrRepr = null != newValue ? newValue.toString() : "";

        _log.debug( "Event [" + property + "] for: *OLD* was[" + oldValueStrRepr + "] *NEW* is [" + newValueStrRepr + "]." );

        // Reload the device profiles when the VM changes
        if( event.getSource() instanceof BlackBerrySDKInstall ) {
            BlackBerrySDKInstall sourceVM = (BlackBerrySDKInstall) event.getSource();
            if( property.equals( BlackBerryVMInstallType.ATTR_DEFINITION_FILE ) ) {
                if( !oldValue.equals( newValue ) ) {
                    DeviceProfileManager.getInstance().reloadDeviceInfo( sourceVM );
                    EJDEEventNotifier.getInstance().notifyJREDefinitionChanged( sourceVM );
                }
            }
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.IVMInstallChangedListener#vmRemoved(org.eclipse .jdt.launching.IVMInstall)
     */
    @Override
    public void vmRemoved( final IVMInstall vm ) {
        _log.debug( "VM: *REMOVED* [" + vm.getId() + "$" + vm.getName() + "]" );
        VMUtils.removeSignKeysFromCache( vm );
        try {
            VMToolsUtils.removeVMTools( vm );
        } catch( IOException ioe ) {
            _log.error( "Error Updating Signature Tool", ioe );
        }
    }

}
