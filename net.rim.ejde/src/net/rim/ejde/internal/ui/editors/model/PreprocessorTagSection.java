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
package net.rim.ejde.internal.ui.editors.model;

import java.util.List;

import net.rim.ejde.internal.core.ClasspathElementChangedListener;
import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.PreprocessorTag;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.ui.preferences.PreprocessDirectiveUI;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * This class creates the preprocessor tag section used in the project properties editor.
 *
 * @author jkeshavarzi
 *
 */
public class PreprocessorTagSection extends AbstractSection {
    private ProjectPreprocessDirectiveUI _preprocessDirectiveUI;
    private Composite _client;

    /**
     * Constructs the PreprocessorTagSection on the given parent composite.
     *
     * @param page
     * @param parent
     * @param toolkit
     * @param style
     */
    public PreprocessorTagSection( BlackBerryProjectPropertiesPage page, Composite parent, FormToolkit toolkit, int style ) {
        super( page, parent, page.getManagedForm().getToolkit(), ( style | Section.DESCRIPTION | ExpandableComposite.TITLE_BAR ) );
        createFormContent( getSection(), toolkit );
    }

    protected void createFormContent( Section section, FormToolkit toolkit ) {
        preBuild();

        GridData gd = new GridData( SWT.FILL, SWT.FILL, true, false );
        gd.minimumWidth = 250;
        section.setLayout( LayoutFactory.createClearGridLayout( false, 1 ) );
        section.setLayoutData( gd );

        section.setDescription( Messages.PreprocessorTagSection_Description );
        _client = toolkit.createComposite( section );
        _client.setLayout( LayoutFactory.createSectionGridLayout( false, 3 ) );
        section.setClient( _client );

        build( _client, toolkit );

        postBuild( _client, toolkit );
    }

    private void preBuild() {
        getSection().setText( Messages.BuildPrefsPage_PreprocessDefine );
    }

    private void build( final Composite body, FormToolkit toolkit ) {
        _preprocessDirectiveUI = new ProjectPreprocessDirectiveUI( body, PreprocessorTag.PJ_SCOPE, getProjectPropertiesPage()
                .getBlackBerryProject() );
        _preprocessDirectiveUI.addListener();
    }

    public ProjectPreprocessDirectiveUI getUI() {
        return _preprocessDirectiveUI;
    }

    private void postBuild( Composite body, FormToolkit toolkit ) {
        toolkit.paintBordersFor( body );
    }

    @Override
    public void commit( boolean onSave ) {
        BlackBerryProperties properties = getProjectPropertiesPage().getBlackBerryProject().getProperties();
        if( _preprocessDirectiveUI != null ) {
            List< PreprocessorTag > directives = _preprocessDirectiveUI.getScopeDirectives();
            properties._compile.setPreprocessorDefines( directives.toArray( new PreprocessorTag[ directives.size() ] ) );
        }

        super.commit( onSave );
    }

    /**
     * Update the controls within this section with values from the given properties object
     *
     * @param properties
     */
    public void insertControlValuesFromModel( BlackBerryProperties properties ) {
        if( _preprocessDirectiveUI != null ) {
            _preprocessDirectiveUI.setProject( new BlackBerryProject( getProjectPropertiesPage().getBlackBerryProject(),
                    properties ) );
            _preprocessDirectiveUI.showData();
        }
    }

    protected class ProjectPreprocessDirectiveUI extends PreprocessDirectiveUI implements IElementChangedListener {

        public ProjectPreprocessDirectiveUI( Composite parent, int scope, BlackBerryProject bbProject ) {
            super( parent, scope, bbProject );
        }

        @Override
        protected void performDefaults() {
            // TODO Auto-generated method stub

        }

        @Override
        protected void performChanged() {
            getPart().markDirty();
            getEditor().setDirty( Boolean.TRUE );
        }

        @Override
        public void elementChanged( ElementChangedEvent event ) {
            IJavaElementDelta[] children = event.getDelta().getChangedChildren();
            IProject project;
            for( int i = 0; i < children.length; i++ ) {
                project = children[ i ].getElement().getJavaProject().getProject();
                if( project.getName().equals( getProjectPropertiesPage().getBlackBerryProject().getProject().getName() )
                        && ClasspathElementChangedListener.isClasspathChangeFlag( children[ i ].getFlags() ) ) {
                    _preprocessDirectiveUI.showData();
                }
            }
        }

        public void addListener() {
            JavaRuntime.removeVMInstallChangedListener( this );// has no effect if the element exists
            JavaRuntime.addVMInstallChangedListener( this );
            JavaCore.removeElementChangedListener( this ); // has no effect if the element exists
            JavaCore.addElementChangedListener( this );
            ContextManager.getDefault().getPreferenceStore().removePropertyChangeListener( this );// has no effect if the element
            // exists
            ContextManager.getDefault().getPreferenceStore().addPropertyChangeListener( this );
        }

        public void removeListener() {
            JavaRuntime.removeVMInstallChangedListener( this );
            JavaCore.removeElementChangedListener( this );
            ContextManager.getDefault().getPreferenceStore().removePropertyChangeListener( this );
        }
    }
}
