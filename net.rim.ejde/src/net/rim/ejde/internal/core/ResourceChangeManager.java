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

import java.util.Hashtable;
import java.util.Map.Entry;

import net.rim.ejde.internal.builders.CompilerToAppDescriptorManager;
import net.rim.ejde.internal.builders.ResourceBuilder;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.ui.dialogs.PreprocessHookInstallDialog;
import net.rim.ejde.internal.validation.ValidationManager;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * This is the main resource change listener used by ejde to listen to eclipse resource changes.
 *
 * @author Zqiu, Jheifetz
 */
public class ResourceChangeManager implements IResourceChangeListener, IResourceDeltaVisitor {
    static private final Logger _log = Logger.getLogger( ResourceChangeManager.class );

    private static class ResourceChangeManagerHolder {
        public static ResourceChangeManager resourceChangeManager = new ResourceChangeManager();
    }

    /**
     * Instantiates a new resource change manager.
     */
    private ResourceChangeManager() {
    }

    /**
     * Gets the filter for which events the resource manager is listening to.
     *
     * @return the filter
     */
    public static int getFilter() {
        return IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_BUILD
                | IResourceChangeEvent.POST_BUILD;
    }

    /**
     * Gets the single instance of ResourceChangeManager.
     *
     * @return single instance of ResourceChangeManager
     */
    static public ResourceChangeManager getInstance() {
        return ResourceChangeManagerHolder.resourceChangeManager;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org .eclipse.core.resources.IResourceChangeEvent)
     */
    @Override
    public void resourceChanged( final IResourceChangeEvent event ) {
        final int type = event.getType();
        if( type == IResourceChangeEvent.PRE_DELETE ) {
            final IResource resource = event.getResource();
            if( resource instanceof IProject ) {
                final IProject proj = (IProject) resource;
                try {
                    if( proj.hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                        // If the project is to be deleted, remove the properties from the cache
                        ContextManager.PLUGIN.removeBBProperties( proj.getName() );
                    }
                } catch( final CoreException ce ) {
                    _log.debug( "Error removing BB properties from Cache on project <" + proj.getName() + "> deletion", ce );
                }
            }
        } else if( type == IResourceChangeEvent.POST_CHANGE ) {
            final IResourceDelta delta = event.getDelta();
            if( null != delta ) {
                try {
                    delta.accept( this );
                } catch( final CoreException ce ) {
                    _log.error( "Post Change Visitor Error", ce );
                }
            }
        } else if( type == IResourceChangeEvent.PRE_BUILD ) {
            PreprocessHookInstallDialog.setIsDialogOn( false );
        } else if( type == IResourceChangeEvent.POST_BUILD ) {
            ResourceBuilder.cleanTmpDir();
        }
    }

    /**
     * The listener interface for receiving IResourceChangeEvent events. This class is specifically interested in closing the
     * associated editor when the resource is closed or deleted.
     *
     * @param delta
     *            the delta
     *
     * @return true, if visit
     *
     * @throws CoreException
     *             the core exception
     *
     * @see DetectResourceChangeEvent
     * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse .core.resources.IResourceDelta)
     */
    public boolean visit( final IResourceDelta delta ) throws CoreException {
        final IResource resource = delta.getResource();
        if( resource instanceof IFile ) {
            final IFile file = (IFile) resource;
            boolean exists;
            if( file.getName().equals( BlackBerryProject.METAFILE ) ) {
                if( delta.getKind() == IResourceDelta.CHANGED ) {
                    // force model re-load
                    ContextManager.PLUGIN.getBBProperties( file.getProject().getName(), true );

                    IJavaProject javaProject = JavaCore.create( file.getProject() );
                    CompilerToAppDescriptorManager.onProjectPropertiesChange( javaProject );

                    IProject project = file.getProject();
                    EJDEEventNotifier.getInstance().notifyProjectPropertiesChanged( project );
                }
                return false;
            } else if( file.getFileExtension() != null && file.getFileExtension().equals( IConstants.KEY_FILE_EXTENSION ) ) {
                if( delta.getKind() == IResourceDelta.REMOVED ) {
                    // if .key file is deleted, delete class/package protection in the model
                    final String projectName = resource.getProject().getName();
                    final BlackBerryProperties properties = ContextManager.PLUGIN.getBBProperties( projectName, false );
                    final Hashtable< String, String > packageProtection = properties._hiddenProperties.getPackageProtection();
                    final Hashtable< String, String > classProtection = properties._hiddenProperties.getClassProtection();
                    boolean changed = false;
                    final Hashtable< String, String > newPackageProtection = new Hashtable< String, String >();
                    final Hashtable< String, String > newClassProtection = new Hashtable< String, String >();
                    String path = file.getProjectRelativePath().toOSString();
                    for( Entry< String, String > entry : packageProtection.entrySet() ) {
                        String value = entry.getValue();
                        if( value != null ) {
                            if( path.equals( value ) ) {
                                changed = true;
                            } else {
                                newPackageProtection.put( entry.getKey(), value );
                            }
                        }
                    }
                    for( Entry< String, String > entry : classProtection.entrySet() ) {
                        String value = entry.getValue();
                        if( value != null ) {
                            if( path.equals( value ) ) {
                                changed = true;
                            } else {
                                newClassProtection.put( entry.getKey(), value );
                            }
                        }
                    }
                    if( changed ) {
                        new Thread() {
                            @Override
                            public void run() {
                                properties._hiddenProperties.setPackageProtection( newPackageProtection );
                                properties._hiddenProperties.setClassProtection( newClassProtection );
                                ContextManager.PLUGIN.setBBProperties( projectName, properties, true );
                            }
                        }.start();
                    }
                }
                return false;
            } else if( !( exists = file.exists() ) && delta.getKind() == IResourceDelta.REMOVED || exists
                    && delta.getKind() == IResourceDelta.ADDED || file.isLinked() && delta.getKind() == IResourceDelta.CHANGED ) {
                IProject iproj = file.getProject();
                BlackBerryProperties bbprops = ContextManager.PLUGIN.getBBProperties( iproj.getName(), false );
                // Skip validation for closed projects
                if( bbprops.mayReferFile( file.getName() ) && iproj.isOpen() ) {
                    // re-validate the App_Descriptor just in case any of these was used
                    ValidationManager.getInstance().validateProject( iproj, null );
                }
            }
        }
        return true;
    }
}
