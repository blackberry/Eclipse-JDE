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
package net.rim.ejde.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.ui.wizards.templates.BBTemplateSelectionPage;
import net.rim.ejde.internal.ui.wizards.templates.BBWizardElement;
import net.rim.ejde.internal.ui.wizards.templates.UiAppNewWizard;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.ui.IPackagesViewPart;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/**
 * This is a sample new wizard. Its role is to create a new file resource in the provided container. If the container resource (a
 * folder or a project) is selected in the workspace when the wizard is opened, it will accept it as the target container. The
 * wizard creates one file with the extension "blackberry". If a sample multi-page editor (also available as a template) is
 * registered for the same extension, it will be able to open it.
 */

public class BlackBerryProjectWizard extends BlackBerryElementWizard implements IExecutableExtension {

    public static final String PLUGIN_POINT = "projectContent"; //$NON-NLS-1$
    public static final String TAG_WIZARD = "wizard"; //$NON-NLS-1$
    public static final String DEF_TEMPLATE_ID = "template-id"; //$NON-NLS-1$

    private BlackBerryProjectWizardPageOne fFirstPage;
    private BlackBerryProjectWizardPageTwo fSecondPage;

    private IConfigurationElement fConfigElement;

    private BBTemplateSelectionPage fWizardListPage;
    private Map< String, String > defaultValues = new HashMap< String, String >();
    private IPluginContentWizard fContentWizard;

    /**
     * Default constructor.
     */
    public BlackBerryProjectWizard() {
        this( null, null );
    }

    /**
     * Constructs the wizard with given parameters.
     *
     * @param pageOne
     *            The wizard page 1
     * @param pageTwo
     *            The wizard page 2
     */
    public BlackBerryProjectWizard( BlackBerryProjectWizardPageOne pageOne, BlackBerryProjectWizardPageTwo pageTwo ) {
        setDefaultPageImageDescriptor( ContextManager.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID,
                "icons/wizban/new_bb_project_wizard.png" ) );
        setDialogSettings( JavaPlugin.getDefault().getDialogSettings() );
        setWindowTitle( Messages.BlackBerryProjectWizard_title );

        fFirstPage = pageOne;
        fSecondPage = pageTwo;
        setDefaultTemplate( UiAppNewWizard.ID );
    }

    /**
     * Add wizard pages.
     *
     * @see org.eclipse.jface.wizard.Wizard#addPages()
     */
    public void addPages() {
        if( fFirstPage == null )
            fFirstPage = new BlackBerryProjectWizardPageOne();
        addPage( fFirstPage );

        if( fSecondPage == null )
            fSecondPage = new BlackBerryProjectWizardPageTwo( fFirstPage );

        fWizardListPage = new BBTemplateSelectionPage( getAvailableCodegenWizards(), fSecondPage,
                Messages.BlackBerryProjectWizard_TemplateSelection );
        String tid = getDefaultValue( DEF_TEMPLATE_ID );
        if( tid != null )
            fWizardListPage.setInitialTemplateId( tid );

        addPage( fSecondPage );
        addPage( fWizardListPage );

        fFirstPage.init( getSelection(), getActivePart() );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#finishPage(org.eclipse .core.runtime.IProgressMonitor)
     */
    protected void finishPage( IProgressMonitor monitor ) throws InterruptedException, CoreException {
        fSecondPage.performFinish( fContentWizard, monitor ); // use the full progress monitor
    }

    /**
     * Handle when user clicks "Finish" button.
     *
     * @see org.eclipse.jface.wizard.IWizard#performFinish()
     */
    public boolean performFinish() {
        fContentWizard = fWizardListPage.getSelectedWizard();
        boolean res = super.performFinish();
        if( res ) {
            if( fSecondPage.getJavaProject() == null ) {
                return false;
            }
            final IJavaElement newElement = getCreatedElement();

            IWorkingSet[] workingSets = fFirstPage.getWorkingSets();
            if( workingSets.length > 0 ) {
                PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets( newElement, workingSets );
            }

            BasicNewProjectResourceWizard.updatePerspective( fConfigElement );
            selectAndReveal( fSecondPage.getJavaProject().getProject() );

            Display.getDefault().asyncExec( new Runnable() {
                public void run() {
                    IWorkbenchPart activePart = getActivePart();
                    if( activePart instanceof IPackagesViewPart ) {
                        PackageExplorerPart view = PackageExplorerPart.openInActivePerspective();
                        view.tryToReveal( newElement );
                    }
                }
            } );
        }
        return res;
    }

    private IWorkbenchPart getActivePart() {
        IWorkbenchWindow activeWindow = getWorkbench().getActiveWorkbenchWindow();
        if( activeWindow != null ) {
            IWorkbenchPage activePage = activeWindow.getActivePage();
            if( activePage != null ) {
                return activePage.getActivePart();
            }
        }
        return null;
    }

    protected void handleFinishException( Shell shell, InvocationTargetException e ) {
        String title = Messages.BlackBerryProjectWizard_op_error_title;
        String message = Messages.BlackBerryProjectWizard_op_error_create_message;
        ExceptionHandler.handle( e, getShell(), title, message );
    }

    /**
     * Stores the configuration element for the wizard. The config element will be used in <code>performFinish</code> to set the
     * result perspective.
     *
     * @param cfig
     *            The configuration element
     * @param propertyName
     *            The property name
     * @param data
     *            The data
     */
    public void setInitializationData( IConfigurationElement cfig, String propertyName, Object data ) {
        fConfigElement = cfig;
    }

    /**
     * Handle user clicks Cancel button.
     *
     * @see IWizard#performCancel()
     */
    public boolean performCancel() {
        fSecondPage.performCancel();
        return super.performCancel();
    }

    /**
     * Returns created element.
     *
     * @return The created element
     * @see org.eclipse.jdt.internal.ui.wizards.NewElementWizard#getCreatedElement()
     */
    public IJavaElement getCreatedElement() {
        return fSecondPage.getJavaProject();
    }

    /**
     * Crate a new wizard element.
     *
     * @param config
     *            The configuration
     * @return The new wizard element
     */
    protected BBWizardElement createWizardElement( IConfigurationElement config ) {
        String name = config.getAttribute( BBWizardElement.ATT_NAME );
        String id = config.getAttribute( BBWizardElement.ATT_ID );
        String className = config.getAttribute( BBWizardElement.ATT_CLASS );
        if( name == null || id == null || className == null )
            return null;
        BBWizardElement element = new BBWizardElement( config );
        String imageName = config.getAttribute( BBWizardElement.ATT_ICON );
        if( imageName != null ) {
            String pluginID = config.getNamespaceIdentifier();
            Image image = ContextManager.PLUGIN.getImageFromPlugin( pluginID, imageName );
            element.setImage( image );
        }
        return element;
    }

    /**
     * Returns list of available code generation wizards.
     *
     * @return The list of code generation wizards
     */
    public ElementList getAvailableCodegenWizards() {
        ElementList wizards = new ElementList( "CodegenWizards" ); //$NON-NLS-1$
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry.getExtensionPoint( ContextManager.PLUGIN_ID, PLUGIN_POINT );
        if( point == null )
            return wizards;
        IExtension[] extensions = point.getExtensions();
        for( int i = 0; i < extensions.length; i++ ) {
            IConfigurationElement[] elements = extensions[ i ].getConfigurationElements();
            for( int j = 0; j < elements.length; j++ ) {
                if( elements[ j ].getName().equals( TAG_WIZARD ) ) {
                    BBWizardElement element = createWizardElement( elements[ j ] );
                    if( element != null ) {
                        wizards.add( element );
                    }
                }
            }
        }
        return wizards;
    }

    /**
     * Returns the default value for the given key.
     *
     * @param key
     *            The key
     * @return The default value
     */
    public final String getDefaultValue( String key ) {
        if( defaultValues == null ) {
            return null;
        }
        return defaultValues.get( key );
    }

    /**
     * Sets the default template used for new project creation.
     *
     * @param tid
     *            The template id
     */
    public void setDefaultTemplate( String tid ) {
        defaultValues.put( DEF_TEMPLATE_ID, tid );
    }
}
