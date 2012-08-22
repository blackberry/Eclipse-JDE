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

import java.io.File;

import com.zerog.ia.api.pub.CustomCodeRule;

public class InvalidEclipseFolder extends CustomCodeRule {
    
    static String _invalid_folder_msg="$INVALID_FOLDER_MSG$";
    
    /* (non-Javadoc)
     * @see com.zerog.ia.api.pub.CustomCodeRule#evaluateRule()
     */
    public boolean evaluateRule() {
        boolean invalidFolder=false;
        
        String installationFoler=ruleProxy.substitute("$USER_INSTALL_DIR$");
        int maxLen=Integer.parseInt(ruleProxy.substitute("$MAX_INSTALLATION_DIR_LEN$"));
        if  (installationFoler.length()>maxLen) {
            invalidFolder=true;
            ruleProxy.setVariable(_invalid_folder_msg, InstallerUtil.ERROR_OVER_LENGTH);
        }else {
            File folderFile=new File(installationFoler);
            if (!folderFile.isAbsolute()) {
                invalidFolder=true;
                ruleProxy.setVariable(_invalid_folder_msg, InstallerUtil.ERROR_NOT_ABSOLUTE);
            } else if (!InstallerUtil.isValidPathPartCharacter(installationFoler)) {
                // installation folder is absolute, now check the special characters
                // valid path should NOT include some special characters #:%<>"!*?|
                invalidFolder=true;
                ruleProxy.setVariable(_invalid_folder_msg, InstallerUtil.ERROR_INVALID_CHARACTER);
            } else {
                // check the chosen eclipse folder
                // check if chosen folder contain "eclipse.exe" and "eclipse.ini" files
                File eclipseExeFile=new File(installationFoler+File.separator+"eclipse.exe");
                File eclipseIniFile=new File(installationFoler+File.separator+"eclipse.ini");
                if (!eclipseExeFile.exists() || !eclipseIniFile.exists()) {
                    invalidFolder=true;
                    ruleProxy.setVariable(_invalid_folder_msg, InstallerUtil.ERROR_INVALID_ECLIPSE_FOLDER);
                }
            }
        }
        
        if (!invalidFolder) {
            ruleProxy.setVariable(InstallerUtil.VALID_USER_INSTALL_DIR_VAR, 
                    InstallerUtil.validateDir(ruleProxy.substitute("$USER_INSTALL_DIR$")));
            /*
            ruleProxy.setVariable(InstallerUtil.VALID_JAVA_DOT_HOME_VAR, 
                    InstallerUtil.validateDir(ruleProxy.substitute("$JAVA_DOT_HOME$")));
            */
        }
        
        return invalidFolder;
    }

}


