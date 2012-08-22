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
package net.rim.ejde.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import net.rim.ejde.internal.util.Messages;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.IThreadListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public abstract class BlackBerryElementWizard extends Wizard implements INewWizard {
    static private final Logger _log = Logger.getLogger( BlackBerryElementWizard.class );
    private IWorkbench _workbench;
    private IStructuredSelection _selection;

    public BlackBerryElementWizard() {
        setNeedsProgressMonitor( true );
    }

    /**
     * Subclasses should override to perform the actions of the wizard. This method is run in the wizard container's context as a
     * workspace runnable.
     *
     * @param monitor
     * @throws InterruptedException
     * @throws CoreException
     */
    protected abstract void finishPage( IProgressMonitor monitor ) throws InterruptedException, CoreException;

    /**
     * Returns the scheduling rule for creating the element.
     *
     * @return returns the scheduling rule
     */
    protected ISchedulingRule getSchedulingRule() {
        return ResourcesPlugin.getWorkspace().getRoot(); // look all by default
    }

    protected boolean canRunForked() {
        return true;
    }

    public abstract IJavaElement getCreatedElement();

    protected void handleFinishException( Shell shell, InvocationTargetException e ) {
        String title = Messages.NewElementWizard_op_error_title;
        String message = Messages.NewElementWizard_op_error_message;
        handleException( e, shell, title, message );
    }

    protected void handleException( InvocationTargetException e, Shell shell, String title, String message ) {
        MessageDialog.openError( shell, title, message );
    }

    /*
     * @see Wizard#performFinish
     */
    public boolean performFinish() {
        IWorkspaceRunnable op = new IWorkspaceRunnable() {
            public void run( IProgressMonitor monitor ) throws CoreException, OperationCanceledException {
                try {
                    finishPage( monitor );
                } catch( InterruptedException e ) {
                    _log.error( e );
                    throw new OperationCanceledException( e.getMessage() );
                }
            }
        };
        try {
            ISchedulingRule rule = null;
            Job job = Job.getJobManager().currentJob();
            if( job != null )
                rule = job.getRule();
            IRunnableWithProgress runnable = null;
            if( rule != null )
                runnable = new WorkbenchRunnableAdapter( op, rule, true );
            else
                runnable = new WorkbenchRunnableAdapter( op, getSchedulingRule() );
            getContainer().run( canRunForked(), true, runnable );
        } catch( InvocationTargetException e ) {
            _log.error( e );
            handleFinishException( getShell(), e );
            return false;
        } catch( InterruptedException e ) {
            _log.error( e );
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init( IWorkbench workbench, IStructuredSelection currentSelection ) {
        _workbench = workbench;
        _selection = currentSelection;
    }

    public IStructuredSelection getSelection() {
        return _selection;
    }

    public IWorkbench getWorkbench() {
        return _workbench;
    }

    protected void selectAndReveal( IResource newResource ) {
        BasicNewResourceWizard.selectAndReveal( newResource, _workbench.getActiveWorkbenchWindow() );
    }

    class WorkbenchRunnableAdapter implements IRunnableWithProgress, IThreadListener {
        private IWorkspaceRunnable _runnable;
        private ISchedulingRule _rule;
        private boolean _transfer;

        /**
         * Runs a workspace runnable with the workspace lock.
         *
         * @param runnable
         *            the runnable
         */
        public WorkbenchRunnableAdapter( IWorkspaceRunnable runnable ) {
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
        public WorkbenchRunnableAdapter( IWorkspaceRunnable runnable, ISchedulingRule rule ) {
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
        public WorkbenchRunnableAdapter( IWorkspaceRunnable runnable, ISchedulingRule rule, boolean transfer ) {
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
}
