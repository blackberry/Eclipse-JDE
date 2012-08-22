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
package net.rim.ejde.internal.ui.editors.locale;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.util.ImportUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PackageUtils;
import net.rim.sdk.resourceutil.RIMResourceLocale;
import net.rim.sdk.resourceutil.ResourceCollection;
import net.rim.sdk.resourceutil.ResourceCollectionFactory;
import net.rim.sdk.resourceutil.ResourceCollectionListener;
import net.rim.sdk.resourceutil.ResourceConstants;
import net.rim.sdk.resourceutil.ResourceElement;
import net.rim.sdk.resourceutil.ResourceLocale;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

/**
 * The multipage editor for blackberry resources
 *
 * @author jkeshavarzi
 *
 */
public class ResourceEditor extends MultiPageEditorPart {
    // vector to keep track of the open pages
    private Vector< ResourceEditorPage > _pages;

    // this variable keeps track of the resourceCollection. It is used to get locales of the file.
    private ResourceCollection _resources;

    private boolean _wasDirty;
    private ResourceEditorListener _resourceEditorListener;
    private static final String ROOT = "Root";

    // this variable keeps track if resourceChangeListener has already been added to the workspace. This variable was introduced
    // as a fix to SDR185428 to load pages dynamically in the resource Editor.
    private static boolean resourceChangeListenerAdded = false;

    private static final Logger _logger = Logger.getLogger( ResourceEditor.class );

    // array of references to open editors in this WorkbenchPage
    private static IEditorReference[] _openEditorReferences = null;

    // filename of .rrh/.rrc file that was opened (i.e. double-clicked) by user
    private String _resourceFilename;

    // The resource header file for the resource collection
    private File rrhFile;

    // The package of the resource when it was opened in the resource editor
    private String _originalPackage = null;

    // vector of resource IFiles to be potentially checked out (i.e. opened for edit in Perforce). all IFiles in this vector
    // correspond to all resource files from all resource bundles opened by user
    private static Vector< IFile > _checkoutFiles = new Vector< IFile >( 0 );

    // vector of ResourceEditorPage objects (used by ResourceEditorOptionsDialog class to activate versioning highlighting)
    private static Vector< ResourceEditorPage > _resourceEditorPages = new Vector< ResourceEditorPage >( 0 );

    private boolean _callByResourceListener = false;

    // return EditorReferences from all the pages in eclipse
    public IEditorReference[] getOpenEditorReferences() {
        Vector< IEditorReference > openEditorReferences = new Vector< IEditorReference >( 0 );
        IWorkbenchPage[] workbenchPage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages();
        int workbenchPageCount = workbenchPage.length;

        for( int i = 0; i < workbenchPageCount; i++ ) {
            Collection< IEditorReference > editorReferences = Arrays.asList( workbenchPage[ i ]
                    .getEditorReferences() );
            openEditorReferences.addAll( editorReferences );
        }

        return openEditorReferences.toArray( new IEditorReference[ 1 ] );
    }

    private File getRRCFile( File s ) {
        String[] splitFiles = s.getName().split( "\\." );
        String extension = "";
        if( splitFiles[ 1 ].toLowerCase().equals( "rrh" ) ) {
            extension = ".rrc";
        } else {
            extension = ".rrh";
        }
        return new File( ( s.getPath().substring( 0, s.getPath().lastIndexOf( File.separator ) ) + File.separator
                + splitFiles[ 0 ] + extension ) );
    }

    private class ResourceEditorListener implements IPartListener {
        private boolean _allowOpen = true;
        private boolean _openTextEditor = false;
        private IEditorSite _site;
        private IEditorInput _input;

        public ResourceEditorListener( IEditorSite site, IEditorInput input ) {
            this._site = site;
            this._input = input;
        }

        public void setAllowOpen( boolean allowOpen ) {
            this._allowOpen = allowOpen;
        }

        public void setOpenTextEditor( boolean openTextEditor ) {
            this._openTextEditor = openTextEditor;
        }

        public void partActivated( IWorkbenchPart part ) {
        }

        public void partBroughtToTop( IWorkbenchPart part ) {
        }

        public void partClosed( IWorkbenchPart part ) {
        }

        public void partDeactivated( IWorkbenchPart part ) {
        }

        public void partOpened( IWorkbenchPart part ) {
            if( !( part instanceof ResourceEditor ) ) {
                return;
            }
            if( !_allowOpen ) {
                part.getSite().getWorkbenchWindow().getActivePage().closeEditor( (IEditorPart) part, false );
                if( _openTextEditor ) {
                    try {
                        IDE.openEditor( _site.getWorkbenchWindow().getActivePage(), _input, EditorsUI.DEFAULT_TEXT_EDITOR_ID );
                    } catch( PartInitException e ) {
                        _logger.error( "Error occurred during Opening the Text Editor for" + _input.getName(), e ); //$NON-NLS-1$
                    }
                }
            }
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            window.getPartService().removePartListener( this );
        }
    }

    /**
     * This function adds resourceChangeListener to the workspace to detect the addition of new locales to the exisiting .rrc
     * files and dynamically add them in the .rrc file if the ResourceEditor is already open The addition of resourceEditor was a
     * fix to SDR185428.
     */
    public ResourceEditor() {
        // when ResourceEditor is first initialized _wasdirty is false. ResourceEditor is not dirty. Dirty bit is used in the
        // doSave function.
        _wasDirty = false;

        // if resource change listener is not already added to the workspace add it to the workspace.
        if( !resourceChangeListenerAdded ) {
            // set the resourceChangeListener flag to true. when this flag is set to true the editor is not closed in the
            // createPages() method
            resourceChangeListenerAdded = true;

            // add Resource Change Listener to listen to addition of locales so that they can be added dynamically
            ResourcesPlugin.getWorkspace().addResourceChangeListener( new IResourceChangeListener() {
                // @Override
                public void resourceChanged( IResourceChangeEvent event ) {
                    IResourceDelta delta = event.getDelta();

                    try {
                        delta.accept( new IResourceDeltaVisitor() {
                            // @Override

                            /**
                             * We are interested in checking if the file added was an RRH or a RRC file.
                             */
                            public boolean visit( IResourceDelta resourceDelta ) throws CoreException {
                                IResource resource = resourceDelta.getResource();
                                int type = resource.getType();
                                int kind = resourceDelta.getKind();

                                // check to see if the resource added is a file
                                if( type == IResource.FILE ) {
                                    // check to see if the resource is of the kind added
                                    if( kind == IResourceDelta.ADDED ) {
                                        // check to see if the resource ends with .rrc or .rrh extension (these are resource file
                                        // extensions)
                                        if( resource.toString().endsWith( ResourceConstants.RRH_SUFFIX )
                                                || resource.getName().toString().endsWith( ResourceConstants.RRC_SUFFIX ) ) {
                                            // store the resource as final so that it can be used in the run method run method can
                                            // not refer to external non final variables
                                            final IResource runResource = resource;

                                            // Display thread was introduced as a 'blog' suggestion because changed to
                                            // ResourceEditor could not be done in the non-UI thread.
                                            Display.getDefault().asyncExec( new Runnable() {
                                                public void run() {
                                                    // your stuff here ...
                                                    try {
                                                        // get a reference to open editors.
                                                        // _openEditorReferences =
                                                        // PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
                                                        _openEditorReferences = getOpenEditorReferences();
                                                        // addPage(new EditorPart(), new FileEditorInput((IFile)resource));
                                                        // ResourceEditor.getInstance().createPages();
                                                        // createPages();
                                                        // System.out.println("Hello");

                                                        // check through all the open editors to see if any of the editors that we
                                                        // want to add the locale to is opened.
                                                        // Example : say if a.rrh and a.rrc already exist. Then if we add
                                                        // a_en.rrc ( english locale ) we want to browse through all the
                                                        // _openEditroReferences to see if a.rrh or a.rrc is opened.
                                                        for( int i = 0; i < _openEditorReferences.length; i++ ) {
                                                            // Check to see if the referenced editor is the instance of
                                                            // ResourceEditor
                                                            if( _openEditorReferences[ i ].getEditor( true ) instanceof ResourceEditor ) {
                                                                ResourceEditor editor = (ResourceEditor) _openEditorReferences[ i ]
                                                                        .getEditor( true );
                                                                // get the name of the Input Editor name from the
                                                                // _openEditorReferences. Usually it is a.rrh file as set in the
                                                                // init function. However it can be a.rrc file in cases where the
                                                                // editor has no .rrh file.
                                                                String existingEditorFilename = _openEditorReferences[ i ]
                                                                        .getEditor( true ).getEditorInput().getName();

                                                                // get the name of the .rrh file or .rrc file that we just added
                                                                String thisEditorFilename = runResource.getName();

                                                                // store the trimmed name for existing editor
                                                                String existingEditorFilenameTrim;

                                                                // store the name for the editor of the file that is added
                                                                String thisEditorFilenameTrim;

                                                                // store the path of the parent of the resource
                                                                String resourcePath = runResource.getParent().getLocation()
                                                                        .toOSString();

                                                                // store the path of the parent of the editor
                                                                String editorPath = ( (FileEditorInput) ( editor.getEditorInput() ) )
                                                                        .getFile().getParent().getLocation().toOSString();

                                                                // strip off both the existingEditor file name and thisEditor
                                                                // filename of its .rrc and .rrh extension and the locale name.
                                                                if( existingEditorFilename
                                                                        .endsWith( ResourceConstants.RRH_SUFFIX )
                                                                        || existingEditorFilename
                                                                                .endsWith( ResourceConstants.RRC_SUFFIX ) ) {
                                                                    if( existingEditorFilename.contains( "_" ) ) { //$NON-NLS-1$
                                                                        existingEditorFilenameTrim = existingEditorFilename
                                                                                .substring( 0,
                                                                                        existingEditorFilename.indexOf( "_" ) ); //$NON-NLS-1$
                                                                    } else {
                                                                        existingEditorFilenameTrim = existingEditorFilename
                                                                                .substring( 0,
                                                                                        existingEditorFilename.indexOf( "." ) ); //$NON-NLS-1$
                                                                    }

                                                                    if( thisEditorFilename.contains( "_" ) ) { //$NON-NLS-1$
                                                                        thisEditorFilenameTrim = thisEditorFilename.substring( 0,
                                                                                thisEditorFilename.indexOf( "_" ) ); //$NON-NLS-1$
                                                                    } else {
                                                                        thisEditorFilenameTrim = thisEditorFilename.substring( 0,
                                                                                thisEditorFilename.indexOf( "." ) ); //$NON-NLS-1$
                                                                    }

                                                                    // if the thisEditorFileNameTrim is equal to the
                                                                    // existingEditorFilenameTrim and the project of this editor
                                                                    // is equal to the project of the resource then only add the
                                                                    // dynamically added locale
                                                                    if( thisEditorFilenameTrim
                                                                            .equals( existingEditorFilenameTrim )
                                                                            && resourcePath.equals( editorPath ) ) {
                                                                        // existing editor on WorkbenchPage and this editor belong
                                                                        // to the same ResourceCollection

                                                                        // save previous changes in the editor
                                                                        editor.doSave( new NullProgressMonitor() );

                                                                        String locale = thisEditorFilename.substring(
                                                                                thisEditorFilename.indexOf( "_" ) + 1, //$NON-NLS-1$
                                                                                thisEditorFilename.indexOf( "." ) ); //$NON-NLS-1$
                                                                        // editor . _wasDirty = false;

                                                                        // if the locale is already a part of the editor do not
                                                                        // add
                                                                        if( editor._resources.getLocale( locale ) != null ) {
                                                                            break;
                                                                        }

                                                                        // set callByResourceListener to true so that in
                                                                        // createPages function the editor. Close is not called
                                                                        editor._callByResourceListener = true;

                                                                        // dispose the existing pages of the editor
                                                                        editor.dispose();

                                                                        // reinitialize the editor again
                                                                        editor.init( _openEditorReferences[ i ].getEditor( true )
                                                                                .getEditorSite(), new FileEditorInput(
                                                                                (IFile) runResource ) );

                                                                        // add Pages to editor
                                                                        editor.createPages();

                                                                        // set the callByResourceListener to false
                                                                        editor._callByResourceListener = false;
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } catch( Exception e ) {
                                                    }
                                                }
                                            } ); // new Runnable object ends here
                                        } // end of if
                                    }// end of if
                                }// end of if

                                // return true to continue visiting the Delta tree.
                                return true;
                            }// visit ends here
                        } );// new ResourceDeltaVisitor ends here
                    } catch( CoreException e ) {
                        // log.error(e.getMessage(), e);
                        e.printStackTrace();
                    }
                }
            } ); // new IResourceChangeListener ends here
        }
    }

    public void doSave( IProgressMonitor monitor ) {
        try {
            _resources.save();

            // Check if package was changed.
            String rrhPackage = PackageUtils.getRRHPackageID( rrhFile );
            if( !_originalPackage.equals( rrhPackage ) ) {
                // Attempt to find a non linked rrhIFile
                IFile files[] = ResourcesPlugin.getWorkspace().getRoot()
                        .findFilesForLocation( new Path( rrhFile.getAbsolutePath() ) );
                IFile rrhIFile = null;
                for( IFile file : files ) {
                    if( !file.isLinked() ) {
                        rrhIFile = file;
                        break;
                    }
                }
                if( files.length != 0 && rrhIFile == null ) {
                    rrhIFile = files[ 0 ];
                }

                IJavaElement packageFolder = JavaCore.create( rrhIFile.getParent() );
                IPackageFragmentRoot sourceFolder = (IPackageFragmentRoot) packageFolder.getParent();
                try {
                    moveResources( rrhIFile, sourceFolder.createPackageFragment( rrhPackage, true, new NullProgressMonitor() ) );
                } catch( Exception e ) {
                    _logger.error( "doSave: error moving resources", e ); //$NON-NLS-1$
                }
            }

            if( _wasDirty ) {
                _wasDirty = false;
                firePropertyChange( IEditorPart.PROP_DIRTY );
            }

            // get eclipse workspace
            final IWorkspace workspace = ResourcesPlugin.getWorkspace();

            // get eclipse workspace description
            final IWorkspaceDescription workspaceDescription = workspace.getDescription();

            // get autoBuildFlag
            final boolean autoBuild = workspaceDescription.isAutoBuilding();
            IProject project = ( (FileEditorInput) ( this.getEditorInput() ) ).getFile().getProject();

            // if autobuild is checked build eclipse project
            if( autoBuild ) {
                try {
                    project.build( IncrementalProjectBuilder.INCREMENTAL_BUILD, null );
                } catch( CoreException e ) {
                    e.printStackTrace();
                }
            }

            // We must refresh the project because we are using a JAR that uses File objects to save, instead of eclipse IFiles.
            project.refreshLocal( IProject.DEPTH_INFINITE, monitor );

        } catch( IOException e ) {
            TableViewer viewer = getActiveResourceEditorPage().getTableViewer();
            Status status = new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, IStatus.OK, "Unable to open file for writing.",
                    e );
            ErrorDialog.openError( viewer.getControl().getShell(), "Error - Resource Editor", "Error saving file.", status );
            monitor.setCanceled( true );
        } catch( CoreException e ) {
            _logger.error( "doSave: error getting package", e ); //$NON-NLS-1$
        }
    }

    public void doSaveAs() {
        // "Save As" not allowed
    }

    public void init( IEditorSite site, IEditorInput input ) throws PartInitException {
        if( !( input instanceof IFileEditorInput ) ) {
            throw new PartInitException( input.getName() + " is not part of the current project." );
        }

        String messagePrompt = ""; //$NON-NLS-1$
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        _resourceEditorListener = new ResourceEditorListener( site, input );
        window.getPartService().addPartListener( _resourceEditorListener );

        FileEditorInput fileEditorInput = (FileEditorInput) input;
        // Check out file corresponding to the locale that is open in this ResourceEditor (Opened for Edit in Perforce)
        IFile[] checkOutFiles = new IFile[ 1 ];
        checkOutFiles[ 0 ] = ( (IFileEditorInput) input ).getFile();
        ResourcesPlugin.getWorkspace().validateEdit( checkOutFiles, null );

        _openEditorReferences = null;
        try {
            // _openEditorReferences = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
            _openEditorReferences = getOpenEditorReferences();
            // array of references to open editors in this WorkbenchPage this will be used to prevent multiple ResourceEditor
            // instances from being open for the same resource collection
        } catch( Exception e ) {
            _logger.error( "init:", e ); //$NON-NLS-1$
        }

        // keeps track of the recently added _resourceFilename for this ResourceEditor.
        _resourceFilename = fileEditorInput.getName();

        // Fix for SDR169264 (eJDE: Resource Editor can show incorrect tab when switching between locales via double-click in
        // managing views) inputFileAbsolutePath = inputFileAbsolutePath.substring(0,
        // inputFileAbsolutePath.lastIndexOf(File.separator)); if selected file is .rrc file, we try to get corresponding .rrh
        // file
        String tempFilename = _resourceFilename;
        if( tempFilename.contains( "_" ) ) { //$NON-NLS-1$
            tempFilename = tempFilename.substring( 0, tempFilename.indexOf( "_" ) ) + ResourceConstants.RRH_SUFFIX; //$NON-NLS-1$
        } else {
            tempFilename = tempFilename.replace( ResourceConstants.RRC_SUFFIX, ResourceConstants.RRH_SUFFIX );
        }

        rrhFile = new File( ( (Path) fileEditorInput.getPath() ).toFile().getParentFile(), tempFilename );
        try {
            _originalPackage = PackageUtils.getRRHPackageID( rrhFile );
        } catch( Exception e ) {
            _logger.error( "init: error getting original package", e ); //$NON-NLS-1$
        }

        // Check#1: Checks to see if the resource header file exists in the same location as the rrc file. This is a mandatory
        // check needed to create the resourceCollection in RIA.
        if( rrhFile.exists() ) {
            try {
                _resources = ResourceCollectionFactory.newResourceCollection( rrhFile.getAbsolutePath() );
                _resources.addListener( createCollectionListener() );
            } catch( Exception e ) {
                throw new PartInitException( e.getMessage() );
            }

            // Check#2: Checks to see if the corresponding rrh file exists in the workspace. If createResourceFileEditorInput does
            // not return null, then the corresponding resource was found. We can then call super and return.
            FileEditorInput rrhFileEditorInput;
            if( ( rrhFileEditorInput = createResourceFileEditorInput( rrhFile ) ) != null ) {
                // Check#3: Checks to ensure that the resource collection has locale files. If no locale files exist, RE cannot be
                // opened.
                ResourceLocale[] resourceLocales = _resources.getLocales();
                IFile rrhIFile = ImportUtils.getProjectBasedFileFromOSBasedFile( rrhFile.getAbsolutePath() );
                File rrcFile = getRRCFile( rrhFile );
                IFile rrcIFile = ImportUtils.getProjectBasedFileFromOSBasedFile( rrcFile.getAbsolutePath() );

                if( resourceLocales.length != 0 ) {
                    // Check#4 Check to make sure at least 1 locale exists in workspace. This is required to create RE pages.
                    for( int i = 0; i < resourceLocales.length; i++ ) {
                        if( StringUtils.isEmpty( resourceLocales[ i ].getLocaleName() ) ) {
                            if( rrcIFile != null && rrcIFile.exists() ) {
                                super.init( site, rrhFileEditorInput );
                                return;
                            }
                        } else {
                            IFile resourceFile = ImportUtils
                                    .getProjectBasedFileFromOSBasedFile( ( (RIMResourceLocale) resourceLocales[ i ] )
                                            .getRrcFileAbsolutePath() );
                            if( resourceFile != null
                                    && resourceFile.getName().equals(
                                            ( ( (RIMResourceLocale) resourceLocales[ i ] ).getRrcFilename() ) ) ) {
                                super.init( site, rrhFileEditorInput );
                                return;
                            }
                        }
                    }
                    MessageDialog.openError( site.getShell(), Messages.NO_LOCALE_ERROR_TITLE, Messages.NO_LOCALE_ERROR_TEXT );
                } else {
                    // Prompt user to create root locale.
                    if( MessageDialog.openQuestion( site.getShell(), Messages.MISSING_LOCALE_ERROR_TITLE,
                            Messages.MISSING_LOCALE_ERROR_TEXT ) ) {
                        File rootRrcFile = getRRCFile( rrhFile );
                        try {
                            if( rootRrcFile.createNewFile() ) {
                                if( rrhIFile.isLinked() ) {
                                    IFile newRrcIFile = rrhIFile.getParent().getFile( new Path( rootRrcFile.getName() ) );
                                    newRrcIFile.createLink( new Path( rootRrcFile.getAbsolutePath() ), IResource.NONE,
                                            new NullProgressMonitor() );
                                } else {
                                    rrhIFile.getProject().refreshLocal( IProject.DEPTH_INFINITE, new NullProgressMonitor() );
                                }

                                // recreate the resource collection
                                _resources = ResourceCollectionFactory.newResourceCollection( rrhFile.getAbsolutePath() );
                                super.init( site, rrhFileEditorInput );
                                return;
                            }
                        } catch( Exception e ) {
                            _logger.error( "init: error creating new root rrc file", e ); //$NON-NLS-1$
                        }
                    }
                }
            } else {
                // The .rrh file does not exist in the workspace, prompt to load .rrh file.
                messagePrompt = NLS.bind( Messages.MESSAGE_BOX_RESOURCE_HEADER_DOES_NOT_EXIST_IN_WORKSPACE, tempFilename,
                        rrhFile.getAbsolutePath() );

                if( MessageDialog.openQuestion( site.getShell(),
                        Messages.MESSAGE_BOX_RESOURCE_HEADER_DOES_NOT_EXIST_IN_WORKSPACE_TITLE, messagePrompt ) ) {
                    IFile fileOpened = fileEditorInput.getFile();
                    IFile newRrhIFile = fileOpened.getParent().getFile( new Path( rrhFile.getName() ) );
                    try {
                        if( !newRrhIFile.exists() ) {
                            newRrhIFile.createLink( new Path( rrhFile.getAbsolutePath() ), IResource.NONE,
                                    new NullProgressMonitor() );
                        }
                    } catch( CoreException e ) {
                        _logger.error( "init: error creating linking rrh file", e ); //$NON-NLS-1$
                    }

                    // Check#2: Checks to see if the corresponding rrh file exists in the workspace. If
                    // createResourceFileEditorInput does not return null, then the corresponding resource was found. We can
                    // then call super and return.
                    if( ( rrhFileEditorInput = createResourceFileEditorInput( rrhFile ) ) != null ) {
                        super.init( site, rrhFileEditorInput );
                        return;
                    }
                } else {
                    // User selected to not load the .rrh file. Prompt to load .rrc file in text editor.
                    messagePrompt = NLS.bind( Messages.MESSAGE_BOX_OPEN_TEXT_EDITOR_PROMPT, _resourceFilename );

                    if( MessageDialog.openQuestion( site.getShell(), Messages.MESSAGE_BOX_OPEN_TEXT_EDITOR_PROMPT_TITLE,
                            messagePrompt ) ) {
                        _resourceEditorListener.setOpenTextEditor( true );
                    }
                }
            }
        } else {
            // The corresponding .rrh file does not exist in the rrc directory. We cannot create a resourceCollection unless we
            // enforce this limitation.
            messagePrompt = NLS.bind( Messages.MESSAGE_BOX_RESOURCE_HEADER_DOES_NOT_EXIST_IN_DIRECTORY, new String[] {
                    tempFilename, rrhFile.getParent(), _resourceFilename } );

            MessageDialog.openError( site.getShell(), Messages.MESSAGE_BOX_RESOURCE_HEADER_DOES_NOT_EXIST_IN_DIRECTORY_TITLE,
                    messagePrompt );
        }
        // Dummy call to avoid Part Init exception caused by the site not getting set properly.
        super.init( site, input );

        // Set flag to close editor when its opened. Eclipse requires the editor to open.
        _resourceEditorListener.setAllowOpen( false );
    }

    public FileEditorInput createResourceFileEditorInput( File rrhFile ) throws PartInitException {
        // For SDR180459: We should get project based file for the local .rrh file that is represented by underlying OS file
        // system.
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        IFile rrhIFile = null;
        for( IProject project : projects ) {
            if( rrhIFile != null ) {
                break;
            }
            rrhIFile = ImportUtils.getProjectBasedFileFromOSBasedFile( project.getName(), rrhFile.getAbsolutePath() );
        }

        if( rrhIFile != null ) {
            return new FileEditorInput( rrhIFile );
        } else {
            return null;
        }
    }

    /**
     * This method will move the given rrhIFile and any of its corresponding locales (if they exist in the workspace) to the
     * passed in package.
     *
     * @param rrhIFile
     *            - The resource header file being moved.
     * @param newPackage
     *            - The package to move the resources to.
     */
    private void moveResources( IFile rrhIFile, IPackageFragment newPackage ) {
        IPath oldRrhLocation = rrhIFile.getLocation();
        RIMResourceLocale locales[] = (RIMResourceLocale[]) _resources.getLocales();

        if( !rrhIFile.isLinked() ) {
            IPath newDestination = newPackage.getPath();
            IContainer sourceFolder = rrhIFile.getParent();

            // First we move the rrh file. Then we Check for links to the rrh file. If they exist, they must be recreated in the
            // proper package and pointing to the new location.
            IFile resourceFile = rrhIFile;
            IFile newIFile = moveCopiedFile( resourceFile, newDestination.append( resourceFile.getName() ) );
            updateLinksLocationAndPackage( oldRrhLocation, newIFile.getLocation(), newPackage.getElementName() );

            // Second we move all the locales (if they exist in the workspace). Then we check for any/all links to the locale
            // files. If they exist, they must be recreated in the proper package and pointing to the new location.
            for( RIMResourceLocale locale : locales ) {
                resourceFile = sourceFolder.getFile( new Path( locale.getRrcFilename() ) );
                newIFile = moveCopiedFile( resourceFile, newDestination.append( resourceFile.getName() ) );
                updateLinksLocationAndPackage( resourceFile.getLocation(), newIFile.getLocation(), newPackage.getElementName() );
            }
        } else {
            // First update all links pointing to rrh file.
            updateLinksLocationAndPackage( oldRrhLocation, null, newPackage.getElementName() );

            // Second update all links pointing to any locales from this resource collection that exist in the workspace.
            IPath resourceLocation = null;
            for( RIMResourceLocale locale : locales ) {
                resourceLocation = new Path( locale.getRrcFileAbsolutePath() );
                updateLinksLocationAndPackage( resourceLocation, null, newPackage.getElementName() );
            }
        }
    }

    private IFile moveCopiedFile( IFile resourceFile, IPath newDestination ) {
        if( resourceFile == null || !resourceFile.exists() || resourceFile.isLinked() ) {
            throw new IllegalArgumentException( "Error: resourceFile must exist and cannot be null or linked." ); //$NON-NLS-1$
        }
        if( newDestination == null ) {
            throw new IllegalArgumentException( "Error: newDestination cannot be null." ); //$NON-NLS-1$
        }

        try {
            resourceFile.move( newDestination, true, new NullProgressMonitor() );
        } catch( CoreException e ) {
            _logger.error( "Error moving resource", e ); //$NON-NLS-1$
        }

        IProject project = resourceFile.getProject();
        IPath projectLocation = newDestination.removeFirstSegments( 1 );
        IFile newIFile = project.getFile( projectLocation );
        return newIFile;
    }

    /**
     * This method will find all links pointing to oldLinkLocation and will point them to the newLinkLocation. The method also
     * moves the links to a package specified by the passed in packageID.
     *
     * @param oldLinkLocation
     *            - The location existing links will be pointing to.
     * @param newLinkLocation
     *            - The location the new links will be pointing to, null if location doesn't change.
     * @param packageID
     *            - The package to place the new links.
     */
    private void updateLinksLocationAndPackage( IPath oldLinkLocation, IPath newLinkLocation, String packageID ) {
        if( newLinkLocation == null ) {
            // Link location will not change
            newLinkLocation = oldLinkLocation;
        }
        IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation( oldLinkLocation );
        for( IFile file : files ) {
            if( file.isLinked() ) {
                IJavaElement packageFolder = JavaCore.create( file.getParent() );
                IPackageFragmentRoot sourceFolder = (IPackageFragmentRoot) packageFolder.getParent();
                try {
                    IPackageFragment newPackage = sourceFolder.createPackageFragment( packageID, true, new NullProgressMonitor() );
                    if( newPackage.exists() ) {
                        IContainer parentFolder = ( (IFolder) newPackage.getResource() );
                        IFile newLocation = parentFolder.getFile( new Path( file.getName() ) );
                        newLocation.createLink( newLinkLocation, IResource.NONE, new NullProgressMonitor() );
                        file.delete( true, new NullProgressMonitor() );
                    }
                } catch( Exception e ) {
                    _logger.error( "Error updating links", e ); //$NON-NLS-1$
                }
            }
        }
    }

    public boolean isDirty() {
        if( _resources != null ) {
            return _resources.isDirty();
        }
        return false;
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    public void setFocus() {
        getActiveResourceEditorPage().setFocus();
    }

    /**
     * This function was added as a fix to SDR185428.
     */
    public void dispose() {
        for( int i = this.getPageCount() - 1; i >= 0; i-- ) {

            removePage( i );
        }
    }

    protected void createPages() {
        // If _resource is null then the .rrh file was not found or if the _allowOpen flag is set to false we must not allow this
        // editor to open. In these cases we create a dummy page with no content in order to satisfy the page requirements. We
        // then simply return.
        if( _resources == null || !_resourceEditorListener._allowOpen ) {
            addPage( new Composite( this.getContainer(), SWT.BORDER_SOLID ) );
            setActivePage( 0 );
            return;
        }

        // Fix for DPI221808. Only add pages for locales that exist in project
        ArrayList< ResourceLocale > localeList = new ArrayList< ResourceLocale >();
        ResourceLocale[] resourceLocales = _resources.getLocales();
        for( int i = 0; i < resourceLocales.length; i++ ) {
            // Special case for root locale file. The getProjectBasedFileFromOSBasedFile method can't find linked root locales.
            if( StringUtils.isEmpty( resourceLocales[ i ].getLocaleName() ) ) {
                File rrcFile = getRRCFile( rrhFile );
                IFile rrcIFile = ImportUtils.getProjectBasedFileFromOSBasedFile( rrcFile.getAbsolutePath() );

                if( rrcIFile != null && rrcIFile.exists() ) {
                    localeList.add( resourceLocales[ i ] );
                }
            } else {
                IFile resourceFile = ImportUtils.getProjectBasedFileFromOSBasedFile( ( (RIMResourceLocale) resourceLocales[ i ] )
                        .getRrcFileAbsolutePath() );
                if( resourceFile != null
                        && resourceFile.getName().equals( ( ( (RIMResourceLocale) resourceLocales[ i ] ).getRrcFilename() ) ) ) {
                    localeList.add( resourceLocales[ i ] );
                }
            }
        }
        ResourceLocale[] locales = localeList.toArray( new ResourceLocale[ localeList.size() ] );
        _pages = new Vector< ResourceEditorPage >( locales.length );
        // locale corresponding to .rrh/.rrc file that was opened from package explorer (i.e. "en", "fr")
        String currentLocaleName = ""; //$NON-NLS-1$

        // open first page (i.e. tab for root locale) of ResourceEditor by default
        int setActivePageIndex = 0;

        for( int i = 0; i < locales.length; ++i ) {
            ResourceEditorPage page = new ResourceEditorPage( getContainer(), locales[ i ] );
            int index = addPage( page.getControl() );
            String localeName = locales[ i ].getLocaleName();
            setPageText( index, localeName == ResourceConstants.ROOT_LOCALE ? ROOT : localeName );
            page.createContextMenu( getSite() );
            _pages.add( i, page );

            if( _resourceFilename.endsWith( ResourceConstants.RRC_SUFFIX ) ) {
                currentLocaleName = _resourceFilename.substring( _resourceFilename.indexOf( "_" ) + 1, _resourceFilename //$NON-NLS-1$
                        .lastIndexOf( ResourceConstants.RRC_SUFFIX ) );
            } else {
                currentLocaleName = _resourceFilename.substring( _resourceFilename.indexOf( "_" ) + 1, _resourceFilename //$NON-NLS-1$
                        .lastIndexOf( ResourceConstants.RRH_SUFFIX ) );
            }

            if( currentLocaleName.equals( localeName ) ) {
                setActivePage( i );
                // keep track of which locale tab to open in ResourceEditor
                setActivePageIndex = i;
            } else {
                setActivePage( 0 );
            }
        }

        // flag used to determine if the resource collection is already open in an existing Resource Editor instance
        boolean resourceCollectionAlreadyOpen = false;
        try {
            if( _openEditorReferences.length > 1 ) {
                for( int i = 0; i < _openEditorReferences.length; i++ ) {

                    if( _openEditorReferences[ i ].getEditor( false ) instanceof ResourceEditor ) {

                        String existingEditorFilename = _openEditorReferences[ i ].getEditor( false ).getEditorInput().getName();
                        String thisEditorFilename = this.getEditorInput().getName();

                        String existingEditorPath = ( (FileEditorInput) ( _openEditorReferences[ i ].getEditor( true )
                                .getEditorInput() ) ).getFile().getParent().getLocation().toOSString();
                        String thisEditorPath = ( (FileEditorInput) ( this.getEditorInput() ) ).getFile().getParent()
                                .getLocation().toOSString();

                        if( existingEditorFilename.endsWith( ResourceConstants.RRH_SUFFIX )
                                || existingEditorFilename.endsWith( ResourceConstants.RRC_SUFFIX ) ) {
                            if( existingEditorFilename.contains( "_" ) ) { //$NON-NLS-1$
                                existingEditorFilename = existingEditorFilename.substring( 0,
                                        existingEditorFilename.indexOf( "_" ) ); //$NON-NLS-1$
                            } else {
                                existingEditorFilename = existingEditorFilename.substring( 0,
                                        existingEditorFilename.indexOf( "." ) ); //$NON-NLS-1$
                            }

                            if( thisEditorFilename.contains( "_" ) ) { //$NON-NLS-1$
                                thisEditorFilename = thisEditorFilename.substring( 0, thisEditorFilename.indexOf( "_" ) ); //$NON-NLS-1$
                            } else {
                                thisEditorFilename = thisEditorFilename.substring( 0, thisEditorFilename.indexOf( "." ) ); //$NON-NLS-1$
                            }

                            // existing editor on WorkbenchPage and this editor belong to the same ResourceCollection
                            if( thisEditorFilename.equals( existingEditorFilename ) && existingEditorPath.equals( thisEditorPath ) ) {
                                if( _callByResourceListener == false ) {
                                    // do not open duplicate editor for resources under the same ResourceCollection
                                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                                            .closeEditor( this, false );
                                }

                                ( (ResourceEditor) _openEditorReferences[ i ].getEditor( true ) )
                                        .setActivePage( setActivePageIndex );
                                // set focus to the editor containing resources for this
                                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
                                        .activate( _openEditorReferences[ i ].getEditor( true ) );
                                resourceCollectionAlreadyOpen = true;
                            }
                        }
                    }
                }
            }
        } catch( Exception e ) {
        }

        if( !resourceCollectionAlreadyOpen ) {
            // Update _resourceEditorPages<ResourceEditorPage> vector with newly opened resource files (used by
            // ResourceEditorOptionsDialog class for versioning highlighting)
            for( int i = 0; i < _pages.size(); i++ ) {
                for( int j = 0; j < _resourceEditorPages.size(); j++ ) {
                    // remove old existing ResourceEditorPage objects
                    if( _resourceEditorPages.get( j ).getRrcFile().getAbsolutePath()
                            .equals( _pages.get( i ).getRrcFile().getAbsolutePath() ) ) {
                        _resourceEditorPages.remove( j );
                        // System.out.println("removed: " + _pages.get(i).getRrcFile().getAbsolutePath());
                    }
                }
                _resourceEditorPages.add( _pages.get( i ) );
                super.setActivePage( setActivePageIndex );
            }

            // Activate Versioning Highlighting when resource file is opened in Resource Editor (if turned on)
            if( _pages.size() > 0 ) {
                String rrhPath = getRRCFile( _pages.get( 0 ).getRrcFile() ).getAbsolutePath();
                // first page (index 0) is always root locale
                File rrhFile = new File( rrhPath );
                String originalLocale = ResourceEditorOptionsDialog.getOriginalLocaleString( rrhFile );
                if( originalLocale != null ) {
                    ResourceEditorOptionsDialog.updateVersioningForResourceEditor( true, _resources.getLocale( "" ) ); //$NON-NLS-1$
                }
            }
            _pages.get( setActivePageIndex ).setFocus();
        }
    }

    protected void pageChange( int newPageIndex ) {
        updateTitle( newPageIndex );
        super.pageChange( newPageIndex );
    }

    private ResourceCollectionListener createCollectionListener() {
        return new ResourceCollectionListener() {
            public void commentChanged( String key ) {
                markDirty();
            }

            public void keyAdded( ResourceElement element ) {
                if( localeExists( element.getLocale().getLocaleName() ) ) {
                    getResourceEditorPage( element ).refresh();
                }
                markDirty();
            }

            public void keyChanged( ResourceElement element ) {
                if( localeExists( element.getLocale().getLocaleName() ) ) {
                    getResourceEditorPage( element ).update( element, new String[] { ResourceEditorPage.KEY_COLUMN_ID } );
                }
                markDirty();
            }

            public void keyDeleted( ResourceElement element ) {
                if( localeExists( element.getLocale().getLocaleName() ) ) {
                    getResourceEditorPage( element ).refresh();
                }
                markDirty();
            }

            public void valueChanged( ResourceElement element ) {
                getResourceEditorPage( element ).update( element, new String[] { ResourceEditorPage.VALUE_COLUMN_ID } );
                markDirty();
            }
        };
    }

    private ResourceEditorPage getActiveResourceEditorPage() {
        return _pages.elementAt( getActivePage() );
    }

    private boolean localeExists( String localeName ) {
        for( int i = 0; i < _pages.size(); ++i ) {
            String currentPageLocale = _pages.elementAt( i ).getLocale().getLocaleName();
            if( currentPageLocale.equals( localeName ) ) {
                return true;
            }
        }
        return false;
    }

    private ResourceEditorPage getResourceEditorPage( ResourceElement element ) {
        String localeName = element.getLocale().getLocaleName();

        if( localeName == ResourceConstants.ROOT_LOCALE ) {
            localeName = ROOT;
        }

        // TODO: make this a more efficient search
        for( int i = 0; i < _pages.size(); ++i ) {
            String pageName = getPageText( i );
            if( pageName.equals( localeName ) ) {
                return _pages.elementAt( i );
            }
        }
        throw new IllegalArgumentException();
    }

    private void markDirty() {
        if( !_wasDirty && _resources.isDirty() ) {
            _wasDirty = true;
            firePropertyChange( IEditorPart.PROP_DIRTY );
        }
    }

    private void updateTitle( int index ) {
        String editorFileName = _resourceFilename;

        if( editorFileName.contains( "_" ) ) { //$NON-NLS-1$
            editorFileName = editorFileName.substring( 0, editorFileName.indexOf( "_" ) ); //$NON-NLS-1$
        } else {
            editorFileName = editorFileName.substring( 0, editorFileName.indexOf( "." ) ); //$NON-NLS-1$
        }
        setPartName( "Resource Editor - " + editorFileName + "-" + getPageText( index ) + " locale" );
    }

    /**
     * Helper method returns vector of IFile objects to be checked out
     *
     * @return
     */
    protected static Vector< IFile > getCheckoutFiles() {
        return _checkoutFiles;
    }

    /**
     * Helper method returns vector of all ResourceEditorPage objects belonging to all resource collections that were ever open in
     * the Resource Editor. (used for versioning highlighting)
     *
     * @return
     */
    protected static Vector< ResourceEditorPage > getResourceEditorPages() {
        return _resourceEditorPages;
    }
}
