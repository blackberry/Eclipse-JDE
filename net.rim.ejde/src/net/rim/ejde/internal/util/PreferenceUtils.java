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

import java.util.ArrayList;
import java.util.StringTokenizer;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;

import org.eclipse.jface.preference.IPreferenceStore;

public class PreferenceUtils {
    public static ArrayList< String > parseList( IPreferenceStore store, String prefsKey, String delim ) {
        ArrayList< String > tagsList = new ArrayList< String >();
        if( delim.isEmpty() ) {
            return tagsList;
        }
        String contentTags = store.getString( prefsKey );
        StringTokenizer st = new StringTokenizer( contentTags, delim );
        while( st.hasMoreTokens() ) {
            String s = st.nextToken();
            tagsList.add( s );
        }
        return tagsList;
    }

    public static ArrayList< String > parseDefaultList( String defaultContents, String delim ) {
        ArrayList< String > tagsList = new ArrayList< String >();
        if( delim.isEmpty() ) {
            return tagsList;
        }
        StringTokenizer st = new StringTokenizer( defaultContents, delim );
        while( st.hasMoreTokens() ) {
            String s = st.nextToken();
            tagsList.add( s );
        }
        return tagsList;
    }

    public static void storeList( IPreferenceStore store, String[] tagsArray, String prefKey, char delim ) {
        StringBuffer buf = new StringBuffer();
        for( String s : tagsArray ) {
            buf.append( s );
            buf.append( delim );
        }
        store.setValue( prefKey, buf.toString() );
    }

    /**
     * Gets the default project startup tier.
     *
     * @return the default project startup tier
     */
    public static Integer getDefaultProjectStartupTier() {
        return Integer.valueOf( ContextManager.getDefault().getPreferenceStore().getInt( IConstants.PROJECT_STARTUP_TIER_KEY ) );
    }

    /**
     * Gets the default value for displaying the home screen position field in the BB_App_Descriptor editor.
     *
     * @return the default value for displaying the home screen position (don't show)
     */
    public static Integer getDefaultVisibleHomeScreenPosition() {
        return Integer.valueOf( ContextManager.getDefault().getPreferenceStore()
                .getInt( IConstants.VISIBLE_HOMESCREENPOSITION_KEY ) );
    }

    /**
     * Gets the default value for displaying the clean simulator menu entry
     *
     * @return the default value for displaying the clean simulator menu entry
     */
    public static Integer getDefaultVisibleCleanSimulator() {
        return Integer
                .valueOf( ContextManager.getDefault().getPreferenceStore().getInt( IConstants.VISIBLE_CLEAN_SIMULATOR_KEY ) );
    }
}
