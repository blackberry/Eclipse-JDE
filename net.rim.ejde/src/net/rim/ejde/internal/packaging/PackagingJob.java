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
package net.rim.ejde.internal.packaging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.core.IRIMMarker;
import net.rim.ejde.internal.launching.DeploymentHelper;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.signing.SignatureToolLaunchAction;
import net.rim.ejde.internal.ui.consoles.PackagingConsole;
import net.rim.ejde.internal.util.ImportUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PackagingUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.ResourceBuilderUtils;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ejde.internal.validation.DiagnosticFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;

/**
 * This class represents the job for packaging BlackBerry projects with or without deployment to the simulator. This job should be
 * run after eclipse building is done. Please use {@link PackagingJobWrapper} to run the packaging job.
 *
 */
public abstract class PackagingJob implements IWorkspaceRunnable {
    static private final Logger _log = Logger.getLogger( PackagingJob.class );
    final static public QualifiedName BUILT_BY_JAVA_BUILDER_FLAG_QUALIFIED_NAME = new QualifiedName( ContextManager.PLUGIN_ID,
            "BuiltByEclipseBuilders" ); //$NON-NLS-1$
    final static private String TRUE_STRING = "true"; //$NON-NLS-1$
    final static private String FALSE_STRING = "false"; //$NON-NLS-1$
    private Set< BlackBerryProject > _projects;
    private int _signingFlag;
    // sign flag
    // always sign
    final static public int SIGN_FORCE = 0;
    // sign only if packaging is needed
    final static public int SIGN_IF_NECESSARY = 1;
    // never sign
    final static public int SIGN_NO = 2;
    // sign only if protected APIs are used
    final static public int SIGN_IF_PROTECTED_API_USED = 3;

    /**
     * Constructs a PackagingJob instance.
     *
     * @param projects
     *            projects need to be packaged.
     */
    public PackagingJob( Set< BlackBerryProject > projects ) {
        _projects = projects;
        _signingFlag = SIGN_NO;
    }

    /**
     * Constructs a PackagingJob instance.
     *
     * @param projects
     * @param signingFlag
     */
    public PackagingJob( Set< BlackBerryProject > projects, int signingFlag ) {
        _projects = projects;
        _signingFlag = signingFlag;
    }

    /**
     * Mark the given <code>project</code> as need to be built.
     *
     * @param project
     * @param needBuild
     *            <code>true</code> mark the given <code>project</code> as need build, otherwise, mark the given
     *            <code>project</code> as not need build.
     */
    static synchronized public void setBuiltByJavaBuilders( IProject project, boolean needBuild ) {
        try {
            _log.trace( "Project " + project.getName() + " is marked as" //$NON-NLS-1$ //$NON-NLS-2$
                    + ( needBuild ? " need RAPC." : " not need RAPC." ) ); //$NON-NLS-1$ //$NON-NLS-2$
            String newBuildFlag = needBuild ? TRUE_STRING : FALSE_STRING;
            String oldBuildFlag = project.getPersistentProperty( BUILT_BY_JAVA_BUILDER_FLAG_QUALIFIED_NAME );
            if( ( oldBuildFlag == null ) || !oldBuildFlag.equals( newBuildFlag ) ) {
                project.setPersistentProperty( BUILT_BY_JAVA_BUILDER_FLAG_QUALIFIED_NAME, newBuildFlag );
            }
        } catch( CoreException e ) {
            _log.error( e );
        }
    }

    /**
     * Check if the given <code>project</code> needs to be built.
     *
     * @param project
     * @return
     */
    static synchronized private boolean builtByJavaBuilder( IProject project ) {
        try {
            String value = project.getProject().getPersistentProperty( BUILT_BY_JAVA_BUILDER_FLAG_QUALIFIED_NAME );
            if( ( value == null ) || value.equals( TRUE_STRING ) ) {
                return true;
            }
        } catch( CoreException e ) {
            _log.error( e );
            return true;
        }
        return false;
    }

    /**
     * Gets projects which need to be packaged;
     *
     * @return
     */
    public Set< BlackBerryProject > getProjects() {
        return _projects;
    }

    @Override
    public void run( IProgressMonitor monitor ) throws CoreException {
        // remove the code signing error
        ResourceBuilderUtils.cleanProblemMarkers( ResourcesPlugin.getWorkspace().getRoot(),
                new String[] { IRIMMarker.SIGNATURE_TOOL_PROBLEM_MARKER }, IResource.DEPTH_ONE );
        // open the packaging console
        PackagingConsole.getInstance().activate();
        LinkedHashSet< BlackBerryProject > projectSet = ProjectUtils.getProjectsByBuildOrder( _projects );
        monitor.beginTask( IConstants.EMPTY_STRING, projectSet.size() * 10 );
        monitor.subTask( Messages.PackagingJob_Name );
        boolean needSign = false;
        // collect projects which need to be signed
        LinkedHashSet< BlackBerryProject > projectsNeedSigning = new LinkedHashSet< BlackBerryProject >();
        // collect projects whose dependent projects need to be signed
        LinkedHashSet< BlackBerryProject > projectsDependencyNeedSigning = new LinkedHashSet< BlackBerryProject >();
        // collect projects which are packaged successfully
        LinkedHashSet< BlackBerryProject > succesfullyPackagedProjects = new LinkedHashSet< BlackBerryProject >();
        for( BlackBerryProject bbProject : projectSet ) {
            // 1. run java build on the project
            if( !isBuildAutomaticallyOn() ) {
                try {
                    bbProject.getProject().build( IncrementalProjectBuilder.AUTO_BUILD, new SubProgressMonitor( monitor, 1 ) );
                } catch( CoreException e ) {
                    _log.error( e );
                }
            }
            monitor.worked( 3 );
            // 2. package the project
            if( !needPackaging( bbProject ) ) {
                if( needGenerateALXFile( bbProject ) ) {
                    PackagingManager.generateALXForProject( bbProject );
                }
            } else {
                // remove the package problems
                ResourceBuilderUtils.cleanProblemMarkers( bbProject.getProject(), new String[] { IRIMMarker.PACKAGING_PROBLEM },
                        IResource.DEPTH_INFINITE );
                try {
                    PackagingManager.packageProject( bbProject );
                    if( !needSign ) {
                        needSign = true;
                    }
                } catch( CoreException e ) {
                    _log.error( e.getMessage() );
                    try {
                        ResourceBuilderUtils.createProblemMarker(
                                e.getStatus().getCode() == DiagnosticFactory.CREATE_FOLDER_ERR_ID ? bbProject
                                        .getMetaFileHandler() : bbProject.getProject(), IRIMMarker.PACKAGING_PROBLEM, e
                                        .getMessage(), -1, IMarker.SEVERITY_ERROR );
                    } catch( Exception e1 ) {
                        _log.error( e1.getMessage() );
                    }
                }
                PackagingJob.setBuiltByJavaBuilders( bbProject.getProject(), false );
            }
            monitor.worked( 4 );
            // 3. run post-build command
            runPostBuild( bbProject );
            monitor.worked( 1 );
            // 4. check if the project needs to be signed or not
            if( !hasPackagingProblems( bbProject.getProject() ) ) {
                succesfullyPackagedProjects.add( bbProject );
                if( PackagingUtils.isSigningNeeded( bbProject ) ) {
                    projectsNeedSigning.add( bbProject );
                } else {
                    if( PackagingUtils.isSigningNeededForDependency( bbProject ) ) {
                        projectsDependencyNeedSigning.add( bbProject );
                    } else {
                        // if a project and its dependent projects do not need to be signed, copy the cod files to the web folder
                        // copy the cod files of dependency projects to the deployment folders
                        copyDependencyDeploymentFiles( bbProject );
                        // copy files from "Standard" to "Web"
                        copyToWebDeploymentFolder( bbProject );
                    }
                }
            }
            monitor.worked( 2 );
            if( monitor.isCanceled() ) {
                monitor.done();
                return;
            }
        }
        // Code signing
        switch( _signingFlag ) {
            case SIGN_FORCE: {
                if( !succesfullyPackagedProjects.isEmpty() ) {
                    signCodFile( succesfullyPackagedProjects, monitor );
                }
                break;
            }
            case SIGN_IF_PROTECTED_API_USED: {
                if( !projectsNeedSigning.isEmpty() ) {
                    signCodFile( projectsNeedSigning, monitor );
                    for( BlackBerryProject project : projectsDependencyNeedSigning ) {
                        // copy the cod files of dependency projects to the deployment folders
                        copyDependencyDeploymentFiles( project );
                        // copy files from "Standard" to "Web"
                        copyToWebDeploymentFolder( project );
                    }
                }
                break;
            }
            case SIGN_IF_NECESSARY: {
                if( needSign ) {
                    if( !projectsNeedSigning.isEmpty() ) {
                        signCodFile( projectsNeedSigning, monitor );
                        for( BlackBerryProject project : projectsDependencyNeedSigning ) {
                            // copy the cod files of dependency projects to the deployment folders
                            copyDependencyDeploymentFiles( project );
                            // copy files from "Standard" to "Web"
                            copyToWebDeploymentFolder( project );
                        }
                    }
                }
                break;
            }
        }
        monitor.done();
        return;
    }

    abstract protected void runPostBuild( BlackBerryProject properties );

    private void signCodFile( Set< BlackBerryProject > projectSet, IProgressMonitor monitor ) throws CoreException {
        boolean successful = SignatureToolLaunchAction.signCodFiles( projectSet, monitor );
        if( successful ) {
            for( BlackBerryProject bbProject : projectSet ) {
                // copy the cod files of dependency projects to the deployment folders
                copyDependencyDeploymentFiles( bbProject );
                // copy files from "Standard" to "Web"
                copyToWebDeploymentFolder( bbProject );
            }
        }
    }

    /**
     * Copies the deployment files of the dependent projects to the corresponding deployment folders.
     *
     * @throws CoreException
     */
    private void copyDependencyDeploymentFiles( BlackBerryProject bbProject ) throws CoreException {
        IPath standardSrcFolderPath, standardDstFolderPath;
        String outputFileName;
        IPath absoluteStandardSrcFolderPath, absoluteStandardDstFolderPath;
        for( BlackBerryProject dependentProj : ProjectUtils.getAllReferencedProjects( bbProject ) ) {
            // get the standard deployment folder of the dependent project
            standardDstFolderPath = new Path( PackagingUtils.getRelativeStandardOutputFolder( dependentProj ) );
            // get the standard corresponding deployment folder of the main project
            absoluteStandardDstFolderPath = bbProject.getProject().getLocation().append( standardDstFolderPath );
            // make sure the destination folder is created
            try {
                ImportUtils.createFolders( bbProject.getProject(), standardDstFolderPath, IResource.DERIVED );
            } catch( CoreException e ) {
                throw new ResourceException( DiagnosticFactory.CREATE_FOLDER_ERR_ID, bbProject.getMetaFileHandler()
                        .getProjectRelativePath(), NLS.bind( Messages.PackagingManager_PACKAGING_CANNOT_CREATE_FOLDER_MSG,
                        standardDstFolderPath ), e );
            }
            // copy deployment files in standard folder
            standardSrcFolderPath = new Path( PackagingUtils.getRelativeStandardOutputFolder( dependentProj ) );
            absoluteStandardSrcFolderPath = dependentProj.getProject().getLocation().append( standardSrcFolderPath );
            outputFileName = dependentProj.getProperties()._packaging.getOutputFileName();
            File[] outputFiles = absoluteStandardSrcFolderPath.toFile()
                    .listFiles( new DeploymentFileNameFilter( outputFileName ) );
            _log.trace( "Dependent project " + dependentProj.getElementName() + " copy folder: " + standardSrcFolderPath
                    + " --> " + bbProject.getElementName() + " / " + standardDstFolderPath );
            for( File file : outputFiles ) {
                if( file.exists() ) {
                    DeploymentHelper.executeCopy( file, absoluteStandardDstFolderPath.append( file.getName() ).toFile() );
                }
            }
            // refresh the standard folder
            IFolder folder = bbProject.getProject().getFolder( standardDstFolderPath );
            folder.refreshLocal( IResource.DEPTH_ONE, new NullProgressMonitor() );
        }
    }

    private void copyToWebDeploymentFolder( BlackBerryProject bbProject ) throws CoreException {
        String outputRootFolder = bbProject.getProperties()._packaging.getOutputFolder();
        IPath outputRootFolderPath = new Path( outputRootFolder );
        IPath standardOutputFolderPath = outputRootFolderPath.append( PackagingUtils.getStandardDeploymentFolderName() );
        IPath webOutputFolderPath = outputRootFolderPath.append( PackagingUtils.getWebDeploymentFolderName() );
        IFolder srcFolder = bbProject.getProject().getFolder( standardOutputFolderPath );
        srcFolder.accept( new CopyResourceVistor( bbProject, standardOutputFolderPath, webOutputFolderPath ) );
        // Refresh to show new resources
        IResource outputFolder = bbProject.getProject().findMember( webOutputFolderPath );
        outputFolder.refreshLocal( IResource.DEPTH_INFINITE, new NullProgressMonitor() );
    }

    class CopyResourceVistor implements IResourceVisitor {
        BlackBerryProject _bbProject;
        IPath _srcRootPath;
        IPath _destRootPath;
        IFolder _srcRootFolder;
        IFolder _destRootFolder;
        IProject _project;

        public CopyResourceVistor( BlackBerryProject bbProject, IPath srcRootPath, IPath destRootPath ) {
            _bbProject = bbProject;
            _project = _bbProject.getProject();
            _srcRootPath = srcRootPath;
            _destRootPath = destRootPath;
            _srcRootFolder = _project.getFolder( _srcRootPath );
            _destRootFolder = _project.getFolder( _destRootPath );

        }

        @Override
        public boolean visit( IResource resource ) throws CoreException {
            if( resource instanceof IFolder ) {
                if( !resource.equals( _srcRootFolder ) ) {
                    IPath subPath = resource.getProjectRelativePath().removeFirstSegments( _srcRootPath.segmentCount() );
                    IPath destPath = _destRootPath.append( subPath );
                    folderCopy( _bbProject, resource.getProjectRelativePath(), destPath );
                } else {
                    return true;
                }
            }
            return false;
        }

    }

    /**
     * Copy files form the <code>srcFolderRelativePath</code> to the <code>destFolderRelativePath</code> in project
     * <code>bbProject</code>. If there is any cod file, un-zip it if it contains any sibling cod files and then copy the sibling
     * files to the <code>destFolder</code> .
     *
     * @param bbProject
     * @param srcFolderRelativePath
     * @param destFolderRelativePath
     * @throws CoreException
     */
    private void folderCopy( BlackBerryProject bbProject, IPath srcFolderRelativePath, IPath destFolderRelativePath )
            throws CoreException {
        try {
            ImportUtils.createFolders( bbProject.getProject(), destFolderRelativePath, IResource.DERIVED );
        } catch( CoreException e ) {
            throw new ResourceException( DiagnosticFactory.CREATE_FOLDER_ERR_ID, bbProject.getMetaFileHandler()
                    .getProjectRelativePath(), NLS.bind( Messages.PackagingManager_PACKAGING_CANNOT_CREATE_FOLDER_MSG,
                    destFolderRelativePath ), e );
        }
        IPath srcFolderPath = bbProject.getProject().getLocation().append( srcFolderRelativePath );
        IPath destFolderPath = bbProject.getProject().getLocation().append( destFolderRelativePath );
        // handle the cod files
        File[] codFiles = srcFolderPath.toFile().listFiles( new CodFileFilter() );
        if( codFiles == null ) {
            return;
        }
        _log.trace( "Project " + bbProject.getElementName() + " folder copy: " + srcFolderRelativePath + " --> "
                + destFolderRelativePath );
        for( int i = 0; i < codFiles.length; i++ ) {
            copySiblingCod( new Path( codFiles[ i ].getAbsolutePath() ), destFolderPath );
        }
        // IPath codFilePath = standardOutputFolderPath.append( bbProject.getProperties()._packaging.getOutputFileName()
        // + IConstants.COD_FILE_EXTENSION_WITH_DOT );
        // copy other files to the web deployment folder
        File[] files = srcFolderPath.toFile().listFiles( new DeploymentFileNameFilter( IConstants.EMPTY_STRING, true ) );
        IPath destinationFilePath;
        String fileName;
        for( File file : files ) {
            fileName = file.getName();
            destinationFilePath = destFolderPath.append( fileName );
            DeploymentHelper.executeCopy( file, destinationFilePath.toFile() );
        }

    }

    class FolderFilter implements FileFilter {

        @Override
        public boolean accept( File pathname ) {
            if( pathname.isDirectory() || pathname.exists() ) {
                return true;
            }
            return false;
        }

    }

    class DeploymentFileNameFilter implements FilenameFilter {
        String _outputFileName;
        boolean _excludeCodFile;

        public DeploymentFileNameFilter( String outputFileName ) {
            _outputFileName = outputFileName;
        }

        public DeploymentFileNameFilter( String outputFileName, boolean excludeCod ) {
            _outputFileName = outputFileName;
            _excludeCodFile = excludeCod;
        }

        @Override
        public boolean accept( File dir, String name ) {
            if( !StringUtils.isBlank( _outputFileName ) && !name.startsWith( _outputFileName ) ) {
                return false;
            }
            if( _excludeCodFile ) {
                if( !name.endsWith( IConstants.COD_FILE_EXTENSION_WITH_DOT ) ) {
                    return true;
                }
            } else {
                return true;
            }
            return false;
        }
    }

    class CodFileFilter implements FilenameFilter {

        @Override
        public boolean accept( File dir, String name ) {
            if( name.endsWith( IConstants.COD_FILE_EXTENSION_WITH_DOT ) ) {
                return true;
            }
            return false;
        }
    }

    /**
     * If the cod file represented by the given <code>codFilePath</code> contains sibling cod file, un-zip it and copy all sibling
     * cod files to the <code>destinationFolderPath</code>. If the cod file is a single cod file, just copy it to the
     * <code>destinationFolderPath</code>.
     *
     * @param codFilePath
     * @param destinationFolderPath
     * @throws CoreException
     */
    private void copySiblingCod( IPath codFilePath, IPath destinationFolderPath ) throws CoreException {
        boolean hasSiblingCod = false;
        File codFile = codFilePath.toFile();
        try {
            JarFile zipFile = new JarFile( codFile );
            Enumeration< JarEntry > entries = zipFile.entries();
            if( entries.hasMoreElements() ) {
                hasSiblingCod = true;
                JarEntry entry;
                for( ; entries.hasMoreElements(); ) {
                    entry = entries.nextElement();
                    if( entry.isDirectory() ) {
                        // this should not happen
                        continue;
                    }
                    InputStream is = zipFile.getInputStream( entry );
                    File outputFile = destinationFolderPath.append( entry.getName() ).toFile();
                    PackagingManager.copyInputStream( is, new BufferedOutputStream( new FileOutputStream( outputFile ) ) );
                }
            } else {
                hasSiblingCod = false;
            }
        } catch( IOException e ) {
            if( codFile.exists() ) {
                // if the cod file does not contain any sibling file, we get IOException
                hasSiblingCod = false;
            } else {
                _log.error( e );
            }
        } finally {
            if( !hasSiblingCod ) {
                // if the cod file is a single cod file, copy it to the destination
                DeploymentHelper.executeCopy( codFile, destinationFolderPath.append( codFile.getName() ).toFile() );
            }
        }
    }

    private boolean isBuildAutomaticallyOn() {
        return ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding();
    }

    private boolean needPackaging( BlackBerryProject bbproj ) throws CoreException {
        // check if the project has any critical problem
        if( ProjectUtils.hasCriticalProblems( bbproj.getProject() ) ) {
            return false;
        }
        List< BlackBerryProject > dependencyPrjoects = ProjectUtils.getAllReferencedProjects( bbproj );
        // check if the project's dependency projects have any critical problem, it does not make sense to package a project while
        // its dependency projects can not be packaged.
        if( hasProblemOnDependency( dependencyPrjoects ) ) {
            return false;
        }
        // check if the project has any packaging problem
        if( hasPackagingProblems( bbproj.getProject() ) ) {
            return true;
        }
        // check if the project has been built by the java builder
        if( PackagingJob.builtByJavaBuilder( bbproj.getProject() ) ) {
            return true;
        }
        // check if the project has been updated
        if( hasBeenUpdated( bbproj ) ) {
            return true;
        }
        // check if the project's dependency projects have been updated
        if( hasBeenUpdated( bbproj, dependencyPrjoects ) ) {
            return true;
        }
        return false;
    }

    private boolean needGenerateALXFile( BlackBerryProject project ) {
        BlackBerryProperties properties = project.getProperties();
        if( !properties._packaging.getGenerateALXFile().booleanValue() ) {
            return false;
        }
        String filename = properties._packaging.getOutputFileName() + IConstants.ALX_FILE_EXTENSION_WITH_DOT;
        IPath alxFilePath = new Path( PackagingUtils.getRelativeAlxFileOutputFolder( project ) + IPath.SEPARATOR + filename );
        IResource alxIFile = project.getProject().findMember( alxFilePath );
        return ( alxIFile == null ) || ( !alxIFile.exists() );
    }

    /**
     * Checks if any of the BlackBerry project in the given <code>bbProjects</code> has been update after the last time the
     * <code>mainProject</code> was packaged.
     *
     * @param mainProject
     * @param bbProjects
     * @return
     */
    private boolean hasBeenUpdated( BlackBerryProject mainProject, List< BlackBerryProject > bbProjects ) {
        // we only check the cod file in the standard output folder
        IPath refFilePath = mainProject.getProject().getLocation()
                .append( PackagingUtils.getRelativeStandardOutputFolder( mainProject ) );
        refFilePath = refFilePath.append( new Path( mainProject.getProperties()._packaging.getOutputFileName()
                + IConstants.COD_FILE_EXTENSION_WITH_DOT ) );
        File refFile = refFilePath.toFile();
        if( !refFile.exists() ) {
            return true;
        }
        IPath dependencySampleFilePath = null;
        File dependencyCodFile = null;
        for( BlackBerryProject bbProject : bbProjects ) {
            dependencySampleFilePath = bbProject.getProject().getLocation()
                    .append( PackagingUtils.getRelativeStandardOutputFolder( bbProject ) );
            dependencySampleFilePath = dependencySampleFilePath.append( new Path( bbProject.getProperties()._packaging
                    .getOutputFileName() + IConstants.DEBUG_FILE_EXTENSION_WITH_DOT ) );
            dependencyCodFile = dependencySampleFilePath.toFile();
            if( dependencyCodFile.lastModified() > refFile.lastModified() ) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBeenUpdated( BlackBerryProject bbproj ) throws CoreException {
        IPath refFilePath = bbproj.getProject().getLocation()
                .append( PackagingUtils.getRelativeStandardOutputFolder( bbproj ) );
        refFilePath = refFilePath.append( new Path( bbproj.getProperties()._packaging.getOutputFileName()
                + IConstants.DEBUG_FILE_EXTENSION_WITH_DOT ) );
        File refFile = refFilePath.toFile();
        if( !refFile.exists() ) {
            return true;
        }
        IFile iDescriptorFile = bbproj.getProject().getFile( BlackBerryProject.METAFILE );
        File descriptorFile = iDescriptorFile.getLocation().toFile();
        // check the time stamp of the project descriptor file
        if( !descriptorFile.exists() && bbproj.getProject().hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
            // TODO not support java project now
            throw new CoreException( StatusFactory.createErrorStatus( "Project does not have the descriptor file." ) );
        }
        if( descriptorFile.lastModified() > refFile.lastModified() ) {
            return true;
        }
        //Find the custom JAD file in the project root
        String custjad = PackagingUtils.getCustomJadFile(bbproj);
        if(custjad!=null) {
        	File jadFile = new File(custjad);
	        if( jadFile.lastModified() > refFile.lastModified() ) {
	            return true;
	        }
        }

        //Find the custom rapc file in the project root
        String custrapc = PackagingUtils.getCustomJadFile(bbproj);
        if(custrapc!=null) {
        	File rapcFile = new File(custrapc);
	        if( rapcFile.lastModified() > refFile.lastModified() ) {
	            return true;
	        }
        }

        // check if the output folders and their children have been updated
        Set< File > outputFolders = ImportUtils.getOutputFolderSet( bbproj );
        for( File folder : outputFolders ) {
            if( ProjectUtils.hasFolderBeenUpdated( folder, refFile.lastModified() ) ) {
                 return true;
            }
        }
        return false;
    }

    /**
     * Check if the projects in the given <code>project</code> have any packaging problems.
     *
     * @param project
     * @return
     */
    private boolean hasPackagingProblems( IProject project ) {
        if( project == null ) {
            return false;
        }
        try {
            IMarker[] markers = project.findMarkers( IRIMMarker.PACKAGING_PROBLEM, true, IResource.DEPTH_INFINITE );
            for( IMarker marker : markers ) {
                Integer severity = (Integer) marker.getAttribute( IMarker.SEVERITY );
                if( ( severity != null ) && ( severity.intValue() >= IMarker.SEVERITY_ERROR ) ) {
                    return true;
                }
            }
            return false;
        } catch( CoreException e ) {
            _log.error( e );
            return true;
        }
    }

    private boolean hasProblemOnDependency( List< BlackBerryProject > dependencyPrjoects ) throws CoreException {
        IProject iProject = null;
        for( BlackBerryProject jProject : dependencyPrjoects ) {
            iProject = jProject.getProject();
            if( ProjectUtils.hasCriticalProblems( iProject ) ) {
                return true;
            }
        }
        return false;
    }
}
