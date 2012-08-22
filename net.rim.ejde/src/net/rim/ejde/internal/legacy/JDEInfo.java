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
package net.rim.ejde.internal.legacy;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public final class JDEInfo {
    private final String _displayName, _path, _version;

    public JDEInfo( String name, String path, String version ) {
        _displayName = name;
        _path = path;
        _version = version;
    }

    public String getName() {
        return _displayName;
    }

    public String getPath() {
        return _path;
    }

    public String getVersion() {
        return _version;
    }

    /*
     * @Override {@link java.lang.Object#hashcode()}
     */
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode( this );
    }

    /*
     * @Override {@link java.lang.Object#toString()}
     */
    public String toString() {
        return ToStringBuilder.reflectionToString( this, ToStringStyle.DEFAULT_STYLE );
    }

    /*
     * @Override {@link java.lang.Object#equals()}
     */
    public boolean equals( Object other ) {
        return EqualsBuilder.reflectionEquals( this, other, new String[] { "_path" } );
    }
}
