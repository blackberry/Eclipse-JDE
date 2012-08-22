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
package net.rim.ejde.internal.ui.intro;

import java.io.IOException;
import java.net.URL;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.IntroPart;
import org.osgi.framework.Bundle;

/**
 * BlackBerry Java Plug-in for Eclipse Welcome page.
 *
 * @author dmeng
 *
 */
public class BlackBerryIntroPart extends IntroPart {

    private static final Logger _logger = Logger.getLogger( BlackBerryIntroPart.class );

    @Override
    public void createPartControl( Composite parent ) {

        Browser browser = new Browser( parent, SWT.NONE );

        // Startup page resides in the net.rim.ejde.doc plug-in
        Bundle bundle = Platform.getBundle( ContextManager.PLUGIN_ID); //IConstants.DOC_PLUGIN_ID );
        IPath pagePath = new Path( IConstants.START_UP_PAGE );
        IPath folderPath = new Path( IConstants.START_UP_FOLDER );
        IPath htmlFolderPath = new Path( IConstants.HTML_PAGE_FOLDER );
        URL pageUrl = FileLocator.find( bundle, pagePath, null );
        URL folderUrl = FileLocator.find( bundle, folderPath, null );
        URL htmlFolderUrl = FileLocator.find( bundle, htmlFolderPath, null );

        try {
            // Extract necessary files from jar into a cache, otherwise the images will not be
            // displayed properly
            FileLocator.toFileURL( folderUrl );
            // Since startup page links to HTML page, we need to extract HTML content as well
            FileLocator.toFileURL( htmlFolderUrl );
            pageUrl = FileLocator.toFileURL( pageUrl );
            boolean success = browser.setUrl( pageUrl.toExternalForm() );
            if( !success ) {
                _logger.error( "Unable to open page:" + pageUrl );
            }
        } catch( IOException e ) {
            _logger.error( "Unable to convert URL", e ); //$NON-NLS-1$
        }
    }

    @Override
    public void standbyStateChanged( boolean standby ) {
        // do nothing
    }

    @Override
    public void setFocus() {
        // do nothing
    }
}
