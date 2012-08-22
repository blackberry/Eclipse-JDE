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
package net.rim.ejde.internal.signing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class CodeSignMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

    public boolean hasResolutions( IMarker marker ) {
        return true;
    }

    public IMarkerResolution[] getResolutions( IMarker marker ) {
        List< IMarkerResolution2 > resolutions = new ArrayList< IMarkerResolution2 >();
        resolutions.add( new CodeSignMarkerResolution() );
        return resolutions.toArray( new IMarkerResolution[ resolutions.size() ] );
    }

}
