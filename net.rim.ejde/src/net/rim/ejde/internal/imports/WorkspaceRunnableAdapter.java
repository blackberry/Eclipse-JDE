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
package net.rim.ejde.internal.imports;

import java.lang.reflect.InvocationTargetException;

import net.rim.ejde.internal.ui.wizards.BlackBerryElementWizard;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.IThreadListener;

/**
 * This class is a help class to create a workspace runnable task which can batch workspace resource change events and broadcast
 * them after the job is done.
 *
 *
 */
public class WorkspaceRunnableAdapter implements IRunnableWithProgress, IThreadListener {
    static private final Logger _log = Logger.getLogger( BlackBerryElementWizard.class );

    private IWorkspaceRunnable _runnable;
    private ISchedulingRule _rule;
    private boolean _transfer;

    /**
     * Runs a workspace runnable with the workspace lock.
     *
     * @param runnable
     *            the runnable
     */
    public WorkspaceRunnableAdapter( IWorkspaceRunnable runnable ) {
        this( runnable, ResourcesPlugin.getWorkspace().getRoot() );
    }

    /**
     * Runs a workspace runnable with the given lock or <code>null</code> to run with no lock at all.
     *
     * @param runnable
     *            the runnable
     * @param rule
     *            the scheduling rule, or <code>null</code>
     */
    public WorkspaceRunnableAdapter( IWorkspaceRunnable runnable, ISchedulingRule rule ) {
        _runnable = runnable;
        _rule = rule;
    }

    /**
     * Runs a workspace runnable with the given lock or <code>null</code> to run with no lock at all.
     *
     * @param runnable
     *            the runnable
     * @param rule
     *            the scheduling rule, or <code>null</code>
     * @param transfer
     *            <code>true</code> if the rule is to be transfered to the modal context thread
     */
    public WorkspaceRunnableAdapter( IWorkspaceRunnable runnable, ISchedulingRule rule, boolean transfer ) {
        _runnable = runnable;
        _rule = rule;
        _transfer = transfer;
    }

    public void run( IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException {
        try {
            JavaCore.run( _runnable, _rule, monitor );
        } catch( OperationCanceledException e ) {
            _log.error( e );
            throw new InterruptedException( e.getMessage() );
        } catch( CoreException e ) {
            _log.error( e );
            throw new InvocationTargetException( e );
        }
    }

    public void threadChange( Thread thread ) {
        if( _transfer ) {
            Job.getJobManager().transferRule( _rule, thread );
        }
    }
}
