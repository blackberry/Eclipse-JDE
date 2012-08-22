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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.rim.ejde.internal.model.BasicBlackBerryProperties.AlternateEntryPoint;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.Icon;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.Action;
import net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.FilePathOperationSelectionListener;
import net.rim.ejde.internal.ui.editors.model.BlackBerryProjectPropertiesPage.TableSelectionListener;
import net.rim.ejde.internal.ui.editors.model.factories.ControlFactory;
import net.rim.ejde.internal.ui.editors.model.factories.LayoutFactory;
import net.rim.ejde.internal.util.Messages;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * This class creates the alternate entry point section used in the project properties editor.
 *
 * @author jkeshavarzi
 *
 */
public class AlternateEntryPointSection extends AbstractSection {
    private TableViewer _alternateEntryPointsTableViewer;
    private ArrayList< AlternateEntryPoint > _aeps = new ArrayList< AlternateEntryPoint >();
    private Map< Action, Button > _actionButtons;
    private Composite _client;

    /**
     * This class creates the AEP section used in the project properties editor.
     *
     * @param page
     * @param parent
     * @param toolkit
     * @param style
     */
    public AlternateEntryPointSection( BlackBerryProjectPropertiesPage page, Composite parent, FormToolkit toolkit, int style ) {
        super( page, parent, page.getManagedForm().getToolkit(), ( style | Section.DESCRIPTION | ExpandableComposite.TITLE_BAR ) );
        createFormContent( getSection(), toolkit );
    }

    protected void createFormContent( Section section, FormToolkit toolkit ) {
        preBuild();

        GridData gd = new GridData( GridData.FILL_BOTH );
        gd.minimumWidth = 250;
        section.setLayout( LayoutFactory.createClearGridLayout( false, 1 ) );
        section.setLayoutData( gd );

        section.setDescription( Messages.AlternateEntryPointSection_Description );
        _client = toolkit.createComposite( section );
        _client.setLayout( LayoutFactory.createSectionGridLayout( false, 3 ) );
        section.setClient( _client );

        build( _client, toolkit );

        postBuild( _client, toolkit );
    }

    private void preBuild() {
        getSection().setText( Messages.AlternateEntryPointSection_Title );
        getEditor().addListener( new PropertyListener() );
    }

    private void build( Composite body, FormToolkit toolkit ) {
        Map< Action, SelectionListener > actionListeners;
        actionListeners = new HashMap< Action, SelectionListener >( Action.values().length );
        actionListeners.put( Action.ADD, new FilePathAddSelectionListener( getProjectPropertiesPage() ) );
        actionListeners.put( Action.REMOVE, new FilePathDeleteSelectionListener( getProjectPropertiesPage() ) );
        actionListeners.put( Action.MOVE_UP, new MoveUpSelectionListener( getProjectPropertiesPage() ) );
        actionListeners.put( Action.MOVE_DOWN, new MoveDownSelectionListener( getProjectPropertiesPage() ) );

        setProjectType( getEditor().getBlackBerryProject().getProperties()._application.getType() );

        _alternateEntryPointsTableViewer = (TableViewer) ControlFactory.buildTableControl( body, toolkit, null,
                Messages.AlternateEntryPointSection_ToolTip, SWT.NONE, 2,
                new String[] { "entry point:" }, new AEPContentProvider(), new AEPLabelProvider(), null ); //$NON-NLS-1$

        _actionButtons = ControlFactory.buildButtonControls( actionListeners, body, toolkit );

        // Set the heightHint of the control to match the height of its detail section
        Table table = _alternateEntryPointsTableViewer.getTable();
        table.setData( BlackBerryProjectPropertiesPage.TABLE_TEXT_INDEX_KEY, 0 );
        GridData gridData = ( (GridData) table.getLayoutData() );
        gridData.heightHint = 370;

        table.addSelectionListener( new TableSelectionListener( _actionButtons ) );

        _alternateEntryPointsTableViewer.addSelectionChangedListener( new ISelectionChangedListener() {
            @Override
            public void selectionChanged( SelectionChangedEvent event ) {
                getProjectPropertiesPage().getManagedForm().fireSelectionChanged( getPart(), event.getSelection() );
            }
        } );

        insertControlValuesFromModel( getProjectPropertiesPage().getBlackBerryProject().getProperties() );

        if( getProjectType().equals( BlackBerryProject.LIBRARY ) ) {
            setEnabled( false );
            getProjectPropertiesPage().getManagedForm().getForm()
                    .setText( Messages.BlackBerryProjectAlternateEntryPointPage_Page_Title_Disabled );
        }
    }

    private void postBuild( Composite body, FormToolkit toolkit ) {
        toolkit.paintBordersFor( body );
    }

    @Override
    public void commit( boolean onSave ) {
        BlackBerryProjectAlternateEntryPointPage page = (BlackBerryProjectAlternateEntryPointPage) getProjectPropertiesPage();
        BlackBerryProperties properties = page.getBlackBerryProject().getProperties();

        AlternateEntryPoint aeps[] = page.getAlternateEntryPoints( onSave );

        // Link any missing external icons from each aep
        for( AlternateEntryPoint aep : aeps ) {
            Icon icons[] = aep.getIconFiles();

            if( icons.length > 0 ) {
                getEditor().linkExternalIcons( icons );
                if( page.getAlternateEntryPointDetails() != null ) {
                    page.getAlternateEntryPointDetails().getIconsSection().setViewerInput( icons );
                }
            }
        }
        properties.setAlternateEntryPoints( aeps );
        super.commit( onSave );
    }

    /**
     * Update the controls within this section with values from the given properties object
     *
     * @param properties
     */
    public void insertControlValuesFromModel( BlackBerryProperties properties ) {
        AlternateEntryPoint[] modelAepList = properties.getAlternateEntryPoints();
        _aeps.clear();
        _aeps.addAll( Arrays.asList( modelAepList ) );
        _alternateEntryPointsTableViewer.setInput( _aeps );
        _alternateEntryPointsTableViewer.refresh();
    }

    public void setEnabled( Boolean enabled ) {
        Table table = _alternateEntryPointsTableViewer.getTable();
        int selectionIndex = table.getSelectionIndex();
        int itemCount = table.getItemCount();
        int selectionCount = table.getSelectionCount();

        getSection().setEnabled( enabled );
        _actionButtons.get( Action.ADD ).setEnabled( enabled );
        _actionButtons.get( Action.REMOVE ).setEnabled( selectionCount > 0 ? enabled : false );
        _actionButtons.get( Action.MOVE_UP ).setEnabled( selectionIndex > 0 ? enabled : false );
        _actionButtons.get( Action.MOVE_DOWN )
                .setEnabled( selectionCount > 0 && selectionIndex < itemCount - 1 ? enabled : false );
    }

    /**
     * @return The AEP table viewer used in this section.
     */
    public TableViewer getAlternateEntryPointsTableViewer() {
        return _alternateEntryPointsTableViewer;
    }

    /**
     * @return An array of the current AlternateEntryPoint objects as shown in the view.
     */
    public AlternateEntryPoint[] getAlternateEntryPoints() {
        return _aeps.toArray( new AlternateEntryPoint[ _aeps.size() ] );
    }

    /**
     * Returns a list of the titles of all installed alternate entry points
     *
     * @return
     */
    public List< String > getAepTitles() {
        List< String > aepTitles = new ArrayList< String >();
        for( AlternateEntryPoint aep : _aeps ) {
            aepTitles.add( aep.getTitle() );
        }
        return aepTitles;
    }

    private boolean aepExists( AlternateEntryPoint aepToSearch ) {
        for( AlternateEntryPoint aep : _aeps ) {
            if( aep.getTitle().equals( aepToSearch.getTitle() ) ) {
                return true;
            }
        }
        return false;
    }

    private void moveSelection( int index ) {
        IStructuredSelection selection = (IStructuredSelection) _alternateEntryPointsTableViewer.getSelection();
        _aeps.remove( selection.getFirstElement() );
        _aeps.add( index, (AlternateEntryPoint) selection.getFirstElement() );
        _alternateEntryPointsTableViewer.setInput( _aeps );
        _alternateEntryPointsTableViewer.refresh();
    }

    /**
     * The content provider used by the AlternateEntryPoint table viewer
     *
     * @author jkeshavarzi
     *
     */
    private class AEPContentProvider implements IStructuredContentProvider {
        @SuppressWarnings("unchecked")
        @Override
        public Object[] getElements( Object inputElement ) {
            if( inputElement instanceof ArrayList< ? > ) {
                ArrayList< AlternateEntryPoint > list = ( (ArrayList< AlternateEntryPoint >) inputElement );
                return list.toArray();
            }
            return new Object[ 0 ];
        }

        @Override
        public void dispose() {
        }

        @Override
        public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
        }
    }

    /**
     * The label provider used by the AlternateEntryPoint table viewer
     *
     * @author jkeshavarzi
     *
     */
    private class AEPLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public String getColumnText( Object obj, int index ) {
            if( obj instanceof AlternateEntryPoint ) {
                AlternateEntryPoint aep = (AlternateEntryPoint) obj;
                return aep.getTitle();
            }
            return ""; //$NON-NLS-1$
        }

        @Override
        public Image getColumnImage( Object obj, int index ) {
            return null;
        }
    }

    /**
     * A listener that listens for add aep events. This listener is trigger when the user pressed add and allows the user to
     * create a new AEP.
     *
     * @author jkeshavarzi
     *
     */
    private class FilePathAddSelectionListener extends FilePathOperationSelectionListener {
        public FilePathAddSelectionListener( BlackBerryProjectPropertiesPage page ) {
            page.super( getPart() );
        }

        @Override
        protected boolean process( SelectionEvent evt ) {
            InputDialog dialog = new InputDialog( _client.getShell(), Messages.AlternateEntryPointSection_Add_Dialog_Title,
                    Messages.AlternateEntryPointSection_Add_Dialog_Label, "", null ); //$NON-NLS-1$
            if( dialog.open() == InputDialog.OK ) {
                String dialogValue = dialog.getValue().trim();
                if( !StringUtils.isEmpty( dialogValue ) ) {
                    AlternateEntryPoint aep = new AlternateEntryPoint( dialogValue, null, null, null, null, null, null, null,
                            null, null, null, null, null, null );
                    if( !aepExists( aep ) ) {
                        _aeps.add( aep );
                        _alternateEntryPointsTableViewer.refresh();
                        getProjectPropertiesPage().selectItemInViewer( _alternateEntryPointsTableViewer, aep.getTitle(),
                                _actionButtons );
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * The listener used for the remove AEP event. When the remove button is pressed, the selected AEP reference is deleted and
     * its table item is removed.
     *
     * @author jkeshavarzi
     *
     */
    private class FilePathDeleteSelectionListener extends FilePathOperationSelectionListener {
        public FilePathDeleteSelectionListener( BlackBerryProjectPropertiesPage page ) {
            page.super( getPart() );
        }

        @Override
        protected boolean process( SelectionEvent e ) {
            Table table = _alternateEntryPointsTableViewer.getTable();
            Integer selectionIndex = table.getSelectionIndex();
            if( selectionIndex != -1 ) {
                StructuredSelection selection = (StructuredSelection) _alternateEntryPointsTableViewer.getSelection();
                Object selectedElement = selection.getFirstElement();
                _aeps.remove( selectedElement );
                getProjectPropertiesPage().removeSelectedTableItem( _alternateEntryPointsTableViewer, _actionButtons, false );

                return true;
            }
            return false;
        }
    }

    /**
     * MoveUpSelectionListener
     *
     * @author bkurz
     *
     */
    private class MoveUpSelectionListener extends FilePathOperationSelectionListener {
        protected MoveUpSelectionListener( BlackBerryProjectPropertiesPage page ) {
            page.super( getPart() );
        }

        @Override
        protected boolean process( SelectionEvent e ) {
            int selectionIndex = _alternateEntryPointsTableViewer.getTable().getSelectionIndex();
            moveSelection( _alternateEntryPointsTableViewer.getTable().getSelectionIndex() - 1 );

            if( selectionIndex - 1 == 0 ) {
                _actionButtons.get( Action.MOVE_UP ).setEnabled( false );
            }
            if( selectionIndex - 1 < _alternateEntryPointsTableViewer.getTable().getItemCount() - 1 ) {
                _actionButtons.get( Action.MOVE_DOWN ).setEnabled( true );
            }
            return true;
        }
    }

    /**
     * MoveDownSelectionListener
     *
     * @author bkurz
     *
     */
    private class MoveDownSelectionListener extends FilePathOperationSelectionListener {
        protected MoveDownSelectionListener( BlackBerryProjectPropertiesPage page ) {
            page.super( getPart() );
        }

        @Override
        protected boolean process( SelectionEvent e ) {
            int selectionIndex = _alternateEntryPointsTableViewer.getTable().getSelectionIndex();
            moveSelection( _alternateEntryPointsTableViewer.getTable().getSelectionIndex() + 1 );

            if( selectionIndex + 1 > 0 ) {
                _actionButtons.get( Action.MOVE_UP ).setEnabled( true );
            }
            if( selectionIndex + 1 == _alternateEntryPointsTableViewer.getTable().getItemCount() - 1 ) {
                _actionButtons.get( Action.MOVE_DOWN ).setEnabled( false );
            }
            return true;
        }
    }

    private class PropertyListener implements PropertyChangeListener {
        @Override
        public void propertyChange( PropertyChangeEvent evt ) {
            String property = evt.getPropertyName();
            if( property.equals( Messages.GeneralSection_Application_Type_Label ) ) {
                Object obj = evt.getNewValue();
                if( obj instanceof String ) {
                    setProjectType( (String) obj );
                    boolean isLibrary = getProjectType().equals( BlackBerryProject.LIBRARY );

                    setEnabled( !isLibrary );
                    getProjectPropertiesPage()
                            .getManagedForm()
                            .getForm()
                            .setText(
                                    isLibrary ? Messages.BlackBerryProjectAlternateEntryPointPage_Page_Title_Disabled
                                            : Messages.BlackBerryProjectAlternateEntryPointPage_Page_Title );
                }
            }
        }
    }
}
