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

import org.eclipse.jdt.core.IJavaElementDelta;

@InternalFragmentReplaceable
public class ClasspathElementChangedListener extends BasicClasspathElementChangedListener {

    @Override
    protected void checkSourceAttachement( IJavaElementDelta[] changedElements ) {
        // noting needs to be done in the external plug-in

    }

}
