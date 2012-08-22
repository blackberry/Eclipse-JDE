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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.ui.launchers.LaunchUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PackagingUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ejde.internal.util.VMToolsUtils;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ide.RIA;
import net.rim.ide.core.Util;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.osgi.util.NLS;

/**
 * Deploy cod files to simulator or device for debugging.
 *
 */
public class DeploymentTask {

    private static final Logger _log = Logger.getLogger( DeploymentTask.class );

    private Set< BlackBerryProject > _projects;
    private ILaunchConfiguration _configuration;
    // if hot-swap on simulator or device is successful
    private static boolean _isHotswapSuccess;

    /**
     * Constructs a DeploymentTask instance.
     *
     * @param name
     * @param projects
     * @param configuration
     */
    public DeploymentTask( String name, Set< BlackBerryProject > projects, ILaunchConfiguration configuration ) {
        _projects = projects;
        _configuration = configuration;
    }

    public IStatus run( IProgressMonitor monitor ) throws CoreException {
        IStatus ret = Status.OK_STATUS;
        LinkedHashSet< BlackBerryProject > projectSet = ProjectUtils.getProjectsByBuildOrder( _projects );
        monitor.beginTask( "Deploying projects", projectSet.size() );
        if( _configuration.getType().getIdentifier().equals( IDeviceLaunchConstants.LAUNCH_CONFIG_ID ) ) {
            ret = handleDeviceDeployment( monitor );
        } else {
            ret = handleSimulatorDeployment( monitor );
        }
        return ret;
    }

    private IStatus handleSimulatorDeployment( IProgressMonitor monitor ) throws CoreException {
        IStatus status = Status.OK_STATUS;
        ILaunch launch = LaunchUtils.getRunningBBLaunch();
        if( launch != null ) {
            LaunchUtils.closeLaunch( launch );
            status = deployProjectsToSDK( monitor );
            if( status.isOK() ) {
                status = hotSwap( monitor );
            }
        } else {
            status = deployProjectsToSDK( monitor );
        }
        return status;
    }

    private IStatus handleDeviceDeployment( IProgressMonitor monitor ) throws CoreException {
        IStatus status = Status.OK_STATUS;
        ILaunch launch = LaunchUtils.getRunningBBLaunch();
        if( launch != null ) {
            LaunchUtils.closeLaunch( launch );
        }
        // Copy build artifacts to SDK folder first, this is needed by debugger to locate .debug files
        status = deployProjectsToSDK( monitor );
        if( status.isOK() && _configuration.getAttribute( IDeviceLaunchConstants.DEPLOY_PROJECT_TO_DEVICE, true ) ) {

            _log.info( "Load projects on device" );
            // Device deployment requires the latest JavaLoader that comes with CP5.0 or later. If JavaLoader does not support
            // device deployment, skip the step and go to attaching debugger.
            String javaLoaderVersion = VMToolsUtils.getStoredVersion();
            if( javaLoaderVersion.compareTo( IConstants.SDK_FIVE_VERSION ) < 0 ) {
                _log.info( "Skip device deployment because Javaloader (" + javaLoaderVersion + ") does not support." );
                return Status.OK_STATUS;
            }

            IVMInstall vm = LaunchUtils.getVMFromConfiguration( _configuration );
            RIA debugger = ContextManager.PLUGIN.getRIA( vm.getInstallLocation().getAbsolutePath() );
            if( debugger == null ) {
                return StatusFactory.createErrorStatus( NLS.bind( Messages.RIA_NO_RIA_INSTANCE_ERROR_MSG, vm.getName() ) );
            }
            // get device PIN
            String debugDest = _configuration.getAttribute( IDeviceLaunchConstants.DEVICE, IDeviceLaunchConstants.ANY_DEVICE );
            if( debugDest.equals( IDeviceLaunchConstants.ANY_DEVICE ) ) {
                String[] attachedDevices = debugger.getDebugDeviceList();
                if( attachedDevices.length == 0 ) {
                    return StatusFactory.createErrorStatus( Messages.DeviceLaunchConfigurationDelegate_noDeviceMsg );
                }
                debugDest = attachedDevices[ 0 ];
            }
            _log.debug( "Debug destination: " + debugDest );
            String javaLoaderCmd = "javaloader";
            IPath javaLoaderPath;
            try {
                javaLoaderPath = VMToolsUtils.getJavaLoaderPath();
                // check the java loader again
                if( !VMToolsUtils.isVMToolValid() ) {
                    return StatusFactory.createErrorStatus( Messages.JavaLoader_Not_Found_Msg );
                }
                javaLoaderCmd = Util.quoteFile( javaLoaderPath.toOSString() );
            } catch( IOException e ) {
                return StatusFactory.createErrorStatus( e.getMessage() );
            }
            _log.debug( "set JavaLoader command: " + javaLoaderCmd );
            debugger.setJavaLoaderCommand( javaLoaderCmd );
            String[] javaLoaderOptions = debugger.getJavaLoaderOptions( debugDest );
            // user enter a wrong password or cancel
            if( javaLoaderOptions == null ) {
                _log.trace( "Deployment Canceled!" );
                return Status.CANCEL_STATUS;
            }
            // check if the device supports hot-swap
            if( debugger.deviceSupportsHotSwap( debugDest ) ) {
                status = deviceHotSwap( monitor, debugDest, _projects );
            } else {
        		String[] args = new String[javaLoaderOptions.length+1];
        		args[0]       = javaLoaderCmd;
        		for(int i = 0; i < javaLoaderOptions.length; i++ ){
        			args[i+1] = javaLoaderOptions[i];
        		}
                status = deviceNormalDeployment( monitor, args, _projects );
            }
        }
        return status;
    }

    /**
     * Deploy the projects to SDK folder.
     *
     * @param monitor
     * @return
     * @throws CoreException
     */
    private IStatus deployProjectsToSDK( IProgressMonitor monitor ) throws CoreException {
        IVMInstall vm = LaunchUtils.getVMFromConfiguration( _configuration );
        DeviceInfo device = LaunchUtils.getDeviceToLaunch( _configuration );
        String simDir = device.getDirectory();
        String vmDir = LaunchUtils.getSimualtorPath( vm );
        boolean internalMode = VMUtils.isInternal( vm );
        for( BlackBerryProject bbProject : _projects ) {
            if( !ProjectUtils.hasCriticalProblems( bbProject.getProject() ) ) {
                // always deploy to VM folder first
                DeploymentHelper.deploy( bbProject, vmDir, internalMode );
                if( !vmDir.equals( simDir ) ) {
                    // this is external simulator, deploy to external simulator folder as well
                    DeploymentHelper.deploy( bbProject, simDir, false );
                }
            }
            if( monitor.isCanceled() ) {
                monitor.done();
                return Status.CANCEL_STATUS;
            }
            monitor.worked( 1 );

        }
        monitor.done();
        return Status.OK_STATUS;
    }

    /**
     * Simulator hot-swap.
     *
     * @param monitor
     * @return
     * @throws CoreException
     */
    private IStatus hotSwap( IProgressMonitor monitor ) throws CoreException {
        final RIA ria = RIA.getCurrentDebugger();
        final List< File > files = getCodFiles( _projects );
        if( files.isEmpty() ) {
            return StatusFactory.createErrorStatus( Messages.Luanch_Error_NoProjectToBeDeployed );
        }
        ria.deployedFilesClear();
        for( File file : files ) {
            ria.deployedFileAdd( file );
        }
        Thread thread = new Thread() {
            public void run() {
                _log.debug( "Hot-swap: " + files );
                _isHotswapSuccess = ria.deployFilesToSimulator();
            }
        };
        thread.start();
        for( ;; ) {
            try {
                thread.join( 2000 );
            } catch( InterruptedException e ) {
                // do nothing
            }
            if( monitor.isCanceled() ) {
                return Status.CANCEL_STATUS;
            }
            if( !thread.isAlive() ) {
                break;
            }
        }
        return _isHotswapSuccess ? Status.OK_STATUS : Status.CANCEL_STATUS;
    }

    /**
     * Device hot-swap.
     *
     * @param monitor
     * @param device
     * @param projects
     * @return
     * @throws CoreException
     */
    private static IStatus deviceHotSwap( IProgressMonitor monitor, final String device, Collection< BlackBerryProject > projects )
            throws CoreException {
        _log.debug( "Performing device hot-swap" );
        final RIA ria = RIA.getCurrentDebugger();
        final List< File > files = getJadFiles( projects );
        if( files.isEmpty() ) {
            return StatusFactory.createErrorStatus( Messages.Luanch_Error_NoProjectToBeDeployed );
        }
        ria.deployedFilesClear();
        for( File file : files ) {
            ria.deployedFileAdd( file );
        }
        Thread thread = new Thread() {
            public void run() {
                _log.debug( "Deploying files through RIA: " + files );
                _isHotswapSuccess = ria.deployFilesToDevice( device );
                _log.debug( _isHotswapSuccess ? "Deployment complete." : "Deployment failed" );
            }
        };
        thread.start();
        for( ;; ) {
            try {
                thread.join( 2000 );
            } catch( InterruptedException e ) {
                // do nothing
            }
            if( monitor.isCanceled() ) {
                return Status.CANCEL_STATUS;
            }
            if( !thread.isAlive() ) {
                break;
            }
        }
        return _isHotswapSuccess ? Status.OK_STATUS : Status.CANCEL_STATUS;
    }

    private static IStatus deviceNormalDeployment( IProgressMonitor monitor, String[] javaLoaderCmd,
            Collection< BlackBerryProject > projects ) {
        _log.debug( "Deploy projects on device, JavaLoader command:" + javaLoaderCmd );
        List< File > deploymentFiles = getJadFiles( projects );
        if( deploymentFiles.isEmpty() ) {
            return StatusFactory.createErrorStatus( Messages.Luanch_Error_NoProjectToBeDeployed );
        }
        for( File f : deploymentFiles ) {
            String cmd = javaLoaderCmd.toString() + " load " + Util.quoteFile( f.getPath() );
            _log.debug( "Deploying: " + cmd );
    		String[] args = new String[javaLoaderCmd.length+2];
    		for(int i = 0; i < javaLoaderCmd.length; i++ ){
    			args[i] = javaLoaderCmd[i];
    		}
    		args[args.length-2] = "load";
    		args[args.length-1] = f.getPath();
            Util.runCommand(args,new Vector(),null);
        }
        return Status.OK_STATUS;
    }

    private static List< File > getCodFiles( Collection< BlackBerryProject > projects ) {
        List< File > files = new ArrayList< File >();
        for( BlackBerryProject project : projects ) {
            if( !ProjectUtils.hasCriticalProblems( project.getProject() ) ) {
                BlackBerryProperties properties = project.getProperties();
                String outputFileName = properties._packaging.getOutputFileName();
                String outputPath = project.getProject().getLocation().toOSString();
                String[] outputPaths = PackagingUtils.getPackagingOutputFolders( project );
                // we deploy the standard deliverables
                outputPath += IPath.SEPARATOR + outputPaths[ PackagingUtils.STANDARD_DEPLOYMENT ];
                String deployFile = outputPath + File.separator + outputFileName + ".cod";
                files.add( new File( deployFile ) );
            }
        }
        return files;
    }

    private static List< File > getJadFiles( Collection< BlackBerryProject > projects ) {
        try {
            filterOutDependencyProjects( projects );
        } catch( CoreException e ) {
            _log.error( e );
        }
        List< File > files = new ArrayList< File >();
        for( BlackBerryProject project : projects ) {
            if( !ProjectUtils.hasCriticalProblems( project.getProject() ) ) {
                BlackBerryProperties properties = project.getProperties();
                String outputFileName = properties._packaging.getOutputFileName();
                String outputPath = project.getProject().getLocation().toOSString();
                String[] outputPaths = PackagingUtils.getPackagingOutputFolders( project );
                // we deploy the standard deliverables
                outputPath += IPath.SEPARATOR + outputPaths[ PackagingUtils.STANDARD_DEPLOYMENT ];
                // look for XXX_full.jad file first
                File deployFile = new File( outputPath + File.separator + outputFileName + IConstants.FULL_JAD_FILE_SUFFIX
                        + IConstants.JAD_FILE_EXTENSION_WITH_DOT );
                if( !deployFile.exists() ) {
                    // if XXX.full.jad file does not exist, look for XXX.jad file
                    deployFile = new File( outputPath + File.separator + outputFileName + IConstants.JAD_FILE_EXTENSION_WITH_DOT );
                }
                files.add( deployFile );
            }
        }
        return files;
    }

    /**
     * Filters out all dependency projects. We only want to deploy jad files of main projects.
     *
     * @param projects
     * @throws CoreException
     */
    private static void filterOutDependencyProjects( Collection< BlackBerryProject > projects ) throws CoreException {
        Collection< BlackBerryProject > projectsNeedToBeRemoved = new HashSet< BlackBerryProject >();
        List< BlackBerryProject > referencedProjects;
        BlackBerryProject bbProject;
        for( Iterator< BlackBerryProject > iterator = projects.iterator(); iterator.hasNext(); ) {
            bbProject = iterator.next();
            referencedProjects = ProjectUtils.getAllReferencedProjects( bbProject );
            for( BlackBerryProject referencedproject : referencedProjects ) {
                for( BlackBerryProject project : projects ) {
                    if( referencedproject.getProject().getName().equals( project.getProject().getName() ) ) {
                        projectsNeedToBeRemoved.add( project );
                        break;
                    }
                }
            }
        }
        projects.removeAll( projectsNeedToBeRemoved );
    }

    /**
     * Load the given {@link BlackBerryProject}s to device currently attached to USB port.
     *
     * @param projects
     *            The projects
     * @param monitor
     *            The progress monitor
     * @return The status
     * @throws CoreException
     */
    public static IStatus loadProjectsToDevice( Collection< BlackBerryProject > projects, IProgressMonitor monitor )
            throws CoreException {

        _log.debug( "Loading projects on device" );

        // Close BlackBerry Launch if there is
        ILaunch launch = LaunchUtils.getRunningBBLaunch();
        if( launch != null ) {
            LaunchUtils.closeLaunch( launch );
        }

        monitor.beginTask( IConstants.EMPTY_STRING, 100 );
        monitor.subTask( Messages.LoadProjectsOnDevice );
        IVMInstall vm = ProjectUtils.getVMForProjects( projects );
        RIA debugger = ContextManager.PLUGIN.getRIA( vm.getInstallLocation().getAbsolutePath() );
        if( debugger == null ) {
            return StatusFactory.createErrorStatus( NLS.bind( Messages.RIA_NO_RIA_INSTANCE_ERROR_MSG, vm.getName() ) );
        }

        // Get device PIN
        String[] attachedDevices = debugger.getDebugDeviceList();
        if( attachedDevices.length == 0 ) {
            throw new CoreException( StatusFactory.createErrorStatus( Messages.DeviceLaunchConfigurationDelegate_noDeviceMsg ) );
        }
        // Use the first device if multiple devices are found
        String debugDest = attachedDevices[ 0 ];

        // Get JavaLoader command
        String javaLoaderCmd = "javaloader";
        IPath javaLoaderPath;
        try {
            javaLoaderPath = VMToolsUtils.getJavaLoaderPath();
            // check the java loader again
            if( !VMToolsUtils.isVMToolValid() ) {
                return StatusFactory.createErrorStatus( Messages.JavaLoader_Not_Found_Msg );
            }
            javaLoaderCmd = Util.quoteFile( javaLoaderPath.toOSString() );
        } catch( IOException e ) {
            return StatusFactory.createErrorStatus( e.getMessage() );
        }
        monitor.worked( 20 );
        _log.debug( "set JavaLoader command:" + javaLoaderCmd );
        debugger.setJavaLoaderCommand( javaLoaderCmd );

        // Get JavaLoader options
        _log.debug( "Get JavaLoader options" );
        String[] javaLoaderOptions = debugger.getJavaLoaderOptions( debugDest );
        _log.debug( "JavaLoader options:" + Arrays.toString(javaLoaderOptions) );
        // user entered wrong password or canceled
        if( javaLoaderOptions == null ) {
            return Status.CANCEL_STATUS;
        }

        IStatus status;
        monitor.worked( 30 );
        if( debugger.deviceSupportsHotSwap( debugDest ) ) {
            status = deviceHotSwap( monitor, debugDest, projects );
        } else {
    		String[] args = new String[javaLoaderOptions.length+1];
    		args[0]       = javaLoaderCmd;
    		for(int i = 0; i < javaLoaderOptions.length; i++ ){
    			args[i+1] = javaLoaderOptions[i];
    		}
            status = deviceNormalDeployment( monitor, args, projects );
        }
        return status;
    }
}
