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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VersionUtil {
    static FolderComparator _folderComparator=new FolderComparator();
    
    public static File getFolderBasedOnLatestVersion(List folderList) {
        File result=null;
        
        if (folderList.size() > 0) {
            if (folderList.size() == 1) {
                // don't need to sort
                result=(File)folderList.get(0);
            }else {
                Collections.sort(folderList, _folderComparator);
                // get the latest one, which is the last element
                result=(File)folderList.get(folderList.size()-1);
            }
        }
        
        return result;
    }
    
    static class FolderComparator implements Comparator {

        public int compare(Object obj1, Object obj2) {
            File folderFile1=(File)obj1;
            File folderFile2=(File)obj2;
            EclipseVersion folderVersion1=EclipseVersion.parseVersion(
                    getVersionStr(folderFile1.getName()));
            EclipseVersion folderVersion2=EclipseVersion.parseVersion(
                    getVersionStr(folderFile2.getName()));
            
            return folderVersion1.compareTo(folderVersion2);
        }
        
        private String getVersionStr(String folderName) {
            String result=folderName.substring(folderName.indexOf("_")+1);
            return result;
        }
    }
}

