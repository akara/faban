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
package com.sun.faban.driver.transport.hc3;

import org.apache.commons.httpclient.cookie.CookieSpecBase;
import org.apache.commons.httpclient.cookie.MalformedCookieException;
import org.apache.commons.httpclient.Cookie;

import java.util.logging.Logger;

/**
 * The Faban cookie spec is even more liberal than Apache's CookieSpecBase.
 * It allows cookies with invalid paths to pass.
 */
public class FabanCookieSpec extends CookieSpecBase {

    static Logger logger = Logger.getLogger(FabanCookieSpec.class.getName());

     /**
      * Performs most common {@link org.apache.commons.httpclient.Cookie} validation
      *
      * @param host the host from which the {@link org.apache.commons.httpclient.Cookie} was received
      * @param port the port from which the {@link org.apache.commons.httpclient.Cookie} was received
      * @param path the path from which the {@link org.apache.commons.httpclient.Cookie} was received
      * @param secure <tt>true</tt> when the {@link org.apache.commons.httpclient.Cookie} was received using a
      * secure connection
      * @param cookie The cookie to validate.
      * @throws org.apache.commons.httpclient.cookie.MalformedCookieException if an exception occurs during
      * validation
      */

    public void validate(String host, int port, String path,
        boolean secure, final Cookie cookie)
        throws MalformedCookieException {

        logger.finer("enter CookieSpecBase.validate("
            + "String, port, path, boolean, Cookie)");
        if (host == null) {
            throw new IllegalArgumentException(
                "Host of origin may not be null");
        }
        if (host.trim().equals("")) {
            throw new IllegalArgumentException(
                "Host of origin may not be blank");
        }
        if (port < 0) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
        if (path == null) {
            throw new IllegalArgumentException(
                "Path of origin may not be null.");
        }
        if (path.trim().equals("")) {
            path = PATH_DELIM;
        }
        host = host.toLowerCase();
        // check version
        if (cookie.getVersion() < 0) {
            throw new MalformedCookieException ("Illegal version number "
                + cookie.getValue());
        }

        // security check... we musn't allow the server to give us an
        // invalid domain scope

        // Validate the cookies domain attribute.  NOTE:  Domains without
        // any dots are allowed to support hosts on private LANs that don't
        // have DNS names.  Since they have no dots, to domain-match the
        // request-host and domain must be identical for the cookie to sent
        // back to the origin-server.
        if (host.indexOf(".") >= 0) {
            // Not required to have at least two dots.  RFC 2965.
            // A Set-Cookie2 with Domain=ajax.com will be accepted.

            // domain must match host
            if (!host.endsWith(cookie.getDomain())) {
                String s = cookie.getDomain();
                if (s.startsWith(".")) {
                    s = s.substring(1, s.length());
                }
                if (!host.equals(s)) {
                    throw new MalformedCookieException(
                        "Illegal domain attribute \"" + cookie.getDomain()
                        + "\". Domain of origin: \"" + host + "\"");
                }
            }
        } else {
            if (!host.equals(cookie.getDomain())) {
                throw new MalformedCookieException(
                    "Illegal domain attribute \"" + cookie.getDomain()
                    + "\". Domain of origin: \"" + host + "\"");
            }
        }
    }
}
