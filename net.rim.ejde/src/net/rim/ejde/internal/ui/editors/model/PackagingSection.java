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

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.ui.editors.model.factories.ControlFactory;
import net.rim.ejde.internal.ui.editors.model.factories.ControlFactory.ControlType;
import net.rim.ejde.internal.ui.editors.model.factories.LayoutFactory;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.validation.BBDiagnostic;
import net.rim.ejde.internal.validation.BBPropertiesValidator;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * This class creates the packaging section used in the project properties editor.
 *
 * @author jkeshavarzi
 *
 */
public class PackagingSection extends AbstractSection implements PropertyChangeListener {
    private Text _name_TextField, _folder_TextField, _preStep_TextField, _postStep_TextField, _cleanStep_TextField;
    private Button _outputCompilerMessages_Button;
    private Button _convertImages_Button;
    private Button _createWarning_Button;
    private Button _compressResources_Button;
    private Button _generateALXFile_Button;
    private Text _aliasListText;
    private static final String filenameKey = "file_name_key"; //$NON-NLS-1$
    private static final String folderKey = "folder_key"; //$NON-NLS-1$

    /**
     * Constructs the PackagingSection on the given parent composite.
     *
     * @param page
     * @param parent
     * @param toolkit
     * @param style
     */
    public PackagingSection( BlackBerryProjectPropertiesPage page, Composite parent, FormToolkit toolkit, int style ) {
        super( page, parent, page.getManagedForm().getToolkit(), ( style | Section.DESCRIPTION | ExpandableComposite.TITLE_BAR ) );
        createFormContent( getSection(), toolkit );
        getEditor().addListener( this );
    }

    protected void createFormContent( Section section, FormToolkit toolkit ) {
        preBuild();

        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, false );
        gd.minimumWidth = 250;
        section.setLayout( LayoutFactory.createClearGridLayout( false, 1 ) );
        section.setLayoutData( gd );

        section.setDescription( Messages.PackagingSection_Description );
        Composite client = toolkit.createComposite( section );
        client.setLayout( LayoutFactory.createSectionGridLayout( true, 2 ) );
        section.setClient( client );
        build( client, toolkit );

        postBuild( client, toolkit );
    }

    private void preBuild() {
        getSection().setText( Messages.PackagingSection_Title );
    }

    private void build( final Composite body, FormToolkit toolkit ) {
        Map< ControlType, Control > controlList;

        BlackBerryProjectPropertiesPage page = getProjectPropertiesPage();
        BlackBerryProperties properties = page.getBlackBerryProject().getProperties();

        String outputFileName = properties._packaging.getOutputFileName();
        String outputFileFolder = properties._packaging.getOutputFolder();
        String preBuildStep = properties._packaging.getPreBuildStep();
        String postBuildStep = properties._packaging.getPostBuildStep();
        String cleanStep = properties._packaging.getCleanStep();
        Boolean outputCompilerMessages = properties._compile.getOutputCompilerMessages();
        Boolean convertImages = properties._compile.getConvertImages();
        Boolean createWarning = properties._compile.getCreateWarningForNoExportedRoutine();
        Boolean compressResources = properties._compile.getCompressResources();
        Boolean generateALXFile = properties._packaging.getGenerateALXFile();
        String aliasList = properties._compile.getAliasList();

        Composite left = toolkit.createComposite( body );
        GridData leftgd = new GridData( SWT.FILL, SWT.FILL, true, false );
        leftgd.minimumWidth = 250;
        left.setLayoutData( leftgd );
        left.setLayout( LayoutFactory.createZeroMarginGridLayout( false, 3 ) );

        Composite right = toolkit.createComposite( body );
        GridData rightgd = new GridData( SWT.FILL, SWT.FILL, true, false );
        rightgd.minimumWidth = 250;
        GridLayout leftgl = LayoutFactory.createZeroMarginGridLayout( false, 3 );
        leftgl.marginLeft = 20;
        right.setLayoutData( rightgd );
        right.setLayout( leftgl );

        controlList = ControlFactory.buildTextWithLabelControl( left, toolkit, Messages.PackagingSection_File_Name_Label,
                outputFileName, null, page.new DirtyListener( this ), null );
        setNameTextField( (Text) controlList.get( ControlType.TEXT ) );
        getNameTextField().addModifyListener( new ModifyListener() {
            @Override
            public void modifyText( ModifyEvent e ) {
                validateHasValidCharacters( getNameTextField() );
            }
        } );

        controlList = ControlFactory.buildTextWithLabelControl( left, toolkit, Messages.PackagingSection_Folder_Label,
                outputFileFolder, null, page.new DirtyListener( this ), null );
        setFolderTextField( (Text) controlList.get( ControlType.TEXT ) );
        getFolderTextField().addModifyListener( new ModifyListener() {
            @Override
            public void modifyText( ModifyEvent e ) {
                validateHasValidCharacters( getFolderTextField() );
            }
        } );

        int projBuildExtraSteps = ContextManager.getDefault().getPreferenceStore().getInt( "project_build_extra_steps" ); //$NON-NLS-1$
        if( projBuildExtraSteps > 0 ) {
            controlList = ControlFactory.buildTextWithLabelControl( left, toolkit,
                    Messages.PackagingSection_Pre_Build_Step_Label, preBuildStep, null, page.new DirtyListener( this ), null );
            _preStep_TextField = (Text) controlList.get( ControlType.TEXT );

            controlList = ControlFactory.buildTextWithLabelControl( left, toolkit,
                    Messages.PackagingSection_Post_Build_Step_Label, postBuildStep, null, page.new DirtyListener( this ), null );
            _postStep_TextField = (Text) controlList.get( ControlType.TEXT );

            controlList = ControlFactory.buildTextWithLabelControl( left, toolkit, Messages.PackagingSection_Clean_Step_Label,
                    cleanStep, null, page.new DirtyListener( this ), null );
            _cleanStep_TextField = (Text) controlList.get( ControlType.TEXT );
        }

        controlList = ControlFactory.buildTextWithLabelControl( left, toolkit, Messages.CompileSection_Alias_List_Label,
                aliasList, Messages.CompileSection_Alias_List_ToolTip, page.new DirtyListener( getProjectPropertiesPage()
                        .getSectionPartProperty( body ) ), null );
        _aliasListText = (Text) controlList.get( ControlType.TEXT );
        _aliasListText.setEnabled( properties._application.getType().equals( BlackBerryProject.LIBRARY ) );

        _outputCompilerMessages_Button = ControlFactory.buildCheckBoxControl( right, toolkit,
                Messages.CompileSection_Output_Messages_Label, null, outputCompilerMessages, page.new DirtyListener( this ) );
        _convertImages_Button = ControlFactory.buildCheckBoxControl( right, toolkit,
                Messages.CompileSection_Convert_Images_Label, null, convertImages, page.new DirtyListener( this ) );
        _createWarning_Button = ControlFactory.buildCheckBoxControl( right, toolkit,
                Messages.CompileSection_Create_Warning_Label, null, createWarning, page.new DirtyListener( this ) );
        _createWarning_Button.setEnabled( properties._application.getType().equals( BlackBerryProject.CLDC_APPLICATION ) );
        _compressResources_Button = ControlFactory.buildCheckBoxControl( right, toolkit,
                Messages.CompileSection_Compress_Resources_Label, Messages.CompileSection_Compress_Resources_ToolTip,
                compressResources, page.new DirtyListener( this ) );
        _generateALXFile_Button = ControlFactory.buildCheckBoxControl( right, toolkit,
                Messages.PackagingSection_Generate_ALX_Label, Messages.PackagingSection_Generate_ALX_ToolTip, generateALXFile,
                page.new DirtyListener( this ) );
    }

    private void postBuild( Composite body, FormToolkit toolkit ) {
        toolkit.paintBordersFor( body );
        validateHasValidCharacters( getNameTextField() );
        validateHasValidCharacters( getFolderTextField() );
    }

    @Override
    public void commit( boolean onSave ) {
        BlackBerryProperties properties = getProjectPropertiesPage().getBlackBerryProject().getProperties();

        properties.setValidOutputFileName( getOutputFileName() );
        String val;
        // setOutputFileName can force some char replacements (see project_output_file_replace_space); Need to sync the Text box
        if( !( val = properties._packaging.getOutputFileName() ).equals( getOutputFileName() ) ) {
            _name_TextField.setText( val );
        }
        properties._packaging.setOutputFolder( getOutputFolder() );
        properties._packaging.setPreBuildStep( getPreStep() );
        properties._packaging.setPostBuildStep( getPostStep() );
        properties._packaging.setCleanStep( getCleanStep() );
        properties._compile.setOutputCompilerMessages( getOutputCompilerMessages() );
        properties._compile.setConvertImages( getConvertImages() );
        properties._compile.setCreateWarningForNoExportedRoutine( getCreateWarningForNoExportedRoutine() );
        properties._compile.setCompressResources( getCompressResources() );
        properties._compile.setAliasList( getAliasList() );
        properties._packaging.setGenerateALXFile( Boolean.valueOf( _generateALXFile_Button.getSelection() ) );

        super.commit( onSave );
    }

    void validateHasValidCharacters( Control control ) {
        String value = ( (Text) control ).getText();
        BBDiagnostic diag = null;
        boolean isname = control.equals( getNameTextField() );
        IProject proj = getProjectPropertiesPage().getBlackBerryProject().getProject();
        if( isname ) {
            diag = BBPropertiesValidator.validateHasValidOutputFileName( value, proj );
        } else {
            diag = BBPropertiesValidator.validateHasValidFolderName( value, proj);
        }

        if( ( diag != null ) && ( diag.getSeverity() != Diagnostic.OK ) ) {
            if( diag.getSeverity() == Diagnostic.ERROR ) {
                getProjectPropertiesPage().createEditorErrorMarker( isname ? filenameKey : folderKey, diag.getMessage(), control );
            } else {
                getProjectPropertiesPage().createEditorWarnMarker( isname ? filenameKey : folderKey, diag.getMessage(), control );
            }
        } else {
            getProjectPropertiesPage().removeEditorErrorMarker( isname ? filenameKey : folderKey, control );
        }
    }

    /**
     * Update the controls within this section with values from the given properties object
     *
     * @param properties
     */
    public void insertControlValuesFromModel( BlackBerryProperties properties ) {
        String outputFileName = properties._packaging.getOutputFileName();
        String outputFileFolder = properties._packaging.getOutputFolder();
        String preBuildStep = properties._packaging.getPreBuildStep();
        String postBuildStep = properties._packaging.getPostBuildStep();
        String cleanStep = properties._packaging.getCleanStep();
        Boolean outputCompilerMessages = properties._compile.getOutputCompilerMessages();
        Boolean convertImages = properties._compile.getConvertImages();
        Boolean createWarning = properties._compile.getCreateWarningForNoExportedRoutine();
        Boolean compressResources = properties._compile.getCompressResources();
        Boolean generateALXFile = properties._packaging.getGenerateALXFile();
        String aliasList = properties._compile.getAliasList();

        getNameTextField().setText( outputFileName );

        getFolderTextField().setText( outputFileFolder );

        int projBuildExtraSteps = ContextManager.getDefault().getPreferenceStore().getInt( IConstants.PROJECT_BUILD_EXTRA_STEPS );
        if( projBuildExtraSteps > 0 ) {
            _preStep_TextField.setText( preBuildStep == null ? "" : preBuildStep ); //$NON-NLS-1$

            _postStep_TextField.setText( postBuildStep == null ? "" : postBuildStep ); //$NON-NLS-1$

            _cleanStep_TextField.setText( cleanStep == null ? "" : cleanStep ); //$NON-NLS-1$
        }
        if( outputCompilerMessages != null ) {
            _outputCompilerMessages_Button.setSelection( outputCompilerMessages.booleanValue() );
        }
        if( convertImages != null ) {
            _convertImages_Button.setSelection( convertImages.booleanValue() );
        }
        if( createWarning != null ) {
            _createWarning_Button.setSelection( createWarning.booleanValue() );
        }
        if( compressResources != null ) {
            _compressResources_Button.setSelection( compressResources.booleanValue() );
        }
        if( generateALXFile != null ) {
            _generateALXFile_Button.setSelection( generateALXFile.booleanValue() );
        }

        if( aliasList != null ) {
            _aliasListText.setText( aliasList );
        }

    }

    /**
     * @return The output file name pulled from the UI
     */
    public String getOutputFileName() {
        return getNameTextField().getText();
    }

    /**
     * @return The output folder value pulled from the UI
     */
    public String getOutputFolder() {
        return getFolderTextField().getText();
    }

    /**
     * @return The pre step value pulled from the UI
     */
    public String getPreStep() {
        if( _preStep_TextField == null ) {
            BlackBerryProperties properties = getProjectPropertiesPage().getBlackBerryProject().getProperties();
            return properties._packaging.getPreBuildStep();
        }

        return _preStep_TextField.getText();
    }

    /**
     * @return The post step value pulled from the UI
     */
    public String getPostStep() {
        if( _postStep_TextField == null ) {
            BlackBerryProperties properties = getProjectPropertiesPage().getBlackBerryProject().getProperties();
            return properties._packaging.getPostBuildStep();
        }

        return _postStep_TextField.getText();
    }

    /**
     * @return The clean step value pulled from the UI
     */
    public String getCleanStep() {
        if( _cleanStep_TextField == null ) {
            BlackBerryProperties properties = getProjectPropertiesPage().getBlackBerryProject().getProperties();
            return properties._packaging.getCleanStep();
        }

        return _cleanStep_TextField.getText();
    }

    /**
     * @return The checked state of the output compiler messages check box pulled from the UI
     */
    public Boolean getOutputCompilerMessages() {
        return Boolean.valueOf( _outputCompilerMessages_Button.getSelection() );
    }

    /**
     * @return The checked state of the convert images check box pulled from the UI
     */
    public Boolean getConvertImages() {
        return Boolean.valueOf( _convertImages_Button.getSelection() );
    }

    /**
     * @return The checked state of the create warning for no exported routine check box pulled from the UI
     */
    public Boolean getCreateWarningForNoExportedRoutine() {
        return Boolean.valueOf( _createWarning_Button.getSelection() );
    }

    /**
     * @return The checked state of the compress resources check box pulled from the UI
     */
    public Boolean getCompressResources() {
        return Boolean.valueOf( _compressResources_Button.getSelection() );
    }

    /**
     * @return A string representation of the alias list pulled from the UI
     */
    public String getAliasList() {
        return _aliasListText.getText();
    }

    /**
     * @return the generateALXFile_Button
     */
    public Boolean getGenerateALXFile() {
        return Boolean.valueOf( _generateALXFile_Button.getSelection() );
    }

    /**
     * @param name_TextField
     *            the name_TextField to set
     */
    void setNameTextField( Text name_TextField ) {
        _name_TextField = name_TextField;
    }

    /**
     * @return the name_TextField
     */
    Text getNameTextField() {
        return _name_TextField;
    }

    /**
     * @param folder_TextField
     *            the folder_TextField to set
     */
    void setFolderTextField( Text folder_TextField ) {
        _folder_TextField = folder_TextField;
    }

    /**
     * @return the folder_TextField
     */
    Text getFolderTextField() {
        return _folder_TextField;
    }

    @Override
    public void propertyChange( PropertyChangeEvent evt ) {
        String name = evt.getPropertyName();
        // if the project type is set as lib, we disable the alias text
        if( name.equals( Messages.GeneralSection_Application_Type_Label ) ) {
            String newValue = (String) evt.getNewValue();
            _aliasListText.setEnabled( newValue.equals( BlackBerryProject.LIBRARY ) );
            _createWarning_Button.setEnabled( newValue.equals( BlackBerryProject.CLDC_APPLICATION ) );
        }
    }
}
