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
package net.rim.ejde.internal.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.PreprocessorTag;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerrySDKInstall;
import net.rim.ejde.internal.model.BlackBerryVMInstallType;
import net.rim.ejde.internal.model.preferences.PreprocessorPreferences;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallChangedListener;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * This class represent a common UI of the view for preprocess directives.
 *
 */
public abstract class PreprocessDirectiveUI implements IPropertyChangeListener, IVMInstallChangedListener {
    static private final Logger log = Logger.getLogger( PreprocessDirectiveUI.class );
    private BlackBerryProject _bbProject;
    private int _scope;
    private Composite _parent;
    private CheckboxTableViewer _preprocessorTableViewer;
    private Table _fTable;
    private Button _addButton;
    private Button _removeButton;
    private Button _editButton;
    private Button _selectAllButton;
    private Button _deselectAllButton;

    /**
     * Construct a PreprocessDirectiveUI instance.
     *
     * @param parent
     * @param scope
     */
    public PreprocessDirectiveUI( Composite parent, int scope ) {
        this( parent, scope, null );
    }

    /**
     * Construct a PreprocessDirectiveUI instance.
     *
     * @param parent
     * @param scope
     * @param bbProject
     */
    public PreprocessDirectiveUI( Composite parent, int scope, BlackBerryProject bbProject ) {
        _parent = parent;
        _scope = scope;
        _bbProject = bbProject;
        createContent();
    }

    private void createContent() {
        Composite main = new Composite( _parent, SWT.NONE );
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.numColumns = 1;
        main.setLayout( layout );
        main.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        Composite tableButtonComposite = new Composite( main, SWT.NONE );
        tableButtonComposite.setLayout( BasicPrefsPage.getGridLayout( 2, 5, 0, 0, 0 ) );
        tableButtonComposite.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        Composite tableComposite = new Composite( tableButtonComposite, SWT.NONE );
        tableComposite.setLayout( BasicPrefsPage.getGridLayout( 1, 0, 0, 0, 5 ) );
        tableComposite.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        Font font = _parent.getFont();
        _fTable = new Table( tableComposite, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION );

        GridData gd = new GridData( GridData.FILL_BOTH );
        gd.heightHint = 250;
        gd.widthHint = 338;
        _fTable.setLayoutData( gd );
        _fTable.setFont( font );
        _fTable.setHeaderVisible( true );
        _fTable.setLinesVisible( true );
        _fTable.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                updateButtons();
            }
        } );

        TableColumn statusColumn = new TableColumn( _fTable, SWT.NONE );
        statusColumn.setText( Messages.BuildPrefsPage_PreprocessActiveColulmnHeader );
        statusColumn.setWidth( 50 );

        TableColumn tagsColumn = new TableColumn( _fTable, SWT.NONE );
        tagsColumn.setText( Messages.BuildPrefsPage_PreprocessTagsColulmnHeader );
        tagsColumn.setWidth( 200 );
        tagsColumn.addSelectionListener( new SelectionAdapter() {

            @Override
            public void widgetSelected( SelectionEvent e ) {
                Object input = _preprocessorTableViewer.getInput();
                if( input instanceof PreprocessorTag[] ) {
                    PreprocessorTag[] tags = (PreprocessorTag[]) input;
                    Arrays.sort( tags, new DirectiveString() );
                    _preprocessorTableViewer.setInput( tags );
                }
            }
        } );
        TableColumn scopeColumn = new TableColumn( _fTable, SWT.NONE );
        scopeColumn.setText( Messages.BuildPrefsPage_PreprocessScopeColulmnHeader );
        scopeColumn.setWidth( 100 );
        scopeColumn.addSelectionListener( new SelectionAdapter() {

            @Override
            public void widgetSelected( SelectionEvent e ) {
                Object input = _preprocessorTableViewer.getInput();
                if( input instanceof PreprocessorTag[] ) {
                    PreprocessorTag[] tags = (PreprocessorTag[]) input;
                    Arrays.sort( tags, new ScopeComparator() );
                    _preprocessorTableViewer.setInput( tags );
                }
            }
        } );

        _preprocessorTableViewer = new CheckboxTableViewer( _fTable );
        _preprocessorTableViewer.setContentProvider( new PreprocessorTagContentProvider() );
        _preprocessorTableViewer.setLabelProvider( new PreprocessorTagLabelProvider() );
        _preprocessorTableViewer.setCheckStateProvider( new PreprocessorTagCheckStateProvider() );
        _preprocessorTableViewer.addCheckStateListener( new ICheckStateListener() {
            @Override
            public void checkStateChanged( CheckStateChangedEvent event ) {
                Object element = event.getElement();
                if( element instanceof PreprocessorTag ) {
                    PreprocessorTag tag = (PreprocessorTag) element;
                    if( tag.getScopeID() <= _scope ) {
                        tag.setIsActive( Boolean.valueOf( event.getChecked() ) );
                        performChanged();
                    } else {
                        _preprocessorTableViewer.setChecked( tag, !event.getChecked() );
                    }
                }
            }
        } );
        // create a button group with buttons
        Composite buttonComposite = new Composite( tableButtonComposite, SWT.NONE );
        buttonComposite.setLayout( BasicPrefsPage.getGridLayout( 1, 0, 0, 0, 0 ) );
        buttonComposite.setLayoutData( new GridData( GridData.FILL_VERTICAL ) );

        // add "Add..." button
        _addButton = new Button( buttonComposite, SWT.PUSH );
        _addButton.setText( Messages.BasicPrefsPage_AddButtonLabel );
        BasicPrefsPage.setDialogConfirmButtonLayoutData( _addButton, 0 );

        // add "Remove" button
        _removeButton = new Button( buttonComposite, SWT.PUSH );
        _removeButton.setText( Messages.BasicPrefsPage_RemoveButtonLabel );
        BasicPrefsPage.setDialogConfirmButtonLayoutData( _removeButton, 5 );

        // add "Edit" button
        _editButton = new Button( buttonComposite, SWT.PUSH );
        _editButton.setText( Messages.BasicPrefsPage_EditButtonLabel );
        BasicPrefsPage.setDialogConfirmButtonLayoutData( _editButton, 5 );

        // add "Select All" button
        _selectAllButton = new Button( buttonComposite, SWT.PUSH );
        _selectAllButton.setText( Messages.BasicPrefsPage_SelectAllButtonLabel );
        BasicPrefsPage.setDialogConfirmButtonLayoutData( _selectAllButton, 5 );

        // add "De-select All" button
        _deselectAllButton = new Button( buttonComposite, SWT.PUSH );
        _deselectAllButton.setText( Messages.BasicPrefsPage_DeselectAllButtonLabel );
        BasicPrefsPage.setDialogConfirmButtonLayoutData( _deselectAllButton, 5 );

        // add listener to listen to the event when "Add..." button is pressed
        final PreprocessDirectiveUI ui = this;
        _addButton.addSelectionListener( new SelectionAdapter() {
            PreprocessDefineInputValidator validator = new PreprocessDefineInputValidator( ui );

            @Override
            public void widgetSelected( SelectionEvent e ) {
                InputDialog dialog = new InputDialog( _parent.getShell(), Messages.BuildPrefsPage_PreprocessAddDialogTitle,
                        Messages.BuildPrefsPage_PreprocessDialogLabel, "", validator );
                dialog.open();
                String tags = dialog.getValue();
                PreprocessorTag tag = null;
                List< PreprocessorTag > input = getInput();
                boolean changed = false;
                for( String tagString : StringUtils.split( tags, IConstants.SEMICOLON_MARK ) ) {
                    tag = new PreprocessorTag( tagString, true, _scope );
                    input.add( tag );
                    if( !changed ) {
                        changed = true;
                    }
                }
                if( changed ) {
                    performChanged();
                }
                _preprocessorTableViewer.setInput( input.toArray( new PreprocessorTag[ input.size() ] ) );
                enableNonAddButtons();
            }
        } );

        // add listener to listen to the event when "Remove" button is pressed
        _removeButton.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                List< PreprocessorTag > input = getInput();
                TableItem[] selections = _fTable.getSelection();
                for( int i = 0; i < selections.length; i++ ) {
                    input.remove( selections[ i ].getData() );
                }
                _preprocessorTableViewer.setInput( input.toArray( new PreprocessorTag[ input.size() ] ) );
                disableNonAddButtons();
                if( selections.length > 0 ) {
                    performChanged();
                }
            }
        } );

        // add listener to listen to the event when "Edit..." button is pressed
        _editButton.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                int[] selectedIndexes = _fTable.getSelectionIndices();
                PreprocessDefineInputValidator validator = null;
                String tagString = null;
                List< PreprocessorTag > input = getInput();
                PreprocessorTag tag;
                boolean changed = false;
                for( int tagIndex : selectedIndexes ) {
                    TableItem item = _fTable.getItem( tagIndex );
                    String initialText = item.getText( 1 );
                    validator = new PreprocessDefineInputValidator( ui, true, initialText );
                    InputDialog dialog = new InputDialog( _parent.getShell(), Messages.BuildPrefsPage_PreprocessEditDialogTitle,
                            Messages.BuildPrefsPage_PreprocessEditLabel, initialText, validator );
                    dialog.open();
                    tagString = dialog.getValue();
                    if( ( !initialText.equals( tagString ) ) && ( validator.isValid( tagString ) == null ) ) {
                        tag = new PreprocessorTag( tagString, true, _scope );
                        input.remove( tagIndex );
                        input.add( tagIndex, tag );
                        if( !changed ) {
                            changed = true;
                        }
                    }
                }
                _preprocessorTableViewer.setInput( input.toArray( new PreprocessorTag[ input.size() ] ) );
                if( changed ) {
                    performChanged();
                }
            }
        } );

        // add listener to listen to the event when "Select All" button is pressed
        _selectAllButton.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                List< PreprocessorTag > input = getInput();
                boolean changed = false;
                for( PreprocessorTag tag : input ) {
                    if( tag.getScopeID() == _scope && !tag.isActive() ) {
                        tag.setIsActive( true );
                        if( !changed ) {
                            changed = true;
                        }
                    }
                }
                _preprocessorTableViewer.setInput( input.toArray( new PreprocessorTag[ input.size() ] ) );
                if( changed ) {
                    performChanged();
                }
            }
        } );
        // add listener to listen to the event when "Deselect All" button is pressed
        _deselectAllButton.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                List< PreprocessorTag > input = getInput();
                boolean changed = false;
                for( PreprocessorTag tag : input ) {
                    if( tag.getScopeID() == _scope && tag.isActive() ) {
                        tag.setIsActive( false );
                        if( !changed ) {
                            changed = true;
                        }
                    }
                }
                _preprocessorTableViewer.setInput( input.toArray( new PreprocessorTag[ input.size() ] ) );
                if( changed ) {
                    performChanged();
                }
            }
        } );
        // show data
        showData();
    }

    private boolean isValidSelections( TableItem[] selections ) {
        if( selections == null ) {
            return false;
        }
        PreprocessorTag tag;
        for( int i = 0; i < selections.length; i++ ) {
            tag = (PreprocessorTag) selections[ i ].getData();
            if( tag.getScopeID() != _scope ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Display data.
     */
    public void showData() {
        List< PreprocessorTag > scopeDirectives = getDirectives();
        List< PreprocessorTag > otherDirectives = getUnChangableDirectives();
        List< PreprocessorTag > allDefines = new ArrayList< PreprocessorTag >();
        allDefines.addAll( otherDirectives );
        allDefines.addAll( scopeDirectives );
        PreprocessorTag[] directiveArray = allDefines.toArray( new PreprocessorTag[ allDefines.size() ] );
        Arrays.sort( directiveArray, new ScopeComparator() );
        setInput( directiveArray );
    }

    protected void setInput( final PreprocessorTag[] input ) {
        Display.getDefault().asyncExec( new Runnable() {
            public void run() {
                _preprocessorTableViewer.setInput( input );
                updateButtons();
            }
        } );

    }

    private List< PreprocessorTag > getDirectives() {
        switch( _scope ) {
            case PreprocessorTag.WS_SCOPE: {
                return getWorkspaceDirectives();
            }
            case PreprocessorTag.PJ_SCOPE: {
                if( _bbProject != null ) {
                    return Arrays.asList( _bbProject.getProperties()._compile.getPreprocessorDefines() );
                } else {
                    return new ArrayList< PreprocessorTag >( 0 );
                }
            }
            default: {
                return new ArrayList< PreprocessorTag >( 0 );
            }
        }
    }

    protected List< PreprocessorTag > getUnChangableDirectives() {
        ArrayList< PreprocessorTag > unChangableDirectives = new ArrayList< PreprocessorTag >();
        unChangableDirectives.addAll( getJREWorkspaceDirectives() );
        if( _scope == PreprocessorTag.PJ_SCOPE ) {
            unChangableDirectives.addAll( getWorkspaceDirectives() );
        }
        return unChangableDirectives;
    }

    protected List< PreprocessorTag > getJREWorkspaceDirectives() {
        ArrayList< PreprocessorTag > JREDirectives = new ArrayList< PreprocessorTag >();
        List< String > JREDirectiveStrings = VMUtils.getAllJREDirectives();
        String currentCPDefine = IConstants.EMPTY_STRING;
        switch( _scope ) {
            case PreprocessorTag.PJ_SCOPE: {
                if( _bbProject != null ) {
                    try {
                        currentCPDefine = VMUtils.getJREDirective( _bbProject );
                    } catch( Exception e ) {
                        // the jre used by the project may be removed
                        log.error( e.getMessage() );
                    }
                }
                break;
            }
            case PreprocessorTag.WS_SCOPE: {
                IVMInstall defaultVm = JavaRuntime.getDefaultVMInstall();
                if( BlackBerryVMInstallType.VM_ID.equals( defaultVm.getVMInstallType().getId() ) ) {
                    currentCPDefine = VMUtils.getJREDirective( (BlackBerrySDKInstall) defaultVm );
                }
                break;
            }
        }

        for( String define : JREDirectiveStrings ) {
            JREDirectives.add( new PreprocessorTag( define, define.equals( currentCPDefine ), PreprocessorTag.SDK_SCOPE ) );
        }
        return JREDirectives;
    }

    protected List< PreprocessorTag > getWorkspaceDirectives() {
        List< PreprocessorTag > workspaceDefines = PreprocessorPreferences.getPreprocessDefines();
        for( PreprocessorTag ppDefine : workspaceDefines ) {
            ppDefine.setScopeID( PreprocessorTag.WS_SCOPE );
        }
        return workspaceDefines;
    }

    protected void disableNonAddButtons() {
        if( isTableEmpty( _fTable ) ) {
            _removeButton.setEnabled( false );
            _editButton.setEnabled( false );
            _selectAllButton.setEnabled( false );
            _deselectAllButton.setEnabled( false );
        }
    }

    protected List< String > getDefineList() {
        List< String > defines = new ArrayList< String >();
        for( TableItem itm : _fTable.getItems() ) {
            defines.add( itm.getText( 1 ) );
        }
        return defines;
    }

    private void enableNonAddButtons() {
        if( !isTableEmpty( _fTable ) ) {
            _selectAllButton.setEnabled( true );
            _deselectAllButton.setEnabled( true );
        }
    }

    protected boolean isTableEmpty( Table table ) {
        if( table == null )
            return false;

        return ( table.getItems().length == 0 );
    }

    protected int getIndexof( PreprocessorTag tag ) {
        List< PreprocessorTag > tags = getInput();
        return tags.indexOf( tag );
    }

    /**
     * Gets the preprocess directives of the scope of this UI.
     *
     * @return
     */
    public List< PreprocessorTag > getScopeDirectives() {
        List< PreprocessorTag > tagList = new ArrayList< PreprocessorTag >();
        Object input = _preprocessorTableViewer.getInput();
        if( input instanceof PreprocessorTag[] ) {
            PreprocessorTag[] tags = (PreprocessorTag[]) input;
            for( int i = 0; i < tags.length; i++ ) {
                if( tags[ i ].getScopeID() == _scope ) {
                    tagList.add( tags[ i ] );
                }
            }
            return tagList;
        } else {
            return new ArrayList< PreprocessorTag >( 0 );
        }
    }

    private void updateButtons() {
        List< PreprocessorTag > tagList = getScopeDirectives();
        if( tagList.size() == 0 ) {
            _removeButton.setEnabled( false );
            _editButton.setEnabled( false );
            _selectAllButton.setEnabled( false );
            _deselectAllButton.setEnabled( false );
        } else {
            _removeButton.setEnabled( true );
            _editButton.setEnabled( true );
            _selectAllButton.setEnabled( true );
            _deselectAllButton.setEnabled( true );
        }
        TableItem[] selections = _fTable.getSelection();
        if( isValidSelections( selections ) ) {
            _editButton.setEnabled( _fTable.getSelectionCount() == 1 );
            _removeButton.setEnabled( _fTable.getSelectionCount() >= 1 );
        } else {
            _editButton.setEnabled( false );
            _removeButton.setEnabled( false );
        }
    }

    /**
     * Set a BlackBerry project.
     *
     * @param bbProject
     */
    public void setProject( BlackBerryProject bbProject ) {
        _bbProject = bbProject;
    }

    public List< PreprocessorTag > getInput() {
        List< PreprocessorTag > tagList = new ArrayList< PreprocessorTag >();
        Object input = _preprocessorTableViewer.getInput();
        if( input instanceof PreprocessorTag[] ) {
            PreprocessorTag[] tags = (PreprocessorTag[]) input;
            for( int i = 0; i < tags.length; i++ ) {
                tagList.add( tags[ i ] );
            }
            return tagList;
        } else {
            return new ArrayList< PreprocessorTag >( 0 );
        }
    }

    public void removeAllDirectives() {
        _fTable.removeAll();
    }

    @Override
    public void propertyChange( PropertyChangeEvent event ) {
        if( event.getProperty().equalsIgnoreCase( PreferenceConstants.PREPROCESSOR_DEFINE_LIST )
                && _scope != PreprocessorTag.WS_SCOPE ) {
            showData();
        }
    }

    abstract protected void performDefaults();

    abstract protected void performChanged();

    abstract protected void addListener();

    abstract protected void removeListener();

    /**
     * The label provider used for the PreprocessorTag table within the UI
     *
     */
    class PreprocessorTagLabelProvider extends LabelProvider implements ITableLabelProvider {

        @Override
        public Image getColumnImage( Object element, int columnIndex ) {
            return null;
        }

        @Override
        public String getColumnText( Object element, int columnIndex ) {
            if( ( null != element ) && PreprocessorTag.class.equals( element.getClass() ) && ( columnIndex >= 0 ) ) {
                PreprocessorTag tag = (PreprocessorTag) element;
                switch( columnIndex ) {
                    case 0: {
                        return ""; //$NON-NLS-1$
                    }

                    case 1:
                        return tag.getPreprocessorDefine();
                    case 2: {
                        if( tag.getScopeID() > _scope ) {
                            int index = getIndexof( (PreprocessorTag) element );
                            if( index >= 0 ) {
                                TableItem item = _preprocessorTableViewer.getTable().getItem( index );
                                if( item != null ) {
                                    item.setForeground( Display.getCurrent().getSystemColor( SWT.COLOR_GRAY ) );
                                }
                            }
                        }
                        return PreprocessorTag.getScope( tag.getScopeID() );
                    }

                }
            }
            return ""; //$NON-NLS-1$
        }

    }

    /**
     * The content provider used for the PreprocessorTags table within the UI
     *
     *
     */
    class PreprocessorTagContentProvider implements IStructuredContentProvider {
        public Object[] getElements( Object inputElement ) {
            if( inputElement instanceof PreprocessorTag[] ) {
                PreprocessorTag input[] = (PreprocessorTag[]) inputElement;
                return input;
            }
            return new Object[ 0 ];
        }

        public void dispose() {
        }

        @Override
        public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
        }
    }

    /**
     * The content provider used for the PreprocessorTags table within the UI
     *
     *
     */
    class PreprocessorTagCheckStateProvider implements ICheckStateProvider {

        @Override
        public boolean isChecked( Object element ) {
            boolean result = false;
            if( element instanceof PreprocessorTag ) {
                result = ( (PreprocessorTag) element ).isActive().booleanValue();
            }
            return result;
        }

        @Override
        public boolean isGrayed( Object element ) {
            return false;
        }

    }

    class ScopeComparator implements Comparator< PreprocessorTag > {

        @Override
        public int compare( PreprocessorTag o1, PreprocessorTag o2 ) {
            if( o1.getScopeID() > o2.getScopeID() ) {
                return -1;
            } else if( o1.getScopeID() < o2.getScopeID() ) {
                return 1;
            } else {
                return o1.getPreprocessorDefine().compareToIgnoreCase( o2.getPreprocessorDefine() );
            }
        }

    }

    class DirectiveString implements Comparator< PreprocessorTag > {

        @Override
        public int compare( PreprocessorTag o1, PreprocessorTag o2 ) {
            return o1.getPreprocessorDefine().compareToIgnoreCase( o2.getPreprocessorDefine() );
        }

    }

    @Override
    public void defaultVMInstallChanged( IVMInstall previous, IVMInstall current ) {
        showData();
    }

    @Override
    public void vmAdded( IVMInstall vm ) {
        showData();
    }

    @Override
    public void vmChanged( org.eclipse.jdt.launching.PropertyChangeEvent event ) {
        showData();

    }

    @Override
    public void vmRemoved( IVMInstall vm ) {
        showData();
    }
}
