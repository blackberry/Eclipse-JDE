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
package net.rim.ejde.internal.ui.wizards;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.rim.ejde.internal.builders.PreprocessingBuilder;
import net.rim.ejde.internal.builders.ResourceBuilder;
import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProjectCoreNature;
import net.rim.ejde.internal.model.BlackBerryProjectPreprocessingNature;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.model.IBlackBerryProject;
import net.rim.ejde.internal.model.preferences.RootPreferences;
import net.rim.ejde.internal.ui.editors.model.BlackBerryProjectFormEditor;
import net.rim.ejde.internal.ui.wizards.templates.BBFieldData;
import net.rim.ejde.internal.util.InternalImportUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PackagingUtils;
import net.rim.ejde.internal.util.ProjectUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.util.CoreUtility;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.wizards.ClassPathDetector;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.ui.IFieldData;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyDelegatingOperation;
import org.eclipse.ui.part.FileEditorInput;

/**
 * The second page of the New Java project wizard. It allows to configure the build path and output location. As addition to the
 * {@link JavaCapabilityConfigurationPage}, the wizard page does an early project creation (so that linked folders can be defined)
 * and, if an existing external location was specified, detects the class path.
 *
 * <p>
 * Clients may instantiate or subclass.
 * </p>
 *
 * @since 3.4
 */
public class BasicBlackBerryProjectWizardPageTwo extends JavaCapabilityConfigurationPage {
    static private final Logger log = Logger.getLogger( BlackBerryProjectWizardPageTwo.class );

    private static final String FILENAME_PROJECT = ".project"; //$NON-NLS-1$
    private static final String FILENAME_CLASSPATH = ".classpath"; //$NON-NLS-1$

    protected final BlackBerryProjectWizardPageOne fFirstPage;

    protected URI fCurrProjectLocation; // null if location is platform location
    protected IProject fCurrProject;

    protected boolean fKeepContent;

    private File fDotProjectBackup;
    private File fDotClasspathBackup;
    private Boolean fIsAutobuild;
    private HashSet fOrginalFolders;

    /**
     * Constructor for the {@link NewBlackBerryProjectWizardPageTwo}.
     *
     * @param mainPage
     *            the first page of the wizard
     */
    public BasicBlackBerryProjectWizardPageTwo( BlackBerryProjectWizardPageOne mainPage ) {
        fFirstPage = mainPage;
        fCurrProjectLocation = null;
        fCurrProject = null;
        fKeepContent = false;

        fDotProjectBackup = null;
        fDotClasspathBackup = null;
        fIsAutobuild = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage#useNewSourcePage ()
     */
    protected final boolean useNewSourcePage() {
        return true;
    }

    /**
     * Set the visibility of the page.
     *
     * @param visible
     *            The visibility
     * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
     */
    public void setVisible( boolean visible ) {
        boolean isShownFirstTime = visible && fCurrProject == null;
        if( visible ) {
            if( isShownFirstTime ) { // entering from the first page
                createProvisonalProject();
            }
        } else {
            if( getContainer().getCurrentPage() == fFirstPage ) { // leaving
                // back to the
                // first page
                removeProvisonalProject();
            }
        }
        super.setVisible( visible );
        if( isShownFirstTime ) {
            setFocus();
        }
    }

    protected boolean hasExistingContent( URI realLocation ) throws CoreException {
        IFileStore file = EFS.getStore( realLocation );
        return file.fetchInfo().exists();
    }

    private IStatus changeToNewProject() {
        class UpdateRunnable implements IRunnableWithProgress {
            public IStatus infoStatus = Status.OK_STATUS;

            public void run( IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException {
                try {
                    if( fIsAutobuild == null ) {
                        fIsAutobuild = Boolean.valueOf( CoreUtility.setAutoBuilding( false ) );
                    }
                    infoStatus = updateProject( monitor );
                } catch( CoreException e ) {
                    throw new InvocationTargetException( e );
                } catch( OperationCanceledException e ) {
                    throw new InterruptedException();
                } finally {
                    monitor.done();
                }
            }
        }
        UpdateRunnable op = new UpdateRunnable();
        try {
            getContainer().run( true, false, new WorkspaceModifyDelegatingOperation( op ) );
            return op.infoStatus;
        } catch( InvocationTargetException e ) {
            final String title = Messages.NewBlackBerryProjectWizardPageTwo_error_title;
            final String message = Messages.NewBlackBerryProjectWizardPageTwo_error_message;
            ExceptionHandler.handle( e, getShell(), title, message );
        } catch( InterruptedException e ) {
            // cancel pressed
        }
        return null;
    }

    protected static URI getRealLocation( String projectName, URI location ) {
        if( location == null ) { // inside workspace
            try {
                URI rootLocation = ResourcesPlugin.getWorkspace().getRoot().getLocationURI();

                location = new URI( rootLocation.getScheme(), null, Path.fromPortableString( rootLocation.getPath() )
                        .append( projectName ).toString(), null );
            } catch( URISyntaxException e ) {
                Assert.isTrue( false, "Can't happen" ); //$NON-NLS-1$
            }
        }
        return location;
    }

    protected IStatus updateProject( IProgressMonitor monitor ) throws CoreException, InterruptedException {
        IStatus result = StatusInfo.OK_STATUS;
        if( monitor == null ) {
            monitor = new NullProgressMonitor();
        }
        try {
            monitor.beginTask( Messages.NewBlackBerryProjectWizardPageTwo_operation_initialize, 7 );
            if( monitor.isCanceled() ) {
                throw new OperationCanceledException();
            }

            String projectName = fFirstPage.getProjectName();

            fCurrProject = ResourcesPlugin.getWorkspace().getRoot().getProject( projectName );
            fCurrProjectLocation = fFirstPage.getProjectLocationURI();

            URI realLocation = getRealLocation( projectName, fCurrProjectLocation );
            fKeepContent = hasExistingContent( realLocation );

            if( monitor.isCanceled() ) {
                throw new OperationCanceledException();
            }

            if( fKeepContent ) {
                rememberExistingFiles( realLocation );
                rememberExisitingFolders( realLocation );
            }

            if( monitor.isCanceled() ) {
                throw new OperationCanceledException();
            }

            try {
                createProject( fCurrProject, fCurrProjectLocation, new SubProgressMonitor( monitor, 2 ) );
            } catch( CoreException e ) {
                if( e.getStatus().getCode() == IResourceStatus.FAILED_READ_METADATA ) {
                    result = new Status( IStatus.INFO, ContextManager.PLUGIN_ID, NLS.bind(
                            Messages.NewBlackBerryProjectWizardPageTwo_DeleteCorruptProjectFile_message, e.getLocalizedMessage() ) );
                    deleteProjectFile( realLocation );
                    if( fCurrProject.exists() )
                        fCurrProject.delete( true, null );

                    createProject( fCurrProject, fCurrProjectLocation, null );
                } else {
                    throw e;
                }
            }

            if( monitor.isCanceled() ) {
                throw new OperationCanceledException();
            }

            IJavaProject eclipseJavaProject = JavaCore.create( fCurrProject );

            initializeBuildPath( eclipseJavaProject, new SubProgressMonitor( monitor, 2 ) );

            configureJavaProject( new SubProgressMonitor( monitor, 3 ) ); // create
            // the
            // Java
            // project
            // to
            // allow
            // the
            // use
            // of
            // the
            // new
            // source
            // folder
            // page
        } finally {
            monitor.done();
        }
        return result;
    }

    /**
     * Evaluates the new build path and output folder according to the settings on the first page. The resulting build path is set
     * by calling {@link #init(IBlackBerryProject, IPath, IClasspathEntry[], boolean)}. Clients can override this method.
     *
     * @param eclipseJavaProject
     *            the new project which is already created when this method is called.
     * @param monitor
     *            the progress monitor
     * @throws CoreException
     *             thrown when initializing the build path failed
     */
    protected void initializeBuildPath( IJavaProject eclipseJavaProject, IProgressMonitor monitor ) throws CoreException {
        if( monitor == null ) {
            monitor = new NullProgressMonitor();
        }
        monitor.beginTask( Messages.NewBlackBerryProjectWizardPageTwo_monitor_init_build_path, 2 );

        try {
            IClasspathEntry[] entries = null;
            IPath outputLocation = null;
            IProject eclipseProject = eclipseJavaProject.getProject();

            if( fKeepContent ) {
                if( !eclipseProject.getFile( FILENAME_CLASSPATH ).exists() ) {
                    final ClassPathDetector detector = new ClassPathDetector( fCurrProject, new SubProgressMonitor( monitor, 2 ) );
                    entries = detector.getClasspath();
                    outputLocation = detector.getOutputLocation();
                    if( entries.length == 0 )
                        entries = null;
                } else {
                    monitor.worked( 2 );
                }
            } else {
                List cpEntries = new ArrayList();
                IWorkspaceRoot root = eclipseProject.getWorkspace().getRoot();

                IClasspathEntry[] sourceClasspathEntries = fFirstPage.getSourceClasspathEntries();
                for( int i = 0; i < sourceClasspathEntries.length; i++ ) {
                    IPath path = sourceClasspathEntries[ i ].getPath();
                    if( path.segmentCount() > 1 ) {
                        IFolder folder = root.getFolder( path );
                        CoreUtility.createFolder( folder, true, true, new SubProgressMonitor( monitor, 1 ) );
                    }
                    cpEntries.add( sourceClasspathEntries[ i ] );
                }

                cpEntries.addAll( Arrays.asList( fFirstPage.getDefaultClasspathEntries() ) );

                entries = (IClasspathEntry[]) cpEntries.toArray( new IClasspathEntry[ cpEntries.size() ] );

                outputLocation = fFirstPage.getOutputLocation();
                if( outputLocation.segmentCount() > 1 ) {
                    IFolder folder = root.getFolder( outputLocation );
                    CoreUtility.createDerivedFolder( folder, true, true, new SubProgressMonitor( monitor, 1 ) );
                }
            }
            if( monitor.isCanceled() ) {
                throw new OperationCanceledException();
            }

            init( eclipseJavaProject, outputLocation, entries, false );
        } finally {
            monitor.done();
        }
    }

    protected void deleteProjectFile( URI projectLocation ) throws CoreException {
        IFileStore file = EFS.getStore( projectLocation );
        if( file.fetchInfo().exists() ) {
            IFileStore projectFile = file.getChild( FILENAME_PROJECT );
            if( projectFile.fetchInfo().exists() ) {
                projectFile.delete( EFS.NONE, null );
            }
        }
    }

    protected void rememberExisitingFolders( URI projectLocation ) {
        fOrginalFolders = new HashSet();

        try {
            IFileStore[] children = EFS.getStore( projectLocation ).childStores( EFS.NONE, null );
            for( int i = 0; i < children.length; i++ ) {
                IFileStore child = children[ i ];
                IFileInfo info = child.fetchInfo();
                if( info.isDirectory() && info.exists() && !fOrginalFolders.contains( child.getName() ) ) {
                    fOrginalFolders.add( child );
                }
            }
        } catch( CoreException e ) {
            JavaPlugin.log( e );
        }
    }

    private void restoreExistingFolders( URI projectLocation ) {
        try {
            IFileStore[] children = EFS.getStore( projectLocation ).childStores( EFS.NONE, null );
            for( int i = 0; i < children.length; i++ ) {
                IFileStore child = children[ i ];
                IFileInfo info = child.fetchInfo();
                if( info.isDirectory() && info.exists() && !fOrginalFolders.contains( child ) ) {
                    child.delete( EFS.NONE, null );
                    fOrginalFolders.remove( child );
                }
            }

            for( Iterator iterator = fOrginalFolders.iterator(); iterator.hasNext(); ) {
                IFileStore deleted = (IFileStore) iterator.next();
                deleted.mkdir( EFS.NONE, null );
            }
        } catch( CoreException e ) {
            JavaPlugin.log( e );
        }
    }

    protected void rememberExistingFiles( URI projectLocation ) throws CoreException {
        fDotProjectBackup = null;
        fDotClasspathBackup = null;

        IFileStore file = EFS.getStore( projectLocation );
        if( file.fetchInfo().exists() ) {
            IFileStore projectFile = file.getChild( FILENAME_PROJECT );
            if( projectFile.fetchInfo().exists() ) {
                fDotProjectBackup = createBackup( projectFile, "project-desc" ); //$NON-NLS-1$
            }
            IFileStore classpathFile = file.getChild( FILENAME_CLASSPATH );
            if( classpathFile.fetchInfo().exists() ) {
                fDotClasspathBackup = createBackup( classpathFile, "classpath-desc" ); //$NON-NLS-1$
            }
        }
    }

    private void restoreExistingFiles( URI projectLocation, IProgressMonitor monitor ) throws CoreException {
        int ticks = ( ( fDotProjectBackup != null ? 1 : 0 ) + ( fDotClasspathBackup != null ? 1 : 0 ) ) * 2;
        monitor.beginTask( "", ticks ); //$NON-NLS-1$
        try {
            IFileStore projectFile = EFS.getStore( projectLocation ).getChild( FILENAME_PROJECT );
            projectFile.delete( EFS.NONE, new SubProgressMonitor( monitor, 1 ) );
            if( fDotProjectBackup != null ) {
                copyFile( fDotProjectBackup, projectFile, new SubProgressMonitor( monitor, 1 ) );
            }
        } catch( IOException e ) {
            IStatus status = new Status( IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR,
                    Messages.NewBlackBerryProjectWizardPageTwo_problem_restore_project, e );
            throw new CoreException( status );
        }
        try {
            IFileStore classpathFile = EFS.getStore( projectLocation ).getChild( FILENAME_CLASSPATH );
            classpathFile.delete( EFS.NONE, new SubProgressMonitor( monitor, 1 ) );
            if( fDotClasspathBackup != null ) {
                copyFile( fDotClasspathBackup, classpathFile, new SubProgressMonitor( monitor, 1 ) );
            }
        } catch( IOException e ) {
            IStatus status = new Status( IStatus.ERROR, JavaUI.ID_PLUGIN, IStatus.ERROR,
                    Messages.NewBlackBerryProjectWizardPageTwo_problem_restore_classpath, e );
            throw new CoreException( status );
        }
    }

    private File createBackup( IFileStore source, String name ) throws CoreException {
        try {
            File bak = File.createTempFile( "eclipse-" + name, ".bak" ); //$NON-NLS-1$//$NON-NLS-2$
            copyFile( source, bak );
            return bak;
        } catch( IOException e ) {
            IStatus status = new Status( IStatus.ERROR, ContextManager.PLUGIN_ID, IStatus.ERROR, NLS.bind(
                    Messages.NewBlackBerryProjectWizardPageTwo_problem_backup, name ), e );
            throw new CoreException( status );
        }
    }

    private void copyFile( IFileStore source, File target ) throws IOException, CoreException {
        InputStream is = source.openInputStream( EFS.NONE, null );
        FileOutputStream os = new FileOutputStream( target );
        copyFile( is, os );
    }

    private void copyFile( File source, IFileStore target, IProgressMonitor monitor ) throws IOException, CoreException {
        FileInputStream is = new FileInputStream( source );
        OutputStream os = target.openOutputStream( EFS.NONE, monitor );
        copyFile( is, os );
    }

    private void copyFile( InputStream is, OutputStream os ) throws IOException {
        try {
            byte[] buffer = new byte[ 8192 ];
            while( true ) {
                int bytesRead = is.read( buffer );
                if( bytesRead == -1 )
                    break;

                os.write( buffer, 0, bytesRead );
            }
        } finally {
            try {
                is.close();
            } finally {
                os.close();
            }
        }
    }

    /**
     * Called from the wizard on finish.
     *
     * @param monitor
     *            the progress monitor
     * @throws CoreException
     *             thrown when the project creation or configuration failed
     * @throws InterruptedException
     *             thrown when the user cancelled the project creation
     */
    public void performFinish( IPluginContentWizard contentWizard, IProgressMonitor monitor ) throws CoreException,
            InterruptedException {
        try {
            monitor.beginTask( Messages.NewBlackBerryProjectWizardPageTwo_operation_create, 3 );
            if( fCurrProject == null ) {
                updateProject( new SubProgressMonitor( monitor, 1 ) );
            }
            String newProjectCompliance = fKeepContent ? null : fFirstPage.getCompilerCompliance();
            configureJavaProject( newProjectCompliance, new SubProgressMonitor( monitor, 2 ) );
            IJavaProject eclipseJavaProject = getJavaProject();
            initializeNatureAndBuilder( eclipseJavaProject );

            // generate content contributed by template wizards
            if( contentWizard != null ) {
                contentWizard.performFinish( fCurrProject, null, new SubProgressMonitor( monitor, 1 ) );
            }

            BlackBerryProject blackBerryProject = new BlackBerryProject( eclipseJavaProject );
            //initializeBlackBerryProperties( blackBerryProject.getProperties() );

            InternalImportUtils.initializeNewBlackBerryProperties( blackBerryProject.getProperties() );

            // save the model
            ContextManager.PLUGIN.setBBProperties( blackBerryProject.getProject().getName(), blackBerryProject.getProperties(),
                    true );

            final IFile metaFileHandler = blackBerryProject.getMetaFileHandler();

            Job openEditor = new Job( "Open Editor ..." ) {
                @Override
                protected IStatus run( IProgressMonitor monitor ) {
                    Display display = Display.getDefault();

                    display.asyncExec( new Runnable() {
                        @Override
                        public void run() {
                            if( RootPreferences.getOpenAppDescriptorOnNew() ) {
                                openAppDescriptor( metaFileHandler );
                            }
                            if( RootPreferences.getOpenStartupOnNew() ) {
                                ProjectUtils.openStartupPage();
                            }
                        }
                    } );

                    return Status.OK_STATUS;
                }
            };

            openEditor.setUser( false );
            openEditor.setSystem( true );
            openEditor.setPriority( Job.DECORATE );
            openEditor.schedule();

            monitor.done();
            fCurrProject = null;
            if( fIsAutobuild != null ) {
                CoreUtility.setAutoBuilding( fIsAutobuild.booleanValue() );
                fIsAutobuild = null;
            }
        } catch( ResourceException re ) {
            final IStatus status = re.getStatus();
            Display.getDefault().asyncExec( new Runnable() {
                public void run() {
                    Shell shell = ContextManager.getActiveWorkbenchWindow().getShell();
                    ErrorDialog.openError( shell, Messages.NewBlackBerryProjectWizardPageTwo_error_dialog_title,
                            Messages.NewBlackBerryProjectWizardPageTwo_error_dialog_message1, status );
                }
            } );
            fCurrProject = null;// reset the project
        } catch( Exception e ) {
            log.error( e.getMessage() );
            Display.getDefault().asyncExec( new Runnable() {
                public void run() {
                    Shell shell = ContextManager.getActiveWorkbenchWindow().getShell();
                    MessageDialog.openError( shell, Messages.NewBlackBerryProjectWizardPageTwo_error_dialog_title,
                            Messages.NewBlackBerryProjectWizardPageTwo_error_dialog_message1 );
                }
            } );
            fCurrProject = null;// reset the project
        }
    }

    /**
     * Creates the provisional project on which the wizard is working on. The provisional project is typically created when the
     * page is entered the first time. The early project creation is required to configure linked folders.
     *
     * @return the provisional project
     */
    protected IProject createProvisonalProject() {
        IStatus status = changeToNewProject();
        if( status != null && !status.isOK() ) {
            ErrorDialog.openError( getShell(), Messages.NewBlackBerryProjectWizardPageTwo_error_title, null, status );
        }
        return fCurrProject;
    }

    /**
     * Removes the provisional project. The provisional project is typically removed when the user cancels the wizard or goes back
     * to the first page.
     */
    protected void removeProvisonalProject() {
        if( !fCurrProject.exists() ) {
            fCurrProject = null;
            return;
        }

        IRunnableWithProgress op = new IRunnableWithProgress() {
            public void run( IProgressMonitor monitor ) throws InvocationTargetException, InterruptedException {
                doRemoveProject( monitor );
            }
        };

        try {
            getContainer().run( true, true, new WorkspaceModifyDelegatingOperation( op ) );
        } catch( InvocationTargetException e ) {
            final String title = Messages.NewBlackBerryProjectWizardPageTwo_error_remove_title;
            final String message = Messages.NewBlackBerryProjectWizardPageTwo_error_remove_message;
            ExceptionHandler.handle( e, getShell(), title, message );
        } catch( InterruptedException e ) {
            // cancel pressed
        }
    }

    private final void doRemoveProject( IProgressMonitor monitor ) throws InvocationTargetException {
        final boolean noProgressMonitor = ( fCurrProjectLocation == null ); // inside
        // workspace
        if( monitor == null || noProgressMonitor ) {
            monitor = new NullProgressMonitor();
        }
        monitor.beginTask( Messages.NewBlackBerryProjectWizardPageTwo_operation_remove, 3 );
        try {
            try {
                URI projLoc = fCurrProject.getLocationURI();

                boolean removeContent = !fKeepContent && fCurrProject.isSynchronized( IResource.DEPTH_INFINITE );
                if( !removeContent ) {
                    restoreExistingFolders( projLoc );
                }
                fCurrProject.delete( removeContent, false, new SubProgressMonitor( monitor, 2 ) );

                restoreExistingFiles( projLoc, new SubProgressMonitor( monitor, 1 ) );
            } finally {
                CoreUtility.setAutoBuilding( fIsAutobuild.booleanValue() ); // fIsAutobuild
                // must
                // be
                // set
                fIsAutobuild = null;
            }
        } catch( CoreException e ) {
            throw new InvocationTargetException( e );
        } finally {
            monitor.done();
            fCurrProject = null;
            fKeepContent = false;
        }
    }

    /**
     * Called from the wizard on cancel.
     */
    public void performCancel() {
        if( fCurrProject != null ) {
            removeProvisonalProject();
        }
    }

    /**
     * Open BlackBerry application descriptor in main editor.
     *
     * @param metaFileHandler
     *            The IFile for BlackBerry_App_Descriptor.xml
     */
    private void openAppDescriptor( IFile metaFileHandler ) {
        if( null != metaFileHandler && metaFileHandler.exists() ) {
            URI uri = metaFileHandler.getLocationURI();

            if( null != uri && StringUtils.isNotBlank( uri.toString() ) ) {
                IEditorInput input = new FileEditorInput( metaFileHandler );
                IEditorRegistry editorRegistry = PlatformUI.getWorkbench().getEditorRegistry();
                IEditorDescriptor editorDescriptor = editorRegistry.findEditor( BlackBerryProjectFormEditor.EDITOR_ID );

                if( null != editorDescriptor ) {
                    IWorkbench workbench = PlatformUI.getWorkbench();

                    if( null != workbench ) {
                        IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();

                        if( null != workbenchWindow ) {
                            IWorkbenchPage page = workbenchWindow.getActivePage();

                            try {
                                if( null != page ) {
                                    page.openEditor( input, editorDescriptor.getId() );
                                }
                            } catch( PartInitException e ) {
                                log.debug( "", e );
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Initialize nature and builder.
     */
    private void initializeNatureAndBuilder( IJavaProject eclipseProject ) {
        initializeJavaCompiler( eclipseProject );
        IProject iproject = eclipseProject.getProject();
        try {
            final IProjectDescription description = iproject.getDescription();
            setNatures( description );
            setBuilders( description );
            iproject.setDescription( description, new NullProgressMonitor() );
        } catch( final CoreException e ) {
            log.debug( e );
        }
    }

    /**
     * Initialize java compiler.
     */
    private void initializeJavaCompiler( IJavaProject eclipseProject ) {
        final Map< String, String > map = eclipseProject.getOptions( false );

        if( map.size() > 0 ) {
            map.remove( JavaCore.COMPILER_COMPLIANCE );
            map.remove( JavaCore.COMPILER_SOURCE );
            map.remove( JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM );
        }

        map.put( JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_3 );
        map.put( JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_4 );
        map.put( JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_2 );

        eclipseProject.setOptions( map );
    }

    /**
     * Sets the nature IDs for the {@link org.eclipse.core.resources.IProjectDescription} description.
     *
     * @param description
     *            the project description requiring new nature IDs
     */
    private void setNatures( final IProjectDescription description ) {
        final String[] prevNatures = description.getNatureIds();
        String[] newNatures = null;
        if( !description.hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
            newNatures = insertString( prevNatures, BlackBerryProjectCoreNature.NATURE_ID );
        }
        if( !description.hasNature( BlackBerryProjectPreprocessingNature.NATURE_ID ) ) {
            newNatures = insertString( null != newNatures ? newNatures : prevNatures,
                    BlackBerryProjectPreprocessingNature.NATURE_ID );
        }

        if( null != newNatures ) {
            description.setNatureIds( newNatures );
        }
    }

    /**
     * Sets the builders. {@link org.eclipse.core.resources.IProjectDescription}
     *
     * @param description
     *            the new builders
     */
    private void setBuilders( final IProjectDescription description ) {
        ICommand[] newICmds = null;
        final ICommand[] prevICmds = description.getBuildSpec();

        if( !hasBuilderID( prevICmds, ResourceBuilder.BUILDER_ID ) ) {
            newICmds = insertICommand( prevICmds, createICommand( description, ResourceBuilder.BUILDER_ID ) );
        }
        if( !hasBuilderID( prevICmds, PreprocessingBuilder.BUILDER_ID ) ) {
            newICmds = insertICommand( null != newICmds ? newICmds : prevICmds,
                    createICommand( description, PreprocessingBuilder.BUILDER_ID ) );
        }
        if( null != newICmds ) {
            description.setBuildSpec( newICmds );
        }
    }

    /**
     * Creates a new String[] with the String newStr prepended to the String[] prevStrArr.
     *
     * @param prevStrArr
     *            the previous String array
     * @param newStr
     *            the new String
     *
     * @return the String[]
     */
    private String[] insertString( final String[] prevStrArr, final String newStr ) {
        final String[] newStrArr = new String[ prevStrArr.length + 1 ];
        newStrArr[ 0 ] = newStr;
        System.arraycopy( prevStrArr, 0, newStrArr, 1, prevStrArr.length );
        return newStrArr;
    }

    /**
     * Checks for builder id.
     *
     * @param cmd
     *            the command
     * @param bid
     *            the builder ID
     *
     * @return true, if successful
     */
    private boolean hasBuilderID( final ICommand[] cmd, final String bid ) {
        for( final ICommand element : cmd ) {
            if( bid.equals( element.getBuilderName() ) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates an {@link org.eclipse.core.resources.ICommand}.
     *
     * @param description
     *            the {@link org.eclipse.core.resources.IProjectDescription}
     * @param bid
     *            the builder ID
     *
     * @return the ICommand
     */
    private ICommand createICommand( final IProjectDescription description, final String bid ) {
        final ICommand newCmd = description.newCommand();
        newCmd.setBuilderName( bid );
        return newCmd;
    }

    /**
     * Inserts an {@link org.eclipse.core.resources.ICommand} into an array of previous ICommands.
     *
     * @param prevICmdArr
     *            the previous ICommand array
     * @param newICmd
     *            the ICommand to be inserted
     *
     * @return the new ICommand[] containing the new command
     */
    private ICommand[] insertICommand( final ICommand[] prevICmdArr, final ICommand newICmd ) {
        final ICommand[] newICmdArr = new ICommand[ prevICmdArr.length + 1 ];
        newICmdArr[ 0 ] = newICmd;
        System.arraycopy( prevICmdArr, 0, newICmdArr, 1, prevICmdArr.length );
        return newICmdArr;
    }

    /**
     * Returns the IFieldData that is used by template selection page.
     *
     * @return The <code>IFieldData</code>
     */
    public IFieldData getData() {
        BBFieldData fd = new BBFieldData();
        fd.setId( ContextManager.PLUGIN_ID );
        fd.setMasterWizard( getWizard() );
        fd.setName( fFirstPage.getProjectName() );
        return fd;
    }

    //private void initializeBlackBerryProperties( BlackBerryProperties properties ) {
        // replace invalid characters in output file name
    //    properties.setValidOutputFileName( properties._packaging.getOutputFileName() );
    //}
}
