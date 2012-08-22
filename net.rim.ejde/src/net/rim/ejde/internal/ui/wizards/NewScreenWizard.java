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

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.util.Messages;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class NewScreenWizard extends Wizard implements INewWizard {

    private static final Logger _log = Logger.getLogger( NewScreenWizard.class );

    private IWorkbench fWorkbench;
    private IStructuredSelection fSelection;
    private NewScreenWizardPage fPage;
    private boolean fOpenEditorOnFinish;

    public NewScreenWizard( NewScreenWizardPage page, boolean openEditorOnFinish ) {
        setDefaultPageImageDescriptor( ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID,
                "icons/wizban/new_bb_screen_wizard.png" ) );
        setDialogSettings( ContextManager.PLUGIN.getDialogSettings() );
        setWindowTitle( Messages.NewScreenWizard_title );
        setNeedsProgressMonitor( true );

        fPage = page;
        fOpenEditorOnFinish = openEditorOnFinish;
    }

    public NewScreenWizard() {
        this( null, true );
    }

    protected void openResource( final IFile resource ) {
        final IWorkbenchPage activePage = ContextManager.getActiveWorkbenchPage();
        if( activePage != null ) {
            final Display display = getShell().getDisplay();
            if( display != null ) {
                display.asyncExec( new Runnable() {
                    public void run() {
                        try {
                            IDE.openEditor( activePage, resource, true );
                        } catch( PartInitException e ) {
                            _log.error( e );
                        }
                    }
                } );
            }
        }
    }

    /**
     * Returns the scheduling rule for creating the element.
     *
     * @return returns the scheduling rule
     */
    protected ISchedulingRule getSchedulingRule() {
        return ResourcesPlugin.getWorkspace().getRoot(); // look all by default
    }

    protected void handleFinishException( Shell shell, InvocationTargetException e ) {
        String title = Messages.NewScreenWizard_op_error_title;
        String message = Messages.NewScreenWizard_op_error_message;
        ExceptionHandler.handle( e, shell, title, message );
    }

    /*
     * @see Wizard#performFinish
     */
    private boolean internalPerformFinish() {
        IWorkspaceRunnable op = new IWorkspaceRunnable() {
            public void run( IProgressMonitor monitor ) throws CoreException, OperationCanceledException {
                try {
                    finishPage( monitor );
                } catch( InterruptedException e ) {
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
            handleFinishException( getShell(), e );
            return false;
        } catch( InterruptedException e ) {
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
        fWorkbench = workbench;
        fSelection = currentSelection;
    }

    public IStructuredSelection getSelection() {
        return fSelection;
    }

    public IWorkbench getWorkbench() {
        return fWorkbench;
    }

    protected void selectAndReveal( IResource newResource ) {
        BasicNewResourceWizard.selectAndReveal( newResource, fWorkbench.getActiveWorkbenchWindow() );
    }

    /*
     * @see Wizard#createPages
     */
    public void addPages() {
        super.addPages();
        if( fPage == null ) {
            fPage = new NewScreenWizardPage();
            fPage.init( getSelection() );
        }
        addPage( fPage );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#canRunForked()
     */
    protected boolean canRunForked() {
        return !fPage.isEnclosingTypeSelected();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void finishPage( IProgressMonitor monitor ) throws InterruptedException, CoreException {
        fPage.createType( monitor ); // use the full progress monitor
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.IWizard#performFinish()
     */
    public boolean performFinish() {
        boolean res = internalPerformFinish();
        if( res ) {
            IResource resource = fPage.getModifiedResource();
            if( resource != null ) {
                selectAndReveal( resource );
                if( fOpenEditorOnFinish ) {
                    openResource( (IFile) resource );
                }
            }
        }
        return res;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#getCreatedElement()
     */
    public IJavaElement getCreatedElement() {
        return fPage.getCreatedType();
    }

}
