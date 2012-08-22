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
package net.rim.ejde.internal.ui.wizards;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.sdk.resourceutil.ResourceConstants;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.internal.ide.misc.ContainerSelectionGroup;
import org.eclipse.ui.internal.ide.misc.ResourceAndContainerGroup;

/**
 * The wizard for creating new .rrh header and .rrc content resource files. Normally, it can be accessed via the menu by File >
 * New > Other... > BlackBerry > BlackBerry Resource File.
 *
 * @author edwong, jkeshavarzi
 *
 */
public class NewResourceFileWizard extends Wizard implements INewWizard {

    protected IWorkbench workbench;
    private IStructuredSelection selection;
    private NewResourceFileWizardPage newResourceFileCreationPage;
    private static final Logger logger = Logger.getLogger( NewResourceFileWizard.class );

    //public static final String WIZARD_ID = "net.rim.eide.ui.wizards.WorkspaceNewProjectWizard"; //$NON-NLS-1$

    public NewResourceFileWizard() {
        super();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init( IWorkbench workbench, IStructuredSelection selection ) {
        this.workbench = workbench;
        this.selection = selection;
        setWindowTitle( Messages.newResourceFileWindowsTitle ); //$NON-NLS-1$
        setDefaultPageImageDescriptor( ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID,
                Messages.IMAGE_DESCRIPTOR_FILE_PATH ) );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.Wizard#addPages()
     */
    public void addPages() {
        if( selection != null ) {
            newResourceFileCreationPage = new NewResourceFileWizardPage( selection );
        } else {
            newResourceFileCreationPage = new NewResourceFileWizardPage( new StructuredSelection() );
        }
        addPage( newResourceFileCreationPage );
    }

    private String createPackageStatement( IPackageFragment userSelectedPackage ) {
        StringBuffer packageStatementBuffer = new StringBuffer();
        packageStatementBuffer.append( "package " ); //$NON-NLS-1$
        packageStatementBuffer.append( userSelectedPackage.getElementName() );
        packageStatementBuffer.append( ";" ); //$NON-NLS-1$

        return packageStatementBuffer.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        final IProject userSelectedProject = ProjectUtils.getProject( newResourceFileCreationPage.getUserSelectedProject() );

        String fileName = newResourceFileCreationPage.getFileName();
        IFile newResourceFile = null;
        IPackageFragmentRoot userSelectedSourceFolder = newResourceFileCreationPage.getUserSelectedSourceFolder();
        IPackageFragment userSelectedPackage = newResourceFileCreationPage.getUserSelectedPackage();

        String _resourcePackageId = newResourceFileCreationPage.getResourcePackageId();

        if( newResourceFileCreationPage.isValidPackage() ) {
            // Package is valid. No need to find or create a package.
            newResourceFile = newResourceFileCreationPage.createNewFile();
        } else {
            // Package selected is invalid. First we try and find package within
            // project workspace.
            IPackageFragmentRoot sourceRoots[] = ProjectUtils.getProjectSourceFolders( userSelectedProject );
            try {
                IJavaElement foundPackageElement = null;

                for( IPackageFragmentRoot sourceRoot : sourceRoots ) {
                    IPackageFragment packageFragment = sourceRoot.getPackageFragment( _resourcePackageId );
                    if( packageFragment.exists() ) {
                        foundPackageElement = packageFragment;
                        break;
                    }
                }
                if( foundPackageElement == null ) {
                    // Package could not be found. We must create the package.
                    IPackageFragment newlyCreatedPackage = userSelectedSourceFolder.createPackageFragment( _resourcePackageId,
                            true, null );

                    /*
                     * The following code uses reflection to access a private TreeViewer. I had to work through multiple levels of
                     * objects to access the private field. In the future this code can break, if the hierarchy for these classes
                     * or the names of these fields change. Once i access the TreeViewer i then refresh it. This allows the newly
                     * created package to be added to the Tree and prevents a NullPointerException.
                     */
                    Field fieldToAccess = WizardNewFileCreationPage.class.getDeclaredField( "resourceGroup" ); //$NON-NLS-1$
                    fieldToAccess.setAccessible( true );
                    ResourceAndContainerGroup resourceGroup = ( (ResourceAndContainerGroup) fieldToAccess
                            .get( newResourceFileCreationPage ) );

                    fieldToAccess = ResourceAndContainerGroup.class.getDeclaredField( "containerGroup" ); //$NON-NLS-1$
                    fieldToAccess.setAccessible( true );
                    ContainerSelectionGroup containerGroup = ( (ContainerSelectionGroup) fieldToAccess.get( resourceGroup ) );

                    fieldToAccess = ContainerSelectionGroup.class.getDeclaredField( "treeViewer" ); //$NON-NLS-1$
                    fieldToAccess.setAccessible( true );
                    TreeViewer treeViewer = ( (TreeViewer) fieldToAccess.get( containerGroup ) );
                    treeViewer.refresh();

                    /*
                     * This code will set the container path to null if the above tree isn't refreshed when the package is
                     * created.
                     */
                    newResourceFileCreationPage.setContainerFullPath( newlyCreatedPackage.getPath() );

                } else {
                    // Package was found in project. Change the container path
                    // to the found package.
                    newResourceFileCreationPage.setContainerFullPath( foundPackageElement.getPath() );
                }

                newResourceFile = newResourceFileCreationPage.createNewFile();

            } catch( Throwable e ) {
                logger.error( "performFinish() error", e ); //$NON-NLS-1$
            }
        }

        // if resource file is linked, we just return. Fix SDR213684
        if( newResourceFile.isLinked() ) {
            return true;
        }

        String packageStmt = createPackageStatement( userSelectedPackage );
        FileOutputStream fout = null;

        try {
            if( fileName.endsWith( ResourceConstants.RRH_SUFFIX ) ) {
                // user enters .rrh file extension
                // 1. create package statement
                File resourceFile = newResourceFile.getLocation().toFile();

                if( resourceFile.length() == 0 && resourceFile.canWrite() ) {
                    fout = new FileOutputStream( resourceFile );
                    new PrintStream( fout ).println( packageStmt );
                }

                // 2. create associated .rrc root locale file if it doesn't
                // exist
                String rrcFileName = fileName.substring( 0, fileName.lastIndexOf( "." ) ) + ResourceConstants.RRC_SUFFIX; //$NON-NLS-1$

                File rrcOSFile = new File( newResourceFile.getLocation().toFile().getParentFile(), rrcFileName );

                if( !rrcOSFile.exists() ) {
                    rrcOSFile.createNewFile();// TO->JDP
                }
            }

            if( fileName.endsWith( ResourceConstants.RRC_SUFFIX ) ) {
                // user enters .rrc file extension
                // if corresponding rrh file doesn't exist, create it and set
                // package statement
                // if corresponding .rrc root locale file doesn't exist, create
                // it as well
                String rrhFileName;
                String rrcRootLocaleFileName = null;
                String rrcRootLanguageLocaleFileName = null;
                boolean hasCountryCode = ( fileName.indexOf( "_" ) != fileName.lastIndexOf( "_" ) ); //$NON-NLS-1$ //$NON-NLS-2$

                if( fileName.contains( "_" ) ) { //$NON-NLS-1$
                    rrhFileName = fileName.substring( 0, fileName.indexOf( "_" ) ) + ResourceConstants.RRH_SUFFIX; //$NON-NLS-1$
                    // set root rrc file name
                    rrcRootLocaleFileName = fileName.substring( 0, fileName.indexOf( "_" ) ) + ResourceConstants.RRC_SUFFIX; //$NON-NLS-1$
                    if( hasCountryCode ) {
                        // set root language rrc file name
                        rrcRootLanguageLocaleFileName = fileName.substring( 0, fileName.lastIndexOf( "_" ) ) + ResourceConstants.RRC_SUFFIX; //$NON-NLS-1$
                    }
                } else {
                    rrhFileName = fileName.substring( 0, fileName.lastIndexOf( "." ) ) + ResourceConstants.RRH_SUFFIX; //$NON-NLS-1$
                }

                File rrhOSFile = new File( newResourceFile.getLocation().toFile().getParentFile(), rrhFileName );

                if( !rrhOSFile.exists() ) {
                    rrhOSFile.createNewFile();// // TO->JDP

                    if( rrhOSFile.length() == 0 && rrhOSFile.canWrite() ) {
                        fout = new FileOutputStream( rrhOSFile );
                        new PrintStream( fout ).println( packageStmt );
                    }
                }

                File rrcOSFile = null;
                // create .rrc root locale file if required
                if( rrcRootLocaleFileName != null ) {
                    rrcOSFile = new File( newResourceFile.getLocation().toFile().getParentFile(), rrcRootLocaleFileName );

                    if( !rrcOSFile.exists() ) {
                        rrcOSFile.createNewFile();
                    }
                }

                // create .rrc root language locale file if required
                if( rrcRootLanguageLocaleFileName != null ) {
                    rrcOSFile = new File( newResourceFile.getLocation().toFile().getParentFile(), rrcRootLanguageLocaleFileName );

                    if( !rrcOSFile.exists() ) {
                        rrcOSFile.createNewFile();
                    }
                }
            }
        } catch( Exception e ) {
            logger.error( "performFinish: Error creating file", e ); //$NON-NLS-1$
            return false;
        } finally {
            try {
                if( null != fout )
                    fout.close();
            } catch( IOException e ) {
                logger.error( "performFinish: Could not close the file", e ); //$NON-NLS-1$
            }
        }

        // Fix for DPI224873. Project becomes out of sync, which results
        // in out
        // of sync errors. The below will refresh project.
        try {
            userSelectedProject.refreshLocal( IResource.DEPTH_INFINITE, new NullProgressMonitor() );
        } catch( CoreException e ) {
            logger.error( "performFinish: Error during project refresh", e ); //$NON-NLS-1$
        }

        return true;
    }

    protected IFile createExtraNewFile( String fileName ) {
        final IPath containerPath = newResourceFileCreationPage.getContainerFullPath();
        IPath newFilePath = containerPath.append( fileName );
        final IFile newFileHandle = ResourcesPlugin.getWorkspace().getRoot().getFile( newFilePath );
        return newFileHandle;
    }

}
