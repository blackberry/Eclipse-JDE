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
package net.rim.ejde.internal.ui.editors.locale;

import net.rim.sdk.resourceutil.ResourceLocale;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * The content provider used for resources
 *
 * @author jkeshavarzi
 *
 */
public class ResourceContentProvider implements IStructuredContentProvider {
    public Object[] getElements( Object inputElement ) {
        if( inputElement instanceof ResourceLocale ) {
            return ( (ResourceLocale) inputElement ).getResourceElements();
        }

        return null;
    }

    public void dispose() {
        // TODO Auto-generated method stub
    }

    public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
        // TODO Auto-generated method stub
    }
}
