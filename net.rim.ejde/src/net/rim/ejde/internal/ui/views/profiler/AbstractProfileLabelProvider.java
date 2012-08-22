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

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.TableItem;

/**
 * This class is the abstract super class of label providers used in profile tabs.
 *
 *
 * @author zqiu
 *
 */

public abstract class AbstractProfileLabelProvider extends AbstractTreeOwnerDrawLabelProvider {
    ProfileTab _tab;

    /**
     * Constructs an instance of ProfileLabelProvider.
     *
     * @param tab
     */
    public AbstractProfileLabelProvider( ProfileTab tab ) {
        super( tab.getViewer() );
        _tab = tab;
    }

    /**
     * (non-javadoc)
     *
     * @see AbstractTreeOwnerDrawLabelProvider#calculateDisplayLevel(Object)
     */
    public int calculateDisplayLevel( Object obj ) {
        return ProfileTab.getDisplayLevel( obj );
    }

    /**
     * (non-javadoc)
     *
     * @see AbstractTreeOwnerDrawLabelProvider#getIndent(Object)
     */
    public int getIndent( Object obj ) {
        return _tab.getIndent( obj );
    }

    /**
     * (non-javadoc)
     *
     * @see AbstractTreeOwnerDrawLabelProvider#findRowAtSameIndent(Object, int)
     */
    public boolean findRowAtSameIndent( Object obj, int indent ) {
        Object[] data = _tab.getData();
        if( data == null )
            return false;
        int row = getIndex( obj ) + 1;
        for( int i = row; i < data.length; i++ ) {
            Object item = data[ i ];
            if( item == null ) {
                return false;
            }
            int indentOfChild = getIndent( item );
            if( indentOfChild < 0 )
                return false;
            if( indentOfChild < indent ) {
                return false;
            }
            if( indentOfChild == indent ) {
                return true;
            }
        }
        return false;
    }

    /**
     * (non-javadoc)
     *
     * @see AbstractTreeOwnerDrawLabelProvider#getIndex(Object)
     */
    public int getIndex( Object obj ) {
        Object item;
        if( obj instanceof TableItem )
            item = ( (TableItem) obj ).getData();
        else
            item = obj;
        return _tab.getItemIndex( _tab.getData(), item );
    }

    /**
     * (non-javadoc)
     *
     * @see AbstractTreeOwnerDrawLabelProvider#hasChildren(Object)
     */
    public boolean hasChildren( Object obj ) {
        return _tab.getChildrenCount( obj ) != 0;
    }

    /**
     * (non-javadoc)
     *
     * @see AbstractTreeOwnerDrawLabelProvider#isExpanded(Object)
     */
    public boolean isExpanded( Object obj ) {
        return _tab.isItemExpanded( obj );
    }

    /**
     * (non-javadoc)
     *
     * @see OwnerDrawLabelProvider#measure(Event, Object)
     */
    protected void measure( Event event, Object element ) {
        // TODO nothing to do

    }

}
