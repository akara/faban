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
package com.sun.faban.harness;

import com.sun.faban.common.NameValuePair;
import com.sun.faban.harness.util.XMLReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Collections;

/**
 * The ParamRepository is the programmatic representation of the
 * configuration file. It allows access to the xml file via xpath.
 * In addition, the ParamRepository also allows updating the configuration
 * file. Such updates should be made during the validation stage.
 */
public class ParamRepository {


    private XMLReader reader;
    private List<HostConfig> hostConfigs;

    /**
     * Constructor: Open specified repository.
     * @param file Name of repository
     * @param warnDeprecated Log warning when config file is deprecated
     */
    public ParamRepository(String file, boolean warnDeprecated) {
        reader = new XMLReader(file, true, warnDeprecated);
        reader.processHostPorts(); //Pre-scan the hosts:ports fields
    }

    /**
     * Generic parameter access method.
     * @param xpath
     * @return value of the parameter of type string
     */
    public String getParameter(String xpath) {
        return reader.getValue(xpath);
    }

    /**
     * Generic parameter access method.
     * @param xpath string
     * @param top element
     * @return value of the parameter
     */
    public String getParameter(String xpath, Element top) {
        return reader.getValue(xpath, top);
    }

    /**
     * Generic method to get NodeList for a given tagName.
     * @param tagName of type string
     * @return NodeList for the tagName
     */
    public NodeList getNodeListForTagName(String tagName){
        return reader.getNodeListForTagName(tagName);
    }

    /**
     * Generic method to get a list of top level nodes.
     * @return NodeList
     */
    public NodeList getTopLevelElements() {
        NodeList topLevelElements = reader.getTopLevelElements();
        return topLevelElements;
    }

     /**
     * Generic method to get NodeList for a given xpath.
     * @param xPath string
     * @return NodeList for the xpath
     */
    public NodeList getNodes(String xPath) {
        return reader.getNodes(xPath);
    }

    /**
     * Generic method to get NodeList for a given xpath
     * with respect to base node.
     * @param xPath string
     * @param top element
     * @return NodeList
     */
    public NodeList getNodes(String xPath, Element top) {
        return reader.getNodes(xPath, top);
    }

    /**
     * Generic method to get a Node for a given xpath.
     * @param xPath string
     * @return Node for the xpath
     */
    public Node getNode(String xPath) {
        return reader.getNode(xPath);
    }

    /**
     * Generic method to get a Node for a given xpath
     * with respect to base node.
     * @param xPath string
     * @param top element
     * @return Node for the xpath
     */
    public Node getNode(String xPath, Element top) {
        return reader.getNode(xPath, top);
    }

    /**
     * Adds a new XPath to the param repository.
     * @param baseXPath The base XPath to add this node
     * @param paramName The element name
     * @return The newly added element, or null on failure
     */
    public Element addParameter(String baseXPath, String paramName) {
        return reader.addNode(baseXPath, null, null, paramName);
    }

    /**
     * Adds a new XPath to the param repository.
     * @param baseXPath The base XPath to add this node
     * @param namespace The namespace for this new element, if any
     * @param prefix The namespace prefix for this new element, if any
     * @param paramName The element name
     * @return The newly added element, or null on failure
     */
    public Element addParameter(String baseXPath, String namespace,
                                String prefix, String paramName) {
        return reader.addNode(baseXPath, namespace, prefix, paramName);
    }

    /**
     * Adds a new XPath to the param repository,
     * based on a previously added parameter.
     * @param parent The parent element to add this node to
     * @param paramName The element name
     * @return The newly added element, or null on failure
     */
    public Element addParameter(Element parent, String paramName) {
        return reader.addNode(parent, null, null, paramName);
    }

    /**
     * Adds a new XPath to the param repository,
     * based on a previously added parameter.
     * @param parent The parent element to add this node to
     * @param namespace The namespace for this new element, if any
     * @param prefix The namespace prefix for this new element, if any
     * @param paramName The element name
     * @return The newly added element, or null on failure
     */
    public Element addParameter(Element parent, String namespace,
                                String prefix, String paramName) {
        return reader.addNode(parent, namespace, prefix, paramName);
    }

    /**
     * Sets or replaces the parameter referenced by the XPath.
     * @param xpath The xpath referencing the parameter
     * @param newValue The new value to set
     */
    public void setParameter(String xpath, String newValue) {
        reader.setValue(xpath, newValue);
    }

    /**
     * Sets the parameter for a newly added DOM element.
     * @param element The newly added element
     * @param value The value to assign to the element
     */
    public void setParameter(Element element, String value) {
        reader.setValue(element, value);
    }

    /**
     * Saves the parameter repository back to file if it has been modified.
     *
     * @throws Exception If there is an exception saving the repository.
     */
    public void save() throws Exception {
        reader.save(null);
    }

    /**
     * Generic parameter access method.
     * @param xpath
     * @return list containing all paramters with the xpath
     */
    public List<String> getParameters(String xpath) {
        return reader.getValues(xpath);
    }

    /**
     * Gets the attribute values for the specified attribute of a certain XPath.
     *
     * @param elementPath The XPath of the element
     * @param attributeName The name of the attribute
     * @return A list of attribute values
     */
    public List<String> getAttributeValues(String elementPath, String attributeName) {
        return reader.getAttributeValues(elementPath, attributeName);
    }

    /**
     * HostConfig structure holding a hostConfig element from the config file.
     */
    public static class HostConfig {
        public String[] hosts;
        public int[] numCpus;
        public String tools;
        public String userCommands;
    }

    private void readHostConfigs() throws ConfigurationException {
        ArrayList<HostConfig> hostConfigs = new ArrayList<HostConfig>();
        NodeList topLevelElements = getTopLevelElements();
        int topLevelSize = topLevelElements.getLength();
        for (int i = 0; i < topLevelSize; i++) {
            Node node = topLevelElements.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element ti = (Element) node;
            String ns = ti.getNamespaceURI();
            String topElement = ti.getNodeName();

            if("http://faban.sunsource.net/ns/fabanharness".equals(ns) &&
                    "jvmConfig".equals(topElement))
                continue;

            // Get the hosts
            String[] hosts = getEnabledHosts(ti);
            if (hosts == null || hosts.length == 0)
                continue;

            HostConfig hostConfig = new HostConfig();
            hostConfig.hosts = hosts;
            String[] vals = getTokenizedValue("fa:hostConfig/fh:cpus", ti);
            if (vals != null) {
                hostConfig.numCpus = new int[vals.length];
                for (int j = 0; j < vals.length; j++) {
                    try {
                        hostConfig.numCpus[j] = Integer.parseInt(vals[j]);
                    } catch (NumberFormatException e) {
                        throw new ConfigurationException(
                                "fa:hostConfig/fh:cpus under " +
                                node.getNodeName() +
                                " has a non-integer value: " + vals[j] + '.',
                                e);
                    }
                }
            }

            hostConfig.tools = getParameter("fa:hostConfig/fh:tools", ti);
            if (hostConfig.tools != null)
                hostConfig.tools = hostConfig.tools.trim();
            if (hostConfig.tools.length() == 0)
                hostConfig.tools = null;

            hostConfig.userCommands =
                    getParameter("fa:hostConfig/fh:userCommands", ti);
            if (hostConfig.userCommands != null)
                hostConfig.userCommands = hostConfig.userCommands.trim();
            if (hostConfig.userCommands.length() == 0)
                hostConfig.userCommands = null;

            hostConfigs.add(hostConfig);
        }
        this.hostConfigs = Collections.unmodifiableList(hostConfigs);
    }

    /**
     * Obtains the list of enabled host configurations.
     * @return A list of enabled hosts configurations
     * @throws ConfigurationException Error reading the configuration
     */
    public List<HostConfig> getHostConfigs() throws ConfigurationException {
        if (hostConfigs == null)
            readHostConfigs();
        return hostConfigs;
    }

    /**
     * Obtains the list of enabled hosts.
     * @param base element
     * @return A list of enabled hosts, grouped by host type.
     * @throws ConfigurationException
     */
    public String[] getEnabledHosts(Element base) throws ConfigurationException {
        String[] enabledHosts;
        if (getBooleanValue("fa:hostConfig/fh:enabled", base, true)) {
            // TODO: Take care of managed hosts/ports
            enabledHosts = getTokenizedValue("fa:hostConfig/fh:managedHosts",
                                                base);
            if (enabledHosts == null || enabledHosts.length == 0)
                enabledHosts = getTokenizedValue("fa:hostConfig/fa:host", base);

            if (enabledHosts == null)
                enabledHosts = new String[0];
        } else {
            enabledHosts = new String[0];
        }
       return enabledHosts;
    }

    /**
     * Obtains the list of enabled hostports.
     * @param base element
     * @return A list of enabled hostports.
     * @throws ConfigurationException
     */
    public List<NameValuePair<Integer>> getEnabledHostPorts(Element base)
            throws ConfigurationException {
        if (getBooleanValue("fa:hostConfig/fh:enabled", base))
            return getHostPorts(base);
        else
            return null;
    }

    /**
     * This returns tokenized values of parameters in a list.
     * Mainly used to get host(s)
     * @param xpath The xpath to the parameters
     * @return List of tokenized values
     */
    public List<String[]> getTokenizedParameters(String xpath) {
        ArrayList<String[]> params = new ArrayList<String[]>();
        List<String> entries = reader.getValues(xpath);
        for (String entry : entries) {
            StringTokenizer st = new StringTokenizer(entry);
            String[] values = new String[st.countTokens()];
            for (int i = 0; st.hasMoreTokens(); i++)
                values[i] = st.nextToken();
            params.add(values);
        }
        return params;
    }

    /**
     * Obtains the value at an XPath, tokenized into an array.
     * @param xpath XPath expression to get SPACE seperated values from a single
     * parameter. For Example sutConfig/host The values are seperated by SPACE
     * @return An array of hostnames.
     */
    public String[] getTokenizedValue(String xpath) {
        String val = reader.getValue(xpath);
        String[] hosts = null;
        if (val != null) {
            val = val.trim();
            if (val.length() > 0) {
                StringTokenizer st = new StringTokenizer(val);
                hosts = new String[st.countTokens()];
                for (int i = 0; st.hasMoreTokens(); i++)
                    hosts[i] = st.nextToken();
            }
        }

        if (hosts == null)
            hosts = new String[0];
        return hosts;
    }

    /**
     * Obtains the value at an XPath, tokenized into an array, from a specific
     * base node in the document.
     * @param xpath XPath expression to get SPACE seperated values from a single
     * parameter. For Example sutConfig/host The values are seperated by SPACE
     * @param base The base element.
     * @return An array of hostnames.
     */
    public String[] getTokenizedValue(String xpath, Element base) {
        String value = reader.getValue(xpath, base);
        if (value == null)
            return null;
        value = value.trim();
        if (value.length() == 0)
            return null;
        StringTokenizer st = new StringTokenizer(value);
        String[] hosts = new String[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++)
            hosts[i] = st.nextToken();
        return hosts;
    }

    /**
     * Obtains xpath values matching an xpath, broken into tokens.
     * @param xpath XPath expression to get  ',' and SPACE seperated 
     * values from a single parameter. For Example sutConfig/instances
     * The values are seperated by ',' and then by SPACE
     * @return List of arrays of hostnames
     */
    public List<String[]> getTokenizedList(String xpath) {
        // Each value should be passed as , and SPACE seperated strings
        ArrayList<String[]> list = new ArrayList<String[]>();
        StringTokenizer st = new StringTokenizer(reader.getValue(xpath));
        while (st.hasMoreTokens()) {
            ArrayList<String> l = new ArrayList<String>();
            StringTokenizer st2 = new  StringTokenizer(st.nextToken(), ",");
            while (st2.hasMoreTokens())
                l.add(st2.nextToken());

            list.add(l.toArray(new String[1]));
        }
        return list;
    }

    /**
     * Obtains the host:port name value pair list from the element
     * matching this XPath.
     * @param xPathExpr
     * @return The list of host:port elements, or null if the XPath does
     * not exist or does not point to a host:port node.
     */
    public List<NameValuePair<Integer>> getHostPorts(String xPathExpr) {
        return reader.getHostPorts(xPathExpr);
    }

    /**
     * Obtains the host:port pairs at this element.
     * @param base The base element
     * @return The list of host:port pairs
     */
    public List<NameValuePair<Integer>> getHostPorts(Element base) {
        return reader.getHostPorts(base);
    }

    /**
     * Obtains the host/role mappings.
     * @return The host/role mappings
     * @throws ConfigurationException Invalid host/role mapping
     */
    public List<NameValuePair<String>> getHostRoles()
            throws ConfigurationException {

        ArrayList<NameValuePair<String>> hostTypeList =
                new ArrayList<NameValuePair<String>>();
        NodeList topLevelElements = getTopLevelElements();
        int topLevelSize = topLevelElements.getLength();
        for (int i = 0; i < topLevelSize; i++) {
            Node node = topLevelElements.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element ti = (Element) node;
            String ns = ti.getNamespaceURI();
            String topElement = ti.getNodeName();
            if ("http://faban.sunsource.net/ns/fabanharness".equals(ns) &&
                    "jvmConfig".equals(topElement))
                continue;

            // Get the hosts
            String[] hosts = getEnabledHosts(ti);
            if (hosts == null || hosts.length == 0)
                continue;

            // Get the type of that host. This is the top level element name.
            String type = ti.getNodeName();

            // Then add the host and type pair to the list.
            for (String host : hosts) {
                NameValuePair<String> hostType = new NameValuePair<String>();
                hostType.name = host;
                hostType.value = type;
                hostTypeList.add(hostType);
            }
        }
        return hostTypeList;
    }

    /**
     * This method reads a value using the XPath and converts it to a boolean.
     * @param xpath XPath expression to the value which is true or false
     * @return true or false
     */
    public boolean getBooleanValue(String xpath) {
        return  Boolean.valueOf(reader.getValue(xpath)).booleanValue();
    }

    /**
     * Obtains the boolean value at the given xpath, providing a default
     * value if it does not exist or is not a boolean.
     * @param xpath The given xpath
     * @param defaultValue The default value
     * @return The boolean at the xpath, or the given default
     */
    public boolean getBooleanValue(String xpath, boolean defaultValue) {
        String s = reader.getValue(xpath);
        if (s == null || s.length() == 0)
            return defaultValue;
        else
            return Boolean.parseBoolean(s);

    }

    /**
     * This method reads a value using the XPath and converts it to a boolean.
     * @param xpath XPath expression to the value which is true or false
     * @param base element
     * @return true or false
     */
    public boolean getBooleanValue(String xpath, Element base) {
        return  Boolean.valueOf(reader.getValue(xpath, base)).booleanValue();
    }

    /**
     * This method reads a value using the XPath and converts it to a boolean.
     * @param xpath XPath expression to the value which is true or false
     * @param base element
     * @param defaultValue
     * @return true or false
     */
    public boolean getBooleanValue(String xpath, Element base,
                                   boolean defaultValue) {
        String s = reader.getValue(xpath, base);
        if (s == null || s.length() == 0)
            return defaultValue;
        else
            return Boolean.parseBoolean(s);

    }
}

