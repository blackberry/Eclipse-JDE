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
package net.rim.ejde.internal.preprocessing.hook;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.ArrayList;

import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.HookConfigurator;
import org.eclipse.osgi.baseadaptor.HookRegistry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.baseadaptor.loader.ClasspathEntry;
import org.eclipse.osgi.baseadaptor.loader.ClasspathManager;
import org.eclipse.osgi.framework.adaptor.BundleProtectionDomain;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.util.ManifestElement;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.osgi.framework.BundleException;

/**
 * Hooks the classloading functionality for preprocessing functionality.
 */
public class PreprocessingClassLoadingHook implements ClassLoadingHook, HookConfigurator {
    private static final String SOURCE_FILE_CLASS = "org.eclipse.jdt.internal.core.builder.SourceFile";

    private static final String SOURCEMAPPER_PACKAGE_NAME = "net.rim.ejde.external.sourceMapper";

    // Type reference to the IFile interface
    private static Type IFILE_TYPE = Type.getType( "Lorg/eclipse/core/resources/IFile;" );

    // Type reference to the JDT SourceFile class
    private static Type SOURCEFILE_TYPE = Type.getType( "Lorg/eclipse/jdt/internal/core/builder/SourceFile;" );

    // Type reference to the SourceMapperAccess type
    private static String SOURCE_MAPPER_CLASS = "net.rim.ejde.external.sourceMapper.SourceMapperAccess";

    private static Type SOURCEMAPPERACCESS_TYPE = Type.getType( "Lnet/rim/ejde/external/sourceMapper/SourceMapperAccess;" );
    // name of the SourceMapperAccess.isHookCodeInstalled() method
    private static final String IS_HOOK_INSTALLED_METHOD = "isHookCodeInstalled";
    // name of the SourceFile.getContents() method
    private static final String GET_CONTENTS_METHOD = "getContents";
    // name of the SourceFile.resource field
    private static final String RESOURCE_FIELD = "resource";
    // name of the SourceMapperAccess.getMappedSourceFile() method
    private static final String GET_MAPPED_SOURCE_FILE_METHOD = "getMappedSourceFile";

    /**
     * Class adapter for rewriting the <code>SourceFile#getContents</code> method.
     *
     */
    class SourceFileClassAdapter extends ClassAdapter {
        /**
         * Construct a new adapter instance.
         *
         * @param cv
         */
        public SourceFileClassAdapter( ClassVisitor cv ) {
            super( cv );
        }

        /**
         * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String,
         *      java.lang.String[])
         */
        public MethodVisitor visitMethod( int access, String name, String desc, String signature, String[] exceptions ) {
            MethodVisitor methodVisitor = super.visitMethod( access, name, desc, signature, exceptions );

            if( name.equals( GET_CONTENTS_METHOD ) ) {
                // log.debug("SourceFile#getContents spotted.  Rewriting method."
                // );

                methodVisitor = new GetContentsMethodVisitor( methodVisitor );
            }

            return methodVisitor;
        }
    }

    /**
     * Class adapter for rewriting the SourceMapperAccess class
     */
    class SourceMapperAccessClassAdapter extends ClassAdapter {
        /**
         * Construct a new adapter instance.
         *
         * @param cv
         */
        public SourceMapperAccessClassAdapter( ClassVisitor cv ) {
            super( cv );
        }

        /**
         * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String,
         *      java.lang.String[])
         */
        public MethodVisitor visitMethod( int access, String name, String desc, String signature, String[] exceptions ) {
            MethodVisitor methodVisitor = super.visitMethod( access, name, desc, signature, exceptions );

            if( name.equals( IS_HOOK_INSTALLED_METHOD ) ) {
                // log.trace(
                // "SourceMapperAccess#isHookInstalled spotted.  Rewriting method."
                // );

                methodVisitor = new IsHookInstalledMethodVisitor( methodVisitor );
            }

            return methodVisitor;
        }

    }

    /**
     * MethodVisitor implementation to rewrite the getContents method of the SourceFile class.
     */
    class GetContentsMethodVisitor extends MethodAdapter {
        boolean foundReturn = false;

        public GetContentsMethodVisitor( MethodVisitor mv ) {
            super( mv );
        }

        /**
         * @see org.objectweb.asm.MethodAdapter#visitFieldInsn(int, java.lang.String, java.lang.String, java.lang.String)
         */
        public void visitFieldInsn( int opcode, String owner, String name, String desc ) {
            if( ( opcode == Opcodes.GETFIELD ) && ( name.equals( RESOURCE_FIELD ) ) && !foundReturn ) {
                insertMappedResourceCode();
            } else {
                super.visitFieldInsn( opcode, owner, name, desc );
            }
        }

        /**
         * @see org.objectweb.asm.MethodAdapter#visitInsn(int)
         */
        public void visitInsn( int opcode ) {
            if( !foundReturn && ( opcode == Opcodes.ARETURN ) ) {
                // log.trace("Found ARETURN in method.");

                foundReturn = true;
            }

            super.visitInsn( opcode );
        }

        /**
         * Insert the code that will attempt to request the mapped resource from the {@link SourceMapperTracker}.
         */
        private void insertMappedResourceCode() {
            // log.trace("Inserting mapped resource lookup code into method.");

            // We want to rewrite the first GETFIELD call for the resource
            // object as a call to the mapping function
            Label endLabel = new Label();

            // Call SourceMapperAccess#getMappedSourceFile(IFile) to get the
            // source file
            // log.trace("Rewritten SourceFile");
            super.visitInsn( Opcodes.POP );
            super.visitVarInsn( Opcodes.ALOAD, 0 ); // Load "this"
            super.visitFieldInsn( Opcodes.GETFIELD, SOURCEFILE_TYPE.getInternalName(), RESOURCE_FIELD, IFILE_TYPE.getDescriptor() );
            super.visitMethodInsn( Opcodes.INVOKESTATIC, SOURCEMAPPERACCESS_TYPE.getInternalName(),
                    GET_MAPPED_SOURCE_FILE_METHOD, Type.getMethodDescriptor( IFILE_TYPE, new Type[] { IFILE_TYPE } ) );
            // If the result was not null, we are done
            super.visitInsn( Opcodes.DUP );
            super.visitJumpInsn( Opcodes.IFNONNULL, endLabel );
            // otherwise we need to clear the extra copy and load the raw
            // resource
            // log.trace("Mapped resource was null");
            super.visitInsn( Opcodes.POP );
            // Load the local (raw) resource
            // log.trace("Using raw resource");
            super.visitVarInsn( Opcodes.ALOAD, 0 ); // Load "this"
            super.visitFieldInsn( Opcodes.GETFIELD, SOURCEFILE_TYPE.getInternalName(), RESOURCE_FIELD, IFILE_TYPE.getDescriptor() );
            // All done with the rewrite
            visitLabel( endLabel );
            // log.trace("Finished generated code");
        }
    }

    /**
     * MethodVisitor implementation to rewrite the isHookInstalled method of the SourceMapperAccess class.
     */
    class IsHookInstalledMethodVisitor extends MethodAdapter {
        public IsHookInstalledMethodVisitor( MethodVisitor mv ) {
            super( mv );
        }

        public void visitInsn( int opcode ) {
            if( opcode == Opcodes.ICONST_0 ) {
                opcode = Opcodes.ICONST_1;
            }

            super.visitInsn( opcode );
        }
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook#addClassPathEntry(java.util.ArrayList, java.lang.String,
     *      org.eclipse.osgi.baseadaptor.loader.ClasspathManager, org.eclipse.osgi.baseadaptor.BaseData,
     *      java.security.ProtectionDomain)
     */
    public boolean addClassPathEntry( ArrayList cpEntries, String cp, ClasspathManager hostmanager, BaseData sourcedata,
            ProtectionDomain sourcedomain ) {
        return false;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook#createClassLoader(java.lang.ClassLoader,
     *      org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate, org.eclipse.osgi.framework.adaptor.BundleProtectionDomain,
     *      org.eclipse.osgi.baseadaptor.BaseData, java.lang.String[])
     */
    public BaseClassLoader createClassLoader( ClassLoader parent, ClassLoaderDelegate delegate, BundleProtectionDomain domain,
            BaseData data, String[] bundleclasspath ) {
        boolean isJdtCore = "org.eclipse.jdt.core".equals( data.getSymbolicName() );
        boolean isEclipseBundleLoader = delegate instanceof BundleLoader;

        // If the bundle loader is of the corrent type and is for
        // the JDT core bundle, we will go ahead and force an OSGi "wire"
        // back to our package implementation from the JDT plugin.
        if( isEclipseBundleLoader && isJdtCore ) {
            // // log.trace("Adding dynamic import into JDT Core bundle");
            //
            // try {
            // ManifestElement[] dynamicElements = ManifestElement.parseHeader( "DynamicImport-Package",
            // SOURCEMAPPER_PACKAGE_NAME );
            // ( (BundleLoader) delegate ).addDynamicImportPackage( dynamicElements );
            // } catch( BundleException e ) {
            // // log.equals(e);
            // }
            // }

            // CAS - The BundleLoader class has been refactored in the 3.5
            // (Galileo)
            // release to a new package. To avoid issues with class loading, the
            // following drops into reflection to do the work.
            ManifestElement[] dynamicElements = getSourceMapperManifestElements();
            Class< ? extends ClassLoaderDelegate > clazz = delegate.getClass();
            Method dynamicImportMethod = null;

            try {
                dynamicImportMethod = clazz.getMethod( "addDynamicImportPackage", dynamicElements.getClass() );
            } catch( NoSuchMethodException e ) {
                // Do nothing...
            }

            if( dynamicImportMethod != null ) {
                try {
                    dynamicImportMethod.invoke( delegate, new Object[] { dynamicElements } );
                } catch( Exception e ) {
                    e.printStackTrace();
                }
            }
        }

        // Let the framework know that we did not create the classloader
        return null;
    }

    private static ManifestElement[] sourceMapperManifestElements;

    /**
     * Retrieve the dynamic manifest elements for use in linking in the source mapper functionality.
     *
     * @return
     */
    private static ManifestElement[] getSourceMapperManifestElements() {
        if( sourceMapperManifestElements == null ) {
            try {
                sourceMapperManifestElements = ManifestElement.parseHeader( "DynamicImport-Package", SOURCEMAPPER_PACKAGE_NAME );
            } catch( BundleException e ) {
                e.printStackTrace();
            }
        }
        return sourceMapperManifestElements;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook#findLibrary(org.eclipse.osgi.baseadaptor.BaseData,
     *      java.lang.String)
     */
    public String findLibrary( BaseData data, String libName ) {
        // Nothing to do
        return null;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook#getBundleClassLoaderParent()
     */
    public ClassLoader getBundleClassLoaderParent() {
        // Nothing to do
        return null;
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook#initializedClassLoader(org.eclipse.osgi.baseadaptor.loader.BaseClassLoader,
     *      org.eclipse.osgi.baseadaptor.BaseData)
     */
    public void initializedClassLoader( BaseClassLoader baseClassLoader, BaseData data ) {
        //
    }

    /**
     * @see org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook#processClass(java.lang.String, byte[],
     *      org.eclipse.osgi.baseadaptor.loader.ClasspathEntry, org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry,
     *      org.eclipse.osgi.baseadaptor.loader.ClasspathManager)
     */
    public byte[] processClass( String name, byte[] classbytes, ClasspathEntry classpathEntry, BundleEntry entry,
            ClasspathManager manager ) {
        byte[] processed = null;

        if( SOURCE_FILE_CLASS.equals( name ) || SOURCE_MAPPER_CLASS.equals( name ) ) {
            processed = rewriteSourceFileClass( name, classbytes );
        }

        return processed;
    }

    /**
     * Rewrite the SourceFile class given the specified class bytes.
     *
     * @param classBytes
     * @return
     */
    private byte[] rewriteSourceFileClass( String name, byte[] classBytes ) {
        byte[] rewritten = classBytes;

        // log.trace(name + " located.  Rewriting class bytes.");
        // Use ASM to rewrite the SourceFile.getMethods call
        ClassReader classReader = new ClassReader( classBytes );
        ClassWriter classWriter = new ClassWriter( ClassWriter.COMPUTE_MAXS );
        // ClassWriter classWriter = new ClassWriter(true);

        ClassAdapter adapter = null;
        if( SOURCE_FILE_CLASS.equals( name ) ) {
            adapter = new SourceFileClassAdapter( classWriter );
        } else {
            adapter = new SourceMapperAccessClassAdapter( classWriter );
        }

        classReader.accept( adapter, ClassReader.SKIP_FRAMES );
        // classReader.accept(adapter, false);
        rewritten = classWriter.toByteArray();

        return rewritten;
    }

    public void addHooks( HookRegistry hookRegistry ) {
        hookRegistry.addClassLoadingHook( this );

    }
}
