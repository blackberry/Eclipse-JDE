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
package net.rim.ejde.internal.model.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.debug.ui.jres.VMDetailsDialog;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;

/**
 * Provides the content for the JREs selection/edit viewer
 *
 * @see {@link ITreeContentProvider}
 * @see {@link VMDetailsDialog}
 * @see {@link VMLibraryBlock}
 * @see {@link LibraryLocation}
 * @see {@link LibraryStandin}
 */
public class LibraryContentProvider implements ITreeContentProvider {

    private Viewer fViewer;

    /**
     * Represents a subelement of a <code>LibraryStandin</code>
     */
    public class SubElement {

        public static final int JAVADOC_URL = 1;
        public static final int SOURCE_PATH = 2;

        private LibraryStandin fParent;
        private int fType;

        public SubElement( LibraryStandin parent, int type ) {
            fParent = parent;
            fType = type;
        }

        public LibraryStandin getParent() {
            return fParent;
        }

        public int getType() {
            return fType;
        }

        public void remove() {
            switch( fType ) {
                case JAVADOC_URL:
                    fParent.setJavadocLocation( null );
                    break;
                case SOURCE_PATH:
                    fParent.setSystemLibrarySourcePath( Path.EMPTY );
                    break;
            }
        }
    }

    private HashMap fChildren = new HashMap();

    private LibraryStandin[] fLibraries = new LibraryStandin[ 0 ];

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
        fChildren.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object,
     * java.lang.Object)
     */
    public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
        fViewer = viewer;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements( Object inputElement ) {
        return fLibraries;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    public Object[] getChildren( Object parentElement ) {
        if( parentElement instanceof LibraryStandin ) {
            LibraryStandin standin = (LibraryStandin) parentElement;
            Object[] children = (Object[]) fChildren.get( standin );
            if( children == null ) {
                children = new Object[] { new SubElement( standin, SubElement.SOURCE_PATH ),
                        new SubElement( standin, SubElement.JAVADOC_URL ) };
                fChildren.put( standin, children );
            }
            return children;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    public Object getParent( Object element ) {
        if( element instanceof SubElement ) {
            return ( (SubElement) element ).getParent();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    public boolean hasChildren( Object element ) {
        return element instanceof LibraryStandin;
    }

    /**
     * Sets the array of libraries to be the specified array of libraries
     *
     * @param libs
     *            the new array of libraries to set
     */
    public void setLibraries( LibraryLocation[] libs ) {
        fLibraries = new LibraryStandin[ libs.length ];
        for( int i = 0; i < libs.length; i++ ) {
            fLibraries[ i ] = new LibraryStandin( libs[ i ] );
        }
        if( fViewer != null ) {
            fViewer.refresh();
        }
    }

    /**
     * Returns the listing of <code>LibraryLocation</code>s
     *
     * @return the listing of <code>LibraryLocation</code>s, or an empty array, never <code>null</code>
     */
    public LibraryLocation[] getLibraries() {
        LibraryLocation[] locations = new LibraryLocation[ fLibraries.length ];
        for( int i = 0; i < locations.length; i++ ) {
            locations[ i ] = fLibraries[ i ].toLibraryLocation();
        }
        return locations;
    }

    /**
     * Returns the list of libraries in the given selection. SubElements are replaced by their parent libraries.
     *
     * @param selection
     *            the current selection
     *
     * @return the current set of selected <code>LibraryStandin</code>s from the current viewer selection, or an empty set, never
     *         <code>null</code>
     */
    private Set getSelectedLibraries( IStructuredSelection selection ) {
        Set libraries = new HashSet();
        for( Iterator iter = selection.iterator(); iter.hasNext(); ) {
            Object element = iter.next();
            if( element instanceof LibraryStandin ) {
                libraries.add( element );
            } else if( element instanceof SubElement ) {
                libraries.add( ( (SubElement) element ).getParent() );
            }
        }
        return libraries;
    }

    /**
     * Move the libraries of the given selection up.
     *
     * @param selection
     *            the current viewer selection
     */
    public void up( IStructuredSelection selection ) {
        Set libraries = getSelectedLibraries( selection );
        for( int i = 0; i < fLibraries.length - 1; i++ ) {
            if( libraries.contains( fLibraries[ i + 1 ] ) ) {
                LibraryStandin temp = fLibraries[ i ];
                fLibraries[ i ] = fLibraries[ i + 1 ];
                fLibraries[ i + 1 ] = temp;
            }
        }
        fViewer.refresh();
        fViewer.setSelection( selection );
    }

    /**
     * Move the libraries of the given selection down.
     *
     * @param selection
     *            the current viewer selection
     */
    public void down( IStructuredSelection selection ) {
        Set libraries = getSelectedLibraries( selection );
        for( int i = fLibraries.length - 1; i > 0; i-- ) {
            if( libraries.contains( fLibraries[ i - 1 ] ) ) {
                LibraryStandin temp = fLibraries[ i ];
                fLibraries[ i ] = fLibraries[ i - 1 ];
                fLibraries[ i - 1 ] = temp;
            }
        }
        fViewer.refresh();
        fViewer.setSelection( selection );
    }

    /**
     * Remove the libraries contained in the given selection.
     *
     * @param selection
     *            the current viewer selection
     */
    public void remove( IStructuredSelection selection ) {
        List newLibraries = new ArrayList();
        for( int i = 0; i < fLibraries.length; i++ ) {
            newLibraries.add( fLibraries[ i ] );
        }
        Iterator iterator = selection.iterator();
        while( iterator.hasNext() ) {
            Object element = iterator.next();
            if( element instanceof LibraryStandin ) {
                newLibraries.remove( element );
            } else {
                SubElement subElement = (SubElement) element;
                subElement.remove();
            }
        }
        fLibraries = (LibraryStandin[]) newLibraries.toArray( new LibraryStandin[ newLibraries.size() ] );
        fViewer.refresh();
    }

    /**
     * Add the given libraries before the selection, or after the existing libraries if the selection is empty.
     *
     * @param libs
     *            the array of <code>LibraryLocation</code>s to add
     * @param selection
     *            the selection to add the new libraries before in the list, or after if the selection is empty.
     */
    public void add( LibraryLocation[] libs, IStructuredSelection selection ) {
        List newLibraries = new ArrayList( fLibraries.length + libs.length );
        for( int i = 0; i < fLibraries.length; i++ ) {
            newLibraries.add( fLibraries[ i ] );
        }
        List toAdd = new ArrayList( libs.length );
        for( int i = 0; i < libs.length; i++ ) {
            toAdd.add( new LibraryStandin( libs[ i ] ) );
        }
        if( selection.isEmpty() ) {
            newLibraries.addAll( toAdd );
        } else {
            Object element = selection.getFirstElement();
            LibraryStandin firstLib;
            if( element instanceof LibraryStandin ) {
                firstLib = (LibraryStandin) element;
            } else {
                firstLib = ( (SubElement) element ).getParent();
            }
            int index = newLibraries.indexOf( firstLib );
            newLibraries.addAll( index, toAdd );
        }
        fLibraries = (LibraryStandin[]) newLibraries.toArray( new LibraryStandin[ newLibraries.size() ] );
        fViewer.refresh();
        fViewer.setSelection( new StructuredSelection( libs ), true );
    }

    /**
     * Set the given URL as the javadoc location for the libraries contained in the given selection.
     *
     * @param javadocLocation
     *            the new java doc location to set
     * @param selection
     *            the selection of libraries to set the new javadoc location for
     */
    public void setJavadoc( URL javadocLocation, IStructuredSelection selection ) {
        Set libraries = getSelectedLibraries( selection );
        Iterator iterator = libraries.iterator();
        while( iterator.hasNext() ) {
            LibraryStandin standin = (LibraryStandin) iterator.next();
            standin.setJavadocLocation( javadocLocation );
        }
        fViewer.refresh();
    }

    /**
     * Set the given paths as the source info for the libraries contained in the given selection.
     *
     * @param sourceAttachmentPath
     *            the path of the new attachment
     * @param sourceAttachmentRootPath
     *            the root path of the new attachment
     * @param selection
     *            the selection of libraries to set the new paths in
     */
    public void setSourcePath( IPath sourceAttachmentPath, IPath sourceAttachmentRootPath, IStructuredSelection selection ) {
        Set libraries = getSelectedLibraries( selection );
        if( sourceAttachmentPath == null ) {
            sourceAttachmentPath = Path.EMPTY;
        }
        if( sourceAttachmentRootPath == null ) {
            sourceAttachmentRootPath = Path.EMPTY;
        }
        Iterator iterator = libraries.iterator();
        while( iterator.hasNext() ) {
            LibraryStandin standin = (LibraryStandin) iterator.next();
            standin.setSystemLibrarySourcePath( sourceAttachmentPath );
            standin.setPackageRootPath( sourceAttachmentRootPath );
        }
        fViewer.refresh();
    }

    /**
     * Returns the standin libraries being edited.
     *
     * @return standins
     */
    LibraryStandin[] getStandins() {
        return fLibraries;
    }
}
