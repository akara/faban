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

import java.io.IOException;
import java.net.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Implementation of the HttpClient for the Faban driver framework.
 * It relies heavily on the sun.net implementation. The only
 * difference is actually the use of SocketFactory instead
 * of plainly creating new sockets. The HttpClient supports both
 * http and https protocols.<br>
 * Derived from code contributed by Scott Oaks.
 *
 * @author Akara Sucharitakul
 */
public class HttpClient extends sun.net.www.http.HttpClient {

    static {
        kac = new KeepAliveCache();
    }

    /** Superclass' inCache is private. Needed to define and check usage. */
    protected boolean inCache;

    private static SocketFactory socketFactory;

    /**
     * Instantiates a HttpClient.
     * @param url The URL to connect
     * @param proxyHost The proxy server, null if no proxy
     * @param proxyPort The proxy server port
     * @param useCache Whether to use a client from cache or not
     * @param timeout The connect timeout, -1 if no timeout
     * @return An instance of HttpClient
     * @throws IOException An I/O error occurred
     */
    public static HttpClient New(URL url, String proxyHost, int proxyPort,
                                 boolean useCache, int timeout) 
            throws IOException {
        return New(url, proxyHost == null ? null :
                newHttpProxy(proxyHost, proxyPort, "http"), -1, useCache);
    }

    /**
     * Instantiates a HttpClient.
     * @param url The URL to connect
     * @param p The proxy server, null if no proxy
     * @param to The connect timeout, -1 if no timeout
     * @param useCache Whether to use a client from cache or not
     * @return An instance of HttpClient
     * @throws IOException An I/O error occurred
     */
    public static HttpClient New(URL url, Proxy p, int to,
                                                  boolean useCache)
            throws IOException {
        if (p == null) {
            p = Proxy.NO_PROXY;
        }
        HttpClient ret = null;
        /* see if one's already around */
        if (useCache) {
            ret = (HttpClient) kac.get(url, null);
            if (ret != null) {
                if ((ret.proxy != null && ret.proxy.equals(p)) ||
                        (ret.proxy == null && p == null)) {
                    synchronized (ret) {
                        ret.cachedHttpClient = true;
                        assert ret.inCache;
                        ret.inCache = false;
                    }
                }
            }
        }
        if (ret == null) {
            ret = new HttpClient(url, p, to);
        } else {
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkConnect(url.getHost(), url.getPort());
            }
            ret.url = url;
        }
        return ret;
    }

    /**
     * Subclass constructor for the client.
     * @param url The URL
     * @param proxy Proxies, if any
     * @param to The connect timeout
     * @throws IOException If an error occurs
     */
    protected HttpClient(URL url, Proxy proxy, int to) throws IOException {
        super(url, proxy, to);
    }

    @Override
	protected synchronized void putInKeepAliveCache() {
	if (inCache) {
	    assert false : "Duplicate put to keep alive cache";
	    return;
	}
	inCache = true;
	kac.put(url, null, this);
    }

    /**
     * Return a socket connected to the server, with any
     * appropriate options pre-established. This method
     * overrides NetworClient.doConnect() to use the provided
     * SocketFactory for socket creation.
     * @param server The server to connect to
     * @param port The port to connect to
     * @return The socket connecting the the server
     * @throws IOException Communication error
     * @throws UnknownHostException The host cannot be found
     */
    @Override
	protected Socket doConnect (String server, int port)
            throws IOException, UnknownHostException {
        Socket s;
        if (proxy != null) {
            if (proxy.type() == Proxy.Type.SOCKS) {
                s = AccessController.doPrivileged(
                        new PrivilegedAction<Socket>() {
                            public Socket run() {
                                return socketFactory.createSocket(proxy);
                            }});
            } else {
				s = socketFactory.createSocket(Proxy.NO_PROXY);
			}
        } else {
			s = socketFactory.createSocket();
		}
        // Instance specific timeouts do have priority, that means
        // connectTimeout & readTimeout (-1 means not set)
        // Then global default timeouts
        // Then no timeout.
        if (connectTimeout >= 0) {
            s.connect(new InetSocketAddress(server, port), connectTimeout);
        } else {
            if (defaultConnectTimeout > 0) {
                s.connect(new InetSocketAddress(server, port),
                        defaultConnectTimeout);
            } else {
                s.connect(new InetSocketAddress(server, port));
            }
        }
        if (readTimeout >= 0) {
			s.setSoTimeout(readTimeout);
		} else if (defaultSoTimeout > 0) {
            s.setSoTimeout(defaultSoTimeout);
        }
        return s;
    }

    /**
     * Sets the socket factory for creating sockets used by this client.
     * @param sf The socket factory
     */
    protected static void setSocketFactory(SocketFactory sf) {
        socketFactory = sf;
    }
}
