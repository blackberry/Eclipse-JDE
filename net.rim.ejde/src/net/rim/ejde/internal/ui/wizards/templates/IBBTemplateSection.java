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

import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;

public interface IBBTemplateSection {

    public URL getTemplateLocation();

    public String getLabel();

    public String getDescription();

    /**
     * Returns a replacement string for the provided key. When a token is found in the template file with a form '$key$', the
     * actual key is passed to this method to obtain the replacement. If replacement is provided, it is substituted for the token
     * (including the '$' characters). Otherwise, it is transfered as-is.
     *
     * @param fileName
     *            the name of the file in which the key was found. You can use it to return different values for different files.
     * @param key
     *            the replacement key found in the template file
     * @return replacement string for the provided key, or the key itself if not found.
     */
    public String getReplacementString( String fileName, String key );

    /**
     * Adds template-related pages to the wizard. A typical section implementation contributes one page, but complex sections may
     * span several pages.
     *
     * @param wizard
     *            the host wizard to add pages into
     */
    public void addPages( Wizard wizard );

    /**
     * Returns a wizard page at the provided index.
     *
     * @return wizard page index.
     */
    public WizardPage getPage( int pageIndex );

    /**
     * Returns number of pages that are contributed by this template.
     */
    public int getPageCount();

    /**
     * Tests whether this template have had a chance to create its pages. This method returns true after 'addPages' has been
     * called.
     *
     * @return <samp>true </samp> if wizard pages have been created by this template.
     */

    public boolean getPagesAdded();

    /**
     * Returns the number of work units that this template will consume during the execution. This number is used to calculate the
     * total number of work units when initializing the progress indicator.
     *
     * @return the number of work units
     */
    public int getNumberOfWorkUnits();

    /**
     * Executes the template. As part of the execution, template may generate resources under the provided project, and/or modify
     * the plug-in model.
     *
     * @param project
     *            the workspace project that contains the plug-in
     * @param monitor
     *            progress monitor to indicate execution progress
     */
    public void execute( IProject project, IProgressMonitor monitor ) throws CoreException;

    public Object getValue( String variable );
}
