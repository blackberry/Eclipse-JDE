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

import net.rim.ejde.internal.util.Messages;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.osgi.util.NLS;

/**
 * @author cbateman
 */
public final class DiagnosticFactory extends AbstractDiagnosticFactory {
    /**
     * We use category mechanism to distinguish different type of errors. Category mechanism: Major category id + Sub category id
     * + Sequence id 0xd d dddd ^ ^ ^^^^ Major Sub Sequence
     */

    // Project property category
    public final static int PROJECT_PROPERTIES_CANNOT_BE_BLANK = 0x100000;
    public final static int PROJECT_VERSION_BAD_SYNTAX_ID = 0x100001;
    public final static int CREATE_FOLDER_ERR_ID = 0x100002;

    public final static String DIAGNOSTIC_SOURCE = "net.rim.ejde.internal.validation"; //$NON-NLS-1$

    public static BBDiagnostic create_PROJECT_PROPERTIES_CANNOT_BE_BLANK( String attrn ) {
        final String txtmsg = NLS.bind( Messages.DiagnosticFactory_Properties_Cannot_Be_Blank, attrn );
        return new BBDiagnostic( Diagnostic.ERROR, DIAGNOSTIC_SOURCE, PROJECT_PROPERTIES_CANNOT_BE_BLANK, txtmsg, null );
    }

    public static BBDiagnostic create_HOME_SCREEN_POSITION_INVALID() {
        final String txtmsg = Messages.DiagnosticFactory_Home_Screen_Error;
        return new BBDiagnostic( Diagnostic.ERROR, DIAGNOSTIC_SOURCE, -1, txtmsg, null );
    }

    public static BBDiagnostic create_ICON_MISSING( String name ) {
        final String txtmsg = NLS.bind( Messages.DiagnosticFactory_Icon_Does_Not_Exist_Error, name );
        return new BBDiagnostic( Diagnostic.ERROR, DIAGNOSTIC_SOURCE, -1, txtmsg, null );
    }

    public static BBDiagnostic create_FILE_MISSING( String name ) {
        final String txtmsg = NLS.bind( Messages.DiagnosticFactory_File_Does_Not_Exist_Error, name );
        return new BBDiagnostic( Diagnostic.ERROR, DIAGNOSTIC_SOURCE, -1, txtmsg, null );
    }

    public static BBDiagnostic create_RESOURCE_MISSING( String resourceName ) {
        final String txtmsg = NLS.bind( Messages.ResourcesSection_invalidResource, resourceName );
        return new BBDiagnostic( Diagnostic.ERROR, DIAGNOSTIC_SOURCE, -1, txtmsg, null );
    }

    public static BBDiagnostic create_RESOURCE_KEY_INVALID( String keyName ) {
        final String txtmsg = NLS.bind( Messages.ResourcesSection_invalidResourceKey, keyName );
        return new BBDiagnostic( Diagnostic.ERROR, DIAGNOSTIC_SOURCE, -1, txtmsg, null );
    }

    public static BBDiagnostic create_VALUE_REQUIRED() {
        final String txtmsg = Messages.DiagnosticFactory_Value_Required_Error;
        return new BBDiagnostic( Diagnostic.ERROR, DIAGNOSTIC_SOURCE, -1, txtmsg, null );
    }

    public static BBDiagnostic create_INVALID_OUTPUT_PATH_CHAR() {
        return new BBDiagnostic( Diagnostic.ERROR, DIAGNOSTIC_SOURCE, -1,
                Messages.DiagnosticFactory_Invalid_Output_Path_Char_Error, null );
    }

    public static BBDiagnostic create_OutputFN_MUST_DIFFER(String ofn) {
        return new BBDiagnostic( Diagnostic.ERROR, DIAGNOSTIC_SOURCE, -1, NLS.bind(  
        		Messages.DiagnosticFactory_OutputFN_Must_Differ, ofn), null );
    }

    public static BBDiagnostic create_SYSTEM_MODULE_PROBLEMATIC() {
        return new BBDiagnostic( BBDiagnostic.ERROR, DIAGNOSTIC_SOURCE, -1, Messages.DiagnosticFactory_System_Module_Problematic,
                null );
    }

    public static BBDiagnostic createDiagnostic( int severity, int code, String txtmsg ) {
        return new BBDiagnostic( severity, DIAGNOSTIC_SOURCE, code, txtmsg, null );
    }

    /**
     * Default constructor
     */
    private DiagnosticFactory() {
        // do nothing
    }
}
