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
package com.sun.faban.driver.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Copyright (c) 2000-2004 by Sun Microsystems, Inc. All Rights Reserved.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * $Id$
 *
 */

/**
 * The PlotServer is the server side implementation to feed data to all
 * StreamChart clients that try to connect to it.
 * @author Akara Sucharitakul
 */
public class PlotServer {

    private List plotChannels;
    private int numSeries;
    private String target;
    private ByteBuffer writeBuffer;

    /**
     * Constructs a PlotServer listening to a particular TCP port and
     * having a buffer for sending certain number of values at a time.
     * @param numSeries The number of data values to send and plot
     * @param target The port number or file to write to
     * @throws IOException Cannot listen to port
     */
    public PlotServer(int numSeries, String target) throws IOException {
        this.numSeries = numSeries;
        this.target = target;
        writeBuffer = ByteBuffer.allocateDirect(8 * numSeries);
        plotChannels = Collections.synchronizedList(new ArrayList());
        new Listener();
    }

    /**
     * Obtains the number of channels currently writing to.
     * @return The number of open channels
     */
    public int numChannels() {
        return plotChannels.size();
    }

    /**
     * Sends a number of y values to the graph to plot.
     * @param values The y values to plot
     * @exception IOException if the data cannot be written
     */
    public void plot(double[] values) throws IOException {
        if (values.length != numSeries)
            throw new ArrayIndexOutOfBoundsException("Dimension mismatch");
        writeBuffer.clear();
        for (int i = 0; i < values.length; i++)
            writeBuffer.putDouble(values[i]);
        sendData();
    }

    /**
     * Sends a single y value to the graph to plot.
     * @param value The y value to plot
     * @exception IOException if the data cannot be written
     */
    public void plot(double value) throws IOException {
        if (numSeries != 1)
            throw new ArrayIndexOutOfBoundsException("Dimension mismatch");
        writeBuffer.clear();
        writeBuffer.putDouble(value);
        sendData();
    }

    private void sendData() throws IOException {
        for (int i = 0; i < plotChannels.size(); i++) {
            WritableByteChannel channel =
                    (WritableByteChannel) plotChannels.get(i);
            try {
                writeBuffer.flip();
                channel.write(writeBuffer);
            } catch (IOException e) {
                plotChannels.remove(i);
                --i;
                try {
                    channel.close();
                } catch (IOException ce) {
                    ce.printStackTrace();
                }
                throw e;
            }
        }
    }

    class Listener extends Thread {

        ServerSocketChannel ssocket;

        Listener() throws IOException {
            try { // First assume its a port
                int port = Integer.parseInt(target);
                ssocket = ServerSocketChannel.open();
                ssocket.socket().bind(new InetSocketAddress(port));
                ssocket.configureBlocking(true);
                setDaemon(true);
                start();
            } catch (NumberFormatException e) {
                // If we're here, its a file
                FileChannel channel = new FileOutputStream(target).getChannel();
                plotChannels.add(channel);
            }
        }

        public void run() {
            for (;;) {
                try {
                    SocketChannel socket = ssocket.accept();
                    socket.configureBlocking(true);
                    plotChannels.add(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
