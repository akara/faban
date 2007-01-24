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
 * $Id: XMLReader.java,v 1.3 2007/01/24 02:35:03 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class XMLReader {

    private Document doc;
    private XPath xPath;
    private String file;
    private boolean updated = false;

    public XMLReader(String file) {
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

    public String getValue(String xpath) {
        // If no absolute xpath is not given use //xpath to find the parameter
        if(xpath.charAt(0) != '/')
            xpath = "//" + xpath;
        else    //the JXPathContext expects 'params' which is the variable name returned by XMLFile
            xpath = "params" + xpath;
        try {
            return xPath.evaluate(xpath, doc);
        } catch (XPathExpressionException e) {
            throw new XMLException("Error evaluating " + xpath + ", " +
                                   e.getMessage(), e);
        }
    }

    public List<String> getValues(String xpath) {
        // If no absolute xpath is not given use //xpath to find the parameter
        if(xpath.charAt(0) != '/')
            xpath = "//" + xpath;
        else    //the JXPathContext expects 'params' (which is the variable name returned by XMLFile
            xpath = "params" + xpath;

        try {
            NodeList nodeList = (NodeList) xPath.evaluate(
                                xpath, doc, XPathConstants.NODESET);
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

    public List<String> getAttributeValues(String xpath, String attribute) {
        if(xpath.charAt(0) != '/')
            xpath = "//" + xpath;
        else    //the JXPathContext expects 'params' (which is the variable name returned by XMLFile
            xpath = "params" + xpath;

        try {
            NodeList nodeList = (NodeList) xPath.evaluate(xpath + "[@" + attribute + "]",
                                doc, XPathConstants.NODESET);
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

    public void setValue(String xpath, String newValue) {
        // If no absolute xpath is not given use //xpath to find the parameter
        if(xpath.charAt(0) != '/')
            xpath = "//" + xpath;
        else    //the JXPathContext expects 'params' (which is the variable name returned by XMLFile
            xpath = "params" + xpath;

        try {
            NodeList nodeList = (NodeList) xPath.evaluate(
                                xpath, doc, XPathConstants.NODESET);
            int length = nodeList.getLength();
            for (int i = 0; i < length; i++) {
                Node node = nodeList.item(i);
                nodeTraceLoop:
                while (node != null)
                    switch (node.getNodeType()) {
                        case Node.ATTRIBUTE_NODE :
                        case Node.CDATA_SECTION_NODE :
                        case Node.COMMENT_NODE :
                        case Node.PROCESSING_INSTRUCTION_NODE :
                        case Node.TEXT_NODE : node.setNodeValue(newValue);
                                              updated = true;
                                              break nodeTraceLoop;
                        default : node = node.getFirstChild();
                    }
            }
        } catch (XPathExpressionException e) {
            throw new XMLException("Error evaluating " + xpath + ", " +
                                   e.getMessage(), e);
        }
    }

    /**
      * This method saves the XML file if it was modified
      * and if a back up file name is specified the original
      * file is backed up
      * @param backupFileName  - the name of the backup file
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


    public static void main(String[] args) throws Exception {
        if(args.length < 3)
            System.out.println("Usage : java XMLReader <XML File> <XPath exp>");

        XMLReader util = new XMLReader(args[0]);

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
