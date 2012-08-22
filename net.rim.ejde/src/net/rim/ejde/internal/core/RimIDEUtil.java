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
package net.rim.ejde.internal.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import net.rim.ejde.internal.util.Messages;
import net.rim.ide.core.IDEError;
import net.rim.ide.core.ObjectsContentsHelper;
import net.rim.ide.core.VarContentsHelper.Line;

import org.apache.log4j.Logger;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * This class defines some static utility methods.
 *
 */
public class RimIDEUtil implements IConstants {
    private static final Logger log = Logger.getLogger( RimIDEUtil.class );

    /**
     * Creates the extension filter string, e.g. "*.java".
     *
     * @param ext
     *            Extension string.
     * @return Extension filter string.
     */
    public static String extensionFilterString( String ext ) {
        if( ( ext == null ) || ( ext.length() == 0 ) ) {
            return "*"; //$NON-NLS-1$
        }
        if( ext.contains( ";" ) ) {
            return "*" + ext.replaceAll( ";", ";*" ); // adds * after ;
        }
        // delimeter
        if( ext.charAt( 0 ) == '.' ) {
            return "*" + ext; //$NON-NLS-1$
        }
        return "*." + ext; //$NON-NLS-1$
    }

    /**
     * Creates a physical file for <code>file<code> on the disk.
     *
     * @param file
     *            A File instance.
     * @param shell
     *            A Shell instance on which message dialogs will be displayed.
     * @return <code>true</code> the file is created correctly, <code>false</code> otherwise.
     */
    public static boolean createFile( File file, Shell shell ) {
        File parent = file.getParentFile();
        if( !parent.exists() ) {
            MessageBox messageBox = new MessageBox( shell, SWT.YES | SWT.NO );
            messageBox.setMessage( "Create the folder " + parent.getPath() //$NON-NLS-1$
                    + " ?" ); //$NON-NLS-1$
            if( messageBox.open() == SWT.YES ) {
                parent.mkdirs();
            }
        }
        if( file.exists() ) {
            MessageBox messageBox = new MessageBox( shell, SWT.YES | SWT.NO );
            messageBox.setMessage( "Overwrite the file " + file.getPath() + " ?" ); //$NON-NLS-1$ //$NON-NLS-2$
            if( messageBox.open() == SWT.YES ) {
                file.delete();
            }
        }
        try {
            return file.createNewFile();
        } catch( IOException ioe ) {
            ioe.printStackTrace();
            return false;
        }
    }

    /**
     * Opens a *.csv file for saving.
     *
     * @param shell
     * @return A *.csv file.
     */
    public static File openCSVFileForSave( Shell shell ) {
        return RimIDEUtil.openFileForSave( shell, new String[] { RimIDEUtil.extensionFilterString( CSV_EXTENSION ) },
                CSV_EXTENSION );
    }

    /**
     * Opens a file for saving.
     *
     * @param shell
     *            the shell
     * @param extensions
     *            the extensions
     * @param forcedExtension
     *            the forced extension
     *
     * @return A file.
     */
    public static File openFileForSave( Shell shell, String[] extensions, String forcedExtension ) {
        return RimIDEUtil.openFileForSave( shell, null, extensions, forcedExtension );
    }

    /**
     * Opens a file for saving.
     *
     * @param shell
     *            the shell
     * @param file
     *            the file
     * @param extensions
     *            the extensions
     *
     * @return A file.
     */
    public static File openFileForSave( Shell shell, String file, String[] extensions ) {
        return RimIDEUtil.openFile( shell, Messages.RimIDEUtil_SAVE_FILE_DIALOG_TITLE, file, SWT.SAVE, extensions, null );
    }

    /**
     * Opens a file for saving.
     *
     * @param shell
     *            the shell
     * @param file
     *            the file
     * @param extensions
     *            the extensions
     * @param forcedExtension
     *            the forced extension
     *
     * @return A file.
     */
    public static File openFileForSave( Shell shell, String file, String[] extensions, String forcedExtension ) {
        return RimIDEUtil.openFile( shell, Messages.RimIDEUtil_SAVE_FILE_DIALOG_TITLE, file, SWT.SAVE, extensions,
                forcedExtension );
    }

    /**
     * Open a file through file dialog.
     *
     * @param shell
     * @param dialogTitle
     * @param file
     * @param purpose
     * @param extensions
     * @return
     */
    public static File openFile( Shell shell, String dialogTitle, String file, int purpose, String[] extensions ) {
        return openFile( shell, dialogTitle, file, purpose, extensions, IConstants.EMPTY_STRING );
    }

    /**
     * Open a file through file dialog.
     *
     * @param shell
     * @param dialogTitle
     * @param file
     * @param purpose
     * @param extensions
     * @return
     */
    public static File openFile( Shell shell, String dialogTitle, String file, int purpose, String[] extensions,
            String forcedExtension ) {
        // create a file open dialog
        FileDialog fileDialog = new FileDialog( shell, purpose | SWT.SINGLE );

        // set extension filter
        for( int i = 0; i < extensions.length; i++ ) {
            if( !extensions[ i ].contains( "*." ) ) {
                extensions[ i ] = "*." + extensions[ i ];
            }
        }
        if( extensions.length != 0 ) {
            fileDialog.setFilterExtensions( extensions );
        }

        // set default file name
        if( ( file != null ) && !file.trim().equals( EMPTY_STRING ) ) {
            fileDialog.setFileName( file );
        }

        // set the title of the dialog
        fileDialog.setText( dialogTitle );

        // open the dialog
        fileDialog.open();

        // get selected file name
        String fileName = fileDialog.getFileName();

        if( ( fileName == null ) || fileName.equals( "" ) ) {
            return null;
        }

        // get the parent path of selected file
        IPath fullPath = new Path( fileDialog.getFilterPath() + File.separator );

        // add the filename
        fullPath = fullPath.append( fileName );

        // add extension
        String currentExtension = fullPath.getFileExtension();
        if( currentExtension == null || currentExtension.equals( IConstants.EMPTY_STRING ) ) {
            if( forcedExtension != null && !forcedExtension.equals( IConstants.EMPTY_STRING ) ) {
                fullPath = fullPath.addFileExtension( forcedExtension );
            }
        }

        // create a File instance for the selected file
        File finalFile = new File( fullPath.toOSString() );
        if( ( purpose == SWT.SAVE ) && !RimIDEUtil.createFile( finalFile, shell ) ) {
            return null;
        }
        return finalFile;
    }

    /**
     * Opens a directory dialog to choose a directory.
     *
     * @param shell
     * @param title
     *
     * @return A File which represents a directory.
     */
    public static File openDirecotryDialog( Shell shell, String title ) {
        // create a file open dialog
        DirectoryDialog directoryDialog = new DirectoryDialog( shell, SWT.SINGLE );
        //
        directoryDialog.setText( title );
        // open the dialog
        String chosenDir = directoryDialog.open();
        if( chosenDir == null ) {
            return null;
        }
        return new File( chosenDir );
    }

    /**
     * Writes out the data of <code>table</code> to <code>file</code>.
     *
     * @param file
     *            Destination file.
     * @param table
     *            Table which will be written out to <code>file</code>.
     *
     * @throws IDEError
     */
    public static void saveTableToFile( File file, Table table ) {
        if( ( file == null ) || !file.exists() ) {
            return;
        }
        if( table == null ) {
            return;
        }
        PrintStream out = null;
        try {
            out = new PrintStream( new FileOutputStream( file ) );
            int cols = table.getColumnCount();
            int rows = table.getItemCount();
            // Print columnTitles
            for( int i = 0; i < cols; i++ ) {
                out.print( table.getColumn( i ).getText() );
                if( i < ( cols - 1 ) ) {
                    out.print( COMMA_MARK );
                }
            }
            out.println();
            // Get all lines and print data
            for( int i = 0; i < rows; i++ ) {
                TableItem item = table.getItem( i );
                for( int j = 0; j < cols; j++ ) {
                    out.print( item.getText( j ) );
                    if( j < ( cols - 1 ) ) {
                        out.print( COMMA_MARK );
                    }
                }
                out.println();
            }
        } catch( IOException fnf ) {
            fnf.printStackTrace();
        } finally {
            if( out != null ) {
                out.close();
            }
        }
    }

    /**
     * Writes out the data of <code>table</code> to <code>file</code> via the ObjectsContentsHelper. Necessary for tables that do
     * not set the text of the TableItem but circumvent this via the labelProvider.
     *
     * @param file
     *            Destination file.
     * @param table
     *            Table which will be written out to <code>file</code>.
     * @param helper
     *            the ObjectsContentsHelper to grab the data required from the TableItem
     *
     * @throws IDEError
     */
    public static void saveTableToFile( File file, Table table, ObjectsContentsHelper helper ) {
        if( ( file == null ) || !file.exists() ) {
            return;
        }
        if( table == null ) {
            return;
        }
        PrintStream out = null;
        try {
            out = new PrintStream( new FileOutputStream( file ) );
            int cols = table.getColumnCount();
            int rows = table.getItemCount();
            // Print columnTitles
            for( int i = 0; i < cols; i++ ) {
                out.print( table.getColumn( i ).getText() );
                if( i < ( cols - 1 ) ) {
                    out.print( COMMA_MARK );
                }
            }
            out.println();
            // Get all lines and print data
            for( int i = 0; i < rows; i++ ) {
                TableItem item = table.getItem( i );
                Line line;
                if( item.getData() != null ) {
                    line = (Line) item.getData();
                } else {
                    line = helper.getLine( i );
                }
                if( line == null ) {
                    continue;
                }

                for( int j = 0; j < cols; j++ ) {
                    // Fix for IDT 390741 - Replaced comma in string with
                    // semicolon
                    out.print( ( helper.getValue( line, j ) ).toString().replace( IConstants.COMMA_MARK,
                            IConstants.SEMICOLON_MARK ) );
                    if( j < ( cols - 1 ) ) {
                        out.print( COMMA_MARK );
                    }
                }
                out.println();
            }
        } catch( IOException fnf ) {
            fnf.printStackTrace();
        } finally {
            if( out != null ) {
                out.close();
            }
        }
    }

    public static String parseResourceChangeEventType( int type ) {
        switch( type ) {
            case IResourceChangeEvent.POST_BUILD:
                return "post_build"; //$NON-NLS-1$
            case IResourceChangeEvent.POST_CHANGE:
                return "post_change"; //$NON-NLS-1$
            case IResourceChangeEvent.PRE_BUILD:
                return "pre_build"; //$NON-NLS-1$
            case IResourceChangeEvent.PRE_CLOSE:
                return "pre_close"; //$NON-NLS-1$
            case IResourceChangeEvent.PRE_DELETE:
                return "pre_delete"; //$NON-NLS-1$
            default:
                return null;
        }
    }

    public static String parseResourceChangeEventType( IResourceChangeEvent event ) {
        if( event != null ) {
            return RimIDEUtil.parseResourceChangeEventType( event.getType() );
        }
        return null;
    }

    public static String parseResourceDeltaKind( int kind ) {
        switch( kind ) {
            case IResourceDelta.ADDED:
                return "added"; //$NON-NLS-1$
            case IResourceDelta.ADDED_PHANTOM:
                return "added_phantom"; //$NON-NLS-1$
            case IResourceDelta.ALL_WITH_PHANTOMS:
                return "all_with_phantoms"; //$NON-NLS-1$
            case IResourceDelta.CHANGED:
                return "changed"; //$NON-NLS-1$
            case IResourceDelta.CONTENT:
                return "content"; //$NON-NLS-1$
            case IResourceDelta.COPIED_FROM:
                return "copied_from"; //$NON-NLS-1$
            case IResourceDelta.DESCRIPTION:
                return "description"; //$NON-NLS-1$
            case IResourceDelta.ENCODING:
                return "encoding"; //$NON-NLS-1$
            case IResourceDelta.MARKERS:
                return "markers"; //$NON-NLS-1$
            case IResourceDelta.MOVED_FROM:
                return "moved_from"; //$NON-NLS-1$
            case IResourceDelta.MOVED_TO:
                return "moved_to"; //$NON-NLS-1$
            case IResourceDelta.NO_CHANGE:
                return "no_change"; //$NON-NLS-1$
            case IResourceDelta.OPEN:
                return "open"; //$NON-NLS-1$
            case IResourceDelta.REMOVED:
                return "removed"; //$NON-NLS-1$
            case IResourceDelta.REMOVED_PHANTOM:
                return "removed_phantom"; //$NON-NLS-1$
            case IResourceDelta.REPLACED:
                return "replaced"; //$NON-NLS-1$
            case IResourceDelta.SYNC:
                return "sync"; //$NON-NLS-1$
            case IResourceDelta.TYPE:
                return "type"; //$NON-NLS-1$
            default:
                return null;
        }
    }

    public static String parseResourceType( int type ) {
        switch( type ) {
            case IResource.FILE:
                return "IResource.FILE"; //$NON-NLS-1$
            case IResource.FOLDER:
                return "IResource.FOLDER"; //$NON-NLS-1$
            case IResource.PROJECT:
                return "IResource.PROJECT"; //$NON-NLS-1$
            case IResource.ROOT:
                return "IResource.ROOT"; //$NON-NLS-1$
            default:
                return null;
        }
    }

    public static String parseResourceType( IResource resource ) {
        if( resource != null ) {
            return RimIDEUtil.parseResourceType( resource.getType() );
        }
        return null;
    }

    /**
     * Generates regular expression from <code>string</code>
     *
     * @param string
     * @return regular expression string
     */
    public static String generateRegex( String string ) {
        StringBuffer buffer = new StringBuffer();
        for( int i = 0; i < string.length(); i++ ) {
            char singleChar = string.charAt( i );
            if( singleChar == START_CHAR ) {
                // convert '*' to ".*"
                buffer.append( WILD_CAST_STRING );
            } else if( singleChar == DOT_CHAR ) {
                // convert '.' to "\."
                buffer.append( DOT_QUOTE_STRING );
            } else {
                buffer.append( singleChar );
            }
        }
        return buffer.toString();
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
                        try {
                            IContainer sourceContainer = (IContainer) resource;
                            File sourceContainerFile = EFS.getStore( resource.getLocationURI() ).toLocalFile( EFS.NONE, null );
                            IPath sourceFolderPath = new Path( sourceContainerFile.getAbsolutePath() );

                            // See if the file path is within this source folder
                            // path
                            if( sourceFolderPath.isPrefixOf( filePath ) ) {
                                int segmentCount = sourceFolderPath.segmentCount();
                                IPath relativePath = filePath.removeFirstSegments( segmentCount );
                                input = sourceContainer.getFile( relativePath );

                                break;
                            }
                        } catch( CoreException e ) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
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
     * Open the source file represented by <code>fileStore</code> in an editor and highlight the line whose index is
     * <code>lineNumber</code>.
     *
     * @param fileStore
     * @param lineNumber
     */
    static public void openSourceFile( IFileStore fileStore, int lineNumber ) {
        try {

            IWorkbenchWindow workbenchWindow = ContextManager.getActiveWorkbenchWindow();
            IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();

            // open the source file in an editor
            IEditorPart editorPart = IDE.openEditorOnFileStore( workbenchPage, fileStore );

            if( editorPart instanceof ITextEditor ) {
                ITextEditor textEditor = (ITextEditor) editorPart;
                IDocument document = textEditor.getDocumentProvider().getDocument( textEditor.getEditorInput() );
                // get IRegion instance of the line
                IRegion region = document.getLineInformation( lineNumber - 1 );
                // highlight line
                textEditor.setHighlightRange( region.getOffset(), region.getLength(), true );
            }
        } catch( PartInitException e ) {
            log.error( e.getMessage() );
        } catch( Exception exception ) {
            log.error( exception.getMessage() );
        }
    }

    /**
     * Converts an array to a list. It will return <code>null</code> if the <code>objs</code> is null.
     *
     * @param objs
     * @return
     */
    static public < T > List< T > convertToList( T[] objs ) {
        if( objs == null ) {
            return null;
        }
        List< T > list = new ArrayList< T >();
        for( T obj : objs ) {
            list.add( obj );
        }
        return list;
    }

    /**
     * Replace the "@" with "0x" in the given <code>string</code>.
     *
     * @param string
     * @return
     */
    static public String convertAddressMark( String string ) {
        String convertedString = string.trim();
        int index = convertedString.indexOf( "@" ); //$NON-NLS-1$
        if( index == 0 ) {
            convertedString = "0x" + convertedString.substring( index + 1 ); //$NON-NLS-1$
        } else if( index > 0 ) {
            convertedString = convertedString.substring( 0, index ) + "0x" + convertedString.substring( index + 1 ); //$NON-NLS-1$
        }
        return convertedString;
    }
}
