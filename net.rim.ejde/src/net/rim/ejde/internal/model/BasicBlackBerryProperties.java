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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

import net.rim.ejde.internal.model.preferences.RootPreferences;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * BasicBlackBerryProperties class holds the common properties shared by both external and internal BlackBerry properties.
 */
public class BasicBlackBerryProperties implements Serializable, Cloneable {

    private static final long serialVersionUID = 6369353351193121185L;
    public static final String DEFAULT_MODEL_VERSION = "1.1.2";
    public static final String COMPRESS_RESOURCE_OPTION = "-cr";
    public static final String CONVERT_PNG_RAPC_OPTION = "-convertpng";
    public static final String MODEL_ALIAS_HEAD = "Properties";
    public static final String PACKAGING__ALIAS_HEAD = "Packaging";
    public static final String DEFAULT_OUTPUT_FOLDER_NAME = "deliverables";
    public static final int DEFAULT_STARTUP_TIER = 7;

    @XStreamAsAttribute
    @XStreamAlias("ModelVersion")
    private String _modelVersion = getDefaultModelVersion();

    /**
     * Gets the model version.
     *
     * @return the _model_version
     */
    public String getModelVersion() {
        return _modelVersion;
    }

    /**
     * Sets the model version.
     *
     * @param version
     */
    public void setModelVersion( String version ) {
        _modelVersion = version;
    }

    /**
     * The Class Icon.
     */
    @XStreamAlias("Icon")
    public static final class Icon implements Serializable, Comparable< Icon >, Cloneable {
        private static final long serialVersionUID = 368208672208339761L;

        @XStreamAsAttribute
        @XStreamAlias("CanonicalFileName")
        protected String _canonicalFileName;

        @XStreamAsAttribute
        @XStreamAlias("IsFocus")
        private Boolean _isFocus = false;

        /**
         * Instantiates a new icon.
         *
         * @param canonicalFileName
         *            the canonical file name
         */
        public Icon( final String canonicalFileName ) {
            this( canonicalFileName, false );
        }

        /**
         * Instantiates a new icon.
         *
         * @param canonicalFileName
         *            the canonical file name
         * @param isFocus
         *            the is focus
         */
        public Icon( final String canonicalFileName, final Boolean isFocus ) {
            _canonicalFileName = canonicalFileName;
            _isFocus = isFocus;
        }

        /**
         * Gets the canonical file name.
         *
         * @return the _canonicalFileName
         */
        public String getCanonicalFileName() {
            return _canonicalFileName;
        }

        /**
         * Sets the canonical file name.
         *
         * @param canonicalFileName
         *            the canonical file name
         */
        public void setCanonicalFileName( final String canonicalFileName ) {
            _canonicalFileName = canonicalFileName;
        }

        /**
         * Checks the focus flag.
         *
         * @return the _isFocus
         */
        public Boolean isFocus() {
            return _isFocus;
        }

        /**
         * Sets the focus flag.
         *
         * @param isFocus
         *            the is focus
         */
        public void setIsFocus( final Boolean isFocus ) {
            _isFocus = isFocus;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "<Icon: file path=[" + _canonicalFileName + "], isFocus=[" + _isFocus + "]>";
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return _canonicalFileName.hashCode();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( final Object other ) {
            if( null == other ) {
                return false;
            }

            if( Icon.class.equals( other.getClass() ) ) {
                return _canonicalFileName.equals( ( (Icon) other )._canonicalFileName );
            }

            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo( final Icon other ) {
            if( null == other ) {
                throw new IllegalArgumentException( "Can't compare against undefined object/null reference!" );
            }

            return _canonicalFileName.compareTo( other._canonicalFileName );
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return new Icon( this.getCanonicalFileName(), this.isFocus() );
        }
    }

    /**
     * The Class AlternateEntryPoint.
     */
    @XStreamAlias("AlternateEntryPoint")
    public static class AlternateEntryPoint implements Comparable< AlternateEntryPoint >, Cloneable {
        @XStreamAsAttribute
        @XStreamAlias("Title")
        protected String _title = "";

        @XStreamAsAttribute
        @XStreamAlias("MainMIDletName")
        protected String _mainMIDletName = "";

        @XStreamAsAttribute
        @XStreamAlias("ArgumentsForMain")
        protected String _argsPassedToMain = "";

        @XStreamAsAttribute
        @XStreamAlias("HomeScreenPosition")
        protected Integer _homeScreenPosition = 0;

        @XStreamAsAttribute
        @XStreamAlias("StartupTier")
        protected Integer _startupTier = getDefaultStartupTier();

        @XStreamAsAttribute
        @XStreamAlias("IsSystemModule")
        protected Boolean _isSystemModule = false;

        @XStreamAsAttribute
        @XStreamAlias("IsAutostartup")
        protected Boolean _isAutostartup = false;

        @XStreamAsAttribute
        @XStreamAlias("hasTitleResource")
        protected Boolean _hasTitleResource = false;

        @XStreamAsAttribute
        @XStreamAlias("TitleResourceBundleKey")
        protected String _titleResourceBundleKey = "";

        @XStreamAsAttribute
        @XStreamAlias("TitleResourceBundleName")
        protected String _titleResourceBundleName = "";

        @XStreamAsAttribute
        @XStreamAlias("TitleResourceBundleClassName")
        protected String _titleResourceBundleClassName = "";

        @XStreamAsAttribute
        @XStreamAlias("TitleResourceBundleRelativePath")
        protected String _titleResourceBundleRelativePath = "";

        @XStreamAsAttribute
        @XStreamAlias("Icons")
        protected Icon[] _iconFiles = new Icon[] {};

        @XStreamAsAttribute
        @XStreamAlias("KeywordResources")
        protected KeywordResources _keywordResources = new KeywordResources();

        /**
         * Instantiates a new alternate entry point.
         */
        public AlternateEntryPoint() {
        }

        /**
         * Instantiates a new alternate entry point.
         *
         * @param title
         *            the title
         * @param mainMIDletName
         *            the main mi dlet name
         * @param argsPassedToMain
         *            the args passed to main
         * @param homeScreenPosition
         *            the home screen position
         * @param startupTier
         *            the startup tier
         * @param isSystemModule
         *            the is system module
         * @param isAutostartup
         *            the is autostartup
         * @param hasTitleResource
         *            the has title resource
         * @param titleResourceBundleName
         *            the title resource bundle name
         * @param titleResourceBundleKey
         *            the title resource bundle key
         * @param titleResourceBundleClassName
         *            the title resource bundle class name
         * @param titleResourceBundleRelativePath
         *            the title resource bundle relative path
         * @param iconFiles
         *            the icon files
         * @param kres
         *            the keyword resources
         */
        public AlternateEntryPoint( final String title, final String mainMIDletName, final String argsPassedToMain,
                final Integer homeScreenPosition, final Integer startupTier, final Boolean isSystemModule,
                final Boolean isAutostartup, final Boolean hasTitleResource, final String titleResourceBundleName,
                final String titleResourceBundleKey, final String titleResourceBundleClassName,
                final String titleResourceBundleRelativePath, final Icon[] iconFiles, final KeywordResources kres ) {
            if( title != null ) {
                _title = title;
            }
            if( mainMIDletName != null ) {
                _mainMIDletName = mainMIDletName;
            }
            if( argsPassedToMain != null ) {
                _argsPassedToMain = argsPassedToMain;
            }
            if( homeScreenPosition != null ) {
                _homeScreenPosition = homeScreenPosition;
            }
            if( startupTier != null ) {
                _startupTier = startupTier;
            }
            if( isSystemModule != null ) {
                _isSystemModule = isSystemModule;
            }
            if( isAutostartup != null ) {
                _isAutostartup = isAutostartup;
            }
            if( hasTitleResource != null ) {
                _hasTitleResource = hasTitleResource;
            }
            if( titleResourceBundleName != null ) {
                _titleResourceBundleName = titleResourceBundleName;
            }
            if( titleResourceBundleKey != null ) {
                _titleResourceBundleKey = titleResourceBundleKey;
            }
            if( titleResourceBundleClassName != null ) {
                _titleResourceBundleClassName = titleResourceBundleClassName;
            }
            if( titleResourceBundleRelativePath != null ) {
                _titleResourceBundleRelativePath = titleResourceBundleRelativePath;
            }
            if( iconFiles != null ) {
                _iconFiles = iconFiles;
            }
            if ( kres != null ){
                _keywordResources = kres;
            }
        }

        /**
         * Gets the title.
         *
         * @return the _title
         */
        public String getTitle() {
            return _title;
        }

        /**
         * Sets the title.
         *
         * @param title
         *            the title
         */
        public void setTitle( final String title ) {
            _title = title;
        }

        /**
         * Gets the main mi dlet name.
         *
         * @return the _mainMIDletName
         */
        public String getMainMIDletName() {
            return _mainMIDletName;
        }

        /**
         * Sets the main midlet name.
         *
         * @param mainMIDletName
         *            the _mainMIDletName to set
         */
        public void setMainMIDletName( final String mainMIDletName ) {
            _mainMIDletName = mainMIDletName;
        }

        /**
         * Gets the arguments passed to main.
         *
         * @return the _argsPassedToMain
         */
        public String getArgsPassedToMain() {
            return _argsPassedToMain;
        }

        /**
         * Sets the arguments passed to main.
         *
         * @param argsPassedToMain
         *            the args passed to main
         */
        public void setArgsPassedToMain( final String argsPassedToMain ) {
            _argsPassedToMain = argsPassedToMain;
        }

        /**
         * Gets the home screen position.
         *
         * @return the _homeScreenPosition
         */
        public Integer getHomeScreenPosition() {
            return _homeScreenPosition;
        }

        /**
         * Sets the home screen position.
         *
         * @param homeScreenPosition
         *            the _homeScreenPosition to set
         */
        public void setHomeScreenPosition( final Integer homeScreenPosition ) {
            _homeScreenPosition = homeScreenPosition;
        }

        /**
         * Gets the startup tier.
         *
         * @return the _startupTier
         */
        public Integer getStartupTier() {
            return _startupTier;
        }

        /**
         * Sets the startup tier.
         *
         * @param startupTier
         *            the _startupTier to set
         */
        public void setStartupTier( final Integer startupTier ) {
            _startupTier = startupTier;
        }

        /**
         * Checks if is system module.
         *
         * @return the _isSystemModule
         */
        public Boolean isSystemModule() {
            return _isSystemModule;
        }

        /**
         * Sets the is system module.
         *
         * @param isSystemModule
         *            the _isSystemModule to set
         */
        public void setIsSystemModule( final Boolean isSystemModule ) {
            _isSystemModule = isSystemModule;
        }

        /**
         * Checks if is autostartup.
         *
         * @return the _isAutostartup
         */
        public Boolean isAutostartup() {
            return _isAutostartup;
        }

        /**
         * Sets the is autostartup.
         *
         * @param isAutostartup
         *            the _isAutostartup to set
         */
        public void setIsAutostartup( final Boolean isAutostartup ) {
            _isAutostartup = isAutostartup;
        }

        /**
         * Gets the has title resource.
         *
         * @return the _hasTitleResource
         */
        public Boolean getHasTitleResource() {
            return _hasTitleResource;
        }

        /**
         * Sets the has title resource.
         *
         * @param hasTitleResource
         *            the _hasTitleResource to set
         */
        public void setHasTitleResource( final Boolean hasTitleResource ) {
            _hasTitleResource = hasTitleResource;
        }

        /**
         * Gets the title resource bundle name.
         *
         * @return the _resourceBundle
         */
        public String getTitleResourceBundleName() {
            return _titleResourceBundleName;
        }

        /**
         * Sets the title resource bundle name.
         *
         * @param resourceBundle
         *            the _resourceBundle to set
         */
        public void setTitleResourceBundleName( final String resourceBundle ) {
            _titleResourceBundleName = resourceBundle;
        }

        /**
         * Gets the title resource bundle key.
         *
         * @return the _titleResourceBundleKey
         */
        public String getTitleResourceBundleKey() {
            return _titleResourceBundleKey;
        }

        /**
         * Sets the title resource bundle key.
         *
         * @param key
         *            the _titleResourceBundleKey to set
         */
        public void setTitleResourceBundleKey( final String key ) {
            _titleResourceBundleKey = key;
        }

        /**
         * Gets the title resource bundle class name.
         *
         * @return the _titleResourceBundleClassName
         */
        public String getTitleResourceBundleClassName() {
            return _titleResourceBundleClassName;
        }

        /**
         * Sets the title resource bundle class name.
         *
         * @param className
         *            the _titleResourceBundleClassName to set
         */
        public void setTitleResourceBundleClassName( final String className ) {
            _titleResourceBundleClassName = className;
        }

        /**
         * Gets the title resource bundle relative path.
         *
         * @return the _titleResourceBundleRelativePath
         */
        public String getTitleResourceBundleRelativePath() {
            return _titleResourceBundleRelativePath;
        }

        /**
         * Sets the title resource bundle relative path.
         *
         * @param path
         *            the _titleResourceBundleRelativePath to set
         */
        public void setTitleResourceBundleRelativePath( final String path ) {
            _titleResourceBundleRelativePath = path;
        }

        /**
         * Gets the icon files.
         *
         * @return the _iconFiles
         */
        public Icon[] getIconFiles() {
            return _iconFiles;
        }

        /**
         * Gets the keyword resource model.
         * @return
         */
        public KeywordResources getKeywordResources(){
            return _keywordResources;
        }

        /**
         * Sets the icon files.
         *
         * @param iconFiles
         *            the icon files
         */
        public void setIconFiles( final Icon[] iconFiles ) {
            _iconFiles = iconFiles;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo( final AlternateEntryPoint o ) {
            if( null == o ) {
                throw new IllegalArgumentException( "Can't compare against undefined object/null reference!" );
            }
            return _title.compareTo( o.getTitle() );
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            // Copy icon array
            Icon icons[] = this.getIconFiles();
            Icon iconClones[] = new Icon[ icons.length ];

            for( int i = 0; i < iconClones.length; i++ ) {
                iconClones[ i ] = (Icon) icons[ i ].clone();
            }
            //
            if( this._keywordResources == null ){
                this._keywordResources = new KeywordResources();
            }
            KeywordResources kres = (KeywordResources)this._keywordResources.clone();

            return new AlternateEntryPoint( this._title, this._mainMIDletName, this._argsPassedToMain, this._homeScreenPosition,
                    this._startupTier, this._isSystemModule, this._isAutostartup, this._hasTitleResource,
                    this._titleResourceBundleName, this._titleResourceBundleKey, this._titleResourceBundleClassName,
                    this._titleResourceBundleRelativePath, iconClones, kres );
        }
    }

    /**
     * The Class General.
     */
    static public class General implements Serializable, Cloneable {
        private static final long serialVersionUID = -6163910799665124451L;

        @XStreamAsAttribute
        @XStreamAlias("Title")
        protected String _title = "";

        @XStreamAsAttribute
        @XStreamAlias("Version")
        protected String _version = RootPreferences.getProjectVersion();

        @XStreamAsAttribute
        @XStreamAlias("Vendor")
        protected String _vendor = RootPreferences.getProjectVendor();

        @XStreamAsAttribute
        @XStreamAlias("Description")
        protected String _description = "";

        /**
         * Instantiates a new general.
         */
        public General() {
        }

        /**
         * Instantiates a new general.
         *
         * @param title
         *            the title
         * @param version
         *            the version
         * @param vendor
         *            the vendor
         * @param description
         *            the description
         */
        public General( final String title, final String version, final String vendor, final String description ) {
            if( title != null ) {
                _title = title;
            }
            if( version != null ) {
                _version = version;
            }
            if( vendor != null ) {
                _vendor = vendor;
            }
            if( description != null ) {
                _description = description;
            }
        }

        /**
         * Sets the title.
         *
         * @param title
         *            the title
         */
        public void setTitle( final String title ) {
            _title = title;
        }

        /**
         * Gets the title.
         *
         * @return the _title
         */
        public String getTitle() {
            return _title;
        }

        /**
         * Sets the version.
         *
         * @param version
         *            the version
         */
        public void setVersion( final String version ) {
            _version = version;
        }

        /**
         * Gets the version.
         *
         * @return the _version
         */
        public String getVersion() {
            return _version;
        }

        /**
         * Sets the vendor.
         *
         * @param vendor
         *            the vendor
         */
        public void setVendor( final String vendor ) {
            _vendor = vendor;
        }

        /**
         * Gets the vendor.
         *
         * @return the _vendor
         */
        public String getVendor() {
            return _vendor;
        }

        /**
         * Sets the description.
         *
         * @param description
         *            the description
         */
        public void setDescription( final String description ) {
            _description = description;
        }

        /**
         * Gets the description.
         *
         * @return the _description
         */
        public String getDescription() {
            return _description;
        }
    }

    /**
     * The Class BasicApplication.
     */
    static abstract public class BasicApplication implements Serializable, Cloneable {
        private static final long serialVersionUID = -8222214091786702095L;

        @XStreamAsAttribute
        @XStreamAlias("Type")
        protected String _type = BlackBerryProject.CLDC_APPLICATION;

        @XStreamAsAttribute
        @XStreamAlias("MainMIDletName")
        protected String _mainMIDletName = "";

        @XStreamAsAttribute
        @XStreamAlias("MainArgs")
        protected String _mainArgs = "";

        @XStreamAsAttribute
        @XStreamAlias("HomeScreenPosition")
        protected Integer _homeScreenPosition = 0;

        @XStreamAsAttribute
        @XStreamAlias("StartupTier")
        protected Integer _startupTier = getDefaultStartupTier();

        @XStreamAsAttribute
        @XStreamAlias("IsSystemModule")
        protected Boolean _isSystemModule = false;

        @XStreamAsAttribute
        @XStreamAlias("IsAutostartup")
        protected Boolean _isAutostartup = false;

        /**
         * Instantiates a new application.
         */
        public BasicApplication() {

        }

        /**
         * Instantiates a new application.
         *
         * @param type
         *            the type
         * @param mainMIDletName
         *            the main mi dlet name
         * @param mainArgs
         *            the main args
         * @param homeScreenPosition
         *            the home screen position
         * @param startupTier
         *            the startup tier
         * @param isSystemModule
         *            the is system module
         * @param isAutostartup
         *            the is autostartup
         */
        public BasicApplication( final String type, final String mainMIDletName, final String mainArgs,
                final Integer homeScreenPosition, final Integer startupTier, final Boolean isSystemModule,
                final Boolean isAutostartup ) {
            if( type != null ) {
                _type = type;
            }
            if( mainMIDletName != null ) {
                _mainMIDletName = mainMIDletName;
            }
            if( mainArgs != null ) {
                _mainArgs = mainArgs;
            }
            if( homeScreenPosition != null ) {
                _homeScreenPosition = homeScreenPosition;
            }
            if( startupTier != null ) {
                _startupTier = startupTier;
            }
            if( isSystemModule != null ) {
                _isSystemModule = isSystemModule;
            }
            if( isAutostartup != null ) {
                _isAutostartup = isAutostartup;
            }
        }

        /**
         * Sets the main args.
         *
         * @param mainArgs
         *            the main args
         */
        public void setMainArgs( final String mainArgs ) {
            _mainArgs = mainArgs;
        }

        /**
         * Gets the main args.
         *
         * @return the _mainArgs
         */
        public String getMainArgs() {
            return _mainArgs;
        }

        /**
         * Sets the type.
         *
         * @param type
         *            the type
         */
        public void setType( final String type ) {
            _type = type;
        }

        /**
         * Gets the type.
         *
         * @return the _type
         */
        public String getType() {
            return _type;
        }

        /**
         * Sets the main mi dlet name.
         *
         * @param mainMIDletName
         *            the main mi dlet name
         */
        public void setMainMIDletName( final String mainMIDletName ) {
            _mainMIDletName = mainMIDletName;
        }

        /**
         * Gets the main mi dlet name.
         *
         * @return the _mainMIDletName
         */
        public String getMainMIDletName() {
            return _mainMIDletName;
        }

        /**
         * Sets the home screen position.
         *
         * @param homeScreenPosition
         *            the home screen position
         */
        public void setHomeScreenPosition( final Integer homeScreenPosition ) {
            _homeScreenPosition = homeScreenPosition;
        }

        /**
         * Gets the home screen position.
         *
         * @return the _homeScreenPosition
         */
        public Integer getHomeScreenPosition() {
            return _homeScreenPosition;
        }

        /**
         * Sets the startup tier.
         *
         * @param startupTier
         *            the startup tier
         */
        public void setStartupTier( final Integer startupTier ) {
            _startupTier = startupTier;
        }

        /**
         * Gets the startup tier.
         *
         * @return the _startupTier
         */
        public Integer getStartupTier() {
            return _startupTier;
        }

        /**
         * Sets the is system module.
         *
         * @param isSystemModule
         *            the is system module
         */
        public void setIsSystemModule( final Boolean isSystemModule ) {
            _isSystemModule = isSystemModule;
        }

        /**
         * Checks if is system module.
         *
         * @return the _isSystemModule
         */
        public Boolean isSystemModule() {
            return _isSystemModule;
        }

        /**
         * Sets the is autostartup.
         *
         * @param isAutostartup
         *            the is autostartup
         */
        public void setIsAutostartup( final Boolean isAutostartup ) {
            _isAutostartup = isAutostartup;
        }

        /**
         * Checks if is autostartup.
         *
         * @return the _isAutostartup
         */
        public Boolean isAutostartup() {
            return _isAutostartup;
        }

        /*
         * Stub method to satisfy RAPCFile. This method is implemented in the internal fragment.
         */
        public Boolean isAutoRestart() {
            return Boolean.FALSE;
        }

        /*
         * Stub method to satisfy RAPCFile. This method is implemented in the internal fragment.
         */
        public Boolean isAddOn() {
            return Boolean.FALSE;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    /**
     * The protect info class.
     */
    @XStreamAlias("ProtectInfo")
    static final public class ProtectInfo implements Serializable, Cloneable {
        private static final long serialVersionUID = -2530911546606391100L;
        @XStreamAsAttribute
        @XStreamAlias("ProtectedClassName")
        private String _protectedClassName;
        @XStreamAsAttribute
        @XStreamAlias("KeyFileName")
        private String _keyFileName;

        public ProtectInfo( String protectedClassName, String keyFileName ) {
            _protectedClassName = protectedClassName;
            _keyFileName = keyFileName;
        }

        public String getProtectedClassName() {
            return _protectedClassName;
        }

        public String getKeyFileName() {
            return _keyFileName;
        }
    }

    /**
     * The Class HiddenProperties.
     */
    @XStreamAlias("HiddenProperties")
    static final public class HiddenProperties implements Serializable, Cloneable {
        private static final long serialVersionUID = -6031402417673592476L;
        @XStreamAlias("ClassProtection")
        private ProtectInfo[] _classProtection = new ProtectInfo[ 0 ];
        @XStreamAlias("PackageProtection")
        private ProtectInfo[] _packageProtection = new ProtectInfo[ 0 ];

        /**
         * Instantiates a new hidden properties.
         */
        public HiddenProperties() {

        }

        /**
         * Instantiates a new hidden properties.
         *
         * @param packageProtection
         *            the package protection
         * @param classProtection
         *            the class protection
         */
        public HiddenProperties( final Hashtable< String, String > packageProtection,
                final Hashtable< String, String > classProtection ) {
            _packageProtection = getProtectionInfoArray( packageProtection );
            _classProtection = getProtectionInfoArray( classProtection );
        }

        private ProtectInfo[] getProtectionInfoArray( Hashtable< String, String > protections ) {
            ProtectInfo[] protectionArray = new ProtectInfo[ protections.size() ];
            Iterator< Entry< String, String >> iterator = protections.entrySet().iterator();
            Entry< String, String > entry;
            for( int i = 0; iterator.hasNext(); i++ ) {
                entry = iterator.next();
                protectionArray[ i ] = new ProtectInfo( entry.getKey(), entry.getValue() );
            }
            return protectionArray;
        }

        private Hashtable< String, String > getProtectionInfoHashtable( ProtectInfo[] protections ) {
            Hashtable< String, String > protectionTable = new Hashtable< String, String >();
            for( int i = 0; i < protections.length; i++ ) {
                protectionTable.put( protections[ i ].getProtectedClassName(), protections[ i ].getKeyFileName() );
            }
            return protectionTable;
        }

        /**
         * Gets the package protection.
         *
         * @return the package protection
         */
        public Hashtable< String, String > getPackageProtection() {
            return getProtectionInfoHashtable( _packageProtection );
        }

        /**
         * Sets the package protection.
         *
         * @param packageProection
         *            the package proection
         */
        public void setPackageProtection( final Hashtable< String, String > packageProection ) {
            _packageProtection = getProtectionInfoArray( packageProection );
        }

        /**
         * Gets the class protection.
         *
         * @return the class protection
         */
        public Hashtable< String, String > getClassProtection() {
            return getProtectionInfoHashtable( _classProtection );
        }

        /**
         * Sets the class protection.
         *
         * @param classProtection
         *            the class protection
         */
        public void setClassProtection( final Hashtable< String, String > classProtection ) {
            _classProtection = getProtectionInfoArray( classProtection );
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    /**
     * The Class Resources.
     */
    @XStreamAlias("Resources")
    static final public class Resources implements Serializable, Cloneable {
        private static final long serialVersionUID = -5971691998095776694L;

        @XStreamAsAttribute
        @XStreamAlias("hasTitleResource")
        private Boolean _hasTitleResource = false;

        @XStreamAsAttribute
        @XStreamAlias("TitleResourceBundleName")
        private String _titleResourceBundleName = "";

        @XStreamAsAttribute
        @XStreamAlias("TitleResourceBundleRelativePath")
        private String _titleResourceBundleRelativePath = "";

        @XStreamAsAttribute
        @XStreamAlias("TitleResourceBundleClassName")
        private String _titleResourceBundleClassName = "";

        @XStreamAsAttribute
        @XStreamAlias("TitleResourceBundleKey")
        private String _titleResourceBundleKey = "";

        @XStreamAsAttribute
        @XStreamAlias("DescriptionId")
        private String _descriptionId = "";

        @XStreamAlias("Icons")
        protected Icon[] _iconFiles = new Icon[] {};

        /**
         * Instantiates a new resources.
         */
        public Resources() {
        }

        /**
         * Instantiates a new resources.
         *
         * @param hasTitleResource
         *            the has title resource
         * @param titleResourceBundleName
         *            the title resource bundle name
         * @param titleResourceBundleKey
         *            the title resource bundle key
         * @param titleResourceBundleRelativePath
         *            the title resource bundle relative path
         * @param titleResourceBundleClassName
         *            the title resource bundle class name
         * @param descriptionId
         *            the description id
         * @param iconFiles
         *            the icon files
         */
        public Resources( final Boolean hasTitleResource, final String titleResourceBundleName,
                final String titleResourceBundleKey, final String titleResourceBundleRelativePath,
                final String titleResourceBundleClassName, final String descriptionId, final Icon[] iconFiles ) {
            if( hasTitleResource != null ) {
                _hasTitleResource = hasTitleResource;
            }
            if( titleResourceBundleName != null ) {
                _titleResourceBundleName = titleResourceBundleName;
            }
            if( titleResourceBundleClassName != null ) {
                _titleResourceBundleClassName = titleResourceBundleClassName;
            }
            if( titleResourceBundleRelativePath != null ) {
                _titleResourceBundleRelativePath = titleResourceBundleRelativePath;
            }
            if( titleResourceBundleKey != null ) {
                _titleResourceBundleKey = titleResourceBundleKey;
            }
            if( descriptionId != null ) {
                _descriptionId = descriptionId;
            }
            if( iconFiles != null ) {
                _iconFiles = iconFiles;
            }
        }

        /**
         * Sets the has title resource.
         *
         * @param _hasTitleResource
         *            the _hasTitleResource to set
         */
        public void setHasTitleResource( final Boolean _hasTitleResource ) {
            this._hasTitleResource = _hasTitleResource;
        }

        /**
         * Checks for title resource.
         *
         * @return the _hasTitleResource
         */
        public Boolean hasTitleResource() {
            return _hasTitleResource;
        }

        /**
         * Sets the title resource bundle name.
         *
         * @param _resourceBundle
         *            the _resourceBundle to set
         */
        public void setTitleResourceBundleName( final String _resourceBundle ) {
            _titleResourceBundleName = _resourceBundle;
        }

        /**
         * Gets the title resource bundle name.
         *
         * @return the _resourceBundle
         */
        public String getTitleResourceBundleName() {
            return _titleResourceBundleName;
        }

        /**
         * Sets the title resource bundle class name.
         *
         * @param resourceBundleClassName
         *            the resourceBundleClassName to set
         */
        public void setTitleResourceBundleClassName( final String resourceBundleClassName ) {
            _titleResourceBundleClassName = resourceBundleClassName;
        }

        /**
         * Gets the title resource bundle class name.
         *
         * @return the resourceBundleClassName
         */
        public String getTitleResourceBundleClassName() {
            return _titleResourceBundleClassName;
        }

        /**
         * Sets the title resource bundle relative path.
         *
         * @param titleResourceBundleRelativePath
         *            the titleResourceBundleRelativePath to set
         */
        public void setTitleResourceBundleRelativePath( final String titleResourceBundleRelativePath ) {
            _titleResourceBundleRelativePath = titleResourceBundleRelativePath;
        }

        /**
         * Gets the title resource bundle relative path.
         *
         * @return the titleResourceBundleRelativePath
         */
        public String getTitleResourceBundleRelativePath() {
            return _titleResourceBundleRelativePath;
        }

        /**
         * Sets the title resource bundle key.
         *
         * @param key
         *            the key to set
         */
        public void setTitleResourceBundleKey( final String key ) {
            _titleResourceBundleKey = key;
        }

        /**
         * Gets the title resource bundle key.
         *
         * @return the key
         */
        public String getTitleResourceBundleKey() {
            return _titleResourceBundleKey;
        }

        /**
         * Sets the description id.
         *
         * @param _descriptionId
         *            the _descriptionId to set
         */
        public void setDescriptionId( final String _descriptionId ) {
            this._descriptionId = _descriptionId;
        }

        /**
         * Gets the description id.
         *
         * @return the _descriptionId
         */
        public String getDescriptionId() {
            return _descriptionId;
        }

        /**
         * Sets the icon files.
         *
         * @param _iconFiles
         *            the _iconFiles to set
         */
        public void setIconFiles( final Icon[] _iconFiles ) {
            this._iconFiles = _iconFiles;
        }

        /**
         * Gets the icon files.
         *
         * @return the _iconFiles
         */
        public Icon[] getIconFiles() {
            return _iconFiles;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            // Copy icon array
            Icon icons[] = this.getIconFiles();
            Icon iconClones[] = new Icon[ icons.length ];

            for( int i = 0; i < iconClones.length; i++ ) {
                iconClones[ i ] = (Icon) icons[ i ].clone();
            }

            return new Resources( this._hasTitleResource, this._titleResourceBundleName, this._titleResourceBundleKey,
                    this._titleResourceBundleRelativePath, this._titleResourceBundleClassName, this._descriptionId, iconClones );
        }
    }

    /**
     * The Keyword Resources.
     */
    @XStreamAlias("KeywordResources")
    static final public class KeywordResources implements Serializable, Cloneable {
        private static final long serialVersionUID = 7859461429522853010L;

        @XStreamAsAttribute
        @XStreamAlias("KeywordResourceBundleName")
        private String _keywordResourceBundleName = "";

        @XStreamAsAttribute
        @XStreamAlias("KeywordResourceBundleRelativePath")
        private String _keywordResourceBundleRelativePath = "";

        @XStreamAsAttribute
        @XStreamAlias("KeywordResourceBundleClassName")
        private String _keywordResourceBundleClassName = "";

        @XStreamAsAttribute
        @XStreamAlias("KeywordResourceBundleKey")
        private String _keywordResourceBundleKey = "";

        /**
         * Instantiates a new KeywordResources.
         */
        public KeywordResources() {
        }

        public KeywordResources( final String keywordResourceBundleName, final String keywordResourceBundleKey,
                final String keywordResourceBundleRelativePath, final String keywordResourceBundleClassName ) {
            if( keywordResourceBundleName != null ) {
                _keywordResourceBundleName = keywordResourceBundleName;
            }
            if( keywordResourceBundleClassName != null ) {
                _keywordResourceBundleClassName = keywordResourceBundleClassName;
            }
            if( keywordResourceBundleRelativePath != null ) {
                _keywordResourceBundleRelativePath = keywordResourceBundleRelativePath;
            }
            if( keywordResourceBundleKey != null ) {
                _keywordResourceBundleKey = keywordResourceBundleKey;
            }
        }

        /**
         * Sets the keyword resource bundle name.
         *
         * @param _resourceBundle
         *            the _resourceBundle to set
         */
        public void setKeywordResourceBundleName( final String _resourceBundle ) {
            _keywordResourceBundleName = _resourceBundle;
        }

        /**
         * Gets the keyword resource bundle name.
         *
         * @return the _resourceBundle
         */
        public String getKeywordResourceBundleName() {
            return _keywordResourceBundleName;
        }

        /**
         * Sets the keyword resource bundle class name.
         *
         * @param resourceBundleClassName
         *            the resourceBundleClassName to set
         */
        public void setKeywordResourceBundleClassName( final String resourceBundleClassName ) {
            _keywordResourceBundleClassName = resourceBundleClassName;
        }

        /**
         * Gets the keyword resource bundle class name.
         *
         * @return the resourceBundleClassName
         */
        public String getKeywordTitleResourceBundleClassName() {
            return _keywordResourceBundleClassName;
        }

        /**
         * Sets the keyword resource bundle relative path.
         *
         * @param resourceBundleRelativePath
         *            the resourceBundleRelativePath to set
         */
        public void setKeywordResourceBundleRelativePath( final String resourceBundleRelativePath ) {
            _keywordResourceBundleRelativePath = resourceBundleRelativePath;
        }

        /**
         * Gets the keyword resource bundle relative path.
         *
         * @return the resourceBundleRelativePath
         */
        public String getKeywordResourceBundleRelativePath() {
            return _keywordResourceBundleRelativePath;
        }

        /**
         * Sets the keyword resource bundle key.
         *
         * @param key
         *            the key to set
         */
        public void setKeywordResourceBundleKey( final String key ) {
            _keywordResourceBundleKey = key;
        }

        /**
         * Gets the keyword resource bundle key.
         *
         * @return the key
         */
        public String getKeywordResourceBundleKey() {
            return _keywordResourceBundleKey;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return new KeywordResources( this._keywordResourceBundleName, this._keywordResourceBundleKey,
                    this._keywordResourceBundleRelativePath, this._keywordResourceBundleClassName );
        }
    }

    /**
     * The Class Compile.
     */
    @XStreamAlias("Compile")
    static final public class Compile implements Serializable, Cloneable {
        private static final long serialVersionUID = -5986680976369239089L;

        @XStreamAsAttribute
        @XStreamAlias("OutputCompilerMessages")
        private Boolean _outputCompilerMessages = Boolean.FALSE;

        @XStreamAsAttribute
        @XStreamAlias("ConvertImages")
        private Boolean _convertImages = Boolean.FALSE;

        @XStreamAsAttribute
        @XStreamAlias("CreateWarningForNoExportedRoutine")
        private Boolean _createWarningForNoExportedRoutine = Boolean.TRUE;

        @XStreamAsAttribute
        @XStreamAlias("CompressResources")
        private Boolean _compressResources = Boolean.FALSE;

        @XStreamAlias("PreprocessorDefines")
        private PreprocessorTag _preprocessorDefines[] = new PreprocessorTag[] {};
        // private String[] _preprocessorDefines = new String[] {};

        @XStreamAsAttribute
        @XStreamAlias("AliasList")
        private String _aliasList = "";

        /**
         * Instantiates a new compile.
         */
        public Compile() {
        }

        /**
         * Instantiates a new compile.
         *
         * @param outputCompilerMessages
         *            the output compiler messages
         * @param outputSourceLocations
         *            the output source locations
         * @param convertImages
         *            the convert images
         * @param createWarningForNoExportedRoutine
         *            the create warning for no exported routine
         * @param compressResources
         *            the compress resources
         * @param preprocessorDefines
         *            the preprocessor defines
         * @param aliasList
         *            the alias list
         */
        public Compile( final Boolean outputCompilerMessages, final Boolean convertImages,
                final Boolean createWarningForNoExportedRoutine, final Boolean compressResources,
                final PreprocessorTag[] preprocessorDefines, final String aliasList ) {
            if( outputCompilerMessages != null ) {
                _outputCompilerMessages = outputCompilerMessages;
            }
            if( convertImages != null ) {
                _convertImages = convertImages;
            }
            if( createWarningForNoExportedRoutine != null ) {
                _createWarningForNoExportedRoutine = createWarningForNoExportedRoutine;
            }
            if( compressResources != null ) {
                _compressResources = compressResources;
            }
            if( preprocessorDefines != null ) {
                _preprocessorDefines = preprocessorDefines;
            }
            if( aliasList != null ) {
                _aliasList = aliasList;
            }
        }

        /**
         * Gets the output compiler messages.
         *
         * @return the _outputCompilerMessages
         */
        public Boolean getOutputCompilerMessages() {
            return _outputCompilerMessages;
        }

        /**
         * Sets the output compiler messages.
         *
         * @param outputCompilerMessages
         *            the _outputCompilerMessages to set
         */
        public void setOutputCompilerMessages( final Boolean outputCompilerMessages ) {
            _outputCompilerMessages = outputCompilerMessages;
        }

        /**
         * Gets the convert images.
         *
         * @return the _dontConvertImages
         */
        public Boolean getConvertImages() {
            return _convertImages;
        }

        /**
         * Sets the convert images.
         *
         * @param boolean1
         *            the _dontConvertImages to set
         */
        public void setConvertImages( final Boolean boolean1 ) {
            _convertImages = boolean1;
        }

        /**
         * Gets the create warning for no exported routine.
         *
         * @return the _noWarningForNoExportedRoutine
         */
        public Boolean getCreateWarningForNoExportedRoutine() {
            return _createWarningForNoExportedRoutine;
        }

        /**
         * Sets the create warning for no exported routine.
         *
         * @param noWarningForNoExportedRoutine
         *            the _noWarningForNoExportedRoutine to set
         */
        public void setCreateWarningForNoExportedRoutine( final Boolean noWarningForNoExportedRoutine ) {
            _createWarningForNoExportedRoutine = noWarningForNoExportedRoutine;
        }

        // /**
        // * @return the _preprocessorDefines
        // */
        // public String[] getPreprocessorDefines() {
        // return _preprocessorDefines;
        // }

        /**
         * Gets the preprocessor defines.
         *
         * @return the _preprocessorDefines
         */
        public PreprocessorTag[] getPreprocessorDefines() {
            return _preprocessorDefines;
        }

        /**
         * Sets the preprocessor defines.
         *
         * @param preprocessorDefines
         *            the _preprocessorDefines to set
         */
        public void setPreprocessorDefines( final PreprocessorTag[] preprocessorDefines ) {
            _preprocessorDefines = preprocessorDefines;
        }

        /**
         * Gets the alias list.
         *
         * @return the _aliasList
         */
        public String getAliasList() {
            return _aliasList;
        }

        /**
         * Sets the alias list.
         *
         * @param aliasList
         *            the _aliasList to set
         */
        public void setAliasList( final String aliasList ) {
            _aliasList = aliasList;
        }

        /**
         * Gets the compress resources.
         *
         * @return the _compressResources
         */
        public Boolean getCompressResources() {
            return _compressResources;
        }

        /**
         * Sets the compress resources.
         *
         * @param compressResources
         *            the _compressResources to set
         */
        public void setCompressResources( final Boolean compressResources ) {
            _compressResources = compressResources;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            // Copy preprocessor tag array
            PreprocessorTag[] tags = this.getPreprocessorDefines();
            PreprocessorTag tagClones[] = new PreprocessorTag[ tags.length ];

            for( int i = 0; i < tagClones.length; i++ ) {
                tagClones[ i ] = (PreprocessorTag) tags[ i ].clone();
            }

            return new Compile( this._outputCompilerMessages, this._convertImages, this._createWarningForNoExportedRoutine,
                    this._compressResources, tagClones, this._aliasList );
        }
    }

    /**
     * The Class Packaging.
     */
    static public class Packaging implements Serializable, Cloneable {
        private static final long serialVersionUID = 5087648927752449338L;

        @XStreamAsAttribute
        @XStreamAlias("PreBuildStep")
        protected String _preBuildStep = "";

        @XStreamAsAttribute
        @XStreamAlias("PostBuildStep")
        protected String _postBuildStep = "";

        @XStreamAsAttribute
        @XStreamAlias("CleanStep")
        protected String _cleanStep = "";

        @XStreamAsAttribute
        @XStreamAlias("OutputFileName")
        protected String _outputFileName = "";

        @XStreamAsAttribute
        @XStreamAlias("OutputFolder")
        protected String _outputFolder = getDefaultOutputFolderName();

        @XStreamAsAttribute
        @XStreamAlias("AlxFiles")
        protected String[] _alxFiles = new String[] {};

        @XStreamAsAttribute
        @XStreamAlias("GenerateALXFile")
        protected Boolean _generateALXFile = Boolean.TRUE;

        /**
         * Instantiates a new packaging.
         */
        public Packaging() {
        }

        /**
         * Instantiates a new packaging.
         *
         * @param outputFileName
         *            the output file name
         * @param outputFolder
         *            the output folder
         * @param preBuildStep
         *            the pre build step
         * @param postBuildStep
         *            the post build step
         * @param cleanStep
         *            the clean step
         * @param alxFiles
         *            the alx files
         * @param generateALXFile
         *            the generate alx file
         */
        public Packaging( String outputFileName, String outputFolder, String preBuildStep, String postBuildStep,
                String cleanStep, String[] alxFiles, Boolean generateALXFile ) {
            super();
            if( outputFileName != null ) {
                _outputFileName = outputFileName;
            }
            if( outputFolder != null ) {
                _outputFolder = outputFolder;
            }
            if( preBuildStep != null ) {
                _preBuildStep = preBuildStep;
            }
            if( postBuildStep != null ) {
                _postBuildStep = postBuildStep;
            }
            if( cleanStep != null ) {
                _cleanStep = cleanStep;
            }
            if( alxFiles != null ) {
                _alxFiles = alxFiles;
            }
            if( generateALXFile != null ) {
                _generateALXFile = generateALXFile;
            }
        }

        /**
         * Gets the generate alx file.
         *
         * @return the generateALXFile
         */
        public Boolean getGenerateALXFile() {
            return _generateALXFile;
        }

        /**
         * Sets the generate alx file.
         *
         * @param generateALXFile
         *            the generateALXFile to set
         */
        public void setGenerateALXFile( Boolean generateALXFile ) {
            _generateALXFile = generateALXFile;
        }

        /**
         * Gets the output file name.
         *
         * @return the _outputFileName
         */
        public String getOutputFileName() {
            return _outputFileName;
        }

        /**
         * Sets the output file name.
         *
         * @param outputFileName
         *            the _outputFileName to set (which will be adjusted if needed)
         */
        protected void setOutputFileName( String outputFileName ) {
            _outputFileName = outputFileName;
        }

        /**
         * Gets the output folder.
         *
         * @return the _outputFolder
         */
        public String getOutputFolder() {
            return _outputFolder;
        }

        /**
         * Sets the output folder.
         *
         */
        public void setOutputFolder( String outputFolder ) {
            _outputFolder = outputFolder;
        }

        /**
         * Gets the pre build step.
         *
         * @return the _preBuildStep
         */
        public String getPreBuildStep() {
            return _preBuildStep;
        }

        /**
         * Sets the pre build step.
         *
         * @param preBuildStep
         *            the _preBuildStep to set
         */
        public void setPreBuildStep( final String preBuildStep ) {
            _preBuildStep = preBuildStep;
        }

        /**
         * Gets the post build step.
         *
         * @return the _postBuildStep
         */
        public String getPostBuildStep() {
            return _postBuildStep;
        }

        /**
         * Sets the post build step.
         *
         * @param postBuildStep
         *            the _postBuildStep to set
         */
        public void setPostBuildStep( final String postBuildStep ) {
            _postBuildStep = postBuildStep;
        }

        /**
         * Gets the clean step.
         *
         * @return the _cleanStep
         */
        public String getCleanStep() {
            return _cleanStep;
        }

        /**
         * Sets the clean step.
         *
         * @param cleanStep
         *            the _cleanStep to set
         */
        public void setCleanStep( final String cleanStep ) {
            _cleanStep = cleanStep;
        }

        /**
         * Gets the alx files.
         *
         * @return the _alxFiles
         */
        public String[] getAlxFiles() {
            return _alxFiles;
        }

        /**
         * Sets the alx files.
         *
         * @param alxFiles
         *            the _alxFiles to set
         */
        public void setAlxFiles( final String[] alxFiles ) {
            _alxFiles = alxFiles;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            // Copy preprocessor tag array
            String[] alxFiles = this.getAlxFiles();
            String alxFilesClone[] = new String[ alxFiles.length ];

            for( int i = 0; i < alxFilesClone.length; i++ ) {
                alxFilesClone[ i ] = alxFiles[ i ];
            }

            return new Packaging( this._outputFileName, this._outputFolder, this._preBuildStep, this._postBuildStep,
                    this._cleanStep, alxFilesClone, this.getGenerateALXFile() );
        }
    } // end Packaging

    /**
     * The Class PreprocessorTag.
     */
    @XStreamAlias("PreprocessorTag")
    public static final class PreprocessorTag implements Serializable, Comparable< PreprocessorTag >, Cloneable {
        private static final long serialVersionUID = -3147836016811642837L;

        public static final int PJ_SCOPE = 0;
        public static final int WS_SCOPE = 1;
        public static final int SDK_SCOPE = 2;

        private static final String[] scopes = new String[] { "Project", "Workspace", "SDK" };

        @XStreamAsAttribute
        @XStreamAlias("IsActive")
        private Boolean _isActive = true;

        @XStreamAsAttribute
        @XStreamAlias("PreprocessorDefine")
        private String _preprocessorDefine;

        @XStreamAsAttribute
        @XStreamAlias("Scope")
        private int _scopeID;

        /**
         * Instantiates a new preprocessor tag.
         *
         * @param preprocessorDefines
         *            the preprocessor defines
         */
        public PreprocessorTag( final String preprocessorDefines ) {
            this( preprocessorDefines, true );
        }

        /**
         * Instantiates a new preprocessor tag.
         *
         * @param preprocessorDefines
         *            the preprocessor defines
         * @param isActive
         *            the is active
         */
        public PreprocessorTag( final String preprocessorDefines, final Boolean isActive ) {
            _preprocessorDefine = preprocessorDefines;
            _isActive = isActive;
        }

        public PreprocessorTag( final String preprocessorDefines, final Boolean isActive, final int scopeID ) {
            _preprocessorDefine = preprocessorDefines;
            _isActive = isActive;
            _scopeID = scopeID;
        }

        public int getScopeID() {
            return _scopeID;
        }

        public void setScopeID( int scopeID ) {
            _scopeID = scopeID;
        }

        /**
         * Gets the preprocessor define.
         *
         * @return the _preprocessorDefines
         */
        public String getPreprocessorDefine() {
            return _preprocessorDefine;
        }

        /**
         * Sets the preprocessor define.
         *
         * @param preprocessorDefine
         *            the preprocessor define
         */
        public void setPreprocessorDefine( final String preprocessorDefine ) {
            _preprocessorDefine = preprocessorDefine;
        }

        /**
         * Checks if is active.
         *
         * @return the _isActive
         */
        public Boolean isActive() {
            return _isActive;
        }

        /**
         * Sets the is active.
         *
         * @param isActive
         *            the _isActive to set
         */
        public void setIsActive( final Boolean isActive ) {
            _isActive = isActive;
        }

        /**
         * Convenience method that creates a PreprocessorTag array given a string array.
         *
         * @param stringArray
         *            - The source string array
         *
         * @return PreprocessorTag[] - The new PreprocessorTag array
         */
        static public PreprocessorTag[] create( final String[] stringArray, final int scopeID ) {
            final ArrayList< PreprocessorTag > defines = new ArrayList< PreprocessorTag >( stringArray.length );

            for( final String preprocessorDefine : stringArray ) {
                defines.add( new PreprocessorTag( preprocessorDefine ) );
            }

            return defines.toArray( new PreprocessorTag[ defines.size() ] );
        }

        static public String getScope( int scopeID ) {
            if( scopeID < 0 || scopeID > scopes.length - 1 ) {
                return "";
            }
            return scopes[ scopeID ];
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo( final PreprocessorTag other ) {
            if( null == other ) {
                throw new IllegalArgumentException( "Can't compare against undefined object/null reference!" );
            }

            return _preprocessorDefine.compareTo( other._preprocessorDefine );
        }

        @Override
        public boolean equals( Object obj ) {
            if( obj == null ) {
                return super.equals( obj );
            }
            if( !( obj instanceof PreprocessorTag ) ) {
                return false;
            }
            PreprocessorTag tag = (PreprocessorTag) obj;
            if( !_preprocessorDefine.equals( tag.getPreprocessorDefine() ) ) {
                return false;
            }
            if( _isActive != tag.isActive() ) {
                return false;
            }
            if( _scopeID != tag.getScopeID() ) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hashCode = 37;
            hashCode = 19 * hashCode + ( _isActive ? 1 : 0 );
            hashCode = 19 * hashCode + _preprocessorDefine.hashCode();
            return hashCode;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return _preprocessorDefine;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return new PreprocessorTag( this.getPreprocessorDefine(), this.isActive() );
        }
    }

    /**
     * Constructs a new instance of BlackBerryProperties.
     */
    public BasicBlackBerryProperties() {
    }

    static int getDefaultStartupTier() {
        Class< ? > packagingUtilsClass;
        try {
            packagingUtilsClass = Class.forName( "net.rim.ejde.internal.util.PreferenceUtils" );
            if( packagingUtilsClass != null ) {
                Method method = null;
                try {
                    method = packagingUtilsClass.getDeclaredMethod( "getDefaultProjectStartupTier", new Class[ 0 ] );
                    int defaultStartupTier = (Integer) method.invoke( packagingUtilsClass.newInstance(), new Object[ 0 ] );
                    return defaultStartupTier;
                } catch( SecurityException e ) {
                    e.printStackTrace();
                } catch( NoSuchMethodException e ) {
                    e.printStackTrace();
                }
            }
        } catch( ClassNotFoundException e ) {
            e.printStackTrace();
        } catch( IllegalArgumentException e ) {
            e.printStackTrace();
        } catch( IllegalAccessException e ) {
            e.printStackTrace();
        } catch( InvocationTargetException e ) {
            e.printStackTrace();
        } catch( InstantiationException e ) {
            e.printStackTrace();
        }
        return DEFAULT_STARTUP_TIER;
    }

    static String getDefaultOutputFolderName() {
        Class< ? > packagingUtilsClass;
        try {
            packagingUtilsClass = Class.forName( "net.rim.ejde.internal.util.PackagingUtils" );
            if( packagingUtilsClass != null ) {
                Method method = null;
                try {
                    method = packagingUtilsClass.getDeclaredMethod( "getDefaultProjectOutputPrefix", new Class[ 0 ] );
                    String defaultFolder = (String) method.invoke( packagingUtilsClass.newInstance(), new Object[ 0 ] );
                    return defaultFolder;
                } catch( SecurityException e ) {
                    e.printStackTrace();
                } catch( NoSuchMethodException e ) {
                    e.printStackTrace();
                }
            }
        } catch( ClassNotFoundException e ) {
            e.printStackTrace();
        } catch( IllegalArgumentException e ) {
            e.printStackTrace();
        } catch( IllegalAccessException e ) {
            e.printStackTrace();
        } catch( InvocationTargetException e ) {
            e.printStackTrace();
        } catch( InstantiationException e ) {
            e.printStackTrace();
        }
        return DEFAULT_OUTPUT_FOLDER_NAME;
    }

    static public String getDefaultModelVersion() {
        Class< ? > packagingUtilsClass;
        try {
            packagingUtilsClass = Class.forName( "net.rim.ejde.internal.util.PackagingUtils" );
            if( packagingUtilsClass != null ) {
                Method method = null;
                try {
                    method = packagingUtilsClass.getDeclaredMethod( "getDefaultModelVersion", new Class[ 0 ] );
                    String defaultModelVersion = (String) method.invoke( packagingUtilsClass.newInstance(), new Object[ 0 ] );
                    return defaultModelVersion;
                } catch( SecurityException e ) {
                    e.printStackTrace();
                } catch( NoSuchMethodException e ) {
                    e.printStackTrace();
                }
            }
        } catch( ClassNotFoundException e ) {
            e.printStackTrace();
        } catch( IllegalArgumentException e ) {
            e.printStackTrace();
        } catch( IllegalAccessException e ) {
            e.printStackTrace();
        } catch( InvocationTargetException e ) {
            e.printStackTrace();
        } catch( InstantiationException e ) {
            e.printStackTrace();
        }
        return DEFAULT_MODEL_VERSION;
    }
}
