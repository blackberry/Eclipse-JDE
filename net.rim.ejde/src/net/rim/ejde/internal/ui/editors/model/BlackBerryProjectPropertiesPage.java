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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.Icon;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.PreprocessorTag;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;

/**
 * This is the base class for all pages within the project application descriptor editor. It provides various helper methods used
 * through out the pages.
 *
 * @author cmalinescu, jkeshavarzi
 *
 */
public abstract class BlackBerryProjectPropertiesPage extends FormPage {
    public static final String CONTROL_TITLE_KEY = "TITLE"; //$NON-NLS-1$
    public static final String TABLE_TEXT_INDEX_KEY = "TEXT_COLUMN"; //$NON-NLS-1$
    public static final String SECTION_PART_KEY = "part"; //$NON-NLS-1$

    private final BlackBerryProject _blackBerryProject;
    private ArrayList< SectionPart > sections = new ArrayList< SectionPart >();

    /**
     * The actions associated with each button event used in the editor
     *
     * @author jkeshavarzi
     *
     */
    public static enum Action {
        ADD, ADD_FROM_PROJECT, ADD_FROM_EXTERNAL, REMOVE, EDIT, SELECT_ALL, SELECT_NONE, BROWSE, MOVE_UP, MOVE_DOWN;

        public String getButtonLabel() {
            switch( this ) {
                case ADD:
                    return Messages.BlackBerryProjectPropertiesPage_Add_Button_Label;
                case REMOVE:
                    return Messages.BlackBerryProjectPropertiesPage_Remove_Button_Label;
                case EDIT:
                    return Messages.BlackBerryProjectPropertiesPage_Edit_Button_Label;
                case ADD_FROM_PROJECT:
                    return Messages.BlackBerryProjectPropertiesPage_Add_From_Project_Button_Label;
                case ADD_FROM_EXTERNAL:
                    return Messages.BlackBerryProjectPropertiesPage_Add_From_External_Button_Label;
                case SELECT_ALL:
                    return Messages.BlackBerryProjectPropertiesPage_Select_All_Button_Label;
                case SELECT_NONE:
                    return Messages.BlackBerryProjectPropertiesPage_Deselect_All_Button_Label;
                case BROWSE:
                    return Messages.BlackBerryProjectPropertiesPage_Browse_Button_Label_Ellipsis;
                case MOVE_UP:
                    return Messages.BlackBerryProjectPropertiesPage_MoveUp_Button_Label;
                case MOVE_DOWN:
                    return Messages.BlackBerryProjectPropertiesPage_MoveDown_Button_Label;
                default:
                    return IConstants.EMPTY_STRING;
            }
        }
    }

    /**
     * @param editor
     * @param id
     * @param title
     */
    public BlackBerryProjectPropertiesPage( FormEditor editor, String id, String title ) {
        super( editor, id, title );
        _blackBerryProject = ( (BlackBerryProjectFormEditor) editor ).getBlackBerryProject();
    }

    protected void addSection( SectionPart part ) {
        getManagedForm().addPart( part );
        sections.add( part );
    }

    protected SectionPart[] getSections() {
        return sections.toArray( new SectionPart[ sections.size() ] );
    }

    protected AbstractSection getSectionPartProperty( Composite body ) {
        AbstractSection section = null;

        Object parent = body.getParent().getData( SECTION_PART_KEY );
        if( parent == null ) {
            // if part cannot be found, check higher. Used in packaging section where there is another level of composite parents
            parent = body.getParent().getParent().getData( SECTION_PART_KEY );
        }
        if( ( parent != null ) && ( parent instanceof AbstractSection ) ) {
            section = (AbstractSection) parent;
        }
        return section;
    }

    /**
     * @param keyTable
     * @return An array of he resource keys in the passed in resource
     */
    protected String[] getResourceKeys( Hashtable< String, String > keyTable ) {
        if( keyTable == null ) {
            return new String[] {};
        }
        ArrayList< String > keys = new ArrayList< String >();

        // Add blank entry to key list to allow user to select none.
        keys.add( "" ); //$NON-NLS-1$

        for( Iterator< String > iterator = keyTable.keySet().iterator(); iterator.hasNext(); ) {
            keys.add( iterator.next() );
        }

        return keys.toArray( new String[ keys.size() ] );
    }

    /**
     * Creates an error marker in the editor with the given key
     *
     * @param key
     * @param message
     * @param control
     */
    public void createEditorErrorMarker( Object key, String message, Control control ) {
        getManagedForm().getMessageManager().addMessage( key, message, null, IMessageProvider.ERROR, control );

    }

    /**
     * Creates a warning marker in the editor with the given key
     *
     * @param key
     * @param message
     * @param control
     */
    public void createEditorWarnMarker( Object key, String message, Control control ) {
        getManagedForm().getMessageManager().addMessage( key, message, null, IMessageProvider.WARNING, control );
    }

    /**
     * Removes an error marker in the editor specified by the given key
     *
     * @param key
     * @param control
     */
    public void removeEditorErrorMarker( Object key, Control control ) {
        getManagedForm().getMessageManager().removeMessage( key, control );
    }

    /**
     * Sets the editor and managed forms to dirty
     */
    protected void notifyModelChanged() {
        ( (BlackBerryProjectFormEditor) getEditor() ).setDirty( true );
        getManagedForm().dirtyStateChanged();
    }

    /**
     * Adds the given object item to the given table if and only if the object does not already exist. Also updates button
     * controls.
     *
     * @param viewer
     * @param item
     * @param actionButtons
     */
    public boolean addTableItem( TableViewer viewer, Object item, Map< Action, Button > actionButtons, boolean updateUIOnly ) {
        if( updateUIOnly ) {
            if( actionButtons.containsKey( Action.EDIT ) ) {
                actionButtons.get( Action.EDIT ).setEnabled( true );
            }

            if( actionButtons.containsKey( Action.REMOVE ) ) {
                actionButtons.get( Action.REMOVE ).setEnabled( true );
            }

            return true;
        } else {
            if( getObjectIndexInViewer( viewer, item ) == -1 ) {
                if( item != null ) {
                    viewer.add( item );
                    Table table = viewer.getTable();
                    table.select( table.getItemCount() - 1 );

                    // Used to fire any selection events
                    viewer.setSelection( viewer.getSelection() );
                }
                if( actionButtons.containsKey( Action.EDIT ) ) {
                    actionButtons.get( Action.EDIT ).setEnabled( true );
                }

                if( actionButtons.containsKey( Action.REMOVE ) ) {
                    actionButtons.get( Action.REMOVE ).setEnabled( true );
                }

                return true;
            }

            return false;
        }
    }

    /**
     * Sets the given viewer to the given input. Convenience method that removes all existing data before setting the input.
     *
     * @param viewer
     * @param input
     */
    public void setViewerInput( TableViewer viewer, Object[] input ) {
        Arrays.sort( input );

        // Remove existing data
        viewer.setInput( null );
        viewer.getTable().removeAll();

        viewer.setInput( input );
        viewer.refresh( true );
    }

    /**
     * Helper method that locates the given itemText and if found selects it.
     *
     * @param viewer
     * @param itemText
     * @param actionButtons
     */
    public void selectItemInViewer( TableViewer viewer, String itemText, Map< Action, Button > actionButtons ) {
        Integer index = getItemIndex( viewer.getTable(), itemText );

        if( index != -1 ) {
            viewer.getTable().select( index );

            // Used to fire any selection events
            viewer.setSelection( viewer.getSelection() );

            if( actionButtons != null ) {
                if( actionButtons.containsKey( Action.EDIT ) ) {
                    actionButtons.get( Action.EDIT ).setEnabled( true );
                }
                if( actionButtons.containsKey( Action.REMOVE ) ) {
                    actionButtons.get( Action.REMOVE ).setEnabled( true );
                }
                if( actionButtons.containsKey( Action.MOVE_UP ) ) {
                    if( viewer.getTable().getItemCount() > 1 ) {
                        actionButtons.get( Action.MOVE_UP ).setEnabled( true );
                    }
                }
                if( actionButtons.containsKey( Action.MOVE_DOWN ) ) {
                    actionButtons.get( Action.MOVE_DOWN )
                            .setEnabled( index < viewer.getTable().getItemCount() - 1 ? true : false );
                }
            }
        }
    }

    /**
     * Removes the currently selected table item from the viewer and updates controls accordingly
     *
     * @param viewer
     * @param actionButtons
     * @param updateUIOnly
     *            <code>true</code> only update UI but not remove items from the table; <code>false</code> update UI and remove
     *            items from the table;
     */
    public void removeSelectedTableItem( TableViewer viewer, Map< Action, Button > actionButtons, boolean updateUIOnly ) {
        StructuredSelection selection = (StructuredSelection) viewer.getSelection();
        Table table = viewer.getTable();
        int selectionIndex = table.getSelectionIndex();
        Boolean entryRemoved = false;

        if( !updateUIOnly ) {
            for( Iterator< ? > iter = selection.iterator(); iter.hasNext(); ) {
                Object selectedElement = iter.next();

                if( selectedElement != null ) {

                    viewer.remove( selectedElement );

                    if( !entryRemoved ) {
                        entryRemoved = true;
                    }
                }
            }
        }

        if( updateUIOnly || entryRemoved ) {
            int itemCount = table.getItemCount();
            if( itemCount != 0 ) {
                table.select( itemCount - 1 > selectionIndex ? selectionIndex : itemCount - 1 );

                if( actionButtons.containsKey( Action.MOVE_DOWN ) ) {
                    if( selectionIndex == itemCount - 1 ) {
                        actionButtons.get( Action.MOVE_DOWN ).setEnabled( false );
                    }

                    actionButtons.get( Action.MOVE_UP ).setEnabled( itemCount > 1 ? true : false );
                }

                // Used to fire any selection events
                viewer.setSelection( viewer.getSelection() );
            } else {
                if( actionButtons.containsKey( Action.EDIT ) ) {
                    actionButtons.get( Action.EDIT ).setEnabled( false );
                }
                if( actionButtons.containsKey( Action.REMOVE ) ) {
                    actionButtons.get( Action.REMOVE ).setEnabled( false );
                }
                if( actionButtons.containsKey( Action.MOVE_UP ) ) {
                    actionButtons.get( Action.MOVE_UP ).setEnabled( false );
                }
                if( actionButtons.containsKey( Action.MOVE_DOWN ) ) {
                    actionButtons.get( Action.MOVE_DOWN ).setEnabled( false );
                }
                if( actionButtons.containsKey( Action.SELECT_ALL ) ) {
                    actionButtons.get( Action.SELECT_ALL ).setEnabled( false );
                }
                if( actionButtons.containsKey( Action.SELECT_NONE ) ) {
                    actionButtons.get( Action.SELECT_NONE ).setEnabled( false );
                }
            }

            // Enable any disabled add buttons - icon scenarios where only 2 icons can be added.
            if( actionButtons.containsKey( Action.ADD ) ) {
                actionButtons.get( Action.ADD ).setEnabled( true );
            }
            if( actionButtons.containsKey( Action.ADD_FROM_PROJECT ) ) {
                actionButtons.get( Action.ADD_FROM_PROJECT ).setEnabled( true );
            }
            if( actionButtons.containsKey( Action.ADD_FROM_EXTERNAL ) ) {
                actionButtons.get( Action.ADD_FROM_EXTERNAL ).setEnabled( true );
            }

            if( table.getColumnCount() > 1 ) {
                table.getColumn( 1 ).pack();
            }
        }
    }

    /**
     * Searches the given viewer for the given object and returns its index or -1 if the object was not found
     *
     * @param viewer
     * @param obj
     * @return
     */
    public Integer getObjectIndexInViewer( TableViewer viewer, Object obj ) {
        for( int i = 0; i < viewer.getTable().getItemCount(); i++ ) {
            Object element = viewer.getElementAt( i );
            if( ( obj instanceof Icon ) && ( element instanceof Icon ) ) {
                IProject project = _blackBerryProject.getProject();

                IPath iconPath = new Path( ( (Icon) element ).getCanonicalFileName() );
                IResource iconRes = project.findMember( iconPath );

                IPath projectPath = project.getLocation(), iconRelPath, iconAbsPath;
                if( iconRes != null ) {
                    iconRelPath = projectPath.append( iconRes.getProjectRelativePath() );
                    iconAbsPath = iconRes.getLocation();
                } else {
                    iconRelPath = null;
                    iconAbsPath = projectPath.append( iconPath );
                }

                IPath newIconPath = projectPath.append( ( (Icon) obj ).getCanonicalFileName() );

                if( newIconPath.equals( iconRelPath ) || newIconPath.equals( iconAbsPath ) ) {
                    return i;
                }
            } else if( ( obj instanceof PreprocessorTag ) && ( element instanceof PreprocessorTag ) ) {
                PreprocessorTag o1 = (PreprocessorTag) obj;
                PreprocessorTag o2 = (PreprocessorTag) element;

                if( o1.getPreprocessorDefine().equals( o2.getPreprocessorDefine() ) ) {
                    return i;
                }
            } else {
                if( obj.equals( element ) ) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Checks the given table for any items with the given text and returns the first found index
     *
     * @param table
     * @param itemText
     * @return The index or -1 if the text was not found
     */
    public static Integer getItemIndex( Table table, String itemText ) {
        Object obj = table.getData( TABLE_TEXT_INDEX_KEY );
        Integer textColumn = null;

        if( ( obj != null ) && ( obj instanceof Integer ) ) {
            textColumn = (Integer) obj;

            for( int i = 0; i < table.getItemCount(); i++ ) {
                TableItem item = table.getItem( i );
                if( item.getText( textColumn ).equals( itemText ) ) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Returns an instance of BlackBerryProject
     *
     * @return Instance of BlackBerryProject
     */
    public BlackBerryProject getBlackBerryProject() {
        return _blackBerryProject;
    }

    public static boolean isImage( String fileName ) {
        if( fileName.toLowerCase().matches( ".*[.](gif|png|xpm|bmp|jpg|jpeg|tiff)" ) ) { //$NON-NLS-1$
            return true;
        }
        return false;
    }

    public static boolean isAlx( String fileName ) {
        if( fileName.toLowerCase().matches( ".*[.](alx)" ) ) { //$NON-NLS-1$
            return true;
        }
        return false;
    }

    /**
     * Dirty listener
     *
     * @author jkeshavarzi
     *
     */
    public class DirtyListener implements ModifyListener, SelectionListener {
        AbstractFormPart _section = null;

        public DirtyListener( AbstractFormPart section ) {
            _section = section;
        }

        @Override
        public void modifyText( ModifyEvent e ) {
            process();
        }

        @Override
        public void widgetDefaultSelected( SelectionEvent e ) {
            process();
        }

        @Override
        public void widgetSelected( SelectionEvent e ) {
            process();
        }

        public void process() {
            notifyModelChanged();

            // The dirty flag must be set on the details rather than section so that changes made on it can be committed
            // see ManagedForm.commit() logic
            if( BlackBerryProjectPropertiesPage.this instanceof BlackBerryProjectAlternateEntryPointPage ) {
                AlternateEntryPointDetails details = ( (BlackBerryProjectAlternateEntryPointPage) BlackBerryProjectPropertiesPage.this )
                        .getAlternateEntryPointDetails();
                if( details != null ) {
                    details.markDirty();
                }
            } else {
                _section.markDirty();
            }
        }
    }

    /**
     * The parent listener to all button event listeners used in the editor
     *
     * @author jkeshavarzi
     *
     */
    protected abstract class FilePathOperationSelectionListener implements SelectionListener {
        AbstractFormPart _section = null;

        protected FilePathOperationSelectionListener( AbstractFormPart section ) {
            _section = section;
        }

        @Override
        public void widgetDefaultSelected( SelectionEvent e ) {
            apply( e );
        }

        @Override
        public void widgetSelected( SelectionEvent e ) {
            apply( e );
        }

        private void apply( SelectionEvent e ) {
            if( process( e ) ) {
                notifyModelChanged();

                if( _section != null ) {
                    _section.markDirty();
                }
            }
        }

        protected abstract boolean process( SelectionEvent e );
    }

    /**
     * Project properties focus listener
     *
     * @author jkeshavarzi
     *
     */
    protected class ProjectPropertiesFocusListener implements FocusListener {
        @Override
        public void focusLost( FocusEvent e ) {
            // TODO Future implementation
        }

        @Override
        public void focusGained( FocusEvent e ) {
            // Will be used in future outline View
            // BlackBerryProjectFormEditor editor = (BlackBerryProjectFormEditor) getEditor();
            // editor.updateContentOutlinePageSelection( (Control) e.getSource() );
        }
    }

    /**
     * The basic listener used in editor tables
     *
     * @author jkeshavarzi
     *
     */
    protected static class TableSelectionListener implements SelectionListener {
        private Button remove, edit, moveUp, moveDown;

        public TableSelectionListener( Map< Action, Button > buttonMap ) {
            remove = buttonMap.get( Action.REMOVE );
            edit = buttonMap.get( Action.EDIT );
            moveUp = buttonMap.get( Action.MOVE_UP );
            moveDown = buttonMap.get( Action.MOVE_DOWN );
        }

        @Override
        public void widgetDefaultSelected( SelectionEvent e ) {
        }

        @Override
        public void widgetSelected( SelectionEvent e ) {
            Table table = ( (Table) e.getSource() );
            int selectionCount = table.getSelectionCount();
            int selectionIndex = table.getSelectionIndex();
            int itemCount = table.getItemCount();

            if( edit != null ) {
                if( selectionCount > 1 ) {
                    edit.setEnabled( false );
                } else {
                    edit.setEnabled( true );
                }
            }
            if( remove != null ) {
                remove.setEnabled( true );
            }
            if( moveUp != null ) {
                moveUp.setEnabled( ( itemCount > 1 ) && ( selectionIndex > 0 ) ? true : false );
            }
            if( moveDown != null ) {
                moveDown.setEnabled( ( itemCount > 1 ) && ( selectionIndex < itemCount - 1 ) ? true : false );
            }
        }
    }
}
