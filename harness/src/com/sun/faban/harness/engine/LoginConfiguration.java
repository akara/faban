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
package com.sun.faban.harness.engine;

import com.sun.faban.harness.common.Config;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Faban harness login configuration object.
 */
public class LoginConfiguration extends Configuration {

    Logger logger = Logger.getLogger(getClass().getName());
    AppConfigurationEntry[] entries = new AppConfigurationEntry[1];

    /**
     * Reads the login configuration from a DOM tree.
     * @param root The root node
     * @param xPath An xpath instance
     * @throws XPathExpressionException If there is an error in the xpath
     */
    public void readConfig(Node root, XPath xPath)
            throws XPathExpressionException {

        Node loginModule = (Node) xPath.evaluate("security/loginModule", root,
                                                 XPathConstants.NODE);
        String moduleName = xPath.evaluate("class", loginModule);
        logger.fine("Login module: " + moduleName);
        Map<String, String> options = new HashMap<String, String>();
        NodeList propNodes = (NodeList) xPath.evaluate("property", loginModule,
                                                       XPathConstants.NODESET);
        for (int i = 0; i < propNodes.getLength(); i++) {
            Node propNode = propNodes.item(i);
            String name = xPath.evaluate("name", propNode);
            String value = xPath.evaluate("value", propNode);
            logger.fine("Property: " + name + '=' + value);
            options.put(name, value);
        }

        entries[0] = new AppConfigurationEntry(moduleName,
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
    }
    /**
     * Retrieve the AppConfigurationEntries for the specified <i>name</i>
     * from this Configuration.
     * <p/>
     * <p/>
     *
     * @param name the name used to index the Configuration.
     * @return an array of AppConfigurationEntries for the specified <i>name</i>
     *         from this Configuration, or null if there are no entries
     *         for the specified <i>name</i>
     */
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        return entries;
    }

    /**
     * Refresh and reload the Configuration.
     * <p/>
     * <p> This method causes this Configuration object to refresh/reload its
     * contents in an implementation-dependent manner.
     * For example, if this Configuration object stores its entries in a file,
     * calling <code>refresh</code> may cause the file to be re-read.
     * <p/>
     * <p/>
     *
     * @throws SecurityException if the caller does not have permission
     *                           to refresh its Configuration.
     */
    public void refresh() {

        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().
                                                newDocumentBuilder();
            XPath xPath = XPathFactory.newInstance().newXPath();
            Node root = parser.parse(new File(Config.CONFIG_DIR +
                        "harness.xml")).getDocumentElement();

            readConfig(root, xPath);

        } catch (Exception e) {
            Logger logger = Logger.getLogger(getClass().getName());
            logger.log(Level.SEVERE, "Error refreshing login configuration.",
                    e);
        }
    }
}
