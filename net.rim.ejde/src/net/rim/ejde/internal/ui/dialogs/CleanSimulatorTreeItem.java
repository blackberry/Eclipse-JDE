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
package net.rim.ejde.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author bkurz
 *
 */
public class CleanSimulatorTreeItem {
    public enum ItemType {
        INTERNAL_BUNDLE, EXTERNAL_BUNDLE
    }

    public enum ItemId {
        SDK, ERASE_SIMULATOR_FILES, ERASE_FILE_SYSTEM, ERASE_NON_VOLATILE_MEMORY, ERASE_REMOVABLE_MEMORY, CLEAN_SIMULATOR_DIRECTORY
    }

    public enum ItemState {
        CHECKED, GRAYED, ENABLED
    }

    private ItemType _itemType;
    private ItemId _itemId;
    private String _itemName;
    private boolean _isChecked;
    private boolean _isGrayed;
    private boolean _isEnabled;

    private CleanSimulatorTreeItem _parentItem;
    private List< CleanSimulatorTreeItem > _childItems;

    /**
     * Default constructor for an instance of CleanSimulatorTreeItem
     */
    public CleanSimulatorTreeItem() {
        this( null, null, "", new ArrayList< CleanSimulatorTreeItem >() );
    }

    /**
     * Creates an instance of CleanSimulatorTreeItem with the given parameters
     *
     * @param itemID
     * @param itemName
     */
    public CleanSimulatorTreeItem( ItemType itemType, ItemId itemID, String itemName ) {
        this( itemType, itemID, itemName, new ArrayList< CleanSimulatorTreeItem >() );
    }

    /**
     * Creates an instance of CleanSimulatorTreeItem with the given parameters
     *
     * @param itemID
     * @param itemName
     * @param childItems
     */
    public CleanSimulatorTreeItem( ItemType itemType, ItemId itemID, String itemName, CleanSimulatorTreeItem[] childItems ) {
        this( itemType, itemID, itemName, Arrays.asList( childItems ) );
    }

    /**
     * Creates an instance of CleanSimulatorTreeItem with the given parameters
     *
     * @param itemID
     * @param itemName
     * @param childItems
     */
    public CleanSimulatorTreeItem( ItemType itemType, ItemId itemID, String itemName, List< CleanSimulatorTreeItem > childItems ) {
        _itemType = itemType;
        _itemId = itemID;
        _itemName = itemName;
        _isChecked = false;
        _isGrayed = false;
        _isEnabled = true;
        _childItems = new ArrayList< CleanSimulatorTreeItem >();
        for( CleanSimulatorTreeItem childItem : childItems ) {
            addChildItem( childItem );
        }
    }

    /**
     * Returns the clean simulator tree item type
     *
     * @return
     */
    public ItemType getItemType() {
        return this._itemType;
    }

    /**
     * Returns the clean simulator tree item id
     *
     * @return Tree item id
     */
    public ItemId getItemID() {
        return this._itemId;
    }

    /**
     * Returns the clean simulator tree item name
     *
     * @return Tree item name
     */
    public String getItemName() {
        return this._itemName;
    }

    /**
     * Returns the parent clean simulator tree item
     *
     * @return Parent tree item
     */
    public CleanSimulatorTreeItem getParentItem() {
        return _parentItem;
    }

    /**
     * Returns the root clean simulator tree item
     *
     * @return Root tree item
     */
    public CleanSimulatorTreeItem getRootItem() {
        if( _parentItem != null ) {
            return _parentItem.getRootItem();
        }
        return this;
    }

    /**
     * Returns an item with the specified id from the tree structure
     *
     * @param id
     * @return Clean simulator tree item
     */
    public CleanSimulatorTreeItem getChildItem( ItemId id ) {
        CleanSimulatorTreeItem foundItem;
        if( hasChildItems() ) {
            for( CleanSimulatorTreeItem childItem : _childItems ) {
                foundItem = childItem.getChildItem( id );
                if( foundItem != null ) {
                    return foundItem;
                }
            }
        }
        return this.getItemID().equals( id ) ? this : null;
    }

    /**
     * Returns all child items
     *
     * @return
     */
    public CleanSimulatorTreeItem[] getChildItems() {
        return _childItems.toArray( new CleanSimulatorTreeItem[ _childItems.size() ] );
    }

    /**
     * Returns a value indicating if this tree item is checked
     *
     * @return
     */
    public boolean isChecked() {
        return _isChecked;
    }

    /**
     * Returns a value indicating if this tree item is grayed
     *
     * @return
     */
    public boolean isGrayed() {
        return _isGrayed;
    }

    /**
     * Returns a value indicating if this tree item is enabled
     *
     * @return
     */
    public boolean isEnabled() {
        return _isEnabled;
    }

    /**
     * Sets the clean simulator tree item type
     *
     * @param itemType
     */
    public void setItemType( ItemType itemType ) {
        this._itemType = itemType;
    }

    /**
     * Sets the clean simulator tree item id
     *
     * @param itemID
     */
    public void setItemId( ItemId itemID ) {
        this._itemId = itemID;
    }

    /**
     * Sets the clean simulator tree item name
     *
     * @param name
     */
    public void setItemName( String name ) {
        _itemName = name;
    }

    /**
     * Sets the parent item
     *
     * @param item
     */
    public void setParentItem( CleanSimulatorTreeItem item ) {
        _parentItem = item;
    }

    /**
     * Sets a value indicating if the item is checked
     *
     * @param checked
     */
    public void setChecked( boolean checked ) {
        _isChecked = checked;
    }

    /**
     * Sets a value indicating if the item is grayed
     *
     * @param grayed
     */
    public void setGrayed( boolean grayed ) {
        _isGrayed = grayed;
    }

    /**
     * Sets a value indicating if the item is enabled
     *
     * @param disabled
     */
    public void setEnabled( boolean enabled ) {
        _isEnabled = enabled;
    }

    /**
     * Adds a child item
     *
     * @param item
     */
    public void addChildItem( CleanSimulatorTreeItem item ) {
        _childItems.add( item );
        item.setParentItem( this );
    }

    /**
     * Returns true if some child items are checked, false otherwise
     *
     * @return
     */
    public boolean isSomeChildChecked() {
        if( this.hasChildItems() ) {
            for( CleanSimulatorTreeItem childItem : _childItems ) {
                if( childItem.isChecked() ) {
                    return true;
                }
                childItem.isSomeChildChecked();
            }
        }
        return false;
    }

    /**
     * Sets all child items checked/unchecked based on value (recursively).
     *
     * @param checked
     */
    public void setAllChildrenChecked( boolean checked ) {
        setAllChildrenState( ItemState.CHECKED, checked );
    }

    /**
     * Sets all child items grayed/ungrayed based on value (recursively).
     *
     * @param grayed
     */
    public void setAllChildrenGrayed( boolean grayed ) {
        setAllChildrenState( ItemState.GRAYED, grayed );
    }

    /**
     * Sets all child items enabled/disabled based on value (recursively)
     *
     * @param enabled
     */
    public void setAllChildrenEnabled( boolean enabled ) {
        setAllChildrenState( ItemState.ENABLED, enabled );
    }

    private void setAllChildrenState( ItemState state, boolean value ) {
        if( this.hasChildItems() ) {
            for( CleanSimulatorTreeItem childItem : _childItems ) {
                childItem.setAllChildrenState( state, value );
            }
        }
        switch( state ) {
            case CHECKED:
                this.setChecked( value );
                break;
            case GRAYED:
                this.setGrayed( value );
                break;
            case ENABLED:
                this.setEnabled( value );
                break;
        }
    }

    /**
     * Sets all parent items checked based on value (recursively)
     *
     * @param checked
     */
    public void setAllParentChecked( boolean checked ) {
        if( this.hasParentItem() ) {
            this.getParentItem().setAllParentChecked( checked );
            this.getParentItem().setChecked( checked );
        }
    }

    /**
     * Returns true if the item has a parent item, false otherwise
     *
     * @return
     */
    public boolean hasParentItem() {
        return _parentItem != null;
    }

    /**
     *
     * @return
     */
    public boolean hasChildItems() {
        return _childItems.size() > 0;
    }

    /**
     * Returns a list of checked child items starting from the item given as a parameter
     *
     * @param item
     * @return
     */
    public static List< CleanSimulatorTreeItem > getCheckedItems( CleanSimulatorTreeItem item ) {
        return getStateItems( ItemState.CHECKED, item );
    }

    /**
     * Returns a list of grayed child items starting from the item given as a parameter
     *
     * @param item
     * @return
     */
    public static List< CleanSimulatorTreeItem > getGrayedItems( CleanSimulatorTreeItem item ) {
        return getStateItems( ItemState.GRAYED, item );
    }

    /**
     * Returns a list of disabled child items starting from the item given as a parameter
     *
     * @param item
     * @return
     */
    public static List< CleanSimulatorTreeItem > getDisabledItems( CleanSimulatorTreeItem item ) {
        return getStateItems( ItemState.ENABLED, item );
    }

    private static List< CleanSimulatorTreeItem > getStateItems( ItemState state, CleanSimulatorTreeItem item ) {
        List< CleanSimulatorTreeItem > items = new ArrayList< CleanSimulatorTreeItem >();

        if( item.hasChildItems() ) {
            for( CleanSimulatorTreeItem childItem : item.getChildItems() ) {
                items.addAll( getStateItems( state, childItem ) );
            }
        }

        switch( state ) {
            case CHECKED:
                if( item.isChecked() )
                    items.add( item );
                break;
            case GRAYED:
                if( item.isGrayed() )
                    items.add( item );
                break;
            case ENABLED:
                if( !item.isEnabled() )
                    items.add( item );
                break;
        }

        return items;
    }

    /**
     * Sets the default check state for all items
     *
     * @param item
     */
    public static void setDefaultChecked( CleanSimulatorTreeItem item ) {
        CleanSimulatorTreeItem rootItem = item.getRootItem();
        rootItem.setChecked( true );
        rootItem.getChildItem( ItemId.CLEAN_SIMULATOR_DIRECTORY ).setChecked( false );
        rootItem.getChildItem( ItemId.ERASE_SIMULATOR_FILES ).setAllChildrenChecked( true );
    }
}
