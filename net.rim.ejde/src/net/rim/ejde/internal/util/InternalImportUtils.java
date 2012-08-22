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
package net.rim.ejde.internal.util;

import java.util.Set;

import net.rim.ejde.internal.builders.PreprocessingBuilder;
import net.rim.ejde.internal.builders.ResourceBuilder;
import net.rim.ejde.internal.imports.LegacyImportHelper;
import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.ui.wizards.imports.GenericSelectionPage;
import net.rim.ide.Project;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;

@InternalFragmentReplaceable
/**
 * Initialize internal properties for the  new BlackBerry properties <code>properties</code>.
 * Do nothing in the external plug-in.
 *
 * @param properties
 * @return
 */
public class InternalImportUtils extends ImportUtils {

    public static void initializeNewBlackBerryProperties( BlackBerryProperties properties ) {
        // do nothing in the external plug-in
    }

    /**
     * Attaches BlackBerry related builders to the <code>project</code>.
     *
     * @param project
     * @throws CoreException
     */
    static public void initiateBuilders( IProject project ) throws CoreException {
        IProjectDescription description = project.getDescription();
        ICommand[] projectBuilders = new ICommand[ 3 ];
        // RIM Resource Builder
        ICommand rimResourceCommand = description.newCommand();
        rimResourceCommand.setBuilderName( ResourceBuilder.BUILDER_ID );
        projectBuilders[ 0 ] = rimResourceCommand;
        // RIM Preprocessing Builder
        ICommand rimPreprocessingCommand = description.newCommand();
        rimPreprocessingCommand.setBuilderName( PreprocessingBuilder.BUILDER_ID );
        projectBuilders[ 1 ] = rimPreprocessingCommand;
        // // Java Builder (built into JDT)
        ICommand javaBuilderCommand = description.newCommand();
        javaBuilderCommand.setBuilderName( JavaCore.BUILDER_ID );
        projectBuilders[ 2 ] = javaBuilderCommand;
        description.setBuildSpec( projectBuilders );
        project.setDescription( description, new NullProgressMonitor() );
    }

    /**
     * Create a LegacyImportHelper object.
     *
     * @param projectSet
     * @param _selectionPage
     * @return
     */
    static public LegacyImportHelper createImportHelper( Set< Project > projectSet, GenericSelectionPage _selectionPage ) {
        return new LegacyImportHelper( projectSet, _selectionPage.getImportType(), _selectionPage.getREPath() );
    }
}
