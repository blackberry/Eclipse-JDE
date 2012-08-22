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
package net.rim.ejde.internal.ui;

/*********************************************************************
 * CompositFactory.java
 *
 * Copyright (c) 2007 Research In Motion Inc.  All rights reserved.
 * This file contains confidential and proprietary information
 *
 * Creation date: May 3, 2007 3:44:07 PM
 *
 * File:          CompositFactory.java
 * Revision:      $Revision$
 * Checked in by: zqiu
 * Last modified: $DateTime$
 *
 *********************************************************************/

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

/**
 * This class is used to create different type of Composite instances.
 *
 *
 */
public class CompositeFactory {

    /**
     * Creates a Composite instance with GridLayout.
     *
     * @param parent
     * @param numColumn
     * @return
     */
    public static Composite gridComposite( Composite parent, int numColumn ) {
        return gridComposite( parent, numColumn, 0 );
    }

    /**
     * Creates a Composite instance with GridLayout.
     *
     * @param parent
     * @param numColumn
     * @param marginWidth
     * @return
     */
    public static Composite gridComposite( Composite parent, int numColumn, int marginWidth ) {
        return gridComposite( parent, numColumn, marginWidth, 0 );
    }

    /**
     * Creates a Composite instance with GridLayout.
     *
     * @param parent
     * @param numColum
     * @param marginWidth
     * @param marginHeight
     * @return
     */
    public static Composite gridComposite( Composite parent, int numColum, int marginWidth, int marginHeight ) {
        Composite composite = new Composite( parent, SWT.NONE );
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = marginWidth;
        layout.marginHeight = marginHeight;
        layout.numColumns = numColum;
        composite.setLayout( layout );
        composite.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        return composite;
    }

    /**
     * Creates a Composite instance with GridLayout and border.
     *
     * @param parent
     * @param numColum
     * @return
     */
    public static Composite gridCompositeWithBorder( Composite parent, int numColum ) {
        return gridCompositeWithBorder( parent, numColum, 0 );
    }

    /**
     * Creates a Composite instance with GridLayout and border.
     *
     * @param parent
     * @param numColum
     * @param marginWidth
     * @return
     */
    public static Composite gridCompositeWithBorder( Composite parent, int numColum, int marginWidth ) {
        Composite composite = new Composite( parent, SWT.BORDER );
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = marginWidth;
        layout.numColumns = numColum;
        composite.setLayout( layout );
        composite.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        return composite;
    }

    /**
     * Creates a Composite instance with FillLayout.
     *
     * @param parent
     * @return
     */
    public static Composite fillComposite( Composite parent ) {
        Composite composite = new Composite( parent, SWT.NONE );
        FillLayout layout = new FillLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout( layout );
        composite.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        return composite;
    }
}
