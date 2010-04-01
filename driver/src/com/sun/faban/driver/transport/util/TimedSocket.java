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
package com.sun.faban.driver.transport.util;

import com.sun.faban.driver.engine.DriverContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The TimedSocket class extends the Socket class by timing the opening of
 * connections and using TimedInputStream and TimedOutputStream as the streams
 * for this socket. This allows collecting times for the benchmark at a very
 * low level, just before the request goes onto the socket and right after
 * the response comes back.
 *
 * @author Akara Sucharitakul
 */
public class TimedSocket extends Socket {

    private static Logger logger = Logger.getLogger(TimedSocket.class.getName());
    private static int bufferSize = -1;

    // Check whether the buffer size is overridden by the system property.
    static {
        String bufferSizeString = System.getProperty(
                "faban.socket.buffer.size");
        if (bufferSizeString != null) {
            int multiplier = 1;
            if (bufferSizeString.endsWith("k") ||
                    bufferSizeString.endsWith("K")) {
                bufferSizeString = bufferSizeString.substring(0,
                        bufferSizeString.length() - 1);
                multiplier = 1024;
            }
            try {
                bufferSize = Integer.parseInt(bufferSizeString) * multiplier;
                logger.log(Level.INFO, "Trying to set socket receive buffer " +
                        "size to " + bufferSize);

            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "faban.socket.buffer.size " +
                        "property format must be 999 or 999k. " +
                        "Leaving unset.");
            }
        }
    }

    static final int BUFFER_SIZE = bufferSize;

    private void verifyReceiveBufferSize() {
        if (BUFFER_SIZE != -1)
            try {
                setReceiveBufferSize(BUFFER_SIZE);
                int bufSize = getReceiveBufferSize();
                if (BUFFER_SIZE == bufSize) {
                    logger.info("Socket receive buffer size set to " + BUFFER_SIZE);
                } else {
                    logger.warning("Socket receive buffer size set to " +
                            bufSize + " despite requesting " + BUFFER_SIZE);
                }
            } catch (SocketException e) {
                logger.log(Level.WARNING, "Error setting socket buffer size.",
                        e);
            }
    }

    /**
     * Creates an unconnected socket, with the
     * system-default type of SocketImpl.
     */
    public TimedSocket() {
        super();
    }

    /**
     * Creates an unconnected socket, specifying the type of proxy, if any,
     * that should be used regardless of any other settings.
     * <P>
     * If there is a security manager, its <code>checkConnect</code> method
     * is called with the proxy host address and port number
     * as its arguments. This could result in a SecurityException.
     * <P>
     * Examples:
     * <UL> <LI><code>Socket s = new Socket(Proxy.NO_PROXY);</code> will create
     * a plain socket ignoring any other proxy configuration.</LI>
     * <LI><code>Socket s = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("socks.mydom.com", 1080)));</code>
     * will create a socket connecting through the specified SOCKS proxy
     * server.</LI>
     * </UL>
     *
     * @param proxy a {@link java.net.Proxy Proxy} object specifying what kind
     *              of proxying should be used.
     * @throws IllegalArgumentException if the proxy is of an invalid type
     *                                  or <code>null</code>.
     * @throws SecurityException        if a security manager is present and
     *                                  permission to connect to the proxy is
     *                                  denied.
     * @see java.net.ProxySelector
     * @see java.net.Proxy
     * @since 1.5
     */
    public TimedSocket(Proxy proxy) {
        super(proxy);
    }

    /**
     * Creates a stream socket and connects it to the specified port
     * number at the specified IP address.
     * <p/>
     * If the application has specified a socket factory, that factory's
     * <code>createSocketImpl</code> method is called to create the
     * actual socket implementation. Otherwise a "plain" socket is created.
     * <p/>
     * If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with the host address and <code>port</code>
     * as its arguments. This could result in a SecurityException.
     *
     * @param address the IP address.
     * @param port    the port number.
     * @throws java.io.IOException if an I/O error occurs when creating the socket.
     * @throws SecurityException   if a security manager exists and its
     *                             <code>checkConnect</code> method doesn't allow the operation.
     * @see java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
     * @see java.net.SocketImpl
     * @see java.net.SocketImplFactory#createSocketImpl()
     * @see SecurityManager#checkConnect
     */
    public TimedSocket(InetAddress address, int port) throws IOException {
        super(address, port);
    }

    /**
     * Creates an unconnected Socket with a user-specified
     * SocketImpl.
     * <P>
     *
     * @param impl an instance of a <B>SocketImpl</B>
     *             the subclass wishes to use on the Socket.
     * @throws java.net.SocketException if there is an error in the underlying protocol,
     *                                  such as a TCP error.
     * @since JDK1.1
     */
    protected TimedSocket(SocketImpl impl) throws SocketException {
        super(impl);
    }

    /**
     * Creates a stream socket and connects it to the specified port
     * number on the named host.
     * <p/>
     * If the specified host is <tt>null</tt> it is the equivalent of
     * specifying the address as <tt>{@link java.net.InetAddress#getByName InetAddress.getByName}(null)</tt>.
     * In other words, it is equivalent to specifying an address of the
     * loopback interface. </p>
     * <p/>
     * If the application has specified a server socket factory, that
     * factory's <code>createSocketImpl</code> method is called to create
     * the actual socket implementation. Otherwise a "plain" socket is created.
     * <p/>
     * If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with the host address and <code>port</code>
     * as its arguments. This could result in a SecurityException.
     *
     * @param host the host name, or <code>null</code> for the loopback address.
     * @param port the port number.
     * @throws java.net.UnknownHostException if the IP address of
     *                                       the host could not be determined.
     * @throws java.io.IOException           if an I/O error occurs when creating the socket.
     * @throws SecurityException             if a security manager exists and its
     *                                       <code>checkConnect</code> method doesn't allow the operation.
     * @see java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
     * @see java.net.SocketImpl
     * @see java.net.SocketImplFactory#createSocketImpl()
     * @see SecurityManager#checkConnect
     */
    public TimedSocket(String host, int port) throws UnknownHostException, IOException {
        super(host, port);
    }

    /**
     * Creates a socket and connects it to the specified remote host on
     * the specified remote port. The Socket will also bind() to the local
     * address and port supplied.
     * <p/>
     * If the specified host is <tt>null</tt> it is the equivalent of
     * specifying the address as <tt>{@link java.net.InetAddress#getByName InetAddress.getByName}(null)</tt>.
     * In other words, it is equivalent to specifying an address of the
     * loopback interface. </p>
     * <p/>
     * If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with the host address and <code>port</code>
     * as its arguments. This could result in a SecurityException.
     *
     * @param host      the name of the remote host, or <code>null</code> for the loopback address.
     * @param port      the remote port
     * @param localAddr the local address the socket is bound to
     * @param localPort the local port the socket is bound to
     * @throws java.io.IOException if an I/O error occurs when creating the socket.
     * @throws SecurityException   if a security manager exists and its
     *                             <code>checkConnect</code> method doesn't allow the operation.
     * @see SecurityManager#checkConnect
     * @since JDK1.1
     */
    public TimedSocket(String host, int port, InetAddress localAddr, int localPort) throws IOException {
        super(host, port, localAddr, localPort);
    }

    /**
     * Creates a socket and connects it to the specified remote address on
     * the specified remote port. The Socket will also bind() to the local
     * address and port supplied.
     * <p/>
     * If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with the host address and <code>port</code>
     * as its arguments. This could result in a SecurityException.
     *
     * @param address   the remote address
     * @param port      the remote port
     * @param localAddr the local address the socket is bound to
     * @param localPort the local port the socket is bound to
     * 
     * @throws java.io.IOException if an I/O error occurs when creating the socket.
     * @throws SecurityException   if a security manager exists and its
     *                             <code>checkConnect</code> method doesn't
     *                             allow the operation.
     * @see SecurityManager#checkConnect
     * @since JDK1.1
     */
    public TimedSocket(InetAddress address, int port, InetAddress localAddr,
                       int localPort) throws IOException {
        super(address, port, localAddr, localPort);
    }

    /**
     * Connects this socket to the server with a specified timeout value.
     * A timeout of zero or negative values default to 30,000 (30 seconds).
     * The connection will then block until established or an error occurs.
     * 
     * @param	endpoint the <code>SocketAddress</code>
     * @param	timeout the timeout value to be used in milliseconds.     
     * 
     * @throws java.nio.channels.IllegalBlockingModeException
     *                                  if this socket has an associated channel,
     *                                  and the channel is in non-blocking mode
     * @throws IllegalArgumentException if endpoint is null or is a
     *                                  SocketAddress subclass not supported by this socket
     * @throws	java.io.IOException if an error occurs during the connection
     * @throws	java.net.SocketTimeoutException if timeout expires before connecting
     * @since 1.4
     */
    @Override
	public void connect(SocketAddress endpoint, int timeout) throws IOException {

        verifyReceiveBufferSize();

        // Here we intercept the connect and capture the start time.
        DriverContext ctx = DriverContext.getContext();
        if (ctx != null)
            ctx.recordStartTime();
        if (timeout <= 0)
            timeout = 30000; // 30 second connect timeout.
        super.connect(endpoint, timeout);
        setSoTimeout(30000); // 30 second socket read timeout.
    }

    /**
     * Returns an input stream for this socket.
     * <p/>
     * <p> If this socket has an associated channel then the resulting input
     * stream delegates all of its operations to the channel.  If the channel
     * is in non-blocking mode then the input stream's <tt>read</tt> operations
     * will throw an {@link java.nio.channels.IllegalBlockingModeException}.
     * <p/>
     * <p>Under abnormal conditions the underlying connection may be
     * broken by the remote host or the network software (for example
     * a connection reset in the case of TCP connections). When a
     * broken connection is detected by the network software the
     * following applies to the returned input stream :-
     * <p/>
     * <ul>
     * <p/>
     * <li><p>The network software may discard bytes that are buffered
     * by the socket. Bytes that aren't discarded by the network
     * software can be read using {@link java.io.InputStream#read read}.
     * <p/>
     * <li><p>If there are no bytes buffered on the socket, or all
     * buffered bytes have been consumed by
     * {@link java.io.InputStream#read read}, then all subsequent
     * calls to {@link java.io.InputStream#read read} will throw an
     * {@link java.io.IOException IOException}.
     * <p/>
     * <li><p>If there are no bytes buffered on the socket, and the
     * socket has not been closed using {@link #close close}, then
     * {@link java.io.InputStream#available available} will
     * return <code>0</code>.
     * <p/>
     * </ul>
     *
     * @return an input stream for reading bytes from this socket.
     * @throws java.io.IOException if an I/O error occurs when creating the
     *                             input stream, the socket is closed, the socket is
     *                             not connected, or the socket input has been shutdown
     *                             using {@link #shutdownInput()}
     */
    @Override
	public InputStream getInputStream() throws IOException {
        // The streams returned areall timed.
        return new TimedInputStream(super.getInputStream());
    }

    /**
     * Returns an output stream for this socket.
     * <p/>
     * <p> If this socket has an associated channel then the resulting output
     * stream delegates all of its operations to the channel.  If the channel
     * is in non-blocking mode then the output stream's <tt>write</tt>
     * operations will throw an {@link
     * java.nio.channels.IllegalBlockingModeException}.
     *
     * @return an output stream for writing bytes to this socket.
     * @throws java.io.IOException if an I/O error occurs when creating the
     *                             output stream or if the socket is not connected.
     */
    @Override
	public OutputStream getOutputStream() throws IOException {
        // The streams returned are all timed.
        return new TimedOutputStream(super.getOutputStream());
    }
}
