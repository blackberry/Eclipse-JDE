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
package net.rim.ejde.internal.util;

import net.rim.ejde.internal.core.ContextManager;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

/**
 * Utility class to auto-create Status objects for this plugin
 *
 * @author bchabot, cmalinescu
 */
public class StatusFactory {

    private StatusFactory() {
    }

    /**
     * Create error status
     *
     * @param message
     * @param exception
     * @return
     */
    public static IStatus createErrorStatus( String message, Throwable exception ) {
        return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, IStatus.ERROR, message, exception );
    }

    /**
     * Create error status
     *
     * @param message
     * @return
     */
    public static IStatus createErrorStatus( String message ) {
        return createErrorStatus( message, null );
    }

    /**
     * Create status of given type
     *
     * @param severity
     * @param message
     * @return
     */
    public static IStatus createStatus( int severity, String message ) {
        return new Status( severity, ContextManager.PLUGIN_ID, severity, message, null );
    }

    /**
     * Create warning status
     *
     * @param message
     * @param exception
     * @return
     */
    public static IStatus createWarningStatus( String message ) {
        return new Status( IStatus.WARNING, ContextManager.PLUGIN_ID, IStatus.WARNING, message, null );
    }

    /**
     * Create info status
     *
     * @param message
     * @param exception
     * @return
     */
    public static IStatus createinfoStatus( String message ) {
        return new Status( IStatus.INFO, ContextManager.PLUGIN_ID, IStatus.INFO, message, null );
    }

    /**
     * Merge's two status objects together
     *
     * @param currentStatus
     * @param newStatus
     * @return merged status
     */
    public static IStatus mergeStatus( IStatus currentStatus, IStatus newStatus ) {
        MultiStatus multiStatus = null;
        if( currentStatus instanceof MultiStatus ) {
            multiStatus = (MultiStatus) currentStatus;
        } else {
            multiStatus = new MultiStatus( ContextManager.PLUGIN_ID, IStatus.OK, "", null );
            multiStatus.add( currentStatus );
        }
        multiStatus.merge( newStatus );
        return multiStatus;
    }

    /**
     * Creates a blank MultiStatus with given message
     *
     * @param msg
     * @return
     */
    public static MultiStatus createMultiStatus( String msg ) {

        return new MultiStatus( ContextManager.PLUGIN_ID, IStatus.OK, msg, null );
    }
}
