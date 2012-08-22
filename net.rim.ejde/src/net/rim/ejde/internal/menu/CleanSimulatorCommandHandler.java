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
package net.rim.ejde.internal.menu;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.launching.DeviceInfo;
import net.rim.ejde.internal.launching.DeviceProfileManager;
import net.rim.ejde.internal.model.preferences.CleanSimulatorPreferences;
import net.rim.ejde.internal.ui.dialogs.CleanSimulatorDialog;
import net.rim.ejde.internal.ui.dialogs.CleanSimulatorTreeItem;
import net.rim.ejde.internal.ui.dialogs.CleanSimulatorTreeItem.ItemId;
import net.rim.ejde.internal.ui.dialogs.CleanSimulatorTreeItem.ItemType;
import net.rim.ejde.internal.ui.dialogs.CleanSimulatorTreeItemExternal;
import net.rim.ejde.internal.ui.dialogs.CleanSimulatorTreeItemInternal;
import net.rim.ejde.internal.util.FileUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ide.RIA;
import net.rim.ide.core.IDEProperties;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IColorDecorator;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * CleanSimulatorCommandHandler
 *
 * @author bkurz
 *
 */
public class CleanSimulatorCommandHandler extends AbstractHandler {
    private static final Logger _log = Logger.getLogger( CleanSimulatorCommandHandler.class );

    @Override
    public Object execute( ExecutionEvent event ) throws ExecutionException {
        List< CleanSimulatorTreeItem > treeStructure = createTreeStructure();

        // Create and display clean simulator dialog
        CleanSimulatorDialog dialog = new CleanSimulatorDialog( ContextManager.getActiveWorkbenchShell(),
                new TreeLabelProvider(), new ColorLabelDecorator(), new TreeContentProvider(), new CheckStateProvider() );
        dialog.setTitle( Messages.CLEAN_SIMULATOR_DIALOG_TITLE );
        dialog.setMessage( Messages.CLEAN_SIMULATOR_DIALOG_MESSAGE );
        dialog.setInput( treeStructure );
        dialog.open();

        // Execute clean for all checked items returned from the clean simulator
        // dialog and set preferences
        final Object[] result = dialog.getResult();
        if( result != null ) {
            executeClean( result );
            CleanSimulatorPreferences.setCleanSimulatorPreferences( treeStructure );
        }
        return null;
    }

    /**
     * Creates the tree structure for the clean options for internal SDKs and external simulator bundles
     *
     * @return
     */
    private List< CleanSimulatorTreeItem > createTreeStructure() {
        List< IVMInstall > installedSDK = VMUtils.getInstalledBBVMs();
        List< CleanSimulatorTreeItem > treeStructure = new ArrayList< CleanSimulatorTreeItem >();
        CleanSimulatorTreeItem rootItem;

        if( installedSDK.size() == 0 ) {
            return treeStructure;
        }

        // Create tree structure for internal simulator bundles
        for( IVMInstall ivmInstall : installedSDK ) {
            // list only those CPs that have simulator
            List< DeviceInfo > deviceProfiles = DeviceProfileManager.getInstance().getInternalDeviceProfiles( ivmInstall );
            if( !deviceProfiles.isEmpty() ) {
                rootItem = createModelInternalBundel( ivmInstall );
                treeStructure.add( rootItem );
            }
        }

        // Create tree structure for external simulator bundles
        List< DeviceInfo > deviceProfiles = DeviceProfileManager.getInstance().getExternalDeviceProfiles();
        for( DeviceInfo deviceProfile : deviceProfiles ) {
            rootItem = createModelExternalBundle( deviceProfile );
            treeStructure.add( rootItem );
        }

        return CleanSimulatorPreferences.getCleanSimulatorPreferences( treeStructure );
    }

    /**
     * Schedules and executes the clean job for all selected SDKs and external simulator bundles
     *
     * @param result
     */
    private void executeClean( final Object[] result ) {
        Job cleanJob = new Job( Messages.CLEAN_SIMULATOR_JOB_TITLE ) {
            @Override
            protected IStatus run( IProgressMonitor monitor ) {
                IStatus status;
                monitor.beginTask( Messages.CLEAN_SIMULATOR_JOB_TITLE, result.length );
                status = executeCleanJob( result, monitor );
                monitor.done();
                return status;
            }
        };
        cleanJob.setUser( true );
        cleanJob.schedule();
    }

    /**
     * Executes the clean job for all selectes SDKs and external simulator bundles
     *
     * @param result
     * @param monitor
     * @return
     */
    private IStatus executeCleanJob( final Object[] result, IProgressMonitor monitor ) {
        List< DeviceInfo > deviceProfiles = new ArrayList< DeviceInfo >();
        DeviceInfo externalDeviceProfile = null;
        IVMInstall vmInstall = null;
        RIA ria = null;
        CleanSimulatorTreeItem bbTreeItem;
        for( Object item : result ) {
            if( monitor != null && monitor.isCanceled() ) {
                return Status.CANCEL_STATUS;
            }

            bbTreeItem = (CleanSimulatorTreeItem) item;
            if( bbTreeItem.getItemType().equals( ItemType.INTERNAL_BUNDLE ) ) {
                CleanSimulatorTreeItemInternal bbTreeItemInternal = (CleanSimulatorTreeItemInternal) bbTreeItem;

                if( vmInstall == null || !vmInstall.equals( bbTreeItemInternal.getVMInstall() ) ) {
                    vmInstall = bbTreeItemInternal.getVMInstall();
                    ria = ContextManager.PLUGIN.getRIA( vmInstall.getInstallLocation().getPath() );
                    deviceProfiles = DeviceProfileManager.getInstance().getInternalDeviceProfiles( vmInstall );
                }
            } else if( bbTreeItem.getItemType().equals( ItemType.EXTERNAL_BUNDLE ) ) {
                ria = ContextManager.PLUGIN.getRIA( VMUtils.getLatestSDK().getInstallLocation().getPath() );
                externalDeviceProfile = DeviceProfileManager.getInstance().getExternalDeviceProfile(
                        bbTreeItem.getRootItem().getItemName() );
            }

            // Clean applications
            if( bbTreeItem.getItemID().equals( ItemId.CLEAN_SIMULATOR_DIRECTORY ) ) {
                cleanSimulator( bbTreeItem.getItemType().equals( ItemType.INTERNAL_BUNDLE ) ? deviceProfiles.get( 0 )
                        : externalDeviceProfile );
            }

            // Search for and clean devices
            if( bbTreeItem.getItemType().equals( ItemType.INTERNAL_BUNDLE ) ) {
                for( DeviceInfo deviceProfile : deviceProfiles ) {
                    cleanRIA( bbTreeItem, ria, deviceProfile );
                }
            } else if( bbTreeItem.getItemType().equals( ItemType.EXTERNAL_BUNDLE ) ) {
                cleanRIA( bbTreeItem, ria, externalDeviceProfile );
            }

            if( monitor != null ) {
                monitor.worked( 1 );
            }
        }
        return Status.OK_STATUS;
    }

    /**
     *
     * @param item
     * @param ria
     */
    private void cleanRIA( CleanSimulatorTreeItem item, RIA ria, DeviceInfo device ) {
        updateRIA( ria, device );
        String deviceBundleName = device.getBundleName();
        String consoleOutput = " (" + device.getDeviceName() + ")";
        switch( item.getItemID() ) {
            case ERASE_FILE_SYSTEM:
                ria.eraseFileSystem();
                _log.debug( Messages.CLEAN_SIMULATOR_ERASE_FILE_SYSTEM_DEBUG_MSG + deviceBundleName + consoleOutput );
                break;
            case ERASE_NON_VOLATILE_MEMORY:
                ria.eraseNvStore();
                _log.debug( Messages.CLEAN_SIMULATOR_ERASE_NON_VOLATILE_MEMORY_DEBUG_MSG + deviceBundleName + consoleOutput );
                break;
            case ERASE_REMOVABLE_MEMORY:
                ria.eraseSDCard();
                _log.debug( Messages.CLEAN_SIMULATOR_ERASE_REMOVABLE_DIRECTORY_DEBUG_MSG + deviceBundleName + consoleOutput );
                break;
        }
    }

    /**
     * Cleans the applications (.cod and .debug files) from the simulator specified
     *
     * @param deviceProfile
     */
    private static void cleanSimulator( DeviceInfo deviceProfile ) {
        _log.info( Messages.CLEAN_SIMULATOR_CLEAN_SIMULATOR_DIRECTORY_DEBUG_MSG + " " + deviceProfile.getDirectory() );

        IPath path = new Path( deviceProfile.getDirectory() ).append( IConstants.SIMULATOR_MANIFEST_FILE_NAME );
        cleanSimulator( path );
    }

    /**
     * Cleans the applications (.cod and .debug files) from the specified simulator directory
     *
     * @param path
     */
    private static void cleanSimulator( IPath path ) {
        File f = path.toFile();
        if( f.exists() ) {
            File[] files = f.getParentFile().listFiles();
            List< String > manifestFiles = FileUtils.readFile( f );
            String fileExtension;

            if( manifestFiles == null ) {
                _log.error( NLS.bind( Messages.CLEAN_SIMULATOR_DIR_CORRUPT_MSG, f.getParent() ) );
                showCleanError( NLS.bind( Messages.CLEAN_SIMULATOR_DIR_CORRUPT_MSG, f.getParent() ) );
            } else if( !f.canWrite() ) {
                _log.error( NLS.bind( Messages.CLEAN_SIMULATOR_MANIFEST_PERM_MSG, path ) );
                showCleanError( NLS.bind( Messages.CLEAN_SIMULATOR_MANIFEST_PERM_MSG, path ) );
            } else {
                manifestFiles = fixManifestFile( f, manifestFiles );
                boolean first = true;
                for( File file : files ) {
                    fileExtension = FileUtils.getFileExtension( file );
                    if( !manifestFiles.contains( file.getName() ) && file.isFile() && !file.equals( f )
                            && !fileExtension.equals( IConstants.SIMULATOR_DMP_FILE_EXTENSION ) ) {
                        if( file.exists() && !file.delete() ) {
                            String msg1 = Messages.CLEAN_SIMULATOR_CANNOT_DEL_FILE_MSG + "'" + file.getName() + "'";
                            final String msg2 = msg1 + "\n"
                                    + NLS.bind( Messages.CLEAN_SIMULATOR_CHECK_PERM_MSG, file.getParent() );
                            _log.error( msg1 );
                            if( first ) { // pop a dialog only for the 1st artifact that could not be deleted
                                showCleanError( msg2 );
                                first = false;
                            }
                        }
                    }
                }
            }
        } else {
            _log.error( NLS.bind( Messages.CLEAN_SIMULATOR_MANIFEST_MISSING_MSG, path ) );
            showCleanError( NLS.bind( Messages.CLEAN_SIMULATOR_MANIFEST_MISSING_MSG, path ) );
        }
    }

    private static List< String > fixManifestFile( File f, List< String > manifestFiles ) {
        // Fix for MKS2085690 and MKS2024707- when _manifest does not contains the _manefest
        // entry itself we will write the entry.
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter( f, true );
            bw = new BufferedWriter( fw );
            if( !manifestFiles.contains( f.getName() ) || !manifestFiles.contains( "uninst.exe" )
                    || !manifestFiles.contains( "Uninstall.dat" ) ) {
                bw.append( '\n' );
            }
            if( !manifestFiles.contains( f.getName() ) ) {
                _log.info( "_manifest file updated with \"" + f.getName() + "\" entry" );
                bw.write( f.getName() + "\n" );
            }
            // produced by installer
            if( !manifestFiles.contains( "uninst.exe" ) ) {
                bw.write( "uninst.exe\n" );
                manifestFiles.add( "uninst.exe\n" );
                _log.info( "_manifest file updated with \"uninst.exe\" entry" );
            }
            if( !manifestFiles.contains( "Uninstall.dat" ) ) {
                bw.write( "Uninstall.dat" );
                manifestFiles.add( "Uninstall.dat" );
                _log.info( "_manifest file updated with \"Uninstall.dat\" entry" );
            }
        } catch( IOException e ) {
            _log.error( NLS.bind( Messages.CLEAN_SIMULATOR_MANIFEST_NOT_REPAIR_MSG, f.getAbsolutePath() ) );
            showCleanError( NLS.bind( Messages.CLEAN_SIMULATOR_MANIFEST_NOT_REPAIR_MSG, f.getAbsolutePath() ) );
        } finally {
            if( bw != null )
                try {
                    bw.close();
                } catch( IOException e ) {
                    ;
                }
        }
        return manifestFiles;
    }

    private static void showCleanError( final String msg ) {
        Runnable r = new Runnable() {
            public void run() {
                MessageDialog.openError( ContextManager.getActiveWorkbenchShell(),
                        Messages.CLEAN_SIMULATOR_ERASE_SIMULATOR_FILES_LABEL, msg );
            }
        };
        ContextManager.getDisplay().asyncExec( r );
    }

    /**
     *
     * @param ria
     * @param deviceProfile
     */
    private void updateRIA( RIA ria, DeviceInfo deviceProfile ) {
        IPath path = new Path( deviceProfile.getDirectory() ).append( IConstants.FLEDGE_FILE_NAME );
        String commandLine = "\"" + path + "\" /handheld=" + deviceProfile.getDeviceName() + " /session="
                + deviceProfile.getDeviceName();
        String workingDirectory = deviceProfile.getDirectory();

        IDEProperties ideProperties = ria.getProperties();
        ideProperties.putStringProperty( "SimulatorDirectory", workingDirectory );
        ideProperties.putStringProperty( "SimulatorCommand", commandLine );
    }

    /**
     * Creates the tree structure for the specified SDK
     *
     * @param vmInstall
     * @return
     */
    private CleanSimulatorTreeItem createModelInternalBundel( IVMInstall vmInstall ) {
        CleanSimulatorTreeItemInternal item = new CleanSimulatorTreeItemInternal( vmInstall, ItemId.SDK, vmInstall.getName(),
                new CleanSimulatorTreeItem[] {
                        new CleanSimulatorTreeItemInternal( vmInstall, ItemId.CLEAN_SIMULATOR_DIRECTORY,
                                Messages.CLEAN_SIMULATOR_CLEAN_SIMULATOR_DIRECTORY_LABEL ),
                        new CleanSimulatorTreeItemInternal( vmInstall, ItemId.ERASE_FILE_SYSTEM,
                                Messages.CLEAN_SIMULATOR_ERASE_FILE_SYSTEM_LABEL ),
                        new CleanSimulatorTreeItemInternal( vmInstall, ItemId.ERASE_REMOVABLE_MEMORY,
                                Messages.CLEAN_SIMULATOR_ERASE_REMOVABLE_MEMORY_LABEL ),
                        new CleanSimulatorTreeItemInternal( vmInstall, ItemId.ERASE_NON_VOLATILE_MEMORY,
                                Messages.CLEAN_SIMULATOR_ERASE_NON_VOLATILE_MEMORY_LABEL ) } );
        return item;
    }

    /**
     * Creates the tree structure for the specified SDK
     *
     * @param deviceProfile
     * @return
     */

    private CleanSimulatorTreeItem createModelExternalBundle( DeviceInfo deviceProfile ) {
        CleanSimulatorTreeItemExternal item = new CleanSimulatorTreeItemExternal( deviceProfile, ItemId.SDK,
                deviceProfile.getBundleName(), new CleanSimulatorTreeItem[] {
                        new CleanSimulatorTreeItemExternal( deviceProfile, ItemId.CLEAN_SIMULATOR_DIRECTORY,
                                Messages.CLEAN_SIMULATOR_CLEAN_SIMULATOR_DIRECTORY_LABEL ),
                        new CleanSimulatorTreeItemExternal( deviceProfile, ItemId.ERASE_FILE_SYSTEM,
                                Messages.CLEAN_SIMULATOR_ERASE_FILE_SYSTEM_LABEL ),
                        new CleanSimulatorTreeItemExternal( deviceProfile, ItemId.ERASE_REMOVABLE_MEMORY,
                                Messages.CLEAN_SIMULATOR_ERASE_REMOVABLE_MEMORY_LABEL ),
                        new CleanSimulatorTreeItemExternal( deviceProfile, ItemId.ERASE_NON_VOLATILE_MEMORY,
                                Messages.CLEAN_SIMULATOR_ERASE_NON_VOLATILE_MEMORY_LABEL ) } );
        return item;
    }

    /**
     * Implementation of a tree label provider
     *
     * @author bkurz
     *
     */
    private class TreeLabelProvider extends LabelProvider {
        @Override
        public Image getImage( Object element ) {
            return null;
        }

        @Override
        public String getText( Object element ) {
            CleanSimulatorTreeItem item = (CleanSimulatorTreeItem) element;
            return item.getItemName();
        }
    }

    /**
     * Implementation of a tree content provider
     *
     * @author bkurz
     *
     */
    private class TreeContentProvider extends ArrayContentProvider implements ITreeContentProvider {
        @Override
        public Object[] getChildren( Object parentElement ) {
            CleanSimulatorTreeItem item = (CleanSimulatorTreeItem) parentElement;
            return item.getChildItems();
        }

        @Override
        public Object getParent( Object element ) {
            CleanSimulatorTreeItem item = (CleanSimulatorTreeItem) element;
            return item.getParentItem();
        }

        @Override
        public boolean hasChildren( Object element ) {
            CleanSimulatorTreeItem item = (CleanSimulatorTreeItem) element;
            return ( item.hasChildItems() );
        }
    }

    /**
     * Implementation of a check state provider
     *
     * @author bkurz
     *
     */
    private class CheckStateProvider implements ICheckStateProvider {
        @Override
        public boolean isChecked( Object element ) {
            CleanSimulatorTreeItem item = (CleanSimulatorTreeItem) element;
            return item.isChecked();
        }

        @Override
        public boolean isGrayed( Object element ) {
            CleanSimulatorTreeItem item = (CleanSimulatorTreeItem) element;
            return item.isGrayed();
        }
    }

    /**
     *
     * @author bkurz
     *
     */
    private class ColorLabelDecorator implements ILabelDecorator, IColorDecorator {
        @Override
        public Image decorateImage( Image image, Object element ) {
            return null;
        }

        @Override
        public String decorateText( String text, Object element ) {
            return text;
        }

        @Override
        public void addListener( ILabelProviderListener listener ) {
            // Do nothing
        }

        @Override
        public void dispose() {
            // Do nothing
        }

        @Override
        public boolean isLabelProperty( Object element, String property ) {
            return false;
        }

        @Override
        public void removeListener( ILabelProviderListener listener ) {
            // Do nothing
        }

        @Override
        public Color decorateBackground( Object element ) {
            return Display.getCurrent().getSystemColor( SWT.COLOR_LIST_BACKGROUND );
        }

        @Override
        public Color decorateForeground( Object element ) {
            CleanSimulatorTreeItem item = (CleanSimulatorTreeItem) element;
            if( !item.isEnabled() ) {
                return new Color( Display.getCurrent(), new RGB( 172, 168, 153 ) );
            } else {
                return Display.getCurrent().getSystemColor( SWT.COLOR_BLACK );
            }
        }
    }
}
