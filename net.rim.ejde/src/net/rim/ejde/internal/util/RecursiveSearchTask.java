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
import java.io.FileFilter;

public class RecursiveSearchTask implements Runnable {
    public interface ISearchDialog {

        boolean isCanceled();

        void updateProgress( String name );

    }

    private class RecursiveSearchFilter implements FileFilter {
        String _target;

        RecursiveSearchFilter( String target ) {
            _target = target;
        }

        public boolean accept( File pathName ) {
            if( pathName.isDirectory() ) {
                return true;
            }
            return pathName.getName().equals( _target );
        }
    }

    private ISearchDialog _dialog;
    private final File _dir;
    private final RecursiveSearchFilter _filter;
    private File _file;

    public RecursiveSearchTask( ISearchDialog dialog, File dir, String name ) {
        _dialog = dialog;
        _dir = dir;
        _filter = new RecursiveSearchFilter( name );
    }

    public File getFile() {
        return _file;
    }

    public void run() {
        _file = recursiveSearch( _dir );
    }

    private File recursiveSearch( File dir ) {
        if( _dialog.isCanceled() ) {
            return null;
        }
        _dialog.updateProgress( dir.getName() );

        // _label.setText( dir.getName() );
        File[] files = dir.listFiles( _filter );
        if( files == null )
            return null;
        for( int i = 0; i < files.length; ++i ) {
            File f = files[ i ];
            if( f.isFile() ) {
                return f;
            }
        }
        for( int i = 0; i < files.length; ++i ) {
            File f = files[ i ];
            if( f.isDirectory() ) {
                f = recursiveSearch( f );
                if( f != null )
                    return f;
            }
        }
        return null;
    }
}
