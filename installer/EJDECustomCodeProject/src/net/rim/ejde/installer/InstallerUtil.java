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
* Additionally, for the Windows(R) installer you must obtain a valid copy of vcredist_x86.exe
* Please visit http://www.microsoft.com/en-us/download/details.aspx?id=29 for the terms.
* 
*/
package net.rim.ejde.installer;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.zerog.awt.ZGStandardDialog;
import java.util.StringTokenizer;

/**
 * InstallerUtil
 */
public class InstallerUtil {
	
    public static String ERROR_OVER_LENGTH="Your chosen folder is too long. Please change.\nNote:  the maximum length of folder is limited to $MAX_INSTALLATION_DIR_LEN$ characters.";
    public static String ERROR_NOT_ABSOLUTE="Your chosen folder is not absolute. Please change.\nNote: you can use Choose button to select one.";
    public static String ERROR_INVALID_CHARACTER="Your chosen folder includes some invalid character(s). Please check.";
    public static String ERROR_INVALID_ECLIPSE_FOLDER="Your chosen folder doesn't contain Eclipse platform.";
    public static String ERROR_SPACE_IN_PATH="Your chosen folder contains space(s).";
    public static final String ERROR_FOLDER_NOT_WRITABLE = "You do not have enough permisions to write and (or) execute software installed into chosen folder";
	
    public static final String VALID_USER_INSTALL_DIR_VAR="$VALID_USER_INSTALL_DIR$";
    
    public static final String VALID_PATH_PART_PATTERN="[^#:%<>\"!*?|]*";

    public static boolean isValidPathPartCharacter(String folderName) {
        boolean valid=true;
        
        try {
            File folderFile=new File(folderName);
            File parentFile=folderFile.getParentFile();
            
            while ((parentFile != null) && valid) {
                valid=folderFile.getName().matches(VALID_PATH_PART_PATTERN);
                folderFile=parentFile;
                parentFile=folderFile.getParentFile();
            }
        }catch (Exception ex) {
            System.out.println("Exception in isValidCharacter:"+ex.getMessage());
            System.out.println("folderName:"+folderName);
        }
        
        return valid;
    }

    public static boolean isPathContainsSpace(String folderName) {
        return folderName.trim().contains(" ");
    }

    public static void showMsgDialog(java.awt.Frame parentFrame, String title, String message) {
        ZGStandardDialog zgDialog = new ZGStandardDialog(parentFrame, title, message, "");
        zgDialog.setDefaultButtonLabel("OK");
        zgDialog.setModal(true);
        zgDialog.show();
        zgDialog.dispose();
    }
	
    public static KeyStore loadKeyStore(InputStream keyStoreInputStream, String keyStorePassword) throws Exception {

        KeyStore keyStore=KeyStore.getInstance("JKS");
        keyStore.load(keyStoreInputStream, keyStorePassword.toCharArray());
        
        return keyStore;
    }
    
    /***
     * replace '\' in path with '/'
     * 
     * @param directory
     * @return
     */
    public static String validateDir(String directory) {
        StringBuffer buffer=new StringBuffer();
        for (int i=0; i<directory.length(); i++) {
            if (directory.charAt(i) == '\\') {
                buffer.append('/');
            }else {
                buffer.append(directory.charAt(i));
            }
        }
        
        return buffer.toString();
    }
    
    /**
     * Delete files or top-level subfolders whose name starts with given prefix
     * 
     * @param rootDir
     * @param prefix
     * @throws IOException
     */
    public static void deleteChildrenWithPrefix(File rootDir, String prefix) throws IOException {
        if (!rootDir.exists()) {
            return;
        }
        
        File[] files = rootDir.listFiles();

        for(int i=0; i<files.length; i++) {
            if (startsWithIgnoreCase(files[i].getName(), prefix)) {
                if (files[i].isDirectory()) {
                    // it is folder
                    deleteDir(files[i]);
                }else {
                    // it is file
                    if (! files[i].delete()) {
                        System.out.println("Fail to delete file:"+files[i].getCanonicalPath());
                    }
                }
            }
        }
    }
    
    public static void deleteDir(File vDir) throws IOException
    {
    	if (! vDir.exists()) {
    		// if specified folder doesn't exist, do nothing.
    		return;
    	}
    	
        File[] files = vDir.listFiles();

        for(int i=0; i<files.length; ++i)
        {
            if(files[i].isDirectory())
                deleteDir(files[i]);
            else
            {
                if (! files[i].delete())
                   System.out.println("Fail to delete file:"+
                        files[i].getCanonicalPath());
            }
        }
        if (! vDir.delete())
           System.out.println("Fail to delete dir:"+vDir.getCanonicalPath());
    }
    
    public static void deleteDir(String vDir) throws IOException
    {
        deleteDir(new File(vDir));
    }
    
    public static void deleteFile(String fileName) throws IOException {
    	deleteFile(new File(fileName));
    }
    
    public static void deleteFile(File file) throws IOException {
    	if (file.exists() && file.isFile()) {
    		if (! file.delete()) {
    			System.out.println("Fail to delete file:"+
    					file.getCanonicalPath());
    		}
    	}
    }
    
    public static boolean startsWithIgnoreCase(String thisString, String prefix) {
        String temp=thisString.substring(0, Math.min(prefix.length(), thisString.length()));
        return temp.equalsIgnoreCase(prefix);
    }

	public static void copyFile(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[1024 * 4];
		int len;

		while ((len = in.read(buffer)) != -1) {
			out.write(buffer, 0, len);
		}

		in.close();
		out.close();
	}
	
	public static void unzip(String zipFileName, String targetFolderPath) {
		Enumeration entries;
		ZipFile zipFile;
		
		try {
			// check target folder
			File targetFolder = new File(targetFolderPath);
			if (!targetFolder.exists()) {
				targetFolder.mkdirs();
			}
			String canonicalTargetPath = targetFolder.getCanonicalPath();

			// unzip zip file
			zipFile = new ZipFile(zipFileName);
			entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				if (entry.isDirectory()) {
					// handle directory entry
					File folder = new File(canonicalTargetPath + File.separator
							+ entry.getName());
					System.out.println(folder.getCanonicalPath());
					if (!folder.exists() && !folder.mkdirs()) {
						System.out.println("failed to create "
								+ folder.getCanonicalPath());
					}
				} else {
					// handle file entry, we must ensure the parent folder exists.
					System.out.println("Extracting file: " + entry.getName());
					File file=new File(canonicalTargetPath+File.separator+entry.getName());
		        	File parentFolder=file.getParentFile();
		        	if (!parentFolder.exists() && !parentFolder.mkdirs()) {
						System.out.println("failed to create parent folder "
								+ parentFolder.getCanonicalPath());
					}
					copyFile(zipFile.getInputStream(entry),
							new BufferedOutputStream(new FileOutputStream(file)));
                                        file.setLastModified(entry.getTime());
				}
			}
			zipFile.close();
		} catch (IOException ex) {
			System.out.println("Unzip Exception: "+ex.getMessage());
		}
	}
	
	public static void installVC2008(String vcFileName) {
		
		try {
			File vcExeFile = new File(vcFileName);
			
			String command=vcExeFile.getCanonicalPath()+" /q";
			
			System.out.println("vcFileName "+vcFileName);
			
			System.out.println("command "+command);
			Runtime.getRuntime().exec(command, null);
			
			//allow vc installer to execute 
			Thread.sleep(5000);
			 			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
    
    public static boolean testFolderWriteability(File installationFoler) {
        File f = installationFoler;
        File prev = installationFoler;
        while(!f.exists()){
            prev = f;
            f = f.getParentFile();
        }

        String fname = prev.toString();
        fname = fname.substring(fname.lastIndexOf(File.separatorChar));        
        f = new File(f, fname); //get first level of directory which does not exists
        
        boolean dummyFailed = false;
                    
        if( f.mkdir() ){ //try to create
            if (System.getProperty("os.name").contains("Windows")){
                String[] echo = new String[]{"cmd.exe", "/c", "echo", "ccc", ">", "dummy"};
                try {
                    Process p = Runtime.getRuntime().exec(echo, null, f);
                    p.waitFor();
                    if (p.exitValue() != 0)
                        dummyFailed = true;

                    File ff = new File(f, "dummy");
                    if (ff.exists()){
                        ff.delete();        
                    } else {
                        dummyFailed = true;
                    }
                } catch (Exception e){
                    dummyFailed = true;
                }
                
                File virtualStore = new File(System.getProperty("user.home") + "\\AppData\\Local\\VirtualStore");
                if (virtualStore.exists()){
                    String path  = installationFoler.getPath().substring(installationFoler.getPath().indexOf(':')+1);
                    StringTokenizer st = new StringTokenizer(path, "\\");
                    int maxPathTokens = st.countTokens();
                    File virtFile = new File(virtualStore, path);            
                    if (virtFile.exists()){
                        File[] files = virtFile.listFiles(); //this will delete IA working files under proposed install folder
                        for (File file : files) {
                            file.delete();
                        }
                        dummyFailed = true; //we failed already
                        File parent;
                        for (int i = 0; i < maxPathTokens; i++){ //remove trash from IA
                            parent  = virtFile.getParentFile();
                            virtFile.delete(); //this will delete install folder
                            virtFile = parent;
                        }
                    }
                }
            }
            
            if (!dummyFailed){
                return f.delete();  //and delete it, this must all pass
            } else {
                f.delete();
            }
        }
        return false;
    }
	        
    /***
     * Only for test purpose
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(testFolderWriteability(new File("c:\\Eclipse")));
//        try {
//            File rootDir=new File("C:\\Eclipse32Test-SDK\\features");
//            deleteChildrenWithPrefix(rootDir, "net.rim.wica.tools");
//            
//            rootDir=new File("C:\\Eclipse32Test-SDK\\plugins");
//            deleteChildrenWithPrefix(rootDir, "net.rim.opensource.tools");
//            deleteChildrenWithPrefix(rootDir, "net.rim.wica.common");
//            deleteChildrenWithPrefix(rootDir, "net.rim.wica.tools");
//            deleteChildrenWithPrefix(rootDir, "org.xmlsoap.schemas.wsdl");
//        }catch (Exception ex) {
//            System.out.println("Exception:"+ex.getMessage());
//        }
    }

}
