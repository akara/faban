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
 * $Id: Listener.java,v 1.2 2006/06/29 19:38:42 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.logging;

import java.io.IOException;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The generic listener listens to incoming requests and handles them
 * appropriately.
 *
 * @author Akara Sucharitakul
 */
public class Listener implements Runnable {

    protected Selector selector;
    protected LogConfig config;

    Thread listenerThread;

    private Logger logger;
    private String className;
    private ArrayList taskList = new ArrayList();
    private boolean isShutdown = false;

    /**
     * Constructs a new listener with this selector. The selector is shared
     * between multiple listener threads.
     * @param selector The selector
     */
    public Listener(Selector selector, LogConfig config) {
        this.selector = selector;
        this.config = config;
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
            try {
                key = (SelectionKey) it.next();
                if (key.isValid()) {
                    if (key.isAcceptable()) {
                        acceptNewClient(key);
                    } else if (key.isReadable()) {
                        RequestProxy proxy = (RequestProxy) key.attachment();
                        if (!proxy.channelReady()) {
                            taskList.add(proxy);
                        }
                    } else if (key.isWritable()) {
                        ((RequestProxy) key.attachment()).channelReady();
                    }
                }
                it.remove();
            } catch (ClosedChannelException e) {
                key.cancel();
            } catch (IOException e) {
                key.cancel();
                logger.severe(e.getMessage());
                logger.throwing(className, "handleKeys", e);
            }
        }
        if (taskList.size() > 0) {
            for (int i = 0; i < taskList.size(); i++)
                config.threadPool.execute((Runnable) taskList.get(i));
            taskList.clear();
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
            throws IOException, ClosedChannelException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel channel = server.accept();
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
