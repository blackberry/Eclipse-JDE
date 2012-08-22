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

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class BlackBerryMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

    private static final Logger _log = Logger.getLogger( BlackBerryMarkerResolutionGenerator.class );

    @Override
    public boolean hasResolutions( IMarker marker ) {
        if( marker != null ) {
            try {
                Object id = marker.getAttribute( IRIMMarker.ID );
                if( ( id != null ) && ( id instanceof Integer )
                        && ( ( (Integer) id ).intValue() == IRIMMarker.LIBMAIN_PROBLEM_ID ) ) {
                    return true;
                }
            } catch( CoreException ce ) {
                _log.error( "Cannot retrieve ID from Marker", ce );
            }
        }
        return false;
    }

    @Override
    public IMarkerResolution[] getResolutions( IMarker marker ) {
        return new IMarkerResolution[] { new BlackBerryLibMainMarkerResolution() };
    }

}
