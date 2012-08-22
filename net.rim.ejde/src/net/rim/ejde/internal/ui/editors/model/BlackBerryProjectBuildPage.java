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

import net.rim.ejde.internal.ui.editors.model.factories.LayoutFactory;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 * This class creates the build page within the project properties editor
 *
 * @author jkeshavarzi
 *
 */
public class BlackBerryProjectBuildPage extends BlackBerryProjectPropertiesPage {
    private boolean formCreated = false;

    private PackagingSection packagingSection;
    private PreprocessorTagSection preprocessorTagSection;
    private ALXFilesSection alxFilesSection;

    /**
     * @param editor
     * @param id
     * @param title
     */
    public BlackBerryProjectBuildPage( FormEditor editor, String id, String title ) {
        super( editor, id, title );
    }

    /**
     * @param editor
     */
    public BlackBerryProjectBuildPage( FormEditor editor ) {
        this( editor, Messages.BlackBerryProjectBuildPage_ID, Messages.BlackBerryProjectBuildPage_Title );
    }

    /**
     * @return A boolean indicating if this page has been created.
     */
    public boolean isFormCreated() {
        return formCreated;
    }

    /**
     * Returns the packaging section
     *
     * @return PackagingSection
     */
    public PackagingSection getPackagingSection() {
        return this.packagingSection;
    }

    /**
     * Returns the preprocessor tag section
     *
     * @return PreprocessorTagSection
     */
    public PreprocessorTagSection getPreprocessorTagSection() {
        return this.preprocessorTagSection;
    }

    /**
     * Returns the ALX file section
     *
     * @return ALXFilesSection
     */
    public ALXFilesSection getAlxFileSection() {
        return this.alxFilesSection;
    }

    @Override
    protected void createFormContent( IManagedForm managedForm ) {
        BlackBerryProjectFormEditor editor = ( (BlackBerryProjectFormEditor) getEditor() );

        ScrolledForm form = managedForm.getForm();
        Composite body = form.getBody();
        FormToolkit toolkit = managedForm.getToolkit();

        form.setImage( editor.getApplicationImage() );
        toolkit.decorateFormHeading( form.getForm() );

        form.getBody().setLayout( new TableWrapLayout() );

        preBuild( form );

        build( managedForm, body, toolkit );

        postBuild( body, toolkit );

        formCreated = true;
    }

    private void preBuild( ScrolledForm form ) {
        form.setText( Messages.BlackBerryProjectBuildPage_Page_Title );
    }

    private void build( IManagedForm managedForm, Composite body, FormToolkit toolkit ) {
        // Initialize page layout
        body.setLayout( LayoutFactory.createFormGridLayout( true, 2 ) );
        Composite main, left, right;
        main = toolkit.createComposite( body, SWT.NONE );
        main.setLayout( LayoutFactory.createFormPaneGridLayout( false, 1 ) );
        GridData gd = new GridData( GridData.FILL_HORIZONTAL );
        gd.horizontalSpan = 2;
        main.setLayoutData( gd );
        left = toolkit.createComposite( body, SWT.NONE );
        left.setLayout( LayoutFactory.createFormPaneGridLayout( false, 1 ) );
        left.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        right = toolkit.createComposite( body, SWT.NONE );
        right.setLayout( LayoutFactory.createFormPaneGridLayout( false, 1 ) );
        right.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        // Create packaging section
        packagingSection = new PackagingSection( this, main, toolkit, 384 );
        addSection( packagingSection );

        // Create preprocessor tag section
        preprocessorTagSection = new PreprocessorTagSection( this, left, toolkit, 384 );
        addSection( preprocessorTagSection );

        // Create ALX file section
        alxFilesSection = new ALXFilesSection( this, right, toolkit, 384 );
        addSection( alxFilesSection );
    }

    private void postBuild( Composite body, FormToolkit toolkit ) {
        toolkit.paintBordersFor( body );
    }
}
