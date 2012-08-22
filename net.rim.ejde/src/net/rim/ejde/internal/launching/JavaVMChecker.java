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
package net.rim.ejde.internal.launching;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.MessageFormat;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.util.CompatibilityVersion;

public class JavaVMChecker {
    public static final String JAVA_EXE_NAME = IConstants.JAVA_CMD;

    // equivalent: Major=, Minor=
    public static final String CHECK_TYPE_EQUIVALENT = "equivalent";
    // greaterOrEqual: Major >=, Minor >=
    public static final String CHECK_TYPE_GREATEROREQUAL = "greaterOrEqual";

    private static JavaVMChecker _instance = null;

    public static JavaVMChecker getInstance() {
        if( _instance == null ) {
            _instance = new JavaVMChecker();
        }

        return _instance;
    }

    /***
     *
     * @param javaVersionStr
     *            the specified java version string
     * @param checkType
     *            the type of check
     * @return null if no such java can meet the specified criteria.
     */
    public JavaVMCheckResult checkJavaVM( String javaVersionStr, String checkType ) {
        return checkJavaVM( new CompatibilityVersion( javaVersionStr ), checkType );
    }

    /***
     *
     * @param javaVersion
     *            the specified java version
     * @param checkType
     *            the type of check
     * @return null if no such java can meet the specified criteria.
     */
    public JavaVMCheckResult checkJavaVM( CompatibilityVersion javaVersion, String checkType ) {
        JavaVMCheckResult result = null;

        // check JAVA_HOME environment variable
        result = checkEnvJavaHome( javaVersion, checkType );

        // check path environment variable
        if( result == null ) {
            result = checkEnvPath( javaVersion, checkType );
        }

        // check Windows Registry
        if( result == null ) {
            result = checkWinRegistry( javaVersion, checkType );
        }

        return result;
    }

    private JavaVMCheckResult checkEnvJavaHome( CompatibilityVersion javaVersion, String checkType ) {
        JavaVMCheckResult result = null;

        String javaHomeDir = System.getenv( "JAVA_HOME" );
        if( javaHomeDir != null ) {
            javaHomeDir = javaHomeDir.trim();
            if( javaHomeDir.length() > 0 ) {
                String javaExePath = javaHomeDir + File.separator + "bin" + File.separator + JAVA_EXE_NAME;
                result = checkJavaVersion( JavaVMCheckResult.LOCATION_ENV_JAVA_HOME, javaHomeDir, javaExePath, javaVersion,
                        checkType );
            }
        }

        return result;
    }

    private JavaVMCheckResult checkEnvPath( CompatibilityVersion javaVersion, String checkType ) {
        String javaHomeDir = "";
        String javaExePath = JAVA_EXE_NAME;

        return checkJavaVersion( JavaVMCheckResult.LOCATION_ENV_PATH, javaHomeDir, javaExePath, javaVersion, checkType );
    }

    private JavaVMCheckResult checkJavaVersion( String locationType, String javaHomeDir, String javaExePath,
            CompatibilityVersion specifiedVersion, String checkType ) {
        JavaVMCheckResult result = null;

        CompatibilityVersion javaCurrentVersion = getJavaExeVersion( javaExePath );

        if( javaCurrentVersion != null ) {
            if( checkType.equalsIgnoreCase( CHECK_TYPE_EQUIVALENT ) ) {
                if( isEquivalent( javaCurrentVersion, specifiedVersion ) ) {
                    result = new JavaVMCheckResult( locationType, javaCurrentVersion, javaHomeDir );
                }

            } else if( checkType.equalsIgnoreCase( CHECK_TYPE_GREATEROREQUAL ) ) {
                if( isGreaterOrEqual( javaCurrentVersion, specifiedVersion ) ) {
                    result = new JavaVMCheckResult( locationType, javaCurrentVersion, javaHomeDir );
                }
            }
        }

        return result;
    }

    private boolean isEquivalent( CompatibilityVersion version1, CompatibilityVersion version2 ) {
        return ( version1.getMajorVersion() == version2.getMajorVersion() )
                && ( version1.getMinorVersion() == version2.getMinorVersion() );
    }

    private boolean isGreaterOrEqual( CompatibilityVersion version1, CompatibilityVersion version2 ) {
        if( version1.getMajorVersion() > version2.getMajorVersion() ) {
            return true;
        }

        if( version1.getMajorVersion() < version2.getMajorVersion() ) {
            return false;
        }

        // since major is equal now, we have to compare the minor
        return ( version1.getMinorVersion() >= version2.getMinorVersion() );
    }

    /**
     *
     * @param javaExePath
     * @return return null if java.exe can not be found in passed path
     */
    protected CompatibilityVersion getJavaExeVersion( String javaExePath ) {
        CompatibilityVersion result = null;

        try {
            String command = javaExePath + " -version";
            Process process = Runtime.getRuntime().exec( command );

            // Note: the output of command "java -version" is placed in error
            // stream
            BufferedReader reader = new BufferedReader( new InputStreamReader( process.getErrorStream() ) );
            String versionLine = null;
            String tmpStr = null;
            while( ( tmpStr = reader.readLine() ) != null ) {
                if( tmpStr.startsWith( "java version" ) ) {
                    // find the correct line
                    versionLine = tmpStr;
                    break;
                }
            }
            process.waitFor();

            if( versionLine != null ) {
                System.out.println( "version line:" + versionLine );
                MessageFormat messageFormat = new MessageFormat( "java version \"{0}\"" );
                Object[] values = messageFormat.parse( versionLine );
                if( values.length > 0 ) {
                    result = new CompatibilityVersion( (String) values[ 0 ] );
                }
            }

        } catch( Exception ex ) {

        }

        return result;
    }

    private JavaVMCheckResult checkWinRegistry( CompatibilityVersion javaVersion, String checkType ) {
        JavaVMCheckResult result = null;

        // Map< String, CompatibilityVersion > installedJDKMap = WindowsRegistryReader.getInstalledJDKMap();
        // for( Iterator iter = installedJDKMap.entrySet().iterator(); iter.hasNext(); ) {
        // Map.Entry entry = (Map.Entry) iter.next();
        // String javaHomeDir = (String) entry.getKey();
        // CompatibilityVersion javaCurrentVersion = (CompatibilityVersion) entry.getValue();
        //
        // if( checkType.equalsIgnoreCase( CHECK_TYPE_EQUIVALENT ) ) {
        // if( isEquivalent( javaCurrentVersion, javaVersion ) ) {
        // result = new JavaVMCheckResult( JavaVMCheckResult.LOCATION_WINREGISTRY, javaCurrentVersion, javaHomeDir );
        // break; // break for statement
        // }
        // } else if( checkType.equalsIgnoreCase( CHECK_TYPE_GREATEROREQUAL ) ) {
        // if( isGreaterOrEqual( javaCurrentVersion, javaVersion ) ) {
        // result = new JavaVMCheckResult( JavaVMCheckResult.LOCATION_WINREGISTRY, javaCurrentVersion, javaHomeDir );
        // break; // break for statement
        // }
        // }
        // }

        return result;
    }

    public static void main( String[] args ) {
        try {

            JavaVMChecker checker = JavaVMChecker.getInstance();

            // checker.getMDSCSVersion();

            String path = "C:\\jdk1.6.0\\bin\\java.exe";
            CompatibilityVersion version = checker.getJavaExeVersion( path );

            path = "C:\\jdk1.7.0\\bin\\java.exe";
            version = checker.getJavaExeVersion( path );

            path = "java.exe";
            version = checker.getJavaExeVersion( path );

            path = "C:\\jdk1.5.0_06\\bin\\java.exe";
            version = checker.getJavaExeVersion( path );

            System.out.println( "ok" );
        } catch( Exception ex ) {
            System.out.println( ex.getMessage() );
        }
    }

    public static class JavaVMCheckResult {
        public static final String LOCATION_ENV_JAVA_HOME = "JAVA_HOME";
        public static final String LOCATION_ENV_PATH = "Path";
        public static final String LOCATION_WINREGISTRY = "winregistry";

        private String _locationType;
        private CompatibilityVersion _javaVersion;
        private String _javaHomeDir;

        public JavaVMCheckResult( String locationType, CompatibilityVersion javaVersion, String javaHomeDir ) {
            _locationType = locationType;
            _javaVersion = javaVersion;
            _javaHomeDir = javaHomeDir;
        }

        public String getLocationType() {
            return _locationType;
        }

        public CompatibilityVersion getJavaVersion() {
            return _javaVersion;
        }

        public String getJavaHomeDir() {
            return _javaHomeDir;
        }
    }

}
