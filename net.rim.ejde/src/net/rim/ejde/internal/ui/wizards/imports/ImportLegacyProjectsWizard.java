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
package net.rim.ejde.internal.ui.wizards.imports;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.imports.LegacyImportHelper;
import net.rim.ejde.internal.imports.WorkspaceRunnableAdapter;
import net.rim.ejde.internal.model.preferences.RootPreferences;
import net.rim.ejde.internal.util.InternalImportUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ide.Project;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.IDEInternalPreferences;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.util.PrefUtil;
import org.eclipse.ui.internal.wizards.newresource.ResourceMessages;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This is the basic legacy import wizard class.
 */
public class ImportLegacyProjectsWizard extends AbstractImporterWizard {
    static private final Logger log = Logger.getLogger( ImportLegacyProjectsWizard.class );
    protected GenericSelectionPage _selectionPage;
    static final private String ICON_PATH = "icons/wizban/import_bb_project_wizard.png";

    /**
     * Constructor
     */
    public ImportLegacyProjectsWizard() {
        setWindowTitle( Messages.ImportLegacyProjects_WIZARD_PAGE_TITLE );
        setDefaultPageImageDescriptor( AbstractUIPlugin.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, ICON_PATH ) );
    }

    public void addPages() {
        _selectionPage = new GenericSelectionPage( true );
        _selectionPage.setTitle( Messages.ImportLegacyProjects_WIZARD_TITLE_LABEL );
        addPage( _selectionPage );
    }

    /*
     * (non-Javadoc)
     *
     * @see net.rim.eide.internal.ui.wizards.AbstractImporterWizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        Set< Project > projectSet = _selectionPage.getSelectedProjects();
        if( projectSet != null && projectSet.size() != 0 ) {
            // ContextManager.getDefault().firePropertyChange(
            // IModelConstants.Events.IMPORT_FROM_LEGACY.name(), null,
            // inBuffer );
            WorkspaceRunnableAdapter task = null;
            try {
                LegacyImportHelper op = InternalImportUtils.createImportHelper( projectSet, _selectionPage );
                task = new WorkspaceRunnableAdapter( op );
                new ProgressMonitorDialog( getShell() ).run( true, true, task );
                IStatus status = op.getStatus();
                // if any error occurred, show the error
                if( status.matches( IStatus.ERROR ) ) {
                    ErrorDialog.openError( getShell(), Messages.IConstants_ERROR_DIALOG_TITLE,
                            Messages.BlackBerryImporterWizard_IMPORT_ERROR_MSG, status, IStatus.ERROR );
                }

                showDialogPersSwitch();
                // Show BlackBerry startup page
                if( RootPreferences.getOpenStartupOnNew() ) {
                    ProjectUtils.openStartupPage();
                }
                return true;

            } catch( InvocationTargetException e ) {
                log.error( e );
                return false;
            } catch( InterruptedException e ) {
                log.error( e );
                return false;
            }
        }
        return true;
    }

    private void showDialogPersSwitch() {
        IWorkbench iwb = PlatformUI.getWorkbench();
        IPerspectiveDescriptor ipd = iwb.getActiveWorkbenchWindow().getActivePage().getPerspective();

        // Check if the BB perspective is currently active, if it isn't, do
        // nothing.

        if( !ipd.getId().equals( "net.rim.ejde.ui.perspective.BlackBerryPerspective" ) ) {
            // Get the default set of preferences.
            IPreferenceStore store = IDEWorkbenchPlugin.getDefault().getPreferenceStore();
            // Get the default value for the persp switch preference.
            String pspm = store.getString( IDEInternalPreferences.PROJECT_SWITCH_PERSP_MODE );

            if( !IDEInternalPreferences.PSPM_PROMPT.equals( pspm ) ) {
                // Check if we need to switch or not. If not, stop here.
                return;
            } else {
                // If the user has already said yes, no need create the dialog. Just switch perspective.
            }

            // Add the perspective message and the perspective name to the
            // dialog
            String message = Messages.NewProject_perspSwitchMessage;
            MessageDialogWithToggle dialog = MessageDialogWithToggle.openYesNoQuestion( null,
                    ResourceMessages.NewProject_perspSwitchTitle, message, null /*
                                                                                 * use the default message for the toggle
                                                                                 */, false /*
                                                                                            * toggle is initially unchecked
                                                                                            */, store,
                    IDEInternalPreferences.PROJECT_SWITCH_PERSP_MODE );
            int result = dialog.getReturnCode();

            String preferenceValue;
            if( dialog.getToggleState() ) {

                if( result == IDialogConstants.YES_ID ) {
                    preferenceValue = IWorkbenchPreferenceConstants.OPEN_PERSPECTIVE_REPLACE;
                } else {
                    preferenceValue = IWorkbenchPreferenceConstants.NO_NEW_PERSPECTIVE;
                }

                PrefUtil.getAPIPreferenceStore().setValue( IDE.Preferences.PROJECT_OPEN_NEW_PERSPECTIVE, preferenceValue );
            }

            // If the user said yes, switch the perspective.
            if( result == IDialogConstants.YES_ID ) {
                switchPerspective();
            }

        }
    }

    private void switchPerspective() {

        // Open the BB Perspective
        try {
            PlatformUI.getWorkbench().showPerspective( "net.rim.ejde.ui.perspective.BlackBerryPerspective",
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow() );
        } catch( WorkbenchException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see net.rim.eide.internal.ui.wizards.AbstractImporterWizard#performCancel()
     */
    @Override
    public boolean performCancel() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init( IWorkbench workbench, IStructuredSelection selection ) {
        // TODO Auto-generated method stub
    }
}
