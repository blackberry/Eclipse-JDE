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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.ui.CompositeFactory;
import net.rim.ejde.internal.util.DebugUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ide.core.VarContentsHelper.MenuAction;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

/**
 * This class is the basic view class of the debugger views (profiler, memory states, objects etc.) All other debugger views are
 * supposed to extend from this class.
 */
abstract public class BasicDebugView extends ViewPart implements IDebugEventSetListener, ILaunchesListener2 {
    private static final Logger log = Logger.getLogger( BasicDebugView.class );

    // bit mask for tool bar buttons
    final public static int REFRESH_BUTTON = 0x0001;

    final public static int SAVE_BUTTON = 0x0002;

    final public static int CLEAR_BUTTON = 0x0004;

    final public static int OPTIONS_BUTTON = 0x0008;

    final public static int COMPARE_BUTTON = 0x0010;

    final public static int SNAPSHOT_BUTTON = 0x0020;

    final public static int GARBAGE_COLLECTION_BUTTON = 0x0040;

    final public static int FILTER_BUTTON = 0x0080;

    final public static int FORWARD_BUTTON = 0x0100;

    final public static int BACKWARD_BUTTON = 0x0200;

    final public static int RETURN_TO_START_BUTTON = 0x0400;

    final public static int RETURN_TO_END_BUTTON = 0x0800;

    final public static int SAVE_TO_XML = 0x2000;

    final public static int SAVE_RAW_TO_XML = 0x4000;

    final public static int OPEN_PROFILEVIS = 0x8000;

    final static String HPROF_EXTENSION = "hprof"; //$NON-NLS-1$

    private int _buttons;

    private boolean _isSuspended;

    protected List< Object > _toolbarActionList;

    protected List< MenuAction > _menuActionList;

    private boolean _hasData;

    private boolean _hasSnapShot;

    private Label _messageLabel;

    /**
     * Constructs an instance of this view.
     *
     * @param buttons
     *            bit mask to indicate what buttons will be created on the tool bar of this view.
     */
    public BasicDebugView( int buttons ) {
        _buttons = buttons;
        _toolbarActionList = new ArrayList< Object >();
        DebugPlugin.getDefault().addDebugEventListener( this );
        DebugPlugin.getDefault().getLaunchManager().addLaunchListener( this );
    }

    /**
     * Creates the UI of this view. This method only creates tool bar buttons on the view, and is supposed to be overridden by its
     * subclasses to create other UI parts.
     *
     * Caution: super#createPartControl(Composite parent) must be called in the beginning of the overridden method in subclasses.
     */
    public void createPartControl( Composite parent ) {
        createToolbarButtons();
        // debugger may be suspended before this view is created
        setSuspended( ContextManager.PLUGIN.isSuspended() );
        // initialize buttons
        updateToolbar();
        parent.setLayout( new GridLayout( 1, false ) );
        // create message labels
        createMessageComposite( parent );
        // create table
        Composite tableComposite = CompositeFactory.gridComposite( parent, 1 );
        tableComposite.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        // create the table view part
        createTableViewPart( tableComposite );
    }

    abstract public void createTableViewPart( Composite parent );

    public void setMessage( final String errorMessage, final boolean error ) {
        Display.getDefault().syncExec( new Runnable() {

            @Override
            public void run() {
                if( _messageLabel != null ) {
                    if( error ) {
                        _messageLabel.setForeground( Display.getCurrent().getSystemColor( SWT.COLOR_RED ) );
                    } else {
                        _messageLabel.setForeground( Display.getCurrent().getSystemColor( SWT.COLOR_BLACK ) );
                    }
                    _messageLabel.setText( errorMessage );
                }
            }

        } );

    }

    public void cleanMessage() {
        Display.getDefault().syncExec( new Runnable() {

            @Override
            public void run() {
                if( _messageLabel != null ) {
                    _messageLabel.setText( "" );
                }
            }

        } );
    }

    private void createMessageComposite( Composite parent ) {
        Composite composite = CompositeFactory.gridComposite( parent, 1 );
        // message label
        _messageLabel = new Label( composite, SWT.NONE );
        _messageLabel.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
    }

    protected void setHasData( boolean hasData ) {
        _hasData = hasData;
    }

    protected void setHasSnapshot( boolean hasSnapshot ) {
        _hasSnapShot = hasSnapshot;
    }

    protected boolean hasData() {
        return _hasData;
    }

    protected boolean hasSnapshot() {
        return _hasSnapShot;
    }

    /**
     * Creates the Image instance indicated by <code>imageId</code>.
     *
     * @param imageId
     *            <code>int</code> number which indicates the image to be created.
     * @return Image instance or <code>null</code> if some exception occurs.
     */
    static protected Image createImage( int imageId ) {
        Image image = null;
        ImageDescriptor descriptor = null;
        switch( imageId ) {
            case REFRESH_BUTTON: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID,
                        "icons/obj16/refresh_enabled.gif" );
                break;
            }
            case SAVE_BUTTON: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID,
                        "icons/obj16/save_edit_enabled.gif" );
                break;
            }
            case CLEAR_BUTTON: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons/obj16/clear.gif" );
                break;
            }
            case OPTIONS_BUTTON: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons/obj16/options.gif" );
                break;
            }
            case COMPARE_BUTTON: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons/obj16/compare.gif" );
                break;
            }
            case SNAPSHOT_BUTTON: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons/obj16/snapshot.gif" );
                break;
            }
            case GARBAGE_COLLECTION_BUTTON: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons/obj16/recycle.gif" );
                break;
            }
            case FILTER_BUTTON: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons/obj16/filter.gif" );
                break;
            }
            case FORWARD_BUTTON: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons/obj16/forward_nav.gif" );
                break;
            }
            case BACKWARD_BUTTON: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons/obj16/backward_nav.gif" );
                break;
            }
            case RETURN_TO_START_BUTTON: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID,
                        "icons/obj16/return_to_start.gif" );
                break;
            }
            case RETURN_TO_END_BUTTON: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons/obj16/return_to_end.gif" );
                break;
            }
            case SAVE_TO_XML: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID,
                        "icons/obj16/save_edit_enabled.gif" );
                break;
            }
            case SAVE_RAW_TO_XML: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID,
                        "icons/obj16/save_edit_enabled.gif" );
                break;
            }
            case OPEN_PROFILEVIS: {
                descriptor = ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons/obj16/openfile.gif" );
                break;
            }
        }
        if( descriptor != null ) {
            image = descriptor.createImage();
        }
        return image;
    }

    /**
     * Disposes created image instances.
     */
    public void dispose() {
        super.dispose();
        log.trace( "Disposing the debug view and removing the debug listener" );
        // fixed IDP350529, remove this object from the listener lists
        DebugPlugin.getDefault().removeDebugEventListener( this );
        DebugPlugin.getDefault().getLaunchManager().removeLaunchListener( this );
        // dispose all created actions
        for( Iterator iterator = _toolbarActionList.iterator(); iterator.hasNext(); ) {
            Object obj = iterator.next();
            if( obj instanceof BasicAction ) {
                BasicAction action = (BasicAction) obj;
                action.dispose();
            } else if( obj instanceof Separator ) {
                Separator separator = (Separator) obj;
                separator.dispose();
            }
        }

    }

    /**
     * Creates a tool bar Action instance indicated by <code>action</code>.
     *
     * @param action
     *            the action to be created.
     */
    private void createToolbarAction( int action ) {
        String text;
        String hint;
        switch( action ) {
            case REFRESH_BUTTON: {
                // create fresh action
                text = Messages.BasicView_REFRESH_ACTION_TITLE;
                hint = Messages.BasicView_REFRESH_ACTION_HINT;
                break;
            }
            case SAVE_BUTTON: {
                // create save action
                text = Messages.BasicView_SAVE_ACTION_TITLE;
                hint = Messages.BasicView_SAVE_ACTION_HINT;
                break;
            }
            case CLEAR_BUTTON: {
                // create clear action
                text = Messages.BasicView_CLEAR_ACTION_TITLE;
                hint = Messages.BasicView_CLEAR_ACTION_HINT;
                break;
            }
            case OPTIONS_BUTTON: {
                // create options action
                text = Messages.BasicView_OPTIONS_ACTION_TITLE;
                hint = Messages.BasicView_OPTIONS_ACTION_HINT;
                break;
            }
            case COMPARE_BUTTON: {
                // create compare action
                text = Messages.BasicView_COMPARE_ACTION_TITLE;
                hint = Messages.BasicView_COMPARE_ACTION_HINT;
                break;
            }
            case SNAPSHOT_BUTTON: {
                // create snapshot action
                text = Messages.BasicView_SNAPSHOT_ACTION_TITLE;
                hint = Messages.BasicView_SNAPSHOT_ACTION_HINT;
                break;
            }
            case GARBAGE_COLLECTION_BUTTON: {
                // create garbage collection action
                text = Messages.BasicView_GC_ACTION_TITLE;
                hint = Messages.BasicView_GC_ACTION_HINT;
                break;
            }
            case FILTER_BUTTON: {
                // create filter action
                text = Messages.BasicView_FILTER_ACTION_TITLE;
                hint = Messages.BasicView_FILTER_ACTION_HINT;
                break;
            }
            case FORWARD_BUTTON: {
                // create forward action
                text = Messages.BasicView_FORWARD_ACTION_TITLE;
                hint = Messages.BasicView_FORWARD_ACTION_HINT;
                break;
            }
            case BACKWARD_BUTTON: {
                // create backward action
                text = Messages.BasicView_BACKWARD_ACTION_TITLE;
                hint = Messages.BasicView_BACKWARD_ACTION_HINT;
                break;
            }
            case RETURN_TO_START_BUTTON: {
                // create "return to start" action
                text = Messages.BasicView_RETURN_TO_START_ACTION_TITLE;
                hint = Messages.BasicView_RETURN_TO_START_ACTION_HINT;
                break;
            }
            case RETURN_TO_END_BUTTON: {
                // create filter action
                text = Messages.BasicView_GO_TO_END_ACTION_TITLE;
                hint = Messages.BasicView_GO_TO_END_ACTION_HINT;
                break;
            }
            case SAVE_TO_XML: {
                // dump heap
                text = Messages.BasicView_SAVE_XML_ACTION_TITLE;
                hint = Messages.BasicView_SAVE_XML_ACTION_HINT;
                break;
            }
            case SAVE_RAW_TO_XML: {
                // dump heap
                text = Messages.BasicView_SAVE_RAW_XML_ACTION_TITLE;
                hint = Messages.BasicView_SAVE_RAW_XML_ACTION_HINT;
                break;
            }
            case OPEN_PROFILEVIS: {
                // dump heap
                text = Messages.BasicView_OPEN_PROFILEVIS_ACTION_TITLE;
                hint = Messages.BasicView_OPEN_PROFILEVIS_ACTION_HINT;
                break;
            }
            default:
                return;
        }
        _toolbarActionList.add( new BasicAction( this, text, action, hint ) );
    }

    /**
     * Creates buttons on the tool bar of this view.
     */
    private void createToolbarButtons() {
        // create and add buttons to tool bar
        IToolBarManager toolBarMgr = getViewSite().getActionBars().getToolBarManager();
        // create actions
        if( ( _buttons & RETURN_TO_START_BUTTON ) != 0 ) {
            createToolbarAction( RETURN_TO_START_BUTTON );
        }
        if( ( _buttons & BACKWARD_BUTTON ) != 0 ) {
            createToolbarAction( BACKWARD_BUTTON );
        }
        if( ( _buttons & FORWARD_BUTTON ) != 0 ) {
            createToolbarAction( FORWARD_BUTTON );
        }
        if( ( _buttons & RETURN_TO_END_BUTTON ) != 0 ) {
            createToolbarAction( RETURN_TO_END_BUTTON );
        }

        if( ( _buttons & RETURN_TO_START_BUTTON ) != 0 || ( _buttons & BACKWARD_BUTTON ) != 0
                || ( _buttons & FORWARD_BUTTON ) != 0 || ( _buttons & RETURN_TO_END_BUTTON ) != 0 )
            _toolbarActionList.add( new Separator( "Navagition Group" ) );

        if( ( _buttons & REFRESH_BUTTON ) != 0 ) {
            createToolbarAction( REFRESH_BUTTON );
        }
        if( ( _buttons & SAVE_BUTTON ) != 0 ) {
            createToolbarAction( SAVE_BUTTON );
        }
        if( ( _buttons & CLEAR_BUTTON ) != 0 ) {
            createToolbarAction( CLEAR_BUTTON );
        }
        if( ( _buttons & OPTIONS_BUTTON ) != 0 ) {
            createToolbarAction( OPTIONS_BUTTON );
        }
        if( ( _buttons & FILTER_BUTTON ) != 0 ) {
            createToolbarAction( FILTER_BUTTON );
        }
        if( ( _buttons & COMPARE_BUTTON ) != 0 ) {
            createToolbarAction( COMPARE_BUTTON );
        }
        if( ( _buttons & SNAPSHOT_BUTTON ) != 0 ) {
            createToolbarAction( SNAPSHOT_BUTTON );
        }
        if( ( _buttons & GARBAGE_COLLECTION_BUTTON ) != 0 ) {
            createToolbarAction( GARBAGE_COLLECTION_BUTTON );
        }
        if( ( _buttons & SAVE_TO_XML ) != 0 ) {
            createToolbarAction( SAVE_TO_XML );
        }
        if( ( _buttons & SAVE_RAW_TO_XML ) != 0 ) {
            createToolbarAction( SAVE_RAW_TO_XML );
        }
        if( ( _buttons & OPEN_PROFILEVIS ) != 0 ) {
            createToolbarAction( OPEN_PROFILEVIS );
        }
        // add the buttons to the tool bar
        for( Iterator iterator = _toolbarActionList.iterator(); iterator.hasNext(); ) {
            Object obj = iterator.next();
            if( obj instanceof BasicAction )
                toolBarMgr.add( (BasicAction) obj );
            else if( obj instanceof IContributionItem )
                toolBarMgr.add( (IContributionItem) obj );
        }
    }

    public void setFocus() {
        // nothing to do
    }

    /**
     * @see IDebugEventSetListener#handleDebugEvents(DebugEvent[]).
     *
     */
    public void handleDebugEvents( DebugEvent[] events ) {
        if( events == null || events.length == 0 )
            return;

        for( int i = 0; i < events.length; i++ ) {
            if( !DebugUtils.isFromRIMLaunch( events[ i ] ) ) {
                continue;
            }
            if( events[ i ].getKind() == DebugEvent.SUSPEND && !_isSuspended ) {
                _isSuspended = true;
                updateToolbar();
            }

            else if( events[ i ].getKind() == DebugEvent.RESUME && _isSuspended ) {
                _isSuspended = false;
                updateToolbar();
            }
            handleRIMDebugEvent( events[ i ] );
        }

    } // method handleDebugEvents

    protected void handleRIMDebugEvent( DebugEvent event ) {
        // do nothing here
    }

    // ------ implementation of methods of ILaunchesListener2 ------

    /**
     * @see ILaunchesListener2#launchesTerminated(ILaunch[]).
     */
    public void launchesTerminated( ILaunch[] launches ) {
        debugTerminated( launches );
    }

    /**
     * @see ILaunchesListener#launchesRemoved(ILaunch[]).
     */
    public void launchesRemoved( ILaunch[] launches ) {
        // in hot-swap, only launch removed event is fired, there is no launch terminated event.
        debugTerminated( launches );
    }

    /**
     * @see ILaunchesListener#launchesAdded(ILaunch[]).
     */
    public void launchesAdded( ILaunch[] launches ) {
    }

    /**
     * @see ILaunchesListener#launchesChanged(ILaunch[]).
     */
    public void launchesChanged( ILaunch[] launches ) {
    }

    /**
     * Set actions indicated by <code>actions</code> as <code>enabled</code>.
     *
     * @param actions
     *            bit mask which indicates what actions will be enabled/disabled.
     * @param enabled
     *            <code>true</code> enable the actions indicated by <code>actions</code>, <code>false</code> otherwise.
     */
    public void enableActions( int actions, boolean enabled ) {
        for( Iterator iterator = _toolbarActionList.iterator(); iterator.hasNext(); ) {
            Object obj = iterator.next();
            if( !( obj instanceof BasicAction ) )
                continue;
            BasicAction action = (BasicAction) obj;
            if( ( action.getActionCode() & actions ) != 0 && action.isEnabled() != enabled )
                action.setEnabled( enabled );
        }

    }

    /**
     * Checks whether the debugger is suspended.
     *
     * @return <code>true</code>if the debugger is suspended, <code>false</code> otherwise.
     */
    public boolean isSuspended() {
        return _isSuspended;
    }

    /**
     * Sets whether the debugger is suspended or not.
     *
     * @param suspeneded
     *            <code>true</code>if the debugger is suspended, <code>false</code> otherwise.
     */
    public void setSuspended( boolean suspeneded ) {
        _isSuspended = suspeneded;
    }

    /**
     * Despatch task when an action is executed (a button is clicked).
     *
     * @param action
     *            the action executed.
     * @throws CoreException
     */
    protected void run( BasicAction action ) throws CoreException {
        switch( action.getActionCode() ) {
            case REFRESH_BUTTON: {
                refresh();
                return;
            }
            case SAVE_BUTTON: {
                save();
                return;
            }
            case CLEAR_BUTTON: {
                clear();
                return;
            }
            case OPTIONS_BUTTON: {
                setOptions();
                return;
            }
            case COMPARE_BUTTON: {
                compare();
                break;
            }
            case SNAPSHOT_BUTTON: {
                snapshot();
                return;
            }
            case GARBAGE_COLLECTION_BUTTON: {
                gc();
                return;
            }
            case FILTER_BUTTON: {
                filter();
                return;
            }
            case FORWARD_BUTTON: {
                forward();
                return;
            }
            case BACKWARD_BUTTON: {
                backward();
                return;
            }
            case RETURN_TO_START_BUTTON: {
                returnToStart();
                return;
            }
            case RETURN_TO_END_BUTTON: {
                returnToEnd();
                return;
            }
            case SAVE_TO_XML: {
                saveXML();
                return;
            }
            case SAVE_RAW_TO_XML: {
                saveRawToXML();
                return;
            }
            case OPEN_PROFILEVIS: {
                openProfileVis();
                return;
            }
            default:
                return;
        }
    }

    /**
     * Debug session is terminated.
     */
    private void debugTerminated( ILaunch[] launches ) {
        List< ILaunch > rimLaunches = DebugUtils.getRIMLaunches( launches );
        if( rimLaunches.size() == 0 ) {
            return;
        }
        _isSuspended = false;
        Display.getDefault().asyncExec( new Runnable() {
            public void run() {
                clear();
            }
        } );
        RIMDebugTerminated( rimLaunches.toArray( new ILaunch[ rimLaunches.size() ] ) );
    }

    public void RIMDebugTerminated( ILaunch[] launches ) {
        // do nothing here
    }

    /**
     * Enables/Disables the buttons on the tool bar.
     */
    public void updateToolbar() {
        enableActions( OPTIONS_BUTTON, optionsEnabled() );
        if( isSuspended() ) {
            enableActions( REFRESH_BUTTON | GARBAGE_COLLECTION_BUTTON | FILTER_BUTTON, true );
            if( _hasSnapShot )
                enableActions( COMPARE_BUTTON, true );
            else
                enableActions( COMPARE_BUTTON, false );
        } else
            enableActions( REFRESH_BUTTON | GARBAGE_COLLECTION_BUTTON | COMPARE_BUTTON | FILTER_BUTTON | BACKWARD_BUTTON
                    | FORWARD_BUTTON | RETURN_TO_START_BUTTON | RETURN_TO_END_BUTTON, false );

        // set others
        if( _hasData )
            enableActions( SNAPSHOT_BUTTON | SAVE_BUTTON | CLEAR_BUTTON | SAVE_TO_XML | SAVE_RAW_TO_XML | OPEN_PROFILEVIS, true );
        else
            enableActions( SNAPSHOT_BUTTON | SAVE_BUTTON | CLEAR_BUTTON | SAVE_TO_XML | SAVE_RAW_TO_XML | OPEN_PROFILEVIS, false );
    }

    /**
     * Checks if the options action button on the view is enabled.<p>
     * <b> The sub classes can override this method to make the options action button to be enabled under different conditions</b>
     *
     * @return
     */
    public boolean optionsEnabled(){
        return DebugUtils.isRIMDebuggerRunning();
    }

    // subclasses of this class are supposed to override
    // these methods but can ignore the actions that are not
    // applied to them
    /**
     * Clears the current display content.
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void clear() {
        // do nothing;
    }

    /**
     * Save the currently displayed data into a csv file.
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void save() {
        // do nothing;
    }

    /**
     * Reloads the data and refresh the display.
     * <p>
     * <b>subclasses need to override this method.</b>
     *
     * @throws CoreException
     */
    public void refresh() throws CoreException {
        // do nothing;
    }

    /**
     * Takes a snapshot of current data
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void snapshot() {
        // do nothing;
    }

    /**
     * Sets up options.
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void setOptions() {
        // do nothing;
    }

    /**
     * Compares the current data with the snapshot.
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void compare() {
        // do nothing;
    }

    /**
     * Perform garbage collection.
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void gc() {
        // do nothing;
    }

    /**
     * Sets filters.
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void filter() {
        // do nothing
    }

    /**
     * Forwards to the next step.
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void forward() {
        // do nothing
    }

    /**
     * Backwards to the last step.
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void backward() {
        // do nothing
    }

    /**
     * Returns to the start.
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void returnToStart() {
        // do nothing
    }

    /**
     * Goes to the end
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void returnToEnd() {
        // do nothing
    }

    /**
     * Save view content to a XML file.
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void saveXML() {
        // do nothing
    }

    /**
     * Save raw data of the view content to a XML file.
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void saveRawToXML() {
        // do nothing
    }

    /**
     * Open ProfileVisDesktop to display the collected profile data
     */
    public void openProfileVis() {
        // do nothing
    }
}
