<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<!--
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
 * $Id: statsnavigator.jsp,v 1.4 2006/07/29 01:03:03 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
<%@ page language="java" import="java.io.FileReader,
                                 com.sun.faban.harness.common.Config,
                                 com.sun.faban.harness.ParamRepository,
                                 java.io.File,
                                 com.sun.faban.harness.common.BenchmarkDescription,
                                 java.util.*,
                                 com.sun.faban.harness.webclient.Result"%>
<%
    String runId = request.getParameter("runId");

    boolean finished = true;
    if ("STARTED".equals(Result.getStatus(runId)))
        finished = false;

    // Now check the output files...
    File outDir = new File(Config.OUT_DIR + runId);
    // Tool output file pattern is <tool>.log.<host>[suffix]
    String[] suffixes = { ".html", "htm", "" }; // The empty string always last
    TreeMap<String, HashSet<String>> allHosts =
            new TreeMap<String, HashSet<String>>();
    TreeSet<String> allTools = new TreeSet<String>();
    
    // These maps map the short host name to the one used in the file name.
    HashMap<String, String> infoHostMap = new HashMap<String, String>();
    HashMap<String, String> toolHostMap = new HashMap<String, String>();

    HashSet<String> toolFiles = new HashSet<String>();

    for (String fileName : outDir.list()) {
        // Screen out all image files...
        if (fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
            fileName.endsWith(".jpeg") || fileName.endsWith(".gif") ||
            "driver.log.lck".equals(fileName)) // Special case,
            continue;                          // looks like a tool output.

        // Then proces the sysinfo files...
        if (fileName.startsWith("sysinfo.")) {
            String toolName = "System&nbsp;Info";
            String hostName = fileName.substring(8, fileName.length() - 5);

            // drop the domain part of the host.
            int domainIdx = hostName.indexOf('.');
            if (domainIdx != -1) {
                String fullName = hostName;
                hostName = fullName.substring(0, domainIdx);
                infoHostMap.put(hostName, fullName);
            }

            HashSet<String> toolSet = allHosts.get(hostName);
            if (toolSet == null) {
                toolSet = new HashSet<String>();
                allHosts.put(hostName, toolSet);
            }
            toolSet.add(toolName);
        // Process the real stats files.
        } else {
            int logIdx = fileName.indexOf(".log.");
            if (logIdx == -1) // New xanadu files need to be .xan.
                logIdx = fileName.indexOf(".xan.");
            if (logIdx == -1)
                continue;
            String toolName = fileName.substring(0, logIdx);
            String hostName = null;

            // Grab the host name from file name, based on suffix
            for (String suffix : suffixes) {
                int suffixSize = suffix.length();
                if (suffixSize == 0 || fileName.endsWith(suffix)) {
                    hostName = fileName.substring(logIdx + 5,
                               fileName.length() - suffixSize);
                    break;
                }
            }

            // drop the domain part of the host.
            int domainIdx = hostName.indexOf('.');
            if (domainIdx != -1) {
                String fullName = hostName;
                hostName = fullName.substring(0, domainIdx);
                toolHostMap.put(hostName, fullName);
            }

            allTools.add(toolName);
            HashSet<String> toolSet = allHosts.get(hostName);
            if (toolSet == null) {
                toolSet = new HashSet<String>();
                allHosts.put(hostName, toolSet);
            }
            toolSet.add(toolName);
            toolFiles.add(fileName);
        }
    }
%>
<html>
    <head>
        <title>JESMark Statistics for Run <%=runId%></title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
        <% if (!finished) { %>
            <meta http-equiv="refresh" content="10">
        <% } %>

    </head>
    <body>
        <table cellpadding="2" cellspacing="0" border="1" width="80%" align="center">
            <% if (allHosts.size() == 0) {
                    if (!finished) {
            %>
                <h2><center>Statistics not yet available.</center></h2>
            <%      } else { %>
                <h2><center>Statistics not available for this run.</center></h2>
            <%      }
               } else { %>
                 <tbody>
                 <tr>
                     <th>System&nbsp;Name</th>
                     <th>System&nbsp;Info</th>
                     <% for (Iterator<String> iter = allTools.iterator(); iter.hasNext();) { %>
                              <th><%= iter.next() %></th>
                      <% } %>
                 </tr>
                 <% for (Iterator<String> hostIter = allHosts.keySet().iterator(); hostIter.hasNext();) {
                        String host = hostIter.next();
                        HashSet<String> toolSet = allHosts.get(host);
                 %>
                 <tr>
                        <td style="text-align: left;"><%= host %></td>
                     <% if (toolSet.contains("System&nbsp;Info")) {
                             String fullName = infoHostMap.get(host);
                             if (fullName == null)
                                 fullName = host;
                     %>
                        <td style="text-align: center;"><a href="output/<%= runId %>/sysinfo.<%= fullName %>.html"><img src="img/view.gif" alt="View" border="0"></img></a></td>
                     <% } else { %>
                        <td style="text-align: center;">&nbsp;</td>
                     <% }
                        for (Iterator<String> iter = allTools.iterator(); iter.hasNext();) {
                            String tool = iter.next();
                            if (toolSet.contains(tool)) {
                                String fullName = toolHostMap.get(host);
                                if (fullName == null)
                                    fullName = host;
                                String[] filePrefix = new String[2];
                                filePrefix[0] = tool + ".log." + fullName;
                                filePrefix[1] = tool + ".xan." + fullName;
                                String path = "output/" + runId + '/';
                     %>
                                <td style="text-align: center;">
                     <%
                                // Do the html link
                                boolean found = false;
                                String fileName = null;
                                for (int i = 0; i < filePrefix.length; i++) {
                                    fileName = filePrefix[i] + ".html";
                                    if (toolFiles.contains(fileName)) {
                                        found = true;
                                        break;
                                    }
                                    fileName = filePrefix[i] + ".htm";
                                    if (toolFiles.contains(fileName)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (found) { %>
                                    <font size="-1"><i><a href="<%= path + fileName %>">html</a></i></font>
                             <% }
                                // Do the text link
                                found = false;
                                for (int i = 0; i < filePrefix.length; i++) {
                                    fileName = filePrefix[i];
                                    if (toolFiles.contains(fileName)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (found) { %>
                                    <font size="-1"><i><a href="<%= path + fileName %>">text</a></i></font>
                             <% }

                     %>
                                </td>
                     <%     } else {    %>
                                <td>&nbsp;</td>
                     <%
                            }
                        }
                     }
                     %>
                 </tr>
                 </tbody>
             <% } %>
         </table>
    </body>
</html>

