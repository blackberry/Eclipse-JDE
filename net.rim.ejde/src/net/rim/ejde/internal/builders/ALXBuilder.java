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
package net.rim.ejde.internal.builders;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.model.BlackBerrySDKInstall;
import net.rim.ejde.internal.util.PackagingUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.ProjectUtils.RRHFile;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ide.core.VMConst;
import net.rim.sdk.rc.ConvertUtil;
import net.rim.sdk.resourceutil.ResourceCollection;
import net.rim.sdk.resourceutil.ResourceCollectionFactory;
import net.rim.sdk.resourceutil.ResourceElement;
import net.rim.sdk.resourceutil.ResourceLocale;
import net.rim.sdk.resourceutil.ResourceParseException;
import net.rim.sdk.resourceutil.ResourceUtil;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.launching.IVMInstall;
import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * A simple tool to generate an ALX file based on the current project.
 *
 * This class was ripped out entirely from the JDE.
 *
 * @see net.rim.sdk.alxbuilder.AlxBuilder
 */
public final class ALXBuilder {
    private static final boolean DEBUG = false;
    // contants ----------------------------------------------------------------
    private static final String PROPERTIES = "net.rim.sdk.alxbuilder.alxbuilder";

    private static final String CLA_FILENAME = "CommandLineArg.filename"; // -filename
    private static final String CLA_APPLICATIONID = "CommandLineArg.appid"; // -applicationId
    private static final String CLA_NAME = "CommandLineArg.name"; // -name
    private static final String CLA_DESC = "CommandLineArg.description"; // -description
    private static final String CLA_VERSION = "CommandLineArg.version"; // -version
    private static final String CLA_VENDOR = "CommandLineArg.vendor"; // -vendor
    private static final String CLA_COPYRIGHT = "CommandLineArg.copyright"; // -copyright
    private static final String CLA_LANG = "CommandLineArg.lang"; // -lang - key value pairs -> id=id;name=<name>;desc=<desc>
    // there can be 0..N of this switch
    private static final String CLA_FILESET = "CommandLineArg.fileset"; // -vendor -fileset -> key value pairs:
    // dir=<dir>;files=<file1>?<file2>?...etc

    private static final String ALX_VERSION = "Version";
    private static final String ALX_TAG_LOADER = "Tag.Loader";
    private static final String ALX_TAG_LOADER_ATTR_VERSION = "Tag.Loader.Version";
    private static final String ALX_TAG_APP = "Tag.Application";
    private static final String ALX_TAG_APP_ATTR_ID = "Tag.Application.Id";
    private static final String ALX_TAG_VERSION = "Tag.Version";
    private static final String ALX_TAG_VENDOR = "Tag.Vendor";
    private static final String ALX_TAG_COPYRIGHT = "Tag.Copyright";
    private static final String ALX_TAG_LANG = "Tag.Language"; // language
    private static final String ALX_TAG_LANG_ATTR_ID = "Tag.Language.LangId"; // langid
    private static final String ALX_TAG_DESCRIPTION = "Tag.Description"; // description
    private static final String ALX_TAG_NAME = "Tag.Name"; // name
    private static final String ALX_TAG_FILESET = "Tag.FileSet"; // fileset
    private static final String ALX_TAG_FILESET_ATTRIBUTE = "Tag.FileSet.Attribute"; // Java
    private static final String ALX_TAG_FILESET_DIRECTORY = "Tag.FileSet.Directory"; // directory
    private static final String ALX_TAG_FILESET_FILES = "Tag.FileSet.Files"; // files
    private static final String ALX_TAG_ALX_IMPORT = "Tag.AlxImport"; // alx imports
    private static final String ALX_TAG_ALX_IMPORT_ATTR_ID = "Tag.AlxImport.Id"; // alx imports

    private static final String ALX_FILENAME_EXTENSION = "Filename.Extension"; // .alx
    private static final String ALX_COD_FILE_EXTENSION = "Filename.Cod.Extension"; // .cod

    private static final String VMVERSION = "1.0"; // debug only
    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"; // required by xml parser

    private static final String BLACKBERRY_VERSION = " _blackberryVersion";
    // statics -----------------------------------------------------------------
    private static AlxProperties _props = new AlxProperties( PROPERTIES );

    // inner classes -----------------------------------------------------------
    /**
     * The Class Alx.
     */
    public static final class Alx {
        private String _applicationId;
        private String _name;
        private String _desc;
        private String _version;
        private String _vendor;
        private String _copyright;
        private Vector< Language > _languages; // vector of Language objects
        private List< FileSet > _filesetList;
        private Vector< Alx > _dependencies; // further AlxElement items describing the dependencies of this module
        private Vector< AlxImport > _alxImports; // further AlxElement items describing required alx files

        /**
         * The core element of the ALX builder
         * <p>
         * This element contains all the information necessary to completely generate an ALX file
         *
         * @param appid
         *            The id attribute specifies a unique identifier for the application. You should use an ID that includes your
         *            company domain, in reverse, to ensure uniqueness (for example, net.rim.samples.contacts).
         * @param name
         *            The name element provides a descriptive name for the application, which appears in the Application Loader.
         *            It does not appear on the handheld.
         * @param desc
         *            The description element provides a brief description of the application, which appears in the Application
         *            Loader. It does not appear on the handheld.
         * @param version
         *            The version element provides the version number of the application. This version number appears in the
         *            Application Loader.
         * @param vendor
         *            The vendor element provides the name of the company that created the application. The vendor name appears in
         *            the Application Loader.
         * @param copyright
         *            The copyright element provides copyright information, which appears in the Application Loader.
         * @param languages
         *            a vector of Language objects describing application information. May be null
         * @param filesetList
         *            a list of FileSet objects describing the location and file names of the CODs to load
         * @param dependencies
         *            a vector of AlxElement objects describing further dependencies for this component. May be null.
         */
        public Alx( String appid, String name, String desc, String version, String vendor, String copyright,
                Vector< Language > languages, List< FileSet > filesetList, Vector< Alx > dependencies,
                Vector< AlxImport > alxImports ) {
            _applicationId = appid == null ? "" : appid;
            _name = name == null ? "" : Alx.encodeSpecialChars( name );
            _desc = desc == null ? "" : Alx.encodeSpecialChars( desc );
            _version = version == null ? "" : Alx.encodeSpecialChars( version );
            _vendor = vendor == null ? "" : Alx.encodeSpecialChars( vendor );
            _copyright = copyright == null ? "" : Alx.encodeSpecialChars( copyright );
            _languages = languages;
            _filesetList = filesetList;
            _dependencies = dependencies;
            _alxImports = alxImports;
            if( _dependencies == null ) {
                _dependencies = new Vector< Alx >( 0 ); // 0 length vector so the toElement routine doesn't need null checking
            }
            if( _languages == null ) {
                _languages = new Vector< Language >( 0 ); // 0 length vector so the toElement routine doesn't need null checking
            }
            if( _filesetList == null ) {
                throw new IllegalArgumentException();
            }
            if( _alxImports == null ) {
                _alxImports = new Vector< AlxImport >( 0 );
            }
        }

        /* package */Element toElement( AlxProperties props ) {
            Element e = new Element( props.getStringProperty( ALX_TAG_APP ) );
            e.attribute( props.getStringProperty( ALX_TAG_APP_ATTR_ID ), _applicationId );

            // the name
            ALXBuilder.debug( "adding name tag" );
            Element name = new Element( props.getStringProperty( ALX_TAG_NAME ), _name );
            e.add( name );

            // description
            Element desc = new Element( props.getStringProperty( ALX_TAG_DESCRIPTION ), _desc );
            e.add( desc );

            // version
            Element ver = new Element( props.getStringProperty( ALX_TAG_VERSION ), _version );
            e.add( ver );

            // vendor
            Element vendor = new Element( props.getStringProperty( ALX_TAG_VENDOR ), _vendor );
            e.add( vendor );

            // copyright
            Element copyright = new Element( props.getStringProperty( ALX_TAG_COPYRIGHT ), _copyright );
            e.add( copyright );

            // langs
            try {
                for( int i = 0; i < _languages.size(); ++i ) {
                    e.add( _languages.elementAt( i ).toElement( props ) );
                }
            } catch( ClassCastException ex ) {
                System.err.println( "AlxBuilder: languages vector contains non Language class instance!!: " + ex );
                return null;
            }

            // fileset
            for( FileSet fileSet : _filesetList ) {
                e.add( fileSet.toElement( props ) );
            }

            // dependencies
            try {
                for( int i = 0; i < _dependencies.size(); ++i ) {
                    e.add( _dependencies.elementAt( i ).toElement( props ) );
                }
            } catch( ClassCastException ex ) {
                System.err.println( "AlxBuilder: dependencies vector contains non AlxElement class instance!!: " + ex );
                return null;
            }

            // alx imports
            try {
                for( int i = 0; i < _alxImports.size(); ++i ) {
                    e.add( _alxImports.elementAt( i ).toElement( props ) );
                }
            } catch( ClassCastException ex ) {
                System.err.println( "AlxBuilder: alx imports vector contains non AlxElement class instance!!: " + ex );
                return null;
            }
            ALXBuilder.debug( "element built" );
            return e;
        }

        private static String encodeSpecialChars( String unencodedString ) {
            if( unencodedString == null ) {
                return null;
            }

            int length = unencodedString.length();
            StringBuffer buffer = new StringBuffer( length );
            for( int i = 0; i < length; ++i ) {
                char ch = unencodedString.charAt( i );
                switch( ch ) {
                    case '&':
                        buffer.append( "&amp;" );
                        break;
                    case '\'':
                        buffer.append( "&apos;" );
                        break;
                    case '"':
                        buffer.append( "&quot;" );
                        break;
                    case '<':
                        buffer.append( "&lt;" );
                        break;
                    case '>':
                        buffer.append( "&gt;" );
                        break;
                    default:
                        if( ch > 127 ) {
                            buffer.append( "&#" );
                            buffer.append( (int) ch );
                            buffer.append( ';' );
                        } else {
                            buffer.append( ch );
                        }
                        break;
                }
            }
            return buffer.toString();
        }
    }

    /**
     * The Class Language.
     */
    public static final class Language {
        private String _langid;
        private String _countrycode;
        private String _name;
        private String _description;

        /**
         * A language object for defining app name and description parameters in different languages
         *
         * @param languageId
         *            iso3 representation of the language
         * @param countrycode
         *            iso2 representation of the country, may be null
         * @param name
         *            the name of the application in the apppropriate language
         * @param description
         *            a description of the application in the appropriate language
         */
        public Language( String languageId, String countrycode, String name, String description ) {
            _langid = languageId;
            _countrycode = countrycode;
            _name = name;
            _description = description;
        }

        /* pacakge */Element toElement( AlxProperties props ) {
            Element lang = new Element( props.getStringProperty( ALX_TAG_LANG ) );
            String key = _langid + ( ( _countrycode != null ) && !_countrycode.equals( "" ) ? "_" + _countrycode : "" );
            key = key.toLowerCase(); // always look for lower case string
            ALXBuilder.debug( "looking for windows language id for: " + key );
            String windowsLangId = props.getStringProperty( key );
            lang.attribute( props.getStringProperty( ALX_TAG_LANG_ATTR_ID ), windowsLangId );
            Element name = new Element( props.getStringProperty( ALX_TAG_NAME ), _name );
            Element description = new Element( props.getStringProperty( ALX_TAG_DESCRIPTION ), _description );
            lang.add( name );
            lang.add( description );
            return lang;
        }
    }

    /**
     * The Class FileSet.
     */
    public static final class FileSet {
        private String _directory;
        private Vector< String > _files;
        private String _vmVersion;
        private String _blackberryVersion;

        /**
         * the set of files that comprise the component
         *
         * @param dir
         *            the directory from which the desktop loader will install the cods - (suggestion: the location of the main
         *            JDP file)
         * @param files
         *            a vector of strings containing the filenames
         * @param vmVersion
         *            the minimum version of the java VM with which the cod files are compatible (currently 1.0)
         * @param bbVersion
         *            the BlackBerry version the code file was compiled against
         */
        public FileSet( String dir, Vector< String > files, String vmVersion, String bbVersion ) {
            _directory = dir;
            _files = files;
            _vmVersion = vmVersion;
            _blackberryVersion = bbVersion;
        }

        /* package */Element toElement( AlxProperties props ) {
            Element fileset = new Element( props.getProperty( ALX_TAG_FILESET ) );
            fileset.attribute( props.getProperty( ALX_TAG_FILESET_ATTRIBUTE ), _vmVersion );
            fileset.attribute( BLACKBERRY_VERSION, _blackberryVersion );
            Element dir = new Element( props.getProperty( ALX_TAG_FILESET_DIRECTORY ), _directory );
            String codExtension = props.getProperty( ALX_COD_FILE_EXTENSION );
            StringBuffer sb = new StringBuffer();
            for( int i = 0; i < _files.size(); ++i ) {
                String f = _files.elementAt( i );
                if( !f.endsWith( codExtension ) ) {
                    f = f + codExtension; // add the .cod extension if not present
                }
                sb.append( f );
                sb.append( '\n' );
            }

            Element files = new Element( props.getProperty( ALX_TAG_FILESET_FILES ), sb.toString() );
            fileset.add( dir );
            fileset.add( files );
            return fileset;
        }
    }

    /**
     * The Class AlxImport.
     */
    public static final class AlxImport {
        private String _filename;

        public AlxImport( String filename ) {
            _filename = filename;
        }

        Element toElement( AlxProperties props ) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            FileInputStream in = null;

            Element alxImport = new Element( props.getStringProperty( ALX_TAG_ALX_IMPORT ) );
            try {
                DocumentBuilder builder = factory.newDocumentBuilder();

                // parse the required alx file for the application tag
                ALXBuilder.debug( "looking for external alx file: " + _filename );

                // prepend the character-set spec to contents of alx file- required by parser
                in = new FileInputStream( new File( _filename ) );
                byte[] declare = XML_DECLARATION.getBytes();
                byte[] bs = new byte[ declare.length + in.available() ];
                for( int i = 0; i < declare.length; ++i ) {
                    bs[ i ] = declare[ i ];
                }
                in.read( bs, declare.length, in.available() );
                ByteArrayInputStream bin = new ByteArrayInputStream( bs );

                Document document = builder.parse( bin );
                org.w3c.dom.NodeList list = document.getElementsByTagName( props.getStringProperty( ALX_TAG_APP ) );
                // use the first application tag found
                if( list.getLength() > 0 ) {
                    org.w3c.dom.Node applicationNode = list.item( 0 );
                    org.w3c.dom.NamedNodeMap attributes = applicationNode.getAttributes();
                    org.w3c.dom.Node idNode = attributes.getNamedItem( props.getStringProperty( ALX_TAG_APP_ATTR_ID ) );

                    alxImport.attribute( props.getProperty( ALX_TAG_ALX_IMPORT_ATTR_ID ), idNode.getNodeValue() );
                }
            } catch( SAXException sxe ) {
                System.err.println( "AlxBuilder: error parsing required alx file: " + sxe );
            } catch( ParserConfigurationException pce ) {
                System.err.println( "AlxBuilder: alx file parser can't be build: " + pce );
            } catch( IOException ioe ) {
                System.err.println( "AlxBuilder: i/o error parsing required alx file: " + ioe );
            } finally {
                try {
                    if( in != null ) {
                        in.close();
                    }
                } catch( IOException e ) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            return alxImport;
        }
    }

    /**
     * from John Dahm's IDEProperties.java class in net.rim.ide (//depot/main/Lynx/ide)
     */
    private static final class AlxProperties {
        private ResourceBundle _resources;

        public AlxProperties( String resourceName ) {
            _resources = ResourceBundle.getBundle( resourceName );
        }

        String getStringProperty( String name ) {
            try {
                return _resources.getString( name );
            } catch( MissingResourceException e ) {
                return null;
            }
        }

        String getProperty( String key ) {
            try {
                return _resources.getString( key );
            } catch( MissingResourceException e ) {
                return null;
            }
        }

    }

    private static class Element {
        private String _key;
        private String _value;
        private Vector< Element > _elements = new Vector< Element >();
        private StringBuffer _attributes = new StringBuffer();

        public Element( String key ) {
            this( key, null );
        }

        public Element( String key, String value ) {
            _key = key;
            _value = value;
        }

        public void add( Element e ) {
            _elements.addElement( e );
        }

        /**
         * add an attribute to this element
         *
         * @param key
         *            the attribute name
         * @param value
         *            the value for this attribute
         */
        public void attribute( String key, String value ) {
            _attributes.append( key );
            _attributes.append( '=' );
            _attributes.append( '"' );
            _attributes.append( value );
            _attributes.append( '"' );
        }

        public void write( Writer out, String indent ) throws IOException {
            out.write( indent );
            out.write( '<' );
            out.write( _key );
            out.write( ' ' );
            out.write( _attributes.toString() );
            if( ( _value != null ) || ( _elements.size() > 0 ) ) {
                String newindent = indent + "\t"; // add a tab
                out.write( ">\n" );
                if( _value != null ) {
                    out.write( newindent );
                    out.write( _value );
                }
                for( int i = 0; i < _elements.size(); ++i ) {
                    _elements.elementAt( i ).write( out, newindent );
                }
                out.write( '\n' );
                out.write( indent );
                out.write( "</" );
                out.write( _key );
                out.write( ">\n" );
            } else {
                out.write( "/>\n" );
            }
        }

    }

    // methods -----------------------------------------------------------------
    private ALXBuilder() {
        // empty private constructor - can't be instantiated
    }

    /**
     * Write out the ALX file. ALX files are used by the desktop tools to load applications onto devices
     *
     * @param filename
     *            the name of alx file
     * @param AlxElement
     *            the object comprising the ALX file
     */
    public static boolean write( String fileName, Alx alx ) {
        if( ( fileName == null ) || ( alx == null ) ) {
            return false;
        }
        try {
            ALXBuilder.debug( "FileName: " + fileName );
            // FileWriter fw = new FileWriter(fileName + _props.getStringProperty(ALX_FILENAME_EXTENSION));
            FileOutputStream fos = new FileOutputStream( fileName + _props.getStringProperty( ALX_FILENAME_EXTENSION ) );
            Writer fw = new BufferedWriter( new OutputStreamWriter( fos, "ISO8859_1" ) );

            ALXBuilder.debug( "adding application tag" );
            Element loader = new Element( _props.getStringProperty( ALX_TAG_LOADER ) );
            loader.attribute( _props.getStringProperty( ALX_TAG_LOADER_ATTR_VERSION ), _props.getStringProperty( ALX_VERSION ) );
            loader.add( alx.toElement( _props ) );
            loader.write( fw, "" );
            fw.close();
        } catch( IOException ex ) {
            System.err.println( "AlxBuilder.write() failed:" + ex );
            return false;
        }
        return true;
    }

    /**
     * Generates the alx based on the old method from RIA.
     *
     * @see net.rim.ide.Project.generateAlx()
     *
     * @param inherited
     *            the inherited
     * @param bbProject
     *            the java proj
     *
     *
     * @return the aLX builder. alx
     *
     * @throws CoreException
     *             the core exception
     * @throws ResourceParseException
     *             the resource parse exception
     * @throws FileNotFoundException
     *             the file not found exception
     */
    public static ALXBuilder.Alx generateAlx( HashMap inherited, BlackBerryProject bbProject ) throws CoreException,
            ResourceParseException, FileNotFoundException {
        return generateAlx( inherited, bbProject, true );
    }

    private static ALXBuilder.Alx generateAlx( HashMap inherited, BlackBerryProject bbProject, boolean checkDependency )
            throws CoreException, ResourceParseException, FileNotFoundException {
        if( inherited == null ) {
            inherited = new HashMap(); // used to pass values to projects this depends on
        }

        BlackBerryProperties properties = ContextManager.PLUGIN.getBBProperties( bbProject.getProject().getName(), false );
        String outputName = properties._packaging.getOutputFileName();
        int end = outputName.lastIndexOf( '.' ) == -1 ? outputName.length() : outputName.lastIndexOf( '.' );
        String outputNamenoext = outputName.substring( 0, end );

        // access the resource tools and generate AlxBuilder.Language instances for each langauge supported (we're
        // looking for the resources for Title and Description, if provided

        // Add the output file name to the list of files required for this app
        HashMap< String, Object > requiredFilesList = new HashMap< String, Object >(); // use a hashmap to eliminate duplicates
        requiredFilesList.put( outputName, null );

        // Languages - generate an AlxBuilder.Lanuage instance for each language supported by the app
        Vector< Language > languages = new Vector< Language >(); // a vector to hold the Lanuage instances
        // If title resource not active, it is possibly a library,skip the languages step
        String titleResourceBundleClassName = properties._resources.getTitleResourceBundleClassName();
        String titleResourceBundleKey = properties._resources.getTitleResourceBundleKey();
        String rootTitle = IConstants.EMPTY_STRING, rootDescription = IConstants.EMPTY_STRING;
        if( properties._resources.hasTitleResource() && !StringUtils.isEmpty( titleResourceBundleClassName )
                && !StringUtils.isEmpty( titleResourceBundleKey ) ) {
            ResourceCollection resources;
            Map< String, RRHFile > resourceMap = ProjectUtils.getProjectResources( bbProject );
            IFile resourceFile = resourceMap.get( properties._resources.getTitleResourceBundleClassName() ).getFile();
            if( resourceFile != null && resourceFile.exists() ) {
                resources = ResourceCollectionFactory.newResourceCollection( resourceFile.getLocation().toOSString() );
                ResourceLocale[] resourceLocales = resources.getLocales();
                String titlevalue = null, descvalue = null;
                for( ResourceLocale resourceLocale : resourceLocales ) {
                    ResourceElement titleElement = resourceLocale.getResourceElement( properties._resources
                            .getTitleResourceBundleKey() );
                    if( titleElement != null ) {
                        titlevalue = ResourceUtil.unicodeToEscaped( titleElement.getValueAsString() );
                    }
                    ResourceElement descrElement = resourceLocale.getResourceElement( properties._resources.getDescriptionId() );
                    if( descrElement != null ) {
                        descvalue = ResourceUtil.unicodeToEscaped( descrElement.getValueAsString() );
                    }
                    if( ( ( titlevalue == null ) && ( descvalue == null ) )
                            || ( ( titlevalue == null ) && ( descvalue.length() == 0 ) )
                            || ( ( titlevalue.length() == 0 ) && ( descvalue == null ) )
                            || ( ( titlevalue.length() == 0 ) && ( descvalue.length() == 0 ) ) ) {
                        continue;
                    }
                    // don't generate a language element for the root
                    if( resourceLocale.getLocaleName().equals( IConstants.EMPTY_STRING ) ) {
                        // we need to remember the root title and the description values
                        rootTitle = titlevalue;
                        rootDescription = descvalue;
                        continue;
                    }
                    Locale locale = ConvertUtil.localeValueOf( resourceLocale.getLocaleName() );
                    languages.addElement( new ALXBuilder.Language( locale.getISO3Language(), locale.getCountry(), titlevalue,
                            descvalue ) );
                }
            }
        }

        String copyright = "Copyright (c) " + Integer.toString( Calendar.getInstance().get( Calendar.YEAR ) ) + " "
                + properties._general.getVendor();

        // TODO: what directory for the FileSet? currently using the vendor name as the subdir, with '_' substituted for ' '
        /*
         * String dir = vendor.replace(' ', '_'); //String any trailing dots (windows strips them in path names for some reason)
         * dir = dir.endsWith(".") ? dir.substring(0, dir.length() - 1) : dir;
         */
        String vmversion = VMConst.DebugAPIVersionMajor + "." + VMConst.DebugAPIVersionMinor;

        // Generate all the dependency ALX objects
        Vector< Alx > dependencies = new Vector< Alx >();

        if( checkDependency ) {
            for( BlackBerryProject dependantProj : ProjectUtils.getAllReferencedProjects( bbProject ) ) {
                // we do not need to recursively check the dependent project
                dependencies.addElement( ALXBuilder.generateAlx( inherited, dependantProj, false ) );
            }
        }

        // a vector to hold the alxImport instances
        Vector< AlxImport > alxImports = new Vector< AlxImport >();

        // create a new alxImport for each filename
        IFile alxFile;
        for( String alxFileName : properties._packaging.getAlxFiles() ) {
            alxFile = bbProject.getProject().getFile( alxFileName );
            if( alxFile.exists() ) {
                alxImports.addElement( new ALXBuilder.AlxImport( alxFile.getLocation().toOSString() ) );
            }
        }

        List< IVMInstall > vmList = VMUtils.getInstalledBBVMs();
        List< ALXBuilder.FileSet > fileSetList = new ArrayList< FileSet >();
        BlackBerrySDKInstall bbVM;
        String currentVersionString;
        Version bbVersionLeftBound, bbVersionRightBound;
        String compatiableVersion;
        Collections.sort( vmList, new VMUtils.VMGeneralComparator() );
        int indexOfNextValidVM = nextValidBBVM( null, vmList, bbProject );
        while( indexOfNextValidVM >= 0 ) {
            bbVM = (BlackBerrySDKInstall) vmList.get( indexOfNextValidVM );
            currentVersionString = bbVM.getVMVersion();
            indexOfNextValidVM = nextValidBBVM( bbVM, vmList, bbProject );
            if( indexOfNextValidVM > 0 ) {
                bbVersionLeftBound = new Version( currentVersionString );
                bbVersionRightBound = new Version( bbVersionLeftBound.getMajor(), bbVersionLeftBound.getMinor(),
                        bbVersionLeftBound.getMicro() + 1 );
                compatiableVersion = "[" + currentVersionString + "," + bbVersionRightBound + ")";
            } else {
                compatiableVersion = "[" + currentVersionString + ")";
            }
            if( currentVersionString.equalsIgnoreCase( IConstants.HEADVER_VM_VERSION ) ) {
                currentVersionString = IConstants.HEADVER_VM_OUTPUTFOLDER;
            }
            fileSetList.add( new ALXBuilder.FileSet( currentVersionString, new Vector< String >( requiredFilesList.keySet() ),
                    vmversion, compatiableVersion ) );
        }
        if( properties._resources.hasTitleResource() && !StringUtils.isEmpty( titleResourceBundleClassName )
                && !StringUtils.isEmpty( titleResourceBundleKey ) ) {
            // if resource is used, we use the root title and description
            return new ALXBuilder.Alx( outputNamenoext, rootTitle, rootDescription, properties._general.getVersion(),
                    properties._general.getVendor(), copyright, languages, fileSetList, dependencies, alxImports );
        } else {
            return new ALXBuilder.Alx( outputNamenoext, properties._general.getTitle(), properties._general.getDescription(),
                    properties._general.getVersion(), properties._general.getVendor(), copyright, languages, fileSetList,
                    dependencies, alxImports );
        }

    }

    private static int nextValidBBVM( BlackBerrySDKInstall currentVM, List< IVMInstall > vmList, BlackBerryProject bbProject ) {
        BlackBerrySDKInstall bbVM;
        Version currentVersion, version;
        if( currentVM == null ) {
            currentVersion = Version.emptyVersion;
        } else {
            currentVersion = new Version( currentVM.getVMVersion() );
        }
        for( int i = 0; i < vmList.size(); i++ ) {
            bbVM = (BlackBerrySDKInstall) vmList.get( i );
            version = new Version( bbVM.getVMVersion() );
            if( version.compareTo( currentVersion ) > 0 && hasPackaged( bbVM, bbProject ) ) {
                return i;
            }
        }
        return -1;
    }

    private static boolean hasPackaged( BlackBerrySDKInstall bbVM, BlackBerryProject bbProject ) {
        String outputFolderPath = PackagingUtils.getRelativeStandardOutputFolder( bbProject, bbVM );
        IFolder outputFolder = bbProject.getProject().getFolder( outputFolderPath );
        IFile codFile = outputFolder.getFile( bbProject.getProperties()._packaging.getOutputFileName()
                + IConstants.COD_FILE_EXTENSION_WITH_DOT );
        if( codFile.exists() || codFile.getLocation().toFile().exists() ) {
            return true;
        }
        return false;
    }

    private static void debug( String msg ) {
        if( DEBUG ) {
            System.out.println( "[ALXBuilder] " + msg );
        }
    }

}
