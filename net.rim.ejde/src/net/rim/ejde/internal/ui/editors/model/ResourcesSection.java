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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.ui.editors.model.factories.ControlFactory;
import net.rim.ejde.internal.ui.editors.model.factories.ControlFactory.ControlType;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.ProjectUtils.RRHFile;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * This class creates the resources section used in the project properties editor.
 *
 * @author jkeshavarzi
 *
 */
public class ResourcesSection extends AbstractResourcesSection {
    private static final Logger _log = Logger.getLogger( ResourcesSection.class );
    private static final String DESCRIPTION_KEY = "description_key"; //$NON-NLS-1$
    private Combo _descriptionId_TextChoiceField;
    private Label _descriptionId_LabelField;

    /**
     * Constructs the ResourcesSection on the given parent composite.
     *
     * @param page
     * @param parent
     * @param toolkit
     * @param style
     */
    public ResourcesSection( BlackBerryProjectPropertiesPage page, Composite parent, FormToolkit toolkit, int style ) {
        super( page, parent, page.getManagedForm().getToolkit(), ( style | Section.DESCRIPTION | ExpandableComposite.TITLE_BAR ) );
    }

    @Override
    protected void createFormContent( Section section, FormToolkit toolkit ) {
        super.createFormContent( section, toolkit );
        build( (Composite) section.getClient(), toolkit );
    }

    @Override
    protected void bundleChanged( RRHFile resource ) {
        super.bundleChanged( resource );
        if( resource != null ) {
            String[] resourceKeys = getProjectPropertiesPage().getResourceKeys( resource.getKeyTalbe() );
            _descriptionId_TextChoiceField.setItems( resourceKeys );
        } else {
            _descriptionId_TextChoiceField.removeAll();
        }
    }

    private void build( final Composite body, FormToolkit toolkit ) {
        Map< ControlType, Control > textInputControlList;

        SelectionAdapter descriptionSelectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent event ) {
                validateProperties();
            }
        };

        textInputControlList = ControlFactory.buildComboBoxControl( body, toolkit, null, null,
                Messages.ResourcesSection_Resource_Description_Label, Messages.ResourcesSection_Resource_Description_ToolTip,
                descriptionSelectionAdapter, getProjectPropertiesPage().new DirtyListener( getProjectPropertiesPage()
                        .getSectionPartProperty( body ) ) );
        _descriptionId_TextChoiceField = (Combo) textInputControlList.get( ControlType.COMBO );
        _descriptionId_LabelField = (Label) textInputControlList.get( ControlType.LABEL );

        refreshControls( true );
    }

    @Override
    public void commit( boolean onSave ) {
        super.commit( onSave );

        BlackBerryProperties properties = getProjectPropertiesPage().getBlackBerryProject().getProperties();
        properties._resources.setHasTitleResource( isTitleResourceAvailable() );

        RRHFile rrhFile = getResourceBundle();

        if( rrhFile != null ) {
            IFile file = rrhFile.getFile();
            String packageString = rrhFile.getResourceClassName();
            String bundleName = packageString.substring( packageString.lastIndexOf( IConstants.DOT_MARK ) + 1 );
            properties._resources.setTitleResourceBundleName( bundleName );
            properties._resources.setTitleResourceBundleClassName( packageString );
            properties._resources.setTitleResourceBundleRelativePath( file == null ? IConstants.EMPTY_STRING : file
                    .getProjectRelativePath().toString() );
            properties._resources.setTitleResourceBundleKey( getTitleId() );
            properties._resources.setDescriptionId( getDescriptionId() );
        } else {
            // user wants to clear the field, set the properties to empty string
            if( _resourceBundleClassName_TextChoiceField.getText().isEmpty() ) {
                properties._resources.setTitleResourceBundleName( IConstants.EMPTY_STRING );
                properties._resources.setTitleResourceBundleClassName( IConstants.EMPTY_STRING );
                properties._resources.setTitleResourceBundleRelativePath( IConstants.EMPTY_STRING );
                properties._resources.setTitleResourceBundleKey( IConstants.EMPTY_STRING );
                properties._resources.setDescriptionId( IConstants.EMPTY_STRING );
            }
        }
    }

    /**
     * Refreshes the resources section controls. This involves repopulating the resource controls with the current set of project
     * resources/keys and updating each the controls based on required business rules.
     *
     * @param loadFromProperties
     *            - If true, will use editor properties object to determine control selections.
     */
    public void refreshControls( boolean loadFromProperties ) {
        _log.trace( "Refreshing the resource section for "
                + getProjectPropertiesPage().getBlackBerryProject().getProject().getName() );
        BlackBerryProperties properties = getProjectPropertiesPage().getBlackBerryProject().getProperties();
        Boolean isDirty = getEditor().isDirty();
        String bundleClassName = "";
        String title = "";
        String description = "";
        setResources( ProjectUtils.getProjectResources( getProjectPropertiesPage().getBlackBerryProject() ) );

        if( loadFromProperties ) {
            bundleClassName = properties._resources.getTitleResourceBundleClassName();
            title = properties._resources.getTitleResourceBundleKey();
            description = properties._resources.getDescriptionId();
            getResourceAvailableField().setSelection( properties._resources.hasTitleResource() );
        } else {
            bundleClassName = getResourceBundleClassNameField().getText();
            title = getTitleIdField().getText();
            description = _descriptionId_TextChoiceField.getText();
        }

        List< String > bundless = new ArrayList< String >();
        for( String key : getResources().keySet() ) {
            bundless.add( key );
        }
        if( !bundless.contains( IConstants.EMPTY_STRING ) ) {
            bundless.add( 0, IConstants.EMPTY_STRING );
        }
        String[] bundleNames = bundless.toArray( new String[ bundless.size() ] );
        getResourceBundleClassNameField().setItems( bundleNames );
        getResourceBundleClassNameField().setText( bundleClassName );
        int index = -1;
        for( int i = 0; i < bundleNames.length; i++ ) {
            if( bundleNames[ i ].equals( bundleClassName ) ) {
                index = i;
            }
        }
        getResourceBundleClassNameField().select( index );

        // DAVID:Test
        RRHFile rrhFile = getResources().get( getResourceBundleClassNameField().getText() );
        List< String > resourceKeys = new ArrayList< String >();
        if( rrhFile != null ) {
            resourceKeys.addAll( Arrays.asList( getProjectPropertiesPage().getResourceKeys( rrhFile.getKeyTalbe() ) ) );
        }
        if( !resourceKeys.contains( IConstants.EMPTY_STRING ) ) {
            resourceKeys.add( 0, IConstants.EMPTY_STRING );
        }
        String[] keys = resourceKeys.toArray( new String[ resourceKeys.size() ] );
        getTitleIdField().setItems( keys );
        getTitleIdField().setText( title == null ? IConstants.EMPTY_STRING : title );
        index = -1;
        for( int i = 0; i < keys.length; i++ ) {
            if( keys[ i ].equals( title ) ) {
                index = i;
            }
        }
        getTitleIdField().select( index );

        _descriptionId_TextChoiceField.setItems( resourceKeys.toArray( new String[ resourceKeys.size() ] ) );
        _descriptionId_TextChoiceField.setText( description == null ? IConstants.EMPTY_STRING : description );

        validateProperties();
        updateControlStates();

        // Restore the dirty flag if this is initial loading of the form as opposed to refresh
        getEditor().setDirty( isDirty );
    }

    @Override
    public void updateControlStates() {
        super.updateControlStates();

        boolean isLibrary = getProjectType().equals( BlackBerryProject.LIBRARY );
        boolean enableIsTitleAvailable = !isLibrary;
        boolean enableResourceFields = enableIsTitleAvailable && getResourceAvailableField().getSelection();

        _descriptionId_TextChoiceField.setEnabled( enableResourceFields );
        _descriptionId_LabelField.setEnabled( enableResourceFields );
    }

    /**
     * @return The selected Description ID key pulled from the UI.
     */
    public String getDescriptionId() {
        return _descriptionId_TextChoiceField.getText();
    }

    protected void validateProperties() {
        super.validateProperties();
        getProjectPropertiesPage().removeEditorErrorMarker( DESCRIPTION_KEY, _descriptionId_TextChoiceField );
        boolean available = isTitleResourceAvailable();
        if( available ) {
            // validate description
            RRHFile rrhFile = getResourceBundle();
            List< String > keys = new ArrayList< String >();
            if( rrhFile != null ) {
                keys.addAll( Arrays.asList( getProjectPropertiesPage().getResourceKeys( rrhFile.getKeyTalbe() ) ) );
            }
            String description = _descriptionId_TextChoiceField.getText();
            if( !description.isEmpty() && !keys.contains( description ) ) {
                // description is invalid
                getProjectPropertiesPage().createEditorErrorMarker( DESCRIPTION_KEY,
                        NLS.bind( Messages.ResourcesSection_invalidResourceKey, description ), _descriptionId_TextChoiceField );
            }
        }
    }
}
