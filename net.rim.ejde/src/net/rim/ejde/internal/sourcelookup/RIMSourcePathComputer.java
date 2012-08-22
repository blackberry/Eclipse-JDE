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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;
import net.rim.ejde.internal.ui.launchers.LaunchUtils;
import net.rim.ide.RIA;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaProjectSourceContainer;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourcePathComputer;
import org.eclipse.jdt.launching.sourcelookup.containers.PackageFragmentRootSourceContainer;

/**
 * An instance of this class is used to compute the source paths.
 */
public class RIMSourcePathComputer extends JavaSourcePathComputer {
    static private Logger _log = Logger.getLogger( RIMSourcePathComputer.class );

    public ISourceContainer[] computeSourceContainers( ILaunchConfiguration configuration, IProgressMonitor monitor )
            throws CoreException {
        _log.trace( "Entering RIMSourcePathComputer.computerSourceContainers()." );

        // call super's method
        IRuntimeClasspathEntry[] entries = JavaRuntime.computeUnresolvedSourceLookupPath( configuration );

        IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveSourceLookupPath( entries, configuration );
        List< IRuntimeClasspathEntry > jarEntriesWithSourceAttached = new ArrayList< IRuntimeClasspathEntry >();
        List< IRuntimeClasspathEntry > jarEntriesWithoutSourceAttached = new ArrayList< IRuntimeClasspathEntry >();
        List< IRuntimeClasspathEntry > otherEntries = new ArrayList< IRuntimeClasspathEntry >();
        // split entries into three types: jar with source attached, jar without source attached, others
        splitClasspathEntries( resolved, jarEntriesWithSourceAttached, jarEntriesWithoutSourceAttached, otherEntries );
        ArrayList< ISourceContainer > sourceContainers = new ArrayList< ISourceContainer >();
        // other entries are added at the beginning of the source lookup path, then jars with source attached,
        // then RIM source paths, then jars without source attached and then rim_net_api.jar
        setContainerToList(
                JavaRuntime.getSourceContainers( otherEntries.toArray( new IRuntimeClasspathEntry[ otherEntries.size() ] ) ),
                sourceContainers );
        setContainerToList( JavaRuntime.getSourceContainers( jarEntriesWithSourceAttached
                .toArray( new IRuntimeClasspathEntry[ jarEntriesWithSourceAttached.size() ] ) ), sourceContainers );
        // add all RIM internal source folders to the source lookup path
        boolean riaSourcesFound = false;
        IVMInstall vm = LaunchUtils.getVMFromConfiguration( configuration );
        RIA ria = ContextManager.PLUGIN.getRIA( vm.getInstallLocation().getAbsolutePath() );
        if( ria != null && ria.getInRIMMode() ) {
            File[] files = ria.getSourcePath();
            for( int i = 0; i < files.length; i++ ) {
                _log.trace( "Adding directory " + files[ i ].getName() + " to source lookup path" );
                sourceContainers.add( new RIMDirSourceContainer( files[ i ], true ) );
                riaSourcesFound = true;
            }
        }
        // add jars without source attached
        setContainerToList( JavaRuntime.getSourceContainers( jarEntriesWithoutSourceAttached
                .toArray( new IRuntimeClasspathEntry[ jarEntriesWithoutSourceAttached.size() ] ) ), sourceContainers );

        /*
         * If an ISourceContainer in the sourceContainers list is a PackageFragmentRootSourceContainer, then it has source
         * attached. If it is a ExternalSrchiveSourceContainer, then it does not. If RIA source files were found and a
         * PackageFragmentRootSourceContainer is found in the list, and it is net_rim_api, then source is attached, so move it to
         * the end of the list in order for debugging with mirrors to work
         */
        for( ISourceContainer iSourceContainer : sourceContainers ) {
            if( riaSourcesFound && iSourceContainer instanceof PackageFragmentRootSourceContainer ) {
                if( "net_rim_api.jar".equals( iSourceContainer.getName() ) ) {
                    sourceContainers.remove( iSourceContainer );
                    sourceContainers.add( iSourceContainer );
                    break;
                }
            }
        }

        _log.trace( "There are " + sourceContainers.size() + " source containers." );
        return sourceContainers.toArray( new ISourceContainer[ 0 ] );
    }

    private void splitClasspathEntries( IRuntimeClasspathEntry[] entries,
            List< IRuntimeClasspathEntry > jarEntriesWithSourceAttached,
            List< IRuntimeClasspathEntry > jarEntriesWithoutSourceAttached, List< IRuntimeClasspathEntry > otherEntries ) {
        for( int i = 0; i < entries.length; i++ ) {
            if( entries[ i ].getType() != IRuntimeClasspathEntry.ARCHIVE ) {
                otherEntries.add( entries[ i ] );
                continue;
            }
            if( entries[ i ].getSourceAttachmentLocation() == null )
                jarEntriesWithoutSourceAttached.add( entries[ i ] );
            else
                jarEntriesWithSourceAttached.add( entries[ i ] );
        }
    }

    private void setContainerToList( ISourceContainer[] containers, List< ISourceContainer > list ) {
        // add the java project ones first
        for( int i = 0; i < containers.length; i++ ) {
            if( containers[ i ] instanceof JavaProjectSourceContainer )
                list.add( 0, containers[ i ] );
            else
                list.add( containers[ i ] );
        }
    }

    protected void addProjectSourcePaths( List< ISourceContainer > sourceContainers ) throws CoreException {
        // Add all RIM projects to the source lookup path
        for( IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects() ) {
            if( project.hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                IJavaProject javaProject = JavaCore.create( project );
                _log.trace( "Adding project " + project.getName() + " to source lookup path" );
                sourceContainers.add( new JavaProjectSourceContainer( javaProject ) );
            }
        }
    }
}
