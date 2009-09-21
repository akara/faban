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

import java.nio.channels.Selector;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.util.Queue;

/**
 * The primary selector is the single entry point another thread may use
 * to modify the select channels and configurations. Otherwise it behaves
 * just like any other listener.
 *
 * @author Akara Sucharitakul
 */
public class PrimaryListener extends Listener {

    private SelectableChannel channelToRegister = null;
    private int operationToRegister = Integer.MIN_VALUE;
    private Object attachmentToRegister = null;

    /**
     * Constructs the primary listener with this selector.
     * @param selector The selector
     * @param config The log configuration
     * @param acceptQueue The accept queue
     */
    public PrimaryListener(Selector selector, LogConfig config,
                           Queue<SocketChannel> acceptQueue) {
        super(selector, config, acceptQueue);
    }

    /**
     * Hook for additional operations to be taken before the selector goes
     * back to block on select. All changes to the channels/keys should
     * be called here. The implementation in this class logs changes to
     * the channel.
     * @param selector The selector
     * @throws IOException Error registering the selector with the channel
     */
    protected void selectorOps(Selector selector) throws IOException {
        super.selectorOps(selector);
        if (channelToRegister != null) { // Register a pending channel
            synchronized (this) {
                try {
                    channelToRegister.register(selector, operationToRegister,
                            attachmentToRegister);
                } finally {
                    // Reset the values.
                    channelToRegister = null;
                    operationToRegister = Integer.MIN_VALUE;
                    attachmentToRegister = null;
                }
            }
        }
    }

    /**
     * Adds/changes the channel registration from another thread.
     * @param channel The channel to register
     * @param operation The operation of interest
     * @param attachment The attachment
     */
    public synchronized void register(SelectableChannel channel,
                                      int operation, Object attachment) {
        channelToRegister = channel;
        operationToRegister = operation;
        attachmentToRegister = attachment;
        selector.wakeup();
    }
}
