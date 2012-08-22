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
import net.rim.ejde.internal.ui.editors.model.factories.LayoutFactory;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IDetailsPageProvider;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.Section;

/**
 * This class creates the master section on the alternate entry point master-details page.
 *
 * @author jkeshavarzi
 *
 */
public class AlternateEntryPointMasterDetailsBlock extends MasterDetailsBlock implements IDetailsPageProvider {
    private BlackBerryProjectPropertiesPage _page;
    private SectionPart masterPart;

    /**
     * @param page
     */
    public AlternateEntryPointMasterDetailsBlock( BlackBerryProjectPropertiesPage page ) {
        _page = page;
    }

    // protected abstract SectionPart createMasterSection( IManagedForm managedForm, Composite parent );
    protected SectionPart createMasterSection( IManagedForm managedForm, Composite parent ) {
        return new AlternateEntryPointSection( getPage(), parent, managedForm.getToolkit(), SWT.NONE );
    }

    protected void registerPages( DetailsPart detailsPart ) {
        detailsPart.setPageProvider( this );
    }

    /**
     * @return The DetailsPart object associated with this block
     */
    public DetailsPart getDetailsPart() {
        return detailsPart;
    }

    /**
     * Returns the master part associated with this block
     *
     * @return The SectionPart object associated with this block
     */
    public SectionPart getMasterPart() {
        return masterPart;
    }

    /**
     * @return The parent BlackBerryProjectPropertiesPage
     */
    public BlackBerryProjectPropertiesPage getPage() {
        return _page;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.forms.MasterDetailsBlock#createContent(org.eclipse.ui.forms.IManagedForm)
     */
    public void createContent( IManagedForm managedForm ) {
        super.createContent( managedForm );
        managedForm.getForm().getBody().setLayout( LayoutFactory.createFormGridLayout( false, 1 ) );
    }

    protected void createMasterPart( final IManagedForm managedForm, Composite parent ) {
        Composite container = managedForm.getToolkit().createComposite( parent );
        container.setLayout( LayoutFactory.createMasterGridLayout( false, 1 ) );
        container.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        masterPart = createMasterSection( managedForm, container );
        managedForm.addPart( masterPart );
        Section section = masterPart.getSection();
        section.setLayout( LayoutFactory.createClearGridLayout( false, 1 ) );
        section.setLayoutData( new GridData( GridData.FILL_BOTH ) );
    }

    protected void createToolBarActions( IManagedForm managedForm ) {
        // No implementation
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.forms.IDetailsPageProvider#getPageKey(java.lang.Object)
     */
    public Object getPageKey( Object object ) {
        if( object instanceof AlternateEntryPoint ) {
            return AlternateEntryPointDetails.class;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.forms.IDetailsPageProvider#getPage(java.lang.Object)
     */
    public IDetailsPage getPage( Object object ) {
        if( object.equals( AlternateEntryPointDetails.class ) ) {
            return new AlternateEntryPointDetails( (AlternateEntryPointSection) this.getMasterPart() );
        }
        return null;
    }
}
