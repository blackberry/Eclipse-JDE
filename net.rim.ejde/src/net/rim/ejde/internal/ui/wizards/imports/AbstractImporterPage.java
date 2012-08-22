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

import static org.apache.log4j.Logger.getLogger;
import net.rim.ejde.internal.ui.wizards.AbstractBlackBerryWizardPage;

import org.apache.log4j.Logger;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 *
 */
public abstract class AbstractImporterPage extends AbstractBlackBerryWizardPage {
    static private final Logger _log = getLogger( AbstractImporterPage.class );

    protected AbstractImporterPage() {
        this( "" );
    }

    /**
     * @param pageName
     */
    protected AbstractImporterPage( String pageName ) {
        super( pageName );
        if( _log.isDebugEnabled() ) {
            _log.debug( String.format( "Instance [%s] of [%s] created.", hashCode(), getClass() ) );
        }
    }

    /**
     * @param pageName
     * @param title
     * @param titleImage
     */
    protected AbstractImporterPage( String pageName, String title, ImageDescriptor titleImage ) {
        super( pageName, title, titleImage );
        if( _log.isDebugEnabled() ) {
            _log.debug( String.format( "Instance [%s] of [%s] created.", hashCode(), getClass() ) );
        }
        ;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets .Composite)
     */
    public void createControl( Composite parent ) {
        initializeDialogUnits( parent );
        final Composite composite = new Composite( parent, SWT.NULL );
        final GridLayout gridLayout = new GridLayout();
        composite.setLayout( gridLayout );
        composite.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        composite.setFont( parent.getFont() );
        setControl( composite );
        buildUI( composite );
    }

    protected abstract void buildUI( Composite parent );

    @Override
    protected void finalize() {
        if( _log.isDebugEnabled() ) {
            _log.debug( String.format( "Instance [%s] of [%s] finalized.", hashCode(), getClass() ) );
        }
    }
}
