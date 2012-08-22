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
import java.util.Vector;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.AlternateEntryPoint;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.Icon;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.PreprocessorTag;
import net.rim.ejde.internal.ui.editors.model.GeneralSection;
import net.rim.ejde.internal.util.PackageUtils;
import net.rim.ejde.internal.util.PackagingUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ide.Project;
import net.rim.ide.core.Util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

@InternalFragmentReplaceable
public class BlackBerryPropertiesFactory {

    private static final Logger _log = Logger.getLogger( BlackBerryPropertiesFactory.class );

    static public BlackBerryProperties createBlackBerryProperties() {
        return new BlackBerryProperties();
    }

    static public BlackBerryProperties createBlackBerryProperties( final AlternateEntryPoint entryPoint ) {
        return new BlackBerryProperties( entryPoint );
    }

    static public BlackBerryProperties createBlackBerryProperties( final IJavaProject javaProject ) {
        BlackBerryProperties properties = new BlackBerryProperties();
        initializeFromJavaProject( properties, javaProject );
        return properties;
    }

    static public BlackBerryProperties createBlackBerryProperties( final Project legacyProject, final IJavaProject javaProject ) {
        BlackBerryProperties properties = new BlackBerryProperties();
        initializeFromLegacy( properties, legacyProject, javaProject );
        return properties;
    }

    /**
     * Initialize from a given java project.
     *
     * @param javaProj
     *            the java project
     */
    static private void initializeFromJavaProject( BlackBerryProperties properties, IJavaProject javaProj ) {
        // Initialize the general section
        properties._general.setTitle( javaProj.getProject().getName() );

        // Initialize the application section
        properties._application.setType( GeneralSection.projectTypeChoiceList[ Project.LIBRARY ] );

        // Initialize the packaging section
        properties.setValidOutputFileName( javaProj.getProject().getName() );
    }

    /**
     * Initialize from a given legacy project.
     *
     * @param properties
     *
     * @param project
     *            the project
     * @param javaProj
     *            the java proj
     */
    static private void initializeFromLegacy( BlackBerryProperties properties, final Project project, final IJavaProject javaProj ) {
        // Initialize the general section
        properties._general.setTitle( project.getTitle() );
        properties._general.setVersion( project.getVersion() );
        properties._general.setDescription( project.getDescription() );
        properties._general.setVendor( project.getVendor() );

        // Initialize the application section
        properties._application.setHomeScreenPosition( project.getRibbonPosition() );
        properties._application.setIsAutostartup( project.getRunOnStartup() );
        properties._application.setIsSystemModule( project.getSystemModule() );
        switch( project.getType() ) {
            case Project.MIDLET: {
                properties._application.setMainArgs( IConstants.EMPTY_STRING );
                properties._application.setMainMIDletName( project.getMidletClass() );
                break;
            }
            case Project.CLDC_APPLICATION: {
                properties._application.setMainArgs( project.getMidletClass() );
                properties._application.setMainMIDletName( IConstants.EMPTY_STRING );
                break;
            }
            case Project.LIBRARY: {
                properties._application.setIsSystemModule( Boolean.TRUE );
                // No break falling through to default case
            }
            default: {
                properties._application.setMainArgs( IConstants.EMPTY_STRING );
                properties._application.setMainMIDletName( IConstants.EMPTY_STRING );
            }
        }
        properties._application.setStartupTier( Math.max( project.getStartupTier(), ProjectUtils.getStartupTiers()[ 0 ] ) );
        properties._application.setType( GeneralSection.projectTypeChoiceList[ project.getType() ] );

        // Initialize the resources section
        properties._resources.setHasTitleResource( project.isTitleResourceBundleActive() );
        properties._resources.setTitleResourceBundleName( project.getTitleResourceBundleName() );
        properties._resources.setTitleResourceBundleKey( project.getTitleResourceTitleKey() );
        properties._resources.setTitleResourceBundleClassName( project.getTitleResourceBundleClassName() );
        final IProject iProject = javaProj.getProject();
        final String resourceBundlePath = project.getTitleResourceBundlePath();
        if( !StringUtils.isBlank( resourceBundlePath ) ) {
            final String resourceBundleFilePath = Util.makeAbsolute( resourceBundlePath, project.getFile() );
            properties._resources.setTitleResourceBundleRelativePath( getTargetRelFilePath( new File( resourceBundleFilePath ),
                    project, JavaCore.create( iProject ) ) );
        } else {
            properties._resources.setTitleResourceBundleRelativePath( IConstants.EMPTY_STRING );
        }
        properties._resources.setDescriptionId( project.getTitleResourceDescriptionKey() );
        properties._resources.setIconFiles( getIcons( project, iProject ) );
        // Initialize the keyword section
        // TODO we use the KeywordResourceBundleKey to store ID for now, later on when we implemented the UI part, we need to
        // change
        properties.getKeywordResources().setKeywordResourceBundleKey( project.getKeywordResourceBundleId() );
        properties.getKeywordResources().setKeywordResourceBundleClassName( project.getKeywordResourceBundleClassName() );
        // Initialize the compiler section
        properties._compile.setAliasList( project.getAlias() );
        properties._compile.setCompressResources( project.getWorkspace().getResourcesCompressed() );
        properties._compile.setConvertImages( !project.getIsNoConvertPng() );
        properties._compile.setCreateWarningForNoExportedRoutine( !project.getIsNoMainWarn() );
        properties._compile.setOutputCompilerMessages( project.getIsNoWarn() );
        final Vector< String > defines = project.getDefines();
        properties._compile.setPreprocessorDefines( PreprocessorTag.create( defines.toArray( new String[ defines.size() ] ),
                PreprocessorTag.PJ_SCOPE ) );

        // Initialize the packaging section
        final Vector< File > alxFileVector = project.getAlxImports();
        final String[] alxFiles = new String[ alxFileVector.size() ];
        for( int i = 0; i < alxFileVector.size(); i++ ) {
            final File alxFile = alxFileVector.get( i );
            alxFiles[ i ] = alxFile.getPath();
        }
        properties._packaging.setAlxFiles( alxFiles );
        properties._packaging.setCleanStep( project.getCleanBuild() );
        properties.setValidOutputFileName( project.getOutputFileName() );
        properties._packaging.setPostBuildStep( project.getPostBuild() );
        properties._packaging.setPreBuildStep( project.getPreBuild() );
        properties._packaging.setGenerateALXFile( PackagingUtils.getDefaultGenerateAlxFile() );

        // Initialize alternate entry points
        final int AEPNumber = project.getNumEntries();
        properties._alternateEntryPoints = new AlternateEntryPoint[ AEPNumber ];
        for( int i = 0; i < AEPNumber; i++ ) {
            final Project entry = project.getEntry( i );
            try {
                properties._alternateEntryPoints[ i ] = createAlternateEntryPoint( entry, iProject );
            } catch( final CoreException e ) {
                _log.error( e.getMessage(), e );
            }
        }

        // Initialize hidden properties
        properties._hiddenProperties.setClassProtection( project.getClassProtection() );
        properties._hiddenProperties.setPackageProtection( project.getPackageProtection() );
    }

    public static AlternateEntryPoint createAlternateEntryPoint( final Project project, final IProject iProject )
            throws CoreException {
        AlternateEntryPoint aep = new AlternateEntryPoint();
        if( project.getEntryFor() == null ) {
            throw new CoreException( StatusFactory.createErrorStatus( "Project is not an alternate entry project." ) );
        }
        if( StringUtils.isBlank( project.getTitle() ) ) {
            aep.setTitle( project.getDisplayName() );
        } else {
            aep.setTitle( project.getTitle() );
        }
        switch( project.getType() ) {
            case Project.MIDLET_ENTRY: {
                aep.setArgsPassedToMain( IConstants.EMPTY_STRING );
                aep.setMainMIDletName( project.getMidletClass() );
                break;
            }
            case Project.CLDC_APPLICATION_ENTRY: {
                aep.setArgsPassedToMain( project.getMidletClass() );
                aep.setMainMIDletName( IConstants.EMPTY_STRING );
                break;
            }
            default: {
                aep.setArgsPassedToMain( IConstants.EMPTY_STRING );
                aep.setMainMIDletName( IConstants.EMPTY_STRING );
            }
        }
        aep.setHomeScreenPosition( project.getRibbonPosition() );
        aep.setTitleResourceBundleName( project.getTitleResourceBundleName() );
        aep.setTitleResourceBundleKey( project.getTitleResourceTitleKey() );
        aep.setTitleResourceBundleClassName( project.getTitleResourceBundleClassName() );
        final String resourceBundlePath = project.getTitleResourceBundlePath();
        if( !StringUtils.isBlank( resourceBundlePath ) ) {
            final String resourceBundleFilePath = Util.makeAbsolute( resourceBundlePath, project.getFile() );
            aep.setTitleResourceBundleRelativePath( getTargetRelFilePath( new File( resourceBundleFilePath ), project,
                    JavaCore.create( iProject ) ) );
        } else {
            aep.setTitleResourceBundleRelativePath( IConstants.EMPTY_STRING );
        }
        // TODO we use the KeywordResourceBundleKey to store ID for now, later on when we implemented the UI part, we need to
        // change
        aep._keywordResources.setKeywordResourceBundleClassName( project.getKeywordResourceBundleClassName() );
        aep._keywordResources.setKeywordResourceBundleKey( project.getKeywordResourceBundleId() );
        aep.setStartupTier( Math.max( project.getStartupTier(), ProjectUtils.getStartupTiers()[ 0 ] ) );
        aep.setIconFiles( getIcons( project, iProject ) );
        aep.setIsAutostartup( project.getRunOnStartup() );
        aep.setIsSystemModule( project.getSystemModule() );
        if( !StringUtils.isBlank( aep.getTitleResourceBundleKey() ) ) {
            aep.setHasTitleResource( true );
        }

        return aep;
    }

    /**
     * Gets the relative path of the <code>file</code>.
     * <p>
     * <b> This method does not calculate path for the given files as the import module does. This method only search the file in
     * the project and return the relative path if found. </b>
     *
     * @param file
     *            the file
     * @param legacyProject
     *            the legacy project
     * @param javaProject
     *            the java project
     *
     * @return the target rel file path
     */
    protected static String getTargetRelFilePath( final File file, final Project legacyProject, final IJavaProject javaProject ) {
        IPath path = null;
        try {
            path = new Path( PackageUtils.getFilePackageString( file, legacyProject ) );
        } catch( CoreException e ) {
            return IConstants.EMPTY_STRING;
        }
        path = path.append( file.getName() );
        path = PackageUtils.getProjectRelativePath( javaProject, path );
        if( path == null ) {
            return IConstants.EMPTY_STRING;
        }
        return path.toOSString();
    }

    /**
     * Gets the icons.
     *
     * @param project
     *            the project
     * @param iProject
     *            the i project
     *
     * @return the icons
     */
    protected static Icon[] getIcons( final Project project, final IProject iProject ) {
        IJavaProject javaProject = JavaCore.create( iProject );
        final Vector< Icon > newIcons = new Vector< Icon >();
        Icon icon = null, rooloverIcon;

        Vector< File > iconFiles = project.getIcons();
        // we only get the first icon
        if( ( iconFiles != null ) && ( iconFiles.size() > 0 ) ) {
            final File iconFile = iconFiles.get( 0 );
            if( iconFile.exists() ) {
                icon = new Icon( getTargetRelFilePath( iconFile, project, javaProject ) );
                newIcons.add( icon );
            }
        }

        iconFiles = project.getRolloverIcons();
        // we only get the first rollover icon
        if( ( iconFiles != null ) && ( iconFiles.size() > 0 ) ) {
            final File iconFile = iconFiles.get( 0 );
            if( iconFile.exists() ) {
                // If there is only 1 icon it cannot be a focus icon, so set focus status based on existence of first icon
                rooloverIcon = new Icon( getTargetRelFilePath( iconFile, project, javaProject ), Boolean.valueOf( icon != null ) );
                newIcons.add( rooloverIcon );
            }
        }

        return newIcons.toArray( new Icon[ newIcons.size() ] );
    }
}
