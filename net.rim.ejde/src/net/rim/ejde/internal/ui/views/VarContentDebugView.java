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
package net.rim.ejde.internal.ui.views;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.RimIDEUtil;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;
import net.rim.ejde.internal.util.Messages;
import net.rim.ide.RIA;
import net.rim.ide.core.IDEError;
import net.rim.ide.core.VarContentsHelper;
import net.rim.ide.core.VarContentsHelper.Callback;
import net.rim.ide.core.VarContentsHelper.Line;
import net.rim.ide.core.VarContentsHelper.MenuItem;

import org.apache.log4j.Logger;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * This view is the parent of views which display data from a {@link VarContentsHelper}, e.g. process view, locals view, locks
 * view,threads view and statics view.
 *
 */
public abstract class VarContentDebugView extends BasicDebugView {

    private static final Logger _log = Logger.getLogger( VarContentDebugView.class );

    private TableViewer _tableViewer;

    private int _selectedColumns = -1;

    private int _selectedRow = -1;

    private VarLabelProvider _labelProvider;

    private int _moveCursorTo;

    private IProgressMonitor _progressMonitor;

    DebuggerLiveUpdateJob _liveUpdateJob;

    /**
     * Get the instance of the contents helper used by this view.
     *
     * @return
     */
    abstract protected VarContentsHelper getContentsHelper();

    /**
     * Constructs an instance of VarContentDebugView.
     */
    public VarContentDebugView( int style ) {
        super( style );
        if( _log.isDebugEnabled() ) {
            _log.debug( String.format( "Instance [%s] created.", hashCode() ) ); //$NON-NLS-1$
        }
    }

    synchronized protected void setProgressMonitor( IProgressMonitor monitor ) {
        _progressMonitor = monitor;
    }

    synchronized protected IProgressMonitor getProgressMonitor() {
        return _progressMonitor;
    }

    protected void startLiveupdate() {
        IPreferenceStore ps = ContextManager.PLUGIN.getPreferenceStore();
        boolean startLiveUpdate = ps.getBoolean( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_LIVE_UPDATE );
        if( !startLiveUpdate ) {
            // if live update is disabled, we cancel the update job if it is available
            if( _liveUpdateJob != null ) {
                _liveUpdateJob.cancelUpdate();
            }
            return;
        }
        if( _liveUpdateJob == null ) {
            _liveUpdateJob = new DebuggerLiveUpdateJob( "", this );
            _liveUpdateJob.schedule();
        } else {
            if( _liveUpdateJob.getState() == Job.NONE || _liveUpdateJob.isUpdateCanceled() ) {
                _liveUpdateJob.schedule();
            }
        }
    }

    /**
     * Disposes created image instances.
     */
    public void dispose() {
        super.dispose();
        if( _liveUpdateJob != null ) {
            _liveUpdateJob.cancelUpdate();
        }
    }

    /*
     * (non-Javadoc) Sub-clsses may override this method.
     *
     * @see BasicDebugView#refresh()
     */
    public void refresh() throws CoreException {
        _log.trace( "Refreshing DebugView" );
        RIA ria = RIA.getCurrentDebugger();
        if( ria != null ) {
            VarContentsHelper contentHelper = getContentsHelper();
            if( contentHelper == null ) {
                return;
            }
            try {
                contentHelper.updateContents();
                Table table = getTableView().getTable();
                table.setRedraw( false );
                getTableView().setInput( contentHelper );
                table.setItemCount( getContentsHelper().getRowCount() );
                table.setRedraw( true );

                if( _selectedRow >= 0 ) {
                    table.setSelection( _selectedRow );
                    table.showSelection();
                }
            } catch( IDEError e ) {
                _log.error( e );
            }
        }
    }

    /**
     * Gets the TableViewer component used in this view.
     *
     * @return
     */
    protected TableViewer getTableView() {
        if( _tableViewer == null ) {
            _log.error( "The TableViewer component has not been created." );
        }
        return _tableViewer;
    }

    /**
     * Sets the TableViewer component used in this view. <b>This must be called after the TableViewer component is created in the
     * sub-classes</b>.
     *
     * @param view
     */
    protected void setTableView( TableViewer view ) {
        _tableViewer = view;
    }

    /**
     * Gets the LabelProvider of the TableViewer component used in this view.
     *
     * @return
     */
    protected VarLabelProvider getLabelProvider() {
        return _labelProvider;
    }

    /**
     * Sets the LabelProvider of the TableViewer component used in this view. <b>This must be called after the TableViewer
     * component is created in the sub-classes</b>.
     *
     * @param view
     */
    protected void setLabelProvider( VarLabelProvider labelProvider ) {
        _labelProvider = labelProvider;
    }

    /***************************************************************************
     * @link java.lang.Object#finalize()
     */
    @Override
    protected void finalize() {
        if( _log.isDebugEnabled() ) {
            _log.debug( String.format( "Instance [%s] finalized.", hashCode() ) ); //$NON-NLS-1$
        }
    }

    protected void createContextMenu() {
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

    protected void fillContextMenu( IMenuManager menuMgr ) {
        RIA ria = RIA.getCurrentDebugger();
        if( ria == null || !ria.isDebuggerSuspended() ) {
            return;
        }
        Vector< MenuItem > menuVector = null;
        VarContentsHelper helper = getContentsHelper();
        if( helper == null ) {
            return;
        }
        try {
            menuVector = helper.getMenu();
        } catch( IDEError e ) {
            _log.error( e.getMessage(), e );
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
        fillExtraContextMenu( menuMgr );
    }

    /**
     * Sub-classes need to override this method if they want to add extra context menus.
     *
     * @param menuMgr
     */
    protected void fillExtraContextMenu( IMenuManager menuMgr ) {
        // do nothing here
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
     * Display the objects information.
     */
    void displayData() {
        // clear all items from the table
        Table table = _tableViewer.getTable();

        table.removeAll();

        VarContentsHelper contentsHelper = getContentsHelper();

        if( null == contentsHelper ) {
            return;
        }

        // update contents (set data to lines)
        try {
            contentsHelper.updateContents();
        } catch( IDEError e ) {
            _log.error( e.getMessage(), e );
            return;
        }
        //

        table.setRedraw( false );
        _tableViewer.setInput( contentsHelper );
        table.setItemCount( contentsHelper.getRowCount() );
        table.setRedraw( true );

        if( _selectedRow >= 0 ) {
            table.setSelection( _selectedRow );
            table.showSelection();
        }
    }

    /**
     * Gets the index of selected column in the table.
     *
     * @return
     */
    protected int getSelectedColumnIndex() {
        return _selectedColumns;
    }

    /**
     * Sets the index of selected column in the table.
     *
     * @param index
     */
    protected void setSelectedColumnIndex( int index ) {
        _selectedColumns = index;
    }

    /**
     * Gets the index of selected row in the table.
     *
     * @return
     */
    protected int getSelectedRowIndex() {
        return _selectedRow;
    }

    /**
     * Sets the index of selected row in the table.
     *
     * @return
     */
    protected void setSelectedRowIndex( int index ) {
        _selectedRow = index;
    }

    /**
     * Gets the index of row where the cursor should be moved to.
     *
     * @return
     */
    protected int getMoveCursorToIndex() {
        return _moveCursorTo;
    }

    /**
     * Sets the index of row where the cursor should be moved to.
     *
     * @return
     */
    protected void setMoveCursorToIndex( int index ) {
        _moveCursorTo = index;
    }

    // ------ Inner Classes ------
    /**
     * Content provider of objects tree view.
     */
    protected class VarContentProvider implements ILazyContentProvider {
        private TableViewer _myViewer;
        private Object[] _models;

        public VarContentProvider( TableViewer viewer ) {
            _myViewer = viewer;
        }

        public void updateElement( int index ) {
            _myViewer.replace( _models[ index ], index );
        }

        public void dispose() {
            // TODO Auto-generated method stub

        }

        public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
            if( ( newInput == null ) || !( newInput instanceof VarContentsHelper ) ) {
                _models = new Object[ 0 ];
            } else {
                VarContentsHelper helper = (VarContentsHelper) newInput;
                int rowCount = helper.getRowCount();
                _models = new Object[ rowCount ];
                for( int i = 0; i < rowCount; i++ ) {
                    _models[ i ] = helper.getLine( i );
                }
            }
        }

    }

    /**
     * (no java doc)
     *
     * @see IBasicActions#clear()
     */
    @Override
    public void clear() {
        TableViewer tableviewer = getTableView();
        if( tableviewer != null ) {
            tableviewer.getTable().removeAll();
        }
        setHasData( false );
        updateToolbar();
    }

    /**
     * Label provider of objects tree view.
     */
    protected class VarLabelProvider extends AbstractTreeOwnerDrawLabelProvider {

        public VarLabelProvider( TableViewer viewer ) {
            super( viewer );
        }

        @Override
        public boolean findRowAtSameIndent( Object obj, int indent ) {
            int row = getIndex( obj ) + 1;

            VarContentsHelper contentsHelper = getContentsHelper();

            if( null == contentsHelper ) {
                return false;
            }

            for( int i = row; i < contentsHelper.getRowCount(); i++ ) {
                Line line = contentsHelper.getLine( i );

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
            if( _tableViewer.getTable().getColumnCount() != 0 ) {
                event.width = _tableViewer.getTable().getColumn( event.index ).getWidth();
            }
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

            VarContentsHelper contentsHelper = getContentsHelper();
            if( contentsHelper == null ) {
                return;
            }

            Line line = (Line) element;

            text = ( contentsHelper.getValue( line, event.index ) ).toString();

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

    protected class VarMouseAdapter extends MouseAdapter {
        public VarMouseAdapter() {
            // TODO Auto-generated constructor stub
        }

        @Override
        public void mouseDown( MouseEvent e ) {
            int selectionIndex = _tableViewer.getTable().getSelectionIndex();
            if( selectionIndex < 0 ) {
                return;
            }
            TableItem selectedItem = _tableViewer.getTable().getItem( selectionIndex );
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
                        _selectedRow = selectionIndex;
                        VarContentsHelper contentsHelper = getContentsHelper();
                        if( contentsHelper == null ) {
                            return;
                        }
                        contentsHelper.toggleExpansion( line );
                        displayData();
                        break;
                    }
                }
            }
        }
    }

    protected class VarTableColumnSelectionListener implements SelectionListener {

        public VarTableColumnSelectionListener() {
            // TODO Auto-generated constructor stub
        }

        public void widgetDefaultSelected( SelectionEvent e ) {
            // nothing to do

        }

        /**
         * Sort the content when a column is selected.
         */
        public void widgetSelected( SelectionEvent e ) {
            TableColumn column = (TableColumn) e.widget;
            _selectedColumns = getColumnIndex( column );
            _moveCursorTo = -1;
            try {
                VarContentsHelper contentsHelper = getContentsHelper();
                if( contentsHelper == null ) {
                    return;
                }
                contentsHelper.sortRows( _selectedColumns );
            } catch( IDEError e1 ) {
                _log.error( e1.getStackTrace(), e1 );
                return;
            }
        }
    }

    protected class VarTableRowSelectionListener implements SelectionListener {

        public VarTableRowSelectionListener() {
            // TODO Auto-generated constructor stub
        }

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

    protected class VarCallbackAdaptor implements Callback {
        @Override
        public int getSelectedRow() {
            return getSelectedRowIndex();
        }

        @Override
        public int getSelectedColumn() {
            return getSelectedColumnIndex();
        }

        @Override
        public void beep() {
            // TODO Auto-generated method stub

        }

        @Override
        public void showSource( String fileName, int line ) {
            IFileSystem fileSystem = EFS.getLocalFileSystem();
            IFileStore fileStore = fileSystem.getStore( new Path( fileName ) );
            RimIDEUtil.openSourceFile( fileStore, line );

        }

        @Override
        public void updateAllDebugWindows() {
            // TODO Auto-generated method stub

        }

        @Override
        public void addWatchExpression( String expr ) throws IDEError {
            // TODO Auto-generated method stub

        }

        @Override
        public void columnHeadersChanged() {
            // TODO Auto-generated method stub

        }

        @Override
        public void moveCursorTo( int row, int column ) {
            setMoveCursorToIndex( row );
        }

        @Override
        public void reload() {
            // TODO Auto-generated method stub
        }

        @Override
        public File promptAndCreateFile( String wildCard, String title ) {
            return RimIDEUtil.openFileForSave( getSite().getShell(), wildCard, new String[] {} );
        }

        @Override
        public void displayLineAttribute( String toStringValue ) {
            MessageDialog.openInformation( getSite().getShell(), Messages.ObjectsView_LINE_ATTRIBUTE_DIALOG_TITLE, toStringValue );
        }
    }
}
