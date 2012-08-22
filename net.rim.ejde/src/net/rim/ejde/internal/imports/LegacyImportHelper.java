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
package net.rim.ejde.internal.imports;

import java.util.Set;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.internalplugin.InternalFragmentReplaceable;
import net.rim.ejde.internal.model.BlackBerryPropertiesFactory;
import net.rim.ejde.internal.util.ImportUtils;
import net.rim.ejde.internal.util.StatusFactory;
import net.rim.ide.Project;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

/**
 * External
 */
@InternalFragmentReplaceable
public class LegacyImportHelper extends BasicLegacyImportHelper {

    static private final Logger _log = Logger.getLogger( LegacyImportHelper.class );

    public LegacyImportHelper( Set< Project > legacyProjects, int importType, IPath REPath ) {
        super( legacyProjects, importType, REPath );
    }

    protected IJavaProject importProject( Project legacyProject, IProgressMonitor monitor ) throws CoreException {
        IJavaProject eclipseJavaProject = null;
        monitor.beginTask( "Importing project " + legacyProject.getDisplayName(), 10 );

        // create a java project for the legacy project
        eclipseJavaProject = ImportUtils.createJavaProject( legacyProject, _importType, _bbLibName, _REPath,
                new SubProgressMonitor( monitor, 8 ) );
        if( eclipseJavaProject == null ) {
            return null;
        }

        // open the project if it is not
        if( !eclipseJavaProject.isOpen() ) {
            try {
                eclipseJavaProject.open( new NullProgressMonitor() );
            } catch( JavaModelException e ) {
                _log.error( e.getMessage(), e );
                monitor.done();
                throw new CoreException( StatusFactory.createErrorStatus( e.getMessage() ) );
            }
        }
        monitor.worked( 1 );

        // add the BB properties to the map
        ContextManager.PLUGIN.setBBProperties( legacyProject.getDisplayName(),
                BlackBerryPropertiesFactory.createBlackBerryProperties( legacyProject, eclipseJavaProject ), true );
        monitor.worked( 1 );
        monitor.done();
        return eclipseJavaProject;
    }
}
