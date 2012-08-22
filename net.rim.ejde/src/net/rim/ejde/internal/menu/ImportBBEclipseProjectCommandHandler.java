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

import net.rim.ejde.internal.util.Messages;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.wizards.datatransfer.ExternalProjectImportWizard;

public class ImportBBEclipseProjectCommandHandler extends AbstractHandler {

    @Override
    public Object execute( ExecutionEvent event ) throws ExecutionException {
        execute();
        return null;
    }

    private void execute() {
        ExternalProjectImportWizard wizard = new ExternalProjectImportWizard();
        wizard.setWindowTitle( Messages.ImportExistingProject_EXISTING_PROJECT_WIZARD_LABEL );
        WizardDialog dialog = new WizardDialog( Display.getDefault().getShells()[ 0 ], wizard );
        dialog.open();
    }
}
