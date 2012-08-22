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
 * The MarkTranslationIncorrectAction defines the action associated with marking the translation of a resource value within a
 * resource locale as being incorrect with respect to the original locale. It is activated from the context menu in the Resource
 * Editor. Correct translations for resource values are not highlighted, whereas incorrect translations are highlighted.
 */
class MarkTranslationIncorrectAction extends Action {
    private ResourceElement _element;
    private String _originalLocaleString = "";

    MarkTranslationIncorrectAction( ResourceElement element ) {
        super( "Mark Translation Incorrect" );
        _element = element;

        // Decide whether to enable or disable this action
        if( element instanceof RIMResourceElement ) {
            try {
                setEnabled( getEnableState() );
            } catch( Exception e ) {
            }
        }
    }

    /**
     * Helper method determines whether this MarkTranslationIncorrectAction should be enabled
     *
     * Returns true only if the user selected resource value is initially marked correct. Returns false otherwise.
     *
     * @return
     */
    private boolean getEnableState() {
        String originalLocaleString = ( (RIMResourceElement) _element ).getLocale().getCollection().getOriginalLocaleName();

        if( null == originalLocaleString ) {
            return false; // versioning highlighting not currently enabled, so
            // disable MarkTranslationIncorrectAction
        } else if( originalLocaleString.equals( ResourceEditorOptionsDialog.ROOT )
                && ( (RIMResourceElement) _element ).getLocale().getLocaleName().equals( "" ) ) {
            return false;
            // never enable action if user selected ResourceElement is from the
            // original locale
        } else if( originalLocaleString.equals( ( (RIMResourceElement) _element ).getLocale().getLocaleName() ) ) {
            return false;
            // never enable action if user selected ResourceElement is from the
            // original locale
        } else {
            ResourceEditorOptionsDialog.generateOriginalLocaleHashtable( _element.getLocale() );
            if( !ResourceEditorOptionsDialog.getOriginalLocaleHashtable()
                    .containsKey( ( (RIMResourceElement) _element ).getKey() ) ) {
                return false;
                // original locale resource value corresponding to key of
                // _element is not defined
            }

            String originalLocaleResourceValueChecksum = ResourceEditorOptionsDialog.getOriginalLocaleHashtable()
                    .get( _element.getKey() ).toString();
            // checksum of the resource value in original locale whose key is
            // the same as the key for _element
            String selectedResourceValueChecksum = "0x"
                    + Long.toHexString( ( (RIMResourceElement) _element ).getHash() ).toUpperCase();

            if( originalLocaleResourceValueChecksum.equals( selectedResourceValueChecksum ) ) {
                return true; // current user selected resource value initially
                // has correct translation
            }
        }
        return false;
    }

    public void run() {
        if( _element instanceof RIMResourceElement ) {
            RIMResourceLocale locale = (RIMResourceLocale) _element.getLocale();
            if( locale.isRrcFileWritable() == false ) {
                if( MessageDialog.openQuestion( null, "Resource Editor", locale.getRrcFilename()
                        + " is read-only.\nDo you want to mark it read-write?" ) ) {
                    locale.setRrcFileWritable();
                    locale.getCollection().setHeaderFileWritable();
                    markTranslationIncorrect( _element ); // activate this
                    // action after
                    // setting file
                    // writable

                } else {
                    MessageDialog.openInformation( null, "Resource Editor", "You will not be able to save changes to "
                            + locale.getRrcFilename() + "." );
                    return;
                }
            } else {
                markTranslationIncorrect( _element ); // activate this action
                // (file already writable)
            }
        } else {
            markTranslationIncorrect( _element ); // _element not an instance of
            // RIMResourceElement
        }
    }

    /**
     * Method updates the hash of the element along with its versioning highlighting.
     *
     * @param element
     */
    private void markTranslationIncorrect( ResourceElement element ) {
        if( element instanceof RIMResourceElement ) {

            // Step 1: Retrieve originalLocale parameter from .rrh file
            // belonging to the
            // RIMResourceCollection associated with _element

            _originalLocaleString = ( (RIMResourceElement) element ).getLocale().getCollection().getOriginalLocaleName();

            if( _originalLocaleString.equals( ResourceEditorOptionsDialog.ROOT ) ) {
                _originalLocaleString = "";
            }

            // Step 2: Update the hash (to #0) of user selected resource element
            ( (RIMResourceElement) element ).setHash( 0 );
            // Resource Editor now in dirty state since we updated hash of user
            // selected RIMResourceElement

            // Step 3: Now refresh versioning highlighting
            ResourceEditorOptionsDialog.updateVersioningForResourceElementOnly( true, element );
        }
    }
}
