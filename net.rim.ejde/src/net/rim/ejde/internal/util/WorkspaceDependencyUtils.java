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
import java.util.ArrayList;
import java.util.List;

import net.rim.ejde.internal.core.IConstants;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.UserLibrary;
import org.eclipse.jdt.internal.core.UserLibraryClasspathContainer;
import org.eclipse.jdt.internal.core.UserLibraryManager;

/**
 */
public class WorkspaceDependencyUtils implements IConstants {

    static private Logger _log = Logger.getLogger( WorkspaceDependencyUtils.class );
    public static final String NET_RIM_API_JAR = "net_rim_api.jar";
    public static final String BLACIBERRY_LIB_PREFIX = "BlackBerry_Library_";

    /**
   *
   */
    static public IClasspathEntry[] toClasspathEntries( File[] jarFiles ) {
        // check the bounds
        if( null == jarFiles || 0 == jarFiles.length ) {
            return new IClasspathEntry[] {};
        }

        /*
         * business rule; enforcement for the jdw files who's import section is tagged as '<none>'
         */
        if( 1 == jarFiles.length && NONE_CLASSPATH_STRING.equalsIgnoreCase( jarFiles[ 0 ].getName() ) ) {
            return new IClasspathEntry[] {};
        }

        String importJarPath;
        IAccessRule[] accessRules = null;
        IPath importJarLocation;
        IClasspathAttribute[] classpathAttributes = {};
        String jdeDocsLocation = null;
        File docFile;
        IClasspathAttribute javadocClasspathAttribute;
        IClasspathEntry entry;

        List< IClasspathEntry > classpathEntries = new ArrayList< IClasspathEntry >();
        // jdeDocsLocation = RimCore.MetaContext.getLegacyJDEDocs();

        if( StringUtils.isNotBlank( jdeDocsLocation ) ) {
            docFile = new File( jdeDocsLocation );
            javadocClasspathAttribute = JavaCore.newClasspathAttribute( IClasspathAttribute.JAVADOC_LOCATION_ATTRIBUTE_NAME,
                    docFile.toURI().toString() );
            classpathAttributes = new IClasspathAttribute[] { javadocClasspathAttribute };
        }

        for( File importJar : jarFiles ) {
            // filter out "<none>" and "net_rim_api.jar"
            if( importJar.getName().equalsIgnoreCase( NONE_CLASSPATH_STRING )
                    || importJar.getName().equalsIgnoreCase( NET_RIM_API_JAR ) ) {
                continue;
            }
            importJarPath = EnvVarUtils.replaceRIAEnvVars( importJar.getPath() );
            importJarLocation = new Path( importJarPath );
            IPath sourceJarPath = ImportUtils.getSourceJarPath( importJarLocation );
            if( sourceJarPath == null || sourceJarPath.isEmpty() || !sourceJarPath.toFile().exists() ) {
                entry = JavaCore.newLibraryEntry( importJarLocation, null, null, accessRules, classpathAttributes, false );
            } else {
                entry = JavaCore
                        .newLibraryEntry( importJarLocation, sourceJarPath, null, accessRules, classpathAttributes, false );
            }
            classpathEntries.add( entry );
        }
        return classpathEntries.toArray( new IClasspathEntry[ classpathEntries.size() ] );
    }

    /**
     * Generates a valid name of the BlackBerry user library for the given <code>workspaceName</code>.
     *
     * @param workspaceName
     * @return
     */
    static public String generateBBLibName( String workspaceName ) {
        UserLibraryManager userLibMgr = JavaModelManager.getUserLibraryManager();
        UserLibrary rimLibs;
        String libName = BLACIBERRY_LIB_PREFIX + workspaceName;
        for( int i = 1; i < 100; i++ ) {
            rimLibs = userLibMgr.getUserLibrary( libName );
            if( rimLibs != null ) {
                libName = BLACIBERRY_LIB_PREFIX + workspaceName + "_" + i;
            } else {
                return libName;
            }
        }

        return IConstants.EMPTY_STRING;
    }

    /**
     * @throws CoreException
     *
     */
    static public void storeDependenciesAsUserLibrary( File[] jarFiles, String userLibrary ) throws CoreException {
        if( null == jarFiles || 0 == jarFiles.length ) {
            return;
        }

        if( null == userLibrary )
            throw new IllegalArgumentException();

        IClasspathEntry[] classPathEntries = toClasspathEntries( jarFiles );

        storeDependenciesAsUserLibrary( classPathEntries, userLibrary );
    }

    /**
     *
     * @param classPathEntries
     * @param userLibrary
     * @throws CoreException
     *
     */
    static public void storeDependenciesAsUserLibrary( IClasspathEntry[] classPathEntries, String userLibrary )
            throws CoreException {
        if( classPathEntries == null || classPathEntries.length == 0 ) {
            return;
        }
        UserLibraryManager userLibMgr = JavaModelManager.getUserLibraryManager();
        UserLibrary rimLibs = userLibMgr.getUserLibrary( userLibrary );

        boolean isSysLib = false;

        if( rimLibs != null ) {
            // this should not happen
            throw new CoreException( StatusFactory.createErrorStatus( "BlackBerry user library already exist." ) );
        }

        String classpathEntriesSequence = "";

        for( IClasspathEntry classpathEntry : classPathEntries ) {
            classpathEntriesSequence = "<" + classpathEntry.toString() + ">";
        }

        _log.debug( "Storing User-Library [" + userLibrary + "] as [" + classpathEntriesSequence + "]" );

        userLibMgr.setUserLibrary( userLibrary, classPathEntries, isSysLib );
    }

    /**
     *
     */
    static public void removeUserLibrary( String userLibrary ) {
        UserLibraryManager userLibMgr = JavaModelManager.getUserLibraryManager();

        if( null != userLibrary ) {
            userLibMgr.removeUserLibrary( userLibrary );
        }
    }

    /**
   *
   */
    static public IClasspathEntry[] getClasspathEntriesForUserLibrary( String userLibrary ) {
        IClasspathEntry[] result = null;

        UserLibraryManager userLibMgr = JavaModelManager.getUserLibraryManager();
        UserLibrary rimLibs = userLibMgr.getUserLibrary( userLibrary );

        if( null == rimLibs )
            return new IClasspathEntry[] {};

        result = rimLibs.getEntries();

        if( 0 < result.length )
            return result;

        return new IClasspathEntry[] {};
    }

    static public void addUserLibraryToProject( String userLibrary, IJavaProject iJavaProject, IProgressMonitor monitor ) {
        UserLibrary library = JavaModelManager.getUserLibraryManager().getUserLibrary( userLibrary );

        if( null != library && null != iJavaProject ) {
            UserLibraryClasspathContainer container = new UserLibraryClasspathContainer( userLibrary );

            IPath path = new Path( JavaCore.USER_LIBRARY_CONTAINER_ID ).append( userLibrary );

            try {
                JavaCore.setClasspathContainer( path, new IJavaProject[] { iJavaProject },
                        new IClasspathContainer[] { container }, null == monitor ? new NullProgressMonitor()
                                : monitor instanceof SubProgressMonitor ? monitor : new SubProgressMonitor( monitor, 1 ) );
            } catch( Throwable e ) {
                _log.error( e.getMessage(), e );
            } finally {
                monitor.done();
            }
        }
    }

    static public void addUserLibraryToProjects( String userLibrary, IJavaProject[] iJavaProjects, IProgressMonitor monitor ) {
        UserLibrary library = JavaModelManager.getUserLibraryManager().getUserLibrary( userLibrary );

        if( null != library && null != iJavaProjects && 0 < iJavaProjects.length ) {
            UserLibraryClasspathContainer[] containers = new UserLibraryClasspathContainer[ iJavaProjects.length ];

            IPath path;

            path = new Path( JavaCore.USER_LIBRARY_CONTAINER_ID ).append( userLibrary );

            for( int i = 0; i < iJavaProjects.length; i++ )
                containers[ i ] = new UserLibraryClasspathContainer( userLibrary );

            try {
                JavaCore.setClasspathContainer( path, iJavaProjects, containers, null == monitor ? new NullProgressMonitor()
                        : monitor instanceof SubProgressMonitor ? monitor : new SubProgressMonitor( monitor, 1 ) );
            } catch( Throwable e ) {
                _log.error( e.getMessage(), e );
            } finally {
                monitor.done();
            }
        }
    }
}
