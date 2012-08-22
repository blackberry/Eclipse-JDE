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
package net.rim.ejde.internal.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BlackBerrySDKInstall;
import net.rim.ejde.internal.model.BlackBerryVMInstallType;
import net.rim.ejde.internal.model.preferences.WarningsPreferences;
import net.rim.ejde.internal.signing.BBSigningKeys;
import net.rim.ejde.internal.sourcelookup.RIMClasspathProvider;
import net.rim.ide.RIA;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.launching.LaunchingMessages;
import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jdt.launching.environments.ExecutionEnvironmentDescription;
import org.osgi.framework.Version;

/**
 * The Class VMUtils.
 *
 * @author cmalinescu, jheifetz, rgunartanam
 */
public class VMUtils {
    static private final Logger _log = Logger.getLogger( VMUtils.class );
    private static Map< String, BBSigningKeys > _signKeysCache = new HashMap< String, BBSigningKeys >();

    /**
     * Gets the sign keys cache.
     *
     * @return the sign keys cache
     */
    public static Map< String, BBSigningKeys > getSignKeysCache() {
        return _signKeysCache;
    }

    /**
     * Adds the sign keys to cache.
     *
     * @param vm
     *            the vm
     *
     * @return the bB signing keys
     */
    public static BBSigningKeys addSignKeysToCache( IVMInstall vm ) {
        if( vm == null ) { // This could happen in import failed due to a bug
            _log.error( "No BlackBerry VM found and Signing Keys cache cannot be populated." );
            return null;
        }
        String vmName = vm.getName();
        if( !_signKeysCache.containsKey( vmName ) ) {
            RIA ria = ContextManager.PLUGIN.getRIA( vm.getInstallLocation().getPath() );
            BBSigningKeys signingKeys = new BBSigningKeys( ria );
            _signKeysCache.put( vmName, signingKeys );
            _log.debug( "SigningKey cache is populated for vm: " + vmName );
            return signingKeys;
        }
        return _signKeysCache.get( vmName );
    }

    /**
     * Removes the sign keys from cache.
     *
     * @param vm
     *            the vm
     */
    public static void removeSignKeysFromCache( IVMInstall vm ) {
        if( vm == null ) { // This could happen in import failed due to a bug
            _log.error( "No BlackBerry VM found and Signing Keys cache cannot be populated." );
            return;
        }
        if( _signKeysCache.containsKey( vm.getName() ) ) {
            _signKeysCache.remove( vm.getName() );
            _log.debug( "SigningKey object for " + vm.getName() + " is removed from the cache." );
        }
    }

    /**
     * Convert key to preference label.
     *
     * @param keyId
     *            the key id
     * @param vmName
     *            the vm name
     *
     * @return the string
     */
    public static String convertKeyToPreferenceLabel( Integer keyId, String vmName ) {
        String id = null;
        if( ( vmName != null ) ) {
            BBSigningKeys keyCache = VMUtils.getSignKeysCache().get( vmName );
            return VMUtils.convertKeyToPreferenceLabel( keyId, keyCache );
        }
        return id;
    }

    /**
     * Convert key to preference label.
     *
     * @param keyId
     *            the key id
     * @param keyCache
     *            the key cache
     *
     * @return the string
     */
    public static String convertKeyToPreferenceLabel( Integer keyId, BBSigningKeys keyCache ) {
        String id = null;
        if( ( keyId != null ) && ( keyCache != null ) ) {
            id = ( ( keyCache.getKeyName( keyId ) != null ) ? keyCache.getKeyName( keyId ).trim() : "" ) + " (0x" + Integer.toHexString( keyId.intValue() ) + ")";//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        return id;
    }

    /**
     * Convert preference label to key.
     *
     * @param preferenceLabel
     *            the preference label
     *
     * @return the integer
     */
    public static Integer convertPreferenceLabelToKey( String preferenceLabel ) {
        int begin = preferenceLabel.indexOf( "(0x" );
        int end = preferenceLabel.indexOf( ')' );
        return Integer.valueOf( Integer.parseInt( preferenceLabel.substring( begin + 3, end ), 16 ) );
    }

    /**
     * Convert code sign error msg to preference label.
     *
     * @param msg
     *            the msg
     *
     * @return the string
     */
    public static String convertCodeSignErrorMsgToPreferenceLabel( String msg ) {
        int indexA = msg.indexOf( ":" );
        String keyLabel = msg.substring( indexA + 1 );
        int indexB = keyLabel.indexOf( ":" );
        return keyLabel.substring( 0, indexB ).trim();
    }

    /**
     * Gets the hidden classes filtered by preferences.
     *
     * @param vmName
     *            the vm name
     *
     * @return the hidden classes filtered by preferences
     */
    public static List< String > getHiddenClassesFilteredByPreferences( String vmName ) {
        List< String > hiddenClasses = new ArrayList< String >();
        if( ( _signKeysCache != null ) && ( vmName != null ) && _signKeysCache.containsKey( vmName ) ) {
            BBSigningKeys keyCache = _signKeysCache.get( vmName );
            // These must be generated through addition, as keyCache.getProtectedClasses() contains classes
            // not associated with any of the desired keys.
            for( int key : keyCache.getKeys() ) {
                Integer iKey = Integer.valueOf( key );
                if( !WarningsPreferences.getWarnStatus( VMUtils.convertKeyToPreferenceLabel( iKey, vmName ) ) ) {
                    hiddenClasses.addAll( keyCache.getClassesByKey( iKey ) );
                }
            }
        }
        return hiddenClasses;
    }

    /**
     * This method returns the list of BlackBerry specific VMs (JREs).
     *
     * @return bbVMlist List<IVMInstall>
     */
    public static List< IVMInstall > getInstalledBBVMs() {
        final IVMInstallType bbVMType = JavaRuntime.getVMInstallType( BlackBerryVMInstallType.VM_ID );
        if( bbVMType != null ) {
            return Arrays.asList( bbVMType.getVMInstalls() );
        }

        return new ArrayList< IVMInstall >();

    }

    /**
     * Gets all the installed VMs.
     *
     * @return the installed VMs
     */
    public static IVMInstall[] getInstalledVMs() {
        List< IVMInstall > vms = new ArrayList< IVMInstall >();
        for( IVMInstallType vmType : JavaRuntime.getVMInstallTypes() ) {
            for( IVMInstall vm : vmType.getVMInstalls() ) {
                vms.add( vm );
            }
        }
        return vms.toArray( new IVMInstall[ vms.size() ] );
    }

    /**
     * Gets the Installed BlackBerry specific VMs (JRE) with the matching ID.
     *
     * @param name
     *            the name
     *
     * @return the bBVM
     */
    public static IVMInstall getBBVM( String vmID ) {
        final IVMInstallType bbVMType = JavaRuntime.getVMInstallType( BlackBerryVMInstallType.VM_ID );
        if( bbVMType != null ) {
            return bbVMType.findVMInstall( vmID );
        }
        return null;
    }

    /**
     * This method returns the HashMap of BlackBerry specific VMs (JREs) Java doc locations with VMs names as a key.
     *
     * @return bbVMMap Map< String, String >
     */
    public static ArrayList< Map.Entry< String, String >> getJREDocsLocation() {
        Map< String, String > bbVMMap = new HashMap< String, String >();

        for( IVMInstall vm : VMUtils.getInstalledBBVMs() ) {
            bbVMMap.put( vm.getName(), vm.getJavadocLocation().toString() );
        }
        ArrayList< Map.Entry< String, String >> bbVMList = new ArrayList< Map.Entry< String, String >>( bbVMMap.entrySet() );
        Collections.sort( bbVMList, new VMMapEntryComparator() );
        return bbVMList;
    }

    /**
     * This method returns the default BlackBerry specific VM (JRE) based on following logic: When user already selected a
     * BlackBerry VM as a default installed VM then this method will return the default installed VM other wise it will iterate
     * through the BB specific VMs and pick the highest version as a default or return null when there is no BB specific VM is
     * available for iteration.
     *
     * @return defaultVM IVMInstall
     */
    public static IVMInstall getDefaultBBVM() {
        IVMInstall defaultVm = JavaRuntime.getDefaultVMInstall();

        if( ( null == defaultVm ) || !BlackBerryVMInstallType.VM_ID.equals( defaultVm.getVMInstallType().getId() ) ) {
            defaultVm = null; // reset the default VM
            String vmId = null;
            // no default is found from workspace preferences and default
            // installed VM is either null or no BlackBerry type
            for( final IVMInstall vm : VMUtils.getInstalledBBVMs() ) {
                if( BlackBerryVMInstallType.VM_ID.equals( vm.getVMInstallType().getId() ) ) {
                    vmId = vm.getId();
                    if( defaultVm == null ) {
                        defaultVm = vm;
                    } else {
                        if( defaultVm.getId().compareTo( vmId ) < 0 ) {
                            defaultVm = vm;
                        }
                    }
                }
            }
        }

        return defaultVm; // when no bb vm exists returns null
    }

    /**
     * Creates a new VM based on the attributes specified in the given execution environment description file. The format of the
     * file is defined by <code>http://wiki.eclipse.org/Execution_Environment_Descriptions</code>.
     *
     * @param eeFile
     *            VM definition file
     * @param name
     *            name for the VM, or <code>null</code> if a default name should be assigned
     * @param id
     *            id to assign to the new VM
     *
     * @return VM standin
     *
     * @throws CoreException
     *             the core exception
     *
     * @exception CoreException
     *                if unable to create a VM from the given definition file
     */
    public static VMStandin createVMFromDefinitionFile( final File eeFile, String name, final String id ) throws CoreException {
        if( ( null == eeFile ) || !eeFile.exists() || !eeFile.isFile() ) {
            return null;
        }

        synchronized( eeFile ) {
            final ExecutionEnvironmentDescription description = new ExecutionEnvironmentDescription( eeFile );
            final BlackBerryVMInstallType bbType = (BlackBerryVMInstallType) JavaRuntime
                    .getVMInstallType( BlackBerryVMInstallType.VM_ID );

            final IStatus defFileValidityStatus = BlackBerryVMInstallType.validateDefinitionFile( description );

            if( defFileValidityStatus.isOK() ) {
                final VMStandin standin = new VMStandin( bbType, id );
                String vmName = name;
                if( ( name == null ) || ( name.length() <= 0 ) ) {
                    vmName = description.getProperty( ExecutionEnvironmentDescription.EE_NAME );
                    if( vmName == null ) {
                        vmName = eeFile.getName();
                    }
                }
                standin.setName( vmName );

                final String home = description.getProperty( ExecutionEnvironmentDescription.JAVA_HOME );

                final File homeFile = new File( home );
                final IStatus installLocValidityStatus = bbType.validateInstallLocation( homeFile );
                if( !installLocValidityStatus.isOK() ) {
                    throw new CoreException( installLocValidityStatus );
                }

                standin.setInstallLocation( new File( home ) );
                standin.setLibraryLocations( description.getLibraryLocations() );
                standin.setVMArgs( description.getVMArguments() );
                standin.setJavadocLocation( BlackBerryVMInstallType.getJavadocLocation( description.getProperties() ) );

                standin.setAttribute( BlackBerryVMInstallType.ATTR_EXECUTION_ENVIRONMENT_ID,
                        description.getProperty( ExecutionEnvironmentDescription.CLASS_LIB_LEVEL ) );

                File exe = description.getExecutable();

                if( exe == null ) {
                    exe = description.getConsoleExecutable();
                }
                if( exe != null ) {
                    try {
                        standin.setAttribute( BlackBerryVMInstallType.ATTR_JAVA_EXE, exe.getCanonicalPath() );
                    } catch( final IOException e ) {
                        throw new CoreException( new Status( IStatus.ERROR, ContextManager.PLUGIN_ID,
                                LaunchingMessages.JavaRuntime_24, e ) );
                    }
                }
                standin.setAttribute( BlackBerryVMInstallType.ATTR_JAVA_VERSION,
                        description.getProperty( ExecutionEnvironmentDescription.LANGUAGE_LEVEL ) );
                standin.setAttribute( BlackBerryVMInstallType.ATTR_DEFINITION_FILE, eeFile.getPath() );
                standin.setAttribute( BlackBerryVMInstallType.ATTR_DEBUG_ARGS,
                        description.getProperty( ExecutionEnvironmentDescription.DEBUG_ARGS ) );
                standin.setAttribute( BlackBerryVMInstallType.ATTR_RAPC_OUTPUT_FOLDER,
                        description.getProperty( BlackBerryVMInstallType.EE_RAPC_OUTPUT_FOLDER ) );
                standin.setAttribute( BlackBerryVMInstallType.ATTR_CLASSPATH_PROVIDER,
                        RIMClasspathProvider.RIM_CLASSPATH_PROVIDER_ID );
                standin.setAttribute( BlackBerryVMInstallType.ATTR_DESCRIPTION,
                        description.getProperty( BlackBerryVMInstallType.EE_DESCRIPTION ) );
                standin.setAttribute( BlackBerryVMInstallType.ATTR_INTERNAL,
                        description.getProperty( BlackBerryVMInstallType.EE_INTERNAL ) );
                standin.setAttribute( BlackBerryVMInstallType.ATTR_DIRECTIVE,
                        description.getProperty( BlackBerryVMInstallType.EE_DIRECTIVE ) );
                standin.setAttribute( BlackBerryVMInstallType.ATTR_VERSION,
                        description.getProperty( BlackBerryVMInstallType.EE_VERSION ) );
                return standin;
            }
            _log.error( "Failed createVMFromDefinitionFile: " + eeFile.getPath() + "| " + defFileValidityStatus );
            throw new CoreException( defFileValidityStatus );
        }
    }

    /**
     * Creates a new VM based on the attributes specified in the given execution environment description file. The format of the
     * file is defined by <code>http://wiki.eclipse.org/Execution_Environment_Descriptions</code>.
     *
     * @param eeFile
     *            VM definition file
     * @param force
     *            Boolean whether to force creation of VM depsite naming conflicts
     *
     * @return VM standin
     *
     * @throws CoreException
     *             the core exception
     *
     * @exception CoreException
     *                if unable to create a VM from the given definition file
     */
    public static VMStandin createVMFromDefinitionFile( final File eeFile, boolean force ) throws CoreException {
        VMStandin vm = null;
        final BlackBerryVMInstallType blackBerryVMInstallType = (BlackBerryVMInstallType) JavaRuntime
                .getVMInstallType( BlackBerryVMInstallType.VM_ID );

        if( blackBerryVMInstallType == null ) {
            throw new CoreException( new Status( IStatus.WARNING, ContextManager.PLUGIN_ID,
                    "Could not find instance of BlackBerry VM Install Type" ) );
        }

        final String vmName = VMUtils.getPropertyFromEEFile( eeFile, ExecutionEnvironmentDescription.EE_NAME );
        _log.trace( "Attempting to create VM with name - " + vmName );

        if( force || ( VMUtils.isVMNameValid( vmName ) && ( null == blackBerryVMInstallType.findVMInstall( vmName ) ) ) ) {
            vm = VMUtils.createVMFromDefinitionFile( eeFile, vmName, vmName );
        } else {
            _log.trace( "VM Creation skipped" );
        }
        return vm;
    }

    /**
     * Checks if is vM name valid.
     *
     * @param newName
     *            the new name
     *
     * @return true, if is vM name valid
     */
    private static boolean isVMNameValid( final String newName ) {
        if( ( newName == null ) || ( newName.trim().length() == 0 ) ) {
            return false;
        }

        if( VMUtils.isDuplicateName( newName ) ) {
            return false;
        }

        final IStatus s = ResourcesPlugin.getWorkspace().validateName( newName, IResource.FILE );
        if( !s.isOK() ) {
            return false;
        }
        return true;

    }

    /**
     * Checks if is duplicate name.
     *
     * @param newName
     *            the new name
     *
     * @return true, if is duplicate name
     */
    private static boolean isDuplicateName( final String newName ) {
        final IVMInstall[] vms = JavaRuntime.getVMInstallType( BlackBerryVMInstallType.VM_ID ).getVMInstalls();
        for( final IVMInstall vm : vms ) {
            if( newName.equals( vm.getName() ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the string value of an EE property from a given EE file.
     *
     * @param eeFile
     *            the ee file
     * @param property
     *            the property
     *
     * @return the string value of the property
     */
    static public String getPropertyFromEEFile( final File eeFile, final String property ) {
        if( ( null != eeFile ) && eeFile.exists() && eeFile.isFile() ) {
            InputStream inputStream = null;
            Properties properties = null;
            WeakReference< Properties > propsStub = null;
            WeakReference< InputStream > streamStub = null;

            try {
                propsStub = new WeakReference< Properties >( new Properties() );
                properties = propsStub.get();

                streamStub = new WeakReference< InputStream >( new FileInputStream( eeFile ) );
                inputStream = streamStub.get();

                properties.load( inputStream );

                final String id = properties.getProperty( property );

                return id;
            } catch( final FileNotFoundException e ) {
                _log.error( "", e ); //$NON-NLS-1$
            } catch( final IOException e ) {
                _log.error( "", e ); //$NON-NLS-1$
            } finally {
                if( null != properties ) {
                    properties.clear();
                    if( propsStub != null ) {
                        propsStub.clear();
                        propsStub = null;
                    }
                }
                if( null != inputStream ) {
                    try {
                        inputStream.close();
                        if( streamStub != null ) {
                            streamStub.clear();
                            streamStub = null;
                        }
                    } catch( final IOException e ) {
                        _log.error( "", e ); //$NON-NLS-1$
                    }
                }
            }
        }

        return StringUtils.EMPTY;
    }

    /**
     * This method returns true when BlackBerry specific VM (JRE) is the default installed VM (JRE) in the workspace otherwise
     * false.
     *
     * @return boolean
     */
    public static boolean isBlackBerryRuntimeTheWorkspaceDefault() {
        final IVMInstall install = JavaRuntime.getDefaultVMInstall();

        if( null != install ) {
            final IVMInstallType installType = install.getVMInstallType();

            if( BlackBerryVMInstallType.VM_ID.equals( installType.getId() ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given <code>vm</code> is a BlackBerry vm.
     *
     * @param vm
     *            the vm
     *
     * @return true, if checks if is black berry vm
     */
    public static boolean isBlackBerryVM( IVMInstall vm ) {
        final IVMInstallType installType = vm.getVMInstallType();

        if( BlackBerryVMInstallType.VM_ID.equalsIgnoreCase( installType.getId() ) ) {
            return true;
        }
        return false;
    }

    /**
     * The Class VMComparator.
     */
    static class VMMapEntryComparator implements Comparator< Map.Entry< String, ? >> {

        /*
         * (non-Javadoc)
         *
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare( final Map.Entry< String, ? > obj1, final Map.Entry< String, ? > obj2 ) {
            final String word1 = obj1.getKey();
            final String word2 = obj2.getKey();
            return word1.compareToIgnoreCase( word2 );
        }
    }

    /**
     * The Class VMComparator.
     */
    static public class VMGeneralComparator implements Comparator< IVMInstall > {

        /*
         * (non-Javadoc)
         *
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare( final IVMInstall obj1, final IVMInstall obj2 ) {
            final BlackBerrySDKInstall bbVM1 = (BlackBerrySDKInstall) obj1;
            final BlackBerrySDKInstall bbVM2 = (BlackBerrySDKInstall) obj2;
            final Version version1 = new Version( bbVM1.getVMVersion() );
            final Version version2 = new Version( bbVM2.getVMVersion() );
            return version1.compareTo( version2 );
        }
    }

    /**
     * The class VMVersionComparator.
     *
     * @author bkurz
     *
     */
    public static class VMVersionComparator implements Comparator< String > {
        @Override
        public int compare( String o1, String o2 ) {
            String[] tokensFirstVersion = o1.split( "[.]" );
            String[] tokensSecondVersion = o2.split( "[.]" );

            for( int i = 0; i < tokensFirstVersion.length; i++ ) {
                if( Integer.valueOf( tokensFirstVersion[ i ] ) < Integer.valueOf( tokensSecondVersion[ i ] ) ) {
                    return -1;
                } else if( Integer.valueOf( tokensFirstVersion[ i ] ) > Integer.valueOf( tokensSecondVersion[ i ] ) ) {
                    return 1;
                }
            }
            return 0;
        }
    }

    /**
     * Find VM by the given name.
     *
     * @param vmName
     *            The VM name to be searched
     * @return The <code>IVMInstall</code> if found; or <code>null</code>
     */
    public static IVMInstall findVMByName( String vmName ) {
        List< IVMInstall > vms = VMUtils.getInstalledBBVMs();
        for( IVMInstall vm : vms ) {
            if( vm.getName().equals( vmName ) ) {
                return vm;
            }
        }
        return null;
    }

    /**
     * Find VM by the given id.
     *
     * @param vmId
     *            The VM id to be searched
     * @return The <code>IVMInstall</code> if found; or <code>null</code>
     */
    public static IVMInstall findVMById( String vmId ) {
        List< IVMInstall > vms = VMUtils.getInstalledBBVMs();
        for( IVMInstall vm : vms ) {
            if( vm.getId().equals( vmId ) ) {
                return vm;
            }
        }
        return null;
    }

    /**
     * Returns the version of the given VM. It looks from the last segment of "ee.description". If it is not a valid version
     * string (x.x.x.x), returns default version 0.0.0.0.
     *
     * @param vm
     *            The VM
     * @return The version of the VM.
     */
    public static String getVMVersion( IVMInstall vm ) {
        BlackBerrySDKInstall bbVM = (BlackBerrySDKInstall) vm;

        String ver = bbVM.getAttribute( BlackBerryVMInstallType.ATTR_VERSION );
        // new VMs has version in ee.version property
        if( ver != null ) {
            return ver;
        }

        String desc = bbVM.getAttribute( BlackBerryVMInstallType.ATTR_DESCRIPTION );
        // Handle old VMs that do not have the description attribute
        if( desc == null ) {
            return IConstants.DEFAULT_VM_VERSION;
        }
        String[] tokens = desc.split( " " );
        String version = IConstants.DEFAULT_VM_VERSION;
        if( tokens.length > 0 ) {
            if( Pattern.matches( "[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+", tokens[ tokens.length - 1 ] ) ) {
                version = tokens[ tokens.length - 1 ];
            } else {
                _log.error( "Invalid VM version found on " + vm.getId() );
            }
        }
        return version;
    }

    /**
     * Returns the SDK with the highest version number
     *
     * @return
     */
    public static IVMInstall getLatestSDK() {
        IVMInstall latestVMInstall = null;
        VMVersionComparator comparator = new VMVersionComparator();

        List< IVMInstall > installedSDK = VMUtils.getInstalledBBVMs();
        for( IVMInstall vmInstall : installedSDK ) {
            if( latestVMInstall == null
                    || comparator.compare( VMUtils.getVMVersion( latestVMInstall ), VMUtils.getVMVersion( vmInstall ) ) == -1 ) {
                latestVMInstall = vmInstall;
            }
        }
        return latestVMInstall;
    }

    /**
     * Returns if the given vm is an internal one (aka lynx)
     *
     * @param vm
     *            The given VM
     * @return <code>true</code> if yes; otherwise returns <code>false</code>
     */
    public static boolean isInternal( IVMInstall vm ) {
        String isInternal = ( (AbstractVMInstall) vm ).getAttribute( BlackBerryVMInstallType.ATTR_INTERNAL );
        return isInternal != null && Integer.parseInt( isInternal ) == 1;
    }

    /**
     * Gets the JRE level pre-defined preprocess directive for the given <code>bbVM</code>. If the .directive attribute is
     * specified in the .ee file (JAVA SDK after 12/17/09), will take precedence
     *
     * @param bbVM
     * @return
     */
    public static String getJREDirective( BlackBerrySDKInstall bbVM ) {
        if( bbVM == null ) {
            return IConstants.EMPTY_STRING;
        }
        String bbvmd = bbVM.getAttribute( BlackBerryVMInstallType.ATTR_DIRECTIVE );
        if( bbvmd == null || bbvmd.length() == 0 ) {
            bbvmd = ContextManager.getDefault().getPreferenceStore().getString( IConstants.JRE_DIRECTIVE_PREFIX_KEY )
                    + bbVM.getAttribute( BlackBerryVMInstallType.ATTR_RAPC_OUTPUT_FOLDER );
        }
        return bbvmd;
    }

    /**
     * Get the component pack level pre-defined preprocess directive for all installed BlackBerry JRE.
     *
     * @return
     */
    static public final List< String > getAllJREDirectives() {
        List< String > directives = new ArrayList< String >();
        List< IVMInstall > vmList = VMUtils.getInstalledBBVMs();
        Collections.sort( vmList, new VMUtils.VMGeneralComparator() );
        BlackBerrySDKInstall bbVM;
        String JREDirective;
        for( int i = 0; i < vmList.size(); i++ ) {
            bbVM = (BlackBerrySDKInstall) vmList.get( i );
            JREDirective = VMUtils.getJREDirective( bbVM );
            if( !StringUtils.isBlank( JREDirective ) ) {
                directives.add( JREDirective );
            }
        }
        return directives;
    }

    /**
     * Get the component pack level pre-defined preprocess directive for the given <code>project</code>.
     *
     * @param project
     * @return
     */
    static public final String getJREDirective( IJavaProject project ) {
        BlackBerrySDKInstall bbVM;
        try {
            IVMInstall vm = JavaRuntime.getVMInstall( project );
            if( !( vm instanceof BlackBerrySDKInstall ) ) {
                if ( vm != null ){
                    _log.trace( vm.getName() + " is not a BlackBerry JRE" );
                }else{
                    _log.trace( project.getProject().getName() + " does not have a valid BlackBerry JRE" );
                }
                return IConstants.EMPTY_STRING;
            }
            bbVM = (BlackBerrySDKInstall) JavaRuntime.getVMInstall( project );
            return VMUtils.getJREDirective( bbVM );
        } catch( CoreException e ) {
            _log.error( e.getMessage() );
            return IConstants.EMPTY_STRING;
        }
    }
}
