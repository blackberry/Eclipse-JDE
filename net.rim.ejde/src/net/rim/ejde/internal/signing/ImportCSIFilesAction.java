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

/*********************************************************************
 * ImportCSIFiles.java
 *
 * Copyright (c) 2009 Research In Motion Inc.  All rights reserved.
 * This file contains confidential and proprietary information
 *
 * Creation date: September, 2009, 2:22:46 AM
 *
 * File:          $File$
 * Revision:      $Revision:$
 * Checked in by:  rgunaratnam
 * Last modified: $DateTime:$
 *
 *********************************************************************/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import javax.swing.UIManager;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMToolsUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * The actions opens a file dialog that allows user to select .csi files, it then call the signature tool to start registration
 * and generate key files.
 */
public class ImportCSIFilesAction implements IWorkbenchWindowActionDelegate {

    private static Logger log = Logger.getLogger( ImportCSIFilesAction.class );

    /**
     * Pointer to process if the signature tool is running
     */
    private Process process;

    /**
     * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
     */
    public void dispose() {
        // Try to shutdown the signature tool if it is still running.
        if( process != null ) {
            process.destroy();
        }
    }

    /**
     * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
     */
    public void init( IWorkbenchWindow window ) {
        // do nothing
    }

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    public void run( IAction action ) {
        if( process != null ) {
            // If the process is running already...
            warnSignatureToolRunning();
        } else {
            // Open file dialog to allow user select a .csi file
            FileDialog dialog = new FileDialog( ContextManager.getActiveWorkbenchShell() );
            String fileDialogLabel = "Select Signature File"; //$NON-NLS-1$
            String[] filters = new String[] { "*.csi" };
            String[] filterNames = new String[] { "Signature Files (*.csi)" };
            dialog.setFilterExtensions( filters );
            dialog.setFilterNames( filterNames );
            dialog.setText( fileDialogLabel );
            final String signatureFileToImport = dialog.open();
            if( StringUtils.isNotBlank( signatureFileToImport ) ) {
                // launch signature tool
                Thread newThread = new Thread() {
                    public void run() {
                        launchSignatureTool( signatureFileToImport );
                    }
                };
                newThread.start();
            }
        }
    }

    /**
     * Launches the Signature tool. It passes the .csi file to the signature tool.
     */
    private void launchSignatureTool( String csiFile ) {
        log.debug( "Entering SignatureToolAction launchSignatureTool()" );

        // This is a list of the commands to run. The first position is the
        // actual command; subsequent entries are arguments.
        List< String > commands = new LinkedList< String >();

        // Find the path to java.exe
        String javaHome = System.getProperty( "java.home" );
        IPath javaBinPath = new Path( javaHome ).append( IConstants.BIN_FOLD_NAME ).append( IConstants.JAVA_CMD );
        commands.add( javaBinPath.toOSString() );

        // Use the system look and feel
        String lookAndFeelClass = UIManager.getSystemLookAndFeelClassName();
        commands.add( "-Dswing.defaultlaf=" + lookAndFeelClass );

        // Load from a jar
        commands.add( "-jar" );

        IPath sigPath;
        String sigPathString = IConstants.EMPTY_STRING;
        try {
            sigPath = VMToolsUtils.getSignatureToolPath();
            // check signature tool again
            if( !VMToolsUtils.isVMToolValid() ) {
                Display.getDefault().syncExec( new Runnable() {
                    public void run() {
                        Shell shell = ContextManager.getActiveWorkbenchShell();
                        MessageDialog.openError( shell, Messages.ErrorHandler_DIALOG_TITLE, Messages.SignatureTool_Not_Found_Msg );
                    }
                } );
                log.error( Messages.SignatureTool_Not_Found_Msg );
                return;
            }
            sigPathString = sigPath.toOSString();
            commands.add( sigPathString );
            commands.add( csiFile );

        } catch( IOException e ) {
            log.error( e.getMessage(), e );
        }

        // Run the command
        ProcessBuilder processBuilder = new ProcessBuilder( commands );
        try {
            process = processBuilder.start();
            BufferedReader is = new BufferedReader( new InputStreamReader( process.getInputStream() ) );
            String buffer;
            while( ( buffer = is.readLine() ) != null ) {
                // Print out console output for debugging purposes...
                System.out.println( buffer );
            }
            process = null;
        } catch( IOException e ) {
            e.printStackTrace();
        }

        log.debug( "Leaving SignatureToolAction launchSignatureTool()" );
    }

    /**
     * Displays a warning dialog indicating that the signature tool is already running.
     */
    private void warnSignatureToolRunning() {
        MessageDialog dialog = new MessageDialog( ContextManager.getActiveWorkbenchShell(),
                "Signature Tool is already running...", null,
                "The Signature Tool is already running.  Please exit the tool before running it again.", MessageDialog.WARNING,
                new String[] { "OK" }, 0 );
        dialog.open();
    }

    /**
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action .IAction,
     *      org.eclipse.jface.viewers.ISelection)
     */
    public void selectionChanged( IAction action, ISelection selection ) {

    }
}
