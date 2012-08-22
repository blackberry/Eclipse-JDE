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
package net.rim.ejde.internal.validation;

import java.io.File;
import java.util.Map;
import java.util.Set;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.core.IRIMMarker;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.AlternateEntryPoint;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.Icon;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.NatureUtils;
import net.rim.ejde.internal.util.PackagingUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.ResourceBuilderUtils;
import net.rim.ejde.internal.util.ProjectUtils.RRHFile;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaProject;

public class BBPropertiesValidator implements IBBComponentValidator {

    private static final Logger _log = Logger.getLogger( BBPropertiesValidator.class );
    public static final String VERSION_STRING_SEPARATOR = "."; //$NON-NLS-1$
    public static final String VERSION_STRING_USER_VISIBLE = "#.#.#"; //$NON-NLS-1$

    public static final char[] CHARS_WARN = new char[] { ' ', ',', '.', ';', ':', '\'', '"', '!', '@', '#', '$', '%', '^', '&',
            '*', '|', '?', '\\', '(', ')', '+', '{', '}', '[', ']', '<', '>' };
    
    /**
     * Validates BlackBerry project properties
     *
     * @param validateThis
     *            - BlackBerryProperties to validate
     * @return diagnostic result of validation
     *
     * @see net.rim.ejde.internal.validation.IBBComponentValidator#validate(Object)
     */
    public BBDiagnostic validate( Object validateThis ) {

        IProject project = (IProject) ( (IFile) validateThis ).getParent();
        BlackBerryProperties properties = ContextManager.PLUGIN.getBBProperties( project.getName(), false );
        BBDiagnostic combinedDiagnostics = AbstractDiagnosticFactory.createChainedDiagnostic();
        BBDiagnostic systemModuleDiag = validateSystemModel( properties._application.isSystemModule(),
                properties._application.getType() );
        BBDiagnostic homeScreenDiag = validateHomeScreenPosition( properties._application.getHomeScreenPosition().toString() );
        BBDiagnostic iconDiag = AbstractDiagnosticFactory.createChainedDiagnostic();
        for( Icon icon : properties._resources.getIconFiles() ) {
            iconDiag.merge( validateIconExists( project, icon ) );
        }
        BBDiagnostic fileNameDiag = validateHasValidOutputFileName( properties._packaging.getOutputFileName(), project );
        BBDiagnostic folderDiag = validateHasValidFolderName( properties._packaging.getOutputFolder(), project );

        BBDiagnostic alxDiag = AbstractDiagnosticFactory.createChainedDiagnostic();
        for( String file : properties._packaging.getAlxFiles() ) {
            alxDiag.merge( validateFileExists( project, new File( file ), false ) );
        }

        Map< String, RRHFile > resourceMap = ProjectUtils.getProjectResources( new BlackBerryProject( JavaCore.create( project ),
                properties ) );
        BBDiagnostic aepDiag = AbstractDiagnosticFactory.createChainedDiagnostic();
        for( AlternateEntryPoint aep : properties.getAlternateEntryPoints() ) {
            aepDiag.merge( validateHomeScreenPosition( aep.getHomeScreenPosition().toString() ) );
            for( Icon icon : aep.getIconFiles() ) {
                aepDiag.merge( validateIconExists( project, icon ) );
            }
            if( aep.getHasTitleResource() ) {
                aepDiag.merge( validateResourceInfo( project, resourceMap, aep.getTitleResourceBundleClassName() ) );
                aepDiag.merge( validateResourceKey( resourceMap.get( aep.getTitleResourceBundleClassName() ),
                        aep.getTitleResourceBundleKey() ) );
            }
        }

        BBDiagnostic resourceDiag = AbstractDiagnosticFactory.createChainedDiagnostic();
        if( properties._resources.hasTitleResource() ) {
            resourceDiag.merge( validateResourceInfo( project, resourceMap,
                    properties._resources.getTitleResourceBundleClassName() ) );
            resourceDiag.merge( validateResourceKey( resourceMap.get( properties._resources.getTitleResourceBundleClassName() ),
                    properties._resources.getTitleResourceBundleKey() ) );
        }

        combinedDiagnostics.merge( systemModuleDiag );
        combinedDiagnostics.merge( homeScreenDiag );
        combinedDiagnostics.merge( iconDiag );
        combinedDiagnostics.merge( fileNameDiag );
        combinedDiagnostics.merge( folderDiag );
        combinedDiagnostics.merge( alxDiag );
        combinedDiagnostics.merge( aepDiag );
        combinedDiagnostics.merge( resourceDiag );

        return combinedDiagnostics;
    }

    static public BBDiagnostic validateResourceInfo( IProject project, Map< String, RRHFile > resourceMap, String className ) {
        BBDiagnostic resourceDiag = AbstractDiagnosticFactory.createChainedDiagnostic();
        RRHFile rrhFile = resourceMap.get( className );
        if( rrhFile == null ) {
            resourceDiag.merge( DiagnosticFactory.create_RESOURCE_MISSING( className ) );
        }
        return resourceDiag;
    }

    static public BBDiagnostic validateResourceKey( RRHFile rrhFile, String keyName ) {
        BBDiagnostic resourceDiag = AbstractDiagnosticFactory.createChainedDiagnostic();
        if( rrhFile == null && !StringUtils.isBlank( keyName ) ) {
            resourceDiag.merge( DiagnosticFactory.create_RESOURCE_KEY_INVALID( keyName ) );
        } else if( !StringUtils.isBlank( keyName ) && rrhFile.getKeyTalbe().get( keyName ) == null ) {
            resourceDiag.merge( DiagnosticFactory.create_RESOURCE_KEY_INVALID( keyName ) );
        }
        return resourceDiag;
    }

    /**
     * Validates home screen position
     *
     * @param position
     * @return
     */
    public static BBDiagnostic validateHomeScreenPosition( String position ) {
        if( !isParsableInt( position ) ) {
            return DiagnosticFactory.create_HOME_SCREEN_POSITION_INVALID();
        }

        return AbstractDiagnosticFactory.getOK();
    }

    /**
     * Validates whether the given icon exists within the project
     *
     * @param project
     * @param icon
     * @return
     */
    public static BBDiagnostic validateIconExists( IProject project, Icon icon ) {
        // TODO: See whether we can always construct the icon with forward slash.
        if( !fileExists( project, new File( icon.getCanonicalFileName().replace( '\\', '/' ) ) ) ) {
            return DiagnosticFactory.create_ICON_MISSING( new Path( icon.getCanonicalFileName() ).lastSegment() );
        }
        return AbstractDiagnosticFactory.getOK();
    }

    /**
     * Validates if the given file exists in the given project or its dependent projects.
     *
     * @param project
     *            The project The main project
     * @param file
     *            The file to be checked
     * @param checkDependentProjects
     *            If dependent projects should be checked
     *
     * @return <code>BBDiagnostic</code>
     */
    public static BBDiagnostic validateFileExists( IProject project, File file, boolean checkDependentProjects ) {
        boolean found = false;
        if( fileExists( project, file ) ) {
            found = true;
        } else if( checkDependentProjects ) {
            // check dependent projects
            try {
                Set< IProject > projects = ProjectUtils.getAllReferencedProjects( project );
                for( IProject proj : projects ) {
                    if( fileExists( proj, file ) ) {
                        found = true;
                        break;
                    }
                }
            } catch( CoreException e ) {
                _log.error( e );
            }
        }
        if( !found ) {
            return DiagnosticFactory.create_FILE_MISSING( file.getName() );
        }
        return AbstractDiagnosticFactory.getOK();
    }

    private static Boolean fileExists( IProject project, File file ) {
        IPath iconPath = new Path( file.getPath() );

        if( iconPath.isAbsolute() ) {
            if( !iconPath.toFile().exists() ) {
                return false;
            }
        } else {
            if( file.getPath().startsWith( ".." ) ) { //$NON-NLS-1$
                // Do an external file check
                File externalFile = project.getLocation().append( iconPath ).toFile();

                if( externalFile != null && !externalFile.exists() ) {
                    return false;
                }
            } else {
                // Do a local proj file check
                IFile iFile = project.getFile( iconPath );

                if( iFile == null || !iFile.exists() || iFile.isLinked() && !iFile.getLocation().toFile().exists() ) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Validates whether the string has a value
     *
     * @param value
     * @return
     */
    public static BBDiagnostic validateHasValue( String value ) {
        if( StringUtils.isEmpty( value ) ) {
            return DiagnosticFactory.create_VALUE_REQUIRED();
        }

        return AbstractDiagnosticFactory.getOK();
    }

    /**
     * Validates the given file name for illegal characters.
     *
     * @param value
     * @return
     */
    public static BBDiagnostic validateHasValidOutputFileName( String value, IProject proj ) {
        if( StringUtils.isEmpty( value ) ) {
            return DiagnosticFactory.create_VALUE_REQUIRED();
        }
        // Output file name must be a valid Java identifier
        IStatus status = JavaConventions.validateJavaTypeName( value, JavaCore.VERSION_1_3, JavaCore.VERSION_1_3 );
        if( status.getSeverity() == IStatus.ERROR ) {
            return DiagnosticFactory.create_INVALID_OUTPUT_PATH_CHAR();
        }
        // Also, this value should not overlap with dependency projects outputFileName
        if(!isValidNameWithDependencies(value, proj)) {
        	return DiagnosticFactory.create_OutputFN_MUST_DIFFER(value);
        }
        
        return AbstractDiagnosticFactory.getOK();
    }
    
	private static boolean isValidNameWithDependencies(String value, IProject proj) {
		try {
			IProject[] refs = proj.getReferencedProjects();
			for (IProject ref : refs) {
				if (NatureUtils.hasBBNature(ref)) {
					if(PackagingUtils.replaceSpecialChars(ref.getName()).equals(value)
							|| !isValidNameWithDependencies(value, ref)) {
						return false;
					}
				}
			}
		} catch (CoreException e) {
			_log.error(e.getMessage());
		}
		return true;
	}

    public static BBDiagnostic validateSystemModel( boolean systemModel, String projectType ) {
        if( projectType.equals( BlackBerryProject.LIBRARY ) && !systemModel ) {
            return DiagnosticFactory.create_SYSTEM_MODULE_PROBLEMATIC();
        }
        return AbstractDiagnosticFactory.getOK();
    }

    /**
     * Validates the given folder name for illegal characters in context & basic URI syntax
     *
     * @param value
     * @return
     */
    public static BBDiagnostic validateHasValidFolderName( String value, IProject project ) {
        // general project context validation (catches some issues...)
        IPath ppath = project.getLocation();
        if( !ppath.isValidPath( value ) ) {
            return DiagnosticFactory.createDiagnostic( BBDiagnostic.ERROR, 0, Messages.BBPropertiesValidator_PATH_INVALID_ERROR );
        }
        IWorkspace workspace = project.getWorkspace();
        IPath p = new Path( value );
        if( p.isAbsolute() || value.indexOf( ':' ) >= 0 ) {
            return DiagnosticFactory.createDiagnostic( BBDiagnostic.ERROR, 0,
                    Messages.BBPropertiesValidator_PATH_NOT_RELATIVE_ERROR );
        }

        // check more thoroughly the validity of each segment
        for( String seg : p.segments() ) {
            if( IConstants.DOUBLE_DOTS.equals( seg ) ) {
                return DiagnosticFactory.createDiagnostic( BBDiagnostic.ERROR, 0,
                        Messages.BBPropertiesValidator_PATH_NOT_INWARDS_ERROR );
            }
            IStatus nameStatus = workspace.validateName( seg, IResource.FOLDER );
            if( null != nameStatus && !nameStatus.isOK() ) {
                return DiagnosticFactory.createDiagnostic( BBDiagnostic.ERROR, 0, nameStatus.getMessage() );
            }
        }

        return AbstractDiagnosticFactory.getOK();
    }

    /**
     * Validates whether the given string is a parsable int value
     *
     * @param i
     * @return true if the string is parsable and false otherwise
     */
    public static boolean isParsableInt( String i ) {
        try {
            Integer.parseInt( i );
            return true;
        } catch( NumberFormatException nfe ) {
            return false;
        }
    }

}
