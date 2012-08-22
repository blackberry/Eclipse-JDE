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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Properties;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BlackBerryVMInstallType;
import net.rim.ejde.internal.ui.dialogs.VCWarningDialog;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;
import net.rim.ide.OSUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;

/**
 * The Class is used to manage VM tools. It copies the tools (e.g. SignatureTool.jar, JavaLoader.exe, etc) from the highest
 * version of the VM installed into a local cache. If the VM is uninstalled, the local cache will be removed.
 *
 * @author jheifetz
 */
public class VMToolsUtils {

    static private final Logger _log = Logger.getLogger( VMToolsUtils.class );
    private static IPath _storedVMToolFolder;
    private static IPath _oldVMToolFolder;

    /**
     * Gets the VM tools folder name.
     *
     * @return the VM tools folder name
     */
    public static String getVMToolsFolderName() {
        return ContextManager.getDefault().getPreferenceStore().getString( IConstants.VMTOOLS_LOCATION_KEY );
    }

    /**
     * Gets the bundle data folder name.
     *
     * @return the bundle data folder name
     */
    public static String getBundleDataFolderName() {
        return ContextManager.getDefault().getPreferenceStore().getString( IConstants.BUNDLE_DATA_LOCATION_KEY );
    }

    /**
     * Gets the VM tools folder path.
     *
     * @return the VM tools folder path
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static IPath getVMToolsFolderPath() throws IOException {
        if( _storedVMToolFolder == null ) {
            Bundle bundle = Platform.getBundle( ContextManager.PLUGIN_ID );
            FileLocator.resolve( FileLocator.find( bundle, Path.ROOT, null ) );
            URL bundleURL = FileLocator.resolve( FileLocator.find( bundle, Path.ROOT, null ) );
            String bundlePath = bundleURL.getFile();
            bundlePath = bundlePath.substring( bundlePath.indexOf( IPath.SEPARATOR ) + 1 );
            _storedVMToolFolder = new Path( bundlePath ).removeLastSegments( 1 ).append(
                    VMToolsUtils.getBundleDataFolderName() + IPath.SEPARATOR + VMToolsUtils.getVMToolsFolderName() );
        }
        return _storedVMToolFolder;
    }

    /**
     * Gets the Signature tools path.
     *
     * @return the Signature tool path
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static IPath getSignatureToolPath() throws IOException {
        return VMToolsUtils.getVMToolsFolderPath().append( IPath.SEPARATOR + IConstants.SIGNATURE_TOOL_FILE_NAME );
    }

    /**
     * Gets the JavaLoader path.
     *
     * @return the JavaLoader path
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static IPath getJavaLoaderPath() throws IOException {
        return VMToolsUtils.getVMToolsFolderPath().append( IPath.SEPARATOR + IConstants.JAVA_LOADER_FILE_NAME );
    }

    /**
     * Gets the version file path.
     *
     * @return the version path
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static IPath getVersionFilePath() throws IOException {
        return VMToolsUtils.getVMToolsFolderPath().append( IPath.SEPARATOR + IConstants.VERSION_PROPERTY_FILE_NAME );
    }

    /**
     * Gets the path for the given tool.
     *
     * @param name
     *            The tool name
     * @return the tool path
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static IPath getToolPath( String name ) throws IOException {
        return VMToolsUtils.getVMToolsFolderPath().append( IPath.SEPARATOR + name );
    }

    /**
     * Gets the fledgehook path.
     *
     * @param file
     *            name. i.e fledgehook.exe or fledgehook.dll the vm that has been added.
     *
     * @return the fledgehook path
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static IPath getFledgeHookPath( String name ) throws IOException {
        return VMToolsUtils.getVMToolsFolderPath().append( IPath.SEPARATOR + name );
    }

    /**
     * Updates the VM tools on the addition of a VM. If the VM version is greater than the stored version, it is copied. If not,
     * nothing happens
     *
     * VMs are compared based on modification stamp of the Jar.
     *
     * @param vm
     *            the vm that has been added.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static void addVMTools( IVMInstall vm ) throws IOException {
        if( ( null == vm ) || !BlackBerryVMInstallType.VM_ID.equals( vm.getVMInstallType().getId() ) ) {
            return;
        }
        // check source attachment
        LibraryLocation[] libraryLocations = vm.getLibraryLocations();
        IPath sourcePath;
        if( libraryLocations != null ) {
            for( int i = 0; i < libraryLocations.length; i++ ) {
                sourcePath = libraryLocations[ i ].getSystemLibrarySourcePath();
                if( sourcePath == null || sourcePath.isEmpty() ) {
                    sourcePath = ImportUtils.getSourceJarPath( libraryLocations[ i ].getSystemLibraryPath() );
                    if( sourcePath.toFile().exists() ) {
                        _log.trace( "Source has been attached to library jar "
                                + libraryLocations[ i ].getSystemLibraryPath().lastSegment() + " in JRE " + vm.getName() );
                        libraryLocations[ i ].setSystemLibrarySource( sourcePath );
                    }
                }
            }
        }
        IPath sigToolPath = new Path( vm.getInstallLocation().getAbsolutePath() + IPath.SEPARATOR + IConstants.BIN_FOLD_NAME
                + IPath.SEPARATOR + IConstants.SIGNATURE_TOOL_FILE_NAME );
        File sigToolFile = sigToolPath.toFile();
        if( !sigToolFile.exists() ) {
            throw new IOException( "Cannot Find Signature Tool At: " + sigToolPath.toString() );
        }
        IPath javaLoaderPath = new Path( vm.getInstallLocation().getAbsolutePath() + IPath.SEPARATOR + IConstants.BIN_FOLD_NAME
                + IPath.SEPARATOR + IConstants.JAVA_LOADER_FILE_NAME );
        File javaLoaderFile = javaLoaderPath.toFile();
        if( !javaLoaderFile.exists() ) {
            throw new IOException( "Cannot Find JavaLoader.exe At: " + javaLoaderPath.toString() );
        }

        // if()
        // copyFledgeHookFile();
        File fledgeHookFile = null;
        File fledgeHookDllFile = null;
        if( OSUtils.isWindows() ) {
            IPath fledgeHookPath = new Path( vm.getInstallLocation().getAbsolutePath() + IPath.SEPARATOR
                    + IConstants.BIN_FOLD_NAME + IPath.SEPARATOR + IConstants.FLEDGE_HOOK_FILE_NAME );
            fledgeHookFile = fledgeHookPath.toFile();

            IPath fledgeHookDllPath = new Path( vm.getInstallLocation().getAbsolutePath() + IPath.SEPARATOR
                    + IConstants.BIN_FOLD_NAME + IPath.SEPARATOR + IConstants.FLEDGE_HOOK_DLL_FILE_NAME );
            fledgeHookDllFile = fledgeHookDllPath.toFile();
        }
        File storedSigTool = VMToolsUtils.getSignatureToolPath().toFile();
        File storedJavaLoader = VMToolsUtils.getJavaLoaderPath().toFile();
        File storedVersionFile = VMToolsUtils.getVersionFilePath().toFile();
        File storedFledgeHookFile = null;
        File storedFledgeHookDllFile = null;
        if( OSUtils.isWindows() ) {
            storedFledgeHookFile = VMToolsUtils.getFledgeHookPath( IConstants.FLEDGE_HOOK_FILE_NAME ).toFile();
            storedFledgeHookDllFile = VMToolsUtils.getFledgeHookPath( IConstants.FLEDGE_HOOK_DLL_FILE_NAME ).toFile();
        }
        // Compare the VM version against the stored version, if it is greater, copy the tools to the stored folder.
        // If the version file does not exist, just copy the tools and create the version file.
        boolean checkFlag = false;
        if( OSUtils.isWindows() ) {
            checkFlag = !storedFledgeHookFile.exists() || !storedFledgeHookDllFile.exists();
        }
        if( ( !storedVersionFile.exists() ) || ( VMUtils.getVMVersion( vm ).compareTo( getStoredVersion() ) > 0 )
                || !storedSigTool.exists() || !storedJavaLoader.exists() || checkFlag ) {
            FileUtils.copyOverwrite( sigToolFile, storedSigTool );
            FileUtils.copyOverwrite( javaLoaderFile, storedJavaLoader );
            if( !storedJavaLoader.canExecute() ) {
                storedJavaLoader.setExecutable( true );
            }
            setStoredVersion( VMUtils.getVMVersion( vm ) );
            if( OSUtils.isWindows() ) {
                if( fledgeHookFile.exists() && fledgeHookDllFile.exists() ) {
                    // copy from new cp/bin to vmTools
                    FileUtils.copyOverwrite( fledgeHookFile, storedFledgeHookFile );
                    FileUtils.copyOverwrite( fledgeHookDllFile, storedFledgeHookDllFile );
                } else {// force copy from bundle to vmTools
                    copyFledgeHookFile();
                }
            }
        }

        if( !OSUtils.isWindows() ) {
            // For non windows environments
            File jnilibFileInCP = new Path( vm.getInstallLocation().getAbsolutePath() + IPath.SEPARATOR
                    + IConstants.BIN_FOLD_NAME + IPath.SEPARATOR + IConstants.CP_JNI_LIB_2_FILE_NAME ).toFile();
            // Currently we do not copy the libRIMUsbJni.jnilib file into the VMTools directory
            // but in this code blog we make sure if such file exists then it has the executable permission.
            File storedJniLibFile = VMToolsUtils.getToolPath( IConstants.CP_JNI_LIB_2_FILE_NAME ).toFile();
            if( storedJniLibFile.exists() && ( !storedJniLibFile.canExecute() ) ) {
                storedJniLibFile.setExecutable( true );// In VMTools directory
            } else if( jnilibFileInCP.exists() && ( !jnilibFileInCP.canExecute() ) ) {
                jnilibFileInCP.setExecutable( true );// In CP
            }
        }

        // Copy csk and db file from old vm tool folder (i.e signTool) if it exists to new location
        File cskFile = VMToolsUtils.getToolPath( IConstants.CSK_FILE_NAME ).toFile();
        File dbFile = VMToolsUtils.getToolPath( IConstants.DB_FILE_NAME ).toFile();
        if( !cskFile.exists() || !dbFile.exists() ) {
            // try to copy the files from old signTool folder if there is
            File oldCskFile = new File( VMToolsUtils.getOldVMToolsFolderPath() + File.separator + IConstants.CSK_FILE_NAME );
            File oldDbFile = new File( VMToolsUtils.getOldVMToolsFolderPath() + File.separator + IConstants.DB_FILE_NAME );
            if( oldCskFile.exists() && oldDbFile.exists() ) {
                FileUtils.copyOverwrite( oldCskFile, cskFile );
                FileUtils.copyOverwrite( oldDbFile, dbFile );
                _log.info( Messages.CodeSigningPrefsPage_MessageDialogMsg8 );
            }
        }

        // show BlackBerry getting started page upon first-time Eclipse launch.
        showStartupPage();

        String eeVersion = VMUtils.getVMVersion( vm );
        if( OSUtils.isWindows() && is6OrLater( eeVersion ) && ( !WindowsRegistryReader.isVC2008RuntimeInstalled() ) ) {
            _log.debug( "Using 6.0+ but missing key FF66E9F6-83E7-3A3E-AF14-8DE9A809A6A4" );
            warnMissingVC2008( eeVersion );
        }
    }

    /**
     * Updates the VM tools on the removal of a VM. If the VM version is the same as the stored version, it is removed, and all
     * VMs are re-scanned. If not, nothing happens
     *
     * @param vm
     *            the vm that has been removed.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static void removeVMTools( IVMInstall vm ) throws IOException {
        if( ( null == vm ) || !BlackBerryVMInstallType.VM_ID.equals( vm.getVMInstallType().getId() ) ) {
            return;
        }
        String vmVersion = VMUtils.getVMVersion( vm );
        String storedVersion = getStoredVersion();
        if( vmVersion.equals( storedVersion ) ) {
            File storedSigTool = VMToolsUtils.getSignatureToolPath().toFile();
            storedSigTool.delete();
            File storedJavaLoader = VMToolsUtils.getJavaLoaderPath().toFile();
            storedJavaLoader.delete();
            File storedVersionFile = VMToolsUtils.getVersionFilePath().toFile();
            boolean deleted = storedVersionFile.delete();
            System.out.println( deleted );
            for( IVMInstall otherVMs : VMUtils.getInstalledBBVMs() ) {
                if( !otherVMs.equals( vm ) ) {
                    VMToolsUtils.addVMTools( otherVMs );
                }
            }
        }
    }

    public static String getStoredVersion() {
        String version = IConstants.DEFAULT_VM_VERSION;
        FileInputStream fis = null;
        try {
            String fileName = getVersionFilePath().toOSString();
            Properties properties = new Properties();
            fis = new FileInputStream( fileName );
            properties.load( fis );
            version = properties.getProperty( IConstants.VM_VERSION );
        } catch( IOException e ) {
            _log.error( e );
        } finally {
            if( fis != null ) {
                try {
                    fis.close();
                } catch( IOException e ) {
                }
            }
        }
        return version;
    }

    private static void setStoredVersion( String version ) throws IOException {
        Properties properties = new Properties();
        properties.setProperty( IConstants.VM_VERSION, version );
        String fileName = getVersionFilePath().toOSString();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream( fileName );
            properties.store( fos, null );
        } finally {
            if( fos != null ) {
                try {
                    fos.close();
                } catch( IOException e ) {
                }
            }
        }
    }

    /**
     * Checks if all the vm tools are valid.
     *
     * @return
     * @throws IOException
     */
    static public final boolean isVMToolValid() throws IOException {
        IPath javaLoaderPath = getJavaLoaderPath();
        IPath signatureToolPath = getSignatureToolPath();
        if( !javaLoaderPath.toFile().exists() || !signatureToolPath.toFile().exists() ) {
            addVMTools( VMUtils.getLatestSDK() );
        }
        if( !javaLoaderPath.toFile().exists() || !signatureToolPath.toFile().exists() ) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the signature tool is valid to be used.
     *
     * @return
     * @throws IOException
     */
    static public final boolean isSignatureToolValid() throws IOException {
        IPath signatureToolPath = getSignatureToolPath();
        // check if the signaturetool.jar is there
        if( !signatureToolPath.toFile().exists() ) {
            addVMTools( VMUtils.getLatestSDK() );
        }
        if( !signatureToolPath.toFile().exists() ) {
            return false;
        }
        // check if there is any csk and db file
        File cskFile = new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.CSK_FILE_NAME );
        File dbFile = new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.DB_FILE_NAME );
        if( ( !cskFile.exists() ) && ( !dbFile.exists() ) ) {
            return false;
        }
        return true;
    }

    /**
     * Copy FledgeHook.exe to the vm tools folder.
     *
     * @throws IOException
     */
    static private void copyFledgeHookFile() throws IOException {
        String[] names = { IConstants.FLEDGE_HOOK_FILE_NAME, IConstants.FLEDGE_HOOK_DLL_FILE_NAME };

        IPath location = new Path( VMToolsUtils.getVMToolsFolderPath() + File.separator );

        InputStream inputStream;
        OutputStream outputStream;
        File fledgeFile;
        byte[] buf;
        int numbytes;
        URL bundUrl;

        for( String fledgeFileName : names ) {
            inputStream = null;
            outputStream = null;

            try {
                fledgeFile = location.append( fledgeFileName ).toFile();
                Bundle bundle = Platform.getBundle( ContextManager.PLUGIN_ID );
                if( fledgeFile.exists() ) {
                    if( !fledgeFile.delete() ) {
                        _log.warn( "Could not replace file " + fledgeFile ); //$NON-NLS-1$
                        return;
                    }
                }

                bundUrl = bundle.getResource( fledgeFileName );

                if( bundUrl == null )
                    continue;

                inputStream = bundUrl.openStream();
                outputStream = new FileOutputStream( fledgeFile );
                buf = new byte[ 4096 ];
                numbytes = 0;

                while( ( numbytes = inputStream.read( buf ) ) > 0 )
                    outputStream.write( buf, 0, numbytes );

            } catch( IOException t ) {
                _log.error( t.getMessage(), t );
            } finally {
                try {
                    if( inputStream != null )
                        inputStream.close();

                    if( outputStream != null )
                        outputStream.close();
                } catch( IOException t ) {
                    _log.error( t.getMessage(), t );
                }
            }
        }
    }

    /**
     * Gets old VM tools (i.e. signTool) folder path.
     *
     * @return the old VM tools folder path
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public static IPath getOldVMToolsFolderPath() throws IOException {
        if( _oldVMToolFolder == null ) {
            Bundle bundle = Platform.getBundle( ContextManager.PLUGIN_ID );
            FileLocator.resolve( FileLocator.find( bundle, Path.ROOT, null ) );
            URL bundleURL = FileLocator.resolve( FileLocator.find( bundle, Path.ROOT, null ) );
            String bundlePath = bundleURL.getFile();
            bundlePath = bundlePath.substring( bundlePath.indexOf( IPath.SEPARATOR ) + 1 );
            _oldVMToolFolder = new Path( bundlePath ).removeLastSegments( 1 ).append(
                    VMToolsUtils.getBundleDataFolderName() + IPath.SEPARATOR + IConstants.OLD_VMTOOLS_LOCATION );
        }
        return _oldVMToolFolder;
    }

    /**
     * Displays a warning dialog indicating that no VC2008 install when running CP 6.0.
     */
    private static void warnMissingVC2008( String eeVersion ) {
        final String message = NLS.bind( Messages.MissingVC2008WarningMsg, eeVersion );
        IEclipsePreferences pref = ( new InstanceScope() ).getNode( ContextManager.PLUGIN_ID );
        boolean pop = pref.getBoolean( PreferenceConstants.POP_FOR_MISSING_VC, true );
        if( pop ) {
            final Display display = getDisplay();
            display.asyncExec( new Runnable() {

                @Override
                public void run() {
                    VCWarningDialog dialog = new VCWarningDialog( display.getActiveShell(), Messages.MissingVC2008WarningTitle,
                            message );
                    dialog.open();

                }
            } );
        }
    }

    static private Display getDisplay() {
        Display display = Display.getCurrent();
        // may be null if outside the UI thread
        if( display == null ) {
            display = Display.getDefault();
        }
        return display;
    }

    /**
     * compare eeVersion is 6.0+ CP
     */
    public static boolean is6OrLater( String eeVersion ) {
        try {
            return ( Integer.valueOf( eeVersion.substring( 0, eeVersion.indexOf( "." ) ) ).intValue() >= 6 );
        } catch( Exception e ) {
        }
        return false;
    }

    /**
     * Displays BlackBerry startup page only when first time eclipse is started.
     */
    private static void showStartupPage() {
        IEclipsePreferences pref = ( new InstanceScope() ).getNode( ContextManager.PLUGIN_ID );
        boolean showStartupPage = pref.getBoolean( PreferenceConstants.OPEN_STARTUP_PAGE_ON_ECLPSE_FIRST_START, true );
        if( showStartupPage ) {
            pref.putBoolean( PreferenceConstants.OPEN_STARTUP_PAGE_ON_ECLPSE_FIRST_START, false );
            final Display display = getDisplay();
            display.asyncExec( new Runnable() {
                @Override
                public void run() {
                    ProjectUtils.openStartupPage();
                }
            } );
        }
    }

}
