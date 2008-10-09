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
 * $Id: Utilities.java,v 1.6 2008/10/09 09:58:32 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Common utilities, usually accessible via static import.
 */
public class Utilities {

    /**
     * Parses a string escaped with \n, \t, \020, etc. Returns the
     * properly escaped string.
     * @param s The string with backslashes
     * @return The string with control characters or unicode
     */
    public static String parseEscapedString(String s) {
        char[] cArray = s.toCharArray();
        boolean escape = false;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < cArray.length; ++i) {
            if (escape) {
                if (cArray[i] == 'b') {
                    buf.append('\b');
                } else if (cArray[i] == 't') {
                    buf.append('\t');
                } else if (cArray[i] == 'n') {
                    buf.append('\n');
                } else if (cArray[i] == 'r') {
                    buf.append('\r');
                } else if (cArray[i] == 'f') {
                    buf.append('\f');
                } else if (cArray[i] == 'u') {
                    // Unicode escape
                    int utf = Integer.parseInt(s.substring(i + 1, i + 5), 16);
                    buf.append((char)utf);
                    i += 4;
                } else if (Character.isDigit(cArray[i])) {
                    // Octal escape
                    int j = 0;
                    for (j = 1; (j < 2) && (i + j < cArray.length); ++j) {
                        if (!Character.isDigit(cArray[i+j]))
                            break;
                    }
                    int octal = Integer.parseInt(s.substring(i, i + j), 8);
                    buf.append((char)octal);
                    i += j;
                } else {
                    buf.append(cArray[i]);
                }
                escape = false;
            } else if (cArray[i] == '\\') {
                escape = true;
            } else {
                buf.append(cArray[i]);
            }
        }
        return buf.toString();
    }

    /**
     * Obtains the JAVA_HOME of the current JVM.
     * @return The current JAVA_HOME
     */
    public static String getJavaHome() {
        String javaHome = System.getProperty("java.home");
        String suffix = File.separator + "jre";
        if (javaHome.endsWith(suffix))
            javaHome = javaHome.substring(0, javaHome.length() -
                       suffix.length());
        return javaHome;
    }


    /**
     * Obtains the jar file that contains the class in question.
     * @param clazz The given class
     * @return The jar file containing the class, or null if the class is not
     *         local or not loaded from a jar file
     */
    public static File getJarFile(Class clazz) {
        String resName = clazz.getName();
        resName = "/" + resName.replace('.', '/') + ".class";
        // Sample URL: jar:file:/opt/faban/benchmarks/web101/lib/web101.jar!/sample/driver/WebDriver.class
        URL classURL = clazz.getResource(resName);
        if (classURL == null)
            return null;
        String jarHeader = "jar:file:";
        String urlString = classURL.toString();

        if (!urlString.startsWith(jarHeader))
            return null;

        int bangIdx = urlString.indexOf('!', jarHeader.length());
        String jarPath = urlString.substring(jarHeader.length(), bangIdx);
        File jarFile = new File(jarPath);
        if (!jarFile.exists())
            return null;

        return jarFile;
    }

    /**
     * Parses the host:port string and puts the list of host:port pairs
     * into a list.
     * @param hostPorts The host:port string
     * @return list of host and ports
     */
    public static List<NameValuePair<Integer>> parseHostPorts(String hostPorts){

        ArrayList<NameValuePair<Integer>> hostPortList =
                                    new ArrayList<NameValuePair<Integer>>();

        _parseHostPorts(hostPorts, hostPortList, null);

        return hostPortList;
    }

    /**
     * Parses the host:port string and puts the list of host:port pairs
     * into the hostPortList
     * @param hostPorts The host:port string
     * @param hostPortList The list to insert the host:port pairs
     * @param hostSet The set to insert host names, null if not needed
     * @return list of host names, space separated
     */
    public static String parseHostPorts(String hostPorts,
                          List<NameValuePair<Integer>> hostPortList,
                          Set<String> hostSet) {

        if (hostSet == null)
            hostSet = new LinkedHashSet<String>();

        _parseHostPorts(hostPorts, hostPortList, hostSet);

        // Now extract the unique hosts
        StringBuffer hosts = new StringBuffer();
        for (String host : hostSet) {
            hosts.append(host);
            hosts.append(' ');
        }
        return hosts.toString();
    }

    private static void _parseHostPorts(String hostPorts,
                               List<NameValuePair<Integer>> hostPortList,
                               Set<String> hostSet) {
        // replacing all the newline characters and other white space
        // characters with a blank space

        hostPorts = hostPorts.replaceAll("\\s", " ");

        // Find the patterns that have either hostname or hostname:port values
        Pattern p1 = Pattern.compile("([a-zA-Z_0-9-\\.]+):?(\\w*)\\s*");
        Matcher m1 = p1.matcher(hostPorts + ' '); // add a whitespace at end

        //  Fill up the hosts set with names of all the hosts
        for (boolean found = m1.find(); found; found = m1.find()) {
            NameValuePair<Integer> hostPort = new NameValuePair<Integer>();
            hostPort.name = m1.group(1);
            String port = m1.group(2);
            if (port != null && port.length() > 1)
                hostPort.value = new Integer(port);
            if (hostSet != null)
                hostSet.add(hostPort.name);
            hostPortList.add(hostPort);
        }
    }

    /**
     * Simple, but frequently used utility function to determine which bucket
     * or group the current value belongs to, providing the total and the number
     * of buckets the values get divided into. If there is a remainder in the
     * division, the remainder is spread equally to the lower numbered buckets.
     * Those buckets will be one value larger than the higher numbered buckets.
     * @param current The current value
     * @param total The total count
     * @param buckets The number of buckets to divide the set into.
     * @return The bucket number of the current value, starting with 0
     */
    public static int selectBucket(int current, int total, int buckets) {
        int[] selector = new int[buckets];

        int base = total / buckets;
        int remainder = total % buckets;

        // Assign the bucket size to the selector
        for (int i = 0; i < selector.length; i++) {
            selector[i] = base;
            if (i < remainder)
                ++selector[i];
        }

        // Accumulate the selector value
        for (int i = 1; i < selector.length; i++)
            selector[i] += selector[i - 1];


        int bucket = 0;
        for (; bucket < selector.length; bucket++)
            if (current < selector[bucket])
                break;
        return bucket;
    }
}
