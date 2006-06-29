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
 * $Id: CmdMap.java,v 1.2 2006/06/29 19:38:43 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.util;

import com.sun.faban.harness.common.Config;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * Reads/parses the command mapping and modifies the exec map with the command
 * map.
 *
 * @author Akara Sucharitakul
 */
public class CmdMap {

    /**
     * Reads the command map file and adds/modifies the exec map accordingly.
     * @param binMap
     * @throws Exception
     */
    public static void addTo(Map<String, String> binMap) throws Exception {
        ArrayList<CmdDetail> cmdList = new ArrayList<CmdDetail>();

        FileInputStream is = new FileInputStream(Config.CONFIG_DIR +
                                 System.getProperty("os.name") +
                                 File.separator + "cmdmap.xml");
        parseStream(is, cmdList);

        // Next step is to use the map command path and prefixes to the
        // actual executable.
        for (Iterator<CmdDetail> iter = cmdList.iterator();
             iter.hasNext();) {
            CmdDetail c = iter.next();
            if (c.path == null)
                c.path = binMap.get(c.name);
            if (c.path == null)
                c.path = c.name;
            for (int i = 0; i < c.prefix.length; i++) {
                int spIdx = c.prefix[i].indexOf(' ');
                String cmd;
                if (spIdx == -1) // No prefix args?
                    cmd = c.prefix[i]; // take the whole
                else
                    cmd = c.prefix[i].substring(0, spIdx); // take command only
                // Lookup the actual path from the binMap
                String path = binMap.get(cmd);
                if (path == null)
                    path = cmd;
                if (spIdx == -1) // No prefix args?
                    c.prefix[i] = path; // Set the whole
                else
                    c.prefix[i] = path + c.prefix[i].substring(spIdx);
            }
        }

        StringBuilder exec = new StringBuilder();
        for (Iterator<CmdDetail> iter = cmdList.iterator();
             iter.hasNext();) {
            CmdDetail c = iter.next();
            for (int i = 0; i < c.prefix.length; i++) {
                exec.append(c.prefix[i]);
                exec.append(' ');
            }
            exec.append(c.path);
            binMap.put(c.name, exec.toString());
            exec.setLength(0);
        }
    }

    private static void parseStream(FileInputStream is,
                                    ArrayList<CmdDetail> cmdList)
            throws Exception {
        SAXParserFactory sFact = SAXParserFactory.newInstance();
        sFact.setFeature("http://xml.org/sax/features/validation", false);
        sFact.setFeature("http://apache.org/xml/features/" +
                "allow-java-encodings", true);
        sFact.setFeature("http://apache.org/xml/features/nonvalidating/" +
                "load-dtd-grammar", false);
        sFact.setFeature("http://apache.org/xml/features/nonvalidating/" +
                "load-external-dtd", false);
        SAXParser parser = sFact.newSAXParser();
        parser.parse(is, new MapReaderHandler(cmdList));
    }

    static class CmdDetail {
        String name;
        String path;
        String[] prefix;

        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(name);
            b.append(':');
            for (int i = 0; i < prefix.length; i++) {
                b.append(prefix[i]);
                b.append(" ");
            }
            b.append(path);
            return b.toString();
        }
    }

    static class MapReaderHandler extends DefaultHandler {
        private CmdDetail currentCmd;
        private ArrayList stack = new ArrayList();
        private StringBuilder buffer = new StringBuilder();
        private TreeMap<Integer, String> prefixes =
                new TreeMap<Integer, String>();
        private int currentSequence = Integer.MIN_VALUE;
        private ArrayList<CmdDetail> cmdList;

        MapReaderHandler(ArrayList<CmdDetail> cmdList) {
            this.cmdList = cmdList;
        }

        /**
         * Receive notification of the start of an element.
         * <p/>
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the start of
         * each element (such as allocating a new tree node or writing
         * output to a file).</p>
         *
         * @param uri        The Namespace URI, or the empty string if the
         *                   element has no Namespace URI or if Namespace
         *                   processing is not being performed.
         * @param localName  The local name (without prefix), or the
         *                   empty string if Namespace processing is not being
         *                   performed.
         * @param qName      The qualified name (with prefix), or the
         *                   empty string if qualified names are not available.
         * @param attributes The attributes attached to the element.  If
         *                   there are no attributes, it shall be an empty
         *                   Attributes object.
         * @throws org.xml.sax.SAXException Any SAX exception, possibly
         *                                  wrapping another exception.
         * @see org.xml.sax.ContentHandler#startElement
         */
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            stack.add(qName);
            if ("command".equals(qName)) {
                currentCmd = new CmdDetail();
            } else if ("prefix".equals(qName)) {
                String s = attributes.getValue("sequence");
                if (s != null) {
                    currentSequence = Integer.parseInt(s);
                    if (currentSequence < 0)
                        throw new SAXException(
                                "Prefix sequence must be 0 or greater");
                }
            }
        }

        /**
         * Receive notification of the end of an element.
         * <p/>
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the end of
         * each element (such as finalising a tree node or writing
         * output to a file).</p>
         *
         * @param uri       The Namespace URI, or the empty string if the
         *                  element has no Namespace URI or if Namespace
         *                  processing is not being performed.
         * @param localName The local name (without prefix), or the
         *                  empty string if Namespace processing is not being
         *                  performed.
         * @param qName     The qualified name (with prefix), or the
         *                  empty string if qualified names are not available.
         * @throws org.xml.sax.SAXException Any SAX exception, possibly
         *                                  wrapping another exception.
         * @see org.xml.sax.ContentHandler#endElement
         */
        public void endElement(String uri, String localName, String qName)
                throws SAXException {

            int depth = stack.size();

            if (!stack.remove(depth - 1).equals(qName))
                throw new SAXException("endElement mismatch: " + qName);

            if ("command".equals(qName)) {
                if (currentCmd.name == null)
                    throw new SAXException("Command name not specified");
                // Get all the prefixes
                int prefixSize = prefixes.size();
                if (prefixSize > 0) {
                    currentCmd.prefix = new String[prefixSize];
                    Iterator<Map.Entry<Integer, String>> iter =
                            prefixes.entrySet().iterator();
                    for (int i = 0; i < prefixSize; i++) {
                        currentCmd.prefix[i] = iter.next().getValue();
                    }
                }
                prefixes.clear();
                cmdList.add(currentCmd);

            } else if ("name".equals(qName)) {
                currentCmd.name = buffer.toString().trim();
            } else if ("path".equals(qName)) {
                currentCmd.path = buffer.toString().trim();
            } else if ("prefix".equals(qName)) {
                if (currentSequence < 0)
                    currentSequence = -1;
                String oldPrefix =
                        prefixes.put(currentSequence, buffer.toString().trim());
                if (oldPrefix != null) {
                    if (currentSequence == -1)
                        throw new SAXException("Need to specify prefix " +
                                "sequence for more than one prefix");
                    else
                        throw new SAXException("Duplicate prefix sequence " +
                                "found");
                }
                currentSequence = Integer.MIN_VALUE;
            }
            buffer.setLength(0);
        }

        /**
         * Receive notification of character data inside an element.
         * <p/>
         * <p>By default, do nothing.  Application writers may override this
         * method to take specific actions for each chunk of character data
         * (such as adding the data to a node or buffer, or printing it to
         * a file).</p>
         *
         * @param ch     The characters.
         * @param start  The start position in the character array.
         * @param length The number of characters to use from the
         *               character array.
         * @see org.xml.sax.ContentHandler#characters
         */
        public void characters(char ch[], int start, int length) {
            buffer.append(ch, start, length);
        }
    }

    public static void main(String[] args) throws Exception {
        ArrayList<CmdDetail> cmdList = new ArrayList<CmdDetail>();
        HashMap<String, String> binMap = new HashMap<String, String>();
        FileInputStream is = new FileInputStream("cmdmap.xml");
        parseStream(is, cmdList);

        // Next step is to use the map command path and prefixes to the
        // actual executable.
        for (Iterator<CmdDetail> iter = cmdList.iterator();
             iter.hasNext();) {
            CmdDetail c = iter.next();
            if (c.path == null)
                c.path = binMap.get(c.name);
            if (c.path == null)
                c.path = c.name;

            // Try to map path back to the binMap
            String path = c.path;

            // Separate out args
            int pathEndIdx = path.indexOf(' ');
            if (pathEndIdx > 0)
                path = path.substring(0, pathEndIdx);

            // Search cmd for path separators ('/')
            int pathSepIdx = path.indexOf(File.separator);

            // If not found, still a relative path
            if (pathSepIdx < 0) {
                // Map it
                path = binMap.get(path);
                // And combine back with args
                if (path != null) {
                    if (pathEndIdx > 0)
                        c.path = path + path.substring(pathEndIdx);
                    else
                        c.path = path;
                }
            }

            for (int i = 0; i < c.prefix.length; i++) {
                int spIdx = c.prefix[i].indexOf(' ');
                String cmd;
                if (spIdx == -1) // No prefix args?
                    cmd = c.prefix[i]; // take the whole
                else
                    cmd = c.prefix[i].substring(0, spIdx); // take command only
                // Lookup the actual path from the binMap
                path = binMap.get(cmd);
                if (path == null)
                    path = cmd;
                if (spIdx == -1) // No prefix args?
                    c.prefix[i] = path; // Set the whole
                else
                    c.prefix[i] = path + c.prefix[i].substring(spIdx);
            }
        }

        for (Iterator<CmdDetail> iter = cmdList.iterator();
             iter.hasNext();) {
             CmdDetail c = iter.next();
             System.out.println(c.toString());
        }
    }
}
