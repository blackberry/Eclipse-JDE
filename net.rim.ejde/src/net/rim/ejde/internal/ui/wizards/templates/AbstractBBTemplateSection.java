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
package net.rim.ejde.internal.ui.wizards.templates;

import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import net.rim.ejde.internal.core.ContextManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.templates.OptionTemplateSection;
import org.osgi.framework.Bundle;

public abstract class AbstractBBTemplateSection extends OptionTemplateSection {

    public static final String KEY_CLASS_NAME = "appClassName"; //$NON-NLS-1$

    public AbstractBBTemplateSection() {
    }

    public String[] getNewFiles() {
        return new String[ 0 ];
    }

    protected URL getInstallURL() {
        return ContextManager.PLUGIN.getInstallURL();
    }

    protected void initializeFields( IFieldData data ) {
        // do nothing
    }

    public String getUsedExtensionPoint() {
        return "org.eclipse.ui.actionSets"; //$NON-NLS-1$
    }

    protected String getTemplateDirectory() {
        return "templates"; //$NON-NLS-1$
    }

    public void updateModel( IProgressMonitor monitor ) throws CoreException {
        // do nothing
    }

    public void initializeFields( IPluginModelBase model ) {
        // do nothing
    }

    public boolean isDependentOnParentWizard() {
        return true;
    }

    protected String getFormattedPackageName( String id ) {
        StringBuffer buffer = new StringBuffer();
        for( int i = 0; i < id.length(); i++ ) {
            char ch = id.charAt( i );
            if( buffer.length() == 0 ) {
                if( Character.isJavaIdentifierStart( ch ) )
                    buffer.append( Character.toLowerCase( ch ) );
            } else {
                if( Character.isJavaIdentifierPart( ch ) || ch == '.' )
                    buffer.append( ch );
            }
        }
        return buffer.toString().toLowerCase( Locale.ENGLISH );
    }

    protected ResourceBundle getPluginResourceBundle() {
        Bundle bundle = Platform.getBundle( ContextManager.PLUGIN_ID );
        return Platform.getResourceBundle( bundle );
    }

    /**
     * Update the wizard page error message.
     *
     * @param page
     *            The wizard page
     * @param names
     *            The component name with error
     * @param status
     *            The error status
     */
    protected void updateStatus( WizardPage page, List< String > names, IStatus status ) {
        if( status.getSeverity() == IStatus.ERROR ) {
            page.setPageComplete( false );
            IStatus[] children = status.getChildren();
            for( int i = 0; i < children.length; i++ ) {
                if( children[ i ].getSeverity() == IStatus.ERROR ) {
                    if( names.get( i ).length() > 0 ) {
                        page.setMessage( names.get( i ) + ": " + children[ i ].getMessage(), IMessageProvider.ERROR );
                    } else {
                        page.setMessage( children[ i ].getMessage(), IMessageProvider.ERROR );
                    }
                    break;
                }
            }
        } else if( status.getSeverity() == IStatus.WARNING ) {
            page.setPageComplete( true );
            IStatus[] children = status.getChildren();
            for( int i = 0; i < children.length; i++ ) {
                if( children[ i ].getSeverity() == IStatus.WARNING ) {
                    if( names.get( i ).length() > 0 ) {
                        page.setMessage( names.get( i ) + ": " + children[ i ].getMessage(), IMessageProvider.WARNING );
                    } else {
                        page.setMessage( children[ i ].getMessage(), IMessageProvider.ERROR );
                    }
                    break;
                }
            }
        } else {
            page.setPageComplete( true );
            page.setMessage( null );
        }
    }
}
