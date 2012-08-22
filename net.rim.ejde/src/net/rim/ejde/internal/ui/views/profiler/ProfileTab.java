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

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.ui.CompositeFactory;
import net.rim.ejde.internal.ui.views.AbstractTreeOwnerDrawLabelProvider;
import net.rim.ejde.internal.ui.views.IDebugLazyContentProvider;
import net.rim.ejde.internal.util.DebugUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ide.RIA;
import net.rim.ide.RIA.ProfileType;
import net.rim.ide.core.ProfileAddress;
import net.rim.ide.core.ProfileByteCode;
import net.rim.ide.core.ProfileData;
import net.rim.ide.core.ProfileItem;
import net.rim.ide.core.ProfileItemSource;
import net.rim.ide.core.ProfileLine;
import net.rim.ide.core.ProfileMethod;
import net.rim.ide.core.ProfileModule;

import org.eclipse.jface.viewers.OwnerDrawLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * This is the super class of all customized tab classes which are used to display summary, method, and source information of a
 * ProfileData instance.
 *
 */
abstract public class ProfileTab implements IConstants {
    // Display levels
    final static int PROFILE_DATA_LEVEL = 5;

    final static int PROFILE_MODULE_LEVEL = 4;

    final static int PROFILE_METHOD_LEVEL = 3;

    final static int PROFILE_LINE_LEVEL = 2;

    final static int PROFILE_BYTECODE_LEVEL = 1;

    final static int UNREGOINZED_LEVEL = 0;

    // Column index
    final static int COLUM_DETAIL = 0;

    final static int COLUM_PERCENT = 1;

    final static int COLUM_TICKS = 2;

    final static int COLUM_COUNT = 3;

    final static private DecimalFormat _percentFormat = new DecimalFormat( Messages.ProfileTab_0 );

    private TabItem _tabItem;

    ProfilerView _profilerView;

    TabFolder _tabFolder;

    TableViewer _tableViewer;

    TableColumn[] _columns = new TableColumn[ 4 ];

    long _total;

    int _displayLevelofTab = -1;

    Comparator _comparator = _ticksComparator;

    protected AbstractTreeOwnerDrawLabelProvider _labelProvider;

    protected IDebugLazyContentProvider _contentProvider;

    // map which stores the objects which are expanded on the view
    protected HashMap< Object, Object > _expansions = new HashMap< Object, Object >();

    // data which will be set to the content provider of a tab to be displayed
    protected Object[] _data;

    // history information
    protected int _historyIndex;
    protected Vector< History > _history;

    /**
     * Constructs a ProfileTab instance.
     *
     * @param view
     */
    public ProfileTab( ProfilerView view ) {
        _profilerView = view;
        _tabFolder = _profilerView.getTabFolder();
        // create a TabItem instance
        _tabItem = new TabItem( _tabFolder, SWT.NONE );
        initializeTableViewer();
    }

    /**
     * Initializes the table viewer.
     */
    void initializeTableViewer() {
        // create a TableViewer instance
        Composite composite = CompositeFactory.gridComposite( _tabFolder, 1 );
        _tableViewer = new TableViewer( composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.VIRTUAL | SWT.SINGLE
                | SWT.FULL_SELECTION );
        _tableViewer.getTable().setLayoutData( new GridData( GridData.FILL_BOTH ) );
        _tableViewer.getTable().setHeaderVisible( true );
        _tableViewer.getTable().setLinesVisible( true );
        _tableViewer.getTable().addMouseListener( new MyMouseAdapter() );
        _tableViewer.setUseHashlookup( true );
        _tabItem.setControl( composite );
        _contentProvider = new MyContentProvider( _tableViewer );
        _tableViewer.setContentProvider( _contentProvider );
        _labelProvider = createLabelProvider();
        _tableViewer.setLabelProvider( _labelProvider );
        OwnerDrawLabelProvider.setUpOwnerDraw( _tableViewer );
        // create the table columns
        createTableColumns();
    }

    private void createTableColumns() {
        _columns[ COLUM_DETAIL ] = new TableColumn( _tableViewer.getTable(), SWT.CENTER );
        _columns[ COLUM_DETAIL ].setText( Messages.ProfileTab_DETAILS_COLUMN_TITLE );
        _columns[ COLUM_DETAIL ].setWidth( 400 );
        _columns[ COLUM_DETAIL ].setResizable( true );
        _columns[ COLUM_PERCENT ] = new TableColumn( _tableViewer.getTable(), SWT.RIGHT );
        _columns[ COLUM_PERCENT ].setText( Messages.ProfileTab_PERCENT_COLUMN_TITLE );
        _columns[ COLUM_PERCENT ].setWidth( 150 );
        _columns[ COLUM_PERCENT ].setResizable( true );
        _columns[ COLUM_TICKS ] = new TableColumn( _tableViewer.getTable(), SWT.RIGHT );
        _columns[ COLUM_TICKS ].setText( getNameOfType( _profilerView.getWhatToProfile() ) );
        _columns[ COLUM_TICKS ].setWidth( 150 );
        _columns[ COLUM_TICKS ].setResizable( true );
        _columns[ COLUM_COUNT ] = new TableColumn( _tableViewer.getTable(), SWT.RIGHT );
        _columns[ COLUM_COUNT ].setText( Messages.ProfileTab_COUNT_COLUMN_TITLE );
        _columns[ COLUM_COUNT ].setWidth( 150 );
        _columns[ COLUM_COUNT ].setResizable( true );
    }

    /**
     * Removes <code>item</code> from expansion map.
     *
     * @param item
     */
    protected void removeFromExpansion( Object item ) {
        if( item instanceof ProfileItem )
            _expansions.remove( ( (ProfileItem) item ).getHandle() );
        else if( item instanceof AllMethods )
            _expansions.remove( ( (AllMethods) item ).getLabel() );
        else if( item instanceof Callers )
            _expansions.remove( ( (Callers) item ).getParent() );
        else if( item instanceof SummaryItem )
            _expansions.remove( ( (SummaryItem) item ).getDetailString() );
    }

    /**
     * Adds <code>item</code> into expansion map.
     *
     * @param item
     */
    protected void putIntoExpansion( Object item ) {
        if( item instanceof ProfileItem )
            _expansions.put( ( (ProfileItem) item ).getHandle(), item );
        else if( item instanceof AllMethods )
            _expansions.put( ( (AllMethods) item ).getLabel(), item );
        else if( item instanceof Callers )
            _expansions.put( ( (Callers) item ).getParent(), item );
        else if( item instanceof SummaryItem )
            _expansions.put( ( (SummaryItem) item ).getDetailString(), item );
    }

    /**
     * Clears expansion map.
     */
    protected void clearExpansion() {
        _expansions.clear();
    }

    /**
     * Clears history vector.
     */
    protected void clearHistory() {
        _historyIndex = 0;
        if( _history != null )
            _history.clear();
    }

    public Object[] getData() {
        return _data;
    }

    /**
     * Checks if <code>item</code> was expanded. This method needs to be overridden in sub-classes.
     *
     * @param item
     * @return
     */
    protected boolean isItemExpanded( Object item ) {
        return false;
    }

    /**
     * Updates the title of the type column (what to profile).
     */
    public void updateTypeColumeTitle() {
        _columns[ COLUM_TICKS ].setText( getNameOfType( _profilerView.getWhatToProfile() ) );
    }

    private String getNameOfType( int id ) {
        if( DebugUtils.isRIMDebuggerRunning() ) {
            RIA ria = RIA.getCurrentDebugger();
            if( ria != null ) {
                // MKS 2486071
                String debugAttachedTo = ria.getDebugAttachTo();
                if( debugAttachedTo != null && !debugAttachedTo.isEmpty() ) {
                    ProfileType[] types = ria.profileGetTypes();
                    for( int i = 0; i < types.length; i++ )
                        if( types[ i ].getId() == id )
                            return types[ i ].getDescription();
                }
            }
        }
        return IConstants.EMPTY_STRING;
    }

    /**
     * Sets the comparator of this tab.
     *
     * @param comparator
     */
    void setComparator( Comparator comparator ) {
        if( _comparator.equals( comparator ) )
            return;
        _comparator = comparator;
        _profilerView.displayProfileData( new ProfileTab[] { this } );
    }

    /**
     * Gets the TabItem control of this ProfileTab instance.
     *
     * @return The TabItem control of this ProfileTab instance.
     */
    public TabItem getTabItem() {
        return _tabItem;
    }

    /**
     * Calculates the percentage of <code>num</code> against the total.
     *
     * @param num
     * @return
     */
    public String getPercent( long num ) {
        if( _total == 0 )
            return _percentFormat.format( (double) 0 );
        return _percentFormat.format( (double) num / _total );
    }

    /**
     * Set the total number of ticks of the ProfileData presented by this ProfileTab instance.
     *
     * @param total
     *            The total number of the ticks.
     */
    public void setTotal( long total ) {
        _total = total;
    }

    void expandItem( Object obj ) {
        if( _data == null || _data.length == 0 )
            return;
        _data = expandItem( _data, obj );
        displayData( _data );
        putIntoExpansion( obj );
    }

    Object[] expandItem( Object[] data, Object item ) {
        Object[] children = getChildren( item );
        int childrenCount = children.length;
        Object[] newData = new Object[ data.length + childrenCount ];
        int index = getItemIndex( data, item );
        if( index < 0 )
            return data;
        // copy the items up to the given item into new data
        System.arraycopy( data, 0, newData, 0, index + 1 );
        // copy the children of the given item into new data
        System.arraycopy( children, 0, newData, index + 1, childrenCount );
        // copy the items after the given item into new data
        System.arraycopy( data, index + 1, newData, index + childrenCount + 1, data.length - index - 1 );
        return newData;
    }

    void collapseItem( Object obj ) {
        if( _data == null || _data.length == 0 )
            return;
        _data = collapseItem( _data, obj );
        displayData( _data );
        removeFromExpansion( obj );
    }

    private Object[] collapseItem( Object[] data, Object item ) {
        int childrenCount = 0;
        childrenCount = numChildrenToRemove( item );
        Object[] newData = new Object[ data.length - childrenCount ];
        int index = getItemIndex( data, item );
        if( index < 0 )
            return data;
        // copy the items up to the given item into new data
        System.arraycopy( data, 0, newData, 0, index + 1 );
        // copy the items after the given item into new data (children are
        // skipped)
        System.arraycopy( data, index + childrenCount + 1, newData, index + 1, data.length - index - childrenCount - 1 );
        return newData;
    }

    private int numChildrenToRemove( Object obj ) {
        Object[] children = getChildren( obj );
        int childrenCount = children.length;
        for( int i = 0; i < children.length; i++ )
            if( isItemExpanded( children[ i ] ) ) {
                childrenCount += numChildrenToRemove( children[ i ] );
                removeFromExpansion( children[ i ] );
            }
        return childrenCount;
    }

    int getItemIndex( Object[] data, Object obj ) {
        if( data == null )
            return -1;
        for( int i = 0; i < data.length; i++ )
            if( data[ i ].equals( obj ) )
                return i;
        return -1;
    }

    void displayData( Object[] objects ) {
        if( objects == null )
            return;
        _tableViewer.getTable().setRedraw( false );
        _tableViewer.setInput( objects );
        _tableViewer.getTable().setItemCount( objects.length );
        _tableViewer.getTable().setRedraw( true );
    }

    /**
     * Gets the number of children of given <code>obj</code>. This method needs to be override by sub-classes.
     *
     * @param obj
     * @return
     */
    int getChildrenCount( Object obj ) {
        if( obj instanceof ProfileItem )
            return ( (ProfileItem) obj ).getChildCount();
        return 0;
    }

    /**
     * Gets children of given <code>obj</code> This method needs to be override by sub-classes.
     *
     * @param obj
     * @return
     */
    Object[] getChildren( Object obj ) {
        return new Object[ 0 ];
    }

    /**
     * Gets the display level of given <code>obj</code>.
     *
     * @param obj
     * @return Display level of given <code>obj</code>.
     */
    static int getDisplayLevel( Object obj ) {
        if( obj instanceof ProfileData )
            return PROFILE_DATA_LEVEL;
        if( obj instanceof ProfileModule )
            return PROFILE_MODULE_LEVEL;
        else if( obj instanceof AllMethods )
            return PROFILE_MODULE_LEVEL;
        else if( obj instanceof ProfileMethod )
            return PROFILE_METHOD_LEVEL;
        else if( obj instanceof Callers )
            return PROFILE_METHOD_LEVEL;
        else if( obj instanceof ProfileAddress )
            return PROFILE_METHOD_LEVEL;
        else if( obj instanceof ProfileLine )
            return PROFILE_LINE_LEVEL;
        else if( obj instanceof Caller )
            return PROFILE_LINE_LEVEL;
        else if( obj instanceof ProfileByteCode )
            return PROFILE_BYTECODE_LEVEL;
        else
            return UNREGOINZED_LEVEL;
    }

    /**
     * Sorts children ProfileItems of <code>source</code>.
     *
     * @param source
     *            ProfileItemSource instance.
     * @param comparator
     *            Comparator instance used to sort children items of <code>source</code>.
     * @return Array of sorted children ProfileItems of <code>source</code>.
     */
    ProfileItem[] sortedElements( ProfileItemSource source, Comparator comparator ) {
        return ProfilerView.sortedElements( source, comparator );
    }

    /**
     * Gets the TreeViewer of this tab.
     *
     * @return TreeViewer of this tab.
     */
    TableViewer getViewer() {
        return _tableViewer;
    }

    /**
     * Clears the display, history, expansions of the tab.
     *
     */
    public void clearTab( boolean clearPreferences ) {
        _tableViewer.getTable().removeAll();
        if( clearPreferences ) {
            clearExpansion();
            clearHistory();
        }
    }

    /**
     * Adds children of expanded items to <code>data</code>.
     *
     * This method should be implemented in sub-classes.
     */
    Object[] handleExpandedItems( Object[] data ) {
        return new Object[ 0 ];
    }

    /**
     * Forward history. Need to be implemented by sub-classes.
     */
    public void forward() {
    }

    /**
     * Backward history. Need to be implemented by sub-classes.
     */
    public void backward() {
    }

    /**
     * Set history information. Need to be implemented by sub-classes.
     */
    void setHistory( ProfileItemSource pis ) {
    }

    // ------ Abstract Methods ------

    /**
     * Displays information of <code>pis</code> on a ProfileTab.
     *
     * @param pis
     *            An instance of ProfileItemSource.
     */
    abstract void displayData( ProfileItemSource pis );

    abstract AbstractTreeOwnerDrawLabelProvider createLabelProvider();

    abstract int getIndent( Object obj );

    // ------ Comparators ------
    static Comparator _countComparator = new Comparator() {
        public int compare( Object o1, Object o2 ) {
            return ( (ProfileItem) o2 ).getCount() - ( (ProfileItem) o1 ).getCount();
        }
    };

    static Comparator _ticksComparator = new Comparator() {
        public int compare( Object o1, Object o2 ) {
            long c1 = ( (ProfileItem) o1 ).getTicks();
            long c2 = ( (ProfileItem) o2 ).getTicks();
            if( c2 > c1 )
                return 1;
            if( c2 < c1 )
                return -1;
            return 0;
        }
    };

    // ------ inner classes ------
    /**
     * Content provider of objects tree view.
     */
    class MyContentProvider implements IDebugLazyContentProvider {
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
            if( newInput == null )
                _models = new Object[ 0 ];
            else
                _models = (Object[]) newInput;
        }

        public Object getData() {
            return _models;
        }
    }

    class SummaryItem {
        String _detail;
        String _percent;
        long _ticks;
        long _count;

        SummaryItem( String detail, ProfileItem item ) {
            _detail = detail;
            _ticks = item.getTicks();
            _percent = getPercent( _ticks );
            _count = item.getCount();
        }

        SummaryItem( String detail, String percent, long ticks, long count ) {
            _detail = detail;
            _percent = percent;
            _ticks = ticks;
            _count = count;
        }

        String getDetailString() {
            return _detail;
        }

        String getPercentString() {
            return _percent;
        }

        long getTicks() {
            return _ticks;
        }

        long getCount() {
            return _count;
        }
    }

    class Callers {
        Object _parent;
        String _label = Messages.ProfileTab_CALLER_TITLE;
        Object[] _children;

        Callers( Object parent ) {
            _parent = parent;
        }

        void setChildren( Object[] children ) {
            _children = new Object[ children.length ];
            for( int i = 0; i < children.length; i++ )
                _children[ i ] = new Caller( (ProfileAddress) children[ i ] );
        }

        Object[] getChildren() {
            return _children;
        }

        Object getParent() {
            return _parent;
        }

        String getLabel() {
            return _label;
        }

        public boolean equals( Object obj ) {
            if( !( obj instanceof Callers ) )
                return false;
            if( !( (Callers) obj ).getParent().equals( this.getParent() ) )
                return false;
            return true;
        }
    }

    class Caller {
        ProfileAddress _address;

        Caller( ProfileAddress address ) {
            _address = address;
        }

        ProfileAddress getProfileAddress() {
            return _address;
        }

        public boolean equals( Object obj ) {
            if( !( obj instanceof Caller ) )
                return false;
            if( !( (Caller) obj ).getProfileAddress().equals( this.getProfileAddress() ) )
                return false;
            return true;
        }
    }

    class AllMethods {
        String _label;
        ProfileData _pd;

        public AllMethods( ProfileData pd ) {
            _label = RIA.getString( "ProfileAll" );
            _pd = pd;
        }

        Object[] getChildren() {
            if( _pd == null )
                return new Object[ 0 ];
            return sortedElements( _pd.getAllMethods(), _comparator );
        }

        int getChildrenCount() {
            return _pd.getAllMethods().getChildCount();
        }

        void setProfileDate( ProfileData pd ) {
            _pd = pd;
        }

        ProfileData getProfileData() {
            return _pd;
        }

        String getLabel() {
            return _label;
        }

        public boolean equals( Object obj ) {
            if( obj instanceof AllMethods )
                return true;
            return false;
        }
    }

    class MyMouseAdapter extends MouseAdapter {
        public void mouseDown( MouseEvent e ) {
            if( _profilerView.getProfileData() == null )
                return;
            TableItem selectedItem = _tableViewer.getTable().getItem( _tableViewer.getTable().getSelectionIndex() );
            if( selectedItem == null )
                return;
            // If user clicked on the [+] or [-], expand or collapse the item
            Object item = selectedItem.getData();
            Rectangle rect = _labelProvider.getImageBounds( item, selectedItem.getBounds( 0 ) );
            if( rect.contains( e.x, e.y ) && getChildrenCount( item ) != 0
                    && ( _displayLevelofTab < 0 || ProfileTab.getDisplayLevel( item ) > _displayLevelofTab ) ) {
                if( isItemExpanded( item ) )
                    collapseItem( item );
                else
                    expandItem( item );
            }
        }
    }

    class History {
        Object _selection;
        HashMap< Object, Object > _expansionMap;
        ProfileItemSource _profileItemSource;

        void setSelectedItem( Object item ) {
            _selection = item;
        }

        Object getSelectedItem() {
            return _selection;
        }

        void setExpansionMap( HashMap< Object, Object > map ) {
            _expansionMap = map;
        }

        HashMap< Object, Object > getExpansionMap() {
            return _expansionMap;
        }

        void setProfileItemSource( ProfileItemSource pis ) {
            _profileItemSource = pis;
        }

        ProfileItemSource getProfileItemSource() {
            return _profileItemSource;
        }
    }
}
