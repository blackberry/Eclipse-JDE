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

import net.rim.ejde.internal.core.ContextManager;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * This class represents a preference page that is contributed to the Preferences dialog. By subclassing
 * <samp>FieldEditorPreferencePage</samp>, we can use the field support built into JFace that allows us to create a page that is
 * small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the preference store that belongs to the main plug-in class.
 * That way, preferences can be accessed directly via the preference store.
 */

public class BasicPrefsPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    /**
     * Default constructor
     */
    public BasicPrefsPage() {
        super( GRID );
        setPreferenceStore( ContextManager.getDefault().getPreferenceStore() );
    }

    @Override
    protected void createFieldEditors() {
        // do nothing
    }

    public void init( IWorkbench workbench ) {
        // do nothing
    }

    /**
     * Sets the layout of the given <code>button</code>.
     *
     * @param button
     *            The button
     * @param verticalIndent
     *            The vertical indent
     */
    public static void setDialogConfirmButtonLayoutData( Button button, int verticalIndent ) {
        GridData data = new GridData( GridData.HORIZONTAL_ALIGN_FILL );
        Point minSize = button.computeSize( SWT.DEFAULT, SWT.DEFAULT, true );
        data.widthHint = Math.max( IDialogConstants.BUTTON_WIDTH + 10, minSize.x );
        data.verticalIndent = verticalIndent;
        button.setLayoutData( data );
    }

    /**
     * Create a GridLayout based on given arguments
     *
     * @param numColumns
     *            The number of colulmns
     * @param marginTop
     *            The top margin
     * @param marginBottom
     *            The bottom margin
     * @param marginLeft
     *            The left margin
     * @param marginRight
     *            The right margin
     *
     * @return gridLayout The GridLayout created
     */
    public static GridLayout getGridLayout( int numColumns, int marginTop, int marginBottom, int marginLeft, int marginRight ) {
        GridLayout gridLayout = new GridLayout();
        gridLayout.marginTop = marginTop;
        gridLayout.marginBottom = marginBottom;
        gridLayout.marginLeft = marginLeft;
        gridLayout.marginRight = marginRight;
        gridLayout.numColumns = numColumns;
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        gridLayout.horizontalSpacing = 0;
        gridLayout.verticalSpacing = 0;
        return gridLayout;
    }
}
