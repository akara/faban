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
 * $Id: URLStreamHandlerFactory.java,v 1.2 2006/06/29 19:38:38 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.transport.http;

import sun.net.www.http.HttpClient;

import java.net.URL;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a ThreadLocal cache used by the HTTP client keepalive.
 * It stores a client vector per URL. Each client vector may
 * contain multiple client objects.
 * @author Scott Oaks
 */
class KeepAliveCache extends sun.net.www.http.KeepAliveCache {

    /** The thread local cache */
    static ThreadLocal localHash = new ThreadLocal() {
        protected Object initialValue() {
            return new HashMap();
        }
    };

    /**
     * Gets an object from the keepalive cache. It will get the most
     * recently used object from the client vector for this URL.
     * @param url The URL to match
     * @param obj An additional key, ignored
     * @return An object in the cache, or null if there is none.
     */
    public Object get(URL url, Object obj) {
        HashMap map = (HashMap) localHash.get();
        KeepAliveKey dkak = new KeepAliveKey(url);
        ArrayList clientVector = (ArrayList) map.get(dkak);
        int cvIdx = 0;
        if (clientVector == null || (cvIdx = clientVector.size()) == 0) {
            return null;
        }
        Object ret = clientVector.get(--cvIdx);
        clientVector.remove(cvIdx);
        return ret;
    }

    /**
     * Puts an object back in the cache based on the given URL.
     * @param url The URL to match
     * @param obj An additional key, ignored
     * @param http The http client to put into the cache
     */
    public void put(URL url, Object obj, HttpClient http) {
        if (http != null) {
            HashMap map = (HashMap) localHash.get();
            KeepAliveKey dkak = new KeepAliveKey(url);
            ArrayList clientVector = (ArrayList) map.get(dkak);
            if (clientVector == null) {
                clientVector = new ArrayList();
                clientVector.add(http);
                map.put(dkak, clientVector);
            }
            else
                clientVector.add(http);
        }
    }

    /**
     * Clears the connection cache for the given URL for this thread.
     * @param url The URL to be cleared.
     */
    public static void clear(URL url) {
        HashMap map = (HashMap) localHash.get();
        KeepAliveKey dkak = new KeepAliveKey(url);
        ArrayList clientVector = (ArrayList) map.get(dkak);
        if (clientVector != null)
            // Since we know that in a driver situation, the same URLs are
            // going to be used over and over. So we decide not to get rid
            // of the clientVector altogether just to create a new one next
            // time, but to clear all the entries.
            clientVector.clear();
    }


    /**
     * This is the URL key into the thread local KeepAlive cache.
     * We could use the URL iteself but that's going to be much
     * slower due to the URL delegation architecture.
     * @author Scott Oaks
     */
    static class KeepAliveKey {
        private String protocol = null;
        private String	host = null;
        private int		port = 0;

        public KeepAliveKey(URL url) {
            this.protocol = url.getProtocol();
            this.host = url.getHost();
            this.port = url.getPort();
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof KeepAliveKey))
                return false;
            KeepAliveKey kae = (KeepAliveKey)obj;
            return host.equals(kae.host)
                    && (port == kae.port) && protocol.equals(kae.protocol);
        }

        public int hashCode() {
            String str = protocol+host+port;
            return str.hashCode();
        }
    }
}

/**
 * The Faban URLStreamHandlerFactory creates Faban's HttpURLConnection.
 * @author Scott Oaks
 */
public class URLStreamHandlerFactory
        implements java.net.URLStreamHandlerFactory {

    public URLStreamHandlerFactory() {
        try {
            Class.forName(
                    "com.sun.faban.driver.transport.http.HttpClient");
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalArgumentException("Can't find httpclient class");
        }
    }

    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("http".equals(protocol))
            return new Handler();
        return null;
    }
}
