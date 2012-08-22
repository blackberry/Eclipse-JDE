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

//import java.util.regex.Pattern;

//import net.rim.eide.RimIDEUtil;
import net.rim.ejde.internal.util.Messages;
import net.rim.ide.RIA;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * Console for RAPC output that uses special highlighting to link resources.
 */

// RapcConsole ==> SimulatorOutputConsole
/**
 * This Class displayed the Simulator Output message in the console <i>Blackberry Simulator Output Console</i> by setting the
 * setSimulatorOutputCallback method.
 *
 * @see net.rim.eide.internal.ui.viewers.simulatoroutput.Messages
 */
public class SimulatorOutputConsole extends MessageConsole {

    private static SimulatorOutputConsole SimulatorOutputConsole;
    private IDocument myDocument;

    private SimulatorOutputConsole() {
        this( "BlackBerry Simulator Output Console" );
        myDocument = this.getDocument();
    }

    public void setRIACallback() {
        // Set the RIA callback for when simulator output is printed
        if( RIA.getCurrentDebugger() == null ) {
            return;
        }
        RIA.getCurrentDebugger().setSimulatorOutputCallback( new RIA.CommandOutput() {
        	private MessageConsoleStream cs = newMessageStream();
            /*
             * (non-Javadoc)
             *
             * @see net.rim.ide.RIA$CommandOutput#addLine(java.lang.String)
             */
            public void addLine( String line ) {
                cs.println(line);
            }
        } );
    }

    public static SimulatorOutputConsole getInstance() {
        if( SimulatorOutputConsole == null ) {
            synchronized( SimulatorOutputConsole.class ) {
                SimulatorOutputConsole = new SimulatorOutputConsole();
            }
        }
        return SimulatorOutputConsole;
    }

    private SimulatorOutputConsole( String name ) {
        super( name, null );
    }

    public MessageConsoleStream newMessageStream() {
		return new BBLogConsoleStream(this);
	}

    private void setDocumentText( final String newText ) {
        if( newText.length() == 0 ) {
            return;
        }

        // super.setHasData( true );
        Display.getDefault().asyncExec( new Runnable() {
            public void run() {
                myDocument.set( newText );
                // textViewer.setTopIndex( myDocument.getNumberOfLines() );
            }
        } );
        // super.updateToolbar();
    }

    /*
     * (non-Javadoc)
     */
    public void clear() {
        // super.setHasData( false );
        myDocument.set( "" ); //$NON-NLS-1$
        // super.updateToolbar();
    }

    /**
     * Called when a launch is executed. Simulator Output only works when the debugger is attached Previously, the JDE always
     * attached a debugger
     *
     * @param launch
     *            A launch is the result of launching a debug session and/or one or more system processes.
     * @see ILaunchManager
     */
    public void launchAdded( ILaunch launch ) {
        if( launch.getLaunchMode().equals( "run" ) ) { //$NON-NLS-1$
            setDocumentText( Messages.SimulatorOutputView_DEBUG_MODE_ONLY );
        } else if( launch.getLaunchMode().equals( "debug" ) ) { //$NON-NLS-1$
            clear();
        }
    }

    public void launchChanged( ILaunch launch ) {
    }

    public void launchRemoved( ILaunch launch ) {
    }

    public void dispose() {
        // Dispose element
        /*
         * if (textViewer != null && textViewer.getControl() != null) { textViewer.getControl().dispose(); }
         */
    }

}
