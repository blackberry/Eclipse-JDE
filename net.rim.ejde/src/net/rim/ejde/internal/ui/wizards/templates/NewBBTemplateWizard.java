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

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.ui.templates.ITemplateSection;
import org.eclipse.pde.ui.templates.NewPluginTemplateWizard;

public class NewBBTemplateWizard extends NewPluginTemplateWizard {

    @Override
    public ITemplateSection[] createTemplateSections() {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean performCancel() {
        BBFieldData data = (BBFieldData) getData();
        IWizard masterWizard = data.getMasterWizard();
        return masterWizard.performCancel();
    }
}
