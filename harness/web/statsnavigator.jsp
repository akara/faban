<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
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
 * $Id$
 *
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
 /**
  * Modified by Shanti Subramanyam to support graphing through xan_view
  */
-->
<%@ page language="java" import="java.io.FileReader,
                                 com.sun.faban.harness.common.Config,
                                 com.sun.faban.harness.ParamRepository,
                                 java.io.File,
                                 com.sun.faban.harness.common.BenchmarkDescription,
                                 java.util.*,
                                 com.sun.faban.harness.webclient.RunResult,
                                 com.sun.faban.harness.common.HostRoles"%>
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
    String[] htmlSuffixes = { ".html", ".htm" };
    TreeSet<String> allHosts = new TreeSet<String>();
    TreeSet<String> allTools = new TreeSet<String>();
    
    // This map maps the short host name to the one used in the file name.
    HashMap<String, String> infoHostMap = new HashMap<String, String>();

    // The toolHostMap maps host:tool to the actual stats file name.
    HashMap<String, ArrayList<String>> toolHostMap =
            new HashMap<String, ArrayList<String>>();

    File hosttypes = new File(outDir, "META-INF");
    hosttypes = new File(hosttypes, "hosttypes");
    HostRoles hostRoles = null;
    if (hosttypes.isFile()) {
        hostRoles = new HostRoles(hosttypes.getAbsolutePath());
    }

    // These are known files that have file names looking like tool output.
    // They should be ignored.
    HashSet<String> ignoreFiles = new HashSet<String>();
    ignoreFiles.add("driver.log.lck");
    ignoreFiles.add("log.xml.lck");

    fileSearchLoop:
    for (String fileName : outDir.list()) {
        // Screen out all image files, hidden files, and files to be ignored...
        if (fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
            fileName.endsWith(".jpeg") || fileName.endsWith(".gif") ||
            fileName.startsWith(".") || ignoreFiles.contains(fileName))
            continue;

        // Then proces the sysinfo files...
        if (fileName.startsWith("sysinfo.")) {
            String hostName = fileName.substring(8, fileName.length() - 5);
            infoHostMap.put(hostName, hostName); // Points to it's own name
            allHosts.add(hostName);

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
                    int idx = hostName.indexOf('.');
                    if (idx > 0)
                        hostName = hostName.substring(0, idx);

                    break;
                }
            }

            if (hostRoles != null) { // Map the alias to the actual host name
                hostName = hostRoles.getHostByAlias(hostName);
            } else { // No roles? Drop the domain part of the host.
                int domainIdx = hostName.indexOf('.');
                if (domainIdx != -1) {
                    String fullName = hostName;
                    hostName = fullName.substring(0, domainIdx);
                }
            }
            if (hostName == null) {
                response.sendError(500, "Error mapping stats. Offending file: "
                                        + fileName);
                return;
            }

            String toolHostKey = hostName + ':' + toolName;
            ArrayList<String> toolHostFiles = toolHostMap.get(toolHostKey);
            if (toolHostFiles == null) {
                toolHostFiles = new ArrayList<String>(2);
                toolHostMap.put(toolHostKey, toolHostFiles);
            }
            toolHostFiles.add(fileName);
            allHosts.add(hostName);
            allTools.add(toolName);
        }
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
            if (hostRoles != null) { // If we know the roles, order by relevance in that role.
                hosts = hostRoles.getHostsInOrder();
        %>
        <link rel="stylesheet" type="text/css" href="/css/style.css" />        
        <link rel="stylesheet" type="text/css" href="css/balloontip2.css" />
        <script type="text/javascript" src="scripts/balloontip2.js"></script>
         <%
            } else { // If we don't know the roles, order by name
                hosts = new String[allHosts.size()];
                hosts = allHosts.toArray(hosts);
            }
         %>
    </head>
    <body>
        <%
            if (hostRoles != null)
                for (String hostName : hosts) {
                    String[] roles = hostRoles.getRolesByHost(hostName);
        %>
        <div id="<%= hostName %>_balloon" class="ballooncontent">
        <%
                    StringBuilder b = new StringBuilder();
                    for (String role : roles) {
                        String[] aliases = hostRoles.getAliasesByHostAndRole(hostName, role);
                        if (aliases.length == 0)
                            aliases = null;
                        if (aliases.length == 1 && hostName.equals(aliases[0]))
                            aliases = null;
                        b.append("<b>").append(role).append("</b>");
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
                    String path = "/output/" + runId + '/';
                    String xanPath = "/controller/view/xan_view/" + runId + '/';
                    ArrayList<String> htmlFiles = new ArrayList<String>(1);
                    ArrayList<String> graphLinks = new ArrayList<String>(1);

                    for (int i = 0; i < hosts.length; i++) {
                        String mouseover = "";
                        if (hostRoles != null) {
                            mouseover = "onmouseover=\"ddrivetip('" + hosts[i] +
                                    "_balloon')\" onmouseout=\"hideddrivetip()\"";
                        }
                 %>
                 <tr class="<%= rowclass[i % 2] %>" <%= mouseover%>>
                     <% String fullName = infoHostMap.get(hosts[i]);
                        if (fullName != null) {
                     %>
                        <td class="tablecell" style="text-align: left;"><a href="<%= path %>sysinfo.<%= fullName %>.html"><%= hosts[i] %></a></td>
                     <% } else { %>
                        <td class="tablecell" style="text-align: left;"><%= hosts[i] %></td>
                     <% }
                        for (String tool : allTools) {
                            ArrayList<String> toolHostFiles = toolHostMap.get(
                                                    hosts[i] + ':' + tool);
                            if (toolHostFiles != null && toolHostFiles.size() > 0) {
                     %>
                                <td class="tablecell" style="text-align: center;">
                                <%
                                // Separate html files out from the raw files in toolHostFiles.
                                toolFileLoop:
                                for (Iterator<String> iter = toolHostFiles.iterator(); iter.hasNext();) {
                                    String fileName = iter.next();
                                    if (fileName.indexOf(".xan.") > 0 || fileName.endsWith(".xan")) {
                                        iter.remove();
                                        graphLinks.add(fileName);
                                    }
                                    else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                                        iter.remove();
                                        htmlFiles.add(fileName);
                                    } else {
                                        // For each of the raw files, there might be an
                                        // html file in the post directory. Check that.
                                        String htmlFileName = Config.POST_DIR + fileName;
                                        for (String suffix : htmlSuffixes) {
                                            String tryFileName = htmlFileName + suffix;
                                            File htmlFile = new File(outDir, tryFileName);
                                            if (htmlFile.exists()) {
                                                htmlFiles.add(tryFileName);
                                                continue toolFileLoop;
                                            }
                                        }
                                    }
                                }
                                // Do the graph link
                                for (String fileName : graphLinks) {
                                %>
                                    <small><i><a href="<%= xanPath + fileName %>">graphs</a></i></small>
                                <% }
                                graphLinks.clear();

                                // Do the html link
                                for (String fileName : htmlFiles) { %>
                                    <small><i><a href="<%= path + fileName %>">html</a></i></small>
                             <% }
                                htmlFiles.clear();

                                // Do the raw link
                                for (String fileName : toolHostFiles) { %>
                                    <small><i><a href="<%= path + fileName %>">raw</a></i></small>
                             <% }
                     %>
                                </td>
                     <%     } else {    %>
                                <td class="tablecell">&nbsp;</td>
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

