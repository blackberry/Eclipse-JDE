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
import java.io.FileInputStream;
import java.util.Map;

import net.rim.ejde.internal.core.ContextManager;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.eval.IEvaluationContext;

/**
 * Custom eJDE BlackBerry properties for the project to be serialized as XML.
 *
 * @author cmalinescu, mcacenco, jkeshavarzi
 */

public final class BlackBerryProject implements IBlackBerryProject, IAdaptable {
    public static final String METAFILE = "BlackBerry_App_Descriptor.xml";
    public static final String CLDC_APPLICATION = "BlackBerry Application";
    public static final String MIDLET = "MIDlet";
    public static final String LIBRARY = "Library";

    private static final Logger _log = Logger.getLogger( BlackBerryProject.class );
    private final IJavaProject _eclipseJavaProject;
    private BlackBerryProperties _properties;
    private IFile _metaFileHandler;

    /**
     * Instantiates a new black berry project.
     *
     * @param eclipseJavaProject
     *            the eclipse java project
     */
    public BlackBerryProject( final IJavaProject eclipseJavaProject ) {
        if( null == eclipseJavaProject ) {
            throw new IllegalArgumentException( "Eclipse Java Project can't be null." );
        }
        _eclipseJavaProject = eclipseJavaProject;
        _properties = ContextManager.PLUGIN.getBBProperties( eclipseJavaProject.getProject().getName(), false );
        addStore();
    }

    /**
     * Instantiates a new black berry project.
     *
     * @param eclipseJavaProject
     *            the eclipse java project
     * @param properties
     *            the properties
     */
    public BlackBerryProject( final IJavaProject eclipseJavaProject, final BlackBerryProperties properties ) {
        if( null == eclipseJavaProject ) {
            throw new IllegalArgumentException( "Eclipse Java Project can't be null." );
        }
        if( null == properties ) {
            throw new IllegalArgumentException( "Project properties can't be null." );
        }
        _eclipseJavaProject = eclipseJavaProject;
        _properties = properties;
        addStore();
    }

    /**
     * Gets the meta file handler.
     *
     * @return the meta file handler
     */
    public IFile getMetaFileHandler() {
        return _metaFileHandler;
    }

    /**
     * Adds the store.
     */
    public void addStore() {
        final IProject eclipseProject = getProject();
        try {
            if( !eclipseProject.hasNature( BlackBerryProjectCoreNature.NATURE_ID ) ) {
                return;
            }
        } catch( CoreException e ) {
            _log.error( e.getMessage() );
        }
        _metaFileHandler = eclipseProject.getFile( BlackBerryProject.METAFILE );

        if( !_metaFileHandler.exists() ) {
            final File metaFile = _metaFileHandler.getLocation().toFile();

            if( !metaFile.exists() ) {
                try {
                    metaFile.createNewFile();
                    // no need to close the stream as the Workbench will do it
                    // through
                    // the handler.
                    _metaFileHandler.create( new FileInputStream( metaFile ), true, new NullProgressMonitor() );
                } catch( final Throwable t ) {
                    BlackBerryProject._log.debug( "", t );
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#decodeClasspathEntry(java.lang.String)
     */
    @Override
    public IClasspathEntry decodeClasspathEntry( final String encodedEntry ) {
        return _eclipseJavaProject.decodeClasspathEntry( encodedEntry );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#encodeClasspathEntry(org.eclipse.jdt .core.IClasspathEntry)
     */
    @Override
    public String encodeClasspathEntry( final IClasspathEntry classpathEntry ) {
        return _eclipseJavaProject.encodeClasspathEntry( classpathEntry );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findElement(org.eclipse.core.runtime .IPath)
     */
    @Override
    public IJavaElement findElement( final IPath path ) throws JavaModelException {
        return _eclipseJavaProject.findElement( path );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findElement(org.eclipse.core.runtime .IPath, org.eclipse.jdt.core.WorkingCopyOwner)
     */
    @Override
    public IJavaElement findElement( final IPath path, final WorkingCopyOwner owner ) throws JavaModelException {
        return _eclipseJavaProject.findElement( path, owner );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findElement(java.lang.String, org.eclipse.jdt.core.WorkingCopyOwner)
     */
    @Override
    public IJavaElement findElement( final String bindingKey, final WorkingCopyOwner owner ) throws JavaModelException {
        return _eclipseJavaProject.findElement( bindingKey, owner );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findPackageFragment(org.eclipse.core .runtime.IPath)
     */
    @Override
    public IPackageFragment findPackageFragment( final IPath path ) throws JavaModelException {
        return _eclipseJavaProject.findPackageFragment( path );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findPackageFragmentRoot(org.eclipse .core.runtime.IPath)
     */
    @Override
    public IPackageFragmentRoot findPackageFragmentRoot( final IPath path ) throws JavaModelException {
        return _eclipseJavaProject.findPackageFragmentRoot( path );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findPackageFragmentRoots(org.eclipse .jdt.core.IClasspathEntry)
     */
    @Override
    public IPackageFragmentRoot[] findPackageFragmentRoots( final IClasspathEntry entry ) {
        return _eclipseJavaProject.findPackageFragmentRoots( entry );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findType(java.lang.String)
     */
    @Override
    public IType findType( final String fullyQualifiedName ) throws JavaModelException {
        return _eclipseJavaProject.findType( fullyQualifiedName );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findType(java.lang.String, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public IType findType( final String fullyQualifiedName, final IProgressMonitor progressMonitor ) throws JavaModelException {
        return _eclipseJavaProject.findType( fullyQualifiedName, progressMonitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findType(java.lang.String, org.eclipse.jdt.core.WorkingCopyOwner)
     */
    @Override
    public IType findType( final String fullyQualifiedName, final WorkingCopyOwner owner ) throws JavaModelException {
        return _eclipseJavaProject.findType( fullyQualifiedName, owner );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findType(java.lang.String, java.lang.String)
     */
    @Override
    public IType findType( final String packageName, final String typeQualifiedName ) throws JavaModelException {
        return _eclipseJavaProject.findType( packageName, typeQualifiedName );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findType(java.lang.String, org.eclipse.jdt.core.WorkingCopyOwner,
     * org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public IType findType( final String fullyQualifiedName, final WorkingCopyOwner owner, final IProgressMonitor progressMonitor )
            throws JavaModelException {
        return _eclipseJavaProject.findType( fullyQualifiedName, owner, progressMonitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findType(java.lang.String, java.lang.String,
     * org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public IType findType( final String packageName, final String typeQualifiedName, final IProgressMonitor progressMonitor )
            throws JavaModelException {
        return _eclipseJavaProject.findType( packageName, typeQualifiedName, progressMonitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findType(java.lang.String, java.lang.String, org.eclipse.jdt.core.WorkingCopyOwner)
     */
    @Override
    public IType findType( final String packageName, final String typeQualifiedName, final WorkingCopyOwner owner )
            throws JavaModelException {
        return _eclipseJavaProject.findType( packageName, typeQualifiedName, owner );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#findType(java.lang.String, java.lang.String, org.eclipse.jdt.core.WorkingCopyOwner,
     * org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public IType findType( final String packageName, final String typeQualifiedName, final WorkingCopyOwner owner,
            final IProgressMonitor progressMonitor ) throws JavaModelException {
        return _eclipseJavaProject.findType( packageName, typeQualifiedName, owner, progressMonitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getAllPackageFragmentRoots()
     */
    @Override
    public IPackageFragmentRoot[] getAllPackageFragmentRoots() throws JavaModelException {
        return _eclipseJavaProject.getAllPackageFragmentRoots();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getNonJavaResources()
     */
    @Override
    public Object[] getNonJavaResources() throws JavaModelException {
        return _eclipseJavaProject.getNonJavaResources();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getOption(java.lang.String, boolean)
     */
    @Override
    public String getOption( final String optionName, final boolean inheritJavaCoreOptions ) {
        return _eclipseJavaProject.getOption( optionName, inheritJavaCoreOptions );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getOptions(boolean)
     */
    @Override
    public Map getOptions( final boolean inheritJavaCoreOptions ) {
        return _eclipseJavaProject.getOptions( inheritJavaCoreOptions );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getOutputLocation()
     */
    @Override
    public IPath getOutputLocation() throws JavaModelException {
        return _eclipseJavaProject.getOutputLocation();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getPackageFragmentRoot(java.lang.String )
     */
    @Override
    public IPackageFragmentRoot getPackageFragmentRoot( final String externalLibraryPath ) {
        return _eclipseJavaProject.getPackageFragmentRoot( externalLibraryPath );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getPackageFragmentRoot(org.eclipse. core.resources.IResource)
     */
    @Override
    public IPackageFragmentRoot getPackageFragmentRoot( final IResource resource ) {
        return _eclipseJavaProject.getPackageFragmentRoot( resource );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getPackageFragmentRoots()
     */
    @Override
    public IPackageFragmentRoot[] getPackageFragmentRoots() throws JavaModelException {
        return _eclipseJavaProject.getAllPackageFragmentRoots();
    }

    /*
     * @deprecated Use {@link IBlackBerryProject#findPackageFragmentRoots(IClasspathEntry)} instead
     */
    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getPackageFragmentRoots(org.eclipse .jdt.core.IClasspathEntry)
     */
    @Override
    public IPackageFragmentRoot[] getPackageFragmentRoots( final IClasspathEntry entry ) {
        return _eclipseJavaProject.getPackageFragmentRoots( entry );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getPackageFragments()
     */
    @Override
    public IPackageFragment[] getPackageFragments() throws JavaModelException {
        return _eclipseJavaProject.getPackageFragments();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getProject()
     */
    @Override
    public IProject getProject() {
        return _eclipseJavaProject.getProject();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getRawClasspath()
     */
    @Override
    public IClasspathEntry[] getRawClasspath() throws JavaModelException {
        return _eclipseJavaProject.getRawClasspath();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getRequiredProjectNames()
     */
    @Override
    public String[] getRequiredProjectNames() throws JavaModelException {
        return _eclipseJavaProject.getRequiredProjectNames();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getResolvedClasspath(boolean)
     */
    @Override
    public IClasspathEntry[] getResolvedClasspath( final boolean ignoreUnresolvedEntry ) throws JavaModelException {
        return _eclipseJavaProject.getResolvedClasspath( ignoreUnresolvedEntry );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#hasBuildState()
     */
    @Override
    public boolean hasBuildState() {
        return _eclipseJavaProject.hasBuildState();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#hasClasspathCycle(org.eclipse.jdt.core .IClasspathEntry[])
     */
    @Override
    public boolean hasClasspathCycle( final IClasspathEntry[] entries ) {
        return _eclipseJavaProject.hasClasspathCycle( entries );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#isOnClasspath(org.eclipse.jdt.core. IJavaElement)
     */
    @Override
    public boolean isOnClasspath( final IJavaElement element ) {
        return _eclipseJavaProject.isOnClasspath( element );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#isOnClasspath(org.eclipse.core.resources .IResource)
     */
    @Override
    public boolean isOnClasspath( final IResource resource ) {
        return _eclipseJavaProject.isOnClasspath( resource );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#newEvaluationContext()
     */
    @Override
    public IEvaluationContext newEvaluationContext() {
        return _eclipseJavaProject.newEvaluationContext();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#newTypeHierarchy(org.eclipse.jdt.core .IRegion,
     * org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public ITypeHierarchy newTypeHierarchy( final IRegion region, final IProgressMonitor monitor ) throws JavaModelException {
        return _eclipseJavaProject.newTypeHierarchy( region, monitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#newTypeHierarchy(org.eclipse.jdt.core .IRegion,
     * org.eclipse.jdt.core.WorkingCopyOwner, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public ITypeHierarchy newTypeHierarchy( final IRegion region, final WorkingCopyOwner owner, final IProgressMonitor monitor )
            throws JavaModelException {
        return _eclipseJavaProject.newTypeHierarchy( region, owner, monitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#newTypeHierarchy(org.eclipse.jdt.core .IType, org.eclipse.jdt.core.IRegion,
     * org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public ITypeHierarchy newTypeHierarchy( final IType type, final IRegion region, final IProgressMonitor monitor )
            throws JavaModelException {
        return _eclipseJavaProject.newTypeHierarchy( type, region, monitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#newTypeHierarchy(org.eclipse.jdt.core .IType, org.eclipse.jdt.core.IRegion,
     * org.eclipse.jdt.core.WorkingCopyOwner, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public ITypeHierarchy newTypeHierarchy( final IType type, final IRegion region, final WorkingCopyOwner owner,
            final IProgressMonitor monitor ) throws JavaModelException {
        return _eclipseJavaProject.newTypeHierarchy( type, region, owner, monitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#readOutputLocation()
     */
    @Override
    public IPath readOutputLocation() {
        return _eclipseJavaProject.readOutputLocation();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#readRawClasspath()
     */
    @Override
    public IClasspathEntry[] readRawClasspath() {
        return _eclipseJavaProject.readRawClasspath();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#setOption(java.lang.String, java.lang.String)
     */
    @Override
    public void setOption( final String optionName, final String optionValue ) {
        _eclipseJavaProject.setOption( optionName, optionValue );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#setOptions(java.util.Map)
     */
    @Override
    public void setOptions( final Map newOptions ) {
        _eclipseJavaProject.setOptions( newOptions );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#setOutputLocation(org.eclipse.core. runtime.IPath,
     * org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void setOutputLocation( final IPath path, final IProgressMonitor monitor ) throws JavaModelException {
        _eclipseJavaProject.setOutputLocation( path, monitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#setRawClasspath(org.eclipse.jdt.core .IClasspathEntry[],
     * org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void setRawClasspath( final IClasspathEntry[] entries, final IProgressMonitor monitor ) throws JavaModelException {
        _eclipseJavaProject.setRawClasspath( entries, monitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#setRawClasspath(org.eclipse.jdt.core .IClasspathEntry[], boolean,
     * org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void setRawClasspath( final IClasspathEntry[] entries, final boolean canModifyResources, final IProgressMonitor monitor )
            throws JavaModelException {
        _eclipseJavaProject.setRawClasspath( entries, canModifyResources, monitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#setRawClasspath(org.eclipse.jdt.core .IClasspathEntry[],
     * org.eclipse.core.runtime.IPath, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void setRawClasspath( final IClasspathEntry[] entries, final IPath outputLocation, final IProgressMonitor monitor )
            throws JavaModelException {
        _eclipseJavaProject.setRawClasspath( entries, outputLocation, monitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#setRawClasspath(org.eclipse.jdt.core .IClasspathEntry[],
     * org.eclipse.core.runtime.IPath, boolean, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void setRawClasspath( final IClasspathEntry[] entries, final IPath outputLocation, final boolean canModifyResources,
            final IProgressMonitor monitor ) throws JavaModelException {
        _eclipseJavaProject.setRawClasspath( entries, outputLocation, canModifyResources, monitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#setRawClasspath(org.eclipse.jdt.core .IClasspathEntry[], org.eclipse.jdt.core
     * .IClasspathEntry[],org.eclipse.core.runtime.IPath,org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void setRawClasspath( IClasspathEntry[] entries, IClasspathEntry[] referencedEntries, IPath outputLocation,
            IProgressMonitor monitor ) throws JavaModelException {
        _eclipseJavaProject.setRawClasspath( entries, referencedEntries, outputLocation, monitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IParent#getChildren()
     */
    @Override
    public IJavaElement[] getChildren() throws JavaModelException {
        return _eclipseJavaProject.getChildren();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IParent#hasChildren()
     */
    @Override
    public boolean hasChildren() throws JavaModelException {
        return _eclipseJavaProject.hasChildren();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#exists()
     */
    @Override
    public boolean exists() {
        return _eclipseJavaProject.exists();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getAncestor(int)
     */
    @Override
    public IJavaElement getAncestor( final int ancestorType ) {
        return _eclipseJavaProject.getAncestor( ancestorType );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getAttachedJavadoc(org.eclipse.core .runtime.IProgressMonitor)
     */
    @Override
    public String getAttachedJavadoc( final IProgressMonitor monitor ) throws JavaModelException {
        return _eclipseJavaProject.getAttachedJavadoc( monitor );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getCorrespondingResource()
     */
    @Override
    public IResource getCorrespondingResource() throws JavaModelException {
        return _eclipseJavaProject.getCorrespondingResource();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getElementName()
     */
    @Override
    public String getElementName() {
        return _eclipseJavaProject.getElementName();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getElementType()
     */
    @Override
    public int getElementType() {
        return _eclipseJavaProject.getElementType();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getHandleIdentifier()
     */
    @Override
    public String getHandleIdentifier() {
        return _eclipseJavaProject.getHandleIdentifier();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getJavaModel()
     */
    @Override
    public IJavaModel getJavaModel() {
        return _eclipseJavaProject.getJavaModel();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getJavaProject()
     */
    @Override
    public IJavaProject getJavaProject() {
        return _eclipseJavaProject.getJavaProject();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getOpenable()
     */
    @Override
    public IOpenable getOpenable() {
        return _eclipseJavaProject.getOpenable();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getParent()
     */
    @Override
    public IJavaElement getParent() {
        return _eclipseJavaProject.getParent();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getPath()
     */
    @Override
    public IPath getPath() {
        return _eclipseJavaProject.getPath();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getPrimaryElement()
     */
    @Override
    public IJavaElement getPrimaryElement() {
        return _eclipseJavaProject.getPrimaryElement();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getResource()
     */
    @Override
    public IResource getResource() {
        return _eclipseJavaProject.getResource();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getSchedulingRule()
     */
    @Override
    public ISchedulingRule getSchedulingRule() {
        return _eclipseJavaProject.getSchedulingRule();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#getUnderlyingResource()
     */
    @Override
    public IResource getUnderlyingResource() throws JavaModelException {
        return _eclipseJavaProject.getUnderlyingResource();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return _eclipseJavaProject.isReadOnly();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaElement#isStructureKnown()
     */
    @Override
    public boolean isStructureKnown() throws JavaModelException {
        return _eclipseJavaProject.isStructureKnown();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter( final Class adapter ) {
        return _eclipseJavaProject.getAdapter( adapter );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IOpenable#close()
     */
    @Override
    public void close() throws JavaModelException {
        _eclipseJavaProject.close();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IOpenable#findRecommendedLineSeparator()
     */
    @Override
    public String findRecommendedLineSeparator() throws JavaModelException {
        return _eclipseJavaProject.findRecommendedLineSeparator();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IOpenable#getBuffer()
     */
    @Override
    public IBuffer getBuffer() throws JavaModelException {
        return _eclipseJavaProject.getBuffer();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IOpenable#hasUnsavedChanges()
     */
    @Override
    public boolean hasUnsavedChanges() throws JavaModelException {
        return _eclipseJavaProject.hasUnsavedChanges();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IOpenable#isConsistent()
     */
    @Override
    public boolean isConsistent() throws JavaModelException {
        return _eclipseJavaProject.isConsistent();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IOpenable#isOpen()
     */
    @Override
    public boolean isOpen() {
        return _eclipseJavaProject.isOpen();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IOpenable#makeConsistent(org.eclipse.core.runtime .IProgressMonitor)
     */
    @Override
    public void makeConsistent( final IProgressMonitor progress ) throws JavaModelException {
        _eclipseJavaProject.makeConsistent( progress );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IOpenable#open(org.eclipse.core.runtime.IProgressMonitor )
     */
    @Override
    public void open( final IProgressMonitor progress ) throws JavaModelException {
        _eclipseJavaProject.open( progress );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IOpenable#save(org.eclipse.core.runtime.IProgressMonitor , boolean)
     */
    @Override
    public void save( final IProgressMonitor progress, final boolean force ) throws JavaModelException {
        _eclipseJavaProject.save( progress, force );
    }

    /*
     * (non-Javadoc)
     *
     * @see net.rim.ejde.internal.model.IBlackBerryProject#getProperties()
     */
    public BlackBerryProperties getProperties() {
        return _properties;
    }

    /**
     * Sets the BlackBerry properties.
     *
     * @param properties
     *            the new BlackBerry properties
     */
    public void setProperties( BlackBerryProperties properties ) {
        _properties = properties;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.IJavaProject#getReferencedClasspathEntries()
     */
    public IClasspathEntry[] getReferencedClasspathEntries() throws JavaModelException {
        // TODO Auto-generated method stub
        return _eclipseJavaProject.getReferencedClasspathEntries();
    }

}
