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
package net.rim.ejde.internal.launching;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.rim.ejde.internal.ui.launchers.LaunchUtils;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ide.OSUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jdt.launching.IVMInstall;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author dmeng
 */
public class DeviceProfileManager {

    private static final String RIM_SIM_PATH = File.separator + "Research In Motion" + File.separator
            + "BlackBerry Device Simulators";

    /** The logger for this class. */
    private static Logger _logger = Logger.getLogger( DeviceProfileManager.class );

    /**
     * The String value indicating that fledge is installed in the same folder as the RC file if the value is given in the RC
     * file.
     */
    private static final String INSTALL_DIR = "<install_dir>";

    private static final String FLEDGE_DAT = "fledge.dat";

    /** The full path of the folders containing the RC file for the external BlackBerry device simulators. */
    private String[] _commonSimDirs;

    /** The singleton instance */
    private static DeviceProfileManager _instance = null;

    /** VM home, device list map */
    private HashMap< String, List< DeviceInfo >> _deviceMap;

    /** VM home, default device map */
    private HashMap< String, DeviceInfo > _defaultDeviceMap;

    /**
     * Returns the singleton instance.
     *
     * @return The singleton instance
     */
    public static DeviceProfileManager getInstance() {
        if( _instance == null ) {
            _instance = new DeviceProfileManager();
        }
        return _instance;
    }

    private DeviceProfileManager() {
        _commonSimDirs = getExternalSimulatorsDirectory();
        _deviceMap = new HashMap< String, List< DeviceInfo >>();
        _defaultDeviceMap = new HashMap< String, DeviceInfo >();
    }

    /**
     * Reload device info.
     *
     * @param vm
     *            the vm
     */
    public void reloadDeviceInfo( IVMInstall vm ) {
        DeviceInfo defaultDevice;
        String vmId = vm.getId();
        List< DeviceInfo > deviceList = new ArrayList< DeviceInfo >();
        if( VMUtils.isInternal( vm ) ) {
            deviceList.addAll( readLynxDevices( vm ) );
            defaultDevice = readLynxDefaultSimulator( vm );
        } else {
            String simulatorPath = vm.getInstallLocation().getPath() + File.separator + "simulator";
            deviceList.addAll( readRCFile( true, simulatorPath ) );
            defaultDevice = readDefaultSimualtor( vm );
        }
        if( defaultDevice != null ) {
            _defaultDeviceMap.put( vmId, defaultDevice );
        }
        for( int i = 0; i < _commonSimDirs.length; i++ ) {
            deviceList.addAll( readRCFile( false, _commonSimDirs[ i ] ) );
        }
        _deviceMap.put( vmId, deviceList );
    }

    public List< DeviceInfo > getDeviceProfiles( IVMInstall vm ) {
        String vmId = vm.getId();
        List< DeviceInfo > deviceList = _deviceMap.get( vmId );
        if( deviceList == null ) {
            deviceList = new ArrayList< DeviceInfo >();
            if( VMUtils.isInternal( vm ) ) {
                deviceList.addAll( readLynxDevices( vm ) );
            } else {
                String simulatorPath = vm.getInstallLocation().getPath() + File.separator + "simulator";
                deviceList.addAll( readRCFile( true, simulatorPath ) );
            }
            for( int i = 0; i < _commonSimDirs.length; i++ ) {
                deviceList.addAll( readRCFile( false, _commonSimDirs[ i ] ) );
            }
            _deviceMap.put( vmId, deviceList );
        }
        return deviceList;
    }

    /**
     * Retrieves all internal simulator profiles for the given SDK
     *
     * @param vm
     * @return
     */
    public List< DeviceInfo > getInternalDeviceProfiles( IVMInstall vm ) {
        List< DeviceInfo > deviceProfiles = new ArrayList< DeviceInfo >();

        if( VMUtils.isInternal( vm ) ) {
            deviceProfiles.addAll( readLynxDevices( vm ) );
        } else {
            String simulatorPath = LaunchUtils.getSimualtorPath( vm );
            deviceProfiles.addAll( readRCFile( true, simulatorPath ) );
        }
        return deviceProfiles;
    }

    /**
     * Retrieves an external simulator profile for the given profile name
     *
     * @param profileName
     * @return
     */
    public DeviceInfo getExternalDeviceProfile( String profileName ) {
        List< DeviceInfo > deviceProfiles = getExternalDeviceProfiles();
        if( !StringUtils.isEmpty( profileName ) ) {
            for( DeviceInfo deviceProfile : deviceProfiles ) {
                if( deviceProfile.getBundleName().equalsIgnoreCase( profileName ) ) {
                    return deviceProfile;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all external simulator profiles
     *
     * @return
     */
    public List< DeviceInfo > getExternalDeviceProfiles() {
        List< DeviceInfo > deviceProfiles = new ArrayList< DeviceInfo >();
        for ( int i = 0; i < _commonSimDirs.length; i++ ) {
            deviceProfiles.addAll( readRCFile( false, _commonSimDirs[i] ) );
        }
        return deviceProfiles;
    }

    /**
     * Loads the RC file at the given directory and retrieves the device information from it.
     *
     * @param internal
     *            Is this the internal directory?
     * @param directory
     *            the complete filepath of the directory containing the RC file to be read
     */
    private List< DeviceInfo > readRCFile( boolean internal, String directory ) {
        // Retrieves the RC file from the given directory. Exits the method if no RC file
        // exists in the directory.
        List< File > rcFiles = getRCFile( directory );
        if( rcFiles.isEmpty() ) {
            return new ArrayList< DeviceInfo >();
        }
        List< DeviceInfo > deviceList = new ArrayList< DeviceInfo >();
        int size = rcFiles.size();
        for( int i = 0; i < size; i++ ) {
            File rcFile = rcFiles.get( i );
            // Extracts each simulator directory from the RC file and validates each one.
            try {
                // Note: The parsing done here will have to be changed if the format of the RC file is changed.
                // Right now it is assumed that the format of the line containing the simulator directory will
                // be "SimulatorDirectory[device name]-[bundle name]=[simulator directory]"
                Pattern pattern = Pattern
                        .compile( "SimulatorCommand(.+)-([^-]+)=(.+)fledge.exe(.+)\\s/handheld=(\\S+)(.+)/app-param=JvmAlxConfigFile:(\\S+)(.+)" );
                String simulatorDirectory;
                String configFile;
                BufferedReader br = new BufferedReader( new FileReader( rcFile ) );
                String line = br.readLine();
                while( line != null ) {
                    Matcher matcher = pattern.matcher( line );
                    if( matcher.matches() ) {
                        simulatorDirectory = matcher.group( 3 );
                        // remove the last "\" character
                        simulatorDirectory = simulatorDirectory.substring( 0, simulatorDirectory.length() - 1 );
                        boolean isValid;

                        // If the simulator directory is "<install_dir>", then the directory containing the
                        // RC file is used for validation.
                        if( simulatorDirectory.equals( INSTALL_DIR ) ) {
                            simulatorDirectory = directory;
                        }
                        isValid = validate( simulatorDirectory );

                        // If the simulator directory exists and it contains fledge, then the information
                        // for the device is added to the list of available devices.
                        if( !isValid ) {
                            // return;
                            break;
                        }

                        String deviceName = matcher.group( 5 );
                        String bundleName;
                        if( internal ) {
                            bundleName = IFledgeLaunchConstants.DEFAULT_SIMULATOR_BUNDLE_NAME;
                        } else {
                            bundleName = rcFile.getName();
                            // remove .rc
                            bundleName = bundleName.substring( 0, bundleName.indexOf( ".rc" ) );
                        }

                        configFile = matcher.group( 7 );
                        // remove .xml
                        configFile = configFile.substring( 0, configFile.indexOf( ".xml" ) );
                        if( simulatorDirectory != null && simulatorDirectory.length() > 0 && bundleName != null
                                && bundleName.length() > 0 && deviceName != null && deviceName.length() > 0 ) {
                            DeviceInfo deviceInfo = new DeviceInfo( bundleName, deviceName, simulatorDirectory, configFile );
                            deviceList.add( deviceInfo );
                        }
                    }
                    line = br.readLine();
                }
            } catch( Exception e ) {
                _logger.error( e.getMessage() );
            }
        }
        return deviceList;
    }

    /**
     * Returns true if the given directory exists and if it contains fledge, or false otherwise.
     *
     * @param directory
     *            the directory to validate
     * @return true if the given directory exists and if it contains fledge, or false otherwise
     */
    private boolean validate( String directory ) {
        File simulatorDirectory = new File( directory );
        if( simulatorDirectory.exists() && simulatorDirectory.isDirectory() ) {
            String[] files = simulatorDirectory.list();
            return Arrays.asList( files ).contains( FLEDGE_DAT );
        }
        return false;
    }

    /**
     * Returns a File object representing the RC file at the given directory, or null if there is no such file in there.
     *
     * @param directory
     *            the complete filepath of the directory containing the RC file to be returned
     * @return a list of File object representing the RC files at the given directory, or empty list if there is no such file in
     *         there
     */
    private List< File > getRCFile( String directory ) {
        List< File > rcFiles = new ArrayList< File >();
        File simulatorsDirectory = new File( directory );
        if( simulatorsDirectory.exists() && simulatorsDirectory.isDirectory() ) {
            File[] files = simulatorsDirectory.listFiles();
            for( int i = 0; i != files.length; i++ ) {
                String fileName = files[ i ].getName();
                if( fileName.startsWith( "SimPackage" ) && fileName.endsWith( ".rc" ) ) {
                    // return files[i];
                    rcFiles.add( files[ i ] );
                }
            }
        }
        return rcFiles;
    }

    /**
     * Returns the full path of the folders containing the RC file for the external BlackBerry device simulators.
     * Note: If simulator installer is not run as administrator on Win-7 machine, the .rc file is stored in %LocalAppData% folder;
     * the .rc file is stored in %CommonProgramFiles% folder on all other cases.
     */
    private String[] getExternalSimulatorsDirectory() {
        List< String > result = new ArrayList< String >();
        String str;
        if( OSUtils.isWindows() ) {
            try {
                // Executes a command to retrieve the filepath of the Common Files folder first.
                Process p = Runtime.getRuntime().exec( "cmd /c echo %CommonProgramFiles%" );
                BufferedReader br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
                str = br.readLine();
                br.close();
                str += RIM_SIM_PATH;
                result.add( str );

                p = Runtime.getRuntime().exec( "cmd /c echo %LocalAppData%" );
                br = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
                str = br.readLine();
                br.close();
                if( !str.equals( "%LocalAppData%" ) ) {
                    str += RIM_SIM_PATH;
                    result.add( str );
                }
            } catch( Exception e ) {
                _logger.error( e.getMessage() );
            }
        }
        return result.toArray( new String[ 0 ] );
    }

    /**
     * Returns the default device info for the given VM. returns <code>null</code> if one is not found.
     *
     * @param IVMInstall
     *            The VM
     * @return The default device info
     */
    public DeviceInfo getDefaultDevice( IVMInstall vm ) {
        String vmId = vm.getId();
        DeviceInfo defaultDevice = _defaultDeviceMap.get( vmId );
        if( defaultDevice == null ) {
            if( VMUtils.isInternal( vm ) ) {
                defaultDevice = readLynxDefaultSimulator( vm );
            } else {
                defaultDevice = readDefaultSimualtor( vm );
            }
            if( defaultDevice != null ) {
                _defaultDeviceMap.put( vmId, defaultDevice );
            }
        }
        // if there is no default device, pick the first one available
        List< DeviceInfo > allDevices = getDeviceProfiles( vm );
        if( defaultDevice == null || !allDevices.contains( defaultDevice ) ) {
            defaultDevice = allDevices.get( 0 );
        }
        return defaultDevice;
    }

    /**
     * Returns devices in the given Lynx VM.
     *
     * @param lynxVM
     *            The lynx VM
     * @return The devices
     */
    private List< DeviceInfo > readLynxDevices( IVMInstall lynxVM ) {
        String vmId = lynxVM.getId();
        List< DeviceInfo > devices = new ArrayList< DeviceInfo >();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            String fledgePath = LaunchUtils.getFledgeExePath( lynxVM );
            String simulatorPath = LaunchUtils.getSimualtorPath( lynxVM );
            Document doc = db.parse( fledgePath + File.separator + "fledge-options.xml" );
            doc.getDocumentElement().normalize();
            NodeList optionNodeLst = doc.getElementsByTagName( "option" );
            for( int i = 0; i < optionNodeLst.getLength(); i++ ) {
                Node optionNode = optionNodeLst.item( i );
                String name = ( optionNode.getAttributes().getNamedItem( "name" ) ).getNodeValue();
                if( name.equals( "handheld" ) ) {
                    NodeList valueNodeList = ( (Element) optionNode ).getElementsByTagName( "value" );
                    if( valueNodeList.getLength() > 0 ) {
                        for( int j = 0; j < valueNodeList.getLength(); j++ ) {
                            Node valueNode = valueNodeList.item( j );
                            Node deviceNameNode = valueNode.getAttributes().getNamedItem( "name" );
                            if( deviceNameNode != null ) {
                                devices.add( new DeviceInfo( vmId, deviceNameNode.getNodeValue(), simulatorPath, deviceNameNode
                                        .getNodeValue() ) );
                            } else {
                                devices.add( new DeviceInfo( vmId, valueNode.getTextContent(), simulatorPath, valueNode
                                        .getTextContent() ) );
                            }
                        }
                    }
                }
            }
        } catch( Exception e ) {
            _logger.error( e );
        }
        return devices;
    }

    /**
     * Returns the default device in the given Lynx VM.
     *
     * @param lynxVM
     *            The lynx VM
     * @return The device
     */
    private DeviceInfo readLynxDefaultSimulator( IVMInstall lynxVM ) {
        String vmId = lynxVM.getId();
        DeviceInfo defaultDevice = null;
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            String fledgePath = LaunchUtils.getFledgeExePath( lynxVM );
            Document doc = db.parse( fledgePath + File.separator + "fledge-options.xml" );
            doc.getDocumentElement().normalize();
            NodeList optionNodeLst = doc.getElementsByTagName( "option" );
            for( int i = 0; i < optionNodeLst.getLength(); i++ ) {
                Node optionNode = optionNodeLst.item( i );
                String name = ( optionNode.getAttributes().getNamedItem( "name" ) ).getNodeValue();
                if( name.equals( "handheld" ) ) {
                    NodeList defaultNodeList = ( (Element) optionNode ).getElementsByTagName( "default" );
                    if( defaultNodeList.getLength() > 0 ) {
                        Node defaultNode = defaultNodeList.item( 0 );
                        String simulatorPath = lynxVM.getInstallLocation().getPath() + File.separator + "debug";
                        defaultDevice = new DeviceInfo( vmId, defaultNode.getTextContent(), simulatorPath,
                                defaultNode.getTextContent() );
                        break;
                    }
                }
            }
        } catch( Exception e ) {
            _logger.error( e );
        }
        return defaultDevice;
    }

    private DeviceInfo readDefaultSimualtor( IVMInstall vm ) {
        DeviceInfo defaultDevice = null;
        String simulatorPath = LaunchUtils.getSimualtorPath( vm );
        File defaultSimulatorFile = new File( simulatorPath + File.separator + "defaultSimulator.bat" );
        if( defaultSimulatorFile.exists() ) {
            try {
                Pattern pattern = Pattern.compile( "fledge.exe(.+)\\s/handheld=(\\S+)(.+)/app-param=JvmAlxConfigFile:(\\S+)(.+)" );
                String configFile;
                BufferedReader br = new BufferedReader( new FileReader( defaultSimulatorFile ) );
                String line = br.readLine();
                while( line != null ) {
                    Matcher matcher = pattern.matcher( line );
                    if( matcher.matches() ) {
                        String deviceName = matcher.group( 2 );
                        String bundleName = IFledgeLaunchConstants.DEFAULT_SIMULATOR_BUNDLE_NAME;
                        configFile = matcher.group( 4 );
                        // remove .xml
                        configFile = configFile.substring( 0, configFile.indexOf( ".xml" ) );
                        if( deviceName != null && deviceName.length() > 0 && configFile != null && configFile.length() > 0 ) {
                            defaultDevice = new DeviceInfo( bundleName, deviceName, simulatorPath, configFile );
                            break;
                        }
                    }
                    line = br.readLine();
                }
            } catch( Exception e ) {
                _logger.error( e );
            }
        }
        return defaultDevice;
    }
}
