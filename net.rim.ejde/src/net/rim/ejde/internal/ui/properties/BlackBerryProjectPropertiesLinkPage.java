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
package net.rim.ejde.internal.ui.properties;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.ui.editors.model.BlackBerryProjectFormEditor;
import net.rim.ejde.internal.util.Messages;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.part.FileEditorInput;

/**
 * Allows an alternate switch to the Application Descriptor editor from the Project properties page.
 *
 * @author mcacenco
 *
 */

public class BlackBerryProjectPropertiesLinkPage extends PropertyPage {
    private Logger _log = Logger.getLogger( BlackBerryProjectPropertiesLinkPage.class );

    @Override
    protected Control createContents( Composite parent ) {
        Link lnk = new Link( parent, SWT.NONE );
        lnk.setText( Messages.BlackBerryProjectPropertiesLinkPage_Text );
        // lnk.setFont(new Font(getShell().getDisplay(),
        // JFaceResources.getDefaultFontDescriptor().getFontData()[0].getName(),
        // 12, SWT.BOLD));
        final IPreferencePageContainer ippc = this.getContainer();

        lnk.addSelectionListener( new SelectionListener() {
            public void widgetDefaultSelected( SelectionEvent e ) {
                // Do Nothing
            }

            public void widgetSelected( SelectionEvent e ) {
                IResource resource = (IResource) getElement().getAdapter( IResource.class );
                if( resource instanceof IProject ) {
                    IJavaProject ijproj = JavaCore.create( (IProject) resource );
                    BlackBerryProject bbProject = new BlackBerryProject( ijproj );
                    // bbProject.addStore();
                    ContextManager.PLUGIN.setBBProperties( bbProject.getProject().getName(), bbProject.getProperties(), true );
                    final IFile mfh = bbProject.getMetaFileHandler();
                    if( null != mfh && mfh.exists() ) {
                        final IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        final IEditorInput input = new FileEditorInput( mfh );
                        if( !activateCurrentEditor( workbenchWindow, input ) ) {
                            Job openEditor = new Job( "Open Editor ..." ) {
                                @Override
                                protected IStatus run( IProgressMonitor monitor ) {
                                    Display.getDefault().asyncExec( new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                workbenchWindow.getActivePage().openEditor( input,
                                                        BlackBerryProjectFormEditor.EDITOR_ID );
                                            } catch( PartInitException e ) {
                                                _log.error( e );
                                            }
                                        }
                                    } );
                                    return Status.OK_STATUS;
                                }
                            };

                            openEditor.setUser( false );
                            openEditor.setSystem( true );
                            openEditor.schedule();
                        }

                        if( ippc instanceof PreferenceDialog ) {
                            ( (PreferenceDialog) ippc ).close(); // .okPressed();
                        }
                    } else {
                        _log.warn( "No file: " + mfh );
                    }
                }
            }
        } );

        noDefaultAndApplyButton();
        return parent;
    }

    private boolean activateCurrentEditor( IWorkbenchWindow iww, IEditorInput input ) {
        IWorkbenchPage[] pages = iww.getPages();
        IEditorPart iedp = null;
        for( int i = 0; i < pages.length; i++ ) {
            IEditorReference[] iderefs = pages[ i ].findEditors( input, BlackBerryProjectFormEditor.EDITOR_ID,
                    IWorkbenchPage.MATCH_INPUT );
            if( iderefs != null && iderefs.length > 0 ) {
                iedp = iderefs[ 0 ].getEditor( true );
                pages[ i ].activate( iedp );
                return true;
            }
        }
        return false;
    }
}
