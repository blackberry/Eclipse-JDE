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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;

import com.zerog.ia.api.pub.CustomCodeAction;
import com.zerog.ia.api.pub.InstallException;
import com.zerog.ia.api.pub.InstallerProxy;
import com.zerog.ia.api.pub.UninstallerProxy;

/**
 * EclipseConfiguration
 * 
 * EclipseConfiguration class is used to configure Eclipse environment for EJDE
 * 1) add "-XX:MaxPermSize=512m" into eclipse.ini file
 * 2) add "osgi.framework.extensions=net.rim.ejde" into config.ini file
 */
public class EclipseConfiguration extends CustomCodeAction
{

    /***
     * call back method invoked by InstallAnywhere.
     * Execute customized install action to set JVM parameter and preprocessor hook
     * 
     * @param proxy: installer proxy
     */
    public void install(InstallerProxy proxy) throws InstallException {
        String eclipseDirStr=proxy.substitute("$USER_INSTALL_DIR$");
        System.out.println("In EclipseConfiguration, eclipse dir:"+eclipseDirStr);
        // set JVM parameter
        setJVMPara(eclipseDirStr);
        
        // Hook will be set at run time.
        // set preprocessor hook
        // setPreprocessorHook(eclipseDirStr);    
    }
    
    /**
     * add "-XX:MaxPermSize=256m" into eclipse.ini file
     */
    protected void setJVMPara(String eclipseDirStr) {
        String vmargsToken="-vmargs";
        boolean isVmargsFound=false;
        String maxPermSizeKey="-XX:MaxPermSize";
        String maxPermSizeValue="512m";
        String maxHeapKey="-Xmx";
        String maxHeapSize="512M";
        
        
        try {
            File eclipseIniFile=new File(eclipseDirStr, "eclipse.ini");
            BufferedReader reader=new BufferedReader(new FileReader(eclipseIniFile));
            ArrayList<String> content=new ArrayList<String>();
            String line=reader.readLine();
            while (line != null) {
                line=line.trim();
                if (line.length() == 0) {
                    line=reader.readLine();
                    continue;
                }
                if ((line.indexOf(maxPermSizeKey) != -1) ||
                		(line.indexOf(maxHeapKey) != -1)) {
                    // find -XX:MaxPermSize or -Xmx, then we ignore the line
                    line=reader.readLine();
                    continue;
                }else {
                    content.add(line);
                    if (line.indexOf(vmargsToken) != -1) {
                        isVmargsFound=true;
                    }
                }
                
                // read next line
                line=reader.readLine();
            }
            reader.close();
            
            // add "-vmargs" if needed and our setting "-XX:MaxPermSize=256m" at the end
            if (!isVmargsFound) {
                content.add(vmargsToken);
            }
            content.add(maxHeapKey+maxHeapSize);
            content.add(maxPermSizeKey+"="+maxPermSizeValue);
            
            //  write content back to eclipse.ini file
            PrintWriter writer=new PrintWriter(new FileOutputStream(eclipseIniFile));
            for (int i=0; i<content.size(); i++) {
                writer.println(content.get(i));
            }
            writer.flush();
            writer.close();
            
        }catch (Exception ex) {
            System.out.println("Exception in setJVMPara"+ex.getMessage());
        }
    }
    
    /**
     * add "osgi.framework.extensions=net.rim.ejde.preprocessing.hook" into config.ini file
     */
    protected void setPreprocessorHook(String eclipseDirStr) {
        try {
            String osgi = "osgi.framework.extensions";
            File configIniFile=new File(eclipseDirStr, "configuration/config.ini");
            Properties configProperties=new Properties();
            configProperties.load(new FileInputStream(configIniFile));
            configProperties.remove(osgi);
            configProperties.setProperty(osgi, "net.rim.ejde");
           
            //  store the preference
            configProperties.store(new FileOutputStream(configIniFile), null);
        }catch (Exception ex) {
            System.out.println("Exception in setPreprocessorHook"+ex.getMessage());
        }
    }

    
    /**
     * @see com.zerog.ia.api.pub.CustomCodeAction#uninstall(com.zerog.ia.api.pub.UninstallerProxy)
     */
    public void uninstall(UninstallerProxy proxy) throws InstallException {
        
    }

    /**
     * @see com.zerog.ia.api.pub.CustomCodeAction#getInstallStatusMessage()
     */
    public String getInstallStatusMessage() {
        return "";
    }

    /** 
     * @see com.zerog.ia.api.pub.CustomCodeAction#getUninstallStatusMessage()
     */
    public String getUninstallStatusMessage() {
        return "";
    }
    
    public static void main(String[] args) {
        EclipseConfiguration eclipseConfiguration=new EclipseConfiguration();
        String eclipseDirStr="C:/SoftwareDepot/Eclipse3.5.0/eclipse";
        eclipseConfiguration.setJVMPara(eclipseDirStr);        
    }
}
