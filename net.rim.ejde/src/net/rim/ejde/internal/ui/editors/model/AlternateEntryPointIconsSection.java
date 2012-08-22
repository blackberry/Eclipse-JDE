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
import net.rim.ejde.internal.model.BasicBlackBerryProperties.Icon;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 *
 * @author bkurz, jkeshavarzi
 *
 */
public class AlternateEntryPointIconsSection extends AbstractIconsSection {
    private AlternateEntryPoint _aep;
    private IconsSubSection _iconSubSection;

    public AlternateEntryPointIconsSection( BlackBerryProjectPropertiesPage page, Composite parent, FormToolkit toolkit, int style ) {
        super( page, parent, toolkit, style );
        this._iconSubSection = getIconsSubSection();
    }

    /**
     *
     * @param aep
     */
    public void setAlternateEntryPoint( AlternateEntryPoint aep ) {
        this._aep = aep;
        insertControlValuesFromModel();
    }

    @Override
    public void commit( boolean onSave ) {
        super.commit( onSave );

        Icon icons[] = _iconSubSection.getIcons();
        if( icons.length > 0 ) {
            // Link any missing external icons
            getEditor().linkExternalIcons( icons );
            setViewerInput( icons );
        }
        _aep.setIconFiles( icons );
    }

    public void setViewerInput( Icon[] icons ) {
        _iconSubSection.setInput( icons );
    }

    public void insertControlValuesFromModel() {
        Icon icons[] = _aep.getIconFiles();
        _iconSubSection.setInput( icons );
    }
}
