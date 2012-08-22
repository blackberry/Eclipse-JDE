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
package net.rim.ejde.internal.util;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.rim.ejde.internal.core.ContextManager;
import net.rim.ejde.internal.ui.dialogs.NewVersionDetectionDialog;
import net.rim.ejde.internal.ui.preferences.PreferenceConstants;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.about.AboutBundleGroupData;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/*
 *  This class is used to reminder user to obtain latest version
 *  of BlackBerry Web Plug-in when latest version is available on
 *  BlackBerry WebSite.
 *
 *  @author qxu
 */
public class UpgradingNotification extends WorkspaceJob {

    private static final String JOB_NAME = "Upgrading Notification";
    private static SimpleDateFormat df = new SimpleDateFormat( "MM/dd/yyyy" );
    private static final String EJDE_CURRENTINFO_XML = "currentInfo.xml";
    private static final String SAMPLE_CURRENTINFO_XML = "/templates/sample-currentInfo.xml";

    private int _snoozeDays;
    private String _toolUpgradeUrl;
    private String _toolMessage;
    private boolean _snoozeBoolean;

    private boolean _initializedBoolean;

    private String _snoozeDate;

    private Version _currentToolVersion;
    private Version _latestToolVersion;

    private static final Logger _log = Logger.getLogger( UpgradingNotification.class );

    public UpgradingNotification() {
        super( JOB_NAME );
    }

    @Override
    public IStatus runInWorkspace( IProgressMonitor monitor ) {
        init();

        if( _snoozeBoolean == true ) {
            if( checkSnoozeDate() ) {
                checkToolVersion();
            }
        } else {
            checkToolVersion();
        }

        if ( !_initializedBoolean ) {
            final Display display = Display.getDefault();
            if( display != null && !display.isDisposed() ) {
                display.syncExec( new Runnable() {

                    public void run() {
		            	if (!MessageDialog.openQuestion(display.getActiveShell(), Messages.UPGRADE_INITIALIZATION_TITLE, Messages.UPGRADE_INITIALIZATION_LABEL)) {
		            		_snoozeBoolean = true;
		            		_snoozeDays = 9999;
		            	} else {
		            		_snoozeBoolean = false;
		            	}
		            	_initializedBoolean = true;
		            	updateInfoFile();
                    }
                });
            }
        }

        IStatus result = Status.OK_STATUS;
        return result;
    }

    /*
     * Initiating information of upgrade notification to create a XML file.
     */
    private void init() {
        try {
            File currentInfoFile = getFile();
            if( !currentInfoFile.exists() ) {
                Bundle bundle = ContextManager.PLUGIN.getBundle();
                IPath path = new Path( SAMPLE_CURRENTINFO_XML );
                InputStream is = FileLocator.openStream( bundle, path, false );
                DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = docBuilder.parse( is );
                XMLUtil.writeXmlFile( doc, currentInfoFile.getCanonicalPath(), "UTF-8", "no" );
                getVersionNumber();
                updateInfoFile();
            } else {
                getInfo();
            }

        } catch( Exception ex ) {
            _log.error( "Error generating XML file", ex );
        }
    }

    /*
     * Getting current the version of tool and SDK
     */
    private void getVersionNumber() {

        boolean internal = false;
        if( _currentToolVersion == null ) {

            _currentToolVersion = getFeatureVersion();

        }
        if( internal ) {
            _currentToolVersion = handleVersion( _currentToolVersion.toString(), internal );
        }
    }

    @SuppressWarnings("restriction")
    private Version getFeatureVersion() {
        // create a descriptive object for each BundleGroup
        IBundleGroupProvider[] providers = Platform.getBundleGroupProviders();
        LinkedList< AboutBundleGroupData > groups = new LinkedList< AboutBundleGroupData >();
        if( providers != null ) {
            for( int i = 0; i < providers.length; ++i ) {
                IBundleGroup[] bundleGroups = providers[ i ].getBundleGroups();
                _log.debug( "PROVIDER NAME: " + providers[ i ].getName() );
                for( int j = 0; j < bundleGroups.length; ++j ) {
                    AboutBundleGroupData data = new AboutBundleGroupData( bundleGroups[ j ] );
                    groups.add( data );
                    if( data.getId().matches( Messages.EJDE_FEATURE_ID ) ) {
                        _log.debug( "Found ejde feature: " + data.getId() + " version " + data.getVersion() );
                        return handleVersion( data.getVersion(), true );
                    }
                }
            }
        }
        return ContextManager.PLUGIN.getBundle().getVersion();
    }

    private Version handleVersion( String bundleVersion, boolean internal ) {

        String[] bundleNumber = bundleVersion.split( "\\-" );
        String[] bundleSeries = bundleVersion.split( "\\." );
        String qualifier;
        int[] temp = new int[ 4 ];
        for( int i = 0; i < bundleSeries.length - 1; i++ ) {
            temp[ i ] = Integer.parseInt( bundleSeries[ i ] );
        }

        if( internal ) {
            qualifier = bundleNumber.length > 1 ? bundleNumber[ 1 ] : "0"; //current dev build do not have qualifier
        } else {
            qualifier = bundleSeries[ 3 ];
        }

        _log.debug( "qualifier: " + qualifier );

        return new Version( temp[ 0 ], temp[ 1 ], temp[ 2 ], qualifier );

    }

    /*
     * Getting all version information from internal XML file about current plug-in.
     */
    private void getInfo() {
        try {
            File currentInfo = getFile();
            if( currentInfo.exists() && currentInfo.isFile() ) {
                Document xmlDoc = XMLUtil.openXmlFile( currentInfo, false );
                if( xmlDoc != null ) {
                    XPath xPath = XPathFactory.newInstance().newXPath();
                    _currentToolVersion = handleVersion( xPath.evaluate("/version/tool[1]/text()", xmlDoc), false );
                    _initializedBoolean = Boolean.parseBoolean( xPath.evaluate("/version/initialized[1]/text()", xmlDoc) );
                    _snoozeBoolean = Boolean.parseBoolean( xPath.evaluate("/version/snooze/boolean[1]/text()", xmlDoc) );
                    if( _snoozeBoolean ) {
                    	_snoozeDays = Integer.parseInt( xPath.evaluate("/version/snooze/days[1]/text()", xmlDoc) );
                    	_snoozeDate = xPath.evaluate( "/version/snooze/date[1]/text()", xmlDoc);
                    }
                }
            }
        } catch( Exception ex ) {
            _log.error( "Error loading file information", ex );
        }
    }

    /*
     * Checking whether the snooze days has been coming.
     *
     * @return
     */
    private Boolean checkSnoozeDate() {

        try {
            Date currentDate = new Date();
            Date pastDate = df.parse( _snoozeDate );
            int distance = Math.abs( (int) ( ( currentDate.getTime() - pastDate.getTime() ) / 24 / 60 / 60 / 1000 ) );
            if( distance >= _snoozeDays ) {
                return true;
            }
        } catch( ParseException e ) {
            e.printStackTrace();
        }

        return false;
    }

    /*
     * Checking whether current plug-in version is lower than latest version on BlackBerry WebSite or equal to latest version
     *
     * @return
     */
    private Boolean checkToolVersion() {

    	retrieveXMLInfo();

        getVersionNumber();

        if( _latestToolVersion != null ) {
            int result = resultFromDialog( _currentToolVersion, _latestToolVersion, _toolMessage, _toolUpgradeUrl,
                    Messages.UPGRADE_NOTIFICATION_OF_EJDE_PLUGIN_TITLE );
            switch( result ) {
                case 0:
                	_snoozeBoolean = true;
                    updateInfoFile();
                    break;
                case 1:
                	_snoozeBoolean = false;
                	_currentToolVersion = _latestToolVersion;
                    updateInfoFile();
                    break;
                case 2:
                	_snoozeBoolean = true;
                	_snoozeDays = 9999;
                    updateInfoFile();
                    break;
                default:
                    return false;
            }
            return true;
        } else {
            _log.error( "Upgrade ejde Plug-In version can't be found on BlackBerry Website for some reasons." );
        }
        return false;
    }

    /*
     * Retrieving all information of latest plug-in version from external XML file on BlackBerry WebSite.
     *
     * @param projectType
     */
    private void retrieveXMLInfo() {
        try {
            // Notice: External url has to replace internal url when releasing.
            // this url only is for internal testing in tester team
            URL url;
            Document xmlDoc = null;

            String internalURL = ContextManager.PLUGIN.getPreferenceStore().getString( PreferenceConstants.UPDATE_NOTIFY_URL );
            if( internalURL.isEmpty() ) {
                // check internal
                url = new URL( Messages.INTERNAL_TESTING_URL );
                xmlDoc = XMLUtil.openXMLStream( url );

                // if does not find in internal, try the release version
                if( xmlDoc == null ) {
                    url = new URL( Messages.EXTERNAL_DEVZONE_URL );
                    xmlDoc = XMLUtil.openXMLStream( url );
                }
            } else { // use for internal testing
                url = new URL( internalURL );
                xmlDoc = XMLUtil.openXMLStream( url );
            }

            if( xmlDoc != null ) {
                XPathFactory factory = XPathFactory.newInstance();
                XPath xPath = factory.newXPath();

                NodeList nodeToolList = (NodeList) xPath.evaluate( "/upgrade/software", xmlDoc, XPathConstants.NODESET );

                for( int i = 0; i < nodeToolList.getLength(); i++ ) {
                    Node node = nodeToolList.item( i );
                    if( node instanceof Element ) {
                        String toolID = ( (Element) node ).getAttribute( Messages.TOOL_ID );
                        if( toolID.equals( "eJDE" ) ) {
                            String toolVersionStr = ( (Element) node ).getAttribute( Messages.LATEST_VERSION );
                            _latestToolVersion = handleVersion( toolVersionStr, false );
                            _toolUpgradeUrl = ( (Element) node ).getAttribute( Messages.UPGRADE_URL );

                            Element messageElmntLst = (Element) node;
                            NodeList messageElmnt = messageElmntLst.getElementsByTagName( "message" );
                            NodeList messageNm = messageElmnt.item( 0 ).getChildNodes();

                            _toolMessage = messageNm.item( 0 ).getNodeValue();
                        }
                    }
                }

            } else {
                // give up: cannot find the version file from: test, internal and external
                _log.debug( "Cannot find the version xml file from host : " + url.getHost() );
            }
        } catch( Exception ex ) {
            _log.error( "Error retrieving upgrade version info.", ex );
        }
    }

    private Element constructPath(Document xmlDoc, XPath xPath, String path) throws XPathExpressionException {
    	Element e = (Element)xPath.evaluate("/" + path + "[1]", xmlDoc, XPathConstants.NODE);
    	if (e == null) {
			int i = path.lastIndexOf('/');
			if (i<0) {
				e = xmlDoc.createElement(path);
				xmlDoc.getDocumentElement().appendChild(e);
			} else {
				e = xmlDoc.createElement(path.substring(i + 1));
				constructPath(xmlDoc, xPath, path.substring(0, i)).appendChild(e);
			}
    	}
		return e;
    }

    /*
     * Updating information in the internal XML file
     */
    private void updateInfoFile() {
        try {
            File currentInfo = getFile();
            if( currentInfo.exists() && currentInfo.isFile() ) {
                Document xmlDoc = XMLUtil.openXmlFile( currentInfo, false );
                if( xmlDoc != null ) {
                    XPath xPath = XPathFactory.newInstance().newXPath();
                    constructPath(xmlDoc, xPath, "version/tool").setTextContent( _currentToolVersion.toString() );
                    constructPath(xmlDoc, xPath, "version/initialized").setTextContent( Boolean.toString( _initializedBoolean ) );
                    constructPath(xmlDoc, xPath, "version/snooze/boolean").setTextContent( Boolean.toString( _snoozeBoolean ) );
                    constructPath(xmlDoc, xPath, "version/snooze/days").setTextContent( _snoozeDays + "" );
                    constructPath(xmlDoc, xPath, "version/snooze/date").setTextContent( df.format( new Date() ) );
                    XMLUtil.writeXmlFile( xmlDoc, currentInfo.getCanonicalPath(), "UTF-8", "no" );
                }
            }
        } catch( Exception ex ) {
            _log.error( "Error updating file information", ex );
        }
    }

    /*
     * Check whether the internal XML is available in plug-in
     *
     * @return
     */
    private File getFile() {
        try {
            IPath location = new Path( VMToolsUtils.getVMToolsFolderPath() + File.separator + EJDE_CURRENTINFO_XML );
            File currentInfoFile = location.toFile();

            return currentInfoFile;

        } catch( Exception ex ) {
            _log.error( "Error generating XML file", ex );
        }

        return null;
    }

    /*
     * Showing up a dialog whether users are interesting in latest version of web plug-in
     *
     * @param current version, latest version, dialog message
     *
     * @return
     */
    private int resultFromDialog( Version currentVersion, Version webVersion, String message, String upgradeUrl,
            String messageTitle ) {
        String[] dialogButtonLabels = { Messages.SNOOZE_DAYS_BUTTON, Messages.IGNORE_UPDATE_BUTTON, Messages.IGNORE_ALL_UPDATES_BUTTON };
        if( RIMVersionComparator.getInstance().compare( currentVersion, webVersion, RIMVersionComparator.VERSION_ALL ) < 0 ) {
            DialogOutput dialog = openMessageDialog( messageTitle, message, upgradeUrl, dialogButtonLabels );
            _initializedBoolean = true;
            return dialog.getStatus();
        }
        return -1;
    }

    /**
     * Help method for opening a dialog
     *
     * @param title
     * @param message
     * @param dialogButtonLabels
     * @return
     */
    private DialogOutput openMessageDialog( final String title, final String message, final String upgradeUrl,
            final String[] dialogButtonLabels ) {
        final DialogOutput result = new DialogOutput();
        final Display display = Display.getDefault();

        if( display != null && !display.isDisposed() ) {
            display.syncExec( new Runnable() {

                public void run() {

                    NewVersionDetectionDialog dialog = new NewVersionDetectionDialog( display.getActiveShell(), title, null,
                            message, upgradeUrl, MessageDialog.INFORMATION, dialogButtonLabels, 0 );

                    result.setStatus( dialog.open() );
                    _snoozeDays = dialog.getSnoozeDays();
                }

            } );
        }

        return result; // return status of the dialog
    }

    /**
     * Stores dialog output after it has been exited
     *
     * @author hrevinskaya
     */
    public static class DialogOutput {
        private int _status = Dialog.OK;
        private boolean _checkboxStatus = false;

        /**
         * Set return status
         *
         * @param status
         *            return status of a dialog
         */
        public void setStatus( int status ) {
            _status = status;
        }

        /**
         * Set status of the dialog's checkbox
         *
         * @param checkboxStatus
         *            true if checkbox was checked; false otherwise
         */
        public void setCheckBoxStatus( boolean checkboxStatus ) {
            _checkboxStatus = checkboxStatus;
        }

        /**
         * Get return status of the dialog
         *
         * @return status of the dialog
         */
        public int getStatus() {
            return _status;
        }

        /**
         * Get checkbox status of the dialog
         *
         * @return true if the checkbox was checked when dialog was exited; false otherwise
         */
        public boolean getCheckboxStatus() {
            return _checkboxStatus;
        }
    }
}
