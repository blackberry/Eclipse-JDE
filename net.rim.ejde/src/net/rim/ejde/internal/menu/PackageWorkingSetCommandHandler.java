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

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.preferences.SignatureToolPreferences;
import net.rim.ejde.internal.packaging.PackagingJob;
import net.rim.ejde.internal.packaging.PackagingJobWrapper;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

public class PackageWorkingSetCommandHandler extends AbstractHandler {

    @Override
    public Object execute( ExecutionEvent event ) throws ExecutionException {
        IWorkingSet[] workingSets = getWorkingSet();
        HashSet< BlackBerryProject > projects = ProjectUtils.extractBBProjects( workingSets );
        PackagingJobWrapper job = new PackagingJobWrapper( Messages.PackagingJob_Name, projects,
                SignatureToolPreferences.getRunSignatureToolAutomatically() ? PackagingJob.SIGN_IF_NECESSARY
                        : PackagingJob.SIGN_NO );
        job.setUser( true );
        job.schedule();
        return null;
    }

    private IWorkingSet[] getWorkingSet() {
        IWorkbenchWindow window = ContextManager.getActiveWorkbenchWindow();
        IWorkingSetManager manager = window.getWorkbench().getWorkingSetManager();
        IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog( window.getShell(), false );
        dialog.open();
        IWorkingSet[] sets = dialog.getSelection();
        // check for cancel
        if( sets == null || sets.length == 0 ) {
            return null;
        }
        return sets;
    }
}
