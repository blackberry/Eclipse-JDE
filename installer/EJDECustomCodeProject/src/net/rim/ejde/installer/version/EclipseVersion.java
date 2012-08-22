/*
* Copyright (c) 2010-2012 Research In Motion Limited. All rights reserved.
*
* This program and the accompanying materials are made available
* under  the terms of the Apache License, Version 2.0,
* which accompanies this distribution and is available at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
*/
package net.rim.ejde.installer.version;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;


/***
 * Code is copied from org.osgi.framework.Version
 * 
 * @author jluo
 *
 */
public class EclipseVersion {
	public static final int TYPE_MAJOR=1;
	public static final int TYPE_MAJOR_MINOR=2;
	public static final int TYPE_MAJOR_MINOR_MICRO=4;
	public static final int TYPE_ALL=8;
	
	private final int			major;
	private final int			minor;
	private final int			micro;
	private final String		qualifier;
	private static final String	SEPARATOR		= ".";					//$NON-NLS-1$


	/**
	 * Creates a version identifier from the specified numerical components.
	 * 
	 * <p>
	 * The qualifier is set to the empty string.
	 * 
	 * @param major Major component of the version identifier.
	 * @param minor Minor component of the version identifier.
	 * @param micro Micro component of the version identifier.
	 * @throws IllegalArgumentException If the numerical components are
	 *         negative.
	 */
	public EclipseVersion(int major, int minor, int micro) {
		this(major, minor, micro, null);
	}

	/**
	 * Creates a version identifier from the specifed components.
	 * 
	 * @param major Major component of the version identifier.
	 * @param minor Minor component of the version identifier.
	 * @param micro Micro component of the version identifier.
	 * @param qualifier Qualifier component of the version identifier. If
	 *        <code>null</code> is specified, then the qualifier will be set
	 *        to the empty string.
	 * @throws IllegalArgumentException If the numerical components are negative
	 *         or the qualifier string is invalid.
	 */
	public EclipseVersion(int major, int minor, int micro, String qualifier) {
		if (qualifier == null) {
			qualifier = ""; //$NON-NLS-1$
		}

		this.major = major;
		this.minor = minor;
		this.micro = micro;
		this.qualifier = qualifier;
		validate();
	}

	/**
	 * Created a version identifier from the specified string.
	 * 
	 * <p>
	 * Here is the grammar for version strings.
	 * 
	 * <pre>
	 * version ::= major('.'minor('.'micro('.'qualifier)?)?)?
	 * major ::= digit+
	 * minor ::= digit+
	 * micro ::= digit+
	 * qualifier ::= (alpha|digit|'_'|'-')+
	 * digit ::= [0..9]
	 * alpha ::= [a..zA..Z]
	 * </pre>
	 * 
	 * There must be no whitespace in version.
	 * 
	 * @param version String representation of the version identifier.
	 * @throws IllegalArgumentException If <code>version</code> is improperly
	 *         formatted.
	 */
	public EclipseVersion(String version) {
		int major = 0;
		int minor = 0;
		int micro = 0;
		String qualifier = ""; //$NON-NLS-1$

		try {
			StringTokenizer st = new StringTokenizer(version, SEPARATOR, true);
			major = Integer.parseInt(st.nextToken());

			if (st.hasMoreTokens()) {
				st.nextToken(); // consume delimiter
				minor = Integer.parseInt(st.nextToken());

				if (st.hasMoreTokens()) {
					st.nextToken(); // consume delimiter
					micro = Integer.parseInt(st.nextToken());

					if (st.hasMoreTokens()) {
						st.nextToken(); // consume delimiter
						qualifier = st.nextToken();

						if (st.hasMoreTokens()) {
							throw new IllegalArgumentException("invalid format"); //$NON-NLS-1$
						}
					}
				}
			}
		}
		catch (NoSuchElementException e) {
			throw new IllegalArgumentException("invalid format"); //$NON-NLS-1$
		}

		this.major = major;
		this.minor = minor;
		this.micro = micro;
		this.qualifier = qualifier;
		validate();
	}

	/**
	 * Called by the Version constructors to validate the version components.
	 * 
	 * @throws IllegalArgumentException If the numerical components are negative
	 *         or the qualifier string is invalid.
	 */
	private void validate() {
		if (major < 0) {
			throw new IllegalArgumentException("negative major"); //$NON-NLS-1$
		}
		if (minor < 0) {
			throw new IllegalArgumentException("negative minor"); //$NON-NLS-1$
		}
		if (micro < 0) {
			throw new IllegalArgumentException("negative micro"); //$NON-NLS-1$
		}
		int length = qualifier.length();
		for (int i = 0; i < length; i++) {
			if ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-".indexOf(qualifier.charAt(i)) == -1) { //$NON-NLS-1$
				throw new IllegalArgumentException("invalid qualifier"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Parses a version identifier from the specified string.
	 * 
	 * <p>
	 * See <code>Version(String)</code> for the format of the version string.
	 * 
	 * @param version String representation of the version identifier. Leading
	 *        and trailing whitespace will be ignored.
	 * @return A <code>Version</code> object representing the version
	 *         identifier. If <code>version</code> is <code>null</code> or
	 *         the empty string then <code>emptyVersion</code> will be
	 *         returned.
	 * @throws IllegalArgumentException If <code>version</code> is improperly
	 *         formatted.
	 */
	public static EclipseVersion parseVersion(String version) {
		if (version == null) {
			return null;
		}

		version = version.trim();
		if (version.length() == 0) {
			return null;
		}

		return new EclipseVersion(version);
	}

	/**
	 * Returns the major component of this version identifier.
	 * 
	 * @return The major component.
	 */
	public int getMajor() {
		return major;
	}

	/**
	 * Returns the minor component of this version identifier.
	 * 
	 * @return The minor component.
	 */
	public int getMinor() {
		return minor;
	}

	/**
	 * Returns the micro component of this version identifier.
	 * 
	 * @return The micro component.
	 */
	public int getMicro() {
		return micro;
	}

	/**
	 * Returns the qualifier component of this version identifier.
	 * 
	 * @return The qualifier component.
	 */
	public String getQualifier() {
		return qualifier;
	}

	/**
	 * Returns the string representation of this version identifier.
	 * 
	 * <p>
	 * The format of the version string will be <code>major.minor.micro</code>
	 * if qualifier is the empty string or
	 * <code>major.minor.micro.qualifier</code> otherwise.
	 * 
	 * @return The string representation of this version identifier.
	 */
	public String toString() {
		String base = major + SEPARATOR + minor + SEPARATOR + micro;
		if (qualifier.length() == 0) { //$NON-NLS-1$
			return base;
		}
		else {
			return base + SEPARATOR + qualifier;
		}
	}

	/**
	 * Returns a hash code value for the object.
	 * 
	 * @return An integer which is a hash code value for this object.
	 */
	public int hashCode() {
		return (major << 24) + (minor << 16) + (micro << 8)
				+ qualifier.hashCode();
	}

	/**
	 * Check if current version is equal with provided version.
	 * 
	 * @param otherEclipseVersion
	 * @param type
	 * @return true if equal otherwise return false
	 */
	public boolean equals(EclipseVersion otherEclipseVersion, int type) {
		boolean result=false;
		
		if (otherEclipseVersion != null) {
			switch (type) {
				case TYPE_MAJOR:
					result=(this.getMajor() == otherEclipseVersion.getMajor());
					break;
				case TYPE_MAJOR_MINOR:
					result=(this.getMajor() == otherEclipseVersion.getMajor())
						&& (this.getMinor() == otherEclipseVersion.getMinor());
					break;
				case TYPE_MAJOR_MINOR_MICRO:
					result=(this.getMajor() == otherEclipseVersion.getMajor())
						&& (this.getMinor() == otherEclipseVersion.getMinor())
						&& (this.getMicro() == otherEclipseVersion.getMicro());
					break;
				case TYPE_ALL:
					result=(this.getMajor() == otherEclipseVersion.getMajor())
						&& (this.getMinor() == otherEclipseVersion.getMinor())
						&& (this.getMicro() == otherEclipseVersion.getMicro())
						&& (this.getQualifier().equalsIgnoreCase(otherEclipseVersion.getQualifier()));
					break;
			}
		}
		
		return result;
	}
	
	/**
	 * Check if current version is equal with any version in provided version array
	 * 
	 * @param otherEclipseVersions
	 * @param type
	 * @return
	 */
	public boolean equals(EclipseVersion[] otherEclipseVersions, int type) {
		boolean result=false;
		
		for (int i=0; !result && i<otherEclipseVersions.length; i++) {
			result=equals(otherEclipseVersions[i], type);
		}
		
		return result;
	}
	
	/**
	 * 
	 * @param object
     * @return A negative integer, zero, or a positive integer if this object is
     *         less than, equal to, or greater than the specified
     *         <code>EclipseVersion</code> object.
	 */
    public int compareTo(Object object) {
        if (object == this) { // quicktest
            return 0;
        }

        EclipseVersion other = (EclipseVersion) object;

        int result = major - other.major;
        if (result != 0) {
            return result;
        }

        result = minor - other.minor;
        if (result != 0) {
            return result;
        }

        result = micro - other.micro;
        if (result != 0) {
            return result;
        }

        return qualifier.compareTo(other.qualifier);
    }
	
	public static void main(String[] args) {
		EclipseVersion[] versions=new EclipseVersion[2];
		versions[0]=new EclipseVersion("3.3.1");
		versions[1]=new EclipseVersion("3.3.2");
		
		System.out.println(versions);
	}
	

}
