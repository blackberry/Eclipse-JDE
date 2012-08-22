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
 * The Class EJDEEventAdapter.
 *
 * @author jheifetz
 */
public class EJDEEventAdapter implements IEJDEEventListener {

    /*
     * (non-Javadoc)
     *
     * @see net.rim.ejde.internal.core.IEJDEEventListener#classPathChanged(org.eclipse.jdt.core.IJavaProject, boolean)
     */
    @Override
    public void classPathChanged( IJavaProject project, boolean isProjectJREChange ) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see net.rim.ejde.internal.core.IEJDEEventListener#jreDefinitionChanged(net.rim.ejde.internal.model.BlackBerrySDKInstall)
     */
    @Override
    public void jreDefinitionChanged( BlackBerrySDKInstall sourceVM ) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see net.rim.ejde.internal.core.IEJDEEventListener#newProjectCreated(org.eclipse.jdt.core.IJavaProject)
     */
    @Override
    public void newProjectCreated( IJavaProject project ) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see net.rim.ejde.internal.core.IEJDEEventListener#projectPreprocessorTagChanged(org.eclipse.core.resources.IProject)
     */
    @Override
    public void projectPreprocessorTagChanged( IProject project ) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see net.rim.ejde.internal.core.IEJDEEventListener#workspaceJREChanged(org.eclipse.jdt.launching.IVMInstall,
     * org.eclipse.jdt.launching.IVMInstall)
     */
    @Override
    public void workspaceJREChanged( IVMInstall previous, IVMInstall current ) {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     *
     * @see net.rim.ejde.internal.core.IEJDEEventListener#workspacePreprocessorTagsChanged()
     */
    @Override
    public void workspacePreprocessorTagsChanged() {
        // TODO Auto-generated method stub

    }

}
