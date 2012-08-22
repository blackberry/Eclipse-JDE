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
package net.rim.ejde.internal.packaging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class present the data model of a jad file.
 * <p>
 * Currently we are only interested in the cod entries later on we can extend this class to have more jad properties
 *
 */
public class JadFile {
    private final static Pattern codEntryPattern = Pattern
            .compile( "(RIM-COD-URL|RIM-COD-Size|RIM-COD-SHA1|RIM-COD-Creation-Time)-?([0-9]*):(.*)" );
    private File _jadFile;
    private List< CodEntry > _codEntries;
    private List< String > _otherProperties;

    /**
     * Constructs a JadFile instance.
     *
     * @param jadFile
     */
    public JadFile( File jadFile ) {
        _jadFile = jadFile;
        _codEntries = new ArrayList< CodEntry >();
        _otherProperties = new ArrayList< String >();
    }

    /**
     * Parses the jad file and initialize the JadFile instance.
     * <p>
     * <b>This must be called before accessing any properties of a JadFile
     * <p>
     *
     * @throws IOException
     */
    public void parseJadFile() throws IOException {
        // check if the jad file exists
        if( _jadFile == null || !_jadFile.exists() ) {
            throw new FileNotFoundException( _jadFile.getPath() );
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader( new FileReader( _jadFile ) );
            String line = "";
            while( ( line = br.readLine() ) != null ) {
                Matcher m = codEntryPattern.matcher( line );
                if( m.matches() ) {
                    setCodEntry( m );
                } else {
                    _otherProperties.add( line );
                }
            }
        } finally {
            if( br != null ) {
                br.close();
            }
        }
    }

    private void setCodEntry( Matcher m ) {
        String codPropertyName = m.group( 1 ).trim();
        int index;
        if( m.group( 2 ) != null && !m.group( 2 ).isEmpty() ) {
            index = Integer.valueOf( m.group( 2 ) );
        } else {
            index = 0;
        }
        List< CodEntry > codEntries = getCodeEntries();
        CodEntry codEntry = null;
        for( int i = 0; i < codEntries.size(); i++ ) {
            if( codEntries.get( i ).index == index ) {
                codEntry = codEntries.get( i );
            }
        }
        if( codEntry == null ) {
            codEntry = new CodEntry();
            codEntry.setIndex( index );
            codEntries.add( codEntry );
        }
        if( "RIM-COD-URL".equals( codPropertyName ) ) {
            codEntry.setUrl( m.group( 3 ).trim() );
        } else if( "RIM-COD-Size".equals( codPropertyName ) ) {
            codEntry.setSize( Long.parseLong( m.group( 3 ).trim() ) );
        } else if( "RIM-COD-SHA1".equals( codPropertyName ) ) {
            codEntry.setShaCode( m.group( 3 ).trim() );
        } else {
            codEntry.setCreationTime( Long.parseLong( m.group( 3 ).trim() ) );
        }
    }

    /**
     * Gets CodEntry list.
     * @return
     */
    public List< CodEntry > getCodeEntries() {
        return _codEntries;
    }

    /**
     * Gets other properties of the jad file.
     * @return
     */
    public List< String > getOtherProperties() {
        return _otherProperties;
    }

    /**
     * Gets the jad file.
     * @return
     */
    public File getFile(){
        return _jadFile;
    }
    /**
     * Adds the given <code>codeEntries</code> to this JadFile.
     *
     * @param codeEntries
     */
    public void addCodEntries( List< CodEntry > codeEntries ) {
        int index = _codEntries.size();
        for( CodEntry entry : codeEntries ) {
            entry.setIndex( index++ );
            _codEntries.add( entry );
        }
    }

    /**
     * This class represents a cod entry in the jad file.
     * <p>
     * For example:
     * <p>
     * RIM-COD-URL: BB1.cod
     * <p>
     * RIM-COD-Size: 2752
     * <p>
     * RIM-COD-Creation-Time: 1317333997
     * <p>
     * RIM-COD-SHA1: 27 70 ee 0f 85 0b 98 30 cb fc 5b cb 77 27 cf 63 4c 34 bd 2f
     * <p>
     *
     */
    static public class CodEntry {
        private int index;
        private String url;
        private long size;
        private long creationTime;
        private String shaCode;

        public CodEntry() {

        }

        public CodEntry( int index, String url, long size, long creationgSize, String shaCode ) {
            this.index = index;
            this.url = url;
            this.size = size;
            this.creationTime = creationgSize;
            this.shaCode = shaCode;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex( int index ) {
            this.index = index;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl( String url ) {
            this.url = url;
        }

        public long getSize() {
            return size;
        }

        public void setSize( long size ) {
            this.size = size;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public void setCreationTime( long creationTime ) {
            this.creationTime = creationTime;
        }

        public String getShaCode() {
            return shaCode;
        }

        public void setShaCode( String shaCode ) {
            this.shaCode = shaCode;
        }

        private String getIndexString() {
            if( getIndex() == 0 ) {
                return "";
            }
            return "-" + getIndex();
        }

        public String getCodURLPropertyLine() {
            return "RIM-COD-URL" + getIndexString() + ":" + getUrl();
        }

        public String getCodSizePropertyLine(){
            return "RIM-COD-Size" + getIndexString() + ":" + getSize();
        }

        public String getCodCreationgTimePropertyLine(){
            return "RIM-COD-Creation-Time" + getIndexString() + ":" + getCreationTime();
        }

        public String getCodShaPropertyLine(){
            return "RIM-COD-SHA1" + getIndexString() + ":" + getShaCode();
        }
    }
}
