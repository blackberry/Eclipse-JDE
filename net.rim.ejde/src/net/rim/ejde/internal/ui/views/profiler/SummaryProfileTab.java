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
package net.rim.ejde.internal.ui.views.profiler;

import net.rim.ejde.internal.ui.views.AbstractTreeOwnerDrawLabelProvider;
import net.rim.ejde.internal.util.Messages;
import net.rim.ide.core.ProfileData;
import net.rim.ide.core.ProfileItem;
import net.rim.ide.core.ProfileItemSource;

import org.eclipse.swt.widgets.Event;

/**
 * An instance of this class is used to display summary information of a ProfileData instance.
 */
public class SummaryProfileTab extends ProfileTab {
    long ramTicks;
    long fullTicks;
    long GCTotalTicks;
    long GCTotalCount;
    Object[] _summary;

    /**
     * Constructs an instance of SummaryProfileTab
     *
     * @param view
     *            An instance of ProfileView on which this tab will be displayed.
     */
    public SummaryProfileTab( ProfilerView view ) {
        super( view );
        getTabItem().setText( Messages.SummaryProfileTab_SUMMARY_TAB_TITLE );
    }

    void refresh() {
        initializeTableViewer();
    }

    /**
     * @see ProfileTab#displayData(ProfileItemSource pis).
     */
    void displayData( ProfileItemSource pis ) {
        if( !( pis instanceof ProfileData ) )
            return;
        ProfileData pd = (ProfileData) pis;
        setTotal( pd.getTotalTicks() );
        ProfileItem itemIdle = pd.getIdle();
        ProfileItem itemGCRam = pd.getGCRam();
        ProfileItem itemFull = pd.getGCFull();
        ProfileItem itemCodeExecution = pd.getCodeExecution();
        ramTicks = itemGCRam.getTicks();
        fullTicks = itemFull.getTicks();
        GCTotalTicks = ramTicks + fullTicks;
        GCTotalCount = itemGCRam.getCount() + itemFull.getCount();
        _summary = new Object[ 5 ];
        _summary[ 0 ] = new SummaryItem( Messages.SummaryProfileTab_IDLE_COLUMN_TITLE, itemIdle );
        _summary[ 1 ] = new SummaryItem( Messages.SummaryProfileTab_CODE_EXECUTION_COLUMN_TITLE, itemCodeExecution );
        _summary[ 2 ] = new SummaryItem( Messages.SummaryProfileTab_GC_COLUMN_TITLE, getPercent( GCTotalTicks ), GCTotalTicks,
                GCTotalCount );
        _summary[ 3 ] = new SummaryItem( Messages.SummaryProfileTab_RAM_COLUMN_TITLE, itemGCRam );
        _summary[ 4 ] = new SummaryItem( Messages.SummaryProfileTab_FULL_COLUMN_TITLE, itemFull );
        _data = new Object[ 3 ];
        System.arraycopy( _summary, 0, _data, 0, 3 );
        displayData( _data );
    }

    int getChildrenCount( Object obj ) {
        if( ( (SummaryItem) obj ).getDetailString().equals( Messages.SummaryProfileTab_GC_COLUMN_TITLE ) )
            return 2;
        else
            return 0;
    }

    void expandItem( Object obj ) {
        if( ( (SummaryItem) obj ).getDetailString().equals( Messages.SummaryProfileTab_GC_COLUMN_TITLE ) )
            _data = _summary;
        displayData( _data );
        putIntoExpansion( obj );
    }

    public int getIndent( Object obj ) {
        if( ( (SummaryItem) obj ).getDetailString().equals( Messages.SummaryProfileTab_GC_COLUMN_TITLE )
                || ( (SummaryItem) obj ).getDetailString().equals( Messages.SummaryProfileTab_IDLE_COLUMN_TITLE )
                || ( (SummaryItem) obj ).getDetailString().equals( Messages.SummaryProfileTab_CODE_EXECUTION_COLUMN_TITLE ) )
            return 0;
        return 1;
    }

    void collapseItem( Object obj ) {
        if( ( (SummaryItem) obj ).getDetailString().equals( Messages.SummaryProfileTab_GC_COLUMN_TITLE ) ) {
            _data = new Object[ 3 ];
            System.arraycopy( _summary, 0, _data, 0, 3 );
        }
        displayData( _data );
        removeFromExpansion( obj );
    }

    /**
     * Checks if <code>item</code> was expanded.
     *
     * @param item
     * @return
     */
    protected boolean isItemExpanded( Object item ) {
        if( item instanceof ProfileItem )
            return _expansions.get( ( (ProfileItem) item ).getHandle() ) != null;
        else if( item instanceof SummaryItem )
            return _expansions.get( ( (SummaryItem) item ).getDetailString() ) != null;
        return false;
    }

    AbstractTreeOwnerDrawLabelProvider createLabelProvider() {
        return new MyLabelProvider( this );
    }

    class MyLabelProvider extends AbstractProfileLabelProvider {

        public MyLabelProvider( ProfileTab tab ) {
            super( tab );
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jface.viewers.OwnerDrawLabelProvider#paint(org.eclipse .swt.widgets.Event, java.lang.Object)
         */
        protected void paint( Event event, Object element ) {
            // call super method to initiate variables
            super.paint( event, element );
            String text = EMPTY_STRING;
            if( element instanceof SummaryItem ) {
                // get value of each column
                SummaryItem item = (SummaryItem) element;
                switch( event.index ) {
                    case COLUM_DETAIL: {
                        text = item.getDetailString();
                        break;
                    }
                    case COLUM_PERCENT: {
                        text = item.getPercentString();
                        break;
                    }
                    case COLUM_TICKS: {
                        text = String.valueOf( item.getTicks() );
                        break;
                    }
                    case COLUM_COUNT: {
                        text = String.valueOf( item.getCount() );
                        break;
                    }
                    default:
                        text = EMPTY_STRING;
                }
            }
            if( event.index == 0 )
                drawFirstColumn( event, element, text, false );
            else
                drawText( event, text, event.x, event.y, false );

        }
    }
}
