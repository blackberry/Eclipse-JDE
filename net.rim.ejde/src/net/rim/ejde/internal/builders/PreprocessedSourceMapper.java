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
package net.rim.ejde.internal.builders;

import net.rim.ejde.external.sourceMapper.ISourceMapper;
import net.rim.ejde.internal.model.BlackBerryProjectPreprocessingNature;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

/**
 * An implementation of the {@link ISourceMapper} hook interface. This mapper attempts to map the requested file to a preprocessed
 * version of the file.
 */
public class PreprocessedSourceMapper implements ISourceMapper {
    private static final Logger log = Logger.getLogger( PreprocessedSourceMapper.class );

    public IFile getMappedResource( IFile sourceFile ) {
        IFile mappedFile = null;

        if( isPreprocessingProject( sourceFile.getProject() ) ) {
            mappedFile = PreprocessingBuilder.getOutputFile( sourceFile );
            if( !mappedFile.exists() ) {
                // if the preprocessed file does not exist, return the original one
                mappedFile = sourceFile;
            }
        } else {
            // if the project does not need to be preprocessed, return the original IFile
            mappedFile = sourceFile;
        }

        return mappedFile;
    }

    /**
     * Return a boolean indicating whether the specified project has preprocessing enabled.
     *
     * @param project
     * @return
     */
    private boolean isPreprocessingProject( IProject project ) {
        boolean preprocessing = false;

        try {
            preprocessing = project.hasNature( BlackBerryProjectPreprocessingNature.NATURE_ID );
        } catch( CoreException e ) {
            log.error( e );
        }

        return preprocessing;
    }

    @Override
    public char[] getContent( IFile sourceFile ) {
        // currently we only simply return an empty char array
        return new char[ 0 ];
    }
}
