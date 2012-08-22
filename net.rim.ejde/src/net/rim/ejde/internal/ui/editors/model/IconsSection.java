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

import net.rim.ejde.internal.model.BasicBlackBerryProperties.Icon;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * This class creates the icon section used in the project properties editor.
 *
 * @author jkeshavarzi
 *
 */
public class IconsSection extends AbstractIconsSection implements PropertyChangeListener {
    private IconsSubSection _iconSubSection;

    /**
     * This class creates the icon section used in the project properties editor.
     *
     * @param page
     * @param parent
     * @param toolkit
     * @param style
     */
    public IconsSection( BlackBerryProjectPropertiesPage page, Composite parent, FormToolkit toolkit, int style ) {
        super( page, parent, page.getManagedForm().getToolkit(), ( style | Section.DESCRIPTION | ExpandableComposite.TITLE_BAR ) );
        _iconSubSection = getIconsSubSection();
        insertControlValuesFromModel( getProjectPropertiesPage().getBlackBerryProject().getProperties() );
        getEditor().addListener( this );
    }

    @Override
    public void commit( boolean onSave ) {
        super.commit( onSave );

        BlackBerryProperties properties = getProjectPropertiesPage().getBlackBerryProject().getProperties();
        Icon icons[] = getIcons();

        if( icons.length > 0 ) {
            // Link any missing external icons
            getEditor().linkExternalIcons( icons );
            _iconSubSection.setInput( icons );
        }

        properties._resources.setIconFiles( icons );
    }

    /**
     * Update the controls within this section with values from the given properties object
     *
     * @param properties
     */
    public void insertControlValuesFromModel( BlackBerryProperties properties ) {
        Icon[] icons = properties._resources.getIconFiles();
        _iconSubSection.setInput( icons );
        _iconSubSection.setEnabled( !getEditor().getBlackBerryProject().getProperties()._application.getType().equals(
                BlackBerryProject.LIBRARY ) );
    }

    @Override
    public void propertyChange( PropertyChangeEvent evt ) {
        String name = evt.getPropertyName();
        // if the project type is set as lib, we disable the icon section
        if( name.equals( Messages.GeneralSection_Application_Type_Label ) ) {
            String newValue = (String) evt.getNewValue();
            _iconSubSection.setEnabled( !newValue.equals( BlackBerryProject.LIBRARY ) );
        }
    }
}
