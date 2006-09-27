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
 * $Id: HttpClient.java,v 1.3 2006/09/27 05:26:47 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.transport.http;

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

    public static HttpClient New(URL url, String proxyHost, int proxyPort,
                                 boolean useCache, int timeout) 
            throws IOException {
        return New(url, newHttpProxy(proxyHost, proxyPort, "http"),
                -1, useCache);
    }

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

    protected HttpClient(URL url, Proxy proxy, int i) throws IOException {
        super(url, proxy, i);
    }

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
     */
    protected Socket doConnect (String server, int port)
            throws IOException, UnknownHostException {
        Socket s;
        if (proxy != null) {
            if (proxy.type() == Proxy.Type.SOCKS) {
                s = (Socket) AccessController.doPrivileged(
                        new PrivilegedAction() {
                            public Object run() {
                                return socketFactory.createSocket(proxy);
                            }});
            } else
                s = socketFactory.createSocket(Proxy.NO_PROXY);
        } else
            s = socketFactory.createSocket();
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
        if (readTimeout >= 0)
            s.setSoTimeout(readTimeout);
        else if (defaultSoTimeout > 0) {
            s.setSoTimeout(defaultSoTimeout);
        }
        return s;
    }

    protected static void setSocketFactory(SocketFactory sf) {
        socketFactory = sf;
    }
}
