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
package net.rim.ejde.internal.signing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.UIManager;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.core.IRIMMarker;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.preferences.SignatureToolPreferences;
import net.rim.ejde.internal.ui.consoles.PackagingConsole;
import net.rim.ejde.internal.ui.preferences.SignatureToolPrefsPage;
import net.rim.ejde.internal.util.FileUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PackagingUtils;
import net.rim.ejde.internal.util.PlatformSpecificMessages;
import net.rim.ejde.internal.util.ResourceBuilderUtils;
import net.rim.ejde.internal.util.VMToolsUtils;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ejde.internal.util.WindowsRegistryReader;
import net.rim.ide.core.Util;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class SignatureToolLaunchAction {
    private static Logger log = Logger.getLogger( SignatureToolLaunchAction.class );
    Pattern _pattern = Pattern.compile( "Signing of files is complete.:(\\d+)/(\\d+)" );

    /**
     * Pointer to process if the signature tool is running
     */
    private static Process _process;
    private String _sigToolPath, _password;
    private boolean _runSilent;
    private IStatus _status;
    private MessageConsoleStream _consoleOutputStream;

    public SignatureToolLaunchAction( String sigToolPath ) {
        _sigToolPath = sigToolPath;
        PackagingConsole packagingConsole = PackagingConsole.getInstance();
        ConsolePlugin.getDefault().getConsoleManager().addConsoles( new IConsole[] { packagingConsole } );
    }

    /**
     * Signs the cod file of the BlackBerry projects in the given <code>projects</code>.
     *
     * @param action
     * @param projects
     */
    public static boolean signCodFiles( final Set< BlackBerryProject > projects, final IProgressMonitor monitor ) {
        try {
            ResourceBuilderUtils.cleanProblemMarkers( ResourcesPlugin.getWorkspace().getRoot(),
                    new String[] { IRIMMarker.SIGNATURE_TOOL_PROBLEM_MARKER }, IResource.DEPTH_ZERO );
        } catch( CoreException e ) {
            log.error( e.getMessage() );
        }
        CodeSigningRunnable runnable = new CodeSigningRunnable( projects );
        new Thread( runnable ).start();
        return runnable.waitForSigningToFinish( monitor );
    }

    /**
     * Displays a warning dialog indicating that the signature tool is already running.
     */
    private static void warnSignatureToolRunning() {
        Display.getDefault().syncExec( new Runnable() {
            public void run() {
                MessageDialog dialog = new MessageDialog( ContextManager.getActiveWorkbenchShell(),
                        Messages.SignCommandHandler_SigToolRunningDialogTitleMsg, null,
                        Messages.SignCommandHandler_SigToolRunningDialogMsg, MessageDialog.WARNING,
                        new String[] { IDialogConstants.OK_LABEL }, 0 );
                dialog.open();
            }
        } );
    }

    /**
     * Run the action.
     */
    public void run( final List< String > codFileList ) {
        if( SignatureToolPreferences.getRunSignatureToolSilently() ) {
            // Run Signature Tool Silently
            _runSilent = true;
            if( !SignatureToolPreferences.isPasswordCached() ) {
                // Password has not been cached
                Display.getDefault().syncExec( new Runnable() {
                    public void run() {
                        SignatureToolPasswordPromptDialog dialog = new SignatureToolPasswordPromptDialog( ContextManager
                                .getActiveWorkbenchShell(), SignatureToolPreferences.getCachedPassword() );
                        dialog.open();
                        if( dialog.getReturnCode() == 0 ) {
                            // User Pressed OK
                            _status = Status.OK_STATUS;
                            _password = dialog.getResponse();
                        } else {
                            _status = Status.CANCEL_STATUS;
                        }
                    }
                } );
                // user canceled the operation
                if( !_status.isOK() ) {
                    return;
                }
            } else {
                // Use the previously cached password
                _password = SignatureToolPreferences.getCachedPassword();
            }
        } else {
            // Don't Run Silently
            _runSilent = false;
        }
        launchSignatureTool( codFileList );
    }

    static class CodeSigningRunnable implements Runnable {
        private Set< BlackBerryProject > _targetProjects;
        private boolean _running = true;
        private boolean _succesful;

        public CodeSigningRunnable( Set< BlackBerryProject > projects ) {
            _targetProjects = projects;
        }

        @Override
        public void run() {
            try {
                _succesful = true;
                List< String > codFileList = PackagingUtils.getCodFilePathsFromProjects( _targetProjects );
                IPath sigPath;
                String sigPathString = IConstants.EMPTY_STRING;
                sigPath = VMToolsUtils.getSignatureToolPath();
                // check signature tool again
                if( !VMToolsUtils.isVMToolValid() ) {
                    ResourceBuilderUtils.createProblemMarker( ResourcesPlugin.getWorkspace().getRoot(),
                            IRIMMarker.SIGNATURE_TOOL_PROBLEM_MARKER, Messages.SignatureTool_Not_Found_Msg, -1,
                            IMarker.SEVERITY_ERROR );
                    log.error( Messages.SignatureTool_Not_Found_Msg );
                    _succesful = false;
                    _running = false;
                    return;
                }

                sigPathString = sigPath.toOSString();
                final SignatureToolLaunchAction action = new SignatureToolLaunchAction( sigPathString );
                File cskFile = new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.CSK_FILE_NAME );
                File dbFile = new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.DB_FILE_NAME );
                while( ( ( !cskFile.exists() ) || ( !dbFile.exists() ) ) && _succesful ) {
                    // prompt user to install/import key files
                    Display.getDefault().syncExec( new Runnable() {
                        public void run() {
                            Shell shell = ContextManager.getActiveWorkbenchShell();
                            MessageDialog dialog = new MessageDialog( shell,
                                    Messages.SignCommandHandler_MissingFilesDialogTitleMsg, null,
                                    PlatformSpecificMessages.SignCommandHandler_MissingFilesDialogMsg, MessageDialog.ERROR,
                                    new String[] { IDialogConstants.PROCEED_LABEL, IDialogConstants.CLOSE_LABEL }, 0 );
                            dialog.open();
                            if( dialog.getReturnCode() == 0 ) {
                                Map< String, Boolean > data = new HashMap< String, Boolean >();
                                data.put( PropertyAndPreferencePage.DATA_NO_LINK, Boolean.TRUE );
                                PreferencesUtil.createPreferenceDialogOn( shell, SignatureToolPrefsPage.ID, null, data ).open();
                            } else {
                                // user choose not to install key, stop signing
                                _succesful = false;
                                try {
                                    ResourceBuilderUtils.createProblemMarker( ResourcesPlugin.getWorkspace().getRoot(),
                                            IRIMMarker.SIGNATURE_TOOL_PROBLEM_MARKER, Messages.SignCommandHandler_MissingFileMsg,
                                            -1, IMarker.SEVERITY_ERROR );
                                } catch( CoreException e ) {
                                    log.error( e.getMessage() );
                                }
                            }
                        }
                    } );
                }
                if( _succesful ) {
                    // If the signature tool is already running, don't start a new one
                    if( action.getProcess() != null ) {
                        warnSignatureToolRunning();
                        _succesful = false;
                    } else {
                        action.run( codFileList );
                    }
                }
            } catch( Exception ex ) {
                log.error( ex );
                _succesful = false;
            }
            _running = false;
        }

        public boolean isSucceed() {
            return _succesful;
        }

        public synchronized boolean waitForSigningToFinish( IProgressMonitor monitor ) {
            while( _running ) {
                Util.sleep( 1000 );
                if( monitor.isCanceled() ) {
                    // close simulator tool
                    if( _process != null ) {
                        _process.destroy();
                    }
                    return true;
                }
            }
            return _succesful;
        }
    }

    /**
     * Returns the process of launched signature tool or <code>null</code> if it has not been launched.
     *
     * @return The <code>Process</code>
     */
    public Process getProcess() {
        return _process;
    }

    /**
     * Launches the Signature tool. It gets all selected projects and passes their output file to the signature tool.
     */
    void launchSignatureTool( List< String > codFileList ) {
        searchAndCopySignatureKeys();
        log.debug( "Entering SignatureToolAction launchSignatureTool()" ); //$NON-NLS-1$

        // This is a list of the commands to run. The first position is the
        // actual command; subsequent entries are arguments.
        List< String > commands = new LinkedList< String >();

        // Find the path to java.exe
        String javaHome = System.getProperty( IConstants.JAVA_HOME_PROPERTY );
        IPath javaBinPath = new Path( javaHome ).append( IConstants.BIN_FOLD_NAME + File.separator + IConstants.JAVA_CMD );
        commands.add( javaBinPath.toOSString() );

        // Use the system look and feel
        String lookAndFeelClass = UIManager.getSystemLookAndFeelClassName();
        commands.add( IConstants.LOOK_AND_FEEL_CMD + lookAndFeelClass ); //$NON-NLS-1$

        // Load from a jar
        commands.add( IConstants.JAR_CMD );
        commands.add( _sigToolPath );

        // Add parameters for silent running
        if( _runSilent ) {
            commands.addAll( generateParameters() );
        }

        // Add cod files
        commands.addAll( codFileList );

        // Run the command
        ProcessBuilder processBuilder = new ProcessBuilder( commands );
        String signedFiles = "0";
        try {
            _consoleOutputStream = PackagingConsole.getInstance().newMessageStream();
            _consoleOutputStream.println( "Signing files: " + codFileList );
            _process = processBuilder.start();
            BufferedReader is = new BufferedReader( new InputStreamReader( _process.getInputStream() ) );
            String buffer;
            while( ( buffer = is.readLine() ) != null ) {
                // Print out console output for debugging purposes...
                log.debug( buffer );
                Matcher matcher = _pattern.matcher( buffer );
                if( matcher.matches() ) {
                    signedFiles = matcher.group( 1 );
                    try {
                        if( Integer.parseInt( signedFiles ) > 0 ) {
                            // at least one file has been signed successful, cache the password
                            SignatureToolPreferences.setCachedPassword( _password );
                        }
                    } catch( NumberFormatException e ) {
                        // do nothing
                    }
                }
            }
            _process.waitFor();
        } catch( IOException e ) {
            log.error( "Cannot run Signature Tool", e );
        } catch( InterruptedException e ) {
            // do nothing
        } finally {
        	//Fix for MKS 1998217 - Current COD signing tool can only output messages
        	//in console or popup wizard not both.
        	if(SignatureToolPreferences.getRunSignatureToolSilently()){
        		_consoleOutputStream.println( "Signing completed:" + signedFiles + " files signed." );
        	}
            _process = null;
            try {
                _consoleOutputStream.close();
            } catch( IOException e ) {
                // do nothing
            }
            log.debug( "Leaving SignatureToolAction launchSignatureTool()" ); //$NON-NLS-1$
        }
    }

    protected List< String > generateParameters() {
        List< String > parameters = new ArrayList< String >();

        // Password
        parameters.add( IConstants.SIGTOOL_PASSWORD );
        parameters.add( _password );

        // Automatic Signing
        parameters.add( IConstants.SIGTOLL_AUTOMATIC );

        // AutoClose
        parameters.add( IConstants.SIGTOLL_AUTO_CLOSE );

        // Print statistics
        parameters.add( "-s" );

        return parameters;
    }

    /**
     * If signature files exist simply return, otherwise search for key files from the following order, copy the key files to
     * sigTool directory if found. PLATFORM Windows
     *
     * Search order: 1. Installed component packs within Eclipse (from highest version to lowest version) 2. Legacy JDE Components
     * (from highest version to lowest version) 3. Legacy JDEs (from highest version to lowest version)
     *
     * @return <code> if key files are found</code> otherwise returns <code>false</code>
     */
    private boolean searchAndCopySignatureKeys() {
        log.debug( "Searching keys files..." );
        // check if key files exist in current SigTool directory
        String sigToolDir = IConstants.EMPTY_STRING;
        try {
            sigToolDir = VMToolsUtils.getVMToolsFolderPath().toOSString();
        } catch( IOException e ) {
            log.error( e.getMessage(), e );
        }
        File cskFile = new File( sigToolDir + File.separator + IConstants.CSK_FILE_NAME );
        File dbFile = new File( sigToolDir + File.separator + IConstants.DB_FILE_NAME );
        if( cskFile.exists() && dbFile.exists() ) {
            return true;
        }

        // search installed VMs in Eclipse
        String location;
        for( IVMInstall vm : VMUtils.getInstalledBBVMs() ) {
            location = vm.getInstallLocation().getAbsolutePath() + File.separator + IConstants.BIN_FOLD_NAME;
            if( copySignatureKeyFiles( location, sigToolDir ) ) {
                return true;
            }
        }

        // search legacy JDE Components registered in Windows registry from
        // highest version to lowest version
        List< String > legacyJDEComponentsPaths = WindowsRegistryReader.getInstalledJDEComponentsPaths();
        int count = legacyJDEComponentsPaths.size();
        for( int i = count - 1; i >= 0; i-- ) {
            location = legacyJDEComponentsPaths.get( i );
            if( copySignatureKeyFiles( location, sigToolDir ) ) {
                return true;
            }
        }

        // search legacy JDE registered in Windows registry from highest version
        // to lowest version
        List< String > legacyJDEPaths = WindowsRegistryReader.getInstalledJDEPaths();
        count = legacyJDEPaths.size();
        for( int i = count - 1; i >= 0; i-- ) {
            location = legacyJDEPaths.get( i );
            if( copySignatureKeyFiles( location, sigToolDir ) ) {
                return true;
            }
        }

        // key files are not found
        return false;
    }

    private static boolean copySignatureKeyFiles( String source, String dest ) {
        File srcCskFile = new File( source + File.separator + IConstants.CSK_FILE_NAME ); //$NON-NLS-1$ //$NON-NLS-2$
        File srcDbFile = new File( source + File.separator + IConstants.DB_FILE_NAME ); //$NON-NLS-1$ //$NON-NLS-2$
        if( srcCskFile.exists() && srcDbFile.exists() ) {
            File dstCskFile = new File( dest + File.separator + IConstants.CSK_FILE_NAME ); //$NON-NLS-1$ //$NON-NLS-2$
            File dstDbFile = new File( dest + File.separator + IConstants.DB_FILE_NAME ); //$NON-NLS-1$ //$NON-NLS-2$
            log.info( "Copy " + srcCskFile.getPath() + " to " + dstCskFile.getPath() ); //$NON-NLS-1$ //$NON-NLS-2$
            log.info( "Copy " + srcDbFile.getPath() + " to " + dstDbFile.getPath() ); //$NON-NLS-1$ //$NON-NLS-2$
            // copy the key files to current component pack
            try {
                FileUtils.copyOverwrite( srcCskFile, dstCskFile );
                FileUtils.copyOverwrite( srcDbFile, dstDbFile );
                return true;
            } catch( IOException e ) {
                log.error( "failed to copy signature key files", e ); //$NON-NLS-1$
            }
        }
        return false;
    }
}
