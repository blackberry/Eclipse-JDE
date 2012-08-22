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
package net.rim.ejde.internal.signing;

import java.io.File;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.ui.preferences.WarningsPrefsPage;
import net.rim.ejde.internal.util.VMUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class CodeSignMarkerResolution implements IMarkerResolution2 {

    public String getDescription() {
        return "BlackBerry Protected APIs require code signing";
    }

    public Image getImage() {
        ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons"
                + File.separator + "bb_perspective.gif" );
        final Image image = imageDescriptor.createImage();
        return image;
    }

    public String getLabel() {
        return "Suppress code signing warnings";
    }

    public void run( IMarker marker ) {
        try {
            String msg = marker.getAttribute( IMarker.MESSAGE ).toString();
            String keyLabel = VMUtils.convertCodeSignErrorMsgToPreferenceLabel( msg );
            PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn( Display.getDefault().getActiveShell(),
                    WarningsPrefsPage.ID, null, null );
            WarningsPrefsPage page = (WarningsPrefsPage) dialog.getSelectedPage();
            page.setSelection( keyLabel, true );
            dialog.open();
        } catch( CoreException e ) {
            e.printStackTrace();
        }
    }
}
