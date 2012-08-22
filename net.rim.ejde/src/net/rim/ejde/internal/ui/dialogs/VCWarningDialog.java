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
package net.rim.ejde.internal.ui.dialogs;

import java.net.MalformedURLException;
import java.net.URL;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;
import net.rim.ejde.internal.util.Messages;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class VCWarningDialog extends MessageDialog {
    private static final Logger _log = Logger.getLogger( VCWarningDialog.class );
    public static IPreferenceStore store = ContextManager.getDefault().getPreferenceStore();
    private Button _checkBoxButton;
    private String _href = Messages.VC2008DownloadLink;

    /**
     * Creates a new dialog
     *
     * @see MessageDialog#MessageDialog(org.eclipse.swt.widgets.Shell, java.lang.String, org.eclipse.swt.graphics.Image,
     *      java.lang.String, int, java.lang.String[], int)
     */
    public VCWarningDialog( Shell shell, String title, String message ) {
        super( shell, title, null, message, WARNING, new String[] { "OK" }, 0 ); // yes is the default
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.MessageDialog#createCustomArea(org.eclipse.swt.widgets.Composite)
     */
    protected Control createCustomArea( Composite parent ) {
        Composite composite = new Composite( parent, SWT.NONE );
        composite.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, false ) );
        _checkBoxButton = new Button( parent, SWT.CHECK );
        _checkBoxButton.setText( Messages.DontAskMeAgainMsg );
        _checkBoxButton.setSelection( false );
        return composite;
    }

    protected Control createMessageArea( Composite composite ) {
        // create composite
        // create image
        Image image = getImage();

        if( image != null ) {
            imageLabel = new Label( composite, SWT.NULL );
            image.setBackground( imageLabel.getBackground() );
            imageLabel.setImage( image );

            GridDataFactory.fillDefaults().align( SWT.CENTER, SWT.BEGINNING ).applyTo( imageLabel );
        }
        // create message
        if( message != null ) {
            Link _link = new Link( composite, SWT.TOP );
            _link.setText( message + " The required package can be installed from: <a href=" + _href
                    + "> MS VC2008 Redistributable Package</a>" );

            GridDataFactory.fillDefaults().align( SWT.FILL, SWT.BEGINNING ).grab( true, false )
                    .hint( convertHorizontalDLUsToPixels( IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH ), SWT.DEFAULT )
                    .applyTo( _link );
            _link.addSelectionListener( new SelectionListener() {
                public void widgetDefaultSelected( SelectionEvent e ) {
                    // Do Nothing
                }

                public void widgetSelected( SelectionEvent e ) {
                    _log.debug( e.text );
                    IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
                    try {
                        IWebBrowser browser = support.createBrowser( IWorkbenchBrowserSupport.AS_EXTERNAL
                                | IWorkbenchBrowserSupport.NAVIGATION_BAR, null, null, null );
                        browser.openURL( new URL( e.text ) );
                    } catch( PartInitException e1 ) {
                        // TODO Auto-generated catch block
                        _log.error( "enable to open external broswer" );
                    } catch( MalformedURLException e2 ) {
                        // TODO Auto-generated catch block
                        _log.error( "enable to open external broswer" );
                    }
                }
            } );
        }

        return composite;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
     */
    protected void buttonPressed( int buttonId ) {
        // ContextManager.getDefault().getPreferenceStore().setValue( PreferenceConstants.POP_FOR_MISSING_VC,
        // _checkBoxButton.getSelection());
        IEclipsePreferences pref = ( new InstanceScope() ).getNode( ContextManager.PLUGIN_ID );
        pref.putBoolean( PreferenceConstants.POP_FOR_MISSING_VC, !_checkBoxButton.getSelection() );

        _log.debug( "setting " + PreferenceConstants.POP_FOR_MISSING_VC + "to " + !_checkBoxButton.getSelection() );
        super.buttonPressed( buttonId );
    }

}
