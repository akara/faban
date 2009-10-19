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
package com.sun.faban.harness.util;

import com.sun.faban.common.NameValuePair;
import com.sun.faban.common.ParamReader;
import com.sun.faban.common.Utilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides all XML accesses to the parameter repository and other XML files.
 */
public class XMLReader {

    private static Logger logger = Logger.getLogger(XMLReader.class.getName());
    private Document doc;
    private XPath xPath;
    private String file;
    private boolean updated = false;
    private HashMap<Node, ArrayList<NameValuePair<Integer>>> hostPortsTable;

    /**
     * Constructs the XML reader.
     * @param file The XML file
     */
    public XMLReader(String file) {
        initLocal(file);
    }

    /**
     * Constructs the XML reader.
     * @param file The XML file
     * @param useFabanNS Whether to apply Faban namespaces in this file
     * @param warnDeprecated Whether to warn for deprecated Faban elements
     */
    public XMLReader(String file, boolean useFabanNS, boolean warnDeprecated) {
        if (useFabanNS)
            try {
                this.file = file;
                ParamReader reader = new ParamReader(file, warnDeprecated);
                doc = reader.getDocument();
                xPath = reader.getXPath();
            } catch (Exception e) {
                throw new XMLException(e.getMessage(), e);
            }
        else
            initLocal(file);
    }

    private String completeXPath(String xpath) {
        // If no absolute xpath is not given use //xpath to find the parameter
        if (xpath.charAt(0) != '/') {
            xpath = "//" + xpath;
        } else {
            //the JXPathContext expects 'params' (which is the variable name returned by XMLFile
            xpath = "params" + xpath;
        }
        return xpath;
    }

    private void initLocal(String file) {
        this.file = file;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().
                    newDocumentBuilder();
            doc = builder.parse(new File(file));
            xPath = XPathFactory.newInstance().newXPath();
        } catch (Exception e) {
            throw new XMLException(e.getMessage(), e);
        }
    }

    /**
     * Returns nodelist for the given tag.
     * @param tagName
     * @return nodelist
     */
    public NodeList getNodeListForTagName(String tagName){
        return doc.getElementsByTagName(tagName);
    }

    /**
     * Returns nodelist for top level elements(Children of root element).
     * @return nodelist.
     */
    public NodeList getTopLevelElements() {
        NodeList topLevelElements = doc.getDocumentElement().getChildNodes();
        return topLevelElements;
    }

    /**
     * Returns the root element.
     * @return element.
     */
    public Element getRootNode() {
        return doc.getDocumentElement();
    }

    /**
     * Returns node for the given xpath.
     * @param xpath
     * @return node
     */
    public Node getNode(String xpath) {
        xpath = completeXPath(xpath);
        return getNode(xpath, doc);
    }

    /**
     * Returns node for the given xpath based on the base node.
     * @param xpath
     * @param base
     * @return node.
     */
    public Node getNode(String xpath, Node base) {
        Node node = null;
        try {
            node = (Node) xPath.evaluate(xpath,
                                                base, XPathConstants.NODE);
        } catch (XPathExpressionException e) {
            logger.log(Level.WARNING, "Error processing XPath expression: " +
                                                                xpath, e);
        }
        return node;
    }

    /**
     * Returns the nodelist for a given xpath.
     * @param xpath
     * @return nodelist.
     */
    public NodeList getNodes(String xpath) {
        xpath = completeXPath(xpath);
        return getNodes(xpath, doc);
    }

    /**
     * Returns the nodelist for a given xpath based on a base node.
     * @param xpath
     * @param base
     * @return nodelist.
     */
    public NodeList getNodes(String xpath, Node base) {
        NodeList nodes = null;
        try {
            nodes = (NodeList) xPath.evaluate(xpath,
                                                base, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            logger.log(Level.WARNING, "Error processing XPath expression: " +
                                                                xpath, e);
        }
        return nodes;
    }

    /**
     * Returns the value for the given xpath.
     * @param xpath
     * @return string.
     */
    public String getValue(String xpath) {
        xpath = completeXPath(xpath);
        return getValue(xpath, doc);
    }

    /**
     * Returns the value for the given xpath based on the base node.
     * @param xpath
     * @param base
     * @return string.
     */
    public String getValue(String xpath, Node base) {
        try {
            return xPath.evaluate(xpath, base);
        } catch (XPathExpressionException e) {
            throw new XMLException("Error evaluating " + xpath + ", " +
                                   e.getMessage(), e);
        }
    }

    /**
     * Returns the list of values for a given xpath.
     * @param xpath
     * @return List<String>.
     */
    public List<String> getValues(String xpath) {
        xpath = completeXPath(xpath);
        return getValues(xpath, doc);
    }

    /**
     * Returns the list of values for a given xpath based on base node.
     * @param xpath
     * @param base
     * @return List<String>.
     */
    public List<String> getValues(String xpath, Node base) {
        try {
            NodeList nodeList = (NodeList) xPath.evaluate(
                                xpath, base, XPathConstants.NODESET);
            int length = nodeList.getLength();
            ArrayList<String> vList = new ArrayList<String>();
            for (int i = 0; i < length; i++) {
                Node node = nodeList.item(i);
                String value = null;
                nodeTraceLoop:
                while (node != null)
                    switch (node.getNodeType()) {
                        case Node.ATTRIBUTE_NODE :
                        case Node.CDATA_SECTION_NODE :
                        case Node.COMMENT_NODE :
                        case Node.PROCESSING_INSTRUCTION_NODE :
                        case Node.TEXT_NODE : value = node.getNodeValue();
                                              break nodeTraceLoop;
                        default : node = node.getFirstChild();
                    }

                if (value == null)
                    value = "";
                vList.add(value);
            }
            return vList;
        } catch (XPathExpressionException e) {
            throw new XMLException("Error evaluating " + xpath + ", " +
                                   e.getMessage(), e);
        }
    }

    /**
     * Returns the list of attribute values for a given xpath and attribute.
     * @param xpath
     * @param attribute
     * @return List<String>.
     */
    public List<String> getAttributeValues(String xpath, String attribute) {
        xpath = completeXPath(xpath);
        return getAttributeValues(xpath, attribute, doc);
    }

    /**
     * Returns the list of attribute values for a given xpath and attribute
     * based on base node.
     * @param xpath
     * @param attribute
     * @param base
     * @return List<String>.
     */
    public List<String> getAttributeValues(String xpath, String attribute, Node base) {
        try {
            NodeList nodeList = (NodeList) xPath.evaluate(xpath + "[@" + attribute + "]",
                                base, XPathConstants.NODESET);
            int length = nodeList.getLength();
            ArrayList<String> vList = new ArrayList<String>();
            for (int i = 0; i < length; i++) {
                Node node = nodeList.item(i);
                String value = null;
                nodeTraceLoop:
                while (node != null)
                    switch (node.getNodeType()) {
                        case Node.ELEMENT_NODE:
                            Element e = (Element) node;
                            value = e.getAttribute(attribute);
                            break nodeTraceLoop;
                        case Node.ATTRIBUTE_NODE :
                            value = node.getNodeValue();
                            break nodeTraceLoop;
                        default : node = node.getFirstChild();
                    }

                if (value == null)
                    value = "";
                vList.add(value);
            }
            return vList;
        } catch (XPathExpressionException e) {
            throw new XMLException("Error evaluating " + xpath + ", " +
                                   e.getMessage(), e);
        }
    }

    /**
     * Sets the new value for a given xpath.
     * @param xpath The XPath to the element to set the value
     * @param newValue The new value
     */
    public void setValue(String xpath, String newValue) {
        // If no absolute xpath is not given use //xpath to find the parameter
        xpath = completeXPath(xpath);
        
        try {
            NodeList nodeList = (NodeList) xPath.evaluate(
                                xpath, doc, XPathConstants.NODESET);
            int length = nodeList.getLength();
            boolean updated = false;
            for (int i = 0; i < length; i++) {
                Node node = nodeList.item(i);
                nodeTraceLoop:
                while (node != null) {
                    short nodeType = node.getNodeType();
                    logger.finer("XPath: " + xpath);
                    logger.finer("NodeType[" + i + "]: " + nodeType);

                    switch (nodeType) {
                        case Node.ATTRIBUTE_NODE :
                        case Node.CDATA_SECTION_NODE :
                        case Node.COMMENT_NODE :
                        case Node.PROCESSING_INSTRUCTION_NODE :
                        case Node.TEXT_NODE : node.setNodeValue(newValue);
                                              updated = true;
                                              break nodeTraceLoop;
                        default :   if (node.hasChildNodes()) {
                                        node = node.getFirstChild();
                                    } else {
                                        node.setTextContent(newValue);
                                        updated = true;
                                        break nodeTraceLoop;
                                    }
                    }
                }
            }
            if (updated)
                this.updated = true;
            else
                throw new XMLException("Update of XPath " + xpath +
                        " unsuccessful!");
        } catch (XPathExpressionException e) {
            throw new XMLException("Error evaluating " + xpath + ", " +
                                   e.getMessage(), e);
        }
    }

    /**
     * Sets the new value for the element.
     * @param element The DOM element
     * @param value The new value
     */
    public void setValue(Element element, String value) {
        NodeList children = element.getChildNodes();
        boolean valueSet = false;
        int childCount = children.getLength();
        for (int i = 0; i < childCount; i++) {
            Node child = children.item(i);
            short nodeType = child.getNodeType();
            if (nodeType == Node.TEXT_NODE) {
                child.setNodeValue(value);
                valueSet = true;
                break;
            }
        }
        if (!valueSet)
            element.appendChild(doc.createTextNode(value));
    }
    
    /**
     * Replaces the new value for the element text.
     * @param element
     * @param value
     */
    public void replaceValue(Element element, String value) {
        NodeList children = element.getChildNodes();
        int childCount = children.getLength();
        for (int i = 0; i < childCount; i++) {
            Node child = children.item(i);
            short nodeType = child.getNodeType();
            if (nodeType == Node.TEXT_NODE) {
                child.setTextContent(value);
                this.updated = true;
                break;
            }
        }
    }

    /**
     * Deletes a node.
     * @param deleteNode
     * @param parent
     * @return Element
     */
    public void deleteNode(Node deleteNode, Node parent) {
        parent.removeChild(deleteNode);
        this.updated = true;
    }

    /**
     * Adds a node.
     * @param parent
     * @param namespaceURI
     * @param prefix
     * @param nodeName
     * @return Element
     */
    public Element addNode(Element parent, String namespaceURI,
                           String prefix, String nodeName) {
        Element newNode;
        if (namespaceURI == null) {
            newNode = doc.createElement(nodeName);
        } else {
            newNode = doc.createElementNS(namespaceURI, nodeName);
            if (prefix != null)
                newNode.setPrefix(prefix);
        }
        parent.appendChild(newNode);
        this.updated = true;
        return newNode;
    }

    /**
     * Adds a node.
     * @param baseXPath
     * @param namespaceURI
     * @param prefix
     * @param nodeName
     * @return Element
     */
    public Element addNode(String baseXPath, String namespaceURI,
                           String prefix, String nodeName) {
        // If no absolute baseXPath is not given use //baseXPath to find the parameter
        if(baseXPath.charAt(0) != '/')
            baseXPath = "//" + baseXPath;
        else    //the JXPathContext expects 'params' (which is the variable name returned by XMLFile
            baseXPath = "params" + baseXPath;
        try {
            NodeList nodeList = (NodeList) xPath.evaluate(
                                baseXPath, doc, XPathConstants.NODESET);
            int length = nodeList.getLength();
            if (length == 0) {
                logger.warning("No match for XPath " + baseXPath);
                return null;
            } else if (length > 1) {
                logger.warning("XPath " + baseXPath +
                        " references more than one node. Please make the " +
                        "XPath more specific.");
                return null;
            }

            Node node = nodeList.item(0);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                logger.warning("XPath " + baseXPath +
                        " does not reference an element.");
                return null;
            }

            return addNode((Element) node, namespaceURI, prefix, nodeName);
            
        } catch (XPathExpressionException e) {
            throw new XMLException("Error evaluating " + baseXPath + ", " +
                                   e.getMessage(), e);
        }

    }

    /**
     * This method saves the XML file if it was modified
     * and if a back up file name is specified the original
     * file is backed up.
     * @param backupFileName  - the name of the backup file
     * @return Whether the save succeeded
     * @throws Exception Error saving the file
     */
    public boolean save(String backupFileName) throws Exception {

        // Check if a save is needed
        if(!updated)
            return false;

        //backup the file
        if(backupFileName != null) {
            BufferedReader in = new BufferedReader(new FileReader(file));
            BufferedWriter out = new BufferedWriter(new FileWriter(backupFileName));
            int ch;
            while((ch = in.read()) != -1)
                out.write(ch);
            in.close();
            out.close();
        }

        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        // transformer.getOutputProperties().list(System.out);

        if (doc.getDoctype() != null){
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doc.getDoctype().getPublicId());
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doc.getDoctype().getSystemId());
        }

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(file));
        transformer.transform(source, result);

        return true;
    }

    /**
     * Scans the XML for specific entries matching
     * "//fa:hostConfig/fa:hostPorts" and processes them for later retrieval.
     */
    public void processHostPorts() {
        String xPathExpr = "//fa:hostConfig/fa:hostPorts";
        hostPortsTable = new HashMap<Node, ArrayList<NameValuePair<Integer>>>();
        try {
            NodeList nodeList = (NodeList) xPath.evaluate(xPathExpr, doc,
                                                        XPathConstants.NODESET);
            int entries = nodeList.getLength();
            for (int i = 0; i < entries; i++) {
                processHostPorts(nodeList.item(i));
            }

        } catch (XPathExpressionException e) {
            logger.log(Level.WARNING, "Error processing XPath expression: " +
                                                                xPathExpr, e);
        }
    }

    private boolean isHostNode(Node hostsNode) {
        if (hostsNode.getNodeType() != Node.ELEMENT_NODE)
            return false;
        String nodeName = hostsNode.getNodeName();
        if (nodeName == null)
            return false;
        if (!nodeName.endsWith(":host"))
            return false;
        if (!ParamReader.FABANURI.equals(hostsNode.getNamespaceURI()))
            return false;
        return true;
    }

    private void processHostPorts(Node hostPortNode) {

        Node hostConfig = hostPortNode.getParentNode();
        Node firstChild = hostConfig.getFirstChild();
        Node hostsNode = firstChild;
        if (!isHostNode(hostsNode)) {
            hostsNode = null;
            NodeList hostConfigNodes = hostConfig.getChildNodes();
            int length = hostConfigNodes.getLength();
            for (int i = 1; i < length; i++) {
                Node pHost = hostConfigNodes.item(i);
                if (isHostNode(pHost)) {
                    hostsNode = pHost;
                    break;
                }
            }
        }

        LinkedHashSet<String> hostsSet = new LinkedHashSet<String>();

        if (hostsNode != null) {
            Node textNode = hostsNode.getFirstChild();
            if (textNode != null && textNode.getNodeType() == Node.TEXT_NODE) {
                String[] hostList = textNode.getNodeValue().split("\\s");
                for (String hostName : hostList) {
                    hostsSet.add(hostName);
                }
            }
        }

        Node clientsNode = hostPortNode.getFirstChild();
        String clients;
        if (clientsNode == null) {
            clients = "";
        } else {
            clients = clientsNode.getNodeValue();
            if (clients == null)
                clients = "";
        }

        ArrayList<NameValuePair<Integer>> hostsPorts =
                                    new ArrayList<NameValuePair<Integer>>();

        String hosts = Utilities.parseHostPorts(clients, hostsPorts, hostsSet);

        if (hostsNode == null) {
            hostsNode = doc.createElementNS(ParamReader.FABANURI, "host");
            hostsNode.setPrefix("fa");
            hostsNode.appendChild(doc.createTextNode(hosts.toString().trim()));
            hostConfig.insertBefore(hostsNode, firstChild);
        } else {
            Node textNode = hostsNode.getFirstChild();
            if (textNode != null)
                textNode.setNodeValue(hosts.toString().trim());
            else
                hostsNode.appendChild(doc.createTextNode(
                        hosts.toString().trim()));                        
        }
        hostPortsTable.put(hostPortNode, hostsPorts);
    }

    /**
     * Obtains the host:port name value pair list from the element
     * matching this XPath.
     * @param xPathExpr
     * @return The list of host:port elements, or null if the XPath does
     * not exist or does not point to a host:port node.
     */
    public List<NameValuePair<Integer>> getHostPorts(String xPathExpr) {
        ArrayList<NameValuePair<Integer>> hostsPorts = null;
        if(xPathExpr.charAt(0) != '/')
            xPathExpr = "//" + xPathExpr;
        else    //the JXPathContext expects 'params' which is the variable name returned by XMLFile
            xPathExpr = "params" + xPathExpr;

        try {
            Node hostPortNode = (Node) xPath.evaluate(xPathExpr,
                                                doc, XPathConstants.NODE);
            if (hostPortNode != null)
                hostsPorts = hostPortsTable.get(hostPortNode);

        } catch (XPathExpressionException e) {
            logger.log(Level.WARNING, "Error processing XPath expression: " +
                                                                xPathExpr, e);
        }
        return hostsPorts;
    }

    /**
     * Obtains the host:port pairs based on a role.
     * @param roleNode The node representing the role
     * @return The host:port pairs
     */
    public List<NameValuePair<Integer>> getHostPorts(Element roleNode) {
        ArrayList<NameValuePair<Integer>> hostsPorts = null;
        try {
            Node hostPortNode = (Node) xPath.evaluate(
                    "fa:hostConfig/fa:hostPorts", roleNode, XPathConstants.NODE);
            if (hostPortNode != null)
                hostsPorts = hostPortsTable.get(hostPortNode);
        } catch (XPathExpressionException e) {
            logger.log(Level.WARNING, "Error processing XPath expression: " +
                    "fa:hostConfig/fa:hostPorts", e);
        }
        return hostsPorts;
    }

    /**
     * Returns NodeList for a given xpath string.
     * @param xPathExpr
     * @return NodeList
     */
    public NodeList getNodeList(String xPathExpr) {
        NodeList nodes = null;
        if(xPathExpr.charAt(0) != '/')
            xPathExpr = "//" + xPathExpr;
        else    //the JXPathContext expects 'params' which is the variable name returned by XMLFile
            xPathExpr = "params" + xPathExpr;

        try {
            nodes = (NodeList) xPath.evaluate(xPathExpr,
                                                doc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            logger.log(Level.WARNING, "Error processing XPath expression: " +
                                                                xPathExpr, e);
        }
        return nodes;
    }

    /**
     * Runs some testing of the XML reader.
     * @param args The command line arguments
     * @throws Exception If there are errors with the test
     */
    public static void main(String[] args) throws Exception {
        if(args.length < 3)
            System.out.println("Usage : java XMLReader <XML File> <XPath exp>");

        XMLReader util = new XMLReader(args[0], true, false);
        util.processHostPorts();

        System.out.println("File : " + args[0]);
        System.out.println("XPath : " + args[1]);

        System.out.print("\ngetValues : " + args[1] + " = ");
        String[] values = (String[])util.getValues(args[1]).toArray(new String[1]);
        for(int j = 0; j < values.length; j++)
            System.out.print(values[j] + "\t");

        System.out.print("\ngetValue : " + args[1] + " = ");
        System.out.println(util.getValue(args[1]));

        System.out.print("\ngetBooleanValue : " + args[1] + " = ");
        System.out.println(Boolean.toString(Boolean.valueOf(util.getValue(args[1])).booleanValue()));

    }
}
