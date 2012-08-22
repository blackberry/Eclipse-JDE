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

/**
 * This class creates the application page within the project properties editor
 *
 * @author jkeshavarzi
 *
 */
public class BlackBerryProjectApplicationPage extends BlackBerryProjectPropertiesPage {
    private boolean formCreated = false;

    private GeneralSection generalSection;
    private ResourcesSection resourcesSection;
    private IconsSection iconSection;

    /**
     * @param editor
     * @param id
     * @param title
     */
    public BlackBerryProjectApplicationPage( FormEditor editor, String id, String title ) {
        super( editor, id, title );
    }

    /**
     * @param editor
     */
    public BlackBerryProjectApplicationPage( FormEditor editor ) {
        this( editor, Messages.BlackBerryProjectApplicationPage_ID, Messages.BlackBerryProjectApplicationPage_Title );
    }

    /**
     * @return A boolean indicating if this page has been created.
     */
    public boolean isFormCreated() {
        return formCreated;
    }

    /**
     * Returns the general section
     *
     * @return GeneralSection
     */
    public GeneralSection getGeneralSection() {
        return this.generalSection;
    }

    /**
     * Returns the resource section
     *
     * @return ResourcesSection
     */
    public ResourcesSection getResourcesSection() {
        return this.resourcesSection;
    }

    /**
     * Returns the icon section
     *
     * @return IconsSection
     */
    public IconsSection getIconSection() {
        return this.iconSection;
    }

    @Override
    protected void createFormContent( IManagedForm managedForm ) {
        BlackBerryProjectFormEditor editor = ( (BlackBerryProjectFormEditor) getEditor() );

        ScrolledForm form = managedForm.getForm();
        Composite body = form.getBody();
        FormToolkit toolkit = managedForm.getToolkit();

        form.setImage( editor.getApplicationImage() );
        toolkit.decorateFormHeading( form.getForm() );

        preBuild( form );

        build( managedForm, body, toolkit );

        postBuild( body, toolkit );

        formCreated = true;
    }

    private void preBuild( ScrolledForm form ) {
        form.setText( Messages.BlackBerryProjectApplicationPage_Page_Title );
    }

    private void build( IManagedForm managedForm, Composite body, FormToolkit toolkit ) {
        // Initialize page layout
        body.setLayout( LayoutFactory.createFormGridLayout( true, 2 ) );
        Composite left, right;
        left = toolkit.createComposite( body, SWT.NONE );
        left.setLayout( LayoutFactory.createFormPaneGridLayout( false, 1 ) );
        left.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        right = toolkit.createComposite( body, SWT.NONE );
        right.setLayout( LayoutFactory.createFormPaneGridLayout( false, 1 ) );
        right.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        // Create general section
        generalSection = new GeneralSection( this, left, toolkit, 384 );
        addSection( generalSection );

        // Create resources section
        resourcesSection = new ResourcesSection( this, right, toolkit, 384 );
        addSection( resourcesSection );

        // Create icon section
        iconSection = new IconsSection( this, right, toolkit, 384 );
        addSection( iconSection );
    }

    private void postBuild( Composite body, FormToolkit toolkit ) {
        toolkit.paintBordersFor( body );
    }
}
