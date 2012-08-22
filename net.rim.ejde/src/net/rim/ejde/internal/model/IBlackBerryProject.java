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
package net.rim.ejde.internal.model;

import org.eclipse.jdt.core.IJavaProject;

/**
 * The IBlackBerryProject Interface serves as an interface to any project that instantiates the BlackBerry Model.
 */
public interface IBlackBerryProject extends IJavaProject {

    /**
     * Gets the BlackBerry properties.
     *
     * @return the BlackBerry properties
     */
    public BlackBerryProperties getProperties();
}
