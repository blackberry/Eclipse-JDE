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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.rim.ejde.internal.launching.DeploymentTask;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.preferences.SignatureToolPreferences;
import net.rim.ejde.internal.packaging.InternalPackagingJob;
import net.rim.ejde.internal.packaging.PackagingJob;
import net.rim.ejde.internal.util.ProjectUtils;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Load projects on device command handler.
 *
 * @author dmeng
 *
 */
public class LoadProjectsOnDeviceCommandHandler extends AbstractHandler {

    /**
     * Deploy projects to device.
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
        ISelection selection = HandlerUtil.getCurrentSelection( event );
        if( selection != null && !selection.isEmpty() && selection instanceof StructuredSelection ) {
            IJavaProject[] javaProjects = ProjectUtils.getSelectProjects( (StructuredSelection) selection );
            if( javaProjects.length > 0 ) {
                Set< IProject > projects = new HashSet< IProject >();
                for( int i = 0; i < javaProjects.length; i++ ) {
                    projects.add( javaProjects[ i ].getProject() );
                }
                Set< BlackBerryProject > bbProjects = ProjectUtils.getBlackBerryProjects( projects );

                // package (if necessary) and deploy
                try {
                    PackagingJobWrapper packagingJob = new PackagingJobWrapper( "Loading projects...", bbProjects );
                    packagingJob.setUser( true );
                    packagingJob.schedule();
                } catch( Exception e ) {
                    throw new ExecutionException( "Loading projects on device failed.", e );
                }
            }
        }
        return null;
    }

    private static class DeploymentJob implements IWorkspaceRunnable {

        private Collection< BlackBerryProject > _projects;

        public DeploymentJob( Collection< BlackBerryProject > projects ) {
            _projects = projects;
        }

        @Override
        public void run( IProgressMonitor monitor ) throws CoreException {
            DeploymentTask.loadProjectsToDevice( _projects, monitor );
        }
    }

    private static class PackagingJobWrapper extends WorkspaceJob {

        private Set< BlackBerryProject > _projects;

        public PackagingJobWrapper( String name, Set< BlackBerryProject > projects ) {
            super( name );
            _projects = projects;
        }

        @Override
        public IStatus runInWorkspace( IProgressMonitor monitor ) throws CoreException {
            boolean needSign = SignatureToolPreferences.getRunSignatureToolAutomatically();
            PackagingJob packagingJob = new InternalPackagingJob( _projects, needSign ? PackagingJob.SIGN_IF_NECESSARY
                    : PackagingJob.SIGN_NO );

            ResourcesPlugin.getWorkspace().run( packagingJob, ResourcesPlugin.getWorkspace().getRuleFactory().buildRule(),
                    IResource.NONE, monitor );

            DeploymentJob deployingJob = new DeploymentJob( _projects );
            ResourcesPlugin.getWorkspace().run( deployingJob, ResourcesPlugin.getWorkspace().getRuleFactory().buildRule(),
                    IResource.NONE, monitor );
            return Status.OK_STATUS;
        }
    }
}
