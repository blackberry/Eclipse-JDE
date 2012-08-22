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
package net.rim.ejde.internal.ui.launchers;

import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.ui.CompositeFactory;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * An instance of this class is used to editor workspace/project imports.
 */
public class SimpleVectorInputDialog extends Dialog implements IConstants {
    List< String > _oldListData;
    List< String > _newListData;
    Text _listEditor;

    /**
     * Constructs an instance of ImportsEditDialog.
     *
     * @param parentShell
     */
    public SimpleVectorInputDialog( Shell parentShell ) {
        super( parentShell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL );
    }

    /**
     * Constructs an instance of ImportsEditDialog.
     *
     * @param title
     * @param vector
     * @param parent
     */
    public SimpleVectorInputDialog( String title, List< String > list, Shell parent ) {
        this( parent );
        setText( title );
        _oldListData = list;
    }

    /**
     * Opens an imports edit dialog.
     *
     * @return Vector which contains imported jar fills.
     *
     */
    public List< String > open() {
        // Create the dialog window
        Shell shell = new Shell( getParent(), getStyle() );
        shell.setBounds( 400, 200, 300, 400 );
        shell.setText( getText() );
        createContents( shell );
        shell.open();
        Display display = getParent().getDisplay();
        while( !shell.isDisposed() ) {
            if( !display.readAndDispatch() ) {
                display.sleep();
            }
        }
        // Return the entered value
        return _newListData;
    }

    private void createContents( final Shell shell ) {
        shell.setLayout( new GridLayout( 1, false ) );
        Composite composite = CompositeFactory.gridComposite( shell, 1 );
        composite.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        _listEditor = new Text( composite, SWT.BORDER | SWT.V_SCROLL | SWT.LEFT | SWT.FILL_WINDING | SWT.MULTI );
        _listEditor.setLayoutData( new GridData( GridData.FILL_BOTH ) );
        Composite buttonComposite = CompositeFactory.gridComposite( composite, 2 );
        // Create the OK button and add a handler so that pressing it will
        // set input to the entered value
        Button okButton = new Button( buttonComposite, SWT.PUSH );
        okButton.setText( Messages.IConstants_OK_BUTTON_TITLE );
        setDialogConfirmButtonLayoutData( okButton );
        ( (GridData) okButton.getLayoutData() ).horizontalAlignment = SWT.END;
        okButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                _newListData = new Vector< String >();
                StringTokenizer tok = new StringTokenizer( _listEditor.getText(), CHANG_LINE_STRING, false );
                while( tok.hasMoreTokens() ) {
                    String s = tok.nextToken();
                    if( s.length() == 0 )
                        continue;
                    _newListData.add( s );
                }
                shell.close();
            }
        } );
        // Create the cancel button and add a handler
        // so that pressing it will set input to null
        Button cancelButton = new Button( buttonComposite, SWT.PUSH );
        cancelButton.setText( Messages.IConstants_CANCEL_BUTTON_TITLE );
        setDialogConfirmButtonLayoutData( cancelButton );
        ( (GridData) okButton.getLayoutData() ).horizontalAlignment = SWT.BEGINNING;
        cancelButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                _newListData = null;
                shell.close();
            }
        } );
        // display data
        displayData();
    }

    @SuppressWarnings("unqualified-field-access")//$NON-NLS-1$
    private void displayData() {
        if( _oldListData == null || _oldListData.size() == 0 )
            return;
        StringBuffer buff = new StringBuffer();
        for( String string : _oldListData ) {
            buff.append( string );
            buff.append( "\n" ); //$NON-NLS-1$
        }
        _listEditor.setText( buff.toString() );
    }

    /**
     * Sets the layout of the given <code>button</code>.
     *
     * @param button
     */
    public static void setDialogConfirmButtonLayoutData( Button button ) {
        GridData data = new GridData( GridData.HORIZONTAL_ALIGN_FILL );
        Point minSize = button.computeSize( SWT.DEFAULT, SWT.DEFAULT, true );
        data.widthHint = Math.max( IDialogConstants.BUTTON_WIDTH, minSize.x );
        button.setLayoutData( data );
    }

}
