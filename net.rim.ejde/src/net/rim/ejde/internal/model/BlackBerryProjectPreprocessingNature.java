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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class BlackBerryProjectPreprocessingNature implements IProjectNature {
    static final public String NATURE_ID = "net.rim.ejde.BlackBerryPreProcessNature"; //$NON-NLS-1$

    private IProject _eclipseProject;

    @Override
    public void configure() throws CoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deconfigure() throws CoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public IProject getProject() {
        return _eclipseProject;
    }

    @Override
    public void setProject( IProject eclipseProject ) {
        _eclipseProject = eclipseProject;

    }
}
