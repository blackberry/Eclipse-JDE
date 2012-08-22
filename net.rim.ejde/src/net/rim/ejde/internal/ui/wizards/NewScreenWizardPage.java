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

import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.NatureUtils;
import net.rim.ejde.internal.util.StatusFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.CodeGeneration;
import org.eclipse.jdt.ui.wizards.NewTypeWizardPage;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page to create a new BlackBerry screen.
 *
 * @author David Meng
 */
public class NewScreenWizardPage extends NewTypeWizardPage {

    // Screen types
    private static final int MAIN_SCREEN = 0;
    private static final int FULL_SCREEN = 1;
    private static final int POPUP_SCREEN = 2;

    private final static String PAGE_NAME = "NewScreenWizardPage"; //$NON-NLS-1$
    private final static String SETTINGS_SCREENTYPE = "screen_type"; //$NON-NLS-1$
    public static final String NEW_SCREEN_WIZARD_PAGE = "net.rim.ejde.new_screen_wizard_page_context"; //$NON-NLS-1$

    private int _screenType;
    private Combo _screenTypeCombo;
    private Text _descriptionText;

    /**
     * Creates a new <code>NewScreenWizardPage</code>
     */
    public NewScreenWizardPage() {
        super( true, PAGE_NAME );

        setTitle( Messages.NewScreenWizardPage_title );
        setDescription( Messages.NewScreenWizardPage_description );
    }

    // -------- Initialization ---------

    /**
     * The wizard owning this page is responsible for calling this method with the current selection. The selection is used to
     * initialize the fields of the wizard page.
     *
     * @param selection
     *            used to initialize the fields
     */
    public void init( IStructuredSelection selection ) {
        IJavaElement jelem = getInitialJavaElement( selection );
        initContainerPage( jelem );
        initTypePage( jelem );
        doStatusUpdate();
    }

    // ------ validation --------
    private void doStatusUpdate() {
        // status of all used components
        IStatus[] status = new IStatus[] { fContainerStatus, isEnclosingTypeSelected() ? fEnclosingTypeStatus : fPackageStatus,
                fTypeNameStatus, fModifierStatus, fSuperClassStatus, fSuperInterfacesStatus };

        // the mode severe status will be displayed and the OK button enabled/disabled.
        updateStatus( status );
    }

    /*
     * @see NewContainerWizardPage#handleFieldChanged
     */
    protected void handleFieldChanged( String fieldName ) {
        super.handleFieldChanged( fieldName );

        doStatusUpdate();
    }

    // ------ UI --------

    /*
     * @see WizardPage#createControl
     */
    public void createControl( Composite parent ) {

        initializeDialogUnits( parent );

        Composite composite = new Composite( parent, SWT.NONE );
        composite.setFont( parent.getFont() );

        int nColumns = 4;

        GridLayout layout = new GridLayout();
        layout.numColumns = nColumns;
        composite.setLayout( layout );

        // pick & choose the wanted UI components

        createContainerControls( composite, nColumns );
        createPackageControls( composite, nColumns );
        createTypeNameControls( composite, nColumns );
        createSeparator( composite, nColumns );

        Composite screenTypeComposite = new Composite( composite, SWT.NONE );
        screenTypeComposite.setLayout( new GridLayout() );
        GridData gridData = new GridData( GridData.FILL_BOTH );
        gridData.horizontalSpan = 4;
        screenTypeComposite.setLayoutData( gridData );

        Label screenTypeLabel = new Label( screenTypeComposite, SWT.NONE | SWT.WRAP );
        screenTypeLabel.setText( Messages.NewScreenWizardPage_screenType );

        _screenTypeCombo = new Combo( screenTypeComposite, SWT.READ_ONLY | SWT.DROP_DOWN );
        gridData = new GridData( GridData.FILL_HORIZONTAL );
        _screenTypeCombo.setLayoutData( gridData );
        _screenTypeCombo.setItems( new String[] { "Main Screen", "Full Screen", "Popup Screen" } );
        _screenTypeCombo.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                setScreenDescription( _screenTypeCombo.getSelectionIndex() );
            }
        } );

        Label descriptionLabel = new Label( screenTypeComposite, SWT.NONE | SWT.WRAP );
        descriptionLabel.setText( Messages.NewScreenWizardPage_screenTypeDesc );

        _descriptionText = new Text( screenTypeComposite, SWT.WRAP | SWT.READ_ONLY | SWT.BORDER );
        _descriptionText.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        setControl( composite );

        Dialog.applyDialogFont( composite );
        PlatformUI.getWorkbench().getHelpSystem().setHelp( composite, NEW_SCREEN_WIZARD_PAGE );

        // set screen type
        int screenType = MAIN_SCREEN;
        IDialogSettings dialogSettings = getDialogSettings();
        if( dialogSettings != null ) {
            IDialogSettings section = dialogSettings.getSection( PAGE_NAME );
            if( section != null ) {
                if( section.get( SETTINGS_SCREENTYPE ) != null ) {
                    screenType = section.getInt( SETTINGS_SCREENTYPE );
                }
            }
        }
        setInitialScreenType( screenType );
        setScreenDescription( screenType );
    }

    /*
     * @see WizardPage#becomesVisible
     */
    public void setVisible( boolean visible ) {
        super.setVisible( visible );
        if( !visible ) {
            // store the screen type
            IDialogSettings dialogSettings = getDialogSettings();
            if( dialogSettings != null ) {
                IDialogSettings section = dialogSettings.getSection( PAGE_NAME );
                if( section == null ) {
                    section = dialogSettings.addNewSection( PAGE_NAME );
                }
                section.put( SETTINGS_SCREENTYPE, getScreenType() );
            }
        }
    }

    // ---- creation ----------------

    private int getScreenType() {
        return _screenTypeCombo.getSelectionIndex();
    }

    private void setInitialScreenType( int screenType ) {
        _screenTypeCombo.select( screenType );
    }

    private void setScreenDescription( int screenType ) {
        _screenType = screenType;
        switch( _screenType ) {
            case MAIN_SCREEN:
                _descriptionText.setText( Messages.NewScreenWizardPage_mainScreenDesc );
                setSuperClass( "net.rim.device.api.ui.container.MainScreen", false );
                break;
            case FULL_SCREEN:
                _descriptionText.setText( Messages.NewScreenWizardPage_fullScreenDesc );
                setSuperClass( "net.rim.device.api.ui.container.FullScreen", false );
                break;
            case POPUP_SCREEN:
                _descriptionText.setText( Messages.NewScreenWizardPage_popupScreenDesc );
                setSuperClass( "net.rim.device.api.ui.container.PopupScreen", false );
                break;
        }
    }

    /*
     * @see NewTypeWizardPage#createTypeMembers
     */
    protected void createTypeMembers( IType type, ImportsManager imports, IProgressMonitor monitor ) throws CoreException {

        createInheritedMethods( type, false, false, imports, new SubProgressMonitor( monitor, 1 ) );

        String className = getTypeName();
        StringBuffer buf = new StringBuffer();
        final String lineDelim = "\n"; // OK, since content is formatted afterwards //$NON-NLS-1$
        String comment = CodeGeneration.getMethodComment( type.getCompilationUnit(), type.getTypeQualifiedName( '.' ), className,
                new String[ 0 ], new String[ 0 ], null, null, lineDelim ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if( comment != null ) {
            buf.append( comment );
            buf.append( lineDelim );
        }
        buf.append( "public " + className + "() {" ); //$NON-NLS-1$
        buf.append( lineDelim );
        StringBuffer bodyStatement = new StringBuffer();
        if( _screenType == MAIN_SCREEN ) {
            bodyStatement.append( "super( MainScreen.VERTICAL_SCROLL | MainScreen.VERTICAL_SCROLLBAR );" );
        } else if( _screenType == FULL_SCREEN ) {
            bodyStatement.append( "super( FullScreen.VERTICAL_SCROLL | FullScreen.VERTICAL_SCROLLBAR );" );
        } else if( _screenType == POPUP_SCREEN ) {
            imports.addImport( "net.rim.device.api.ui.Field" );
            imports.addImport( "net.rim.device.api.ui.component.ButtonField" );
            imports.addImport( "net.rim.device.api.ui.container.PopupScreen" );
            imports.addImport( "net.rim.device.api.ui.container.VerticalFieldManager" );
            imports.addImport( "net.rim.device.api.ui.component.LabelField" );
            bodyStatement.append( "super( new VerticalFieldManager() );" );
            bodyStatement.append( lineDelim );
            bodyStatement.append( "LabelField labelField = new LabelField(\"Popup Screen\", Field.FIELD_HCENTER);" );
            bodyStatement.append( lineDelim );
            bodyStatement.append( "add(labelField);" );
            bodyStatement.append( lineDelim );
            bodyStatement
                    .append( "ButtonField btnfldOk = new ButtonField(\"OK\", ButtonField.CONSUME_CLICK | Field.FIELD_HCENTER );" );
            bodyStatement.append( lineDelim );
            bodyStatement.append( "btnfldOk.setMinimalWidth(100);" );
            bodyStatement.append( lineDelim );
            bodyStatement.append( "add(btnfldOk);" );
            bodyStatement.append( lineDelim );
            bodyStatement
                    .append( "ButtonField btnfldCancel = new ButtonField(\"Cancel\", ButtonField.CONSUME_CLICK | Field.FIELD_HCENTER );" );
            bodyStatement.append( "add(btnfldCancel);" );
        }
        final String content = CodeGeneration.getMethodBodyContent( type.getCompilationUnit(), type.getTypeQualifiedName( '.' ),
                className, true, bodyStatement.toString(), lineDelim );
        if( content != null && content.length() != 0 ) {
            buf.append( content );
        }
        // buf.append( bodyStatement );
        buf.append( lineDelim );
        buf.append( "}" ); //$NON-NLS-1$
        type.createMethod( buf.toString(), null, false, null );

        if( monitor != null ) {
            monitor.done();
        }
    }

    protected IStatus containerChanged() {
        IJavaProject project = getJavaProject();
        if( project != null ) {
            IProject iproject = project.getProject();
            if( !NatureUtils.hasBBNature( iproject ) ) {
                setErrorMessage( Messages.NewScreenWizardPage_invalidProject );
                return StatusFactory.createErrorStatus( Messages.NewScreenWizardPage_invalidProject );
            }
        }
        return super.containerChanged();
    }
}
