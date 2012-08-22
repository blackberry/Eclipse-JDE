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

import net.rim.ejde.internal.ui.launchers.AbstractLaunchShortcut;
import net.rim.ejde.internal.ui.launchers.FledgeLaunchShortcut;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.handlers.HandlerUtil;

public class DebugConfigurationsCommandHandler extends AbstractHandler {

    @Override
    public Object execute( ExecutionEvent event ) throws ExecutionException {
        StructuredSelection selection = (StructuredSelection) HandlerUtil.getCurrentSelection( event );
        if( selection != null && !selection.isEmpty() ) {
            AbstractLaunchShortcut ls = new FledgeLaunchShortcut();
            ls.openLaunchConfiguration( selection, "debug" );
        } else {
            DebugUITools
                    .openLaunchConfigurationDialog( Display.getDefault().getShells()[ 0 ], StructuredSelection.EMPTY, "debug" );
        }
        return null;
    }
}
