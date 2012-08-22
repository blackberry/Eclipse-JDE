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
package net.rim.ejde.internal.codeassisstant;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.rim.ejde.internal.builders.PreprocessingBuilder;
import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.core.IConstants;
import net.rim.ejde.internal.model.BlackBerryProject;
import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.model.BlackBerrySDKInstall;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.VMUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * This class is used to computer the code completion proposals for preprocess defines. Basically, when a user type "//#ifdef" and
 * then press Ctrl + Space, all preprocess directives are popped up for the user to choose.
 *
 *
 *
 */
public class PreprocessDirectiveCompletionProposalComputer implements IJavaCompletionProposalComputer {
    static Logger _log = Logger.getLogger( PreprocessDirectiveCompletionProposalComputer.class );
    static private final String BASIC_PREPROCESS_DIRECTIVE_PROPOSAL_PATTERN = "\\s*?//#ifdef\\s+.*";
    Image _preprocessImage;

    public PreprocessDirectiveCompletionProposalComputer() {
        // TODO Auto-generated constructor stub
    }

    public List< ICompletionProposal > computeCompletionProposals( ContentAssistInvocationContext context,
            IProgressMonitor monitor ) {
        List< ICompletionProposal > proposals = new ArrayList< ICompletionProposal >();

        if( !( context instanceof JavaContentAssistInvocationContext ) ) {
            return proposals;
        }

        JavaContentAssistInvocationContext javaContext = (JavaContentAssistInvocationContext) context;

        IJavaProject javaProject = javaContext.getProject();
        int offset = javaContext.getCoreContext().getOffset();
        IDocument doc = javaContext.getDocument();
        String lineContent;
        IRegion line = null;

        Pattern p = Pattern.compile( BASIC_PREPROCESS_DIRECTIVE_PROPOSAL_PATTERN );
        try {
            line = doc.getLineInformationOfOffset( offset );
            int length = offset > line.getOffset() ? offset - line.getOffset() : 0;
            lineContent = doc.get( line.getOffset(), length );
            Matcher matcher = p.matcher( lineContent );
            // check if it matches the basic pattern
            if( matcher.matches() ) {
                proposals = getProposals( javaProject, offset, lineContent );
            }
        } catch( BadLocationException e ) {
            _log.error( e );
        }
        return proposals;
    }

    @Override
    public List< IContextInformation > computeContextInformation( ContentAssistInvocationContext context, IProgressMonitor monitor ) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getErrorMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void sessionEnded() {
        // nothing need to be done here

    }

    @Override
    public void sessionStarted() {
        // nothing need to be done here
    }

    private List< ICompletionProposal > getProposals( IJavaProject javaProject, int offset, String content ) {
        List< IVMInstall > vmList = VMUtils.getInstalledBBVMs();
        Collections.sort( vmList, new VMUtils.VMGeneralComparator() );
        List< ICompletionProposal > proposalList = new ArrayList< ICompletionProposal >( vmList.size() );
        PreprocessDirectiveCompletionProposal proposal = null;
        BlackBerrySDKInstall bbVM;
        String directive;
        String directivePrefix;
        int indexOfLastSpace = content.lastIndexOf( IConstants.ONE_BLANK_STRING ); // indexOfLastSpace can never be negative
        // get proposals for CP level pre-defined preprocess directives
        for( int i = 0; i < vmList.size(); i++ ) {
            bbVM = (BlackBerrySDKInstall) vmList.get( i );
            directive = VMUtils.getJREDirective( bbVM );
            if( StringUtils.isBlank( directive ) ) {
                continue;
            }
            directivePrefix = content.substring( indexOfLastSpace + 1, content.length() );
            proposal = createProposal( directivePrefix, directive, offset - directivePrefix.length(), getPreprocessDefineImage(),
                    directive, NLS.bind( Messages.PREDEFINED_PREPROCESS_DEFINE_INFO, directive ) );
            if( proposal != null ) {
                proposalList.add( proposal );
            }
        }
        // get proposals for workspace level and project level preprocess directives
        BlackBerryProperties properties = ContextManager.PLUGIN.getBBProperties( javaProject.getProject().getName(), false );
        List< String > directives = PreprocessingBuilder.getDefines( new BlackBerryProject( javaProject, properties ), false );
        for( int i = 0; i < directives.size(); i++ ) {
            directive = directives.get( i );
            directivePrefix = content.substring( indexOfLastSpace + 1, content.length() );
            proposal = createProposal( directivePrefix, directive, offset - directivePrefix.length(), getPreprocessDefineImage(),
                    directive, Messages.CUSTOMIZED_PREPROCESS_DEFINE_INFO );
            if( proposal != null && !proposalList.contains(proposal)) {
                proposalList.add( proposal );
            }
        }
        return proposalList;
    }

    private PreprocessDirectiveCompletionProposal createProposal( String prefix, String define, int offset, Image image,
            String displayString, String otherInfo ) {
        PreprocessDirectiveCompletionProposal proposal = null;
        if( define.toLowerCase().startsWith( prefix.toLowerCase() ) ) {
            proposal = new PreprocessDirectiveCompletionProposal( define, offset, 0, define.length(), image, define, null,
                    otherInfo );

        }
        return proposal;
    }

    public Image getPreprocessDefineImage() {
        if( _preprocessImage == null ) {
            ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin( ContextManager.PLUGIN_ID, "icons"
                    + File.separator + "bb_perspective.gif" );
            _preprocessImage = imageDescriptor.createImage();
        }
        return _preprocessImage;
    }

    public void dispose() {
        if( _preprocessImage != null ) {
            _preprocessImage.dispose();
        }
    }
}
