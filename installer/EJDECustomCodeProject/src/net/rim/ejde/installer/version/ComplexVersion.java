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

public class ComplexVersion {
	// version number for released eclipse product.
	protected EclipseVersion _eclipseVersionRelease;
	
	// version number is retrieved from "Manifest.mf" file in specified product plugin
	protected EclipseVersion _eclipseVersionManifest;
	
	// version number is retrieved from "plugin.proerties" file in specified product plugin
	protected EclipseVersion _eclipseVersionPluginProperties;
	
	/**
	 * Create an instance from version string
	 */
	public ComplexVersion(String versionRelease, String versionManifest, String versionPluginProperties) {
		_eclipseVersionRelease=EclipseVersion.parseVersion(versionRelease);
		_eclipseVersionManifest=EclipseVersion.parseVersion(versionManifest);
		_eclipseVersionPluginProperties=EclipseVersion.parseVersion(versionPluginProperties);
	}
	
	/**
	 * 
	 * @param versionManifest
	 * @param versionPluginProperties
	 * @return
	 */
	public EclipseVersion getReleasedVersion(String versionManifest, String versionPluginProperties) {
		return getReleasedVersion(EclipseVersion.parseVersion(versionManifest),
				EclipseVersion.parseVersion(versionPluginProperties));
	}
	
	/**
	 * 
	 * @param versionManifest
	 * @param versionPluginProperties
	 * @return
	 */
	public EclipseVersion getReleasedVersion(EclipseVersion versionManifest, EclipseVersion versionPluginProperties) {
		EclipseVersion result=null;
		
		if ((versionManifest != null) && (versionPluginProperties != null)){	
			if (isMatched(versionManifest, versionPluginProperties)) {
				// if matched, return corresponding released version.
				result=_eclipseVersionRelease;
			}
		}
		
		return result;
	}
	
	/**
	 * Test if versions in manifest and plugin.properties are matched with their counterparts
	 * 
	 * subclasses can define their own matching rules.
	 * 
	 * @param versionManifest
	 * @param versionPluginProperties
	 * @return
	 */
	protected boolean isMatched(EclipseVersion versionManifest, EclipseVersion versionPluginProperties) {
		boolean result=false;
		
		// the default matching rule is to compare major.minor.micro in manifest and plugin.properties files
		if ((versionManifest != null) && (versionPluginProperties != null)){
			result=_eclipseVersionManifest.equals(versionManifest, EclipseVersion.TYPE_MAJOR_MINOR_MICRO)
				&& _eclipseVersionPluginProperties.equals(versionPluginProperties, EclipseVersion.TYPE_MAJOR_MINOR_MICRO);
		}
		
		return result;
	}
	
}
