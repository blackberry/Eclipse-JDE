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

import net.rim.ejde.internal.core.IConstants;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/**
 * This is the basic action class for all the actions used in debug views(profiler, memory states, objects etc.).
 */
public class BasicAction extends Action implements IConstants {
    private static final Logger _log = Logger.getLogger( BasicAction.class );
    BasicDebugView _view;
    Image _image;
    int _actionCode;

    /**
     * Constructs a new BasicAction instance.
     *
     * @param view
     *            the view this action belongs to.
     * @param text
     *            the string used as the text for the action, or <code>null</code> if there is no text
     * @param actionCode
     *            the code which indicates the type of the action (e.g. refresh action)
     * @param hint
     *            the hint string for the action.
     */
    public BasicAction( BasicDebugView view, String text, int actionCode, String hint ) {
        this( view, text, actionCode, hint, AS_UNSPECIFIED );
    }

    /**
     * Constructs a new BasicAction instance.
     *
     * @param view
     *            the view this action belongs to.
     * @param text
     *            the string used as the text for the action, or <code>null</code> if there is no text
     * @param actionCode
     *            the code which indicates the type of the action (e.g. refresh action)
     * @param hint
     *            the hint string for the action.
     * @param style
     *            of the action.
     */
    public BasicAction( BasicDebugView view, String text, int actionCode, String hint, int style ) {
        super( text, style );
        _view = view;
        _actionCode = actionCode;
        if( actionCode != 0 ) {
            _image = BasicDebugView.createImage( actionCode );
            if( _image != null )
                setImageDescriptor( ImageDescriptor.createFromImage( _image ) );
        }
        setToolTipText( hint );
        if( actionCode == BasicDebugView.OPTIONS_BUTTON || actionCode == BasicDebugView.FILTER_BUTTON )
            setEnabled( true );
        else
            setEnabled( false );
    }

    /**
     * Does the job of the action when it is clicked.
     */
    public void run() {
        if( _view != null )
            try {
                _view.run( this );
            } catch( CoreException e ) {
                _log.error( e );
                MessageDialog.openError( _view.getSite().getShell(), _view.getTitle(), e.toString() );
            }
    }

    /**
     * Disposes the resources used by the action.
     */
    public void dispose() {
        if( _image != null )
            _image.dispose();
    }

    /**
     * Get the code of the action.
     */
    public int getActionCode() {
        return _actionCode;
    }
}
