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

import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;

import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;

/**
 * The second page of the New Java project wizard. It allows to configure the build path and output location. As addition to the
 * {@link JavaCapabilityConfigurationPage}, the wizard page does an early project creation (so that linked folders can be defined)
 * and, if an existing external location was specified, detects the class path.
 *
 * <p>
 * Clients may instantiate or subclass.
 * </p>
 *
 * @since 3.4
 */
@InternalFragmentReplaceable
public class BlackBerryProjectWizardPageTwo extends BasicBlackBerryProjectWizardPageTwo {

    public BlackBerryProjectWizardPageTwo( BlackBerryProjectWizardPageOne mainPage ) {
        super( mainPage );
    }

}
