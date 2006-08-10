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
 * $Id: LogConfig.java,v 1.4 2006/08/10 01:34:37 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.logging;

import com.sun.faban.harness.common.Config;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
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
    public int port = Config.LOGGING_PORT;
    public int listenerThreads = 1;
    public int listenQSize = -1;
    public int coreServiceThreads = 2;
    public int maxServiceThreads = 10;
    public int serviceThreadTimeout = 300; // timeout after 5 minutes.
    public PrimaryListener primaryListener = null;
    public ExecutorService threadPool = null;
    public int readBufferSize = 2048;

    public LogConfig() {
        Logger logger = Logger.getLogger(getClass().getName());
        File harnessXml = new File(Config.CONFIG_FILE);
        if (harnessXml.exists())
            try {
                DocumentBuilder parser = DocumentBuilderFactory.newInstance().
                        newDocumentBuilder();
                XPath xPath = XPathFactory.newInstance().newXPath();

                Node root = parser.parse(harnessXml).getDocumentElement();
                Node logConfig = (Node) xPath.evaluate("logConfig", root,
                                                        XPathConstants.NODE);

                String v = xPath.evaluate("port", logConfig);
                if (v != null && v.length() > 0)
                    port = Integer.parseInt(v);

                v = xPath.evaluate("listenerThreads", logConfig);
                if (v != null && v.length() > 0)
                    listenerThreads = Integer.parseInt(v);

                v = xPath.evaluate("listenQueueSize", logConfig);
                if (v != null && v.length() > 0)
                    listenQSize = Integer.parseInt(v);

                v = xPath.evaluate("serviceThreads/core", logConfig);
                if (v != null && v.length() > 0)
                    coreServiceThreads = Integer.parseInt(v);

                v = xPath.evaluate("serviceThreads/max", logConfig);
                if (v != null && v.length() > 0)
                    maxServiceThreads = Integer.parseInt(v);

                v = xPath.evaluate("serviceThreads/timeOut", logConfig);
                if (v != null && v.length() > 0)
                    serviceThreadTimeout = Integer.parseInt(v);

                v = xPath.evaluate("bufferSize", logConfig);
                if (v != null && v.length() > 0)
                    readBufferSize = Integer.parseInt(v);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error reading harness " +
                                            "configuration file.", e);
            }
    }
}
