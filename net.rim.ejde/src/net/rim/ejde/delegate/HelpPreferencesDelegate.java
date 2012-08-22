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
package net.rim.ejde.delegate;

import java.util.ArrayList;
import java.util.Map;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.VMUtils;

public class HelpPreferencesDelegate {
    static HelpPreferencesDelegate _instance;
    ContextManager _core;

    private HelpPreferencesDelegate() {
        _core = ContextManager.PLUGIN;
    }

    public static HelpPreferencesDelegate getInstance() {
        if( _instance == null ) {
            _instance = new HelpPreferencesDelegate();
        }

        return _instance;
    }

    public ArrayList< Map.Entry< String, String >> getJREDocsLocation() {
        return VMUtils.getJREDocsLocation();
    }

    public void openStartupPage() {
        ProjectUtils.openStartupPage();
    }
}
