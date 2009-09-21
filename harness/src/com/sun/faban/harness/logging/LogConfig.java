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

import com.sun.faban.harness.common.Config;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathException;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The log server configuration class.
 *
 * @author Akara Sucharitakul
 */
public class LogConfig {

    /** The logging port. */
    public int port = Config.LOGGING_PORT;

    /** The number of listener threads. */
    public int listenerThreads = 1;

    /** The listen queue size. Defaults to OS setting. */
    public int listenQSize = -1;

    /** Base number of service threads. */
    public int coreServiceThreads = 2;

    /** Maximum number of service threads. */
    public int maxServiceThreads = 10;

    /**
     * The service thread timeout.
     * The idle time for a thread before it gets terminated.
     */
    public int serviceThreadTimeout = 300; // timeout after 5 minutes.

    /** The primary listener. */
    public PrimaryListener primaryListener = null;

    /** The thread pool. */
    public ExecutorService threadPool = Config.THREADPOOL;

    /** The read buffer size. */
    public int readBufferSize = 2048;

    /**
     * Constructs the log server configuration.
     */
    public LogConfig() {
        Logger logger = Logger.getLogger(getClass().getName());
        File harnessXml = new File(Config.CONFIG_FILE);
        if (harnessXml.exists())
            try {
                DocumentBuilder parser = DocumentBuilderFactory.newInstance().
                        newDocumentBuilder();
                XPath xPath = XPathFactory.newInstance().newXPath();

                Node root = parser.parse(harnessXml).getDocumentElement();
                Node logServer = (Node) xPath.evaluate("logServer", root,
                                                        XPathConstants.NODE);

                if (logServer == null)
                    throw new XPathException("Element logServer not found.");

                String v = xPath.evaluate("port", logServer);
                if (v != null && v.length() > 0)
                    port = Integer.parseInt(v);

                v = xPath.evaluate("listenerThreads", logServer);
                if (v != null && v.length() > 0)
                    listenerThreads = Integer.parseInt(v);

                v = xPath.evaluate("listenQueueSize", logServer);
                if (v != null && v.length() > 0)
                    listenQSize = Integer.parseInt(v);

                v = xPath.evaluate("serviceThreads/core", logServer);
                if (v != null && v.length() > 0)
                    coreServiceThreads = Integer.parseInt(v);

                v = xPath.evaluate("serviceThreads/max", logServer);
                if (v != null && v.length() > 0)
                    maxServiceThreads = Integer.parseInt(v);

                v = xPath.evaluate("serviceThreads/timeOut", logServer);
                if (v != null && v.length() > 0)
                    serviceThreadTimeout = Integer.parseInt(v);

                v = xPath.evaluate("bufferSize", logServer);
                if (v != null && v.length() > 0)
                    readBufferSize = Integer.parseInt(v);

            } catch (Exception e) {
                logger.log(Level.WARNING, "Error reading harness " +
                                            "configuration file. " +
                                            e.getMessage(), e);
            }
    }
}
