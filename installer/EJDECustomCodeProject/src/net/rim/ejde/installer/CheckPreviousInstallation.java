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

import com.zerog.ia.api.pub.CustomCodeAction;
import com.zerog.ia.api.pub.InstallException;
import com.zerog.ia.api.pub.InstallerProxy;
import com.zerog.ia.api.pub.UninstallerProxy;

/***
 * This class is used to check if previous product has been installed in the same folder 
 * 
 * @author jluo
 *
 */
public class CheckPreviousInstallation extends CustomCodeAction {
    private static String UNINSTALL_PREFIX="Uninstall";
    private InstallerProxy _proxy;
    private String _installationFolder;
    
    static private String _hasPreviousInstallation="$HAS_PREVIOUS_INSTALLATION$";
    static private String _installFolderIsEmpty="$INSTALL_FOLDER_ISEMPTY$";
    static private String _previousInstallationUninstallerFolder="$PREVIOUS_INSTALLATION_UNINSTALLER_FOLDER$";

    public void install(InstallerProxy proxy) throws InstallException  {
        _proxy=proxy;
        _installationFolder=_proxy.substitute("$USER_INSTALL_DIR$");
        
        if(isEmptyDirectory()) {
            _proxy.setVariable(_installFolderIsEmpty, "true");
        }
        else {
            _proxy.setVariable(_installFolderIsEmpty, "false");
        }
        
        if (hasPreviousEJDE()) {
            _proxy.setVariable(_hasPreviousInstallation, "true");
            _proxy.setVariable(_previousInstallationUninstallerFolder, getUninstallerFolder());
        }else {
            _proxy.setVariable(_hasPreviousInstallation, "false");
        }
    }

    public void uninstall(UninstallerProxy arg0) throws InstallException {
        // TODO Auto-generated method stub
        
    }

    private boolean hasPreviousEJDE() {
        boolean foundPreviousEJDE=false;
        
        try {
            File bbEclipse = new File(_installationFolder);
            if(bbEclipse.isDirectory()){
                if(bbEclipse.listFiles().length > 0) {
                    
                }
            }
            File pluginsFolderFile=new File(_installationFolder+File.separator+"plugins");
            if (pluginsFolderFile.exists() && pluginsFolderFile.isDirectory()) {
                File[] files = pluginsFolderFile.listFiles();
                for(int i=0; i<files.length; ++i) {
                    if (InstallerUtil.startsWithIgnoreCase(files[i].getName(),"net.rim.ejde")) {
                        foundPreviousEJDE=true;
                        break;
                    }
                }
            }
        }catch (Exception ex) {
            System.out.println("Exception:"+ex.getMessage());
        }
        
        return foundPreviousEJDE;
    }
    
    private boolean isEmptyDirectory() {
        boolean isEmpty=true;
        
        
        File bbEclipse = new File(_installationFolder);
        if(bbEclipse.isDirectory()){
            if(bbEclipse.listFiles().length > 0) {
                isEmpty = false;
            }
        }
        return isEmpty;
                
    }
    
    private String getUninstallerFolder() {
        String uninstallerFolder=_installationFolder; // set the default value
        
        try {
            boolean found=false;
            File  installPathFile=new File(_installationFolder);
            File[] files = installPathFile.listFiles();

            for(int i=0; i<files.length; ++i)
            {
                if(files[i].isDirectory()) {
                    if (InstallerUtil.startsWithIgnoreCase(files[i].getName(),UNINSTALL_PREFIX+_proxy.substitute("_$PRODUCT_NAME$"))) {
                        // find candidate uninstall folder
                        // we need double-check to ensure it
                        File[] subFiles=files[i].listFiles();
                        for (int j=0; j<subFiles.length; j++) {
                            if (InstallerUtil.startsWithIgnoreCase(subFiles[j].getName(),UNINSTALL_PREFIX)
                                    && subFiles[j].getName().endsWith(".exe")) {
                                found=true;
                                uninstallerFolder=files[i].getCanonicalPath();
                                break;
                            }
                        }// for
                    }
                }
                
                if (found) break;
            }
        }catch (Exception ex) {
            System.out.println("Exception:"+ex.getMessage());
        }
        
        System.out.println("uninstallerFolder:"+uninstallerFolder);
        return uninstallerFolder;
    }
    
    
    public String getInstallStatusMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getUninstallStatusMessage() {
        // TODO Auto-generated method stub
        return null;
    }

}

