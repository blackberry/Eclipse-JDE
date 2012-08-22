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
package net.rim.ejde.internal.ui.editors.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.Action;
import net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.FilePathOperationSelectionListener;
import net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.TableSelectionListener;
import net.rim.ejde.internal.ui.editors.model.factories.ControlFactory;
import net.rim.ejde.internal.ui.editors.model.factories.LayoutFactory;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.validation.BBDiagnostic;
import net.rim.ejde.internal.validation.BBPropertiesValidator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * This class creates the ALX files section used in the project properties editor.
 *
 * @author jkeshavarzi
 */
public class ALXFilesSection extends AbstractSection {
    private TableViewer _alxFilesViewer;
    private Map< Action, Button > _actionButtons;
    private Composite _client;
    private String _lastSelectedPath = null;

    /**
     * This class creates the ALX files section used in the project properties editor.
     *
     * @param page
     *            the page
     * @param parent
     *            the parent
     * @param toolkit
     *            the toolkit
     * @param style
     *            the style
     */
    public ALXFilesSection( BlackBerryProjectPropertiesPage page, Composite parent, FormToolkit toolkit, int style ) {
        super( page, parent, page.getManagedForm().getToolkit(), ( style | Section.DESCRIPTION | ExpandableComposite.TITLE_BAR ) );
        createFormContent( getSection(), toolkit );
    }

    /**
     * Creates the form content.
     *
     * @param section
     *            the section
     * @param toolkit
     *            the toolkit
     */
    protected void createFormContent( Section section, FormToolkit toolkit ) {
        preBuild();

        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, false );
        gd.minimumWidth = 250;
        section.setLayout( LayoutFactory.createClearGridLayout( false, 1 ) );
        section.setLayoutData( gd );

        section.setDescription( Messages.ALXFilesSection_Description );
        setClient( toolkit.createComposite( section ) );
        getClient().setLayout( LayoutFactory.createSectionGridLayout( false, 3 ) );
        section.setClient( getClient() );

        build( getClient(), toolkit );

        postBuild( getClient(), toolkit );
    }

    private void preBuild() {
        getSection().setText( Messages.ALXFilesSection_Title );
    }

    private void build( final Composite body, FormToolkit toolkit ) {
        Map< Action, SelectionListener > actionListeners = new HashMap< Action, SelectionListener >( Action.values().length );
        actionListeners.put( Action.ADD_FROM_PROJECT, new AddSelectionListener( getParentPage() ) );
        actionListeners.put( Action.ADD_FROM_EXTERNAL, new AddExternalSelectionListener( getParentPage() ) );
        actionListeners.put( Action.EDIT, new EditSelectionListener( getParentPage() ) );
        actionListeners.put( Action.REMOVE, new DeleteSelectionListener( getParentPage() ) );

        setAlxFilesViewer( (TableViewer) ControlFactory.buildTableControl( body, toolkit, null, null,
                Integer.valueOf( SWT.NONE ), Integer.valueOf( 2 ),
                new String[] { "alx file:" }, new ArrayContentProvider(), new AlxTableLabelProvider(), null ) ); //$NON-NLS-1$

        setActionButtons( ControlFactory.buildButtonControls( actionListeners, body, toolkit ) );

        Table table = _alxFilesViewer.getTable();
        table.addSelectionListener( new TableSelectionListener( _actionButtons ) );
        table.setData( BlackBerryProjectPropertiesPage.TABLE_TEXT_INDEX_KEY, Integer.valueOf( 1 ) );

        insertControlValuesFromModel( getParentPage().getBlackBerryProject().getProperties() );
    }

    private void postBuild( Composite body, FormToolkit toolkit ) {
        toolkit.paintBordersFor( body );
    }

    private List< IPath > getInput() {
        List< IPath > alxList = new ArrayList< IPath >();
        Object input = _alxFilesViewer.getInput();
        if( input instanceof List ) {
            alxList = (List< IPath >) input;
            return alxList;
        } else {
            return new ArrayList< IPath >( 0 );
        }
    }

    @Override
    public void commit( boolean onSave ) {
        List< IPath > alxFiles = getInput();
        IPath alxFilePath;
        for( int i = 0; i < alxFiles.size(); i++ ) {
            alxFilePath = alxFiles.get( i );
            alxFilePath = getEditor().linkExternalFile( alxFilePath );
            alxFiles.remove( i );
            alxFiles.add( i, alxFilePath );
        }
        _alxFilesViewer.setInput( alxFiles );
        setAlxFilsToModel( alxFiles );
        super.commit( onSave );
    }

    private void setAlxFilsToModel( List< IPath > alxFiles ) {
        String[] alxFileStrings = new String[ alxFiles.size() ];
        for( int i = 0; i < alxFiles.size(); i++ ) {
            alxFileStrings[ i ] = alxFiles.get( i ).toOSString();
        }
        BlackBerryProperties properties = getProjectPropertiesPage().getBlackBerryProject().getProperties();
        properties._packaging.setAlxFiles( alxFileStrings );
    }

    private List< IPath > getAlxFilesFromModel() {
        BlackBerryProperties properties = getProjectPropertiesPage().getBlackBerryProject().getProperties();
        String[] alxFileStrings = properties._packaging.getAlxFiles();
        List< IPath > input = new ArrayList< IPath >();
        for( int i = 0; i < alxFileStrings.length; i++ ) {
            input.add( new Path( alxFileStrings[ i ] ) );
        }
        return input;
    }

    private void validateALXFile( IPath filePath ) {
        BBDiagnostic diag = BBPropertiesValidator.validateFileExists( getProjectPropertiesPage().getBlackBerryProject()
                .getProject(), filePath.toFile(), false );

        if( diag.getSeverity() == Diagnostic.ERROR ) {
            if( diag.getSeverity() == Diagnostic.ERROR ) {
                getProjectPropertiesPage().createEditorErrorMarker( filePath.toString(), diag.getMessage(),
                        _alxFilesViewer.getTable() );
            } else {
                getProjectPropertiesPage().removeEditorErrorMarker( filePath.toString(), _alxFilesViewer.getTable() );
            }
        }
    }

    /**
     * Update the controls within this section with values from the given properties object.
     *
     * @param properties
     *            the properties
     */
    public void insertControlValuesFromModel( BlackBerryProperties properties ) {
        Boolean generateALXFile = properties._packaging.getGenerateALXFile();
        if( generateALXFile != null ) {
            properties._packaging.setGenerateALXFile( generateALXFile );
        }
        _alxFilesViewer.setInput( getAlxFilesFromModel() );
    }

    public void validateAlxFiles() {
        String alxFiles[] = getAlxFiles();
        for( String alxFile : alxFiles ) {
            validateALXFile( new Path( alxFile ) );
        }
    }

    String openFileDialog( Shell shell, IPath filePath ) {
        FileDialog dialog = new FileDialog( shell, SWT.OPEN );
        String filterName[] = new String[] { Messages.ALXFilesSection_Add_Dialog_Filter_Name };
        String filterExtension[] = new String[] { "*.alx" }; //$NON-NLS-1$
        String filterPath = "/"; //$NON-NLS-1$
        String fileName = null;
        String platform = SWT.getPlatform();

        if( platform.equals( "win32" ) || platform.equals( "wpf" ) ) { //$NON-NLS-1$ //$NON-NLS-2$
            if( filePath != null ) {
                filterPath = filePath.removeLastSegments( 1 ).toString();
                fileName = filePath.lastSegment();
            } else {
                if( getLastSelectedPath() != null ) {
                    filterPath = getLastSelectedPath();
                } else {
                    filterPath = getParentPage().getBlackBerryProject().getProject().getLocation().toOSString();
                }
            }

        }

        dialog.setFilterNames( filterName );
        dialog.setFilterExtensions( filterExtension );
        dialog.setFilterPath( filterPath );
        dialog.setFileName( fileName );

        return dialog.open();
    }

    /**
     * Gets the alx files.
     *
     * @return An array of String objects containing the relative paths to each ALX file pulled from the view
     */
    public String[] getAlxFiles() {
        ArrayList< String > list = new ArrayList< String >();
        for( TableItem item : getAlxFilesViewer().getTable().getItems() ) {
            list.add( ( (IPath) item.getData() ).toOSString() );
        }
        return list.toArray( new String[ list.size() ] );
    }

    /**
     * @param client
     *            the client to set
     */
    void setClient( Composite client ) {
        _client = client;
    }

    /**
     * @return the client
     */
    Composite getClient() {
        return _client;
    }

    /**
     * @param lastSelectedPath
     *            the lastSelectedPath to set
     */
    void setLastSelectedPath( String lastSelectedPath ) {
        _lastSelectedPath = lastSelectedPath;
    }

    /**
     * @return the lastSelectedPath
     */
    String getLastSelectedPath() {
        return _lastSelectedPath;
    }

    /**
     * @param parentPage
     *            the parentPage to set
     */
    void setParentPage( BlackBerryProjectPropertiesPage parentPage ) {
        setProjectPropertiesPage( parentPage );
    }

    /**
     * @return the parentPage
     */
    BlackBerryProjectPropertiesPage getParentPage() {
        return getProjectPropertiesPage();
    }

    /**
     * @param actionButtons
     *            the actionButtons to set
     */
    void setActionButtons( Map< Action, Button > actionButtons ) {
        _actionButtons = actionButtons;
    }

    /**
     * @return the actionButtons
     */
    Map< Action, Button > getActionButtons() {
        return _actionButtons;
    }

    /**
     * @param alxFilesViewer
     *            the alxFilesViewer to set
     */
    void setAlxFilesViewer( TableViewer alxFilesViewer ) {
        _alxFilesViewer = alxFilesViewer;
    }

    /**
     * @return the alxFilesViewer
     */
    TableViewer getAlxFilesViewer() {
        return _alxFilesViewer;
    }

    protected void removeTableItems( TableItem item ) {
        getParentPage().removeEditorErrorMarker( item.getText(), getAlxFilesViewer().getTable() );
        getParentPage().removeSelectedTableItem( getAlxFilesViewer(), getActionButtons(), true );
    }

    /**
     * The listener that listens for add events. When the add button is pressed, a file dialog will provided to the user to select
     * an file.
     *
     */
    private class AddExternalSelectionListener extends BlackBerryProjectPropertiesPage.FilePathOperationSelectionListener {
        /**
         * Instantiates a new adds the selection listener.
         *
         * @param page
         *            the page
         */
        public AddExternalSelectionListener( BlackBerryProjectPropertiesPage page ) {
            page.super( getPart() );
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.FilePathOperationSelectionListener#process(org
         * .eclipse.swt.events.SelectionEvent)
         */
        @Override
        protected boolean process( SelectionEvent evt ) {
            Shell shell = getClient().getShell();

            String filePath = openFileDialog( shell, null );
            if( filePath != null ) {
                setLastSelectedPath( new File( filePath ).getParent() );
                IPath relativePath = getEditor().makeRelative( new Path( filePath ) );
                if( isValidFile( relativePath ).isOK() ) {
                    List< IPath > input = getInput();
                    input.add( relativePath );
                    _alxFilesViewer.setInput( input );
                    return getProjectPropertiesPage().addTableItem( getAlxFilesViewer(), null, getActionButtons(), true );
                }
            }
            return false;
        }
    }

    protected IStatus isValidFile( IPath filePath ) {
        TableItem[] items = _alxFilesViewer.getTable().getItems();
        String alxFilePath;
        for( int i = 0; i < items.length; i++ ) {
            alxFilePath = items[ i ].getText();
            if( getAbsolutePath( new Path( alxFilePath ) ).equals( filePath ) ) {
                return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID,
                        Messages.BlackBerryProjectPropertiesPage_Dup_File_Err_Status_Msg );
            }
        }
        String fileName = filePath.lastSegment();
        if( !BlackBerryProjectPropertiesPage.isAlx( fileName ) ) {
            return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, NLS.bind(
                    Messages.BlackBerryProjectPropertiesPage_Add_File_Error_Status_Invalid_File, "alx" ) );
        }
        return Status.OK_STATUS;
    }

    protected IPath getAbsolutePath( IPath path ) {
        IProject project = getEditor().getBlackBerryProject().getProject();
        if( path.isAbsolute() ) {
            return path;
        } else {
            if( path.segment( 0 ).equals( ".." ) ) { //$NON-NLS-1$
                // Do an external file check
                File externalFile = project.getLocation().append( path ).toFile();

                return new Path( externalFile.getAbsolutePath() );
            } else {
                // Do a local proj file check
                IFile iFile = project.getFile( path );
                return iFile.getLocation();
            }
        }
    }

    /**
     * The listener that handles add events. When the add button is pressed the user will be be prompted with a project based
     * dialog to select an alx file
     *
     */
    private class AddSelectionListener extends FilePathOperationSelectionListener {
        BlackBerryProjectPropertiesPage _page;

        protected AddSelectionListener( BlackBerryProjectPropertiesPage page ) {
            page.super( getPart() );
            _page = page;
        }

        @Override
        protected boolean process( SelectionEvent evt ) {
            ILabelProvider lp = new WorkbenchLabelProvider();
            ITreeContentProvider cp = new WorkbenchContentProvider();
            ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog( getClient().getShell(), lp, cp );
            ISelectionStatusValidator validator = new ISelectionStatusValidator() {
                public IStatus validate( Object[] selection ) {
                    IStatus errorStatus = new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, NLS.bind(
                            Messages.BlackBerryProjectPropertiesPage_Add_File_Error_Status_Invalid_File, "alx" ) );
                    IStatus status = null;
                    for( Object element : selection ) {
                        if( element instanceof IFile ) {
                            IFile file = (IFile) element;
                            status = isValidFile( file.getLocation() );
                            if( !status.isOK() ) {
                                return status;
                            }
                        } else {
                            return errorStatus;
                        }
                    }
                    return Status.OK_STATUS;
                }
            };

            dialog.addFilter( new ViewerFilter() {
                @Override
                public boolean select( Viewer viewer, Object parentElement, Object element ) {
                    if( element instanceof IProject ) {
                        return true;
                    }
                    if( element instanceof IFolder ) {
                        IFolder folder = (IFolder) element;
                        return !folder.getName().equals( IConstants.BIN_FOLD_NAME ) && !folder.isDerived();
                    }
                    if( element instanceof IFile ) {
                        return BlackBerryProjectPropertiesPage.isAlx( ( (IFile) element ).getName() );
                    }

                    return false;
                }
            } );
            dialog.setComparator( new ResourceComparator( ResourceComparator.NAME ) );
            dialog.setValidator( validator );
            dialog.setTitle( Messages.BlackBerryProjectPropertiesPage_Add_Icon_Title );
            dialog.setMessage( Messages.BlackBerryProjectPropertiesPage_Add_Icon_Message );
            dialog.setInput( _page.getBlackBerryProject().getProject() );
            dialog.setAllowMultiple( false );
            if( dialog.open() == Window.OK ) {
                Object[] elements = dialog.getResult();
                if( elements != null ) {
                    boolean result = false;
                    List< IPath > input = getInput();
                    for( Object element : elements ) {
                        IResource resource = (IResource) element;
                        input.add( resource.getProjectRelativePath() );
                        result = result
                                || getProjectPropertiesPage().addTableItem( getAlxFilesViewer(), null, getActionButtons(), true );
                    }
                    _alxFilesViewer.setInput( input );
                    return result;
                }
            }
            return false;
        }
    }

    /**
     * The listener that listens for remove events. When the remove button is pressed, the selected ALX file table item will be
     * removed
     *
     * @author jkeshavarzi
     */
    class DeleteSelectionListener extends BlackBerryProjectPropertiesPage.FilePathOperationSelectionListener {

        /**
         * Instantiates a new delete selection listener.
         *
         * @param page
         *            the page
         */
        public DeleteSelectionListener( BlackBerryProjectPropertiesPage page ) {
            page.super( getPart() );
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.FilePathOperationSelectionListener#process(org
         * .eclipse.swt.events.SelectionEvent)
         */
        @Override
        protected boolean process( SelectionEvent e ) {
            List< IPath > input = getInput();
            TableItem items[] = getAlxFilesViewer().getTable().getSelection();
            if( items.length > 0 ) {
                TableItem item = items[ 0 ];
                removeTableItems( item );
                input.remove( items[ 0 ].getData() );
                _alxFilesViewer.setInput( input );
                return true;
            }

            return false;
        }
    }

    /**
     * The listener that listens for edit events. When the edit button is pressed, the user will be prompted with a file dialog to
     * edit the selected ALX file location
     *
     * @author jkeshavarzi
     */
    class EditSelectionListener extends BlackBerryProjectPropertiesPage.FilePathOperationSelectionListener {

        /**
         * Instantiates a new edits the selection listener.
         *
         * @param page
         *            the page
         */
        public EditSelectionListener( BlackBerryProjectPropertiesPage page ) {
            page.super( getPart() );
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.FilePathOperationSelectionListener#process(org
         * .eclipse.swt.events.SelectionEvent)
         */
        @Override
        protected boolean process( SelectionEvent e ) {
            Shell shell = getClient().getShell();

            int selectedIndex = getAlxFilesViewer().getTable().getSelectionIndex();
            if( selectedIndex != -1 ) {
                String alxItem = ( (IPath) getAlxFilesViewer().getElementAt( selectedIndex ) ).toOSString();
                IPath itemPath = getParentPage().getBlackBerryProject().getProject().getLocation().append( alxItem );
                String filePath = openFileDialog( shell, itemPath );
                if( filePath != null ) {
                    setLastSelectedPath( new File( filePath ).getParent() );
                    IPath relativePath = getEditor().makeRelative( new Path( filePath ) );
                    if( getProjectPropertiesPage().getObjectIndexInViewer( getAlxFilesViewer(), relativePath.toString() )
                            .intValue() == -1 ) {
                        getAlxFilesViewer().replace( relativePath.toString(), selectedIndex );
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * The label provider used by the alx table viewer
     *
     *
     */
    class AlxTableLabelProvider implements ITableLabelProvider {

        @Override
        public String getColumnText( Object element, int columnIndex ) {
            if( ( null != element ) && ( columnIndex >= 0 ) ) {
                IPath filePath = (IPath) element;
                switch( columnIndex ) {
                    case 0:
                        return pathToColumnValue( filePath );
                }
            }
            return ""; //$NON-NLS-1$
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
        public Image getColumnImage( Object element, int columnIndex ) {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
