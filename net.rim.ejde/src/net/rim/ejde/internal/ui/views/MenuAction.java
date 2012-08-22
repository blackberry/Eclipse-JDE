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
package net.rim.ejde.internal.ui.views;

import net.rim.ejde.internal.core.RimIDEUtil;
import net.rim.ide.core.IDEError;
import net.rim.ide.core.VarContentsHelper.MenuItem;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;

public class MenuAction extends BasicAction {
    private static final Logger log = Logger.getLogger( MenuAction.class );
    private MenuItem _menuItem;

    public MenuAction( BasicDebugView view, String text, int image, String hint ) {
        super( view, text, image, hint );
    }

    public MenuAction( BasicDebugView view, MenuItem menuItem ) {
        super( view, RimIDEUtil.convertAddressMark( menuItem.text ), 0, EMPTY_STRING, menuItem.isCheck ? AS_CHECK_BOX
                : AS_UNSPECIFIED );
        _menuItem = menuItem;
        setEnabled( _menuItem.enabled );
        setChecked( _menuItem.isSelected );
    }

    public MenuItem getMenuItem() {
        return _menuItem;
    }

    public void setMenuItem( MenuItem item ) {
        _menuItem = item;
        setText( _menuItem.text );
        setEnabled( _menuItem.enabled );
    }

    public void run() {
        if( _menuItem == null )
            return;
        try {
            _menuItem.action.invoked();
        } catch( IDEError e ) {
            log.error( e.getMessage(), e );
            MessageDialog.openError( _view.getSite().getShell(), _view.getTitle(), e.toString() );
        }
    }
}
