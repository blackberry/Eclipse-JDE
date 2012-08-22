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
package net.rim.ejde.internal.menu;

import java.util.HashSet;
import java.util.Set;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.packaging.PackagingJob;
import net.rim.ejde.internal.packaging.PackagingJobWrapper;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class SignCommandHandler extends AbstractHandler {

    private Set< BlackBerryProject > _projects;

    public Object execute( ExecutionEvent event ) throws ExecutionException {
        _projects = new HashSet< BlackBerryProject >();
        ISelection selection = HandlerUtil.getCurrentSelection( event );
        if( ( selection != null ) && !selection.isEmpty() && ( selection instanceof StructuredSelection ) ) {
            IJavaProject[] javaProjects = ProjectUtils.getSelectProjects( (StructuredSelection) selection );
            if( javaProjects.length > 0 ) {
                for( IJavaProject javaProject : javaProjects ) {
                    BlackBerryProperties properties = null;
                    properties = ContextManager.PLUGIN.getBBProperties( javaProject.getProject().getName(), false );
                    if( properties == null ) {
                        continue;
                    }
                    _projects.add( new BlackBerryProject( javaProject, properties ) );
                }
            }
        }
        // Always launch packaging. Packaging will not re-package unless necessary
        PackagingJobWrapper job = new PackagingJobWrapper( Messages.PackagingJob_Name, _projects, PackagingJob.SIGN_FORCE );
        job.setUser( true );
        job.schedule();
        return null;
    }
}
