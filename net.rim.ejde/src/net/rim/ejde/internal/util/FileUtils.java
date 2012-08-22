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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.rim.ejde.internal.core.IConstants;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * A utility class to manipulate files.
 *
 * @author dmeng
 */
public class FileUtils {
    private static final Logger log = Logger.getLogger( FileUtils.class );

    /**
     * Copy the source file to the destination file. An <code>IllegalArgumentException</code> will be thrown if the destination
     * file exists.
     *
     * @param src
     *            The source file
     * @param dest
     *            The destination file
     * @throws IOException
     *             Any error during file copy
     */
    public static void copy( File src, File dest ) throws IOException {
        if( ( src == null ) || ( dest == null ) ) {
            log.error( "Source and Destination files cannot be null" ); //$NON-NLS-1$
            throw new IllegalArgumentException( "Source and Destination files cannot be null" ); //$NON-NLS-1$
        }
        if( !src.exists() || !src.canRead() || !src.isFile() ) {
            log.error( "Could not access or read file " + src ); //$NON-NLS-1$
            throw new IllegalArgumentException( "Could not access or read file " + src ); //$NON-NLS-1$
        }
        if( dest.exists() ) {
            log.error( "Destination file already exists: " + dest ); //$NON-NLS-1$
            throw new IllegalArgumentException( "Destination file already exists: " + dest ); //$NON-NLS-1$
        }

        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        FileChannel inputChannel = null, outputChannel = null;

        try {
            inputStream = new FileInputStream( src );
            outputStream = new FileOutputStream( dest );

            inputChannel = inputStream.getChannel();
            outputChannel = outputStream.getChannel();

            long count = 0, size = inputChannel.size();

            while( ( count += outputChannel.transferFrom( inputChannel, count, size - count ) ) < size )
                ;

        } catch( Throwable e ) {
            log.error( e.getMessage(), e );
        } finally {// just in case the Eclipse API doesn't close it for whatever
            // reason.
            try {
                if( null != inputStream )
                    inputStream.close();

                if( outputStream != null ) {
                    outputStream.close();
                }
            } catch( Throwable e ) {
                ;
                log.error( e.getMessage(), e );
            }
        }
    }

    /**
     * Copy the source file to the destination file. An <code>IllegalArgumentException</code> will be thrown if the destination
     * file exists.
     *
     * @param inputStream
     *            the input stream that acts as a source
     * @param dest
     *            The destination file
     *
     * @throws IOException
     *             Any error during file copy
     */
    public static void copy( InputStream inputStream, File dest ) throws IOException {
        if( ( inputStream == null ) || ( dest == null ) ) {
            log.error( "Source and Destination cannot be null" ); //$NON-NLS-1$
            throw new IllegalArgumentException( "Source and Destination cannot be null" ); //$NON-NLS-1$
        }
        if( dest.exists() ) {
            log.error( "Destination file already exists: " + dest ); //$NON-NLS-1$
            throw new IllegalArgumentException( "Destination file already exists: " + dest ); //$NON-NLS-1$
        }

        FileOutputStream outputStream = null;
        ReadableByteChannel inputChannel = null;
        FileChannel outputChannel = null;

        try {
            outputStream = new FileOutputStream( dest );

            inputChannel = Channels.newChannel( inputStream );
            outputChannel = outputStream.getChannel();

            long count = 0, size = Long.MAX_VALUE;

            outputChannel.transferFrom( inputChannel, count, size );

        } catch( Throwable e ) {
            log.error( e.getMessage(), e );
        } finally {// just in case the Eclipse API doesn't close it for whatever
            // reason.
            try {
                inputStream.close();

                if( outputStream != null ) {
                    outputStream.close();
                }
            } catch( Throwable e ) {
                log.error( e.getMessage(), e );
            }
        }
    }

    /**
     * Copy the source file to the destination file. Overwrite it if the destination file exists.
     *
     * @param src
     *            The source file
     * @param dest
     *            The destination file
     * @throws IOException
     *             Any error during file copy
     */
    public static void copyOverwrite( File src, File dest ) throws IOException {
        if( ( src == null ) || ( dest == null ) ) {
            log.error( "Source and Destination files cannot be null" ); //$NON-NLS-1$
            throw new IllegalArgumentException( "Source and Destination files cannot be null" ); //$NON-NLS-1$
        }

        if( !src.exists() || !src.canRead() || !src.isFile() ) {
            log.error( "Could not access or read file " + src ); //$NON-NLS-1$
            throw new IllegalArgumentException( "Could not access or read file " + src ); //$NON-NLS-1$
        }

        // delete the destination file if it exists.
        if( dest.exists() ) {
            if( !dest.delete() ) {
                log.warn( "Could not replace file " + dest ); //$NON-NLS-1$
                return;
            }
        }

        copy( src, dest );
    }

    public static IStatus canChange( final java.io.File osFile ) {
        return canChange( osFile, "Change Resource Problem", "Resource is read-only and cannot be changed: ", true,
                "\n\nDo you want to make the resource writable?" );
    }

    public static IStatus canChange( final java.io.File osFile, final String dialogTitle, final String errorMessage,
            final boolean showResourcePath, final String questionMessage ) {
        IStatus result = Status.CANCEL_STATUS;

        if( ( osFile != null ) && ( osFile.exists() ) ) {
            if( osFile.canWrite() ) {
                result = Status.OK_STATUS;
            } else {
                // prompt for making resource writable
                Display.getDefault().syncExec( new Runnable() {
                    public void run() {
                        StringBuffer buffer = new StringBuffer();
                        buffer.append( errorMessage );
                        if( showResourcePath ) {
                            buffer.append( osFile.getAbsolutePath() );
                        }
                        buffer.append( questionMessage );
                        String[] buttons = new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL };
                        Shell shell = new Shell();
                        MessageDialog promptSaveDia = new MessageDialog( shell, dialogTitle, null, buffer.toString(),
                                MessageDialog.WARNING, buttons, 0 );
                        int ret = promptSaveDia.open();
                        shell.dispose();
                        if( ret == Window.OK ) {
                            // make this resource writable
                            setWritable( osFile );
                        }
                    }
                } );

                if( osFile.canWrite() ) {
                    result = Status.OK_STATUS;
                }
            }
        }

        return result;

    }

    public static void setWritable( java.io.File osFile ) {
        // Since there is no method in Java to set writable directly, we have to
        // use following approach.
        java.io.File srcFile = osFile;
        java.io.File destFile = new java.io.File( srcFile.getParentFile(), String.valueOf( System.currentTimeMillis() ) );

        FileInputStream in = null;
        FileOutputStream out = null;
        FileChannel inChannel = null, outChannel = null;

        // create dest file
        try {
            destFile.createNewFile();

            // copy content from source file to dest file
            in = new FileInputStream( srcFile );
            out = new FileOutputStream( destFile );

            inChannel = in.getChannel();
            outChannel = out.getChannel();

            inChannel.transferTo( 0, inChannel.size(), outChannel );
        } catch( Throwable e ) {
            log.error( e.getMessage(), e );
        } finally {
            try {
                if( null != inChannel )
                    inChannel.close();

                if( null != outChannel )
                    outChannel.close();
            } catch( Throwable e ) {
                log.error( e.getMessage(), e );
            }
        }

        // delete source file
        boolean success = srcFile.delete();

        if( !success ) {
            destFile.delete();
            throw new RuntimeException( "source file can not be deleted:" + srcFile.getAbsolutePath() );
        }

        // rename dest file to source file
        destFile.renameTo( srcFile );
    }

    /**
     * Add the given <code>file</code> to the given </code>container</code>.
     *
     * @param container
     * @param file
     * @param createLink
     * @return
     */
    public static IFile addResourceToProject( IContainer container, File file, Boolean createLink ) {
        // TODO:Check if has package
        IProject iProject = container.getProject();
        IFile iFile = null;

        iFile = container.getFile( new Path( file.getName() ) );
        if( iFile != null && !iFile.exists() ) {
        	if( createLink ) {
        		try {
        			iFile.createLink( new Path( file.getAbsolutePath() ), IResource.NONE, new NullProgressMonitor() );
        		} catch( Exception e ) {
        			log.error( "Error linking resource to project" );
        		}
        	} else {
        		ImportUtils.copyFile( iProject, file, iFile.getProjectRelativePath() );
        	}
        }
        return iFile;
    }

    /**
     * Returns where the resources (non-package) should be created
     *
     * @param iProject
     * @return
     */
    public static IFolder getResFolder( IProject iProject ) {
        IFolder sourceFolder = null;

        String resFolderName = ImportUtils.getProjectResFolderName();
        if( resFolderName.length() > 0 ) {
            sourceFolder = iProject.getFolder( new Path( resFolderName ) );
        }

        if( sourceFolder == null || !sourceFolder.exists() ) {
            // Get the 1st found source folder
            sourceFolder = get1stNonDerivedSourceFolder( iProject );
        }
        return sourceFolder;
    }

    public static IFolder get1stNonDerivedSourceFolder( IProject iProject ) {
        IFolder sfolder = null;
        IPackageFragmentRoot roots[] = ProjectUtils.getProjectSourceFolders( iProject );
        if( roots != null && roots.length > 0 ) {
            Collections.sort( Arrays.asList( roots ), new Comparator< IPackageFragmentRoot >() {
                @Override
                public int compare( IPackageFragmentRoot root1, IPackageFragmentRoot root2 ) {
                    return root1.getElementName().compareTo( root2.getElementName() );
                }
            } );
            for( int i = 0; i < roots.length; i++ ) {
                sfolder = iProject.getFolder( roots[ i ].getResource().getProjectRelativePath() );
                if( !sfolder.isDerived() ) {
                    break;
                }
            }
        }
        return sfolder;
    }

    public static void deleteAll( File[] files ) {
        if( files == null ) {
            throw new IllegalArgumentException( "files cannot be null" ); //$NON-NLS-1$
        }
        for( File file : files ) {
            if( !file.exists() || !file.canWrite() ) {
                throw new IllegalArgumentException( "Could not access or write file " + file ); //$NON-NLS-1$
            }
            if( file.isDirectory() ) {
                deleteDir( file );
            } else {
                file.delete();
            }
        }
    }

    /**
     * Reads the content of a file and returns an ArrayList with the data
     *
     * @param f
     *            File that should be read
     * @return ArrayList of data
     */
    public static List< String > readFile( File f ) {
        List< String > data = new ArrayList< String >();

        try {
            BufferedReader br = new BufferedReader( new FileReader( f ) );
            while( br.ready() ) {
                data.add( br.readLine() );
            }
            return data;
        } catch( FileNotFoundException e ) {
            return null;
        } catch( IOException e ) {
            return null;
        }
    }

    /**
     * Returns the file extension of the given file.
     *
     * @param f
     * @return File extension
     */
    public static String getFileExtension( File f ) {
        String fileName = f.getName();
        int index = fileName.lastIndexOf( IConstants.DOT_MARK );

        if( index > 0 && index < fileName.length() ) {
            String fileExtension = fileName.substring( index + 1 );
            return fileExtension;
        }
        return IConstants.EMPTY_STRING;
    }

    private static void deleteDir( File file ) {
        if( file == null ) {
            throw new IllegalArgumentException( "file cannot be null" ); //$NON-NLS-1$
        }
        if( !file.exists() || !file.canWrite() ) {
            throw new IllegalArgumentException( "Could not access or write file " + file ); //$NON-NLS-1$
        }
        for( File subFile : file.listFiles() ) {
            if( !subFile.exists() || !subFile.canWrite() ) {
                throw new IllegalArgumentException( "Could not access or write file " + subFile ); //$NON-NLS-1$
            }
            if( subFile.isDirectory() ) {
                deleteDir( subFile );
            } else {
                subFile.delete();
            }
        }
        file.delete();
    }
}
