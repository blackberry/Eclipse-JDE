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

import java.util.regex.Pattern;

/**
 * Represents a major.minor.micro version in a form that can be compared and converted to and from a string.
 *
 * @author ddebruin
 */
public class CompatibilityVersion implements Comparable {
    // Constants
    /** The regular expression for the compatibility version format of "x.y.z" */
    public static final String MICRO_VERSION_PATTERN = "[0-9]+\\.+[0-9]+\\.[0-9]+";

    /**
     * The regular expression for the compatibility version format of "x.y" Micro version will be treated as 0
     * */
    public static final String MINOR_VERSION_PATTERN = "[0-9]+\\.+[0-9]+.*";

    // Instance variables
    /** The major version identifier. */
    private int _majorVersion;
    /** The minor version identifier. */
    private int _minorVersion;
    /** The micro version identifier. */
    private int _microVersion;

    public CompatibilityVersion( int majorVersion, int minorVersion ) {
        init( majorVersion, minorVersion, 0 );
    }

    /**
     * Constructs an instance from the given version identifiers.
     *
     * @param majorVersion
     *            the major version identifier.
     * @param minorVersion
     *            the minor version identifier.
     * @param microVersion
     *            the micro version identifier.
     */
    public CompatibilityVersion( int majorVersion, int minorVersion, int microVersion ) {
        init( majorVersion, minorVersion, microVersion );
    }

    /**
     * Constructs an instance from the given version string. The given string must match the VERSION_PATTERN.
     *
     * @param modelString
     *            the version as a string.
     * @throws IllegalArgumentException
     *             if the given string does not match the version format.
     */
    public CompatibilityVersion( String modelString ) throws IllegalArgumentException {
        if( Pattern.matches( MICRO_VERSION_PATTERN, modelString ) ) {
            String[] versions = Pattern.compile( "\\." ).split( modelString );
            init( Integer.valueOf( versions[ 0 ] ).intValue(), Integer.valueOf( versions[ 1 ] ).intValue(),
                    Integer.valueOf( versions[ 2 ] ).intValue() );
        } else if( Pattern.matches( MINOR_VERSION_PATTERN, modelString ) ) {
            String[] versions = Pattern.compile( "\\." ).split( modelString );
            init( Integer.valueOf( versions[ 0 ] ).intValue(), Integer.valueOf( versions[ 1 ] ).intValue(), 0 );

        } else {
            throw new IllegalArgumentException( "Invalid format: " + modelString );
        }
    }

    /**
     * Initializes this instance with the given version identifiers.
     *
     * @param majorVersion
     *            the major version identifier.
     * @param minorVersion
     *            the minor version identifier.
     * @param microVersion
     *            the micro version identifier.
     */
    private void init( int majorVersion, int minorVersion, int microVersion ) {
        _majorVersion = majorVersion;
        _minorVersion = minorVersion;
        _microVersion = microVersion;
    }

    /**
     * Compares two <code>CompatibilityVersion</code> objects numerically by major, minor and micro identifiers in that order.
     *
     * @param that
     *            the other version to be compared.
     * @return a negative integer, zero, or a positive integer if this <code>CompatibilityVersion</code> is less than, equal to,
     *         or greater than the given argument.
     * @throws ClassCastException
     *             if the argument is not a <code>CompatibilityVersion</code>.
     */
    public int compareTo( CompatibilityVersion that ) {
        int result = compareMajor( that );

        // if major is the same, check minor to break the tie
        if( result == 0 ) {
            result = compareMinor( that );
            // otherwise, check micro compatible
            if( result == 0 ) {
                result = compareMicro( that );
            }
        }
        return result;
    }

    /*
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( Object o ) {
        return compareTo( (CompatibilityVersion) o );
    }

    /**
     * @param that
     *            the compatibility version to compare with.
     * @return negative, zero or positive depending on if this major version number is less than, equal to or greater than the
     *         given version.
     */
    protected int compareMajor( CompatibilityVersion that ) {
        if( getMajorVersion() < that.getMajorVersion() ) {
            // if my major version is less than yours, I'm less than
            return -1;
        } else if( getMajorVersion() > that.getMajorVersion() ) {
            // if my major version is more than yours, I'm greater than
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * @param that
     *            the compatibility version to compare with.
     * @return negative, zero or positive depending on if my minor version number is less than, equal to or greater than the given
     *         version.
     */
    protected int compareMinor( CompatibilityVersion that ) {
        if( getMinorVersion() < that.getMinorVersion() ) {
            // if my minor version is less than yours, I'm less than
            return -1;
        } else if( getMinorVersion() > that.getMinorVersion() ) {
            // if my minor version is more than yours, I'm greater than
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * @param that
     *            the compatibility version to compare with.
     * @return negative, zero or positive depending on if my micro version number is less than, equal to or greater than the given
     *         version.
     */
    protected int compareMicro( CompatibilityVersion that ) {
        if( getMicroVersion() < that.getMicroVersion() ) {
            // if my micro version is less than yours, I'm less than
            return -1;
        } else if( getMicroVersion() > that.getMicroVersion() ) {
            // if my micro version is more than yours, I'm greater than
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Compares this object to the given object. The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>CompatibilityVersion</code> object that contains the same major, minor and micro version
     * identifiers as this object.
     *
     * @param obj
     *            the object to compare with.
     * @return <code>true</code> if the objects are the same; <code>false</code> otherwise.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object obj ) {
        if( obj instanceof CompatibilityVersion ) {
            return compareTo( (CompatibilityVersion) obj ) == 0;
        }
        return false;
    }

    /**
     * Generate a hashCode for this object Overrides Object.hashCode Necessary to satisfy constraint that equal objects have equal
     * hashCodes, since Object.equals is overriden for this class, and
     */
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Returns the major version identifier.
     *
     * @return the major version identifier.
     */
    public int getMajorVersion() {
        return _majorVersion;
    }

    /**
     * Returns the minor version identifier.
     *
     * @return the minor version level within a specific major version.
     */
    public int getMinorVersion() {
        return _minorVersion;
    }

    /**
     * Returns the micro version identifier.
     *
     * @return the micro version level within a specific major.minor version
     */
    public int getMicroVersion() {
        return _microVersion;
    }

    /**
     * Returns this compatibility version in the form major.minor.micro.
     *
     * @return a string representation of the object.
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return _majorVersion + "." + _minorVersion + "." + _microVersion;
    }

} // end class CompatibilityVersion
