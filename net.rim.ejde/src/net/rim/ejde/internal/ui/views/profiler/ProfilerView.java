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

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.RimIDEUtil;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;
import net.rim.ejde.internal.ui.views.BasicDebugView;
import net.rim.ejde.internal.util.DebugUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ide.RIA;
import net.rim.ide.RIA.ProfileType;
import net.rim.ide.core.IDEError;
import net.rim.ide.core.ProfileData;
import net.rim.ide.core.ProfileData.SourceResolver;
import net.rim.ide.core.ProfileItem;
import net.rim.ide.core.ProfileItemSource;
import net.rim.ide.core.ProfileLine;
import net.rim.ide.core.ProfileMethod;
import net.rim.ide.core.ProfileSourceLine;
import net.rim.ide.core.Util;
import net.rim.tools.compiler.debug.DebugMethod;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * View to display profile data.
 */
public class ProfilerView extends BasicDebugView implements SourceResolver {

    public static final String PROFILER_VIEW_ID = "net.rim.ejde.ui.viewers.ProfilerView";

    // Indexes of profile tabs
    final static public int INDEX_OF_TAB_SUMMARY = 0;

    final static public int INDEX_OF_TAB_METHOD = 1;

    final static public int INDEX_OF_TAB_SOURCE = 2;

    private static final Logger log = Logger.getLogger( ProfilerView.class );

    private TabFolder _tabFolder;

    private int _whatToProfile = 0;

    private boolean _methodTimeType;

    private ProfileTab[] _profileTabs;

    private ProfileData _pd;

    boolean _isInitialized = false;

    private static final String BLACKBERRY_PROFILEVISDESKTOP_TMP_FILE_PREFIX = "bbprof.prefix.";
    private static final String BLACKBERRY_PROFILEVISDESKTOP_TMP_FILE_SUFFIX = ".bbprof";

    /**
     * Constructs a ProfilerView instance.
     *
     * @throws CoreException
     *
     */
    public ProfilerView() throws CoreException {
        super( FORWARD_BUTTON | BACKWARD_BUTTON | REFRESH_BUTTON | CLEAR_BUTTON | OPTIONS_BUTTON | SAVE_BUTTON | SAVE_TO_XML
                | SAVE_RAW_TO_XML );
        RIA ria = RIA.getCurrentDebugger();
        if( ria != null && !_isInitialized ) {
            initProfileParameters( ria );
        }
    }

    /**
     * Initializes all parameters related to profile.
     *
     * @throws CoreException
     *
     */
    private void initProfileParameters( RIA ria ) {
        if( ria != null ) {
            if( !DebugUtils.isRIMDebuggerRunning() ) {
                return;
            }
            if( !ria.getProfileEnabled() ) {
                ria.setProfileEnabled( true );
            }
            cleanMessage();
            try {
                // get profile options from preference store
                IPreferenceStore ps = ContextManager.PLUGIN.getPreferenceStore();
                _whatToProfile = ps.getInt( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_WHATTOPROFILE );
                ProfileType[] types;
                // MKS 2486071
                String debugAttachedTo = ria.getDebugAttachTo();
                if( debugAttachedTo == null || debugAttachedTo.isEmpty() ) {
                    return;
                } else {
                    types = ProfilingViewOptionsDialog.getProfileTypes( ria );
                }

                if( !isValidProfileId( types, _whatToProfile ) ) {
                    _whatToProfile = types[ 0 ].getId();
                }
                _methodTimeType = ps.getBoolean( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_METHOD_TIME_TYPE );
                log.debug( "Profile option - type:" + _whatToProfile + ", method: "
                        + ( _methodTimeType ? "Cumulative" : "In method only" ) );
                ria.profileSetType( _whatToProfile );
                updateTypeColumeTitle();
                _isInitialized = true;
            } catch( Exception e ) {
                log.error( "", e );
            }
        }
    }

    protected void handleRIMDebugEvent( DebugEvent event ) {
        if( event.getKind() == DebugEvent.CREATE && !_isInitialized ) {
            initProfileParameters( RIA.getCurrentDebugger() );
        }
    }

    public void createTableViewPart( Composite parent ) {
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        parent.setLayout( gridLayout );
        // create a TabFolder component on the view
        _tabFolder = new TabFolder( parent, SWT.BOTTOM );
        _tabFolder.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        _profileTabs = new ProfileTab[ 3 ];
        _profileTabs[ INDEX_OF_TAB_SUMMARY ] = new SummaryProfileTab( this );
        _profileTabs[ INDEX_OF_TAB_METHOD ] = new MethodProfileTab( this );
        _profileTabs[ INDEX_OF_TAB_SOURCE ] = new SourceProfileTab( this );
        //
        if( !DebugUtils.isRIMDebuggerRunning() ) {
            setMessage( Messages.ProcessView_NO_BB_DEBUG_SESSION_MSG, true );
        }
    }

    /**
     * Set focus to a certain UI component.
     */
    public void setFocus() {
        // nothing to do
    }

    /**
     * Gets the profile type that need to be displayed.
     *
     * @return Profile type.
     */
    public int getWhatToProfile() {
        return _whatToProfile;
    }

    /**
     * Gets method time type.
     *
     * @return <code>true</code> method time will be cumulated, <code>false</code> otherwise.
     */
    public boolean getMethodTimeType() {
        return _methodTimeType;
    }

    /**
     * Set the tab at the given zero-relative index in the tabfolder as selected.
     *
     * @param index
     *            the index of the item to select.
     */
    public void setActiveTab( int index ) {
        _tabFolder.setSelection( index );
    }

    protected TabFolder getTabFolder() {
        return _tabFolder;
    }

    /**
     * Updates the title of the type column (what to profile) of each tab.
     */
    void updateTypeColumeTitle() {
        Display.getDefault().syncExec( new Runnable() {

            @Override
            public void run() {
                // the _profileTabs could be null if this is called during the view creation
                if( _profileTabs == null ) {
                    return;
                }
                for( int i = 0; i < _profileTabs.length; i++ ) {
                    _profileTabs[ i ].updateTypeColumeTitle();
                }
            }

        } );
    }

    /**
     * Display profile data.
     */
    public void displayProfileData( ProfileTab[] tabs ) {
        if( tabs == null || tabs.length == 0 )
            return;
        if( _pd != null ) {
            for( int i = 0; i < tabs.length; i++ )
                tabs[ i ].displayData( _pd );
        }
    }

    /**
     * Display source code information of <code>pi</code>.
     *
     * @param pi
     *            An instance of ProfileItem.
     */
    protected void displaySourceData( ProfileItem pi ) {
        SourceProfileTab sourceTab = (SourceProfileTab) _profileTabs[ INDEX_OF_TAB_SOURCE ];
        sourceTab.setTotal( _pd.getTotalExecutionTicks() );
        sourceTab.setHistory( pi );
        sourceTab.clearExpansion();
        sourceTab.displayData( pi );
        setActiveTab( INDEX_OF_TAB_SOURCE );
    }

    /**
     * Clears the view (all tabs).
     *
     * @param clearPreferences
     *            <code>true</code> record or the last operation on this tab will be cleaned; <code>false</code> record or the
     *            last operation on this tab will not be cleaned.
     */
    public void clearVeiwer( boolean clearPreferences ) {

        if( _pd != null )
            // clear display on each tab
            for( int i = 0; i < _profileTabs.length; i++ )
                _profileTabs[ i ].clearTab( clearPreferences );
    }

    /**
     * Save profile data to a csv file.
     */
    private void saveProfile() {
        if( _pd == null )
            return;
        try {
            // save profile data to the file
            saveContents( RimIDEUtil.openCSVFileForSave( getSite().getShell() ) );
        } catch( IDEError e ) {
            log.error( "", e );
        }
    }

    /**
     * Writes the profile data to <code>file</code>.
     *
     * @param file
     *            Destination file.
     * @throws IDEError
     */
    private void saveContents( File file ) throws IDEError {
        if( file == null ) {
            return;
        }
        RIA ria = RIA.getCurrentDebugger();
        if( ria == null ) {
            return;
        }
        String debugAttachedTo = ria.getDebugAttachTo();
        if( debugAttachedTo == null || debugAttachedTo.isEmpty() ) {
            return;
        }

        PrintStream out = null;
        try {
            out = new PrintStream( new FileOutputStream( file ) );
            out.print( RIA.getString( "ProfileCSVFileHeader1" ) ); //$NON-NLS-1$
            out.print( ria.profileGetTypes()[ _whatToProfile ].getDescription() );
            out.print( RIA.getString( "ProfileCSVFileHeader2" ) ); //$NON-NLS-1$
            out.println();

            ProfileItem[] modules = sortedElements( _pd, null );
            for( int i = 0; i < modules.length; i++ ) {
                ProfileItem module = modules[ i ];
                Object moduleName = module;

                ProfileItem[] methods = sortedElements( module, null );
                for( int j = 0; j < methods.length; j++ ) {
                    ProfileItem method = methods[ j ];

                    out.print( moduleName );
                    out.print( ", " ); //$NON-NLS-1$
                    String methodStr = method.toString();
                    Object handle = method.getMethodHandle();
                    if( handle != null && handle instanceof DebugMethod ) {
                        methodStr = ( (DebugMethod) handle ).getFullName();
                    }
                    out.print( Util.replace( methodStr, ",", "" ) ); //$NON-NLS-1$ //$NON-NLS-2$
                    out.print( ", " ); //$NON-NLS-1$
                    out.print( method.getTicks() );
                    out.print( ", " ); //$NON-NLS-1$
                    out.print( method.getCount() );
                    out.println();
                }
            }
            out.close();
        } catch( IOException e ) {
            log.error( "", e );
        }
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
    protected static ProfileItem[] sortedElements( ProfileItemSource source, Comparator comparator ) {
        ProfileItem profileItems[] = getUnsortedElements( source );
        if( profileItems.length == 0 )
            return profileItems;
        if( ( source instanceof ProfileMethod ) || ( source instanceof ProfileLine ) )
            // source lines are sorted use ProfileMethod's default comparator
            Arrays.sort( profileItems, source.getComparator() );
        else
            Arrays.sort( profileItems, comparator == null ? source.getComparator() : comparator );
        return profileItems;
    }

    /**
     * Gets unsorted children of given <code>source</code>.
     *
     * @param source
     * @return
     */
    protected static ProfileItem[] getUnsortedElements( ProfileItemSource source ) {
        Enumeration enumeration = source.getChildrenKeys();
        if( enumeration == null )
            return new ProfileItem[ 0 ];
        ProfileItem profileItems[] = new ProfileItem[ source.getChildCount() ];
        int i = 0;
        while( enumeration.hasMoreElements() ) {
            ProfileItem pi = source.getChild( enumeration.nextElement() );
            profileItems[ i++ ] = pi;
        }
        return profileItems;
    }

    private void refresh( boolean clearPreferences ) throws CoreException {
        RefreshProfilerViewJob job = new RefreshProfilerViewJob();
        try {
            PlatformUI.getWorkbench().getProgressService().run( false, true, job );
        } catch( InvocationTargetException e ) {
            log.error( "", e );
        } catch( InterruptedException e ) {
            log.error( "", e );
        }
    }

    class RefreshProfilerViewJob implements IRunnableWithProgress {

        public void run( IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException {
            monitor.beginTask( Messages.ProfilerView_Refresh, 100 );
            RIA ria = RIA.getCurrentDebugger();
            if( ria == null ) {
                return;
            }
            clearVeiwer( true );
            monitor.worked( 10 );
            try {
                ria.profileRefreshData();
                monitor.worked( 20 );
                _pd = ria.profileGetData();
                monitor.worked( 30 );
                if( _pd == null )
                    return;
                ProfileItem.setTickMode( _methodTimeType );
                monitor.worked( 40 );
                displayProfileData( new ProfileTab[] { _profileTabs[ INDEX_OF_TAB_SUMMARY ], _profileTabs[ INDEX_OF_TAB_METHOD ],
                        _profileTabs[ INDEX_OF_TAB_SOURCE ] } );
                monitor.worked( 50 );
                setHasData( true );
                monitor.worked( 60 );
                updateToolbar();
            } catch( IDEError e ) {
                log.error( "", e );
            }
            monitor.done();
        }
    }

    // ------ Methods in BasicDebugView to be overridden ------
    /**
     * RIM Debug session is terminated.
     *
     * @see BasicDebugView#RIMDebugTerminated().
     */
    public void RIMDebugTerminated( ILaunch[] launches ) {
        setMessage( Messages.ProcessView_NO_BB_DEBUG_SESSION_MSG, true );
        _isInitialized = false;
        this.getSite().getShell().getDisplay().syncExec( new Runnable() {

            @Override
            public void run() {
                clear();
            }

        } );
        // this.getSite().getShell().getDisplay().asyncExec( new CloseViewJob( this ) );
    }

    /**
     * Gets new profile data, erases the current profile data, and display the new profile data.
     *
     * @throws CoreException
     * @see BasicDebugView#refresh().
     *
     */
    public void refresh() throws CoreException {
        refresh( true );
    }

    public void forward() {
        if( !( _tabFolder.getSelectionIndex() == INDEX_OF_TAB_SOURCE ) )
            return;
        _profileTabs[ INDEX_OF_TAB_SOURCE ].forward();
    }

    public void backward() {
        if( !( _tabFolder.getSelectionIndex() == INDEX_OF_TAB_SOURCE ) )
            return;
        _profileTabs[ INDEX_OF_TAB_SOURCE ].backward();
    }

    /**
     * (non-Javadoc)
     *
     * @see BasicDebugView#clear().
     */
    public void clear() {
        // clear the debugger
        RIA ria = RIA.getCurrentDebugger();
        if( ria == null ) {
            return;
        }
        try {
            ria.profileClearData();
        } catch( IDEError e ) {
            log.error( e );
            return;
        }
        // clear the display data
        clearVeiwer( true );
        _pd = null;
        setHasData( false );
        updateToolbar();
        updateTypeColumeTitle();
    }

    /**
     * Gets the ProfileData instance represented by this view.
     *
     * @return a ProfileData instance.
     */
    public ProfileData getProfileData() {
        return _pd;
    }

    /**
     * (non-Javadoc)
     *
     * @see BasicDebugView#save().
     */
    public void save() {
        saveProfile();
    }

    public void dispose() {
        super.dispose();
        if( RIA.getCurrentDebugger() != null ) {
            RIA.getCurrentDebugger().setProfileEnabled( false );
        }
        _pd = null;
        _profileTabs = null;
    }

    private static boolean isValidProfileId( ProfileType[] types, int id ) {
        for( int i = 0; i < types.length; i++ ) {
            if( types[ i ].getId() == id ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Opens an option dialog. Options related to profiler can be set on the dialog and will be saved as references when "OK"
     * button is pressed.
     *
     * @see BasicDebugView#setOptions().
     */
    public void setOptions() {
        // create an ImplicitBuildRuleEditDialog instance
        ProfilingViewOptionsDialog optionsDialog = new ProfilingViewOptionsDialog( getSite().getShell() );
        // show the dialog
        optionsDialog.open();
        if( optionsDialog.isOkButtonClicked() ) {
            try {
                IPreferenceStore ps = ContextManager.PLUGIN.getPreferenceStore();
                int whatToProfile = ps.getInt( PreferenceConstants.NET_RIM_EJDE_UI_VIEWS_WHATTOPROFILE );
                if( whatToProfile != _whatToProfile ) {
                    initProfileParameters( RIA.getCurrentDebugger() );
                    clear();
                    enableActions( REFRESH_BUTTON, false );
                } else {
                    initProfileParameters( RIA.getCurrentDebugger() );
                    refresh( false );
                }
            } catch( Exception e ) {
                log.error( "", e );
            }
        }
    }

    /**
     * Save view content to a XML file.
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void saveXML() {
        File xmlFile = chooseDataFile();
        if( xmlFile == null ) {
            return;
        }
        if( !xmlFile.exists() ) {
            xmlFile = ProjectUtils.createFile( xmlFile );
            if( xmlFile == null || !xmlFile.exists() ) {
                return;
            }
        }
        ProgressMonitorDialog dialog = new ProgressMonitorDialog( ContextManager.getActiveWorkbenchShell() );
        SaveDataRunnale runnable = new SaveDataRunnale( xmlFile, this );
        try {
            dialog.run( false, true, runnable );
        } catch( InvocationTargetException e ) {
            log.error( e );
            MessageDialog
                    .openError( ContextManager.getActiveWorkbenchShell(), e.getMessage(), Messages.ErrorHandler_DIALOG_TITLE );
        } catch( InterruptedException e ) {
            log.error( e );
            MessageDialog
                    .openError( ContextManager.getActiveWorkbenchShell(), e.getMessage(), Messages.ErrorHandler_DIALOG_TITLE );
        }
    }

    /**
     * Save raw data of the view content to a XML file.
     * <p>
     * <b>subclasses need to override this method.</b>
     */
    public void saveRawToXML() {
        File xmlFile = chooseRawDataFile();
        if( xmlFile == null ) {
            return;
        }
        if( !xmlFile.exists() ) {
            xmlFile = ProjectUtils.createFile( xmlFile );
            if( xmlFile == null || !xmlFile.exists() ) {
                return;
            }
        }
        ProgressMonitorDialog dialog = new ProgressMonitorDialog( ContextManager.getActiveWorkbenchShell() );
        SaveRawDataRunnale runnable = new SaveRawDataRunnale( xmlFile, this );
        try {
            dialog.run( false, true, runnable );
        } catch( InvocationTargetException e ) {
            log.error( e );
            MessageDialog
                    .openError( ContextManager.getActiveWorkbenchShell(), e.getMessage(), Messages.ErrorHandler_DIALOG_TITLE );
        } catch( InterruptedException e ) {
            log.error( e );
            MessageDialog
                    .openError( ContextManager.getActiveWorkbenchShell(), e.getMessage(), Messages.ErrorHandler_DIALOG_TITLE );
        }
    }

    private void saveRawData( File xmlFile ) {
        RIA ria = RIA.getCurrentDebugger();
        if( ria == null ) {
            return;
        }
        if( xmlFile == null ) {
            return;
        }
        if( !xmlFile.exists() ) {
            xmlFile = ProjectUtils.createFile( xmlFile );
            if( xmlFile == null || !xmlFile.exists() ) {
                return;
            }
        }
        PrintStream out = null;
        try {
            out = new PrintStream( new FileOutputStream( xmlFile ) );
            ria.profileDumpRawXML( out );
        } catch( Exception e ) {
            log.error( e );
            MessageDialog
                    .openError( ContextManager.getActiveWorkbenchShell(), Messages.ErrorHandler_DIALOG_TITLE, e.getMessage() );
        } finally {
            if( out != null ) {
                out.close();
            }
        }
    }

    private File chooseDataFile() {
        return chooseXMLFile( new String[] { "*.xml" }, new String[] { "XML File (*.xml)" } );
    }

    private File chooseRawDataFile() {
        return chooseXMLFile( new String[] { "*.xml", "*.bbprof" }, new String[] { "XML File (*.xml)",
                "BlackBerry ProfileVisDesktop File (*.bbprof)" } );
    }

    private File chooseXMLFile( String[] filterExtensions, String[] filterNamesExtensions ) {
        FileDialog dialog = new FileDialog( this.getSite().getShell(), SWT.SAVE );
        dialog.setFilterExtensions( filterExtensions );
        dialog.setFilterNames( filterNamesExtensions );
        String xmlFile = dialog.open();
        if( !StringUtils.isBlank( xmlFile ) ) {
            return new File( xmlFile );
        }
        return null;
    }

    @Override
    public String resolveSourceLine( ProfileItem item ) {
        RIA ria = RIA.getCurrentDebugger();
        if( ria == null ) {
            return Messages.SourceProfileTab_NO_SOURCE_MESSAGE;
        }
        ProfileSourceLine psl = item.getLineHandle();
        if( psl == null )
            return Messages.SourceProfileTab_NO_SOURCE_MESSAGE;
        Object line = psl.getLine();
        if( line == null ) {
            return Messages.SourceProfileTab_NO_SOURCE_MESSAGE;
        }
        return line.toString();
    }

    public void openProfileVis() {
        File tmpFile = getTmpFile();
        ProgressMonitorDialog dialog = new ProgressMonitorDialog( ContextManager.getActiveWorkbenchShell() );
        SaveRawDataRunnale runnable = new SaveRawDataRunnale( tmpFile, this );
        try {
            dialog.run( false, true, runnable );
        } catch( InvocationTargetException e ) {
            log.error( e );
            MessageDialog
                    .openError( ContextManager.getActiveWorkbenchShell(), e.getMessage(), Messages.ErrorHandler_DIALOG_TITLE );
            return;
        } catch( InterruptedException e ) {
            log.error( e );
            MessageDialog
                    .openError( ContextManager.getActiveWorkbenchShell(), e.getMessage(), Messages.ErrorHandler_DIALOG_TITLE );
            return;
        }
        try {
            Desktop.getDesktop().open( tmpFile );
        } catch( IOException e ) {
            log.error( e );
            MessageDialog.openError( ContextManager.getActiveWorkbenchShell(), Messages.ErrorHandler_DIALOG_TITLE,
                    Messages.BBProfileVis_Not_Installed_ErrMsg );
        }
    }

    private static synchronized File getTmpFile() {
        String tmpDir = System.getProperty( "java.io.tmpdir" );
        if( tmpDir == null ) {
            // Shouldn't happen
            tmpDir = ".";
        }
        File file = null;
        for( int retry = 0; retry < 10; retry++ ) {
            file = new File( tmpDir, BLACKBERRY_PROFILEVISDESKTOP_TMP_FILE_PREFIX + System.currentTimeMillis()
                    + BLACKBERRY_PROFILEVISDESKTOP_TMP_FILE_SUFFIX );
            if( !file.exists() ) {
                return file;
            }
        }
        throw new RuntimeException( "unable to create temporary file" );
    }

    // ----- Inner Classes ------
    class CloseViewJob implements Runnable {
        ProfilerView _view;

        CloseViewJob( ProfilerView view ) {
            _view = view;
        }

        public void run() {
            IViewSite viewSite = _view.getViewSite();
            if( viewSite == null ) {
                return;
            }
            IWorkbenchPage workbenchPage = viewSite.getPage();
            if( workbenchPage == null ) {
                return;
            }
            workbenchPage.hideView( _view );
        }

    }

    class SaveRawDataRunnale implements IRunnableWithProgress {
        File destFile;
        SourceResolver sourceResolver;

        public SaveRawDataRunnale( File destFile, SourceResolver sourceResolver ) {
            this.sourceResolver = sourceResolver;
            this.destFile = destFile;
        }

        @Override
        public void run( IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException {
            try {
                monitor.beginTask( "Saving profiling raw data...", 10 );
                log.trace( "Save raw to XML" );
                monitor.worked( 1 );
                saveRawData( destFile );
            } finally {
                monitor.done();
            }

        }

    }

    class SaveDataRunnale implements IRunnableWithProgress {
        File destFile;
        SourceResolver sourceResolver;

        public SaveDataRunnale( File destFile, SourceResolver sourceResolver ) {
            this.sourceResolver = sourceResolver;
            this.destFile = destFile;
        }

        @Override
        public void run( IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException {
            try {
                monitor.beginTask( "Saving profiling data...", 10 );
                log.trace( "Save XML" );
                RIA ria = RIA.getCurrentDebugger();
                if( ria == null ) {
                    return;
                }
                monitor.worked( 1 );
                ProfileData profileData = getProfileData();
                if( profileData == null ) {
                    return;
                }
                monitor.worked( 1 );
                try {
                    profileData.saveContentsInXml( destFile, ria.profileGetTypes()[ getWhatToProfile() ].getDescription(),
                            this.sourceResolver );
                } catch( IDEError e ) {
                    log.error( e );
                    MessageDialog.openError( ContextManager.getActiveWorkbenchShell(), Messages.ErrorHandler_DIALOG_TITLE,
                            e.getMessage() );
                }
            } finally {
                monitor.done();
            }

        }

    }
}
