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
package net.rim.ejde.internal.launching;

import java.io.File;

import net.rim.ejde.DebugFileLoader;
import net.rim.ejde.IDebugConsoleWriter;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.preferences.RootPreferences;
import net.rim.ejde.internal.ui.dialogs.DownloadDebugFilesDialog;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;
import net.rim.ejde.internal.util.Messages;
import net.rim.ide.RIA;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;

public class EJDEDebugFilesClient implements RIA.DebugFilesClient {

    private String _serverUrl;
    private String _downloadTo;
    private IDebugConsoleWriter _console;
    private int _answer;
	private static Logger _logger = Logger.getLogger(EJDEDebugFilesClient.class);

    public EJDEDebugFilesClient( String serverUrl, String downloadTo ) {
        _serverUrl = serverUrl;
        _downloadTo = downloadTo;
        _console = null;
    }

    public EJDEDebugFilesClient( String serverUrl, String downloadTo, IDebugConsoleWriter console ) {
        _serverUrl = serverUrl;
        _downloadTo = downloadTo;
        _console = console;
    }

    public String downloadDebugFiles( String timestamp ) {
        String bundleFile = _downloadTo + File.separator + timestamp + IConstants.JAR_EXTENSION_WITH_DOT;
        if( !new File( bundleFile ).exists() ) {
            _answer = RootPreferences.getDownloadDebugFilesOption();
            if( _answer == PreferenceConstants.DOWNLOAD_DEBUG_FILES_PROMPT ) {
                Display.getDefault().syncExec( new Runnable() {
                    public void run() {
                        _answer = DownloadDebugFilesDialog.openQuestion( Messages.DownloadDebugFilesDialogTitle,
                                Messages.DownloadDebugFilesDialogText );
                    }
                } );
            }
            if( _answer == PreferenceConstants.DOWNLOAD_DEBUG_FILES_YES ) {
				try {
					bundleFile = new DebugFileLoader(_serverUrl, _downloadTo,
							_console).downloadDebugFiles(timestamp);
				} catch (Exception e) {
					_logger.error("Error occured in downloading debug files", e);
				}
            } else {
                bundleFile = null;
            }
        }
        return bundleFile;
    }
}
