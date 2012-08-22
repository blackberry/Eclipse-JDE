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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IRIMMarker;
import net.rim.ejde.internal.model.BasicBlackBerryProperties.AlternateEntryPoint;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.NatureUtils;
import net.rim.ejde.internal.util.ResourceBuilderUtils;
import net.rim.ejde.internal.util.VMUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.swt.widgets.Display;

/**
 * The Class CompilerToProjectEditorManager is used to manage markers that can be generated both by the app descriptor and the
 * compiler.
 */
public class CompilerToAppDescriptorManager {

    /** The _lib main marker map. */
    private static Map< IJavaProject, List< IMarker >> _managedMarkerMap;

    /** The _managed problem map. */
    private static Map< ICompilationUnit, List< CategorizedProblem >> _managedProblemMap;

    /** The _problem to marker map. */
    private static Map< CategorizedProblem, IMarker > _problemToMarkerMap;

    /** The Constant _log. */
    static final Logger _log = Logger.getLogger( CompilerToAppDescriptorManager.class );

    /** The INSTANCE. */
    private static CompilerToAppDescriptorManager INSTANCE;

    /**
     * Gets the single instance of LibMainMethodManager.
     *
     * @return single instance of LibMainMethodManager
     */
    public static CompilerToAppDescriptorManager getInstance() {
        if( INSTANCE == null ) {
            INSTANCE = new CompilerToAppDescriptorManager();
        }
        return INSTANCE;
    }

    /**
     * Instantiates a new compiler to project editor manager.
     */
    private CompilerToAppDescriptorManager() {
        super();
        _managedMarkerMap = new HashMap< IJavaProject, List< IMarker > >();
        _managedProblemMap = new HashMap< ICompilationUnit, List< CategorizedProblem > >();
        _problemToMarkerMap = new HashMap< CategorizedProblem, IMarker >();
    }

    /**
     * Check for the error caused by having a libMain and not setting AutoStartup
     *
     * @param cu
     *            the cu
     *
     * @throws JavaModelException
     *             the java model exception
     * @throws CoreException
     *             the core exception
     */
    private void findLibMainInCU( ICompilationUnit cu ) throws JavaModelException, CoreException {
        IType primaryType;
        if( cu != null ) {
            primaryType = cu.findPrimaryType();
            if( primaryType == null ) {
                if( ( cu.getAllTypes() != null ) && ( cu.getAllTypes().length > 0 ) ) {
                    primaryType = cu.getAllTypes()[ 0 ];
                } else {
                    return;
                }
            }
            final IMethod libMainMethod = primaryType.getMethod( "libMain", new String[] { "[QString;" } ); //$NON-NLS-1$ //$NON-NLS-2$
            if( libMainMethod.exists() && ( libMainMethod.getExceptionTypes().length == 0 )
                    && Flags.isStatic( libMainMethod.getFlags() ) && Flags.isPublic( libMainMethod.getFlags() ) ) {
                final IJavaProject javaProject = cu.getJavaProject();
                IProject project = javaProject.getProject();
                if( NatureUtils.hasBBNature( project ) ) {
                    BlackBerryProperties properties = ContextManager.PLUGIN.getBBProperties( project.getName(), false );
                    if( properties._application.getType().equals( BlackBerryProject.LIBRARY )
                            && !properties._application.isAutostartup().booleanValue() ) {

                        Display.getDefault().asyncExec( new Runnable() {

                            @Override
                            public void run() {
                                IMarker problem;
                                try {
                                    problem = libMainMethod.getResource().createMarker( IRIMMarker.BLACKBERRY_PROBLEM );
                                    int startPos = libMainMethod.getSourceRange().getOffset(), endPos = startPos
                                            + libMainMethod.getSourceRange().getLength() - 1;
                                    problem.setAttributes(
                                            new String[] { IMarker.MESSAGE, IMarker.CHAR_START, IMarker.CHAR_END,
                                                    IMarker.SEVERITY, IRIMMarker.ID },
                                            new Object[] { Messages.CompilerToProjectEditorManager_libWarnMsg,
                                                    Integer.valueOf( startPos ), Integer.valueOf( endPos ),
                                                    Integer.valueOf( IMarker.SEVERITY_WARNING ),
                                                    Integer.valueOf( IRIMMarker.LIBMAIN_PROBLEM_ID ) } );

                                    addManagedMarker( javaProject, problem );
                                } catch( CoreException e ) {
                                    _log.error( "LibMainMethodManager.add: ", e );
                                }

                            }
                        } );
                    }
                }
            }
        }
    }

    /**
     * Checks for the error caused by having a compile time field usage and setting autoStartup
     *
     * @param cu
     *            the cu
     * @param problem
     *            the problem
     * @param hasCompileTimeValue
     *            the has compile time value
     */
    private void findCompileTimeFieldUse( final ICompilationUnit cu, final CategorizedProblem problem, boolean hasCompileTimeValue ) {
        final boolean hasMarker = _problemToMarkerMap.get( problem ) != null;

        if( !hasMarker && ( !hasCompileTimeValue ) ) {
            Display.getDefault().asyncExec( new Runnable() {

                @Override
                public void run() {
                    IMarker marker;
                    try {
                    	String preferenceLabel = VMUtils.convertCodeSignErrorMsgToPreferenceLabel(problem.getMessage());
                    	Integer key = VMUtils.convertPreferenceLabelToKey(preferenceLabel);

                        marker = cu.getResource().createMarker( IRIMMarker.CODE_SIGN_PROBLEM_MARKER );
                        marker.setAttributes(
                                new String[] { IMarker.MESSAGE, IMarker.CHAR_START, IMarker.CHAR_END, IMarker.SEVERITY,
                                               IRIMMarker.KEY, IRIMMarker.ID, IMarker.LOCATION },
                                new Object[] { problem.getMessage(), Integer.valueOf( problem.getSourceStart() ),
                                        Integer.valueOf( problem.getSourceEnd() ), Integer.valueOf( IMarker.SEVERITY_WARNING ),key,
                                        Integer.valueOf(IRIMMarker.FIELD_USAGE_CODE_SIGN_PROBLEM_ID),"line " +problem.getSourceLineNumber() } );
                        addMarkerForProblem( problem, marker );
                    } catch( CoreException e ) {
                        _log.error( "LibMainMethodManager.add: ", e );
                    }

                }
            } );
        }
    }

    /**
     * Checks if a given resource's BB project is set to autostartup
     *
     * @param res
     *            the res
     *
     * @return true, if is autostartup
     */
    static private boolean isAutostartup( IResource res ) {
        boolean isAutoStartup = false;
        IProject project = res.getProject();
        if( ( project != null ) && project.exists() && NatureUtils.hasBBNature( project ) ) {
            BlackBerryProperties properties = ContextManager.getDefault().getBBProperties( project.getName(), false );
            isAutoStartup |= properties._application.isAutostartup().booleanValue();
            for( AlternateEntryPoint entryPoint : properties.getAlternateEntryPoints() ) {
                isAutoStartup |= entryPoint.isAutostartup().booleanValue();
            }
        }
        return isAutoStartup;
    }

    /**
     * Deletes managed markers for a given project
     *
     * @param javaProject
     *            the java project
     */
    private void deleteMarkersForProject( IJavaProject javaProject ) {
        List< IMarker > recordedProblems = _managedMarkerMap.get( javaProject );
        _managedMarkerMap.put( javaProject, null );
        if( recordedProblems != null ) {
            for( final IMarker problem : recordedProblems ) {
                Display.getDefault().asyncExec( new Runnable() {

                    @Override
                    public void run() {
                        try {
                            problem.delete();
                        } catch( CoreException e ) {
                            _log.error( "LibMainMethodManager.clean: ", e );
                        }
                    }
                } );

            }
        }
    }

    /**
     * Delete managed problems and their associated markers for a given project
     *
     * @param project
     *            the project
     * @param preserveProblems
     *            whether or not to preserve the root problems
     *
     * @throws CoreException
     *             the core exception
     */
    private void deleteProblemsForProject( IJavaProject project, boolean preserveProblems ) throws CoreException {
        Set< ICompilationUnit > cus = _managedProblemMap.keySet();
        for( ICompilationUnit cu : cus ) {
            if( cu.getJavaProject().equals( project ) ) {
                deleteProblemsForCU( cu, preserveProblems );
            }
        }
    }

    /**
     * Deletes the managed markers associated with a CompilationUnit
     *
     * @param cu
     *            the cu
     *
     * @throws CoreException
     *             the core exception
     */
    private void deleteMarkersForCU( ICompilationUnit cu ) throws CoreException {
        IMarker[] markers = cu.getResource().findMarkers( IRIMMarker.BLACKBERRY_PROBLEM, true, IResource.DEPTH_ZERO );
        for( final IMarker marker : markers ) {
            Object id = marker.getAttribute( IRIMMarker.ID );
            if( id != null && id.equals( Integer.valueOf( IRIMMarker.LIBMAIN_PROBLEM_ID ) ) ) {
                Display.getDefault().asyncExec( new Runnable() {

                    @Override
                    public void run() {
                        try {
                            marker.delete();
                        } catch( CoreException e ) {
                            _log.error( "LibMainMethodManager.clean: ", e );
                        }
                    }
                } );
            }
        }
    }

    /**
     * Delete the managed problems and their associated markers for a given CompilationUnit
     *
     * @param cu
     *            the cu
     * @param preserveProblems
     *            whether or not to preserve the root problems
     *
     * @throws CoreException
     *             the core exception
     */
    private void deleteProblemsForCU( ICompilationUnit cu, boolean preserveProblems ) throws CoreException {
        List< CategorizedProblem > problems = _managedProblemMap.get( cu );
        // Delete Root problems for CU
        if( !preserveProblems ) {
            _managedProblemMap.put( cu, null );
        }
        // Delete markers associated with problems for CU
        if( problems != null ) {
            for( CategorizedProblem problem : problems ) {
                final IMarker marker = _problemToMarkerMap.get( problem );
                if( marker != null ) {
                    _problemToMarkerMap.put( problem, null );
                    Display.getDefault().asyncExec( new Runnable() {
                        @Override
                        public void run() {
                            try {
                                marker.delete();
                            } catch( CoreException e ) {
                                _log.error( "LibMainMethodManager.clean: ", e );
                            }
                        }
                    } );
                }
            }
        }
    }

    /**
     * Called when a Compilation unit is being re-compiled so all problems must be deleted.
     *
     * @param cu
     *            the cu
     *
     * @throws CoreException
     *             the core exception
     */
    public void onCompilationUnitCompile( ICompilationUnit cu ) throws CoreException {
        deleteMarkersForCU( cu );
        deleteProblemsForCU( cu, false );
        findLibMainInCU( cu );
        // No need to check for *** because the compiler will
    }

    /**
     * Called when a project has had a clean compilation. This means all problems must be deleted for this project.
     *
     * @param javaProject
     *            the java project
     *
     * @throws CoreException
     *             the core exception
     */
    public void onProjectClean( IJavaProject javaProject ) throws CoreException {
        deleteMarkersForProject( javaProject );
        deleteProblemsForProject( javaProject, false );
    }

    /**
     * On qualified name field usage.
     *
     * @param problem
     *            the problem
     * @param cu
     *            the cu
     * @param hasCompileTimeValue
     *            the has compile time value
     */
    public void onQualifiedNameFieldUsage( final CategorizedProblem problem, final ICompilationUnit cu,
            boolean hasCompileTimeValue ) {
        addManagedProblem( cu, problem );
        findCompileTimeFieldUse( cu, problem, hasCompileTimeValue );
    }

    /**
     * Called whenever the project properties have changes
     *
     * @param javaProject
     *            the java project
     */
    static public void onProjectPropertiesChange( IJavaProject javaProject ) {
        IProject project = javaProject.getProject();
        boolean isAutoStartup = isAutostartup( javaProject.getProject() );
        try {
            if( isAutoStartup ) {
                IMarker[] marks = project.findMarkers( IRIMMarker.CODE_SIGN_PROBLEM_MARKER, false, IResource.DEPTH_ONE );
                if( marks.length == 0 ) {
                    // create a new marker
                    ResourceBuilderUtils.createProblemMarker( project, IRIMMarker.CODE_SIGN_PROBLEM_MARKER,
                            "Run On Startup entry point requires signing with key: RIM Runtime API", 0, IMarker.SEVERITY_WARNING );
                }
            } else {
                ResourceBuilderUtils.cleanProblemMarkers( project, new String[] { IRIMMarker.CODE_SIGN_PROBLEM_MARKER },
                        IResource.DEPTH_ONE );
            }
        } catch( CoreException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // if( ( javaProject != null ) && javaProject.exists() ) {
        // try {
        // //Clean all the markers, they will be re-created if necessary
        // deleteMarkersForProject( javaProject );
        // //DO NOT delete the root problems, they will not be re-created without a compiler call
        // deleteProblemsForProject( javaProject, true );
        //
        // for( IPackageFragment pkgFragment : javaProject.getPackageFragments() ) {
        // for( ICompilationUnit cu : pkgFragment.getCompilationUnits() ) {
        // if( cu.exists() ) {
        // findLibMainInCU( cu );
        // findCompileTimeFieldUseInCU( cu );
        // }
        // }
        // }
        // } catch( JavaModelException e ) {
        // _log.error( "LibMainMethodManager.add: ", e );
        // } catch( CoreException e ) {
        // _log.error( "LibMainMethodManager.add: ", e );
        // }
        // }
    }

    /**
     * Put problem in map.
     *
     * @param javaProject
     *            the java project
     * @param problem
     *            the problem
     */
    void addManagedMarker( IJavaProject javaProject, IMarker problem ) {
        List< IMarker > recordedProblems = _managedMarkerMap.get( javaProject );
        if( recordedProblems == null ) {
            recordedProblems = new ArrayList< IMarker >();
        }
        recordedProblems.add( problem );
        _managedMarkerMap.put( javaProject, recordedProblems );
    }

    /**
     * Adds the managed problem.
     *
     * @param cu
     *            the cu
     * @param problem
     *            the problem
     */
    void addManagedProblem( ICompilationUnit cu, CategorizedProblem problem ) {
        List< CategorizedProblem > recordedProblems = _managedProblemMap.get( cu );
        if( recordedProblems == null ) {
            recordedProblems = new ArrayList< CategorizedProblem >();
        }
        recordedProblems.add( problem );
        _managedProblemMap.put( cu, recordedProblems );
    }

    /**
     * Adds the marker for problem.
     *
     * @param problem
     *            the problem
     * @param marker
     *            the marker
     */
    void addMarkerForProblem( CategorizedProblem problem, IMarker marker ) {
        _problemToMarkerMap.put( problem, marker );
    }

}
