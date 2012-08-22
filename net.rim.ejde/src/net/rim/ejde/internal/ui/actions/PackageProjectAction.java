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
package net.rim.ejde.internal.ui.actions;

import java.util.HashSet;
import java.util.Set;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.model.preferences.SignatureToolPreferences;
import net.rim.ejde.internal.packaging.PackagingJob;
import net.rim.ejde.internal.packaging.PackagingJobWrapper;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IActionDelegate;

public class PackageProjectAction implements IActionDelegate {
    // static private final Logger _log = Logger.getLogger( PackageProjectAction.class );
    IJavaProject[] javaProjects;

    public void run( IAction action ) {
        if( javaProjects == null || javaProjects.length == 0 ) {
            return;
        }
        if( javaProjects.length > 0 ) {
            Set< BlackBerryProject > projects = new HashSet< BlackBerryProject >();
            for( IJavaProject javaProject : javaProjects ) {
                BlackBerryProperties properties = null;
                properties = ContextManager.PLUGIN.getBBProperties( javaProject.getProject().getName(), false );
                if( properties == null ) {
                    continue;
                }
                projects.add( new BlackBerryProject( javaProject, properties ) );
            }
            PackagingJobWrapper job = new PackagingJobWrapper( Messages.PackagingJob_Name, projects,
                    SignatureToolPreferences.getRunSignatureToolAutomatically() ? PackagingJob.SIGN_IF_NECESSARY
                            : PackagingJob.SIGN_NO );
            job.setUser( true );
            job.schedule();
        }
    }

    public void selectionChanged( IAction action, ISelection selection ) {
        if( selection == null || selection.isEmpty() ) {
            return;
        }
        if( selection instanceof StructuredSelection ) {
            javaProjects = ProjectUtils.getSelectProjects( (StructuredSelection) selection );
        }
    }
}
