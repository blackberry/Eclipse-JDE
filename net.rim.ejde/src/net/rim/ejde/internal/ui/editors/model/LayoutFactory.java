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
package net.rim.ejde.internal.ui.editors.model;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 * This class provides several helper methods to create various layouts used within the editor.
 *
 * @author jkeshavarzi
 *
 */
public class LayoutFactory {
    public static TableWrapLayout createEditorPageLayout( boolean makeColumnsEqualWidth, int numColumns ) {
        TableWrapLayout layout = new TableWrapLayout();

        layout.topMargin = 12;
        layout.bottomMargin = 12;
        layout.leftMargin = 6;
        layout.rightMargin = 6;

        layout.horizontalSpacing = 20;
        layout.verticalSpacing = 17;

        layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
        layout.numColumns = numColumns;

        return layout;
    }

    public static TableWrapLayout createEditorCompositeLayout( boolean makeColumnsEqualWidth, int numColumns ) {
        TableWrapLayout layout = new TableWrapLayout();

        layout.topMargin = 0;
        layout.bottomMargin = 0;
        layout.leftMargin = 0;
        layout.rightMargin = 0;

        layout.horizontalSpacing = 20;
        layout.verticalSpacing = 17;

        layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
        layout.numColumns = numColumns;

        return layout;
    }

    /**
     * Creates the GridLayout used within many of the sections clients.
     *
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createSectionGridLayout( boolean makeColumnsEqualWidth, int numColumns ) {
        GridLayout layout = new GridLayout();

        layout.marginHeight = 0;
        layout.marginWidth = 0;

        layout.marginTop = 5;
        layout.marginBottom = 5;
        layout.marginLeft = 2;
        layout.marginRight = 2;

        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 5;

        layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
        layout.numColumns = numColumns;

        return layout;
    }

    /**
     * Creates a GridLayout with a value of 0 for all margins
     *
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createZeroMarginGridLayout( boolean makeColumnsEqualWidth, int numColumns ) {
        GridLayout layout = new GridLayout();

        layout.marginHeight = 0;
        layout.marginWidth = 0;

        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;

        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 5;

        layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
        layout.numColumns = numColumns;

        return layout;
    }

    /**
     * Creates a TableWrapLayout used in sections of the App Descriptor editor
     *
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static TableWrapLayout createSectionTableWrapLayout( boolean makeColumnsEqualWidth, int numColumns ) {
        TableWrapLayout layout = new TableWrapLayout();

        layout.topMargin = 2;
        layout.bottomMargin = 2;
        layout.leftMargin = 2;
        layout.rightMargin = 2;

        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;

        layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
        layout.numColumns = numColumns;

        return layout;
    }

    /**
     * Creates the TableWrapLayout used within the general section of the descriptor editor
     *
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static TableWrapLayout createSectionClientTableWrapLayout( boolean makeColumnsEqualWidth, int numColumns ) {
        TableWrapLayout layout = new TableWrapLayout();

        layout.topMargin = 5;
        layout.bottomMargin = 5;
        layout.leftMargin = 2;
        layout.rightMargin = 2;

        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 5;

        layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
        layout.numColumns = numColumns;

        return layout;
    }

    /**
     * Creates the GridLayout used for each page in the descriptor editor
     *
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createFormGridLayout( boolean makeColumnsEqualWidth, int numColumns ) {
        GridLayout layout = new GridLayout();

        layout.marginHeight = 0;
        layout.marginWidth = 0;

        layout.marginTop = 12;
        layout.marginBottom = 12;
        layout.marginLeft = 6;
        layout.marginRight = 6;

        layout.horizontalSpacing = 20;
        layout.verticalSpacing = 17;

        layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
        layout.numColumns = numColumns;

        return layout;
    }

    /**
     * Creates the Gridlayout used for the left/right panes within each descriptor editor page
     *
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createFormPaneGridLayout( boolean makeColumnsEqualWidth, int numColumns ) {
        GridLayout layout = new GridLayout();

        layout.marginHeight = 0;
        layout.marginWidth = 0;

        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;

        layout.horizontalSpacing = 20;
        layout.verticalSpacing = 17;

        layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
        layout.numColumns = numColumns;

        return layout;
    }

    /**
     * Creates the GridLayout used in each section of the descriptor editor
     *
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createClearGridLayout( boolean makeColumnsEqualWidth, int numColumns ) {
        GridLayout layout = new GridLayout();

        layout.marginHeight = 0;
        layout.marginWidth = 0;

        layout.marginTop = 2;
        layout.marginBottom = 2;
        layout.marginLeft = 2;
        layout.marginRight = 2;

        layout.horizontalSpacing = 0;
        layout.verticalSpacing = 0;

        layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
        layout.numColumns = numColumns;

        return layout;
    }

    /**
     * Creates the GridLayout used in the master section of the AEP page
     *
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createMasterGridLayout( boolean makeColumnsEqualWidth, int numColumns ) {
        GridLayout layout = new GridLayout();

        layout.marginHeight = 0;
        layout.marginWidth = 0;

        layout.marginTop = 0;
        layout.marginBottom = 0;
        layout.marginLeft = 0;

        int marginRight = 20;
        marginRight = marginRight / 2;
        marginRight--;

        layout.marginRight = marginRight;

        layout.horizontalSpacing = 20;
        layout.verticalSpacing = 17;

        layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
        layout.numColumns = numColumns;

        return layout;
    }

    /**
     * Creates the GridLayout used in the details section of the AEP page
     *
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createDetailsGridLayout( boolean makeColumnsEqualWidth, int numColumns ) {
        GridLayout layout = new GridLayout();

        layout.marginHeight = 0;
        layout.marginWidth = 0;

        layout.marginTop = 0;
        layout.marginBottom = 0;

        int marginLeft = 20;
        marginLeft = marginLeft / 2;
        marginLeft--;

        layout.marginLeft = marginLeft;
        layout.marginRight = 1;

        layout.horizontalSpacing = 20;
        layout.verticalSpacing = 17;

        layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
        layout.numColumns = numColumns;

        return layout;
    }

    /**
     * Creates a GridLayout used in the AEP page details section
     *
     * @param makeColumnsEqualWidth
     * @param numColumns
     * @return
     */
    public static GridLayout createSectionClientGridLayout( boolean makeColumnsEqualWidth, int numColumns ) {
        GridLayout layout = new GridLayout();

        layout.marginHeight = 0;
        layout.marginWidth = 0;

        layout.marginTop = 5;
        layout.marginBottom = 5;
        layout.marginLeft = 2;
        layout.marginRight = 2;

        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 5;

        layout.makeColumnsEqualWidth = makeColumnsEqualWidth;
        layout.numColumns = numColumns;

        return layout;
    }
}
