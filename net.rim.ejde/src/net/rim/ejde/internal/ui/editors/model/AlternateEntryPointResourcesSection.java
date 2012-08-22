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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.AlternateEntryPoint;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.ProjectUtils.RRHFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 *
 * @author bkurz
 *
 */
public class AlternateEntryPointResourcesSection extends AbstractResourcesSection {
    private AlternateEntryPoint _aep = null;

    public AlternateEntryPointResourcesSection( BlackBerryProjectPropertiesPage page, Composite parent, FormToolkit toolkit,
            int style ) {
        super( page, parent, toolkit, style );
    }

    /**
     * Returns the alternate entry point associated with this resource section
     *
     * @return
     */
    public AlternateEntryPoint getAlternateEntryPoint() {
        return this._aep;
    }

    /**
     * Sets the alternate entry point associated with this resource section
     *
     * @param aep
     */
    public void setAlternateEntryPoint( AlternateEntryPoint aep ) {
        this._aep = aep;
    }

    @Override
    protected void createFormContent( Section section, FormToolkit toolkit ) {
        super.createFormContent( section, toolkit );
        refreshControls( false );
    }

    @Override
    public void refreshControls( boolean loadFromProperties ) {
        setResources( ProjectUtils.getProjectResources( getProjectPropertiesPage().getBlackBerryProject() ) );
        String bundleClassName = "";
        String title = "";
        boolean isResourceAvailable = false;
        Boolean isDirty = getEditor().isDirty();

        if( loadFromProperties ) {
            bundleClassName = _aep.getTitleResourceBundleClassName();
            title = _aep.getTitleResourceBundleKey();
            isResourceAvailable = _aep.getHasTitleResource();
        } else {
            bundleClassName = getResourceBundleClassNameField().getText();
            title = getTitleIdField().getText();
            isResourceAvailable = getResourceAvailableField().getSelection();
        }

        // bundles
        List< String > bundless = new ArrayList< String >();
        for( String key : getResources().keySet() ) {
            bundless.add( key );
        }
        if( !bundless.contains( IConstants.EMPTY_STRING ) ) {
            bundless.add( 0, IConstants.EMPTY_STRING );
        }
        String[] bundleNames = bundless.toArray( new String[ bundless.size() ] );
        getResourceBundleClassNameField().setItems( bundleNames );
        getResourceBundleClassNameField().setText( bundleClassName );

        // keys
        RRHFile rrhFile = getResources().get( getResourceBundleClassNameField().getText() );
        List< String > keys = new ArrayList< String >();
        if( rrhFile != null ) {
            keys.addAll( Arrays.asList( getProjectPropertiesPage().getResourceKeys( rrhFile.getKeyTalbe() ) ) );
        }
        if( !keys.contains( IConstants.EMPTY_STRING ) ) {
            keys.add( 0, IConstants.EMPTY_STRING );
        }
        getTitleIdField().setItems( keys.toArray( new String[ keys.size() ] ) );
        getTitleIdField().setText( title == null ? IConstants.EMPTY_STRING : title );

        getResourceAvailableField().setSelection( isResourceAvailable );

        validateProperties();
        updateControlStates();
        // don't change the dirty status
        getEditor().setDirty( getEditor().isDirty() );

        getEditor().setDirty( isDirty );
    }

    @Override
    public void commit( boolean onSave ) {
        super.commit( onSave );
        _aep.setHasTitleResource( getResourceAvailableField().getSelection() );
        String bundleClassName = getResourceBundleClassNameField().getText();
        _aep.setTitleResourceBundleClassName( bundleClassName );
        _aep.setTitleResourceBundleName( bundleClassName.substring( bundleClassName.lastIndexOf( IConstants.DOT_MARK ) + 1,
                bundleClassName.length() ) );
        _aep.setTitleResourceBundleKey( getTitleIdField().getText() );
        if( getResourceBundle() != null ) {
            IFile file = getResourceBundle().getFile();
            _aep.setTitleResourceBundleRelativePath( file == null ? IConstants.EMPTY_STRING : file.getProjectRelativePath()
                    .toString() );
        } else {
            _aep.setTitleResourceBundleRelativePath( IConstants.EMPTY_STRING );
        }
    }
}
