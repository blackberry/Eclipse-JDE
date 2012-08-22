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

/**
 * An instance of this class is used to display information of methods included
 * in given profile data. Information of a method includes information of the
 * module to which the method belongs, information of the method itself,
 * information of the source lines of the method, and information of byte code
 * of the source lines.
 */
import java.util.Map.Entry;

import net.rim.ejde.internal.ui.views.AbstractTreeOwnerDrawLabelProvider;
import net.rim.ejde.internal.util.Messages;
import net.rim.ide.core.ProfileData;
import net.rim.ide.core.ProfileItem;
import net.rim.ide.core.ProfileItemSource;
import net.rim.ide.core.ProfileLine;
import net.rim.ide.core.ProfileMethod;
import net.rim.ide.core.ProfileModule;

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Event;

public class MethodProfileTab extends ProfileTab {
    private static final int PROFILE_MODULE = 0;
    private static final int PROFILE_METHOD = 1;
    private static final int UNRECOGINZED_TYPE = -1;
    AllMethods _allMethods;

    /**
     * Constructs an instance of MethodProfileTab
     *
     * @param view
     *            An instance of ProfileView on which this tab will be displayed.
     */
    public MethodProfileTab( ProfilerView view ) {
        super( view );
        getTabItem().setText( Messages.MethodProfileTab_METHOD_TAB_TITLE );
        _displayLevelofTab = PROFILE_METHOD_LEVEL;
    }

    /**
     * Initializes the tab.
     */
    void initializeTableViewer() {
        // call super's method
        super.initializeTableViewer();
        _columns[ COLUM_PERCENT ].addSelectionListener( new SelectionListener() {

            public void widgetDefaultSelected( SelectionEvent e ) {
                // nothing to do

            }

            public void widgetSelected( SelectionEvent e ) {
                setComparator( _ticksComparator );
            }

        } );

        _columns[ COLUM_TICKS ].addSelectionListener( new SelectionListener() {

            public void widgetDefaultSelected( SelectionEvent e ) {
                // nothing to do

            }

            public void widgetSelected( SelectionEvent e ) {
                setComparator( _ticksComparator );
            }

        } );
        _columns[ COLUM_COUNT ].addSelectionListener( new SelectionListener() {

            public void widgetDefaultSelected( SelectionEvent e ) {
                // nothing to do

            }

            public void widgetSelected( SelectionEvent e ) {
                setComparator( _countComparator );
            }

        } );
        _tableViewer.getTable().addMouseListener( new MyMouseAdapter() );
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
        else if( item instanceof AllMethods )
            return _expansions.get( ( (AllMethods) item ).getLabel() ) != null;
        return false;
    }

    public int getIndent( Object obj ) {
        if( ProfileTab.getDisplayLevel( obj ) < _displayLevelofTab )
            return UNRECOGINZED_TYPE;
        if( obj instanceof ProfileModule )
            return PROFILE_MODULE;
        if( obj instanceof AllMethods )
            return PROFILE_MODULE;
        else if( obj instanceof ProfileMethod )
            return PROFILE_METHOD;
        else
            return UNRECOGINZED_TYPE;

    }

    Object[] getChildren( Object obj ) {
        if( obj instanceof ProfileItem )
            return ( (ProfileItem) obj ).sortedChildren( _comparator );
        else if( obj instanceof AllMethods )
            return ( (AllMethods) obj ).getChildren();
        return new Object[ 0 ];
    }

    int getChildrenCount( Object obj ) {
        if( obj instanceof ProfileItem )
            return ( (ProfileItem) obj ).getChildCount();
        else if( obj instanceof AllMethods )
            return ( (AllMethods) obj ).getChildren().length;
        return 0;
    }

    /**
     * @see ProfileTab#displayData(ProfileItemSource pis).
     *
     * @param pis
     *            An instance of ProfileData.
     */
    void displayData( ProfileItemSource pis ) {
        if( !( pis instanceof ProfileData ) )
            return;
        ProfileData pd = (ProfileData) pis;
        setTotal( pd.getTotalExecutionTicks() );
        // set pd as input and its data will be automatically displayed
        // on the tree
        Object[] children = pd.sortedChildren( _comparator );
        _data = new Object[ children.length + 1 ];
        System.arraycopy( children, 0, _data, 1, children.length );
        _allMethods = new AllMethods( pd );
        _data[ 0 ] = _allMethods;
        _data = handleExpandedItems( _data );
        displayData( _data );
    }

    Object[] handleExpandedItems( Object[] data ) {
        if( data == null )
            return new Object[ 0 ];
        Object[] newData = data;
        for( int i = PROFILE_MODULE; i <= PROFILE_METHOD; i++ ) {
            for( Object object : _expansions.entrySet() ) {
                Object value = ( (Entry) object ).getValue();
                if( getIndent( value ) == i )
                    newData = expandItem( newData, value );
            }
        }
        return newData;
    }

    /**
     * @see ProfileTab#createLabelProvider()
     */
    AbstractTreeOwnerDrawLabelProvider createLabelProvider() {
        AbstractTreeOwnerDrawLabelProvider provider = new MyLabelProvider( this );
        provider.setDiaplsyLevel( PROFILE_METHOD_LEVEL );
        return provider;
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
            if( element instanceof ProfileItem ) {
                // if the element is a ProfileItem, get value of each column
                ProfileItem pi = (ProfileItem) element;
                long ticks = pi.getTicks();
                switch( event.index ) {
                    case COLUM_DETAIL: {
                        if( pi instanceof ProfileLine )
                            // display source line
                            text = (String) ( (ProfileLine) pi ).getLineHandle().getLine();
                        else
                            text = pi.toString();
                        break;
                    }
                    case COLUM_PERCENT: {
                        text = getPercent( ticks );
                        break;
                    }
                    case COLUM_TICKS: {
                        text = String.valueOf( ticks );
                        break;
                    }
                    case COLUM_COUNT: {
                        text = String.valueOf( pi.getCount() );
                        break;
                    }
                    default:
                        text = EMPTY_STRING;
                }
            } else if( element instanceof AllMethods )
                text = ( (AllMethods) element ).getLabel();

            if( event.index == 0 )
                drawFirstColumn( event, element, text, false );
            else
                drawText( event, text, event.x, event.y, false );
        }
    }

    class MyMouseAdapter extends MouseAdapter {
        /**
         * When a TableItem which presents a ProfileLine instance is double clicked, the source file will be opened in an editor.
         */
        public void mouseDoubleClick( MouseEvent e ) {
            // Get selected TableItem
            int index = _tableViewer.getTable().getSelectionIndex();
            if( index < 0 )
                // if nothing selected, return
                return;
            // its a single-select tree
            Object obj = _tableViewer.getTable().getItem( index ).getData();
            if( obj instanceof ProfileMethod ) {
                // if selected TableItem present a ProfileMethod instance
                // display the source code of the ProfileMethod.
                _profilerView.displaySourceData( (ProfileItem) obj );
            }
        }
    }
}
