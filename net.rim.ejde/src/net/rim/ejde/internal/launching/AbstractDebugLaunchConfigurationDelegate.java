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
package net.rim.ejde.internal.launching;

import java.util.Set;

import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.packaging.PackagingJob;
import net.rim.ejde.internal.packaging.PackagingJobWrapper;
import net.rim.ejde.internal.ui.launchers.LaunchUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.StatusFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Abstract Launch delegate for a "debug only mode" launch
 *
 * @author bchabot
 */
public abstract class AbstractDebugLaunchConfigurationDelegate extends AbstractLaunchConfigurationDelegate {

    Logger _logger = Logger.getLogger( AbstractDebugLaunchConfigurationDelegate.class );

    public AbstractDebugLaunchConfigurationDelegate() {
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org. eclipse.debug.core.ILaunchConfiguration,
     * java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void launch( ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor )
            throws CoreException {

        try {
            if( mode.equals( "run" ) ) { //$NON-NLS-1$
                throw new CoreException( StatusFactory.createErrorStatus( Messages.DeviceLaunchConfigurationDelegate_runErrorMsg ) );
            } else if( mode.equals( "debug" ) ) { //$NON-NLS-1$
                debug( configuration, launch, monitor );
            }
        } catch( OperationCanceledException e ) {
            // dispose
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate2#buildForLaunch
     * (org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public boolean buildForLaunch( ILaunchConfiguration configuration, String mode, IProgressMonitor monitor )
            throws CoreException {
        // packaging projects
        packageProjects( configuration );
        return false;
    }

    /**
     * Deploy projects in the launch configuration into simulator folder. It packages the projects if necessary.
     *
     * @param configuration
     *            The launch configuration
     * @throws CoreException
     * @throws OperationCanceledException
     */
    protected void packageProjects( ILaunchConfiguration configuration ) throws CoreException, OperationCanceledException {
        String jobName = "Packaging " + configuration.getName(); //$NON-NLS-1$
        _logger.debug( jobName );
        Set< IProject > iProjects = LaunchUtils.getProjectsFromConfiguration( configuration );
        Set< BlackBerryProject > bbProjects = ProjectUtils.getBlackBerryProjects( iProjects );
        boolean needSign = LaunchUtils.getBooleanAttribute( configuration,
                IFledgeLaunchConstants.ATTR_GENERAL_ENABLE_DEVICE_SECURITY, false );
        PackagingJobWrapper packagingJob = new PackagingJobWrapper( jobName, bbProjects,
                needSign ? PackagingJob.SIGN_IF_NECESSARY : PackagingJob.SIGN_NO );
        packagingJob.setUser( true );
        packagingJob.schedule();
        try {
            packagingJob.join();
        } catch( InterruptedException e ) {
            _logger.error( e );
        }
    }

    /**
     * Deploy projects in the launch configuration into simulator folder.
     *
     * @param configuration
     *            The launch configuration
     * @param launch
     *            The new launch
     * @throws CoreException
     * @throws OperationCanceledException
     * @return The deployment status
     */
    protected IStatus deployProjects( ILaunchConfiguration configuration, IProgressMonitor monitor ) throws CoreException,
            OperationCanceledException {
        String taskName = "Deploying " + configuration.getName(); //$NON-NLS-1$
        Set< IProject > iProjects = LaunchUtils.getProjectsFromConfiguration( configuration );
        Set< BlackBerryProject > bbProjects = ProjectUtils.getBlackBerryProjects( iProjects );
        DeploymentTask deployTask = new DeploymentTask( taskName, bbProjects, configuration );
        return deployTask.run( monitor );
    }
}
