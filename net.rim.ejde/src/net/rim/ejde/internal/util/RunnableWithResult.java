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
package net.rim.ejde.internal.util;

/**
 * Utility class for providing Runnable services that returns an arbitrary result
 *
 * @author bchabot
 *
 */
public abstract class RunnableWithResult implements Runnable {

    private Object _result;

    public final void run() {
        _result = doRunWithResult();
    }

    public Object getResult() {
        return _result;
    }

    protected abstract Object doRunWithResult();

    public final Object runWithResult() {
        run();
        return getResult();
    }
}
