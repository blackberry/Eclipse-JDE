/*
* Copyright (c) 2010-2012 Research In Motion Limited. All rights reserved.
*
* This program and the accompanying materials are made available
* under  the terms of the Apache License, Version 2.0,
* which accompanies this distribution and is available at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* To use this code further you must also obtain a valid copy of
* InstallAnywhere 8.0 Enterprise/resource/IAClasses.zip
* Please visit http://www.flexerasoftware.com/products/installanywhere.htm for the terms.
* 
*/
package net.rim.ejde.installer;

import net.rim.ejde.installer.version.EclipseVersion;

import com.zerog.ia.api.pub.InstallerProxy;

public class CheckEclipseVersion4FullInstaller extends
		AbstractCheckEclipseVersion {

	@Override
	protected void doCheck(InstallerProxy proxy) {
    	String checkResult=CHECK_RESULT_FAILED;
    	String msg="Eclipse installation is not found.";

    	if (_currentEclipsePlatformVersion != null) {
    		// find existing eclipse platform
    		System.out.println("Version of selected Eclipse platform: "+_currentEclipsePlatformVersion.toString());
    		
    		if (_currentEclipsePlatformVersion.equals(_certifiedEclipseVersions, EclipseVersion.TYPE_MAJOR_MINOR)) {
    			if ((_currentJDTVersion != null) 
    					&& _currentJDTVersion.equals(_certifiedJDTVersions, EclipseVersion.TYPE_MAJOR_MINOR)) {
        			checkResult=CHECK_RESULT_PASSED;
        			msg="Certified Eclipse Java Developer Platform "+_currentJDTVersion.toString()+" is found.";
    			}else {
    				// JDT with certified version is not found
    				checkResult=CHECK_RESULT_WARNING_JDT;
    				msg="Eclipse Java Developer Platform "+debugVersions(_certifiedJDTVersions)+" is not found.";
    			}
    		}else {
    			checkResult=CHECK_RESULT_WARNING;
    			msg="Eclipse "+_currentEclipsePlatformVersion.toString()+" found in the install folder is not compatible with " 
    				+proxy.substitute("$PRODUCT_NAME$") + "\n";
    		}
    	}else {
    		// no eclipse is found
    		// do nothing
    	}
    	
    	proxy.setVariable(IA_VAR_ECLIPSE_VERSION_RESULT, checkResult);
    	proxy.setVariable(IA_VAR_ECLIPSE_VERSION_RESULT_MSG, msg);
    	
    	System.out.println(IA_VAR_ECLIPSE_VERSION_RESULT+" = "+checkResult);
    	System.out.println(IA_VAR_ECLIPSE_VERSION_RESULT_MSG+" = "+msg);

	}

}
