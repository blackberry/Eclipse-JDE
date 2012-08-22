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

import net.rim.ejde.internal.model.BasicBlackBerryProperties.AlternateEntryPoint;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * This class creates the alternate entry point page within the project properties editor.
 *
 * @author jkeshavarzi
 *
 */
public class BlackBerryProjectAlternateEntryPointPage extends BlackBerryProjectPropertiesPage {
    private boolean formCreated = false;

    private AlternateEntryPointMasterDetailsBlock aepBlock;
    private AlternateEntryPointSection alternateEntryPointSection;

    /**
     * @param editor
     * @param id
     * @param title
     */
    public BlackBerryProjectAlternateEntryPointPage( FormEditor editor, String id, String title ) {
        super( editor, id, title );
        aepBlock = new AlternateEntryPointMasterDetailsBlock( this );
    }

    /**
     * @param editor
     */
    public BlackBerryProjectAlternateEntryPointPage( FormEditor editor ) {
        this( editor, Messages.BlackBerryProjectAlternateEntryPointPage_ID,
                Messages.BlackBerryProjectAlternateEntryPointPage_Title );
    }

    /**
     * Returns the alternate entry point section
     *
     * @return Instance of AlternateEntryPointSection
     */
    public AlternateEntryPointSection getAlternateEntryPointSection() {
        return this.alternateEntryPointSection;
    }

    /**
     * Returns the alternate entry point details page
     *
     * @return Instance of AlternateEntryPointDetails
     */
    public AlternateEntryPointDetails getAlternateEntryPointDetails() {
        return (AlternateEntryPointDetails) aepBlock.getDetailsPart().getCurrentPage();
    }

    /**
     * @return An array of AlternateEntryPoint objects associated with this page
     */
    public AlternateEntryPoint[] getAlternateEntryPoints( boolean isSave ) {
        DetailsPart part = aepBlock.getDetailsPart();
        if( part.isDirty() ) {
            part.commit( false );
        }
        return alternateEntryPointSection.getAlternateEntryPoints();
    }

    /**
     * @return A boolean indicating if this page has been created.
     */
    public boolean isFormCreated() {
        return formCreated;
    }

    @Override
    protected void createFormContent( IManagedForm managedForm ) {
        BlackBerryProjectFormEditor editor = ( (BlackBerryProjectFormEditor) getEditor() );
        FormToolkit toolkit = managedForm.getToolkit();
        ScrolledForm form = managedForm.getForm();

        form.setText( Messages.BlackBerryProjectAlternateEntryPointPage_Page_Title );
        form.setImage( editor.getApplicationImage() );
        toolkit.decorateFormHeading( form.getForm() );

        aepBlock.createContent( managedForm );
        alternateEntryPointSection = (AlternateEntryPointSection) aepBlock.getMasterPart();
        toolkit.paintBordersFor( managedForm.getForm().getBody() );

        formCreated = true;
    }
}
