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

import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;

@InternalFragmentReplaceable
public class InternalContextManagerUtils {
    /**
     * Some extra steps need to be done when ejde starts up
     */
    static public void startupInitialize() {
        // noting needs to be done in the external plug-in
    }

    /**
     * Some extra steps need to be done when ejde stops
     */
    static public void stopCleanup() {
        // noting needs to be done in the external plug-in
    }
}
