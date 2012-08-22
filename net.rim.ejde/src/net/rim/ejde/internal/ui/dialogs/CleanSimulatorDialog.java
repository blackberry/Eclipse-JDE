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
package net.rim.ejde.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.ui.dialogs.CleanSimulatorTreeItem.ItemId;
import net.rim.ejde.internal.ui.dialogs.CleanSimulatorTreeItem.ItemType;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ejde.internal.util.VMUtils.VMVersionComparator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionStatusDialog;
import org.eclipse.ui.internal.MessageLine;

/**
 * CleanSimulatorDialog
 *
 * @author bkurz
 *
 */
public class CleanSimulatorDialog extends SelectionStatusDialog {
    private static String SDK_FIVE_VERSION = "5.0.0.0";

    private final int _treeViewerWidth = 78;
    private final int _treeViewerHeight = 18;

    private CheckboxTreeViewer _viewer;
    private ILabelProvider _labelProvider;
    private ILabelDecorator _labelDecorator;
    private ITreeContentProvider _contentProvider;
    private ICheckStateProvider _checkStateProvider;
    private Object _input;
    private IStatus _currentStatus;

    private String message = ""; //$NON-NLS-1$

    public CleanSimulatorDialog( Shell parent, ILabelProvider labelProvider, ILabelDecorator labelDecorator,
            ITreeContentProvider contentProvider, ICheckStateProvider checkStateProvider ) {
        super( parent );
        _labelProvider = labelProvider;
        _labelDecorator = labelDecorator;
        _contentProvider = contentProvider;
        _checkStateProvider = checkStateProvider;
        setResult( new ArrayList< CleanSimulatorTreeItem >( 0 ) );
        setStatusLineAboveButtons( true );
        setStatus( null );
    }

    /**
     * Sets the input for the tree viewer
     *
     * @param input
     */
    public void setInput( Object input ) {
        _input = input;
    }

    @Override
    protected Control createDialogArea( Composite parent ) {
        Composite composite = (Composite) super.createDialogArea( parent );
        GridData gridData = null;

        // Create message area and apply layout
        Label messageArea = createMessageArea( composite );
        gridData = new GridData( SWT.FILL, SWT.FILL, true, false );
        gridData.widthHint = convertWidthInCharsToPixels( _treeViewerWidth );
        messageArea.setLayoutData( gridData );

        // Create tree viewer and apply layout
        CheckboxTreeViewer treeViewer = createTreeViewer( composite );
        gridData = new GridData( SWT.FILL, SWT.FILL, true, true );
        gridData.widthHint = convertWidthInCharsToPixels( _treeViewerWidth );
        gridData.heightHint = convertHeightInCharsToPixels( _treeViewerHeight );

        Tree tree = treeViewer.getTree();
        tree.setLayoutData( gridData );
        tree.setFont( parent.getFont() );

        createSelectionButtons( composite );

        return composite;
    }

    @Override
    protected Label createMessageArea( Composite composite ) {
        Label label = new Label( composite, SWT.WRAP );
        if( message != null ) {
            label.setText( message );
        }
        label.setFont( composite.getFont() );
        return label;
    }

    @Override
    protected String getMessage() {
        return message;
    }

    @Override
    public void setMessage( String message ) {
        this.message = message;
    }

    protected CheckboxTreeViewer createTreeViewer( Composite parent ) {
        _viewer = new CheckboxTreeViewer( parent, SWT.BORDER );
        _viewer.setLabelProvider( new DecoratingLabelProvider( _labelProvider, _labelDecorator ) );
        _viewer.setContentProvider( _contentProvider );
        _viewer.setCheckStateProvider( _checkStateProvider );
        _viewer.setInput( _input );
        _viewer.setAutoExpandLevel( 2 );
        _viewer.addCheckStateListener( new ICheckStateListener() {
            @Override
            public void checkStateChanged( CheckStateChangedEvent event ) {
                boolean isChecked = event.getChecked();
                CleanSimulatorTreeItem item = (CleanSimulatorTreeItem) event.getElement();
                if( !item.isEnabled() ) {
                    // Sets enable state for SDKs with a running simulator
                    CheckboxTreeViewer source = (CheckboxTreeViewer) event.getSource();
                    source.setChecked( item, !isChecked );
                } else {
                    // Set check state for all items
                    item.setChecked( isChecked );
                    if( item.hasChildItems() ) {
                        item.setAllChildrenChecked( isChecked );
                    }

                    // Check for older SDKs
                    boolean isOlderSDK = false;
                    if( item.getItemType().equals( ItemType.INTERNAL_BUNDLE )
                            && item.getRootItem().getChildItem( ItemId.CLEAN_SIMULATOR_DIRECTORY ).isChecked() ) {
                        isOlderSDK = isOlderSDK( item );
                    }

                    // Set check state for older SDKs
                    if( item.getItemID().equals( ItemId.CLEAN_SIMULATOR_DIRECTORY )
                            && item.getItemType().equals( ItemType.INTERNAL_BUNDLE ) ) {
                        if( isOlderSDK && isChecked ) {
                            item.getParentItem().getChildItem( ItemId.ERASE_FILE_SYSTEM ).setChecked( isChecked );
                        }
                    }

                    if( item.hasParentItem() ) {
                        CleanSimulatorTreeItem parentItem = item.getParentItem();
                        boolean isSomeChildChecked = parentItem.isSomeChildChecked();
                        item.setAllParentChecked( isChecked ? isChecked : isSomeChildChecked );
                    }
                }

                updateViewer();
                setStatus( null );
                setWarningOlderSDK();
            }
        } );
        _viewer.expandToLevel( 1 );
        _viewer.setExpandedElements( _viewer.getCheckedElements() );

        updateViewer();
        setStatus( null );

        setWarningOlderSDK();
        return _viewer;
    }

    @SuppressWarnings("unchecked")
    private void updateViewer() {
        List< CleanSimulatorTreeItem > items = (List< CleanSimulatorTreeItem >) _viewer.getInput();
        List< CleanSimulatorTreeItem > checkedItems = new ArrayList< CleanSimulatorTreeItem >();
        List< CleanSimulatorTreeItem > grayedItems = new ArrayList< CleanSimulatorTreeItem >();

        for( CleanSimulatorTreeItem item : items ) {
            if( item.isEnabled() ) {
                grayedItems.addAll( CleanSimulatorTreeItem.getGrayedItems( item ) );
                checkedItems.addAll( CleanSimulatorTreeItem.getCheckedItems( item ) );
            }
        }
        _viewer.setCheckedElements( checkedItems.toArray() );
        _viewer.setGrayedElements( grayedItems.toArray() );
    }

    @SuppressWarnings("restriction")
    @Override
    protected Control createButtonBar( Composite parent ) {
        Composite composite = (Composite) super.createButtonBar( parent );

        for( Control c : composite.getChildren() ) {
            if( c instanceof MessageLine ) {
                GridData layoutData = new GridData( SWT.FILL, SWT.TOP, true, true );
                layoutData.horizontalSpan = 2;
                layoutData.verticalAlignment = SWT.TOP;
                layoutData.widthHint = 100;
                layoutData.heightHint = 30;
                c.setLayoutData( layoutData );
            }
        }
        return composite;
    }

    @Override
    protected void createButtonsForButtonBar( Composite parent ) {
        createButton( parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true );
        createButton( parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false );
    }

    protected void createSelectionButtons( Composite parent ) {
        Composite composite = new Composite( parent, SWT.RIGHT );

        GridLayout layout = new GridLayout();
        layout.numColumns = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = convertHorizontalDLUsToPixels( IDialogConstants.HORIZONTAL_SPACING );
        composite.setLayout( layout );

        GridData gridData = new GridData( SWT.RIGHT, SWT.FILL, true, false );
        composite.setLayoutData( gridData );

        Button selectAllButton = createButton( composite, IDialogConstants.SELECT_ALL_ID,
                Messages.CLEAN_SIMULATOR_DIALOG_SELECT_ALL_BUTTON, false );
        selectAllButton.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                Object[] items = _contentProvider.getElements( _input );
                selectAll( items, true );
            }
        } );

        Button deselectAllButton = createButton( composite, IDialogConstants.DESELECT_ALL_ID,
                Messages.CLEAN_SIMULATOR_DIALOG_DESELECT_ALL_BUTTON, false );
        deselectAllButton.addSelectionListener( new SelectionAdapter() {
            @Override
            public void widgetSelected( SelectionEvent e ) {
                Object[] items = _contentProvider.getElements( _input );
                selectAll( items, false );
            }
        } );
    }

    private void selectAll( Object[] items, boolean selected ) {
        CleanSimulatorTreeItem treeItem;
        for( Object item : items ) {
            treeItem = (CleanSimulatorTreeItem) item;

            if( treeItem.isEnabled() ) {
                treeItem.getRootItem().setAllChildrenChecked( selected );
            }
        }
        updateViewer();
        setStatus( null );
        setWarningOlderSDK();
    }

    @Override
    protected void computeResult() {
        _viewer.expandAll();
        setResult( Arrays.asList( _viewer.getCheckedElements() ) );
    }

    @Override
    protected void cancelPressed() {
        setResult( null );
        super.cancelPressed();
    }

    private void setStatus( Status status ) {
        if( status == null ) {
            if( _viewer != null && _viewer.getCheckedElements().length > 0 ) {
                _currentStatus = new Status( IStatus.OK, PlatformUI.PLUGIN_ID, 0, IConstants.EMPTY_STRING, null );
            } else {
                _currentStatus = new Status( IStatus.ERROR, PlatformUI.PLUGIN_ID, 0,
                        Messages.CLEAN_SIMULATOR_DIALOG_ERROR_NO_SELECTION, null );
            }
        } else {
            _currentStatus = status;
        }
        updateStatus( _currentStatus );
        updateButtonsEnableState( _currentStatus );
    }

    private void setWarningOlderSDK() {
        Object[] items = _contentProvider.getElements( _input );
        for( Object item : items ) {
            CleanSimulatorTreeItem treeItem = (CleanSimulatorTreeItem) item;
            if( treeItem.getItemType().equals( ItemType.INTERNAL_BUNDLE ) && isOlderSDK( treeItem ) ) {
                if( treeItem.getChildItem( ItemId.CLEAN_SIMULATOR_DIRECTORY ).isChecked()
                        && !treeItem.getChildItem( ItemId.ERASE_FILE_SYSTEM ).isChecked() ) {
                    setStatus( new Status( IStatus.WARNING, PlatformUI.PLUGIN_ID, 0,
                            Messages.CLEAN_SIMULATOR_DIALOG_WARNING_OLDER_SDK, null ) );
                }
                break;
            }
        }
    }

    private boolean isOlderSDK( CleanSimulatorTreeItem item ) {
        CleanSimulatorTreeItemInternal itemInternalBundle = (CleanSimulatorTreeItemInternal) item;
        VMVersionComparator comparator = new VMVersionComparator();
        int result = comparator.compare( VMUtils.getVMVersion( itemInternalBundle.getVMInstall() ), SDK_FIVE_VERSION );
        return result == -1 ? true : false;
    }
}
