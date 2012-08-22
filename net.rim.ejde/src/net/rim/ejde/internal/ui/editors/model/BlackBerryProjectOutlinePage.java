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

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/**
 * The outline view page for the editor. Please note, some of this code was created for demo purposes only and does not reflect
 * the final implementation. Some areas of this code should be changed and coded better once implementation of the outline view
 * officially begins. Also note that the outline view prototype is currently disabled and to re-enable it, please uncomment the
 * appropriate code in getAdapter() method.
 *
 * @author jkeshavarzi
 *
 */
public class BlackBerryProjectOutlinePage extends ContentOutlinePage implements ISelectionChangedListener {
    BlackBerryProjectFormEditor _editor;

    public BlackBerryProjectOutlinePage( BlackBerryProjectFormEditor editor ) {
        this._editor = editor;
    }

    @Override
    public void createControl( Composite parent ) {
        super.createControl( parent );
        TreeViewer contentOutlineViewer = getTreeViewer();
        contentOutlineViewer.addSelectionChangedListener( this );

        contentOutlineViewer.setContentProvider( new ContentProvider() );
        contentOutlineViewer.setLabelProvider( new LabelProvider() );
        contentOutlineViewer.setInput( _editor.getPages() );
    }

    @Override
    public void selectionChanged( SelectionChangedEvent event ) {
        super.selectionChanged( event );
        Object source = event.getSource();
        BlackBerryProjectPropertiesPage page = null;
        Control control = null;

        if( source instanceof TreeViewer ) {
            TreeViewer v = (TreeViewer) source;
            TreeSelection t = (TreeSelection) v.getSelection();
            Object obj = t.getFirstElement();
            if( obj instanceof BlackBerryProjectPropertiesPage ) {
                page = (BlackBerryProjectPropertiesPage) obj;
            } else {
                page = (BlackBerryProjectPropertiesPage) t.getPaths()[ 0 ].getFirstSegment();
            }

            if( _editor.getActivePage() != page.getIndex() ) {
                _editor.setActivePage( page.getIndex() );
            }

            if( obj instanceof Control ) {
                control = (Control) obj;
                control.setFocus();
                if( control instanceof Text ) {
                    Text text = (Text) control;
                    text.selectAll();
                }
            } else if( obj instanceof TableItem ) {
                // Table table = (Table) t.getPaths()[ 0 ].getParentPath().getLastSegment();
                TreePath path = t.getPaths()[ 0 ];
                Object parentObject = path.getSegment( path.getSegmentCount() - 2 );
                if( parentObject instanceof Table ) {
                    Table table = (Table) parentObject;
                    table.select( BlackBerryProjectPropertiesPage.getItemIndex( table, v.getTree().getSelection()[ 0 ].getText() )
                            .intValue() );
                }
            }
        }
    }

    public void updateTreeSelection( Control control ) {
        TreeViewer viewer = getTreeViewer();
        Tree tree = viewer.getTree();
        TreeItem item = null;

        if( tree.getSelection() == null ) {
            return;
        }

        if( control instanceof Table ) {
            Table table = (Table) control;
            TableItem selections[] = table.getSelection();

            if( selections.length > 0 ) {
                item = getTableItem( tree, selections[ 0 ].getText( 2 ) );
            }
        } else {
            item = getControlItem( tree, control.getData( BlackBerryProjectPropertiesPage.CONTROL_TITLE_KEY ).toString() );
        }

        if( item != null ) {
            TreeItem selectedItem[] = tree.getSelection();

            if( ( selectedItem.length > 0 ) && selectedItem[ 0 ].equals( item ) ) {
                return;
            }

            tree.deselectAll();
            tree.select( item );
        }
    }

    public void updateTreeSelection( BlackBerryProjectPropertiesPage page ) {
        TreeViewer viewer = getTreeViewer();
        Tree tree = viewer.getTree();
        TreeItem item = getPageItem( tree, page.getTitle() );

        if( item != null ) {
            TreeItem selectedItem[] = tree.getSelection();

            if( ( selectedItem.length > 0 ) && selectedItem[ 0 ].equals( item ) ) {
                return;
            }

            tree.deselectAll();
            tree.select( item );
        }
    }

    private TreeItem getControlItem( Tree tree, String text ) {
        // TODO Change this demo prototype code. Find a better way to get a control item.
        for( TreeItem item : tree.getItems()[ 0 ].getItems() ) {
            if( item.getText().equals( text ) ) {
                return item;
            }
        }

        return null;
    }

    private TreeItem getTableItem( Tree tree, String text ) {
        // TODO Change this demo prototype code. Find a better way to get a table item.
        for( TreeItem item : tree.getItems()[ 0 ].getItems()[ 4 ].getItems() ) {
            if( item.getText().equals( text ) ) {
                return item;
            }
        }

        return null;
    }

    private TreeItem getPageItem( Tree tree, String text ) {
        // TODO Change this demo prototype code. Find a better way to get a page item.
        for( TreeItem item : tree.getItems() ) {
            if( item.getText().equals( text ) ) {
                return item;
            }
        }

        return null;
    }

    private class LabelProvider implements ILabelProvider {
        Image image = null;

        @Override
        public Image getImage( Object element ) {
            if( element instanceof BlackBerryProjectPropertiesPage ) {
                ImageDescriptor desc = ContextManager.getImageDescriptor( "icons/obj16/page_obj.gif" ); //$NON-NLS-1$
                image = desc.createImage();
                return image;
            }
            if( element instanceof TableItem ) {
                TableItem item = (TableItem) element;
                return item.getImage( 1 );
            }

            return null;
        }

        @Override
        public String getText( Object element ) {
            if( element instanceof BlackBerryProjectPropertiesPage ) {
                return ( (BlackBerryProjectPropertiesPage) element ).getTitle();
            } else if( element instanceof SectionPart ) {
                return ( (SectionPart) element ).getSection().getText();
            } else if( element instanceof Control ) {
                Object text = ( (Control) element ).getData( BlackBerryProjectPropertiesPage.CONTROL_TITLE_KEY );
                return text == null ? IConstants.EMPTY_STRING : text.toString();
            } else if( element instanceof TableItem ) {
                TableItem item = ( (TableItem) element );
                Object obj = item.getParent().getData( BlackBerryProjectPropertiesPage.TABLE_TEXT_INDEX_KEY );
                if( ( obj != null ) && ( obj instanceof Integer ) ) {
                    return item.getText( ( (Integer) obj ).intValue() );
                }
            }

            return null;
        }

        @Override
        public void addListener( ILabelProviderListener listener ) {
            // Do Nothing
        }

        @Override
        public void dispose() {
            // Do Nothing
        }

        @Override
        public boolean isLabelProperty( Object element, String property ) {
            return false;
        }

        @Override
        public void removeListener( ILabelProviderListener listener ) {
            // Do Nothing
        }
    }

    private class ContentProvider implements ITreeContentProvider {
        @Override
        public Object[] getChildren( Object parentElement ) {
            if( parentElement instanceof BlackBerryProjectPropertiesPage ) {
                return ( (BlackBerryProjectPropertiesPage) parentElement ).getSections();
            } else if( parentElement instanceof AbstractSection ) {
                return ( (AbstractSection) parentElement ).getOutlineControls();
            } else if( parentElement instanceof Table ) {
                return ( (Table) parentElement ).getItems();
            }
            return null;
        }

        @Override
        public Object getParent( Object element ) {
            return null;
        }

        @Override
        public boolean hasChildren( Object element ) {
            if( ( element instanceof BlackBerryProjectPropertiesPage ) || ( element instanceof SectionPart )
                    || ( element instanceof Table ) ) {
                return true;
            }
            return false;
        }

        @Override
        public Object[] getElements( Object inputElement ) {
            if( inputElement instanceof BlackBerryProjectPropertiesPage[] ) {
                return (BlackBerryProjectPropertiesPage[]) inputElement;
            } else if( inputElement instanceof TableItem[] ) {
                return (TableItem[]) inputElement;
            }
            return new Object[] {};
        }

        @Override
        public void dispose() {
            // Do Nothing
        }

        @Override
        public void inputChanged( Viewer viewer, Object oldInput, Object newInput ) {
            // Do Nothing
        }
    }
}
