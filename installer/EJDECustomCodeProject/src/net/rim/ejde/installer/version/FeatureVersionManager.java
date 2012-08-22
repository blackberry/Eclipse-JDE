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

import java.io.File;
import java.util.ArrayList;

import net.rim.ejde.installer.XMLHelper;

public class FeatureVersionManager {
	public static EclipseVersion getFeatureVersion(String eclipseFolder, String featureId) {
		EclipseVersion result=null;
		
		try {
			File featureFolder=getFeatureFolder(eclipseFolder, featureId);
			if (featureFolder != null) {
				File featureXMLFile=new File(featureFolder.getCanonicalPath() +
						File.separator + "feature.xml");
				if (featureXMLFile.exists() && featureXMLFile.isFile()) {
					String versionStr=XMLHelper.evaluate(featureXMLFile, "/feature/@version");
					if (versionStr != null) {
						result=EclipseVersion.parseVersion(versionStr);
					}
				}
			}
		}catch (Exception ex) {
			System.out.println("eclipseFolder:" + eclipseFolder);
			System.out.println("featureId:" + featureId);
			System.out.println("Exception in getFeatureVersion :" + ex.getMessage());
		}
		
		return result;
	}
	
	public static File getFeatureFolder(String eclipseFolder, String featureId) {
		File result=null;
		String prefix=featureId+"_";
		
		File featuresFolder=new File(eclipseFolder+File.separator+"features");
		if (featuresFolder.exists() && featuresFolder.isDirectory()) {
			File[] files=featuresFolder.listFiles();
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

	public static void main(String[] args) {
		EclipseVersion eclipseVersion=FeatureVersionManager.getFeatureVersion("C:\\test\\eclipsetest", "org.eclipse.jdt");
		if (eclipseVersion != null) {
			System.out.println(eclipseVersion.toString());
		}else {
			System.out.println("Null");
		}

	}

}
