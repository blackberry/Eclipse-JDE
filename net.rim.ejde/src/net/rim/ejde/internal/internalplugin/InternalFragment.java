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
package net.rim.ejde.internal.internalplugin;

/**
 * Responsible for initialization work - e.g. trigger rimification logic
 */
@InternalFragmentReplaceable
public class InternalFragment {

    /**
     * If the internal fragment is present this method will kick in verification if the RIM internal settings have been applied.
     */
    public static void startup() {
        // do nothing if this class is not replaced by the internal fragment
    }
}
