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

import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import net.rim.ejde.internal.util.InternalSchemaValidatorUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.Diagnostic;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class BBSchemaValidator implements IBBComponentValidator {

    private static final Logger _logger = Logger.getLogger( BBSchemaValidator.class );

    /** Indicates the source of the diagnostic. */
    public static final String SCHEMA_SOURCE = "net.rim.ejde.internal.validation.schema";

    /**
     * Validates the given object (IProject)
     */
    public BBDiagnostic validate( Object validateThis ) {
        IFile modelFile = (IFile) validateThis;
        URL schema = InternalSchemaValidatorUtils.getSchemaLocation();
        BBDiagnostic ret = validateBBSchema( modelFile, schema );
        return ret;
    }

    private BBDiagnostic validateBBSchema( IFile modelFile, URL schemaLocation ) {

        BBDiagnostic diag = AbstractDiagnosticFactory.createChainedDiagnostic();

        // 1. Lookup a factory for the W3C XML Schema language
        SchemaFactory factory = SchemaFactory.newInstance( "http://www.w3.org/2001/XMLSchema" );

        try {
            // 2. Compile the schema.
            // Here the schema is loaded from a java.io.File, but you could use
            // a java.net.URL or a javax.xml.transform.Source instead.
            Schema schema = factory.newSchema( schemaLocation );
            // 3. Get a validator from the schema.
            Validator validator = schema.newValidator();
            validator.setErrorHandler( new MyErrorHandler( diag ) );
            // 4. Parse the document you want to check.
            Source source = new StreamSource( modelFile.getContents() );
            // 5. Check the document
            validator.validate( source );
        } catch( Exception e ) {
            _logger.error( e );
            diag.add( DiagnosticFactory.createDiagnostic( BBDiagnostic.ERROR, 0, e.getMessage() ) );
        }
        return diag;
    }

    private static class MyErrorHandler implements ErrorHandler {
        private BBDiagnostic _diagnostic;

        public MyErrorHandler( BBDiagnostic diagnostic ) {
            _diagnostic = diagnostic;
        }

        public void warning( SAXParseException e ) throws SAXException {
            addDiagnostic( e, Diagnostic.WARNING );
        }

        public void error( SAXParseException e ) throws SAXException {
            addDiagnostic( e, Diagnostic.ERROR );
        }

        public void fatalError( SAXParseException e ) throws SAXException {
            addDiagnostic( e, Diagnostic.ERROR );
        }

        private void addDiagnostic( SAXParseException e, int severity ) {
            BBDiagnostic newdiag = new BBDiagnostic( severity, SCHEMA_SOURCE,
                    new int[] { e.getLineNumber(), e.getColumnNumber() }, e.getMessage() );
            _diagnostic.mergeDiagnostic( newdiag );
        }
    }
}
