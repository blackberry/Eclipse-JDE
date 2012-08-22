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
package net.rim.ejde.internal.menu;

import java.util.HashSet;
import java.util.Set;

import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.preferences.SignatureToolPreferences;
import net.rim.ejde.internal.packaging.PackagingJob;
import net.rim.ejde.internal.packaging.PackagingJobWrapper;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

public class PackageAllCommandHandler extends AbstractHandler {

    /**
     * Executes the command.
     *
     * @param event
     *            The execution event
     * @return The execution result
     * @throws ExecutionException
     *             if any error occurs during the command execution
     *
     * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
     */
    public Object execute( ExecutionEvent event ) throws ExecutionException {
        final Set< BlackBerryProject > projects = new HashSet< BlackBerryProject >();
        IProject[] projectArray = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        BlackBerryProject bbProject;
        for( int i = 0; i < projectArray.length; i++ ) {
            bbProject = ProjectUtils.createBBProject( projectArray[ i ] );
            if( bbProject != null ) {
                projects.add( bbProject );
            }
        }
        PackagingJobWrapper job = new PackagingJobWrapper( Messages.PackagingJob_Name, projects,
                SignatureToolPreferences.getRunSignatureToolAutomatically() ? PackagingJob.SIGN_IF_NECESSARY
                        : PackagingJob.SIGN_NO );
        job.setUser( true );
        job.schedule();
        return null;
    }

}
