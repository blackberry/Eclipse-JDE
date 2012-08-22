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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.Adler32;

import net.rim.ejde.internal.util.Messages;
import net.rim.sdk.resourceutil.RIMResourceCollection;
import net.rim.sdk.resourceutil.RIMResourceElement;
import net.rim.sdk.resourceutil.RIMResourceLocale;
import net.rim.sdk.resourceutil.ResourceConstants;
import net.rim.sdk.resourceutil.ResourceElement;
import net.rim.sdk.resourceutil.ResourceLocale;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

/**
 * The options dialog used in the resource editor.
 *
 * @author jkeshavarzi
 *
 */
public class ResourceEditorOptionsDialog extends Dialog implements IDisplayable {
    /*
     * High Level Details: The Resource Editor options dialog allows the user to perform the following actions:
     *
     * 1) Resource Package - Allows for changing of package name within the .rrh file in the resource bundle from which this
     * options dialog was activated from (i.e. if the user opens HelloWorldRes_en.rrc, the package name in HelloWorldRes.rrh can
     * be changed)
     *
     * 2) Use versioning highlighting from a given resource - Allows user to mark language translations as being correct/incorrect
     * with respect to an original locale (user's native language locale)
     *
     * 3) Set in translator mode - Allows user to mark all resource translations in a particular locale as being correct (versus
     * marking each one individually correct). When this mode is activated, the "Validate" button beside the Options button in the
     * Resource Editor changes to "Mark all correct".
     *
     * 4) Match resource bundle when validating resources - This setting affects two user actions: Find Key (right click to
     * activate from context menu) and Validate. If it is turned on and "Find Key" has been initiated, then the system will search
     * for that key which matches the resource bundle in the workspace's .java files. If the user does a validate on a
     * HelloWorldRes resource bundle, the system will search for all textual instances of "HelloWorldResResource.keyname" where
     * keyname is a resource key in the resource bundle)
     *
     * Terminology: (search "translation support" in JDE online help for further details) original locale: native language locale
     * as defined by the user to assist in managing resource translations originalLocale: name of the parameter within an .rrh
     * file that represents the original locale strict mode: turned on when
     * "Match resource bundle when validating resources (strict)" has been activated
     */

    private File _rrhFile; // .rrh file belonging to resource bundle currently
    // open in resource editor (i.e. from which this options dialog is activated)
    private String _rrhFilePackageName = ""; // original package name of .rrh file
    private String[] _localeNames; // names of resource locales belonging to
    // this resource bundle (e.g. en, fr) to populate
    // "Use versioning highlighting from given resource" dropdown widget

    private String _newRrhFilePackageName = ""; // new package name as entered by user
    private RIMResourceLocale _locale; // object corresponding to the locale currently
    // open in the Resource Editor from which this options dialog was activated

    private boolean _wasVersioningHighlighting = false; // true if original locale
    // existed in .rrh file
    private boolean _isVersioningHighlighting = false; // true if user activates
    // "Use versioning highlighting from given resource"
    private String _oldOriginalLocale = ""; // initial value of "originalLocale"
    // in .rrh file prior to user changes
    private String _newOriginalLocale = ""; // new value of "originalLocale" as
    // selected by user

    private static Hashtable _originalLocaleHashtable = new Hashtable(); // resource keys
    // in original locale and Adler-32 hex checksum corresponding values
    // see generateOriginalLocaleHashtable(ResourceLocale) method

    private static boolean _isTranslatorModeSet = false; // true if
    // "Set in Translator" mode is activated
    private static boolean _isStrictModeSet = false; // true if
    // "Match resource bundle when validating resources (strict)" is activated
    private Button _translatorModeButton = null;
    private Button _matchResourceBundleButton = null;

    // Static Constant Labels
    public static final String PACKAGE = "package"; // package keyword that
    // appears in .rrh files
    public static final String ROOT = "Root"; // used as a text label to represent
    // the root locale in "Use versioning highlighting from given resource" dropdown
    public static final String ORIGINAL_LOCALE = "originalLocale"; // name of originalLocale
    // parameter used in underlying .rrh file
    public static final String HEX_PREFIX = "0x"; // hexadecimal prefix

    private static final Logger log = Logger.getLogger( ResourceEditorOptionsDialog.class );

    // Constructor
    public ResourceEditorOptionsDialog( RIMResourceLocale locale ) {
        super( new Shell() );
        _rrhFile = new File( locale.getCollection().getRrhFileAbsolutePath() );
        _rrhFilePackageName = locale.getCollection().getRrhPackageName();
        _localeNames = locale.getCollection().getLocaleNames();

        _newRrhFilePackageName = _rrhFilePackageName; // value will change if
        // user modifies package name

        _locale = locale;
    }

    protected void configureShell( Shell newShell ) {
        super.configureShell( newShell );
        newShell.setText( "Resource Editor Options" ); // Title of Resource Editor Options Dialog
    }

    /**
     * Generates a hash table that maps resource keys to the hexadecimal Adler-32 checksums of the corresponding resource values.
     * Note: This is done for the user defined original locale only. Used to assist in versioning highlighting (marking
     * translations correct/incorrect).
     *
     * e.g. if HelloWorld_en.rrc contains the key-value pair: HELLOWORLD_CONTENTSTRING#0="Hello World!"; and is defined as the
     * original locale, then:
     *
     * The generated hash table would have key = "HELLOWORLD_CONTENTSTRING" and value = "0x670A063B"
     *
     * since Adler-32 checksum value (in hex) of "Hello World!" is equal to 0x670A063B
     *
     * @param locale
     */
    protected static void generateOriginalLocaleHashtable( ResourceLocale locale ) {
        if( locale instanceof RIMResourceLocale ) {
            String originalLocaleString = ( (RIMResourceLocale) locale ).getCollection().getOriginalLocaleName();
            // equal to "Root" if root locale is original locale

            if( null == originalLocaleString ) {
                return;
                // originalLocale parameter does not exist in .rrh file (i.e. versioning
                // highlighting is turned off) so we do not generate the hash table
            }

            // Retrieve all RIMResourceElements from original locale
            RIMResourceLocale originalLocale = null;
            if( originalLocaleString.equals( ROOT ) ) {
                originalLocale = ( (RIMResourceLocale) locale ).getCollection().getLocale( ResourceConstants.ROOT_LOCALE );
                // Root locale is a special case
            } else {
                originalLocale = ( (RIMResourceLocale) locale ).getCollection().getLocale( originalLocaleString );
            }

            _originalLocaleHashtable.clear();
            RIMResourceElement[] elements = originalLocale.getResourceElements();
            for( int i = 0; i < elements.length; i++ ) {
                _originalLocaleHashtable.put( elements[ i ].getKey(), getChecksumInHex( elements[ i ].getValue().toString() ) );
            }
        }
    }

    /**
     * Helper method used to compute checksum for a string and returns result in hexadecimal prepended with "0x" (using Adler-32
     * checksum algorithm)
     *
     * @param string
     * @return
     */
    public static String getChecksumInHex( String string ) {
        Adler32 hashMaker = new Adler32();
        try {
            hashMaker.update( string.getBytes( "utf-16" ) );
        } catch( UnsupportedEncodingException e ) {
        }
        return HEX_PREFIX + Long.toHexString( hashMaker.getValue() ).toUpperCase();
    }

    /**
     * Helper method used to compute checksum for a string and returns result as a long value (using Adler-32 checksum algorithm)
     *
     * @param string
     * @return
     */
    public static long getChecksum( String string ) {
        Adler32 hashMaker = new Adler32();
        try {
            hashMaker.update( string.getBytes( "utf-16" ) );
        } catch( UnsupportedEncodingException e ) {
        }
        return hashMaker.getValue();
    }

    protected void okPressed() {
        /**
         * This method handles all actions that need to be performed when the OK button is pressed.
         */

        // Check for changes to package name in package statement
        // Fix for DPI221974
        if( !checkRrhPackageName() ) {
            return;
        }

        // Check for changes to
        // "Use versioning highlighting from given resource"
        checkVersioningHighlighting();

        // Check for changes to "Set in translator mode"
        checkTranslatorMode();

        // Check for changes to
        // "Match resource bundle when validating resources (strict)"
        checkStrictMode();

        super.okPressed();
    }

    /**
     * Helper method used to handle all user changes to "Use versioning highlighting from given resource"
     */
    private void checkVersioningHighlighting() {
        /*
         * Case 1: User turns OFF versioning highlighting when it is initially activated OR user activates versioning and selects
         * first blank item in locale dropdown (this is in effect the same as removing versioning)
         */
        if( ( false == _isVersioningHighlighting && true == _wasVersioningHighlighting )
                || ( _newOriginalLocale.equals( "" ) && true == _isVersioningHighlighting ) ) {
            String originalLocaleString = _oldOriginalLocale;
            if( null == originalLocaleString ) {
                originalLocaleString = ""; // original locale not defined
            }
            boolean proceed = MessageDialog
                    .openQuestion(
                            null,
                            "Resource Editor",
                            "Removing '"
                                    + originalLocaleString
                                    + "' as the default native language will affect the versioning highlighting in the resource editor. Are you sure you want to do this?" );
            if( proceed ) {
                if( !originalLocaleString.equals( "" ) ) {
                    _locale.setOriginalLocale( null ); // removes original
                    // locale for this
                    // resource bundle
                }
                ResourceEditorOptionsDialog.updateVersioningForResourceEditor( false, _locale ); // update
                // versioning
                // colors
            }
        }
        /*
         * Case 2: User selects first blank item in locale dropdown
         */
        else if( _newOriginalLocale.equals( "" ) && true == _isVersioningHighlighting ) {
            // Do nothing (invalid choice for locale)
        }
        /*
         * Case 3: User changes existing original locale to a different locale
         */
        else if( true == _isVersioningHighlighting && true == _wasVersioningHighlighting
                && !_oldOriginalLocale.equals( _newOriginalLocale ) ) {
            boolean proceed = MessageDialog
                    .openQuestion(
                            null,
                            "Resource Editor",
                            "Changing the default native language will affect the versioning highlighting in the resource editor. Are you sure you want to change the native resource from '"
                                    + _oldOriginalLocale + "'?" );
            if( proceed ) {
                _locale.setOriginalLocale( _newOriginalLocale ); // set original
                // locale to
                // new locale
                // selected by
                // user
                ResourceEditorOptionsDialog.updateVersioningForResourceEditor( true, _locale ); // update
                // versioning
                // colors
            }
        }
        /*
         * Case 4: User activates versioning highlighting when it is initially turned off
         */
        else if( true == _isVersioningHighlighting && false == _wasVersioningHighlighting ) {
            _locale.setOriginalLocale( _newOriginalLocale );
            ResourceEditorOptionsDialog.updateVersioningForResourceEditor( true, _locale );
        }
    }

    /**
     * Helper method used to handle all user changes to "Set in translator mode"
     */
    private void checkTranslatorMode() {
        _isTranslatorModeSet = _translatorModeButton.getSelection();
        Vector< ResourceEditorPage > pages = ResourceEditor.getResourceEditorPages();
        for( int i = 0; i < pages.size(); i++ ) {
            try {
                pages.get( i ).updateModeForValidateButton();
            } catch( Exception e ) {
            }
        }
    }

    /**
     * Helper method used to handle all user changes to "Match resource bundle when validating resources"
     */
    private void checkStrictMode() {
        _isStrictModeSet = _matchResourceBundleButton.getSelection();
    }

    /**
     * Checks to see if a segment of the package identifier is "package"
     *
     * @param The
     *            package that needs to be tested
     * @return False when one segment of the package is "package"
     */

    private boolean checkForPackageInIdentifier( String name ) {
        // Split @ .
        String[] splitName = name.split( "\\." );
        for( String s : splitName ) {
            if( s.toLowerCase().equals( "package" ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Helper method used to change package name in package statement within .rrh file (if necessary)
     */
    private boolean checkRrhPackageName() {
        // Fix for DPI221974 && Fix for MKS1045766
        if( !_newRrhFilePackageName.matches( "([A-Za-z]{1}[A-Za-z0-9]*)([.]{1}([A-Za-z]{1}[A-Za-z0-9]*))*" )
                || ( !checkForPackageInIdentifier( _newRrhFilePackageName ) ) ) {
            String message = NLS.bind( Messages.INVALID_PACKAGE_TEXT, _newRrhFilePackageName );
            MessageDialog.openError( null, Messages.INVALID_PACKAGE_TITLE, message );
            return false;
        } else {
            RIMResourceCollection collection = _locale.getCollection();
            if( !( ( _rrhFilePackageName.trim() ).equals( _newRrhFilePackageName.trim() ) ) ) {
                File rrhFile = new File( collection.getRrhFileAbsolutePath() );
                if( !rrhFile.canWrite() ) {
                    String message = NLS.bind( Messages.FILE_READ_ONLY_TEXT, rrhFile.getName() );
                    if( MessageDialog.openQuestion( null, Messages.MessageDialog_title, message ) ) {
                        collection.setHeaderFileWritable();
                        collection.setRrhPackageName( _newRrhFilePackageName.trim() );
                        return true;
                    }
                    return false;
                }
                // Fix for 368943, save file then close currently opened tab
                // Close REF editor
                IWorkbench workbench = PlatformUI.getWorkbench();
                IWorkbenchPage page = workbench.getActiveWorkbenchWindow().getActivePage();
                IEditorPart part = page.getActiveEditor();
                if( MessageDialog.openQuestion( null, "Save changes to file?",
                        "This will move the file to a different directory. Are you sure you want to perform this action?" ) ) {
                    collection.setRrhPackageName( _newRrhFilePackageName.trim() );
                    page.closeEditor( part, true );
                }
            }

            return true;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    protected Control createDialogArea( Composite parent ) {
        /**
         * This method handles the creation of all UI elements in the options dialog
         */

        Composite container = (Composite) super.createDialogArea( parent );
        container.setLayout( new GridLayout( 1, false ) );
        final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = SWT.FILL;

        // Label widget for .rrh file package name
        Label packageNameLabel = new Label( container, SWT.NONE );
        packageNameLabel.setText( "Resource package (ex. com.myco.foo)" );

        // Text widget for .rrh file package name
        final Text packageNameText = new Text( container, SWT.BORDER );
        packageNameText.setText( _rrhFilePackageName );
        packageNameText.setLayoutData( data );
        packageNameText.addModifyListener( new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
                _newRrhFilePackageName = packageNameText.getText(); // new
                // package
                // name
                // entered
                // by user
            }
        } );

        // Button (checkbox) widget for
        // "Use versioning highlighting from given resource"
        final Button versioningHighlightingButton = new Button( container, SWT.CHECK );
        versioningHighlightingButton.setText( "Use versioning highlighting from given resource" );

        // Combo (dropdown) widget for
        // "Use versioning highlighting from given resource"
        final Combo versioningHighlightingCombo = new Combo( container, SWT.READ_ONLY );

        // Check if an original locale has been defined
        int indexOfOriginalLocale = -1; // index of original locale in
        // versioning dropdown
        _oldOriginalLocale = _locale.getCollection().getOriginalLocaleName();

        // Generate list of locale names to populate versioning highlighting
        // dropdown
        _localeNames[ 0 ] = ROOT; // first array item always guaranteed to be
        // root locale
        String[] displayLocales = new String[ _localeNames.length + 1 ]; // first
        // item
        // in
        // dropdown
        // to
        // be
        // blank
        displayLocales[ 0 ] = "";

        for( int i = 1; i < displayLocales.length; i++ ) {
            displayLocales[ i ] = _localeNames[ i - 1 ];

            if( displayLocales[ i ].equals( _oldOriginalLocale ) && _oldOriginalLocale != null ) {
                indexOfOriginalLocale = i; // index of versioning highlighting
                // dropdown to be selected by default
                // when options dialog activated
            }
        }

        // Populate items of versioning highlighting dropdown
        versioningHighlightingCombo.setItems( displayLocales );
        versioningHighlightingCombo.setLayoutData( data );

        // Set selection of versioning highlighting checkbox and enablement of
        // dropdown based on whether original locale has been defined
        if( indexOfOriginalLocale != -1 ) { // original locale has been defined
            versioningHighlightingCombo.select( indexOfOriginalLocale );
            versioningHighlightingCombo.setEnabled( true );
            versioningHighlightingButton.setSelection( true );
            _wasVersioningHighlighting = true;
            _isVersioningHighlighting = true;
        } else {
            versioningHighlightingCombo.setEnabled( false );
            versioningHighlightingButton.setSelection( false );
            _wasVersioningHighlighting = false;
            _isVersioningHighlighting = false;
        }

        // Update enablement of versioning dropdown based on whether versioning
        // is turned on/off
        versioningHighlightingButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                if( versioningHighlightingButton.getSelection() ) {
                    versioningHighlightingCombo.setEnabled( true );
                    _isVersioningHighlighting = true;
                } else {
                    versioningHighlightingCombo.setEnabled( false );
                    _isVersioningHighlighting = false;
                }
            }
        } );

        // Retrieve existing value of _newOriginalLocale (same as
        // _oldOriginalLocale if user does not make changes)
        if( versioningHighlightingButton.getSelection() ) {
            _newOriginalLocale = versioningHighlightingCombo.getItem( versioningHighlightingCombo.getSelectionIndex() );
        }

        versioningHighlightingCombo.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                _newOriginalLocale = versioningHighlightingCombo.getItem( versioningHighlightingCombo.getSelectionIndex() );
            }
        } );

        // Button (checkbox) widget for "Set in translator mode"
        final Button translatorModeButton = new Button( container, SWT.CHECK );
        translatorModeButton.setText( "Set in translator mode" );
        translatorModeButton.setSelection( getTranslatorMode() );
        _translatorModeButton = translatorModeButton;

        // Button (checkbox) widget for
        // "Match resource bundle when validating resources (strict)"
        Button matchResourceBundleButton = new Button( container, SWT.CHECK );
        matchResourceBundleButton.setText( "Match resource bundle when validating resources (strict)" );
        matchResourceBundleButton.setSelection( getStrictMode() );
        _matchResourceBundleButton = matchResourceBundleButton;

        // Button (checkbox) + text for future enhancement
/*        final Button useTemplateButton = new Button( container, SWT.CHECK );
        useTemplateButton.setText( "Use template with $key to drag values into source code window" );
        final Text useTemplateText = new Text( container, SWT.BORDER );
        useTemplateText.setLayoutData( data );
        useTemplateText.addModifyListener( new ModifyListener() {
            public void modifyText( ModifyEvent e ) {
            }
        } );
        useTemplateText.setEnabled( false );
        useTemplateButton.addSelectionListener( new SelectionAdapter() {
            public void widgetSelected( SelectionEvent e ) {
                if( useTemplateButton.getSelection() ) {
                    useTemplateText.setEnabled( true );
                } else {
                    useTemplateText.setEnabled( false );
                }
            }
        } );
*/
        return container;
    } // end Control createDialogArea(Composite)

    public void display() {
        // TODO Auto-generated method stub
        // method not used but required since this class implements IDisplayable
    }

    /**
     * Helper method that returns value of originalLocale parameter found in user specified .rrh file Returns null if
     * originalLocale not defined in .rrh file.
     *
     * @param rrhFile
     * @return
     */
    protected static String getOriginalLocaleString( File rrhFile ) {
        BufferedReader bufferedReader = null;
        try {
            // Read each line in rrhFile to look for originalLocale parameter
            bufferedReader = new BufferedReader( new FileReader( rrhFile ) );
            String currentLine = bufferedReader.readLine();
            String originalLocale = "";
            while( currentLine != null ) {
                currentLine = currentLine.trim();
                if( currentLine.startsWith( ORIGINAL_LOCALE ) && currentLine.endsWith( ";" ) ) { // originalLocale
                    // parameter found
                    // (i.e. versioning
                    // highlighting is
                    // turned on)
                    originalLocale = currentLine.substring( currentLine.indexOf( " " ), currentLine.indexOf( ";" ) );
                    originalLocale = originalLocale.trim();
                    return originalLocale;
                }
                currentLine = bufferedReader.readLine();
            }
        } catch( Exception e ) {
            log.error( "getOriginalLocaleString(): Error", e );
        } finally {
            if( bufferedReader != null ) {
                try {
                    bufferedReader.close();
                } catch( IOException e ) {
                    log.error( "getOriginalLocaleString(): Error closing reader", e );
                }
            }
        }

        return null;
    }

    /**
     * Helper method updates the color of the table row (in resource editor) containing element. This is used by the
     * MarkTranslationCorrectAction and MarkTranslationIncorrectAction classes
     *
     * @param highlight
     *            true if element should be marked as incorrect, false if it should be marked correct
     * @param element
     *            the ResourceElement whose row color in the resource editor needs to be updated
     *
     * @see MarkTranslationCorrectAction
     * @see MarkTranslationIncorrectAction
     */
    protected static void updateVersioningForResourceElementOnly( boolean highlight, ResourceElement element ) {
        if( element instanceof RIMResourceElement ) {
            Vector< ResourceEditorPage > pages = ResourceEditor.getResourceEditorPages();

            for( int i = 0; i < pages.size(); i++ ) {
                if( pages.get( i ).getRrcFile().getAbsolutePath()
                        .equals( ( (RIMResourceElement) element ).getLocale().getRrcFileAbsolutePath() ) ) {
                    TableViewer viewer = pages.get( i ).getTableViewer();
                    Table table = viewer.getTable();
                    Color versioningColor = getVersioningColor( table );

                    for( int j = 0; j < table.getItemCount(); j++ ) {
                        if( table.getItem( j ).getText( ResourceEditorPage.KEY_COLUMN_INDEX )
                                .equals( ( (RIMResourceElement) element ).getKey() ) ) {
                            if( highlight ) {
                                table.getItem( j ).setBackground( versioningColor );
                            } else {
                                table.getItem( j ).setBackground( table.getDisplay().getSystemColor( SWT.COLOR_WHITE ) );
                            }
                        }
                    }

                }
            }
        }
    }

    /**
     * Helper method is used to update the versioning highlighting after user modifies resources in some manner (e.g. Add Key,
     * delete/edit resource value, etc.)
     *
     * @param element
     */
    protected static void updateVersioningAfterResourceElementEdited( ResourceElement element ) {
        if( element instanceof RIMResourceElement ) {
            String originalLocaleString = ( (RIMResourceElement) element ).getLocale().getCollection().getOriginalLocaleName();

            if( originalLocaleString != null ) { // versioning highlighting
                // turned on
                ( (RIMResourceElement) element ).setHash( 0 );
                ResourceEditorOptionsDialog
                        .updateVersioningForResourceEditor( true, ( (RIMResourceElement) element ).getLocale() );
            }
        }
    }

    /**
     * Helper method used to update versioning highlighting for a TableViewer object. This is used to preserve versioning
     * highlighting after user clicks on sort indicator in resource editor to sort resource key columns/values.
     *
     * @param turnOn
     *            true if versioning highlighting is to be turned on, false if it is to be turned off
     * @param viewer
     *            the TableViewer object associated with a resource locale in the resource editor
     * @param locale
     *            the ResourceLocale associated with viewer
     *
     * @see ResourceEditorPage
     */
    protected static void updateVersioningForTableViewer( boolean turnOn, TableViewer viewer, ResourceLocale locale ) {
        String originalLocaleString = ( (RIMResourceLocale) locale ).getCollection().getOriginalLocaleName();
        Table table = viewer.getTable();
        Color versioningColor = getVersioningColor( table );

        Hashtable originalLocaleHashTable = getOriginalLocaleHashtable();

        // Reset colors in Resource Editor table widget to white prior to
        // highlighting
        for( int k = 0; k < table.getItemCount(); k++ ) {
            table.getItem( k ).setBackground( table.getDisplay().getSystemColor( SWT.COLOR_WHITE ) );
        }

        if( turnOn ) {
            for( int j = 0; j < table.getItemCount(); j++ ) {
                String resourceKey = table.getItem( j ).getText( ResourceEditorPage.KEY_COLUMN_INDEX );
                String originalLocaleChecksumHex = originalLocaleHashTable.get( resourceKey ).toString().trim();
                ResourceLocale resourceLocale = locale;
                long hash = ( (RIMResourceLocale) resourceLocale ).getResourceElement( resourceKey ).getHash();
                String currentLocaleChecksumHex = HEX_PREFIX + Long.toHexString( hash ).toUpperCase();

                if( originalLocaleChecksumHex.equals( currentLocaleChecksumHex ) ) {
                    // Mark as correct
                    table.getItem( j ).setBackground( table.getDisplay().getSystemColor( SWT.COLOR_WHITE ) );
                } else {
                    // Check if current locale is the original locale
                    boolean highlight = false; // boolean flag used to determine
                    // whether we highlight the given
                    // resource element
                    String currentLocaleName = ( (RIMResourceLocale) locale ).getLocaleName();

                    if( currentLocaleName.equals( ResourceConstants.ROOT_LOCALE ) && originalLocaleString.equals( ROOT ) ) {
                        highlight = false; // do not highlight original locale
                        // under any circumstances
                    } else if( currentLocaleName.equals( originalLocaleString ) ) {
                        highlight = false; // do not highlight original locale
                        // under any circumstances
                    } else {
                        highlight = true; // mark as incorrect
                    }

                    if( highlight ) {
                        table.getItem( j ).setBackground( versioningColor );
                    } else {
                        table.getItem( j ).setBackground( table.getDisplay().getSystemColor( SWT.COLOR_WHITE ) );
                    }
                }
            }
        }
    }

    /**
     * Helper method used to update versioning highlighting for all locales in resource bundle that locale belongs to.
     *
     * @param turnOn
     *            true if versioning highlighting is to be turned on, false otherwise
     * @param locale
     *            ResourceLocale object whose resource bundle needs to be updated on versioning highlighting
     */
    protected static void updateVersioningForResourceEditor( boolean turnOn, ResourceLocale locale ) {
        if( locale instanceof RIMResourceLocale ) {
            // Update hash table first to ensure up to date mappings for
            // resource keys and resource value
            // checksums for the original locale
            generateOriginalLocaleHashtable( locale );

            File rrhFile = new File( ( (RIMResourceLocale) locale ).getCollection().getRrhFileAbsolutePath() );
            String originalLocaleString = ( (RIMResourceLocale) locale ).getCollection().getOriginalLocaleName();
            Vector< ResourceEditorPage > pages = ResourceEditor.getResourceEditorPages();

            for( int i = 0; i < pages.size(); i++ ) {
                // Check if current ResourceEditorPage object in pages vector
                // belongs to this resource bundle
                String rrcPath = pages.get( i ).getRrcFile().getAbsolutePath(); // .
                // rrc
                // file
                // in
                // pages
                // vector
                String rrhPath = rrhFile.getAbsolutePath(); // .rrh file from
                // which this
                // options dialog
                // was activated
                // from
                int indexOfRrhExt = rrhPath.lastIndexOf( ResourceConstants.RRH_SUFFIX );
                rrhPath = rrhPath.substring( 0, indexOfRrhExt );
                rrcPath = rrcPath.substring( 0, indexOfRrhExt );

                if( !rrhPath.equals( rrcPath ) ) {
                    continue; // current ResourceEditorPage object is not in
                    // this resource bundle, so skip to next object
                    // prevents versioning highlighting from inadvertently being
                    // turned on for other resource bundles (i.e. other
                    // ResourceEditor instances)
                }

                TableViewer viewer = pages.get( i ).getTableViewer();
                Table table = viewer.getTable();
                Color versioningColor = getVersioningColor( table );
                Hashtable originalLocaleHashTable = getOriginalLocaleHashtable();

                // Reset colors in Resource Editor table widget to white prior
                // to highlighting
                for( int k = 0; k < table.getItemCount(); k++ ) {
                    table.getItem( k ).setBackground( table.getDisplay().getSystemColor( SWT.COLOR_WHITE ) );
                }

                if( turnOn ) {
                    for( int j = 0; j < table.getItemCount(); j++ ) {
                        String resourceKey = table.getItem( j ).getText( ResourceEditorPage.KEY_COLUMN_INDEX );
                        String originalLocaleChecksumHex = originalLocaleHashTable.get( resourceKey ).toString().trim();
                        ResourceLocale resourceLocale = pages.get( i ).getLocale();
                        long hash = ( (RIMResourceLocale) resourceLocale ).getResourceElement( resourceKey ).getHash();
                        String currentLocaleChecksumHex = HEX_PREFIX + Long.toHexString( hash ).toUpperCase();

                        if( originalLocaleChecksumHex.equals( currentLocaleChecksumHex ) ) {
                            // Marked correct
                            table.getItem( j ).setBackground( table.getDisplay().getSystemColor( SWT.COLOR_WHITE ) );
                        } else {
                            // Check if current locale is the original locale
                            boolean highlight = false; // boolean flag used to
                            // determine whether we
                            // highlight the given
                            // resource element
                            String currentLocaleName = ( (RIMResourceLocale) pages.get( i ).getLocale() ).getLocaleName();

                            if( currentLocaleName.equals( ResourceConstants.ROOT_LOCALE ) && originalLocaleString.equals( ROOT ) ) {
                                highlight = false; // do not highlight original
                                // locale under any
                                // circumstances
                            } else if( currentLocaleName.equals( originalLocaleString ) ) {
                                highlight = false; // do not highlight original
                                // locale under any
                                // circumstances
                            } else {
                                highlight = true;
                            }

                            // Highlight the appropriate table row in resource
                            // editor page if necessary
                            if( highlight ) {
                                table.getItem( j ).setBackground( versioningColor );
                            } else {
                                table.getItem( j ).setBackground( table.getDisplay().getSystemColor( SWT.COLOR_WHITE ) );
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns hash table used to store resource keys and checksum values of the corresponding resource values for the original
     * locale
     *
     * @return
     */
    protected static Hashtable getOriginalLocaleHashtable() {
        return _originalLocaleHashtable;
    }

    /**
     * Accessor returns whether or not "Set in translator mode" has been set in the Resource Editor options dialog
     *
     * @return
     */
    protected static boolean getTranslatorMode() {
        return _isTranslatorModeSet;
    }

    /**
     * Accessor returns whether or not "Match resource bundle when validating resources (strict)" has been set in the Resource
     * Editor options dialog
     *
     * @return
     */
    protected static boolean getStrictMode() {
        return _isStrictModeSet;
    }

    /**
     * Returns Color object used for versioning highlighting (light pink color)
     *
     * @param table
     * @return
     */
    private static Color getVersioningColor( Table table ) {
        RGB versioningRGB = new RGB( 255, 204, 204 );
        Color versioningColor;

        if( !table.isDisposed() ) {
            versioningColor = new Color( table.getDisplay(), versioningRGB );
        } else {
            versioningColor = new Color( null, versioningRGB );
        }

        return versioningColor;
    }

}
