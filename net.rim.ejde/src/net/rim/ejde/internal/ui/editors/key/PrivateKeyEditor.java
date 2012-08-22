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
package net.rim.ejde.internal.ui.editors.key;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.ui.CompositeFactory;
import net.rim.ejde.internal.ui.editors.key.CodeSigningState.OnePackage;
import net.rim.ejde.internal.ui.editors.key.CodeSigningState.TreeNode;
import net.rim.ejde.internal.ui.editors.key.CodeSigningState.TreeNodeAndCheckBox;
import net.rim.ejde.internal.util.Messages;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

/**
 * An instance of this class is associated with a *.key file to select which packages and class in the current project are
 * protected by this key file.
 *
 * For the internal fragment, this class is replaced entirely with a new class that removes access to this editor. Internal users
 * are directed to the new editor implmentation.
 */
@InternalFragmentReplaceable
public class PrivateKeyEditor extends EditorPart implements ICheckStateListener, IConstants {
    private static final Logger _logger = Logger.getLogger( PrivateKeyEditor.class );
    private CheckboxTreeViewer _treeViewer;
    private CheckTreeItem _rootTreeItem;
    private CodeSigningState _codeSigningState;
    private String _inputFileName;
    private boolean _isPageModified;
    private IProject _project;
    private BlackBerryProperties _bbProperties;
    private Combo _keyCombo;

    /**
     * Creates the UI of this editor.
     *
     * @param node
     *            The key file.
     * @param parent
     *            The parent composite
     * @return
     */
    private void createUI( IFile node, Composite parent ) {
        // parse the project to get code signing information
        try {
            parseForPackages( node );
        } catch( CoreException e ) {
            _logger.error( e );
            return;
        }
        Composite composite = CompositeFactory.gridComposite( parent, 1 );
        composite.setLayoutData( GridData.FILL_BOTH );
        Vector< String > keys = _codeSigningState.getKeys();
        if( !keys.contains( _inputFileName ) ) {
            MessageDialog.openError( this.getSite().getShell(), Messages.PrivateKeyEditor_MESSAGE_DIALOG_TITLE,
                    NLS.bind( Messages.PrivateKeyEditor_UNEXPECTED_KEY_FILE_MESSAGE, _inputFileName ) );
            return;
        }
        // create the combo for key files
        _keyCombo = new Combo( composite, SWT.READ_ONLY );
        _keyCombo.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        String[] items = new String[ keys.size() ];
        _keyCombo.setItems( keys.toArray( items ) );
        _keyCombo.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                doSaveInMemory();
                // update the singing information when the selection of key file
                // is changed
                _codeSigningState.setKey( _keyCombo.getItem( _keyCombo.getSelectionIndex() ).toString() );
                _treeViewer.setInput( _rootTreeItem );
                checkSelectionStatus( _treeViewer.getTree() );
            }

        } );
        _keyCombo.select( keys.indexOf( _inputFileName ) );
        // Combo.select() method does not invoke the SelectionEvent, we do it
        // implicitly here
        _codeSigningState.setKey( _keyCombo.getItem( _keyCombo.getSelectionIndex() ).toString() );

        // code signing function button area
        Composite buttonComp = CompositeFactory.gridCompositeWithBorder( composite, 4 );
        buttonComp.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        Button selectAllPackages = new Button( buttonComp, SWT.PUSH );
        selectAllPackages.setText( Messages.PrivateKeyEditor_PROTECT_ALL_PACKAGES_TITLE );
        selectAllPackages.setToolTipText( Messages.PrivateKeyEditor_PROTECT_ALL_PACKAGES_TITLE_TOOLTIP );
        Button selectAllClasses = new Button( buttonComp, SWT.PUSH );
        selectAllClasses.setText( Messages.PrivateKeyEditor_PROTECT_ALL_CLASSES_TITLE );
        selectAllClasses.setToolTipText( Messages.PrivateKeyEditor_PROTECT_ALL_CLASSES_TITLE_TOOLTIP );
        Button clear = new Button( buttonComp, SWT.PUSH );
        clear.setText( Messages.PrivateKeyEditor_CLEAR_PROTECT_TITLE );
        clear.setToolTipText( Messages.PrivateKeyEditor_CLEAR_PROTECT_TITLE_TOOLTIP );
        Button clearAll = new Button( buttonComp, SWT.PUSH );
        clearAll.setText( Messages.PrivateKeyEditor_CLEARALL_PROTECT_TITLE );
        clearAll.setToolTipText( Messages.PrivateKeyEditor_CLEARALL_PROTECT_TITLE_TOOLTIP );

        selectAllPackages.addSelectionListener( new SelectionListener() {

            public void widgetDefaultSelected( SelectionEvent e ) {
                // nothing to do

            }

            public void widgetSelected( SelectionEvent e ) {
                _codeSigningState.checkEnabledPackages();
                if( _treeViewer != null )
                    checkSelectionStatus( _treeViewer.getTree() );
                handleChange();
            }

        } );

        selectAllClasses.addSelectionListener( new SelectionListener() {

            public void widgetDefaultSelected( SelectionEvent e ) {
                // nothing to do

            }

            public void widgetSelected( SelectionEvent e ) {
                _codeSigningState.checkEnabledClasses();
                if( _treeViewer != null )
                    checkSelectionStatus( _treeViewer.getTree() );
                handleChange();
            }

        } );

        clear.addSelectionListener( new SelectionListener() {

            public void widgetDefaultSelected( SelectionEvent e ) {
                // nothing to do

            }

            public void widgetSelected( SelectionEvent e ) {
                _codeSigningState.clearEnabledPackagesAndClasses();
                if( _treeViewer != null )
                    checkSelectionStatus( _treeViewer.getTree() );
                handleChange();
            }

        } );

        clearAll.addSelectionListener( new SelectionListener() {

            public void widgetDefaultSelected( SelectionEvent e ) {
                // nothing to do

            }

            public void widgetSelected( SelectionEvent e ) {
                MessageBox messageBox = new MessageBox( getSite().getShell(), SWT.YES | SWT.NO );
                messageBox.setMessage( Messages.PrivateKeyEditor_CLEARALL_PROTECT_WARNING_MESSAGE );
                if( messageBox.open() == SWT.NO )
                    return;
                _codeSigningState.clearAllPackagesAndClasses();
                if( _treeViewer != null ) {
                    checkSelectionStatus( _treeViewer.getTree() );
                    _treeViewer.refresh();
                }
                handleChange();
            }

        } );

        _treeViewer = new CheckboxTreeViewer( composite, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER );
        _treeViewer.addCheckStateListener( this );
        _treeViewer.getTree().setLayoutData( new GridData( GridData.FILL_BOTH ) );
        _treeViewer.addCheckStateListener( new ICheckStateListener() {

            public void checkStateChanged( CheckStateChangedEvent event ) {
                CheckTreeItem item = (CheckTreeItem) event.getElement();
                if( !item.isEnabled() ) {
                    // if the element is disabled, we change the check status back
                    // just mimic it is not changed
                    _treeViewer.setChecked( event.getElement(), !event.getChecked() );
                    item.setSelected( !event.getChecked() );
                }
            }

        } );
        _treeViewer.getTree().addTreeListener( new TreeListener() {

            public void treeCollapsed( TreeEvent e ) {
                // nothing to do

            }

            public void treeExpanded( TreeEvent e ) {
                // usually the tree is lazily displayed, each time only one
                // layer is displayed. We need to mark the selection when we
                // expand a tree item
                checkSelectionStatus( e.item );
            }

        } );
        _rootTreeItem = (CheckTreeItem) _codeSigningState.getTree();
        _treeViewer.setContentProvider( new MyContentProvider() );
        _treeViewer.setLabelProvider( new MyLabelProvider() );
        _treeViewer.setInput( _rootTreeItem );
        checkSelectionStatus( _treeViewer.getTree() );
    }

    /**
     * Checks the selection statuses of all children of <code>item</code> and sets the check statuses of corresponding check-boxes
     * in the tree viewer.
     *
     * @param item
     */
    private void checkSelectionStatus( Object obj ) {
        TreeItem[] children;
        if( obj instanceof Tree )
            children = ( (Tree) obj ).getItems();
        else if( obj instanceof TreeItem )
            children = ( (TreeItem) obj ).getItems();
        else
            return;
        for( int i = 0; i < children.length; i++ ) {
            CheckTreeItem item = (CheckTreeItem) children[ i ].getData();
            if( item != null ) {
                children[ i ].setChecked( item.isSelected() );
                _treeViewer.setGrayed( item, !item.isEnabled() );
                if( !item.isEnabled() ) {
                    // children [ i ].setBackground( Display.getCurrent().getSystemColor( SWT.COLOR_GRAY ) );
                    children[ i ].setForeground( Display.getCurrent().getSystemColor( SWT.COLOR_DARK_GRAY ) );
                } else {
                    // children [ i ].setBackground( Display.getCurrent().getSystemColor( SWT.COLOR_WHITE ) );
                    children[ i ].setForeground( Display.getCurrent().getSystemColor( SWT.COLOR_BLACK ) );
                }
                checkSelectionStatus( children[ i ] );
            }
        }
    }

    /**
     * Parses the project to which <code>node</code> (usually a key file) belongs and retrieves code signing information.
     *
     * @param node
     * @return Code signing information related to the <code>node</code>.
     * @throws CoreException
     */
    private List< OnePackage > parseForPackages( final IFile node ) throws CoreException {

        class ParseAll implements CodeSigningState.Callback {
            List< OnePackage > _pakkages = new Vector< OnePackage >();

            public TreeNodeAndCheckBox newTreeNodeAndCheckBox( TreeNode parent ) {
                CheckTreeItem newItem = new CheckTreeItem( (CheckTreeItem) parent );
                return new TreeNodeAndCheckBox( newItem, newItem );
            }

            public void nodeChanged( CodeSigningState.TreeNode node ) {
                // do nothing
            }

            public void setCurrentlyParsing( String name ) {
                // TODO may use this method when we add progress monitor into
                // the program.
            }

            public void initialize() throws CoreException {
                _pakkages = _codeSigningState.initialize( _project, _bbProperties, node, this );
            }

            public List< OnePackage > getPackages() {
                return _pakkages;
            }
        }

        ParseAll parseAll = new ParseAll();
        parseAll.initialize();
        List< OnePackage > pakkages = parseAll.getPackages();
        return pakkages;
    }

    /**
     * Saves the page if it is dirty.
     */
    public void doSave( IProgressMonitor monitor ) {
        if( _isPageModified ) {
            _isPageModified = false;
            doSaveInMemory();
            if( _project != null ) {
                ContextManager.PLUGIN.setBBProperties( _project.getName(), _bbProperties, true );
            }
            firePropertyChange( IEditorPart.PROP_DIRTY );
        }
    }

    private void doSaveInMemory() {
        _codeSigningState.updateCheck( _rootTreeItem );
        _codeSigningState.ok();
    }

    public void doSaveAs() {
        // do nothing

    }

    /**
     * Initializes this editor.
     *
     * @param site
     *            The IEditorSite of this editor.
     * @param input
     *            The IEditorInput of this editor.
     */
    public void init( IEditorSite site, IEditorInput input ) throws PartInitException {
        if( !( input instanceof IFileEditorInput ) )
            throw new PartInitException( Messages.PrivateKeyEditor_INVALID_EDITOR_INPUT_MESSAGE );
        setSite( site );
        setInput( input );
        _codeSigningState = new CodeSigningState();
    }

    /**
     * Handles a property change notification from a nested editor. In our case, the <code>_isPageModified</code> field is
     * adjusted as appropriate and superclass is called to notify listeners of the change.
     */
    protected void handlePropertyChange( int propertyId ) {
        if( propertyId == IEditorPart.PROP_DIRTY ) {
            _isPageModified = isDirty();
        }
        firePropertyChange( propertyId );
    }

    /**
     * Checks if the content is modified.
     *
     * @return <code>true</code> if the content is modified, <code>false</code> otherwise.
     */
    public boolean isDirty() {
        return _isPageModified;
    }

    public boolean isSaveAsAllowed() {
        return false;
    }

    /**
     * Gets the selected workspace file (key file) and creates the UI of the editor of it.
     */
    public void createPartControl( Composite parent ) {
        IEditorInput input = getEditorInput();
        IFile file = (IFile) input.getAdapter( IFile.class );
        if( file == null ) {
            throw new IllegalArgumentException( "Could not load selected file" );
        }
        _project = file.getProject();
        _bbProperties = ContextManager.PLUGIN.getBBProperties( _project.getName(), false );
        // key file name must be relative to workspace
        _inputFileName = file.getProjectRelativePath().toOSString();
        updateTitle();
        createUI( file, parent );
    }

    public void setFocus() {
        // nothing to do
    }

    /**
     * Implementation of {@link ICheckStateListener#checkStateChanged(CheckStateChangedEvent)}. When a TreeItem is
     * checked/unchecked, the <code>selected</code> attribute of corresponding <code>CheckTreeItem</code> need to be updated.
     *
     * @param event
     */
    public void checkStateChanged( CheckStateChangedEvent event ) {
        if( !( event.getElement() instanceof CheckTreeItem ) ) {
            return;
        }
        CheckTreeItem checkTreeItem = (CheckTreeItem) event.getElement();
        if( !checkTreeItem.isEnabled() ) {
            return;
        }
        checkTreeItem.setSelected( event.getChecked() );
        _codeSigningState.updateCheck( checkTreeItem );
        _treeViewer.refresh();
        handleChange();
    }

    /**
     * Update the editor's title based upon the content being edited.
     */
    private void updateTitle() {
        IEditorInput input = getEditorInput();
        IFile newFile = (IFile) input.getAdapter( IFile.class );
        setPartName( newFile.getName() );
        setTitleToolTip( input.getToolTipText() );
    }

    private void handleChange() {
        if( !_isPageModified ) {
            _isPageModified = true;
            firePropertyChange( IEditorPart.PROP_DIRTY );
        }
    }

    // ------ Inner classes ------
    /**
     * An instance of this class presents an element of the tree item in the checkbox tree viewer.
     */
    private class CheckTreeItem implements CodeSigningState.CheckBox, CodeSigningState.TreeNode {
        private boolean _checked;
        private boolean _enabled;
        private Object _text;
        private CheckTreeItem _parent;
        private Vector< CheckTreeItem > _childrenVec;

        /**
         * Constructs an instance of CheckTreeItem.
         *
         * @param parent
         */
        public CheckTreeItem( CheckTreeItem parent ) {
            _parent = parent;
            _childrenVec = new Vector< CheckTreeItem >();
        }

        /**
         * Gets the children of this item.
         *
         * @return the children of this item.
         */
        public Vector< CheckTreeItem > getChildren() {
            return _childrenVec;
        }

        /**
         * Gets the parent of this item.
         *
         * @return the parent of this item.
         */
        public CheckTreeItem getParent() {
            return _parent;
        }

        // ------ Methods in CodeSigningState.CheckBox ------
        public boolean isEnabled() {
            return _enabled;
        }

        public boolean isSelected() {
            return _checked;
        }

        public void setEnabled( boolean on ) {
            _enabled = on;
        }

        public void setSelected( boolean on ) {
            _checked = on;
        }

        public String getText() {
            return _text.toString();
        }

        public void setText( String text ) {
            _text = text;

        }

        // ------ Methods in CodeSigningState.TreeNode ------
        public void addChild( TreeNode child ) {
            _childrenVec.add( (CheckTreeItem) child );
        }

        public void setText( Object o ) {
            _text = o;
        }
    }

    /**
     * The content provider of the tree viewer.
     */
    class MyContentProvider implements ITreeContentProvider {

        public Object[] getChildren( Object parentElement ) {
            return ( (CheckTreeItem) parentElement ).getChildren().toArray();
        }

        public Object getParent( Object element ) {
            return ( (CheckTreeItem) element ).getParent();
        }

        public boolean hasChildren( Object element ) {
            return ( (CheckTreeItem) element ).getChildren().size() == 0 ? false : true;
        }

        public Object[] getElements( Object inputElement ) {
            return ( (CheckTreeItem) inputElement ).getChildren().toArray();
        }

        public void dispose() {
            // nothing to do

        }

        public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
            // nothing to do

        }
    }

    /**
     * The label provider of the tree viewer.
     */
    class MyLabelProvider implements ILabelProvider {

        public Image getImage( Object element ) {
            return null;
        }

        public String getText( Object element ) {
            return ( (CheckTreeItem) element ).getText();
        }

        public void addListener( ILabelProviderListener listener ) {
            // nothing to do.
        }

        public void dispose() {
            // nothing to do

        }

        public boolean isLabelProperty( Object element, String property ) {
            return false;
        }

        public void removeListener( ILabelProviderListener listener ) {
            // nothing to do.
        }

    }

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

    public void switchKey( String newKey ) {
        if( !newKey.equals( _codeSigningState.getKey() ) ) {
            Vector< String > keys = _codeSigningState.getKeys();
            int index = keys.indexOf( newKey );
            if( index != -1 ) {
                doSaveInMemory();
                _keyCombo.select( index );
                _codeSigningState.setKey( newKey );
                _treeViewer.setInput( _rootTreeItem );
                checkSelectionStatus( _treeViewer.getTree() );
            }
        }
    }
}
