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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.texteditor.ISchedulingRuleProvider;

public abstract class WorkspaceOperation implements IRunnableWithProgress, ISchedulingRuleProvider {

    /**
     * The main functionality of this operation.
     *
     * @param monitor
     *            the progress monitor
     * @throws CoreException
     *             if the execution fails
     */
    protected abstract void execute( IProgressMonitor monitor ) throws CoreException;

    /*
     * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
     */
    public void run( IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException {
        try {
            execute( monitor );
        } catch( CoreException x ) {
            throw new InvocationTargetException( x );
        }
    }

    /*
     * @see org.eclipse.ui.texteditor.ISchedulingRuleProvider#getSchedulingRule()
     */
    public ISchedulingRule getSchedulingRule() {
        return ResourcesPlugin.getWorkspace().getRoot();
    }
}
