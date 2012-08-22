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
import java.util.Map;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.ui.editors.model.factories.ControlFactory;
import net.rim.ejde.internal.ui.editors.model.factories.ControlFactory.ControlType;
import net.rim.ejde.internal.ui.editors.model.factories.LayoutFactory;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PreferenceUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.validation.BBDiagnostic;
import net.rim.ejde.internal.validation.BBPropertiesValidator;

import org.eclipse.swt.SWT;
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
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * This class creates the general section used in the project properties editor.
 *
 * @author jkeshavarzi
 *
 */
public class GeneralSection extends AbstractSection {
    public static final String[] projectTypeChoiceList = new String[] { BlackBerryProject.CLDC_APPLICATION,
            BlackBerryProject.MIDLET, BlackBerryProject.LIBRARY };
    private Label _title_LabelField, _startupTier_LabelField, _mainMIDletClassName_LabelField, _argumentsPassedToMain_LabelField,
            _homeScreenPosition_LabelField;
    private Text _title_TextInputField, _version_TextInputField, _vendor_TextInputField, _description_TextInputField,
            _mainMIDletClassName_TextInputField, _argumentsPassedToMain_TextInputField, _homeScreenPosition_TextInputField;
    private Combo _projectTypeField, _startupTier_TextChoiceField;
    private Button _systemModule_booleanField, _autoRun_booleanField;
    private static final String homeScreenKey = "home_screen_position_key"; //$NON-NLS-1$
    private boolean _previousSystemModuleValue;

    /**
     * This class creates the general section used in the project properties editor.
     *
     * @param page
     * @param parent
     * @param toolkit
     * @param style
     */
    public GeneralSection( BlackBerryProjectPropertiesPage page, Composite parent, FormToolkit toolkit, int style ) {
        super( page, parent, page.getManagedForm().getToolkit(), ( style | Section.DESCRIPTION | ExpandableComposite.TITLE_BAR ) );
        createFormContent( getSection(), toolkit );
    }

    protected void createFormContent( Section section, FormToolkit toolkit ) {
        preBuild();

        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, false );
        gd.minimumWidth = 250;
        section.setLayout( LayoutFactory.createClearGridLayout( false, 1 ) );
        section.setLayoutData( gd );

        section.setDescription( Messages.GeneralSection_Description );
        Composite client = toolkit.createComposite( section );
        client.setLayout( LayoutFactory.createSectionClientTableWrapLayout( false, 3 ) );
        section.setClient( client );

        build( client, toolkit );

        postBuild( client, toolkit );

    }

    /**
     * Update the controls within this section with values from the given properties object
     *
     * @param properties
     */
    public void insertControlValuesFromModel( BlackBerryProperties properties ) {
        String title = properties._general.getTitle();
        String version = properties._general.getVersion();
        String vendor = properties._general.getVendor();
        String desc = properties._general.getDescription();
        String projectType = properties._application.getType();
        String mainMidletName = properties._application.getMainMIDletName();
        String mainArgs = properties._application.getMainArgs();
        Integer homeScreenPosition = properties._application.getHomeScreenPosition();
        Integer startUpTier = properties._application.getStartupTier();
        Boolean systemModule = properties._application.isSystemModule();
        Boolean autoRun = properties._application.isAutostartup();

        if( title != null ) {
            _title_TextInputField.setText( title );
        }
        if( version != null ) {
            _version_TextInputField.setText( version );
        }
        if( vendor != null ) {
            _vendor_TextInputField.setText( vendor );
        }
        if( desc != null ) {
            _description_TextInputField.setText( desc );
        }
        if( projectType != null ) {
            String oldValue = _projectTypeField.getText();
            if( !oldValue.equals( projectType ) ) {
                _projectTypeField.setText( projectType );
                getEditor().notifyListeners( Messages.GeneralSection_Application_Type_Label, oldValue, projectType );
            }
        }
        if( mainMidletName != null ) {
            _mainMIDletClassName_TextInputField.setText( mainMidletName );
        }
        if( mainArgs != null ) {
            _argumentsPassedToMain_TextInputField.setText( mainArgs );
        }
        if( homeScreenPosition != null ) {
            _homeScreenPosition_TextInputField.setText( homeScreenPosition.toString() );
        }
        if( startUpTier != null ) {
            _startupTier_TextChoiceField.setText( startUpTier.toString() );
        }
        if( systemModule != null ) {
            setIsSystemModule( systemModule );
        }
        if( autoRun != null ) {
            _autoRun_booleanField.setSelection( autoRun );
        }

        updateProjectTypeSpecificControls();
    }

    private void preBuild() {
        getSection().setText( Messages.GeneralSection_Title );
        getEditor().addListener( new GeneralSectionPropertyChangeListener() );
    }

    private void build( Composite body, FormToolkit toolkit ) {
        Map< ControlType, Control > controlList;

        BlackBerryProjectPropertiesPage propertiesPage = getProjectPropertiesPage();
        BlackBerryProperties properties = propertiesPage.getBlackBerryProject().getProperties();

        String title = properties._general.getTitle();
        String version = properties._general.getVersion();
        String vendor = properties._general.getVendor();
        String desc = properties._general.getDescription();
        String projectType = properties._application.getType();
        String mainMidletName = properties._application.getMainMIDletName();
        String mainArgs = properties._application.getMainArgs();
        Integer homeScreenPosition = properties._application.getHomeScreenPosition();
        Integer startUpTier = Math.max( properties._application.getStartupTier(), ProjectUtils.getStartupTiers()[ 0 ] );
        Boolean systemModule = properties._application.isSystemModule();
        Boolean autoRun = properties._application.isAutostartup();

        // Create title control
        controlList = ControlFactory.buildTextWithLabelControl( body, toolkit, Messages.GeneralSection_Title_Label, title,
                Messages.GeneralSection_Title_ToolTip, propertiesPage.new DirtyListener( this ), null );
        _title_LabelField = (Label) controlList.get( ControlType.LABEL );
        _title_TextInputField = (Text) controlList.get( ControlType.TEXT );
        _title_TextInputField.addFocusListener( getProjectPropertiesPage().new ProjectPropertiesFocusListener() );

        // Create version control
        controlList = ControlFactory.buildTextWithLabelControl( body, toolkit, Messages.GeneralSection_Version_Label, version,
                Messages.GeneralSection_Version_ToolTip, propertiesPage.new DirtyListener( this ), null );
        _version_TextInputField = (Text) controlList.get( ControlType.TEXT );
        _version_TextInputField.addFocusListener( getProjectPropertiesPage().new ProjectPropertiesFocusListener() );

        // Create vendor control
        controlList = ControlFactory.buildTextWithLabelControl( body, toolkit, Messages.GeneralSection_Vendor_Label, vendor,
                Messages.GeneralSection_Vendor_ToolTip, propertiesPage.new DirtyListener( this ), null );
        _vendor_TextInputField = (Text) controlList.get( ControlType.TEXT );
        _vendor_TextInputField.addFocusListener( getProjectPropertiesPage().new ProjectPropertiesFocusListener() );

        // Create description control
        controlList = ControlFactory.buildTextWithLabelControl( body, toolkit, Messages.GeneralSection_Description_Label, desc,
                null, propertiesPage.new DirtyListener( this ), null );
        _description_TextInputField = (Text) controlList.get( ControlType.TEXT );
        _description_TextInputField.addFocusListener( getProjectPropertiesPage().new ProjectPropertiesFocusListener() );

        // Create project type control
        controlList = ControlFactory.buildComboBoxControl( body, toolkit, projectTypeChoiceList, projectType,
                Messages.GeneralSection_Application_Type_Label, Messages.GeneralSection_Application_Type_ToolTip, null,
                propertiesPage.new DirtyListener( this ) );
        _projectTypeField = (Combo) controlList.get( ControlType.COMBO );
        _projectTypeField.addSelectionListener( new SelectionListener() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                Object obj = e.getSource();
                if( obj instanceof Combo ) {
                    Combo combo = (Combo) obj;
                    getEditor().notifyListeners( Messages.GeneralSection_Application_Type_Label, null, combo.getText() );
                }
            }

            @Override
            public void widgetDefaultSelected( SelectionEvent e ) {
                widgetSelected( e );
            }
        } );

        // Create main MIDlet class name control
        controlList = ControlFactory.buildTextWithLabelControl( body, toolkit, Messages.GeneralSection_Main_Midlet_Label,
                mainMidletName, null, propertiesPage.new DirtyListener( this ), null );
        _mainMIDletClassName_LabelField = (Label) controlList.get( ControlType.LABEL );
        _mainMIDletClassName_TextInputField = (Text) controlList.get( ControlType.TEXT );

        // Create arguments passed to main control
        controlList = ControlFactory.buildTextWithLabelControl( body, toolkit,
                Messages.GeneralSection_Application_Argument_Label, mainArgs, null, propertiesPage.new DirtyListener( this ),
                null );
        _argumentsPassedToMain_LabelField = (Label) controlList.get( ControlType.LABEL );
        _argumentsPassedToMain_TextInputField = (Text) controlList.get( ControlType.TEXT );

        // Create home screen position control
        controlList = ControlFactory.buildTextWithLabelControl( body, toolkit,
                Messages.GeneralSection_Home_Screen_Position_Label, homeScreenPosition.toString(), null,
                propertiesPage.new DirtyListener( this ), null );
        _homeScreenPosition_LabelField = (Label) controlList.get( ControlType.LABEL );
        _homeScreenPosition_TextInputField = (Text) controlList.get( ControlType.TEXT );
        _homeScreenPosition_TextInputField.addModifyListener( new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
                validateHomeScreenPosition();
            }
        } );
        switchHomeScreenPositionVisibility();

        // Create autorun control
        _autoRun_booleanField = ControlFactory.buildCheckBoxControl( body, toolkit, Messages.GeneralSection_Auto_Run_Label, null,
                autoRun, propertiesPage.new DirtyListener( this ) );
        _autoRun_booleanField.addSelectionListener( new SelectionListener() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                updateProjectTypeSpecificControls();
            }

            @Override
            public void widgetDefaultSelected( SelectionEvent e ) {
                widgetSelected( e );
            }
        } );

        // Create startup tier control
        controlList = ControlFactory.buildComboBoxControl( body, toolkit, ProjectUtils.getStartupTierStrings(), startUpTier
                .toString(), Messages.GeneralSection_Startup_Tier_Label, null, null, propertiesPage.new DirtyListener( this ) );
        _startupTier_LabelField = (Label) controlList.get( ControlType.LABEL );
        _startupTier_TextChoiceField = (Combo) controlList.get( ControlType.COMBO );

        // Create system module control
        _systemModule_booleanField = ControlFactory.buildCheckBoxControl( body, toolkit,
                Messages.GeneralSection_System_Module_Label, null, systemModule, propertiesPage.new DirtyListener( this ) );
        _systemModule_booleanField.setToolTipText( Messages.GeneralSection_System_Module_ToolTip );
        setIsSystemModule( systemModule );

        getEditor().notifyListeners( Messages.GeneralSection_Application_Type_Label, IConstants.EMPTY_STRING, projectType );
    }

    private void postBuild( Composite client, FormToolkit toolkit ) {
        toolkit.paintBordersFor( client );
        validateHomeScreenPosition();
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
        _homeScreenPosition_LabelField.setVisible( visible );
        _homeScreenPosition_TextInputField.setVisible( visible );
    }

    @Override
    public void commit( boolean onSave ) {
        super.commit( onSave );

        BlackBerryProperties properties = getProjectPropertiesPage().getBlackBerryProject().getProperties();

        properties._general.setTitle( _title_TextInputField.getText() );
        properties._general.setVersion( _version_TextInputField.getText() );
        properties._general.setVendor( _vendor_TextInputField.getText() );
        properties._general.setDescription( _description_TextInputField.getText() );
        properties._application.setType( _projectTypeField.getText() );
        properties._application.setMainMIDletName( _mainMIDletClassName_TextInputField.getText() );
        properties._application.setMainArgs( _argumentsPassedToMain_TextInputField.getText() );
        properties._application.setHomeScreenPosition( getHomeScreenPosition() );
        properties._application.setIsSystemModule( _systemModule_booleanField.getSelection() );
        properties._application.setIsAutostartup( _autoRun_booleanField.getSelection() );
        properties._application.setStartupTier( getStartupTier() );
    }

    private void validateHomeScreenPosition() {
        String homeScreenPosition = _homeScreenPosition_TextInputField.getText();
        BBDiagnostic diag = BBPropertiesValidator.validateHasValue( homeScreenPosition );

        if( diag.getSeverity() == BBDiagnostic.ERROR ) {
            getProjectPropertiesPage().createEditorErrorMarker( homeScreenKey, diag.getMessage(),
                    _homeScreenPosition_TextInputField );
        } else {
            diag = BBPropertiesValidator.validateHomeScreenPosition( homeScreenPosition );

            if( diag.getSeverity() == BBDiagnostic.ERROR ) {
                getProjectPropertiesPage().createEditorErrorMarker( homeScreenKey, diag.getMessage(),
                        _homeScreenPosition_TextInputField );
            } else {
                getProjectPropertiesPage().removeEditorErrorMarker( homeScreenKey, _homeScreenPosition_TextInputField );
            }
        }
    }

    private void setEnabled( Boolean enabled ) {
        _title_TextInputField.setEnabled( enabled );
        _title_LabelField.setEnabled( enabled );
        _mainMIDletClassName_TextInputField.setEnabled( enabled );
        _mainMIDletClassName_LabelField.setEnabled( enabled );
        _argumentsPassedToMain_TextInputField.setEnabled( enabled );
        _argumentsPassedToMain_LabelField.setEnabled( enabled );
        _homeScreenPosition_TextInputField.setEnabled( enabled );
        _homeScreenPosition_LabelField.setEnabled( enabled );
        _systemModule_booleanField.setEnabled( enabled );
        _autoRun_booleanField.setEnabled( enabled );
        _startupTier_TextChoiceField.setEnabled( enabled );
        _startupTier_LabelField.setEnabled( enabled );
    }

    private void updateProjectTypeSpecificControls() {
        String applicationType = _projectTypeField.getText().trim();

        if( applicationType.equals( BlackBerryProject.CLDC_APPLICATION ) ) {
            setEnabled( true );
            _mainMIDletClassName_TextInputField.setEnabled( false );
            _mainMIDletClassName_LabelField.setEnabled( false );
            if( _autoRun_booleanField.getSelection() ) {
                _startupTier_TextChoiceField.setEnabled( true );
                _startupTier_LabelField.setEnabled( true );
            } else {
                _startupTier_TextChoiceField.setEnabled( false );
                _startupTier_LabelField.setEnabled( false );
            }
            restoreIsSystemModule();
        } else if( applicationType.equals( BlackBerryProject.MIDLET ) ) {
            setEnabled( true );
            _argumentsPassedToMain_TextInputField.setEnabled( false );
            _argumentsPassedToMain_LabelField.setEnabled( false );
            _autoRun_booleanField.setEnabled( false );
            _startupTier_TextChoiceField.setEnabled( false );
            _startupTier_LabelField.setEnabled( false );
            restoreIsSystemModule();
        } else if( applicationType.equals( BlackBerryProject.LIBRARY ) ) {
            setEnabled( false );
            _autoRun_booleanField.setEnabled( true );
            if( _autoRun_booleanField.getSelection() ) {
                _startupTier_TextChoiceField.setEnabled( true );
                _startupTier_LabelField.setEnabled( true );
            } else {
                _startupTier_TextChoiceField.setEnabled( false );
                _startupTier_LabelField.setEnabled( false );
            }
            setIsSystemModule( true );
        }
    }

    private Integer getHomeScreenPosition() {
        String text = _homeScreenPosition_TextInputField.getText();
        if( !BBPropertiesValidator.isParsableInt( text ) ) {
            return 0;
        }
        return Integer.decode( _homeScreenPosition_TextInputField.getText() );
    }

    private Integer getStartupTier() {
        return Integer.valueOf( _startupTier_TextChoiceField.getText() );
    }

    private void setIsSystemModule( boolean value ) {
        _previousSystemModuleValue = _systemModule_booleanField.getSelection();
        _systemModule_booleanField.setSelection( value );
    }

    private void restoreIsSystemModule() {
        _systemModule_booleanField.setSelection( _previousSystemModuleValue );
    }

    /**
     * @return The Project type selected by the user from the view.
     */
    public String getProjectType() {
        return _projectTypeField.getText();
    }

    private class GeneralSectionPropertyChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange( PropertyChangeEvent evt ) {
            String property = evt.getPropertyName();
            if( property.equals( Messages.GeneralSection_Application_Type_Label ) ) {
                Object obj = evt.getNewValue();
                if( obj instanceof String ) {
                    updateProjectTypeSpecificControls();
                }
            }
        }
    }
}
