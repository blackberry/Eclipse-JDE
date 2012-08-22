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
package net.rim.ejde.internal.ui.editors.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import net.rim.ejde.internal.core.ClasspathElementChangedListener;
import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.EJDEEventAdapter;
import net.rim.ejde.internal.core.EJDEEventNotifier;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.core.WorkspaceOperation;
import net.rim.ejde.internal.core.WorkspaceOperationRunner;
import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.AlternateEntryPoint;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.Icon;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.PreprocessorTag;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.packaging.PackagingManager;
import net.rim.ejde.internal.util.FileUtils;
import net.rim.ejde.internal.util.ImportUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PackageUtils;
import net.rim.ejde.internal.util.PackagingUtils;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ide.Project;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

import com.ibm.icu.text.MessageFormat;

// TODO: Auto-generated Javadoc
/**
 * This class creates the project application descriptor editor for modifying the model (.xml).
 *
 * @author cmalinescu, jkeshavarzi, jheifetz
 */
@InternalFragmentReplaceable
public abstract class BlackBerryProjectFormEditorBase extends FormEditor {
    public static final String EDITOR_ID = "net.rim.ejde.BlackBerryProjectFormEditor"; //$NON-NLS-1$

    protected static final Logger _log = Logger.getLogger( BlackBerryProjectFormEditor.class );
    private final PropertyChangeSupport pcs = new PropertyChangeSupport( this );

    private Boolean _isDirty = Boolean.FALSE;
    private BlackBerryProject _blackBerryProject;
    private BlackBerryProperties _propertiesClone;
    private BlackBerryProperties _propertiesCache;
    protected Image _applicationImage = null;
    private long _modificationStamp = IResource.NULL_STAMP;

    protected BlackBerryProjectApplicationPage _applicationPage;
    protected BlackBerryProjectBuildPage _buildPage;
    protected BlackBerryProjectAlternateEntryPointPage _aepPage;
    private BlackBerryProjectOutlinePage contentOutlinePage;

    private BBFileInputListener _resourceListener;
    private ActivationListener _activationListener;
    private MyClassPathChangeListener _classPathChangeListener;

    /**
     * The listener interface for receiving IResourceChangeEvent events. This class is specifically interested in closing the
     * associated editor when the resource is closed or deleted.
     *
     * @see IResourceChangeEvent
     */
    protected class BBFileInputListener implements IResourceChangeListener, IResourceDeltaVisitor {
        private final IFile _fInput;

        /**
         * Instantiates a new bB file input listener.
         *
         * @param fileEdInput
         *            the file ed input
         */
        public BBFileInputListener( final IFile fileEdInput ) {
            super();
            _fInput = fileEdInput;
            ResourcesPlugin.getWorkspace().addResourceChangeListener( this, IResourceChangeEvent.POST_CHANGE );
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged
         * (org.eclipse.core.resources.IResourceChangeEvent)
         */
        @Override
        public void resourceChanged( final IResourceChangeEvent event ) {
            if( event.getType() == IResourceChangeEvent.POST_CHANGE ) {
                final IResourceDelta delta = event.getDelta();
                IResourceDelta deltaMember = delta.findMember( _fInput.getFullPath() );
                if( null != deltaMember ) {
                    try {
                        deltaMember.accept( this );
                    } catch( final CoreException ce ) {
                        _log.error( "Resource Visitor Error", ce ); //$NON-NLS-1$
                    }
                } else {
                    Set< IProject > allEffectedProjects;
                    try {
                        allEffectedProjects = ProjectUtils.getAllReferencedProjects( getBlackBerryProject().getProject() );
                        for( IProject project : allEffectedProjects ) {
                            deltaMember = delta.findMember( project.getFullPath() );
                            if( null != deltaMember ) {
                                try {
                                    deltaMember.accept( this );
                                } catch( final CoreException ce ) {
                                    _log.error( "Resource Visitor Error", ce ); //$NON-NLS-1$
                                }
                            }
                        }
                    } catch( CoreException e ) {
                        _log.error( e );
                    }
                }
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse .core.resources.IResourceDelta)
         */
        public boolean visit( final IResourceDelta delta ) throws CoreException {
            final IResource resource = delta.getResource();
            if( resource.isDerived() ) {
                return false;
            }
            if( resource instanceof IFile ) {
                final IFile file = (IFile) resource;
                if( file.equals( _fInput ) ) {
                    if( ( delta.getKind() == IResourceDelta.REMOVED ) || ( delta.getKind() == IResourceDelta.REPLACED ) ) {
                        final Display display = getSite().getShell().getDisplay();
                        display.asyncExec( new Runnable() {
                            public void run() {
                                getSite().getPage().closeEditor( BlackBerryProjectFormEditorBase.this, false );
                            }
                        } );
                    }
                    return false;
                }
                // Check if the delta affects a resource. If it does, update the
                // resources section controls.
                final String fileName = file.getName();
                if( PackageUtils.hasRRHExtension( fileName ) ) {
                    Display.getDefault().asyncExec( new Runnable() {
                        @Override
                        public void run() {
                            if( ( _applicationPage != null ) && _applicationPage.isFormCreated() ) {
                                _applicationPage.getResourcesSection().refreshControls( false );
                            }

                            if( ( _aepPage != null ) && _aepPage.isFormCreated() ) {
                                if( _aepPage.getAlternateEntryPointDetails() != null ) {
                                    _aepPage.getAlternateEntryPointDetails().getResourcesSection().refreshControls( false );
                                }
                            }
                        }
                    } );
                } else if( fileName.endsWith( IConstants.ALX_FILE_EXTENSION_WITH_DOT ) ) {
                    Display.getDefault().asyncExec( new Runnable() {
                        @Override
                        public void run() {
                            // Check if the delta affects an ALX file specified
                            // in editor. If it does, re-validate.
                            if( ( _buildPage != null ) && _buildPage.isFormCreated() ) {
                                String alxFiles[] = _buildPage.getAlxFileSection().getAlxFiles();
                                for( String alxFile : alxFiles ) {
                                    if( file.getProjectRelativePath().equals( new Path( alxFile ) ) ) {
                                        _buildPage.getAlxFileSection().validateAlxFiles();
                                    }
                                }
                            }
                        }
                    } );
                } else if( BlackBerryProjectPropertiesPage.isImage( fileName ) ) {
                    Display.getDefault().asyncExec( new Runnable() {
                        @Override
                        public void run() {
                            // Check if the delta affects a icon selected in
                            // editor. If it does, re-validate.
                            if( ( _applicationPage != null ) && _applicationPage.isFormCreated() ) {
                                Icon applicationIcons[] = _applicationPage.getIconSection().getIcons();
                                for( Icon icon : applicationIcons ) {
                                    if( file.getName().equals( new Path( icon.getCanonicalFileName() ).lastSegment() ) ) {
                                        _applicationPage.getIconSection().validateIcons();
                                    }
                                }
                            }

                            if( ( _aepPage != null ) && _aepPage.isFormCreated() ) {
                                AlternateEntryPointIconsSection iconSection = _aepPage.getAlternateEntryPointDetails()
                                        .getIconsSection();
                                Icon aepIcons[] = iconSection.getIcons();
                                for( Icon icon : aepIcons ) {
                                    if( file.getName().equals( new Path( icon.getCanonicalFileName() ).lastSegment() ) ) {
                                        iconSection.validateIcons();
                                    }
                                }
                            }
                        }
                    } );
                }
            }
            return true;
        }

        /**
         * Dispose.
         */
        public void dispose() {
            ResourcesPlugin.getWorkspace().removeResourceChangeListener( this );
        }
    }

    /**
     * The listener interface for receiving activation events. The class that is interested in processing a activation event
     * implements this interface, and the object created with that class is registered with a component using the component's
     * <code>addActivationListener<code> method. When
     * the activation event occurs, that object's appropriate
     * method is invoked.
     *
     * @see AbstractTextEditor.ActivationListener
     */
    protected class ActivationListener implements IPartListener, IWindowListener {
        /** Cache of the active workbench part. */
        private IWorkbenchPart _activePart;
        /** Indicates whether activation handling is currently be done. */
        private boolean _isHandlingActivation = false;
        /**
         * The part service.
         *
         * @since 3.1
         */
        private IPartService _partService;

        /**
         * Creates this activation listener.
         *
         * @param partService
         *            the part service on which to add the part listener
         */
        public ActivationListener( IPartService partService ) {
            _partService = partService;
            _partService.addPartListener( this );
            PlatformUI.getWorkbench().addWindowListener( this );
        }

        /**
         * Disposes this activation listener.
         */
        public void dispose() {
            _partService.removePartListener( this );
            PlatformUI.getWorkbench().removeWindowListener( this );
            _partService = null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.IPartListener#partActivated(org.eclipse.ui.IWorkbenchPart )
         */
        @Override
        public void partActivated( IWorkbenchPart part ) {
            _activePart = part;
            handleActivation();
        }

        /*
         * (non-Javadoc)
         *
         * @seeorg.eclipse.ui.IPartListener#partBroughtToTop(org.eclipse.ui. IWorkbenchPart)
         */
        @Override
        public void partBroughtToTop( IWorkbenchPart part ) {
            // Do Nothing
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.IPartListener#partClosed(org.eclipse.ui.IWorkbenchPart )
         */
        @Override
        public void partClosed( IWorkbenchPart part ) {
            // Do Nothing
        }

        /*
         * (non-Javadoc)
         *
         * @seeorg.eclipse.ui.IPartListener#partDeactivated(org.eclipse.ui. IWorkbenchPart)
         */
        @Override
        public void partDeactivated( IWorkbenchPart part ) {
            _activePart = null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.IPartListener#partOpened(org.eclipse.ui.IWorkbenchPart )
         */
        @Override
        public void partOpened( IWorkbenchPart part ) {
            /**
             * If we ever want to save this into IMementos we would load it here.
             */
        }

        /*
         * (non-Javadoc)
         *
         * @seeorg.eclipse.ui.IWindowListener#windowActivated(org.eclipse.ui. IWorkbenchWindow)
         */
        @Override
        public void windowActivated( IWorkbenchWindow window ) {
            if( window == getEditorSite().getWorkbenchWindow() ) {
                /*
                 * REFERENCED FROM AbstractTextEditor.ActivationListener
                 *
                 * Workaround for problem described in http://dev.eclipse.org/bugs/show_bug.cgi?id=11731 Will be removed when SWT
                 * has solved the problem.
                 */
                window.getShell().getDisplay().asyncExec( new Runnable() {
                    public void run() {
                        handleActivation();
                    }
                } );
            }
        }

        /*
         * (non-Javadoc)
         *
         * @seeorg.eclipse.ui.IWindowListener#windowClosed(org.eclipse.ui. IWorkbenchWindow)
         */
        @Override
        public void windowClosed( IWorkbenchWindow window ) {
            // Do Nothing
        }

        /*
         * (non-Javadoc)
         *
         * @seeorg.eclipse.ui.IWindowListener#windowDeactivated(org.eclipse.ui. IWorkbenchWindow)
         */
        @Override
        public void windowDeactivated( IWorkbenchWindow window ) {
            // Do Nothing
        }

        /*
         * (non-Javadoc)
         *
         * @seeorg.eclipse.ui.IWindowListener#windowOpened(org.eclipse.ui. IWorkbenchWindow)
         */
        @Override
        public void windowOpened( IWorkbenchWindow window ) {
            // Do Nothing
        }

        /**
         * Handles the activation triggering a element state check in the editor.
         */
        private void handleActivation() {
            if( _isHandlingActivation ) {
                return;
            }

            if( _activePart == BlackBerryProjectFormEditorBase.this ) {
                _isHandlingActivation = true;
                try {
                    long newStamp = getInputHandler().getModificationStamp();
                    if( newStamp != _modificationStamp ) {
                        doLoad( new NullProgressMonitor() );
                        _modificationStamp = newStamp;
                    }

                } finally {
                    _isHandlingActivation = false;
                }
            }
        }
    }

    private class MyClassPathChangeListener extends EJDEEventAdapter {
        public MyClassPathChangeListener() {
            EJDEEventNotifier.getInstance().addEJDEEventListener( this );
        }

        /**
         * Dispose.
         */
        public void dispose() {
            EJDEEventNotifier.getInstance().removeEJDEEventListener( this );
        }

        @Override
        public void classPathChanged( IJavaProject project, boolean isProjectJREChange ) {
            Display.getDefault().asyncExec( new Runnable() {
                @Override
                public void run() {
                    if( ( _applicationPage != null ) && _applicationPage.isFormCreated() ) {
                        _applicationPage.getResourcesSection().refreshControls( false );
                    }

                    if( ( _aepPage != null ) && _aepPage.isFormCreated() ) {
                        if( _aepPage.getAlternateEntryPointDetails() != null ) {
                            _aepPage.getAlternateEntryPointDetails().getResourcesSection().refreshControls( false );
                        }
                    }
                }
            } );
        }

    }

    /**
     * Instantiates a new black berry project form editor.
     */
    public BlackBerryProjectFormEditorBase() {
        // Do Nothing
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.forms.editor.FormEditor#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
     */
    @Override
    public void init( IEditorSite site, IEditorInput input ) throws PartInitException {
        super.init( site, input );
        setPartName( getIProject().getName() );
        _resourceListener = new BBFileInputListener( ( (FileEditorInput) input ).getFile() );
        _activationListener = new ActivationListener( site.getWorkbenchWindow().getPartService() );
        _classPathChangeListener = new MyClassPathChangeListener();
    }

    protected void addListener( String event, PropertyChangeListener changeListener ) {
        pcs.addPropertyChangeListener( event, changeListener );
    }

    protected void addListener( PropertyChangeListener changeListener ) {
        pcs.addPropertyChangeListener( changeListener );
    }

    protected PropertyChangeListener[] getListeners( String event ) {
        return pcs.getPropertyChangeListeners( event );
    }

    protected void notifyListeners( String event, Object oldValue, Object newValue ) {
        pcs.firePropertyChange( event, oldValue, newValue );
    }

    @Override
    public void pageChange( int newPageIndex ) {
        int oldPageIndex = getCurrentPage();
        if( oldPageIndex != newPageIndex ) {
            if( contentOutlinePage != null ) {
                contentOutlinePage.updateTreeSelection( (BlackBerryProjectPropertiesPage) pages.get( newPageIndex ) );
            }
            if( oldPageIndex != -1 ) {
                IFormPage oldFormPage = (IFormPage) pages.get( oldPageIndex );
                IManagedForm mform = oldFormPage.getManagedForm();
                if( mform != null ) {
                    mform.commit( false );
                }
            }
        }
        super.pageChange( newPageIndex );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.forms.editor.FormEditor#dispose()
     */
    @Override
    public void dispose() {
        if( _resourceListener != null ) {
            _resourceListener.dispose();
            _resourceListener = null;
        }

        if( _activationListener != null ) {
            _activationListener.dispose();
            _activationListener = null;
        }

        if( _classPathChangeListener != null ) {
            _classPathChangeListener.dispose();
            _classPathChangeListener = null;
        }

        if( _buildPage != null && _buildPage.getPreprocessorTagSection() != null
                && _buildPage.getPreprocessorTagSection().getUI() != null ) {
            _buildPage.getPreprocessorTagSection().getUI().removeListener();
        }

        super.dispose();
    }

    /**
     * Gets the i project.
     *
     * @return the i project
     */
    protected IProject getIProject() {
        IEditorInput editorInput = getEditorInput();
        IFile eclipseFileHandler = (IFile) editorInput.getAdapter( IFile.class );
        return eclipseFileHandler.getProject();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
     */
    @Override
    protected void addPages() {
        _propertiesCache = ContextManager.PLUGIN.getBBProperties( getEclipseJavaProject().getProject().getName(), false );

        if( _propertiesCache != null ) {
            try {
                // Clone the cache.
                _propertiesClone = (BlackBerryProperties) _propertiesCache.clone();
            } catch( CloneNotSupportedException e1 ) {
                _log.error( "Error cloning properties cache", e1 ); //$NON-NLS-1$
            }
        }

        if( _propertiesClone != null ) {
            _blackBerryProject = new BlackBerryProject( getEclipseJavaProject(), _propertiesClone );
        } else {
            _blackBerryProject = new BlackBerryProject( getEclipseJavaProject() );
            _propertiesClone = _blackBerryProject.getProperties();
        }

        _modificationStamp = getInputHandler().getModificationStamp();
        doAddPages();
    }

    protected abstract void doAddPages();

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {
        // Do Nothing
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.forms.editor.FormEditor#isDirty()
     */
    @Override
    public boolean isDirty() {
        return _isDirty.booleanValue();
    }

    /**
     * Sets the dirty state.
     *
     * @param isDirty
     *            the new dirty state
     */
    public void setDirty( Boolean isDirty ) {
        if( _isDirty.equals( isDirty ) ) {
            return;
        }

        _isDirty = isDirty;
        editorDirtyStateChanged();
    }

    /**
     * Do save.
     *
     * @param monitor
     *            the monitor
     *
     * @see org.eclipse.ui.forms.editor.FormPage#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave( IProgressMonitor monitor ) {
        try {
            WorkspaceOperation operation = new SaveOperation();
            IRunnableContext runner = new WorkspaceOperationRunner();
            runner.run( false, false, operation );
        } catch( InvocationTargetException e ) {
            _log.error( e );
        } catch( InterruptedException e ) {
            _log.error( e );
        }
    }

    /**
     * Something need to be done before the model changes are saved.
     */
    private void preSaveProcess() {
        buildProject();
        deleteOldAlxFile();
    }

    private void deleteOldAlxFile() {
        BlackBerryProperties oldProperties = ContextManager.PLUGIN.getBBProperties(
                getBlackBerryProject().getProject().getName(), false );
        String oldOutputName = oldProperties._packaging.getOutputFileName();
        String newOutputName = _propertiesClone._packaging.getOutputFileName();
        if( !oldOutputName.equals( newOutputName ) ) {
            String alxFileFolderPath = PackagingUtils.getRelativeAlxFileOutputFolder( getBlackBerryProject() );
            IFolder alxFileFolder = getBlackBerryProject().getProject().getFolder( new Path( alxFileFolderPath ) );
            IFile alxFile = alxFileFolder.getFile( oldOutputName + IConstants.ALX_FILE_EXTENSION_WITH_DOT );
            if( alxFile.exists() ) {
                try {
                    alxFile.delete( true, new NullProgressMonitor() );
                } catch( CoreException e ) {
                    _log.error( e );
                }
            }
        }
    }

    private boolean isProjectTypeChanged() {
        BlackBerryProperties oldProperties = ContextManager.PLUGIN.getBBProperties(
                getBlackBerryProject().getProject().getName(), false );
        if( oldProperties == null ) {
            return false;
        }
        int oldProjectType = PackagingManager.getProjectTypeID( oldProperties._application.getType() );
        int newProjectType = PackagingManager.getProjectTypeID( _propertiesClone._application.getType() );
        if( oldProjectType != newProjectType && ( oldProjectType == Project.LIBRARY || newProjectType == Project.LIBRARY ) ) {
            return true;
        }
        return false;
    }

    private void checkProjectDependency() {
        IProject project = getBlackBerryProject().getProject();
        IProject[] dependentProjects = project.getReferencingProjects();
        for( IProject dependentProject : dependentProjects ) {
            ClasspathElementChangedListener.hasProjectDependencyProblem( JavaCore.create( dependentProject ) );
        }
    }

    /**
     * Builds the project if necessary.
     * <p>
     * <b> Be aware, this method must be called before the dirtied model has been committed. </b>
     */
    private void buildProject() {
        if( needBuildProject() ) {
            IProject project = getBlackBerryProject().getProject();
            try {
                project.build( IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor() );
            } catch( CoreException e ) {
                _log.error( e.getMessage() );
            }
        }
    }

    private boolean needBuildProject() {
        BlackBerryProperties oldProperties = ContextManager.PLUGIN.getBBProperties(
                getBlackBerryProject().getProject().getName(), false );
        PreprocessorTag[] oldTags = oldProperties._compile.getPreprocessorDefines();
        PreprocessorTag[] newTags = _propertiesClone._compile.getPreprocessorDefines();
        if( oldTags.length != newTags.length ) {
            return true;
        }
        Arrays.sort( oldTags );
        Arrays.sort( newTags );
        for( int i = 0; i < oldTags.length; i++ ) {
            if( ( !oldTags[ i ].equals( newTags[ i ] ) ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates all controls within each page with property values obtained from the current model store.
     *
     * @param monitor
     *            the monitor
     */
    public void doLoad( IProgressMonitor monitor ) {
        BlackBerryProperties properties = ContextManager.PLUGIN.getBBProperties( getBlackBerryProject().getProject().getName(),
                false );
        if( _propertiesClone.equals( properties ) ) {
            return;
        }

        if( isDirty() ) {
            final String title = Messages.BlackBerryProjectFormEditor_Res_Chng_Diag_Title;
            String msg = Messages.BlackBerryProjectFormEditor_Res_Chng_Diag_Msg;
            msg = MessageFormat.format( msg, new Object[] { getEditorInput().getName() } );
            final Shell shell = getSite().getShell();
            if( !MessageDialog.openQuestion( shell, title, msg ) ) {
                return;
            }
        }
        _propertiesClone = properties;
        _blackBerryProject.setProperties( _propertiesClone );

        Display.getDefault().asyncExec( new Runnable() {
            @Override
            public void run() {
                if( _applicationPage.isFormCreated() ) {
                    /**
                     * General model - pulling from the view
                     * */
                    _applicationPage.getIconSection().insertControlValuesFromModel( _propertiesClone );

                    /**
                     * Resources model - pulling from the view
                     * */
                    _applicationPage.getResourcesSection().insertControlValuesFromModel();

                    _applicationPage.getIconSection().insertControlValuesFromModel( _propertiesClone );
                    Icon[] icons = _propertiesClone._resources.getIconFiles();

                    // Link any missing external icons
                    linkExternalIcons( icons );

                } else {
                    _applicationPage.getBlackBerryProject().setProperties( _propertiesClone );
                }

                if( _buildPage.isFormCreated() ) {
                    _buildPage.getPreprocessorTagSection().insertControlValuesFromModel( _propertiesClone );

                    /**
                     * Build model - pulling from the view
                     * */
                    _buildPage.getPackagingSection().insertControlValuesFromModel( _propertiesClone );
                    _buildPage.getAlxFileSection().insertControlValuesFromModel( _propertiesClone );
                } else {
                    _buildPage.getBlackBerryProject().setProperties( _propertiesClone );
                }

                if( _aepPage.isFormCreated() ) {
                    /**
                     * Alternate entry points properties - pulling from the view
                     * */

                    AlternateEntryPoint aeps[] = _propertiesClone.getAlternateEntryPoints();

                    // Link any missing external icons from each aep
                    for( AlternateEntryPoint aep : aeps ) {
                        linkExternalIcons( aep.getIconFiles() );
                    }

                    _aepPage.getAlternateEntryPointSection().insertControlValuesFromModel( _propertiesClone );
                } else {
                    _aepPage.getBlackBerryProject().setProperties( _propertiesClone );
                }
                setDirty( Boolean.FALSE );
            }
        } );
    }

    /**
     * Sets the application image.
     *
     * @return the image
     */
    protected Image setApplicationImage() {
        Icon icons[] = _propertiesClone._resources.getIconFiles();
        Icon applicationIcon = null;

        Image image = null;

        if( ( null != icons ) && ( icons.length > 0 ) ) {
            for( Icon icon : icons ) {
                if( !icon.isFocus().booleanValue() ) {
                    applicationIcon = icon;
                    break;
                }
            }
            image = createIconImage( applicationIcon );
        }
        return image;
    }

    /**
     * Link external icons.
     *
     * @param icons
     *            the icons
     */
    public void linkExternalIcons( Icon icons[] ) {
        // Link any external icons
        for( Icon icon : icons ) {
            IPath storedLocation = new Path( icon.getCanonicalFileName() );
            IPath iconPath = null;

            if( storedLocation.isAbsolute() ) {
                iconPath = storedLocation;
            } else {
                IPath projectLocation = getBlackBerryProject().getProject().getLocation();
                iconPath = projectLocation.append( storedLocation );
            }

            IFile iconFile = ImportUtils.getProjectBasedFileFromOSBasedFile( getBlackBerryProject().getElementName(),
                    iconPath.toOSString() );
            if( iconFile != null ) {
                icon.setCanonicalFileName( iconFile.getProjectRelativePath().toPortableString() );
            } else {
                // Icon does not exist in project, link it.
                IFile linkedFile = FileUtils.addResourceToProject( FileUtils.getResFolder( getBlackBerryProject().getProject() ),
                        iconPath.toFile(), Boolean.TRUE );
                if( linkedFile != null ) {
                    // Change icon file path to new link location
                    icon.setCanonicalFileName( linkedFile.getProjectRelativePath().toPortableString() );
                }
            }
        }
    }

    /**
     * Link an external file.
     *
     * @param filePath
     *            the external file
     */
    public IPath linkExternalFile( IPath filePath ) {
        IPath absolutePath = null;

        if( filePath.isAbsolute() ) {
            absolutePath = filePath;
        } else {
            IPath projectLocation = getBlackBerryProject().getProject().getLocation();
            absolutePath = projectLocation.append( filePath );
        }

        IFile existingFile = ImportUtils.getProjectBasedFileFromOSBasedFile( getBlackBerryProject().getElementName(),
                absolutePath.toOSString() );
        if( existingFile != null ) {
            return existingFile.getProjectRelativePath();
        } else {
            // file does not exist in project, link it.
            IFile linkedFile = FileUtils.addResourceToProject( getBlackBerryProject().getProject(), absolutePath.toFile(), true );

            if( linkedFile != null ) {
                // Change file path to new link location
                return linkedFile.getProjectRelativePath();
            }
        }
        return new Path( IConstants.EMPTY_STRING );
    }

    /**
     * Returns the file paths relative path to the project.
     *
     * @param filepath
     *            the filepath
     *
     * @return The new IPath relative value
     */
    public IPath makeRelative( IPath filepath ) {
        IPath projectLocation = _blackBerryProject.getProject().getLocation();
        return filepath.makeRelativeTo( projectLocation );
    }

    /**
     * Creates the icon image.
     *
     * @param icon
     *            the icon
     *
     * @return the image
     */
    protected Image createIconImage( Icon icon ) {
        Display display = Display.getCurrent();
        FileInputStream stream = null;
        Image image = null;

        if( icon == null ) {
            return null;
        }

        try {
            try {
                // TODO: Try to construct the icon with forward slash.
                Path storedLocation = new Path( icon.getCanonicalFileName().replace( '\\', '/' ) );

                if( storedLocation.isAbsolute() ) {
                    stream = new FileInputStream( storedLocation.toOSString() );
                } else {
                    IPath projectLocation = getBlackBerryProject().getProject().getLocation();
                    IPath iconLocation = projectLocation.append( storedLocation );
                    IFile iconFile = ImportUtils.getProjectBasedFileFromOSBasedFile( getBlackBerryProject().getElementName(),
                            iconLocation.toOSString() );
                    if( iconFile != null ) {
                        // Icon file found in project (copied or linked, both
                        // scenarios covered).
                        stream = new FileInputStream( iconFile.getLocation().toOSString() );
                    } else {
                        // Icon file was not found in project, try and create a
                        // stream using the calculated path
                        stream = new FileInputStream( iconLocation.toOSString() );
                    }
                }

                ImageData data = new ImageData( stream );
                data = data.scaledTo( 16, 16 );

                if( data.transparentPixel > 0 ) {
                    image = new Image( display, data, data.getTransparencyMask() );
                } else {
                    image = new Image( display, data );
                }

            } finally {
                if( stream != null ) {
                    stream.close();
                }
            }
        } catch( Exception e ) {
            _log.error( "Error creating image: " + e.getMessage() ); //$NON-NLS-1$
        }

        return image;
    }

    /**
     * Gets the eclipse java project.
     *
     * @return the eclipse java project
     */
    protected IJavaProject getEclipseJavaProject() {
        IFile eclipseFileHandler = getInputHandler();
        return JavaCore.create( eclipseFileHandler.getProject() );
    }

    /**
     * Gets the input handler.
     *
     * @return the input handler
     */
    protected IFile getInputHandler() {
        IEditorInput editorInput = getEditorInput();
        IFile eclipseFileHandler = (IFile) editorInput.getAdapter( IFile.class );
        return eclipseFileHandler;
    }

    /**
     * Gets the black berry project.
     *
     * @return the black berry project
     */
    public BlackBerryProject getBlackBerryProject() {
        return _blackBerryProject;
    }

    /**
     * Gets the application image.
     *
     * @return the _applicationImage
     */
    public Image getApplicationImage() {
        return _applicationImage;
    }

    protected BlackBerryProjectPropertiesPage[] getPages() {
        ArrayList< BlackBerryProjectPropertiesPage > pagesList = new ArrayList< BlackBerryProjectPropertiesPage >();

        for( int i = 0; i < pages.size(); i++ ) {
            Object page = pages.get( i );
            if( page instanceof BlackBerryProjectPropertiesPage ) {
                pagesList.add( (BlackBerryProjectPropertiesPage) page );
            }
        }

        return pagesList.toArray( new BlackBerryProjectPropertiesPage[ pagesList.size() ] );
    }

    protected void setActivePage( int pageIndex ) {
        super.setActivePage( pageIndex );
    }

    @Override
    public void setFocus() {
        super.setFocus();

    }

    @Override
    public Object getAdapter( Class key ) {
        // TODO Uncomment the below code to re-enable the outline view
        // prototype. Currently disabled in headrev until
        // implementation officially begins. Please refer to the "OutlinePage"
        // javadoc description for more info.
        // if( key.equals( IContentOutlinePage.class ) ) {
        // return getContentOutlinePage();
        // }
        return super.getAdapter( key );

    }

    public IContentOutlinePage getContentOutlinePage() {
        if( contentOutlinePage == null ) {
            contentOutlinePage = new BlackBerryProjectOutlinePage( (BlackBerryProjectFormEditor) this );
        }
        return contentOutlinePage;
    }

    public void updateContentOutlinePageSelection( Control control ) {
        contentOutlinePage.updateTreeSelection( control );
    }

    private class SaveOperation extends WorkspaceOperation {

        @Override
        protected void execute( IProgressMonitor monitor ) throws CoreException {
            if( isDirty() ) {
                commitPages( true );
                preSaveProcess();
                boolean isProjectTypeChanged = isProjectTypeChanged();
                ContextManager.PLUGIN.setBBProperties( getBlackBerryProject().getProject().getName(), _propertiesClone, true );
                // check project dependency after the properties is set
                if( isProjectTypeChanged ) {
                    checkProjectDependency();
                }
                _modificationStamp = getInputHandler().getModificationStamp();
                _isDirty = Boolean.FALSE;
                editorDirtyStateChanged();
            }
        }

        /*
         * @see org.eclipse.ui.texteditor.ISchedulingRuleProvider#getSchedulingRule()
         */
        public ISchedulingRule getSchedulingRule() {
            return ResourcesPlugin.getWorkspace().getRoot();
        }

    }
}
