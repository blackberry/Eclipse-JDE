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
package net.rim.ejde.internal.launching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.PackagingUtils;
import net.rim.ejde.internal.util.StatusFactory;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;


/**
 * A helper class to deploy a BlackBerry project to destination folder.
 *
 * @author dmeng
 *
 */
public class DeploymentHelper {

    static final String defExtension = ".def";
    static final String alxExtension = ".alx";
    static final String codeExtension = ".cod";
    static final String cslExtension = ".wts";
    static final String wtsExtension = ".csl";
    static final String csoExtension = ".cso";
    static final String csvExtension = ".csv";
    static final String debugExtension = ".debug";
    static final String dmpExtension = ".dmp";
    static final String dllExtension = ".dll";
    static final String exportExtension = ".export.xml";
    static final String gifExtension = ".gif";
    static final String htmlExtension = ".html";
    static final String jarExtension = ".jar";
    static final String jadExtension = ".jad";
    static final String javaExtension = ".java";
    static final String jsExtension = ".js";
    static final String projectExtension = ".jdp";
    static final String workspaceExtension = ".jdw";
    static final String jpgExtension = ".jpg";
    static final String keyExtension = ".key";
    static final String lstExtension = ".lst";
    static final String pngExtension = ".png";
    static final String rapcExtension = ".rapc";
    static final String languageResourceExtension = ".rrc";
    static final String languageHeaderExtension = ".rrh";
    static final String keyMapResourceExtension = ".rcmap";
    static final String keyMapHeaderExtension = ".rhmap";
    static final String txtExtension = ".txt";
    static final String xmlExtension = ".xml";
    static final String manifestExtension = ".manifest.xml";

    public static final String deployableFileExtensions[] = { codeExtension, jarExtension, lstExtension, debugExtension,
            cslExtension, csoExtension, wtsExtension };

    private static final Logger _logger = Logger.getLogger( DeploymentHelper.class );

    /**
     * Deploy the given project.
     *
     * @param project
     *            The project to be deployed
     * @param deploymentPath
     *            The deployment path
     * @param internalMode
     *            If we are running in internal model
     *
     * @return <code>true</code> if deployment completes successfully. otherwise return <code>false</code>.
     */
    public static int deploy( BlackBerryProject project, String deploymentPath, boolean internalMode ) throws CoreException {
        int rc;
        // undeploy existing files first
        rc = deploy( project, deploymentPath, true, internalMode );
        if( rc != 0 ) {
            return rc;
        }
        // deploy new files
        rc = deploy( project, deploymentPath, false, internalMode );
        return rc;
    }

    /**
     * Checks if the file represented by the <code>fileName</code> needs to be deployed.
     *
     * @param fileName
     * @return
     */
    public static final boolean isDeploymentFile( String fileName ) {
        for( int i = 0; i < DeploymentHelper.deployableFileExtensions.length; i++ ) {
            if( fileName.endsWith( DeploymentHelper.deployableFileExtensions[ i ] ) ) {
                return true;
            }
        }
        return false;
    }

    private static int deploy( BlackBerryProject project, String depolymentPath, boolean erase, boolean isRIMModel )
            throws CoreException {
        BlackBerryProperties properties = project.getProperties();
        String outputFileName = properties._packaging.getOutputFileName();
        String outputPath = project.getProject().getLocation().toOSString();
        String[] outputPaths = PackagingUtils.getPackagingOutputFolders( project );
        // we deploy the standard deliverables
        outputPath += IPath.SEPARATOR + outputPaths[ PackagingUtils.STANDARD_DEPLOYMENT ];
        _logger.trace( "Project " + project.getElementName() + ( erase ? " undeploy from " : " deploy --> " ) + depolymentPath );
        for( int i = 0; i < deployableFileExtensions.length; ++i ) {
            int rc = deploy( outputFileName, outputPath, depolymentPath, deployableFileExtensions[ i ], erase );
            if( rc != 0 ) {
                return rc;
            }
        }
        // if it is RIM model, also deploy the cod file to the lynx/debug/java folder
        if( isRIMModel ) {
            int rc = deploy( outputFileName, outputPath, depolymentPath + File.separator + "Java", codeExtension, erase );
            if( rc != 0 ) {
                return rc;
            }
        }
        return 0;
    }

    private static int deploy( String projectName, String srcPath, String dstPath, String extension, boolean erase )
            throws CoreException {
        if( erase ) {
            List< File > list = getSiblingList( projectName, dstPath, extension );
            int rc = 0;
            int count = list.size();
            for( int i = 0; i < count; ++i ) {
                File f = list.get( i );
                if( !f.exists() ) {
                    break;
                }
                rc = executeErase( f );
                if( rc != 0 ) {
                    _logger.error( "error deleting " + f.getName() );
                    return rc;
                }
            }
            return 0;
        } else {
            // _logger.info( "Deploy " + projectName + extension + " from " + srcPath + " to " + dstPath );
            int rc = 0;
            int i = 0;
            String sibling = "";
            for( ;; ) {
                String name = projectName + sibling + extension;
                File srcFile = new File( srcPath + File.separator + name );
                if( !srcFile.exists() ) {
                    break;
                }
                File dstFile = new File( dstPath + File.separator + name );
                rc = deployFile( srcFile, dstFile );
                if( rc != 0 ) {
                    return rc;
                }
                sibling = makeSiblingExtension( Integer.toString( ++i ) );
            }
            return rc;
        }
    }

    private static int deployFile( File srcFile, File dstFile ) throws CoreException {
        if( srcFile.lastModified() != dstFile.lastModified() ) {
            int rc = executeCopy( srcFile, dstFile );
            if( rc != 0 ) {
                return rc;
            }
        } else {
            _logger.info( "File " + srcFile.getName() + " is skipped because the destination file is newer" );
        }
        return 0;
    }

    private static List< File > getSiblingList( String fileName, String path, String extension ) {
        List< File > v = new ArrayList< File >();
        String sibling = "";
        int i = 0;
        for( ;; ) {
            StringBuffer sb = new StringBuffer( path );
            sb.append( File.separator );
            sb.append( fileName );
            sb.append( sibling );
            sb.append( extension );
            File f = new File( sb.toString() );
            if( !f.exists() ) {
                break;
            }
            v.add( f );
            sibling = makeSiblingExtension( Integer.toString( ++i ) );
        }
        return v;
    }

    private static String makeSiblingExtension( String s ) {
        return "-" + s;
    }

    /**
     * Erase a file. File.delete() is an acceptable implementation
     *
     * @param f
     *            the file to erase. It is guaranteed to exist
     * @return 0 for success, non-zero if error
     */
    private static int executeErase( File file ) {
        _logger.trace( "Erase " + file.getName() );
        return file.delete() ? 0 : -1;
    }


    /**
     * Copy a file. Util.copyFile( srcFile, dstFile ) is an acceptable implementation
     *
     * @param srcFile
     *            the source of the copy
     * @param dstFile
     *            the destination of the copy
     * @return 0 for success, non-zero for failure
     * @throws CoreException
     */
    public static int executeCopy( final File srcFile, final File dstFile ) throws CoreException {
    	_logger.trace( "Copy " + srcFile.getName() ); // + " --> " + dstFile );
        if( !srcFile.exists() ) {
            String msg = NLS.bind( Messages.DeploymentHelper_FILE_NOT_EXIST_MSG, srcFile.getAbsolutePath() );
            _logger.error( msg );
            throw new CoreException( StatusFactory.createErrorStatus( msg ) );
        }
    	InputStream in = null;
        OutputStream out = null;
        int retValue = 0;
        try {
            in = new FileInputStream( srcFile );
            out = new FileOutputStream( dstFile );
            byte[] buf = new byte[ 64 * 1024 ];
            int len;
            while( ( len = in.read( buf ) ) > 0 ) {
                out.write( buf, 0, len );
            }
        } catch( final IOException e ) {
            if(e instanceof FileNotFoundException) {
                String msg = NLS.bind( Messages.DeploymentHelper_FILE_NOT_WRITABLE_MSG, dstFile.getParent() );
                _logger.error( msg );
                throw new CoreException( StatusFactory.createErrorStatus( msg ) );
            }
        	_logger.error( e.getMessage(), e );
            retValue = -1;
        } finally {
            try {
                if( in != null ) {
                    in.close();
                }
            } catch( IOException e ) {
                _logger.error( e.getMessage(), e );
                retValue = -1;
            }
            try {
                if( out != null ) {
                    out.close();
                }
            } catch( IOException e ) {
                _logger.error( e.getMessage(), e );
                retValue = -1;
            }
        }
        if( retValue == 0 ) {
            dstFile.setLastModified( srcFile.lastModified() );
        }
        return retValue;
    }

}
