/*
* Copyright (c) 2010-2012 Research In Motion Limited. All rights reserved.
*
* This program and the accompanying materials are made available
* under the terms of the Eclipse Public License, Version 1.0,
* which accompanies this distribution and is available at
*
* http://www.eclipse.org/legal/epl-v10.html
*
*/
package net.rim.ejde.internal.model;

import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jdt.launching.environments.IAccessRuleParticipant;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;

public class BlackBerrySDKAccessRuleParticipant implements IAccessRuleParticipant {

    public IAccessRule[][] getAccessRules( IExecutionEnvironment environment, IVMInstall vm, LibraryLocation[] libraries,
            IJavaProject project ) {
        IAccessRule[] accessRule = new IAccessRule[ 0 ];
        /*
         * //The below code fragment is left for future possible usage //Currently access rules are handled by
         * ClasspathChangeManager class for(LibraryLocation lib:libraries){
         * if(lib.getSystemLibraryPath().lastSegment().equalsIgnoreCase(IConstants.RIM_API_JAR)){ accessRule =
         * RIAUtils.createAccessRules(lib.getSystemLibraryPath(),vm); } }
         */
        return new IAccessRule[][] { accessRule };
    }
}
