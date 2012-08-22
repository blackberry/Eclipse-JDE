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
package net.rim.ejde.internal.ui.launchers;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.rim.ejde.internal.ui.viewers.project.BlackBerryProjectTreeViewer;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.internal.ui.workingsets.IWorkingSetIDs;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.internal.dialogs.SimpleWorkingSetSelectionDialog;

/**
 * UI that displays selectable list of all BB apps in workspace
 *
 * @author bchabot
 */
public class AppSelectionUI {

    protected BlackBerryProjectTreeViewer _projectsTreeViewer;
    private Button _selectAllButton;
    private Button _deselectButton;
    private Button _selectWorkingSetButton;
    private Label _counterLabel;

    private PropertyChangeSupport _dispatcher = new PropertyChangeSupport( this );

    private Listener _listener = new Listener();

    class Listener extends SelectionAdapter {
        public void widgetSelected( SelectionEvent e ) {
            Object source = e.getSource();
            if( source == _selectAllButton ) {
                _projectsTreeViewer.setAllChecked( true );
                updateCounter();
            } else if( source == _deselectButton ) {
                _projectsTreeViewer.setAllChecked( false );
                updateCounter();
            } else if( source == _selectWorkingSetButton ) {
                handleWorkingSetSelection();
                updateCounter();
            }
            _dispatcher.firePropertyChange( "", null, null );
        }
    }

    /**
     * Constructor
     */
    public AppSelectionUI() {
    }

    public void addPropertyChangeListener( PropertyChangeListener listener ) {
        _dispatcher.addPropertyChangeListener( listener );
    }

    public void removePropertyChangeListener( PropertyChangeListener listener ) {
        _dispatcher.removePropertyChangeListener( listener );
    }

    public List< String > getCheckedProjectNames() {
        return _projectsTreeViewer.getCheckedProjectNames();
    }

    public List< IProject > getCheckedProjects() {
        return _projectsTreeViewer.getCheckedProjects();
    }

    public boolean isChecked( IProject project ) {
        return _projectsTreeViewer.getChecked( project );
    }

    /**
     * Refreshes the project selected count UI
     */
    protected void updateCounter() {
        if( _counterLabel != null ) {
            int checked = _projectsTreeViewer.getCheckedElements().length;
            int total = _projectsTreeViewer.getNumProjects();
            _counterLabel
                    .setText( NLS.bind( Messages.AppSelectionUI_counter, Integer.valueOf( checked ), Integer.valueOf( total ) ) );
        }
    }

    /**
     * Creates the UI
     *
     * @param parent
     * @param span
     * @param indent
     */
    public void createControl( Composite parent, int span, int indent ) {
        createProjectViewer( parent, span - 1, indent );
        createButtonContainer( parent );
    }

    /**
     * Creates the project viewer UI
     *
     * @param composite
     * @param span
     * @param indent
     */
    protected void createProjectViewer( Composite composite, int span, int indent ) {
        _projectsTreeViewer = new BlackBerryProjectTreeViewer( composite, span, indent );

        GridData layoutData = new GridData( GridData.FILL_BOTH );
        layoutData.widthHint = 410;
        _projectsTreeViewer.getTree().setLayoutData( layoutData );

        _projectsTreeViewer.addCheckStateListener( new ICheckStateListener() {
            public void checkStateChanged( final CheckStateChangedEvent event ) {
                updateCounter();
                _dispatcher.firePropertyChange( "", null, null );
            }
        } );
    }

    private void createButtonContainer( Composite parent ) {
        Composite composite = new Composite( parent, SWT.NONE );
        GridLayout layout = new GridLayout();
        layout.marginHeight = layout.marginWidth = 0;
        composite.setLayout( layout );
        composite.setLayoutData( new GridData( GridData.FILL_VERTICAL ) );

        _selectAllButton = createButton( composite, Messages.AppSelectionUI_selectAll );
        _deselectButton = createButton( composite, Messages.AppSelectionUI_deselectAll );
        _selectWorkingSetButton = createButton( composite, Messages.AppSelectionUI_selectWorkingSet );

        _counterLabel = new Label( composite, SWT.NONE );
        _counterLabel.setLayoutData( new GridData( GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_END ) );
        updateCounter();
    }

    protected int getTreeViewerStyle() {
        return SWT.BORDER;
    }

    private Button createButton( Composite composite, String text ) {
        Button button = new Button( composite, SWT.PUSH );
        button.setText( text );
        button.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ) );
        // SWTUtil.setButtonDimensionHint(button);
        button.addSelectionListener( _listener );
        return button;
    }

    /**
     * Initializes the projects selected with the given config data
     *
     * @param config
     */
    public void initializeFrom( Collection< IProject > allProjects, Collection< IProject > checkedProjects ) {
        if( _projectsTreeViewer.getInput() == null ) {
            _projectsTreeViewer.setUseHashlookup( true );
            _projectsTreeViewer.initInput( allProjects );
        }
        setCheckedProjects( checkedProjects );
        updateCounter();
    }

    protected void setCheckedProjects( Collection< IProject > checked ) {
        _projectsTreeViewer.setCheckedElements( checked.toArray( new IProject[ 0 ] ) );
    }

    /**
     * @deprecated
     * */
    public void performApply( ILaunchConfigurationWorkingCopy config ) {
        /*
         * config.setAttribute(IFledgeLaunchConstants.PROJECTS, _projectsTreeViewer.getSelectedProjectNames());
         */
    }

    public void enableViewer( boolean enable ) {
        _projectsTreeViewer.getTree().setEnabled( enable );
        _selectAllButton.setEnabled( enable );
        _deselectButton.setEnabled( enable );
        _counterLabel.setEnabled( enable );
    }

    public void dispose() {
    }

    protected boolean isEnabled() {
        return _projectsTreeViewer.getTree().isEnabled();
    }

    private void handleWorkingSetSelection() {
        Shell shell = _selectWorkingSetButton.getShell();
        String[] workingSetIds = new String[] { IWorkingSetIDs.JAVA, IWorkingSetIDs.RESOURCE };
        IWorkingSet[] selectedWorkingSets = new IWorkingSet[ 0 ];
        SimpleWorkingSetSelectionDialog dialog = new SimpleWorkingSetSelectionDialog( shell, workingSetIds, selectedWorkingSets,
                true );
        dialog.setMessage( WorkbenchMessages.WorkingSetGroup_WorkingSetSelection_message );
        if( dialog.open() == Window.OK ) {
            IWorkingSet[] result = dialog.getSelection();
            if( result != null && result.length > 0 ) {
                selectedWorkingSets = result;
            } else {
                selectedWorkingSets = new IWorkingSet[ 0 ];
            }
            Set< IProject > checked = new HashSet< IProject >();
            if( selectedWorkingSets.length > 0 ) {
                for( int i = 0; i < selectedWorkingSets.length; i++ ) {
                    IAdaptable[] adapters = selectedWorkingSets[ i ].getElements();
                    for( int k = 0; k < adapters.length; k++ ) {
                        IResource res = (IResource) adapters[ k ].getAdapter( IResource.class );
                        checked.add( res.getProject() );
                    }
                }
            }
            setCheckedProjects( checked );
        }
    }
}
