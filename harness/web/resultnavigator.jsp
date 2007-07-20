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
 * $Id: resultnavigator.jsp,v 1.9 2007/07/20 22:16:31 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
<%@ page language="java" import="java.io.FileReader,
                                 com.sun.faban.harness.common.Config,
                                 com.sun.faban.harness.ParamRepository,
                                 java.io.File,
                                 com.sun.faban.harness.common.BenchmarkDescription,
                                 java.io.IOException,
                                 com.sun.faban.harness.webclient.Result,
                                 com.sun.faban.harness.common.RunId"%>
<%
    String runId = request.getParameter("runId");
    String benchmark = new RunId(runId).getBenchName();

    // First, we try to get the meta info from the results.
    BenchmarkDescription desc = BenchmarkDescription.
            readDescription(benchmark, Config.OUT_DIR + runId);
    if (desc == null)

    // If not, we fetch it from the benchmark meta info.
    desc = BenchmarkDescription.getDescription(benchmark);
    String status = Result.getStatus(runId);
    boolean finished = true;
    if ("STARTED".equals(status))
        finished = false;
    String scale = "";
    if (desc != null) {
        ParamRepository par = new ParamRepository(Config.OUT_DIR + runId +
                File.separator + desc.configFileName, false);
        scale = par.getParameter("fa:scale");
    }
%>
<html>
    <head>
        <title>Result for Run <%=runId%></title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
        <% if (!finished) { %>
            <meta http-equiv="refresh" content="10">
        <% } %>
    </head>
    <body>
        <%
           String outputRef = null;
           File summaryFile = new File(Config.OUT_DIR + runId, desc.resultFilePath);
           if (summaryFile.exists()) {
        %>
               <a href="output/<%= runId %>/<%= desc.resultFilePath %>" target="display">Summary&nbsp;Result</a>&nbsp;
        <% } else { %>
               <span style="color: rgb(102, 102, 102);">Summary&nbsp;Result</span>&nbsp;
        <%
           }
           outputRef = null;
           String[] detailFiles = { "detail.html", "detail.xan.html", "detail.xml.html" };
           for (int i = 0; i < detailFiles.length; i++) {
               File detailOutput = new File (Config.OUT_DIR + runId, detailFiles[i]);
               if (detailOutput.exists()) {
                   outputRef = "output/" + runId + '/' + detailFiles[i];
                   break;
               }
           }
           if (outputRef == null) {
        %>
                   <span style="color: rgb(102, 102, 102);">Detailed&nbsp;Results</span>&nbsp;
        <%     } else {    %>
                   <a href="<%= outputRef %>" target="display">Detailed&nbsp;Results</a>&nbsp;
        <%
               }
           if (desc != null) { %>
            <a href="output/<%= runId %>/<%= desc.configFileName %>" target="display">
                Run&nbsp;Configuration</a>&nbsp;
        <% } else { %>
            <span style="color: rgb(102, 102, 102);">Run&nbsp;Configuration</span>&nbsp;
        <% } %>
        <% if (!finished) { %>
            <a href="LogReader?runId=<%= runId %>&startId=end#end" target="display">Run&nbsp;Log</a>&nbsp;
        <% } else { %>
            <a href="LogReader?runId=<%= runId %>" target="display">Run&nbsp;Log</a>&nbsp;
        <% } %>
            <a href="statsnavigator.jsp?runId=<%= runId %>" target="display">Statistics</a>&nbsp;
        <br><br><table border="0" cellspacing="0" cellpadding="0" width="100%">
            <tr>
                <td width="30%" style="text-align: left; vertical-align: bottom;"><%= desc.scaleName %><%= desc.scaleName == null ? "" : ":" %> <%= scale %> <%= desc.scaleUnit %></td>
                <td width="40%" style="text-align: center; vertical-align: bottom;"><b><big><big>RunID: <%= runId %></big></big></b></td>
                <td width="30%" style="text-align: right; vertical-align: bottom;">&nbsp;</td>
            </tr>
        </table>
    </body>
</html>