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

import org.apache.log4j.Logger;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;

/**
 * This class is a utility for providing job synchronization services For some scenarios, the auto or manual buid job and the rapc
 * job are scheduled to run asynchronously, and our code must make sure the rapc job waits for the workspace build jobs to finish
 * before completed. The utilities defined in this class might be useful in the future features as well.
 *
 * @author dmateescu
 * @since December 2008
 *
 */
public class BuildSynchronizer {
    private static BuildSynchronizer _instance;
    private static final Logger _log = Logger.getLogger( BuildSynchronizer.class );

    /**
     * Private default constructor
     */
    private BuildSynchronizer() {
        super();
    }

    /**
     * Singleton getInstance() method
     *
     * @return the unique instance
     */
    public static BuildSynchronizer getInstance() {
        if( _instance == null ) {
            _instance = new BuildSynchronizer();
        }
        return _instance;
    }

    /**
     * This method waits for all build jobs (AUTO_BUILD, MANUAL_BUILD) to finish
     */
    public void waitForBuildJobs() {
        final IJobManager jobManager = Job.getJobManager();
        final IProgressMonitor monitor = new NullProgressMonitor();
        if( jobManager.find( ResourcesPlugin.FAMILY_AUTO_BUILD ).length > 0
                || jobManager.find( ResourcesPlugin.FAMILY_MANUAL_BUILD ).length > 0 ) {
            try {
                jobManager.join( ResourcesPlugin.FAMILY_MANUAL_BUILD, monitor );
                jobManager.join( ResourcesPlugin.FAMILY_AUTO_BUILD, monitor );
            } catch( InterruptedException e ) {
                _log.error( e.getMessage() );
            }
        }
    }

}
