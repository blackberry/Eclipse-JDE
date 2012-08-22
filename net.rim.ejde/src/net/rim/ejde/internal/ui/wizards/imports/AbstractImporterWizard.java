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

import org.apache.log4j.Logger;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;

/**
 * @author cmalinescu
 *
 */
public abstract class AbstractImporterWizard extends Wizard implements IImportWizard {
    static private final Logger log = getLogger( AbstractImporterWizard.class );

    /**
	 *
	 */
    public AbstractImporterWizard() {
        if( log.isDebugEnabled() ) {
            log.debug( String.format( "Instance [%s] of [%s] created.", hashCode(), getClass() ) );
        }
    }
}
