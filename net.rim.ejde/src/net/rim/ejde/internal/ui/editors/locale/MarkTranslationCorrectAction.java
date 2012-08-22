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
package net.rim.ejde.internal.ui.editors.locale;

import net.rim.sdk.resourceutil.RIMResourceElement;
import net.rim.sdk.resourceutil.RIMResourceLocale;
import net.rim.sdk.resourceutil.ResourceElement;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * The MarkTranslationCorrectAction defines the action associated with marking the translation of a resource value within a
 * resource locale as being correct with respect to the original locale. It is activated from the context menu in the Resource
 * Editor. Correct translations for resource values are not highlighted, whereas incorrect translations are highlighted.
 */
class MarkTranslationCorrectAction extends Action {
    private ResourceElement _element;
    private String _originalLocaleString = "";

    MarkTranslationCorrectAction( ResourceElement element ) {
        super( "Mark Translation Correct" );
        _element = element;

        // Determine whether to enable or disable this action
        if( element instanceof RIMResourceElement ) {
            try {
                setEnabled( getEnableState() );
            } catch( Exception e ) {
            }
        }
    }

    /**
     * Helper method determines whether this MarkTranslationCorrectAction should be enabled.
     *
     * Returns false if: 1) User selected resource value belongs to the same locale as the original locale. 2) The translation of
     * the resource value is already marked correct. 3) The originalLocale parameter is not defined in the corresponding .rrh file
     * (i.e. versioning highlighting currently turned off).
     *
     * Returns true otherwise. Precondition: _element should be an instance of RIMResourceElement
     *
     * @return
     */
    private boolean getEnableState() {
        String originalLocaleString = ( (RIMResourceElement) _element ).getLocale().getCollection().getOriginalLocaleName();

        if( null == originalLocaleString ) {
            return false; // versioning highlighting not currently enabled, so
            // disable this MarkTranslationCorrectAction
        } else if( originalLocaleString.equals( ResourceEditorOptionsDialog.ROOT )
                && ( (RIMResourceElement) _element ).getLocale().getLocaleName().equals( "" ) ) {
            return false; // never enable action if user selected resource value
            // is from the original locale
        } else if( originalLocaleString.equals( ( (RIMResourceElement) _element ).getLocale().getLocaleName() ) ) {
            return false; // never enable action if user selected resource value
            // is from the original locale
        } else {
            ResourceEditorOptionsDialog.generateOriginalLocaleHashtable( _element.getLocale() );
            if( !ResourceEditorOptionsDialog.getOriginalLocaleHashtable()
                    .containsKey( ( (RIMResourceElement) _element ).getKey() ) ) {
                return false; // original locale resource value corresponding to
                // key of _element is not defined
            }

            String originalLocaleResourceValueChecksum = ResourceEditorOptionsDialog.getOriginalLocaleHashtable()
                    .get( _element.getKey() ).toString();
            // checksum of the resource value in original locale whose key is
            // the same as the key for _element
            String selectedResourceValueChecksum = "0x"
                    + Long.toHexString( ( (RIMResourceElement) _element ).getHash() ).toUpperCase();

            if( originalLocaleResourceValueChecksum.equals( selectedResourceValueChecksum ) ) {
                return false; // user selected resource value already has
                // correct translation
            }
        }
        return true;
    }

    public void run() {
        if( _element instanceof RIMResourceElement ) {
            RIMResourceLocale locale = ( (RIMResourceElement) _element ).getLocale();
            if( locale.isRrcFileWritable() == false ) {
                if( MessageDialog.openQuestion( null, "Resource Editor", locale.getRrcFilename()
                        + " is read-only.\nDo you want to mark it read-write?" ) ) {
                    locale.setRrcFileWritable();
                    locale.getCollection().setHeaderFileWritable();
                    markTranslationCorrect( _element ); // activate this action
                    // after setting file
                    // writable
                } else {
                    MessageDialog.openInformation( null, "Resource Editor", "You will not be able to save changes to "
                            + locale.getRrcFilename() + "." );
                    return;
                }
            } else {
                markTranslationCorrect( _element ); // activate this action
                // (file already writable)
            }
        } else {
            markTranslationCorrect( _element ); // _element not an instance of
            // RIMResourceElement
        }
    }

    /**
     * Method updates the hash of the element along with its versioning highlighting.
     *
     * @param element
     */
    private void markTranslationCorrect( ResourceElement element ) {
        if( element instanceof RIMResourceElement ) {

            // Step 1: Retrieve originalLocale parameter from .rrh file
            // belonging to the
            // RIMResourceCollection associated with _element
            _originalLocaleString = ( (RIMResourceElement) element ).getLocale().getCollection().getOriginalLocaleName();

            if( _originalLocaleString.equals( ResourceEditorOptionsDialog.ROOT ) ) {
                _originalLocaleString = "";
            }

            // Step 2: Update hash of user selected element with appropriate
            // checksum value
            Object originalLocaleResourceValue = ( (RIMResourceElement) element ).getLocale().getCollection()
                    .getLocale( _originalLocaleString ).getResourceElement( element.getKey() ).getValue();
            // resource value associated with _element's key *as defined* in
            // original locale
            ( (RIMResourceElement) element ).setHash( ResourceEditorOptionsDialog.getChecksum( originalLocaleResourceValue
                    .toString() ) );
            // Resource Editor now in dirty state since we updated hash of user
            // selected RIMResourceElement

            // Step 3: Now refresh versioning highlighting
            ResourceEditorOptionsDialog.updateVersioningForResourceElementOnly( false, element );
        }
    }
}
