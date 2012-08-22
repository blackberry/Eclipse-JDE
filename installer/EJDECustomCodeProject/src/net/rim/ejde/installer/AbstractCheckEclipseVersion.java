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
import net.rim.ejde.installer.version.FeatureVersionManager;
import net.rim.ejde.installer.version.ProductVersionManager;

import com.zerog.ia.api.pub.CustomCodeAction;
import com.zerog.ia.api.pub.InstallException;
import com.zerog.ia.api.pub.InstallerProxy;
import com.zerog.ia.api.pub.UninstallerProxy;

public abstract class AbstractCheckEclipseVersion extends CustomCodeAction {

	public static final String IA_VAR_ECLIPSE_VERSION_RESULT="$ECLIPSE_VERSION_RESULT$";
	public static final String IA_VAR_ECLIPSE_VERSION_RESULT_MSG="$ECLIPSE_VERSION_RESULT_MSG$";
	
	// Eclipse Java Developer Platform with certified version is found
	public static final String CHECK_RESULT_PASSED="passed"; 
	// Non-certified version of Eclipse is found.
	public static final String CHECK_RESULT_WARNING="warning";
	// Eclipse with certified version is found. However JDT with certified version is not found
	public static final String CHECK_RESULT_WARNING_JDT="warning_jdt"; 
	// Eclipse is not found
	public static final String CHECK_RESULT_FAILED="failed";
	
	protected static ProductVersionManager _versionManagerPlatform=new ProductVersionManager(ProductVersionManager.PLUGIN_ID_ORG_ECLIPSE_PLATFORM);
	protected static ProductVersionManager _versionManagerSDK=new ProductVersionManager(ProductVersionManager.PLUGIN_ID_ORG_ECLIPSE_SDK);
	
	protected EclipseVersion[] _certifiedEclipseVersions;
	protected EclipseVersion[] _certifiedJDTVersions;
	protected EclipseVersion _currentEclipsePlatformVersion;
	protected EclipseVersion _currentEclipseSDKVersion;
	protected EclipseVersion _currentJDTVersion;
	
	
    /* (non-Javadoc)
     * @see com.zerog.ia.api.pub.CustomCodeAction#install(com.zerog.ia.api.pub.InstallerProxy)
     */
    public void install(InstallerProxy proxy) throws InstallException {
    	// get certified Eclipse version and JDT version
    	_certifiedEclipseVersions=parseVersions(proxy.substitute("$CERTIFIED_ECLIPSE_VERSIONS$").trim());
    	System.out.println("Certified Eclipse versions:"+debugVersions(_certifiedEclipseVersions));
    	_certifiedJDTVersions=parseVersions(proxy.substitute("$CERTIFIED_JDT_VERSIONS$").trim());
    	System.out.println("Certified JDT versions:"+debugVersions(_certifiedJDTVersions));

    	// get Eclipse and JDT version in specified install folder
    	String installationFoler=proxy.substitute("$USER_INSTALL_DIR$");
    	_currentEclipsePlatformVersion = _versionManagerPlatform.getEclipseVersion(installationFoler);
    	_currentEclipseSDKVersion=_versionManagerSDK.getEclipseVersion(installationFoler);
    	_currentJDTVersion=FeatureVersionManager.getFeatureVersion(installationFoler, "org.eclipse.jdt");
    	
    	// do check work
    	doCheck(proxy);
    }
    
    /**
     * Parse the string of versions and construct version array
     * 
     * @param versionsString
     * @return
     */
    private EclipseVersion[] parseVersions(String versionsString) {
    	EclipseVersion[] result=new EclipseVersion[0];
    	
    	if ((versionsString != null) && (versionsString.length() > 0)) {
    		String[] strArray=versionsString.split(",");
    		result=new EclipseVersion[strArray.length];
    		for (int i=0; i<strArray.length; i++) {
    			result[i]=new EclipseVersion(strArray[i].trim());
    		}
    	}
    	
    	return result;
    }
    
    protected String debugVersions(EclipseVersion[] versions) {
    	StringBuffer buffer=new StringBuffer();
    	
    	for (int i=0; i<versions.length; i++) {
    		if (i > 0) {
    			buffer.append(",");
    		}
    		
    		buffer.append(versions[i].toString());
    	}
    	
    	return buffer.toString();
    }
    

    protected abstract void doCheck(InstallerProxy proxy);
    
    /* (non-Javadoc)
     * @see com.zerog.ia.api.pub.CustomCodeAction#uninstall(com.zerog.ia.api.pub.UninstallerProxy)
     */
    public void uninstall(UninstallerProxy proxy) throws InstallException {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see com.zerog.ia.api.pub.CustomCodeAction#getInstallStatusMessage()
     */
    public String getInstallStatusMessage() {
        // TODO Auto-generated method stub
        return "";
    }

    /* (non-Javadoc)
     * @see com.zerog.ia.api.pub.CustomCodeAction#getUninstallStatusMessage()
     */
    public String getUninstallStatusMessage() {
        // TODO Auto-generated method stub
        return "";
    }

}
