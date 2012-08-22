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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.imports.LegacyImportHelper;
import net.rim.ejde.internal.util.ImportUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ide.OSUtils;
import net.rim.ide.Project;
import net.rim.ide.RIA;
import net.rim.ide.Workspace;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * This is a common UI group which allow user to choose projects from a jdw file or the Samples workspace of the selected
 * BlackBerry JRE. This UI group communicates with the class which uses it through the {@link IProjectImportSelectionUICallback}.
 *
 *
 */
public class ProjectImportSelectionUI {
    static private final Logger _log = Logger.getLogger( ProjectImportSelectionUI.class );
    static public final String SAMPLE_JDW_RELATIVE_PATH = "samples/samples.jdw";
    private final static int BUTTON_DEFAULT_WIDTH = 70;
    private IPath _currentWorkspace;
    private Button _browseButton;
    private Text _importPathField;
    private Button _copyButton;
    private Button _selectDependencyButton;
    public CheckboxTableViewer _tableViewer;
    private Button _selectAllButton;
    private Button _deselectAllButton;
    Composite _parent;
    Set< String > _existingProjects;
    IProjectImportSelectionUICallback _callback;
    private boolean _allowBrowseJDW;
    private IVMInstall _currentVM;
    private boolean _isValidWorkspaceFile = false;
    private List< IImportTypeChangeListener > listeners;

    public ProjectImportSelectionUI( Composite parent, IProjectImportSelectionUICallback callback, boolean allowBrowseJDW ) {
        _parent = parent;
        _callback = callback;
        _allowBrowseJDW = allowBrowseJDW;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IProject[] eclipseProjects = workspace.getRoot().getProjects();
        _existingProjects = new HashSet< String >( eclipseProjects.length );
        if( 0 < eclipseProjects.length ) {
            for( int i = 0; i < eclipseProjects.length; i++ ) {
                _existingProjects.add( eclipseProjects[ i ].getName() );
            }
        }
        listeners = new ArrayList< IImportTypeChangeListener >();
    }

    public void setCurrentVM( IVMInstall vm ) {
        _currentVM = vm;
    }

    /**
     * Checks if all dependency projects of the projects selected on the UI are also selected.
     *
     * @return
     */
    public String hasDependencyProblem() {
        Table table = _tableViewer.getTable();
        TableItem[] items = table.getItems();
        Object[] checkedProjects = _tableViewer.getCheckedElements();
        for( TableItem item : items ) {
            Project project = (Project) item.getData();
            if( _tableViewer.getChecked( project ) ) {
                continue;
            }
            Project masterProject = isDependedByOthers( project, checkedProjects, new HashSet< Project >() );
            if( masterProject == null ) {
                continue;
            }
            return NLS.bind( Messages.GenericSelectionPage_DEPENDENCY_ERROR_MSG, new String[] { project.getDisplayName(),
                    masterProject.getDisplayName() } );
        }
        return IConstants.EMPTY_STRING;
    }

    /**
     * Gets import type.
     *
     * @return
     */
    public int getImportType() {
        return _copyButton.getSelection() ? LegacyImportHelper.COPY_IMPORT : LegacyImportHelper.LINK_IMPORT;
    }

    /**
     * Gets the number of selected projects.
     *
     * @return
     */
    public int getSelectedProjectsNumber() {
        return _tableViewer.getCheckedElements().length;
    }

    /**
     * Gets the number of exist projects.
     *
     * @return
     */
    public int getExistingProjectsNumber() {
        return _tableViewer.getGrayedElements().length;
    }

    /**
     * Gets the number of all projects.
     *
     * @return
     */
    public int getAllProjectNumber() {
        return _tableViewer.getTable().getItemCount();
    }

    /**
     * Returns a value indicating if the selected workspace file is valid
     *
     * @return
     */
    public boolean isValidWorkspaceFile() {
        return _isValidWorkspaceFile;
    }

    /**
     * Gets the current workspace path.
     *
     * @return
     */
    public IPath getCurrentWorkspace() {
        return _currentWorkspace;
    }

    private boolean projectExists( String projectName ) {
        if( null == _existingProjects || _existingProjects.isEmpty() )
            return false;
        return _existingProjects.contains( projectName );
    }

    public void loadWorkspace( IPath workspacePath ) {
        if( _currentVM == null ) {
            return;
        }

        if( workspacePath == null || workspacePath.isEmpty() ) {
            return;
        }

        if( workspacePath.equals( _currentWorkspace ) ) {
            return;
        }

        _currentWorkspace = workspacePath;
        File workspaceFile = workspacePath.toFile();
        if( !workspaceFile.exists() || !workspaceFile.isFile() ) {
            _isValidWorkspaceFile = false;
            _tableViewer.setInput( null );
            _callback.setComplete( false );
            return;
        }

        _isValidWorkspaceFile = true;
        LoadLegacyWorkspaceTask task = new LoadLegacyWorkspaceTask( workspaceFile );
        ProgressMonitorDialog monitorDialog = new ProgressMonitorDialog( ContextManager.getActiveWorkbenchShell() );
        try {
            monitorDialog.run( false, false, task );
        } catch( Throwable e ) {
            _log.debug( e.getMessage(), e );
            _callback.setComplete( false );
        }
        Workspace workspace = task.getWorkspace();
        List< Project > nonExistingProjects = new ArrayList< Project >();
        List< Project > allProjects = new ArrayList< Project >();
        int numProjects = workspace.getNumProjects();
        for( int i = 0; i < numProjects; i++ ) {
            Project project = workspace.getProject( i );
            String projectName = project.getDisplayName();
            // do not count AEPs
            if( project.getType() != Project.CLDC_APPLICATION_ENTRY && project.getType() != Project.MIDLET_ENTRY ) {
                if( !projectExists( projectName ) ) { // checks if the project
                    // already exists in the previous workspace
                    nonExistingProjects.add( project );
                }
                allProjects.add( project );
            }
        }
        Collections.sort( allProjects, ProjectNameComparator );
        _tableViewer.setInput( allProjects );
        _tableViewer.setCheckedElements( nonExistingProjects.toArray( new Project[ nonExistingProjects.size() ] ) );
        checkExistingProjects();
        if( nonExistingProjects.size() == 0 ) {
            _callback.setComplete( false );
        } else {
            _callback.setComplete( true );
        }
    }

    private void checkExistingProjects() {
        TableItem[] items = _tableViewer.getTable().getItems();
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        for( TableItem item : items ) {
            Project project = (Project) item.getData();
            if( workspaceRoot.getProject( project.getDisplayName() ).exists() ) {
                // existing projects are mark as grayed and checked
                item.setForeground( Display.getCurrent().getSystemColor( SWT.COLOR_GRAY ) );
                item.setGrayed( true );
                item.setChecked( true );
            }
        }
    }

    /**
     * Gets selected projects.
     *
     * @return
     */
    public Set< Project > getSelectedProjects() {
        Set< Project > projectSet = new HashSet< Project >();
        TableItem[] items = _tableViewer.getTable().getItems();
        for( TableItem item : items ) {
            if( item.getChecked() && !item.getGrayed() ) {
                projectSet.add( (Project) ( item.getData() ) );
            }
        }
        return projectSet;
    }

    /**
     * Gets a project based on project name
     *
     * @param projectName
     * @return
     */
    public Project getProject( String projectName ) {
        TableItem[] items = _tableViewer.getTable().getItems();
        Project project;
        for( TableItem item : items ) {
            project = (Project) item.getData();
            if( project.getDisplayName().equalsIgnoreCase( projectName ) ) {
                return project;
            }
        }
        return null;
    }

    /**
     * Enable/disable the UI.
     *
     * @param enabled
     */
    public void enableUI( boolean enabled ) {
        if( _browseButton != null ) {
            _browseButton.setEnabled( enabled );
        }
        if( _importPathField != null ) {
            _importPathField.setEnabled( enabled );
        }
        if( _copyButton != null && _allowBrowseJDW ) {
            _copyButton.setEnabled( enabled );
        }
        if( _tableViewer != null ) {
            _tableViewer.getTable().setEnabled( enabled );
        }
        if( _selectAllButton != null ) {
            _selectAllButton.setEnabled( enabled );
        }
        if( _deselectAllButton != null ) {
            _deselectAllButton.setEnabled( enabled );
        }
    }

    /**
     * Checks if the given <code>project</code> is dependent by any checked project in the <code>checkedProjects</code> but is not
     * in the <code>dependentProjects</code>.
     *
     * @param project
     * @param checkedProjects
     * @param dependentProjects
     * @return Project the first project which depends on the given <code>project</code>.
     */
    private Project isDependedByOthers( Project project, Object[] checkedProjects, Set< Project > dependentProjects ) {
        for( Object obj : checkedProjects ) {
            Project checkedProject = (Project) obj;
            if( !dependentProjects.contains( checkedProject ) ) {
                for( int i = 0; i < checkedProject.getNumDependsDirectlyOn(); i++ ) {
                    if( project.equals( checkedProject.getDependsDirectlyOn( i ) ) ) {
                        return checkedProject;
                    }
                }
            }
        }
        return null;
    }

    public void creatContent() {
        // workspace location entry field
        Composite pathComp = new Composite( _parent, SWT.NONE );
        pathComp.setLayout( new GridLayout( 3, false ) );
        pathComp.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        if( _allowBrowseJDW ) {
            String importLocationLabel = Messages.GenericSelectionPage_WORKSPACE_LABEL;
            Label projectContentsLabel = new Label( pathComp, SWT.NONE );
            projectContentsLabel.setText( importLocationLabel ); //$NON-NLS-1$*/
            projectContentsLabel.setLayoutData( new GridData( SWT.BEGINNING ) );
            // jdw path text
            _importPathField = new Text( pathComp, SWT.BORDER );
            _importPathField.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
            _importPathField.setText( "" ); //$NON-NLS-1$
            _importPathField.addKeyListener( new KeyAdapter() {
                @Override
                public void keyPressed( KeyEvent e ) {
                    if( ( e.keyCode == SWT.CR ) || ( e.keyCode == SWT.KEYPAD_CR ) ) {
                        _log.debug( "\"Entry\" was pressed" );
                        String workspaceFileToImport = _importPathField.getText();
                        loadWorkspace( new Path( workspaceFileToImport ) );
                    }
                }
            } );
            _importPathField.addFocusListener( new FocusAdapter() {
                @Override
                public void focusLost( FocusEvent e ) {
                    String workspaceFilePath = _importPathField.getText();
                    loadWorkspace( new Path( workspaceFilePath ) );
                }
            } );
            // browse button
            _browseButton = new Button( pathComp, SWT.PUSH );
            GridData data = new GridData( GridData.END );
            data.widthHint = BUTTON_DEFAULT_WIDTH;
            _browseButton.setLayoutData( data );
            _browseButton.setText( Messages.IConstants_BROWSE_BUTTON_TITLE ); //$NON-NLS-1$
            _browseButton.addSelectionListener( new SelectionAdapter() {
                public void widgetSelected( SelectionEvent event ) {
                    FileDialog dialog = new FileDialog( _importPathField.getShell() );
                    String[] filters = new String[] { "*.jdw" };
                    String[] filterNames = new String[] { Messages.BLACKBERRY_WORKSPACE_FILTER_NAME };
                    dialog.setFilterExtensions( filters );
                    dialog.setFilterNames( filterNames );
                    String workspaceFileToImport = dialog.open();
                    if( null != workspaceFileToImport ) {
                        _importPathField.setText( workspaceFileToImport );
                        loadWorkspace( new Path( workspaceFileToImport ) );
                    }
                }
            } );
        }
        Composite comp = new Composite( _parent, SWT.NONE );
        GridLayout gridLayout = new GridLayout( 3, false );
        comp.setLayout( gridLayout );
        GridData gd = new GridData( GridData.FILL_BOTH );
        gd.heightHint = 250;
        comp.setLayoutData( gd );
        // projects label
        Label projectsLabel = new Label( comp, SWT.NONE );
        projectsLabel.setLayoutData( new GridData( SWT.BEGINNING, SWT.CENTER, true, false, 3, 1 ) );
        projectsLabel.setText( Messages.GenericSelectionPage_PROJECT_TABLE_TITLE );
        // projects table
        _tableViewer = CheckboxTableViewer.newCheckList( comp, SWT.SINGLE | SWT.FULL_SELECTION | SWT.CHECK | SWT.BORDER );
        _tableViewer.setContentProvider( new ArrayContentProvider() );
        _tableViewer.setLabelProvider( new ProjectTableLabelProvider() );
        _tableViewer.addCheckStateListener( new CheckStateListenerImpl() );
        Table table = _tableViewer.getTable();
        table.addKeyListener( new KeyListenerImpl() );
        table.setHeaderVisible( true );
        table.setLinesVisible( true );
        table.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, 2, 2 ) );
        TableColumn projectIconColumn, projectNameColumn;
        projectIconColumn = new TableColumn( table, SWT.NONE );
        projectIconColumn.setText( "Project Icon" ); //$NON-NLS-1$
        projectIconColumn.setWidth( 80 );
        projectNameColumn = new TableColumn( table, SWT.NONE );
        projectNameColumn.setText( "Project Name" ); //$NON-NLS-1$
        projectNameColumn.setWidth( 450 );
        // select / de-select project buttons
        _selectAllButton = new Button( comp, SWT.PUSH );
        _selectAllButton.setText( "Select All" ); //$NON-NLS-1$
        GridData gridData = new GridData( GridData.HORIZONTAL_ALIGN_FILL );
        gridData.widthHint = BUTTON_DEFAULT_WIDTH;
        _selectAllButton.setLayoutData( gridData );
        _deselectAllButton = new Button( comp, SWT.PUSH );
        //_deselectAllButton.setText( _resources.getString( "Wizard.GenericSelectionPage.DeselectAll" ) ); //$NON-NLS-1$
        _deselectAllButton.setText( "Deselect All" ); //$NON-NLS-1$
        GridData gridData2 = new GridData( SWT.FILL, SWT.TOP, false, false );
        gridData2.widthHint = BUTTON_DEFAULT_WIDTH;
        _deselectAllButton.setLayoutData( gridData2 );
        _selectAllButton.addSelectionListener( new SelectionAdapter() {
            /*
             * (non-Javadoc)
             *
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org .eclipse .swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected( SelectionEvent e ) {
                Table table = _tableViewer.getTable();
                for( TableItem item : table.getItems() ) {
                    if( !item.getGrayed() ) {
                        item.setChecked( true );
                    }
                }
                table.redraw();
                _callback.setMessage( IConstants.EMPTY_STRING, IMessageProvider.NONE );
                _callback.setComplete( table.getItems().length > 0 ); // donot
            }
        } );
        _deselectAllButton.addSelectionListener( new SelectionAdapter() {
            /*
             * (non-Javadoc)
             *
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org .eclipse .swt.events.SelectionEvent)
             */
            @Override
            public void widgetSelected( SelectionEvent e ) {
                Table table = _tableViewer.getTable();
                for( TableItem item : table.getItems() ) {
                    if( !item.getGrayed() ) {
                        item.setChecked( false );
                    }
                }
                _callback.setMessage( Messages.GenericSelectionPage_NO_PROJECT_SELECTED_ERROR_MSG, IMessageProvider.ERROR );
                _callback.setComplete( false );
            }
        } );
        // the copy project checkbox
        _copyButton = new Button( comp, SWT.CHECK );
        _copyButton.setLayoutData( new GridData( SWT.BEGINNING, SWT.CENTER, true, false, 3, 1 ) );
        _copyButton.setText( Messages.GenericSelectionPage_COPY_MODEL_LABEL );
        // by default we suggest users to use copy model
        _copyButton.setSelection( true );
        // if it is sample import wizard, we only allow copy import
        _copyButton.setEnabled( _allowBrowseJDW );
        _copyButton.addSelectionListener( new SelectionListener() {

            @Override
            public void widgetDefaultSelected( SelectionEvent e ) {
                // TODO Auto-generated method stub

            }

            @Override
            public void widgetSelected( SelectionEvent e ) {
                fireImportTypeChangeEvent( e );

            }

        } );
        // the select dependency project checkbox
        _selectDependencyButton = new Button( comp, SWT.CHECK );
        _selectDependencyButton.setText( "Automatically select dependent projects" );
        // by default we suggest users to use copy model
        _selectDependencyButton.setSelection( true );
    }

    public void addImportTypeChangeListener( IImportTypeChangeListener listener ) {
        listeners.add( listener );
    }

    private void fireImportTypeChangeEvent( SelectionEvent event ) {
        for( IImportTypeChangeListener listener : listeners ) {
            listener.ImportTypeChanged( event );
        }
    }

    private class KeyListenerImpl extends KeyAdapter {
        long startTime = 0;
        char[] inputs;
        int index = 0;

        /**
         * We listen to the keys
         */
        public void keyPressed( KeyEvent e ) {
            if( !( ( e.character >= 'a' && e.character <= 'z' ) || ( e.character >= 'A' && e.character <= 'Z' ) ) ) {
                return;
            }
            if( ( startTime == 0 ) || ( System.currentTimeMillis() - startTime ) > 1000 ) {
                startTime = System.currentTimeMillis();
                inputs = new char[ 10 ];
                index = 0;
            }
            inputs[ index++ ] = e.character;
            String inputString = new String( inputs );
            inputString = inputString.trim().toLowerCase();
            Table table = _tableViewer.getTable();
            TableItem[] items = table.getItems();
            for( int i = 0; i < items.length; i++ ) {
                Project project = (Project) items[ i ].getData();
                String projectName = project.getDisplayName().toLowerCase();
                if( projectName.startsWith( inputString ) ) {
                    _tableViewer.setSelection( new StructuredSelection( new Project[] { project } ), true );
                    break;
                }
            }
        }
    }

    private class CheckStateListenerImpl implements ICheckStateListener {
        public void checkStateChanged( CheckStateChangedEvent event ) {
            Project project = (Project) event.getElement();
            if( event.getChecked() ) {
                _log.debug( "Checked : [" + project.getDisplayName() + "]" ); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                _log.debug( "Unchecked : [" + project.getDisplayName() + "]" ); //$NON-NLS-1$ //$NON-NLS-2$
            }
            CheckboxTableViewer view = (CheckboxTableViewer) event.getSource();
            if( view.getGrayed( project ) ) {
                // if the checked item was grayed, we always mark it as checked
                view.setChecked( project, true );
                return;
            }
            if( _selectDependencyButton.getSelection() ) {
                checkProject( view, project, event.getChecked() );
            }
            _callback.setComplete( true );
        }

        private void checkProject( CheckboxTableViewer view, Project project, boolean checked ) {
            Set< Project > dependentProjects = null;
            try {
                dependentProjects = ImportUtils.getAllReferencedProjects( project );
            } catch( CoreException e ) {
                _log.error( e.getMessage(), e );
            }
            if( dependentProjects == null || dependentProjects.size() == 0 ) {
                return;
            }
            Table table = view.getTable();
            TableItem[] items = table.getItems();
            Object[] checkedProjects = view.getCheckedElements();
            for( TableItem item : items ) {
                Project data = (Project) item.getData();
                if( dependentProjects.contains( data ) ) {
                    if( checked ) {
                        if( !item.getGrayed() && !item.getChecked() ) {
                            // if the dependent project is not grayed, gray
                            // it
                            item.setChecked( true );
                        }
                    } else {
                        if( !item.getGrayed() && item.getChecked()
                                && ( isDependedByOthers( data, checkedProjects, dependentProjects ) == null ) ) {
                            // if the dependent project is not an existing project, it is checked and it is not depended by
                            // another project, un-gray it item.setGrayed(false);
                            item.setChecked( false );
                        }
                    }
                }
            }
        }
    }

    protected class ProjectTableLabelProvider extends LabelProvider implements ITableLabelProvider {
        public Image getColumnImage( Object element, int columnIndex ) {
            if( null != element && element instanceof Project && columnIndex >= 0 ) {
                Project project = (Project) element;
                switch( columnIndex ) {
                    case 0: {
                        Vector< File > icons = project.getIcons();
                        Image icon = null;
                        try {
                            if( null != icons && !icons.isEmpty() ) {
                                File iconFile = new File( OSUtils.replaceFileSeperator( icons.firstElement().getAbsolutePath() ) );
                                if( !iconFile.exists() ) {
                                    return null;
                                }
                                ImageData imageData = new ImageData( new BufferedInputStream( new FileInputStream( iconFile ) ) );
                                imageData = imageData.scaledTo( _tableViewer.getTable().getItemHeight(), _tableViewer.getTable()
                                        .getItemHeight() );
                                icon = new Image( Display.getCurrent(), imageData );
                            }
                        } catch( FileNotFoundException e ) {
                            _log.debug( "", e ); //$NON-NLS-1$
                        }
                        return icon;
                    }
                    default:
                        return null; //$NON-NLS-1$
                }
            }
            return null;
        }

        public String getColumnText( Object element, int columnIndex ) {
            if( null != element && element instanceof Project && columnIndex >= 0 ) {
                Project project = (Project) element;
                switch( columnIndex ) {
                    case 1:
                        return project.getDisplayName();
                    default:
                        return ""; //$NON-NLS-1$
                }
            }
            return ""; //$NON-NLS-1$
        }
    }

    static Comparator< Project > ProjectNameComparator = new Comparator< Project >() {
        public int compare( Project project1, Project project2 ) {
            return project1.getDisplayName().toUpperCase().compareTo( project2.getDisplayName().toUpperCase() );
        }
    };

    protected class LoadLegacyWorkspaceTask implements IRunnableWithProgress {
        File workspaceFileToImport;
        Workspace workspace;

        // Workspace workspace;
        // List< Project > notExistingProjects;

        public LoadLegacyWorkspaceTask( File workspace ) {
            workspaceFileToImport = workspace;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse .core.runtime.IProgressMonitor)
         */
        @Override
        public void run( IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException {
            if( workspaceFileToImport.exists() && workspaceFileToImport.isFile() ) {
                monitor.beginTask( "Loading workspace...", 10 );

                try {
                    RIA ria = ContextManager.PLUGIN.getRIA( _currentVM.getInstallLocation().getPath() );
                    workspace = createWorkspace( workspaceFileToImport, ria );
                } catch( Throwable e ) {
                    _log.debug( e.getMessage(), e );
                    return;
                }
                monitor.worked( 8 );
                if( workspace == null ) {
                    throw new InvocationTargetException( new CoreException(
                            StatusFactory.createErrorStatus( "Could not load the workspace." ) ) );
                }
            }
        }

        private Workspace createWorkspace( File jdw, RIA ria ) {
            Workspace workspace = null;

            try {
                workspace = new Workspace( jdw, ria );
                workspace.save();
            } catch( Throwable e ) {
                _log.debug( "", e );
            }

            return workspace;
        }

        public Workspace getWorkspace() {
            return workspace;
        }
    }

    static protected interface IImportTypeChangeListener {
        public void ImportTypeChanged( SelectionEvent event );
    }
}
