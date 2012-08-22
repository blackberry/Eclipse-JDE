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

import java.net.URL;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;

@InternalFragmentReplaceable
public class InternalSchemaValidatorUtils {
    /** BlackBerry application descriptor schema location */
    private static final String SCHEMA_LOCATION = "/schema/BlackBerry_App_Descriptor.xsd";

    public static URL getSchemaLocation() {
        return ContextManager.PLUGIN.getBundle().getEntry( SCHEMA_LOCATION );
    }
}
