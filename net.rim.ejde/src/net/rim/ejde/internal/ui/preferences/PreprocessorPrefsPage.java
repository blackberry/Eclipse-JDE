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

import java.util.List;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.PreprocessorTag;
import net.rim.ejde.internal.model.preferences.PreprocessorPreferences;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class PreprocessorPrefsPage extends BasicPrefsPage {
    private WorkspacePreprocesDirectiveUI _preprocessDirectiveUI;
    private Button _popForPreprocessHookMissing;

    /**
     * Creates and returns the SWT control for the customized body of this property page under the given parent composite.
     *
     * @param parent
     *            the parent composite
     * @return the new control
     */
    @Override
    protected Control createContents( Composite parent ) {
        // preprocess label
        Label preprocessLabel = new Label( parent, SWT.NONE );
        preprocessLabel.setText( Messages.BuildPrefsPage_PreprocessTab );
        // preprocess tag table
        _preprocessDirectiveUI = new WorkspacePreprocesDirectiveUI( parent, PreprocessorTag.WS_SCOPE );
        _preprocessDirectiveUI.addListener();
        //
        _popForPreprocessHookMissing = new Button( parent, SWT.CHECK );
        _popForPreprocessHookMissing.setText( "Open popup when preprocess hook is not configured" );
        _popForPreprocessHookMissing.setSelection( PreprocessorPreferences.getPopForPreprocessHookMissing() );
        return parent;
    }

    @Override
    public boolean performOk() {
        storePrefValues();
        return true;
    }

    @Override
    public boolean performCancel() {
        if( _preprocessDirectiveUI != null ) {
            _preprocessDirectiveUI.removeAllDirectives();
        }
        return super.performCancel();
    }

    @Override
    protected void performDefaults() {
        if( _preprocessDirectiveUI != null ) {
            _preprocessDirectiveUI.performDefaults();
        }
        _popForPreprocessHookMissing.setSelection( PreprocessorPreferences.getDefaultPopForPreprocessHookMissing() );
        super.performDefaults();
    }

    @Override
    public void propertyChange( PropertyChangeEvent event ) {

    }

    private void storePrefValues() {
        PreprocessorPreferences.setPreprocessDefines( _preprocessDirectiveUI.getScopeDirectives() );
        PreprocessorPreferences.setPopForPreprocessHookMissing( _popForPreprocessHookMissing.getSelection() );
    }

    class WorkspacePreprocesDirectiveUI extends PreprocessDirectiveUI {

        public WorkspacePreprocesDirectiveUI( Composite parent, int scope ) {
            super( parent, scope );
        }

        @Override
        protected void performDefaults() {
            List< PreprocessorTag > JREDirectives = getJREWorkspaceDirectives();
            setInput( JREDirectives.toArray( new PreprocessorTag[ JREDirectives.size() ] ) );
        }

        @Override
        protected void performChanged() {
            // TODO Auto-generated method stub

        }

        @Override
        protected void addListener() {
            ContextManager.getDefault().getPreferenceStore().removePropertyChangeListener( this );
            ContextManager.getDefault().getPreferenceStore().addPropertyChangeListener( this );
            JavaRuntime.removeVMInstallChangedListener( this );
            JavaRuntime.addVMInstallChangedListener( this );
        }

        @Override
        protected void removeListener() {
            ContextManager.getDefault().getPreferenceStore().removePropertyChangeListener( this );
            JavaRuntime.removeVMInstallChangedListener( this );
        }

    }
}
