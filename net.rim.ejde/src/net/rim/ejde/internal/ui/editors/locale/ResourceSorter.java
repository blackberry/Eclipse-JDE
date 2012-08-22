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

import java.util.Comparator;
import java.util.Vector;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.sdk.resourceutil.ResourceElement;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.TableColumn;

public class ResourceSorter extends ViewerSorter {
    private static class SortInfo {
        int _columnIndex;
        Comparator< ResourceElement > _comparator;
        boolean _descending;
    }

    private static Image _ascendingImage;
    private static Image _descendingImage;

    private TableViewer _viewer;
    private Vector< SortInfo > _sortInfos;

    static {
        _ascendingImage = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons/obj16/uparrow.png" )
                .createImage();
        _descendingImage = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons/obj16/downarrow.png" )
                .createImage();
    }

    public ResourceSorter( TableViewer viewer, Vector< Comparator< ResourceElement >> comparators ) {
        _viewer = viewer;
        _sortInfos = createSortInfos( comparators );
        setColumnSortIcon( 0, false );
    }

    public int compare( Viewer viewer, Object o1, Object o2 ) {
        ResourceElement element1 = (ResourceElement) o1;
        ResourceElement element2 = (ResourceElement) o2;
        for( SortInfo sortInfo : _sortInfos ) {
            int result = sortInfo._comparator.compare( element1, element2 );
            if( result != 0 ) {
                return sortInfo._descending ? -result : result;
            }
        }
        return 0;
    }

    private void createSelectionListener( TableColumn column, final SortInfo sortInfo ) {
        column.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                sort( sortInfo );
            }
        } );
    }

    private Vector< SortInfo > createSortInfos( Vector< Comparator< ResourceElement >> comparators ) {
        TableColumn[] columns = _viewer.getTable().getColumns();
        Vector< SortInfo > sortInfos = new Vector< SortInfo >( columns.length );
        for( int i = 0; i < columns.length; ++i ) {
            SortInfo sortInfo = new SortInfo();
            sortInfo._columnIndex = i;
            sortInfo._comparator = comparators.elementAt( i );
            sortInfo._descending = false;
            sortInfos.add( sortInfo );
            createSelectionListener( columns[ i ], sortInfo );
        }

        return sortInfos;
    }

    private void setColumnSortIcon( int columnIndex, boolean descending ) {
        TableColumn[] columns = _viewer.getTable().getColumns();
        for( int i = 0; i < columns.length; ++i ) {
            if( i == columnIndex ) {
                if( descending ) {
                    columns[ i ].setImage( _descendingImage );
                } else {
                    columns[ i ].setImage( _ascendingImage );
                }
            } else {
                columns[ i ].setImage( null );
            }
        }
    }

    private void sort( SortInfo sortInfo ) {
        if( sortInfo == _sortInfos.elementAt( 0 ) ) {
            // Column was clicked when it was already being used...reverse sort
            // ordering.
            sortInfo._descending = !sortInfo._descending;
        } else {
            // New column has been chosen...move this SortInfo to the front of
            // the list.
            _sortInfos.remove( sortInfo );
            _sortInfos.insertElementAt( sortInfo, 0 );
            sortInfo._descending = false;
        }
        setColumnSortIcon( sortInfo._columnIndex, sortInfo._descending );
        _viewer.refresh();
    }
}
