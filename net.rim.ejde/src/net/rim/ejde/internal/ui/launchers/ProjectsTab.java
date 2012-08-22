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
package net.rim.ejde.internal.ui.launchers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.launching.IFledgeLaunchConstants;
import net.rim.ejde.internal.sourcelookup.RIMSourcePathProvider;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ide.Project;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class ProjectsTab extends AbstractLaunchConfigurationTab implements PropertyChangeListener {
    private AppSelectionUI _appSelectionUI;
    private Set< IProject > _allProjects;
    private AbstractBlackBerryLaunchConfigurationTabGroup _tabGroup;

    /**
     * Constructor
     */
    public ProjectsTab( AbstractBlackBerryLaunchConfigurationTabGroup tabGroup ) {
        _tabGroup = tabGroup;
    }

    /*
     * Returns false if there is no project stored in launch configuration; it disables Run/Debug button.
     *
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse .debug.core.ILaunchConfiguration)
     */
    @Override
    public boolean isValid( ILaunchConfiguration configuration ) {
        return LaunchUtils.getProjectsFromConfiguration( configuration ).size() > 0;
    }

    /**
     * Creates the UI for this tab
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl( Composite parent ) {
        Composite mainComposite = new Composite( parent, SWT.NONE );
        GridLayout layout = new GridLayout( 3, false );
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        mainComposite.setLayout( layout );
        GridData layoutData = new GridData( SWT.FILL, SWT.FILL, true, true );
        layoutData.horizontalSpan = 3;

        mainComposite.setLayoutData( layoutData );
        mainComposite.setFont( parent.getFont() );

        _appSelectionUI = new AppSelectionUI();
        _appSelectionUI.createControl( mainComposite, 3, 0 );
        _appSelectionUI.addPropertyChangeListener( this );

        setControl( mainComposite );
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
     */
    public String getName() {
        return Messages.ProjectsTab_Projects;
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
     */
    public Image getImage() {
        ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons"
                + File.separator + "activated.gif" );
        final Image image = imageDescriptor.createImage();
        return image;
    }

    /**
     * Inits current UI selection to data given by configuration parameter
     *
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
     */
    public void initializeFrom( ILaunchConfiguration configuration ) {
        _allProjects = ProjectUtils.getAllBBProjectsAndDependencies();
        _appSelectionUI.initializeFrom( _allProjects, LaunchUtils.getProjectsFromConfiguration( configuration ) );
        validate();
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void performApply( ILaunchConfigurationWorkingCopy configuration ) {
        configuration.setAttribute( IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
                RIMSourcePathProvider.RIM_SOURCEPATH_PROVIDER_ID );
        configuration.setAttribute( IFledgeLaunchConstants.ATTR_DEPLOYED_PROJECTS, getSelectedProjectNames() );
    }

    private List< String > getSelectedProjectNames() {
        List< String > checkedProjecs = _appSelectionUI.getCheckedProjectNames();
        return checkedProjecs;
    }

    /**
     * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#deactivated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void deactivated( ILaunchConfigurationWorkingCopy workingCopy ) {
        // do nothing
    }

    /**
     * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
     */
    public void setDefaults( ILaunchConfigurationWorkingCopy configuration ) {
    }

    /**
     * Sets the layout of the given <code>button</code>.
     *
     * @param button
     */
    public static void setDialogConfirmButtonLayoutData( Button button ) {
        GridData data = new GridData( GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_BEGINNING );
        Point minSize = button.computeSize( SWT.DEFAULT, SWT.DEFAULT, true );
        data.widthHint = Math.max( IDialogConstants.BUTTON_WIDTH, minSize.x );
        button.setLayoutData( data );
    }

    protected static class ProjectTableLabelProvider extends LabelProvider implements ITableLabelProvider {
        public Image getColumnImage( Object element, int columnIndex ) {
            return null;
        }

        public String getColumnText( Object element, int columnIndex ) {
            if( null != element && element instanceof Project && columnIndex >= 0 ) {
                Project project = (Project) element;
                switch( columnIndex ) {
                    case 0:
                        return project.getDisplayName();
                    case 1:
                        return project.getFile().toString();
                    default:
                        return ""; //$NON-NLS-1$
                }
            }
            return ""; //$NON-NLS-1$
        }
    }

    public void propertyChange( PropertyChangeEvent evt ) {
        validate();
        updateLaunchConfigurationDialog();
    }

    private void validate() {
        if( hasProjectSelected() == false ) {
            setErrorMessage( Messages.ProjectsTab_noProjectSelected );
            return;
        }
        if( hasDependencyProblem() ) {
            return;
        }
        if( _tabGroup instanceof RunningFledgeLaunchConfigurationTabGroup ) {
            setMessage( Messages.RunningFledgeLaunchConfiguration_projectNotDeploy );
        } else {
            setMessage( null );
        }
        setErrorMessage( null );
    }

    private boolean hasProjectSelected() {
        return _appSelectionUI.getCheckedProjects().size() > 0;
    }

    private boolean hasDependencyProblem() {
        List< IProject > checkedProjects = _appSelectionUI.getCheckedProjects();
        for( IProject project : _allProjects ) {
            if( _appSelectionUI.isChecked( project ) ) {
                continue;
            }
            IProject masterProject = ProjectUtils.isDependedByOthers( project, checkedProjects, new HashSet< IProject >() );
            if( masterProject == null ) {
                continue;
            }
            setMessage( NLS.bind( Messages.ProjectsTab_dependencyErrorMsg,
                    new String[] { project.getName(), masterProject.getName() } ) );
            return true;
        }
        return false;
    }

    public Collection< IProject > getSelectedProjects() {
        return _appSelectionUI.getCheckedProjects();
    }
}
