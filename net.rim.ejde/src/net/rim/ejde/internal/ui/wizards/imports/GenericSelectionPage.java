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

import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;
import net.rim.ejde.internal.util.Messages;

/**
 * This class provide the UI which allows users to select projects from a legacy workspace file and import them.
 *
 */
@InternalFragmentReplaceable
public class GenericSelectionPage extends BasicGenericSelectionPage {

    public GenericSelectionPage( boolean generalImport ) {
        super( generalImport );
        super.setTitle( Messages.ImportExistingProject_EXISTING_PROJECT_WIZARD_LABEL );
    }

}
