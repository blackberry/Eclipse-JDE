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

import java.util.Map;

import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;
import net.rim.ejde.internal.util.ProjectUtils.RRHFile;

import org.eclipse.jdt.core.IJavaProject;

@InternalFragmentReplaceable
public class InternalProjectUtils {

    public static void getResourcesFromJars( IJavaProject javaProject, Map< String, RRHFile > rrhFileMap ) {
        // do nothing in the main plug-in
    }
}
