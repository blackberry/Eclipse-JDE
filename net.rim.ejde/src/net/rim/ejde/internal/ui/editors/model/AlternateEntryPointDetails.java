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
import java.util.List;
import java.util.Map;

import net.rim.ejde.internal.model.BasicBlackBerryProperties.AlternateEntryPoint;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.ui.editors.model.factories.ControlFactory;
import net.rim.ejde.internal.ui.editors.model.factories.ControlFactory.ControlType;
import net.rim.ejde.internal.ui.editors.model.factories.LayoutFactory;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PreferenceUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.validation.BBDiagnostic;
import net.rim.ejde.internal.validation.BBPropertiesValidator;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * This class creates the details section on the alternate entry point master-details page.
 *
 * @author jkeshavarzi
 *
 */
public class AlternateEntryPointDetails extends AbstractFormPart implements IDetailsPage {
    private AlternateEntryPoint _aep;
    private AlternateEntryPointSection _fMasterSection;
    private Section _detailSection;
    private BlackBerryProjectAlternateEntryPointPage _bbPage;
    private BlackBerryProjectFormEditor _editor;
    private String _projectType;

    private Text _titleField, _argsField, _screenPositionField, _mainMIDletClassName_TextInputField;
    private Button _isAutoStartUpField;

    // Startup
    private Combo _startupTier_TextChoiceField;

    // System module
    private Button _systemModule_booleanField;

    // Locale Resources Section
    private AlternateEntryPointResourcesSection _resourcesSection;

    // Application arguments section
    private Label _argsLabel, _titleLabel, _screenPositionLabel, _startupTier_LabelField, _mainMIDletClassName_LabelField;
    private final String HOME_SCREEN_KEY = "aep_home_screen_position_key"; //$NON-NLS-1$

    // Icons Section
    private AlternateEntryPointIconsSection _iconsSection;

    /**
     * @param masterSection
     */
    public AlternateEntryPointDetails( AlternateEntryPointSection masterSection ) {
        _fMasterSection = masterSection;
        _bbPage = (BlackBerryProjectAlternateEntryPointPage) _fMasterSection.getProjectPropertiesPage();
        _editor = (BlackBerryProjectFormEditor) _bbPage.getEditor();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.forms.IDetailsPage#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createContents( Composite parent ) {
        Map< ControlType, Control > controlList;

        FormToolkit toolkit = getManagedForm().getToolkit();

        _projectType = _bbPage.getBlackBerryProject().getProperties()._application.getType();

        _editor.addListener( new PropertyListener() );

        parent.setLayout( LayoutFactory.createDetailsGridLayout( false, 1 ) );

        _detailSection = toolkit.createSection( parent, ExpandableComposite.TITLE_BAR | Section.DESCRIPTION );
        _detailSection.clientVerticalSpacing = 6;
        _detailSection.setText( Messages.AlternateEntryPointDetails_Title );
        _detailSection.setDescription( Messages.AlternateEntryPointDetails_Description );
        _detailSection.setLayout( LayoutFactory.createClearGridLayout( false, 1 ) );
        _detailSection.setLayoutData( new GridData( GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING ) );

        Composite client = toolkit.createComposite( _detailSection );
        client.setLayout( LayoutFactory.createSectionClientGridLayout( false, 3 ) );
        client.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

        toolkit.paintBordersFor( client );
        _detailSection.setClient( client );

        markDetailsPart( _detailSection );

        controlList = ControlFactory.buildTextWithLabelControl( client, toolkit, Messages.GeneralSection_Title_Label, null,
                Messages.AlternateEntryPointDetailsSection_Title_ToolTip,
                _bbPage.new DirtyListener( _bbPage.getSectionPartProperty( parent ) ), null );
        _titleField = (Text) controlList.get( ControlType.TEXT );
        _titleLabel = (Label) controlList.get( ControlType.LABEL );
        _titleField.addModifyListener( new DirtyListener() );
        _titleField.addFocusListener( new FocusAdapter() {
            @Override
            public void focusLost( FocusEvent e ) {
                updateAepTitle();
                _fMasterSection.getAlternateEntryPointsTableViewer().refresh();
            }
        } );

        controlList = ControlFactory.buildTextWithLabelControl( client, toolkit, Messages.GeneralSection_Main_Midlet_Label, null,
                null, _bbPage.new DirtyListener( _bbPage.getSectionPartProperty( parent ) ), null );
        _mainMIDletClassName_TextInputField = (Text) controlList.get( ControlType.TEXT );
        _mainMIDletClassName_LabelField = (Label) controlList.get( ControlType.LABEL );
        _mainMIDletClassName_TextInputField.addModifyListener( new DirtyListener() );

        controlList = ControlFactory.buildTextWithLabelControl( client, toolkit,
                Messages.GeneralSection_Application_Argument_Label, null, null,
                _bbPage.new DirtyListener( _bbPage.getSectionPartProperty( parent ) ), null );
        _argsField = (Text) controlList.get( ControlType.TEXT );
        _argsLabel = (Label) controlList.get( ControlType.LABEL );
        _argsField.addModifyListener( new DirtyListener() );

        controlList = ControlFactory.buildTextWithLabelControl( client, toolkit,
                Messages.GeneralSection_Home_Screen_Position_Label, null, null,
                _bbPage.new DirtyListener( _bbPage.getSectionPartProperty( parent ) ), null );
        _screenPositionField = (Text) controlList.get( ControlType.TEXT );
        _screenPositionLabel = (Label) controlList.get( ControlType.LABEL );
        _screenPositionField.addModifyListener( new DirtyListener() );
        _screenPositionField.addModifyListener( new ModifyListener() {
            @Override
            public void modifyText( ModifyEvent e ) {
                validateHomeScreenPosition();
            }
        } );
        switchHomeScreenPositionVisibility();

        _isAutoStartUpField = ControlFactory.buildCheckBoxControl( client, toolkit, Messages.GeneralSection_Auto_Run_Label, null,
                null, _bbPage.new DirtyListener( _bbPage.getSectionPartProperty( parent ) ) );
        _isAutoStartUpField.addSelectionListener( new DirtyListener() );
        _isAutoStartUpField.addSelectionListener( new SelectionListener() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                updateControlStates();
            }

            @Override
            public void widgetDefaultSelected( SelectionEvent e ) {
                updateControlStates();
            }
        } );

        controlList = ControlFactory.buildComboBoxControl( client, toolkit, ProjectUtils.getStartupTierStrings(), null,
                Messages.GeneralSection_Startup_Tier_Label, null, null,
                _bbPage.new DirtyListener( _bbPage.getSectionPartProperty( parent ) ) );
        _startupTier_TextChoiceField = (Combo) controlList.get( ControlType.COMBO );
        _startupTier_LabelField = (Label) controlList.get( ControlType.LABEL );
        _startupTier_TextChoiceField.addModifyListener( new DirtyListener() );

        _systemModule_booleanField = ControlFactory.buildCheckBoxControl( client, toolkit,
                Messages.GeneralSection_System_Module_Label, Messages.GeneralSection_System_Module_ToolTip, null,
                _bbPage.new DirtyListener( _bbPage.getSectionPartProperty( parent ) ) );
        _systemModule_booleanField.addSelectionListener( new DirtyListener() );

        ControlFactory.insertLine( client, toolkit );

        _resourcesSection = new AlternateEntryPointResourcesSection( _bbPage, client, toolkit, 0 );
        _resourcesSection.getSection().setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false, 3, 0 ) );

        ControlFactory.insertLine( client, toolkit );

        _iconsSection = new AlternateEntryPointIconsSection( _bbPage, client, toolkit, 0 );
        _iconsSection.getSection().setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false, 3, 0 ) );
        _iconsSection.getSection().setText( Messages.BlackBerryProjectPropertiesPage_Table_Title );

        updateControls();
        updateControlStates();
    }

    public boolean isDirty() {
    	return super.isDirty() || _resourcesSection.isDirty() || _iconsSection.isDirty();
    }

    public AlternateEntryPointResourcesSection getResourcesSection() {
        return this._resourcesSection;
    }

    private void switchHomeScreenPositionVisibility() {
        // This will be changed later to three different cases
        // Case 0 : don't show the field
        // Case 1: show only for BB internal projects
        // Case 2: always show
        switch( PreferenceUtils.getDefaultVisibleHomeScreenPosition() ) {
            case 0:
                setHomeScreenPositionVisibility( false );
                break;
            case 1:
                setHomeScreenPositionVisibility( true );
                break;
        }
    }

    private void setHomeScreenPositionVisibility( boolean visible ) {
        _screenPositionField.setVisible( visible );
        _screenPositionLabel.setVisible( visible );
    }

    private void validateHomeScreenPosition() {
        String homeScreenPosition = _screenPositionField.getText();
        BBDiagnostic diag = BBPropertiesValidator.validateHasValue( homeScreenPosition );
        String msg = ""; //$NON-NLS-1$

        if( diag.getSeverity() == BBDiagnostic.ERROR ) {
            msg = "(" + _aep.getTitle() + ")" + diag.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$
            _bbPage.createEditorErrorMarker( createUniquePrefix() + HOME_SCREEN_KEY, msg, _screenPositionField );
        } else {
            diag = BBPropertiesValidator.validateHomeScreenPosition( homeScreenPosition );
            msg = "(" + _aep.getTitle() + ")" + diag.getMessage(); //$NON-NLS-1$ //$NON-NLS-2$

            if( diag.getSeverity() == BBDiagnostic.ERROR ) {
                _bbPage.createEditorErrorMarker( createUniquePrefix() + HOME_SCREEN_KEY, msg, _screenPositionField );
            } else {
                _bbPage.removeEditorErrorMarker( createUniquePrefix() + HOME_SCREEN_KEY, _screenPositionField );
            }
        }
    }

    protected String createUniquePrefix() {
        return _aep.getTitle() + ": "; //$NON-NLS-1$
    }

    private void insertControlValues() {
        if( _aep != null ) {
            Boolean wasDirty = _editor.isDirty();

            String title = _aep.getTitle();
            String mainMidletClass = _aep.getMainMIDletName();
            String args = _aep.getArgsPassedToMain();
            String screenPosition = _aep.getHomeScreenPosition().toString();
            Boolean autoStart = _aep.isAutostartup();
            Integer startUpTier = _aep.getStartupTier();
            Boolean systemModule = _aep.isSystemModule();

            _titleField.setText( title );
            _mainMIDletClassName_TextInputField.setText( mainMidletClass );
            _argsField.setText( args );
            _screenPositionField.setText( screenPosition );
            _isAutoStartUpField.setSelection( autoStart );

            if( startUpTier != null ) {
                _startupTier_TextChoiceField.setText( String.valueOf( startUpTier ) );
            }

            if( systemModule != null ) {
                _systemModule_booleanField.setSelection( systemModule );
            }

            _resourcesSection.insertControlValuesFromModel();
            _iconsSection.insertControlValuesFromModel();

            _resourcesSection.refreshControls( true );

            if( !wasDirty ) {
                // Temp fix - Editor becomes dirty when we insert values into UI.
                _editor.setDirty( false );
            }

            validateHomeScreenPosition();
        } else {
            _editor.setDirty( false );
        }
    }

    private void updateControlStates() {
        Boolean isLibrary = _projectType.equals( BlackBerryProject.LIBRARY );
        Boolean enableStartupTier = _isAutoStartUpField.getSelection() && !isLibrary;
        _startupTier_LabelField.setEnabled( enableStartupTier );
        _startupTier_TextChoiceField.setEnabled( enableStartupTier );
        _resourcesSection.updateControlStates();
    }

    /**
     * Updates the control states (enabled/disabled) within this details part based on the passed in application type
     *
     * @param applicationType
     */
    public void updateApplicationTypeControls( String applicationType ) {
        if( applicationType.equals( BlackBerryProject.LIBRARY ) ) {
            enableControls( false );
        } else {
            enableControls( true );

            if( applicationType.equals( BlackBerryProject.CLDC_APPLICATION ) ) {
                _startupTier_LabelField.setEnabled( _isAutoStartUpField.getSelection() );
                _startupTier_TextChoiceField.setEnabled( _isAutoStartUpField.getSelection() );
                _mainMIDletClassName_LabelField.setEnabled( false );
                _mainMIDletClassName_TextInputField.setEnabled( false );
            } else if( applicationType.equals( BlackBerryProject.MIDLET ) ) {
                _argsLabel.setEnabled( false );
                _argsField.setEnabled( false );
                _isAutoStartUpField.setEnabled( false );
                _startupTier_LabelField.setEnabled( false );
                _startupTier_TextChoiceField.setEnabled( false );
            }
        }
    }

    protected void enableControls( Boolean enabled ) {
        _titleLabel.setEnabled( enabled );
        _titleField.setEnabled( enabled );
        _mainMIDletClassName_LabelField.setEnabled( enabled );
        _mainMIDletClassName_TextInputField.setEnabled( enabled );
        _argsLabel.setEnabled( enabled );
        _argsField.setEnabled( enabled );
        _screenPositionLabel.setEnabled( enabled );
        _screenPositionField.setEnabled( enabled );
        _isAutoStartUpField.setEnabled( enabled );
        _startupTier_LabelField.setEnabled( enabled );
        _startupTier_TextChoiceField.setEnabled( enabled );
        _systemModule_booleanField.setEnabled( enabled );
        _iconsSection.getIconsSubSection().setEnabled( enabled );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.forms.IPartSelectionListener#selectionChanged(org.eclipse.ui.forms.IFormPart,
     * org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void selectionChanged( IFormPart part, ISelection selection ) {
        if( part instanceof AlternateEntryPointSection ) {
            ( (AlternateEntryPointSection) part ).getAlternateEntryPointsTableViewer().refresh();
        }
        IStructuredSelection sel = (IStructuredSelection) selection;
        Object element = sel.getFirstElement();

        if( element instanceof AlternateEntryPoint ) {
            _aep = (AlternateEntryPoint) element;
            _resourcesSection.setAlternateEntryPoint( _aep );
            _iconsSection.setAlternateEntryPoint( _aep );
        }

        update();
        updateControls();
    }

    private void updateAepTitle() {
        List< String > aepTitles = _fMasterSection.getAepTitles();
        String aepTitle = StringUtils.trim( _titleField.getText() );
        if( !StringUtils.isBlank( aepTitle ) && !aepTitles.contains( aepTitle ) ) {
            _aep.setTitle( aepTitle );
        }
    }

    private void saveState( boolean onSave ) {
        if( _aep != null ) {
            updateAepTitle();

            _aep.setMainMIDletName( _mainMIDletClassName_TextInputField.getText() );
            _aep.setArgsPassedToMain( _argsField.getText() );

            String txt = _screenPositionField.getText();
            if( !StringUtils.isEmpty( txt ) && BBPropertiesValidator.isParsableInt( txt ) ) {
                _aep.setHomeScreenPosition( Integer.parseInt( txt ) );
            }
            _aep.setIsSystemModule( _systemModule_booleanField.getSelection() );
            _aep.setIsAutostartup( _isAutoStartUpField.getSelection() );

            txt = _startupTier_TextChoiceField.getText();
            if( !StringUtils.isEmpty( txt ) ) {
                _aep.setStartupTier( Integer.parseInt( txt ) );
            }
            _resourcesSection.commit( onSave );
            _iconsSection.commit( onSave );
        }
    }

    /**
     * Updates the section/control states (enabled/disabled) based on the application project type
     *
     */
    protected void updateControls() {
        if( _projectType.equals( BlackBerryProject.LIBRARY ) ) {
            _fMasterSection.setEnabled( false );
            _detailSection.setEnabled( false );
            enableControls( false );
        } else {
            _fMasterSection.setEnabled( true );
            _detailSection.setEnabled( true );
            updateApplicationTypeControls( _projectType );
            updateControlStates();
        }
    }

    private void update() {
        insertControlValues();
    }

    @Override
    public void setFocus() {
        _titleField.setFocus();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
     */
    @Override
    public void commit( boolean onSave ) {
        saveState( onSave );
        super.commit( false );
        _fMasterSection.getAlternateEntryPointsTableViewer().refresh();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
     */
    @Override
    public void refresh() {
        update();
        super.refresh();
    }

    protected void markDetailsPart( Control control ) {
        control.setData( BlackBerryProjectPropertiesPage.SECTION_PART_KEY, this );
    }

    /**
     * @return The AlternateEntryPointSection object pulled from the Managed FOrm
     */
    public AlternateEntryPointSection getPage() {
        return (AlternateEntryPointSection) getManagedForm().getContainer();
    }

    /**
     * @return The currently selected AlternateEntryPoint object
     */
    public AlternateEntryPoint getCurrentAep() {
        return _aep;
    }

    /**
     * Returns the icons section
     *
     * @return The icons section
     */
    public AlternateEntryPointIconsSection getIconsSection() {
        return this._iconsSection;
    }

    private class DirtyListener implements ModifyListener, SelectionListener {
        @Override
        public void modifyText( ModifyEvent e ) {
            markDirty();
        }

        @Override
        public void widgetDefaultSelected( SelectionEvent e ) {
            markDirty();
        }

        @Override
        public void widgetSelected( SelectionEvent e ) {
            markDirty();
        }
    }

    private class PropertyListener implements PropertyChangeListener {
        public void propertyChange( PropertyChangeEvent evt ) {
            String property = evt.getPropertyName();
            if( property.equals( Messages.GeneralSection_Application_Type_Label ) ) {
                Object obj = evt.getNewValue();
                if( obj instanceof String ) {
                    _projectType = (String) obj;
                    updateControlStates();
                    updateControls();
                }
            }
        }
    }
}
