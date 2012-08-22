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

import java.util.Vector;
import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;
import net.rim.ejde.internal.model.BlackBerryProperties;

@InternalFragmentReplaceable
public class InternalPackagingUtils {
    static public void addOtherOptions( Vector< String > options, BlackBerryProperties properties ) {
        // do nothing in the external plug-in
    }

}
