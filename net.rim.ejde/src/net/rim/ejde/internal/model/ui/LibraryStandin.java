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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.debug.ui.IJavaDebugUIConstants;
import org.eclipse.jdt.launching.LibraryLocation;

/**
 * Wrapper for an original library location, to support editing.
 *
 */
public final class LibraryStandin {
    private IPath fSystemLibrary;
    private IPath fSystemLibrarySource;
    private IPath fPackageRootPath;
    private URL fJavadocLocation;

    /**
     * Creates a new library standin on the given library location.
     */
    public LibraryStandin( LibraryLocation libraryLocation ) {
        fSystemLibrary = libraryLocation.getSystemLibraryPath();
        setSystemLibrarySourcePath( libraryLocation.getSystemLibrarySourcePath() );
        setPackageRootPath( libraryLocation.getPackageRootPath() );
        setJavadocLocation( libraryLocation.getJavadocLocation() );
    }

    /**
     * Returns the JRE library jar location.
     *
     * @return The JRE library jar location.
     */
    public IPath getSystemLibraryPath() {
        return fSystemLibrary;
    }

    /**
     * Returns the JRE library source zip location.
     *
     * @return The JRE library source zip location.
     */
    public IPath getSystemLibrarySourcePath() {
        return fSystemLibrarySource;
    }

    /**
     * Sets the source location for this library.
     *
     * @param path
     *            path source archive or Path.EMPTY if none
     */
    void setSystemLibrarySourcePath( IPath path ) {
        fSystemLibrarySource = path;
    }

    /**
     * Returns the path to the default package in the sources zip file
     *
     * @return The path to the default package in the sources zip file.
     */
    public IPath getPackageRootPath() {
        return fPackageRootPath;
    }

    /**
     * Sets the root source location within source archive.
     *
     * @param path
     *            path to root source location or Path.EMPTY if none
     */
    void setPackageRootPath( IPath path ) {
        fPackageRootPath = path;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object obj ) {
        if( obj instanceof LibraryStandin ) {
            LibraryStandin lib = (LibraryStandin) obj;
            return getSystemLibraryPath().equals( lib.getSystemLibraryPath() )
                    && equals( getSystemLibrarySourcePath(), lib.getSystemLibrarySourcePath() )
                    && equals( getPackageRootPath(), lib.getPackageRootPath() )
                    && equalsOrNull( getJavadocLocation(), lib.getJavadocLocation() );
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return getSystemLibraryPath().hashCode();
    }

    /**
     * Returns whether the given paths are equal - either may be <code>null</code>.
     *
     * @param path1
     *            path to be compared
     * @param path2
     *            path to be compared
     * @return whether the given paths are equal
     */
    protected boolean equals( IPath path1, IPath path2 ) {
        return equalsOrNull( path1, path2 );
    }

    /**
     * Returns whether the given objects are equal - either may be <code>null</code>.
     *
     * @param o1
     *            object to be compared
     * @param o2
     *            object to be compared
     * @return whether the given objects are equal or both null
     * @since 3.1
     */
    private boolean equalsOrNull( Object o1, Object o2 ) {
        if( o1 == null ) {
            return o2 == null;
        }
        if( o2 == null ) {
            return false;
        }
        return o1.equals( o2 );
    }

    /**
     * Returns the Javadoc location associated with this Library location.
     *
     * @return a url pointing to the Javadoc location associated with this Library location, or <code>null</code> if none
     * @since 3.1
     */
    public URL getJavadocLocation() {
        return fJavadocLocation;
    }

    /**
     * Sets the javadoc location of this library.
     *
     * @param url
     *            The location of the javadoc for <code>library</code> or <code>null</code> if none
     */
    void setJavadocLocation( URL url ) {
        fJavadocLocation = url;
    }

    /**
     * Returns an equivalent library location.
     *
     * @return library location
     */
    LibraryLocation toLibraryLocation() {
        return new LibraryLocation( getSystemLibraryPath(), getSystemLibrarySourcePath(), getPackageRootPath(),
                getJavadocLocation() );
    }

    /**
     * Returns a status for this library describing any error states
     *
     * @return
     */
    IStatus validate() {
        if( !getSystemLibraryPath().toFile().exists() ) {
            return new Status( IStatus.ERROR, IJavaDebugUIConstants.PLUGIN_ID, IJavaDebugUIConstants.INTERNAL_ERROR,
                    "System library does not exist: " + getSystemLibraryPath().toOSString(), null ); //$NON-NLS-1$
        }
        IPath path = getSystemLibrarySourcePath();
        if( !path.isEmpty() ) {
            if( !path.toFile().exists() ) {
                // check for workspace resource
                IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember( path );
                if( resource == null || !resource.exists() ) {
                    return new Status( IStatus.ERROR, IJavaDebugUIConstants.PLUGIN_ID, IJavaDebugUIConstants.INTERNAL_ERROR,
                            "Source attachment does not exist: " + path.toOSString(), null ); //$NON-NLS-1$
                }
            }
        }
        return Status.OK_STATUS;
    }

}
