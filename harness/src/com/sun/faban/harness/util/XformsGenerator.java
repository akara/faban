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
* Copyright 2005 Sun Microsystems Inc. All Rights Reserved
*/

package com.sun.faban.harness.util;

import com.sun.faban.common.FabanNamespaceContext;
import com.sun.faban.harness.common.Config;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Sheetal Patil
 */

public class XformsGenerator {

    static Document doc;
    static int idCount = 0;
    static boolean hasMoreElements = false;
    static ArrayList idStack = new ArrayList();
    static ArrayList labelsStack = new ArrayList();
    static StringBuffer xformsBindBuffer;
    static StringBuffer xformsLabelsBuffer;
    static StringBuffer xformsTriggersBuffer;
    static StringBuffer xformsCasesBuffer;
    static FabanNamespaceContext ns = new FabanNamespaceContext();
    static HashMap<String, String> map = new HashMap<String, String>();

    public static void generate(File infile, File outfile, File templateFile) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder docBuilder = factory.newDocumentBuilder();
            doc = docBuilder.parse(infile);
            doc.getDocumentElement().normalize ();
            loadMap();
            startDocument();
            FileHelper.copyFile(templateFile.getAbsolutePath(), outfile.getAbsolutePath(),false);
            endDocument(outfile);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void loadMap() {
        map.put("scale", "positiveInteger");
        map.put("rampup", "positiveInteger");
        map.put("steadystate", "positiveInteger");
        map.put("rampdown", "positiveInteger");
        map.put("delay", "positiveInteger");
        map.put("statsinterval", "positiveInteger");
        map.put("port", "positiveInteger");
    }

    private static void startDocument() {
        String id = "";
        int spaces = 0;
        buildXformsBind(doc.getDocumentElement(),spaces,id);
        buildXformsLabels(doc.getDocumentElement());
    }

    private static String indent(int spaces) {
        StringBuffer buffer = new StringBuffer();
         for (int i = 0; i < spaces; i++) {
             buffer.append("\t");
         }
         return buffer.toString();
     }

    private static void buildXformsBind(Node eNode, int spaces, String id) {
        String tab = indent(spaces);
        Node prevNode = null;
        String strg = eNode.getLocalName();
        String binds = strg;
        String inputs = strg;
        String nsStr = null;
        String attr = null;
        if(doc.getDocumentElement() == eNode){
            binds = tab + "<xforms:bind id='bind-"+ strg +"' xforms:nodeset='/" + strg + "'>\n";
        }else{
            String s = null;
            if(id.lastIndexOf("-") > 0)
                s = id.substring(id.lastIndexOf("-")+1, id.length());
            s = map.get(s);
            if (s != null)
                s = "xforms:type='"+s+"'";
            else
                s = " ";
            nsStr = eNode.getNamespaceURI();
            NamedNodeMap attrList = eNode.getAttributes();
            for (int i = 0; i < attrList.getLength(); i++) {
                if (attrList.item(i).getNodeType() == Node.ATTRIBUTE_NODE) {
                    if(attrList.item(i).getNodeName().equals("name"))
                        attr = attrList.item(i).getNodeValue();
                }
            }
            if (nsStr != null) {
                nsStr = ns.getPrefix(nsStr);
                if (attr != null) {
                    binds = tab + "<xforms:bind id='bind-" + id + "' xforms:nodeset='" + nsStr + ":" + strg + "[@name=" + '"' + attr + '"' + "]' " + s + ">\n";
                } else {
                    binds = tab + "<xforms:bind id='bind-" + id + "' xforms:nodeset='" + nsStr + ":" + strg + "' " + s + ">\n";
                }
            } else {
                if (attr != null) {
                    binds = tab + "<xforms:bind id='bind-" + id + "' xforms:nodeset='" + strg + "[@name=" + '"' + attr + '"' + "]' " + s + ">\n";
                } else {
                    binds = tab + "<xforms:bind id='bind-" + id + "' xforms:nodeset='" + strg + "' " + s + ">\n";
                }
            }

        }
        if (xformsBindBuffer == null) {
            xformsBindBuffer = new StringBuffer(binds);
        } else {
            xformsBindBuffer.append(binds);
        }

        if(eNode == doc.getDocumentElement()){
            inputs = "";
        }else if(eNode != doc.getDocumentElement() && eNode.getParentNode() == doc.getDocumentElement()){
            inputs = tab + "<xforms:case id='case-" + strg + "'>" + "\n" +
                     tab + tab +"<xforms:group id='group-" + strg + "'>" + "\n";
        }else {
            if (hasMoreElements(eNode)) {
                inputs = "\n";
            }else{
                if (eNode.getNodeName().equalsIgnoreCase("description")) {
                    inputs = tab + "<xforms:textarea id='input-" + id + "' xforms:bind='bind-" + id + "'>" + "\n" +
                            "\t\t\t<xforms:label xforms:model='benchmark-labels' xforms:ref='/labels/" + strg + "'/>" + "\n" +
                            "\t\t</xforms:textarea>\n";
                } else {
                    inputs = tab + "<xforms:input id='input-" + id + "' xforms:bind='bind-" + id + "'>" + "\n" +
                            "\t\t\t<xforms:label xforms:model='benchmark-labels' xforms:ref='/labels/" + strg + "'/>" + "\n" +
                            "\t\t</xforms:input>\n";
                }
            }
        }
        if (xformsCasesBuffer == null) {
            xformsCasesBuffer = new StringBuffer(inputs);
        } else {
            xformsCasesBuffer.append(inputs);
        }
        NodeList list = eNode.getChildNodes();
        for(int i=0; i<list.getLength(); i++){
            if(list.item(i).getNodeType() == Node.ELEMENT_NODE){
                spaces++;
                int j=i;
                while(j>0){
                    if(list.item(j).getPreviousSibling().getNodeType() == Node.ELEMENT_NODE){
                            prevNode = list.item(j);
                            break;
                    }
                    j--;
                }
                String idStr = list.item(i).getLocalName();
                if(prevNode != null){
                    int index = id.lastIndexOf("-");
                    String targetStr = id.substring(index+1, id.length());
                    id = id.replace(targetStr,idStr);
                }else{
                    if(!"".equals(id))
                        id = id + "-" + idStr;
                    else
                        id = idStr;
                }
                if (idStack.size() > 0 && idStack.contains(id)) {
                        for (int x = 0; x < idStack.size(); x++) {
                            if (idStack.get(x).equals(id)) {
                                int cnt = idCount++;
                                id = id + "_" + cnt;
                            }
                        }
                }
                idStack.add(id);
                buildXformsBind(list.item(i),spaces,id);
                spaces--;
            }
        }
        xformsBindBuffer.append(tab + "</xforms:bind>\n");
        if(eNode == doc.getDocumentElement()){
            // do nothing
        }else if(eNode != doc.getDocumentElement() && eNode.getParentNode() == doc.getDocumentElement()){
            xformsCasesBuffer.append(tab + tab + "</xforms:group>\n" +
                                    tab + "</xforms:case>\n");
        }else if (hasMoreElements(eNode)) {
            xformsCasesBuffer.append("\n");
        }
        spaces--;
    }

    private static void buildXformsLabels(Node eNode) {
        String s = eNode.getNodeName();
        if(s.lastIndexOf(":") > 0)
            s = s.substring(s.lastIndexOf(":")+1, s.length());
        if(!labelsStack.contains(s)) {
            labelsStack.add(s);
            s = "<"+s+">"+ makeLabel(s) +"</"+s+">" + "\n";
            if (xformsLabelsBuffer == null) {
                xformsLabelsBuffer = new StringBuffer(s);
            } else {
                xformsLabelsBuffer.append(s);
            }
        }
        NodeList list = eNode.getChildNodes();
        for(int i=0; i<list.getLength(); i++){
            if(list.item(i).getNodeType() == Node.ELEMENT_NODE){
                buildXformsLabels(list.item(i));
                if(list.item(i).getParentNode() == doc.getDocumentElement())
                    buildXformsTriggers(list.item(i).getLocalName());
            }
        }
    }

    private static void buildXformsTriggers(String s) {
        String trigger = "<xforms:trigger id='trigger-"+s+"'>" + "\n" +
                "\t<xforms:label xforms:model='benchmark-labels' xforms:ref='/labels/"+s+"'/>" + "\n" +
                "\t<xforms:action id='action-"+s+"'>" + "\n" +
                    "\t\t<xforms:revalidate xforms:model='benchmark-model' id='revalidate-"+s+"'/>" + "\n" +
                    "\t\t<xforms:toggle id='toggle-"+s+"' xforms:case='case-"+s+"'/>" + "\n" +
                "\t</xforms:action>" + "\n" +
            "</xforms:trigger>" + "\n";
        if (xformsTriggersBuffer == null) {
            xformsTriggersBuffer = new StringBuffer(trigger);
        } else {
            xformsTriggersBuffer.append(trigger);
        }
    }

    private static boolean hasMoreElements(Node node) {
        boolean hasNodes = false;
        NodeList list = node.getChildNodes();
        for(int i=0; i<list.getLength(); i++){
            if(list.item(i).getNodeType() == Node.ELEMENT_NODE){
                hasNodes = true;
                break;
            }
        }
        return hasNodes;
    }
    public static String makeLabel(String s) {
        int cnt = 0, j=0;
        ArrayList str = new ArrayList();
        for (int i = 0; i < s.length(); i++) {
            for (char c = 'A'; c <= 'Z'; c++) {
                if (s.charAt(i) == c) {
                    str.add(s.substring(cnt,i));
                    cnt = i;
                    j++;
                }
            }
        }
        str.add(s.substring(cnt,s.length()));
        String newStr = (String) str.get(0);
        if(str != null){
            for(int i = 1; i < str.size(); i++){
                newStr = newStr + " " + str.get(i);
            }
            s = newStr;
        }
        return (s.length()>0)? Character.toUpperCase(s.charAt(0))+s.substring(1) :s;
    }

    private static void endDocument(File outfile) {
        FileHelper.tokenReplace(outfile.getAbsolutePath(), "@binds@", xformsBindBuffer.toString(), null);
        FileHelper.tokenReplace(outfile.getAbsolutePath(), "@labels@", xformsLabelsBuffer.toString(), null);
        FileHelper.tokenReplace(outfile.getAbsolutePath(), "@triggers@", xformsTriggersBuffer.toString(), null);
        FileHelper.tokenReplace(outfile.getAbsolutePath(), "@cases@", xformsCasesBuffer.toString(), null);
    }

    public static void main(String[] args){
        File infile = new File(args[0]);
        File outfile = new File(args[1]);
        File templateFile = new File(args[2] + "/resources/config-template.xhtml");
        generate(infile,outfile,templateFile);
    }

}
