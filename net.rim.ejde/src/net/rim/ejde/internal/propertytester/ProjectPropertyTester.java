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
package net.rim.ejde.internal.propertytester;

import java.util.Iterator;

import net.rim.ejde.internal.util.NatureUtils;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * Test if selection contains only IProject and all the projects are BB project.
 *
 * @author dmeng
 *
 */
public class ProjectPropertyTester extends PropertyTester {

    public static final String PROPERTY_NAME = "net.rim.ejde.isBBProject";

    @Override
    public boolean test( Object receiver, String property, Object[] args, Object expectedValue ) {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if( page != null ) {
            IViewPart viewPart = page.findView( "org.eclipse.jdt.ui.PackageExplorer" );
            if( viewPart != null ) {
                IPackagesViewPart packageView = (IPackagesViewPart) viewPart;
                ISelection selection = packageView.getSite().getSelectionProvider().getSelection();
                if( selection instanceof StructuredSelection ) {
                    StructuredSelection ssel = (StructuredSelection) selection;
                    if( ssel.isEmpty() ) {
                        return false;
                    } else {
                        Iterator< Object > iter = ssel.iterator();
                        while( iter.hasNext() ) {
                            Object item = iter.next();
                            if( !( item instanceof IJavaProject ) ) {
                                return false;
                            }
                            if( !NatureUtils.hasBBNature( ( (IJavaProject) item ).getProject() ) ) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
