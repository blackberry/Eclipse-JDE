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
package net.rim.ejde.internal.ui.widgets.dialog;

/*********************************************************************
 * SigningSearchDialog.java
 *
 * Copyright (c) 2007 Research In Motion Inc.  All rights reserved.
 * This file contains confidential and proprietary information
 *
 * Creation date: Sep 09, 2009 12:03:54 PM
 *
 * File:          SigningSearchDialog.java
 * Revision:      $Revision$
 * Checked in by: rgunaratnam
 * Last modified: $DateTime$
 *
 *********************************************************************/

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.RecursiveSearchTask;
import net.rim.ejde.internal.util.RecursiveSearchTask.ISearchDialog;
import net.rim.ejde.internal.util.VMToolsUtils;
import net.rim.ide.core.Util;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class SigningSearchDialog {
    private Shell _parentShell;

    private static final Logger _logger = Logger.getLogger( SigningSearchDialog.class );

    public SigningSearchDialog( Shell parentShell ) {
        _parentShell = parentShell;
    }

    public void copyFileIntoSignToolDir( ArrayList< File > oldKeyFiles ) {
        String dest_file;
        try {
            for( File srcFile : oldKeyFiles ) {
                dest_file = VMToolsUtils.getVMToolsFolderPath() + File.separator + srcFile.getName();
                File destFile = new File( dest_file );
                Util.copyFile( srcFile, destFile );
            }

        } catch( IOException e ) {
            _logger.error( "The chosen file cannot be copied into the directory", e );
        }
    }

    public ArrayList< File > search() {
        DirectoryDialog dialog = new DirectoryDialog( _parentShell, SWT.OPEN );
        dialog.setText( Messages.SigningSearchDialog_DirDialogTitleMsg );
        String chosenDir = dialog.open();

        File oldCskFile;
        File oldDbFile;
        ArrayList< File > foundFiles = new ArrayList< File >();

        if( chosenDir == null ) {
            return null;
        }
        File dir = new File( chosenDir );
        if( !dir.isDirectory() ) {
            throw new IllegalArgumentException( NLS.bind( Messages.SigningSearchDialog_ExceptionMessage6, chosenDir ) );
        }
        RecursiveSearchOp op = new RecursiveSearchOp( dir, IConstants.CSK_FILE_NAME );
        try {
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile( op );
        } catch( InvocationTargetException e ) {
            throw new IllegalArgumentException( Messages.SigningSearchDialog_ExceptionMessage5 );
        } catch( InterruptedException e ) {
            throw new IllegalArgumentException( Messages.SigningSearchDialog_ExceptionMessage5 );
        }
        if( op.getFile() == null ) {
            throw new IllegalArgumentException( Messages.SigningSearchDialog_ExceptionMessage4 );
        }
        oldCskFile = op.getFile();
        oldDbFile = new File( op.getFile().getParent() + File.separator + IConstants.DB_FILE_NAME );

        if( oldDbFile.exists() ) {
            foundFiles.add( oldCskFile );
            foundFiles.add( oldDbFile );
            return foundFiles;
        }
        if( foundFiles.isEmpty() ) {
            throw new IllegalArgumentException( Messages.SigningSearchDialog_ExceptionMessage7 );
        }
        return null;
    }

    /**
     * Search for a file in a given directory and its subdirectories
     */
    private static class RecursiveSearchOp implements IRunnableWithProgress {
        private File _dir;
        private String _name;
        private File _file;

        public RecursiveSearchOp( File dir, String name ) {
            _dir = dir;
            _name = name;
        }

        File getFile() {
            return _file;
        }

        public void run( final IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException {
            ISearchDialog dialog = new ISearchDialog() {
                public boolean isCanceled() {
                    return monitor.isCanceled();
                }

                public void updateProgress( String dirName ) {
                    monitor.subTask( Messages.BrowseSearchDialog_searchingLabel + dirName );
                }
            };
            RecursiveSearchTask task = new RecursiveSearchTask( dialog, _dir, _name );
            task.run();
            _file = task.getFile();
        }
    }
}
