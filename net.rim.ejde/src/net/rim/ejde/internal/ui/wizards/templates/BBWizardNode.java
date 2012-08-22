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

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.jface.wizard.WizardSelectionPage;
import org.eclipse.pde.internal.ui.wizards.BaseWizardSelectionPage;
import org.eclipse.pde.ui.IBasePluginWizard;
import org.eclipse.swt.graphics.Point;

public abstract class BBWizardNode implements IWizardNode {

    static private final Logger _log = Logger.getLogger( BBWizardNode.class );
    private IWizard wizard;
    private WizardSelectionPage parentWizardPage;
    protected BBWizardElement wizardElement;

    public BBWizardNode( WizardSelectionPage parentPage, BBWizardElement element ) {
        parentWizardPage = parentPage;
        wizardElement = element;
    }

    protected abstract IBasePluginWizard createWizard() throws CoreException;

    public void dispose() {
        if( wizard != null ) {
            wizard.dispose();
            wizard = null;
        }
    }

    public BBWizardElement getElement() {
        return wizardElement;
    }

    public Point getExtent() {
        return new Point( -1, -1 );
    }

    public IWizard getWizard() {
        if( wizard != null )
            return wizard; // we've already created it

        IBasePluginWizard pluginWizard;
        try {
            pluginWizard = createWizard(); // create instance of target wizard
        } catch( CoreException e ) {
            if( parentWizardPage instanceof BaseWizardSelectionPage )
                ( (BaseWizardSelectionPage) parentWizardPage ).setDescriptionText( "" ); //$NON-NLS-1$
            _log.error( e );
            parentWizardPage.setErrorMessage( "Error..." );
            MessageDialog.openError( parentWizardPage.getWizard().getContainer().getShell(), "Error...", "Error..." );
            return null;
        }
        wizard = pluginWizard;
        // wizard.setUseContainerState(false);
        return wizard;
    }

    public boolean isContentCreated() {
        return wizard != null;
    }
}
