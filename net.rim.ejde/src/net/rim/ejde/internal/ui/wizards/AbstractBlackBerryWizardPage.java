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
package net.rim.ejde.internal.ui.wizards;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.layout.GridLayout;

abstract public class AbstractBlackBerryWizardPage extends WizardPage {

    static private final Logger log = Logger.getLogger( AbstractBlackBerryWizardPage.class );

    /**
     * Construct the wizard page.
     *
     * @param pageName
     *            The page name
     */
    protected AbstractBlackBerryWizardPage( String pageName ) {
        super( pageName );
        if( log.isDebugEnabled() ) {
            log.debug( String.format( "Instance [%s] of [%s] created.", hashCode(), getClass() ) );
        }
        ;
    }

    /**
     * Construct the wizard page.
     *
     * @param pageName
     *            The page name
     * @param title
     *            The page title
     * @param titleImage
     *            The title image
     */
    protected AbstractBlackBerryWizardPage( String pageName, String title, ImageDescriptor titleImage ) {
        super( pageName, title, titleImage );
        if( log.isDebugEnabled() ) {
            log.debug( String.format( "Instance [%s] of [%s] created.", hashCode(), getClass() ) );
        }
    }

    /**
     * Initialize the given grid layout.
     *
     * @param layout
     *            The grid layout
     * @param margins
     *            boolean indicating if the margin is required
     * @return The initialized <code>GridLayout</code>
     */
    public GridLayout initGridLayout( GridLayout layout, boolean margins ) {
        layout.horizontalSpacing = convertHorizontalDLUsToPixels( IDialogConstants.HORIZONTAL_SPACING );
        layout.verticalSpacing = convertVerticalDLUsToPixels( IDialogConstants.VERTICAL_SPACING );
        if( margins ) {
            layout.marginWidth = convertHorizontalDLUsToPixels( IDialogConstants.HORIZONTAL_MARGIN );
            layout.marginHeight = convertVerticalDLUsToPixels( IDialogConstants.VERTICAL_MARGIN );
        } else {
            layout.marginWidth = 0;
            layout.marginHeight = 0;
        }
        return layout;
    }

}
