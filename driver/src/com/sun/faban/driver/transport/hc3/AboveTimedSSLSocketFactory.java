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

import com.sun.faban.driver.transport.util.TimedSocketWrapper;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * The BelowTimedSSLSocketFactory is used for the Apache Commons
 * HttpClient 3.1 SSL connections to let the transport be timed.
 *
 * @author Akara Sucharitakul
 */
public class AboveTimedSSLSocketFactory implements SecureProtocolSocketFactory {

    private static SSLSocketFactory sslFactory =
            (SSLSocketFactory) SSLSocketFactory.getDefault();

    public Socket createSocket(String host, int port, InetAddress localAddress,
                               int localPort) throws IOException {

        Socket socket = new TimedSocketWrapper(sslFactory.createSocket());
        InetSocketAddress endpoint = new InetSocketAddress(host, port);
        socket.bind(new InetSocketAddress(localAddress, localPort));
        socket.connect(endpoint);
        return socket;
    }

    public Socket createSocket(String host, int port, InetAddress localAddress,
                               int localPort, HttpConnectionParams params)
            throws IOException {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        int timeout = params.getConnectionTimeout();
        if (timeout <= 0) {
            return createSocket(host, port, localAddress, localPort);
        } else {
            Socket socket = new TimedSocketWrapper(sslFactory.createSocket());
            InetSocketAddress endpoint = new InetSocketAddress(host, port);
            socket.bind(new InetSocketAddress(localAddress, localPort));
            socket.connect(endpoint, timeout);
            return socket;
        }
    }

    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = new TimedSocketWrapper(sslFactory.createSocket());
        InetSocketAddress endpoint = new InetSocketAddress(host, port);
        socket.connect(endpoint);
        return socket;
    }

    public Socket createSocket(Socket socket, String host, int port,
                               boolean close)
            throws IOException, UnknownHostException {

        // Starting the timer at this point is the most accurate we can do
        // with an existing socket - so the time includes createSocket.
        TimedSocketWrapper wrapper = new TimedSocketWrapper();
        wrapper.setSocket(sslFactory.createSocket(socket, host, port, close));
        return wrapper;
    }

    /**
     * All instances of ProtocolTimedSocketFactory are the same.
     * @param obj The other object to compare to
     * @return true if obj is a ProtocolTimedSocketFactory, false otherwise
     */
    public boolean equals(Object obj) {
        return ((obj != null) && obj.getClass().equals(getClass()));
    }

    /**
     * All instances of ProtocolTimedSocketFactory have the same hash code.
     * @return The hash code of the class, thus all instances are the same
     */
    public int hashCode() {
        return getClass().hashCode();
    }

}
