/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.sun.com/cddl/cddl.html or
 * install_dir/legal/LICENSE
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at install_dir/legal/LICENSE.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id$
 *
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.transport.sunhttp;

import java.net.URI;
import java.util.Map;

/**
 * Faban's implementation of the cookie handler.
 *
 * @author Akara Sucharitakul
 */
public class CookieHandler extends java.net.CookieHandler {

    /**
     * Gets all the applicable cookies from a cookie cache for the
     * specified uri in the request header.
     *
     * HTTP protocol implementers should make sure that this method is
     * called after all request headers related to choosing cookies
     * are added, and before the request is sent.
     *
     * @param uri a <code>URI</code> to send cookies to in a request
     * @param map - a Map from request header
     *            field names to lists of field values representing
     *            the current request headers
     * @return an immutable map from state management headers, with
     *            field names "Cookie" or "Cookie2" to a list of
     *            cookies containing state information
     *
     * @throws IllegalArgumentException if either argument is null
     * @see #put(URI, Map)
     * @see java.net.CookieHandler#get(java.net.URI, java.util.Map)
     */
    @Override
	public Map<String, java.util.List<String>> get(
            URI uri, Map<String, java.util.List<String>> map) {
        return ThreadCookieHandler.getInstance().get(uri, map);
    }

    /**
     * Sets all the applicable cookies, examples are response header
     * fields that are named Set-Cookie2, present in the response
     * headers into a cookie cache.
     *
     * @param uri a <code>URI</code> where the cookies come from
     * @param responseHeaders an immutable responseHeaders from field names to
     *            lists of field values representing the response
     *            header fields returned
     * @throws  IllegalArgumentException if either argument is null
     * @see #get(URI, Map)
     */
    @Override
	public void put(URI uri,
                    Map<String, java.util.List<String>> responseHeaders) {
        ThreadCookieHandler.getInstance().put(uri, responseHeaders);
    }
}
