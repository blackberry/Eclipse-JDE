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
import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.rim.ejde.internal.core.ContextManager;

import org.eclipse.core.runtime.FileLocator;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class IntroPageUtils {
    private static final String EXTENSION_CONTENT_NODE = "extensionContent";
    private static final String STYLE_ATTRIBUTE_NAME = "style";
    private static final String XML_FILE = "intro\\introContentExt.xml";

    public static void updateIntroExtensionFile( String replacementValue ) {
        try {
            URL pluginUrl = FileLocator.resolve( ContextManager.getDefault().getBundle().getEntry( "/" ) );
            File f = new File( pluginUrl.getPath().substring( 1 ) + XML_FILE );

            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse( f );
            NodeList nodes = doc.getElementsByTagName( EXTENSION_CONTENT_NODE );
            NamedNodeMap attributes = nodes.item( 0 ).getAttributes();
            String value = attributes.getNamedItem( STYLE_ATTRIBUTE_NAME ).getTextContent();
            String[] tokens = value.split( "/" );
            attributes.getNamedItem( STYLE_ATTRIBUTE_NAME ).setTextContent( value.replace( tokens[ 1 ], replacementValue ) );

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform( new DOMSource( doc ), new StreamResult( f ) );
        } catch( IOException e ) {
        } catch( ParserConfigurationException e ) {
        } catch( SAXException e ) {
        } catch( TransformerConfigurationException e ) {
        } catch( TransformerException e ) {
        }
    }
}
