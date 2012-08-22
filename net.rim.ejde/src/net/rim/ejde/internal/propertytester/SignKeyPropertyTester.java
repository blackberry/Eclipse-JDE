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
package net.rim.ejde.internal.propertytester;

import java.io.File;
import java.io.IOException;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.util.VMToolsUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.expressions.PropertyTester;

public class SignKeyPropertyTester extends PropertyTester {

    public static final String PROPERTY_NAME = "net.rim.ejde.isSignKeyInstalled";
    private static Logger _log = Logger.getLogger( SignKeyPropertyTester.class );

    @Override
    public boolean test( Object receiver, String property, Object[] args, Object expectedValue ) {
        File cskFile;
        File dbFile;
        boolean result = false;
        try {
            cskFile = new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.CSK_FILE_NAME );
            dbFile = new File( VMToolsUtils.getVMToolsFolderPath() + File.separator + IConstants.DB_FILE_NAME );
            if( ( cskFile.exists() ) && ( dbFile.exists() ) ) {
                result = true;
            }
        } catch( IOException io ) {
            _log.error( io.getMessage() );
        }
        return result;
    }
}
