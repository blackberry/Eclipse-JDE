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
package net.rim.ejde.internal.menu;

import java.net.URL;

import net.rim.ejde.internal.util.Messages;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class OpenAppWorldCommandHandler extends AbstractHandler {

    private static final Logger _log = Logger.getLogger( OpenAppWorldCommandHandler.class );

    @Override
    public Object execute( ExecutionEvent event ) throws ExecutionException {
        IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
        try {
            IWebBrowser browser = support.createBrowser( IWorkbenchBrowserSupport.AS_EXTERNAL
                    | IWorkbenchBrowserSupport.NAVIGATION_BAR, null, null, null );
            browser.openURL( new URL( Messages.BlackBerryAppWorldPageURL ) );
        } catch( Exception e1 ) {
            _log.error( e1 );
        }
        return null;
    }

}
