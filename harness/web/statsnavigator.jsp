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
 * $Id: statsnavigator.jsp,v 1.14 2009/05/18 19:38:52 akara Exp $
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
                                 com.sun.faban.harness.webclient.RunResult,
                                 com.sun.faban.harness.common.HostTypes"%>
<%
    String runId = request.getParameter("runId");
    String[] statusFileContent = RunResult.readStatus(runId);
    boolean finished = true;
    if ("STARTED".equals(statusFileContent[0]))
        finished = false;

    // Now check the output files...
    File outDir = new File(Config.OUT_DIR + runId);
    // Tool output file pattern is <tool>.log.<host>[suffix]
    String[] suffixes = { ".html", ".htm", "" }; // The empty string always last
    TreeMap<String, HashSet<String>> allHosts =
            new TreeMap<String, HashSet<String>>();
    TreeSet<String> allTools = new TreeSet<String>();
    
    // These maps map the short host name to the one used in the file name.
    HashMap<String, String> infoHostMap = new HashMap<String, String>();
    HashMap<String, String> toolHostMap = new HashMap<String, String>();

    HashSet<String> toolFiles = new HashSet<String>();

    // These are known files that have file names looking like tool output.
    // They should be ignored.
    HashSet<String> ignoreFiles = new HashSet<String>();
    ignoreFiles.add("driver.log.lck");
    ignoreFiles.add("log.xml.lck");

    fileSearchLoop:
    for (String fileName : outDir.list()) {
        // Screen out all image files and files to be ignored...
        if (fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
            fileName.endsWith(".jpeg") || fileName.endsWith(".gif") ||
            ignoreFiles.contains(fileName))
            continue;

        // Then proces the sysinfo files...
        if (fileName.startsWith("sysinfo.")) {
            String toolName = "SystemInfo";
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
            if (logIdx == -1) // New FenXi files need to be .xan.
                logIdx = fileName.indexOf(".xan.");
            if (logIdx == -1) // summary.xml.hostname case
                logIdx = fileName.indexOf(".xml.");
            if (logIdx == -1)
                continue;
            String toolName = fileName.substring(0, logIdx);
            String hostName = null;

            // Grab the host name from file name, based on suffix
            for (String suffix : suffixes) {
                int suffixSize = suffix.length();
                if (suffixSize == 0 || fileName.endsWith(suffix)) {
                    int hostBegin = logIdx + 5;
                    int hostEnd = fileName.length() - suffixSize;
                    // if the host name part is missing, it is not a stat file.
                    if (hostBegin >= hostEnd)
                        continue fileSearchLoop;
                    hostName = fileName.substring(hostBegin, hostEnd);
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

    File hosttypes = new File(outDir, "META-INF");
    hosttypes = new File(hosttypes, "hosttypes");
    HostTypes hostTypes = null;
    if (hosttypes.isFile()) {
        hostTypes = new HostTypes(hosttypes.getAbsolutePath());
    }
%>
<html>
    <head>
        <title>Statistics for Run <%=runId%> [<%= Config.FABAN_HOST %>]</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
        <% if (!finished) { %>
            <meta http-equiv="refresh" content="10">
        <% } %>
        <%
            String[] hosts;
            if (hostTypes != null) { // If we know the types, order by relevance in that type.
                hosts = hostTypes.getHostsInOrder();
        %>
        <link rel="stylesheet" type="text/css" href="/css/style.css" />        
        <link rel="stylesheet" type="text/css" href="css/balloontip2.css" />
        <script type="text/javascript" src="scripts/balloontip2.js"></script>
         <%
            } else { // If we don't know the types, order by name
                Set<String> hostSet = allHosts.keySet();
                hosts = new String[hostSet.size()];
                hosts = hostSet.toArray(hosts);
            }
         %>
    </head>
    <body>
        <%
            if (hostTypes != null)
                for (String hostName : hosts) {
                    String[] types = hostTypes.getTypesByHost(hostName);
        %>
        <div id="<%= hostName %>_balloon" class="ballooncontent">
        <%
                    StringBuilder b = new StringBuilder();
                    for (String type : types) {
                        String[] aliases = hostTypes.getAliasesByHostAndType(hostName, type);
                        if (aliases.length == 0)
                            aliases = null;
                        if (aliases.length == 1 && hostName.equals(aliases[0]))
                            aliases = null;
                        b.append("<b>").append(type).append("</b>");
                        if (aliases != null) {
                            b.append(": ").append(aliases[0]);
                            for (int i = 1; i < aliases.length; i++)
                                b.append(", ").append(aliases[i]);
                        }
                        b.append("<br/>");
                    }
                    out.print(b.toString());
        %>
        </div>
        <%      } %>
        <table border="0" cellpadding="4" cellspacing="3"
            style="padding: 2px; border: 2px solid #cccccc; text-align: center; width: 80%;">

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
                     <th class="header">System</th>
                     <% for (String tool : allTools) { %>
                        <th class="header"><%= tool %></th>
                     <% } %>
                 </tr>
                 <% String[] rowclass = { "even", "odd" };
                    for (int i = 0; i < hosts.length; i++) {
                        HashSet<String> toolSet = allHosts.get(hosts[i]);
                        if (toolSet == null)
                            continue;
                        String mouseover = "";
                        if (hostTypes != null) {
                            mouseover = "onmouseover=\"ddrivetip('" + hosts[i] +
                                    "_balloon')\" onmouseout=\"hideddrivetip()\"";
                        }
                 %>
                 <tr class="<%= rowclass[i % 2] %>" <%= mouseover%>>
                     <% if (toolSet.contains("SystemInfo")) {
                             String fullName = infoHostMap.get(hosts[i]);
                             if (fullName == null)
                                 fullName = hosts[i];
                     %>
                        <td style="text-align: left;"><a href="output/<%= runId %>/sysinfo.<%= fullName %>.html"><%= hosts[i] %></a></td>
                     <% } else { %>
                        <td style="text-align: left;"><%= hosts[i] %></td>
                     <% }
                        for (String tool : allTools) {
                            if (toolSet.contains(tool)) {
                                String fullName = toolHostMap.get(hosts[i]);
                                if (fullName == null)
                                    fullName = hosts[i];
                                String[] filePrefix = new String[3];
                                filePrefix[0] = tool + ".log." + fullName;
                                filePrefix[1] = tool + ".xan." + fullName;
                                filePrefix[2] = tool + ".xml." + fullName;
                                String path = "output/" + runId + '/';
                     %>
                                <td style="text-align: center;">
                     <%
                                // Do the html link
                                boolean found = false;
                                String fileName = null;
                                for (int j = 0; j < filePrefix.length; j++) {
                                    fileName = filePrefix[j] + ".html";
                                    if (toolFiles.contains(fileName)) {
                                        found = true;
                                        break;
                                    }
                                    fileName = filePrefix[j] + ".htm";
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
                                for (int j = 0; j < filePrefix.length; j++) {
                                    fileName = filePrefix[j];
                                    if (toolFiles.contains(fileName)) {
                                        found = true;
                                        break;
                                    }
                                }
                                if (found) { %>
                                    <font size="-1"><i><a href="<%= path + fileName %>">raw</a></i></font>
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

