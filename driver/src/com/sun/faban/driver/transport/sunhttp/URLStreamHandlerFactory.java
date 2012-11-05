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

import java.net.URL;
import java.net.URLStreamHandler;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import sun.net.www.http.HttpClient;

/**
 * This is a ThreadLocal cache used by the HTTP client keepalive.
 * It stores a client vector per URL. Each client vector may
 * contain multiple client objects.
 * @author Scott Oaks
 */
class KeepAliveCache extends sun.net.www.http.KeepAliveCache {

	private static final long serialVersionUID = 1L;

    private static Logger logger =
            Logger.getLogger(KeepAliveCache.class.getName());
    
	/** The thread local cache. */
    static ThreadLocal<Map<KeepAliveKey, List<HttpClient>>> localHash =
            new ThreadLocal<Map<KeepAliveKey, List<HttpClient>>>() {
        @Override
		protected Map<KeepAliveKey, List<HttpClient>> initialValue() {
            return new HashMap<KeepAliveKey, List<HttpClient>>();
        }
    };

    /**
     * Gets an object from the keepalive cache. It will get the most
     * recently used object from the client vector for this URL.
     * @param url The URL to match
     * @param obj An additional key, ignored
     * @return An object in the cache, or null if there is none.
     */
    @Override
	public HttpClient get(URL url, Object obj) {
        Map<KeepAliveKey, List<HttpClient>> map = localHash.get();
        KeepAliveKey dkak = new KeepAliveKey(url);
        List<HttpClient> clientVector = map.get(dkak);
        int cvIdx = 0;
        if (clientVector == null || (cvIdx = clientVector.size()) == 0) {
            logger.fine("No connection in cache for " + url + ".");
            return null;
        }
        HttpClient ret = clientVector.get(--cvIdx);
        clientVector.remove(cvIdx);
        logger.finest("Obtained " + ret + " from cache.");
        return ret;
    }

    /**
     * Puts an object back in the cache based on the given URL.
     * @param url The URL to match
     * @param obj An additional key, ignored
     * @param http The http client to put into the cache
     */
    @Override
	public void put(URL url, Object obj, HttpClient http) {
        logger.finest("Putting " + http + "in cache for URL " + url);
        if (http != null) {
            Map<KeepAliveKey, List<HttpClient>> map = localHash.get();
            KeepAliveKey dkak = new KeepAliveKey(url);
            List<HttpClient> clientVector =  map.get(dkak);
            if (clientVector == null) {
                clientVector = new ArrayList<HttpClient>();
                clientVector.add(http);
                map.put(dkak, clientVector);
            } else {
				clientVector.add(http);
			}
        }
    }

    /**
     * Clears the connection cache for the given URL for this thread.
     * @param url The URL to be cleared.
     */
    public static void clear(URL url) {
    	Map<KeepAliveKey, List<HttpClient>> map = localHash.get();
        KeepAliveKey dkak = new KeepAliveKey(url);
        List<HttpClient> clientVector = map.get(dkak);
        if (clientVector != null) {
			// Since we know that in a driver situation, the same URLs are
            // going to be used over and over. So we decide not to get rid
            // of the clientVector altogether just to create a new one next
            // time, but to clear all the entries.
            clientVector.clear();
		}
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

        /**
         * Construct a {@link KeepAliveKey}.
         * 
         * @param url The url to keepalive
         */
        public KeepAliveKey(URL url) {
            protocol = url.getProtocol();
            host = url.getHost();
            port = url.getPort();
        }

        /**
         * Compare the keys.
         * @param obj The other key to compare
         * @return true if the keys are the same, false otherwise
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
		public boolean equals(Object obj) {
            if (!(obj instanceof KeepAliveKey)) {
				return false;
			}
            KeepAliveKey kae = (KeepAliveKey)obj;
            return host.equals(kae.host)
                    && (port == kae.port) && protocol.equals(kae.protocol);
        }

        /**
         * Obtains the hash code of this key.
         * @return The hash code
         * @see java.lang.Object#hashCode()
         */
        @Override
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

    /**
     * Construct a {@link URLStreamHandlerFactory}.
     */
    public URLStreamHandlerFactory() {
        try {
            Class.forName(
                    "com.sun.faban.driver.transport.sunhttp.HttpClient");
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalArgumentException("Can't find httpclient class");
        }
    }

    /**
     * Creates a URL stream handler for a given protocol.
     * @param protocol The protocol
     * @return The URL stream handler
     * @see java.net.URLStreamHandlerFactory#createURLStreamHandler(java.lang.String)
     */
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("http".equals(protocol)) {
			return new Handler();
		}
	if ("https".equals(protocol)) {
	    initSSL();
	    // We want the default handler; we just needed to make sure that
	    // the appropriate trust manager was installed
	    return null;
	}
        return null;
    }

    private static boolean doneInitSSL = false;
    private static synchronized void initSSL() {
	if (doneInitSSL) {
		return;
	}
	doneInitSSL = true;
	try {
	    // A trust manager that accepts all certificates
            TrustManager[] tm = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs,
                                                   String authType) {
                    	// do nothing
                    }
                    public void checkServerTrusted(X509Certificate[] certs,
                                                   String authType) {
                    	// do nothing
                    }
                }
	    };
	    SSLContext sc = SSLContext.getInstance("SSL");
	    sc.init(null, tm, new SecureRandom());
	    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

	    HostnameVerifier verifier = new HostnameVerifier() {
			@SuppressWarnings("unused")
			public boolean verify(String url, String server) {
			    return true;
			}
			public boolean verify(String url, SSLSession sess) {
			    return true;
			}
	    };
	    HttpsURLConnection.setDefaultHostnameVerifier(verifier);
	} catch (Exception e) {
	    throw new ExceptionInInitializerError(e);
	}
    }
}
