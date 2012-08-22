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
package net.rim.ejde.internal.model;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

public interface IModelConstants {
    static public enum Events {
        IMPORT_FROM_LEGACY
    }

    static interface IArtifacts {

        static public enum DefaultDirectories {
            src, res
        }

        static interface ILegacy {
            public static enum IDs {
                cod, cso, debug, err, jar, jad, jdp, jdw, lst, rapc, wts, rrh, rrc
            }

            static public final class Workspace {
                private Workspace() {
                }

                public static IWorkspaceRoot getWorkspaceRoot() {
                    return ResourcesPlugin.getWorkspace().getRoot();
                }

                /**
                 * Returns a File pointing to the workspace root location
                 *
                 * @return
                 */
                public static File getWorkspaceDir() {
                    return getWorkspaceRoot().getLocation().toFile();
                }

                /**
                 * Returns the workspace root raw location as a string
                 *
                 * @return
                 */
                public static String getWorkspaceRawPath() {
                    return getWorkspaceRoot().getRawLocation().toPortableString();
                }

                /**
                 * The extension for the settings file of a BlackBerry workspace
                 **/
                public static final String MetaFileID = "." + IDs.jdw.name();
                public static final String MetaFileFilter = "*" + MetaFileID;

                static final public String DefaultMetaFileDirectoryID = ".BlackBerry";
                static final public String DefaultMetafileID = "BlackBerry";

                /**
                 * Provides a unique, default location for RIM BlackBerry workspace and projects private artifacts
                 */
                static final public String MetaFileDirectoryPath;

                /**
                 * Provides a unique, default file for RIM BlackBerry workspace definitions
                 */
                static final public String MetaFilePath;

                static {
                    MetaFileDirectoryPath = String.format( "%s%s%s", getWorkspaceRawPath(), File.separator,
                            DefaultMetaFileDirectoryID );
                    MetaFilePath = String.format( "%s%s%s%s", MetaFileDirectoryPath, File.separator, DefaultMetafileID,
                            MetaFileID );
                }

                static public File getMetaFileDirectory() {
                    return new File( MetaFileDirectoryPath );
                }

                static public IFolder getMetaFileDirectoryHandle() {
                    IWorkspaceRoot workspaceRoot = getWorkspaceRoot();

                    IResource resource = workspaceRoot.findMember( new Path( MetaFileDirectoryPath ) );

                    if( IResource.FOLDER == resource.getType() ) {
                        return (IFolder) resource;
                    }

                    return null;
                }

                static public File getMetaFile() {
                    return new File( MetaFilePath );
                }

                static public IFile getMetaFileHandle() {
                    IWorkspaceRoot workspaceRoot = getWorkspaceRoot();

                    IResource resource = workspaceRoot.findMember( new Path( MetaFilePath ) );

                    if( IResource.FILE == resource.getType() ) {
                        return (IFile) resource;
                    }

                    return null;
                }
            }

            static public final class Project {
                /**
                 * The extension for the settings file of a BlackBerry project
                 **/
                public static final String MetaFileID = "." + IDs.jdp.name();
                public static final String MetaFileFilter = "*" + MetaFileID;
            }
        }
    }
}
