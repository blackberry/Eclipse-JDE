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

import net.rim.ejde.internal.model.BasicBlackBerryProperties.Icon;
import net.rim.ejde.internal.ui.editors.model.factories.LayoutFactory;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * AbstractIconsSection.class
 *
 * @author bkurz, jkeshavarzi
 *
 */
public abstract class AbstractIconsSection extends AbstractSection {
    private IconsSubSection iconSubSection;

    public AbstractIconsSection( BlackBerryProjectPropertiesPage page, Composite parent, FormToolkit toolkit, int style ) {
        super( page, parent, toolkit, style );
        createFormContent( getSection(), toolkit );
    }

    /**
     * Returns the icons associated with the icons section
     *
     * @return
     */
    public Icon[] getIcons() {
        return iconSubSection.getIcons();
    }

    /**
     * Validates the icons associated with the icons section
     */
    public void validateIcons() {
        iconSubSection.validateIcons();
    }

    /**
     * Refreshes the icons associated with the icons section
     */
    @Override
    public void refresh() {
        iconSubSection.refresh();
    }

    protected IconsSubSection getIconsSubSection() {
        return this.iconSubSection;
    }

    protected void createFormContent( Section section, FormToolkit toolkit ) {
        preBuild( section );

        Composite client = toolkit.createComposite( section );
        client.setLayout( LayoutFactory.createSectionGridLayout( false, 3 ) );
        section.setClient( client );

        build( client, toolkit );
        postBuild( client, toolkit );
    }

    private void preBuild( Section section ) {
        GridData gridData = new GridData( GridData.FILL_BOTH );
        gridData.minimumWidth = 250;
        section.setLayout( LayoutFactory.createClearGridLayout( false, 1 ) );
        section.setLayoutData( gridData );

        section.setText( Messages.IconsSection_Title );
        section.setDescription( Messages.IconsSection_Description );
    }

    private void build( final Composite body, FormToolkit toolkit ) {
        iconSubSection = new IconsSubSection( getProjectPropertiesPage(), this, body, toolkit );
        iconSubSection.create( false, Messages.IconSection_Application_Icons_ToolTip );
    }

    private void postBuild( Composite body, FormToolkit toolkit ) {
        toolkit.paintBordersFor( body );
    }
}
