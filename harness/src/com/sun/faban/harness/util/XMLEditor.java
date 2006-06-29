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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: XMLEditor.java,v 1.1 2006/06/29 18:51:44 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.util;

import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.OutputKeys;

/**
 *
 * XMLEditor class can edit the XML files
 *
 * @author Ramesh Ramachandran
 */
public class XMLEditor {

    Document document;
    String file;
    boolean updated;
    String matchAttrName;
    String matchAttrValue;
    boolean match;

    public XMLEditor() {
        document = null;
        file = null;
        updated = false;

        // Needed for validating using external dtds
        System.setProperty("http.proxyHost", "webcache");
        System.setProperty("http.proxyPort", "8080");
    }

    /**
      * This method opens the XML file
      * @param xmlFileName  - the full pathname of the file
      */
    public boolean open(String xmlFileName) throws Exception {

        file = xmlFileName;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        File f = new File(file);
        if(f.isFile() && f.canRead() && f.canWrite()) {
            DocumentBuilder builder = factory.newDocumentBuilder();
            document = builder.parse(f);
            return true;
        }
        return false;
    }

    private void printType(Node node) {
        switch(node.getNodeType()) {
            case Node.ATTRIBUTE_NODE :
                System.out.println("Name " + node.getNodeName() + " type : ATTRIBUTE_NODE");
                break;

          case Node.CDATA_SECTION_NODE :
                System.out.println("Name " + node.getNodeName() + " type : CDATA_SECTION_NODE");
                break;
          case Node.COMMENT_NODE :
                System.out.println("Name " + node.getNodeName() + " type : COMMENT_NODE");
                break;
          case Node.DOCUMENT_FRAGMENT_NODE :
                System.out.println("Name " + node.getNodeName() + " type : DOCUMENT_FRAGMENT_NODE");
                break;
          case Node.DOCUMENT_NODE :
                System.out.println("Name " + node.getNodeName() + " type : DOCUMENT_NODE");
                break;
          case Node.DOCUMENT_TYPE_NODE :
                System.out.println("Name " + node.getNodeName() + " type : DOCUMENT_TYPE_NODE");
                break;
          case Node.ELEMENT_NODE :
                System.out.println("Name " + node.getNodeName() + " type : ELEMENT_NODE");
                break;
          case Node.ENTITY_NODE :
                System.out.println("Name " + node.getNodeName() + " type : ENTITY_NODE");
                break;
          case Node.ENTITY_REFERENCE_NODE :
                System.out.println("Name " + node.getNodeName() + " type : ENTITY_REFERENCE_NODE");
                break;
          case Node.NOTATION_NODE :
                System.out.println("Name " + node.getNodeName() + " type : NOTATION_NODE");
                break;
          case Node.PROCESSING_INSTRUCTION_NODE :
                System.out.println("Name " + node.getNodeName() + " type : PROCESSING_INSTRUCTION_NODE");
                break;
          case Node.TEXT_NODE :
                System.out.println("Name " + node.getNodeName() + " type : TEXT_NODE");
                break;
          default :
                System.out.println("Name " + node.getNodeName() + " type : Unknown");
                break;

       }
   }

    /**
      * This recursive method traverses through the XML and
      * finds the 'element' that contains text 'value' and
      * delete this node from its parent
      * @param current - the parent element for searching
      * @param elements  - a string in the following format
      *             element[\telement..]\tvalue
      */
    public boolean delete(Node current, String delim, String elements) {

        if(current == null)
            current = document;

        StringTokenizer st = new StringTokenizer(elements, delim);
        int count = st.countTokens();
        boolean deleted = false;

        if(count == 2) {
            String key = st.nextToken();
            String value = st.nextToken();
            System.out.println("delete: key=" + key + " value=" + value);

            // Try to replace child node's text
            if(current.hasChildNodes()) {
                NodeList childList = ((Element)current).getElementsByTagName(key);
                for(int i = 0; i < childList.getLength(); i++) {
                    Node child = childList.item(i);
                    NodeList textList = child.getChildNodes();
                    for(int j = 0; j < textList.getLength(); j++) {
                        Node text = textList.item(j);
                        if(text.getNodeType() == Node.TEXT_NODE) {
                            String data = ((Text)text).getData();
                            int index = data.indexOf(value);
                            if(index != -1) {
                                // If the value is at the start
                                if(index == 0) {
                                    if(value.length() == data.length()) {
                                        data = null;
                                    }
                                    else // there are some more props
                                        data = data.substring(value.length());
                                }
                                // the value is at the end
                                else if((index + value.length()) == data.length()) {
                                    data = data.substring(0, index - 1);
                                }
                                else // the value is in the middle
                                    data = data.substring(0, index - 1) + data.substring(index + value.length());

                                // Update the data now
                                if(data == null)
                                    current.removeChild(child);
                                else
                                    ((Text)text).setData(data);
                                deleted =  true;
                                updated = true;
 				                break;
                            }
                        }
                    }
                }
            }
        }
        else if( (count > 2) && current.hasChildNodes())  {
            String name = st.nextToken();
            String substr = elements.substring(elements.indexOf(name) + name.length()).trim();
            NodeList list = null;

            if(current.getNodeType() == Node.DOCUMENT_NODE)
                list = ((Document)current).getElementsByTagName(name);
            else
                list = ((Element)current).getElementsByTagName(name);

            for(int i = 0; i < list.getLength(); i++) {
                delete(list.item(i), delim, substr);
            }
        }
        return deleted;
    }

    /**
      * This method traverses through the XML and
      * finds the 'element' with an attribute name 'tag'
      * and replaces the value of this to 'value
      * @param current - the parent element for searching
      * @param elements  - a string in the following format
      *             element [element..] Atrribute value
      */
    public boolean replace(Node current, String delim, String elements) {
        return replaceRecur(current, delim, elements);
    }

    /**
      * This recursive method traverses through the XML and
      * finds the 'element' or an attribute
      * and replaces the text node or attribute to 'value'
      * @param current - the parent element for searching
      * @param elements  - a string in the following format
      *   \element\[element..]\[Atrribute]\value
      */
    private boolean replaceRecur(Node current, String delim, String elements) {

        if(current == null)
            current = document;

        System.out.println(elements);

        StringTokenizer st = new StringTokenizer(elements, delim);
        int count = st.countTokens();
        boolean replaced = false;

        if(count == 2) {
            String key = st.nextToken();
            String value = st.nextToken();

            System.out.println("Key = " + key + " Value  = " + value);

            // Try to see if its an attrib
            NamedNodeMap nodeMap = current.getAttributes();
            Node attrib = nodeMap.getNamedItem(key);
            if(attrib != null) {
                if(!attrib.getNodeValue().equals(value))
                {
                    attrib.setNodeValue(value);
                    updated = true;
                }
                replaced =  true;
            }
            // Try to replace child node's text
            else if(current.hasChildNodes()) {
                NodeList childList = ((Element)current).getElementsByTagName(key);
                for(int i = 0; i < childList.getLength(); i++) {
                    Node child = childList.item(i);
                    NodeList textList = child.getChildNodes();
                    for(int j = 0; j < textList.getLength(); j++) {
                        Node text = textList.item(j);
                        if(text.getNodeType() == Node.TEXT_NODE) {
                            System.out.println("Checking " + ((Text)text).getData() + " == " + value);
                            // check if it already exists
                            if(((Text)text).getData().equals(value))
                                replaced =  true;
                            else if (childList.getLength() == 1) {
                                ((Text)text).setData(value);
                                replaced =  true;
                                updated = true;
                            }
                            else if((((Text)text).getData().indexOf(value) != -1) ||
                                    (value.indexOf(((Text)text).getData()) != -1)) {
                                ((Text)text).setData(value);
                                replaced =  true;
                                updated = true;
                            }
                        }
                    }
                }
                if((!replaced) && (childList.getLength() > 0)) {
                    // We need to insert a new Text with Value
                    System.out.println("Adding a new text " + value);
                    Node n = childList.item(0).cloneNode(true);
                    ((Text)n.getFirstChild()).setData(value);
                    current.appendChild(n);
                    replaced =  true;
                    updated = true;
                }
            }
        }
        else if( (count > 2) && current.hasChildNodes())  {
            String name = st.nextToken();
            String substr = elements.substring(elements.indexOf(name) + name.length()).trim();
            NodeList list = null;

            if(current.getNodeType() == Node.DOCUMENT_NODE)
                list = ((Document)current).getElementsByTagName(name);
            else
                list = ((Element)current).getElementsByTagName(name);

            for(int i = 0; i < list.getLength(); i++) {
                replaced = replaced ||  replaceRecur(list.item(i), delim, substr);
            }
        }
        return replaced;
    }

    /**
     * This method traverses through the XML and
     * finds the 'element' with an attribute name 'tag'
     * and replaces the value of this to 'value'
     * if another attribute of the element matches its
     * value (defined in matchRule)
     * @param current - the parent element for searching
     * @param elements  - a string in the following format
     *             element [element..] Atrribute value
     * @param matchRule - do the replace if matchRule
     *              in the format of "key=value" matches
     */
    public boolean replace(Node current,  String delim, String matchRule, String elements) {
        boolean match = false;
        if (matchRule != null){
            StringTokenizer st = new StringTokenizer(matchRule, "=");
            if ( (st !=  null)
                    && ((matchAttrName = st.nextToken()) != null)
                    && (matchAttrValue = st.nextToken()) != null)
                match = true;
        }
        if (match)
            return replaceMatchRecur(current, delim, elements);
        else
            return replaceRecur(current, delim, elements);
    }

    /**
      * This recursive method traverses through the XML and
      * finds the 'element' with an attribute name 'tag'
      * and replaces the value of this to 'value
      * @param current - the parent element for searching
      * @param elements  - a string in the following format
      *             element [element..] Atrribute value
      */
    private boolean replaceMatchRecur(Node current,  String delim, String elements) {

        if(current == null)
            current = document;

        StringTokenizer st = new StringTokenizer(elements, delim);
        int count = st.countTokens();
        boolean replaced = false;

        if(count == 2) {
            String key = st.nextToken();
            String value = st.nextToken();

            // Try to see if its an attrib
            NamedNodeMap nodeMap = current.getAttributes();
            Node attrib = nodeMap.getNamedItem(key);
            if(attrib != null) {
                Node matchAttrNode = nodeMap.getNamedItem(matchAttrName);
                if ( matchAttrNode != null
                        && matchAttrNode.getNodeValue().equals(matchAttrValue) ) {
                    if (!attrib.getNodeValue().equals(value))
                    {
                        attrib.setNodeValue(value);
                        updated = true;
                    }
                    replaced = true;
                }
            }
            // Try to replace child node's text
            else if(current.hasChildNodes()) {
                NodeList childListMatch = ((Element)current).getElementsByTagName(matchAttrName);
                NodeList childList = ((Element)current).getElementsByTagName(key);
                if (childListMatch != null && childList != null){

                    boolean matched = false;
                    for (int i = 0; i < childListMatch.getLength() && !matched; i++) {
                        Node child = childListMatch.item(i);
                        NodeList textList = child.getChildNodes();
                        for(int j = 0; j < textList.getLength(); j++) {
                            Node text = textList.item(j);
                            if(text.getNodeType() != Node.TEXT_NODE)
                                continue;
                            if(((Text)text).getData().equals(matchAttrValue)) {
                                matched = true;
                                break;
                            }
                        }
                    }
                    if (matched) {

                        for(int i = 0; i < childList.getLength(); i++) {
                            Node child = childList.item(i);
                            NodeList textList = child.getChildNodes();
                            for(int j = 0; j < textList.getLength(); j++) {
                                Node text = textList.item(j);
                                if(text.getNodeType() == Node.TEXT_NODE) {
                                    // System.out.println("Checking " + ((Text)text).getData() + " == " + value);
                                    // check if it already exists
                                    if(((Text)text).getData().equals(value))
                                        replaced =  true;
                                    else if (childList.getLength() == 1) {
                                        ((Text)text).setData(value);
                                        replaced =  true;
                                        updated = true;
                                    }
                                    else if((((Text)text).getData().indexOf(value) != -1) ||
                                            (value.indexOf(((Text)text).getData()) != -1)) {
                                        ((Text)text).setData(value);
                                        replaced =  true;
                                        updated = true;
                                    }
                                }
                            }
                        }
                        if((!replaced) && (childList.getLength() > 0)) {
                            // We need to insert a new Text with Value
                            System.out.println("Adding a new text " + value);
                            Node n = childList.item(0).cloneNode(true);
                            ((Text)n.getFirstChild()).setData(value);
                            current.appendChild(n);
                            replaced =  true;
                            updated = true;
                        }
                    }
                }
            }
        }
        else if( (count > 2) && current.hasChildNodes())  {
            String name = st.nextToken();
            String substr = elements.substring(elements.indexOf(name) + name.length()).trim();
            NodeList list = null;

            if(current.getNodeType() == Node.DOCUMENT_NODE)
                list = ((Document)current).getElementsByTagName(name);
            else
                list = ((Element)current).getElementsByTagName(name);

            for(int i = 0; i < list.getLength(); i++) {
                replaced = replaced ||  replaceMatchRecur(list.item(i), delim, substr);
            }
        }
        return replaced;
    }

    /**
     * This method traverses through the XML and
     * finds all the elements that has the 'path'.
     * Based on the size of the properties array
     * remove some of the elements or append more elements
     * to current node so that the number of elements
     * matches the size of the properties array.
     * Then use properties props[i] to replace
     * attribute and/or child node text of elements[i]
     * @param current - the parent element for searching
     * @param path  - a string in the following format
     *             element [\telement..]
     * @param props - properties array
     *              format: "[element\t...]key", "value"
     */

    public void dupAndReplace(Node current,  String delim, String path, Properties[] props) {


        if(current == null)
            current = document;

        StringTokenizer st = new StringTokenizer(path, delim);
        int count = st.countTokens();
        // System.out.println("count = " + count);

        if (count > 1 && current.hasChildNodes()) {
            String name = st.nextToken();
            String substr = path.substring(path.indexOf(name) + name.length()).trim();
            // System.out.println("name = " + name + ", next = " + substr);

            NodeList list = null;

            if(current.getNodeType() == Node.DOCUMENT_NODE)
                list = ((Document)current).getElementsByTagName(name);
            else
                list = ((Element)current).getElementsByTagName(name);

            for(int i = 0; i < list.getLength(); i++) {
                dupAndReplace(list.item(i), delim, substr, props);
            }

        } else if (current.hasChildNodes()){
            // "current" is the paranet to the node to be duplicated or replaced

            String name = st.nextToken();
            NodeList list = null;
            if(current.getNodeType() == Node.DOCUMENT_NODE)
                list = ((Document)current).getElementsByTagName(name);
            else
                list = ((Element)current).getElementsByTagName(name);

            int numNodes = list.getLength();
            int numProps = props.length;

            // System.out.println("numNodes = " + numNodes + "numProps = " + numProps);
            Node nodeNextSibling = list.item(numNodes-1).getNextSibling();

            for (int i = numProps; i < numNodes; i++) {
                current.removeChild(list.item(i));
                updated = true;
            }
            for (int i = numNodes; i < numProps; i++) {
                Node node = list.item(0).cloneNode(true);
                if (nodeNextSibling == null) current.appendChild(node);
                else current.insertBefore(node, nodeNextSibling);
                updated = true;
            }
            // now there should be the same number of nodes and props
            // get the updated list again

            if(current.getNodeType() == Node.DOCUMENT_NODE)
                list = ((Document)current).getElementsByTagName(name);
            else
                list = ((Element)current).getElementsByTagName(name);

            // System.out.println("listNum = " + list.getLength());
            for(int i = 0; i < numProps; i++) {

                Enumeration keys = props[i].keys();
                while (keys.hasMoreElements()) {
                    String key = (String) keys.nextElement();
                    String value = props[i].getProperty(key);
                    // System.out.println("i = " + i + "key = " + key + "value = " + value);
                    replaceRecur(list.item(i), delim, key+"\t"+value);
                }

            }

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

        if (document.getDoctype() != null){
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, document.getDoctype().getPublicId());
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, document.getDoctype().getSystemId());
        }

        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(new File(file));
        transformer.transform(source, result);

        return true;
    }

        /**
     * This method traverses through the XML and
     * finds the 'element' with an attribute name 'tag'
     * and replaces the value of this to 'value'
     * if another attribute of the element matches its
     * value (defined in matchRule)
     * @param current - the parent element for searching
     * @param elements  - a string in the following format
     *             element [element..] Atrribute value
     * @param matchRule - do the replace if matchRule
     *              in the format of "key=value" matches
     */
    public String find(Node current,  String delim, String matchRule, String elements) {

        if(current == null)
            current = document;

        boolean match = false;
        if (matchRule != null){
            StringTokenizer st = new StringTokenizer(matchRule, "=");
            if ( (st !=  null)
                    && ((matchAttrName = st.nextToken()) != null)
                    && (matchAttrValue = st.nextToken()) != null)
                match = true;
        }
        if (match)
            return findMatchRecur(current, delim, elements);
        else
            return findRecur(current, delim, elements);
    }

    /**
      * This recursive method traverses through the XML and
      * finds the 'element' with an attribute name 'tag'
      * and replaces the value of this to 'value
      * @param current - the parent element for searching
      * @param elements  - a string in the following format
      *             element [element..] Atrribute value
      */
    private String findRecur(Node current, String delim, String elements) {
        String retS = null;

        if(current == null)
            current = document;

        StringTokenizer st = new StringTokenizer(elements, "\t");
        int count = st.countTokens();

        if(count == 1) {
            String key = st.nextToken();

            // Try to see if its an attrib
            NamedNodeMap nodeMap = current.getAttributes();
            Node attrib = nodeMap.getNamedItem(key);
            if(attrib != null) {
                retS = attrib.getNodeValue();
            }
            // Try to search child node's text
            else if(current.hasChildNodes()) {
                NodeList childList = ((Element)current).getElementsByTagName(key);
                for(int i = 0; i < childList.getLength(); i++) {
                    Node child = childList.item(i);
                    NodeList textList = child.getChildNodes();
                    for(int j = 0; j < textList.getLength(); j++) {
                        Node text = textList.item(j);
                        if(text.getNodeType() == Node.TEXT_NODE) {
                            // get the first text node on the list
                            retS = ((Text)text).getData();
                        }
                    }
                }
            }
        }
        else if( (count > 1) && current.hasChildNodes())  {
            String name = st.nextToken();
            String substr = elements.substring(elements.indexOf(name) + name.length()).trim();
            NodeList list = null;

            if(current.getNodeType() == Node.DOCUMENT_NODE)
                list = ((Document)current).getElementsByTagName(name);
            else
                list = ((Element)current).getElementsByTagName(name);

            for(int i = 0; i < list.getLength(); i++) {
                retS = findRecur(list.item(i), delim, substr);  // find first occurrence
                if (retS != null)
                    break;
            }
        }
        // all other cases
        return retS;
    }


    /**
      * This recursive method traverses through the XML and
      * finds the 'element' with an attribute name 'tag'
      * @param current - the parent element for searching
      * @param elements  - a string in the following format
      *             element [element..] Atrribute value
      */
    private String findMatchRecur(Node current, String delim, String elements) {

        String retS = null;

        if(current == null)
            current = document;

        StringTokenizer st = new StringTokenizer(elements, delim);
        int count = st.countTokens();

        if(count == 1) {
            String key = st.nextToken();

            // Try to see if its an attrib
            NamedNodeMap nodeMap = current.getAttributes();
            Node attrib = nodeMap.getNamedItem(key);
            if(attrib != null) {
                Node matchAttrNode = nodeMap.getNamedItem(matchAttrName);
                if ( matchAttrNode != null
                        && matchAttrNode.getNodeValue().equals(matchAttrValue) ) {
                    retS = attrib.getNodeValue();
                }
            }
            // search in child node's text
            else if(current.hasChildNodes()) {
                NodeList childList = ((Element)current).getElementsByTagName(key);
                for(int i = 0; i < childList.getLength(); i++) {
                    Node child = childList.item(i);
                    NodeList textList = child.getChildNodes();
                    for(int j = 0; j < textList.getLength(); j++) {
                        Node text = textList.item(j);
                        if(text.getNodeType() == Node.TEXT_NODE) {
                            // System.out.println("Checking " + ((Text)text).getData() + " == " + value);
                            // check if it already exists
                            retS = ((Text)text).getData();
                        }
                    }
                }
            }
        }
        else if( (count > 1) && current.hasChildNodes())  {
            String name = st.nextToken();
            String substr = elements.substring(elements.indexOf(name) + name.length()).trim();
            NodeList list = null;

            if(current.getNodeType() == Node.DOCUMENT_NODE)
                list = ((Document)current).getElementsByTagName(name);
            else
                list = ((Element)current).getElementsByTagName(name);

            for(int i = 0; i < list.getLength(); i++) {
                retS = findMatchRecur(list.item(i), delim, substr);
                if (retS != null)
                    break;
            }
        }
        return retS;
    }


    // Unit test the functionality
    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: java XMLEditor -[d][r] File element<delim>[element...][<delim>attribute]<delim>value <delim>");
            return;
        }

        XMLEditor edit = new XMLEditor();
        System.out.println("opening " + args[1]);

        edit.open(args[1]);

        if(args[0].indexOf("d") != -1) {
            edit.delete(null, args[3], args[2]);
            System.out.println("Deleting " + args[2]);
        }
        else {
            edit.replace(null, args[3], args[2]);
            System.out.println("Replacing " + args[2]);
        }

        edit.save(args[0] + ".old");
    }
}
