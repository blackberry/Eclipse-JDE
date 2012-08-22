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
package net.rim.ejde.internal.sourceprovider;

import java.util.HashMap;
import java.util.Map;

import net.rim.ejde.internal.util.ProjectUtils;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.ISources;
import org.eclipse.ui.services.IServiceLocator;

public class CommandSourceProvider extends AbstractSourceProvider {
    private static final String COMMANDSTATE = "net.rim.ejde.internal.sourceprovider.enableState";
    private static final String ENABLED = "enabled";
    private static final String DISABLED = "disabled";
    private boolean currentState = false;

    @Override
    public void initialize( final IServiceLocator locator ) {
        currentState = ProjectUtils.containsProjects() && !ProjectUtils.isAllBBProjectsClosed();
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        workspace.addResourceChangeListener( new IResourceChangeListener() {
            @Override
            public void resourceChanged( IResourceChangeEvent event ) {
                setEnabled( ProjectUtils.containsProjects() && !ProjectUtils.isAllBBProjectsClosed() );
            }
        } );
    }

    @Override
    public void dispose() {
    }

    @Override
    public Map< String, String > getCurrentState() {
        Map< String, String > map = new HashMap< String, String >();
        map.put( COMMANDSTATE, currentState ? ENABLED : DISABLED );
        return map;
    }

    @Override
    public String[] getProvidedSourceNames() {
        return new String[] { COMMANDSTATE };
    }

    void setEnabled( boolean value ) {
        currentState = value;
        // Run asnchronously to avoid SWT exception when projects have been closed.
        Display.getDefault().asyncExec( new Runnable() {
            @Override
            public void run() {
                fireSourceChanged( ISources.WORKBENCH, COMMANDSTATE, currentState ? ENABLED : DISABLED );
            }
        } );
    }
}
