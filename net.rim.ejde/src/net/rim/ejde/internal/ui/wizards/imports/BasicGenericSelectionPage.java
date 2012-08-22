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

import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BlackBerryVMInstallType;
import net.rim.ejde.internal.model.IModelConstants;
import net.rim.ejde.internal.ui.wizards.JRESelectionUI;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ide.Project;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * This class provide the UI which allows users to select projects from a legacy workspace file and import them.
 *
 */
public class BasicGenericSelectionPage extends AbstractImporterPage implements IModelConstants, IProjectImportSelectionUICallback {
    static private final Logger _log = Logger.getLogger( GenericSelectionPage.class );
    private JRESelectionUI _jreSelectionUI;
    private ProjectImportSelectionUI _projectSelectionTableGroup;
    private final Set< String > _existingProjects;
    protected boolean _generalImport;

    /**
     * Constructor
     *
     */
    public BasicGenericSelectionPage( boolean generalImport ) {
        _generalImport = generalImport;
        if( generalImport ) {
            setTitle( Messages.GenericSelectionPage_IMPORT_PAGE_TITLE );
            setDescription( Messages.GenericSelectionPage_IMPORT_PAGE_DESCRIPTION );
        } else {
            setTitle( Messages.GenericSelectionPage_SAMPLES_IMPORT_PAGE_TITLE );
            setDescription( Messages.GenericSelectionPage_SAMPLES_IMPORT_PAGE_DESCRIPTION );
        }
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] eclipseProjects = workspace.getRoot().getProjects();
        _existingProjects = new HashSet< String >( eclipseProjects.length );
        if( 0 < eclipseProjects.length ) {
            for( int i = 0; i < eclipseProjects.length; i++ ) {
                _existingProjects.add( eclipseProjects[ i ].getName() );
            }
        }
    }

    public ProjectImportSelectionUI getProjectSelectionUI() {
        return _projectSelectionTableGroup;
    }

    public String convertToString( String[] projectNames ) {
        StringBuffer formattedString = new StringBuffer();

        for( String projectName : projectNames ) {
            formattedString.append( "\n-" + projectName ); //$NON-NLS-1$
        }

        return formattedString.toString();
    }

    public IPath getREPath() {
        return _jreSelectionUI.getJREContainerPath();
    }

    @Override
    public boolean isPageComplete() {
        if( VMUtils.getDefaultBBVM() == null ) {
            setMessage( net.rim.ejde.internal.util.Messages.NewBlackBerryProjectWizardPageOne_Message_noBBJREInstalled,
                    IMessageProvider.ERROR );
            return false;
        }
        if( !isBBVMSelected( _jreSelectionUI ) ) {
            setMessage( net.rim.ejde.internal.util.Messages.NewBlackBerryProjectWizardPageOne_Message_noJRESelected,
                    IMessageProvider.ERROR );
            return false;
        }
        IPath currentWorkspace = _projectSelectionTableGroup.getCurrentWorkspace();
        if( currentWorkspace == null || currentWorkspace.isEmpty() ) {
            setMessage( Messages.GenericSelectionPage_NO_WORKSPACE_SELECTED_MSG, IMessageProvider.INFORMATION );
            return false;
        }
        if( !_projectSelectionTableGroup.isValidWorkspaceFile() ) {
            setMessage( NLS.bind( Messages.GenericSelectionPage_FILE_NOT_EXIST_MSG, currentWorkspace.toOSString() ),
                    IMessageProvider.INFORMATION );
            return false;
        }
        if( _projectSelectionTableGroup.getAllProjectNumber() == 0 ) {
            setMessage( Messages.GenericSelectionPage_NO_WORKSPACE_LOADED_MSG, IMessageProvider.INFORMATION );
            return false;
        }
        if( ( _projectSelectionTableGroup.getSelectedProjectsNumber() - _projectSelectionTableGroup.getExistingProjectsNumber() ) == 0 ) {
            setMessage( Messages.GenericSelectionPage_NO_PROJECT_SELECTED_ERROR_MSG, IMessageProvider.INFORMATION );
            return false;
        }
        String message = _projectSelectionTableGroup.hasDependencyProblem();
        if( !StringUtils.isBlank( message ) ) {
            setMessage( message, IMessageProvider.WARNING );
            return true;
        }
        if( _projectSelectionTableGroup.getExistingProjectsNumber() > 0 ) {
            setMessage( Messages.GenericSelectionPage_SOME_PROJECTS_EXIST_MSG, IMessageProvider.WARNING );
            return true;
        }
        setMessage( IConstants.EMPTY_STRING );
        return true;
    }

    protected boolean isBBVMSelected( JRESelectionUI jreSelectionUI ) {
        IVMInstall vm = jreSelectionUI.getSelectedJVM();
        return ( vm != null ) && ( BlackBerryVMInstallType.VM_ID.equals( vm.getVMInstallType().getId() ) );
    }

    protected void enableProjectSelectionUI( boolean enabled ) {
        _projectSelectionTableGroup.enableUI( enabled );
        if( enabled ) {
            setMessage( IConstants.EMPTY_STRING );
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see net.rim.eide.internal.ui.wizards.AbstractImporterPage#buildUIContainer (org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected void buildUI( Composite parent ) {
        Composite composite = new Composite( parent, SWT.NONE );
        GridLayout gridLayout = new GridLayout( 1, false );
        composite.setLayout( gridLayout );
        GridData gridData = new GridData( GridData.FILL_BOTH );
        composite.setLayoutData( gridData );
        // BB RE selection group
        createBBREGroup( composite );
        _projectSelectionTableGroup = new ProjectImportSelectionUI( composite, this, _generalImport );
        _projectSelectionTableGroup.creatContent();
        enableProjectSelectionUI( VMUtils.getDefaultBBVM() != null && isBBVMSelected( _jreSelectionUI ) );
        setPageComplete( false );
        _projectSelectionTableGroup.setCurrentVM( _jreSelectionUI.getSelectedJVM() );
        if( !_generalImport ) {
            loadSameples();
        }
        composite.setFocus();
    }

    private void loadSameples() {
        IVMInstall vm = getSelectedJVM();
        if( vm == null ) {
            return;
        }
        IPath jdwFilePath = new Path( vm.getInstallLocation().getPath() );
        jdwFilePath = jdwFilePath.append( ProjectImportSelectionUI.SAMPLE_JDW_RELATIVE_PATH );
        _projectSelectionTableGroup.setCurrentVM( vm );
        _projectSelectionTableGroup.loadWorkspace( jdwFilePath );
    }

    private void createBBREGroup( Composite comp ) {
        _jreSelectionUI = new JRESelectionUI( this );
        Control control = _jreSelectionUI.createControl( comp );
        control.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        _jreSelectionUI.addObserver( new REObserver() );
    }

    private final class REObserver implements Observer {

        /*
         * (non-Javadoc)
         *
         * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
         */
        @Override
        public void update( Observable arg0, Object arg1 ) {
            if( !( arg0 instanceof JRESelectionUI ) ) {
                return;
            }
            JRESelectionUI ui = (JRESelectionUI) arg0;
            _log.debug( "JRE selection is changed to " + ui.getSelectedJVM().getId() );
            if( !_generalImport ) {
                loadSameples();
            }
            _projectSelectionTableGroup.setCurrentVM( ui.getSelectedJVM() );
            enableProjectSelectionUI( VMUtils.getDefaultBBVM() != null && isBBVMSelected( ui ) );
            setPageComplete( isPageComplete() );
        }
    }

    public Set< Project > getSelectedProjects() {
        return _projectSelectionTableGroup.getSelectedProjects();
    }

    public int getImportType() {
        return _projectSelectionTableGroup.getImportType();
    }

    @Override
    public IVMInstall getSelectedJVM() {
        return _jreSelectionUI.getSelectedJVM();
    }

    public void setComplete( boolean complete ) {
        setPageComplete( complete );
    }
}
