/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://faban.dev.java.net/public/CDDLv1.0.html or
 * install_dir/license.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id$
 *
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

import javax.xml.namespace.NamespaceContext;
import javax.xml.XMLConstants;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * A NamespaceContext with Faban namespace conventions precoded.
 * The Faban prefixes and namespace URIs are as follows:<br>
 * <table>
 * <tr><th>Prefix</th><th>Namespace URI</th></tr>
 * <tr><td>fa</td><td>http://faban.sunsource.net/ns/faban</td></tr>
 * <tr><td>fd</td><td>http://faban.sunsource.net/ns/fabandriver</td></tr>
 * <tr><td>fh</td><td>http://faban.sunsource.net/ns/fabanharness</td></tr>
 * </table>
 * <br>You can add additional namespace prefixes using the addNamespace method.
 *
 * @author Akara Sucharitakul
 */
public class FabanNamespaceContext implements NamespaceContext {

    private static Logger logger = Logger.getLogger(
                                    FabanNamespaceContext.class.getName());
    LinkedHashMap<String, String> nsMap = new LinkedHashMap<String, String>();
    LinkedHashMap<String, ArrayList<String>> prefixMap =
            new LinkedHashMap<String, ArrayList<String>>();

    /**
     * Constructs the Faban namespace context that contains all Faban
     * namespaces to start with.
     */
    public FabanNamespaceContext() {
        addNamespace(XMLConstants.XML_NS_URI, XMLConstants.XML_NS_PREFIX);
        addNamespace(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                                            XMLConstants.XMLNS_ATTRIBUTE);
        addNamespace("http://faban.sunsource.net/ns/faban", "fa");
        addNamespace("http://faban.sunsource.net/ns/fabandriver","fd");
        addNamespace("http://faban.sunsource.net/ns/fabanharness", "fh");
    }

    /**
     * Adds a namespace prefix to the NamespaceContext.
     * @param namespaceURI The namespace URI
     * @param prefix The prefix we use for referring to the elements/attributes
     */
    public void addNamespace(String namespaceURI, String prefix) {
        logger.finer("Adding " + namespaceURI + ", " + prefix);
        if (namespaceURI == null || prefix == null)
            throw new IllegalArgumentException("prefix or namespaceURI is null!");
        nsMap.put(prefix, namespaceURI);
        ArrayList<String> prefixList = prefixMap.get(namespaceURI);
        if (prefixList == null) {
            prefixList = new ArrayList<String>();
            prefixList.add(prefix);
            prefixMap.put(namespaceURI, prefixList);
        } else {
            if (!prefixList.contains(prefix))
                prefixList.add(prefix);
        }
    }

    public String getNamespaceURI(String prefix) {
        if (prefix == null)
            throw new IllegalArgumentException("prefix is null!");
        String namespaceURI = nsMap.get(prefix);
        if (namespaceURI == null)
            namespaceURI = XMLConstants.NULL_NS_URI;
        logger.finer("getNamespaceURI(\"" + prefix + "\") = " + namespaceURI);
        return namespaceURI;
    }

    public String getPrefix(String namespaceURI) {
        if (namespaceURI == null)
            throw new IllegalArgumentException("namespaceURI is null!");
        ArrayList<String> prefixList = prefixMap.get(namespaceURI);
        String prefix = null;
        if (prefixList != null)
            prefix = prefixList.get(0);
        logger.finer("getPrefix(\"" + namespaceURI + "\") = " + prefix);
        return prefix;
    }


    public Iterator getPrefixes(String namespaceURI) {
        logger.finer("getPrefixes(\"" + namespaceURI + "\")");
        if (namespaceURI == null)
            throw new IllegalArgumentException("namespaceURI is null!");
        ArrayList<String> prefixList = prefixMap.get(namespaceURI);
        if (prefixList == null)
            prefixList = new ArrayList<String>();
        return Collections.unmodifiableList(prefixList).iterator();
    }
}
