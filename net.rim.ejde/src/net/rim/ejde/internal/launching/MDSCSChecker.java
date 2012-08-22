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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.rim.ejde.internal.launching.JavaVMChecker.JavaVMCheckResult;
import net.rim.ejde.internal.util.CompatibilityVersion;
import net.rim.ejde.internal.util.Messages;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.tree.ClassNode;

/**
 * A utility class to check MDS-CS version and Java version, and determine if the MDS-CS can be launched. It also updates the
 * MDS-CS launch batch file.
 *
 * @author dmeng
 *
 */
public class MDSCSChecker {

    // OK: launch MDS-CS without problem
    // CANCEL: stop launching
    // DISABLE_MDSCS: launching with disabled MDS-CS
    public static enum MDSCSCheckResult {
        OK, CANCEL, DISABLE_MDSCS
    }

    private static Logger _logger = Logger.getLogger( MDSCSChecker.class );
    private static int _userDecision = -1;
    private static String _currentVersion;

    /***
     * Check if required version of Java can be found for launching MDS-CS
     *
     * @param MDSHomePath
     *            The MDS-CS home path
     * @return <code>true</code> continue this launching session, otherwise <code>false</code>.
     */
    public static MDSCSCheckResult checkMDSCS( File MDSHomePath ) {
        String dialogTitle = "", dialogMessage = ""; //$NON-NLS-1$
        JavaVMCheckResult javaVMResult = null;

        // Get version of current MDS-CS
        CompatibilityVersion mdscsVersion = getMDSCSVersion( MDSHomePath );
        // if MDS-CS version is null, warn user that MDS-CS can not be found
        if( mdscsVersion == null ) {
            dialogTitle = Messages.FledgeLaunchConfigurationDelegate_noMDSCSMsg;
        } else {
            // if versions of MDS-CS is equal or greater than 4.1.5, Java 1.6 is
            // required
            CompatibilityVersion mdscs415Version = new CompatibilityVersion( "4.1.5" ); //$NON-NLS-1$
            String requiredJavaVersion;
            if( mdscsVersion.compareTo( mdscs415Version ) >= 0 ) {
                requiredJavaVersion = "1.6"; //$NON-NLS-1$
            } else {
                requiredJavaVersion = "1.5"; //$NON-NLS-1$
            }
            javaVMResult = JavaVMChecker.getInstance().checkJavaVM( requiredJavaVersion, JavaVMChecker.CHECK_TYPE_GREATEROREQUAL ); //$NON-NLS-1$
            if( javaVMResult == null ) {
                // required version of Java can not be found in system.
                dialogTitle = Messages.FledgeLaunchConfigurationDelegate_noLaunchMDSCSMsg;
                dialogMessage = NLS.bind( Messages.FledgeLaunchConfigurationDelegate_noJavaMDSCSMsg, requiredJavaVersion );
            }
        }

        if( javaVMResult == null ) {
            // either MDS-CS is not found or required Java to run MDS-CS is not
            // found
            dialogMessage = dialogMessage
                    + NLS.bind( Messages.FledgeLaunchConfigurationDelegate_clickNoMDSCSMsg, IDialogConstants.PROCEED_LABEL,
                            IDialogConstants.STOP_LABEL );
            askUserDecision( dialogTitle, dialogMessage );
            if( _userDecision == 0 ) { // continue launching
                return MDSCSCheckResult.DISABLE_MDSCS;
            } else {
                return MDSCSCheckResult.CANCEL;
            }
        }

        // required version of Java has been found
        if( javaVMResult.getLocationType() != JavaVMCheckResult.LOCATION_ENV_JAVA_HOME ) {
            if( !isJavaHomeEnvEmpty() || ( javaVMResult.getLocationType() != JavaVMCheckResult.LOCATION_ENV_PATH ) ) {
                updateBatchFiles( MDSHomePath, javaVMResult );
            }
        }

        return MDSCSCheckResult.OK;
    }

    private static boolean isJavaHomeEnvEmpty() {
        String javaHomeDir = System.getenv( "JAVA_HOME" ); //$NON-NLS-1$
        if( javaHomeDir != null ) {
            javaHomeDir = javaHomeDir.trim();
        }

        return ( javaHomeDir == null ) || ( javaHomeDir.length() == 0 );
    }

    private static void updateBatchFiles( File MDSHome, JavaVMCheckResult javaVMResult ) {
        try {
            String javaExePath;
            if( javaVMResult.getLocationType() == JavaVMCheckResult.LOCATION_ENV_PATH ) {
                // required Java is found in path environment variable.
                javaExePath = "java.exe"; //$NON-NLS-1$
            } else {
                // required Java must be found in window registry
                javaExePath = javaVMResult.getJavaHomeDir() + "\\bin\\java.exe"; //$NON-NLS-1$
            }

            // for run.bat
            String runBatFilePath = MDSHome.getCanonicalPath() + "\\run.bat"; //$NON-NLS-1$
            backup( runBatFilePath );
            PrintWriter runBatWriter = new PrintWriter( new FileOutputStream( runBatFilePath ) );
            runBatWriter.println( "@ECHO  OFF" ); //$NON-NLS-1$
            runBatWriter.println( "call setBMDSEnv" ); //$NON-NLS-1$
            runBatWriter
                    .println( "start cmd /v:on /c \"" //$NON-NLS-1$
                            + javaExePath
                            + "\" -classpath !BMDS_CLASSPATH!;!BMDS_CLASSPATH2! -Xmx512M -Djava.endorsed.dirs=classpath\\endorsed -DKeystore.Password=password net.rim.application.ipproxyservice.IPProxyServiceApplication -log.console.dump" ); //$NON-NLS-1$
            runBatWriter.println( ":END" ); //$NON-NLS-1$
            runBatWriter.flush();
            runBatWriter.close();

            // for event.bat
            String eventBatFilePath = MDSHome.getCanonicalPath() + "\\event.bat"; //$NON-NLS-1$
            backup( eventBatFilePath );
            PrintWriter eventBatWriter = new PrintWriter( new FileOutputStream( eventBatFilePath ) );
            eventBatWriter.println( "@ECHO  OFF" ); //$NON-NLS-1$
            eventBatWriter.println( "IF /I NOT [%BMDS_ENV_SET%] == [true]  call setBMDSEnv" ); //$NON-NLS-1$
            eventBatWriter
                    .println( "\"" //$NON-NLS-1$
                            + javaExePath
                            + "\" -classpath %BMDS_CLASSPATH% net.rim.application.ipproxyservice.IPProxyEvent %1 %2 %3 %4 %5 %6 %7 %8 %9" ); //$NON-NLS-1$
            eventBatWriter.flush();
            eventBatWriter.close();

        } catch( Exception ex ) {
            _logger.error( "updateBatchFiles exception:" //$NON-NLS-1$
                    + ex.getMessage() );
        }
    }

    private static void backup( String originalFilePath ) throws Exception {
        String backupFilePath = originalFilePath + ".ori";
        File backupFile = new File( backupFilePath );
        if( !backupFile.exists() ) {
            FileInputStream input = new FileInputStream( originalFilePath );
            FileOutputStream output = new FileOutputStream( backupFile );

            byte[] buffer = new byte[ 1024 * 4 ];
            int len;
            while( ( len = input.read( buffer ) ) != -1 ) {
                output.write( buffer, 0, len );
            }

            input.close();
            output.close();
        }
    }

    /***
     *
     * @return return null if MDS-CS can not be found. Otherwise, corresponding version is returned
     */
    private static CompatibilityVersion getMDSCSVersion( File MDSHome ) {
        CompatibilityVersion mdscsVersion = null;
        try {
            String bmdsJarPath = MDSHome.getPath() + File.separator + "classpath" + File.separator + "bmds.jar"; //$NON-NLS-1$
            File bmdsJarFile = new File( bmdsJarPath );
            if( bmdsJarFile.exists() ) {
                ZipFile zipFile = new ZipFile( bmdsJarFile );
                Enumeration< ? > entries = zipFile.entries();
                while( entries.hasMoreElements() ) {
                    ZipEntry entry = (ZipEntry) entries.nextElement();
                    if( !entry.isDirectory() ) {
                        _logger.debug( entry.getName() );
                        if( entry.getName().equalsIgnoreCase( "net/rim/application/ipproxyservice/Version.class" ) ) { //$NON-NLS-1$
                            // find Version class
                            InputStream is = zipFile.getInputStream( entry );
                            String versionStr = getMDSCSVersion( is );
                            if( versionStr != null ) {
                                org.osgi.framework.Version osgiVersion = new org.osgi.framework.Version( versionStr );
                                mdscsVersion = new CompatibilityVersion( osgiVersion.getMajor(), osgiVersion.getMinor(),
                                        osgiVersion.getMicro() );
                            }
                            break;
                        }
                    }
                } // end of while
            }
        } catch( Exception ex ) {
            _logger.error( "getMDSCSVersion error:" + ex.getMessage() ); ////$NON-NLS-1$
        }
        return mdscsVersion;
    }

    private static String getMDSCSVersion( InputStream inputStream ) {
        _currentVersion = null; // reset the value
        try {
            ClassReader classReader = new ClassReader( inputStream );
            ClassNode myVisitor = new ClassNode() {
                public FieldVisitor visitField( int access, String name, String desc, String signature, Object value ) {
                    if( value instanceof String ) {
                        String[] parts = ( (String) value ).split( "\\." );
                        if( parts.length > 2 ) {
                            _currentVersion = (String) value;
                        }
                    }
                    return super.visitField( access, name, desc, signature, value );
                }
            };
            classReader.accept( myVisitor, ClassReader.SKIP_FRAMES );
        } catch( Throwable ex ) {
            _logger.error( "", ex );
        }
        return _currentVersion;
    }

    private static void askUserDecision( final String title, final String message ) {
        Display.getDefault().syncExec( new Runnable() {
            public void run() {
                Shell shell = new Shell();
                MessageDialog dialog = new MessageDialog( shell, title, null, // accept the default window icon
                        message, MessageDialog.WARNING, new String[] { IDialogConstants.PROCEED_LABEL,
                                IDialogConstants.STOP_LABEL }, 0 ); // proceed
                // is the default
                _userDecision = dialog.open();
                shell.dispose();
            }
        } );
    }
}
