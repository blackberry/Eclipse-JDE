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
package net.rim.ejde.internal.validation;

import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;

/**
 * @author cbateman, dmeng, mcacenco
 */
public class BBDiagnostic extends BasicDiagnostic {
    public final static BBDiagnostic OK_INSTANCE = new BBDiagnostic( OK, "", -1, "", null );
    protected int[] _errPos = new int[] { 0, 0 };

    /**
     * @param source
     * @param code
     * @param message
     * @param data
     */
    protected BBDiagnostic( String source, int code, String message, Object[] data ) {
        super( source, code, message, data );
    }

    /**
     * @param severity
     * @param source
     * @param code
     * @param message
     * @param data
     */
    protected BBDiagnostic( int severity, String source, int code, String message, Object[] data ) {
        super( severity, source, code, message, data );
    }

    /**
     * @param severity
     * @param source
     * @param code
     * @param message
     * @param data
     */
    protected BBDiagnostic( int severity, String source, int[] errPos, String message ) {
        super( severity, source, errPos[ 0 ], message, null );
        _errPos = errPos;
    }

    /**
     * @param source
     * @param code
     * @param children
     * @param message
     * @param data
     */
    protected BBDiagnostic( String source, int code, List< Diagnostic > children, String message, Object[] data ) {
        super( source, code, children, message, data );
    }

    public boolean isOk() {
        return getSeverity() == OK;
    }

    public String getChainedMessage( final char lineSeparator ) {
        String chainedMessage = getMessage();

        if( chainedMessage != null && !chainedMessage.equals( "" ) ) {
            chainedMessage += lineSeparator;
        }

        for( Iterator< Diagnostic > it = getChildren().iterator(); it.hasNext(); ) {
            BBDiagnostic diagnostic = (BBDiagnostic) it.next();
            chainedMessage += diagnostic.getChainedMessage( lineSeparator );
            chainedMessage += lineSeparator;
        }

        if( chainedMessage.length() > 0 && chainedMessage.charAt( chainedMessage.length() - 1 ) == '\n' ) {
            chainedMessage = chainedMessage.substring( 0, chainedMessage.length() - 1 );
        }

        return chainedMessage;
    }

    /**
     * Determines if two WicaDiagnostics are equivalent. Sub-classes should override if they add comparison-relevant attributes
     *
     * @param otherDiag
     * @return true if equal, false if not
     */
    boolean compare( BBDiagnostic otherDiag ) {
        if( getCode() == otherDiag.getCode() && getMessage().equals( otherDiag.getMessage() )
                && getSeverity() == otherDiag.getSeverity() && getSource().equals( otherDiag.getSource() )
                && getChildren().size() == otherDiag.getChildren().size() ) {

            // compare children
            for( int i = 0; i < getChildren().size(); i++ ) {
                BBDiagnostic oldChildDiag = (BBDiagnostic) getChildren().get( i );
                BBDiagnostic newChildDiag = (BBDiagnostic) otherDiag.getChildren().get( i );
                if( !oldChildDiag.compare( newChildDiag ) ) {
                    return false;
                }
            }
            return true;

        }
        return false;
    }

    protected void mergeDiagnostic( BBDiagnostic otherDiag ) {
        if( overlapDiag( this, otherDiag ) ) {
            return;
        }
        for( Diagnostic diag : getChildren() ) {
            if( diag instanceof BBDiagnostic && overlapDiag( (BBDiagnostic) diag, otherDiag ) ) {
                return;
            }
        }
        this.merge( otherDiag );
    }

    private boolean overlapDiag( BBDiagnostic diag1, BBDiagnostic diag2 ) {
        // display the most significant message of 2 overlapping diagnostics
        if( isOverlappingDiag( diag1, diag2 ) ) {
            if( diag2.message.length() > diag1.message.length() ) {
                diag1.message = diag2.message;
            }
            return true;
        }
        return false;
    }

    private boolean isOverlappingDiag( BBDiagnostic diag1, BBDiagnostic diag2 ) {
        if( diag1._errPos[ 0 ] == diag2._errPos[ 0 ] && diag1._errPos[ 1 ] == diag2._errPos[ 1 ]
                && diag1.source.equals( diag2.source ) ) {
            return true;
        }
        return false;
    }
}
