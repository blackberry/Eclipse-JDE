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
package net.rim.ejde.internal.model.ui;

import java.net.URL;

import net.rim.ejde.internal.model.ui.LibraryContentProvider.SubElement;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.JDIImageDescriptor;
import org.eclipse.jdt.internal.debug.ui.jres.JREMessages;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Copy of the LabelProvider to support our LibraryStandin
 *
 */
public class LibraryLabelProvider extends LabelProvider {

    public Image getImage( Object element ) {
        String key = null;
        IStatus status = Status.OK_STATUS;
        if( element instanceof LibraryStandin ) {
            LibraryStandin library = (LibraryStandin) element;
            IPath sourcePath = library.getSystemLibrarySourcePath();
            if( sourcePath != null && !Path.EMPTY.equals( sourcePath ) ) {
                key = ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE_WITH_SOURCE;
            } else {
                key = ISharedImages.IMG_OBJS_EXTERNAL_ARCHIVE;
            }
            status = library.validate();

        } else if( element instanceof SubElement ) {
            if( ( (SubElement) element ).getType() == SubElement.SOURCE_PATH ) {
                key = ISharedImages.IMG_OBJS_JAR_WITH_SOURCE;
            } else {
                key = ISharedImages.IMG_OBJS_JAVADOCTAG;
            }
        }
        if( key != null ) {
            if( !status.isOK() ) {
                ImageDescriptor base = JavaUI.getSharedImages().getImageDescriptor( key );
                JDIImageDescriptor descriptor = new JDIImageDescriptor( base, JDIImageDescriptor.IS_OUT_OF_SYNCH );
                return JDIDebugUIPlugin.getImageDescriptorRegistry().get( descriptor );
            }
            return JavaUI.getSharedImages().getImage( key );
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    public String getText( Object element ) {
        if( element instanceof LibraryStandin ) {
            return ( (LibraryStandin) element ).getSystemLibraryPath().toOSString();
        } else if( element instanceof SubElement ) {
            SubElement subElement = (SubElement) element;
            StringBuffer text = new StringBuffer();
            if( subElement.getType() == SubElement.SOURCE_PATH ) {
                text.append( JREMessages.VMLibraryBlock_0 );
                IPath systemLibrarySourcePath = subElement.getParent().getSystemLibrarySourcePath();
                if( systemLibrarySourcePath != null && !Path.EMPTY.equals( systemLibrarySourcePath ) ) {
                    text.append( systemLibrarySourcePath.toOSString() );
                } else {
                    text.append( JREMessages.VMLibraryBlock_1 );
                }
            } else if( subElement.getType() == SubElement.JAVADOC_URL ) {
                text.append( JREMessages.LibraryLabelProvider_0 );
                URL javadocLocation = subElement.getParent().getJavadocLocation();
                if( javadocLocation != null ) {
                    text.append( javadocLocation.toExternalForm() );
                } else {
                    text.append( JREMessages.VMLibraryBlock_1 );
                }
            }
            return text.toString();
        }
        return null;
    }
}
