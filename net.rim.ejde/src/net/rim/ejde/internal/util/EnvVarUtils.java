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

import java.io.File;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ide.core.Util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;

/**
 * A utility class to support RIA environmental variables.
 *
 * @author Raj Gunaratnam, Cris Malinescu
 * @since 05 January, 09
 */
public class EnvVarUtils {
    static private final Logger log = Logger.getLogger( EnvVarUtils.class );

    /**
     * This method works as a wrapper method for net.rim.ide.Util.replaceEnvVars(...) method. For an example if any environmental
     * variable is undefined then this method will add a File.separator as prefix of the return value otherwise it will return the
     * proper substituted path String.
     *
     * @param path
     *            - String
     * @return result - String
     */
    public static String replaceRIAEnvVars( String path ) {
        String result = null;
        result = Util.replaceEnvVars( path );
        // The following block provides the fix for DPI222003
        if( result.startsWith( IConstants.DOLLAR_MARK ) ) {
            int index1 = result.indexOf( '(' );
            int index2 = result.indexOf( ')' );
            IPath rpath;
            String var;
            // This block provides support for eclipse ClassPath Variables
            if( index1 > 0 && index2 > index1
                    && ( rpath = JavaCore.getClasspathVariable( var = result.substring( index1 + 1, index2 ) ) ) != null ) {
                result = rpath.toOSString() + result.substring( index2 + 1 );
                Util.setEnvVar( var, rpath.toOSString() );
            } else {
                log.debug( result + " contains undefined environmental variable" );
                // org.eclipse.jdt.core.JavaCore.newLibraryEntry(...) requires
                // an absolute path otherwise it will assert with the following
                // message: Path for IClasspathEntry must be absolute.
                // Please note any path starts with $ is not absolute.
                result = File.separator + result;
            }
        }
        return result;
    }// end of method

    public static String resolveLegacyEnvVars( String path ) {
        return Util.replaceEnvVars( path );
    }

    static public String resolveEclipseEnvVars( String path ) {
        return resolveEclipseClasspathVar( path );
    }

    static public String resolveEclipseClasspathVar( String path ) {
        if( path.startsWith( IConstants.DOLLAR_MARK ) ) {
            int index1 = path.indexOf( '(' );
            int index2 = path.indexOf( ')' );

            if( index1 > 0 && index2 > index1 ) {
                String var = path.substring( index1 + 1, index2 );
                IPath rpath = JavaCore.getClasspathVariable( var );

                if( null != rpath ) {
                    path = rpath.toOSString() + path.substring( index2 + 1 );
                    Util.setEnvVar( var, rpath.toOSString() );
                }
            }
        }

        return path;
    }

    static public String resolveSystemVar( String var ) {
        String sysVar = System.getenv( var );

        if( StringUtils.isEmpty( sysVar ) )
            return var;

        return sysVar;
    }

    static public String resolveVarToString( String var ) {
        var = resolveEclipseEnvVars( var );

        if( var.startsWith( IConstants.DOLLAR_MARK ) ) {
            var = resolveLegacyEnvVars( var );
        }

        if( var.startsWith( IConstants.DOLLAR_MARK ) ) {
            var = resolveSystemVar( var );
        }

        return var;
    }

    static public IPath resolveVarToPath( String var ) {
        return new Path( var.substring( var.indexOf( '/' ) ) );
    }
}// end of class
