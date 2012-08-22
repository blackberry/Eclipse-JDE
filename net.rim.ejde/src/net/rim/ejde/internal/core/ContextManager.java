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
package net.rim.ejde.internal.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.rim.ejde.IDebugConsoleWriter;
import net.rim.ejde.external.sourceMapper.SourceMapperAccess;
import net.rim.ejde.internal.builders.ClasspathChangeManager;
import net.rim.ejde.internal.builders.PreprocessedSourceMapper;
import net.rim.ejde.internal.internalplugin.InternalFragment;
import net.rim.ejde.internal.launching.EJDEDebugFilesClient;
import net.rim.ejde.internal.launching.IFledgeLaunchConstants;
import net.rim.ejde.internal.launching.IRunningFledgeLaunchConstants;
import net.rim.ejde.internal.legacy.RIADialog;
import net.rim.ejde.internal.model.BasicBlackBerryProperties;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.ui.consoles.SimulatorOutputConsole;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;
import net.rim.ejde.internal.util.DebugUtils;
import net.rim.ejde.internal.util.InternalContextManagerUtils;
import net.rim.ejde.internal.util.PackagingUtils;
import net.rim.ejde.internal.util.RIAUtils;
import net.rim.ejde.internal.util.UpgradingNotification;
import net.rim.ejde.internal.util.VMToolsUtils;
import net.rim.ejde.internal.validation.ValidationManager;
import net.rim.ide.OSUtils;
import net.rim.ide.RIA;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import com.thoughtworks.xstream.XStream;

/**
 * The activator class controls the plug-in life cycle
 */
public class ContextManager extends AbstractUIPlugin implements IDebugEventSetListener, ILaunchesListener2 {
    static private final Logger log = Logger.getLogger( ContextManager.class );

    private Hashtable< String, BlackBerryProperties > BBModelMap;
    private Hashtable< IPath, RIA > BBVMMap;
    private XStream _xStream;
    // The plug-in ID
    public static final String PLUGIN_ID = "net.rim.ejde";
    // The shared instance
    public static ContextManager PLUGIN;
    private ResourceBundle _coreResourcesBundle;
    private boolean _isSuspended;
    private Image _blankImage;
    private HashMap< Object, Image > _images = new HashMap< Object, Image >();

    {
        try {
            _coreResourcesBundle = ResourceBundle.getBundle( "CorePluginResources" ); //$NON-NLS-1$
        } catch( MissingResourceException x ) {
            _coreResourcesBundle = null;
        }
    }

    /**
     * The constructor
     */
    public ContextManager() {
        PLUGIN = this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext )
     */
    public void start( BundleContext context ) throws Exception {
        log.trace( "ContextManager starting, bundle version: " + context.getBundle().getVersion() );
        super.start( context );
        BBModelMap = new Hashtable< String, BlackBerryProperties >();
        BBVMMap = new Hashtable< IPath, RIA >();
        // register classpath change listener
        ClasspathChangeManager classpathChangeManager = ClasspathChangeManager.getInstance();
        classpathChangeManager.addElementChangedListener();
        // register property change listener
        PropertyChangeListenerImp.addListener();
        BundleListenerHandler.getInstance( context );
        RuntimeInstallsHandler.getInstance();
        enableResourceChangeListener( true );
        // set preprocess mapper
        SourceMapperAccess.setSourceMapper( new PreprocessedSourceMapper() );
        // initialize validation manager
        ValidationManager.getInstance();
        // Add launch and debug event listener used by debugging view
        DebugPlugin.getDefault().addDebugEventListener( this );
        DebugPlugin.getDefault().getLaunchManager().addLaunchListener( this );

        IPreferenceStore psc = ContextManager.getDefault().getPreferenceStore();

        if( OSUtils.isMac() && !psc.getBoolean( IConstants.LC_FILTERING_SET_KEY ) ) {
            // For Mac, filter out simulator LaunchConfig(s), if not already
            // Once LC_filtering_set=true, there is no more forcing of this logic to allow diff testing
            IPreferenceStore psd = DebugUIPlugin.getDefault().getPreferenceStore();
            if( !psd.getBoolean( IInternalDebugUIConstants.PREF_FILTER_LAUNCH_TYPES ) ) {
                psc.setValue( IConstants.LC_FILTERING_SET_KEY, true );
                psd.setValue( IInternalDebugUIConstants.PREF_FILTER_LAUNCH_TYPES, true );
                String pftl = psd.getString( IInternalDebugUIConstants.PREF_FILTER_TYPE_LIST );
                if( pftl == null ) {
                    pftl = "";
                }
                if( !pftl.contains( IFledgeLaunchConstants.LAUNCH_CONFIG_ID ) ) {
                    pftl += IFledgeLaunchConstants.LAUNCH_CONFIG_ID + IConstants.COMMA_MARK;
                }
                if( !pftl.contains( IRunningFledgeLaunchConstants.LAUNCH_CONFIG_ID ) ) {
                    pftl += IRunningFledgeLaunchConstants.LAUNCH_CONFIG_ID + IConstants.COMMA_MARK;
                }
                psd.setValue( IInternalDebugUIConstants.PREF_FILTER_TYPE_LIST, pftl );
            }
        }
        // check if we need to clean the workspace
        boolean needClean = psc.getBoolean( IConstants.NEED_CLEAN_WORKSPACE_KEY );
        if( needClean ) {
            CleanWorkspaceJob job = new CleanWorkspaceJob( "Cleaning workspace" );
            job.schedule();
        }

        InternalContextManagerUtils.startupInitialize();
        InternalFragment.startup();
    }

    protected void refreshPluginActions() {
        super.refreshPluginActions();
        boolean needUpdateCheck = ContextManager.getDefault().getPreferenceStore().getBoolean( PreferenceConstants.UPDATE_NOTIFY );
        log.trace( "ContextManager startingneed update: " + needUpdateCheck );
        if( needUpdateCheck ) {
            UpgradingNotification updateJob = new UpgradingNotification();
            updateJob.schedule();
        }
    }

    public XStream getXStream() {
        if( _xStream == null ) {
            _xStream = new XStream();
            _xStream.alias( BasicBlackBerryProperties.MODEL_ALIAS_HEAD, BlackBerryProperties.class );
            _xStream.alias( BasicBlackBerryProperties.PACKAGING__ALIAS_HEAD, BlackBerryProperties.ExtendedPackaging.class );
            _xStream.processAnnotations( BlackBerryProperties.class );
        }
        return _xStream;
    }

    /**
     * Get the RIA instance of the given <code>homePath</code>.
     *
     * @param homePath
     * @return
     */
    public RIA getRIA( String homePath ) {
        return getRIA( new Path( homePath ) );
    }

    /**
     * Get the RIA instance of the given <code>homePath<code>.
     *
     * @param homePath
     *            the home path of the RIA
     * @return RIA instances or <code>null</code> if the corresponding RIA instance can not be found or created.
     */
    public RIA getRIA( IPath homePath ) {
        if( homePath == null || homePath.isEmpty() ) {
            log.error( "getRIA(): RIA home path is empty." );
            return null;
        }
        RIA ria = BBVMMap.get( homePath );
        if( ria != null ) {
            return ria;
        }
        String legacyJDEHome = RIAUtils.getValidJDEHome( homePath.toOSString() );
        if( StringUtils.isBlank( legacyJDEHome ) ) {
            log.error( "getRIA(): Invalid RIA home path: " + homePath.toOSString() );
            return null;
        }
        // TODO:In the else part invoke the Mac/Linux version of executables
        if( OSUtils.isWindows() ) {
            RIAUtils.initDLLs();
        }
        log.debug( "RIA Initialization is started for " + homePath + "....." );
        long start = System.currentTimeMillis();
        String DLLPath = ContextManager.PLUGIN.getStateLocation().append( "installDlls" ).toOSString();
        long stop = System.currentTimeMillis();
        ria = RiaMaker.createRia( legacyJDEHome, DLLPath );
        // hook to RIA dialog callback for missing debug files
        ria.setDialogCallback( new RIADialog() );
        // enable logging
        String enableLog = System.getProperties().getProperty( "JDWP_LOGGING" ); //$NON-NLS-1$
        ria.setLogging( enableLog != null );
        log.debug( "RIA Initialization finished in : " + ( stop - start ) + " ms" );
        BBVMMap.put( homePath, ria );

        IPreferenceStore store = ContextManager.getDefault().getPreferenceStore();
        String serverURL = store.getString( PreferenceConstants.DEBUG_FILE_SERVER_URL );

        try {
            IPath vmToolPath = VMToolsUtils.getVMToolsFolderPath();
            ria.setDebugFilesClient( new EJDEDebugFilesClient( serverURL, vmToolPath.toString() + File.separator + "BundleInfo",
                    new DebugConsoleWriter() ) );
        } catch( IOException e ) {
            log.error( "Error retrieving vmtool path", e );
            return null;
        }

        return ria;
    }

    /**
     * Set the given <code>ria</code> to the cached RIA map.
     *
     * @param ria
     */
    synchronized public void setRIA( RIA ria ) {
        if( ria != null ) {
            BBVMMap.put( new Path( ria.getHomePath() ), ria );
        }
    }

    /**
     * Enable ResourceChangeManager to listen to eclipse resource changes.
     */
    public void enableResourceChangeListener( boolean enabled ) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        if( enabled ) {
            workspace.addResourceChangeListener( ResourceChangeManager.getInstance(), ResourceChangeManager.getFilter() );
        } else {
            workspace.removeResourceChangeListener( ResourceChangeManager.getInstance() );
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext )
     */
    public void stop( BundleContext context ) throws Exception {
        super.stop( context );
        BundleListenerHandler.removeInstance( context );
        RuntimeInstallsHandler.removeInstance();
        enableResourceChangeListener( false );
        // remove property change listener
        PropertyChangeListenerImp.removeListener();
        // remove debug event and launch listener
        DebugPlugin.getDefault().removeDebugEventListener( this );
        DebugPlugin.getDefault().getLaunchManager().removeLaunchListener( this );
        // stop RIAs
        RIA ria = null;
        for( IPath home : BBVMMap.keySet() ) {
            ria = BBVMMap.get( home );
            if( ria != null ) {
                ria.stop( true );
            }
        }
        BBModelMap = null;
        PLUGIN = null;
        InternalContextManagerUtils.stopCleanup();
    }

    /**
     * Get the BlackBerry properties instances for the given <code>projectName</code>.
     * <p>
     * <b>This method needs to be called when a BlackBerry properties instance is required. </b>
     *
     *
     * @param projectName
     * @param forceLoad
     *            <code>true</code> force to load the properties from the project description file;
     *            <p>
     *            <code>false</code> if the properties is in the cache, return the cached properties;
     * @return The BlackBerry properties of the project or <code>null</code> if the project is a BlackBerry project but does not
     *         have a project description xml file or any exception occurred.
     */
    public BlackBerryProperties getBBProperties( String projectName, boolean forceLoad ) {
        BlackBerryProperties properties = BBModelMap.get( projectName );
        if( properties != null && !forceLoad ) {
            return properties;
        }
        IWorkspace eclipseWS = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot eclipseWSRoot = eclipseWS.getRoot();
        IProject project = eclipseWSRoot.getProject( projectName );
        // try to get properties from the project description file
        IFile propertiesFile = project.getFile( BlackBerryProject.METAFILE );
        if( propertiesFile.exists() ) {
            properties = loadModelFromStore( propertiesFile.getLocation().toFile() );
            if( properties != null ) {
                if( properties.getModelVersion().equals( "1.1.1" ) ) {
                    boolean changed = false;
                    // if the project property file was created by ejde 1.1.1, we change the output folder to the default value
                    String oldOutputFolder = properties._packaging.getOutputFileName();
                    String defaultOutputFolder = PackagingUtils.getDefaultProjectOutputPrefix();
                    if( !oldOutputFolder.equalsIgnoreCase( defaultOutputFolder ) ) {
                        log.trace( " Project property file was created by ejde 1.1.1, the output folder will be changed to the default value" );
                        properties._packaging.setOutputFolder( PackagingUtils.getDefaultProjectOutputPrefix() );
                        changed = true;
                    }
                    if( properties._application.getType().equals( BlackBerryProject.LIBRARY )
                            && !properties._application.isSystemModule() ) {
                        log.trace( " Project property file was created by ejde 1.1.1, the isSystemModule value is changed to true for a lib project" );
                        properties._application.setIsSystemModule( true );
                        changed = true;
                    }
                    if( changed ) {
                        properties.setModelVersion( BlackBerryProperties.getDefaultModelVersion() );
                        IStatus status = ResourcesPlugin.getWorkspace().validateEdit( new IFile[] { propertiesFile }, null );
                        if( status.isOK() ) {
                            commitModelToStore( properties, propertiesFile );
                        }
                    }
                }
                BBModelMap.put( projectName, properties );
            }
        } else {
            properties = new BlackBerryProperties();
            properties.setValidOutputFileName( project.getName() );
        }
        return properties;
    }

    /**
     * Sets the given <code>properties</code> to the cached table.
     * <p>
     * <b>This method needs to be called when a BlackBerry properties instance is going to be set to the cached table or commit to
     * the filesystem. </b>
     *
     * @param name
     *            name of the BlackBerryProject the properties belong to
     * @param properties
     *            BlackBerryProperties
     * @param force
     *            <code>true</code> if the given <code>properties</code> is already cached, we replace it with the given one and
     *            commit it to file system
     *            <p>
     *            <code>false</code> if the given <code>properties</code> is already cached, do nothing
     *            </p>
     */
    public void setBBProperties( String name, BlackBerryProperties properties, boolean force ) {
        log.trace( "setBBProperties(); " + name + "; " + force );
        BlackBerryProperties oldProperties = BBModelMap.get( name );
        boolean isequ = properties.equals( oldProperties );
        if( isequ && !force ) {
            return;
        }
        if( !isequ ) {
            BBModelMap.put( name, properties );
        }
        IProject iProject = ResourcesPlugin.getWorkspace().getRoot().getProject( name );
        IFile propertiesFile = iProject.getFile( BlackBerryProject.METAFILE );
        try {
            if( propertiesFile.exists() ) {
                propertiesFile.deleteMarkers( null, false, IResource.DEPTH_ZERO );
            }
        } catch( CoreException e ) {
            log.error( e );
        }
        commitModelToStore( properties, propertiesFile );
    }

    public void removeBBProperties( String projectName ) {
        BBModelMap.remove( projectName );
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static ContextManager getDefault() {
        return PLUGIN;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in relative path
     *
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor( String path ) {
        return imageDescriptorFromPlugin( PLUGIN_ID, path );
    }

    /**
     * Returns the plugin's resource bundle,
     */
    public ResourceBundle getCoreResourcesBundle() {
        return _coreResourcesBundle;
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not found.
     */
    public static String getResourceString( String key ) {
        ResourceBundle bundle = PLUGIN.getCoreResourcesBundle();

        try {
            return ( bundle != null ) ? bundle.getString( key ) : key;
        } catch( MissingResourceException e ) {
            return key;
        }
    }

    public static Shell getActiveWorkbenchShell() {
        IWorkbenchWindow window = getActiveWorkbenchWindow();

        if( window != null ) {
            return window.getShell();
        }

        return null;
    }

    public static IWorkbenchWindow getActiveWorkbenchWindow() {
        return PLUGIN.getWorkbench().getActiveWorkbenchWindow();
    }

    /**
     * Gets the active workbench page.
     *
     * @return the active workbench page
     */
    public static IWorkbenchPage getActiveWorkbenchPage() {
        IWorkbenchWindow window = getActiveWorkbenchWindow();
        if( window == null ) {
            return null;
        }
        return window.getActivePage();
    }

    public BlackBerryProperties loadModelFromStore( File file ) {
        BlackBerryProperties blackBerryProperties = null;

        if( null == file || !file.exists() || !file.isFile() ) {
            return null;
        }
        log.trace( "loading " + file.getPath() );
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream( file );

            if( 0 < fileInputStream.available() ) {
                XStream xStream = getXStream();
                blackBerryProperties = (BlackBerryProperties) xStream.fromXML( fileInputStream );
            } else {
                blackBerryProperties = new BlackBerryProperties();
            }
        } catch( Throwable t ) {
            log.debug( "", t );
        } finally {
            if( null != fileInputStream ) {
                try {
                    fileInputStream.close();
                } catch( IOException e ) {
                    log.error( "Error closing input stream", e );
                }
            }
        }
        log.trace( "finished loading " + file.getPath() );
        return blackBerryProperties;
    }

    private void outputAppDescriptorHeaderComment( ByteArrayOutputStream outputStream ) throws IOException {
        StringBuffer temp = new StringBuffer( "<!-- " );
        temp.append( "This file has been generated by the BlackBerry Plugin for Eclipse v" );

        Version v = ResourcesPlugin.getPlugin().getBundle().getVersion();
        temp.append( v.getMajor() );
        temp.append( "." );
        temp.append( v.getMinor() );
        temp.append( "." );
        temp.append( v.getMicro() );

        temp.append( ". -->\n\n" );
        outputStream.write( temp.toString().getBytes() );
    }

    private void commitModelToStore( BlackBerryProperties blackBerryProperties, IFile ifile ) {
        if( null == blackBerryProperties ) {
            return;
        }

        if( null == ifile ) {
            return;
        }
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            log.trace( "Writing " + ifile.getLocation().toOSString() );
            outputStream = new ByteArrayOutputStream();
            XStream xStream = getXStream();
            outputAppDescriptorHeaderComment( outputStream );
            xStream.toXML( blackBerryProperties, outputStream );
            inputStream = new ByteArrayInputStream( outputStream.toByteArray() );
            if( ifile.exists() ) {
                ifile.setContents( inputStream, IFile.FORCE, new NullProgressMonitor() );
            } else {
                ifile.create( inputStream, true, new NullProgressMonitor() );
            }
        } catch( CoreException e ) {
            log.error( e.getMessage() );
        } catch( Throwable t ) {
            log.error( t.getMessage() );
        } finally {
            if( inputStream != null ) {
                try {
                    inputStream.close();
                } catch( IOException e ) {
                    log.error( e.getMessage() );
                }
            }
            if( outputStream != null ) {
                try {
                    outputStream.close();
                } catch( IOException e ) {
                    log.error( e.getMessage() );
                }
            }
        }
        log.trace( "Finish writing " + ifile.getLocation().toOSString() );
    }

    static public Display getDisplay() {
        if( PlatformUI.isWorkbenchRunning() ) {
            return PlatformUI.getWorkbench().getDisplay();
        }

        return Display.getDefault();
    }

    /**
     * (no java doc)
     *
     * @see IDebugEventSetListener#handleDebugEvents(DebugEvent[]).
     *
     */
    public void handleDebugEvents( DebugEvent[] events ) {
        if( events == null || events.length == 0 )
            return;

        for( int i = 0; i < events.length; i++ ) {
            if ( !DebugUtils.isFromRIMLaunch( events[i] )){
                continue;
            }
            if( events[ i ].getKind() == DebugEvent.SUSPEND && !_isSuspended ) {
                _isSuspended = true;
            }

            else if( events[ i ].getKind() == DebugEvent.RESUME && _isSuspended ) {
                _isSuspended = false;
            }
        } // end for
    }

    public boolean isSuspended() {
        return _isSuspended;
    }

    /**
     * @see ILaunchesListener2#launchesTerminated(ILaunch[]).
     */
    public void launchesTerminated( ILaunch[] launches ) {
        if ( !DebugUtils.hasRIMLaunch(launches) ){
            return;
        }
        RIA currentRIA = RIA.getCurrentDebugger();
        if( currentRIA != null ) {
            // when debugger is terminated, close simulator as well
            currentRIA.stopSimulator();
        }
        _isSuspended = false;
    }

    /**
     * @see ILaunchesListener#launchesRemoved(ILaunch[]).
     */
    public void launchesRemoved( ILaunch[] launches ) {
        // do nothing
    }

    /**
     * @see ILaunchesListener#launchesAdded(ILaunch[]).
     */
    public void launchesAdded( ILaunch[] launches ) {
        // do nothing
    }

    /**
     * @see ILaunchesListener#launchesChanged(ILaunch[]).
     */
    public void launchesChanged( ILaunch[] launches ) {
        // do nothing
    }

    private class CleanWorkspaceJob extends WorkspaceJob {

        public CleanWorkspaceJob( String name ) {
            super( name );
            // TODO Auto-generated constructor stub
        }

        public boolean belongsTo( Object family ) {
            return ResourcesPlugin.FAMILY_MANUAL_BUILD.equals( family );
        }

        @Override
        public IStatus runInWorkspace( IProgressMonitor monitor ) throws CoreException {
            ResourcesPlugin.getWorkspace().build( IncrementalProjectBuilder.CLEAN_BUILD, monitor );
            ContextManager.getDefault().getPreferenceStore().setValue( IConstants.NEED_CLEAN_WORKSPACE_KEY, false );
            return Status.OK_STATUS;
        }

    }

    /**
     * Gets the install url.
     *
     * @return the install url
     */
    public URL getInstallURL() {
        return getBundle().getEntry( "/" ); //$NON-NLS-1$
    }

    /**
     * Gets the image from plugin.
     *
     * @param bundleID
     *            the bundle id
     * @param path
     *            the path
     *
     * @return the image from plugin
     */
    public Image getImageFromPlugin( String bundleID, String path ) {
        ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin( bundleID, path );
        return ( desc != null ) ? get( desc ) : getBlankImage();
    }

    /**
     * Gets the blank image.
     *
     * @return the blank image
     */
    public Image getBlankImage() {
        if( _blankImage == null ) {
            _blankImage = ImageDescriptor.getMissingImageDescriptor().createImage();
        }
        return _blankImage;
    }

    /**
     * Gets the.
     *
     * @param desc
     *            the desc
     *
     * @return the image
     */
    public Image get( ImageDescriptor desc ) {
        Object key = desc;

        Image image = _images.get( key );
        if( image == null ) {
            image = desc.createImage();
            _images.put( key, image );
        }
        return image;
    }

    private class DebugConsoleWriter implements IDebugConsoleWriter {

        private SimulatorOutputConsole _console;
        private MessageConsoleStream _stream;

        public DebugConsoleWriter() {
            _console = SimulatorOutputConsole.getInstance();
        }
        public void init() {
            _stream = _console.newMessageStream();
        }
        public void log(String message) {
            _stream.println( message );
        }
        public void close() {
            try {
                if ( _stream != null) _stream.close();
            } catch( IOException e ) {
                // do nothing
            }

        }
    }
}
