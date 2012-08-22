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
package net.rim.ejde.internal.ui.consoles;

import org.apache.log4j.Logger;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;

public class ConsoleUtils {
    private static Logger log = Logger.getLogger( ConsoleUtils.class );

    /**
     *
     * @param console
     */
    public static void openConsole( IConsole console ) {
        IWorkbenchPage workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        String consoleViewID = IConsoleConstants.ID_CONSOLE_VIEW;

        try {
            IConsoleView consoleView = (IConsoleView) workbenchPage.showView( consoleViewID );
            consoleView.display( console );
        } catch( PartInitException e ) {
            log.error( e.getMessage(), e );
        }
    }
}
