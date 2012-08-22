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
package net.rim.ejde.internal.ui.views.process;

import java.util.Iterator;
import java.util.Vector;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.ui.views.MenuAction;
import net.rim.ejde.internal.ui.views.VarContentDebugView;
import net.rim.ejde.internal.util.DebugUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ide.RIA;
import net.rim.ide.core.IDEError;
import net.rim.ide.core.ProcessesContentsHelper;
import net.rim.ide.core.VarContentsHelper;
import net.rim.ide.core.VarContentsHelper.MenuItem;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;

/**
 * View to display ProcessView information.
 */
public class ProcessView extends VarContentDebugView implements IConstants {
    public static final String PROCESS_VIEW_ID = "net.rim.ejde.ui.viewers.ProcessView";

    static final Logger _log = Logger.getLogger( ProcessView.class );
    private ProcessesContentsHelper _processesContentsHelper;
    boolean _isInitialized = false;

    /**
     * Constructs a new MemoryStatsView.
     */
    public ProcessView() {
        super( REFRESH_BUTTON | OPTIONS_BUTTON );
    }

    public void createTableViewPart( Composite parent ) {
        TableViewer tableViewer = new TableViewer( parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.VIRTUAL | SWT.SINGLE
                | SWT.FULL_SELECTION );
        tableViewer.getTable().setLayoutData( new GridData( GridData.FILL_BOTH ) );
        tableViewer.getTable().setHeaderVisible( true );
        tableViewer.getTable().setLinesVisible( true );
        tableViewer.setContentProvider( new VarContentProvider( tableViewer ) );
        VarLabelProvider labelProvider = new VarLabelProvider( tableViewer );
        setLabelProvider( labelProvider );
        tableViewer.setLabelProvider( labelProvider );
        tableViewer.getTable().addMouseListener( new VarMouseAdapter() );
        tableViewer.getTable().addSelectionListener( new VarTableRowSelectionListener() );
        setTableView( tableViewer );
        // create table columns
        createColumns();
        // create context menu(pop-up menu)
        createContextMenu();
        // start live update
        startLiveupdate();
    }

    public void refresh() throws CoreException {
        super.refresh();
    }

    protected void handleRIMDebugEvent( DebugEvent event ) {
        if( event.getKind() == DebugEvent.CREATE && !_isInitialized ) {
            Display.getDefault().syncExec( new Runnable() {

                @Override
                public void run() {
                    createColumns();
                }

            } );
        }
    }

    private void createColumns() {
        if( getContentsHelper() == null ) {
            setMessage( Messages.ProcessView_NO_BB_DEBUG_SESSION_MSG, true );
            return;
        }
        cleanMessage();
        if( getTableView().getTable().getColumnCount() == 0 ) {
            // create columns
            for( int i = 0; i < ProcessesContentsHelper.NUM_COLUMNS; i++ ) {
                createColumn( i );
            }
        }
        _isInitialized = true;
    }

    protected void fillExtraContextMenu( IMenuManager menuMgr ) {
        RIA ria = RIA.getCurrentDebugger();
        if( ria == null || !ria.isDebuggerSuspended() ) {
            return;
        }
        Vector< MenuItem > menuVector = null;
        VarContentsHelper helper = getContentsHelper();
        if( helper == null ) {
            return;
        }
        menuVector = getProcessMenus();
        MenuItem menuItem;
        for( Iterator< MenuItem > iterator = menuVector.iterator(); iterator.hasNext(); ) {
            menuItem = iterator.next();

            if( menuItem.isSeparator ) {
                menuMgr.add( new Separator() );
            } else {
                menuMgr.add( new MenuAction( this, menuItem ) );
            }
        }
    }

    private Vector< MenuItem > getProcessMenus() {
        Vector< MenuItem > menuVector = new Vector< MenuItem >();
        VarContentsHelper helper = getContentsHelper();
        if( helper == null ) {
            return menuVector;
        }
        final RIA ria = RIA.getCurrentDebugger();
        if( ria == null ) {
            return menuVector;
        }
        // add a menu separator
        MenuItem menuItem = helper.new MenuItem();
        menuItem.isSeparator = true;
        menuItem.text = "---------";
        menuItem.action = helper.new MenuAction() {
            public void invoked() throws IDEError {
            }
        };
        menuVector.add( menuItem );
        // add Clear Heap Highwater menu
        menuItem = helper.new MenuItem();
        menuItem.text = "Clear Heap Highwater";
        try {
            menuItem.enabled = ria.canDoProcessOperations();
        } catch( IDEError e ) {
            _log.error( e );
            menuItem.enabled = false;
        }
        menuItem.action = helper.new MenuAction() {
            public void invoked() throws IDEError {
                ria.processClearHeapHighWater();
                try {
                    refresh();
                } catch( CoreException e ) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        menuVector.add( menuItem );
        // add Clear Total Allocations menu
        menuItem = helper.new MenuItem();
        menuItem.text = "Clear Total Allocations";
        try {
            menuItem.enabled = ria.canDoProcessOperations();
        } catch( IDEError e ) {
            _log.error( e );
            menuItem.enabled = false;
        }
        menuItem.action = helper.new MenuAction() {
            public void invoked() throws IDEError {
                ria.processClearTotalAllocations();
                try {
                    refresh();
                } catch( CoreException e ) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        menuVector.add( menuItem );
        // add Clear Total CPU menu
        menuItem = helper.new MenuItem();
        menuItem.text = "Clear Total CPU";
        try {
            menuItem.enabled = ria.canDoProcessOperations();
        } catch( IDEError e ) {
            _log.error( e );
            menuItem.enabled = false;
        }
        menuItem.action = helper.new MenuAction() {
            public void invoked() throws IDEError {
                ria.processClearTotalCPU();
                try {
                    refresh();
                } catch( CoreException e ) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        menuVector.add( menuItem );
        return menuVector;

    }

    /**
     * Creates columns of the tree view.
     *
     * @param index
     *            index of column
     *
     */
    private void createColumn( int index ) {
        int width;
        switch( index ) {
            case 0:
            case 2:
                width = 250;
                break;
            default:
                width = 100;
        }
        TableViewer tableviewer = getTableView();
        if( tableviewer == null ) {
            return;
        }
        TableColumn tableColumn = new TableColumn( tableviewer.getTable(), SWT.LEFT );
        tableColumn.setText( ProcessesContentsHelper.getColumnName( index ) );
        tableColumn.setWidth( width );
        tableColumn.setResizable( true );
        tableColumn.addSelectionListener( new VarTableColumnSelectionListener() );
    }

    @Override
    public void setOptions() {
        ProcessViewOptionsDialog dialog = new ProcessViewOptionsDialog( this.getSite().getShell(),
                Messages.ProcessOptionsDialog_TITLE, null, "", MessageDialog.NONE, new String[] { "OK", "Cancel" }, 0 );
        if( dialog.open() == 0 ) {
            startLiveupdate();
        }
    }

    @Override
    protected VarContentsHelper getContentsHelper() {
        if( DebugUtils.isRIMDebuggerRunning() ) {
            if( _processesContentsHelper == null ) {
                RIA ria = RIA.getCurrentDebugger();
                _processesContentsHelper = new ProcessesContentsHelper( ria, ria.getBaseDebugAPI() );
                _processesContentsHelper.setCallBack( new MyCallback() );
            }
            return _processesContentsHelper;
        }
        return null;
    }

    public void RIMDebugTerminated( ILaunch[] launchs ) {
        _processesContentsHelper = null;
        _isInitialized = false;
        Display.getDefault().syncExec( new Runnable() {
            @Override
            public void run() {
                // dispose table columns
                TableColumn[] columns = getTableView().getTable().getColumns();
                for( int i = 0; i < columns.length; i++ ) {
                    columns[ i ].dispose();
                }
                setMessage( Messages.ProcessView_NO_BB_DEBUG_SESSION_MSG, true );
            }
        } );
    }

    public boolean optionsEnabled() {
        // for the process view, the options is always enabled
        return true;
    }

    class MyCallback extends VarCallbackAdaptor {
        @Override
        public void reload() {
            setMoveCursorToIndex( -1 );
            try {
                refresh();
            } catch( CoreException e ) {
                _log.error( e );
            }
        }
    }
}
