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

import java.util.ArrayList;
import java.util.List;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.Icon;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ejde.internal.util.VMUtils;
import net.rim.ejde.internal.util.VMUtils.VMVersionComparator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.ui.templates.TemplateOption;

/**
 * Hello BlackBerry Template.
 *
 * @author dmeng
 */
public class HelloBlackBerryTemplate extends AbstractBBTemplateSection {

    public static final String SECTION_ID = "helloBlackBerry"; //$NON-NLS-1$
    public static final String KEY_CLASS_NAME = "appClassName"; //$NON-NLS-1$
    public static final String KEY_SCREEN_CLASS_NAME = "screenClassName"; //$NON-NLS-1$
    public static final String KEY_TITLE = "title"; //$NON-NLS-1$
    public static final String KEY_STRING_PROVIDER = "stringProvider"; //$NON-NLS-1$
    public static final String KEY_STRING_PROVIDER_IMPORT = "stringProviderImport"; //$NON-NLS-1$
    public static final String HELP_CONTEXT_ID = "net.rim.ejde.doc.template_hello_BlackBerry"; //$NON-NLS-1$

    public static final String PACKAGE_NAME = "mypackage"; //$NON-NLS-1$
    public static final String CLASS_NAME = "HelloBlackBerry"; //$NON-NLS-1$
    public static final String SCREEN_CLASS_NAME = "HelloBlackBerryScreen"; //$NON-NLS-1$
    public static final String TITLE = "HelloBlackBerry"; //$NON-NLS-1$

    /**
     * Constructor for HelloBlackBerryTemplate.
     */
    public HelloBlackBerryTemplate() {
        setPageCount( 1 );
        createOptions();
    }

    public String getSectionId() {
        return SECTION_ID;
    }

    /*
     * @see ITemplateSection#getNumberOfWorkUnits()
     */
    public int getNumberOfWorkUnits() {
        return super.getNumberOfWorkUnits() + 1;
    }

    private void createOptions() {
        addOption( KEY_PACKAGE_NAME, "&Package Name:", PACKAGE_NAME, 0 );
        addOption( KEY_CLASS_NAME, "&Application Class Name:", CLASS_NAME, 0 );
        addOption( KEY_SCREEN_CLASS_NAME, "&Screen Class Name:", SCREEN_CLASS_NAME, 0 );
        addOption( KEY_TITLE, "&Screen Title:", TITLE, 0 );
    }

    public void addPages( Wizard wizard ) {
        WizardPage page = createPage( 0, HELP_CONTEXT_ID );
        page.setTitle( "Application Details" );
        page.setDescription( "Please provide details that are used to generate the application." );
        wizard.addPage( page );
        markPagesAdded();
    }

    public void updateModel( IProgressMonitor monitor ) throws CoreException {
        // update application descriptor to use the correct icon
        BlackBerryProject bbProject = ProjectUtils.createBBProject( project );
        BlackBerryProperties bbProperties = bbProject.getProperties();
        Icon[] icons = new Icon[ 1 ];
        icons[ 0 ] = new Icon( "res\\img\\icon.png", false );
        bbProperties._resources.setIconFiles( icons );
        bbProperties._compile.setConvertImages( true );
        ContextManager.PLUGIN.setBBProperties( project.getName(), bbProperties, false );
    }

    public void validateOptions( TemplateOption changed ) {
        WizardPage page = getPage( 0 );
        TemplateOption[] options = getOptions( 0 );
        MultiStatus status = new MultiStatus( ContextManager.PLUGIN_ID, 0, "", null );
        List< String > names = new ArrayList< String >();
        for( int i = 0; i < options.length; i++ ) {
            String text = (String) options[ i ].getValue();
            if( options[ i ].getName().equals( KEY_PACKAGE_NAME ) ) {
                names.add( options[ i ].getMessageLabel() );
                status.add( JavaConventions.validatePackageName( text, JavaCore.VERSION_1_3, JavaCore.VERSION_1_3 ) );
            } else if( options[ i ].getName().equals( KEY_CLASS_NAME ) ) {
                names.add( options[ i ].getMessageLabel() );
                status.add( JavaConventions.validateJavaTypeName( text, JavaCore.VERSION_1_3, JavaCore.VERSION_1_3 ) );
            } else if( options[ i ].getName().equals( KEY_SCREEN_CLASS_NAME ) ) {
                names.add( options[ i ].getMessageLabel() );
                status.add( JavaConventions.validateJavaTypeName( text, JavaCore.VERSION_1_3, JavaCore.VERSION_1_3 ) );
            }
        }
        // Application class name and screen class name cannot be the same
        String appClassName = (String) options[ 1 ].getValue();
        String scrClassName = (String) options[ 2 ].getValue();
        if( appClassName.equals( scrClassName ) ) {
            names.add( "" );
            status.add( StatusFactory.createErrorStatus( "Application class name cannot be the same as screen class name." ) );
        }
        updateStatus( page, names, status );
    }

    /**
     * Override parent method to provide some default options.
     *
     * @see org.eclipse.pde.ui.templates.ITemplateSection#execute(IProject, IPluginModelBase, IProgressMonitor)
     */
    public void execute( IProject project, IPluginModelBase model, IProgressMonitor monitor ) throws CoreException {
        // inject default options
        IVMInstall vm = ProjectUtils.getVMForProject( JavaCore.create( project ) );
        if( vm != null ) {
            VMVersionComparator comparator = new VMVersionComparator();
            int result = comparator.compare( VMUtils.getVMVersion( vm ), IConstants.SDK_SIX_VERSION );
            // use StringProvider in 6.0 and above SDKs, and use string otherwise
            if( result >= 0 ) {
                addOption( KEY_STRING_PROVIDER, KEY_STRING_PROVIDER, "new StringProvider( \"Say Hello\" )", 0 );
                addOption( KEY_STRING_PROVIDER_IMPORT, KEY_STRING_PROVIDER_IMPORT,
                        "import net.rim.device.api.util.StringProvider;\n", 0 );
            } else {
                addOption( KEY_STRING_PROVIDER, KEY_STRING_PROVIDER, "\"Say Hello\"", 0 );
                addOption( KEY_STRING_PROVIDER_IMPORT, KEY_STRING_PROVIDER_IMPORT, "", 0 );
            }
        }
        super.execute( project, model, monitor );
    }
}
