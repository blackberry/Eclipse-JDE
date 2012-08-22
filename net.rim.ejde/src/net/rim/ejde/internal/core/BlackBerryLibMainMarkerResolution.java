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
package net.rim.ejde.internal.core;

import java.io.File;

import net.rim.ejde.internal.model.BlackBerryProperties;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class BlackBerryLibMainMarkerResolution implements IMarkerResolution2 {

    @Override
    public String getDescription() {
        return "Project containing a libMain method is not set for auto-startup";
    }

    @Override
    public Image getImage() {
        ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons"
                + File.separator + "bb_perspective.gif" );
        final Image image = imageDescriptor.createImage();
        return image;
    }

    @Override
    public String getLabel() {
        return "Set project to autostartup on startup";
    }

    @Override
    public void run( IMarker marker ) {
        String projectName = marker.getResource().getProject().getName();
        BlackBerryProperties properties = ContextManager.PLUGIN.getBBProperties( projectName, false );
        properties._application.setIsAutostartup( Boolean.TRUE );
        ContextManager.PLUGIN.setBBProperties( projectName, properties, true );
    }

}
