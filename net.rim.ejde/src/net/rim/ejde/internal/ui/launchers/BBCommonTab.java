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

import java.lang.reflect.Field;

import org.eclipse.debug.ui.CommonTab;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class BBCommonTab extends CommonTab {
	public void createControl(Composite parent) {
		super.createControl(parent);

		//hiding console settings from CommonTab
        try {
            Field f = CommonTab.class.getDeclaredField("fConsoleOutput");
            f.setAccessible(true);
            Control c = (Control)f.get(this);
            if (c != null) {
            	c = c.getParent();
            	if (c != null) {
            		c = c.getParent();
            		if (c != null) c.setVisible(false);
            	}
            }
        } catch (Exception ex) {
        	//ignore
        }
	}
}
