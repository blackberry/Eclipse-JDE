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
import java.net.URL;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.legacy.JDEInfo;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.VMStandin;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * This class is meant to house all utils to do with the CPs and the extension used
 *
 * @author jheifetz
 */
public class ComponentPackUtils {

    static final private Logger log = Logger.getLogger( ComponentPackUtils.class );

    private static boolean _isInitialInstall = false;

    private static String SUFFIX = File.separator + IConstants.EE_FILE_LOCATION;

    /**
     * The Class ComponentPackComparator.
     */
    static public final class ComponentPackComparator implements Comparator< String > {

        /**
         * Instantiates a new component pack comparator.
         */
        public ComponentPackComparator() {
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare( final String cPack1, final String cPack2 ) {
            if( ( null == cPack1 ) || ( null == cPack2 ) )
                throw new IllegalArgumentException( Messages.ComponentPackHandler_Undefined_Object_Argument_Err_Msg );

            final int result = cPack1.compareTo( cPack2 );

            if( result < 0 )
                return 1;
            else if( result > 0 )
                return -1;
            else
                return 0;
        }

    }

    /**
     * Gets the component pack paths based on the CP extension point
     *
     * @return the component pack paths
     */
    public static Map< String, JDEInfo > getComponentPackPaths() {
        ComponentPackUtils.log.debug( "Starting Search for CPs" ); //$NON-NLS-1$
        IExtension[] extensions;
        final IExtensionRegistry registry = RegistryFactory.getRegistry();
        final IExtensionPoint point = registry.getExtensionPoint( IConstants.CP_EXTENSION_POINT_ID );
        final TreeMap< String, JDEInfo > packs = new TreeMap< String, JDEInfo >( new ComponentPackComparator() );

        if( ( null == point ) || !point.isValid() ) {
            ComponentPackUtils.log.debug( "Extention Point Null or Invalid" ); //$NON-NLS-1$
            return packs;
        }
        extensions = point.getExtensions();

        if( ( null == extensions ) || ( 0 == extensions.length ) ) {
            ComponentPackUtils.log.debug( "Extentions Null or Non-Existant" ); //$NON-NLS-1$
            return packs;
        }

        Bundle bundle;
        URL url;

        String name, version, path;
        File file;

        for( final IExtension extension : extensions ) {
            try {
                bundle = Platform.getBundle( extension.getNamespaceIdentifier() );
                final int bundleState = bundle.getState();

                if( ( bundleState != Bundle.UNINSTALLED ) && ( bundleState != Bundle.STOPPING ) ) {

                    url = FileLocator.resolve( FileLocator.find( bundle, Path.ROOT, null ) );

                    name = bundle.getHeaders().get( Constants.BUNDLE_NAME );
                    version = bundle.getHeaders().get( Constants.BUNDLE_VERSION );

                    if( StringUtils.isBlank( name ) || StringUtils.isBlank( version ) ) {
                        break;
                    }

                    file = new File( url.getFile() );

                    if( !file.exists() ) {
                        break;
                    }

                    path = file.getAbsolutePath() + ComponentPackUtils.SUFFIX;

                    ComponentPackUtils.log.debug( "CP named " + name + " was found at " + path ); //$NON-NLS-1$ //$NON-NLS-2$
                    packs.put( name, new JDEInfo( name, path, version ) );
                }
            } catch( final Throwable e ) {
                ComponentPackUtils.log.error( e.getMessage(), e );
            }
        }
        return packs;
    }

    /**
     * Public method to insure that CPs are only loaded once.
     */
    public static synchronized void initialLoad() {
        if( !_isInitialInstall ) {
            _isInitialInstall = true;
            loadAllCPPluginsAsVMs();
        }
    }

    /**
     * Searches for CPs and creates corresponding VMs for those that are found.
     */
    private static void loadAllCPPluginsAsVMs() {
        final Map< String, JDEInfo > cpPaths = ComponentPackUtils.getComponentPackPaths();
        try {
            for( final JDEInfo info : cpPaths.values() ) {

                final File bbSdkEEConfFile = new File( info.getPath() + File.separator + IConstants.EE_FILE_NAME );

                if( bbSdkEEConfFile.exists() && bbSdkEEConfFile.isFile() ) {

                    final VMStandin standin = VMUtils.createVMFromDefinitionFile( bbSdkEEConfFile, true );
                    if( standin != null ) {
                        standin.convertToRealVM();
                    }
                }
            }

            // Save changes to the preferences for storage.
            JavaRuntime.saveVMConfiguration();
        } catch( final CoreException ce ) {
            log.error( "VM Initial Load Error", ce );
        }
    }
}
