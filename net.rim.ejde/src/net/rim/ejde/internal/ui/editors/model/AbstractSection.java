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

import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.util.Messages;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * The base class for all sections in the app descriptor editor.
 *
 * @author jkeshavarzi
 *
 */
public class AbstractSection extends SectionPart {
    private BlackBerryProjectPropertiesPage _parentPage;
    private BlackBerryProjectFormEditor _editor;
    private String _projectType;
    private AbstractSection _part;

    public AbstractSection( BlackBerryProjectPropertiesPage page, Composite parent, FormToolkit toolkit, int style ) {
        super( parent, toolkit, style );
        Section section = getSection();
        _parentPage = page;
        _editor = (BlackBerryProjectFormEditor) page.getEditor();
        _projectType = _editor.getBlackBerryProject().getProperties()._application.getType();
        _part = this;

        initialize( page.getManagedForm() );
        section.clientVerticalSpacing = 6;
        section.setData( BlackBerryProjectPropertiesPage.SECTION_PART_KEY, this );
    }

    /**
     * Returns an instance of BlackBerryProjectPropertiesPage
     *
     * @return Instance of BlackBerryProjectPropertiesPage
     */
    protected BlackBerryProjectPropertiesPage getProjectPropertiesPage() {
        return this._parentPage;
    }

    /**
     * Sets the BlackBerryProjectPropertiesPage
     *
     * @param page
     */
    protected void setProjectPropertiesPage( BlackBerryProjectPropertiesPage page ) {
        this._parentPage = page;
    }

    /**
     * Returns an instance of BlackBerryProjectFormEditor
     *
     * @return Instance of BlackBerryProjectFormEditor
     */
    protected BlackBerryProjectFormEditor getEditor() {
        return this._editor;
    }

    /**
     * Returns the project type
     *
     * @return Project type
     */
    protected String getProjectType() {
        return this._projectType;
    }

    protected void setProjectType( String projectType ) {
        this._projectType = projectType;
    }

    protected AbstractSection getPart() {
        return this._part;
    }

    static protected IFile figureFile( IPath relPath, IContainer resFolder ) {
        return resFolder.getFile( new Path( relPath.toFile().getName() ) );
    }

    static protected boolean isFileClear( IFile file1, IFile file2, IContainer resFolder, Shell shell ) {
        if( file1.equals( file2 ) || file1.exists() ) {
            MessageDialog.openError( shell, Messages.BlackBerryProjectPropertiesPage_Dup_File_Err_Dialog_Title,
                    NLS.bind( Messages.BlackBerryProjectPropertiesPage_Dup_File_Link_Err_Dialog_Msg, resFolder.getName() ) );
            return false;
        }
        return true;
    }

    static public String pathToColumnValue( IPath path ) {
        String filename;
        String directory;
        if( path.segmentCount() > 1 ) {
            filename = path.lastSegment();
            directory = path.removeLastSegments( 1 ).toString();
            return filename + " - " + directory; //$NON-NLS-1$
        } else if( path.segmentCount() == 1 ) {
            filename = path.lastSegment();
            directory = "/";
            return filename + " - " + directory; //$NON-NLS-1$
        }

        return IConstants.EMPTY_STRING;
    }

    /**
     * Returns a list of text controls, combo box controls, and table controls
     *
     * @return
     */
    protected Control[] getOutlineControls() {
        ArrayList< Control > controls = new ArrayList< Control >();
        Control control = getSection().getClient();
        Control children[] = null;

        if( control instanceof Composite ) {
            children = ( (Composite) control ).getChildren();

            for( Control child : children ) {
                if( child instanceof Text || child instanceof Combo || child instanceof Table ) {
                    controls.add( child );
                }
            }
        }
        return controls.toArray( new Control[ controls.size() ] );
    }
}
