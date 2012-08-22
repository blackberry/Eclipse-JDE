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

import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;
import net.rim.ide.RIA;

/**
 * RiaMaker is a small class for contructing an instance of RIA. It is called by ContextManager.
 *
 * This class may be overriden by the RIM internal plug-in fragment (if present). The fragment uses this call as an opportunity to
 * set the name of the debug mirror in RIA, based on InternalPreferences.
 */
@InternalFragmentReplaceable
public class RiaMaker {

    static RIA createRia( final String homePath, final String dllPath ) {
        RIA ria = new RIA( homePath, dllPath, null );
        return ria;
    }

}
