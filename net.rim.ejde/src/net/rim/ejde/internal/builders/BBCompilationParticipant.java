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

import net.rim.ejde.internal.core.IRIMMarker;
import net.rim.ejde.internal.packaging.PackagingJob;
import net.rim.ejde.internal.signing.BBSigningKeys;
import net.rim.ejde.internal.util.NatureUtils;
import net.rim.ejde.internal.util.PackagingUtils;
import net.rim.ejde.internal.util.ProblemFactory;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ejde.internal.util.ResourceBuilderUtils;
import net.rim.ejde.internal.util.VMUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * The BBCompilationParticipant updates the compilation process for BB projects.
 */
public class BBCompilationParticipant extends CompilationParticipant {

    /**
     * The Class BBASTRequestor.
     */
    public class BBASTRequestor extends ASTRequestor {

        /** The _protected classes. */
        private List< String > _protectedClasses;

        /** The _signing keys. */
        private BBSigningKeys _signingKeys;

        /** The _build context map. */
        private Map< ICompilationUnit, BuildContext > _buildContextMap;

        /**
         * Instantiates a new bBAST requestor.
         *
         * @param buildContextMap
         *            the build context map
         * @param signingKeys
         *            the signing keys
         * @param protectedClasses
         *            the protected classes
         */
        public BBASTRequestor( Map< ICompilationUnit, BuildContext > buildContextMap, BBSigningKeys signingKeys,
                List< String > protectedClasses ) {
            super();
            _signingKeys = signingKeys;
            _protectedClasses = protectedClasses;
            _buildContextMap = buildContextMap;
        }

        /**
         * Gets the keys for all of the types that need to be resolved. If we ever need to add hidden methods we may need to add
         * these to the keys here as well.
         *
         * @return the keys
         */
        public String[] getKeys() {
            String[] keys = new String[ _protectedClasses.size() ];
            for( int i = 0; i < _protectedClasses.size(); i++ ) {
                keys[ i ] = "L" + _protectedClasses.get( i ).replaceAll( "[.]", "/" );
            }
            return keys;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.dom.ASTRequestor#acceptAST(org.eclipse.jdt.core.ICompilationUnit,
         * org.eclipse.jdt.core.dom.CompilationUnit)
         */
        @Override
        public void acceptAST( ICompilationUnit source, CompilationUnit ast ) {
            BBASTVisitor visitor = new BBASTVisitor( source, ast, _signingKeys, _protectedClasses );
            ast.accept( visitor );
            _buildContextMap.get( source ).recordNewProblems( visitor.getProblems() );
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.dom.ASTRequestor#acceptBinding(java.lang.String, org.eclipse.jdt.core.dom.IBinding)
         */
        @Override
        public void acceptBinding( String bindingKey, IBinding binding ) {
            // TODO Auto-generated method stub
            super.acceptBinding( bindingKey, binding );
        }

    }

    /**
     * The Class BBASTVisitor.
     */
    public class BBASTVisitor extends ASTVisitor {

        /** The _comp unit. */
        private CompilationUnit _compUnit;

        private ICompilationUnit _cu;

        /** The _created problems. */
        private List< CategorizedProblem > _createdProblems;

        /** The _protected classes. */
        private List< String > _protectedClasses;

        /** The _protected methods. */
        // private List<String> _protectedMethods;

        /** The _res. */
        private IResource _res;

        /** The _signing keys. */
        private BBSigningKeys _signingKeys;

        /**
         * Instantiates a new bBAST visitor.
         *
         * @param res
         *            the res
         * @param compUnit
         *            the compilation unit
         * @param signingKeys
         *            the signing keys
         * @param protectedClasses
         *            the protected classes
         */
        public BBASTVisitor( ICompilationUnit cu, CompilationUnit compUnit, BBSigningKeys signingKeys,
                List< String > protectedClasses ) {
            super( false );
            _cu = cu;
            _res = _cu.getResource();
            _compUnit = compUnit;
            _createdProblems = new ArrayList< CategorizedProblem >();
            _signingKeys = signingKeys;
            _protectedClasses = protectedClasses;
            // _protectedMethods = Arrays.asList( _signingKeys.getProtectedMethods());
        }

        /**
         * Creates the problem.
         *
         * @param node
         *            the node
         * @param className
         *            the class name
         * @param msg
         *            the msg
         *
         * @return the categorized problem
         */
        private CategorizedProblem createProblem( ASTNode node, String className, String msg ) {
            return createProblem( node, className, msg, null, null );
        }

        /**
         * Creates the problem.
         *
         * @param node
         *            the node
         * @param className
         *            the class name
         * @param msg
         *            the msg
         *
         * @return the categorized problem
         */
        private CategorizedProblem createProblem( ASTNode node, String className, String msg, String[] extraAttributeNames,
                Object[] extraAttributeValues ) {
            int startPos = node.getStartPosition(), lineNumber = _compUnit.getLineNumber( startPos ), endPos = startPos
                    + node.getLength() - 1;
            Integer key = _signingKeys.getKey( className );
            String msgPrefix = "Signing Required: " + VMUtils.convertKeyToPreferenceLabel( key, _signingKeys ) + ": ";
            return new BBCategorizedProblem( msgPrefix + msg, _res.getName().toCharArray(), startPos, endPos, lineNumber, key,
                    extraAttributeNames, extraAttributeValues );
        }

        /**
         * Gets the problems.
         *
         * @return the problems
         */
        public CategorizedProblem[] getProblems() {
            return _createdProblems.toArray( new CategorizedProblem[ _createdProblems.size() ] );
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.ImportDeclaration)
         */
        @Override
        public boolean visit( ImportDeclaration node ) {
            // do nothing for the import part
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.dom.ASTVisitor#endVisit(org.eclipse.jdt.core.dom.QualifiedName)
         */
        @Override
        public boolean visit( QualifiedName node ) {
            Name name = node.getQualifier();
            if( name != null ) {
                ITypeBinding typeBinding = name.resolveTypeBinding();
                if( typeBinding != null ) {
                    String className = typeBinding.getQualifiedName();
                    if( _protectedClasses.contains( className ) ) {
                        boolean hasCompileTimeValue = node.resolveConstantExpressionValue() != null;
                        // Fields with compileTime values will have their linking removed by the compiler so will not register a
                        // code signing warning UNLESS they are set to autoStartup.
                        CompilerToAppDescriptorManager.getInstance().onQualifiedNameFieldUsage(
                                createProblem( node, className, "Protected Class " + className, new String[] { IRIMMarker.DATA },
                                        new Object[] { Boolean.valueOf( hasCompileTimeValue ) } ), _cu, hasCompileTimeValue );
                        return false;
                    }
                }
            }
            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.SimpleName)
         */
        @Override
        public boolean visit( SimpleName node ) {
            IBinding binding = node.resolveBinding();
            if( binding != null ) {
                if( binding instanceof ITypeBinding ) {
                    ITypeBinding typeBinding = (ITypeBinding) binding;
                    String className = typeBinding.getQualifiedName();
                    if( className != null ) {
                        if( _protectedClasses.contains( className ) ) {
                            _createdProblems.add( createProblem( node, className, "Protected Class " + className ) );
                        }
                    }

                }
            }
            return true;
        }

        /**
         * This method could be left out and the cases would be caught by SimpleName, however then we would not highlight the
         * whole element. Additionally this will allow us to add back in hiddenMethods if this is ever a requirement.
         *
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.MethodInvocation)
         */
        @Override
        public boolean visit( MethodInvocation node ) {
            IMethodBinding methodBinding = node.resolveMethodBinding();
            if( methodBinding != null ) {
                ITypeBinding typeBinding = methodBinding.getDeclaringClass();
                if( typeBinding != null ) {
                    String className = typeBinding.getQualifiedName();
                    if( className != null ) {
                        if( _protectedClasses.contains( className ) ) {
                            _createdProblems.add( createProblem( node, className, "Protected Class " + className ) );
                            return false;
                        }
                        /**
                         * This should be the logic necessary to check for hidden methods but these do not seem to be required by
                         * the signing tool.
                         */
                        // String methodName = className+"."+methodBinding.getName()+"(";
                        // for(ITypeBinding methodParameterTypeBinding : methodBinding.getParameterTypes()){
                        // methodName += methodParameterTypeBinding.getKey();
                        // }
                        // methodName += ")"+methodBinding.getReturnType().getKey();
                        //
                        // if (_protectedMethods.contains(methodName) ){
                        // _createdProblems.add( createProblem( node, className, "Protected Method "+methodName ) );
                        // return false;
                        // }
                    }
                }
            }
            return true;
        }

    }

    /**
     * The Class BBCategorizedProblem.
     */
    class BBCategorizedProblem extends CategorizedProblem {

        /** The _msg. */
        private String _msg;

        /** The _orig file name. */
        private char[] _origFileName;

        /** The _line num. */
        private int _startPos, _endPos, _lineNum;

        private String[] _extraAttributeNames;

        private Object[] _extraAttributeValues;

        /**
         * Instantiates a new bB categorized problem.
         *
         * @param msg
         *            the msg
         * @param origFileName
         *            the orig file name
         * @param startPos
         *            the start pos
         * @param endPos
         *            the end pos
         * @param lineNum
         *            the line num
         * @param keyID
         *            the key id
         */
        public BBCategorizedProblem( String msg, char[] origFileName, int startPos, int endPos, int lineNum, Integer keyID ) {
            this( msg, origFileName, startPos, endPos, lineNum, keyID, null, null );
        }

        /**
         * Instantiates a new bB categorized problem.
         *
         * @param msg
         *            the msg
         * @param origFileName
         *            the orig file name
         * @param startPos
         *            the start pos
         * @param endPos
         *            the end pos
         * @param lineNum
         *            the line num
         * @param keyID
         *            the key id
         */
        public BBCategorizedProblem( String msg, char[] origFileName, int startPos, int endPos, int lineNum, Integer keyID,
                String[] extraAttributeNames, Object[] extraAttributeValues ) {
            super();
            _msg = msg;
            _origFileName = origFileName;
            _startPos = startPos;
            _endPos = endPos;
            _lineNum = lineNum;

            if( extraAttributeNames != null ) {
                int length = extraAttributeNames.length;
                _extraAttributeNames = new String[ 1 + length ];
                for( int i = 0; i < length; i++ ) {
                    _extraAttributeNames[ i ] = extraAttributeNames[ i ];
                }
                _extraAttributeNames[ length ] = IRIMMarker.KEY;
            } else {
                _extraAttributeNames = new String[] { IRIMMarker.KEY };
            }

            if( extraAttributeValues != null ) {
                int length = extraAttributeValues.length;
                _extraAttributeValues = new Object[ 1 + length ];
                for( int i = 0; i < length; i++ ) {
                    _extraAttributeValues[ i ] = extraAttributeValues[ i ];
                }
                _extraAttributeValues[ length ] = keyID;
            } else {
                _extraAttributeValues = new Object[] { keyID };
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.IProblem#getArguments()
         */
        @Override
        public String[] getArguments() {
            // We have no arguments currently
            return new String[ 0 ];
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.CategorizedProblem#getCategoryID()
         */
        @Override
        public int getCategoryID() {
            return IRIMMarker.CODE_SIGN_CATEGORY_ID;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.CategorizedProblem#getExtraMarkerAttributeNames()
         */
        @Override
        public String[] getExtraMarkerAttributeNames() {
            return _extraAttributeNames;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.CategorizedProblem#getExtraMarkerAttributeValues()
         */
        @Override
        public Object[] getExtraMarkerAttributeValues() {
            return _extraAttributeValues;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.IProblem#getID()
         */
        @Override
        public int getID() {
            return IRIMMarker.CODE_SIGN_PROBLEM_ID;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.CategorizedProblem#getMarkerType()
         */
        @Override
        public String getMarkerType() {
            return IRIMMarker.CODE_SIGN_PROBLEM_MARKER;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.IProblem#getMessage()
         */
        @Override
        public String getMessage() {
            return _msg;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.IProblem#getOriginatingFileName()
         */
        @Override
        public char[] getOriginatingFileName() {
            return _origFileName;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.IProblem#getSourceEnd()
         */
        @Override
        public int getSourceEnd() {
            return _endPos;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.IProblem#getSourceLineNumber()
         */
        @Override
        public int getSourceLineNumber() {
            return _lineNum;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.IProblem#getSourceStart()
         */
        @Override
        public int getSourceStart() {
            return _startPos;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.IProblem#isError()
         */
        @Override
        public boolean isError() {
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.IProblem#isWarning()
         */
        @Override
        public boolean isWarning() {
            return true;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.IProblem#setSourceEnd(int)
         */
        @Override
        public void setSourceEnd( int sourceEnd ) {
            _endPos = sourceEnd;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.IProblem#setSourceLineNumber(int)
         */
        @Override
        public void setSourceLineNumber( int lineNumber ) {
            _lineNum = lineNumber;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jdt.core.compiler.IProblem#setSourceStart(int)
         */
        @Override
        public void setSourceStart( int sourceStart ) {
            _startPos = sourceStart;
        }

    }

    /** The Constant _log. */
    private static final Logger _log = Logger.getLogger( BBCompilationParticipant.class );

    // private static long _totalTimeParsing = 0;

    /** The _current project. */
    private IJavaProject _currentProject;

    /**
     * Instantiates a new BB compilation participant.
     */
    public BBCompilationParticipant() {
        super();
        _currentProject = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.compiler.CompilationParticipant#aboutToBuild(org.eclipse.jdt.core.IJavaProject)
     */
    @Override
    public int aboutToBuild( IJavaProject project ) {
        _currentProject = project;
        return READY_FOR_BUILD;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.compiler.CompilationParticipant#buildFinished(org.eclipse.jdt.core.IJavaProject)
     */
    @Override
    public void buildFinished( IJavaProject project ) {
        // check run on startup property
        CompilerToAppDescriptorManager.onProjectPropertiesChange( project );
        // _buildStop = System.currentTimeMillis();
        // _log.debug( "*********************************** BUILD STOPPING  ***********************************" );
        // _totalBuiltTime += ( _buildStop - _buildStart );
        // _totalBuiltTimeWClean += ( _buildStop - _cleanStart );
        // _log.debug( "Total build time: " + _totalBuiltTime );
        // _log.debug( "Time lost this session: " + _builtTimeLost );
        // double result = ( _builtTimeLost / ( _totalBuiltTime * 1.0 ) ) * 100;
        // _log.debug( "Percentage of Build Time Lost: " + result );
        // _log.debug( "*********************************** LOST TIME STATS  *********************************" );
        // _log.debug( "Time time lost: " + _totalTimeLost );
        // _log.debug( "Total time finding Java Element: " + _totalTimeFindingJE );
        // result = ( _totalTimeFindingJE / ( _totalTimeLost * 1.0 ) ) * 100;
        // _log.debug( "Total time Parsing: " + _totalTimeParsing );
        // result = ( _totalTimeParsing / ( _totalTimeLost * 1.0 ) ) * 100;
        // _log.debug( "Percentage of Lost Time Parsing: " + result );
        // _log.debug( "Total time Visiting: " + _totalTimeVisiting );
        // result = ( _totalTimeVisiting / ( _totalTimeLost * 1.0 ) ) * 100;
        // _log.debug( "Percentage of Lost Time Visiting: " + result );
        // _log.debug( "*********************************** OVERALL STATS  ***********************************" );
        // _log.debug( "Total build time: " + _totalBuiltTime );
        // _log.debug( "Total build time from clean: " + _totalBuiltTimeWClean );
        // _log.debug( "Time time lost: " + _totalTimeLost );
        // result = ( _totalTimeLost / ( _totalBuiltTime * 1.0 ) ) * 100;
        // _log.debug( "Percentage of Build Time Lost: " + result );
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.compiler.CompilationParticipant#buildStarting(org.eclipse.jdt.core.compiler.BuildContext[],
     * boolean)
     */
    @Override
    public void buildStarting( BuildContext[] files, boolean isBatch ) {
        // _log.debug( "*********************************** BUILD STARTING ***********************************" );

        // 5 files determined experimentally to be about the difference to make batch processing worth it.
        boolean runBatch = isBatch && ( files.length > 5 );
        //        _log.trace( "Entering BBCompilationParticipant buildStarting();Running As Batch=" + runBatch ); //$NON-NLS-1$
        // long start = System.currentTimeMillis();
        if( files.length > 0 ) {
            // mark this project is built by the java builder
            IProject project = files[ 0 ].getFile().getProject();
            // remove the packaging problems
            try {
                ResourceBuilderUtils.cleanProblemMarkers( project, new String[] { IRIMMarker.SIGNATURE_TOOL_PROBLEM_MARKER,
                        IRIMMarker.PACKAGING_PROBLEM }, IResource.DEPTH_INFINITE );
            } catch( CoreException e ) {
                _log.error( e.getMessage() );
            }
            PackagingJob.setBuiltByJavaBuilders( project, true );
            try {
                IJavaElement javaElem;
                Map< ICompilationUnit, BuildContext > buildContextMap = new HashMap< ICompilationUnit, BuildContext >();
                IVMInstall vm = JavaRuntime.getVMInstall( _currentProject );
                if( ( vm == null ) || !VMUtils.isBlackBerryVM( vm ) ) {
                    throw ProblemFactory.create_VM_MISSING_exception( _currentProject.getElementName() );
                }
                BBSigningKeys signingKeys = null;
                List< String > protectedClasses = null;
                boolean needParse = false;
                // No need to parse when there are no protected classes
                for( BuildContext file : files ) {
                    if( !NatureUtils.hasBBNature( file.getFile().getProject() ) ) {
                        continue;
                    }
                    // we only initialize these variables if there is any file in a BB project
                    if( ( signingKeys == null ) || ( protectedClasses == null ) ) {
                        signingKeys = VMUtils.addSignKeysToCache( vm );
                        protectedClasses = VMUtils.getHiddenClassesFilteredByPreferences( vm.getName() );
                        needParse = protectedClasses.size() > 0;
                    }

                    IPath srcFilePath = file.getFile().getProjectRelativePath();
                    // For some odd reason we must remove the original pkg name or it won't be found.
                    if( srcFilePath.segmentCount() > 1 ) {
                        srcFilePath = srcFilePath.removeFirstSegments( 1 );
                    }
                    javaElem = _currentProject.findElement( srcFilePath );
                    if( ( javaElem != null ) && ( javaElem instanceof ICompilationUnit ) ) {
                        ICompilationUnit cu = (ICompilationUnit) javaElem;

                        CompilerToAppDescriptorManager.getInstance().onCompilationUnitCompile( cu );

                        if( needParse ) {
                            if( runBatch ) {
                                buildContextMap.put( cu, file );
                            } else {
                                ASTParser parser = ASTParser.newParser( AST.JLS3 );
                                parser.setResolveBindings( true );
                                parser.setSource( cu );
                                parser.setProject( _currentProject );

                                CompilationUnit astRoot = (CompilationUnit) parser.createAST( new NullProgressMonitor() );

                                BBASTVisitor visitor = new BBASTVisitor( cu, astRoot, signingKeys, protectedClasses );
                                astRoot.accept( visitor );

                                file.recordNewProblems( visitor.getProblems() );
                            }
                        }
                    } else {
                        // Will be the case for Resources
                        // _log.error( "buildStarting: Error retrieving source to Parse for file " + file.getFile().getName()
                        // );
                    }
                }

                if( needParse && runBatch ) {

                    ASTParser parser = ASTParser.newParser( AST.JLS3 );
                    parser.setResolveBindings( true );
                    parser.setProject( _currentProject );

                    BBASTRequestor astRequestor = new BBASTRequestor( buildContextMap, signingKeys, protectedClasses );

                    parser.createASTs( buildContextMap.keySet().toArray( new ICompilationUnit[ buildContextMap.size() ] ),
                            astRequestor.getKeys(), astRequestor, new NullProgressMonitor() );
                }

            } catch( JavaModelException jme ) {
                _log.error( "buildStarting: Error Parsing for Access Restrictions", jme );
            } catch( CoreException ce ) {
                _log.error( "buildStarting: " + ce.getMessage() );
            }
            // long stop = System.currentTimeMillis();
            // long result = stop - start;
            // _totalTimeParsing += result;
            // _log.debug( _currentProject.getElementName() + " \t " + result + " \t Total \t " + _totalTimeParsing );
            // _log.debug( "*******************************************************************************" );
            //            _log.trace( "Leaving BBCompilationParticipant buildStarting()" ); //$NON-NLS-1$
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.compiler.CompilationParticipant#cleanStarting(org.eclipse.jdt.core.IJavaProject)
     */
    @Override
    public void cleanStarting( IJavaProject project ) {
        // _log.debug( "*********************************** CLEAN STARTING ***********************************" );
        // _cleanStart = System.currentTimeMillis();
        try {
            PackagingUtils.cleanProjectOutputFolder( project );
            CompilerToAppDescriptorManager.getInstance().onProjectClean( project );
            // remove the packaging problems
            try {
                ResourceBuilderUtils.cleanProblemMarkers( project.getProject(), new String[] {
                        IRIMMarker.SIGNATURE_TOOL_PROBLEM_MARKER, IRIMMarker.PACKAGING_PROBLEM }, IResource.DEPTH_INFINITE );
            } catch( CoreException e ) {
                _log.error( e.getMessage() );
            }
        } catch( CoreException e ) {
            _log.error( e );
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jdt.core.compiler.CompilationParticipant#isActive(org.eclipse.jdt.core.IJavaProject)
     */
    @Override
    public boolean isActive( IJavaProject project ) {
        if( NatureUtils.hasBBNature( project.getProject() ) || ProjectUtils.isDependedByBBProject( project.getProject() ) ) {
            return true;
        }
        return false;
    }
}
