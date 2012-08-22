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
package net.rim.ejde.internal.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ide.RIA;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.osgi.framework.Bundle;

public class RIAUtils {
    static private final Logger _log = Logger.getLogger( RIAUtils.class );
    static final private String[] dllNames = { "RIMIDEWin32Util.dll", "RIMUsbJni.dll" }; //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * Gets valid jde home based on the given <code>path</code>. The given <code>path</code> should not contain <b>"bin"</b>.
     *
     * @param path
     *            of jde home
     * @return if the give <code>path</code> is a valid jde home, returns the full path of the jde home; otherwise, returns empty
     *         string.
     */
    static public String getValidJDEHome( String path ) {
        String root = path;
        if( StringUtils.isEmpty( root ) ) {
            return StringUtils.EMPTY;
        }
        // if "/bin" is already appended, we do not append it again
        if( !path.endsWith( IConstants.BIN_FOLD_NAME ) )
            root = String.format( "%s%s%s", root, File.separator, IConstants.BIN_FOLD_NAME ); //$NON-NLS-1$
        if( RIA.validateHomePath( root ) )
            return root;
        return StringUtils.EMPTY;
    }

    public static void initDLLs() {
        IPath dllStoreLocation = ContextManager.PLUGIN.getStateLocation().append( "installDlls" ); //$NON-NLS-1$
        File dllStoreFile = dllStoreLocation.toFile();

        if( !dllStoreFile.exists() )
            dllStoreFile.mkdir();

        InputStream inputStream;
        OutputStream outputStream;
        File dllFile;
        byte[] buf;
        int numbytes;
        URL bundUrl;

        for( String dllFileName : dllNames ) {
            inputStream = null;
            outputStream = null;

            try {
                dllFile = dllStoreLocation.append( dllFileName ).toFile();
                Bundle bundle = ContextManager.PLUGIN.getBundle();
                if( !dllFile.exists() || bundle.getLastModified() > dllFile.lastModified() ) {
                    bundUrl = bundle.getResource( dllFileName );

                    if( bundUrl == null )
                        continue;

                    inputStream = bundUrl.openStream();
                    outputStream = new FileOutputStream( dllFile );
                    buf = new byte[ 4096 ];
                    numbytes = 0;

                    while( ( numbytes = inputStream.read( buf ) ) > 0 )
                        outputStream.write( buf, 0, numbytes );
                }
            } catch( IOException t ) {
                _log.error( t.getMessage(), t );
            } finally {
                try {
                    if( inputStream != null )
                        inputStream.close();

                    if( outputStream != null )
                        outputStream.close();
                } catch( IOException t ) {
                    _log.error( t.getMessage(), t );
                }
            }
        } // end for
    } // end initRIADLLs

    public static boolean canSwitchRIA() {
        RIA ria = RIA.getCurrentDebugger();
        if( ria == null ) {
            return true;
        }
        if( ria.isDebuggerAttached() || ria.isSimulatorRunning() ) {
            return false;
        }
        return true;
    }
}
