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

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The protocol handler factory analyzes the input buffer for different
 * supported protocols and returns the appropriate protocol handler
 * upon determining the correct one.
 *
 * @author Akara Sucharitakul
 */
public class ProtocolHandlerFactory {

    /**
     * The handlers are thread-local and are reused from one invocation
     * to another.
     */
    private static ThreadLocal localHandlers = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return new HashMap();
        }
    };

    private static final byte[][] HEADERS = { "<record>".getBytes(),
                                              "<?xml".getBytes(),
                                              "</log>".getBytes()};

    private static int maxHeaderLength;

    static {
        maxHeaderLength = 0;
        for (int i = 0; i < HEADERS.length; i++)
            if (HEADERS[i].length > maxHeaderLength)
                maxHeaderLength = HEADERS[i].length;
    }

    private static final String[] handlerNames = {
        "com.sun.faban.harness.logging.LogHandler",
        "com.sun.faban.harness.logging.LogHandler",
        "com.sun.faban.harness.logging.LogHandler"};

    private LogConfig config;
    private Logger logger;
    private String className;
    private HashMap handlerClasses;

    /**
     * Constructs the protocol handler factory.
     * @param config The log configuration
     */
    public ProtocolHandlerFactory(LogConfig config) {
        this.config = config;
        className = getClass().getName();
        logger = Logger.getLogger(className);
        handlerClasses = new HashMap(HEADERS.length);
    }

    /**
     * Looks at the buffer and determines the protocol. Returns
     * null if there is no suficient data to determine the protocol.
     *
     * @param buffer The buffer
     * @param count The significant size of the buffer to analyze
     * @return  The appropriate protocol handler, or null if there is not
     *          enough data
     * @throws  UnsupportedProtocolException If the protocol cannot be
     *          determined
     */
    public ProtocolHandler getHandler(ByteBuffer buffer, int count)
            throws UnsupportedProtocolException {

        int headerIdx = matchBuffer(buffer, count);
        if (headerIdx == -1)
            return null;

        HashMap handlers = (HashMap) localHandlers.get();

        if (logger.isLoggable(Level.FINEST))
            logger.finest("Handler header index: " + headerIdx +
                    "\nHandler class: " + handlerNames[headerIdx]);

        ProtocolHandler handler = (ProtocolHandler)
                handlers.get(handlerNames[headerIdx]);
        if (handler != null)
            return handler;

        try {
            Class handlerClass = (Class) handlerClasses.get(
                    handlerNames[headerIdx]);

            if (handlerClass == null) {
                handlerClass = Class.forName(handlerNames[headerIdx]);
                handlerClasses.put(handlerNames[headerIdx], handlerClass);
            }

            handler = (ProtocolHandler) handlerClass.newInstance();
            handler.setConfig(config);
            handlers.put(handlerNames[headerIdx], handler);
            return handler;
        } catch (InstantiationException e) {
            logger.severe(e.getMessage());
            logger.throwing(className, "getHandler", e);
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            logger.severe(e.getMessage());
            logger.throwing(className, "getHandler", e);
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            logger.severe(e.getMessage());
            logger.throwing(className, "getHandler", e);
            throw new RuntimeException(e);
        }
    }

    private int matchBuffer(ByteBuffer buffer, int count)
            throws UnsupportedProtocolException {

        if (count < maxHeaderLength)
            return -1;

        // Initialize the not-matching array to false
        boolean[] notMatch = new boolean[HEADERS.length];

        for (int i = 0; i < maxHeaderLength; i++) {
            boolean matchAny = false;
            byte c = buffer.get(i);
            for (int j = 0; j < HEADERS.length; j++) {
                if (notMatch[j] || HEADERS[j].length <= i)
                    continue;
                if (HEADERS[j][i] != c) {
                    notMatch[j] = true;
                    continue;
                }
                matchAny = true;
            }
            if (!matchAny)
                break;
        }

        for (int i = 0; i < notMatch.length; i++) {
            if (!notMatch[i])
                return i;
        }

        throw new UnsupportedProtocolException(
                "Data does not match any protocol header!");
    }
}
