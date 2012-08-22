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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BasicBlackBerryProperties;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.model.BlackBerryPropertiesFactory;
import net.rim.ejde.internal.model.BlackBerrySDKInstall;
import net.rim.ejde.internal.model.BlackBerryVMInstallType;
import net.rim.ejde.internal.validation.BBPropertiesValidator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * A util class to deal with the tasks needed for steps in the packaging process.
 *
 * @author jheifetz
 */
public class PackagingUtils {
    static Logger _log = Logger.getLogger( PackagingUtils.class );
    static char _replaceChar;
    /**
     * The index of standard deployment folder name
     */
    static final public int STANDARD_DEPLOYMENT = 0;
    /**
     * The index of web deployment folder name
     */
    static final public int WEB_DEPLOYMENT = 1;

    /**
     * Gets the default project output folder.
     *
     * @return the default project output folder
     */
    public static String getDefaultProjectOutputPrefix() {
        return ContextManager.getDefault().getPreferenceStore().getString( IConstants.PROJECT_OUTPUT_FOLDER_PREFIX_KEY );
    }

    /**
     * Gets the default generate alx file value.
     *
     * @return the default generate alx file value
     */
    public static boolean getDefaultGenerateAlxFile() {
        return ContextManager.getDefault().getPreferenceStore().getBoolean( IConstants.GENERATE_ALX_FILE_KEY );
    }

    /**
     * Gets the default BlackBerry model version.
     *
     * @return the default BlackBerry model version
     */
    public static String getDefaultModelVersion() {
        // ContextManager.getDefault().getPreferenceStore().getString( IConstants.BB_MODEL_VERSION );
        return BasicBlackBerryProperties.DEFAULT_MODEL_VERSION;
    }

    /**
     * If the exported jars in a BlackBerry project should be packaged into the project's cod file.
     *
     * @return
     */
    public static boolean getPackagExportedJar() {
        return ContextManager.getDefault().getPreferenceStore().getBoolean( IConstants.PACKAGE_EXPORTED_JAR );
    }

    /**
     * Cleans the contents of the project output folder.
     *
     * @param javaProj
     *            the java project
     *
     * @throws CoreException
     *             Exceptions in the process
     */
    public static void cleanProjectOutputFolder( final IJavaProject javaProj ) throws CoreException {
        if( javaProj != null ) {
            boolean isBBProject = javaProj.getProject().hasNature( BlackBerryProjectCoreNature.NATURE_ID );
            final String projName = javaProj.getProject().getName();
            BlackBerryProperties bbProperties = null;
            if( isBBProject ) {
                bbProperties = ContextManager.PLUGIN.getBBProperties( projName, false );
            } else {
                bbProperties = BlackBerryPropertiesFactory.createBlackBerryProperties( javaProj );
            }
            if( bbProperties != null ) {
                // clean the deployment files of the current project
                String[] outputFolderPaths = PackagingUtils.getPackagingOutputFolders( new BlackBerryProject( javaProj,
                        bbProperties ) );
                for( int i = 0; i < outputFolderPaths.length; i++ ) {
                    String outputFolder = outputFolderPaths[ i ];
                    String outputFileN = bbProperties._packaging.getOutputFileName();
                    if( ( null != outputFolder ) && ( null != outputFileN ) && ( outputFileN.length() > 0 ) ) {
                        final IResource outputFolderRes = javaProj.getProject().findMember( outputFolder );
                        removeFiles( (IContainer) outputFolderRes, null );
                    }
                }
                // clean the deployment files of the dependency projects
                List< IJavaProject > dependentProjects = ProjectUtils.getAllReferencedJavaProjects( javaProj );
                String outputFileName;
                for( IJavaProject dependentProject : dependentProjects ) {
                    bbProperties = ContextManager.PLUGIN.getBBProperties( dependentProject.getProject().getName(), false );
                    outputFileName = bbProperties._packaging.getOutputFileName();
                    outputFolderPaths = PackagingUtils.getPackagingOutputFolders( new BlackBerryProject( dependentProject,
                            bbProperties ) );
                    for( int i = 0; i < outputFolderPaths.length; i++ ) {
                        String outputFolder = outputFolderPaths[ i ];
                        if( ( null != outputFolder ) && ( null != outputFileName ) && ( outputFileName.length() > 0 ) ) {
                            final IResource outputFolderRes = javaProj.getProject().findMember( outputFolder );
                            removeFiles( (IContainer) outputFolderRes, outputFileName );
                        }
                    }
                }
            } else {
                if( isBBProject ) {
                    throw new CoreException( new Status( IStatus.ERROR, ContextManager.PLUGIN_ID,
                            "Cannot retrieve metadata from project " + projName ) );
                }
            }
        } else {
            throw new CoreException( new Status( IStatus.ERROR, ContextManager.PLUGIN_ID,
                    "Cannot change output folder for null IJavaProject" ) );
        }
    }

    private static void removeFiles( IContainer container, String fileName ) throws CoreException {
        if( ( container != null ) && container.exists() ) {
            IResource[] resources = ( container ).members();
            for( final IResource child : resources ) {
                if( StringUtils.isBlank( fileName ) || child.getName().startsWith( fileName ) ) {
                    child.delete( true, new NullProgressMonitor() );
                }
            }
        }
    }

    /**
     * Gets the bbsdk install for the given JavaProject
     *
     * @param javaProj
     *            the java proj
     *
     * @return The BlackBerrySDKInstall for JavaProject or <code>null</code>
     */
    public static BlackBerrySDKInstall getBBSDKInstall( IJavaProject javaProj ) {
        if( javaProj == null ) {
            return null;
        }
        try {
            IVMInstall vm = null;
            if( javaProj.getProject().hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                if( javaProj.getProject().isOpen() ) {
                    vm = JavaRuntime.getVMInstall( javaProj );
                    if( vm != null && vm instanceof BlackBerrySDKInstall ) {
                        return (BlackBerrySDKInstall) vm;
                    }
                }
            } else {
                if( javaProj.getProject().isOpen() ) {
                    // for java proejct, we use the default BB jre
                    return (BlackBerrySDKInstall) VMUtils.getDefaultBBVM();
                }
            }

        } catch( CoreException e ) {
            _log.error( e );
        }
        return null;
    }

    /**
     * Get the output folder name of the BlackBerry JRE of the given <code>bbProject</code>.
     *
     * @param bbProject
     * @return
     */
    public static String getVMOutputFolderName( BlackBerryProject bbProject ) {
        BlackBerrySDKInstall bbVM = PackagingUtils.getBBSDKInstall( bbProject.getJavaProject() );
        if( bbVM == null ) {
            return IConstants.DEFAULT_VM_OUTPUT_FOLDER_NAME;
        }
        String name = bbVM.getAttribute( BlackBerryVMInstallType.ATTR_RAPC_OUTPUT_FOLDER );
        if( name == null ) {
            return IConstants.DEFAULT_VM_OUTPUT_FOLDER_NAME;
        }
        return name;
    }

    /**
     * Gets the array of output folders of packaging.
     *
     * @param javaProj
     *            the java proj
     *
     * @return The output folders of packaging. If there is not output folder found, an empty string array is returned not
     *         <code>null</code>.
     *
     * @see PackagingUtils#STANDARD_DEPLOYMENT
     * @see PackagingUtils#WEB_DEPLOYMENT
     */
    public static String[] getPackagingOutputFolders( BlackBerryProject bbProject ) {
        String outputRootFolder = bbProject.getProperties()._packaging.getOutputFolder();
        final BlackBerrySDKInstall bbVM = PackagingUtils.getBBSDKInstall( bbProject.getJavaProject() );
        String[] outputFolders;
        String outputFolderPrefix = StringUtils.isBlank( outputRootFolder ) ? IConstants.EMPTY_STRING : outputRootFolder
                + IPath.SEPARATOR;
        if( bbVM != null ) {
            outputFolders = new String[ 2 ];
            final String outputFolderSuffix = bbVM.getAttribute( BlackBerryVMInstallType.ATTR_RAPC_OUTPUT_FOLDER );
            if( ( outputFolderSuffix != null ) && !StringUtils.isBlank( outputFolderSuffix ) ) {
                outputFolders[ STANDARD_DEPLOYMENT ] = outputFolderPrefix + getStandardDeploymentFolderName() + IPath.SEPARATOR
                        + outputFolderSuffix;
                outputFolders[ WEB_DEPLOYMENT ] = outputFolderPrefix + getWebDeploymentFolderName() + IPath.SEPARATOR
                        + outputFolderSuffix;
            } else {
                outputFolders[ STANDARD_DEPLOYMENT ] = outputFolderPrefix + getStandardDeploymentFolderName();
                outputFolders[ WEB_DEPLOYMENT ] = outputFolderPrefix + getWebDeploymentFolderName();
            }
        } else {
            outputFolders = new String[ 2 ];
            outputFolders[ STANDARD_DEPLOYMENT ] = outputFolderPrefix + getStandardDeploymentFolderName();
            outputFolders[ WEB_DEPLOYMENT ] = outputFolderPrefix + getWebDeploymentFolderName();
        }
        return outputFolders;
    }

    /**
     * Gets the relative web output folder.
     *
     * @param bbProject
     * @return
     */
    public static String getRelativeWebOutputFolder( BlackBerryProject bbProject ) {
        String[] outputFolders = getPackagingOutputFolders( bbProject );
        if( outputFolders.length > WEB_DEPLOYMENT ) {
            return outputFolders[ WEB_DEPLOYMENT ];
        } else {
            return IConstants.EMPTY_STRING;
        }
    }

    /**
     * Gets the relative standard output folder.
     *
     * @param bbProject
     * @return
     */
    public static String getRelativeStandardOutputFolder( BlackBerryProject bbProject ) {
        String[] outputFolders = getPackagingOutputFolders( bbProject );
        if( outputFolders.length > STANDARD_DEPLOYMENT ) {
            return outputFolders[ STANDARD_DEPLOYMENT ];
        } else {
            return IConstants.EMPTY_STRING;
        }
    }

    /**
     * Gets the relative standard output folder for the given <code>vm</code>.
     *
     * @param bbProject
     * @return
     */
    public static String getRelativeStandardOutputFolder( BlackBerryProject bbProject, BlackBerrySDKInstall vm ) {
        String bbVersion = vm.getAttribute( BlackBerryVMInstallType.ATTR_RAPC_OUTPUT_FOLDER );
        String outputFolder = bbProject.getProperties()._packaging.getOutputFolder();
        if( !StringUtils.isBlank( outputFolder ) ) {
            outputFolder += IPath.SEPARATOR + getStandardDeploymentFolderName() + IPath.SEPARATOR + bbVersion;
        } else {
            outputFolder = getStandardDeploymentFolderName();
        }
        return outputFolder;
    }

    /**
     * Gets the output folder of alx file.
     *
     * @param bbProject
     * @return
     */
    public static String getRelativeAlxFileOutputFolder( BlackBerryProject bbProject ) {
        String outputFolder = bbProject.getProperties()._packaging.getOutputFolder();
        if( !StringUtils.isBlank( outputFolder ) ) {
            outputFolder += IPath.SEPARATOR + getStandardDeploymentFolderName();
        } else {
            outputFolder = getStandardDeploymentFolderName();
        }
        return outputFolder;
    }

    /**
     * Gets the absolute path of the output file for standard deployment, e.g.
     * c:/workspace/project1/deliverable/Standard/5.0.0/Helloworld
     *
     * @param project
     * @return
     */
    static public IPath getAbsoluteStandardOutputFilePath( BlackBerryProject bbProject ) {
        IPath standardOutputPath = bbProject.getProject().getLocation();
        standardOutputPath = standardOutputPath.append( getRelativeStandardOutputFilePath( bbProject ) );
        return standardOutputPath;
    }

    /**
     * Gets the relative path of the output file for standard deployment, e.g. /deliverable/Standard/5.0.0/Helloworld.
     *
     * @param project
     * @return
     */
    static public IPath getRelativeStandardOutputFilePath( BlackBerryProject bbProject ) {
        String[] outputPaths = getPackagingOutputFolders( bbProject );
        IPath standardOutputPath = new Path( outputPaths[ STANDARD_DEPLOYMENT ] );
        standardOutputPath = standardOutputPath.append( bbProject.getProperties()._packaging.getOutputFileName() );
        return standardOutputPath;
    }

    /**
     * Gets the web deployment folder name.
     *
     * @return
     */
    public static String getWebDeploymentFolderName() {
        return ImportUtils.getImportPref( "web_deployment_folder_name" );
    }

    /**
     * Gets the standard deployment folder name.
     *
     * @return
     */
    public static String getStandardDeploymentFolderName() {
        return ImportUtils.getImportPref( "standard_deployment_folder_name" );
    }

    /**
     * Replaces special characters in the given string to conform to Java standard.
     *
     * @param val
     * @return
     */
    public static String replaceSpecialChars( String val ) {
        // replace special characters
        final char replaceChr = getProjectOutputReplaceChar();
        for( char chr : BBPropertiesValidator.CHARS_WARN ) {
            if( val.indexOf( chr ) >= 0 && replaceChr != 0 ) {
                val = val.replace( chr, replaceChr );
            }
        }
        // append "_" in front if the first character is digit
        if( !val.isEmpty() && Character.isDigit( val.charAt( 0 ) ) ) {
            val = IConstants.UNDERSCORE_STRING + val;
        }
        return val;
    }

    /**
     * Returns the preferences.ini project_output_file_replace_space or 0
     *
     * @return
     */
    public static char getProjectOutputReplaceChar() {
        if( _replaceChar <= 0 ) {
            String replaceVal;
            if( ( replaceVal = ContextManager.getDefault().getPreferenceStore()
                    .getString( IConstants.PROJECT_OUTPUT_FILE_REPLACE_CHAR ) ) != null
                    && replaceVal.length() > 0 ) {
                _replaceChar = replaceVal.charAt( 0 );
            }
        }
        return _replaceChar;
    }

    public static String[] getDependantProjectOutputFolders( BlackBerryProject parentProject, BlackBerryProject dependantProject ) {
        String outputRootFolder = parentProject.getProperties()._packaging.getOutputFolder();
        final BlackBerrySDKInstall bbVM = PackagingUtils.getBBSDKInstall( dependantProject.getJavaProject() );
        String[] outputFolders = null;
        if( bbVM != null ) {
            outputFolders = new String[ 2 ];
            final String outputFolderSuffix = bbVM.getAttribute( BlackBerryVMInstallType.ATTR_RAPC_OUTPUT_FOLDER );
            String outputFolderPrefix = StringUtils.isBlank( outputRootFolder ) ? IConstants.EMPTY_STRING : outputRootFolder
                    + IPath.SEPARATOR;
            if( ( outputFolderSuffix != null ) && !StringUtils.isBlank( outputFolderSuffix ) ) {
                outputFolders[ STANDARD_DEPLOYMENT ] = outputFolderPrefix + getStandardDeploymentFolderName() + IPath.SEPARATOR
                        + outputFolderSuffix;
                outputFolders[ WEB_DEPLOYMENT ] = outputFolderPrefix + getWebDeploymentFolderName() + IPath.SEPARATOR
                        + outputFolderSuffix;
            } else {
                outputFolders[ STANDARD_DEPLOYMENT ] = outputFolderPrefix + getStandardDeploymentFolderName();
                outputFolders[ WEB_DEPLOYMENT ] = outputFolderPrefix + getWebDeploymentFolderName();
            }
        }
        return outputFolders;
    }

    public static List< String > getCodFilePathsFromProjects( Set< BlackBerryProject > projects ) {
        List< String > codFiles = new ArrayList< String >();
        // Add the output files. These are the *.cod's of all active projects in
        // the RIM workspace
        for( BlackBerryProject project : projects ) {
            String outputFolderPath = project.getProject().getLocation().toOSString() + File.separator
                    + getRelativeStandardOutputFolder( project );
            String outputFileName = project.getProperties()._packaging.getOutputFileName();
            File codFile = new File( outputFolderPath + File.separator + outputFileName + IConstants.COD_FILE_EXTENSION_WITH_DOT );
            File cslFile;
            codFiles.add( codFile.getPath() );
            try {
                List< BlackBerryProject > dependantProjects = ProjectUtils.getAllReferencedProjects( project );
                for( BlackBerryProject dependantProject : dependantProjects ) {
                    outputFolderPath = project.getProject().getLocation().toOSString() + File.separator
                            + getRelativeStandardOutputFolder( dependantProject );

                    outputFileName = dependantProject.getProperties()._packaging.getOutputFileName();
                    codFile = new File( outputFolderPath + File.separator + outputFileName
                            + IConstants.COD_FILE_EXTENSION_WITH_DOT );
                    cslFile = new File( outputFolderPath + File.separator + outputFileName
                            + IConstants.CSL_FILE_EXTENSION_WITH_DOT );

                    // The signature tool brakes if we add a file that doesn't exist to
                    // the list of files to import. This might happen if a project has
                    // build errors, for instance.
                    if( codFile.exists() && cslFile.exists() ) {
                        codFiles.add( codFile.getPath() );
                    }
                }
            } catch( CoreException ce ) {
                _log.error( "launchSignatureTool: Cannot find dependant projects ", ce );
            }
        }
        return codFiles;
    }

    /**
     * Returns if the given <code>BlackBerryProject</code> needs to be signed. This method only checks the
     * <code>BlackBerryProject</code> itself.
     *
     * @param project
     *            The project to be tested
     * @return <code>true</code> if the project needs to be signed; otherwise returns <code>false</code>
     */
    public static boolean isSigningNeeded( BlackBerryProject project ) {
        boolean signingNeeded = false;
        String outputFolderPath = project.getProject().getLocation().toOSString() + File.separator
                + getRelativeStandardOutputFolder( project );
        String outputFileName = project.getProperties()._packaging.getOutputFileName();
        File codFile = new File( outputFolderPath + File.separator + outputFileName + IConstants.COD_FILE_EXTENSION_WITH_DOT );
        File cslFile = new File( outputFolderPath + File.separator + outputFileName + IConstants.CSL_FILE_EXTENSION_WITH_DOT );
        if( codFile.exists() && cslFile.exists() ) {
            signingNeeded = true;
        }
        return signingNeeded;
    }

    /**
     * Returns if the given <code>BlackBerryProject</code> or any of its dependent projects needs to be signed
     *
     * @param bbProject
     * @return <code>true</code> if the project or any of its dependent project needs to be signed; otherwise returns
     *         <code>false</code>
     */
    public static boolean isSigningNeededForDependency( BlackBerryProject bbProject ) {
        try {
            if( isSigningNeeded( bbProject ) ) {
                return true;
            }
            List< BlackBerryProject > dependencies = ProjectUtils.getAllReferencedProjects( bbProject );
            for( BlackBerryProject project : dependencies ) {
                if( isSigningNeeded( project ) ) {
                    return true;
                }
            }
            return false;
        } catch( CoreException e ) {
            _log.error( e );
            return false;
        }
    }

    /**
     * Gets the custom jad file form the root of the given <code>project</code>.
     *
     * @param bbproj
     * @return
     */
    public static String getCustomJadFile( BlackBerryProject bbproj ) {
        return getProjectCustomFile( bbproj, IConstants.JAD_FILE_EXTENSION );
    }

    /**
     * Gets the custom rapc file from the root of the given <code>project</code>.
     *
     * @param bbproj
     * @return
     */
    public static String getCustomRapcFile( BlackBerryProject bbproj ) {
        return getProjectCustomFile( bbproj, IConstants.RAPC_FILE_EXTENSION );
    }

    /**
     * Gets the custom file with given <project-name>.<code>fileExtension</code> form the root of the <code>project</code>.
     *
     * @param bbproj
     * @return
     */
    public static String getProjectCustomFile( BlackBerryProject bbproj, String fileExtension ) {
    	if( bbproj != null && !StringUtils.isEmpty( fileExtension ) ) {
    		try {
    			IProject project = bbproj.getProject();
    			IResource[] resources = project.members();
    			for( IResource res : resources ) {
    				if( ( res instanceof IFile ) &&
    						fileExtension.equalsIgnoreCase( res.getFileExtension())) {
    					String resn = res.getName().substring(0,  res.getName().length() - fileExtension.length() -1);
    					if(resn.equals(bbproj.getProperties()._packaging.getOutputFileName()) ||
    							resn.equals(project.getName())) {
    						return res.getLocation().toOSString();
    					}
    				}
    			}
    		} catch( CoreException e ) {
    			_log.error( e );
    		}
    	}
    	return null;
    }
}
