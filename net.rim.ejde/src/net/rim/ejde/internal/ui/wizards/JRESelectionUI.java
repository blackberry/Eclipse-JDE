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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import net.rim.ejde.internal.ui.widgets.dialog.ComboDialogField;
import net.rim.ejde.internal.ui.widgets.dialog.DialogField;
import net.rim.ejde.internal.ui.widgets.dialog.IDialogFieldListener;
import net.rim.ejde.internal.ui.widgets.dialog.SelectionButtonDialogField;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMUtils;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.CompliancePreferencePage;
import org.eclipse.jdt.internal.ui.preferences.PropertyAndPreferencePage;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathSupport;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.Policy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * A common UI for user to choose a BlackBerry JRE from all installed BlackBerry JREs. This is used by new project and project
 * import wizard.
 *
 * @author dmeng
 */
public class JRESelectionUI extends Observable implements Observer, SelectionListener, IDialogFieldListener {

    private static final String LAST_SELECTED_JRE_SETTINGS_KEY = JavaUI.ID_PLUGIN + ".last.selected.project.jre"; //$NON-NLS-1$
    private static final String LAST_SELECTED_JRE_KIND2_KEY = JavaUI.ID_PLUGIN + ".last.selected.jre.kind2"; //$NON-NLS-1$

    public static final int DEFAULT_RE = 0;
    public static final int PROJECT_RE = 1;

    private final SelectionButtonDialogField _useDefaultRE, _useProjectRE;
    private final ComboDialogField _RECombo;
    private Group _group;
    private Link _preferenceLink;
    private IVMInstall[] _installedVMs;
    private String[] _RECompliance;
    private boolean _enabled;
    private boolean _fireEvent;

    private AbstractBlackBerryWizardPage _wizardPage;

    /**
     * Constructor.
     *
     * @param wizardPage
     *            The <code>AbstractBlackBerryWizardPage</code>
     */
    public JRESelectionUI( AbstractBlackBerryWizardPage wizardPage ) {
        _useDefaultRE = new SelectionButtonDialogField( SWT.RADIO );
        _useDefaultRE.setLabelText( getDefaultJVMLabel() );
        _useProjectRE = new SelectionButtonDialogField( SWT.RADIO );
        _useProjectRE.setLabelText( Messages.NewBlackBerryProjectWizardPageOne_JREGroup_specific_compliance );
        _RECombo = new ComboDialogField( SWT.READ_ONLY );
        fillInstalledJREs( _RECombo );
        _RECombo.setDialogFieldListener( this );

        _useDefaultRE.setDialogFieldListener( this );
        _useProjectRE.setDialogFieldListener( this );

        _enabled = true;
        _wizardPage = wizardPage;
        _fireEvent = true;
    }

    /**
     * Creates the control.
     *
     * @param parent
     *            The parent composite
     * @return The created control
     */
    public Control createControl( Composite parent ) {
        _group = new Group( parent, SWT.NONE );
        _group.setFont( parent.getFont() );
        _group.setLayout( _wizardPage.initGridLayout( new GridLayout( 2, false ), true ) );
        _group.setText( Messages.NewBlackBerryProjectWizardPageOne_JREGroup_title );

        _useProjectRE.doFillIntoGrid( _group, 1 );
        Combo comboControl = _RECombo.getComboControl( _group );
        comboControl.setLayoutData( new GridData( GridData.FILL, GridData.CENTER, true, false ) );

        Control[] controls = _useDefaultRE.doFillIntoGrid( _group, 1 );
        // Fixed IDT 233814, make sure there is enough room to display the label if user change
        // default JRE from java to BB
        GridData gd = new GridData();
        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.minimumWidth = 260;
        controls[ 0 ].setLayoutData( gd );
        _preferenceLink = new Link( _group, SWT.NONE );
        _preferenceLink.setFont( _group.getFont() );
        _preferenceLink.setText( Messages.NewBlackBerryProjectWizardPageOne_JREGroup_link_description );
        _preferenceLink.setLayoutData( new GridData( GridData.END, GridData.CENTER, false, false ) );
        _preferenceLink.addSelectionListener( this );

        setDefaultButtonState();

        return _group;
    }

    /**
     * Notify observers for changes.
     */
    protected void fireEvent() {
        setChanged();
        notifyObservers();
    }

    private void fillInstalledJREs( ComboDialogField comboField ) {
        String selectedItem = getLastSelectedJRE();

        int selectionIndex = -1;

        if( _useProjectRE.isSelected() ) {
            selectionIndex = comboField.getSelectionIndex();
            if( selectionIndex != -1 ) {// paranoia
                selectedItem = comboField.getItems()[ selectionIndex ];
            }
        }

        List< IVMInstall > standins = VMUtils.getInstalledBBVMs();
        _installedVMs = ( standins.toArray( new IVMInstall[ standins.size() ] ) );

        Arrays.sort( _installedVMs, new Comparator< IVMInstall >() {
            public int compare( IVMInstall arg0, IVMInstall arg1 ) {
                String cc0, cc1;

                if( arg1 instanceof IVMInstall2 && arg0 instanceof IVMInstall2 ) {
                    cc0 = JavaModelUtil.getCompilerCompliance( (IVMInstall2) arg0, JavaCore.VERSION_1_4 );
                    cc1 = JavaModelUtil.getCompilerCompliance( (IVMInstall2) arg1, JavaCore.VERSION_1_4 );
                    int result = cc1.compareTo( cc0 );

                    if( result != 0 )
                        return result;
                }
                return Policy.getComparator().compare( arg0.getName(), arg1.getName() );
            }
        } );

        selectionIndex = -1;// find new index

        String[] jreLabels = new String[ _installedVMs.length ];

        _RECompliance = new String[ _installedVMs.length ];

        for( int i = 0; i < _installedVMs.length; i++ ) {
            jreLabels[ i ] = _installedVMs[ i ].getName();
            if( selectedItem != null && jreLabels[ i ].equals( selectedItem ) ) {
                selectionIndex = i;
            }
            if( _installedVMs[ i ] instanceof IVMInstall2 ) {
                _RECompliance[ i ] = JavaModelUtil.getCompilerCompliance( (IVMInstall2) _installedVMs[ i ], JavaCore.VERSION_1_4 );
            } else {
                _RECompliance[ i ] = JavaCore.VERSION_1_4;
            }
        }

        // don't fire event when setting combobox items.
        _fireEvent = false;
        comboField.setItems( jreLabels );
        if( selectionIndex == -1 ) {
            comboField.selectItem( getDefaultBBJRE() );
        } else {
            comboField.selectItem( selectedItem );
        }
        _fireEvent = true;
    }

    private String getDefaultBBJRE() {
        IVMInstall vm = VMUtils.getDefaultBBVM();
        return vm != null ? vm.getName() : "";
    }

    private String getDefaultJVMName() {
        IVMInstall install = JavaRuntime.getDefaultVMInstall();
        if( install != null ) {
            return install.getName();
        } else {
            return Messages.NewBlackBerryProjectWizardPageOne_UnknownDefaultJRE_name;
        }
    }

    private String getDefaultJVMLabel() {
        return NLS.bind( Messages.NewBlackBerryProjectWizardPageOne_JREGroup_default_compliance, getDefaultJVMName() );
    }

    /**
     * @see java.util.Observer#update(Observable, Object)
     */
    public void update( Observable o, Object arg ) {
        updateEnableState();
    }

    private void setDefaultButtonState() {
        _useDefaultRE.setEnabled( true );
        _useProjectRE.setEnabled( true );
        boolean isBBDefaultRE = VMUtils.isBlackBerryRuntimeTheWorkspaceDefault();
        // if the default JRE is non-BlackBerry, select project specific JRE
        if( !isBBDefaultRE ) {
            _useProjectRE.setSelection( true );
            _useDefaultRE.setSelection( false );
            setLastSelectedJREKind( PROJECT_RE );
        } else {
            int kind = getLastSelectedJREKind();
            _useProjectRE.setSelection( kind == PROJECT_RE );
            _useDefaultRE.setSelection( kind == DEFAULT_RE );
        }
        _RECombo.setEnabled( _useProjectRE.isSelected() );
        if( _preferenceLink != null ) {
            _preferenceLink.setEnabled( true );
        }
        if( _group != null ) {
            _group.setEnabled( true );
        }
    }

    public void setDefaultJRESelected() {
        _useDefaultRE.setSelection( true );
        _useProjectRE.setSelection( false );
        setLastSelectedJREKind( DEFAULT_RE );
        fireEvent();
    }

    private void updateEnableState() {
        _useDefaultRE.setEnabled( _enabled );
        _useProjectRE.setEnabled( _enabled );
        _RECombo.setEnabled( _enabled && _useProjectRE.isSelected() );
        if( _preferenceLink != null ) {
            _preferenceLink.setEnabled( _enabled );
        }
        if( _group != null ) {
            _group.setEnabled( _enabled );
        }
    }

    /**
     * Handle widget select event.
     *
     * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse .swt.events.SelectionEvent)
     */
    public void widgetSelected( SelectionEvent e ) {
        widgetDefaultSelected( e );
    }

    /**
     * Handle widget default select event.
     *
     * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org .eclipse.swt.events.SelectionEvent)
     */
    public void widgetDefaultSelected( SelectionEvent e ) {
        String jreID = BuildPathSupport.JRE_PREF_PAGE_ID;
        String eeID = BuildPathSupport.EE_PREF_PAGE_ID;
        String complianceId = CompliancePreferencePage.PREF_ID;
        Map data = new HashMap();
        data.put( PropertyAndPreferencePage.DATA_NO_LINK, Boolean.TRUE );
        PreferencesUtil.createPreferenceDialogOn( e.display.getActiveShell(), jreID, new String[] { jreID, complianceId, eeID },
                data ).open();

        handlePossibleJVMChange();
        fireEvent();
    }

    /**
     * Handle possible JVM change.
     */
    public void handlePossibleJVMChange() {
        _useDefaultRE.setLabelText( getDefaultJVMLabel() );
        fillInstalledJREs( _RECombo );
    }

    /**
     * Handle dialog field changed.
     *
     * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener
     *      #dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields. DialogField)
     */
    public void dialogFieldChanged( DialogField field ) {
        updateEnableState();
        if( field == _RECombo ) {
            if( _useProjectRE.isSelected() ) {
                storeSelectionValue( _RECombo, LAST_SELECTED_JRE_SETTINGS_KEY );
                if( _fireEvent ) {
                    fireEvent();
                }
            }
        } else if( field == _useDefaultRE ) {
            if( _useDefaultRE.isSelected() ) {
                setLastSelectedJREKind( DEFAULT_RE );
                _useProjectRE.setSelection( false );
                fireEvent();
            }
        } else if( field == _useProjectRE ) {
            if( _useProjectRE.isSelected() ) {
                setLastSelectedJREKind( PROJECT_RE );
                _useDefaultRE.setSelection( false );
                fireEvent();
            }
        }
    }

    private void storeSelectionValue( ComboDialogField combo, String preferenceKey ) {
        int index = combo.getSelectionIndex();
        if( index == -1 )
            return;

        String item = combo.getItems()[ index ];
        JavaPlugin.getDefault().getDialogSettings().put( preferenceKey, item );
    }

    private String getLastSelectedJRE() {
        IDialogSettings settings = JavaPlugin.getDefault().getDialogSettings();
        return settings.get( LAST_SELECTED_JRE_SETTINGS_KEY );
    }

    private int getLastSelectedJREKind() {
        int kind = PROJECT_RE;
        IDialogSettings settings = JavaPlugin.getDefault().getDialogSettings();
        if( settings.get( LAST_SELECTED_JRE_KIND2_KEY ) != null ) {
            kind = settings.getInt( LAST_SELECTED_JRE_KIND2_KEY );
        }
        return kind;
    }

    private void setLastSelectedJREKind( int newKind ) {
        JavaPlugin.getDefault().getDialogSettings().put( LAST_SELECTED_JRE_KIND2_KEY, newKind );
    }

    /**
     * Get selected JVM.
     *
     * @return Selected JVM or <code>null</code> if one is not found
     */
    public IVMInstall getSelectedJVM() {
        if( _useProjectRE.isSelected() ) {
            int index = _RECombo.getSelectionIndex();
            if( index >= 0 && index < _installedVMs.length ) { // paranoia
                return _installedVMs[ index ];
            }
            return null;
        }
        // user selects workspace default JRE
        return JavaRuntime.getDefaultVMInstall();
    }

    /**
     * Returns the JVM kind user selected (i.e. project specific or workspace default)
     *
     * @return The JVM kind user selected
     */
    public int getSelectedJVMKind() {
        if( _useProjectRE.isSelected() ) {
            return PROJECT_RE;
        }
        return DEFAULT_RE;
    }

    /**
     * Returns the JRE container path
     *
     * @return The JRE container path
     */
    public IPath getJREContainerPath() {
        if( _useProjectRE.isSelected() ) {
            int index = _RECombo.getSelectionIndex();
            if( index >= 0 && index < _installedVMs.length ) { // paranoia
                return JavaRuntime.newJREContainerPath( _installedVMs[ index ] );
            }
        }
        return null;
    }

    /**
     * Returns the selected JRE compliance level.
     *
     * @return The JRE compliance level
     */
    public String getSelectedCompilerCompliance() {
        if( _useProjectRE.isSelected() ) {
            int index = _RECombo.getSelectionIndex();
            if( index >= 0 && index < _RECompliance.length ) { // paranoia
                return _RECompliance[ index ];
            }
        }
        return null;
    }

    /**
     * Set the enable/disable the UI.
     *
     * @param enabled
     */
    public void setEnabled( boolean enabled ) {
        _enabled = enabled;
        updateEnableState();
    }
}
