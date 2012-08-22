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

import java.util.ArrayList;
import java.util.List;

import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;
import net.rim.ejde.internal.ui.dialogs.CleanProjectsDialog;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;
import net.rim.ejde.internal.util.Messages;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.PlatformUI;

/**
 * This class is used to listen to property changes which require corresponding actions, e.g. workspace level preprocess defines
 * change.
 */
public class PropertyChangeListenerImp implements IPropertyChangeListener {

    static private final Logger log = Logger.getLogger( PropertyChangeListenerImp.class );

    private static class PropertyChangeListenerHolder {
        public static PropertyChangeListenerImp propertyChangeListenerImp = new PropertyChangeListenerImp();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
     */
    public void propertyChange( PropertyChangeEvent event ) {
        log.trace( event.getProperty() + " was changed from \"" + event.getOldValue() + "\" to \"" + event.getNewValue() + "\"" );
        if( event.getProperty().equalsIgnoreCase( PreferenceConstants.PREPROCESSOR_DEFINE_LIST ) ) {
            rebuildWorkspace( Messages.CleanProjectsDialog_PreprocessTag_description );
            EJDEEventNotifier.getInstance().notifyWorkspacePreprocessorTagsChanged();
        } else if( event.getProperty().equalsIgnoreCase( PreferenceConstants.RUN_SIGNATURE_TOOL_AUTOMATICALLY ) ) {
            Object newValue = event.getNewValue();
            if( newValue instanceof Boolean ) {
                if( ( (Boolean) newValue ).booleanValue() ) {
                    rebuildWorkspace( Messages.CleanProjectsDialog_CodeSigning_description );
                }
            } else if( event.getNewValue() instanceof String ) {
                // Fix MKS518907: When import BlackBerry preference, the new value is a String instead of Boolean
                if( Boolean.parseBoolean( (String) newValue ) ) {
                    rebuildWorkspace( Messages.CleanProjectsDialog_CodeSigning_description );
                }
            }
        }
    }

    /**
     * Rebuild workspace.
     *
     * @param msg
     *            the msg
     */
    private void rebuildWorkspace( String msg ) {
        // if workspace level preprocess defines have been changed, ask users for a clean
        IProject[] projects = getProjects();
        if( projects.length > 0 ) {
            new CleanProjectsDialog( PlatformUI.getWorkbench().getActiveWorkbenchWindow(), projects, msg ).open();
        }
    }

    /**
     * Gets the projects.
     *
     * @return the projects
     */
    private IProject[] getProjects() {
        List< IProject > bbProjects = new ArrayList< IProject >();
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for( IProject project : projects ) {
            try {
                if( project.hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                    bbProjects.add( project );
                }
            } catch( CoreException e ) {
                log.error( e.getMessage() );
            }
        }
        return bbProjects.toArray( new IProject[ bbProjects.size() ] );
    }

    /**
     * Adds the listener.
     */
    static public void addListener() {
        ContextManager.getDefault().getPreferenceStore()
                .addPropertyChangeListener( PropertyChangeListenerHolder.propertyChangeListenerImp );
        log.debug( "Property change listener is enabled." );
    }

    /**
     * Removes the listener.
     */
    static public void removeListener() {
        ContextManager.getDefault().getPreferenceStore()
                .removePropertyChangeListener( PropertyChangeListenerHolder.propertyChangeListenerImp );
        log.debug( "Property change listener is disabled." );
    }
}
