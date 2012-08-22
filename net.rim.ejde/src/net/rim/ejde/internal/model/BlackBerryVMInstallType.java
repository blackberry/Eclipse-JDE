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
package net.rim.ejde.internal.model;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.util.Messages;
import net.rim.ide.OSUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.launching.LaunchingMessages;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.AbstractVMInstallType;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.launching.environments.ExecutionEnvironmentDescription;

// TODO: Auto-generated Javadoc
/**
 * BlackBerry implementation of a VM install type.
 */
public class BlackBerryVMInstallType extends AbstractVMInstallType {

    static final private Logger log = Logger.getLogger( BlackBerryVMInstallType.class );

    static final public String VM_ID = IConstants.BB_VM_ID;
    static final public String VM_NAME = IConstants.BB_VM_NAME;

    /**
     * Attribute key for Java version property
     */
    public static final String ATTR_JAVA_VERSION = "ATTR_JAVA_VERSION"; //$NON-NLS-1$

    /**
     * Attribute key for supported execution environment by this runtime
     */
    public static final String ATTR_EXECUTION_ENVIRONMENT_ID = "ATTR_EXECUTION_ENVIRONMENT_ID"; //$NON-NLS-1$

    /**
     * Attribute key for Java executable used by this VM
     */
    public static final String ATTR_JAVA_EXE = "ATTR_JAVA_EXE"; //$NON-NLS-1$

    /**
     * Attribute key for VM debug arguments
     */
    public static final String ATTR_DEBUG_ARGS = "ATTR_DEBUG_ARGS"; //$NON-NLS-1$

    /**
     * Path to file used to define the JRE
     */
    public static final String ATTR_DEFINITION_FILE = "ATTR_DEFINITION_FILE"; //$NON-NLS-1$
    public static final String ATTR_RAPC_OUTPUT_FOLDER = "ATTR_RAPC_OUTPUT_FOLDER"; //$NON-NLS-1$
    public static final String ATTR_CLASSPATH_PROVIDER = "ATTR_CLASSPATH_PROVIDER";
    public static final String ATTR_DESCRIPTION = "ATTR_DESCRIPTION"; //$NON-NLS-1$
    public static final String ATTR_INTERNAL = "ATTR_INTERNAL"; //$NON-NLS-1$
    public static final String ATTR_DIRECTIVE = "ATTR_DIRECTIVE"; //$NON-NLS-1$
    public static final String ATTR_VERSION = "ATTR_VERSION"; //$NON-NLS-1$

    public static final String EE_RAPC_OUTPUT_FOLDER = "-Dee.output.folder.suffix"; //$NON-NLS-1$
    public static final String EE_DESCRIPTION = "-Dee.description"; //$NON-NLS-1$
    /** Indicate if the ee file is an internal (or lynx) ee file */
    public static final String EE_INTERNAL = "-Dee.internal"; //$NON-NLS-1$
    /** The pre-defined preprocess directive for this BlackBerry JRE */
    public static final String EE_DIRECTIVE = "-Dee.directive"; //$NON-NLS-1$
    public static final String EE_VERSION = "-Dee.version"; //$NON-NLS-1$

    /** List of Properties required to be defined by the BlackBerry.ee file. */
    private static final String[] REQUIRED_PROPERTIES = new String[] { ExecutionEnvironmentDescription.EXECUTABLE,
            ExecutionEnvironmentDescription.BOOT_CLASS_PATH, ExecutionEnvironmentDescription.LANGUAGE_LEVEL,
            ExecutionEnvironmentDescription.JAVA_HOME, BlackBerryVMInstallType.EE_RAPC_OUTPUT_FOLDER };

    /**
     * List of files derived from RIA._homePathFiles
     */
    private static String _binPathFilesForMac[] = { IConstants.DEFAULT_BUILD_FILE_NAME, IConstants.PREVERIFY_FILE_NAME,
            IConstants.RAPC_FILE_NAME, IConstants.RUNTIME_FILE_NAME, IConstants.SIGNATURE_TOOL_FILE_NAME,
            IConstants.JAVA_LOADER_FILE_NAME, IConstants.CP_JNI_LIB_2_FILE_NAME };

    /**
     * List of files derived from RIA._homePathFiles
     */
    private static String _binPathFiles[] = { IConstants.DEFAULT_BUILD_FILE_NAME, IConstants.PREVERIFY_FILE_NAME,
            IConstants.RAPC_FILE_NAME, IConstants.RUNTIME_FILE_NAME, IConstants.SIGNATURE_TOOL_FILE_NAME,
            IConstants.JAVA_LOADER_FILE_NAME, IConstants.CP_DLL_1_FILE_NAME, IConstants.CP_DLL_2_FILE_NAME };

    /**
     * Instantiates a new black berry vm install type.
     */
    public BlackBerryVMInstallType() {

    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.AbstractVMInstallType#doCreateVMInstall(java .lang.String)
     */
    @Override
    protected IVMInstall doCreateVMInstall( final String id ) {
        return new BlackBerrySDKInstall( this, id );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.IVMInstallType#detectInstallLocation()
     */
    @Override
    public File detectInstallLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.IVMInstallType#getDefaultLibraryLocations(java .io.File)
     */
    @Override
    public LibraryLocation[] getDefaultLibraryLocations( final File installLocation ) {
        // TODO Auto-generated method stub
        return new LibraryLocation[ 0 ];
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.IVMInstallType#getName()
     */
    @Override
    public String getName() {
        return BlackBerryVMInstallType.VM_NAME;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.launching.IVMInstallType#validateInstallLocation(java .io.File)
     */
    @Override
    public IStatus validateInstallLocation( final File installLocation ) {

        if( null == installLocation ) {
            return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, MessageFormat.format( LaunchingMessages.EEVMType_3, //$NON-NLS-1$
                    new String[] { "installLocation can not be null" } ) );
        } else if( !installLocation.exists() ) {
            return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, MessageFormat.format( LaunchingMessages.EEVMType_3, //$NON-NLS-1$
                    new String[] { installLocation.getPath() } ) );
        }
        try {
            final String eeFilePath = installLocation.getCanonicalPath();
            final String missingFile = validateBinFilesExist( eeFilePath + File.separator + IConstants.BIN_FOLD_NAME );
            if( !StringUtils.isEmpty( missingFile ) )
                return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, Messages.BlackBerryVMInstallType_Location_Err_Msg
                        + missingFile );

        } catch( final IOException e ) {
            BlackBerryVMInstallType.log.error( "", e ); //$NON-NLS-1$
            return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, "EE File does not exist" ); //$NON-NLS-1$
        }

        return Status.OK_STATUS;

    }

    /**
     * return missing file name if the bin directory doesn't contain the files expected by RIA.init() See
     * RIA.validateHomePath(String)
     *
     * @param homePath
     *            the absolute path of the directory containing JDWP.jar
     */
    private String validateBinFilesExist( final String homePath ) {
        File f;
        if( OSUtils.isWindows() ) {
            for( final String binPathFile : BlackBerryVMInstallType._binPathFiles ) {
                if( !( f = new File( homePath + File.separator + binPathFile ) ).exists() )
                    return f.getName();
            }
        } else {
            for( final String binPathFile : BlackBerryVMInstallType._binPathFilesForMac ) {
                if( !( f = new File( homePath + File.separator + binPathFile ) ).exists() )
                    return f.getName();
            }
        }
        return IConstants.EMPTY_STRING;
    }

    /**
     * Returns the specified property value from the given map, as a {@link String}, or <code>null</code> if none.
     *
     * @param properties
     *            property map
     * @param property
     *            the property
     *
     * @return value or <code>null</code>
     */
    private static String getProperty( final String property, final Map properties ) {
        return (String) properties.get( property );
    }

    /**
     * Returns the default javadoc location specified in the properties or <code>null</code> if none.
     *
     * @param properties
     *            properties map
     *
     * @return javadoc location specified in the properties or <code>null</code> if none
     */
    public static URL getJavadocLocation( final Map properties ) {
        final String javadoc = BlackBerryVMInstallType.getProperty( ExecutionEnvironmentDescription.JAVADOC_LOC, properties );
        if( ( javadoc != null ) && ( javadoc.length() > 0 ) ) {
            try {
                URL url = new URL( javadoc );
                if( "file".equalsIgnoreCase( url.getProtocol() ) ) { //$NON-NLS-1$
                    final File file = new File( url.getFile() );
                    url = file.getCanonicalFile().toURL();
                }
                return url;
            } catch( final MalformedURLException e ) {
                BlackBerryVMInstallType.log.error( "", e ); //$NON-NLS-1$
                return null;
            } catch( final IOException e ) {
                BlackBerryVMInstallType.log.error( "", e ); //$NON-NLS-1$
                return null;
            }
        }
        final String version = BlackBerryVMInstallType.getProperty( ExecutionEnvironmentDescription.LANGUAGE_LEVEL, properties );
        if( version != null )
            return StandardVMType.getDefaultJavadocLocation( version );
        return null;
    }

    /**
     * Returns a status indicating if the given definition file is valid.
     *
     * @param description
     *            the description
     *
     * @return status indicating if the given definition file is valid
     */
    public static IStatus validateDefinitionFile( final ExecutionEnvironmentDescription description ) {
        // validate required properties
        for( final String key : BlackBerryVMInstallType.REQUIRED_PROPERTIES ) {
            final String property = description.getProperty( key );
            if( property == null )
                return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, MessageFormat.format( LaunchingMessages.EEVMType_1, //$NON-NLS-1$
                        new String[] { key } ) );
            else if( key.equals( ExecutionEnvironmentDescription.EXECUTABLE ) ) {
                final IStatus propertyValidationStatus = BlackBerryVMInstallType.validatePropertyPointsToFile( property, key );
                if( !propertyValidationStatus.isOK() )
                    return propertyValidationStatus;
            } else if( key.equals( ExecutionEnvironmentDescription.BOOT_CLASS_PATH ) ) {
                final IStatus propertyValidationStatus = BlackBerryVMInstallType.validatePropertyPointsToFile( property, key );
                if( !propertyValidationStatus.isOK() )
                    return propertyValidationStatus;
                // comments this part out to support an ee file which does not contains the net_rim_api.jar
                // TODO we can remove this part of code later on if we really do not need it
                // final IStatus propertyValidationStatus = BlackBerryVMInstallType.validatePropertyPointsToFile( property, key,
                // IConstants.RIM_API_JAR );
                // if( !propertyValidationStatus.isOK() )
                // return propertyValidationStatus;
            }
        }
        return Status.OK_STATUS;
    }

    /**
     * Validate property points to file.
     *
     * @param property
     *            the property
     * @param key
     *            the key
     * @param fileName
     *            the file name
     *
     * @return The status
     */
    public static IStatus validatePropertyPointsToFile( final String property, final String key ) {
        final IPath fledgePath = new Path( property );
        final File fledgeFile = fledgePath.toFile();
        if( ( fledgeFile != null ) && fledgeFile.exists() )
            return Status.OK_STATUS;
        return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, Messages.BlackBerryVMInstallType_EE_Prop_Err_Msg1 + key
                + Messages.BlackBerryVMInstallType_EE_Prop_Err_Msg2 + fledgePath.lastSegment() );
    }

}
