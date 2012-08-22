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
package net.rim.ejde.internal.ui.dialogs;

import net.rim.ejde.internal.util.Messages;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.ide.dialogs.CleanDialog;

/**
 * This class is used to display a clean projects dialog after project/workspace level preprocess tags have been changed.
 *
 */
public class CleanProjectsDialog extends CleanDialog {

    public CleanProjectsDialog( IWorkbenchWindow window, IProject[] selection, String msg ) {
        super( window, selection );
        this.message = getMessage( msg );
    }

    private String getMessage( String msg ) {
        String message = NLS.bind( Messages.CleanProjectsDialog_description, msg );
        message += "\n\n";
        boolean autoBuilding = ResourcesPlugin.getWorkspace().isAutoBuilding();
        if( autoBuilding ) {
            message += Messages.CleanProjectsDialog_buildCleanAuto;
        } else {
            message += Messages.CleanProjectsDialog_buildCleanManual;
        }
        return message;
    }
}
