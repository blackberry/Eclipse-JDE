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
package net.rim.ejde.internal.legacy;

import java.io.File;

import net.rim.ejde.internal.model.preferences.WarningsPreferences;
import net.rim.ejde.internal.util.BrowseSearchDialog;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PromptDialog;
import net.rim.ejde.internal.util.RunnableWithResult;
import net.rim.ide.RIA;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Error and Dialog handling Dialog class. Implements the RIA callback for displaying dialogs. Ensures dialogs launched are SWT UI
 * safe (ie launched on SWT ui thread)
 *
 * @author cwetherly, bchabot, cmalinescu
 *
 */
public class RIADialog implements RIA.DialogsEx {

    static final Logger _logger = Logger.getLogger( RIADialog.class );

    /**
     * Default constructor.
     */
    public RIADialog() {
        // do nothing
    }

    /**
     * Callback by RIA to notify an error message.
     *
     * @see net.rim.ide.RIA$Dialogs#error(java.lang.String, boolean)
     */
    public boolean error( final String msg, final boolean hasDontTellAgainOption ) {
        RunnableWithResult dialogRun = new RunnableWithResult() {
            @Override
            protected Object doRunWithResult() {
                if( hasDontTellAgainOption ) {
                    MessageDialogWithToggle dialog = MessageDialogWithToggle.openError( getShell(),
                            Messages.ErrorHandler_DIALOG_TITLE, msg, Messages.DontAskMeAgainMsg, false, null, null );
                    return Boolean.valueOf( dialog.getToggleState() );
                }
                MessageDialog.openError( getShell(), Messages.ErrorHandler_DIALOG_TITLE, msg );
                return Boolean.FALSE;
            }
        };
        Boolean result = (Boolean) showDialogWithResult( dialogRun );
        return result.booleanValue();
    }

    /**
     * Callback by RIA to notify a warning message.
     *
     * @see net.rim.ide.RIA$Dialogs#warning(java.lang.String, boolean)
     */
    public boolean warning( final String msg, final boolean hasDontTellAgainOption ) {
        // Currently RIA notifies eJDE through this method a general message of missing debug files.
        // Since it does not provide much useful information, we will not show this to the user.
        return false;
    }

    private void showDialog( Runnable dialogRun ) {
        if( Display.getCurrent() == null ) {
            Display.getDefault().syncExec( dialogRun );
        } else {
            dialogRun.run();
        }
    }

    private Object showDialogWithResult( RunnableWithResult dialogRun ) {
        showDialog( dialogRun );
        return dialogRun.getResult();
    }

    static Shell getShell() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.rim.ide.RIA$Dialogs#password(java.lang.String, boolean)
     */
    public String password( final String title, final boolean obscure ) {
        return prompt( title, "", obscure ); //$NON-NLS-1$
    }

    private String prompt( final String title, final String initialResponse, final boolean obscure ) {
        RunnableWithResult runnable = new RunnableWithResult() {
            @Override
            protected Object doRunWithResult() {
                PromptDialog dia = new PromptDialog( getShell(), title, initialResponse, obscure );
                dia.open();
                return dia.getResponse();
            }
        };
        return (String) showDialogWithResult( runnable );
    }

    /*
     * (non-Javadoc)
     *
     * @see net.rim.ide.RIA$Dialogs#promptForDebugFile(java.lang.String)
     */
    public File promptForDebugFile( final String name ) {
        /*
         * if( RimCore.PLUGIN.getRIANoDialog().getDontPromptForMissingDebugFiles() ) return null;
         */

        final boolean prompt = WarningsPreferences.getPromptForMissingDebugFiles();

        if( prompt ) {
            RunnableWithResult runnable = new RunnableWithResult() {
                @Override
                protected Object doRunWithResult() {
                    String chosen_path = null;
                    while( true ) {
                        BrowseSearchDialog dialog = new BrowseSearchDialog( getShell(), name );
                        dialog.open();
                        // Don't ask or Cancel button pressed
                        if( dialog.isDontAskAgain() || dialog.isCancel() ) {
                            break;
                        }
                        _logger.debug( "promptForDebugFile(name) filepath: " + dialog.getFilePath() ); //$NON-NLS-1$
                        chosen_path = dialog.getFilePath();
                        if( chosen_path != null ) {
                            if( new File( chosen_path ).length() == 0 ) {
                                _logger.debug( "Chosen debug file has zero bytes or invalid debug informations" ); //$NON-NLS-1$
                                error( Messages.RIADialog_invalidFileMsg, false );
                                continue;
                            }
                            break;
                        }
                        error( Messages.RIADialog_noDebugFileSelected, false );
                    }
                    return chosen_path;
                }
            };
            String file_path = (String) showDialogWithResult( runnable );
            if( file_path != null ) {
                file_path = file_path.trim();
                File file = new File( file_path );
                if( file.exists() ) {
                    _logger.debug( "promptForDebugFile(name) file name: " + file.getName() ); //$NON-NLS-1$
                    return file;
                }
            }
        }
        return null;
    }

    public boolean askYesNo( final String question ) {
        RunnableWithResult dialogRun = new RunnableWithResult() {
            @Override
            protected Object doRunWithResult() {
                final boolean result = MessageDialog.openQuestion( getShell(), "Question", question ); //$NON-NLS-1$
                return Boolean.valueOf( result );

            }
        };
        Boolean result = (Boolean) showDialogWithResult( dialogRun );
        return result.booleanValue();
    }

    public String prompt( final String msg, final String answer ) {
        return prompt( msg, answer, false );
    }

}
