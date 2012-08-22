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

import org.osgi.framework.Version;

/**
 *
 * RIMVersionComparator is used to compare 2 version objects. We treat the fourth digit in version number as integer instead of
 * string
 *
 * @author jluo
 *
 */
public class RIMVersionComparator {
    public static final int VERSION_MAJOR = 1;
    public static final int VERSION_MAJOR_MINOR = 2;
    public static final int VERSION_MAJOR_MINOR_SERVICE = 3;
    public static final int VERSION_ALL = 4;

    private static RIMVersionComparator _instance = null;

    /**
     *
     * @return the RIMVersionComparator object
     */
    public static RIMVersionComparator getInstance() {
        if( _instance == null ) {
            _instance = new RIMVersionComparator();
        }

        return _instance;
    }

    /***
     *
     * @param version1
     *            the first version
     * @param version2
     *            the second version
     * @param level
     *            comparison level
     *
     * @return A negative integer, zero, or a positive integer if version1 is less than, equal to, or greater than version2
     *
     */
    public int compare( Version version1, Version version2, int level ) {
        if( version1 == version2 ) { // quicktest
            return 0;
        }

        int result = version1.getMajor() - version2.getMajor();
        if( ( level == VERSION_MAJOR ) || ( result != 0 ) ) {
            return result;
        }

        result = version1.getMinor() - version2.getMinor();
        if( ( level == VERSION_MAJOR_MINOR ) || ( result != 0 ) ) {
            return result;
        }

        result = version1.getMicro() - version2.getMicro();
        if( ( level == VERSION_MAJOR_MINOR_SERVICE ) || ( result != 0 ) ) {
            return result;
        }

        return parseInteger( version1.getQualifier().trim() ) - parseInteger( version2.getQualifier().trim() );

    }

    private int parseInteger( String intStr ) {
        int result = 0;

        try {
            result = Integer.parseInt( intStr );
        } catch( NumberFormatException ex ) {
            // do nothing
        }

        return result;
    }

}
