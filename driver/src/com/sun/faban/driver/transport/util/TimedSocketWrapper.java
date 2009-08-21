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
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.transport.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

/**
 * The TimedSocketWrapper wraps an existing, opened socket
 * with the timed artifacts. If a socket can be created from scratch,
 * TimedSocket should be used. But if we obtain a socket from a factory,
 * such as an SSL socket, we cannot extend that socket implementation. We
 * need to wrap it with this wrapper to enable timing of the sockets.
 *
 * @author Akara Sucharitakul
 */
public class TimedSocketWrapper extends Socket {

    private Socket delegate;

    /**
     * Creates a TimedSocketWrapper wrapping an existing socket.
     * @param socket The existing socket
     */
    public TimedSocketWrapper(Socket socket) {
        delegate = socket;
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
        // The streams returned are all timed.
        return new TimedInputStream(delegate.getInputStream());
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
        return new TimedOutputStream(delegate.getOutputStream());
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        delegate.setTcpNoDelay(on);
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return delegate.getTcpNoDelay();
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        delegate.setSoLinger(on, linger);
    }

    @Override
    public int getSoLinger() throws SocketException {
        return delegate.getSoLinger();
    }

    @Override
    public void sendUrgentData (int data) throws IOException  {
        delegate.sendUrgentData(data);
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        delegate.setOOBInline(on);
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return delegate.getOOBInline();
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        delegate.setSoTimeout(timeout);
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return delegate.getSoTimeout();
    }

    @Override
    public void setSendBufferSize(int size)
    throws SocketException{
        delegate.setSendBufferSize(size);
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        return delegate.getSendBufferSize();
    }

    @Override
    public void setReceiveBufferSize(int size)
    throws SocketException{
        delegate.setReceiveBufferSize(size);
    }

    @Override
    public int getReceiveBufferSize()
    throws SocketException{
        return delegate.getReceiveBufferSize();
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        delegate.setKeepAlive(on);
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return delegate.getKeepAlive();
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        delegate.setTrafficClass(tc);
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return delegate.getTrafficClass();
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        delegate.setReuseAddress(on);
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return delegate.getReuseAddress();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public void shutdownInput() throws IOException {
        delegate.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        delegate.shutdownOutput();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean isConnected() {
        return delegate.isConnected();
    }

    @Override
    public boolean isBound() {
        return delegate.isBound();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public boolean isInputShutdown() {
        return delegate.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return delegate.isOutputShutdown();
    }

    @Override
    public void setPerformancePreferences(int connectionTime,
                                          int latency,
                                          int bandwidth) {
        delegate.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        delegate.connect(endpoint);
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout)
            throws IOException {
        delegate.connect(endpoint, timeout);
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        delegate.bind(bindpoint);
    }

    @Override
    public InetAddress getInetAddress() {
        return delegate.getInetAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        return delegate.getLocalAddress();
    }

    @Override
    public int getPort() {
        return delegate.getPort();
    }

    @Override
    public int getLocalPort() {
        return delegate.getLocalPort();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return delegate.getRemoteSocketAddress();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return delegate.getLocalSocketAddress();
    }

    @Override
    public SocketChannel getChannel() {
        return delegate.getChannel();
    }
}
