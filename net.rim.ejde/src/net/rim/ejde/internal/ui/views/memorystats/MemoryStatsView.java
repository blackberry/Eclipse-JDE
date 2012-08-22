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
package net.rim.ejde.internal.ui.views.memorystats;

import java.io.IOException;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.core.RimIDEUtil;
import net.rim.ejde.internal.ui.views.BasicDebugView;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ide.RIA;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * View to display memory statuses information.
 */
public class MemoryStatsView extends BasicDebugView implements IConstants {

    public static final String MEMORY_STATS_VIEW_ID = "net.rim.ejde.ui.viewers.MemoryStatsView";

    static final Logger _log = Logger.getLogger( MemoryStatsView.class );
    private Table _table;
    private int _displayValues[][];
    private int _snapshotValues[][];
    private TableItem[] _tableItems;

    /**
     * Constructs a new MemoryStatsView.
     */
    public MemoryStatsView() {
        super( REFRESH_BUTTON | SNAPSHOT_BUTTON | COMPARE_BUTTON | SAVE_BUTTON );
    }

    public void createTableViewPart( Composite parent ){
        // create table
        _table = new Table( parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.VIRTUAL | SWT.SINGLE | SWT.FULL_SELECTION );
        _table.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        _table.setHeaderVisible( true );
        _table.setLinesVisible( true );
        _tableItems = new TableItem[ RIA.MEMSTATS_NUM_ROWS ];
        // create rows
        for( int i = 0; i < RIA.MEMSTATS_NUM_ROWS; i++ ) {
            _tableItems[ i ] = new TableItem( _table, SWT.NONE );
            _tableItems[ i ].setText( 0, RIA.memStatsRowName( i ) );
        }
        // create columns
        TableColumn rowNameColumn = new TableColumn( _table, SWT.NONE );
        rowNameColumn.setText( RIA.memStatsRowNameHeading() );
        rowNameColumn.setWidth( 180 );
        for( int i = 0; i < RIA.MEMSTATS_NUM_COLUMNS; i++ ) {
            TableColumn column = new TableColumn( _table, SWT.NONE );
            column.setText( RIA.memStatsColumnName( i ) );
            column.pack();
        }
    }

    /**
     * Display memory statuses information in table.
     */
    private void fillTable() {
        // Turn off drawing to avoid flicker
        _table.setRedraw( false );

        // We remove all the table entries
        clearTableContent();
        // row
        for( int i = 0; i < RIA.MEMSTATS_NUM_ROWS; ++i ) {
            // column
            for( int j = 0; j < RIA.MEMSTATS_NUM_COLUMNS; ++j ) {
                _tableItems[ i ].setText( j + 1, Integer.toString( _displayValues[ i ][ j ] ) );
            }
        }
        for( int i = 0; i < 5; i++ ) {
            _table.getColumn( i ).pack();
        }
        // Turn drawing back on
        _table.setRedraw( true );
    }

    /**
     * Clear the table.
     */
    private void clearTableContent() {
        for( int i = 0; i < _table.getItemCount(); i++ ) {
            TableItem item = _table.getItem( i );
            for( int j = 1; j < _table.getColumnCount(); j++ )
                item.setText( j, EMPTY_STRING );
        }

    }

    public void setFocus() {
        // nothing to do

    }

    /**
     * Compares the current memory statuses information to the snapshot.
     */
    private void compareSnapshot() {
        if( _snapshotValues == null || _snapshotValues.length == 0 ) {
            MessageDialog.openWarning( getSite().getShell(), Messages.MemoryStatsView_MESSAGE_DIALOG_TITLE,
                    Messages.MemoryStatsView_SNAPSHOT_NOT_TAKEN_MESSAGE );
            return;
        }
        RIA ria = RIA.getCurrentDebugger();
        if( ria != null ) {
            int newValues[][] = ria.memStatsGetData();
            _displayValues = new int[ 6 ][ 4 ];
            for( int i = 0; i < RIA.MEMSTATS_NUM_ROWS; ++i ) {
                for( int j = 0; j < RIA.MEMSTATS_NUM_COLUMNS; ++j ) {
                    _displayValues[ i ][ j ] = newValues[ i ][ j ] - _snapshotValues[ i ][ j ];
                }
            }
            fillTable();
        }
    }

    // ------ Methods in interface IBasicActions ------
    /**
     * Gets the current memory statuses information and displays it.
     *
     * @see IBasicActions#refresh().
     */
    public void refresh() {
        RIA ria = RIA.getCurrentDebugger();
        if( ria != null ) {
            _displayValues = ria.memStatsGetData();
            if( _displayValues == null || _displayValues.length == 0 )
                return;
            fillTable();
            setHasData( true );
            updateToolbar();
        }
    }

    /**
     * (non-Javadoc)
     *
     * @see IBasicActions#compare().
     */
    public void compare() {
        compareSnapshot();
    }

    /**
     * Saves the current memory statues information to a csv file.
     *
     * @see IBasicActions#save().
     */
    public void save() {
        if( RIA.getCurrentDebugger() != null ) {
            if( _displayValues == null || _displayValues.length == 0 ) {
                _displayValues = RIA.getCurrentDebugger().memStatsGetData();
            }
            FileDialog dialog = new FileDialog( getSite().getShell(), SWT.SAVE );
            String[] filters = new String[] { "*.csv" };
            String[] filterNames = new String[] { "csv" };
            dialog.setFilterExtensions( filters );
            dialog.setFilterNames( filterNames );
            String csvFilePathString = dialog.open();
            IPath csvFilePath = new Path( csvFilePathString );
            // for some reason, on Mac, the file extension is not returned
            if( !IConstants.CSV_EXTENSION.equals( csvFilePath.getFileExtension() ) ) {
                csvFilePath = new Path( csvFilePath.toOSString() + ".csv" );
            }

            try {
                if( !csvFilePath.toFile().exists() ) {
                    csvFilePath.toFile().createNewFile();
                }
                RimIDEUtil.saveTableToFile( csvFilePath.toFile(), _table );
            } catch( IOException e ) {
                ErrorDialog.openError( getSite().getShell(), Messages.ErrorHandler_DIALOG_TITLE,
                        NLS.bind( Messages.MemoryStatsView_File_Creation_Error_Msg, csvFilePath.toOSString() ),
                        StatusFactory.createErrorStatus( e.getMessage() ) );
                _log.error( e );
            }
        }
    }

    /**
     * Takes a snapshot of current memory statuses information.
     *
     * @see IBasicActions#snapshot().
     */
    public void snapshot() {
        _snapshotValues = _displayValues;
        setHasSnapshot( true );
        updateToolbar();
    }

    /**
     * Clear the content of the view.
     *
     * @see net.rim.ejde.internal.ui.views.BasicDebugView#clear()
     */
    @Override
    public void clear() {
        clearTableContent();
        setHasData( false );
        updateToolbar();
    }
}
