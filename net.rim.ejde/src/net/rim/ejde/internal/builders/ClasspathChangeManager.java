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
package net.rim.ejde.internal.builders;

import net.rim.ejde.internal.core.ClasspathElementChangedListener;

import org.eclipse.jdt.core.JavaCore;

/**
 * The Class ClasspathChangeManager.
 */
public class ClasspathChangeManager {

    private static class ClasspathChangeManagerHolder {
        public static ClasspathChangeManager classpathChangeManager = new ClasspathChangeManager();
    }

    private ClasspathElementChangedListener _elementChangedListener;

    /**
     * Private default constructor
     *
     */
    private ClasspathChangeManager() {
        super();
        _elementChangedListener = new ClasspathElementChangedListener();
    }

    /**
     * Singleton getInstance() method.
     *
     * @return the unique instance of the class
     */
    public static synchronized ClasspathChangeManager getInstance() {
        return ClasspathChangeManagerHolder.classpathChangeManager;
    }

    /**
     * This method adds an element change listener to Java Core after removing its prior instance in the case the method is called
     * multiple times.
     */
    public synchronized void addElementChangedListener() {
        JavaCore.removeElementChangedListener( _elementChangedListener ); // has no effect if the element exists
        JavaCore.addElementChangedListener( _elementChangedListener );
    }

    /**
     * Removes the unique element changed listener. Has no affect if the unique listener is not registered.
     */
    public synchronized void removeAddElementChangedListener() {
        JavaCore.removeElementChangedListener( _elementChangedListener );
    }

}
