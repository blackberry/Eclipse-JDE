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
package net.rim.ejde.internal.sourcelookup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.ui.launchers.LaunchUtils;
import net.rim.ejde.internal.util.NatureUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry2;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardSourcePathProvider;

public class RIMSourcePathProvider extends StandardSourcePathProvider {

    private static Logger log = Logger.getLogger( RIMSourcePathProvider.class );

    public final static String RIM_SOURCEPATH_PROVIDER_ID = "net.rim.ejde.sourcelookup.RIMSourcePathProvider";
    public static final String DEFAIULT_TYPE_ID = "org.eclipse.jdt.launching.classpathentry.defaultClasspath"; //$NON-NLS-1$
    public static final String STRING_VARIABLE_TYPE_ID = "org.eclipse.jdt.launching.classpathentry.variableClasspathEntry";

    /*
     * (non-Javadoc)
     *
     * @seeorg.eclipse.jdt.launching.IRuntimeClasspathProvider#
     * computeUnresolvedClasspath(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public IRuntimeClasspathEntry[] computeUnresolvedClasspath( ILaunchConfiguration configuration ) throws CoreException {
        boolean useDefault = configuration.getAttribute( IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true );
        IRuntimeClasspathEntry[] entries = null;

        if( useDefault ) {
            IJavaProject[] projects = getJavaProjects( configuration );
            entries = new IRuntimeClasspathEntry[ 0 ];

            for( int j = 0; j < projects.length; j++ ) {
                IJavaProject proj = projects[ j ];
                IRuntimeClasspathEntry[] subEntries = JavaRuntime.computeUnresolvedRuntimeClasspath( proj );
                IRuntimeClasspathEntry[] newEntries = new IRuntimeClasspathEntry[ entries.length + subEntries.length ];
                System.arraycopy( entries, 0, newEntries, 0, entries.length );
                System.arraycopy( subEntries, 0, newEntries, entries.length, subEntries.length );
                entries = newEntries;
            }
        } else {
            // recover persisted source path
            entries = recoverRuntimePath( configuration, IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH );
        }

        return entries;
    }

    /**
     * Computes and returns the default unresolved runtime classpath for the given project.
     *
     * @return runtime classpath entries
     * @exception CoreException
     *                if unable to compute the runtime classpath
     * @see IRuntimeClasspathEntry
     */
    public static IRuntimeClasspathEntry[] computeUnresolvedRuntimeClasspath( IJavaProject project ) throws CoreException {
        IClasspathEntry[] entries = project.getRawClasspath();
        List< IRuntimeClasspathEntry > classpathEntries = new ArrayList< IRuntimeClasspathEntry >();
        for( int i = 0; i < entries.length; i++ ) {
            IClasspathEntry entry = entries[ i ];
            switch( entry.getEntryKind() ) {
                case IClasspathEntry.CPE_CONTAINER:
                    IClasspathContainer container = JavaCore.getClasspathContainer( entry.getPath(), project );
                    if( container != null ) {
                        switch( container.getKind() ) {
                            case IClasspathContainer.K_APPLICATION:
                                // don't look at application entries
                                break;
                            case IClasspathContainer.K_DEFAULT_SYSTEM:
                                classpathEntries.add( JavaRuntime.newRuntimeContainerClasspathEntry( container.getPath(),
                                        IRuntimeClasspathEntry.STANDARD_CLASSES, project ) );
                                break;
                            case IClasspathContainer.K_SYSTEM:
                                classpathEntries.add( JavaRuntime.newRuntimeContainerClasspathEntry( container.getPath(),
                                        IRuntimeClasspathEntry.BOOTSTRAP_CLASSES, project ) );
                                break;
                        }
                    }
                    break;
                case IClasspathEntry.CPE_VARIABLE:
                    if( JavaRuntime.JRELIB_VARIABLE.equals( entry.getPath().segment( 0 ) ) ) {
                        IRuntimeClasspathEntry jre = JavaRuntime.newVariableRuntimeClasspathEntry( entry.getPath() );
                        jre.setClasspathProperty( IRuntimeClasspathEntry.STANDARD_CLASSES );
                        classpathEntries.add( jre );
                    }
                    break;
                default:
                    break;
            }
        }
        classpathEntries.add( JavaRuntime.newDefaultProjectClasspathEntry( project ) );
        return classpathEntries.toArray( new IRuntimeClasspathEntry[ classpathEntries.size() ] );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.IRuntimeClasspathProvider#resolveClasspath(
     * org.eclipse.jdt.launching.IRuntimeClasspathEntry[], org.eclipse.debug.core.ILaunchConfiguration)
     */
    public IRuntimeClasspathEntry[] resolveClasspath( IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration )
            throws CoreException {
        List< IRuntimeClasspathEntry > all = new UniqueList( entries.length );
        for( int i = 0; i < entries.length; i++ ) {
            try {
                switch( entries[ i ].getType() ) {
                    case IRuntimeClasspathEntry.PROJECT:
                        // a project resolves to itself for source lookup
                        // (rather
                        // than the class file output locations)
                        all.add( entries[ i ] );
                        break;
                    case IRuntimeClasspathEntry.OTHER:
                        IRuntimeClasspathEntry2 entry = (IRuntimeClasspathEntry2) entries[ i ];
                        String typeId = entry.getTypeId();
                        IRuntimeClasspathEntry[] res = null;
                        if( typeId.equals( DEFAIULT_TYPE_ID ) ) {
                            // add the resolved children of the project
                            IRuntimeClasspathEntry[] children = entry.getRuntimeClasspathEntries( configuration );
                            res = JavaRuntime.resolveSourceLookupPath( children, configuration );
                        } else {
                            res = JavaRuntime.resolveRuntimeClasspathEntry( entry, configuration );
                        }
                        if( res != null ) {
                            for( int j = 0; j < res.length; j++ ) {
                                all.add( res[ j ] );
                                addManifestReferences( res[ j ], all );
                            }
                        }
                        break;
                    default:
                        IRuntimeClasspathEntry[] resolved = JavaRuntime
                                .resolveRuntimeClasspathEntry( entries[ i ], configuration );
                        for( int j = 0; j < resolved.length; j++ ) {
                            all.add( resolved[ j ] );
                            addManifestReferences( resolved[ j ], all );
                        }
                        break;
                }
            } catch( CoreException e ) {
                // if an exception is caught, we will still try to resolve the
                // rest classpathes
                // TODO: should we pop-up a dialog to display the error
                // messages? or display the
                // exceptions in problem view?
                log.error( e );
            }
        }
        return all.toArray( new IRuntimeClasspathEntry[ all.size() ] );
    }

    /**
     * Return the <code>IJavaProject</code> referenced in the specified configuration or <code>null</code> if none.
     *
     * @exception CoreException
     *                if the referenced Java project does not exist
     */
    private IJavaProject[] getJavaProjects( ILaunchConfiguration configuration ) throws CoreException {
        Set< IProject > iProjects = LaunchUtils.getProjectsFromConfiguration( configuration );
        List< IJavaProject > javaProjects = new ArrayList< IJavaProject >();
        for( Iterator< IProject > iterator = iProjects.iterator(); iterator.hasNext(); ) {
            IProject project = iterator.next();
            if( NatureUtils.hasBBNature( project ) ) {
                IJavaProject javaProject = JavaCore.create( project );
                javaProjects.add( javaProject );
                if( javaProject != null && javaProject.getProject().exists() && !javaProject.getProject().isOpen() ) {
                    handldError( "Project is closed.", IJavaLaunchConfigurationConstants.ERR_PROJECT_CLOSED, null );
                }
                if( ( javaProject == null ) || !javaProject.exists() ) {
                    handldError( "Project was not found.", IJavaLaunchConfigurationConstants.ERR_NOT_A_JAVA_PROJECT, null );
                }
            }
        }
        return javaProjects.toArray( new IJavaProject[ 0 ] );
    }

    /**
     * Throws a core exception.
     *
     * @param message
     *            the error message
     * @param code
     *            severity code
     * @param exception
     */
    private static void handldError( String message, int code, Throwable exception ) throws CoreException {
        throw new CoreException( new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, code, message, exception ) );
    }

    /*
     * An ArrayList that acts like a set -i.e. does not allow duplicate items.
     */
    class UniqueList extends ArrayList {

        public UniqueList( int length ) {
            super( length );
        }

        public UniqueList() {
            super();
        }

        public boolean add( Object o ) {
            if( !super.contains( o ) )
                return super.add( o );
            return false;
        }
    }
}
