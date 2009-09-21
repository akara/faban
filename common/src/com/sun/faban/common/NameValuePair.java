/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://faban.dev.java.net/public/CDDLv1.0.html or
 * install_dir/license.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id$
 *
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

import java.io.Serializable;

/**
 * Generic object representing a String name and an Object<T> value.
 *
 * @author Akara Sucharitakul
 */
public class NameValuePair<V> implements Serializable {

    private static final long serialVersionUID = 20070523L;

    /** The name part. */
    public String name;

    /** The value part. */
    public V value;

    /**
     * Constructs a NameValuePair with a given name and value.
     * @param name The name
     * @param value The value
     */
    public NameValuePair(String name, V value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Constructs an empty NameValuePair.
     */
    public NameValuePair() {
    }

    /**
     * A NameValuePair equals another if and only if both the name and value
     * equal.
     * @param o The other NameValuePair
     * @return True if the o equals this object, false otherwise
     */
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof NameValuePair))
            return false;

        final NameValuePair nameValuePair = (NameValuePair) o;

        if (name != null ? !name.equals(nameValuePair.name) :
                nameValuePair.name != null)
            return false;
        if (value != null ? !value.equals(nameValuePair.value) :
                nameValuePair.value != null)
            return false;

        return true;
    }

    /**
     * Obtains the hash code of this NameValuePair.
     * @return The hash code
     */
    public int hashCode() {
        int result;
        result = (name != null ? name.hashCode() : 0);
        result = 29 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
