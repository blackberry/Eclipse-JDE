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
package net.rim.ejde.internal.ui.wizards.templates;

import java.util.Iterator;

import net.rim.ejde.internal.ui.wizards.BlackBerryProjectWizardPageTwo;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardSelectionPage;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.pde.internal.ui.elements.ListContentProvider;
import org.eclipse.pde.internal.ui.parts.FormBrowser;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.ui.IBasePluginWizard;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;

public class BBTemplateSelectionPage extends WizardSelectionPage implements ISelectionChangedListener {

    private BlackBerryProjectWizardPageTwo fContentPage;
    private String fInitialTemplateId;

    protected TableViewer wizardSelectionViewer;
    protected ElementList wizardElements;
    private WizardSelectedAction doubleClickAction = new WizardSelectedAction();

    private String label;
    private FormBrowser descriptionBrowser;
    private boolean _firstTimeShow = true;

    private class WizardSelectedAction extends Action {
        public WizardSelectedAction() {
            super( "wizardSelection" ); //$NON-NLS-1$
        }

        public void run() {
            selectionChanged( new SelectionChangedEvent( wizardSelectionViewer, wizardSelectionViewer.getSelection() ) );
            advanceToNextPage();
        }
    }

    static class WizardFilter extends ViewerFilter {
        public boolean select( Viewer viewer, Object parentElement, Object element ) {
            return true;
        }

    }

    /**
     * Constructor
     *
     * @param wizardElements
     *            a list of TemplateElementWizard objects
     * @param page
     *            content wizard page
     * @param message
     *            message to provide to the user
     */
    public BBTemplateSelectionPage( ElementList wizardElements, BlackBerryProjectWizardPageTwo page, String message ) {
        super( "List Selection" );
        this.wizardElements = wizardElements;
        this.label = message;
        fContentPage = page;
        descriptionBrowser = new FormBrowser( SWT.BORDER | SWT.V_SCROLL );
        descriptionBrowser.setText( "" ); //$NON-NLS-1$
        setTitle( Messages.BBTemplateSelectionPage_title );
        setDescription( Messages.BBTemplateSelectionPage_desc );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.pde.internal.ui.wizards.WizardListSelectionPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    public void createControl( Composite parent ) {
        Composite container = new Composite( parent, SWT.NONE );
        GridLayout layout = new GridLayout();
        layout.verticalSpacing = 10;
        container.setLayout( layout );
        container.setLayoutData( new GridData( GridData.FILL_BOTH ) );

        createAbove( container, 1 );
        Label label = new Label( container, SWT.NONE );
        label.setText( getLabel() );
        GridData gd = new GridData();
        label.setLayoutData( gd );

        SashForm sashForm = new SashForm( container, SWT.HORIZONTAL );
        gd = new GridData( GridData.FILL_BOTH );
        gd.widthHint = 300;
        sashForm.setLayoutData( gd );

        wizardSelectionViewer = new TableViewer( sashForm, SWT.BORDER );
        wizardSelectionViewer.setContentProvider( new ListContentProvider() );
        wizardSelectionViewer.setLabelProvider( ListUtil.TABLE_LABEL_PROVIDER );
        // don't sort the template list
        // wizardSelectionViewer.setComparator( ListUtil.NAME_COMPARATOR );
        wizardSelectionViewer.addDoubleClickListener( new IDoubleClickListener() {
            public void doubleClick( DoubleClickEvent event ) {
                doubleClickAction.run();
            }
        } );
        createDescriptionIn( sashForm );
        createBelow( container, 1 );
        initializeViewer();
        wizardSelectionViewer.setInput( wizardElements );
        wizardSelectionViewer.addSelectionChangedListener( this );
        Dialog.applyDialogFont( container );
        setControl( container );

        PlatformUI.getWorkbench().getHelpSystem().setHelp( getControl(), IHelpContextIds.NEW_PROJECT_CODE_GEN_PAGE );
    }

    public void createAbove( Composite container, int span ) {
        // do nothing
    }

    protected void initializeViewer() {
        wizardSelectionViewer.addFilter( new WizardFilter() );
        if( getInitialTemplateId() != null )
            selectInitialTemplate();
    }

    private void selectInitialTemplate() {
        Object[] children = wizardElements.getChildren();
        for( int i = 0; i < children.length; i++ ) {
            BBWizardElement welement = (BBWizardElement) children[ i ];
            if( welement.getID().equals( getInitialTemplateId() ) ) {
                wizardSelectionViewer.setSelection( new StructuredSelection( welement ), true );
                setSelectedNode( createWizardNode( welement ) );
                setDescriptionText( welement.getDescription() );
                break;
            }
        }
    }

    protected IWizardNode createWizardNode( BBWizardElement element ) {
        return new BBWizardNode( this, element ) {
            public IBasePluginWizard createWizard() throws CoreException {
                IPluginContentWizard wizard = (IPluginContentWizard) wizardElement.createExecutableExtension();
                wizard.init( fContentPage.getData() );
                return wizard;
            }
        };
    }

    public IPluginContentWizard getSelectedWizard() {
        IWizardNode node = getSelectedNode();
        if( node != null ) {
            return (IPluginContentWizard) node.getWizard();
        }
        return null;
    }

    public boolean isPageComplete() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.wizard.WizardSelectionPage#canFlipToNextPage()
     */
    public boolean canFlipToNextPage() {
        IStructuredSelection ssel = (IStructuredSelection) wizardSelectionViewer.getSelection();
        if( ssel != null && !ssel.isEmpty() ) {
            return true;
        }
        return false;
    }

    /**
     * @return Returns the fInitialTemplateId.
     */
    public String getInitialTemplateId() {
        return fInitialTemplateId;
    }

    /**
     * @param initialTemplateId
     *            The fInitialTemplateId to set.
     */
    public void setInitialTemplateId( String initialTemplateId ) {
        fInitialTemplateId = initialTemplateId;
    }

    public void setVisible( boolean visible ) {
        if( visible ) {
            wizardSelectionViewer.refresh();
        }
        super.setVisible( visible );
        if( visible && _firstTimeShow ) {
            _firstTimeShow = false;
            focusAndSelectFirst();
        }
    }

    /**
     * @return Returns <code>false</code> if no Template is available, and <code>true</code> otherwise.
     */
    public boolean isAnyTemplateAvailable() {
        if( wizardSelectionViewer != null ) {
            wizardSelectionViewer.refresh();
            Object firstElement = wizardSelectionViewer.getElementAt( 0 );
            if( firstElement != null ) {
                return true;
            }
        }
        return false;
    }

    protected void createBelow( Composite container, int span ) {
    }

    public void selectionChanged( SelectionChangedEvent event ) {
        setErrorMessage( null );
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        BBWizardElement currentWizardSelection = null;
        Iterator iter = selection.iterator();
        if( iter.hasNext() )
            currentWizardSelection = (BBWizardElement) iter.next();
        if( currentWizardSelection == null ) {
            setDescriptionText( "" ); //$NON-NLS-1$
            setSelectedNode( null );
            return;
        }
        final BBWizardElement finalSelection = currentWizardSelection;
        setSelectedNode( createWizardNode( finalSelection ) );
        setDescriptionText( finalSelection.getDescription() );
        getContainer().updateButtons();
    }

    public IWizardPage getNextPage( boolean shouldCreate ) {
        if( !shouldCreate )
            return super.getNextPage();
        IWizardNode selectedNode = getSelectedNode();
        selectedNode.dispose();
        IWizard wizard = selectedNode.getWizard();
        if( wizard == null ) {
            super.setSelectedNode( null );
            return null;
        }
        if( shouldCreate )
            // Allow the wizard to create its pages
            wizard.addPages();
        return wizard.getStartingPage();
    }

    protected void focusAndSelectFirst() {
        Table table = wizardSelectionViewer.getTable();
        table.setFocus();
        TableItem[] items = table.getItems();
        if( items.length > 0 ) {
            TableItem first = items[ 0 ];
            Object obj = first.getData();
            wizardSelectionViewer.setSelection( new StructuredSelection( obj ) );
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement,
     * java.lang.String, java.lang.Object)
     */
    public void setInitializationData( IConfigurationElement config, String propertyName, Object data ) throws CoreException {
    }

    public void createDescriptionIn( Composite composite ) {
        descriptionBrowser.createControl( composite );
        Control c = descriptionBrowser.getControl();
        GridData gd = new GridData( GridData.FILL_BOTH );
        gd.widthHint = 200;
        c.setLayoutData( gd );
    }

    public String getLabel() {
        return label;
    }

    public void setDescriptionText( String text ) {
        if( text == null )
            text = PDEUIMessages.BaseWizardSelectionPage_noDesc;
        descriptionBrowser.setText( text );
    }

    public void setDescriptionEnabled( boolean enabled ) {
        Control dcontrol = descriptionBrowser.getControl();
        if( dcontrol != null )
            dcontrol.setEnabled( enabled );
    }

    public void advanceToNextPage() {
        getContainer().showPage( getNextPage() );
    }

    public ElementList getWizardElements() {
        return wizardElements;
    }
}
