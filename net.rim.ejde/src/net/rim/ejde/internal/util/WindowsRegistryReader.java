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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * A utility class provides static methods to read windows Registry.
 *
 */
public class WindowsRegistryReader {

    private static Logger log = Logger.getLogger( WindowsRegistryReader.class );
    private static final String REGQUERY_UTIL = "reg query ";
    private static final String REGSTR_TOKEN = "REG_SZ";

    private static final String JDE_INSTALL_CMD = REGQUERY_UTIL
            + "\"HKLM\\SOFTWARE\\Research In Motion\\BlackBerry Handheld JDE\"";
    private static final String JDE_COMPONENT_INSTALL_CMD = REGQUERY_UTIL
            + "\"HKLM\\SOFTWARE\\Research In Motion\\BlackBerry JDE Components\"";
    private static final String JDK_INSTALL_CMD = REGQUERY_UTIL + "\"HKLM\\SOFTWARE\\JavaSoft\\Java Development Kit\"";

    private static final String VC2008_RUNTIME_CMD = REGQUERY_UTIL
            + "\"HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\{FF66E9F6-83E7-3A3E-AF14-8DE9A809A6A4}\"";

    private static final String JDE_INSTALL_DIR_CMD = " /v InstallDir";
    private static final String JDK_JAVA_HOME_DIR_CMD = " /v JavaHome";
    private static final String VC2008_DISPLAYNAME_CMD = " /v DisplayName";

    private Map< String, CompatibilityVersion > installedJDEMap;
    private Map< String, CompatibilityVersion > installedJDKMap;

    public WindowsRegistryReader() {
        installedJDKMap = getInstalledJDKMap();
    }

    /**
     * Returns the installed JDE paths from the Windows Registry.
     *
     * @return - Windows Registry JDE paths.
     */
    public static List< String > getInstalledJDEPaths() {
        List< String > paths = new ArrayList< String >();

        List< String > registryPaths = getRegistryPaths( JDE_INSTALL_CMD );
        for( String regPath : registryPaths ) {
            String pathToAdd = getInstalledDir( regPath );
            if( !paths.contains( pathToAdd ) )
                paths.add( pathToAdd );
        }

        return paths;
    }

    /**
     * Returns the installed JDE Components paths from the Windows Registry.
     *
     * @return - Windows Registry JDE Components paths.
     */
    public static List< String > getInstalledJDEComponentsPaths() {
        List< String > paths = new ArrayList< String >();

        List< String > registryPaths = getRegistryPaths( JDE_COMPONENT_INSTALL_CMD );
        for( String regPath : registryPaths ) {
            String pathToAdd = getInstalledDir( regPath );
            if( !paths.contains( pathToAdd ) )
                paths.add( pathToAdd );
        }

        return paths;
    }

    /**
     * Returns the installed JDK paths from the Windows Registry.
     *
     * @return - Windows Registry JDK paths.
     */
    public static List< String > getInstalledJDKPaths() {
        List< String > paths = new ArrayList< String >();

        List< String > registryPaths = getRegistryPaths( JDK_INSTALL_CMD );
        for( String regPath : registryPaths ) {

            String pathToAdd = getJavaHomeDir( regPath );
            if( !paths.contains( pathToAdd ) )
                paths.add( pathToAdd );
        }

        return paths;
    }

    /**
     * Returns true if VC2008 Runtime present in the Windows Registry.
     *
     * @return boolean
     */
    public static boolean isVC2008RuntimeInstalled() {
        String displayName = getRegistryPathString( VC2008_RUNTIME_CMD + VC2008_DISPLAYNAME_CMD );
        if( displayName.indexOf( REGSTR_TOKEN ) != -1 ) {
            return true;
        }
        return false;
    }

    private static List< String > getRegistryPaths( String key ) {
        List< String > registryPaths = new ArrayList< String >();
        String paths = getRegistryPathString( key );
        String parsedPaths[] = paths.split( "\r\n" );
        for( int i = 0; i < parsedPaths.length; i++ ) {

            IPath check = new Path( parsedPaths[ i ] );
            /*
             * This check is to support reading in both XP and Vista. XP returns reg.exe version and root folder and paths as well
             * as empty strings and Vista does not return any of these, simply returns the paths.
             */
            if( check.segmentCount() >= 5 ) {
                registryPaths.add( parsedPaths[ i ] );
            }
        }
        return registryPaths;
    }

    private static String getInstalledDir( String regPath ) {

        String result = getRegistryPathString( REGQUERY_UTIL + "\"" + regPath + "\"" + JDE_INSTALL_DIR_CMD );
        int index = result.indexOf( REGSTR_TOKEN );

        if( index == -1 )
            return null;

        return result.substring( index + REGSTR_TOKEN.length() ).trim();

    }

    private static String getJavaHomeDir( String regPath ) {

        String result = getRegistryPathString( REGQUERY_UTIL + "\"" + regPath + "\"" + JDK_JAVA_HOME_DIR_CMD );
        int index = result.indexOf( REGSTR_TOKEN );

        if( index == -1 )
            return null;

        return result.substring( index + REGSTR_TOKEN.length() ).trim();

    }

    private static String getRegistryPathString( String path ) {
        try {
            Process process = Runtime.getRuntime().exec( path );
            StreamReader reader = new StreamReader( process.getInputStream() );

            reader.start();
            process.waitFor();
            reader.join();

            String result = reader.getResult();
            return result;

        } catch( Exception e ) {

            log.error( "Unable to Read Windows Registry." );
            return null;
        }
    }

    /*
     * Private class to read inputStream.
     */
    static class StreamReader extends Thread {
        private InputStream is;
        private StringWriter sw;

        StreamReader( InputStream is ) {
            this.is = is;
            sw = new StringWriter();
        }

        public void run() {
            try {
                int c;
                while( ( c = is.read() ) != -1 )
                    sw.write( c );
            } catch( IOException e ) {
                log.error( "Stream reaser failed to read from Windows Registry" );
            }
        }

        String getResult() {
            return sw.toString();
        }
    }

    public static Map< String, CompatibilityVersion > getInstalledJDKMap() {
        Map< String, CompatibilityVersion > paths = new HashMap< String, CompatibilityVersion >();

        List< String > registryPaths = getRegistryPaths( JDK_INSTALL_CMD );
        for( String regPath : registryPaths ) {

            String pathToAdd = getJavaHomeDir( regPath );
            IPath path = new Path( regPath );
            try {
                CompatibilityVersion compVer = new CompatibilityVersion( path.lastSegment() );
                paths.put( pathToAdd, compVer );

            } catch( IllegalArgumentException e ) {
                log.error( "Invalid version format read from Registry", e );
            }
        }

        return paths;
    }

    public CompatibilityVersion getJDEVersionFromPath( String JDEpath ) {

        // Assuming map = Map<String, String>
        for( Iterator< Map.Entry< String, CompatibilityVersion >> iter = installedJDEMap.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry< String, CompatibilityVersion > entry = iter.next();
            String key = entry.getKey();
            CompatibilityVersion value = entry.getValue();
            if( key.equals( JDEpath ) ) {
                return value;
            }
        }
        return null;

    }

    public CompatibilityVersion getJDKVersionFromPath( String JDKpath ) {
        // Assuming map = Map<String, String>
        for( Iterator< Map.Entry< String, CompatibilityVersion >> iter = installedJDKMap.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry< String, CompatibilityVersion > entry = iter.next();
            String key = entry.getKey();
            CompatibilityVersion value = entry.getValue();
            if( key.equals( JDKpath ) ) {
                return value;
            }
        }
        return null;

    }
}
