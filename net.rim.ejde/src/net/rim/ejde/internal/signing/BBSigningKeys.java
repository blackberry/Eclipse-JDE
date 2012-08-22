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
package net.rim.ejde.internal.signing;

import java.util.List;

import net.rim.ide.RIA;

public class BBSigningKeys {

    // static private final Logger log = Logger.getLogger( BBSigningKeys.class );
    private RIA _ria;

    public BBSigningKeys( RIA ria ) {
        _ria = ria;
    }

    /**
     * Return the Array of classes which should not be used for auto-complete ( includes the internal classes )
     */
    public String[] getProtectedClasses() {
        return _ria.getHiddenClasses();
    }

    /**
     * Gets the protected methods.
     *
     * @return the protected methods
     */
    public String[] getProtectedMethods() {
        return _ria.getHiddenMethods();
    }

    /**
     * Return the array of protected API keys
     *
     * @return keys int[]
     */
    public int[] getKeys() {
        return _ria.getKeys();
    }

    /**
     * Returns the key name crossponding to the key id
     *
     * @param keyId
     *            Integer
     * @return keyName String
     */
    public String getKeyName( Integer keyId ) {
        return _ria.keyName( keyId );
    }

    /**
     * Return the list of classes crossponding to the key id
     *
     * @param keyId
     *            Integer
     * @return classes List<String>
     */
    public List< String > getClassesByKey( Integer keyId ) {
        return _ria.getClassesByKey( keyId );
    }

    /**
     * Returns the key crossponding to the class
     *
     * @param class String
     * @return key Integer
     */
    public Integer getKey( String cls ) {
        return _ria.getKey( cls );
    }

    /**
     * Return the allow status crossponding to the key
     *
     * @param keyId
     *            Integer
     * @return status boolean
     */
    public boolean getAllowKey( Integer keyId ) {
        return _ria.getAllowKey( keyId );
    }
}
