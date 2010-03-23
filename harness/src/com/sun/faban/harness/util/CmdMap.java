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

import com.sun.faban.harness.common.Config;
import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Checks the binary paths and the command mapping file and creates the exec
 * map.
 *
 * @author Akara Sucharitakul
 */
public class CmdMap {

    static Logger logger = Logger.getLogger(CmdMap.class.getName());

    /**
     * Scans the bin directories and command map file and returns the command
     * map.
     * @param benchName The name of the benchmark,
     *                  null if the map is not benchmark-specific
     * @return The command map
     * @throws Exception Something went wrong obtaining the command map.
     */
    public static HashMap<String, List<String>> getCmdMap(String benchName)
            throws Exception {
        HashMap<String, List<String>> binMap =
                                        new HashMap<String, List<String>>();

        // The platform-specific and benchmark-specific binaries
        // take precedence, add last to map.
        File binDir = new File(Config.FABAN_HOME + "bin"); // $FABAN_HOME/bin
        addExecMap(binDir, binMap, null);

        logger.finer("Scanning OS-specific binaries in " + Config.OS_DIR);
        File sbinDir = new File(binDir, Config.OS_DIR); // $FABAN_HOME/bin/SunOS
        addExecMap(sbinDir, binMap, null);

        logger.finer("Scanning architecture-specific binaries in " +
                     Config.ARCH_DIR);
        sbinDir = new File(binDir, Config.ARCH_DIR); // $FABAN_HOME/bin/SunOS/sparc
        addExecMap(sbinDir, binMap, null);

        if (benchName != null)
            addResourceMap(binMap, Config.BENCHMARK_DIR, benchName);

        mapPathExt(binMap);

        addCmdMapFile(binMap);

        // Dump the binMap for debugging
        if (logger.isLoggable(Level.FINER)) {
            StringBuilder b = new StringBuilder("Executable map:\n");
            for (Map.Entry<String, List<String>> entry : binMap.entrySet()) {
                b.append(entry.getKey());
                b.append(" :");
                List<String> l = entry.getValue();
                for (String v : l) {
                    b.append(' ').append(v);
                }
                b.append('\n');
            }
            logger.finer(b.toString());
        }
       return binMap;
    }

    /**
     * Obtains the command map for a service/tool deployment.
     * @param serviceName The name of the service
     * @return The command map, if applicable, for that service
     * @throws Exception Something went wrong obtaining the command map.
     */
    public static HashMap<String, List<String>> getServiceBinMap(
                                    String serviceName) throws Exception {
        HashMap<String, List<String>> binMap =
                                        new HashMap<String, List<String>>();

        if (serviceName != null)
            addResourceMap(binMap, Config.SERVICE_DIR, serviceName);

        mapPathExt(binMap);

        addCmdMapFile(binMap);

        // Dump the binMap for debugging
        if (logger.isLoggable(Level.FINER)) {
            StringBuilder b = new StringBuilder("Executable map for service ");
            b.append(serviceName).append(":\n");
            for (Map.Entry<String, List<String>> entry : binMap.entrySet()) {
                b.append(entry.getKey());
                b.append(" :");
                List<String> l = entry.getValue();
                for (String v : l) {
                    b.append(' ').append(v);
                }
                b.append('\n');
            }
            logger.finer(b.toString());
        }
       return binMap;

    }


    private static void addResourceMap(HashMap<String, List<String>> binMap,
                                       String baseDir, String resourceName) {
        // chmod is the way to make a file executable on Unix. Other platforms
        // like Win32 does not have it and uses a different mechanism. So
        // we'll run chmod only if it's there.
        File chmodCmd = new File("/bin/chmod");
        if (!chmodCmd.exists()) {
            chmodCmd = new File("/usr/bin/chmod");
            if (!chmodCmd.exists())
                chmodCmd = null;
        }

        ArrayList<String> chmod = null;
        if (chmodCmd != null) {
            chmod = new ArrayList<String>();
            chmod.add(chmodCmd.getAbsolutePath());
            chmod.add("+x");
        }
        File binDir = new File(baseDir + resourceName + "/bin/");
        boolean emptyList = addExecMap(binDir, binMap, chmod);
        File sbinDir = new File(binDir, Config.OS_DIR);
        emptyList = addExecMap(sbinDir, binMap, chmod) && emptyList;
        sbinDir = new File(binDir, Config.ARCH_DIR);
        emptyList = addExecMap(sbinDir, binMap, chmod) && emptyList;
        if (!emptyList)
            try {
                logger.fine("Changing mode for bin directories.");
                Command cmd = new Command(chmod);
                CommandHandle handle = cmd.execute();
                int exitValue = handle.exitValue();
                if (exitValue != 0)
                    logger.severe("Failed to chmod bin files. Exit value is " +
                                                                    exitValue);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot change mode on bin files", e);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE,
                           "Interrupted changing mode on bin files", e);
            }
    }

    private static boolean addExecMap(File binDir,
                                      HashMap<String, List<String>> binMap,
                                      List<String> chmod) {
        boolean emptyList = true;
        if (binDir.isDirectory()) {
            File[] binFiles = binDir.listFiles();
            for (int i = 0; i < binFiles.length; i++)
                if (!binFiles[i].isDirectory()) {
                    String name = binFiles[i].getName();
                    String fullPath = binFiles[i].getAbsolutePath();
                    ArrayList<String> v = new ArrayList<String>();
                    v.add(fullPath);
                    binMap.put(name, v);
                    if (chmod != null) {
                        chmod.add(fullPath);
                        emptyList = false;
                    }
                }
        }
        return emptyList;
    }

    /**
     * Obtains a list of path extensions valid in this environment.
     * @return A string array of valid path extensions
     */
    public static String[] getPathExt() {
        String pathExt = System.getProperty("faban.pathext");

        // Check the environment on a Windows system, in case
        // the faban.pathext property is not set.
        if (pathExt == null && File.separatorChar == '\\')
            pathExt = System.getenv("PATHEXT");

        if (pathExt == null)
            return null;
        pathExt = pathExt.trim();
        if (pathExt.length() == 0)
            return null;

        // Ensure each of the exts are lowercase.
        String[] pathExts = pathExt.split(File.pathSeparator);
        for (int i = 0; i < pathExts.length; i++) {
            pathExts[i] = pathExts[i].toLowerCase();
        }
        return pathExts;
    }

    /**
     * The mapPathExt modifies the binMap according to the Win32
     * conventions. For example, faban.cmd can be called with just faban.
     * But calling it with faban.cmd also works. The PATHEXT needs to be
     * passed to the JVM as a system property faban.pathext. PATHEXT
     * matching is case insensitive. On Win32-like systems, faban.pathext
     * would be set at the JVM invocation. On Unix systems, this property
     * should not be set. This method would be a noop in this case.
     * @param binMap The binMap
     */
    private static void mapPathExt(Map<String, List<String>> binMap) {

        String[] pathExts = getPathExt();
        if (pathExts == null)
            return;

        // Use a separate map so we don't modify binmap while iterating
        HashMap<String, List<String>> pathExtMap =
                                new HashMap<String, List<String>>();

        Set<String> binKeys = binMap.keySet();

        // Scan from back to front so the frontmost one put latest
        // overrides the others that were put before.
        for (int i = pathExts.length - 1; i >= 0; i--)
            for (String key : binKeys)
                if (key.toLowerCase().endsWith(pathExts[i])) {
                    List<String> value = binMap.get(key);
                    String newKey = key.substring(0, key.length() -
                                    pathExts[i].length());
                    pathExtMap.put(newKey, value);
                }

        binMap.putAll(pathExtMap);
    }


    /**
     * Reads the command map file and adds/modifies the exec map accordingly.
     * @param binMap
     * @throws Exception
     */
    private static void addCmdMapFile(Map<String, List<String>> binMap)
            throws Exception {
        ArrayList<CmdDetail> cmdList = new ArrayList<CmdDetail>();

        File cmdMap = new File(
                            Config.CONFIG_DIR + Config.OS_DIR + "cmdmap.xml");

        if (cmdMap.exists()) {

            FileInputStream is = new FileInputStream(cmdMap);
            parseStream(is, cmdList);

            // Next step is to use the map command path and prefixes to the
            // actual executable.
            for (Iterator<CmdDetail> iter = cmdList.iterator();
                 iter.hasNext();) {
                CmdDetail c = iter.next();
                if (c.exec == null) {
                    c.exec = binMap.get(c.name);
                } else {
                    String cmd = c.exec.get(0);
                    File f = new File(cmd); // The exec can still be in
                    if (!f.isAbsolute()) {     // the Faban path. May need
                        List<String> path = binMap.get(cmd); // another mapping.
                        if (path != null)      // if not absolute path. Just
                            c.exec.set(0, path.get(0)); // ignore if not found in map.
                    }                          // Should be in OS path instead.                        
                }
                if (c.exec == null) {
                    c.exec = new ArrayList<String>();
                    c.exec.add(c.name);
                }
                for (List<String> prefix : c.prefix) {
                    String cmd = prefix.get(0);
                    List<String> path = binMap.get(cmd);
                    if (path != null)
                        replaceFirst(prefix, path);
                }
            }

            for (CmdDetail cmd : cmdList) {
                ArrayList<String> exec = new ArrayList<String>();
                for (List<String> prefix : cmd.prefix)
                    exec.addAll(prefix);
                exec.addAll(cmd.exec);
                binMap.put(cmd.name, exec);
            }
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
        List<String> exec;
        List<List<String>> prefix;

        public String toString() {
            StringBuilder b = new StringBuilder();
            b.append(name);
            b.append(':');

            for (List<String> p : prefix)
                for (String e : p)
                    b.append(e).append(' ');

            b.append(exec);
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
                currentCmd.prefix = new ArrayList<List<String>>();
                if (prefixSize > 0) {
                    for (Map.Entry<Integer, String> entry : prefixes.entrySet())
                        currentCmd.prefix.add(Command.parseArgs(
                                                            entry.getValue()));
                }
                prefixes.clear();
                cmdList.add(currentCmd);

            } else if ("name".equals(qName)) {
                currentCmd.name = buffer.toString().trim();
            } else if ("exec".equals(qName)) {
                currentCmd.exec = Command.parseArgs(buffer.toString().trim());
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

    /**
     * Replace the first element of the command list with the relacement.
     * The original list will likely grow.
     * @param orig The original command list
     * @param replacement The replacement for the first entry in the list
     */
    public static void replaceFirst(List orig, List replacement) {
        ArrayList tmp = new ArrayList();
        tmp.addAll(orig);
        orig.clear();
        orig.addAll(replacement);
        for (int i = 1; i < tmp.size(); i++) {
            orig.add(tmp.get(i));
        }
    }

    /**
     * Main to test the command map.
     * @param args The command line argument
     * @throws Exception If the mapping shows any errors
     */
    public static void main(String[] args) throws Exception {
        ArrayList<CmdDetail> cmdList = new ArrayList<CmdDetail>();
        HashMap<String, List<String>> binMap =
                new HashMap<String, List<String>>();
        FileInputStream is = new FileInputStream("cmdmap.xml");
        parseStream(is, cmdList);

        // Next step is to use the map command path and prefixes to the
        // actual executable.
        for (CmdDetail c : cmdList) {
            if (c.exec == null)
                c.exec = binMap.get(c.name);
            if (c.exec == null) {
                c.exec = new ArrayList<String>();
                c.exec.add(c.name);
            }

            // Try to map exec cmd back to the binMap
            String exec = c.exec.get(0);

            // Search cmd for path separators ('/')
            int pathSepIdx = exec.indexOf(File.separator);

            // If not found, still a relative path
            if (pathSepIdx < 0) {
                // Map it
                List<String> execList = binMap.get(exec);
                // And combine back with args
                if (execList != null)
                    replaceFirst(c.exec, execList);
            }

            for (List<String> prefix : c.prefix) {
                String cmd = prefix.get(0);
                List<String> execList = binMap.get(cmd);
                if (execList != null) {
                    replaceFirst(prefix, execList);
                }
            }
        }

        for (Iterator<CmdDetail> iter = cmdList.iterator();
             iter.hasNext();) {
             CmdDetail c = iter.next();
             System.out.println(c.toString());
        }
    }
}
