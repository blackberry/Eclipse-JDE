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
package net.rim.ejde.internal.ui.editors.locale;

import net.rim.ejde.internal.util.Messages;
import net.rim.sdk.resourceutil.ResourceCollection;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.viewers.ICellEditorValidator;

/**
 * Class for the validation of a resource key. It implements interface ICellEditorValidator and interface IInputValidator
 *
 * @author
 *
 */
class ResourceKeyValidator implements ICellEditorValidator, IInputValidator {
    private boolean _allowEmpty;
    private ResourceCollection _collection;
    private String _currentKey;

    private static String[] RESERVED_WORDS = { "boolean", "byte", "char", "class", "double", "false", "final", "float", "int",
            "long", "new", "null", "short", "true", "void" };

    /**
     * Class constructor specifying resource collection and if a key can be empty
     *
     * @param collection
     *            A collection of resources
     * @param allowEmpty
     *            Flag indicating if a key is allowed to be empty
     */
    ResourceKeyValidator( ResourceCollection collection, boolean allowEmpty ) {
        this( collection, allowEmpty, null );
    }

    /**
     * Class constructor specifying resource collection, if a key can be empty and the current key
     *
     * @param collection
     *            A collection of resources
     * @param allowEmpty
     *            Flag indicating if a key is allowed to be empty
     * @param currentKey
     *            Name of the current key
     */
    ResourceKeyValidator( ResourceCollection collection, boolean allowEmpty, String currentKey ) {
        _allowEmpty = allowEmpty;
        _collection = collection;
        _currentKey = currentKey;
    }

    /**
     * This method checks if a key is valid. A key cannot have an empty name unless it is allowed. It also have to follow a
     * certain pattern and cannot contain special characters. This method also checks if a key consists of reserved words or if a
     * key already exists.
     *
     * @param key
     *            The key to be validated
     */
    public String isValid( Object key ) {
        return isValid( (String) key );
    }

    /**
     * This method checks if a key is valid. A key cannot have an empty name unless it is allowed. It also have to follow a
     * certain pattern and cannot contain special characters. This method also checks if a key consists of reserved words or if a
     * key already exists.
     *
     * @param key
     *            The key to be validated
     */
    public String isValid( String key ) {
        // Check for empty key name
        if( !_allowEmpty && StringUtils.isBlank( key ) )
            return Messages.ResourceKeyValidator_ResourceKey_Empty;

        // Check if key contains only whitespace
        if( StringUtils.isWhitespace( key ) )
            return Messages.ResourceKeyValidator_ResourceKey_Whitespace;

        // Check if key is reserved word
        if( isReservedWord( key ) )
            return Messages.ResourceKeyValidator_ResourceKey_ReservedWord;

        // Check if key contains invalid characters
        if( containsInvalidChar( key ) )
            return Messages.ResourceKeyValidator_ResourceKey_InvalidCharacter;

        // Check if key already exists
        if( _collection.containsKey( key ) && !( key.equals( _currentKey ) ) )
            return Messages.ResourceKeyValidator_ResourceKey_KeyExists;

        return null;
    }

    private static boolean containsInvalidChar( String key ) {
        if( !key.matches( "[A-Za-z]{1}[A-Za-z0-9]*([_]{1}[A-Za-z0-9]+)*" ) ) {
            return true;
        }
        return false;
    }

    private static boolean isReservedWord( String key ) {
        for( String reservedWord : RESERVED_WORDS ) {
            if( key.equals( reservedWord ) )
                return true;
        }
        return false;
    }
}
