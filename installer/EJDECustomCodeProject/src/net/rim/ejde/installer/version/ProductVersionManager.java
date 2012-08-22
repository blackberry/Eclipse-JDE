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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/***
 * Retrieve corresponding version number for given plugin id
 * @author jluo
 *
 */
public class ProductVersionManager {
	public static final String PLUGIN_ID_ORG_ECLIPSE_PLATFORM="org.eclipse.platform";
	public static final String PLUGIN_ID_ORG_ECLIPSE_SDK="org.eclipse.sdk";
		
	ArrayList _knownEclipseVesions=null;
	String _productPluginId;
	
	public ProductVersionManager() {
		this(PLUGIN_ID_ORG_ECLIPSE_PLATFORM);
	}
	
	public ProductVersionManager(String productPluginId) {
		if ((productPluginId != null) && (productPluginId.length() > 0)){
			_productPluginId=productPluginId;
		}else {
			_productPluginId=PLUGIN_ID_ORG_ECLIPSE_PLATFORM;
		}

		// add all know Eclipse versions
		_knownEclipseVesions=new ArrayList();
		addKnownEclipseVersions();
	}
	
	protected void addKnownEclipseVersions() {
		// for eclipse 3.3.0
		ComplexVersion eclipse330=new ComplexVersion("3.3.0", "3.3.0", "3.3.0");
		_knownEclipseVesions.add(eclipse330);
		
		// for eclipse 3.3.1
		ComplexVersion eclipse331=new ComplexVersion("3.3.1", "3.3.1", "3.3.0");
		_knownEclipseVesions.add(eclipse331);
		
		// for eclipse 3.3.1.1
		ComplexVersion eclipse3311=new ComplexVersion("3.3.1.1", "3.3.2", "3.3.1.1") {
			@Override
			protected boolean isMatched(EclipseVersion versionManifest,
					EclipseVersion versionPluginProperties) {
				boolean result=false;
				
				// for manifest, compare with major.minor.micro
				// for plugin.properties, compare with major.minor.micro.qualifier
				if ((versionManifest != null) && (versionPluginProperties != null)){
					result=_eclipseVersionManifest.equals(versionManifest, EclipseVersion.TYPE_MAJOR_MINOR_MICRO)
						&& _eclipseVersionPluginProperties.equals(versionPluginProperties, EclipseVersion.TYPE_ALL);
				}
				
				return result;
			}
		};
		_knownEclipseVesions.add(eclipse3311);
	}
	
	public ArrayList getKnownEclipseVesions() {
		return _knownEclipseVesions;
	}
	
	public EclipseVersion getEclipseVersion(String eclipseFolder) {
		EclipseVersion result=null;
		
		try {
			String versionManifest=getVersionFromManifest(eclipseFolder);
			String versionPluginProperties=getVersionFromPluginProperties(eclipseFolder);
			for (int i=0; i<_knownEclipseVesions.size(); i++) {
				result=((ComplexVersion)_knownEclipseVesions.get(i))
					.getReleasedVersion(versionManifest, versionPluginProperties);
				if (result != null) {
					// find corresponding eclipse
					break;
				}
			}
			
			if (result == null) {
				// no corresponding eclipse, we have to use one found in plugin.properties file
				result=EclipseVersion.parseVersion(versionPluginProperties);
			}
		}catch (Exception ex) {
			System.out.println("eclipseFolder:" + eclipseFolder);
			System.out.println("_productPluginId:" + _productPluginId);
			System.out.println("Exception in getEclipseVersion :" + ex.getMessage());
		}

		return result;
	}
	
	private String getVersionFromManifest(String eclipseFolder) {
		String result=null;
		
		try {
			File platformPluginFolder=getPluginFolder(eclipseFolder, _productPluginId);
			if (platformPluginFolder != null) {
				File manifestFile=new File(platformPluginFolder.getCanonicalPath() + File.separator +
						"META-INF"+ File.separator + "MANIFEST.MF");
				if (manifestFile.exists() && manifestFile.isFile()) {
					Manifest manifest = new Manifest();
					manifest.read(new FileInputStream(manifestFile));
					Attributes attributes = manifest.getMainAttributes();
					result=attributes.getValue("Bundle-Version").trim();
				}
			}
		}catch (Exception ex) {
			System.out.println("Exception in getVersionFromManifest:"+ex.getMessage());
		}
		
		return result;
	}
	
	private String getVersionFromPluginProperties(String eclipseFolder) {
		String result=null;
		
		try {
			File platformPluginFolder=getPluginFolder(eclipseFolder, _productPluginId);
			if (platformPluginFolder != null) {
				File propertyFile=new File(platformPluginFolder.getCanonicalPath() + File.separator +
						"plugin.properties");
				if (propertyFile.exists() && propertyFile.isFile()) {
					Properties properties=new Properties();
					properties.load(new FileInputStream(propertyFile));
					String blurb=properties.getProperty("productBlurb");
					if ((blurb != null ) && (blurb.length() > 0)){
						result=extractVersion(blurb);
					}
				}
			}
		}catch (Exception ex) {
			System.out.println("Exception in getVersionFromManifest:"+ex.getMessage());
		}
		
		return result;
	}
	
	private String extractVersion(String blurb){
		String result=null;
		
		try {
	        BufferedReader reader=new BufferedReader(new StringReader(blurb));
	        String line=reader.readLine();
	        while (line != null) {
	            line=line.trim();
	            if (line.startsWith("Version:")) {
	            	// find the right line, now get the version number
	            	String[] parts=line.split(":");
	            	if (parts.length == 2) {
	            		result=parts[1].trim();
	            	}
	            	
	            	break;
	            }
	            
	            // read next line
	            line=reader.readLine();
	        }
	        reader.close();
		}catch (Exception ex) {
			System.out.println("Exception in extractVersion:"+ex.getMessage());
		}
		
		return result;
	}
	
	private File getPluginFolder(String eclipseFolder, String pluginId) {
		File result=null;
		String prefix=pluginId+"_";
		
		File pluginsFolder=new File(eclipseFolder+File.separator+"plugins");
		if (pluginsFolder.exists() && pluginsFolder.isDirectory()) {
			File[] files=pluginsFolder.listFiles();
			ArrayList candidates=new ArrayList();
			for (int i=0; i<files.length; i++) {
				if (files[i].isDirectory()) {
					if (files[i].getName().startsWith(prefix)) {
						// find the right one
					    /*
						result=files[i];
						break;
						*/
					    candidates.add(files[i]);
					}
				}
			}
			result=VersionUtil.getFolderBasedOnLatestVersion(candidates);
		}
		
		return result;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// VersionManager manager=new VersionManager(PLUGIN_ID_ORG_ECLIPSE_PLATFORM);
		ProductVersionManager manager=new ProductVersionManager(PLUGIN_ID_ORG_ECLIPSE_PLATFORM);
		EclipseVersion eclipseVersion=manager.getEclipseVersion("C:\\test\\eclipsetest");
		if (eclipseVersion != null) {
			System.out.println(eclipseVersion.toString());
		}else {
			System.out.println("Null");
		}

	}
	
	

}
