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
package com.sun.faban.harness.logging;

import java.io.IOException;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The generic listener listens to incoming requests and handles them
 * appropriately.
 *
 * @author Akara Sucharitakul
 */
public class Listener implements Runnable {

    /** The selector instance used for listening. */
    protected Selector selector;

    /** The log configuration. */
    protected LogConfig config;

    Thread listenerThread;

    private Logger logger;
    private String className;
    Queue<SocketChannel> acceptQueue;
    private ArrayList taskList = new ArrayList();
    private boolean isShutdown = false;

    /**
     * Constructs a new listener with this selector. The selector is shared
     * between multiple listener threads.
     * @param selector The selector
     * @param config The log configuration
     * @param acceptQueue The queue for accepted connections
     */
    public Listener(Selector selector, LogConfig config,
                    Queue<SocketChannel> acceptQueue) {
        this.selector = selector;
        this.config = config;
        this.acceptQueue = acceptQueue;
        className = this.getClass().getName();
        logger = Logger.getLogger(className);
    }

    /**
     * The thread's run method.
     */
    public void run() {
        while (!isShutdown) {
            try {
                selector.select();
                Set keySet = selector.selectedKeys();
                handleKeys(keySet);

                // Hook for additional select operations
                // before selector goes back waiting.
                selectorOps(selector);
            } catch (CancelledKeyException e) {
                logger.log(Level.FINER, "Error in selector operation", e);
            } catch (Exception e) {
                logger.log(Level.SEVERE,  "Error in selector operation", e);
            }
        }
        logger.fine("Exiting Listener Thread " + Thread.currentThread().hashCode());
    }

    /**
     * Handles the selected keys once they are selected.
     * @param keySet The set of selected keys
     */
    private void handleKeys(Set keySet) {
        SelectionKey key = null;

        Iterator it = keySet.iterator();
        while (it.hasNext()) {
            // try {
                key = (SelectionKey) it.next();
                if (key.isValid()) {
                    if (key.isReadable()) {
                        RequestProxy proxy = (RequestProxy) key.attachment();
                        if (!proxy.channelReady()) {
                            taskList.add(proxy);
                        }
                    } else if (key.isWritable()) {
                        ((RequestProxy) key.attachment()).channelReady();
                    }
                }
                it.remove();
        }
        if (taskList.size() > 0) {
            for (int i = 0; i < taskList.size(); i++)
                config.threadPool.execute((Runnable) taskList.get(i));
            taskList.clear();
        }

        // Accepting clients.
        SocketChannel channel;
        while ((channel = acceptQueue.poll()) != null)
            try {
                acceptNewClient(channel);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error accepting new client", e);
            }
    }

    /**
     * Accepts a new client and registers the resulting
     * socket to the selector.
     *
     * @param key The key result of the accept
     * @throws java.io.IOException Error accepting new client
     * @throws java.nio.channels.ClosedChannelException Client has already disconnected
     */
    public void acceptNewClient(SelectionKey key)
            throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        acceptNewClient(server.accept());
    }

    void acceptNewClient(SocketChannel channel) throws IOException {
        channel.configureBlocking(false);
        SelectionKey readKey = channel.register(selector, SelectionKey.OP_READ);
        RequestProxy proxy = new RequestProxy(config, readKey);
        readKey.attach(proxy);
    }


    /**
     * Hook for additional operations to be taken before the selector goes
     * back to block on select. All changes to the channels/keys should
     * be called here. The implementation in this class is empty.
     * @param selector The selector
     * @throws IOException An I/O error occurred
     */
    protected void selectorOps(Selector selector) throws IOException {
    }

    void shutdown() {
        isShutdown = true;
        selector.wakeup();
        // TODO: More cleanup
        Set keySet = selector.keys();
        for (Iterator iter = keySet.iterator(); iter.hasNext();) {
            SelectionKey key = (SelectionKey) iter.next();
            key.cancel();
            try {
                key.channel().close();
            } catch(IOException e) {
                logger.severe(e.getMessage());
                logger.log(Level.FINE, "Exception", e);
            }
        }
    }
}
