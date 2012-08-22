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
package net.rim.ejde.internal.ui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.util.Messages;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.osgi.util.NLS;

public class PreprocessDefineInputValidator implements IInputValidator {

    private PreprocessDirectiveUI UI;
    private boolean isEdit;
    private String initialText;
    private static final Pattern validPPTagPattern = Pattern.compile( IConstants.PP_VALIDATION_REG_EX );

    public PreprocessDefineInputValidator( PreprocessDirectiveUI preprocessDirectiveUI, boolean isEdit, String initialText ) {
        this.UI = preprocessDirectiveUI;
        this.isEdit = isEdit;
        this.initialText = initialText;
    }

    public PreprocessDefineInputValidator( PreprocessDirectiveUI preprocessDirectiveUI ) {
        this.UI = preprocessDirectiveUI;
        this.isEdit = false;
        initialText = "";
    }

    public String isValid( String newText ) {
    	if(newText==null || newText.length()==0) {
    		return Messages.BuildPrefsPage_PreprocessValidationMsg1;
    	}
        String[] tagStrings = StringUtils.split( newText, IConstants.SEMICOLON_MARK );
        List< String > validtag = new ArrayList< String >();
        for( String tagString : tagStrings ) {
            if( StringUtils.isEmpty( tagString ) ) {
                return Messages.BuildPrefsPage_PreprocessValidationMsg1;
            } else if( !( validPPTagPattern.matcher( tagString ).matches() ) ) {
                if( tagString.contains( IConstants.SEMICOLON_MARK ) && !isEdit ) {// for add ; is a valid character
                    for( String tag : StringUtils.split( tagString, IConstants.SEMICOLON_MARK ) ) {
                        if( !( validPPTagPattern.matcher( tag ).matches() ) ) {
                            return Messages.BuildPrefsPage_PreprocessValidationMsg2 + tag;
                        }
                    }
                } else {
                    return Messages.BuildPrefsPage_PreprocessValidationMsg2 + newText;
                }
            } else if( validtag.contains( tagString ) ) {
                return NLS.bind( Messages.BuildPrefsPage_PreprocessValidationMsg4, tagString );
            } else if( isExistsInTable( UI.getDefineList(), tagString ) ) {
                if( !( initialText.equals( tagString ) ) && isEdit ) {
                    return NLS.bind( Messages.BuildPrefsPage_PreprocessValidationMsg3, tagString );
                } else if( !isEdit ) {// eliminate duplication during add
                    return NLS.bind( Messages.BuildPrefsPage_PreprocessValidationMsg3, tagString );
                }
            }
            validtag.add( tagString );
        }
        return null;
    }

    private boolean isExistsInTable( List< String > defineList, String tag ) {
        boolean result = false;
        if( ( tag == null ) || ( defineList == null ) ) {
            return result;
        }
        for( String define : defineList ) {
            if( define.equals( tag ) ) {
                result = true;
                break;
            }
        }
        return result;
    }
}
