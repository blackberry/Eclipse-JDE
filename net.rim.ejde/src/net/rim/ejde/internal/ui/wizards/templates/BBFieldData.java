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
package net.rim.ejde.internal.ui.wizards.templates;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.ui.IFieldData;

public class BBFieldData implements IFieldData {

    private String _id;
    private String _name;
    private String _version;
    private IWizard _masterWizard;

    /**
     * @return The main plug-in id.
     */
    @Override
    public String getId() {
        return _id;
    }

    @Override
    public String getLibraryName() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return The project name.
     */
    @Override
    public String getName() {
        return _name;
    }

    @Override
    public String getOutputFolderName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProvider() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSourceFolderName() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @return The project version.
     */
    @Override
    public String getVersion() {
        return _version;
    }

    @Override
    public boolean hasBundleStructure() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLegacy() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isSimple() {
        // TODO Auto-generated method stub
        return false;
    }

    public void setId( String id ) {
        _id = id;
    }

    public IWizard getMasterWizard() {
        return _masterWizard;
    }

    public void setMasterWizard( IWizard masterWizard ) {
        _masterWizard = masterWizard;
    }

    public void setName( String projectName ) {
        _name = projectName;
    }
}
