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

import com.zerog.ia.api.pub.CustomCodeAction;
import com.zerog.ia.api.pub.InstallException;
import com.zerog.ia.api.pub.InstallerProxy;
import com.zerog.ia.api.pub.UninstallerProxy;


public class UnzipFileAction extends CustomCodeAction {

    /* (non-Javadoc)
     * @see com.zerog.ia.api.pub.CustomCodeAction#install(com.zerog.ia.api.pub.InstallerProxy)
     */
    public void install(InstallerProxy proxy) throws InstallException {    	
        
        String zipFileName=proxy.substitute("$ZIPFILE_NAME$");    	
        String targetFolderName=proxy.substitute("$TARGET_FOLDER_NAME$");
        InstallerUtil.unzip(zipFileName, targetFolderName);
    }

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

