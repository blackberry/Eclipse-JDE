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
package net.rim.ejde.internal.model.preferences;

import java.util.ArrayList;
import java.util.List;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.PreprocessorTag;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;
import net.rim.ejde.internal.util.PreferenceUtils;

import org.eclipse.jface.preference.IPreferenceStore;

public class PreprocessorPreferences {
    static IPreferenceStore store = ContextManager.getDefault().getPreferenceStore();

    public static List< PreprocessorTag > getPreprocessDefines() {
        ArrayList< String > tagsList = PreferenceUtils.parseList( store, PreferenceConstants.PREPROCESSOR_DEFINE_LIST, "," );
        return getPPList( tagsList, "|" );
    }

    public static void setPreprocessDefines( List< PreprocessorTag > ppObjList ) {
        String[] preprocessDefines = convertObjToArray( ppObjList, "|" );
        PreferenceUtils.storeList( store, preprocessDefines, PreferenceConstants.PREPROCESSOR_DEFINE_LIST, ',' );
    }

    public static List< PreprocessorTag > getDefaultPreprocessDefines() {
        final String preprocessDefault = store.getDefaultString( PreferenceConstants.PREPROCESSOR_DEFINE_LIST );
        ArrayList< String > tagsList = PreferenceUtils.parseDefaultList( preprocessDefault, "," );
        return getPPList( tagsList, "|" );
    }

    private static String[] convertObjToArray( List< PreprocessorTag > ppObjList, String delim ) {
        String[] ppTags = new String[ ppObjList.size() ];
        int i = 0;
        for( PreprocessorTag tag : ppObjList ) {
            ppTags[ i ] = tag.getPreprocessorDefine() + delim + tag.isActive();
            i++;
        }
        return ppTags;
    }

    private static ArrayList< PreprocessorTag > getPPList( ArrayList< String > tagsList, String delim ) {
        ArrayList< PreprocessorTag > ppList = new ArrayList< PreprocessorTag >();
        String ppDefine = null;
        String isActive = null;
        for( String tagObj : tagsList ) {
            if( tagObj.contains( "|" ) ) {// skip parsing the corrupted or old pp objs
                ppDefine = tagObj.substring( 0, tagObj.indexOf( delim ) );
                isActive = tagObj.substring( tagObj.indexOf( delim ) + 1 ).toLowerCase();
                PreprocessorTag tag = new PreprocessorTag( ppDefine, Boolean.valueOf( isActive ) );
                ppList.add( tag );
            }
        }
        return ppList;
    }

    /**
     * Gets the default value if it needs to popup when preprocess hook is missing
     *
     * @return
     */
    public static boolean getDefaultPopForPreprocessHookMissing() {
        return store.getDefaultBoolean( PreferenceConstants.POP_FOR_PREPROCESS_HOOK_MISSING );
    }

    /**
     * Gets if it needs to popup when preprocess hook is missing
     *
     * @return
     */
    public static boolean getPopForPreprocessHookMissing() {
        return store.getBoolean( PreferenceConstants.POP_FOR_PREPROCESS_HOOK_MISSING );
    }

    /**
     * Sets if it needs to popup when preprocess hook is missing
     *
     * @param enabled
     */
    public static void setPopForPreprocessHookMissing( boolean enabled ) {
        store.setValue( PreferenceConstants.POP_FOR_PREPROCESS_HOOK_MISSING, enabled );
    }
}
