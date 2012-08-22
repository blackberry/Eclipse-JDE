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

import java.io.FileOutputStream;
import java.io.IOException;
import net.rim.ejde.internal.model.preferences.RootPreferences;

import org.apache.log4j.Logger;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

public class BBLogConsoleStream extends MessageConsoleStream {
    private static Logger log = Logger.getLogger( ConsoleUtils.class );

	private FileOutputStream out = null;

	public BBLogConsoleStream(MessageConsole console) {
		super(console);
		if (RootPreferences.getAppendConsoleLogToFile()) try {
			out = new FileOutputStream(RootPreferences.getConsoleLogFile(), true);
		} catch (Exception e) {
			log.error(e.getLocalizedMessage(), e);
		}
	}

    public void write(String str) throws IOException {
    	super.write(str);
    	if (out != null) out.write(str.getBytes());
    }

    public void write(byte[] b, int off, int len) throws IOException {
    	super.write(b, off, len);
    	if (out != null) out.write(b, off, len);
    }

    public void close() throws IOException {
    	super.close();
    	if (out != null) {
    		out.close();
    		out = null;
    	}
    }

    public void flush() throws IOException {
    	super.flush();
    	if (out != null) {
    		out.flush();
    		out = null;
    	}
    }

}
