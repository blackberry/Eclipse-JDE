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
package net.rim.ejde.internal.util;

import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * Contains utility methods for project natures
 *
 * @author bchabot
 */
public class NatureUtils {

    private static final Logger _logger = Logger.getLogger( NatureUtils.class );

    /**
     * Test if given IProject has given nature
     *
     * @param iproject
     * @param natureID
     * @return true if project has given nature
     */
    public static boolean testForNature( IProject iproject, String natureID ) {
        try {
            return iproject.isOpen() && iproject.getDescription().hasNature( natureID );
        } catch( CoreException e ) {
            _logger.info( "", e );
            return false;
        }
    }

    /**
     * Check if the givien project has BlackBerry nature.
     *
     * @param iproject
     *            The IProject
     * @return <code>true</code> if yes; otherwise <code>false</code>
     */
    public static boolean hasBBNature( IProject iproject ) {
        return testForNature( iproject, BlackBerryProjectCoreNature.NATURE_ID );
    }

}
