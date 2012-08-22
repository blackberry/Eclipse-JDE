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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ide.JavaParser;
import net.rim.ide.Project;
import net.rim.sdk.rc.ConvertUtil;
import net.rim.sdk.rc.ResourceCollection;
import net.rim.sdk.rc.parser.ResourceHeaderParser;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.osgi.util.NLS;

/**
 * A utility class to construct/retrieve package informations.
 *
 * @author Raj Gunaratnam
 * @since 11 December, 08
 */
public class PackageUtils implements net.rim.ejde.internal.core.IConstants {
    private static final Logger _logger = Logger.getLogger( PackageUtils.class );

    /**
     * Get the package name of the given <code>rrhFile</code>.
     * <p>
     * The returned package is in <code>com/rim/test</code> format.
     * </p>
     *
     * @param rrhFile
     * @return packageId String or <code>null</code> if the given file is null or the given file is not a rrh file
     * @throws CoreException
     */
    public static String getRRHPackageString( File rrhFile ) throws CoreException {
        String packageName = getRRHPackageID( rrhFile );
        if( !StringUtils.isBlank( packageName ) ) {
            packageName = convertPkgIDToString( packageName );
        }
        return packageName;
    }

    /**
     * Get the file name of corresponding rrh file for the given <code>rrcFile</code>.
     *
     * @param rrcFile
     * @return rrhFile
     */
    public static File getCorrespondingRRHFile( File rrcFile ) {
        if( rrcFile == null ) {
            return null;
        }
        IPath rrcPath = new Path( rrcFile.getPath() );
        String extension = rrcPath.getFileExtension();
        if( ( extension == null ) || !extension.equalsIgnoreCase( RRC_FILE_EXTENSION ) ) {
            return null;
        }
        String fileName = rrcFile.getName();
        if( fileName.contains( "_" ) ) {
            fileName = fileName.substring( 0, fileName.indexOf( '_' ) );
        } else {
            fileName = fileName.substring( 0, fileName.indexOf( DOT_CHAR ) );
        }
        fileName += RRH_FILE_EXTENSION_WITH_DOT;
        IPath rrhPath = rrcPath.removeLastSegments( 1 );
        rrhPath = rrhPath.append( fileName );
        return rrhPath.toFile();
    }

    /**
     * Get the package string of the given <code>rrcFile</code>.
     * <p>
     * The returned package is in <code>com/rim/test</code> format.
     * </p>
     *
     * @param rrcFile
     *            File
     * @return packageId String or <code>null</code> if the given file is null or the given file is not a rrc file
     * @throws CoreException
     */
    public static String getRRCPackageString( File rrcFile ) throws CoreException {
        String packageId;
        packageId = getRRCPackageID( rrcFile );
        if( !StringUtils.isBlank( packageId ) ) {
            packageId = convertPkgIDToString( packageId );
        }
        return packageId;
    }

    /**
     * Get the package id of the given <code>rrcFile</code>.
     * <p>
     * The returned package is in <code>com.rim.test</code> format.
     * </p>
     *
     * @param rrcFile
     *            File
     * @return packageId String or <code>null</code> if the given file is null or the given file is not a rrc file
     * @throws CoreException
     */
    public static String getRRCPackageID( File rrcFile ) throws CoreException {
        if( rrcFile == null ) {
            return null;
        }
        if( !rrcFile.getName().endsWith( RRC_FILE_EXTENSION_WITH_DOT ) ) {
            return null;
        }
        String packageId = null;
        File rrhFile = getCorrespondingRRHFile( rrcFile );
        if( rrhFile != null ) {
            try {
                packageId = getRRHPackageID( rrhFile );
            } catch( CoreException e ) {
                throw new CoreException( StatusFactory.createErrorStatus( NLS.bind(
                        Messages.PackageUtils_RELATED_RRH_FILE_ERROR_MESSAGE, rrcFile.getPath() ) ) );
            }
        }
        return packageId;
    }

    /**
     * Get the package id of the given <code>rrhFile</code>.
     * <p>
     * The returned package is in <code>com.rim.test</code> format.
     * </p>
     *
     * @param rrhFile
     *            File
     * @return packageId String or <code>null</code> if the given file is null or the given file is not a rrh file
     * @throws CoreException
     */
    public static String getRRHPackageID( File rrhFile ) throws CoreException {
        if( rrhFile == null ) {
            return null;
        }
        if( !rrhFile.getName().endsWith( RRH_FILE_EXTENSION_WITH_DOT ) ) {
            return null;
        }
        if( !rrhFile.exists() ) {
            throw new CoreException( StatusFactory.createErrorStatus( NLS.bind( Messages.PackageUtils_RRH_FILE_NOT_EXIST,
                    rrhFile.toString() ) ) );
        }
        String fileStringPath = rrhFile.getPath();
        ResourceCollection collection = new ResourceCollection( fileStringPath.substring( 0, fileStringPath.length() - 4 ) );
        ResourceHeaderParser parser = null;
        String packageName = null;
        try {
            parser = new ResourceHeaderParser( fileStringPath, collection );
            parser.read();
        } catch( IOException e ) {
            _logger.error( NLS.bind( Messages.PackageUtils_PACKAGE_ID_ERROR_MSG, rrhFile.toString() ) );
            throw new CoreException( StatusFactory.createErrorStatus(
                    NLS.bind( Messages.PackageUtils_PACKAGE_ID_ERROR_MSG, rrhFile.toString() ), e ) );
        }
        packageName = collection.getPackage();
        return packageName;
    }

    /**
     * Get the package string of the given <code>imgFile</code> within given <code>eclipseProject</code>. This method will
     * calculate package id based on the JDP file location. This method can be used for any non java, non rrh and non rrc files
     * package calculation.
     * <p>
     * The returned package is in <code>com/rim/test</code> format.
     * </P>
     *
     * @param file
     *            File
     * @return packageId String
     */
    static public String buildGenaralFilePackageString( File file, Project legacyProject ) {
        IPath filePath = null;
        String pathAlias = EMPTY_STRING;

        if( ( file == null ) ) {
            throw new IllegalArgumentException(
                    NLS.bind( Messages.PackageUtils_UNDEFINED_FILE_ERROR_MSG, IConstants.EMPTY_STRING ) );
        }

        if( file.getName().endsWith( JAVA_EXTENSION_WITH_DOT ) || file.getName().endsWith( RRC_FILE_EXTENSION_WITH_DOT )
                || file.getName().endsWith( RRH_FILE_EXTENSION_WITH_DOT ) ) {
            throw new IllegalArgumentException( "This method doesn't process java/rrh/rrc files!" );
        }

        filePath = new Path( file.getPath() );

        if( legacyProject != null ) {
            IPath bbwkspath = ( new Path( legacyProject.getWorkspace().getFile().toString() ) ).removeLastSegments( 1 );
            IPath bbprjpath = ( new Path( legacyProject.getFile().toString() ) ).removeLastSegments( 1 );
            // remove the file name segment
            IPath resolvedPath = resolvePathForFile( filePath, bbprjpath, bbwkspath ).removeLastSegments( 1 );
            List< String > sources = ImportUtils.getSources( legacyProject );
            String firstSegment = resolvedPath.segment( 0 );
            if( sources.contains( firstSegment ) ) {
                resolvedPath = resolvedPath.removeFirstSegments( 1 );
            }
            pathAlias = resolvedPath.toString();
        }
        return pathAlias;// com/rim/test
    }

    /**
     * Deresolves a file path 1st referred to a project path, then to a workspace path or flatten if completely outside
     *
     * @param file
     * @param projectPath
     * @param workspacePath
     * @return
     */
    public static IPath resolvePathForFile( IPath filePath, IPath projectPath, IPath workspacePath ) {
        IPath deresolvedPath = deResolve( filePath, projectPath );

        if( deresolvedPath.isAbsolute() || DOUBLE_DOTS.equals( deresolvedPath.segment( 0 ) ) ) {
            deresolvedPath = deResolve( filePath, workspacePath );

            if( deresolvedPath.isAbsolute() || DOUBLE_DOTS.equals( deresolvedPath.segment( 0 ) ) ) {
                deresolvedPath = new Path( filePath.lastSegment() );
            }
        }

        return deresolvedPath;
    }

    /**
     * Return the absolute path of the given entry.
     *
     * @param currentEntry
     * @return the absolute path of the give entry. Return <code>null<code> if the entry can not be found.
     */
    public static IPath getAbsolutePath( IClasspathEntry currentEntry ) {
        if( currentEntry == null ) {
            return null;
        }
        if( currentEntry.getEntryKind() != IClasspathEntry.CPE_LIBRARY ) {
            return currentEntry.getPath();
        }
        IPath absolutePath = null;
        IPath path = currentEntry.getPath();
        Object target = JavaModel.getTarget( path, true );
        if( target instanceof IResource ) {
            // if the target was found inside workspace
            IResource resource = (IResource) target;
            absolutePath = resource.getLocation();
        } else if( target instanceof File ) {
            // if the target was found outside of the workspace
            absolutePath = new Path( ( (File) target ).getAbsolutePath() );
        }
        return absolutePath;
    }

    /**
     * @deprecatated Use {@link getAbsolutePath(IClasspathEntry)} instead
     */
    public static IPath getAbsoluteEntryPath( IClasspathEntry currentEntry ) {
        return getAbsolutePath( currentEntry );
    }

    /**
     * De-resolves a path vs a base path Example: path --> C:\workspace\myProject\com\rim\test\a.JPG base -->
     * C:\workspace\myProject\P1.jdp return path --> com/rim/test/a.JPG -- relative to JDP
     *
     * @param path
     *            IPath
     * @param base
     *            IPath
     * @return path IPath
     */
    public static IPath deResolve( IPath path, IPath base ) {
        IPath modfiedBase = base;
        if( modfiedBase.toFile().isFile() ) {
            modfiedBase = modfiedBase.removeLastSegments( 1 );
        }
        int mn = modfiedBase.matchingFirstSegments( path );
        if( mn > 0 ) {
            IPath dpath = path.setDevice( EMPTY_STRING ).removeFirstSegments( mn );
            int nj = modfiedBase.segmentCount() - mn;

            // TODO: Try using forward slash as a path separator in Windows
            String str = DOUBLE_DOTS + File.separator;

            IPath tempPath = new Path( str );
            for( int i = 0; i < nj; i++ ) {
                dpath = tempPath.append( dpath );
            }
            return dpath;
        }
        return path; // com/rim/test/a.JPG
    }

    /**
     * Gets all the source folders of the given <code>iJavaProject</code>.
     *
     * @param iJavaProject
     * @return
     */
    public static ArrayList< IContainer > getAllSrcFolders( IJavaProject iJavaProject ) {
        ArrayList< IContainer > result = new ArrayList< IContainer >();
        IProject iProject = iJavaProject.getProject();
        IClasspathEntry[] classPathEntries = null;
        IFolder srcFolder = null;
        IPath srcFolderPath = null;
        IPath relativeSrcFolderPath = null;
        try {
            classPathEntries = iJavaProject.getRawClasspath();
            for( IClasspathEntry classPathEntry : classPathEntries ) {
                if( classPathEntry.getEntryKind() != IClasspathEntry.CPE_SOURCE ) {
                    continue;
                }
                srcFolderPath = classPathEntry.getPath();
                if( srcFolderPath.segment( 0 ).equals( iProject.getName() ) ) {
                    // remove the project name from the path
                    relativeSrcFolderPath = srcFolderPath.removeFirstSegments( 1 );
                }
                if( relativeSrcFolderPath == null || relativeSrcFolderPath.isEmpty() ) {
                    result.add( iProject );
                } else {
                    srcFolder = iProject.getFolder( relativeSrcFolderPath );
                    if( srcFolder.exists() ) {
                        result.add( srcFolder );
                    }
                }
            }
        } catch( Throwable e ) {
            _logger.error( e.getMessage(), e );
        }
        return result;
    }

    /**
     * This method will retrieves the list of non derived source folders of the given <code>iJavaProject</code>.
     *
     * @param iJavaProject
     *            IJavaProject
     * @return result ArrayList<IFolder>
     */
    public static ArrayList< IContainer > getNonDerivedSrcFolders( IJavaProject iJavaProject ) {
        ArrayList< IContainer > result = getAllSrcFolders( iJavaProject );
        ArrayList< IContainer > newResult = new ArrayList< IContainer >();

        for( IContainer container : result ) {
            if( !container.isDerived() ) {
                newResult.add( container );
            }
        }
        return newResult;
    }

    /**
     * This method will retrieves the 1st source-folder other than src if multiple
     *
     * @param iJavaProject
     * @return <IFolder> srcFolder or null if no source-folders
     */
    public static IContainer getSrcFolderForNonPackage( IJavaProject iJavaProject ) {
        ArrayList< IContainer > srcFolders = getNonDerivedSrcFolders( iJavaProject );
        for( IContainer srcFolder : srcFolders ) {
            if( "src".equals( srcFolder.getName() ) && ( srcFolders.size() > 1 ) ) {
                continue;
            }
            return srcFolder;
        }
        return null;
    }

    /**
     * This method will return true if the given resource is under any source folders
     *
     * @param resource
     *            IResource
     * @return found Boolean
     */
    public static boolean isUnderSrcFolder( IResource resource ) {
        boolean found = false;
        IPath path = resource.getFullPath();
        IProject iProject = resource.getProject();
        IJavaProject iJavaProject = JavaCore.create( iProject );
        ArrayList< IContainer > srcc = PackageUtils.getAllSrcFolders( iJavaProject );
        for( IContainer srcFolder : srcc ) {
            IPath p = srcFolder.getFullPath();
            if( p.isPrefixOf( path ) || path.isPrefixOf( p ) ) {
                found = true;
                break;
            }
        }
        return found;
    }

    /**
     * This method will return true if the resource represented by the given <code>path</code> is under/linked to any source
     * folders of the <code>eclipseJavaProject</code>.
     *
     * @param eclipseJavaProject
     *            IJavaProject
     * @param path
     *            IPath
     *
     * @return
     */
    public static boolean isUnderSrcFolder( IJavaProject eclipseJavaProject, IPath path ) {
        List< IContainer > srcc = PackageUtils.getAllSrcFolders( eclipseJavaProject );
        IPath p;
        for( IContainer srcFolder : srcc ) {
            p = srcFolder.getLocation();
            if( p.isPrefixOf( path ) ) {
                return true;
            } else {
                // continue to check if the resource represented by the path is linked to the project
                FileSearchVisitor visitor = new FileSearchVisitor( path );
                try {
                    srcFolder.accept( visitor );
                } catch( CoreException e ) {
                    _logger.error( e.getMessage() );
                    return false;
                }
                if( visitor.found() ) {
                    return true;
                }
            }
        }
        return false;
    }

    static private class FileSearchVisitor implements IResourceVisitor {
        IPath _path;
        boolean _found;

        public FileSearchVisitor( IPath path ) {
            _path = path;
            _found = false;
        }

        @Override
        public boolean visit( IResource resource ) throws CoreException {
            if( _found ) {
                return false;
            }
            if( !( resource instanceof IFile ) ) {
                return true;
            }
            if( resource.getLocation().equals( _path ) ) {
                _found = true;
            }
            return false;
        }

        public boolean found() {
            return _found;
        }
    }

    /**
     * Get the project relative path of the resource represented by the given <code>filePath</code>. The <code>filePath</code> is
     * a path which contains package and file name.
     *
     * @param javaProject
     * @param filePath
     * @return Project relative path which contains source folder/package/filename, e.g. src/net/rim/api/a.java.
     *         <p>
     *         or <code>null</code> if the filepath does not exist in the project.
     */
    public static IPath getProjectRelativePath( IJavaProject javaProject, IPath filePath ) {
        List< IContainer > srcc = PackageUtils.getNonDerivedSrcFolders( javaProject );
        IPath p;
        IFile iFile;
        for( IContainer srcFolder : srcc ) {
            p = new Path( srcFolder.getName() ).append( filePath );
            iFile = javaProject.getProject().getFile( p );
            if( iFile.exists() ) {
                return p;
            }
        }
        return null;
    }

    /**
     * This method will return the source folder in which the given <code>resource</code> is located.
     *
     * @param resource
     *            IResource
     * @return the source folder in which the given <code>resource</code> is located or <code>null</code> if the given
     *         <code>resource</code> is not located in any source folder
     */
    public static IContainer getSrcFolder( IResource resource ) {
        IPath path = resource.getFullPath();
        IProject iProject = resource.getProject();
        IJavaProject iJavaProject = JavaCore.create( iProject );
        ArrayList< IContainer > srcFolders = getNonDerivedSrcFolders( iJavaProject );
        for( IContainer srcFolder : srcFolders ) {
            IPath p = srcFolder.getFullPath();
            if( p.isPrefixOf( path ) ) {
                return srcFolder;
            }
        }
        return null;
    }

    /**
     * Get package string for java file in <code>com/rim/test</code> format.
     *
     * @param file
     *            File
     * @return packageId String
     */
    public static String getJavaFilePackageString( File file ) {
        String packageId = getJavaFilePackageID( file );
        if( !StringUtils.isBlank( packageId ) ) {
            packageId = convertPkgIDToString( packageId );
        }
        return packageId; // com/rim/test
    }

    /**
     * Get package id for java file in com.rim.test format
     *
     * @param file
     *            File
     * @return packageId String
     */
    public static String getJavaFilePackageID( File file ) {
        String packageId = EMPTY_STRING;
        if( file == null ) {
            return EMPTY_STRING;
        }
        try {
            FileReader fileReader = new FileReader( file );
            JavaParser parser = new JavaParser( file, fileReader, true );
            packageId = parser.getPackage();
            try {
                fileReader.close();
            } catch( IOException ioe ) {
                _logger.error( ioe );
            }
            return packageId; // com.rim.test
        } catch( FileNotFoundException fnfe ) {
            return EMPTY_STRING;
        }
    }

    /**
     * Whether the given filename has a BB source type extension
     *
     * @param filename
     * @return
     */
    public static boolean hasSourceExtension( String filename ) {
        String filename_ = filename.toLowerCase();
        return filename_.endsWith( JAVA_EXTENSION_WITH_DOT ) || filename_.endsWith( RRH_FILE_EXTENSION_WITH_DOT )
                || filename_.endsWith( RRC_FILE_EXTENSION_WITH_DOT );
    }

    /**
     * Whether is a java file by extension
     *
     * @param filename
     * @return
     */
    public static boolean hasJavaExtension( String filename ) {
        return filename.toLowerCase().endsWith( JAVA_EXTENSION_WITH_DOT );
    }

    /**
     * Whether is a rrh file by extension
     *
     * @param filename
     * @return
     */
    public static boolean hasRRHExtension( String filename ) {
        return filename.toLowerCase().endsWith( RRH_FILE_EXTENSION_WITH_DOT );
    }

    /**
     * Whether is a rrc file by extension
     *
     * @param filename
     * @return
     */
    public static boolean hasRRCExtension( String filename ) {
        return filename.toLowerCase().endsWith( RRC_FILE_EXTENSION_WITH_DOT );
    }

    /**
     * A common method to get a relative file path w\ package string for given <code>File</code>.
     * <p>
     * The returned path is in <code>com/rim/test</code> format.
     * </p>
     *
     * @param file
     * @param legacyProject
     * @return package string
     * @throws IOException
     */
    public static String getFilePackageString( File file, Project legacyProject ) throws CoreException {
        String packageString;
        String fileName = file.getName();

        if( fileName.endsWith( JAVA_EXTENSION_WITH_DOT ) ) {
            packageString = getJavaFilePackageString( file );
        } else if( fileName.endsWith( RRH_FILE_EXTENSION_WITH_DOT ) ) {
            packageString = getRRHPackageString( file );
        } else if( fileName.endsWith( RRC_FILE_EXTENSION_WITH_DOT ) ) {
            packageString = getRRCPackageString( file );
        } else {
            packageString = buildGenaralFilePackageString( file, legacyProject );
        }
        return packageString;
    }

    /**
     * This method will convert the package name (com.rim.test) into package id (exclusion pattern) format (com/rim/test)
     *
     * @param packageID
     *            String
     * @return result String
     */
    public static String convertPkgIDToString( String packageID ) {
        String result = packageID;
        if( result == null ) {
            return null;
        }
        result = packageID.replace( DOT_CHAR, File.separatorChar );
        return result;// my/net/rim
    }

    /**
     * This method will convert the package String (com/rim/test) into package id format (com.rim.test)
     *
     * @param packageString
     *            String
     * @return result String
     */
    public static String convertPkgStringToID( String packageString ) {
        StringBuffer buf = new StringBuffer();
        if( packageString == null ) {
            return null;
        }
        IPath path = new Path( packageString );
        for( int i = 0; i < path.segmentCount(); i++ ) {
            if( i == 0 ) {
                buf.append( path.segment( i ) );
            } else {
                buf.append( DOT_MARK + path.segment( i ) );
            }

        }
        return buf.toString(); // com.rim.test
    }

    /**
     * Compose the crb file name based on the given <code>rrcFileNem</code> and <code>packageID</code>. The given
     * <code>rrcFileName</code> should not contain the file extension (.rrc).
     *
     * <p>
     * If the given <code>rrcFile</code> is resource_en_US and the given <code>packageID</code> is "net.rim.test", the returned
     * crb file name is <code>net.rim.test.resource£en_US.crb</code>.
     * </p>
     *
     * @param rrcFileName
     * @return
     */
    public static String getCRBFileName( String rrcFileName, String packageID, boolean alt ) {
        int index = rrcFileName.indexOf( "_" ); //$NON-NLS-1$
        String crbFileName;
        if( index > 0 ) {
            String fileNameWithoutLocal = rrcFileName.substring( 0, index );
            // get the locale string, e.g. en_us
            String localeString = rrcFileName.substring( index + 1 );
            localeString = ConvertUtil.localeValueOf( localeString ).toString().trim();
            // resource_en_us --> resource£en_us
            if( alt ) {
                localeString = convertAltLocale( localeString );
            }
            crbFileName = fileNameWithoutLocal + String.valueOf( ConvertUtil.LOCALE_SEPARATOR ) + localeString;
        } else {
            crbFileName = rrcFileName + String.valueOf( ConvertUtil.LOCALE_SEPARATOR );
        }
        // append .crb extension
        crbFileName += CRB_EXTENSION_WITH_DOT;
        // add the package id
        if( !StringUtils.isBlank( packageID ) ) {
            crbFileName = packageID + DOT_CHAR + crbFileName;
        }
        return crbFileName;
    }

    private static String convertAltLocale( String localeString ) {
        String altLocale = localeString;
        if( localeString.startsWith( "iw" ) ) {
            altLocale = "he" + localeString.substring( 2 );
        } else if( localeString.startsWith( "in" ) ) {
            altLocale = "id" + localeString.substring( 2 );
        }
        return altLocale;
    }

    public static void createFolder( IFolder folder ) throws CoreException {
        if( !folder.exists() ) {
            IContainer parent = folder.getParent();
            if( parent instanceof IFolder ) {
                createFolder( (IFolder) parent );
            }
            folder.create( true, true, new NullProgressMonitor() );
        }
    }

    static public String parseFileForPackageId( File file ) {
        if( ( null == file ) || file.isDirectory() || !file.exists() ) {
            String msg = NLS.bind( Messages.PackageUtils_UNDEFINED_FILE_ERROR_MSG, IConstants.EMPTY_STRING );
            _logger.error( msg );
            throw new RuntimeException( msg );
        }

        String packageId = null, filePath = file.getAbsolutePath(), line, token;

        BufferedReader bufferedReader = null;
        StringTokenizer tokenizer;

        try {
            if( PackageUtils.hasRRHExtension( filePath ) || PackageUtils.hasRRCExtension( filePath )
                    || PackageUtils.hasJavaExtension( filePath ) ) {

                bufferedReader = new BufferedReader( new FileReader( file ) );

                if( !bufferedReader.ready() ) {
                    throw new RuntimeException( NLS.bind( Messages.PackageUtils_EMPTY_FILE_ERROR_MSG, file.getPath() ) );
                }

                while( null != ( line = bufferedReader.readLine() ) ) {
                    // pull lines from file till find the one containing
                    // package definition
                    line = line.trim();

                    if( ( 0 < line.length() ) && // criterion to recognize a
                            // package declaration
                            line.startsWith( "package" ) ) {
                        while( !line.contains( IConstants.SEMICOLON_MARK ) ) {
                            line = line + bufferedReader.readLine();
                        }

                        tokenizer = new StringTokenizer( line );

                        while( tokenizer.hasMoreTokens() ) {
                            // pull the package tokens
                            token = tokenizer.nextToken();

                            if( !"package".equals( token ) ) {
                                if( token.contains( IConstants.SEMICOLON_MARK ) ) {
                                    token = token.replaceAll( IConstants.SEMICOLON_MARK, IConstants.EMPTY_STRING );
                                }

                                packageId = StringUtils.isNotBlank( token ) ? token : IConstants.EMPTY_STRING;

                                break;
                            }// end pulling of eventual package tokens
                        }
                    }

                    if( null != packageId ) {
                        // file
                        break;
                    }
                }
            } else {
                throw new RuntimeException( NLS.bind( Messages.PackageUtils_UNSUPPORTED_FILE_ERROR_MSG, file.getPath() ) );
            }
        } catch( Throwable t ) {
            _logger.debug( t.getMessage(), t );
        } finally {
            if( null != bufferedReader ) {
                try {
                    bufferedReader.close();
                } catch( IOException e ) {
                    _logger.error( e.getMessage(), e );
                }
            }
        }

        return packageId;
    }
}
