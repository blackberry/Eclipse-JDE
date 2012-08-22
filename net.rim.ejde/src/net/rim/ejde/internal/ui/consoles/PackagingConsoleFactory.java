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

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;

public class PackagingConsoleFactory implements IConsoleFactory {
    PackagingConsole console = PackagingConsole.getInstance();
    IConsole[] consoles = new IConsole[] { console };

    /**
     * This method is used to show an existing RapcConsole.
     *
     * @see ConsolePlugin.getDefault().getConsoleManager().showConsoleView()
     */
    public void showConsole() {
        ConsolePlugin.getDefault().getConsoleManager().showConsoleView( consoles[ 0 ] );
    }

    /**
     * This method is used to activate an existing RapcConsole.
     */
    public void openConsole() {
        ConsolePlugin.getDefault().getConsoleManager().addConsoles( consoles );
        showConsole();
    }

    /**
     * Returns an instance of RapcConsole
     *
     * @return An instance of RapcConsole
     */
    public PackagingConsole getConsole() {
        return console;
    }
}
