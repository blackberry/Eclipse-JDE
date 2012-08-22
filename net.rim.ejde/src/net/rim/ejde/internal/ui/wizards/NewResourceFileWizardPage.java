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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.URI;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.util.FileUtils;
import net.rim.ejde.internal.util.ImportUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PackageUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.sdk.resourceutil.ResourceCollection;
import net.rim.sdk.resourceutil.ResourceCollectionFactory;
import net.rim.sdk.resourceutil.ResourceConstants;
import net.rim.sdk.resourceutil.ResourceElement;
import net.rim.sdk.resourceutil.ResourceLocale;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.internal.ide.dialogs.CreateLinkedResourceGroup;

/**
 * This page is used to create and link new resource files.
 *
 * @author jkeshavarzi
 */
public class NewResourceFileWizardPage extends WizardNewFileCreationPage {
    private String _resourcePackageId;
    private boolean _validPackage;
    private static final Logger logger = Logger.getLogger( NewResourceFileWizardPage.class );
    private IPackageFragment _userSelectedPackage;
    private IPackageFragmentRoot _userSelectedSourceFolder;
    private String _userSelectedProject;
    private URI linkFileLocation;

    /**
     * Constructs a new resource wizard page.
     */
    public NewResourceFileWizardPage( IStructuredSelection selection ) {
        super( "NewResourceFileWizardPage", selection ); //$NON-NLS-1$
        setTitle( Messages.NewResourceFileWizardNewProjectTitle );
        setDescription( Messages.NewResourceFileWizardDescription );
    }

    /**
     * This method returns the resource package id.
     *
     * @return A string containing the package id.
     */
    public String getResourcePackageId() {
        return _resourcePackageId;
    }

    /**
     * @return the package selected by the user within the new resource dialog
     */
    public IPackageFragment getUserSelectedPackage() {
        return _userSelectedPackage;
    }

    /**
     * @return the source folder selected by the user within the new resource dialog
     */
    public IPackageFragmentRoot getUserSelectedSourceFolder() {
        return _userSelectedSourceFolder;
    }

    /**
     * @return the project selected by the user within the new resource dialog
     */
    public String getUserSelectedProject() {
        return _userSelectedProject;
    }

    /**
     * This method returns whether the package selected is valid.
     *
     * @return True is the package is valid, false otherwise.
     */
    public boolean isValidPackage() {
        return _validPackage;
    }

    @Override
    protected boolean validatePage() {
        // perform preliminary page validation with
        // WizardNewFileCreationPage.validatePage() method
        boolean validPage = super.validatePage();

        if( validPage ) {
            if( !getFileName().matches( "([A-Za-z0-9]+([_][A-Za-z0-9]+){0,2}.rrc)|([A-Za-z0-9]+.rrh)" ) ) {
                setErrorMessage( Messages.INVALID_FILE_NAME );
                validPage = false;
            }
        }

        if( validPage ) {
            /* ensures the user selected a package under an eclipse source directory */
            boolean sourceFolderExists = false;
            _userSelectedProject = getContainerFullPath().segment( 0 );
            String path = getContainerFullPath().toString();

            IPackageFragmentRoot sourceRoots[] = ProjectUtils.getProjectSourceFolders( ProjectUtils
                    .getProject( _userSelectedProject ) );
            // Iterate through all source folders
            for( IPackageFragmentRoot sourceRoot : sourceRoots ) {
                String sourcePath = sourceRoot.getPath().toString() + IPath.SEPARATOR;
                if( path.startsWith( sourcePath ) && ( path.length() > sourcePath.length() ) ) {
                    String selectedPackage = path.substring( sourcePath.length() ).replaceAll( "/", "." ); //$NON-NLS-1$ //$NON-NLS-2$
                    // Since our current code standard is 1.5 do not use 1.6 methods yet.
                    if( !StringUtils.isEmpty( selectedPackage ) && sourceRoot.getPackageFragment( selectedPackage ).exists() ) {
                        sourceFolderExists = true;
                        _userSelectedPackage = sourceRoot.getPackageFragment( selectedPackage );
                        _userSelectedSourceFolder = sourceRoot;
                        break;
                    }
                }
            }
            if( !sourceFolderExists ) {
                setErrorMessage( Messages.INVALID_PARENT_FOLDER_WIZARD_PAGE );
                validPage = false;
            }
        }

        if( validPage ) {
            try {
                /*
                 * Here we get access to the private field linkedResourceGroup and get the URI information for the linked .rrh
                 * file. We then parse that file to get its package information
                 */
                Field field = WizardNewFileCreationPage.class.getDeclaredField( "linkedResourceGroup" ); //$NON-NLS-1$
                field.setAccessible( true );
                linkFileLocation = ( (CreateLinkedResourceGroup) field.get( this ) ).getLinkTargetURI();

                // If file is not linked, skip next code.
                if( linkFileLocation == null ) {
                    _validPackage = true;
                } else {
                    // Initialize the resourcePackageID to properly identify if the package is valid
                    _resourcePackageId = null;

                    File linkFile = new File( linkFileLocation );
                    if( getFileName().endsWith( ResourceConstants.RRH_SUFFIX ) ) {
                        /*
                         * Resource file is a rrh file. We must determine if its going in the correct package.
                         */
                        if( linkFile.length() == 0 ) {
                            setErrorMessage( Messages.RRH_NO_PACKAGE_ERROR );
                            return false;
                        }
                        _resourcePackageId = PackageUtils.getRRHPackageID( linkFile );

                    } else {
                        /*
                         * Resource file is a rrc file. We must determine the correct package with its corresponding rrh file.
                         */
                        String rrhFileName = getFileName();

                        if( rrhFileName.contains( "_" ) ) { //$NON-NLS-1$
                            rrhFileName = rrhFileName.substring( 0, rrhFileName.indexOf( "_" ) ) + ResourceConstants.RRH_SUFFIX; //$NON-NLS-1$
                        } else {
                            rrhFileName = rrhFileName.replace( ResourceConstants.RRC_SUFFIX, ResourceConstants.RRH_SUFFIX );
                        }

                        File rrhFile = new File( linkFile.getParent() + File.separator + rrhFileName );
                        if( rrhFile.exists() && ( rrhFile.length() != 0 ) ) {
                            _resourcePackageId = PackageUtils.getRRHPackageID( rrhFile );
                        }
                    }

                    /*
                     * Get package selected by user within the new resource wizard dialog and ensure it matches the package
                     * described in the .rrh file.
                     */
                    _userSelectedProject = getContainerFullPath().segment( 0 );
                    if( _resourcePackageId == null ) {
                        /*
                         * Resource header file could not be found or contains no package. Ensuring correct package will have to
                         * be left to user.
                         */
                        _validPackage = true;
                    } else if( !_resourcePackageId.equals( _userSelectedPackage.getElementName() ) ) {
                        setMessage( NLS.bind( Messages.INVALID_PACKAGE_SELECTED, _resourcePackageId ), IMessageProvider.WARNING );
                        _validPackage = false;
                    } else {
                        _validPackage = true;
                    }
                }
            } catch( Throwable e ) {
                logger.error( "Validation Error", e ); //$NON-NLS-1$
            }
        }
        if( validPage ) {
            // Add warning if existing sibling resource exist
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject( _userSelectedProject );
            IFile foundSiblingFile = getSiblingFile( getFileName(), project );

            if( foundSiblingFile != null ) {
                if( foundSiblingFile.isLinked() ) {
                    setMessage(
                            NLS.bind( Messages.EXISTING_LINKED_SIBLING_FOUND_WARNING, foundSiblingFile.getName(), getFileName() ),
                            IMessageProvider.WARNING );
                } else {
                    setMessage(
                            NLS.bind( Messages.EXISTING_COPIED_SIBLING_FOUND_WARNING, foundSiblingFile.getName(), getFileName() ),
                            IMessageProvider.WARNING );
                }
            }
        }
        return validPage;
    }

    private IFile getSiblingFile( String fileName, IProject project ) {
        String rrhFileName = null;
        String rrcRootLocaleFileName = null;
        String rrcRootLanguageLocaleFileName = null;
        String siblingFileName = null;
        String fileNamePrefix = null;
        IFile foundSiblingFile = null;

        boolean hasCountryCode = ( fileName.indexOf( IConstants.UNDERSCORE_STRING ) != fileName
                .lastIndexOf( IConstants.UNDERSCORE_STRING ) );
        if( fileName.contains( IConstants.UNDERSCORE_STRING ) ) {
            rrhFileName = fileName.substring( 0, fileName.indexOf( IConstants.UNDERSCORE_STRING ) )
                    + ResourceConstants.RRH_SUFFIX;
            // set root rrc file name
            rrcRootLocaleFileName = fileName.substring( 0, fileName.indexOf( IConstants.UNDERSCORE_STRING ) )
                    + ResourceConstants.RRC_SUFFIX;
            if( hasCountryCode ) {
                // set root language rrc file name
                rrcRootLanguageLocaleFileName = fileName.substring( 0, fileName.lastIndexOf( IConstants.UNDERSCORE_STRING ) )
                        + ResourceConstants.RRC_SUFFIX;
            }
        } else {
            rrhFileName = fileName.substring( 0, fileName.lastIndexOf( IConstants.DOT_MARK ) ) + ResourceConstants.RRH_SUFFIX;
            rrcRootLocaleFileName = fileName.substring( 0, fileName.lastIndexOf( IConstants.DOT_MARK ) )
                    + ResourceConstants.RRC_SUFFIX;
        }

        if( fileName.endsWith( ResourceConstants.RRH_SUFFIX ) ) {
            siblingFileName = rrcRootLocaleFileName;
            foundSiblingFile = ProjectUtils.getProjectIFile( project, siblingFileName, false );
            if( foundSiblingFile == null ) {
                fileNamePrefix = fileName.substring( 0, fileName.lastIndexOf( IConstants.DOT_MARK ) )
                        + IConstants.UNDERSCORE_STRING;
                foundSiblingFile = ProjectUtils.getProjectIFile( project, fileNamePrefix, false );
                siblingFileName = foundSiblingFile == null ? null : foundSiblingFile.getName();
            }
        } else {
            siblingFileName = rrhFileName;
            foundSiblingFile = ProjectUtils.getProjectIFile( project, siblingFileName, false );
            if( foundSiblingFile == null ) {
                siblingFileName = rrcRootLocaleFileName;
                foundSiblingFile = ProjectUtils.getProjectIFile( project, siblingFileName, false );
            }
            if( foundSiblingFile == null ) {
                siblingFileName = rrcRootLanguageLocaleFileName;
                foundSiblingFile = ProjectUtils.getProjectIFile( project, siblingFileName, false );
            }
            if( foundSiblingFile == null ) {
                if( fileName.contains( IConstants.UNDERSCORE_STRING ) ) {
                    fileNamePrefix = fileName.substring( 0, fileName.indexOf( IConstants.UNDERSCORE_STRING ) )
                            + IConstants.UNDERSCORE_STRING;
                } else {
                    fileNamePrefix = fileName.substring( 0, fileName.lastIndexOf( IConstants.DOT_MARK ) )
                            + IConstants.UNDERSCORE_STRING;
                }
                foundSiblingFile = ProjectUtils.getProjectIFile( project, fileNamePrefix, false );
                siblingFileName = foundSiblingFile == null ? null : foundSiblingFile.getName();
            }
        }
        return foundSiblingFile;
    }

    // This method was overwritten to provide fix for DPI222448 and some
    // new functionality.
    @Override
    public IFile createNewFile() {
        String fileName = this.getFileName();
        IFile newResourceFile = null;
        IPath resFilePath = null;
        IPath resParentPath = null;

        String rrcRootLocaleFileName = null;
        String rrcRootLanguageLocaleFileName = null;
        IFile siblingIFile = null;
        String pkgString = null;

        boolean hasCountryCode = ( fileName.indexOf( IConstants.UNDERSCORE_STRING ) != fileName
                .lastIndexOf( IConstants.UNDERSCORE_STRING ) );
        if( fileName.contains( IConstants.UNDERSCORE_STRING ) ) {
            // set root rrc file name
            rrcRootLocaleFileName = fileName.substring( 0, fileName.indexOf( IConstants.UNDERSCORE_STRING ) )
                    + ResourceConstants.RRC_SUFFIX;
            if( hasCountryCode ) {
                // set root language rrc file name
                rrcRootLanguageLocaleFileName = fileName.substring( 0, fileName.lastIndexOf( IConstants.UNDERSCORE_STRING ) )
                        + ResourceConstants.RRC_SUFFIX;
            }
        } else {
            rrcRootLocaleFileName = fileName.substring( 0, fileName.lastIndexOf( IConstants.DOT_MARK ) )
                    + ResourceConstants.RRC_SUFFIX;
        }

        IProject iProject = ProjectUtils.getProject( _userSelectedProject );
        siblingIFile = getSiblingFile( fileName, iProject );
        String userSelectedPackage = null;

        try {
            if( ( siblingIFile != null ) && siblingIFile.exists() ) {
                // sibling file is found in the rim project but not under
                // the same directory. Possibly file is linked from another
                // place.
                resFilePath = siblingIFile.getLocation();
                File siblingFile = new File( resFilePath.toOSString() );
                resParentPath = resFilePath.removeLastSegments( 1 );
                resFilePath = resParentPath.append( fileName );

                // even though user selects the improper package
                // we will match the right package based on
                // sibling file package string
                if( siblingIFile.getName().endsWith( ResourceConstants.RRH_SUFFIX ) ) {
                    pkgString = PackageUtils.getRRHPackageString( siblingFile );
                } else {
                    File rrhFile = PackageUtils.getCorrespondingRRHFile( siblingFile );
                    // eg:- com.rim.test
                    userSelectedPackage = _userSelectedPackage.getElementName();
                    // eg:- com/rim/test
                    userSelectedPackage = PackageUtils.convertPkgIDToString( userSelectedPackage );

                    if( !rrhFile.exists() ) {
                        pkgString = userSelectedPackage;
                    } else {
                        try {
                            pkgString = PackageUtils.getRRCPackageString( siblingFile );
                        } catch( CoreException e ) {
                            // we don't need to log the error here - method already logging the error
                            pkgString = userSelectedPackage;
                        }
                    }
                }

                // special case when user try to link resource
                // file with country code
                // (Res1_en_CA.rrc) in the linked location
                // but rrcRootLocaleFileName and
                // rrcRootLanguageLocaleFileName is missing in
                // that directory will be an error. So we need
                // to create those files and linked them as
                // well.
                File newRootFile = null;
                if( !StringUtils.isBlank( rrcRootLocaleFileName ) ) {
                    newRootFile = new File( resParentPath.append( rrcRootLocaleFileName ).toOSString() );
                    if( !newRootFile.exists() ) {
                        newRootFile.createNewFile();
                    }
                }
                File newRootLangLocaleFile = null;
                if( !StringUtils.isBlank( rrcRootLanguageLocaleFileName ) ) {
                    newRootLangLocaleFile = new File( resParentPath.append( rrcRootLanguageLocaleFileName ).toOSString() );
                    if( !newRootLangLocaleFile.exists() ) {
                        newRootLangLocaleFile.createNewFile();
                    }
                }

                File newFile = resFilePath.toFile();
                if( !newFile.exists() ) {
                    String newFileName = newFile.getName();
                    newFile = new File( resFilePath.toOSString() );

                    File existingFile = null;
                    if( linkFileLocation != null ) {
                        existingFile = new File( linkFileLocation );
                    }

                    if( ( existingFile != null ) && existingFile.exists() ) {
                        FileUtils.copy( existingFile, newFile );
                        ResourceCollection collection = ResourceCollectionFactory.newResourceCollection( newFile
                                .getAbsolutePath() );
                        if( newFileName.endsWith( ResourceConstants.RRH_SUFFIX ) ) {
                            ResourceLocale existingLocales[] = collection.getLocales();
                            for( ResourceLocale locale : existingLocales ) {
                                addMissingLocaleKeys( collection, locale );
                            }
                        } else {
                            String localeName = newFileName.substring(
                                    newFileName.indexOf( "_" ) + 1, newFileName.lastIndexOf( "." ) ); //$NON-NLS-1$ //$NON-NLS-2$
                            ResourceLocale newLocale = collection.getLocale( localeName );
                            if( newLocale != null ) {
                                addMissingLocaleKeys( collection, newLocale );
                            }
                        }
                    } else {
                        newFile.createNewFile();
                        if( newFileName.endsWith( ResourceConstants.RRH_SUFFIX ) ) {
                            // rrh file must have a package declaration
                            PrintWriter out = new PrintWriter( new FileWriter( newFile ) );
                            out.println( "package " + PackageUtils.convertPkgStringToID( pkgString ) + ";" );
                            out.close();
                        }
                    }
                }

                if( siblingIFile.isLinked() ) {
                    IPath sourceLocation = _userSelectedSourceFolder.getCorrespondingResource().getProjectRelativePath()
                            .append( pkgString );
                    IContainer parentFolder = iProject.getFolder( sourceLocation );
                    if( newRootFile != null ) {
                        IFile linkRootFile = parentFolder.getFile( new Path( rrcRootLocaleFileName ) );
                        if( !linkRootFile.exists() ) {
                            linkRootFile.createLink( new Path( newRootFile.getAbsolutePath() ), IResource.NONE,
                                    new NullProgressMonitor() );
                        }
                    }
                    if( newRootLangLocaleFile != null ) {
                        IFile linkRootLangFile = parentFolder.getFile( new Path( rrcRootLanguageLocaleFileName ) );
                        if( !linkRootLangFile.exists() ) {
                            linkRootLangFile.createLink( new Path( newRootLangLocaleFile.getAbsolutePath() ), IResource.NONE,
                                    new NullProgressMonitor() );
                        }
                    }
                    IFile linkFile = parentFolder.getFile( new Path( fileName ) );
                    if( !linkFile.exists() ) {
                        linkFile.createLink( resFilePath, IResource.NONE, new NullProgressMonitor() );
                        newResourceFile = linkFile;
                    }
                } else {
                    iProject.refreshLocal( IProject.DEPTH_INFINITE, new NullProgressMonitor() );
                    newResourceFile = ImportUtils.getProjectBasedFileFromOSBasedFile( _userSelectedProject,
                            newFile.getAbsolutePath() );
                }

            } else {
                // Sibling file does not exists in the linked
                // location.At this point we have no clue where
                // to link, so it is better to create the file
                // in eclipse workspace.
            }
            if( newResourceFile == null ) {
                super.setFileName( super.getFileName().trim() );
                newResourceFile = super.createNewFile();
            }
        } catch( Throwable e ) {
            logger.error( "createNewFile() error", e ); //$NON-NLS-1$
        }
        return newResourceFile;
    }

    /**
     * This method will add any keys found in the locale but not in the header file to the header file.
     *
     * @param collection
     *            - The collection to add missing keys to.
     * @param newLocale
     *            - The locale to check for keys.
     */
    private void addMissingLocaleKeys( ResourceCollection collection, ResourceLocale locale ) {
        ResourceElement elements[] = locale.getResourceElements();
        String key = null;
        for( ResourceElement element : elements ) {
            key = element.getKey();
            if( !collection.containsKey( key ) ) {
                collection.addKey( key );
            }
        }
        try {
            collection.save();
        } catch( IOException e ) {
            logger.error( "addMissingLocaleKeys() error", e ); //$NON-NLS-1$
        }
    }

}
