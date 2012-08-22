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
package net.rim.ejde.internal.ui.viewers.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import net.rim.ejde.internal.util.ProjectUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * A tree viewer to display BlackBerry projects in a checkbox tree.
 *
 * @author dmeng
 *
 */
public class BlackBerryProjectTreeViewer extends CheckboxTreeViewer {

    private static final Logger _logger = Logger.getLogger( BlackBerryProjectTreeViewer.class );

    private BBProjectContentProvider _contentProvider;

    static Comparator< IProject > ProjectNameComparator = new Comparator< IProject >() {
        public int compare( IProject project1, IProject project2 ) {
            return project1.getName().toUpperCase().compareTo( project2.getName().toUpperCase() );
        }
    };

    /**
     * Constructor
     *
     * @param parent
     */
    public BlackBerryProjectTreeViewer( Composite parent, int span, int indent ) {
        super( parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL );
        _contentProvider = new BBProjectContentProvider();
        setContentProvider( _contentProvider );
        setLabelProvider( new BBProjectLabelProvider() );
        setAutoExpandLevel( ALL_LEVELS );
        addCheckStateListener( new CheckStateListenerImpl() );

        // TODO: is this needed?
        GridData gd = new GridData( GridData.FILL_BOTH );
        gd.horizontalSpan = span;
        gd.horizontalIndent = indent;
        getTree().setLayoutData( gd );

    }

    public BlackBerryProjectTreeViewer( Composite parent ) {
        this( parent, 1, 0 );
    }

    /**
     * Returns the projects that should be made active based on selection
     */
    public List< IProject > getCheckedProjects() {
        List< IProject > selectedProjects = new ArrayList< IProject >();
        // filter the checked elements list to only include root elements
        // i.e. active-able elements
        Object[] checkedElements = getCheckedElements();
        for( Object element : checkedElements ) {
            if( _contentProvider.getParent( element ) == null ) {
                selectedProjects.add( (IProject) element );
            }
        }
        return selectedProjects;
    }

    public List< String > getCheckedProjectNames() {
        List< String > selectedProjects = new ArrayList< String >();
        // filter the checked elements list to only include root elements
        // i.e. active-able elements
        Object[] checkedElements = getCheckedElements();
        for( Object element : checkedElements ) {
            IProject project = (IProject) element;
            selectedProjects.add( project.getName() );
        }
        return selectedProjects;
    }

    /**
     *
     * @param elements
     *            type really == Project[]
     */
    protected void initializeCheckedState( Shell shell, final Object[] elements ) {
        BusyIndicator.showWhile( shell.getDisplay(), new Runnable() {
            public void run() {
                setCheckedElements( elements );
            }
        } );
    }

    /**
     * Get the number of projects
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public int getNumProjects() {
        Object input = getInput();
        if( input != null ) {
            return ( (Collection< IProject >) input ).size();
        }
        return 0;
    }

    public void initInput( Collection< IProject > allProjects ) {
        // sort the projects alphabetically
        List< IProject > sortedProjects = new ArrayList< IProject >();
        sortedProjects.addAll( allProjects );
        Collections.sort( sortedProjects, ProjectNameComparator );
        setInput( sortedProjects );
    }

    private class CheckStateListenerImpl implements ICheckStateListener {
        public void checkStateChanged( CheckStateChangedEvent event ) {
            IProject project = (IProject) event.getElement();
            if( event.getChecked() ) {
                _logger.debug( "Checked : [" + project.getName() + "]" ); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                _logger.debug( "Unchecked : [" + project.getName() + "]" ); //$NON-NLS-1$ //$NON-NLS-2$
            }
            // the dependent projects are alwasy
            checkDependentProject( project, event.getChecked() );
        }

        private void checkDependentProject( IProject project, boolean checked ) {
            Set< IProject > dependentProjects = null;
            try {
                dependentProjects = ProjectUtils.getAllReferencedProjects( project );
            } catch( CoreException e ) {
                _logger.error( e.getMessage(), e );
            }
            if( dependentProjects == null || dependentProjects.size() == 0 ) {
                return;
            }
            Tree tree = BlackBerryProjectTreeViewer.this.getTree();
            TreeItem[] allItems = tree.getItems();
            List< IProject > checkedProjects = BlackBerryProjectTreeViewer.this.getCheckedProjects();
            for( TreeItem item : allItems ) {
                IProject data = (IProject) item.getData();
                if( dependentProjects.contains( data ) ) {
                    if( checked ) {
                        if( !item.getChecked() ) {
                            // if the dependent project is not checked, check it
                            item.setChecked( true );
                        }
                    } else {
                        if( item.getChecked()
                                && ( ProjectUtils.isDependedByOthers( data, checkedProjects, dependentProjects ) == null ) ) {
                            // if the dependent project is not depended by another project, un-check it
                            item.setChecked( false );
                        }
                    }
                }
            }
        }
    }
}
