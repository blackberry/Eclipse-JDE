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
package net.rim.ejde.doc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import net.rim.ejde.delegate.HelpPreferencesDelegate;

import org.eclipse.help.IHelpContentProducer;

/**
 * This class generates the dynamic content for the JAva Doc help pages. The toc.xml calls the javadocLocation.xhml page where the
 * content is dynamically generated and the page is redirected to JavaDoc location selected by the user.
 *
 * @author nbhasin
 *
 */
public class DynamicContentProducer implements IHelpContentProducer {

    public InputStream getInputStream( String pluginID, String name, Locale locale ) {

        if( name.indexOf( "javadocLocation.xhml" ) >= 0 ) {

            ArrayList< Map.Entry< String, String >> docLocations = HelpPreferencesDelegate.getInstance().getJREDocsLocation();

            return new ByteArrayInputStream( generateHTMLPage( docLocations ).getBytes() );

        } else
            return null;

    }

    /**
     * Generate html page that have all link to all installed JRE path.
     *
     * @param path
     * @return
     */
    private String generateHTMLPage( ArrayList< Map.Entry< String, String >> locs ) {
        String beginHtml, endHtml, html = "";

        beginHtml = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\" \"http://www.w3.org/MarkUp/Wilbur/HTML32.dtd\"> \n"
                + " <html> \n" + "<body><h1>Available BlackBerry JRE: </h1>\n";

        StringBuffer buf = new StringBuffer();

        for( Map.Entry< String, String > entry : locs ) {
            buf.append( "<p><a	href=\"" + entry.getValue() + "index.html" + "\">"
                    + entry.getKey().replaceFirst( "BlackBerry JRE", "BlackBerry JRE version" ) + "</a></p>\n" );
        }
        html = buf.toString();
        endHtml = "</body>\n" + "</html>\n";

        return beginHtml + html + endHtml;

    }

}
