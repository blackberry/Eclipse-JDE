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

import net.rim.ejde.internal.ui.views.profiler.ProfilerView;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ProfilerViewCommandHandler extends AbstractHandler {

    @Override
    public Object execute( ExecutionEvent event ) throws ExecutionException {
        try {
            execute();
        } catch( PartInitException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private void execute() throws PartInitException {
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView( ProfilerView.PROFILER_VIEW_ID );

    }
}
