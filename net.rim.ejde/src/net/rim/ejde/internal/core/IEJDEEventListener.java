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

import net.rim.ejde.internal.model.BlackBerrySDKInstall;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IVMInstall;

/**
 * The listener interface for receiving eJDE events. The class that is interested in processing these events implements this
 * interface, and the object created with that class is registered with
 * {@link EJDEEventNotifier#addEJDEEventListener(IEJDEEventListener)} When the events occur that object's appropriate method is
 * invoked.
 */
public interface IEJDEEventListener {

    /**
     * Workspace jre changed.
     *
     * @param previous
     *            the previous
     * @param current
     *            the current
     */
    public void workspaceJREChanged( IVMInstall previous, IVMInstall current );

    /**
     * Jre definition changed.
     *
     * @param sourceVM
     *            the source vm
     */
    public void jreDefinitionChanged( BlackBerrySDKInstall sourceVM );

    /**
     * Invoked when new project is created.
     *
     * @param project
     *            the project
     */
    public void newProjectCreated( IJavaProject project );

    /**
     * Class path changed.
     *
     * @param project
     *            the project
     */
    public void classPathChanged( IJavaProject project, boolean isProjectJREChange );

    /**
     * Workspace preprocessor tags changed.
     */
    public void workspacePreprocessorTagsChanged();

    /**
     * Project preprocessor tag changed.
     *
     * @param project
     *            the project
     */
    public void projectPreprocessorTagChanged( IProject project );

}
