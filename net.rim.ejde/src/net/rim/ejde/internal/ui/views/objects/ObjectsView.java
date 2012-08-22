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
package net.rim.ejde.internal.ui.views.objects;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Vector;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.core.RimIDEUtil;
import net.rim.ejde.internal.ui.dialogs.ObjectViewFilterDialog;
import net.rim.ejde.internal.ui.dialogs.ObjectViewPathToDialog;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;
import net.rim.ejde.internal.ui.views.AbstractTreeOwnerDrawLabelProvider;
import net.rim.ejde.internal.ui.views.BasicDebugView;
import net.rim.ejde.internal.ui.views.MenuAction;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ide.RIA;
import net.rim.ide.core.IDEError;
import net.rim.ide.core.ObjectsContentsHelper;
import net.rim.ide.core.ObjectsContentsHelper.Callback;
import net.rim.ide.core.ObjectsContentsHelper.LoadProgressCallback;
import net.rim.ide.core.VarContentsHelper.Line;
import net.rim.ide.core.VarContentsHelper.MenuItem;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;

/**
 * View to display information of objects created in the memory.
 */
public class ObjectsView extends BasicDebugView implements Callback {

    public static final String OBJECTS_VIEW_ID = "net.rim.ejde.ui.viewers.ObjectsView";

    private static final Logger log = Logger.getLogger( ObjectsView.class );

    final static int NUMBER_OF_OBJECTS_MESSAGE = 0;

    final static int SIZE_OF_MEMORY_MESSAGE = 1;

    final static int NUMBER_OF_ADDED_OBJECTS_MESSAGE = 2;

    final static int NUMBER_OF_DELETED_OBJECTS_MESSAGE = 3;

    private String[] _messages = new String[ 4 ];

    private TableViewer _tableViewer;

    private TableColumn[] _tableColumns;

    private int _selectedColumns = -1;

    private int _selectedRow = -1;

    private MyLabelProvider _labelProvider;

    private int _moveCursorTo;

    private IProgressMonitor _progressMonitor;

    // filter options
    private int _snapshotFilter = 0;

    private String _type;

    private String _process;

    private int _location;

    private boolean _showGroupMember;

    private boolean _showRecursiveSize;

    private boolean _includeAllInstance;

    static private ObjectsContentsHelper getObjectsContentsHelper() {
        RIA ria = RIA.getCurrentDebugger();

        if( null != ria ) {
            return ria.getObjectsContentsHelper();
        }

        return null;
    }

    /**
     * Constructs an instance of ObjectsView.
     */
    public ObjectsView() {
        super( REFRESH_BUTTON | GARBAGE_COLLECTION_BUTTON | FILTER_BUTTON | SAVE_BUTTON | CLEAR_BUTTON | SNAPSHOT_BUTTON
                | FORWARD_BUTTON | BACKWARD_BUTTON | RETURN_TO_START_BUTTON | RETURN_TO_END_BUTTON );
        // get RIA instance
        // create column array
        _tableColumns = new TableColumn[ ObjectsContentsHelper.NUM_COLUMNS ];

        initializeOptions();

        if( log.isDebugEnabled() ) {
            log.debug( String.format( "Instance [%s] created.", hashCode() ) ); //$NON-NLS-1$
        }
    }

    synchronized void setProgressMonitor( IProgressMonitor monitor ) {
        _progressMonitor = monitor;
    }

    synchronized IProgressMonitor getProgressMonitor() {
        return _progressMonitor;
    }

    /***************************************************************************
     * @link java.lang.Object#finalize()
     */
    @Override
    protected void finalize() {
        if( log.isDebugEnabled() ) {
            log.debug( String.format( "Instance [%s] finalized.", hashCode() ) ); //$NON-NLS-1$
        }
    }

    private void initializeOptions() {
        IPreferenceStore ps = ContextManager.PLUGIN.getPreferenceStore();
        _showGroupMember = ps.getBoolean( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_SHOW_GROUP_MEMBER );
        _showRecursiveSize = ps.getBoolean( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_SHOW_RECURSIVE_SIZE );
    }

    public void createTableViewPart( Composite parent ){
        parent.setLayout( new GridLayout( 1, false ) );
        // create table viewer
        _tableViewer = new TableViewer( parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.VIRTUAL | SWT.SINGLE
                | SWT.FULL_SELECTION );
        _tableViewer.getTable().setLayoutData( new GridData( GridData.FILL_BOTH ) );
        _tableViewer.getTable().setHeaderVisible( true );
        _tableViewer.getTable().setLinesVisible( true );
        _tableViewer.getTable().addMouseListener( new MyMouseAdapter() );
        _tableViewer.getTable().addSelectionListener( new MyTableRowSelectionListener() );
        _tableViewer.setUseHashlookup( true );
        // create columns
        for( int i = 0; i < _tableColumns.length; i++ ) {
            createColumn( i );
        }

        _labelProvider = new MyLabelProvider( _tableViewer );
        _tableViewer.setLabelProvider( _labelProvider );

        OwnerDrawLabelProvider.setUpOwnerDraw( _tableViewer );

        _tableViewer.setContentProvider( new MyContentProvider( _tableViewer ) );
        // create context menu(pop-up menu)
        createContextMenu();
    }

    private void createContextMenu() {
        MenuManager menuManager = new MenuManager( "PopupMenu" ); //$NON-NLS-1$
        menuManager.setRemoveAllWhenShown( true );

        menuManager.addMenuListener( new IMenuListener() {
            public void menuAboutToShow( IMenuManager m ) {
                fillContextMenu( m );
            }
        } );

        Menu menu = menuManager.createContextMenu( _tableViewer.getControl() );

        _tableViewer.getControl().setMenu( menu );

        getSite().registerContextMenu( menuManager, _tableViewer );
    }

    void fillContextMenu( IMenuManager menuMgr ) {
        Vector< MenuItem > menuVector = null;

        try {
            menuVector = ObjectsView.getObjectsContentsHelper().getMenu();
        } catch( IDEError e ) {
            log.error( e.getMessage(), e );
        }

        if( menuVector == null ) {
            return;
        }

        MenuItem menuItem;
        for( Iterator< MenuItem > iterator = menuVector.iterator(); iterator.hasNext(); ) {
            menuItem = iterator.next();

            if( menuItem.isSeparator ) {
                menuMgr.add( new Separator() );
            } else {
                menuMgr.add( new MenuAction( this, menuItem ) );
            }
        }

        menuMgr.add( new Separator( IWorkbenchActionConstants.MB_ADDITIONS ) );
    }

    void setMessageLabel( String string, int index ) {
        _messages[ index ] = string;
        displayMessage();
    }

    private void displayMessage() {
        StringBuffer buf = new StringBuffer();
        for( String message : _messages ) {
            if( ( message == null ) || message.trim().equals( IConstants.EMPTY_STRING ) ) {
                continue;
            }
            buf.append( message + IConstants.SEMICOLON_MARK );
        }
        setMessage( buf.toString(), false );
    }

    private void clearMessage() {
        cleanMessage();
    }

    /**
     * Creates columns of the tree view.
     *
     * @param index
     *            index of column
     */
    private void createColumn( int index ) {
        int width;
        switch( index ) {
            case 0:
                width = 200;
                break;
            case 1:
                width = 350;
                break;
            case 2:
                width = 150;
                break;
            case 3:
                width = 100;
                break;
            default:
                width = 0;
        }
        _tableColumns[ index ] = new TableColumn( _tableViewer.getTable(), SWT.LEFT );
        _tableColumns[ index ].setText( ObjectsContentsHelper.getColumnName( index ) );
        _tableColumns[ index ].setWidth( width );
        _tableColumns[ index ].setResizable( true );
        _tableColumns[ index ].addSelectionListener( new MyTableColumnSelectionListener() );
    }

    /**
     * Gets the index of <code>column</code>.
     *
     * @param column
     * @return index of <code>column</code> or <em>-1</em> if <code>column</code> is not in the table
     */
    private int getColumnIndex( TableColumn column ) {
        Table table = _tableViewer.getTable();
        for( int i = 0; i < table.getColumnCount(); i++ ) {
            if( table.getColumn( i ).equals( column ) ) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Gets the <em>int</em> or <em>long</em> number from the given <code>string</code>. The method supposes that there is only
     * one number in the <code>string</code>.
     *
     * @param string
     *            A string which contains a <code>int</code> or <code>long</code> number (is not supposed to be <code>null</code>
     *            ).
     * @return String of the number in the <code>string</code>, or <code>null</code> if there is no number found in
     *         <code>string</code>
     */
    static private String getNumberFromString( String string ) {

        StringBuffer buf = new StringBuffer( IConstants.EMPTY_STRING );
        boolean findNumber = false;

        char mychar;
        for( int i = 0; i < string.length(); i++ ) {
            mychar = string.charAt( i );
            if( ( mychar >= '0' ) && ( mychar <= '9' ) ) {
                buf.append( String.valueOf( mychar ) );
                findNumber = true;
            } else if( findNumber ) {
                break;
            }
        }
        return buf.toString();
    }

    /**
     * Display the objects information.
     */
    void displayData() {
        // clear all items from the table
        Table table = _tableViewer.getTable();

        table.removeAll();

        ObjectsContentsHelper objectsContentsHelper = ObjectsView.getObjectsContentsHelper();

        if( null == objectsContentsHelper ) {
            return;
        }

        // update contents (set data to lines)
        try {
            objectsContentsHelper.updateContents();
        } catch( IDEError e ) {
            log.error( e.getMessage(), e );
            return;
        }
        //

        table.setRedraw( false );
        _tableViewer.setInput( objectsContentsHelper );
        table.setItemCount( objectsContentsHelper.getRowCount() );
        table.setRedraw( true );

        if( _moveCursorTo >= 0 ) {
            table.setSelection( _moveCursorTo );
            table.showSelection();
        }
    }

    int getIndexOf( Object element ) {
        return 0;
    }

    // ------ Methods in interface IBasicActions ------
    /**
     * (no java doc)
     *
     * @see IBasicActions#clear()
     */
    @Override
    public void clear() {
        _tableViewer.getTable().removeAll();
        clearMessage();
        setHasData( false );
        updateToolbar();
    }

    /**
     * (no java doc)
     *
     * @see IBasicActions#gc()
     */
    @Override
    public void gc() {
        ObjectsContentsHelper objectsContentsHelper = ObjectsView.getObjectsContentsHelper();

        if( null == objectsContentsHelper ) {
            return;
        }

        try {
            objectsContentsHelper.gc();
        } catch( IDEError e ) {
            log.error( e.getMessage(), e );
        }
    }

    /**
     * Gets new objects information, erases the current objects information, and displays the new information.
     *
     * @see IBasicActions#refresh().
     *
     */
    @Override
    public void refresh() {
        RefreshObjectsViewJob job = new RefreshObjectsViewJob( this );
        try {
            PlatformUI.getWorkbench().getProgressService().run( true, true, job );
        } catch( InvocationTargetException e ) {
            log.error( e );
        } catch( InterruptedException e ) {
            log.error( e );
        }
    }

    /**
     * (no java doc)
     *
     * @see IBasicActions#save()
     */
    @Override
    public void save() {
        if( null == ObjectsView.getObjectsContentsHelper() ) {
            return;
        }

        // save profile data to the file
        RimIDEUtil.saveTableToFile( RimIDEUtil.openCSVFileForSave( getSite().getShell() ), _tableViewer.getTable(),
                ObjectsView.getObjectsContentsHelper() );
    }

    /**
     * (no java doc)
     *
     * @see IBasicActions#filter()
     */
    @Override
    public void filter() {
        // create the filters and options setting dialog
        ObjectViewFilterDialog dialog = new ObjectViewFilterDialog( getSite().getShell(), this );
        // display the dialog
        dialog.open();
        if( dialog.isOkButtonClicked() ) {
            // if options are changed, reload objects information
            if( ( _showGroupMember != dialog.getShowGroupMember() ) || ( _showRecursiveSize != dialog.getShowRecursiveSize() ) ) {
                _showGroupMember = dialog.getShowGroupMember();
                _showRecursiveSize = dialog.getShowRecursiveSize();
                refresh();
            }

            // fire a job to update the objects view
            FilterJob job = new FilterJob();
            try {
                PlatformUI.getWorkbench().getProgressService().run( true, true, job );
            } catch( InvocationTargetException e ) {
                log.error( e );
            } catch( InterruptedException e ) {
                log.error( e );
            }
        }
    }

    /**
     * (no java doc)
     *
     * @see IBasicActions#snapshot()
     */
    @Override
    public void snapshot() {
        SnapshotJob job = new SnapshotJob();
        try {
            PlatformUI.getWorkbench().getProgressService().run( true, true, job );
        } catch( InvocationTargetException e ) {
            log.error( e );
        } catch( InterruptedException e ) {
            log.error( e );
        }
    }

    /**
     * Forwards to the next step.
     */
    @Override
    public void forward() {
        try {
            ObjectsView.getObjectsContentsHelper().forwardHistory();
        } catch( IDEError ie ) {
            log.error( ie.getMessage(), ie );
        }
    }

    /**
     * Backwards to the last step.
     */
    @Override
    public void backward() {
        try {
            ObjectsView.getObjectsContentsHelper().backHistory();
        } catch( IDEError ie ) {
            log.error( ie.getMessage(), ie );
        }
    }

    /**
     * Returns to the start.
     */
    @Override
    public void returnToStart() {
        try {
            ObjectsView.getObjectsContentsHelper().startOfHistory();
        } catch( IDEError ie ) {
            log.error( ie.getMessage(), ie );
        }
    }

    /**
     * Goes to the end
     */
    @Override
    public void returnToEnd() {
        try {
            ObjectsView.getObjectsContentsHelper().endOfHistory();
        } catch( IDEError ie ) {
            log.error( ie.getMessage(), ie );
        }
    }

    /**
     * Finds the currently selected context in the UI.
     */
    protected IDebugElement getContext() {
        IAdaptable object = DebugUITools.getDebugContext();
        IDebugElement context = null;

        if( object instanceof IDebugElement ) {
            context = (IDebugElement) object;
        } else if( object instanceof ILaunch ) {
            context = ( (ILaunch) object ).getDebugTarget();

        }

        return context;
    }

    public boolean getShowGroupMember() {
        return _showGroupMember;
    }

    public void setShowGroupMember( boolean showGroupMember ) {
        _showGroupMember = showGroupMember;
    }

    public boolean getShowRecursiveSize() {
        return _showRecursiveSize;
    }

    public void setShowRecursiveSize( boolean showRecursiveSize ) {
        _showRecursiveSize = showRecursiveSize;
    }

    // ------ Methods in interface CallBack ------
    public int askForPathFrom( String title ) {
        ObjectViewPathToDialog dialog = new ObjectViewPathToDialog( getSite().getShell(), title );

        if( dialog.open() == Window.OK ) {
            return dialog.getPath();
        }

        return 0;
    }

    public int getLocationFilterIndex() {
        return _location;
    }

    public String getProcessFilterText() {
        return _process == null ? IConstants.EMPTY_STRING : _process;
    }

    public int getSnapshotFilterIndex() {
        return _snapshotFilter;
    }

    public String getTypeFilterText() {
        // TODO Auto-generated method stub
        return _type == null ? IConstants.EMPTY_STRING : _type;
    }

    public void setAddedText( String text ) {
        if( ( text == null ) || text.trim().equals( IConstants.EMPTY_STRING ) ) {
            setMessageLabel( null, NUMBER_OF_ADDED_OBJECTS_MESSAGE );
        } else {
            String number = ObjectsView.getNumberFromString( text );
            setMessageLabel( NLS.bind( Messages.ObjectsView_NUMBER_OF_ADDED_OBJECTS_MESSAGE, number ),
                    NUMBER_OF_ADDED_OBJECTS_MESSAGE );
        }
    }

    public void setBackButtonEnabled( boolean enabled ) {
        enableActions( BasicDebugView.BACKWARD_BUTTON, enabled );
    }

    public void setDeletedText( String text ) {
        if( ( text == null ) || text.trim().equals( IConstants.EMPTY_STRING ) ) {
            setMessageLabel( null, NUMBER_OF_DELETED_OBJECTS_MESSAGE );
        } else {
            String number = ObjectsView.getNumberFromString( text );
            setMessageLabel( NLS.bind( Messages.ObjectsView_NUMBER_OF_DELETED_OBJECTS_MESSAGE, number ),
                    NUMBER_OF_DELETED_OBJECTS_MESSAGE );
        }
    }

    public void setEndButtonEnabled( boolean enabled ) {
        enableActions( BasicDebugView.RETURN_TO_END_BUTTON, enabled );
    }

    public void setForwardButtonEnabled( boolean enabled ) {
        enableActions( BasicDebugView.FORWARD_BUTTON, enabled );
    }

    public void setLocationFilterIndex( int x ) {
        _location = x;
    }

    public void setProcessFilterText( String s ) {
        _process = s;
    }

    public void setSizeText( String text ) {
        if( ( text == null ) || text.trim().equals( IConstants.EMPTY_STRING ) ) {
            setMessageLabel( null, SIZE_OF_MEMORY_MESSAGE );
        } else {
            String number = ObjectsView.getNumberFromString( text );
            setMessageLabel( NLS.bind( Messages.ObjectsView_SIZE_OF_USED_MEMORY_MESSAGE, number ), SIZE_OF_MEMORY_MESSAGE );
        }
    }

    public void setSnapshotFilterIndex( int x ) {
        _snapshotFilter = x;

    }

    public void setStartButtonEnabled( boolean enabled ) {
        enableActions( BasicDebugView.RETURN_TO_START_BUTTON, enabled );
    }

    public void setStatusText( String text ) {
        if( ( text == null ) || text.trim().equals( IConstants.EMPTY_STRING ) ) {
            setMessageLabel( null, NUMBER_OF_OBJECTS_MESSAGE );
        } else {
            String number = ObjectsView.getNumberFromString( text );
            setMessageLabel( NLS.bind( Messages.ObjectsView_NUMBER_OF_OBJECTS_MESSAGE, number ), NUMBER_OF_OBJECTS_MESSAGE );
        }
    }

    public void setTypeFilterText( String s ) {
        _type = s;

    }

    public void addWatchExpression( String expr ) throws IDEError {
        // create a watch expression
        // IWatchExpression watchExpression=
        // DebugPlugin.getDefault().getExpressionManager().newWatchExpression(expr
        // );
        // //$NON-NLS-1$
        // DebugPlugin.getDefault().getExpressionManager().addExpression(
        // watchExpression);
        // watchExpression.setExpressionContext(getContext());
    }

    public void beep() {
        // TODO Auto-generated method stub

    }

    public void columnHeadersChanged() {
        // TODO Auto-generated method stub

    }

    public void displayLineAttribute( String toStringValue ) {
        MessageDialog.openInformation( getSite().getShell(), Messages.ObjectsView_LINE_ATTRIBUTE_DIALOG_TITLE, toStringValue );
    }

    public int getSelectedColumn() {
        return _selectedColumns;
    }

    public int getSelectedRow() {
        return _selectedRow;
    }

    public void moveCursorTo( int row, int column ) {
        _moveCursorTo = row;
    }

    public File promptAndCreateFile( String wildCard, String title ) {
        return RimIDEUtil.openFileForSave( getSite().getShell(), wildCard, new String[] {} );
    }

    public void reload() {
        _moveCursorTo = -1;
        ReloadObjectsTask job = new ReloadObjectsTask( this, getProgressMonitor() );
        job.doJob();
    }

    class MyRunnable implements Runnable {
        ObjectsView _view;

        public MyRunnable( ObjectsView view ) {
            _view = view;
        }

        public void run() {
            _view.setHasData( true );
            _view.updateToolbar();
            _view.displayData();
        }

    }

    public void updateAllDebugWindows() {
        // TODO Auto-generated method stub
    }

    public void showSource( String fileName, int line ) {
        IFileSystem fileSystem = EFS.getLocalFileSystem();
        IFileStore fileStore = fileSystem.getStore( new Path( fileName ) );
        RimIDEUtil.openSourceFile( fileStore, line );
    }

    public boolean getIncludeAllInstances() {
        return _includeAllInstance;
    }

    public void setIncludeAllInstances( boolean b ) {
        _includeAllInstance = b;

    }

    // ------ Inner Classes ------
    /**
     * Content provider of objects tree view.
     */
    class MyContentProvider implements ILazyContentProvider {
        private TableViewer _myViewer;
        private Object[] _models;

        public MyContentProvider( TableViewer viewer ) {
            _myViewer = viewer;
        }

        public void updateElement( int index ) {
            _myViewer.replace( _models[ index ], index );
        }

        public void dispose() {
            // TODO Auto-generated method stub

        }

        public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
            if( ( newInput == null ) || !( newInput instanceof ObjectsContentsHelper ) ) {
                _models = new Object[ 0 ];
            } else {
                ObjectsContentsHelper helper = (ObjectsContentsHelper) newInput;
                int rowCount = helper.getRowCount();
                _models = new Object[ rowCount ];
                for( int i = 0; i < rowCount; i++ ) {
                    _models[ i ] = helper.getLine( i );
                }
            }
        }

    }

    /**
     * Label provider of objects tree view.
     */
    class MyLabelProvider extends AbstractTreeOwnerDrawLabelProvider {

        public MyLabelProvider( TableViewer viewer ) {
            super( viewer );
        }

        @Override
        public boolean findRowAtSameIndent( Object obj, int indent ) {
            int row = getIndex( obj ) + 1;

            ObjectsContentsHelper objectsContentsHelper = ObjectsView.getObjectsContentsHelper();

            if( null == objectsContentsHelper ) {
                return false;
            }

            for( int i = row; i < objectsContentsHelper.getRowCount(); i++ ) {
                Line line = objectsContentsHelper.getLine( i );

                if( line == null ) {
                    return false;
                }

                int indentOfChild = getIndent( line );

                if( indentOfChild < indent ) {
                    return false;
                }

                if( indentOfChild == indent ) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public int getIndent( Object obj ) {
            Line line = (Line) obj;
            return line.getIndent();
        }

        @Override
        public int getIndex( Object obj ) {
            return _tableViewer.getTable().indexOf( (TableItem) obj );
        }

        @Override
        public boolean hasChildren( Object obj ) {
            Line line = (Line) obj;
            return line.getIcon() != Line.ICON_NONE;
        }

        @Override
        public boolean isExpanded( Object obj ) {
            Line line = (Line) obj;
            return line.getIcon() == Line.ICON_MINUS;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jface.viewers.OwnerDrawLabelProvider#measure(Event, Object)
         */
        @Override
        protected void measure( Event event, Object element ) {
            event.width = _tableViewer.getTable().getColumn( event.index ).getWidth();
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jface.viewers.OwnerDrawLabelProvider#paint(org.eclipse .swt.widgets.Event, java.lang.Object)
         */
        @Override
        protected void paint( Event event, Object element ) {
            // call super method to initiate variables
            super.paint( event, element );
            String text;

            ObjectsContentsHelper objectsContentsHelper = ObjectsView.getObjectsContentsHelper();

            Line line = (Line) element;

            if( objectsContentsHelper != null ) {
                text = ( objectsContentsHelper.getValue( line, event.index ) ).toString();
            } else {
                text = StringUtils.EMPTY;
            }

            if( event.index == 0 ) {
                drawFirstColumn( event, element, text, line.getHilight() );
            } else {
                drawText( event, text, event.x, event.y, line.getHilight() );
            }
        }

        @Override
        public int calculateDisplayLevel( Object obj ) {
            // nothing to do
            return 0;
        }
    }

    class MyMouseAdapter extends MouseAdapter {
        @Override
        public void mouseDown( MouseEvent e ) {
            TableItem selectedItem = _tableViewer.getTable().getItem( _tableViewer.getTable().getSelectionIndex() );
            if( selectedItem == null ) {
                return;
            }
            // If user clicked on the [+] or [-], expand or collapse the item
            Line line = (Line) selectedItem.getData();
            Rectangle rect = _labelProvider.getImageBounds( line, selectedItem.getBounds( 0 ) );
            if( rect.contains( e.x, e.y ) ) {
                switch( line.getIcon() ) {
                    case Line.ICON_PLUS:
                    case Line.ICON_MINUS: {
                        _moveCursorTo = -1;
                        ObjectsView.getObjectsContentsHelper().toggleExpansion( line );
                        displayData();
                        break;
                    }
                }
            }
        }
    }

    class MyTableColumnSelectionListener implements SelectionListener {

        public void widgetDefaultSelected( SelectionEvent e ) {
            // nothing to do

        }

        /**
         * Sort the content when a column is selected.
         */
        public void widgetSelected( SelectionEvent e ) {
            TableColumn column = (TableColumn) e.widget;
            _selectedColumns = getColumnIndex( column );
            // System.out
            //					.println("_moveCursorTO is set as -1 in widgetSelected()"); //$NON-NLS-1$
            _moveCursorTo = -1;
            try {
                ObjectsView.getObjectsContentsHelper().sortRows( _selectedColumns );
            } catch( IDEError e1 ) {
                log.error( e1.getStackTrace(), e1 );
                return;
            }
        }
    }

    class MyTableRowSelectionListener implements SelectionListener {

        public void widgetDefaultSelected( SelectionEvent e ) {
            // nothing to do

        }

        /**
         * Sort the content when a column is selected.
         */
        public void widgetSelected( SelectionEvent e ) {
            _selectedRow = _tableViewer.getTable().getSelectionIndex();
        }
    }

    class FilterJob implements IRunnableWithProgress {

        public void run( IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException {
            ObjectsContentsHelper objectsContentsHelper = ObjectsView.getObjectsContentsHelper();

            if( null == objectsContentsHelper ) {
                return;
            }
            setProgressMonitor( monitor );
            // set filters
            _moveCursorTo = -1;
            try {
                objectsContentsHelper.filterChanged();
            } catch( IDEError e ) {
                log.error( e.getMessage(), e );
            }
            updateToolbar();
        }

    }

    class SnapshotJob implements IRunnableWithProgress {

        public void run( IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException {
            ObjectsContentsHelper objectsContentsHelper = ObjectsView.getObjectsContentsHelper();

            if( null == objectsContentsHelper ) {
                return;
            }
            try {
                setProgressMonitor( monitor );
                objectsContentsHelper.setSnapshot();
                setHasSnapshot( true );
                updateToolbar();
            } catch( IDEError e ) {
                log.error( e.getMessage(), e );
            }
        }

    }

    class RefreshObjectsViewJob implements IRunnableWithProgress {
        ObjectsView _objectsView;

        public RefreshObjectsViewJob( ObjectsView callback ) {
            _objectsView = callback;
        }

        public void run( IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException {
            // get object helper instance
            ObjectsContentsHelper objectsContentsHelper = ObjectsView.getObjectsContentsHelper();

            try {
                if( null != objectsContentsHelper ) {
                    setProgressMonitor( monitor );
                    objectsContentsHelper.setShowGroupMembers( _showGroupMember );
                    objectsContentsHelper.setShowRecursiveSizes( _showRecursiveSize );
                    objectsContentsHelper.setCallBack( _objectsView );
                    objectsContentsHelper.refresh();
                    setHasData( true );
                } else {
                    setHasData( false );
                }
                updateToolbar();
            } catch( IDEError e ) {
                log.error( e.getMessage(), e );
            }
        }
    }

    class ReloadObjectsTask implements LoadProgressCallback {
        IProgressMonitor _reloadProgressMonitor;
        ObjectsView _objectsView;
        boolean _isBeginTaskCalledl;

        public ReloadObjectsTask( ObjectsView view, IProgressMonitor progressmonitor ) {
            _reloadProgressMonitor = progressmonitor;
            _objectsView = view;
        }

        public void setProgress( int n, int total ) {
            if( _reloadProgressMonitor == null ) {
                return;
            }
            if( !_isBeginTaskCalledl ) {
                _reloadProgressMonitor.beginTask( Messages.ObjectsView_RELOADING_MSG, total );
                _isBeginTaskCalledl = true;
            }
            _reloadProgressMonitor.worked( 1 );
        }

        public boolean stopRequested() {
            if( _reloadProgressMonitor == null ) {
                return false;
            }
            return _reloadProgressMonitor.isCanceled();
        }

        public void stopped() {
            // nothing to do
        }

        public IStatus doJob() {
            try {
                _isBeginTaskCalledl = false;
                // reload data
                ObjectsView.getObjectsContentsHelper().reload( this );
            } catch( IDEError e ) {
                return StatusFactory.createStatus( IStatus.ERROR, e.getMessage() == null ? Messages.ObjectsView_REFRESH_ERROR_MSG
                        : e.getMessage() );
            } finally {
                _reloadProgressMonitor.done();
            }
            // display data
            _objectsView.getSite().getShell().getDisplay().asyncExec( new Runnable() {
                public void run() {
                    _objectsView.setHasData( true );
                    _objectsView.updateToolbar();
                    _objectsView.displayData();
                }
            } );

            return StatusFactory.createStatus( IStatus.OK, Messages.ObjectsView_RELOADING_FINISH_MSG );
        }
    }
}
