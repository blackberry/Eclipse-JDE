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
package net.rim.ejde.internal.ui.wizards.templates;

import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.ITemplateSection;

public class BlankLibraryNewWizard extends NewBBTemplateWizard {

    public static final String ID = "net.rim.ejde.libraryWizard";

    /**
     * Constructor for BlankLibraryNewWizard.
     */
    public BlankLibraryNewWizard() {
        super();
    }

    public void init( IFieldData data ) {
        super.init( data );
        setWindowTitle( "BlackBerry Library Application" );
    }

    /*
     * @see NewExtensionTemplateWizard#createTemplateSections()
     */
    public ITemplateSection[] createTemplateSections() {
        return new ITemplateSection[] { new BlankLibraryTemplate() };
    }
}
