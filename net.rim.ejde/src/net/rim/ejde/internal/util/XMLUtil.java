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
package net.rim.ejde.internal.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author jluo
 *
 */
public class XMLUtil {
    /**
     * Write DOM to a file
     *
     * @param doc
     *            DOM document
     * @param filename
     *            target file name
     * @throws Exception
     */
    public static void writeXmlFile( Document doc, String filename ) throws Exception {
        writeXmlFile( doc, filename, "ISO-8859-1", "no" );
    }

    /**
     * Write DOM to a file
     *
     * @param doc
     *            DOM document
     * @param filename
     *            target file name
     * @param encoding
     *            specified encoding
     * @param omitXmlDeclaration
     *            flag to indicate if xml declaration statement is included
     * @throws Exception
     */
    public static void writeXmlFile( Document doc, String filename, String encoding, String omitXmlDeclaration ) throws Exception {

        // Prepare the DOM document for writing
        Source source = new DOMSource( doc );

        // Prepare the output file
        FileOutputStream outputStream = new FileOutputStream( new File( filename ) );
        Result result = new StreamResult( outputStream );

        // Write the DOM document to the file
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty( OutputKeys.METHOD, "xml" );
        transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
        transformer.setOutputProperty( OutputKeys.ENCODING, encoding );
        transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration );

        transformer.transform( source, result );

        outputStream.flush();
        outputStream.close();
    }

    /**
     * Open an xml file
     *
     * @param filename
     *            source file name
     * @param validating
     *            flag to indicate if validation is required
     * @return the DOM document
     */
    public static Document openXmlFile( String filename, boolean validating ) {
        return openXmlFile( new File( filename ), validating );
    }

    /**
     * Open an xml file
     *
     * @param file
     *            the source file
     * @param validating
     *            flag to indicate if validation is required
     * @return the DOM document
     */
    public static Document openXmlFile( File file, boolean validating ) {
        try {
            // Create a builder factory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating( validating );

            // Create the builder and parse the file
            Document doc = factory.newDocumentBuilder().parse( file );
            return doc;
        } catch( SAXException e ) {
            // A parsing error occurred; the xml input is not valid
        } catch( ParserConfigurationException ex ) {
            System.out.println( ex.getMessage() );
        } catch( IOException ex ) {
            System.out.println( ex.getMessage() );
        }

        return null;
    }

    /**
     * open instream for a XML file from URL
     *
     * @param the
     *            source url
     * @return the DOM document
     */
    public static Document openXMLStream( URL url ) {
        try {
            // Create a builder factory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            InputStream stream = url.openStream();

            // Create the builder and parse the stream
            Document doc = factory.newDocumentBuilder().parse( stream );

            return doc;
        } catch( SAXException e ) {
            // A parsing error occurred; the xml input is not valid
        } catch( ParserConfigurationException ex ) {
            System.out.println( ex.getMessage() );
        } catch( IOException ex ) {
            System.out.println( ex.getMessage() );
        }

        return null;
    }

    /**
     * Get the value of evaluation of the xPath
     *
     * @param xmlFile
     *            the source xml file
     * @param xPathExpression
     *            xpath expression
     * @return the result
     */
    public static String evaluate( File xmlFile, String xPathExpression ) {
        String result = null;

        try {
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            result = xPath.evaluate( xPathExpression, new InputSource( new FileInputStream( xmlFile ) ) );
        } catch( Exception ex ) {
            System.out.println( ex.getMessage() );
        }

        return result;
    }

}
