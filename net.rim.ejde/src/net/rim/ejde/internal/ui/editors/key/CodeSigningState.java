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
package net.rim.ejde.internal.ui.editors.key;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import net.rim.ejde.internal.model.BlackBerryProperties;
import net.rim.ejde.internal.util.Messages;
import net.rim.ejde.internal.util.ProjectUtils;
import net.rim.ide.JavaParser;
import net.rim.ide.core.ObjectCounter;
import net.rim.ide.core.Util;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;

/**
 * a base class to provide support for creating the workspace/package protection dialog
 */
public class CodeSigningState {
    private static final Logger _logger = Logger.getLogger( CodeSigningState.class );
    private static final String NAME_PUBLIC = ".public";
    private static final String NAME_NON_PUBLIC = ".nonpublic";
    private static final String NAME_NO_KEY = "nokey";
    private String _textPublic;
    private String _textNonPublic;
    private List< ACheck > _allChecks;
    private String _textNoKey;
    // package name to key name mapping
    private Hashtable< String, String > _temporaryPackageProtection;
    // class name to key name mapping
    private Hashtable< String, String > _temporaryClassProtection;
    // package name to key name mapping
    private Hashtable< String, String > _allPackages;
    // class name to key name mapping
    private Hashtable< String, String > _allClasses;
    private String _currentKeyFile;
    private Hashtable< String, String > _keyTitles;
    private Vector< String > _keys;
    private boolean _change;
    private TreeNode _tree;
    private List< CheckBox > _packageChecks;
    private List< CheckBox > _classChecks;
    private Callback _cb;
    private IProject _project;
    private BlackBerryProperties _bbProperties;

    /**
     * an abstract check box implementation
     */
    public interface CheckBox {
        public boolean isEnabled();

        public void setEnabled( boolean on );

        public boolean isSelected();

        public void setSelected( boolean on );

        public String getText();

        public void setText( String text );
    }

    /**
     * an abstract tree node implementation
     */
    public interface TreeNode {
        public void setText( Object o );

        public void addChild( TreeNode child );
    }

    public class OneClass {
        private String _name;
        private CheckBox _check;
        private boolean _isPublic;

        OneClass( String name, boolean isPublic ) {
            _name = name;
            _isPublic = isPublic;
        }

        public String toString() {
            return ( _isPublic ? "public " : "" ) + _name;
        }

        String getName() {
            return _name;
        }

        boolean isPublic() {
            return _isPublic;
        }

        CheckBox getCheck() {
            return _check;
        }

        void setCheck( CheckBox check ) {
            _check = check;
        }
    }

    public class OnePackage {
        private String _name;
        private String _displayName;
        private Vector< OneClass > _classes;
        private CheckBox _check;

        OnePackage( String name, String displayName ) {
            _displayName = displayName;
            _name = name;
            _classes = new Vector< OneClass >();
        }

        OnePackage( String name ) {
            this( name, name );
        }

        String getName() {
            return _name;
        }

        void addClass( OneClass cls ) {
            _classes.addElement( cls );
        }

        OneClass getClass( int i ) {
            return _classes.elementAt( i );
        }

        int getNumClasses() {
            return _classes.size();
        }

        void sortClasses() {
            Util.stringSort( _classes, false );
        }

        public String toString() {
            return _name;
        }

        String getDisplayName() {
            return _displayName;
        }

        CheckBox getCheck() {
            return _check;
        }

        void setCheck( CheckBox check ) {
            _check = check;
        }
    }

    private class ACheck {
        private ACheck _parent;
        private CheckBox _check;
        // package/class name to key mapping
        private Hashtable< String, String > _table;
        // package/class name
        private String _tableKey;
        private String _displayName;
        private TreeNode _node;
        private boolean _isDefaultProtection;

        ACheck( CheckBox check, ACheck parent, TreeNode node, Hashtable< String, String > table, String tableKey,
                String displayName ) {
            _parent = parent;
            _check = check;
            _table = table;
            _tableKey = tableKey;
            _displayName = displayName;
            _node = node;
            String key = _table.get( _tableKey );
            _check.setSelected( key != null && key.equals( _currentKeyFile ) );
            _check.setText( _displayName );
            if( tableKey.equals( NAME_PUBLIC ) || tableKey.equals( NAME_NON_PUBLIC ) ) {
                _isDefaultProtection = true;
            }
        }

        void clear() {
            if( _table.remove( _tableKey ) != null ) {
                _change = true;
            }
            _check.setSelected( false );
            _check.setEnabled( true );
            _check.setText( _displayName );
            _cb.nodeChanged( _node );
        }

        void updateCheck( boolean force ) {
            String parentKey = null;
            if( _parent != null ) {
                _parent.updateCheck( force );
                parentKey = _parent._table.get( _parent._tableKey );
            }
            String oldText = _check.getText();
            boolean wasEnabled = _check.isEnabled();
            String key = _table.get( _tableKey );
            if( key != null ) {
                if( key.equals( _currentKeyFile ) ) {
                    _check.setText( _displayName );
                    _check.setEnabled( true );
                    _check.setSelected( true );
                } else {
                    _check.setText( NLS.bind( Messages.PrivateKeyEditor_ProtectOtherKey, _displayName, key ) );
                    _check.setEnabled( false );
                    _check.setSelected( false );
                }
            } else {
                if( parentKey != null ) {
                    _check.setText( NLS.bind( Messages.PrivateKeyEditor_ProtectOtherKey, _displayName, parentKey ) );
                } else {
                    _check.setText( _displayName );
                }
                _check.setEnabled( true );
                _check.setSelected( false );
            }
            if( _isDefaultProtection && _currentKeyFile.equals( NAME_NO_KEY ) ) {
                _check.setEnabled( false );
            }
            if( force || !oldText.equals( _check.getText() ) || _check.isEnabled() != wasEnabled ) {
                _cb.nodeChanged( _node );
            }
        }

        void updateState() {
            String key = _table.get( _tableKey );
            if( _check.isSelected() ) {
                if( key == null || !key.equals( _currentKeyFile ) ) {
                    _table.put( _tableKey, _currentKeyFile );
                    _change = true;
                }
            } else {
                if( key != null && key.equals( _currentKeyFile ) ) {
                    _table.remove( _tableKey );
                    _change = true;
                }
            }
        }
    }

    /**
     */
    public void finalize() throws Throwable {
        ObjectCounter.remove( this );
        super.finalize();
    }

    public CodeSigningState() {
        ObjectCounter.add( this );
        _textNoKey = Messages.PrivateKeyEditor_ProtectNoKey;
        _textPublic = Messages.PrivateKeyEditor_ProtectPublic;
        _textNonPublic = Messages.PrivateKeyEditor_ProtectNonPublic;
    }

    interface PackageParserCallback {
        public void setCurrentlyParsing( String name );
    }

    public static class TreeNodeAndCheckBox {
        public TreeNodeAndCheckBox( TreeNode t, CheckBox c ) {
            tree = t;
            check = c;
        }

        TreeNode tree;
        CheckBox check;
    }

    /**
     * functions which are called back during dialog initialization
     */
    public interface Callback extends PackageParserCallback {
        /**
         * callback to inform the UI which java file is being parsed
         */
        public void setCurrentlyParsing( String name );

        /**
         * callback to ask the UI to create a new tree node and check box element
         */
        public TreeNodeAndCheckBox newTreeNodeAndCheckBox( TreeNode parent );

        /**
         * callback to inform the UI that a node has changed significantly
         */
        public void nodeChanged( TreeNode node );
    }

    private Vector< OnePackage > parse( PackageParserCallback cb, Hashtable< String, String > packageProtection,
            Hashtable< String, String > classProtection ) throws CoreException {
        Hashtable< String, OnePackage > packageHash = new Hashtable< String, OnePackage >();

        List< IFile > files;
        files = ProjectUtils.getProtectedFiles( _project );
        for( IFile ifile : files ) {
            try {
                String name = ifile.getLocation().toOSString();
                cb.setCurrentlyParsing( name );
                File f = new File( name );

                FileReader fr = new FileReader( f );
                BufferedReader br = new BufferedReader( fr );
                JavaParser parser = new JavaParser( f, br, false );
                fr.close();
                String packageName = parser.getPackage();
                if( packageName != null && packageName.length() != 0 ) {
                    OnePackage pack =  packageHash.get( packageName );
                    if( pack == null ) {
                        pack = new OnePackage( packageName );
                        packageHash.put( packageName, pack );
                    }
                    for( int k = 0; k < parser.getClassCount(); ++k ) {
                        pack.addClass( new OneClass( parser.getClassName( k ), parser.isClassPublic( k ) ) );
                    }
                    pack.sortClasses();
                }
            } catch( IOException ioe ) {
                _logger.error( ioe );
            }
        }
        Vector< OnePackage > pakkages = Util.hashtableToVector( packageHash );
        Util.stringSort( pakkages, true );
        pakkages.insertElementAt( new OnePackage( NAME_NON_PUBLIC, _textNonPublic ), 0 );
        pakkages.insertElementAt( new OnePackage( NAME_PUBLIC, _textPublic ), 0 );
        return pakkages;
    }

    /**
     * Initialize the Package and class protector dialog data structures
     *
     * @param cb
     *            An interface that is called back while parsing and building datastructures
     * @param node
     *            The WorkspaceFile for the .key file
     * @throws CoreException
     */
    public List< OnePackage > initialize( IProject project, BlackBerryProperties properties, IFile node, Callback cb )
            throws CoreException {
        _project = project;
        _bbProperties = properties;
        _cb = cb;
        final Hashtable< String, String > packageProtection = _bbProperties._hiddenProperties.getPackageProtection();
        final Hashtable< String, String > classProtection = _bbProperties._hiddenProperties.getClassProtection();
        _change = false;

        List< OnePackage > packages = parse( cb, packageProtection, classProtection );
        _allPackages = new Hashtable< String, String >();
        _allClasses = new Hashtable< String, String >();
        _tree = cb.newTreeNodeAndCheckBox( null ).tree;
        _temporaryPackageProtection = (Hashtable< String, String >) ( packageProtection.clone() );
        _temporaryClassProtection = (Hashtable< String, String >) ( classProtection.clone() );
        _allChecks = new Vector< ACheck >();

        for( OnePackage pack : packages ) {
            String pakkage = pack.getName();
            String displayName = pack.getDisplayName();
            _allPackages.put( displayName, displayName );
            TreeNodeAndCheckBox treeAndCheck = cb.newTreeNodeAndCheckBox( _tree );
            pack.setCheck( treeAndCheck.check );
            TreeNode packageNode = treeAndCheck.tree;
            _tree.addChild( packageNode );
            ACheck parent = new ACheck( treeAndCheck.check, null, packageNode, _temporaryPackageProtection, pakkage, displayName );
            _allChecks.add( parent );
            for( int j = 0; j < pack.getNumClasses(); ++j ) {
                OneClass cls = pack.getClass( j );
                String className = cls.getName();
                String fullName = pakkage + "." + className;
                _allClasses.put( fullName, fullName );
                treeAndCheck = cb.newTreeNodeAndCheckBox( packageNode );
                CheckBox classCheck = treeAndCheck.check;
                cls.setCheck( classCheck );
                TreeNode classNode = treeAndCheck.tree;
                packageNode.addChild( classNode );
                _allChecks.add( new ACheck( classCheck, parent, classNode, _temporaryClassProtection, fullName, cls.toString() ) );
            }
        }
        _allPackages.put( NAME_PUBLIC, NAME_PUBLIC );
        _allPackages.put( NAME_NON_PUBLIC, NAME_NON_PUBLIC );

        _keys = new Vector< String >();
        _keyTitles = new Hashtable< String, String >();
        _keys.add( _textNoKey );
        _keyTitles.put( NAME_NO_KEY, Messages.PrivateKeyEditor_ProtectExplictlyUnsigned );
        List< IFile > keyFiles = ProjectUtils.getKeyFiles( _project );
        for( IFile f : keyFiles ) {
            String keyFile = f.getProjectRelativePath().toOSString();
            if( f.getLocation().equals( node.getLocation() ) ) {
                _currentKeyFile = keyFile;
            }
            _keys.add( keyFile );

            Properties keyContents = new Properties();
            String keyName = "";
            String keyId = "";
            InputStream is = null;
            try {
                is = node.getContents();
                keyContents.load( is );
                keyName = keyContents.getProperty( "Name" );
                keyId = keyContents.getProperty( "ID" );
            } catch( IOException ioe ) {
                _logger.error( ioe );
            } finally {
                if( is != null ) {
                    try {
                        is.close();
                    } catch( IOException e ) {
                    }
                }
            }
            _keyTitles.put( keyFile,
                    NLS.bind( Messages.PrivateKeyEditor_ProtectKeyTitle, new Object[] { keyFile, keyName, keyId } ) );
        }
        Util.stringSort( _keys );
        if( removeStaleEntries( _temporaryPackageProtection, _allPackages ) ) {
            _change = true;
        }
        if( removeStaleEntries( _temporaryClassProtection, _allClasses ) ) {
            _change = true;
        }
        updateChecksFromState( true );

        _packageChecks = new ArrayList< CheckBox >();
        _classChecks = new ArrayList< CheckBox >();
        for( OnePackage pack : packages ) {
            _packageChecks.add( pack.getCheck() );
            for( int j = 0; j < pack.getNumClasses(); ++j ) {
                OneClass cls = pack.getClass( j );
                _classChecks.add( cls.getCheck() );
            }
        }

        return packages;
    }

    private boolean removeStaleEntries( Hashtable< String, String > table, Hashtable< String, String > allValid ) {
        Enumeration< String > e = table.keys();
        Vector< Object > toRemove = new Vector< Object >();
        while( e.hasMoreElements() ) {
            Object key = e.nextElement();
            if( allValid.get( key ) != null )
                continue;
            toRemove.add( key );
        }
        for( int i = toRemove.size() - 1; i >= 0; --i ) {
            table.remove( toRemove.elementAt( i ) );
        }
        return toRemove.size() != 0;

    }

    /**
     * return the list of valid key file names ( for the dropdown combo box )
     */
    public Vector< String > getKeys() {
        Vector< String > v = new Vector< String >();
        v.addAll( _keys );
        return v;
    }

    private void updateChecksFromState( boolean force ) {
        for( int i = 0; i < _allChecks.size(); ++i ) {
            ( _allChecks.get( i ) ).updateCheck( force );
        }
    }

    private void updateStateFromChecks() {
        for( int i = 0; i < _allChecks.size(); ++i ) {
            ( _allChecks.get( i ) ).updateState();
        }
    }

    /**
     * used to inform this dialog when a checkbox has been updated
     */
    public void updateCheck( CheckBox check ) {
        updateAllChecks( false );
    }

    private void updateAllChecks( boolean force ) {
        updateStateFromChecks();
        updateChecksFromState( force );
    }

    /**
     * used to inform this dialog when the key file (dropdown) has been updated
     *
     * @param key
     *            the name of the key file
     */
    public void setKey( String key ) {
        _currentKeyFile = key;
        if( _currentKeyFile.equals( _textNoKey ) ) {
            _currentKeyFile = NAME_NO_KEY;
        }
        if( _currentKeyFile.length() != 0 ) {
            updateChecksFromState( false );
            _tree.setText( _keyTitles.get( _currentKeyFile ) );
        }
        updateAllChecks( true );
    }

    /**
     * return the current key file
     *
     * @return the current key file
     */
    public String getKey() {
        return _currentKeyFile;
    }

    /**
     * return the tree structure for the dialog
     */
    public TreeNode getTree() {
        return _tree;
    }

    /**
     * return a list of packages and classes that are signed with the key
     *
     * @param node
     *            WorkspaceFile .key file
     * @return list of packages and classes that are signed with the key
     * @throws CoreException
     */
    public Vector< String > keyUsedBy( IFile node ) throws CoreException {
        _project = node.getProject();
        Hashtable< String, String > packageProtection = _bbProperties._hiddenProperties.getPackageProtection();
        Hashtable< String, String > classProtection = _bbProperties._hiddenProperties.getClassProtection();
        PackageParserCallback cb = new PackageParserCallback() {
            public void setCurrentlyParsing( String name ) {
            }
        };
        Vector< OnePackage > pakkages = parse( cb, packageProtection, classProtection );
        if( pakkages == null ) {
            return new Vector< String >();
        }
        String name = node.getProjectRelativePath().toOSString();
        Vector< String > usingKey = new Vector< String >();
        for( int i = 0; i < pakkages.size(); ++i ) { // Inspect packages
            OnePackage pack = pakkages.elementAt( i );
            String pakkage = pack.getName();
            String key = packageProtection.get( pakkage );
            if( key != null && key.equals( name ) ) {
                usingKey.add( pakkage );
            }
            for( int j = 0; j < pack.getNumClasses(); ++j ) { // classes
                OneClass cls = pack.getClass( j );
                String className = cls.getName();
                String fullName = pakkage + "." + className;
                key = classProtection.get( fullName );
                if( key != null && key.equals( name ) ) {
                    usingKey.add( fullName );
                }
            }
        }
        return usingKey;
    }

    private void checkAll( List< CheckBox > v, boolean on ) {
        for( CheckBox check : v ) {
            if( check.isEnabled() ) {
                check.setEnabled( true );
                check.setSelected( on );
            }
        }
    }

    /**
     * check all of the enabled class check boxes
     */
    public void checkEnabledClasses() {
        checkAll( _classChecks, true );
        updateAllChecks( true );
    }

    /**
     * check all of the enabled package check boxes
     */
    public void checkEnabledPackages() {
        checkAll( _packageChecks, true );
        updateAllChecks( true );
    }

    /**
     * uncheck all of the enabled package and class check boxes
     */
    public void clearEnabledPackagesAndClasses() {
        checkAll( _classChecks, false );
        checkAll( _packageChecks, false );
        updateAllChecks( true );
    }

    /**
     * clear all checkboxes in the dialog, even the disabled ones
     */
    public void clearAllPackagesAndClasses() {
        for( int i = 0; i < _allChecks.size(); ++i ) {
            ( _allChecks.get( i ) ).clear();
        }
        updateAllChecks( true );
    }

    /**
     * OK button clicked. Transfer the state from here to the underlying project
     */
    public void ok() {
        if( _change ) {
            _bbProperties._hiddenProperties.setPackageProtection( _temporaryPackageProtection );
            _bbProperties._hiddenProperties.setClassProtection( _temporaryClassProtection );
        }
    }
}
