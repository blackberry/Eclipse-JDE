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
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import net.rim.ejde.internal.imports.LegacyImportHelper;
import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;
import net.rim.ejde.internal.model.BlackBerryVMInstallType;
import net.rim.ejde.internal.ui.widgets.dialog.DialogField;
import net.rim.ejde.internal.ui.widgets.dialog.IDialogFieldListener;
import net.rim.ejde.internal.ui.widgets.dialog.IStringButtonAdapter;
import net.rim.ejde.internal.ui.widgets.dialog.SelectionButtonDialogField;
import net.rim.ejde.internal.ui.widgets.dialog.StringButtonDialogField;
import net.rim.ejde.internal.ui.widgets.dialog.StringDialogField;
import net.rim.ejde.internal.util.ImportUtils;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMUtils;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.preferences.CompliancePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage;
import org.eclipse.jdt.internal.ui.viewsupport.BasicElementLabels;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.workingsets.IWorkingSetIDs;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.WorkingSetConfigurationBlock;

/**
 * The first page of the New Java Project wizard. This page is typically used in combination with
 * {@link NewBlackBerryProjectWizardPageTwo}. Clients can extend this page to modify the UI: Add, remove or reorder sections.
 *
 * <p>
 * Clients may instantiate or subclass.
 * </p>
 *
 * @since 3.4
 */
@InternalFragmentReplaceable
public class BlackBerryProjectWizardPageOne extends AbstractBlackBerryWizardPage {

    private static final String PAGE_NAME = "NewBlackBerryProjectWizardPageOne"; //$NON-NLS-1$

    private final NameGroup _nameGroup;
    private final LocationGroup _locationGroup;
    private final JRESelectionUI _JREGroup;
    private final DetectGroup _detectGroup;
    private final Validator _validator;
    private final WorkingSetGroup _workingSetGroup;
    private static final IWorkingSet[] EMPTY_WORKING_SET_ARRAY = new IWorkingSet[ 0 ];

    /**
     * Default constructor.
     */
    public BlackBerryProjectWizardPageOne() {
        super( PAGE_NAME );
        setPageComplete( false );
        setTitle( Messages.NewBlackBerryProjectWizardPageOne_page_title );
        setDescription( Messages.NewBlackBerryProjectWizardPageOne_page_description );

        _nameGroup = new NameGroup();
        _locationGroup = new LocationGroup();
        _JREGroup = new JRESelectionUI( this );
        _workingSetGroup = new WorkingSetGroup();
        _detectGroup = new DetectGroup();

        // establish connections
        _nameGroup.addObserver( _locationGroup );
        _locationGroup.addObserver( _detectGroup );

        // initialize all elements
        _nameGroup.notifyObservers();

        // create and connect validator
        _validator = new Validator();
        _nameGroup.addObserver( _validator );
        _locationGroup.addObserver( _validator );
        _JREGroup.addObserver( _validator );
        _detectGroup.addObserver( _validator );

        // initialize defaults
        setProjectName( "" ); //$NON-NLS-1$
        setProjectLocationURI( null );
        setWorkingSets( new IWorkingSet[ 0 ] );

        initializeDefaultVM();
    }

    /**
     * Request a project name. Fires an event whenever the text field is changed, regardless of its content.
     */
    private final class NameGroup extends Observable implements IDialogFieldListener {

        protected final StringDialogField _nameField;

        public NameGroup() {
            // text field for project name
            _nameField = new StringDialogField();
            _nameField.setLabelText( Messages.NewBlackBerryProjectWizardPageOne_NameGroup_label_text );
            _nameField.setDialogFieldListener( this );
        }

        public Control createControl( Composite composite ) {
            Composite container = new Composite( composite, SWT.NONE );
            container.setFont( composite.getFont() );
            container.setLayout( initGridLayout( new GridLayout( 2, false ), false ) );

            _nameField.doFillIntoGrid( container, 2 );
            LayoutUtil.setHorizontalGrabbing( _nameField.getTextControl( null ) );

            return container;
        }

        protected void fireEvent() {
            setChanged();
            notifyObservers();
        }

        public String getName() {
            return _nameField.getText().trim();
        }

        public void postSetFocus() {
            _nameField.postSetFocusOnDialogField( getShell().getDisplay() );
        }

        public void setName( String name ) {
            _nameField.setText( name );
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener
         * #dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields. DialogField)
         */
        public void dialogFieldChanged( DialogField field ) {
            fireEvent();
        }
    }

    /**
     * Request a location. Fires an event whenever the checkbox or the location field is changed, regardless of whether the change
     * originates from the user or has been invoked programmatically.
     */
    private final class LocationGroup extends Observable implements Observer, IStringButtonAdapter, IDialogFieldListener {

        protected final SelectionButtonDialogField fWorkspaceRadio;
        protected final SelectionButtonDialogField fExternalRadio;
        protected final StringButtonDialogField fLocation;

        private String fPreviousExternalLocation;

        private static final String DIALOGSTORE_LAST_EXTERNAL_LOC = JavaUI.ID_PLUGIN + ".last.external.project"; //$NON-NLS-1$

        public LocationGroup() {
            fWorkspaceRadio = new SelectionButtonDialogField( SWT.RADIO );
            fWorkspaceRadio.setDialogFieldListener( this );
            fWorkspaceRadio.setLabelText( Messages.NewBlackBerryProjectWizardPageOne_LocationGroup_workspace_desc );

            fExternalRadio = new SelectionButtonDialogField( SWT.RADIO );
            fExternalRadio.setLabelText( Messages.NewBlackBerryProjectWizardPageOne_LocationGroup_external_desc );

            fLocation = new StringButtonDialogField( this );
            fLocation.setDialogFieldListener( this );
            fLocation.setLabelText( Messages.NewBlackBerryProjectWizardPageOne_LocationGroup_locationLabel_desc );
            fLocation.setButtonLabel( Messages.NewBlackBerryProjectWizardPageOne_LocationGroup_browseButton_desc );

            fExternalRadio.attachDialogField( fLocation );

            fWorkspaceRadio.setSelection( true );
            fExternalRadio.setSelection( false );

            fPreviousExternalLocation = ""; //$NON-NLS-1$
        }

        public Control createControl( Composite composite ) {
            final int numColumns = 3;

            final Group group = new Group( composite, SWT.NONE );
            group.setLayout( initGridLayout( new GridLayout( numColumns, false ), true ) );
            group.setText( Messages.NewBlackBerryProjectWizardPageOne_LocationGroup_title );

            fWorkspaceRadio.doFillIntoGrid( group, numColumns );
            fExternalRadio.doFillIntoGrid( group, numColumns );
            fLocation.doFillIntoGrid( group, numColumns );
            LayoutUtil.setHorizontalGrabbing( fLocation.getTextControl( null ) );

            return group;
        }

        protected void fireEvent() {
            setChanged();
            notifyObservers();
        }

        protected String getDefaultPath( String name ) {
            final IPath path = Platform.getLocation().append( name );
            return path.toOSString();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
         */
        public void update( Observable o, Object arg ) {
            if( isWorkspaceRadioSelected() ) {
                fLocation.setText( getDefaultPath( _nameGroup.getName() ) );
            }
            fireEvent();
        }

        public IPath getLocation() {
            if( isWorkspaceRadioSelected() ) {
                return Platform.getLocation();
            }
            return Path.fromOSString( fLocation.getText().trim() );
        }

        public boolean isWorkspaceRadioSelected() {
            return fWorkspaceRadio.isSelected();
        }

        /**
         * Returns <code>true</code> if the location is in the workspace
         *
         * @return <code>true</code> if the location is in the workspace
         */
        public boolean isLocationInWorkspace() {
            final String location = _locationGroup.getLocation().toOSString();
            IPath projectPath = Path.fromOSString( location );
            return Platform.getLocation().isPrefixOf( projectPath );
        }

        public void setLocation( IPath path ) {
            fWorkspaceRadio.setSelection( path == null );
            if( path != null ) {
                fLocation.setText( path.toOSString() );
            } else {
                fLocation.setText( getDefaultPath( _nameGroup.getName() ) );
            }
            fireEvent();
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter #
         * changeControlPressed(org.eclipse.jdt.internal.ui.wizards.dialogfields .DialogField)
         */
        public void changeControlPressed( DialogField field ) {
            final DirectoryDialog dialog = new DirectoryDialog( getShell() );
            dialog.setMessage( Messages.NewBlackBerryProjectWizardPageOne_directory_message );
            String directoryName = fLocation.getText().trim();
            if( directoryName.length() == 0 ) {
                String prevLocation = JavaPlugin.getDefault().getDialogSettings().get( DIALOGSTORE_LAST_EXTERNAL_LOC );
                if( prevLocation != null ) {
                    directoryName = prevLocation;
                }
            }

            if( directoryName.length() > 0 ) {
                final File path = new File( directoryName );
                if( path.exists() )
                    dialog.setFilterPath( directoryName );
            }
            final String selectedDirectory = dialog.open();
            if( selectedDirectory != null ) {
                String oldDirectory = new Path( fLocation.getText().trim() ).lastSegment();
                fLocation.setText( selectedDirectory );
                String lastSegment = new Path( selectedDirectory ).lastSegment();
                if( lastSegment != null && ( _nameGroup.getName().length() == 0 || _nameGroup.getName().equals( oldDirectory ) ) ) {
                    _nameGroup.setName( lastSegment );
                }
                JavaPlugin.getDefault().getDialogSettings().put( DIALOGSTORE_LAST_EXTERNAL_LOC, selectedDirectory );
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener
         * #dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields. DialogField)
         */
        public void dialogFieldChanged( DialogField field ) {
            if( field == fWorkspaceRadio ) {
                final boolean checked = fWorkspaceRadio.isSelected();
                if( checked ) {
                    fPreviousExternalLocation = fLocation.getText();
                    fLocation.setText( getDefaultPath( _nameGroup.getName() ) );
                } else {
                    fLocation.setText( fPreviousExternalLocation );
                }
            }
            fireEvent();
        }
    }

    private final class WorkingSetGroup {

        private WorkingSetConfigurationBlock fWorkingSetBlock;

        public WorkingSetGroup() {
            String[] workingSetIds = new String[] { IWorkingSetIDs.JAVA, IWorkingSetIDs.RESOURCE };
            fWorkingSetBlock = new WorkingSetConfigurationBlock( workingSetIds, JavaPlugin.getDefault().getDialogSettings() );
            // fWorkingSetBlock.setDialogMessage(NewWizardMessages.NewBlackBerryProjectWizardPageOne_WorkingSetSelection_message);
        }

        public Control createControl( Composite composite ) {
            Group workingSetGroup = new Group( composite, SWT.NONE );
            workingSetGroup.setFont( composite.getFont() );
            workingSetGroup.setText( Messages.NewBlackBerryProjectWizardPageOne_WorkingSets_group );
            workingSetGroup.setLayout( new GridLayout( 1, false ) );

            fWorkingSetBlock.createContent( workingSetGroup );

            return workingSetGroup;
        }

        public void setWorkingSets( IWorkingSet[] workingSets ) {
            fWorkingSetBlock.setWorkingSets( workingSets );
        }

        public IWorkingSet[] getSelectedWorkingSets() {
            return fWorkingSetBlock.getSelectedWorkingSets();
        }
    }

    /**
     * Show a various warnings such as when the project location is existing directory; or the the compiler user selected does not
     * match the workspace default level etc.
     */
    private final class DetectGroup extends Observable implements Observer, SelectionListener {

        private Link fHintText;
        private Label fIcon;
        private boolean _detectState;

        public DetectGroup() {
            _detectState = false;
        }

        public Control createControl( Composite parent ) {

            Composite composite = new Composite( parent, SWT.NONE );
            composite.setLayoutData( new GridData( SWT.FILL, SWT.TOP, true, false ) );
            GridLayout layout = new GridLayout( 2, false );
            layout.horizontalSpacing = 10;
            composite.setLayout( layout );

            fIcon = new Label( composite, SWT.LEFT );
            fIcon.setImage( Dialog.getImage( Dialog.DLG_IMG_MESSAGE_WARNING ) );
            GridData gridData = new GridData( SWT.LEFT, SWT.CENTER, false, false );
            fIcon.setLayoutData( gridData );

            fHintText = new Link( composite, SWT.WRAP );
            fHintText.setFont( composite.getFont() );
            fHintText.addSelectionListener( this );
            gridData = new GridData( GridData.FILL, SWT.FILL, true, true );
            gridData.widthHint = convertWidthInCharsToPixels( 50 );
            gridData.heightHint = convertHeightInCharsToPixels( 3 );
            fHintText.setLayoutData( gridData );

            handlePossibleJVMChange();
            return composite;
        }

        public void handlePossibleJVMChange() {

            // alert user if there is no default VM installed
            if( JavaRuntime.getDefaultVMInstall() == null ) {
                fHintText.setText( Messages.NewBlackBerryProjectWizardPageOne_NoJREFound_link );
                fHintText.setVisible( true );
                fIcon.setImage( Dialog.getImage( Dialog.DLG_IMG_MESSAGE_WARNING ) );
                fIcon.setVisible( true );
                return;
            }

            // alert user if user selected compiler compliance level is different from workspace default
            String selectedCompliance = _JREGroup.getSelectedCompilerCompliance();
            if( selectedCompliance != null ) {
                String defaultCompliance = JavaCore.getOption( JavaCore.COMPILER_COMPLIANCE );
                if( selectedCompliance.equals( defaultCompliance ) ) {
                    fHintText.setVisible( false );
                    fIcon.setVisible( false );
                } else {
                    fHintText.setText( NLS.bind(
                            Messages.NewBlackBerryProjectWizardPageOne_DetectGroup_differendWorkspaceCC_message,
                            new String[] { BasicElementLabels.getVersionName( defaultCompliance ),
                                    BasicElementLabels.getVersionName( selectedCompliance ) } ) );
                    fHintText.setVisible( true );
                    fIcon.setImage( Dialog.getImage( Dialog.DLG_IMG_MESSAGE_INFO ) );
                    fIcon.setVisible( true );
                }
                return;
            }

            // alert user if current JVM compliance level is different from workspace one
            selectedCompliance = JavaCore.getOption( JavaCore.COMPILER_COMPLIANCE );
            IVMInstall selectedJVM = _JREGroup.getSelectedJVM();
            // no BlackBerry JRE is installed
            if( selectedJVM == null ) {
                fHintText.setVisible( false );
                fIcon.setVisible( false );
                return;
            }

            String jvmCompliance = JavaCore.VERSION_1_4;
            if( selectedJVM instanceof IVMInstall2 ) {
                jvmCompliance = JavaModelUtil.getCompilerCompliance( (IVMInstall2) selectedJVM, JavaCore.VERSION_1_4 );
            }
            if( !selectedCompliance.equals( jvmCompliance )
                    && ( JavaModelUtil.is50OrHigher( selectedCompliance ) || JavaModelUtil.is50OrHigher( jvmCompliance ) ) ) {
                if( selectedCompliance.equals( JavaCore.VERSION_1_5 ) )
                    selectedCompliance = "5.0"; //$NON-NLS-1$
                else if( selectedCompliance.equals( JavaCore.VERSION_1_6 ) )
                    selectedCompliance = "6.0"; //$NON-NLS-1$

                fHintText.setText( NLS.bind(
                        Messages.NewBlackBerryProjectWizardPageOne_DetectGroup_jre_message,
                        new String[] { BasicElementLabels.getVersionName( selectedCompliance ),
                                BasicElementLabels.getVersionName( jvmCompliance ) } ) );
                fHintText.setVisible( true );
                fIcon.setImage( Dialog.getImage( Dialog.DLG_IMG_MESSAGE_WARNING ) );
                fIcon.setVisible( true );
            } else {
                fHintText.setVisible( false );
                fIcon.setVisible( false );
            }
        }

        /**
         * Detects if the project location is on existing directory.
         *
         * @return <code>true</code> if yes; otherwise return <code>false</code>
         */
        private boolean computeDetectState() {
            if( _locationGroup.isWorkspaceRadioSelected() ) {
                String name = _nameGroup.getName();
                if( name.length() == 0 || JavaPlugin.getWorkspace().getRoot().findMember( name ) != null ) {
                    return false;
                } else {
                    final File directory = _locationGroup.getLocation().append( name ).toFile();
                    return directory.isDirectory();
                }
            } else {
                final File directory = _locationGroup.getLocation().toFile();
                return directory.isDirectory();
            }
        }

        /**
         * @see java.util.Observer#update(Observable, Object)
         */
        public void update( Observable o, Object arg ) {
            if( o instanceof LocationGroup ) {
                boolean oldDetectState = _detectState;
                _detectState = computeDetectState();

                if( oldDetectState != _detectState ) {
                    setChanged();
                    notifyObservers();

                    if( _detectState ) {
                        fHintText.setVisible( true );
                        fHintText.setText( Messages.NewBlackBerryProjectWizardPageOne_DetectGroup_message );
                        fIcon.setImage( Dialog.getImage( Dialog.DLG_IMG_MESSAGE_INFO ) );
                        fIcon.setVisible( true );
                    } else {
                        handlePossibleJVMChange();
                    }
                }
            }
        }

        /**
         * Returns if the project is created on an existing directory.
         *
         * @return <code>true</code> if yes; otherwise return <code>false</code>
         */
        public boolean isProjectCreatedOnExistingFolder() {
            return _detectState;
        }

        /**
         * Handle widget selection.
         *
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse .swt.events.SelectionEvent)
         */
        public void widgetSelected( SelectionEvent e ) {
            widgetDefaultSelected( e );
        }

        /**
         * Handle default widget selection.
         *
         * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org .eclipse.swt.events.SelectionEvent)
         */
        public void widgetDefaultSelected( SelectionEvent e ) {
            String jreID = BuildPathSupport.JRE_PREF_PAGE_ID;
            String eeID = BuildPathSupport.EE_PREF_PAGE_ID;
            String complianceId = CompliancePreferencePage.PREF_ID;
            Map data = new HashMap();
            data.put( PropertyAndPreferencePage.DATA_NO_LINK, Boolean.TRUE );
            String id = "JRE".equals( e.text ) ? jreID : complianceId; //$NON-NLS-1$
            PreferencesUtil.createPreferenceDialogOn( getShell(), id, new String[] { jreID, complianceId, eeID }, data ).open();

            _JREGroup.handlePossibleJVMChange();
            handlePossibleJVMChange();
        }
    }

    /**
     * Validate this page and show appropriate warnings and error NewWizardMessages.
     */
    private final class Validator implements Observer {

        public void update( Observable o, Object arg ) {

            final IWorkspace workspace = JavaPlugin.getWorkspace();

            final String name = _nameGroup.getName();

            // check whether the project name field is empty
            if( name.length() == 0 ) {
                setErrorMessage( null );
                setMessage( Messages.NewBlackBerryProjectWizardPageOne_Message_enterProjectName );
                setPageComplete( false );
                return;
            }

            // check whether the project name is valid
            final IStatus nameStatus = workspace.validateName( name, IResource.PROJECT );
            if( !nameStatus.isOK() ) {
                setErrorMessage( nameStatus.getMessage() );
                setPageComplete( false );
                return;
            }

            // check whether project already exists
            final IProject handle = workspace.getRoot().getProject( name );
            if( handle.exists() ) {
                setErrorMessage( Messages.NewBlackBerryProjectWizardPageOne_Message_projectAlreadyExists );
                setPageComplete( false );
                return;
            }

            IPath projectLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation().append( name );
            if( projectLocation.toFile().exists() ) {
                try {
                    // correct casing
                    String canonicalPath = projectLocation.toFile().getCanonicalPath();
                    projectLocation = new Path( canonicalPath );
                } catch( IOException e ) {
                    JavaPlugin.log( e );
                }

                String existingName = projectLocation.lastSegment();
                if( !existingName.equals( _nameGroup.getName() ) ) {
                    setErrorMessage( NLS.bind(
                            Messages.NewBlackBerryProjectWizardPageOne_Message_invalidProjectNameForWorkspaceRoot,
                            BasicElementLabels.getResourceName( existingName ) ) );
                    setPageComplete( false );
                    return;
                }

            }

            final String location = _locationGroup.getLocation().toOSString();

            // check whether location is empty
            if( location.length() == 0 ) {
                setErrorMessage( null );
                setMessage( Messages.NewBlackBerryProjectWizardPageOne_Message_enterLocation );
                setPageComplete( false );
                return;
            }

            // check whether the location is a syntactically correct path
            if( !Path.EMPTY.isValidPath( location ) ) {
                setErrorMessage( Messages.NewBlackBerryProjectWizardPageOne_Message_invalidDirectory );
                setPageComplete( false );
                return;
            }

            IPath projectPath = Path.fromOSString( location );

            if( _locationGroup.isWorkspaceRadioSelected() )
                projectPath = projectPath.append( _nameGroup.getName() );

            if( projectPath.toFile().exists() ) {// create from existing source
                IPath sourcePath = Platform.getLocation();
                if( sourcePath.isPrefixOf( projectPath ) ) { // create
                    // from
                    // existing
                    // source
                    // in
                    // workspace
                    if( !Platform.getLocation().equals( projectPath.removeLastSegments( 1 ) ) ) {
                        setErrorMessage( Messages.NewBlackBerryProjectWizardPageOne_Message_notOnWorkspaceRoot );
                        setPageComplete( false );
                        return;
                    }

                    if( !projectPath.toFile().exists() ) {
                        setErrorMessage( Messages.NewBlackBerryProjectWizardPageOne_Message_notExisingProjectOnWorkspaceRoot );
                        setPageComplete( false );
                        return;
                    }
                } else if( projectPath.isPrefixOf( sourcePath ) ) {
                    setErrorMessage( NLS.bind( Messages.NewBlackBerryProjectWizardPageOne_ExistingSrcLocOverlapsWSMsg,
                            projectPath.toOSString(), sourcePath.toOSString() ) );
                    setPageComplete( false );
                    return;
                }
            } else if( !_locationGroup.isWorkspaceRadioSelected() ) {// create
                // at non
                // existing
                // external
                // location
                if( !canCreate( projectPath.toFile() ) ) {
                    setErrorMessage( Messages.NewBlackBerryProjectWizardPageOne_Message_cannotCreateAtExternalLocation );
                    setPageComplete( false );
                    return;
                }

                // If we do not place the contents in the workspace validate the
                // location.
                final IStatus locationStatus = workspace.validateProjectLocation( handle, projectPath );
                if( !locationStatus.isOK() ) {
                    setErrorMessage( locationStatus.getMessage() );
                    setPageComplete( false );
                    return;
                }
            }

            // check whether there is BB-JRE installed
            if( VMUtils.getInstalledBBVMs().isEmpty() ) {
                setErrorMessage( Messages.NewBlackBerryProjectWizardPageOne_Message_noBBJREInstalled );
                setPageComplete( false );
                return;
            }

            // handle the case where project is created on existing source
            boolean onExistingFolder = _detectGroup.isProjectCreatedOnExistingFolder();
            _JREGroup.setEnabled( !onExistingFolder );

            // Start Fix for IDT 321272
            if( !onExistingFolder ) {
                // check whether there is a valid BB-JRE selected
                IVMInstall vm = _JREGroup.getSelectedJVM();
                if( vm == null ) {
                    setErrorMessage( Messages.NewBlackBerryProjectWizardPageOne_Message_noJRESelected );
                    setPageComplete( false );
                    return;
                }

                // check if the JRE is BlackBerry JRE
                IVMInstallType vmType = vm.getVMInstallType();
                if( !BlackBerryVMInstallType.VM_ID.equals( vmType.getId() ) ) {
                    setErrorMessage( Messages.NewBlackBerryProjectWizardPageOne_Message_nonBBDefaultJRESelected );
                    setPageComplete( false );
                    return;
                }
            }
            // End Fix for IDT 321272

            setPageComplete( true );
            setErrorMessage( null );
            setMessage( null );
        }

        private boolean canCreate( File file ) {
            while( !file.exists() ) {
                file = file.getParentFile();
                if( file == null )
                    return false;
            }

            return file.canWrite();
        }
    }

    /**
     * The wizard owning this page can call this method to initialize the fields from the current selection and active part.
     *
     * @param selection
     *            used to initialize the fields
     * @param activePart
     *            the (typically active) part to initialize the fields or <code>null</code>
     */
    public void init( IStructuredSelection selection, IWorkbenchPart activePart ) {
        setWorkingSets( getSelectedWorkingSet( selection, activePart ) );
    }

    private void initializeDefaultVM() {
        JavaRuntime.getDefaultVMInstall();
    }

    /**
     * Creates the control.
     *
     * @param parent
     *            The parent composite
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets .Composite)
     */
    public void createControl( Composite parent ) {
        initializeDialogUnits( parent );

        final Composite container = new Composite( parent, SWT.NULL );
        container.setFont( parent.getFont() );
        container.setLayout( initGridLayout( new GridLayout( 1, false ), true ) );
        container.setLayoutData( new GridData( GridData.HORIZONTAL_ALIGN_FILL ) );

        // create UI elements
        Control nameControl = createNameControl( container );
        nameControl.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

        Control locationControl = createLocationControl( container );
        locationControl.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

        Control jreControl = createJRESelectionControl( container );
        jreControl.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

        Control workingSetControl = createWorkingSetControl( container );
        workingSetControl.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

        Control infoControl = createInfoControl( container );
        infoControl.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );

        setControl( container );
    }

    protected void setControl( Control newControl ) {
        Dialog.applyDialogFont( newControl );

        PlatformUI.getWorkbench().getHelpSystem().setHelp( newControl, IJavaHelpContextIds.NEW_JAVAPROJECT_WIZARD_PAGE );

        super.setControl( newControl );
    }

    /**
     * Creates the controls for the name field.
     *
     * @param composite
     *            the parent composite
     * @return the created control
     */
    protected Control createNameControl( Composite composite ) {
        return _nameGroup.createControl( composite );
    }

    /**
     * Creates the controls for the location field.
     *
     * @param composite
     *            the parent composite
     * @return the created control
     */
    protected Control createLocationControl( Composite composite ) {
        return _locationGroup.createControl( composite );
    }

    /**
     * Creates the controls for the JRE selection
     *
     * @param composite
     *            the parent composite
     * @return the created control
     */
    protected Control createJRESelectionControl( Composite composite ) {
        return _JREGroup.createControl( composite );
    }

    /**
     * Creates the controls for the working set selection.
     *
     * @param composite
     *            the parent composite
     * @return the created control
     */
    protected Control createWorkingSetControl( Composite composite ) {
        return _workingSetGroup.createControl( composite );
    }

    /**
     * Creates the controls for the info section.
     *
     * @param composite
     *            the parent composite
     * @return the created control
     */
    protected Control createInfoControl( Composite composite ) {
        return _detectGroup.createControl( composite );
    }

    /**
     * Gets a project name for the new project.
     *
     * @return the new project resource handle
     */
    public String getProjectName() {
        return _nameGroup.getName();
    }

    /**
     * Sets the name of the new project
     *
     * @param name
     *            The new name
     */
    public void setProjectName( String name ) {
        if( name == null )
            throw new IllegalArgumentException();

        _nameGroup.setName( name );
    }

    /**
     * Returns the current project location path as entered by the user, or <code>null</code> if the project should be created in
     * the workspace.
     *
     * @return the project location path or its anticipated initial value.
     */
    public URI getProjectLocationURI() {
        if( _locationGroup.isLocationInWorkspace() ) {
            return null;
        }
        return URIUtil.toURI( _locationGroup.getLocation() );
    }

    /**
     * Sets the project location of the new project or <code>null</code> if the project should be created in the workspace
     *
     * @param uri
     *            the new project location
     */
    public void setProjectLocationURI( URI uri ) {
        IPath path = uri != null ? URIUtil.toPath( uri ) : null;
        _locationGroup.setLocation( path );
    }

    public void setDefaultJRE() {
        _JREGroup.setDefaultJRESelected();
    }

    /**
     * Returns the compiler compliance to be used for the project, or <code>null</code> to use the workspace compiler compliance.
     *
     * @return compiler compliance to be used for the project or <code>null</code>
     */
    public String getCompilerCompliance() {
        return _JREGroup.getSelectedCompilerCompliance();
    }

    /**
     * Returns the default class path entries to be added on new projects. By default this is the JRE container as selected by the
     * user.
     *
     * @return returns the default class path entries
     */
    public IClasspathEntry[] getDefaultClasspathEntries() {
        IPath newPath = _JREGroup.getJREContainerPath();
        if( newPath != null ) {
            return new IClasspathEntry[] { JavaCore.newContainerEntry( newPath ) };
        }
        return PreferenceConstants.getDefaultJRELibrary();
    }

    /**
     * Returns the source class path entries to be added on new projects. The underlying resources may not exist. All entries that
     * are returned must be of kind {@link IClasspathEntry#CPE_SOURCE}.
     *
     * @return returns the source class path entries for the new project
     */
    public IClasspathEntry[] getSourceClasspathEntries() {
        List< IClasspathEntry > entries = new ArrayList< IClasspathEntry >();
        IPath sourceFolderPath = new Path( getProjectName() ).makeAbsolute();
        IPath srcPath = new Path( PreferenceConstants.getPreferenceStore().getString( PreferenceConstants.SRCBIN_SRCNAME ) );
        sourceFolderPath = sourceFolderPath.append( srcPath );
        entries.add( JavaCore.newSourceEntry( sourceFolderPath ) );

        // create res folder
        String resFolderName = ImportUtils.getImportPref( LegacyImportHelper.PROJECT_RES_FOLDER_NAME_KEY );
        if( resFolderName.length() > 0 ) {
            IPath resFolderPath = new Path( getProjectName() ).makeAbsolute();
            IPath resPath = new Path( resFolderName );
            resFolderPath = resFolderPath.append( resPath );
            entries.add( JavaCore.newSourceEntry( resFolderPath ) );
        }

        return entries.toArray( new IClasspathEntry[ 0 ] );
    }

    /**
     * Returns the source class path entries to be added on new projects. The underlying resource may not exist.
     *
     * @return returns the default class path entries
     */
    public IPath getOutputLocation() {
        IPath outputLocationPath = new Path( getProjectName() ).makeAbsolute();
        IPath binPath = new Path( PreferenceConstants.getPreferenceStore().getString( PreferenceConstants.SRCBIN_BINNAME ) );
        if( binPath.segmentCount() > 0 ) {
            outputLocationPath = outputLocationPath.append( binPath );
        }
        return outputLocationPath;
    }

    /**
     * Returns the working sets to which the new project should be added.
     *
     * @return the selected working sets to which the new project should be added
     */
    public IWorkingSet[] getWorkingSets() {
        return _workingSetGroup.getSelectedWorkingSets();
    }

    /**
     * Sets the working sets to which the new project should be added.
     *
     * @param workingSets
     *            The initial selected working sets
     */
    public void setWorkingSets( IWorkingSet[] workingSets ) {
        if( workingSets == null ) {
            throw new IllegalArgumentException();
        }
        _workingSetGroup.setWorkingSets( workingSets );
    }

    /**
     * Set the visibility of the wizard page.
     *
     * @param visible
     *            The visibility
     * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
     */
    public void setVisible( boolean visible ) {
        super.setVisible( visible );
        if( visible ) {
            _nameGroup.postSetFocus();
        }
    }

    private IWorkingSet[] getSelectedWorkingSet( IStructuredSelection selection, IWorkbenchPart activePart ) {
        IWorkingSet[] selected = getSelectedWorkingSet( selection );
        if( selected != null && selected.length > 0 ) {
            for( int i = 0; i < selected.length; i++ ) {
                if( !isValidWorkingSet( selected[ i ] ) )
                    return EMPTY_WORKING_SET_ARRAY;
            }
            return selected;
        }

        if( !( activePart instanceof PackageExplorerPart ) )
            return EMPTY_WORKING_SET_ARRAY;

        PackageExplorerPart explorerPart = (PackageExplorerPart) activePart;
        if( explorerPart.getRootMode() == PackageExplorerPart.PROJECTS_AS_ROOTS ) {
            // Get active filter
            IWorkingSet filterWorkingSet = explorerPart.getFilterWorkingSet();
            if( filterWorkingSet == null )
                return EMPTY_WORKING_SET_ARRAY;

            if( !isValidWorkingSet( filterWorkingSet ) )
                return EMPTY_WORKING_SET_ARRAY;

            return new IWorkingSet[] { filterWorkingSet };
        } else {
            // If we have been gone into a working set return the working set
            Object input = explorerPart.getViewPartInput();
            if( !( input instanceof IWorkingSet ) )
                return EMPTY_WORKING_SET_ARRAY;

            IWorkingSet workingSet = (IWorkingSet) input;
            if( !isValidWorkingSet( workingSet ) )
                return EMPTY_WORKING_SET_ARRAY;

            return new IWorkingSet[] { workingSet };
        }
    }

    private IWorkingSet[] getSelectedWorkingSet( IStructuredSelection selection ) {
        if( !( selection instanceof ITreeSelection ) )
            return EMPTY_WORKING_SET_ARRAY;

        ITreeSelection treeSelection = (ITreeSelection) selection;
        if( treeSelection.isEmpty() )
            return EMPTY_WORKING_SET_ARRAY;

        List elements = treeSelection.toList();
        if( elements.size() == 1 ) {
            Object element = elements.get( 0 );
            TreePath[] paths = treeSelection.getPathsFor( element );
            if( paths.length != 1 )
                return EMPTY_WORKING_SET_ARRAY;

            TreePath path = paths[ 0 ];
            if( path.getSegmentCount() == 0 )
                return EMPTY_WORKING_SET_ARRAY;

            Object candidate = path.getSegment( 0 );
            if( !( candidate instanceof IWorkingSet ) )
                return EMPTY_WORKING_SET_ARRAY;

            IWorkingSet workingSetCandidate = (IWorkingSet) candidate;
            if( isValidWorkingSet( workingSetCandidate ) )
                return new IWorkingSet[] { workingSetCandidate };

            return EMPTY_WORKING_SET_ARRAY;
        }

        ArrayList result = new ArrayList();
        for( Iterator iterator = elements.iterator(); iterator.hasNext(); ) {
            Object element = iterator.next();
            if( element instanceof IWorkingSet && isValidWorkingSet( (IWorkingSet) element ) ) {
                result.add( element );
            }
        }
        return (IWorkingSet[]) result.toArray( new IWorkingSet[ result.size() ] );
    }

    private static boolean isValidWorkingSet( IWorkingSet workingSet ) {
        String id = workingSet.getId();
        if( !IWorkingSetIDs.JAVA.equals( id ) && !IWorkingSetIDs.RESOURCE.equals( id ) )
            return false;

        if( workingSet.isAggregateWorkingSet() )
            return false;

        return true;
    }
}
