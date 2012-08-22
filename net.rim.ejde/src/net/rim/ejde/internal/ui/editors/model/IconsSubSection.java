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
import java.util.Map;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.Icon;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.Action;
import net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.FilePathOperationSelectionListener;
import net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.TableSelectionListener;
import net.rim.ejde.internal.ui.editors.model.factories.ControlFactory;
import net.rim.ejde.internal.util.FileUtils;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * The icon sub section used in both the icons section and the aep details section
 *
 * @author jkeshavarzi
 *
 */
public class IconsSubSection {
    private String _lastSelectedPath = null;
    private AbstractFormPart _parent;
    private BlackBerryProjectPropertiesPage _parentPage;
    private CheckboxTableViewer _viewer;
    private Map< Action, Button > _actionButtons;
    private Composite _body;
    private FormToolkit _toolkit;

    /**
     * Instantiates the IconSubSection object
     *
     * @param page
     * @param parent
     * @param body
     * @param toolkit
     */
    public IconsSubSection( BlackBerryProjectPropertiesPage page, AbstractFormPart parent, final Composite body,
            FormToolkit toolkit ) {
        _parent = parent;
        _parentPage = page;
        _body = body;
        _toolkit = toolkit;
    }

    /**
     * When called, the icon section will be creates on the given parent
     *
     * @param createLabel
     */
    public void create( Boolean createLabel, String toolTipText ) {
        Map< Action, SelectionListener > _actionListeners = new HashMap< Action, SelectionListener >( Action.values().length );
        _actionListeners.put( Action.ADD_FROM_PROJECT, new AddIconSelectionListener( _parent ) );
        _actionListeners.put( Action.ADD_FROM_EXTERNAL, new AddExternalIconSelectionListener( _parent ) );
        _actionListeners.put( Action.REMOVE, new DeleteIconSelectionListener( _parent ) );

        String label = createLabel ? Messages.BlackBerryProjectPropertiesPage_Table_Title : null;

        _viewer = (CheckboxTableViewer) ControlFactory
                .buildIconTableControl(
                        _body,
                        _toolkit,
                        label,
                        toolTipText,
                        SWT.CHECK,
                        2,
                        new String[] { "Icon file:" }, null, new ArrayContentProvider(), new IconsTableLabelProvider(), new IconTableCheckStateProvider(), null ); //$NON-NLS-1$

        _actionButtons = ControlFactory.buildButtonControls( _actionListeners, _body, _toolkit );
        _viewer.getTable().addSelectionListener( new TableSelectionListener( _actionButtons ) );
        _viewer.addCheckStateListener( new IconTableCheckStateListener() );
    }

    /**
     * Adds the given icon object to the icon table
     *
     * @param icon
     */
    public boolean addIconTableItem( Icon icon ) {
        Table table = _viewer.getTable();
        if( table.getItemCount() == 1 ) {
            if( !table.getItem( 0 ).getChecked() ) {
                icon.setIsFocus( Boolean.TRUE );
            }
        }

        boolean result = _parentPage.addTableItem( _viewer, icon, _actionButtons, false );

        if( result ) {
            updateIconButtons();
            validateIcon( icon );
            _parent.markDirty();
            refresh();
        } else {
            MessageDialog.openError( _parent.getManagedForm().getForm().getShell(),
                    Messages.BlackBerryProjectPropertiesPage_Dup_File_Err_Dialog_Title,
                    Messages.BlackBerryProjectPropertiesPage_Dup_Icon_Err_Dialog_Msg );
        }
        return result;
    }

    /**
     * Removes the selected icon object from the icon table Updates the Focus status and UI accordingly
     *
     * @param icon
     */
    public void removeSelectedIconTableItem() {
        _parentPage.removeSelectedTableItem( _viewer, _actionButtons, false );

        Table table = _viewer.getTable();
        if( table.getItemCount() == 1 ) {
            ( (Icon) _viewer.getElementAt( 0 ) ).setIsFocus( Boolean.FALSE );
        }

        updateIconButtons();
        _parent.markDirty();
        refresh();
    }

    public void refresh() {
        Object obj1 = _viewer.getElementAt( 0 );
        Object obj2 = _viewer.getElementAt( 1 );

        if( obj2 != null ) {
            _viewer.update( obj2, null );
        }

        if( obj1 != null ) {
            _viewer.update( obj1, null );
        }
    }

    public void setInput( Icon[] icons ) {
        _viewer.getTable().removeAll();
        _viewer.setInput( icons );
        // refresh();
        refreshSelection();
        updateIconButtons();
    }

    public void refreshSelection() {
        Table table = _viewer.getTable();
        if( table.getItemCount() > 0 ) {
            table.setSelection( 0 );
        }
    }

    public void validateIcons() {
        Icon icons[] = getIcons();
        for( Icon icon : icons ) {
            validateIcon( icon );
        }
    }

    private void validateIcon( Icon icon ) {
        BlackBerryProjectFormEditor editor = ( (BlackBerryProjectFormEditor) _parentPage.getEditor() );

        BBDiagnostic diag = BBPropertiesValidator.validateIconExists( editor.getBlackBerryProject().getProject(), icon );

        if( diag.getSeverity() == Diagnostic.ERROR ) {
            String key = icon.getCanonicalFileName();
            String msg = diag.getMessage();

            if( _parent instanceof AlternateEntryPointDetails ) {
                // uniquely identify the keys
                AlternateEntryPointDetails details = (AlternateEntryPointDetails) _parent;
                String title = details.getCurrentAep().getTitle();
                key = details.createUniquePrefix() + key;
                msg = "(" + title + ") " + msg; //$NON-NLS-1$ //$NON-NLS-2$
            }

            if( diag.getSeverity() == Diagnostic.ERROR ) {
                _parentPage.createEditorErrorMarker( key, msg, _viewer.getTable() );
            } else {
                _parentPage.removeEditorErrorMarker( key, _viewer.getTable() );
            }
        }
    }

    /**
     * Sets the state (enabled/disabled) of the section
     *
     * @param enabled
     */
    public void setEnabled( boolean enabled ) {
        if( enabled ) {
            updateIconButtons();
        } else {
            _actionButtons.get( Action.ADD_FROM_PROJECT ).setEnabled( enabled );
            _actionButtons.get( Action.ADD_FROM_EXTERNAL ).setEnabled( enabled );
            _actionButtons.get( Action.REMOVE ).setEnabled( true );
        }
    }

    private void updateIconButtons() {
        int count = _viewer.getTable().getItemCount();

        if( count > 1 ) {
            if( _actionButtons.get( Action.ADD_FROM_PROJECT ) != null ) {
                _actionButtons.get( Action.ADD_FROM_PROJECT ).setEnabled( false );
            }
            if( _actionButtons.get( Action.ADD_FROM_EXTERNAL ) != null ) {
                _actionButtons.get( Action.ADD_FROM_EXTERNAL ).setEnabled( false );
            }
            if( _actionButtons.get( Action.REMOVE ) != null ) {
                _actionButtons.get( Action.REMOVE ).setEnabled( true );
            }
        } else {
            if( _actionButtons.get( Action.ADD_FROM_PROJECT ) != null ) {
                _actionButtons.get( Action.ADD_FROM_PROJECT ).setEnabled( true );
            }
            if( _actionButtons.get( Action.ADD_FROM_EXTERNAL ) != null ) {
                _actionButtons.get( Action.ADD_FROM_EXTERNAL ).setEnabled( true );
            }
            if( _actionButtons.get( Action.REMOVE ) != null ) {
                _actionButtons.get( Action.REMOVE ).setEnabled( count == 0 ? false : true );
            }
        }
    }

    private String openImageFileDialog( Shell shell, String fileName ) {
        FileDialog dialog = new FileDialog( shell, SWT.SINGLE );
        String[] filterNames = new String[] { Messages.BlackBerryProjectPropertiesPage_Dialog_Filter_Image_Files };
        String[] filterExtensions = new String[] { "*.gif;*.png;*.xpm;*.jpg;*.jpeg;*.tiff", "*" }; //$NON-NLS-1$ //$NON-NLS-2$
        String filterPath = "/"; //$NON-NLS-1$
        String platform = SWT.getPlatform();

        if( platform.equals( "win32" ) || platform.equals( "wpf" ) ) { //$NON-NLS-1$ //$NON-NLS-2$
            filterNames = new String[] { Messages.BlackBerryProjectPropertiesPage_Dialog_Filter_Image_Files };
            filterExtensions = new String[] { "*.gif;*.png;*.bmp;*.jpg;*.jpeg;*.tiff", "*.*" }; //$NON-NLS-1$ //$NON-NLS-2$

            if( _lastSelectedPath != null ) {
                filterPath = _lastSelectedPath;
            } else {
                filterPath = _parentPage.getBlackBerryProject().getProject().getLocation().toOSString();
            }
        }

        dialog.setFilterNames( filterNames );
        dialog.setFilterExtensions( filterExtensions );
        dialog.setFilterPath( filterPath );
        dialog.setFileName( fileName );

        return dialog.open();
    }

    /**
     * The label provider used by the icon table viewer
     *
     * @author jkeshavarzi
     *
     */
    public class IconsTableLabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage( Object element, int columnIndex ) {
            Image image = null;

            if( ( null != element ) && ( element instanceof Icon ) && ( columnIndex == 1 ) ) {
                Icon icon = (Icon) element;
                image = ( (BlackBerryProjectFormEditor) _parentPage.getEditor() ).createIconImage( icon );
            }

            return image;
        }

        @Override
        public String getColumnText( Object element, int columnIndex ) {
            if( ( null != element ) && Icon.class.equals( element.getClass() ) && ( columnIndex >= 0 ) ) {
                Icon icon = (Icon) element;
                switch( columnIndex ) {
                    case 0:
                        if( icon.isFocus().booleanValue() && ( _viewer.getTable().getItemCount() > 1 ) ) {
                            _viewer.setChecked( element, true );
                        }
                        return ""; //$NON-NLS-1$
                    case 1:
                        return ""; //$NON-NLS-1$
                    case 2:
                        return AbstractSection.pathToColumnValue( new Path( icon.getCanonicalFileName() ) );
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
    }

    /**
     * The checked state listener used by the icon table viewer. Ensures that only one focus icon can be selected.
     *
     * @author jkeshavarzi, bkurz
     *
     */
    private class IconTableCheckStateListener implements ICheckStateListener {
        @Override
        public void checkStateChanged( CheckStateChangedEvent event ) {
            BlackBerryProjectFormEditor editor = (BlackBerryProjectFormEditor) _parentPage.getEditor();
            Object eventSource = event.getSource();
            Object eventElement = event.getElement();
            boolean isChecked = event.getChecked();

            if( eventSource instanceof CheckboxTableViewer ) {
                CheckboxTableViewer tableViewer = (CheckboxTableViewer) eventSource;
                int firstElementIndex = _parentPage.getObjectIndexInViewer( tableViewer, eventElement ).intValue();
                BlackBerryProjectApplicationPage projectApplicationPage = editor._applicationPage;
                if( projectApplicationPage.getGeneralSection().getProjectType().equals( BlackBerryProject.LIBRARY ) ) {
                    tableViewer.setChecked( eventElement, !isChecked );
                } else if( tableViewer.getTable().getItemCount() <= 1 ) {
                    tableViewer.setChecked( eventElement, false );
                } else {
                    if( firstElementIndex != -1 ) {
                        Object otherElement = tableViewer.getElementAt( firstElementIndex == 0 ? 1 : 0 );
                        updateIconFocus( tableViewer, eventElement, isChecked );
                        updateIconFocus( tableViewer, otherElement, !isChecked );

                        _parent.markDirty();
                        editor.setDirty( true );
                        refresh();
                    }
                }
            }
        }

        private void updateIconFocus( CheckboxTableViewer tableViewer, Object element, boolean focusState ) {
            if( element instanceof Icon ) {
                Icon icon = (Icon) element;
                icon.setIsFocus( focusState );
            }
        }
    }

    /**
     * The listener that handles add events. When the add button is pressed the user will be be prompted with a project based
     * dialog to select an icon
     *
     * @author jkeshavarzi
     *
     */
    private class AddIconSelectionListener extends FilePathOperationSelectionListener {
        protected AddIconSelectionListener( AbstractFormPart section ) {
            _parentPage.super( section );
        }

        @Override
        protected boolean process( SelectionEvent evt ) {
            Table table = _viewer.getTable();

            if( table.getItemCount() <= 1 ) {
                Object iconObj = null;
                if( table.getItemCount() == 1 ) {
                    Object tableObj = table.getItem( 0 );
                    TableItem item = (TableItem) tableObj;
                    iconObj = item.getData();
                }
                final Icon icon = (Icon) iconObj;
                ILabelProvider lp = new WorkbenchLabelProvider();
                ITreeContentProvider cp = new WorkbenchContentProvider();
                ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog( _body.getShell(), lp, cp );
                ISelectionStatusValidator validator = new ISelectionStatusValidator() {
                    public IStatus validate( Object[] selection ) {
                        Status errorStatus = new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, NLS.bind(
                                Messages.BlackBerryProjectPropertiesPage_Add_File_Error_Status_Invalid_File, "icon" ) );
                        IStatus okStatus = Status.OK_STATUS;

                        for( Object element : selection ) {
                            if( element instanceof IFile ) {
                                IFile file = (IFile) element;
                                if( icon != null ) {
                                    // Relative Path
                                    IPath fileAbsPath = file.getLocation();
                                    IPath fileRelPath = _parentPage.getBlackBerryProject().getProject().getLocation()
                                            .append( file.getProjectRelativePath() );
                                    IPath iconPath = _parentPage.getBlackBerryProject().getProject().getLocation()
                                            .append( icon.getCanonicalFileName() );
                                    if( fileAbsPath.equals( iconPath ) || fileRelPath.equals( iconPath ) ) {
                                        return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID,
                                                Messages.BlackBerryProjectPropertiesPage_Dup_File_Err_Status_Msg );
                                    }
                                }
                                String fileName = file.getName();
                                if( !BlackBerryProjectPropertiesPage.isImage( fileName ) ) {
                                    return errorStatus;
                                }
                            } else {
                                return errorStatus;
                            }
                        }

                        if( selection.length > 2 ) {
                            return new Status( IStatus.ERROR, ContextManager.PLUGIN_ID,
                                    Messages.BlackBerryProjectPropertiesPage_Add_Icon_Error_Status_Max_Two );
                        }

                        return okStatus;
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
                            return BlackBerryProjectPropertiesPage.isImage( ( (IFile) element ).getName() );
                        }

                        return false;
                    }
                } );
                dialog.setComparator( new ResourceComparator( ResourceComparator.NAME ) );
                dialog.setValidator( validator );
                dialog.setTitle( Messages.BlackBerryProjectPropertiesPage_Add_Icon_Title );
                dialog.setMessage( Messages.BlackBerryProjectPropertiesPage_Add_Icon_Message );
                dialog.setInput( _parentPage.getBlackBerryProject().getProject() );
                dialog.setAllowMultiple( false );
                if( dialog.open() == Window.OK ) {
                    Object[] elements = dialog.getResult();
                    if( elements != null ) {
                        boolean result = false;
                        for( Object element : elements ) {
                            IResource resource = (IResource) element;
                            result = result
                                    || addIconTableItem( new Icon( resource.getProjectRelativePath().toOSString(), false ) );
                            table.getColumn( 2 ).pack();
                        }

                        if( table.getItemCount() > 1 ) {
                            _actionButtons.get( Action.ADD_FROM_PROJECT ).setEnabled( false );
                            _actionButtons.get( Action.ADD_FROM_EXTERNAL ).setEnabled( false );
                        }

                        return result;
                    }
                }
            }
            return false;
        }
    }

    /**
     * The listener that handles add external events. When the add external button is pressed the user will be be prompted with a
     * file browser dialog to select an icon
     *
     * @author jkeshavarzi
     *
     */
    private class AddExternalIconSelectionListener extends FilePathOperationSelectionListener {
        protected AddExternalIconSelectionListener( AbstractFormPart section ) {
            _parentPage.super( section );
        }

        @Override
        protected boolean process( SelectionEvent e ) {
            Shell shell = _body.getShell();
            Table table = _viewer.getTable();

            if( table.getItemCount() <= 1 ) {
                String absolutePath = openImageFileDialog( shell, null );
                if( absolutePath != null ) {
                    _lastSelectedPath = new File( absolutePath ).getParent();
                    IPath relPath = ( (BlackBerryProjectFormEditor) _parentPage.getEditor() ).makeRelative( new Path(
                            absolutePath ) );

                    // Will not be able to link external icon if one already exists in the res folder.
                    IFolder resFolder = FileUtils.getResFolder( _parentPage.getBlackBerryProject().getProject() );
                    IFile relFile = AbstractSection.figureFile( relPath, resFolder );
                    if( !AbstractSection.isFileClear( relFile, null, resFolder, _parent.getManagedForm().getForm().getShell() ) ) {
                        return false;
                    }

                    if( table.getItemCount() > 0 ) {
                        Object ico = table.getItem( 0 ).getData();
                        if( ico instanceof Icon ) {
                            IPath prevPath = new Path( ( (Icon) ico ).getCanonicalFileName() );
                            if( !AbstractSection.isFileClear( relFile, AbstractSection.figureFile( prevPath, resFolder ),
                                    resFolder, _parent.getManagedForm().getForm().getShell() ) ) {
                                return false;
                            }
                        }
                    }
                    boolean result = addIconTableItem( new Icon( relPath.toOSString(), false ) );
                    table.getColumn( 2 ).pack();

                    if( table.getItemCount() > 1 ) {
                        _actionButtons.get( Action.ADD_FROM_PROJECT ).setEnabled( false );
                        _actionButtons.get( Action.ADD_FROM_EXTERNAL ).setEnabled( false );
                    }
                    return result;
                }
            }
            return false;
        }
    }

    /**
     * The listener that handles remove events. When the remove button is pressed thw selected icon will be removed from the
     * viewer
     *
     * @author jkeshavarzi
     *
     */
    private class DeleteIconSelectionListener extends FilePathOperationSelectionListener {
        protected DeleteIconSelectionListener( AbstractFormPart section ) {
            _parentPage.super( section );
        }

        @Override
        protected boolean process( SelectionEvent e ) {
            IStructuredSelection selection = (IStructuredSelection) _viewer.getSelection();
            Object icon = selection.getFirstElement();
            if( icon != null ) {
                if( _parent instanceof AlternateEntryPointDetails ) {
                    AlternateEntryPointDetails details = (AlternateEntryPointDetails) _parent;
                    String key = details.createUniquePrefix() + ( (Icon) icon ).getCanonicalFileName();
                    _parentPage.removeEditorErrorMarker( key, _viewer.getTable() );
                } else {
                    _parentPage.removeEditorErrorMarker( ( (Icon) icon ).getCanonicalFileName(), _viewer.getTable() );
                }

                removeSelectedIconTableItem();

                return true;
            }
            return false;
        }
    }

    private class IconTableCheckStateProvider implements ICheckStateProvider {
        @Override
        public boolean isChecked( Object element ) {
            boolean result = false;
            if( element instanceof Icon ) {
                result = ( (Icon) element ).isFocus().booleanValue();
            }
            return result;
        }

        @Override
        public boolean isGrayed( Object element ) {
            return false;
        }
    }

    /**
     * @return An array of Icon objects pulled from the UI
     */
    public Icon[] getIcons() {
        ArrayList< Icon > icons = new ArrayList< Icon >();
        Object icon1 = _viewer.getElementAt( 0 );
        Object icon2 = _viewer.getElementAt( 1 );

        if( icon1 != null ) {
            icons.add( (Icon) icon1 );
        }
        if( icon2 != null ) {
            icons.add( (Icon) icon2 );
        }

        return icons.toArray( new Icon[ icons.size() ] );
    }

    /**
     * @return The icon table viewer
     */
    public CheckboxTableViewer getViewer() {
        return _viewer;
    }
}
