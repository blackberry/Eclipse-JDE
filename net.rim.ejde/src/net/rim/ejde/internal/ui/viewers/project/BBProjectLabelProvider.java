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
package net.rim.ejde.internal.ui.viewers.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class BBProjectLabelProvider extends LabelProvider {

    protected ILabelProvider _iprojectLabelProvider;

    public BBProjectLabelProvider() {
        _iprojectLabelProvider = new WorkbenchLabelProvider();
    }

    /**
     * returns the workbench IProject image for a rimProject
     */
    @Override
    public Image getImage( Object element ) {
        if( element instanceof IProject ) {
            return _iprojectLabelProvider.getImage( element );
        }
        return null;

    }

    @Override
    public String getText( Object element ) {
        if( element instanceof IProject ) {
            return ( (IProject) element ).getName();
        }
        return "";
    }
}
