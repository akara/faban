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

import com.sun.faban.driver.transport.util.TimedSocketFactory;

import java.io.IOException;
import java.net.Proxy;
import java.net.URL;

/**
 * Faban's implementation of HttpURLConnection relies heavily on the sun.net
 * implementation. It is actually a subclass that creates instances of
 * faban's HttpClient instead of the sun.net version. It also opens up the
 * interface for clients to use a custom SocketFactory. This HttpURLConnection
 * serves both http and https connections.
 *
 * @author Akara Sucharitakul
 */
public class HttpURLConnection
        extends sun.net.www.protocol.http.HttpURLConnection {

    private static SocketFactory defaultSocketFactory;
    private SocketFactory socketFactory;

    /**
     * Creates a connection.
     * @param url The target URL
     * @param proxy Proxy, if any
     * @param handler The protocol handler
     */
    protected HttpURLConnection(URL url, Proxy proxy, Handler handler) {
        super(url, proxy, handler);
    }

    /**
     * Obtain a HttpClient object. Use the cached copy if specified.
     *
     * @param url       the URL being accessed
     * @param useCache  whether the cached connection should be used
     *        if present
     * @throws IOException Communication error
     */
    @Override
	protected void setNewClient (URL url, boolean useCache)
	        throws IOException {
        HttpClient.setSocketFactory(checkSocketFactory());
	    http = HttpClient.New(url, null, -1, useCache, getConnectTimeout());
	    http.setReadTimeout(getReadTimeout());
    }

    /**
     * Connects via proxy.
     * @param url The URL to connect
     * @param proxyHost The proxy host
     * @param proxyPort The proxy port
     * @param useCache Whether to use cached connections
     * @throws IOException Communication error
     */
    @Override
	protected void proxiedConnect(URL url,
					   String proxyHost, int proxyPort,
					   boolean useCache)
	throws IOException {
	SecurityManager security = System.getSecurityManager();
	if (security != null) {
	    security.checkConnect(proxyHost, proxyPort);
	}
    HttpClient.setSocketFactory(checkSocketFactory());
	http = HttpClient.New(url, proxyHost, proxyPort, useCache, getConnectTimeout());
	http.setReadTimeout(getReadTimeout());
    }

    @Override
	protected HttpClient getNewHttpClient(URL url, Proxy p, int connectTimeout)
            throws IOException {
        HttpClient.setSocketFactory(checkSocketFactory());
        return HttpClient.New(url, p, connectTimeout, true);
    }

    /**
     * Sets the default <code>SocketFactory</code> inherited by new
     * instances of this class.
     * <P>
     * The socket factories are used when creating sockets for secure
     * https URL connections.
     *
     * @param sf the default socket factory
     * @throws IllegalArgumentException if the SocketFactory
     *		parameter is null.
     * @see #getDefaultSocketFactory()
     */
    public static void setDefaultSocketFactory(SocketFactory sf) {
        if (sf == null) {
			throw new IllegalArgumentException(
                    "no default SocketFactory specified");
		}

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
			sm.checkSetFactory();
		}

        defaultSocketFactory = sf;
    }

    /**
     * Gets the default static <code>SocketFactory</code> that is
     * inherited by new instances of this class.
     * <P>
     * The socket factories are used when creating sockets for secure
     * https URL connections.
     *
     * @return the default <code>SocketFactory</code>
     * @see #setDefaultSocketFactory(SocketFactory)
     */
    public static SocketFactory getDefaultSocketFactory() {
        if (defaultSocketFactory == null) {
			defaultSocketFactory = TimedSocketFactory.getInstance();
		}
        return defaultSocketFactory;
    }

    /**
     * Sets the <code>SocketFactory</code> to be used when this instance
     * creates sockets for secure https URL connections.
     * <P>
     * New instances of this class inherit the default static
     * <code>SocketFactory</code> set by
     * {@link #setDefaultSocketFactory(SocketFactory)
     * setDefaultSocketFactory}.  Calls to this method replace
     * this object's <code>SocketFactory</code>.
     *
     * @param sf the socket factory
     * @throws IllegalArgumentException if the <code>SocketFactory</code>
     *		parameter is null.
     * @see #getSocketFactory()
     */
    public void setSocketFactory(SocketFactory sf) {
        if (sf == null) {
			throw new IllegalArgumentException("no SocketFactory specified");
		}

        socketFactory = sf;
    }

    /**
     * Gets the  socket factory to be used when creating sockets
     * for secure https URL connections.
     *
     * @return the <code>SocketFactory</code>
     * @see #setSocketFactory(SocketFactory)
     */
    public SocketFactory getSocketFactory() {
        return socketFactory;
    }

    private SocketFactory checkSocketFactory() {
        if (socketFactory != null) {
			return socketFactory;
		}
        return getDefaultSocketFactory();
    }
}
