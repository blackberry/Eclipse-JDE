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
package net.rim.ejde.internal.packaging;

import java.util.Set;

import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;
import net.rim.ejde.internal.model.BlackBerryProject;

@InternalFragmentReplaceable
public class InternalPackagingJob extends PackagingJob {

    /**
     * Constructs a PackagingJob instance.
     *
     * @param projects
     */
    public InternalPackagingJob( Set< BlackBerryProject > projects ) {
        super( projects );
        // TODO Auto-generated constructor stub
    }

    /**
     * Constructs a PackagingJob instance.
     *
     * @param projects
     * @param signingFlag
     */
    public InternalPackagingJob( Set< BlackBerryProject > projects, int signingFlag ) {
        super( projects, signingFlag );
    }

    @Override
    protected void runPostBuild( BlackBerryProject properties ) {
        // do nothing in the main plug-in

    }

}
