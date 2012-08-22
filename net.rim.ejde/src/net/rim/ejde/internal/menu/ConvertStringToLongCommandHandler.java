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
package net.rim.ejde.internal.menu;

import java.util.Collection;
import java.util.Iterator;

import net.rim.ide.RIA;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.AbstractTextEditor;

public class ConvertStringToLongCommandHandler extends AbstractHandler {
    private static final Logger _log = Logger.getLogger( ConvertStringToLongCommandHandler.class );

    @Override
    public Object execute( ExecutionEvent event ) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection( event );
        IEditorPart editor = HandlerUtil.getActiveEditor( event );
        if( ( selection != null ) && ( selection instanceof ITextSelection ) && ( editor != null )
                && ( editor instanceof AbstractTextEditor ) ) {
            AbstractTextEditor textEditor = (AbstractTextEditor) editor;
            ITextSelection textSelection = (ITextSelection) selection;
            try {
                String newString = "0x" + Long.toHexString( RIA.stringToLong( textSelection.getText() ) ) + "L";

                textEditor.getDocumentProvider().getDocument( textEditor.getEditorInput() )
                        .replace( textSelection.getOffset(), textSelection.getLength(), newString );
            } catch( BadLocationException e ) {
                _log.error( "ConvertStringToLongHangdler: ", e );
            }
        }

        return null;
    }

    @Override
    public void setEnabled( Object evaluationContext ) {
        boolean enablement = false;
        if( ( evaluationContext != null ) && ( evaluationContext instanceof IEvaluationContext ) ) {
            IEvaluationContext evalContext = (IEvaluationContext) evaluationContext;
            Object var = evalContext.getDefaultVariable();
            if( ( var != null ) && ( var instanceof Collection ) ) {
                Collection collection = (Collection) var;
                Iterator selectionIterator = collection.iterator();
                enablement = true;
                for( ; selectionIterator.hasNext(); ) {
                    Object selection = selectionIterator.next();
                    if( selection instanceof ITextSelection ) {
                        ITextSelection textSelection = (ITextSelection) selection;
                        enablement = enablement && !textSelection.getText().isEmpty();
                    } else {
                        enablement = false;
                        break;
                    }
                }
            }
        }
        setBaseEnabled( enablement );
    }

}
