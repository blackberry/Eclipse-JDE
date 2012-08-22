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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import net.rim.ejde.internal.core.RimIDEUtil;
import net.rim.ejde.internal.ui.views.AbstractTreeOwnerDrawLabelProvider;
import net.rim.ejde.internal.ui.views.BasicDebugView;
import net.rim.ejde.internal.util.Messages;
import net.rim.ide.core.ProfileAddress;
import net.rim.ide.core.ProfileByteCode;
import net.rim.ide.core.ProfileData;
import net.rim.ide.core.ProfileItem;
import net.rim.ide.core.ProfileItemSource;
import net.rim.ide.core.ProfileLine;
import net.rim.ide.core.ProfileMethod;
import net.rim.ide.core.ProfileSourceLine;

import org.apache.log4j.Logger;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Event;

/**
 * An instance of this class is used to display information (source code and callers) of the ProfileMethod instance selected on
 * the MethodProfileTab.
 */
public class SourceProfileTab extends ProfileTab {
    private static final Logger log = Logger.getLogger( SourceProfileTab.class );
    private static final int PROFILE_METHOD = 0;
    private static final int PROFILE_SOURCE_LINE = 1;
    private static final int PROFILE_BYTE_CODE = 2;
    private static final int UNRECOGINZED_TYPE = -1;
    private ProfileData _pd;
    // The ProfileMethod instance represented by this tab
    private ProfileMethod _profileItem;
    private Object _methodHandle;
    // IPath of the source file of the ProfileMethod represented by this tab
    private IPath _sourcePath;
    // IFileStore of the source file of the ProfileMethod represented by this
    // tab
    private IFileStore _fileStore;

    /**
     * Constructs an instance of SourceProfileTab.
     *
     * @param view
     *            An instance of ProfileView on which this tab will be displayed.
     */
    public SourceProfileTab( ProfilerView view ) {
        super( view );
        _historyIndex = 0;
        _history = new Vector< History >();
        getTabItem().setText( Messages.SourceProfileTab_SOURCE_TAB_TITLE );
        _displayLevelofTab = PROFILE_BYTECODE_LEVEL;
    }

    /**
     * Initializes the tab.
     */
    void initializeTableViewer() {
        super.initializeTableViewer();
        _tableViewer.getTable().addMouseListener( new MyMouseAdapter() );
    }

    /**
     * Gets the path of source file of .
     *
     * @return path of source file of <code>pi</code>, or <code>null</code> will be returned if no path information is found.
     */
    IPath setSourcePath() {
        Enumeration enumeration = _profileItem.getChildrenKeys();
        if( enumeration == null )
            return null;
        ProfileItem childPI = _profileItem.getChild( enumeration.nextElement() );
        ProfileSourceLine sourceLine = childPI.getLineHandle();
        if( sourceLine == null )
            return null;
        return new Path( childPI.getLineHandle().getFileName() );
    }

    public int getIndent( Object obj ) {
        if( ProfileTab.getDisplayLevel( obj ) < _displayLevelofTab )
            return UNRECOGINZED_TYPE;
        if( obj instanceof ProfileMethod )
            return PROFILE_METHOD;
        if( obj instanceof Callers )
            return PROFILE_METHOD;
        else if( obj instanceof Caller )
            return PROFILE_SOURCE_LINE;
        else if( obj instanceof ProfileLine )
            return PROFILE_SOURCE_LINE;
        else if( obj instanceof ProfileAddress )
            return PROFILE_BYTE_CODE;
        else if( obj instanceof ProfileByteCode )
            return PROFILE_BYTE_CODE;
        else
            return UNRECOGINZED_TYPE;

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
        else if( item instanceof Callers )
            return _expansions.get( ( (Callers) item ).getParent() ) != null;
        return false;
    }

    Object[] getChildren( Object obj ) {
        if( obj instanceof ProfileItem ) {
            if( obj instanceof ProfileMethod )
                return sortedElements( (ProfileItemSource) obj, null );
            return ( (ProfileItem) obj ).sortedChildren( _comparator );
        } else if( obj instanceof Callers )
            return ( (Callers) obj ).getChildren();
        return new Object[ 0 ];
    }

    int getChildrenCount( Object obj ) {
        if( obj instanceof ProfileItem )
            return ( (ProfileItem) obj ).getChildCount();
        else if( obj instanceof Callers )
            return ( (Callers) obj ).getChildren().length;
        return 0;
    }

    Object[] handleExpandedItems( Object[] data ) {
        if( data == null )
            return new Object[ 0 ];
        Object[] newData = data;
        for( int i = PROFILE_METHOD; i <= PROFILE_BYTE_CODE; i++ ) {
            for( Object object : _expansions.entrySet() ) {
                Object value = ( (Entry) object ).getValue();
                if( getIndent( value ) == i )
                    newData = expandItem( newData, value );
            }
        }
        return newData;
    }

    /**
     * @see ProfileTab#displayData(ProfileItem pi).
     *
     * @param pi
     *            An instance of ProfileItem.
     */
    /**
     * Displays information of <code>pis</code> on a ProfileTab.
     *
     * @param pis
     *            An instance of ProfileItemSource.
     */
    void displayData( ProfileItemSource pis ) {
        if( pis == null && _methodHandle == null )
            return;
        if( pis instanceof ProfileMethod ) {
            Object handle = ( (ProfileMethod) pis ).getHandle();
            if( !handle.equals( _methodHandle ) )
                _methodHandle = handle;
            _profileItem = (ProfileMethod) pis;
            displayData( _profileItem );
        } else if( pis instanceof ProfileData )
            displayData( (ProfileData) pis );
    }

    private void displayData( ProfileMethod pm ) {
        clearDisplay();
        if( pm == null )
            return;
        _sourcePath = setSourcePath();
        if( _sourcePath != null ) {
            IFileSystem fileSystem = EFS.getLocalFileSystem();
            _fileStore = fileSystem.getStore( _sourcePath );
        } else
            _fileStore = null;
        _data = new Object[ 2 ];
        Callers callers = new Callers( pm );
        callers.setChildren( sortedElements( pm.getCallers(), _comparator ) );
        _data[ 0 ] = callers;
        _data[ 1 ] = pm;
        _data = handleExpandedItems( _data );
        displayData( _data );
        refreshForwardButton();
        refreshBackwardButton();
    }

    private void displayData( ProfileData pd ) {
        if( _methodHandle == null )
            return;
        _pd = pd;
        _profileItem = pd.getProfileMethod( _methodHandle );
        displayData( _profileItem );
    }

    public void setProfildData( ProfileData pd ) {
        _pd = pd;
    }

    private void clearDisplay() {
        _tableViewer.getTable().removeAll();
    }

    void displayHistory( History history ) {
        // reset expansion information
        HashMap< Object, Object > map = history.getExpansionMap();
        if( map != null )
            _expansions = history.getExpansionMap();
        // display data
        displayData( history.getProfileItemSource() );
        // reset selection information
        Object selection = history.getSelectedItem();
        if( selection != null ) {
            _tableViewer.setSelection( (ISelection) selection );
        }
    }

    void setHistory( ProfileItemSource pis ) {
        for( int i = _historyIndex + 1; i < _history.size(); i++ )
            _history.removeElementAt( i );
        // set the selection and expansion information of current ProfileItem
        History history;
        if( _history.size() != 0 ) {
            history = _history.get( _historyIndex );
            history.setExpansionMap( (HashMap< Object, Object >) _expansions.clone() );
            int selectionIndex = _tableViewer.getTable().getSelectionIndex();
            if( selectionIndex >= 0 )
                // history.setSelectedItem(_tableViewer.getTable().getItem(
                // selectionIndex).getData());
                history.setSelectedItem( _tableViewer.getSelection() );
        }
        // create a new history instance for the ProfileItem which is going to
        // be shown
        history = new History();
        history.setProfileItemSource( pis );
        _history.add( history );
        _historyIndex = _history.size() - 1;
        refreshForwardButton();
        refreshBackwardButton();
    }

    public void forward() {
        if( _tabFolder.getSelectionIndex() == ProfilerView.INDEX_OF_TAB_SOURCE )
            if( _history.size() <= 1 )
                System.out.println( "History size is not bigger than 1." );
        if( _history.size() != 0 && _history.size() > _historyIndex ) {
            History history = _history.get( _historyIndex );
            history.setExpansionMap( (HashMap< Object, Object >) _expansions.clone() );
            int selectionIndex = _tableViewer.getTable().getSelectionIndex();
            if( selectionIndex >= 0 )
                // history.setSelectedItem(_tableViewer.getTable().getItem(
                // selectionIndex).getData());
                history.setSelectedItem( _tableViewer.getSelection() );
        }
        _historyIndex++;
        if( _historyIndex == _history.size() )
            System.out.println( "No more forward data." );
        displayHistory( _history.get( _historyIndex ) );
    }

    public void backward() {
        if( _history.size() != 0 && _history.size() > _historyIndex ) {
            History history = _history.get( _historyIndex );
            history.setExpansionMap( (HashMap< Object, Object >) _expansions.clone() );
            int selectionIndex = _tableViewer.getTable().getSelectionIndex();
            if( selectionIndex >= 0 )
                // history.setSelectedItem(_tableViewer.getTable().getItem(
                // selectionIndex).getData());
                history.setSelectedItem( _tableViewer.getSelection() );
        }
        _historyIndex--;
        if( _historyIndex < 0 )
            System.out.println( "No more backward data." );
        displayHistory( _history.get( _historyIndex ) );
    }

    /**
     * Overrides the ProfileTab#sortedElement(ProfileItemSource source). Added function that fetches string source code for each
     * ProfileLine.
     *
     * @param source
     *            ProfileItemSource instance.
     * @param comparator
     *            Comparator instance used to sort children items of <code>source</code>.
     * @return An array of ProfileItem.
     */
    ProfileItem[] sortedElements( ProfileItemSource source, Comparator comparator ) {
        // sort the children ProfileItems of source
        ProfileItem[] profileItems = super.sortedElements( source, null );
        if( source instanceof ProfileMethod )
            // fetch source line string for each ProfileLine
            setSourceLineString( profileItems );
        return profileItems;
    }

    /**
     * Fetches corresponding source line string for each ProfileItem in <code>profileItems</code>. The ProfileItems in
     * <code>profileItems</code> are supposed to be sorted by line number (ascent).
     *
     * @param profileItems
     *            Array of ProfileItem.
     */
    void setSourceLineString( ProfileItem[] profileItems ) {
        if( profileItems == null || profileItems.length == 0 )
            return;
        if( _sourcePath == null )
            return;
        BufferedReader reader = null;
        try {
            // create the BufferedReader for the source code file
            reader = new BufferedReader( new FileReader( _sourcePath.toFile() ) );
        } catch( FileNotFoundException e ) {
            log.error( e.getMessage() );
        }
        ProfileSourceLine psl = null;
        try {
            int currentLineIndex = 0;
            for( int i = 0; i < profileItems.length; i++ ) {
                psl = profileItems[ i ].getLineHandle();
                if( reader == null )
                    psl.setLine( Messages.SourceProfileTab_NO_SOURCE_MESSAGE );
                else {
                    int lineNumber = psl.getLineNumber();
                    String stringLine = null;
                    // read the corresponding source line string
                    for( ; currentLineIndex < lineNumber; currentLineIndex++ )
                        try {
                            stringLine = reader.readLine();
                        } catch( IOException e ) {
                            log.error( e.getMessage() );
                            try {
                                reader.close();
                            } catch( IOException e1 ) {
                                log.error( e1.getMessage() );
                            }
                        }
                    psl.setLine( stringLine == null ? Messages.SourceProfileTab_SOURCE_NOT_FOUND_MESSAGE : stringLine );
                }
            }
            if( reader != null )
                reader.close();
        } catch( IOException e ) {
            log.error( e.getMessage() );
        }
    }

    void refreshForwardButton() {
        if( _history.size() > 1 && _historyIndex < ( _history.size() - 1 ) )
            _profilerView.enableActions( BasicDebugView.FORWARD_BUTTON, true );
        else
            _profilerView.enableActions( BasicDebugView.FORWARD_BUTTON, false );
    }

    void refreshBackwardButton() {
        if( _history.size() > 1 && _historyIndex > 0 )
            _profilerView.enableActions( BasicDebugView.BACKWARD_BUTTON, true );
        else
            _profilerView.enableActions( BasicDebugView.BACKWARD_BUTTON, false );
    }

    class MyMouseAdapter extends MouseAdapter {

        public void mouseDoubleClick( MouseEvent e ) {
            // Get selected TableItem
            int index = _tableViewer.getTable().getSelectionIndex();
            if( index < 0 )
                // if nothing selected, return
                return;
            // its a single-select tree
            Object obj = _tableViewer.getTable().getItem( index ).getData();
            if( obj instanceof ProfileLine && _fileStore != null )
                // if selected TableItem present a ProfileLine instance
                // open the corresponding source file in an editor and
                // highlight the line
                RimIDEUtil.openSourceFile( _fileStore, ( (ProfileLine) obj ).getLineHandle().getLineNumber() );
            else if( obj instanceof ProfileItem ) {
                // if selected TableItem present a ProfileItem instance
                // get the ProfileMethod of it and display the source code
                // of the ProfileMethod.
                if( _profilerView.getProfileData() == null ) {
                    MessageDialog.openError( _profilerView.getSite().getShell(), Messages.SourceProfileTab_PROFILE_VIEW_TITLE,
                            NLS.bind( Messages.SourceProfileTab_PROFILE_DATA_IS_NULL_MSG, obj.toString() ) );
                    return;
                }
                ProfileMethod pm = _profilerView.getProfileData().getProfileMethod( (ProfileItem) obj );
                if( pm != null )
                    _profilerView.displaySourceData( pm );
            } else if( obj instanceof Caller ) {
                ProfileAddress address = ( (Caller) obj ).getProfileAddress();
                if( _profilerView.getProfileData() == null ) {
                    MessageDialog.openError( _profilerView.getSite().getShell(), Messages.SourceProfileTab_PROFILE_VIEW_TITLE,
                            NLS.bind( Messages.SourceProfileTab_PROFILE_DATA_IS_NULL_MSG, address.toString() ) );
                    return;
                }
                _profilerView.displaySourceData( _profilerView.getProfileData().getProfileMethod( address ) );
            }
        }
    }

    /**
     * @see ProfileTab#createLabelProvider()
     */
    AbstractTreeOwnerDrawLabelProvider createLabelProvider() {
        AbstractTreeOwnerDrawLabelProvider provider = new MyLabelProvider( this );
        provider.setDiaplsyLevel( PROFILE_BYTECODE_LEVEL );
        return provider;
    }

    class MyLabelProvider extends AbstractProfileLabelProvider {

        public MyLabelProvider( ProfileTab tab ) {
            super( tab );
        }

        @Override
        public int getIndent( Object obj ) {
            return this._tab.getIndent( obj );
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
            boolean isCaller = element instanceof Caller;
            if( element instanceof ProfileItem || isCaller ) {
                // get value of each column
                ProfileItem pi;
                if( isCaller )
                    pi = ( (Caller) element ).getProfileAddress();
                else
                    pi = (ProfileItem) element;
                long ticks = pi.getTicks();
                switch( event.index ) {
                    case COLUM_DETAIL: {
                        if( pi instanceof ProfileLine ) {
                            ProfileSourceLine sourceLine = ( (ProfileLine) pi ).getLineHandle();
                            if( sourceLine != null )
                                // display source line
                                text = (String) ( (ProfileLine) pi ).getLineHandle().getLine();
                            else
                                text = Messages.SourceProfileTab_NO_SOURCE_MESSAGE;
                        } else
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
            } else if( element instanceof Callers )
                text = ( (Callers) element ).getLabel();

            if( event.index == 0 )
                drawFirstColumn( event, element, text, false );
            else
                drawText( event, text, event.x, event.y, false );
        }
    }
}
