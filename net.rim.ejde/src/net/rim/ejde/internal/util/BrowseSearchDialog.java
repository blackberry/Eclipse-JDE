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
import java.lang.reflect.InvocationTargetException;

import net.rim.ejde.internal.model.preferences.WarningsPreferences;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class BrowseSearchDialog extends MessageDialog {
    private final String _fileName;
    private String _chosenFilePath;
    private Shell _parentShell;
    // these constants must correspond with order buttons are added in
    // constructor
    private static final int SEARCH_INDEX = 0;
    private static final int BROWSE_INDEX = 1;
    private static final int CANCEL_INDEX = 2;
    private static final int DONT_ASK_INDEX = 3;

    private static final int SUCCESS_INDEX = 0;
    private static final int FAILURE_INDEX = -1;
    private boolean _dontAskAgain = false;
    private boolean _doCancel = false;

    private static final Logger log = Logger.getLogger( BrowseSearchDialog.class );

    /**
     * Constructs the dialog.
     *
     * @param parentShell
     *            The parent shell
     * @param fileName
     *            The debug file name
     */
    public BrowseSearchDialog( Shell parentShell, String fileName ) {

        super( parentShell, Messages.BrowseSearchDialog_browseTitle, null, NLS.bind( Messages.BrowseSearchDialog_message,
                fileName ), WARNING,
                new String[] { Messages.BrowseSearchDialog_searchLabel, Messages.BrowseSearchDialog_browseLabel,
                        IDialogConstants.CANCEL_LABEL, Messages.BrowseSearchDialog_dontAskLabel }, 0 );

        _fileName = fileName;
        _chosenFilePath = null;
        _parentShell = parentShell;
    }

    /**
     * Gets the debug file path.
     *
     * @return The debug file path
     */
    public String getFilePath() {
        return _chosenFilePath;
    }

    /**
     * Returns if user selected don't ask again button
     *
     * @return <code>true</code> if yes; otherwise <code>false</code>
     */
    public boolean isDontAskAgain() {
        return _dontAskAgain;
    }

    /**
     * Returns if the Cancel button is clicked.
     *
     * @return <code>true</code> if yes; otherwise <code>false</code>
     */
    public boolean isCancel() {
        return _doCancel;
    }

    @Override
    protected void buttonPressed( int buttonId ) {
        close();
        switch( buttonId ) {
            case SEARCH_INDEX: {
                search();
                break;
            }
            case BROWSE_INDEX: {
                browse();
                break;
            }
            case CANCEL_INDEX: {
                cancel();
                break;
            }
            case DONT_ASK_INDEX: {
                dontAsk();
                break;
            }
        }
    }

    private void dontAsk() {
        _dontAskAgain = true;
        WarningsPreferences.setPromptForMissingDebugFiles( false );
    }

    private void cancel() {
        _doCancel = true;
    }

    private int browse() {
        FileDialog dialog = new FileDialog( _parentShell, SWT.OPEN );
        dialog.setFilterExtensions( new String[] { "*.debug" } ); //$NON-NLS-1$
        dialog.setFileName( _fileName );
        _chosenFilePath = dialog.open();
        // When user press Cancel button in the FileChooser
        if( _chosenFilePath == null ) {
            return FAILURE_INDEX;
        }
        // make sure the user is choosing the right file and that file exists in
        // that specific directory.
        File debugFile = new File( _chosenFilePath );
        if( debugFile.exists() && debugFile.getName().equals( _fileName ) ) {
            return SUCCESS_INDEX;
        }
        _chosenFilePath = null;
        return FAILURE_INDEX;
    }

    private int search() {
        DirectoryDialog dialog = new DirectoryDialog( _parentShell, SWT.OPEN );
        String chosenDir = dialog.open();
        _chosenFilePath = null;
        if( chosenDir == null ) {
            return FAILURE_INDEX;
        }
        File dir = new File( chosenDir );
        if( !dir.isDirectory() ) {
            log.error( "Did not get expected directory " + chosenDir ); //$NON-NLS-1$
            return FAILURE_INDEX;
        }
        RecursiveSearchOp op = new RecursiveSearchOp( dir, _fileName );
        try {
            PlatformUI.getWorkbench().getProgressService().busyCursorWhile( op );
        } catch( InvocationTargetException e ) {
            ErrorHandler.handleOperationException( getShell(), Messages.BrowseSearchDialog_searchExceptionMsg, e );
            return FAILURE_INDEX;
        } catch( InterruptedException e ) {
            ErrorHandler.handleOperationException( getShell(), Messages.BrowseSearchDialog_searchExceptionMsg, e );
            return FAILURE_INDEX;
        }
        if( op.getFile() == null ) {
            return FAILURE_INDEX;
        }
        _chosenFilePath = op.getFile().getAbsolutePath();
        return SUCCESS_INDEX;
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
            RecursiveSearchTask.ISearchDialog dialog = new RecursiveSearchTask.ISearchDialog() {
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
