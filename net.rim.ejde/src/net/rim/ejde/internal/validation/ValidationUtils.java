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
package net.rim.ejde.internal.validation;

import java.util.regex.Pattern;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BlackBerryProject;

import org.eclipse.core.resources.IResource;

public class ValidationUtils {

    /**
     * Determines if the change of the given eclipse resource need to make the project to be validated.
     *
     * @param res
     * @return
     */
    public static boolean needToBeValidated( IResource res ) {
        if( res.getType() == IResource.FILE ) {
            if( BlackBerryProject.METAFILE.equalsIgnoreCase( res.getFullPath().lastSegment() ) ) {
                return true;
            }
            if( res.getFullPath().getFileExtension().equals( IConstants.RRH_FILE_EXTENSION ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns if the given string is a positive integer.
     *
     * @param txt
     *            The given string
     * @return <code>true</code> if yes; otherwise <code>false</code>
     */
    public static boolean isPostiveInteger( String txt ) {
        if( !Pattern.matches( "[0-9]+", txt ) ) {
            return false;
        }
        return true;
    }

    /**
     * Returns if the given resource is BlackBerry resource file (i.e .rrh or .rrc)
     *
     * @param res
     *            The resource
     * @return <code>true</code> if yes; otherwise <code>false</code>
     */
    public static boolean isResourceFile( IResource res ) {
        if( res.getType() == IResource.FILE ) {
            if( res.getFileExtension().equals( IConstants.RRH_FILE_EXTENSION )
                    || res.getFileExtension().equals( IConstants.RRC_FILE_EXTENSION ) ) {
                return true;
            }
        }
        return false;
    }

}
