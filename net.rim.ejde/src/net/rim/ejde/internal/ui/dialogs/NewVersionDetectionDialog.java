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
package net.rim.ejde.internal.ui.dialogs;

import net.rim.ejde.internal.util.Messages;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.about.AboutTextManager;
import org.eclipse.ui.internal.about.AboutUtils;

/**
 * New Version Detection Dialog
 *
 * @author tlai
 *
 */
@SuppressWarnings("restriction")
public class NewVersionDetectionDialog extends MessageDialog implements SelectionListener {

    private static final int DEFAULT_DAYS = 10;
    private static final int TOTAL_DAYS = 15;
    private String upgradeUrl;
    private Button _snooze;
    private Button _ignore;
    private Button _ignoreAll;
    private Button[] buttons;
    private String[] buttonLabels;
    private int defaultButtonIndex;
    private int snoozeDays;
    private Combo combo;

    /**
     * A constructor extends a constructor of MessageDialog
     *
     * @param parentShell
     * @param dialogTitle
     * @param dialogTitleImage
     * @param dialogMessage
     * @param dialogImageType
     * @param dialogButtonLabels
     * @param defaultIndex
     */
    public NewVersionDetectionDialog( Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage,
            String dialogUpgradeUrl, int dialogImageType, String[] dialogButtonLabels, int defaultIndex ) {
        super( parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, 0 );
        this.buttonLabels = dialogButtonLabels;
        this.defaultButtonIndex = defaultIndex;
        this.upgradeUrl = dialogUpgradeUrl;
    }

    protected Control createMessageArea( Composite composite ) {

        Control parent = super.createMessageArea( composite );

        final Label spacerLabel = new Label( composite, SWT.NONE );
        spacerLabel.setLayoutData( new GridData() );

        String titleLabel = Messages.BB_PLUG_IN_URL_LABEL;

        Composite noteControl = createMessageComposite( (Composite) parent, titleLabel, upgradeUrl );
        GridData gd = new GridData( GridData.HORIZONTAL_ALIGN_FILL );
        noteControl.setLayoutData( gd );

        return parent;

    }

    /**
     * add a message into message area
     *
     * @param parent
     * @param titleLabel
     * @param upgradeUrl
     * @return
     */
    private Composite createMessageComposite( Composite parent, String titleLabel, String upgradeUrl ) {

        Composite messageComposite = new Composite( parent, SWT.NONE );
        GridLayout messageLayout = new GridLayout();
        messageLayout.marginWidth = 0;
        messageLayout.marginHeight = 0;
        messageComposite.setLayout( messageLayout );
        messageComposite.setLayoutData( new GridData( GridData.HORIZONTAL_ALIGN_FILL ) );

        final Label noteLabel = new Label( messageComposite, SWT.BOLD );
        noteLabel.setText( titleLabel );
        noteLabel.setLayoutData( new GridData( GridData.VERTICAL_ALIGN_BEGINNING ) );

        StyledText text = new StyledText( messageComposite, SWT.FULL_SELECTION | SWT.MULTI | SWT.WRAP | SWT.READ_ONLY );

        text.setFont( parent.getFont() );
        GridData data = new GridData( GridData.FILL, GridData.FILL, true, true );
        text.setLayoutData( data );
        text.setBackground( messageComposite.getBackground() );
        text.setCursor( null );
        text.setFocus();

        AboutTextManager textManager = new AboutTextManager( text );
        textManager.setItem( AboutUtils.scan( upgradeUrl ) );

        return messageComposite;
    }

    /**
     * override method
     *
     * @param parent
     */
    protected void createButtonsForButtonBar( Composite parent ) {

        buttons = new Button[ buttonLabels.length ];
        String label = buttonLabels[ 0 ];
        _snooze = createButton( parent, 0, label, false );
        buttons[ 0 ] = _snooze;

        ( (GridLayout) parent.getLayout() ).numColumns++;
        combo = new Combo( parent, SWT.Selection );

        String[] days = new String[ TOTAL_DAYS ];
        days[ 0 ] = "1 " + Messages.DAY;
        for( int i = 1; i <= TOTAL_DAYS - 1; i++ ) {
            days[ i ] = i + 1 + " " + Messages.DAYS;
        }
        combo.setItems( days );
        combo.setText( Messages.DEFAULT_DAYS );

        GridData data = new GridData( GridData.HORIZONTAL_ALIGN_BEGINNING );
        combo.setLayoutData( data );
        combo.addSelectionListener( this );
        combo.pack();

        String label2 = buttonLabels[ 1 ];
        _ignore = createButton( parent, 1, label2, false );
        buttons[ 1 ] = _ignore;

        String label3 = buttonLabels[ 2 ];
        _ignoreAll = createButton( parent, 2, label3, false );
        buttons[ 2 ] = _ignoreAll;
    }

    public void widgetDefaultSelected( SelectionEvent e ) {
        // TODO Auto-generated method stub
    }

    public void widgetSelected( SelectionEvent e ) {
        String[] temp = combo.getText().split( " " );
        snoozeDays = Integer.parseInt( temp[ 0 ] );
    }

    public int getSnoozeDays() {
        if( snoozeDays == 0 ) {
            snoozeDays = DEFAULT_DAYS;
        }
        return snoozeDays;
    }

}
