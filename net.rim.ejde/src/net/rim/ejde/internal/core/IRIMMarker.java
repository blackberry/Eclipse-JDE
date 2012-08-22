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
package net.rim.ejde.internal.core;

/**
 * Defines constants for BlackBerry JDE markers
 *
 *
 */
public interface IRIMMarker {
    // Marker Types
    public static final String BLACKBERRY_PROBLEM = ContextManager.PLUGIN_ID + ".BlackBerryProblem"; //$NON-NLS-1$
    public static final String PACKAGING_PROBLEM = ContextManager.PLUGIN_ID + ".BlackBerryPackagingProblem"; //$NON-NLS-1$
    public static final String PREPROCESSING_PROBLEM_MARKER = ContextManager.PLUGIN_ID + ".BlackBerryPreprocessingProblem"; //$NON-NLS-1$
    public static final String RESOURCE_BUILD_PROBLEM_MARKER = ContextManager.PLUGIN_ID + ".BlackBerryResourceProblem"; //$NON-NLS-1$
    public static final String CODE_SIGN_PROBLEM_MARKER = ContextManager.PLUGIN_ID + ".CodeSignProblem"; //$NON-NLS-1$
    public static final String SIGNATURE_TOOL_PROBLEM_MARKER = ContextManager.PLUGIN_ID + ".SignatureToolProblem"; //$NON-NLS-1$
    public static final String PROJECT_DEPENDENCY_PROBLEM_MARKER = ContextManager.PLUGIN_ID
            + ".BlackBerryProjectDependencyProblem"; //$NON-NLS-1$
    public static final String MODEL_PROBLEM = ContextManager.PLUGIN_ID + ".xmlProblem"; //$NON-NLS-1$

    // Category IDs
    public static final int CODE_SIGN_CATEGORY_ID = 0;
    public static final int MODEL_CATEGORY_ID = 100;

    // Problem IDs
    public static final int CODE_SIGN_PROBLEM_ID = CODE_SIGN_CATEGORY_ID + 0;
    public static final int FIELD_USAGE_CODE_SIGN_PROBLEM_ID = CODE_SIGN_CATEGORY_ID + 1;
    public static final int LIBMAIN_PROBLEM_ID = MODEL_CATEGORY_ID + 0;

    // Marker Attributes
    public static final String ID = "id";
    public static final String KEY = "key";
    public static final String DATA = "data";

}
