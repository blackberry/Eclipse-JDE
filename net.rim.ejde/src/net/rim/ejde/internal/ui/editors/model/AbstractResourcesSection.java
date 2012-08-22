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
import java.util.HashMap;
import java.util.Map;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.ui.editors.model.factories.ControlFactory;
import net.rim.ejde.internal.ui.editors.model.factories.ControlFactory.ControlType;
import net.rim.ejde.internal.ui.editors.model.factories.LayoutFactory;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils.RRHFile;
import net.rim.ejde.internal.validation.BBDiagnostic;
import net.rim.ejde.internal.validation.BBPropertiesValidator;

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 *
 * @author bkurz
 *
 */
public abstract class AbstractResourcesSection extends AbstractSection {
    private Map< String, RRHFile > _resources = new HashMap< String, RRHFile >();

    private static final String RESOURCE_BUNDLE_KEY = "resource_bundle_key"; //$NON-NLS-1$
    private static final String TITLE_KEY = "title_key"; //$NON-NLS-1$
    protected Button _isTitleResourceAvailable_BooleanInputField;
    protected Label _resourceBundle_LabelField, _titleId_LabelField;
    protected Combo _resourceBundleClassName_TextChoiceField, _titleId_TextChoiceField;

    public AbstractResourcesSection( BlackBerryProjectPropertiesPage page, Composite parent, FormToolkit toolkit, int style ) {
        super( page, parent, toolkit, style );
        createFormContent( getSection(), toolkit );
    }

    protected abstract void refreshControls( boolean loadFromProperties );

    protected void validateProperties() {
        getProjectPropertiesPage().removeEditorErrorMarker( RESOURCE_BUNDLE_KEY, _resourceBundleClassName_TextChoiceField );
        getProjectPropertiesPage().removeEditorErrorMarker( TITLE_KEY, _titleId_TextChoiceField );
        boolean available = isTitleResourceAvailable();
        if( available ) {
            // validate bundle name
            String bundleName = _resourceBundleClassName_TextChoiceField.getText();
            if( !bundleName.equals( IConstants.EMPTY_STRING ) ) {
                BBDiagnostic diag = BBPropertiesValidator.validateResourceInfo( this.getProjectPropertiesPage()
                        .getBlackBerryProject().getProject(), getResources(), bundleName );
                if( diag.getSeverity() == BBDiagnostic.ERROR ) {
                    // resource file does not exist
                    getProjectPropertiesPage().createEditorErrorMarker( RESOURCE_BUNDLE_KEY,
                            NLS.bind( Messages.ResourcesSection_invalidResource, bundleName ),
                            _resourceBundleClassName_TextChoiceField );
                }
            }
            // validate title
            BBDiagnostic keyDiag = BBPropertiesValidator.validateResourceKey( getResourceBundle(), getTitleIdField().getText() );
            if( keyDiag.getSeverity() == BBDiagnostic.ERROR ) {
                // title is invalid
                getProjectPropertiesPage().createEditorErrorMarker( TITLE_KEY,
                        NLS.bind( Messages.ResourcesSection_invalidResourceKey, getTitleIdField().getText() ),
                        _titleId_TextChoiceField );
            }
        }
    }

    /**
     * @return The checked state of the is title resource available checkbox within this section.
     */
    public Boolean isTitleResourceAvailable() {
        return _isTitleResourceAvailable_BooleanInputField.getSelection();
    }

    /**
     * @return The selected resource bundle pulled from the UI.
     */
    public RRHFile getResourceBundle() {
        return getResources().get( _resourceBundleClassName_TextChoiceField.getText() );
    }

    /**
     * @return The selected Title ID key pulled from the UI.
     */
    public String getTitleId() {
        return _titleId_TextChoiceField.getText();
    }

    /**
     * Update the controls within this section with values from the given properties object
     *
     * @param properties
     */
    public void insertControlValuesFromModel() {
        refreshControls( true );
    }

    public void updateControlStates() {
        boolean isLibrary = getProjectType().equals( BlackBerryProject.LIBRARY );
        boolean enableIsTitleAvailable = !isLibrary;
        boolean enableResourceFields = enableIsTitleAvailable && _isTitleResourceAvailable_BooleanInputField.getSelection();

        _isTitleResourceAvailable_BooleanInputField.setEnabled( enableIsTitleAvailable );
        _resourceBundleClassName_TextChoiceField.setEnabled( enableResourceFields );
        _resourceBundle_LabelField.setEnabled( enableResourceFields );
        _titleId_TextChoiceField.setEnabled( enableResourceFields );
        _titleId_LabelField.setEnabled( enableResourceFields );
    }

    protected Map< String, RRHFile > getResources() {
        return this._resources;
    }

    protected void setResources( Map< String, RRHFile > _resources ) {
        this._resources = _resources;
    }

    protected Button getResourceAvailableField() {
        return this._isTitleResourceAvailable_BooleanInputField;
    }

    protected Combo getResourceBundleClassNameField() {
        return this._resourceBundleClassName_TextChoiceField;
    }

    protected Combo getTitleIdField() {
        return this._titleId_TextChoiceField;
    }

    protected void createFormContent( Section section, FormToolkit toolkit ) {
        preBuild();

        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, false );
        gd.minimumWidth = 250;
        section.setLayout( LayoutFactory.createClearGridLayout( false, 1 ) );
        section.setLayoutData( gd );

        section.setDescription( Messages.ResourcesSection_Description );
        Composite client = toolkit.createComposite( section );
        client.setLayout( LayoutFactory.createSectionGridLayout( false, 3 ) );
        section.setClient( client );

        build( client, toolkit );

        postBuild( client, toolkit );
        section.getChildren()[ 0 ].setToolTipText( Messages.ResourcesSection_ToolTip );
    }

    private void preBuild() {
        getSection().setText( Messages.ResourcesSection_Title );
        getEditor().addListener( new ResourcesPropertyChangeListener() );
    }

    /**
     * Response to resource bundle change event.
     *
     * @param resource
     *            The new resource bundle
     */
    protected void bundleChanged( RRHFile resource ) {
        if( resource != null ) {
            String[] resourceKeys = getProjectPropertiesPage().getResourceKeys( resource.getKeyTalbe() );
            _titleId_TextChoiceField.setItems( resourceKeys );
        } else {
            _titleId_TextChoiceField.removeAll();
        }
    }

    private void build( final Composite body, FormToolkit toolkit ) {
        Map< ControlType, Control > textInputControlList;

        _isTitleResourceAvailable_BooleanInputField = ControlFactory.buildCheckBoxControl( body, toolkit,
                Messages.ResourcesSection_Resource_Available_Label, Messages.ResourcesSection_Resource_Available_ToolTip, false,
                getProjectPropertiesPage().new DirtyListener( getProjectPropertiesPage().getSectionPartProperty( body ) ) );
        _isTitleResourceAvailable_BooleanInputField.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                validateProperties();
                updateControlStates();
            }
        } );

        SelectionAdapter resourceBundleSelectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent event ) {
                Object object = event.getSource();
                if( object instanceof Combo ) {
                    if( getResources() != null ) {
                        RRHFile resource = getResources().get( ( (Combo) object ).getText() );
                        bundleChanged( resource );
                        validateProperties();
                        updateControlStates();
                    }
                }
            }
        };

        SelectionAdapter titleSelectionAdapter = new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent event ) {
                validateProperties();
            }
        };

        textInputControlList = ControlFactory.buildComboBoxControl( body, toolkit, null, null,
                Messages.ResourcesSection_Resource_Bundle_Label, null, resourceBundleSelectionAdapter,
                getProjectPropertiesPage().new DirtyListener( getProjectPropertiesPage().getSectionPartProperty( body ) ) );
        _resourceBundleClassName_TextChoiceField = (Combo) textInputControlList.get( ControlType.COMBO );
        _resourceBundle_LabelField = (Label) textInputControlList.get( ControlType.LABEL );

        textInputControlList = ControlFactory.buildComboBoxControl( body, toolkit, null, null,
                Messages.ResourcesSection_Resource_Title_Label, null, titleSelectionAdapter,
                getProjectPropertiesPage().new DirtyListener( getProjectPropertiesPage().getSectionPartProperty( body ) ) );
        _titleId_TextChoiceField = (Combo) textInputControlList.get( ControlType.COMBO );
        _titleId_LabelField = (Label) textInputControlList.get( ControlType.LABEL );
    }

    private void postBuild( Composite body, FormToolkit toolkit ) {
        toolkit.paintBordersFor( body );
    }

    private class ResourcesPropertyChangeListener implements PropertyChangeListener {
        @Override
        public void propertyChange( PropertyChangeEvent evt ) {
            String property = evt.getPropertyName();
            if( property.equals( Messages.GeneralSection_Application_Type_Label ) ) {
                Object obj = evt.getNewValue();
                if( obj instanceof String ) {
                    setProjectType( (String) obj );
                    updateControlStates();
                }
            }
        }
    }
}
