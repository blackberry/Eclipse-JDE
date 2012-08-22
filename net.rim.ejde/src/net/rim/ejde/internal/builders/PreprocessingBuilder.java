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
package net.rim.ejde.internal.builders;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import net.rim.ejde.external.sourceMapper.SourceMapperAccess;
import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.core.IRIMMarker;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.PreprocessorTag;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.model.preferences.PreprocessorPreferences;
import net.rim.ejde.internal.util.EnvVarUtils;
import net.rim.ejde.internal.util.ImportUtils;
import net.rim.ejde.internal.util.PackageUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.ResourceBuilderUtils;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ide.RIA;
import net.rim.ide.Workspace;
import net.rim.tools.javapp.delegate.IOutputFileCallbackDelegate;
import net.rim.tools.javapp.delegate.IPreprocessingListenerDelegate;
import net.rim.tools.javapp.delegate.JavaPPDelegate;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.osgi.util.NLS;

/**
 * An incremental builder implementation that provides preprocess support as defined for the Antenna Ant tools.<br/>
 */
public class PreprocessingBuilder extends IncrementalProjectBuilder {
    public static final String BUILDER_ID = "net.rim.ejde.internal.builder.BlackBerryPreprocessBuilder"; //$NON-NLS-1$
    public static final String PREPROCESSED_FILE_FOLDER_NAME = "project_preprocessed_folder"; //$NON-NLS-1$
    private static final Logger _log = Logger.getLogger( PreprocessingBuilder.class );
    final static public QualifiedName NOT_BUILD_BY_JAVA_BUILDER_FLAG_QUALIFIED_NAME = new QualifiedName(
            ContextManager.PLUGIN_ID, "NotBuiltByJavaBuilders" ); //$NON-NLS-1$

    private JavaPPDelegate _javaPP = new JavaPPDelegate();

    private IFolder _preprocessedFolder;

    /**
     * Construct a new builder instance.
     */
    public PreprocessingBuilder() {
        super();
    }

    /**
     * @see org.eclipse.core.resources.IncrementalProjectBuilder#build(int, java.util.Map,
     *      org.eclipse.core.runtime.IProgressMonitor)
     */
    protected IProject[] build( int kind, Map args, IProgressMonitor monitor ) throws CoreException {
        _log.trace( "Entering PreprocessingBuilder build();kind=" + kind ); //$NON-NLS-1$
        IProject project = getProject();
        _preprocessedFolder = createPreprocessedFolder( getProject(), new NullProgressMonitor() );
        // fixed MKS516466, after importing an existing project, the .locale_interface and .preprocessed folder are not marked as
        // derived. we should mark them as derived.
        if( !_preprocessedFolder.isDerived() ) {
            try {
                _preprocessedFolder.setDerived( true );
            } catch( CoreException e ) {
                _log.error( e );
            }
        }
        IFolder localeInterfaceFolderRoot = project.getProject().getFolder(
                ImportUtils.getImportPref( ResourceBuilder.LOCALE_INTERFACES_FOLDER_NAME ) );
        if( localeInterfaceFolderRoot.exists() && !localeInterfaceFolderRoot.isDerived() ) {
            try {
                localeInterfaceFolderRoot.setDerived( true );
            } catch( CoreException e ) {
                _log.error( e );
            }
        }
        IResourceDelta delta = getDelta( getProject() );
        IResourceDelta classpathFile = null;
        if( delta != null ) {
            classpathFile = delta.findMember( new Path( IConstants.CLASSPATH_FILE_NAME ) );
        }
        if( kind == IncrementalProjectBuilder.FULL_BUILD || classpathFile != null ) {
            // if is is a full build, we build all java files
            ResourceDeltaVisitor resourceVisitor = new ResourceDeltaVisitor( monitor );
            project.accept( resourceVisitor );
        } else {
            // if it is not a full build, we only build changed java files
            if( delta != null ) {
                ResourceDeltaVisitor deltaVisitor = new ResourceDeltaVisitor( monitor );
                delta.accept( deltaVisitor );
            }
        }
        _preprocessedFolder.refreshLocal( IResource.DEPTH_INFINITE, monitor );
        _log.trace( "Leaving PreprocessingBuilder build()" ); //$NON-NLS-1$
        return null;
    }

    /**
     * @see org.eclipse.core.resources.IncrementalProjectBuilder#clean(org.eclipse.core.runtime.IProgressMonitor)
     */
    protected void clean( IProgressMonitor monitor ) throws CoreException {
        removePreprocessingMarkers( getProject(), IResource.DEPTH_INFINITE );
        IFolder preprocessedFolder = getProject().getFolder( ImportUtils.getImportPref( PREPROCESSED_FILE_FOLDER_NAME ) );
        if( !preprocessedFolder.exists() )
            return;
        IResource[] members = preprocessedFolder.members();
        for( int i = 0; i < members.length; i++ ) {
            if( members[ i ] instanceof IFile ) {
                members[ i ].delete( true, monitor );
            } else if( members[ i ] instanceof IFolder )
                ( (IFolder) members[ i ] ).delete( true, false, monitor );
            else
                throw new CoreException( StatusFactory.createErrorStatus( NLS.bind( "", members[ i ].getName() ) ) ); //$NON-NLS-1$
        }
    }

    /**
     * Return an appropriate output file for the specified resource. This file is not guaranteed to exist.
     *
     * @param resource
     * @return
     */
    public static IFile getOutputFile( IResource resource ) {
        IContainer sourceFolder = PackageUtils.getSrcFolder( resource );
        if( sourceFolder == null ) {
            return (IFile) resource;
        }
        IPath sourceFolderPath = sourceFolder.getProjectRelativePath();
        IPath resourcePath = resource.getProjectRelativePath();
        IPath firstSegment = resourcePath.removeLastSegments( resourcePath.segmentCount() - 1 );
        IPath projectRelativePath = null;
        if( firstSegment.toString().equals( ImportUtils.getImportPref( PREPROCESSED_FILE_FOLDER_NAME ) ) )
            return (IFile) resource;
        projectRelativePath = new Path( ImportUtils.getImportPref( PREPROCESSED_FILE_FOLDER_NAME ) ).append( resource
                .getProjectRelativePath().removeFirstSegments( sourceFolderPath.segmentCount() ) );
        return resource.getProject().getFile( projectRelativePath );
    }

    void deletePreprocessedFile( IFile file, IProgressMonitor monitor ) throws CoreException {
        IFile preprocessedFile = getOutputFile( file );
        // eclipse resource might not be refreshed, we need double check the filesystem
        if( preprocessedFile.exists() )
            preprocessedFile.delete( true, monitor );
    }

    /**
     * Clears previous preprocessor markers from the specified resource.
     *
     * @param resource
     * @param depth
     *
     * @throws CoreException
     */
    private void removePreprocessingMarkers( IResource resource, int depth ) throws CoreException {
        ResourceBuilderUtils.cleanProblemMarkers( resource, new String[] { IRIMMarker.PREPROCESSING_PROBLEM_MARKER }, depth );
    }

    /**
     * Mark the given <code>resource</code> as need to be built by java compiler.
     *
     * @param resource
     * @param needBuild
     *            <code>true</code> mark the given <code>resource</code> as need build, otherwise, mark the given
     *            <code>resource</code> as not need build.
     */
    static synchronized public void setShouldBuiltByJavaBuilder( IResource resource, boolean needBuild ) {
        try {
            _log.trace( "File " + resource.getName() + " is marked as" //$NON-NLS-1$ //$NON-NLS-2$
                    + ( needBuild ? " need to be built by java compiler." : " not need to be built by java compiler." ) ); //$NON-NLS-1$ //$NON-NLS-2$
            String newBuildFlag = needBuild ? "true" : "false";
            String oldBuildFlag = resource.getPersistentProperty( NOT_BUILD_BY_JAVA_BUILDER_FLAG_QUALIFIED_NAME );
            if( ( oldBuildFlag == null ) || !oldBuildFlag.equals( newBuildFlag ) ) {
                resource.setPersistentProperty( NOT_BUILD_BY_JAVA_BUILDER_FLAG_QUALIFIED_NAME, newBuildFlag );
            }
        } catch( CoreException e ) {
            _log.error( e );
        }
    }

    /**
     * Check if the given <code>resource</code> needs to be built.
     *
     * @param file
     * @return
     */
    static synchronized public boolean shouldBuiltByJavaBuilder( IResource resource ) {
        try {
            String value = resource.getPersistentProperty( NOT_BUILD_BY_JAVA_BUILDER_FLAG_QUALIFIED_NAME );
            if( ( value == null ) || value.equals( "true" ) ) {
                return true;
            }
        } catch( CoreException e ) {
            _log.error( e );
            return true;
        }
        return false;
    }

    /**
     * Preprocess the specified java resource.
     *
     * @param resource
     * @param symbols
     * @param monitor
     * @throws CoreException
     */
    private void preprocessResource( final IResource resource, IProgressMonitor monitor ) throws CoreException {
        // remove the old markers and preprocessed file
        removePreprocessingMarkers( resource, IResource.DEPTH_ONE );
        deletePreprocessedFile( (IFile) resource, monitor );
        //
        BlackBerryProperties properties = ContextManager.PLUGIN.getBBProperties( getProject().getName(), false );
        if( properties == null ) {
            _log.error( "Could not find the correspond BlackBerry properties." );
            return;
        }
        // get defined directives
        Vector< String > defines = getDefines( new BlackBerryProject( JavaCore.create( getProject() ), properties ), true );
        // if there is no directive defined, we do not do preprocessing
        if( defines == null || defines.size() == 0 )
            return;
        // check preprocess hook
        if( !SourceMapperAccess.isHookCodeInstalled() && PreprocessorPreferences.getPopForPreprocessHookMissing() ) {
            _log.error( "Preprocessing hook was not installed." ); //$NON-NLS-1$
            ProjectUtils.setPreprocessorHook();
            return;
        } else {
            _log.trace( "Preprocessing file : " + resource.getLocation() ); //$NON-NLS-1$
        }
        // remove the fake preprocess derive
        defines.remove( Workspace.getDefineOptNull() );
        // get java file
        File javaFile = resource.getLocation().toFile();
        Vector< File > javaFiles = new Vector< File >();
        javaFiles.add( javaFile );
        IPreprocessingListenerDelegate listener = new PreprocessingListener( resource );
        IOutputFileCallbackDelegate callback = new OutputFileCallback();
        _javaPP.setPreprocessingListener( listener );
        _javaPP.setOutputFileCallback( callback );
        try {
            _javaPP.preProcess( javaFiles, defines, _preprocessedFolder.getLocation().toFile() );
            resource.touch( monitor );
        } catch( IOException e ) {
            // Is handled by PreprocessingListener
        } catch( Exception e ) {
            try {
                ResourceBuilderUtils.createProblemMarker( resource, IRIMMarker.PREPROCESSING_PROBLEM_MARKER, e.getMessage(), -1,
                        IMarker.SEVERITY_ERROR );
            } catch( CoreException e1 ) {
                _log.error( e1 );
            }
            // if we got any exception, delete the proprocessed file.
            _log.debug( e.getMessage(), e );
            deletePreprocessedFile( (IFile) resource, monitor );
        }
        setShouldBuiltByJavaBuilder( resource, javaFiles.size() != 0 );
    }

    /**
     * Get all preprocess defines: JRE leve, workspace level and project level.
     *
     * @param BBProject
     * @param ignoreInActive
     * @return
     */
    static public Vector< String > getDefines( BlackBerryProject BBProject, boolean ignoreInActive ) {
        List< PreprocessorTag > workspaceDefines = PreprocessorPreferences.getPreprocessDefines();
        PreprocessorTag[] projectDefines = BBProject.getProperties()._compile.getPreprocessorDefines();
        Vector< String > newDefines = new Vector< String >();
        for( PreprocessorTag ppDefine : workspaceDefines ) {
            if( !ignoreInActive || ppDefine.isActive() ) {
                newDefines.add( EnvVarUtils.replaceRIAEnvVars( ppDefine.getPreprocessorDefine() ) );
            }
        }
        for( int i = 0; i < projectDefines.length; i++ ) {
            if( !ignoreInActive || projectDefines[ i ].isActive() ) {
                newDefines.add( EnvVarUtils.replaceRIAEnvVars( projectDefines[ i ].getPreprocessorDefine() ) );
            }
        }
        String cpDefine = VMUtils.getJREDirective( BBProject );
        if( !StringUtils.isBlank( cpDefine ) ) {
            newDefines.add( cpDefine );
        }
        return newDefines;
    }

    private IFolder createPreprocessedFolder( IProject project, IProgressMonitor monitor ) throws CoreException {
        IContainer sourceContainer = project.getFolder( ImportUtils.getImportPref( PREPROCESSED_FILE_FOLDER_NAME ) );
        // refresh the project to reveal the .preprocessed folder
        sourceContainer.refreshLocal( IResource.DEPTH_ZERO, monitor );
        if( !sourceContainer.exists() ) {
            // if the .BlackBerryPreprocessed folder does not exist, create it
            ( (IFolder) sourceContainer ).create( IResource.DERIVED, true, monitor );
        }
        return (IFolder) sourceContainer;
    }

    /**
     * Create a new marker in the specified resource.
     *
     * @param resource
     * @param message
     * @param lineNumber
     * @param severity
     * @throws CoreException
     * @throws BadLocationException
     */
    void createResourceMarker( IResource resource, String message, int lineNumber, int severity ) {
        try {
            ResourceBuilderUtils.createProblemMarker( resource, IRIMMarker.PREPROCESSING_PROBLEM_MARKER, message, lineNumber,
                    severity );
        } catch( Exception e ) {
            _log.error( e );
        }
    }

    protected class PreprocessingListener implements IPreprocessingListenerDelegate {
        IResource _resource;

        public PreprocessingListener( IResource resource ) {
            _resource = resource;
        }

        public void setResource( IResource resource ) {
            _resource = resource;
        }

        public void error( String message, int lineNumber ) {

            createResourceMarker( _resource, message, lineNumber, IMarker.SEVERITY_ERROR );
        }

        public void warning( String message, int lineNumber ) {

            createResourceMarker( _resource, message, lineNumber, IMarker.SEVERITY_WARNING );
        }
    };

    protected class OutputFileCallback implements IOutputFileCallbackDelegate {

        public File getOutputFile( File parent, File javaFile ) {
            String packageName = RIA.getPackage( javaFile );
            if( packageName != null && !packageName.trim().equals( IConstants.EMPTY_STRING ) ) {
                IPath parentPath = new Path( parent.getPath() );
                IPath packagePath = new Path( packageName.replace( '.', '/' ) );
                IFolder packageFolder = _preprocessedFolder.getFolder( packagePath );
                if( !packageFolder.exists() )
                    try {
                        ImportUtils.createFolders( getProject(), packageFolder.getProjectRelativePath(), IResource.DERIVED );
                    } catch( CoreException e ) {
                        _log.error( e );
                    }
                parentPath = parentPath.append( packagePath );
                File parentDir = parentPath.toFile();
                return new File( parentDir, javaFile.getName() );
            }
            return new File( parent, javaFile.getName() );
        }

    }

    /**
     * Delta visitor for visiting changed resource files for resource builder.
     */
    private class ResourceDeltaVisitor extends BasicBuilderResourceDeltaVisitor {

        public ResourceDeltaVisitor( IProgressMonitor monitor ) {
            super( monitor );
        }

        @Override
        protected void removeResource( IResource resource, IProgressMonitor monitor ) throws CoreException {
            if( !( resource instanceof IFile ) )
                return;
            deletePreprocessedFile( (IFile) resource, monitor );
        }

        @Override
        protected void buildResource( IResource resource, IProgressMonitor monitor ) throws CoreException {
            preprocessResource( resource, monitor );
        }

        /**
         * Checks if the given <code>resource</code> is a java file which needs to be proprocessed.
         *
         * @param resource
         * @return
         */
        protected boolean needBuild( IResource resource ) {
            if( resource == null ) {
                return false;
            }
            if( !resource.exists() ) {
                return false;
            }
            // check if it's an IFile
            if( !( resource instanceof IFile ) )
                return false;
            // check if it's a java file
            if( !PackageUtils.hasJavaExtension( resource.getName() ) ) {
                return false;
            }
            // we do not process a resource which is not on the classpath
            if( !JavaCore.create( resource.getProject() ).isOnClasspath( resource ) ) {
                try {
                    deletePreprocessedFile( (IFile) resource, new NullProgressMonitor() );
                } catch( CoreException e ) {
                    _log.error( e.getMessage() );
                }
                return false;
            }
            return true;
        }
    }
}
