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
package net.rim.ejde.internal.packaging;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import net.rim.ejde.internal.model.BasicBlackBerryProperties.AlternateEntryPoint;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.Icon;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PackageUtils;
import net.rim.ejde.internal.util.PackagingUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.ProjectUtils.RRHFile;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ide.core.IDEError;
import net.rim.ide.core.Util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.util.NLS;

/**
 * An instance of this class represents the rapc file of a BlackBerry Project.
 *
 */
public class RAPCFile {
    static private final Logger _log = Logger.getLogger( RAPCFile.class );
    /**
     * The maximum startup tiers available to a RIM project
     */
    public static final int MAX_STARTUP_TIER = 7;

    BlackBerryProject _bbProject;
    File _rapcFile;
    Vector< String > _rapcContents = new Vector< String >();

    /**
     * Constructs a RAPCFileContent instance.
     *
     * @param bbProject
     */
    public RAPCFile( BlackBerryProject bbProject ) {
        this._bbProject = bbProject;
    }

    /**
     * Loads the content of the RAPC file. <b>This method must be called before calling {@link #flushToFile()}.</b>
     *
     * @throws CoreException
     *
     */
    public void loadContent() throws CoreException {
        initialize();
    }

    void initialize() throws CoreException {
        BlackBerryProperties properties = _bbProject.getProperties();
        _rapcContents.addElement( "MIDlet-Name: " + _bbProject.getProject().getName() );

        String version = properties._general.getVersion();
        if( version == null || version.length() == 0 ) {
            version = "0.0";
        }
        _rapcContents.addElement( "MIDlet-Version: " + version );

        String vendor = properties._general.getVendor();
        if( vendor == null || vendor.length() == 0 ) {
            vendor = "<unknown>";
        }
        _rapcContents.addElement( "MIDlet-Vendor: " + vendor );

        String description = properties._general.getDescription();
        if( description != null && description.length() != 0 ) {
            _rapcContents.addElement( "MIDlet-Description: " + description );
        }

        _rapcContents.addElement( "MIDlet-Jar-URL: " + _bbProject.getProject().getName() + ".jar" );
        _rapcContents.addElement( "MIDlet-Jar-Size: 0" );
        _rapcContents.addElement( "MicroEdition-Profile: MIDP-2.0" );
        _rapcContents.addElement( "MicroEdition-Configuration: CLDC-1.1" );

        // handle alternate entry points
        String type = properties._application.getType();
        if( !type.equals( BlackBerryProject.LIBRARY ) ) {
            _rapcFile = getRapcFile( _bbProject );
            addMidletEntries( _rapcFile, properties, _rapcContents, 1 );

            AlternateEntryPoint[] entryPoints = properties.getAlternateEntryPoints();
            for( int i = 0; i < entryPoints.length; ++i ) {
                addMidletEntries( _rapcFile, new BlackBerryProperties( entryPoints[ i ] ), _rapcContents, i + 2 );
            }
        } else {
            _rapcContents.addElement( "RIM-Library-Flags: " + getFlags( properties, true ) );
        }

        addRimOptionsEntries( properties );
    }

    /**
     * Convert all forward slashes in the given <code>string</code> to back slashes.
     *
     * @param string
     * @return
     */
    private String convertToBackSlash( String string ) {
        return string.replace( '\\', '/' );
    }

    private void addMidletEntries( File projectFile, BlackBerryProperties properties, Vector< String > entries, int index )
            throws CoreException {
        StringBuffer sb = new StringBuffer();

        sb.append( "MIDlet-" ).append( index );
        sb.append( ": " );
        sb.append( properties._general.getTitle() );
        sb.append( ',' );
        Icon[] icons = properties._resources.getIconFiles();
        for( int i = 0; i < icons.length; i++ ) {
            if( !icons[ i ].isFocus() ) {
                // need to remove the source folder segment
                IPath iconFilePath = getSourceFolderRelativePath( icons[ i ].getCanonicalFileName() );
                sb.append( convertToBackSlash( iconFilePath.toOSString() ) );
            }
        }
        sb.append( ',' );
        if( !StringUtils.isBlank( properties._application.getMainMIDletName().trim() ) ) {
            sb.append( properties._application.getMainMIDletName() );
        } else {
            sb.append( properties._application.getMainArgs() );
        }
        entries.addElement( sb.toString() );

        boolean hasFocusIcon = false;
        // set focus icon
        for( int i = 0; i < icons.length; ++i ) {
            if( icons[ i ].isFocus() ) {
                sb.setLength( 0 );
                // we only allow one focus icon
                sb.append( "RIM-MIDlet-Icon-" ).append( index ).append( '-' ).append( 1 );
                sb.append( ": " );
                // need to remove the source folder segment
                IPath iconFilePath = getSourceFolderRelativePath( icons[ i ].getCanonicalFileName() );
                sb.append( convertToBackSlash( iconFilePath.toOSString() ) );
                sb.append( ",focused" );
                entries.addElement( sb.toString() );
                hasFocusIcon = true;
            }
        }
        if( hasFocusIcon ) {
            sb.setLength( 0 );
            sb.append( "RIM-MIDlet-Icon-Count-" ).append( index );
            sb.append( ": " );
            sb.append( 1 );
            entries.addElement( sb.toString() );
        }
        sb.setLength( 0 );
        sb.append( "RIM-MIDlet-Flags-" ).append( index );
        sb.append( ": " );
        sb.append( getFlags( properties, false ) );
        entries.addElement( sb.toString() );

        int ribbonPosition = properties._application.getHomeScreenPosition();
        if( ribbonPosition != 0 ) {
            sb.setLength( 0 );
            sb.append( "RIM-MIDlet-Position-" ).append( index );
            sb.append( ": " );
            sb.append( ribbonPosition );
            entries.addElement( sb.toString() );
        }

        // we need to check all resource related properties to make sure the resource is valid
        String titleResourceBundleClassName = properties._resources.getTitleResourceBundleClassName();
        String titleResourceBundleKey = properties._resources.getTitleResourceBundleKey();
        if( properties._resources.hasTitleResource() && !StringUtils.isEmpty( titleResourceBundleClassName )
                && !StringUtils.isEmpty( titleResourceBundleKey ) ) {
            sb.setLength( 0 );
            sb.append( "RIM-MIDlet-NameResourceBundle-" ).append( index );
            sb.append( ": " );
            sb.append( titleResourceBundleClassName );
            entries.addElement( sb.toString() );

            sb.setLength( 0 );
            sb.append( "RIM-MIDlet-NameResourceId-" ).append( index );
            sb.append( ": " );
            sb.append( getTitleResourceId( properties ) );
            entries.addElement( sb.toString() );
        }
        // check kewword resources

        String keywordResourceBundleClassName = properties.getKeywordResources().getKeywordTitleResourceBundleClassName();
        String keywordResourceBundleKey = properties.getKeywordResources().getKeywordResourceBundleKey();
        if( !StringUtils.isEmpty( keywordResourceBundleClassName ) && !StringUtils.isEmpty( keywordResourceBundleKey ) ) {
            sb.setLength( 0 );
            sb.append( "RIM-MIDlet-KeywordResourceBundle-" ).append( index );
            sb.append( ": " );
            sb.append( keywordResourceBundleClassName );
            entries.addElement( sb.toString() );

            sb.setLength( 0 );
            sb.append( "RIM-MIDlet-KeywordResourceId-" ).append( index );
            sb.append( ": " );
            sb.append( keywordResourceBundleKey );
            entries.addElement( sb.toString() );
        }
    }

    /*
     * These are internal options. There is no UI presented for them in external eJDE.
     */
    private void addRimOptionsEntries( BlackBerryProperties properties ) {
        StringBuffer sb = new StringBuffer();
        String prefix = "RIM-Options: "; //$NON-NLS-1$
        if( properties._application.isAddOn() ) {
            if( sb.length() == 0 ) {
                sb.append( prefix );
            } else {
                sb.append( "," ); //$NON-NLS-1$
            }
            sb.append( "add-on" ); //$NON-NLS-1$
        }
        _rapcContents.addElement( sb.toString() );
    }

    /**
     * Get the resource id for the given project. We look this up given the resource title key that has been chosen. We used to
     * store the id number but problems occur if people update their resources outside of the resource editor (which preserves the
     * id numbers).
     *
     * @throws CoreException
     */
    private int getTitleResourceId( BlackBerryProperties properties ) throws CoreException {
        String key = properties._resources.getTitleResourceBundleKey();
        Map< String, RRHFile > resourceMap = ProjectUtils.getProjectResources( _bbProject );
        RRHFile rrhFile = resourceMap.get( properties._resources.getTitleResourceBundleClassName() );
        if( rrhFile == null ) {
            String msg = "Could not find key information for " + properties._resources.getTitleResourceBundleClassName();
            _log.error( msg );
            throw new CoreException( StatusFactory.createErrorStatus( msg ) );
        }
        Hashtable< String, String > headerKey2Id = rrhFile.getKeyTalbe();
        if( headerKey2Id == null ) {
            String path;
            if( rrhFile.getFile() != null ) {
                path = rrhFile.getFile().getLocation().toOSString();
            } else {
                path = properties._resources.getTitleResourceBundleClassName();
            }
            String msg = NLS.bind( Messages.RAPCFIlE_NO_KEY_MSG, path );
            _log.error( msg );
            throw new CoreException( StatusFactory.createErrorStatus( msg ) );
        }
        String resourceId = headerKey2Id.get( key );
        if( resourceId == null ) {
            String msg = NLS.bind( Messages.RAPCFIlE_RESOURCE_KEY_NOT_FOUND_MSG, key );
            _log.error( msg );
            throw new CoreException( StatusFactory.createErrorStatus( msg ) );
        }
        try {
            return Integer.parseInt( resourceId );
        } catch( NumberFormatException nfe ) {
            _log.error( nfe );
            throw new CoreException( StatusFactory.createErrorStatus( nfe.getMessage() ) );
        }
    }

    /**
     * Get the path relative to the source folder of the given <code>canonicalFileName</code>.
     *
     * @param canonicalFileName
     * @return
     */
    private IPath getSourceFolderRelativePath( String canonicalFileName ) {
        IPath iconFilePath = new Path( canonicalFileName );
        IFile iconFile = _bbProject.getProject().getFile( iconFilePath );
        IContainer folder = PackageUtils.getSrcFolder( iconFile );
        if( folder == null ) {
            return iconFilePath;
        }
        return iconFilePath.removeFirstSegments( folder.getProjectRelativePath().segmentCount() );
    }

    private int getFlags( BlackBerryProperties properties, boolean forceSystemModule ) {
        // The flags are encoded into a single byte:
        // bit 0 - run on startup
        // bit 1 - system module
        // bit 2 - auto restart
        // bit 3 - unused
        // bit 4 - unused
        // bit 5 - startup tier (0 is 111b, 7 is 000b)
        // bit 6 - startup tier
        // bit 7 - startup tier
        boolean isAutostartup = properties._application.isAutostartup();
        boolean isSystemModule = properties._application.isSystemModule()
                || properties._application.getType().equals( BlackBerryProject.LIBRARY );// a lib has to be run as system module
        boolean isAutoRestart = properties._application.isAutoRestart();
        int flags = ( isAutostartup ? 1 : 0 ) + ( ( isSystemModule || forceSystemModule ) ? 2 : 0 ) + ( isAutoRestart ? 4 : 0 )
                + ( ( MAX_STARTUP_TIER - properties._application.getStartupTier() ) << 5 );
        return flags;
    }

    final static public File getRapcFile( BlackBerryProject project ) {
        IPath outputFilePath = project.getProject().getLocation();
        outputFilePath = outputFilePath.append( getRelativeRapcFilePath( project ) );
        return outputFilePath.toFile();
    }

    /**
     * Get the relative path of the rapc file of the given <code>project</code>.
     *
     * @param project
     * @return
     */
    final static public IPath getRelativeRapcFilePath( BlackBerryProject project ) {
        IPath outputFilePath = PackagingUtils.getRelativeStandardOutputFilePath( project );
        outputFilePath = outputFilePath.removeLastSegments( 1 );
        outputFilePath = outputFilePath.append( project.getProperties()._packaging.getOutputFileName() + ".rapc" );
        return outputFilePath;
    }

    /**
     * Collects a resource file content (like .rapc)
     *
     * @param relativeTo
     * @return
     * @throws IDEError
     */
    private byte[] collectResourceInformation() throws IDEError {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream out;
        try {
            out = new PrintStream( bout, false, "UTF-8" );
        } catch( UnsupportedEncodingException uee ) {
            out = new PrintStream( bout );
        }

        for( int i = 0; i < _rapcContents.size(); ++i ) {
            out.println( _rapcContents.elementAt( i ) );
        }

        out.close();
        return bout.toByteArray();
    }

    public void flushToFile() {
        // TODO Clean up old generated jad files unless it is specifically listed in
        // project
        // String type = _bbProject.getProperties()._application.getType();
        // if( type.equals( "MIDLET" ) || type.equals( "MIDLET_ENTRY" ) ) {
        // File jadFile = new File( _bbProject.getProperties()._packaging.getOutputFolder() + ".jad" );
        // if( jadFile.exists() ) {
        // boolean canDelete = true;
        // TODO how to handle the case if the jad file is part of the
        // project

        // for ( int j=0; j < _files.size(); ++j ) {
        // WorkspaceFile fn =
        // (WorkspaceFile/*Node*/)_files.elementAt(j);
        // if ( fn.getIsJad() ) {
        // if ( fn.getFile().equals( jadFile ) ) {
        // canDelete = false; // jad file is part of project, don't
        // delete!
        // }
        // }
        // }
        // if( canDelete ) { // jad file was auto-generated by rapc, delete
        // // it
        // jadFile.delete();
        // }
        // }
        // }

        File outputFile = getRapcFile( _bbProject );
        byte[] newBytes;
        try {
            newBytes = collectResourceInformation();
            if( !Util.isFileIdentical( outputFile, newBytes ) ) {
                // either old file doesn't exist or it isn't the same write out
                // the new data
                FileOutputStream fout = new FileOutputStream( outputFile );
                fout.write( newBytes );
                fout.close();
            }
        } catch( IDEError e ) {
            _log.error( e );
        } catch( FileNotFoundException e ) {
            _log.error( e );
        } catch( IOException e ) {
            _log.error( e );
        }
    }
}
