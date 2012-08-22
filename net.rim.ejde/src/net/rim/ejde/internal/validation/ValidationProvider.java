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
package net.rim.ejde.internal.validation;

import java.util.ArrayList;
import java.util.List;

import net.rim.ejde.internal.model.BlackBerryProject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

/**
 * @author cbateman
 */
public class ValidationProvider implements IValidationProvider {

    /**
     * Default constructor
     */
    public ValidationProvider() {
    }

    public List< Object > getValidationChildren( Object parent ) {
        List< Object > children = new ArrayList< Object >();
        if( parent instanceof IProject ) {
            IFile modelFile = ( (IProject) parent ).getFile( BlackBerryProject.METAFILE );
            if( modelFile != null && modelFile.exists() ) {
                children.add( modelFile );
            }
        }
        return children;
    }
}
