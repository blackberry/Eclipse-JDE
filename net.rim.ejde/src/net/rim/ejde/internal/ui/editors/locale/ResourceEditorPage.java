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
package net.rim.ejde.internal.ui.editors.locale;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.rim.ejde.internal.ui.consoles.ConsoleUtils;
import net.rim.ejde.internal.ui.consoles.PackagingConsole;
import net.rim.ejde.internal.ui.consoles.PackagingConsoleFactory;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.sdk.resourceutil.RIMResourceElement;
import net.rim.sdk.resourceutil.RIMResourceLocale;
import net.rim.sdk.resourceutil.ResourceConstants;
import net.rim.sdk.resourceutil.ResourceElement;
import net.rim.sdk.resourceutil.ResourceLocale;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * A page used in the resource editor.
 *
 * @author jkeshavarzi
 *
 */
public class ResourceEditorPage {
    static final int KEY_COLUMN_INDEX = 0;
    static final int VALUE_COLUMN_INDEX = 1;
    static final String KEY_COLUMN_ID = "Key";
    static final String VALUE_COLUMN_ID = "Value";

    private static final int NUM_COLUMNS = 2;

    private ResourceCellEditorUIFactory _uiFactory;
    private Composite _composite;
    private IInputValidator _newKeyValidator;
    private TableViewer _viewer;

    private static ResourceContentProvider _contentProvider = new ResourceContentProvider();
    private static ResourceLabelProvider _labelProvider = new ResourceLabelProvider();
    private static String[] _columnProperties;
    private static Vector< Comparator< ResourceElement >> _comparators = new Vector< Comparator< ResourceElement >>( NUM_COLUMNS );

    // underlying .rrc file associated with then locale represented by this ResourceEditorPage (if applicable)
    private File _rrcFile = null;

    // ResourceLocale object associated with the locale represented by this ResourceEditorPage (if applicable)
    private ResourceLocale _locale;

    // stores reference to "Validate" button (used for translator mode in ResourceEditorOptionsDialog)
    private Button _validateButton = null;

    // stores reference to "Mark all correct" button (used for translator mode in ResourceEditorOptionsDialog)
    private Button _markAllCorrectButton = null;

    // used by "Validate" button
    private static Hashtable _resourceKeysInJavaFilesTable = new Hashtable();

    static {
        _columnProperties = new String[ NUM_COLUMNS ];
        _columnProperties[ KEY_COLUMN_INDEX ] = KEY_COLUMN_ID;
        _columnProperties[ VALUE_COLUMN_INDEX ] = VALUE_COLUMN_ID;

        _comparators.add( KEY_COLUMN_INDEX, new Comparator< ResourceElement >() {
            public int compare( ResourceElement e1, ResourceElement e2 ) {
                return e1.getKey().compareToIgnoreCase( e2.getKey() );
            }
        } );

        _comparators.add( VALUE_COLUMN_INDEX, new Comparator< ResourceElement >() {
            public int compare( ResourceElement e1, ResourceElement e2 ) {
                String value1 = e1.getValueAsString();
                String value2 = e2.getValueAsString();
                if( value1.length() == 0 && value2.length() != 0 ) {
                    return 1;
                }
                if( value1.length() != 0 && value2.length() == 0 ) {
                    return -1;
                }
                return value1.compareToIgnoreCase( value2 );
            }
        } );
    }

    public ResourceEditorPage( Composite container, ResourceLocale locale ) {
        _composite = createComposite( container );
        Button addButton = createAddButton( locale );
        _viewer = createTableViewer( addButton, locale );
        _uiFactory = new ResourceCellEditorUIFactory( _viewer.getTable() );
        _newKeyValidator = new ResourceKeyValidator( locale.getCollection(), false );

        if( locale instanceof RIMResourceLocale ) {
            _rrcFile = new File( ( (RIMResourceLocale) locale ).getRrcFileAbsolutePath() );
        }
        _locale = locale;

        // Preserve Versioning Highlighting if user sorts resource key column in Resource Editor
        Table table = _viewer.getTable();
        TableColumn keyColumn = table.getColumn( KEY_COLUMN_INDEX );
        keyColumn.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                String originalLocaleString = ( (RIMResourceLocale) _locale ).getCollection().getOriginalLocaleName();
                ResourceEditorOptionsDialog.updateVersioningForTableViewer( null != originalLocaleString, _viewer, _locale );
            }
        } );

        // Preserve Versioning Highlighting if user sorts resource value column in Resource Editor
        TableColumn valueColumn = table.getColumn( VALUE_COLUMN_INDEX );
        valueColumn.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                String originalLocaleString = ( (RIMResourceLocale) _locale ).getCollection().getOriginalLocaleName();
                ResourceEditorOptionsDialog.updateVersioningForTableViewer( null != originalLocaleString, _viewer, _locale );
            }
        } );
    }

    void createContextMenu( IWorkbenchPartSite site ) {
        MenuManager menuManager = new MenuManager( "#PopupMenu" );
        menuManager.setRemoveAllWhenShown( true );
        menuManager.addMenuListener( new IMenuListener() {
            public void menuAboutToShow( IMenuManager m ) {
                ResourceEditorPage.this.fillContextMenu( m );
            }
        } );
        Table table = _viewer.getTable();
        Menu menu = menuManager.createContextMenu( table );
        table.setMenu( menu );
        // commented out to remove non essential default actions in context menu
        // site.registerContextMenu( menuManager, _viewer );
    }

    void fillContextMenu( IMenuManager menuManager ) {
        IStructuredSelection selection = (IStructuredSelection) _viewer.getSelection();
        if( !selection.isEmpty() ) {
            ResourceElement element = (ResourceElement) selection.getFirstElement();

            menuManager.add( new PropertiesAction( _uiFactory, element ) );
            menuManager.add( new Separator() );

            menuManager.add( new ConvertToMultipleValuesAction( element, _composite.getShell() ) );
            menuManager.add( new ConvertToSingleValueAction( element, _composite.getShell() ) );
            menuManager.add( new Separator() );

            menuManager.add( new DeleteValueAction( element, _composite.getShell() ) );
            menuManager.add( new Separator() );

            menuManager.add( new DeleteValueFromAllAction( element, _composite.getShell() ) );
            menuManager.add( new Separator() );

            menuManager.add( new MarkTranslationCorrectAction( element ) );
            menuManager.add( new MarkTranslationIncorrectAction( element ) );
            menuManager.add( new Separator() );
        }
        menuManager.add( new Separator( IWorkbenchActionConstants.MB_ADDITIONS ) );
    }

    Control getControl() {
        return _composite;
    }

    TableViewer getTableViewer() {
        return _viewer;
    }

    void refresh() {
        _viewer.refresh();
    }

    void setFocus() {
        _viewer.getTable().setFocus();
    }

    void update( Object element, String[] properties ) {
        _viewer.update( element, properties );
    }

    private Button createAddButton( final ResourceLocale locale ) {
        FormData data = new FormData();
        data.left = new FormAttachment( 0, 0 );
        data.top = new FormAttachment( 0, 0 );

        Button addButton = new Button( _composite, SWT.PUSH | SWT.CENTER );
        addButton.setText( Messages.AddButton_text );
        addButton.setLayoutData( data );
        addButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                InputDialog dialog = new InputDialog( _composite.getShell(), Messages.AddButton_text,
                        Messages.InputDialog_message, null, _newKeyValidator );
                if( dialog.open() == Window.OK ) {
                    // flag used to determine whether to proceed with adding resource key (i.e. if file is read-only, user will be
                    // prompted to set file writable prior to being able to add key)
                    boolean proceedToAddKey = false;
                    if( locale instanceof RIMResourceLocale ) {
                        // .rrc file currently read-only
                        if( ( (RIMResourceLocale) locale ).isRrcFileWritable() == false ) {
                            if( MessageDialog.openQuestion( null, Messages.MessageDialog_title,
                                    ( (RIMResourceLocale) locale ).getRrcFilename()
                                            + " is read-only.\nDo you want to mark it read-write?" ) ) {
                                // User decides to set file writable, proceed with adding key
                                ( (RIMResourceLocale) locale ).setRrcFileWritable();
                                ( (RIMResourceLocale) locale ).getCollection().setHeaderFileWritable();
                                proceedToAddKey = true;
                            } else {
                                // User decides not to set file writable, Add Key action aborted
                                MessageDialog.openInformation(
                                        null,
                                        Messages.MessageDialog_title,
                                        "You will not be able to save changes to "
                                                + ( (RIMResourceLocale) locale ).getRrcFilename() + "." );
                                proceedToAddKey = false;
                            }
                        } else {
                            // File already writable, proceed with adding key
                            proceedToAddKey = true;
                        }
                    } else {
                        // locale is not an instance of RIMResourceLocale (used for .res files later on)
                        proceedToAddKey = true;
                    }

                    if( proceedToAddKey ) {
                        // Fix for DPI221924
                        String userInput = dialog.getValue().trim();
                        if( !userInput.matches( "[\\w]+" ) ) {
                            MessageDialog
                                    .openError( null, Messages.MessageDialog_title, Messages.MessageDialog_ResourceNameEmpty );
                            return;
                        }

                        locale.getCollection().addKey( userInput );

                        // Refresh Versioning Highlighting after adding key
                        if( locale instanceof RIMResourceLocale ) {
                            String originalLocaleString = ( (RIMResourceLocale) locale ).getCollection().getOriginalLocaleName();
                            ResourceEditorOptionsDialog.updateVersioningForResourceEditor( null != originalLocaleString, locale );
                        }
                    }
                }
            }
        } );

        /**
         * Options Button (activates Resource Editor Options Dialog)
         */
        data = new FormData();
        data.left = new FormAttachment( addButton );
        data.top = new FormAttachment( 0, 0 );
        Button optionsButton = new Button( _composite, SWT.PUSH | SWT.CENTER );
        optionsButton.setText( "Options" );
        optionsButton.setLayoutData( data );
        optionsButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent event ) {
                if( locale instanceof RIMResourceLocale ) {
                    ResourceEditorOptionsDialog optionsDialog = new ResourceEditorOptionsDialog( (RIMResourceLocale) locale );
                    optionsDialog.open();
                }
            }
        } );

        /**
         * Validate button
         */
        data = new FormData();
        data.left = new FormAttachment( optionsButton );
        data.top = new FormAttachment( 0, 0 );
        Button validateButton = new Button( _composite, SWT.PUSH | SWT.CENTER );
        validateButton.setLayoutData( data );
        validateButton.setText( "Validate" );
        validateButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent event ) {
                if( locale instanceof RIMResourceLocale ) {
                    boolean proceed = MessageDialog
                            .openQuestion( null, "Resource Editor",
                                    "This will search for uses of these keys in java files.\nThis may take a few minutes.\nAre you sure?" );
                    if( !proceed ) {
                        return;
                    }

                    // index of project that this ResourceEditorPage belongs to
                    int indexOfCurrentProject = -1;
                    Vector< File > javaFiles = new Vector< File >( 0 ); // vector
                    // of .java files in project
                    Vector< String > keysFound = new Vector< String >( 0 ); // vector
                    // of Strings used to determine which resource keys were found in .java files for project
                    keysFound.clear();

                    // Check if strict mode is enabled (i.e. Match resource bundle when validating resources (strict) has been
                    // activated)
                    boolean isStrictMode = ResourceEditorOptionsDialog.getStrictMode();

                    // Populate javaFiles vector with .java files of current project traversed in for loop
                    IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
                    for( int i = 0; i < projects.length && -1 == indexOfCurrentProject; i++ ) {
                        javaFiles.clear();
                        IFile files[] = ProjectUtils.getProjectFiles( projects[ i ] );
                        for( IFile file : files ) {
                            File currentFile = file.getLocation().toFile();
                            String filePath = currentFile.getAbsolutePath();

                            if( filePath.equals( getRrcFile().getAbsolutePath() ) ) {
                                // currently traversed project corresponds to the project that this ResourceEditorPage belongs to
                                indexOfCurrentProject = i;
                            }
                            if( filePath.endsWith( ".java" ) ) {
                                javaFiles.add( currentFile );
                            }
                        }
                    }

                    // Traverse javaFiles vector and search for resource keys in
                    // .java files
                    RIMResourceElement[] elements = ( (RIMResourceLocale) locale ).getResourceElements();

                    for( int i = 0; i < javaFiles.size(); i++ ) {
                        String currentJavaFilePath = javaFiles.get( i ).getAbsolutePath();

                        if( !_resourceKeysInJavaFilesTable.containsKey( currentJavaFilePath ) ) {
                            // current .java file has not been searched before for instances of resource keys, so we need to
                            // search the file System.out.println("=====" + currentJavaFilePath + " was NEVER searched before.");
                            keysFound = searchAndRetrieveKeysFound( elements, javaFiles.get( i ), keysFound );
                        } else {
                            // current .java file has been searched before for instances of resource keys
                            // System.out.println("=====" + currentJavaFilePath + " WAS searched before");

                            boolean searchCurrentJavaFile = true; // flag
                            // determines whether we will need to search currently traversed .java file
                            long currentJavaFileLastModified = javaFiles.get( i ).lastModified();

                            if( _resourceKeysInJavaFilesTable.get( currentJavaFilePath ).getClass().toString().endsWith( "Long" ) ) {
                                // current .java file did not contain any resource keys in previous search

                                if( !isStrictMode ) {
                                    searchCurrentJavaFile = true;
                                    // Handles case where if no resource keys (in strict mode) are found, there may still be
                                    // resource keys (w/o strict mode) present
                                } else {
                                    Long searchedJavaFileLastModified = (Long) _resourceKeysInJavaFilesTable
                                            .get( currentJavaFilePath );
                                    if( currentJavaFileLastModified == searchedJavaFileLastModified.longValue() ) {
                                        searchCurrentJavaFile = false;
                                    }
                                }
                            } else {
                                Hashtable resourceKeysAndTimestampsTable = (Hashtable) _resourceKeysInJavaFilesTable
                                        .get( currentJavaFilePath );
                                // Check if current .java file has been modified since last search of resource keys

                                for( int j = 0; j < elements.length; j++ ) {
                                    String resourceKey = "";
                                    if( isStrictMode ) {
                                        resourceKey = getResourceBundleName() + "Resource." + elements[ j ].getKey();
                                    } else {
                                        resourceKey = elements[ j ].getKey();
                                    }

                                    if( resourceKeysAndTimestampsTable.containsKey( resourceKey ) ) {
                                        Long keyLastModified = (Long) resourceKeysAndTimestampsTable.get( resourceKey );

                                        if( keyLastModified.longValue() == currentJavaFileLastModified ) {
                                            searchCurrentJavaFile = false;
                                            if( !keysFound.contains( elements[ j ].getKey() ) ) { // avoid
                                                // adding duplicate keys in keysFound vector
                                                keysFound.add( elements[ j ].getKey() );
                                            }
                                        }
                                    }
                                }
                            }
                            // System.out.println( "===== Do we need to search .java file? -> " + searchCurrentJavaFile );

                            if( searchCurrentJavaFile ) {
                                // current .java file was modified since last search for resource keys, so search it again
                                keysFound = searchAndRetrieveKeysFound( elements, javaFiles.get( i ), keysFound );
                            }
                        }
                    }

                    // Output to console which keys were not found
                    // Fix for DPI221973
                    PackagingConsoleFactory consoleFactory = new PackagingConsoleFactory();
                    PackagingConsole rapcConsole = consoleFactory.getConsole();
                    MessageConsoleStream out = rapcConsole.newMessageStream();

                    ConsoleUtils.openConsole( rapcConsole );
                    consoleFactory.showConsole();

                    String resourceBundleName = getResourceBundleName();

                    if( elements.length == keysFound.size() ) {
                        out.println( "All resources in " + resourceBundleName + " are in use." );
                    } else {
                        out.println( "Strings not found from " + resourceBundleName + ":" );
                        for( int i = 0; i < elements.length; i++ ) {
                            if( !keysFound.contains( elements[ i ].getKey() ) ) {
                                if( isStrictMode ) {
                                    out.println( resourceBundleName + "Resource." + elements[ i ].getKey() );
                                } else {
                                    out.println( elements[ i ].getKey() );
                                }
                            }
                        }
                    }
                    out.println( "" );
                }
            }
        } );
        _validateButton = validateButton;

        /**
         * Mark all correct button
         */
        data = new FormData();
        data.left = new FormAttachment( optionsButton );
        data.top = new FormAttachment( 0, 0 );
        Button markAllCorrectButton = new Button( _composite, SWT.PUSH | SWT.CENTER );
        markAllCorrectButton.setLayoutData( data );
        markAllCorrectButton.setText( "Mark all correct" );
        markAllCorrectButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent event ) {
                if( locale instanceof RIMResourceLocale ) {

                    boolean proceed = MessageDialog.openQuestion( null, "Resource Editor",
                            "This will mark all lines as being translated.\nAre you sure?" );
                    if( proceed ) {
                        String originalLocaleString = ( (RIMResourceLocale) locale ).getCollection().getOriginalLocaleName();

                        if( ( (RIMResourceLocale) locale ).getLocaleName().equals( "" )
                                && ResourceEditorOptionsDialog.ROOT.equals( originalLocaleString ) ) {
                            // Case #1: Do nothing if user selected locale and original locale are both root (root is a special
                            // case)
                        } else if( ( (RIMResourceLocale) locale ).getLocaleName().equals( originalLocaleString ) ) {
                            // Case #2: Do nothing if user selected locale is the original locale but both are non-root similar to
                            // Case #1
                        } else if( null != originalLocaleString ) {
                            ResourceEditorOptionsDialog.generateOriginalLocaleHashtable( locale );
                            for( Enumeration e = ResourceEditorOptionsDialog.getOriginalLocaleHashtable().keys(); e
                                    .hasMoreElements(); ) {
                                String resourceKey = e.nextElement().toString();
                                RIMResourceElement currentElement = ( (RIMResourceLocale) locale )
                                        .getResourceElement( resourceKey );
                                String checksumHexValue = ResourceEditorOptionsDialog.getOriginalLocaleHashtable()
                                        .get( resourceKey ).toString();

                                if( checksumHexValue.startsWith( ResourceEditorOptionsDialog.HEX_PREFIX ) ) {
                                    checksumHexValue = checksumHexValue.substring( 2 ); // remove
                                    // "0x" hex prefix
                                }
                                long checksumLongValue = Long.parseLong( checksumHexValue.trim(), 16 );
                                currentElement.setHash( checksumLongValue );
                                ResourceEditorOptionsDialog.updateVersioningForResourceElementOnly( false, currentElement );
                            }
                        }
                    }
                }
            }
        } );
        _markAllCorrectButton = markAllCorrectButton;

        // Decide whether to show the Validate button or Mark all correct button (depending on whether "Set in translator mode"
        // has been activated from the Resource Editor options dialog
        if( ResourceEditorOptionsDialog.getTranslatorMode() ) {
            markAllCorrectButton.setVisible( true );
            validateButton.setVisible( false );
        } else {
            validateButton.setVisible( true );
            markAllCorrectButton.setVisible( false );
        }

        return addButton;

        /*
         * data = new FormData(); data.left = new FormAttachment( _addButton ); data.top = new FormAttachment( 0, 0 ); Button
         * deleteButton = new Button( _composite, SWT.PUSH | SWT.CENTER ); deleteButton.setText( "Delete Key(s)" );
         * deleteButton.setLayoutData( data );
         */
    }

    private static Composite createComposite( Composite container ) {
        FormLayout layout = new FormLayout();
        layout.marginHeight = 5;
        layout.marginWidth = 5;
        layout.spacing = 5;

        Composite composite = new Composite( container, SWT.NONE );
        composite.setLayout( layout );

        return composite;
    }

    private Table createTable( Control topAttachControl ) {
        FormData data = new FormData();
        data.bottom = new FormAttachment( 100, 0 );
        data.left = new FormAttachment( 0, 0 );
        data.right = new FormAttachment( 100, 0 );
        data.top = new FormAttachment( topAttachControl );

        final Table table = new Table( _composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION
                | SWT.HIDE_SELECTION );
        table.setHeaderVisible( true );
        table.setLinesVisible( true );
        table.setLayout( new TableLayout() );
        table.setLayoutData( data );

        table.addMouseListener( new MouseAdapter() {
            public void mouseDoubleClick( MouseEvent e ) {
                Point selectionPoint = new Point( e.x, e.y );
                int columnIndex = getColumnIndex( selectionPoint.x );
                TableItem item = table.getItem( selectionPoint );

                if( columnIndex != -1 && item != null ) {
                    ResourceElement element = (ResourceElement) item.getData();
                    if( element instanceof RIMResourceElement ) {
                        RIMResourceLocale locale = (RIMResourceLocale) element.getLocale();
                        if( locale.isRrcFileWritable() == false ) {
                            if( MessageDialog.openQuestion( null, "Resource Editor",
                                    locale.getRrcFilename()
                                            + " is read-only.\nDo you want to mark it read-write?" ) ) {
                                locale.setRrcFileWritable();
                                locale.getCollection().setHeaderFileWritable();
                                _uiFactory.createCellEditorUI( element, columnIndex ).display();
                            } else {
                                MessageDialog.openInformation(
                                        null,
                                        "Resource Editor",
                                        "You will not be able to save changes to "
                                                + locale.getRrcFilename() + "." );
                                return;
                            }
                        } else {
                            _uiFactory.createCellEditorUI( element, columnIndex ).display();
                        }
                    } else {
                        _uiFactory.createCellEditorUI( element, columnIndex ).display();
                    }
                }
            }

            public void mouseDown( MouseEvent e ) {
                Point selectionPoint = new Point( e.x, e.y );
                int columnIndex = getColumnIndex( selectionPoint.x );
                TableItem item = table.getItem( selectionPoint );
                if( columnIndex == -1 || item == null ) {
                    table.deselectAll();
                }
            }
        } );

        FontData unicodeFontData = new FontData( "Arial Unicode MS", 12, SWT.NORMAL );
        Font unicodeFont = new Font( table.getDisplay(), unicodeFontData );
        table.setFont( unicodeFont );

        TableColumn keyColumn = new TableColumn( table, SWT.NONE );
        keyColumn.setText( "Keys" );
        ( (TableLayout) table.getLayout() ).addColumnData( new ColumnWeightData( 50 ) );

        TableColumn valueColumn = new TableColumn( table, SWT.NONE );
        valueColumn.setText( "Values" );
        ( (TableLayout) table.getLayout() ).addColumnData( new ColumnWeightData( 50 ) );

        return table;
    }

    private TableViewer createTableViewer( Control topAttachControl, ResourceLocale locale ) {
        Table table = createTable( topAttachControl );
        TableViewer viewer = new TableViewer( table );
        viewer.setColumnProperties( _columnProperties );
        viewer.setContentProvider( _contentProvider );
        viewer.setLabelProvider( _labelProvider );
        viewer.setSorter( new ResourceSorter( viewer, _comparators ) );
        viewer.setInput( locale );

        return viewer;
    }

    private int getColumnIndex( int x ) {
        int gridLineWidth = _viewer.getTable().getGridLineWidth();
        TableColumn[] columns = _viewer.getTable().getColumns();
        for( int i = 0; i < columns.length; ++i ) {
            if( x < columns[ i ].getWidth() + gridLineWidth ) {
                return i;
            }
            x -= columns[ i ].getWidth() + gridLineWidth;
        }
        return -1;
    }

    /**
     * Returns underlying .rrc file associated with locale represented by this ResourceEditorPage. Returns null if not applicable.
     *
     * @return
     */
    protected File getRrcFile() {
        return _rrcFile;
    }

    /**
     * Helper method used to show/hide "Validate" and "Mark all correct" buttons according to whether "Set in translator mode" in
     * the Resource Editor options dialog has been activated.
     */
    protected void updateModeForValidateButton() {
        if( ResourceEditorOptionsDialog.getTranslatorMode() ) {
            // "Set in translator mode" in Resource Editor options dialog has been activated, so hide "Validate" button and show
            // "Mark all correct" button
            _markAllCorrectButton.setVisible( true );
            _validateButton.setVisible( false );
        } else {
            _validateButton.setVisible( true );
            _markAllCorrectButton.setVisible( false );
        }
    }

    /**
     * Returns ResourceLocale object associated with locale represented by this ResourceEditorPage
     *
     * @return
     */
    protected ResourceLocale getLocale() {
        return _locale;
    }

    /**
     * Searches for resource keys from elements in .java file represented by javaFile. Returns a vector of Strings representing
     * resource keys that were found in the .java file.
     *
     * @param elements
     *            RIMResourceElements whose keys are to be searched for in javaFile
     * @param javaFile
     *            .java file to search in
     * @param keysFound
     *            Vector of key strings that have been found in other .java files so far
     * @return
     */
    private Vector< String > searchAndRetrieveKeysFound( RIMResourceElement[] elements, File javaFile, Vector< String > keysFound ) {
        // Check if strict mode is enabled (i.e. Match resource bundle when validating resources (strict) has been activated)
        boolean isStrictMode = ResourceEditorOptionsDialog.getStrictMode();

        Hashtable resourceKeysAndTimestampsTable = new Hashtable();
        try {
            resourceKeysAndTimestampsTable.clear();
            BufferedReader bufferedReader = new BufferedReader( new FileReader( javaFile ) );
            String currentLine = bufferedReader.readLine();
            String resourceBundleName = getResourceBundleName();

            while( currentLine != null ) {
                if( currentLine.contains( "//" ) ) {
                    currentLine = currentLine.substring( 0, currentLine.indexOf( "//" ) );
                    // ignore everything after single-line // comments
                }

                if( currentLine.contains( "/*" ) ) {
                    currentLine = currentLine.substring( 0, currentLine.indexOf( "/*" ) );
                    // ignore everything after muli-line /* comments
                }

                for( int j = 0; j < elements.length; j++ ) {
                    String resourceKey = elements[ j ].getKey();
                    String resourceKeyInStrictMode = resourceBundleName + "Resource." + resourceKey;
                    if( currentLine.contains( resourceKey ) ) {
                        boolean resourceKeyFound = false;
                        if( isStrictMode ) {
                            if( currentLine.contains( resourceKeyInStrictMode ) ) {
                                resourceKeyFound = true; // resource key found in current line
                            } else {
                                resourceKeyFound = false;
                            }
                        } else {
                            resourceKeyFound = true;
                        }

                        if( resourceKeyFound ) {
                            if( !keysFound.contains( resourceKey ) ) { // avoid adding duplicate keys in keysFound vector
                                keysFound.add( resourceKey );
                            }

                            if( resourceKeysAndTimestampsTable.containsKey( resourceKey ) ) {
                                resourceKeysAndTimestampsTable.remove( resourceKey ); // avoid duplicate keys in hash table
                            }

                            if( isStrictMode ) {
                                resourceKeysAndTimestampsTable.put( resourceKeyInStrictMode, new Long( javaFile.lastModified() ) );
                            } else {
                                resourceKeysAndTimestampsTable.put( resourceKey, new Long( javaFile.lastModified() ) );
                            }
                        }
                    }
                }
                currentLine = bufferedReader.readLine();
            }
            bufferedReader.close();
        } catch( Exception e ) {
        }

        if( _resourceKeysInJavaFilesTable.containsKey( javaFile.getAbsolutePath() ) ) {
            _resourceKeysInJavaFilesTable.remove( javaFile.getAbsolutePath() ); // avoid duplicate keys in hash table
        }

        if( 0 == resourceKeysAndTimestampsTable.size() ) {
            // no resource keys found in javaFile, hence save last modified timestamp of javaFile
            _resourceKeysInJavaFilesTable.put( javaFile.getAbsolutePath(), Long.valueOf( javaFile.lastModified() ) );
        } else {
            _resourceKeysInJavaFilesTable.put( javaFile.getAbsolutePath(), resourceKeysAndTimestampsTable );
        }
        return keysFound;
    }

    /**
     * Helper method returns resource bundle name to which this ResourceEditorPage belongs to. (e.g. if this ResourceEditorPage
     * represents contents of HelloWorld_en.rrc, this method will return "HelloWorld"). Used by the Validate button.
     *
     * @return
     */
    private String getResourceBundleName() {
        String resourceBundleName = getRrcFile().getName();
        if( resourceBundleName.contains( "_" ) ) {
            resourceBundleName = resourceBundleName.substring( 0, resourceBundleName.indexOf( "_" ) );
        } else {
            resourceBundleName = resourceBundleName.substring( 0, resourceBundleName.lastIndexOf( ResourceConstants.RRC_SUFFIX ) );
        }
        return resourceBundleName;
    }

}
