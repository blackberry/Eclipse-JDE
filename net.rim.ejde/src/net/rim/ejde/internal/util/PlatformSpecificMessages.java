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

import net.rim.ide.OSUtils;

import org.eclipse.osgi.util.NLS;

/**
 * This class handles all platform specific messages.
 *
 * @author dmeng
 *
 */
public class PlatformSpecificMessages extends NLS {
    private static final String BUNDLE_NAME_WIN = Messages.class.getPackage().getName() + ".messages_win"; //$NON-NLS-1$
    private static final String BUNDLE_NAME_MAC = Messages.class.getPackage().getName() + ".messages_mac"; //$NON-NLS-1$

    public static String SignCommandHandler_MissingFilesDialogMsg;

    static {
        // initialize resource bundle
        if( OSUtils.isMac() ) {
            NLS.initializeMessages( BUNDLE_NAME_MAC, PlatformSpecificMessages.class );
        } else {
            NLS.initializeMessages( BUNDLE_NAME_WIN, PlatformSpecificMessages.class );
        }
    }

    private PlatformSpecificMessages() {
    }
}
