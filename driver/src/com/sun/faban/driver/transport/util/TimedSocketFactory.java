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
 * $Id: TimedSocketFactory.java,v 1.7 2009/07/29 00:29:30 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.transport.util;

import com.sun.faban.driver.transport.sunhttp.SocketFactory;

import java.net.Socket;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.Proxy;
import java.io.IOException;

/**
 * Socket factory to create new timed socket.
 *
 * @author Akara Sucharitakul
 */
public class TimedSocketFactory extends SocketFactory {

    /**
     * Creates a socket through the given proxy.
     * @param proxy The proxy
     * @return The socket
     */
	public Socket createSocket(Proxy proxy) {
        return new TimedSocket(proxy);
    }

    /**
     * Creates a new socket.
     * @return The newly created socket
     * @throws IOException Error creating the socket
     */
    @Override public Socket createSocket() throws IOException {
        return new TimedSocket();
    }

    /**
     * @see javax.net.SocketFactory#createSocket(java.lang.String, int)
     */
    /**
     * Creates a new socket connected to the given host and port.
     * @param host The host to connect
     * @param port The port to use
     * @return The newly created and connected socket
     * @throws IOException Error creating the socket
     * @throws UnknownHostException The host is unknown
     */
	public Socket createSocket(String host, int port)
            throws IOException, UnknownHostException {
        return new TimedSocket(host, port);
    }

    /**
     * Creates a stream socket and connects it to the specified port using
     * a specified local address and port.
     * @param host      the name of the remote host, or <code>null</code>
     *                  for the loopback address.
     * @param port      the remote port
     * @param localAddr the local address the socket is bound to
     * @param localPort the local port the socket is bound to
     * @return The newly created and connected socket
     * @throws IOException if an I/O error occurs when creating the socket.
     * @throws UnknownHostException The host is unknown
     */
	public Socket createSocket(String host, int port, InetAddress localAddr,
                               int localPort)
            throws IOException, UnknownHostException {
        return new TimedSocket(host, port, localAddr, localPort);
    }

    /**
     * Creates a stream socket and connects it to the specified port
     * number at the specified IP address.     *
     * @param address the IP address.
     * @param port    the port number.
     * @return The newly created and connected socket
     * @throws IOException if an I/O error occurs when creating the socket.
     */
	public Socket createSocket(InetAddress address, int port)
            throws IOException {
        return new TimedSocket(address, port);
    }

    /**
     * Creates a stream socket and connects it to the specified port using
     * a specified local address and port.
     * @param host      the address of the remote host
     * @param port      the remote port
     * @param localAddr the local address the socket is bound to
     * @param localPort the local port the socket is bound to
     * @return The newly created and connected socket
     * @throws IOException if an I/O error occurs when creating the socket.
     */
	public Socket createSocket(InetAddress host, int port,
                               InetAddress localAddr, int localPort)
            throws IOException {
        return new TimedSocket(host, port, localAddr, localPort);
    }
}
