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

/**
 * Provides the allowable start up tiers. This class may be overriden by the RIM internal plug-in (if present).
 */
@InternalFragmentReplaceable
public class StartupTiers {

    public static int[] getAllowedTiers() {
        return new int[] { 6, 7 };
    }

}
