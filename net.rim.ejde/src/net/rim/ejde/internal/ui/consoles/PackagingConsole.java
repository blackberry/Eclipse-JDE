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
package net.rim.ejde.internal.ui.consoles;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.util.ProjectUtils;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Console for RAPC output that uses special highlighting to link resources.
 */
public class PackagingConsole extends MessageConsole {

    private Logger log = Logger.getLogger( PackagingConsole.class );
    private static PackagingConsole rapcConsole;

    // The difference between a "LinePattern" and a simple "Pattern" is that a
    // LinePattern matches a whole line that contains that pattern. A Pattern
    // will return the exact occurence of that thing. This is confusing, I
    // agree, but saves time having to compile regex'es dynamically on the fly.
    static public final Pattern errorLinePattern = Pattern.compile( "\\S.*\\.[Jj][Aa][Vv][Aa]:\\d+:\\s.*" );
    static public final Pattern errorPattern = Pattern.compile( "\\S.*\\.[Jj][Aa][Vv][Aa]:\\d+:" );
    static public final Pattern projectLinePattern = Pattern.compile( "(Cleaning|Building|Project).*" );

    private static Pattern projectPattern = null;

    private String cachedProjectName;

    private PackagingConsole() {
        this( "BlackBerry Packaging Console" );
        // Cache the initial project pattern and depend on
        // RimResourceChangeAdapter.java to update it when necessary.
        updateProjectPattern();

        this.addPatternMatchListener( new RapcPatternMatchListener() );
        // this.setConsoleWidth(80);
    }

    public static PackagingConsole getInstance() {
        if( rapcConsole == null ) {
            synchronized( PackagingConsole.class ) {
                rapcConsole = new PackagingConsole();
            }
        }
        return rapcConsole;
    }

    private PackagingConsole( String name ) {
        super( name, null );
    }

    public MessageConsoleStream newMessageStream() {
		return new BBLogConsoleStream(this);
	}

    /**
     * Pattern matcher that recognizes project names.
     */
    class RapcPatternMatchListener implements IPatternMatchListener {

        private TextConsole console;

        public RapcPatternMatchListener() {
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#connect(org. eclipse.ui.console.TextConsole)
         */
        public void connect( TextConsole console ) {
            this.console = console;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#disconnect()
         */
        public void disconnect() {
            console = null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.console.IPatternMatchListenerDelegate#matchFound(org .eclipse.ui.console.PatternMatchEvent)
         */
        public void matchFound( PatternMatchEvent event ) {
            try {
                int offset = event.getOffset();
                int length = event.getLength();

                String match = console.getDocument().get( event.getOffset(), event.getLength() );
                if( projectLinePattern.matcher( match ).matches() ) {
                    processProject( match, offset, length );
                } else if( errorLinePattern.matcher( match ).matches() ) {
                    processError( match, offset, length );
                }
            } catch( BadLocationException e ) {
                log.error( e.getMessage(), e );
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.console.IPatternMatchListener#getCompilerFlags()
         */
        public int getCompilerFlags() {
            // See flags here:
            // http://java.sun.com/j2se/1.4.2/docs/api/java/util/regex/Pattern.
            // html#compile(java.lang.String,%20int)
            return 0;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.console.IPatternMatchListener#getLineQualifier()
         */
        public String getLineQualifier() {
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.console.IPatternMatchListener#getPattern()
         */
        public String getPattern() {
            return ".*\\w.*"; //$NON-NLS-1$
        }

        private void processProject( String match, int offset, int length ) throws BadLocationException {
            if( projectPattern == null ) {
                return;
            }
            Matcher matcher = projectPattern.matcher( match );
            if( matcher.find() ) {
                String projectName = matcher.group();

                // This lets the subsequent error lines (if any, have something
                // to reduce the lookup scope by...)
                cachedProjectName = projectName;

                ProjectHyperlink link = new ProjectHyperlink( projectName );
                console.addHyperlink( link, offset + matcher.start(), matcher.end() - matcher.start() );
            }
        }

        private void processError( String match, int offset, int length ) throws BadLocationException {
            Matcher matcher = errorPattern.matcher( match );
            if( matcher.find() ) {
                SourceCodeHyperlink link = new SourceCodeHyperlink( matcher.group(), cachedProjectName );
                console.addHyperlink( link, offset + matcher.start(), matcher.end() - matcher.start() - 1 );
            }
        }
    }

    /**
     * Hyperlink class that opens up the package explorer declaration when clicked.
     */
    class ProjectHyperlink implements IHyperlink {
        private String projectName;

        public ProjectHyperlink( String projectName ) {
            this.projectName = projectName;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.console.IHyperlink#linkActivated()
         */
        @SuppressWarnings("restriction")
        public void linkActivated() {
            PackageExplorerPart view = PackageExplorerPart.openInActivePerspective();
            IJavaProject javaProject = JavaCore.create( ResourcesPlugin.getWorkspace().getRoot().getProject( projectName ) );
            view.tryToReveal( javaProject );
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.console.IHyperlink#linkEntered()
         */
        public void linkEntered() {
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.console.IHyperlink#linkExited()
         */
        public void linkExited() {
        }
    }

    /**
     * A hyperlink that opens up a line of source code in an editor. It searchs all project source folders.
     */
    class SourceCodeHyperlink implements IHyperlink {
        private String code;
        private String projectName;

        public SourceCodeHyperlink( String code, String projectName ) {
            this.code = code;
            this.projectName = projectName;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.console.IHyperlink#linkActivated()
         */
        public void linkActivated() {
            int lastColon = code.lastIndexOf( ':' );
            int secondLastColon = code.lastIndexOf( ':', lastColon - 1 );

            String file = code.substring( 0, secondLastColon );
            int lineNumber = Integer.parseInt( code.substring( secondLastColon + 1, lastColon ) );
            IPath filePath = new Path( file );

            log.debug( "Jumping to " + filePath.toOSString() + " on line " + lineNumber ); //$NON-NLS-1$ //$NON-NLS-2$

            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject( projectName );

            IResource resource = ProjectUtils.getResource( project, filePath.toFile() );
            try {
                if( resource instanceof IFile ) {
                    IFile input = (IFile) resource;

                    IWorkbenchWindow workbenchWindow = ContextManager.getActiveWorkbenchWindow();
                    IWorkbenchPage workbenchPage = workbenchWindow.getActivePage();
                    // open the source file in an editor
                    IEditorPart editorPart = IDE.openEditor( workbenchPage, input );
                    if( editorPart instanceof ITextEditor ) {
                        ITextEditor textEditor = (ITextEditor) editorPart;
                        IDocument document = textEditor.getDocumentProvider().getDocument( textEditor.getEditorInput() );
                        // get IRegion instance of the line
                        IRegion region = document.getLineInformation( lineNumber - 1 );
                        // highlight line
                        textEditor.setHighlightRange( region.getOffset(), region.getLength(), true );
                    }
                }
            } catch( CoreException e ) {
                log.error( e.getMessage(), e );
            } catch( BadLocationException e ) {
                log.error( e.getMessage(), e );
            }
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.console.IHyperlink#linkEntered()
         */
        public void linkEntered() {
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.ui.console.IHyperlink#linkExited()
         */
        public void linkExited() {
        }

    }

    public static void updateProjectPattern() {
        String pattern = ""; //$NON-NLS-1$
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = workspaceRoot.getProjects();
        String addenum = null;
        IProject project = null;
        Pattern metaCharSearch = Pattern.compile( "[\\Q([{\\^-$|]})?*+.\\E]" ); //$NON-NLS-1$
        StringBuffer buf = new StringBuffer();
        for( int i = 0; i < projects.length; i++ ) {
            project = projects[ i ];
            if( metaCharSearch.matcher( project.getName() ).find() ) {
                // Project name contains Pattern meta characters
                addenum = Pattern.quote( project.getName() ) + "|"; //$NON-NLS-1$
            } else {
                addenum = project.getName() + "|"; //$NON-NLS-1$
            }
            buf.append( addenum );
        }
        pattern = buf.toString();
        if( pattern.length() == 0 ) {
            // Only the beginning of the group was captured
            projectPattern = null;
        } else {
            pattern = "\\b(" + pattern.substring( 0, pattern.length() - 1 ) + ")\\b"; //$NON-NLS-1$ //$NON-NLS-2$
            projectPattern = Pattern.compile( pattern );
        }
    }
}
