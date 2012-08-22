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

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that a class in the main eJDE plug-in may be replaced by the RIM internal plug-in fragment (if present)
 * to supply RIM-internal behaviour.
 */
@Target({ ElementType.TYPE })
public @interface InternalFragmentReplaceable {

}
