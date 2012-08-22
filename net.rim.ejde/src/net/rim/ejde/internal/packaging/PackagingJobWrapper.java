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
package net.rim.ejde.internal.packaging;

import java.util.Set;

import net.rim.ejde.internal.model.BlackBerryProject;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * This class is a wrapper class of the {@link PackagingJob}. Users should use this class to do packaging instead of using the
 * {@link PackagingJob} class directly.
 *
 */
public class PackagingJobWrapper extends WorkspaceJob {

    private Set< BlackBerryProject > _projects;
    private int _signingFlag;

    /**
     * Constructs a PackagingJob instance.
     *
     * @param name
     */
    private PackagingJobWrapper( String name ) {
        super( name );
    }

    /**
     * Constructs a PackagingJob instance.
     *
     * @param name
     * @param projects
     *            projects need to be packaged.
     */
    public PackagingJobWrapper( String name, Set< BlackBerryProject > projects ) {
        this( name );
        _projects = projects;
        _signingFlag = PackagingJob.SIGN_NO;
    }

    public PackagingJobWrapper( String name, Set< BlackBerryProject > projects, int signingFlag ) {
        this( name );
        _projects = projects;
        _signingFlag = signingFlag;
    }

    /**
     * Sets the signing flag.
     *
     * @see PackagingJob#SIGN_FORCE
     * @see PackagingJob#SIGN_IF_NECESSARY
     * @see PackagingJob#SIGN_NO
     *
     * @param signingFlag
     */
    public void setForceSigning( int signingFlag ) {
        _signingFlag = signingFlag;
    }

    @Override
    public IStatus runInWorkspace( IProgressMonitor monitor ) throws CoreException {
        // wait until all other eclipse builders have finished
        BuildSynchronizer.getInstance().waitForBuildJobs();
        PackagingJob packagingJob = new InternalPackagingJob( _projects, _signingFlag );
        ResourcesPlugin.getWorkspace().run( packagingJob, ResourcesPlugin.getWorkspace().getRuleFactory().buildRule(),
                IResource.NONE, monitor );
        return Status.OK_STATUS;
    }
}
