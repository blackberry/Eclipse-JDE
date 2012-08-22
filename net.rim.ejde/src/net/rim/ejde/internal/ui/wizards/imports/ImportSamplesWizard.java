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
package net.rim.ejde.internal.ui.wizards.imports;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This wizard provides a convenient approach to import the BlackBerry Samples.
 *
 */
public class ImportSamplesWizard extends ImportLegacyProjectsWizard {
    static final private String ICON_PATH = "icons/wizban/import_bb_project_wizard.png";

    public ImportSamplesWizard() {
        setWindowTitle( Messages.ImportSampleProjects_SAMPLE_PROJECT_IMPORT ); //$NON-NLS-1$
        setDefaultPageImageDescriptor( AbstractUIPlugin.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, ICON_PATH ) );
    }

    public void addPages() {
        _selectionPage = new GenericSelectionPage( false );
        _selectionPage.setTitle( Messages.ImportSampleProjects_SAMPLE_PROJECT_IMPORT );
        addPage( _selectionPage );
    }
}
