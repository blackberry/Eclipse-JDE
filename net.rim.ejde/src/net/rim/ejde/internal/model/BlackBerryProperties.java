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

import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;
import net.rim.ejde.internal.util.PackagingUtils;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * External
 */
@InternalFragmentReplaceable
public class BlackBerryProperties extends BasicBlackBerryProperties {

    private static final long serialVersionUID = 8888696296983095195L;

    @XStreamAlias("General")
    public final ExtendedGeneral _general;

    @XStreamAlias("Application")
    public Application _application;

    @XStreamAlias("Resources")
    public Resources _resources;

    @XStreamAlias("KeywordResources")
    public KeywordResources _keywordResources;

    @XStreamAlias("Compile")
    public Compile _compile;

    @XStreamAlias("Packaging")
    public final ExtendedPackaging _packaging;

    @XStreamAlias("HiddenProperties")
    public HiddenProperties _hiddenProperties;

    @XStreamAlias("AlternateEntryPoints")
    public AlternateEntryPoint[] _alternateEntryPoints;

    public BlackBerryProperties() {
        _application = new Application();
        _resources = new Resources();
        _keywordResources = new KeywordResources();
        _compile = new Compile();
        _hiddenProperties = new HiddenProperties();
        _alternateEntryPoints = new AlternateEntryPoint[] {};
        _general = new ExtendedGeneral();
        _packaging = new ExtendedPackaging();
    }

    public BlackBerryProperties( AlternateEntryPoint entryPoint ) {
        this();
        intializeFromAEP( entryPoint );
        _general.setTitle( entryPoint.getTitle() );
    }

    /**
     * Gets the Packaging model.
     *
     * @return
     */
    public ExtendedPackaging getPackaging() {
        return _packaging;
    }

    /**
     * Gets the General Model
     *
     * @return
     */
    public ExtendedGeneral getGeneral() {
        return _general;
    }

    /**
     * Sets the alternate entry points.
     *
     * @param _alternateEntryPoints
     *            the _alternateEntryPoints to set
     */
    public void setAlternateEntryPoints( final AlternateEntryPoint[] _alternateEntryPoints ) {
        this._alternateEntryPoints = _alternateEntryPoints;
    }

    /**
     * Gets the alternate entry points.
     *
     * @return
     */
    public AlternateEntryPoint[] getAlternateEntryPoints() {
        return _alternateEntryPoints;
    }

    /**
     * Gets the Application model.
     *
     * @return
     */
    public Application getApplication() {
        return _application;
    }

    /**
     * Gets the Compile model.
     *
     * @return
     */
    public Compile getCompile() {
        return _compile;
    }

    /**
     * Gets the HiddenProperties model.
     *
     * @return
     */
    public HiddenProperties getHiddenProperties() {
        return _hiddenProperties;
    }

    /**
     * Gets the Resource model.
     *
     * @return
     */
    public Resources getResources() {
        return _resources;
    }

    /**
     * Gets the keyword resource model.
     *
     * @return
     */
    public KeywordResources getKeywordResources() {
        if ( _keywordResources == null ){
            _keywordResources = new KeywordResources();
        }
        return _keywordResources;
    }
    
    /**
     * Use this setting to correct invalid BB app names
     * @param outputFileName
     */
    public void setValidOutputFileName(String outputFileName) {
    	_packaging.setOutputFileName(PackagingUtils.replaceSpecialChars( outputFileName ));
    }

    /**
     * Constructs a new instance of BlackBerryProperties based on the given properties.
     *
     * @param gen
     *            General properties
     * @param app
     *            Application properties
     * @param hp
     *            Hidden properties
     * @param res
     *            Resource properties\
     * @param kres
     *            KeywordResource properties
     * @param com
     *            Compile properties
     * @param tools
     *            Tools properties
     * @param pack
     *            Package properties
     * @param aep
     *            List of alternate entry points
     */
    public BlackBerryProperties( ExtendedGeneral gen, Application app, HiddenProperties hp, Resources res, KeywordResources kres,
            Compile com, ExtendedPackaging pack, AlternateEntryPoint[] aep ) {
        _application = app;
        _hiddenProperties = hp;
        _resources = res;
        _keywordResources = kres;
        _compile = com;
        _alternateEntryPoints = aep;
        _general = gen;
        _packaging = pack;
    }

    protected void intializeFromAEP( AlternateEntryPoint entryPoint ) {
        // Initialize the application section
        _application.setMainMIDletName( entryPoint.getMainMIDletName() );
        _application.setMainArgs( entryPoint.getArgsPassedToMain() );
        _application.setHomeScreenPosition( entryPoint.getHomeScreenPosition() );
        if( entryPoint.getStartupTier() < 6 ) {
            _application.setStartupTier( 6 );
        } else {
            _application.setStartupTier( entryPoint.getStartupTier() );
        }
        _application.setIsAutostartup( entryPoint.isAutostartup() );
        _application.setIsSystemModule( entryPoint.isSystemModule() );

        // Initialize the resources section
        _resources.setIconFiles( entryPoint.getIconFiles() );
        _resources.setTitleResourceBundleName( entryPoint.getTitleResourceBundleName() );
        _resources.setTitleResourceBundleKey( entryPoint.getTitleResourceBundleKey() );
        _resources.setTitleResourceBundleClassName( entryPoint.getTitleResourceBundleClassName() );
        _resources.setTitleResourceBundleRelativePath( entryPoint.getTitleResourceBundleRelativePath() );
        _resources.setHasTitleResource( entryPoint.getHasTitleResource() );
        // TODO we use the KeywordResourceBundleKey to store ID for now, later on when we implemented the UI part, we need to
        // change
        getKeywordResources().setKeywordResourceBundleClassName( entryPoint._keywordResources
                .getKeywordTitleResourceBundleClassName() );
        getKeywordResources().setKeywordResourceBundleKey( entryPoint._keywordResources.getKeywordResourceBundleKey() );
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ExtendedGeneral gen = (ExtendedGeneral) this._general.clone();
        Application app = (Application) this._application.clone();
        HiddenProperties hp = (HiddenProperties) this._hiddenProperties.clone();
        Resources res = (Resources) this._resources.clone();
        KeywordResources kres = (KeywordResources) this.getKeywordResources().clone();
        Compile com = (Compile) this._compile.clone();
        ExtendedPackaging pack = (ExtendedPackaging) this._packaging.clone();

        // Copy aep array
        AlternateEntryPoint aeps[] = this.getAlternateEntryPoints();
        AlternateEntryPoint aepClones[] = new AlternateEntryPoint[ aeps.length ];
        for( int i = 0; i < aepClones.length; i++ ) {
            aepClones[ i ] = (AlternateEntryPoint) aeps[ i ].clone();
        }

        return new BlackBerryProperties( gen, app, hp, res, kres, com, pack, aepClones );
    }

    /**
     * Whether this BB properties could refer some file name
     *
     * @param fn
     * @return
     */
    public boolean mayReferFile( String fn ) {
        for( Icon ico : _resources._iconFiles ) {
            if( ico._canonicalFileName.endsWith( fn ) ) {
                return true;
            }
        }
        for( String alx : _packaging._alxFiles ) {
            if( alx.endsWith( fn ) ) {
                return true;
            }
        }
        for( AlternateEntryPoint aep : _alternateEntryPoints ) {
            for( Icon ico : aep._iconFiles ) {
                if( ico._canonicalFileName.endsWith( fn ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This version of Application is a stub class that is replaced by the internal fragment for internal eJDE. The internal
     * version adds additional attributes.
     */
    @XStreamAlias("Application")
    static final public class Application extends BasicApplication {
        private static final long serialVersionUID = -4295905032546587541L;

        /**
         * Instantiates a new application.
         */
        public Application() {

        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    @XStreamAlias("Packaging")
    static public class ExtendedPackaging extends Packaging {
        private static final long serialVersionUID = 1809840727405144082L;

        public ExtendedPackaging() {
            super();
        }

        public ExtendedPackaging( String outputFileName, String outputFolder, String preBuildStep, String postBuildStep,
                String cleanStep, String[] alxFiles, Boolean generateALXFile ) {
            super( outputFileName, outputFolder, preBuildStep, postBuildStep, cleanStep, alxFiles, generateALXFile );
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            // Copy preprocessor tag array
            String[] alxFiles = this.getAlxFiles();
            String alxFilesClone[] = new String[ alxFiles.length ];

            for( int i = 0; i < alxFilesClone.length; i++ ) {
                alxFilesClone[ i ] = alxFiles[ i ];
            }

            return new ExtendedPackaging( this._outputFileName, this._outputFolder, this._preBuildStep, this._postBuildStep,
                    this._cleanStep, alxFilesClone, this.getGenerateALXFile() );
        }
    }

    @XStreamAlias("General")
    static public class ExtendedGeneral extends General {
        private static final long serialVersionUID = 7509249690858071040L;

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

}
