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
import net.rim.ejde.internal.model.BasicBlackBerryProperties.Icon;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.util.ProjectUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.ui.templates.TemplateOption;

/**
 * Blank BlackBerry Library template.
 *
 * @author dmeng
 */
public class BlankLibraryTemplate extends AbstractBBTemplateSection {

    public static final String SECTION_ID = "emptyLib"; //$NON-NLS-1$
    public static final String HELP_CONTEXT_ID = "net.rim.ejde.doc.template_library"; //$NON-NLS-1$
    public static final String KEY_CLASS_NAME = "appClassName"; //$NON-NLS-1$
    public static final String PACKAGE_NAME = "mypackage"; //$NON-NLS-1$
    public static final String CLASS_NAME = "MyLib"; //$NON-NLS-1$

    /**
     * Construct BlankLibraryTemplate.
     */
    public BlankLibraryTemplate() {
        setPageCount( 1 );
        createOptions();
    }

    @Override
    public String getSectionId() {
        return SECTION_ID;
    }

    @Override
    public int getNumberOfWorkUnits() {
        return super.getNumberOfWorkUnits() + 1;
    }

    private void createOptions() {
        addOption( KEY_PACKAGE_NAME, "&Package Name:", PACKAGE_NAME, 0 );
        addOption( KEY_CLASS_NAME, "&Application Class Name:", CLASS_NAME, 0 );
    }

    @Override
    public void addPages( Wizard wizard ) {
        WizardPage page = createPage( 0, HELP_CONTEXT_ID );
        page.setTitle( "Application Details" );
        page.setDescription( "This template will generate an empty BlackBerry library." );
        wizard.addPage( page );
        markPagesAdded();
    }

    @Override
    public void updateModel( IProgressMonitor monitor ) throws CoreException {
        // set library type in application descriptor
        BlackBerryProject bbProject = ProjectUtils.createBBProject( project );
        BlackBerryProperties bbProperties = bbProject.getProperties();
        bbProperties._application.setType( BlackBerryProject.LIBRARY );
        bbProperties._application.setIsSystemModule( true );
        Icon[] icons = new Icon[ 1 ];
        icons[ 0 ] = new Icon( "res\\img\\icon.png", false );
        bbProperties._resources.setIconFiles( icons );
        bbProperties._compile.setConvertImages( true );
        ContextManager.PLUGIN.setBBProperties( project.getName(), bbProperties, false );
    }

    @Override
    public void validateOptions( TemplateOption changed ) {
        // make sure the package name and class name are valid
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
            }
        }
        updateStatus( page, names, status );
    }
}
