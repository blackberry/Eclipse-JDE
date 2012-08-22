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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.core.IRIMMarker;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.model.BlackBerryPropertiesFactory;
import net.rim.ejde.internal.ui.dialogs.PreprocessHookInstallDialog;
import net.rim.ide.OSUtils;
import net.rim.ide.core.Util;
import net.rim.sdk.resourceutil.ResourceCollection;
import net.rim.sdk.resourceutil.ResourceCollectionFactory;
import net.rim.sdk.resourceutil.ResourceConstants;
import net.rim.sdk.resourceutil.ResourceElement;
import net.rim.sdk.resourceutil.ResourceLocale;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.ide.ResourceUtil;
import org.osgi.framework.Bundle;

/**
 * Helper class for projects
 *
 * @author jkeshavarzi
 */
public class ProjectUtils {
    private static final Logger logger = Logger.getLogger( ProjectUtils.class );

    /**
     * Finds the given project in the workspace.
     *
     * @param projectName
     *            Name of the project to find.
     * @return The IProject if it was found, null otherwise.
     */
    public static IProject getProject( String projectName ) {
        IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for( IProject project : projects ) {
            if( project.getName().equalsIgnoreCase( projectName ) ) {
                return project;
            }
        }
        return null;
    }

    /**
     * Locates all the source folders represented by type IPackageFragmentRoot.K_SOURCE for the given project.
     *
     * @param project
     *            The IProject to search for source folders.
     * @return An IPackageFragmentRoot array containing each K_SOURCE fragment found for the given project.
     */
    public static IPackageFragmentRoot[] getProjectSourceFolders( IProject project ) {
        IJavaProject iJavaProject = JavaCore.create( project );
        ArrayList< IPackageFragmentRoot > sourceRoots = new ArrayList< IPackageFragmentRoot >();
        if( iJavaProject.exists() && iJavaProject.isOpen() ) {
            try {
                IPackageFragmentRoot[] roots = iJavaProject.getAllPackageFragmentRoots();
                for( IPackageFragmentRoot root : roots ) {
                    if( IPackageFragmentRoot.K_SOURCE == root.getKind() ) {
                        sourceRoots.add( root );
                    }
                }
            } catch( JavaModelException e ) {
                logger.error( "findProjectSources: Could not retrieve project sources:", e ); //$NON-NLS-1$
                return new IPackageFragmentRoot[ 0 ];
            }
        }
        return sourceRoots.toArray( new IPackageFragmentRoot[ sourceRoots.size() ] );
    }

    /**
     * Checks the given project for a file with the given name
     *
     * @param project
     *            - The IProject to search for the file
     * @param name
     *            - The name of the file to search for
     * @param isPrefix
     *            - Indicates whether the passed in file name should be treated as a prefix when searching
     * @return The found IFile object, or null if nothing was found
     */
    public static IFile getProjectIFile( IProject project, String name, Boolean isPrefix ) {
        IFile file = null;

        // source folders
        IPackageFragmentRoot roots[] = getProjectSourceFolders( project );

        for( IPackageFragmentRoot root : roots ) {
            try {
                IJavaElement elements[] = root.getChildren();
                for( IJavaElement element : elements ) {
                    if( element.getElementType() == IJavaElement.PACKAGE_FRAGMENT ) {
                        IPackageFragment packageFragment = (IPackageFragment) element;
                        Object packageChildren[] = packageFragment.getNonJavaResources();
                        for( Object child : packageChildren ) {
                            if( child instanceof IFile ) {
                                IFile childFile = (IFile) child;
                                if( isPrefix.booleanValue() ) {
                                    if( childFile.getName().startsWith( name ) ) {
                                        return childFile;
                                    }
                                } else {
                                    if( childFile.getName().equals( name ) ) {
                                        return childFile;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch( JavaModelException e ) {
                logger.error( "getProjectIFile: error" );
            }
        }

        return file;
    }

    /**
     * Get all files found in project
     *
     * @param project
     *            - The IProject to retrieve files
     * @return A IFile array containing all found files within the passed in project
     */
    public static IFile[] getProjectFiles( IProject project ) {
        ArrayList< IFile > files = new ArrayList< IFile >();

        // source folders
        IPackageFragmentRoot roots[] = getProjectSourceFolders( project );

        for( IPackageFragmentRoot root : roots ) {
            try {
                IJavaElement sourceElements[] = root.getChildren();
                for( IJavaElement sourceElement : sourceElements ) {
                    if( sourceElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT ) {
                        IPackageFragment packageFragment = (IPackageFragment) sourceElement;
                        IJavaElement packageElements[] = packageFragment.getChildren();
                        for( IJavaElement packageElement : packageElements ) {
                            if( packageElement instanceof IFile ) {
                                files.add( (IFile) packageElement );
                            }
                        }
                    }
                }
            } catch( JavaModelException e ) {
                logger.error( "getProjectFiles: error" );
            }
        }

        return files.toArray( new IFile[ files.size() ] );
    }

    /**
     * Returns the set of all referenced IProjects for a given IProject
     *
     * @param project
     *            the given IProject
     * @return the array of referenced IProjects, calculated recursively
     * @throws CoreException
     *             if an error occurs while computing referenced projects
     */
    public static Set< IProject > getAllReferencedProjects( IProject project ) throws CoreException {
        Set< IProject > referencedProjects = new HashSet< IProject >();
        addReferencedProjects( project, referencedProjects );
        return referencedProjects;
    }

    private static void addReferencedProjects( IProject project, Set< IProject > references ) throws CoreException {
        if( project.isOpen() ) {
            IJavaProject javaProject = JavaCore.create( project );
            String[] requiredProjectNames = javaProject.getRequiredProjectNames();
            IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
            IProject requiredProject = null;
            for( String projectName : requiredProjectNames ) {
                requiredProject = workspaceRoot.getProject( projectName );
                if( requiredProject.exists() && requiredProject.isOpen() && !references.contains( requiredProject ) ) {
                    references.add( requiredProject );
                    addReferencedProjects( requiredProject, references );
                }
            }
        }
    }

    /**
     * Returns the array of all referenced IJavaProjects for a given IJavaProject
     *
     * @param project
     *            the given IJavaProject
     * @return the array of referenced IJavaProjects, calculated recursively
     * @throws CoreException
     *             if an error occurs while computing referenced projects
     */
    public static List< IJavaProject > getAllReferencedJavaProjects( IJavaProject project ) throws CoreException {
        Set< IProject > referencedProjects = new HashSet< IProject >();
        List< IJavaProject > refJavaProjects = new ArrayList< IJavaProject >();
        addReferencedProjects( project.getProject(), referencedProjects );

        for( IProject proj : referencedProjects ) {
            refJavaProjects.add( JavaCore.create( proj ) );
        }
        return refJavaProjects;
    }

    /**
     * Returns the array of all referenced projects for a given <code>bbProject</code>.
     * <p>
     * <b>If the <code>bbProject</code> depends on some java projects, we created BB projects on-the-fly for those java
     * projects.</b>
     *
     * @param bbProject
     *            the given BlackBerryProject
     * @return the array of referenced BlackBerryProjects, calculated recursively
     * @throws CoreException
     *             if an error occurs while computing referenced projects
     */
    public static List< BlackBerryProject > getAllReferencedProjects( BlackBerryProject bbProject ) throws CoreException {
        Set< IProject > referencedProjects = new HashSet< IProject >();
        List< BlackBerryProject > refJavaProjects = new ArrayList< BlackBerryProject >();
        addReferencedProjects( bbProject.getProject(), referencedProjects );

        for( IProject proj : referencedProjects ) {
            BlackBerryProperties properties = null;
            final IJavaProject javaProject = JavaCore.create( proj );
            if( proj.hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                properties = ContextManager.PLUGIN.getBBProperties( proj.getName(), false );
                if( properties == null ) {
                    continue;
                }
            } else {
                // create a BB properties for a java project on-the-fly
                properties = BlackBerryPropertiesFactory.createBlackBerryProperties( javaProject );
                ;
            }
            refJavaProjects.add( new BlackBerryProject( javaProject, properties ) );
        }
        return refJavaProjects;
    }

    /**
     * Determines whether a given IJavaProject represents a parent our of a list of selected projects
     *
     * @param project
     *            the given IJavaProject
     * @param selectedProjects
     *            the selection of IJavaProjects
     * @return
     */
    public static boolean isParentProject( IJavaProject project, List< IJavaProject > selectedProjects ) {
        IProject eclipseProj = project.getProject();
        IProject[] projects = eclipseProj.getReferencingProjects();
        for( IProject refProject : projects ) {
            if( selectedProjects.contains( JavaCore.create( refProject ) ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the set of all referenced IJavaProjects for a list of objects supposed to be IJavaProject
     *
     * @param javaProjects
     *            the list of objects supposed to be of type IJavaProject
     * @return the set of referenced IJavaProjects, included the IJavaProjects themselves, calculated recursively
     * @throws CoreException
     *             if an error occurs while computing referenced projects
     */
    public static Set< IJavaProject > getAllJavaProjects( List< Object > javaProjects ) throws CoreException {
        Set< IJavaProject > allJavaProjects = new HashSet< IJavaProject >();
        Set< IProject > allProjects = new HashSet< IProject >();
        for( Object obj : javaProjects ) {
            if( obj instanceof IJavaProject ) {
                IProject currentProject = ( (IJavaProject) obj ).getProject();
                allProjects.add( currentProject );
                addReferencedProjects( currentProject, allProjects );
                for( IProject p : allProjects ) {
                    allJavaProjects.add( JavaCore.create( p ) );
                }
            }
        }
        return allJavaProjects;
    }

    /**
     * Gets a file that exists in an Eclipse project.
     * <p>
     * TODO: Someone can probably optimize this method better. Like using some of the IWorkspaceRoot.find*() methods...
     *
     * @param project
     *            the Eclipse project the file belongs to
     * @param file
     *            the File which is in the Eclipse project
     * @return the Eclipse resource file associated with the file
     */
    public static IResource getResource( IProject project, File file ) {
        IJavaProject javaProject = JavaCore.create( project );
        IPath filePath = new Path( file.getAbsolutePath() );
        try {
            IClasspathEntry[] classpathEntries = javaProject.getResolvedClasspath( true );

            IFile input = null;
            // Look for a source folder
            for( IClasspathEntry classpathEntry : classpathEntries ) {
                if( classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE ) {

                    // Try to resolve the source container
                    IWorkspaceRoot workspaceRoot = project.getWorkspace().getRoot();
                    IResource resource = workspaceRoot.findMember( classpathEntry.getPath() );
                    if( resource instanceof IContainer ) {
                        IContainer sourceContainer = (IContainer) resource;
                        File sourceContainerFile = resource.getLocation().toFile();
                        IPath sourceFolderPath = new Path( sourceContainerFile.getAbsolutePath() );

                        // See if the file path is within this source folder
                        // path
                        if( sourceFolderPath.isPrefixOf( filePath ) ) {
                            int segmentCount = sourceFolderPath.segmentCount();
                            IPath relativePath = filePath.removeFirstSegments( segmentCount );
                            input = sourceContainer.getFile( relativePath );
                            break;
                        }
                    }
                }
            }
            return input;
        } catch( JavaModelException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets selected IJavaProjects from the selection.
     *
     * @param selection
     *            The <code>StructuredSelection</code>
     * @return <code>IJavaProject</code>
     */
    public static IJavaProject[] getSelectProjects( StructuredSelection selection ) {
        StructuredSelection ss = selection;
        List< IJavaProject > projects = new ArrayList< IJavaProject >();
        Object p = null;
        Iterator< Object > i = ss.iterator();
        while( i.hasNext() ) {
            p = i.next();
            if( p instanceof IJavaProject ) {
                try {
                    // we only package BB projects
                    if( ( (IJavaProject) p ).getProject().hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                        projects.add( (IJavaProject) p );
                    }
                } catch( CoreException e ) {
                    logger.error( e.getMessage() );
                }
            } else if( p instanceof IProject ) {
                try {
                    // we only package BB projects
                    if( ( (IProject) p ).hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                        projects.add( JavaCore.create( (IProject) p ) );
                    }
                } catch( CoreException e ) {
                    logger.error( e.getMessage() );
                }
            }
        }
        return projects.toArray( new IJavaProject[ projects.size() ] );
    }

    /**
     * Gets the IPath of the .project file of the given <code>project</code>.
     *
     * @param project
     * @return
     */
    public static IPath getProjectDescriptionFilePath( IProject project ) {
        IFile iFile = project.getFile( ".project" );
        return iFile.getLocation();
    }

    public static LinkedHashSet< BlackBerryProject > getProjectsByBuildOrder( Set< BlackBerryProject > selectedProjects )
            throws CoreException {
        Set< BlackBerryProject > allProjects = new HashSet< BlackBerryProject >();

        for( BlackBerryProject project : selectedProjects ) {
            if( project != null ) {
                allProjects.addAll( ProjectUtils.getAllReferencedProjects( project ) );
                allProjects.add( project );
            }
        }
        LinkedHashSet< BlackBerryProject > sortedProjects = new LinkedHashSet< BlackBerryProject >( allProjects.size() );
        Map< String, Boolean > visitHistory = new HashMap< String, Boolean >( allProjects.size() );

        for( BlackBerryProject project : allProjects ) {
            ProjectUtils.visitNode( project, visitHistory, sortedProjects );
        }
        return sortedProjects;

    }

    private static void visitNode( BlackBerryProject rootNode, Map< String, Boolean > visitHistory,
            Set< BlackBerryProject > sortedProjects ) throws CoreException {
        Boolean visited = visitHistory.get( rootNode.getElementName() );
        if( visited == null ) {
            visitHistory.put( rootNode.getElementName(), Boolean.TRUE );
            for( BlackBerryProject childNode : ProjectUtils.getAllReferencedProjects( rootNode ) ) {
                ProjectUtils.visitNode( childNode, visitHistory, sortedProjects );
            }
            sortedProjects.add( rootNode );
        }
    }

    /**
     * Gets non-BlackBerry projects in the given <code>javaProjects</code>.
     *
     * @param javaProjects
     * @return
     */
    public static List< IJavaProject > getNonBBJavaProjects( List< IJavaProject > javaProjects ) {
        List< IJavaProject > nonBBProjects = new ArrayList< IJavaProject >();
        for( IJavaProject javaProject : javaProjects ) {
            try {
                if( !javaProject.getProject().hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                    nonBBProjects.add( javaProject );
                }
            } catch( CoreException e ) {
                logger.error( e.getMessage() );
                continue;
            }
        }
        return nonBBProjects;
    }

    /**
     * Gets non-BlackBerry java projects which have JDE compatibility problem in the given <code>javaProjects</code>.
     *
     * @param javaProjects
     *            non-BlackBerry java projects
     * @return
     */
    public static List< IJavaProject > getJavaProjectsContainJDKCompatibilityProblem( List< IJavaProject > javaProjects ) {
        List< IJavaProject > projectWithProblem = new ArrayList< IJavaProject >();
        for( IJavaProject javaProject : javaProjects ) {
            if( hasJDKCompatibilityProblem( javaProject ) ) {
                projectWithProblem.add( javaProject );
            }
        }
        return projectWithProblem;
    }

    /**
     * Checks if the given <code>javaProject</code> has compatibility problem.
     *
     * @param javaProject
     * @return
     */
    public static boolean hasJDKCompatibilityProblem( IJavaProject javaProject ) {
        final Map map = javaProject.getOptions( true );
        if( map.size() > 0 ) {
            String value = (String) map.get( JavaCore.COMPILER_COMPLIANCE );
            if( !value.equalsIgnoreCase( JavaCore.VERSION_1_4 ) ) {
                return true;
            }
            value = (String) map.get( JavaCore.COMPILER_SOURCE );
            if( !value.equalsIgnoreCase( JavaCore.VERSION_1_3 ) ) {
                return true;
            }
            value = (String) map.get( JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM );
            if( !value.equalsIgnoreCase( JavaCore.VERSION_1_2 ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all BlackBerry project in workspace.
     *
     * @return All BlackBerry projects
     * @throws CoreException
     */
    public static Set< IProject > getAllBBProjectsAndDependencies() {
        IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        List< IProject > bbProjects = new ArrayList< IProject >();
        for( IProject project : allProjects ) {
            if( NatureUtils.hasBBNature( project ) ) {
                bbProjects.add( project );
            }
        }
        Set< IProject > bbAndDependentProjects = new HashSet< IProject >();
        bbAndDependentProjects.addAll( bbProjects );
        try {
            bbAndDependentProjects.addAll( ProjectUtils.getAllReferencedProjects( bbProjects ) );
        } catch( CoreException e ) {
            logger.error( "", e );
        }
        return bbAndDependentProjects;
    }

    /**
     * Checks if the given <code>project</code> is dependent by any checked project in the <code>checkedProjects</code> but is not
     * in the <code>dependentProjects</code>.
     *
     * @param project
     * @param checkedProjects
     * @param dependentProjects
     * @return Project the first project which depends on the given <code>project</code>.
     */
    public static IProject isDependedByOthers( IProject project, List< IProject > checkedProjects,
            Set< IProject > dependentProjects ) {
        for( Object obj : checkedProjects ) {
            IProject checkedProject = (IProject) obj;
            if( !dependentProjects.contains( checkedProject ) ) {
                Set< IProject > depProjects;
                try {
                    depProjects = ProjectUtils.getAllReferencedProjects( checkedProject );
                } catch( CoreException e ) {
                    logger.error( "", e );
                    return null;
                }
                for( IProject depProject : depProjects ) {
                    if( project.equals( depProject ) ) {
                        return checkedProject;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the VM assigned to build the given project.
     *
     * @param project
     *            The <code>IJavaProject</code>
     * @return <code>IVMInstall</code>
     */
    public static IVMInstall getVMForProject( IJavaProject project ) {
        IVMInstall vm = null;

        try {
            vm = JavaRuntime.getVMInstall( project );
        } catch( CoreException e ) {
            logger.debug( "", e );
        }

        return vm;
    }

    /**
     * Returns list of BlackBerryProject for the given iprojects.
     *
     * @param iprojects
     * @return List of <code>BlackBerryProject</code>
     */
    public static Set< BlackBerryProject > getBlackBerryProjects( Set< IProject > iprojects ) {
        Set< BlackBerryProject > bbProjects = new HashSet< BlackBerryProject >();
        for( IProject iproject : iprojects ) {
            BlackBerryProperties properties = null;
            final IJavaProject javaProject = JavaCore.create( iproject );
            if( NatureUtils.hasBBNature( iproject ) ) {
                properties = ContextManager.PLUGIN.getBBProperties( iproject.getName(), false );
                if( properties == null ) {
                    continue;
                }
            } else {
                // create a BB properties for a java project on-the-fly
                properties = BlackBerryPropertiesFactory.createBlackBerryProperties( javaProject );
                ;
            }
            bbProjects.add( new BlackBerryProject( javaProject, properties ) );
        }
        return bbProjects;
    }

    /**
     * Checks if the given <code>project</code> is depended by any BlackBerry project.
     *
     * @param project
     * @return
     */
    public static boolean isDependedByBBProject( IProject project ) {
        IProject[] referedProjects = project.getReferencingProjects();
        for( IProject referedProject : referedProjects ) {
            try {
                if( referedProject.hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                    return true;
                }
                if( isDependedByBBProject( referedProject ) ) {
                    return true;
                }

            } catch( CoreException e ) {
                logger.error( e.getMessage() );
            }
        }
        return false;
    }

    /**
     * Returns the set of all referenced IProjects for the given projects.
     *
     * @param projects
     *            the given projects
     * @return the set of referenced IProjects, calculated recursively
     * @throws CoreException
     *             if an error occurs while computing referenced projects
     */
    public static Set< IProject > getAllReferencedProjects( List< IProject > projects ) throws CoreException {
        Set< IProject > referencedProjects = new HashSet< IProject >();
        for( IProject project : projects ) {
            referencedProjects.addAll( getAllReferencedProjects( project ) );
        }
        return referencedProjects;
    }

    /**
     * Extracts the BB projects from the given <code>workingSets</code>.
     *
     * @param workingSets
     *            The selected workingsets.
     * @return BB projects in the selected workingsets.
     */
    public static HashSet< BlackBerryProject > extractBBProjects( IWorkingSet[] workingSets ) {
        HashSet< BlackBerryProject > projects = new HashSet< BlackBerryProject >();
        if( workingSets != null ) {
            BlackBerryProject bbProject = null;
            for( IWorkingSet workingSet : workingSets ) {
                Object[] selection = workingSet.getElements();
                for( Object element : selection ) {
                    IResource resource = ResourceUtil.getResource( element );
                    if( resource != null ) {
                        bbProject = createBBProject( resource.getProject() );
                        if( bbProject != null ) {
                            projects.add( bbProject );
                        }
                    } else {
                        ResourceMapping mapping = ResourceUtil.getResourceMapping( element );
                        if( mapping != null ) {
                            IProject[] theProjects = mapping.getProjects();
                            for( IProject theProject : theProjects ) {
                                bbProject = createBBProject( theProject );
                                if( bbProject != null ) {
                                    projects.add( bbProject );
                                }
                            }
                        }
                    }
                }
            }
        }
        return projects;
    }

    /**
     * Creates a BlackBerryProject instance for the given <code>iProject</code>.
     *
     * @param iProject
     * @return The BlackBerryProject instance for the given <code>iProject</code> or <code>null</code> if the
     *         <code>iProject</code> does not have BB nature or any exception has occurred.
     */
    static public BlackBerryProject createBBProject( IProject iProject ) {
        try {
            // we only package BB projects
            if( !iProject.hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                return null;
            }
        } catch( CoreException e ) {
            logger.error( e.getMessage() );
            return null;
        }
        BlackBerryProperties properties = null;
        properties = ContextManager.PLUGIN.getBBProperties( iProject.getProject().getName(), false );
        if( properties == null ) {
            return null;
        }
        return new BlackBerryProject( JavaCore.create( iProject ), properties );
    }

    static public List< String > getContents( File file ) {
        List< String > contents = new ArrayList< String >();

        BufferedReader input = null;
        try {
            input = new BufferedReader( new FileReader( file ) );
            String line = null; // not declared within while loop
            while( ( line = input.readLine() ) != null ) {
                contents.add( line );
            }
        } catch( IOException e ) {
            logger.error( e );
        } finally {
            if( input != null ) {
                try {
                    input.close();
                } catch( IOException e ) {
                    logger.error( e );
                }
            }
        }

        return contents;
    }

    static public void commitContents( File file, List< String > contents ) {
        BufferedWriter output = null;
        try {
            output = new BufferedWriter( new FileWriter( file ) );
            for( int i = 0; i < contents.size(); i++ ) {
                output.write( contents.get( i ) );
                output.newLine();
            }
            output.flush();
        } catch( IOException e ) {
            logger.error( e );
        } finally {
            if( output != null ) {
                try {
                    output.close();
                } catch( IOException e ) {
                    logger.error( e );
                }
            }
        }
    }

    static private IStatus updateEclipseConfig() {
        IPath configFilePath = new Path( Platform.getInstallLocation().getURL().getPath() );

        if( OSUtils.isMac() ) {
            configFilePath = configFilePath.append( File.separator + "Eclipse.app" + File.separator + "Contents" + File.separator
                    + "MacOS" + File.separator );
        }

        configFilePath = configFilePath.append( "eclipse.ini" );
        File configFile = configFilePath.toFile();
        if( !configFile.exists() ) {
            logger.error( NLS.bind( Messages.PreprocessHookEclipseIniNotFoundErr, configFile.getPath() ) );
            return StatusFactory
                    .createErrorStatus( NLS.bind( Messages.PreprocessHookEclipseIniNotFoundErr, configFile.getPath() ) );
        }
        try {
            List< String > contents = getContents( configFile );
            String osgiConfigString = "-Dosgi.framework.extensions=";
            String osgiString = IConstants.EMPTY_STRING;
            int osgiConfigLineIndex = -1;
            // "-Dosgi.framework.extensions=" is a JVM argument, we have to add it after "-vmargs" line
            int vmargIndex = -1;
            boolean bundleExisting = false;
            String line;
            for( int i = 0; i < contents.size(); i++ ) {
                line = contents.get( i );
                if( line.trim().equals( "-vmargs" ) ) {
                    vmargIndex = i;
                } else if( line.trim().startsWith( osgiConfigString ) ) {
                    if( line.indexOf( IConstants.PREPROCESSING_HOOK_FRGMENT_ID ) > 0 ) {
                        bundleExisting = true;
                    }
                    osgiConfigLineIndex = i;
                    osgiString = line;
                }
            }
            if( osgiConfigLineIndex < 0 ) {
                // "-Dosgi.framework.extensions=" is not there
                osgiString = osgiConfigString + IConstants.PREPROCESSING_HOOK_FRGMENT_ID;
                contents.add( vmargIndex + 1, osgiString );
            } else {
                if( bundleExisting ) {
                    // "-Dosgi.framework.extensions=" is there, we should recomment users to re-install ejde
                    return StatusFactory.createErrorStatus( Messages.PreprocessHookCanNotBeConfiguredErr );
                } else {
                    contents.set( osgiConfigLineIndex, osgiString + "," + IConstants.PREPROCESSING_HOOK_FRGMENT_ID );
                }
            }
            commitContents( configFile, contents );
            return Status.OK_STATUS;
        } catch( Exception ex ) {
            logger.error( ex.getMessage() );
            return StatusFactory.createErrorStatus( ex.getMessage() );
        }
    }

    /**
     * Install the preprocess hook. We do not need to actually add anything to the configuration.ini. We just need to restart the
     * eclipse.
     */
    static public void setPreprocessorHook() {
        if( PreprocessHookInstallDialog.isDialogOn() ) {
            return;
        }
        PreprocessHookInstallDialog.setIsDialogOn( true );
        // need to clean the workspace after restart
        ContextManager.getDefault().getPreferenceStore().setValue( IConstants.NEED_CLEAN_WORKSPACE_KEY, true );
        Display.getDefault().asyncExec( new Runnable() {
            public void run() {
                int result = PreprocessHookInstallDialog.openQuestion( Messages.PreprocessHookInstallDialogTitle,
                        Messages.PreprocessHookInstallDialog_Text );
                if( result == IDialogConstants.OK_ID ) {
                    IStatus status = updateEclipseConfig();
                    if( status.isOK() ) {
                        PlatformUI.getWorkbench().restart();
                    } else {
                        MessageDialog.openError( ContextManager.getActiveWorkbenchShell(), Messages.ErrorHandler_DIALOG_TITLE,
                                status.getMessage() );
                    }
                }
            }
        } );
    }

    /**
     * Open the project startup page if it's not already opened
     *
     */
    public static void openStartupPage() {
        Bundle bundle = Platform.getBundle( ContextManager.PLUGIN_ID); //IConstants.DOC_PLUGIN_ID );
        IPath pagePath = new Path( IConstants.START_UP_PAGE );
        IPath folderPath = new Path( IConstants.START_UP_FOLDER );
        IPath htmlFolderPath = new Path( IConstants.HTML_PAGE_FOLDER );  // may be empty
        URL pageUrl = FileLocator.find( bundle, pagePath, null );
        URL folderUrl = FileLocator.find( bundle, folderPath, null );
        URL htmlFolderUrl = FileLocator.find( bundle, htmlFolderPath, null );
        IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();

        try {
            // extract necessary files from jar into a cache, otherwise the images will not be
            // displayed proplerly
            FileLocator.toFileURL( folderUrl );
            // Since startup page links to html page, we need to extract html content as well
            FileLocator.toFileURL( htmlFolderUrl );

            pageUrl = FileLocator.toFileURL( pageUrl );
            IWebBrowser browser = support.createBrowser( IWorkbenchBrowserSupport.AS_EDITOR
                    | IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.LOCATION_BAR
                    | IWorkbenchBrowserSupport.STATUS, IConstants.BROWSER_ID, null, null );

            browser.openURL( pageUrl );
        } catch( PartInitException e ) {
            logger.error( "Could not open the start up page in the editor part", e ); //$NON-NLS-1$
        } catch( IOException e ) {
            logger.error( "Unable to convert URL", e ); //$NON-NLS-1$
        }
    }

    /**
     * Checks if the active workspace contains any projects
     *
     * @return
     */
    public static boolean containsProjects() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        return projects.length > 0;
    }

    /**
     * Checks if all projects are closed in the active workspace
     *
     * @return
     */
    public static boolean isAllBBProjectsClosed() {
        List< IProject > projects = Arrays.asList( ResourcesPlugin.getWorkspace().getRoot().getProjects() );

        if( !projects.isEmpty() ) {
            for( IProject project : projects ) {
                if( project.isOpen() && NatureUtils.hasBBNature( project ) ) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Use a simple breadth-first algorithm to check if the given <code>folder</code> and its children folders have bee updated
     * after the <code>lastModifiedTimeStamp</code>.
     *
     * @param folder
     * @param lastModifiedTimeStamp
     * @return
     */
    public static boolean hasFolderBeenUpdated( File folder, long lastModifiedTimeStamp ) {
        File[] folders;
        SimpleQueue queue = new SimpleQueue();
        queue.offer( folder );
        File elementFolder;
        while( ( elementFolder = (File) queue.poll() ) != null ) {
            if( elementFolder.lastModified() > lastModifiedTimeStamp ) {
                return true;
            }
            folders = elementFolder.listFiles( new FileFilterImpl() );
            for( int i = 0; i < folders.length; i++ ) {
                queue.offer( folders[ i ] );
            }
        }
        return false;
    }

    static private class FileFilterImpl implements FileFilter {

        @Override
        public boolean accept( File pathname ) {
            if( pathname.isDirectory() ) {
                return true;
            }
            return false;
        }

    }

    /**
     * This is a simple implementation of Queue.
     *
     *
     */
    static public class SimpleQueue {
        Vector< Object > _vector;

        public SimpleQueue() {
            _vector = new Vector< Object >();
        }

        /**
         * Gets the first element and remove it from the queue.
         *
         * @return
         */
        public synchronized Object poll() {
            if( _vector.isEmpty() ) {
                return null;
            }
            return _vector.remove( 0 );
        }

        /**
         * Gets the first element but not remove it from the queue.
         *
         * @return
         */
        public synchronized Object peak() {
            if( _vector.isEmpty() ) {
                return null;
            }
            return _vector.get( 0 );
        }

        /**
         * Adds an element to the bottom of the queue.
         *
         * @param obj
         */
        public synchronized void offer( Object obj ) {
            _vector.addElement( obj );
        }
    }

    /**
     * Check if the projects in the given <code>project</code> have any critical problems.
     *
     * @param project
     * @return
     */
    public static boolean hasCriticalProblems( IProject project ) {
        if( project == null ) {
            return false;
        }
        try {
            IMarker[] markers = project.findMarkers( IMarker.PROBLEM, true, IResource.DEPTH_INFINITE );
            if( markers.length > 0 ) {
                for( IMarker marker : markers ) {
                    if( isCriticalProblem( marker ) ) {
                        return true;
                    }
                }
            }
            return false;
        } catch( CoreException e ) {
            logger.error( e );
            return true;
        }
    }

    /**
     * Check if the projects in the given <code>resource</code> have any error of the given <code>types</code>.
     *
     * @param resource
     * @return
     */
    public static boolean hasError( IResource resource, String[] types ) {
        if( resource == null ) {
            return false;
        }
        try {
            IMarker[] markers = resource.findMarkers( IMarker.PROBLEM, true, IResource.DEPTH_INFINITE );
            if( markers.length > 0 ) {
                for( IMarker marker : markers ) {
                    Integer severity = (Integer) marker.getAttribute( IMarker.SEVERITY );
                    if( severity != null ) {
                        if( severity.intValue() >= IMarker.SEVERITY_ERROR && typeMatch( marker, types ) ) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } catch( CoreException e ) {
            logger.error( e );
            return true;
        }
    }

    public static boolean typeMatch( IMarker marker, String[] types ) {
        for( int i = 0; i < types.length; i++ ) {
            try {
                if( marker.isSubtypeOf( types[ i ] ) ) {
                    return true;
                }
            } catch( CoreException e ) {
                logger.error( e.getMessage() );
            }
        }
        return false;
    }

    /**
     * Check if the marker is a critical problem.
     *
     * @param marker
     * @return
     * @throws CoreException
     */
    public static boolean isCriticalProblem( IMarker marker ) throws CoreException {
        Integer severity = (Integer) marker.getAttribute( IMarker.SEVERITY );
        if( severity != null ) {
            // TODO need to improve this rule
            return ( severity.intValue() >= IMarker.SEVERITY_ERROR )
                    && ( marker.getType().equals( IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER ) || ( marker
                            .isSubtypeOf( IRIMMarker.BLACKBERRY_PROBLEM ) && !marker.getType().equals(
                            IRIMMarker.PACKAGING_PROBLEM ) ) );
        }
        return false;
    }

    /**
     * Get the map of all found resource collections in the given project and the projects it depends on.
     */
    static public Map< String, RRHFile > getProjectResources( BlackBerryProject bbProject ) {
        logger.trace( "****** Get resource for " + bbProject.getProject().getName() );
        Map< String, RRHFile > rrhFileMap = new HashMap< String, RRHFile >();
        List< IJavaProject > projects = null;
        try {
            projects = getAllReferencedJavaProjects( bbProject );
            projects.add( 0, bbProject.getJavaProject() );
            for( IJavaProject javaProject : projects ) {
                getResourceFilesRecursively( javaProject, rrhFileMap );
            }
        } catch( CoreException e ) {
            logger.error( e );
        }
        return rrhFileMap;
    }

    static protected void getResourceFilesRecursively( IJavaProject javaProject, Map< String, RRHFile > rrhFileMap )
            throws CoreException {
        // search for rrh files in the project
        FileVisitor visitor = new FileVisitor( IConstants.RRH_FILE_EXTENSION, false );
        javaProject.getProject().accept( visitor );
        List< IFile > rrhFiles = visitor.getFiles();
        String fileNameWithPackage, fileName;
        Hashtable< String, String > constantsTable;
        for( IFile file : rrhFiles ) {
            fileNameWithPackage = PackageUtils.getRRHPackageID( file.getLocation().toFile() );
            fileName = file.getName();
            fileNameWithPackage += "." + fileName.substring( 0, fileName.indexOf( ResourceConstants.RRH_SUFFIX ) );
            if( rrhFileMap.get( fileNameWithPackage ) != null ) {
                logger.debug( "Found duplicated rrh file: " + fileNameWithPackage );
            } else {
                constantsTable = getKeysFromRRHFile( file.getLocation().toOSString() );
                if( constantsTable != null ) {
                    rrhFileMap.put( fileNameWithPackage, new RRHFile( fileNameWithPackage, file, constantsTable ) );
                }
            }
        }
        // search for rrh interfaces in the jar files imported by the project
        InternalProjectUtils.getResourcesFromJars( javaProject, rrhFileMap );
    }

    /**
     * Get key&value pairs from the rrh file </code>rrhFilePath</code>.
     *
     * @param rrhFilePath
     * @return
     * @throws CoreException
     */
    public static Hashtable< String, String > getKeysFromRRHFile( String rrhFilePath ) throws CoreException {
        if( rrhFilePath == null || !new File( rrhFilePath ).exists() ) {
            return null;
        }
        Hashtable< String, String > headerKey2Id = new Hashtable< String, String >();
        Util.parseRRHFile( rrhFilePath, headerKey2Id );
        ResourceCollection rc = null;
        try {
            rc = ResourceCollectionFactory.newResourceCollection( rrhFilePath );
            ResourceLocale rootLocale = rc.getLocale( "" ); //$NON-NLS-1$
            if( rootLocale != null ) {
                ResourceElement[] elements = rootLocale.getResourceElements();
                for( ResourceElement element : elements ) {
                    // Only allow single value keys in the title and description fields
                    if( element.isMulti() ) {
                        headerKey2Id.remove( element.getKey() );
                    }
                }
            }
            return headerKey2Id;
        } catch( Exception e ) {
            logger.error( e.getMessage() ); //$NON-NLS-1$
            return new Hashtable< String, String >();
        }
    }

    /**
     * Obtain the allowed startup tiers as an int array.
     *
     * @return startup tiers as an int array.
     */
    public static int[] getStartupTiers() {
        return StartupTiers.getAllowedTiers();
    }

    /**
     * Obtain the allowed startup tiers as a String array.
     *
     * @return startup tiers as a String array
     */
    public static String[] getStartupTierStrings() {
        int[] allowedTiers = StartupTiers.getAllowedTiers();
        String[] tiers = new String[ allowedTiers.length ];
        for( int i = 0; i < allowedTiers.length; i++ ) {
            tiers[ i ] = String.valueOf( allowedTiers[ i ] );
        }
        return tiers;
    }

    public static List< IFile > getProtectedFiles( IProject project ) throws CoreException {
        FileVisitor visitor = new FileVisitor( IConstants.JAVA_EXTENSION, false );
        project.accept( visitor );
        return visitor.getFiles();
    }

    public static List< IFile > getKeyFiles( IProject project ) throws CoreException {
        KeyFileVisitor visitor = new KeyFileVisitor();
        project.accept( visitor );
        return visitor.getFiles();
    }

    protected static class FileVisitor implements IResourceVisitor {
        IJavaProject _javaProject;
        String _fileExtension;
        boolean _shouldOnClassPath;
        List< IFile > _files;

        public FileVisitor( String fileExtension, boolean shouldOnClassPath ) {
            _fileExtension = fileExtension;
            _shouldOnClassPath = shouldOnClassPath;
            _files = new ArrayList< IFile >();
        }

        public boolean visit( IResource resource ) throws CoreException {
            if( !( resource instanceof IFile ) ) {
                return shouldBeSourceRoot( resource );
            }
            String extension = resource.getFileExtension();
            if( extension != null && extension.equalsIgnoreCase( _fileExtension ) ) {
                if( _javaProject == null ) {
                    _javaProject = JavaCore.create( resource.getProject() );
                }
                if( _shouldOnClassPath ) {
                    if( _javaProject.isOnClasspath( resource ) ) {
                        _files.add( (IFile) resource );
                    }
                } else {
                    _files.add( (IFile) resource );

                }
            }
            return false;
        }

        private boolean shouldBeSourceRoot( IResource resource ) {
            if( resource instanceof IProject ) {
                return true;
            }
            if( PackageUtils.isUnderSrcFolder( resource ) ) {
                return true;
            }
            return false;
        }

        public List< IFile > getFiles() {
            return _files;
        }
    }

    /**
     * Gets all the projects in the current workspace which refer to the given <code>projects</code> including the
     * <code>projects</code> themselves.
     *
     * @param projects
     * @return
     */
    public static Set< IProject > getAllReferencingProjects( IProject[] projects ) {
        Set< IProject > allProjects = new HashSet< IProject >();
        IProject[] referencedProjects;
        for( IProject project : projects ) {
            allProjects.add( project );
            referencedProjects = project.getReferencingProjects();
            for( int i = 0; i < referencedProjects.length; i++ ) {
                allProjects.add( referencedProjects[ i ] );
            }
        }
        return allProjects;
    }

    private static class KeyFileVisitor implements IResourceVisitor {
        List< IFile > _files;

        public KeyFileVisitor() {
            _files = new ArrayList< IFile >();
        }

        public boolean visit( IResource resource ) throws CoreException {
            if( !( resource instanceof IFile ) ) {
                return shouldBeSourceRoot( resource );
            }
            String extension = resource.getFileExtension();
            if( extension != null ) {
                if( extension.equalsIgnoreCase( IConstants.KEY_FILE_EXTENSION ) ) {
                    _files.add( (IFile) resource );
                }
            }
            return false;
        }

        private boolean shouldBeSourceRoot( IResource resource ) {
            if( resource instanceof IProject ) {
                return true;
            }
            if( PackageUtils.isUnderSrcFolder( resource ) ) {
                return true;
            }
            return false;
        }

        public List< IFile > getFiles() {
            return _files;
        }
    }

    /**
     * This class hold the key information for a rrh file.
     *
     *
     */
    static public class RRHFile {
        IFile _file;
        String _resourceClassName;
        Hashtable< String, String > _keyTable;

        public RRHFile( String resourceClassName, IFile file, Hashtable< String, String > keyTable ) {
            _resourceClassName = resourceClassName;
            _file = file;
            _keyTable = keyTable;
        }

        public IFile getFile() {
            return _file;
        }

        public Hashtable< String, String > getKeyTalbe() {
            if( _keyTable == null && _file != null ) {
                try {
                    _keyTable = getKeysFromRRHFile( _file.getLocation().toOSString() );
                } catch( CoreException e ) {
                    logger.error( e );
                }
            }
            return _keyTable;
        }

        public String getResourceClassName() {
            return _resourceClassName;
        }
    }

    /**
     * Returns the highest VM used by given projects.
     *
     * @param projects
     *            The collection of <code>BlackBerryProject</code>
     * @return The <code>IVMInstall</code>
     */
    public static IVMInstall getVMForProjects( Collection< BlackBerryProject > projects ) {
        IVMInstall targetVM = null;
        List< IVMInstall > availableVMs = VMUtils.getInstalledBBVMs();
        for( BlackBerryProject project : projects ) {
            IVMInstall vm = ProjectUtils.getVMForProject( project );
            // The VM must be available
            if( vm != null && availableVMs.contains( vm ) ) {
                if( targetVM != null ) {
                    // use the highest version of VM
                    if( vm.getId().compareTo( targetVM.getId() ) > 0 ) {
                        targetVM = vm;
                    }
                } else {
                    targetVM = vm;
                }
            }
        }
        if( targetVM == null ) {
            targetVM = VMUtils.getDefaultBBVM();
        }
        return targetVM;
    }

    /**
     * Returns the JVM version as conventional last token
     *
     * @param iproj
     * @return -like "6.0.0"
     */
    public static String getVMVersionForProject( IProject iproj ) {
        String ver = "";
        IVMInstall ivmi = ProjectUtils.getVMForProject( JavaCore.create( iproj ) );
        if( ivmi != null ) {
            ver = ivmi.getId().substring( ivmi.getId().lastIndexOf( ' ' ) + 1 );
        }
        return ver;
    }

    /**
     * Create the given <code>file</code> if it does not exist. Also, this methods creates the parent folder if it does not exist.
     *
     * @param file
     * @return A File instance if the file has been successfully created otherwise return <code>null</code>.
     */
    public static File createFile( File file ) {
        if( !file.exists() ) {
            File parent = file.getParentFile();
            if( !parent.exists() ) {
                parent.mkdirs();
            }
            try {
                file.createNewFile();
            } catch( IOException e ) {
                logger.error( e );
            }
        }
        if( !file.exists() ) {
            return null;
        }
        return file;
    }
}
