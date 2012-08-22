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
package net.rim.ejde.internal.ui.perspective;

import org.eclipse.jdt.internal.ui.JavaPerspectiveFactory;
import org.eclipse.ui.IPageLayout;

/**
 * BlackBerry application development perspective.
 *
 * @author dmeng, jkeshavarzi
 *
 */
public class BlackBerryPerspectiveFactory extends JavaPerspectiveFactory {

    // While extending the JavaPerspectiveFactory guarantees we add the Actions,
    // Views, and shortcuts directly associated with the Java Perspective, it
    // does not add the various contributions made from other perspectives.
    // In the plugin.xml file, the org.eclipse.ui.perspectiveExtensions extension
    // will allow a perspective to contribute Actions, Views and shortcuts to another perspective.
    // One such example would be the below org.eclipse.debug.ui.breakpointActionSet
    // action set that i had to manually add below, because it is a contribution from
    // the debug perspective and not contained directly under the Java perspective.
    // This is one example of a contribution made to the Java Perspective that we have had
    // to mimic in the BB Perspective. To fully extend the Java Perspective, we will need
    // to create a mechanism for reading the contributions made to the Java Perspective
    // and mimic them in the BB Perspective. J.K

    public void createInitialLayout( IPageLayout layout ) {
        super.createInitialLayout( layout );

        // add shortcuts
        addShortcuts( layout );
    }

    private void addShortcuts( IPageLayout layout ) {

        // add new wizard shortcut
        layout.addNewWizardShortcut( "net.rim.ejde.internal.ui.wizards.BlackBerryProjectWizard" );
        layout.addNewWizardShortcut( "net.rim.ejde.internal.ui.wizards.NewResourceFileWizard" );
        layout.addNewWizardShortcut( "net.rim.ejde.internal.ui.wizards.NewScreenWizard" );

        // add perspective shortcuts
        layout.addPerspectiveShortcut( "org.eclipse.jdt.ui.JavaPerspective" );

        // Java Perspective contributions manually added below (see above comment)
        layout.addActionSet( "org.eclipse.debug.ui.breakpointActionSet" );

    }

}
