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

// RapcConsoleFactory ==> SimulatorOutputConsoleFactory
/**
 * This class is called when in the <strong>Console Window</strong> Open Console's <i>Dropdown menu</i> is selected then
 * <i>BlackBerry Simulator Output </i> is chosen. It creates a new instance of SimulatorOutputConsole.
 *
 * @see SimulatorOutputConsole
 */
public class SimulatorOutputConsoleFactory implements IConsoleFactory {

    IConsole[] consoles = new IConsole[] { SimulatorOutputConsole.getInstance() };

    /**
     * This method is used to activate an existing SimulatorOutputConsole.
     *
     * @see ConsolePlugin.getDefault().getConsoleManager().showConsoleView()
     */
    public void showConsole() {
        ConsolePlugin.getDefault().getConsoleManager().showConsoleView( consoles[ 0 ] );
    }

    public void openConsole() {
        SimulatorOutputConsole console = SimulatorOutputConsole.getInstance();
        console.setRIACallback();
        ConsolePlugin.getDefault().getConsoleManager().addConsoles( consoles );
        console.clearConsole();
        showConsole();
    }

}
