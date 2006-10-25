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
 * $Id: analyzeruns.jsp,v 1.3 2006/10/25 23:04:43 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
<%@ page language="java" import="java.io.File,
                                 java.util.HashSet,
                                 java.util.logging.Logger,
                                 com.sun.faban.common.Command,
                                 com.sun.faban.harness.common.Config,
                                 com.sun.faban.harness.common.RunId,
                                 com.sun.faban.harness.webclient.Result"%>
<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Akara Sucharitakul"/>
        <meta name="Description" content="Reults JSP"/>
        <title>Analyze Runs</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
    <%
        Logger logger = Logger.getLogger(this.getClass().getName());
        final int COMPARE = 0;
        final int AVERAGE = 1;
        final String[] modes = { "compare", "average" };

        String processString = request.getParameter("process");
        int mode;
        if ("Compare".equals(processString)) {
            mode = COMPARE;
        } else if ("Average".equals(processString)) {
            mode = AVERAGE;
        } else {
            String msg = "Mode " + processString + " not supported.";
            out.println(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }        
        
        String[] runIdStrings = request.getParameterValues("select");
        if (runIdStrings.length < 2) {
            String msg;
            if (mode == COMPARE)
                msg = "Select 2 runs to compare.";
            else
                msg = "Select at least 2 runs to average.";
            out.println(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
        
        if (mode == COMPARE & runIdStrings.length > 2) {
            String msg = "Compare no more than two runs. " +
                         runIdStrings.length + " runs requested.";
            out.println(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }

        String output = request.getParameter("output");
        if (output == null) {
            StringBuilder runList = new StringBuilder();
            StringBuilder suggestion = new StringBuilder();
            HashSet<String> benchNameSet = new HashSet<String>();
            HashSet<String> hostNameSet = new HashSet<String>();
            suggestion.append(modes[mode]);

            RunId[] runIds = new RunId[runIdStrings.length];
            String benchName = null;
            String hostName = null;
            for (int i = 0; i < runIdStrings.length; i++) {
                RunId runId = new RunId(runIdStrings[i]);
                runList.append(runId).append(", ");
                benchName = runId.getBenchName();
                hostName = runId.getHostName();
                benchNameSet.add(benchName);
                hostNameSet.add(hostName);
                runIds[i] = runId;
            }
            runList.setLength(runList.length() - 2); //Strip off the last comma

            /*
             * The run name can come in the form of bench.id or
             * host.bench.id. We try to keep the suggested name
             * as short as possible. We have three formats:
             * 1. Generic format: mode_runId1_runId2...
             * 2. Same benchmark: mode_bench_<host.>runId1_<host>.runId2...
             * 3. Same host, same benchmark: mode_bench_host_runId1_runId2...
             */
            if (benchNameSet.size() == 1) {
                suggestion.append('-').append(benchName);
                if (hostNameSet.size() == 1) {
                    if (hostName.length() > 0)
                        suggestion.append('-').append(hostName);
                    for (RunId runId : runIds)
                        suggestion.append('_').append(runId.getRunSeq());
                } else {
                    for (RunId runId : runIds) {
                        suggestion.append('_');
                        hostName = runId.getHostName();
                        if (hostName.length() > 0)
                            suggestion.append(hostName).append('.');
                        suggestion.append(runId.getRunSeq());
                    }
                }
            } else {
                for (RunId runId : runIds)
                    suggestion.append('_').append(runId);
            }
    %>
    </head>
    <body>
    <h2><center><%= processString %></center></h2>
    <form name="analyzename" method="post" action="analyzeruns.jsp">
    <table cellpadding="0" cellspacing="2" border="0" align="center">
      <tbody>
        <tr>
          <td style="text-align: right;">Runs: </td>
          <td style="font-family: sans-serif;"><%= runList %></td>
        </tr>
        <tr>
          <td style="text-align: right;">Result name: </td>
          <td>
            <input type="text" name="output"
                   title="Enter name of the analysis result."
                   value="<%= suggestion %>" size="40">
          </td>
        </tr>
      </tbody>
    </table><br>
    <%
            for (int i = 0; i < runIdStrings.length; i++) {
    %>
    <input type="hidden" name="select" value="<%= runIdStrings[i] %>">
    <%
            }
    %>
    <center><input type="submit" name="process" value="<%= processString%>"
            >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="reset"></center>
    </form>
    <%
        } else {
            File analysisDir = new File(Config.ANALYSIS_DIR + output);
            if (!analysisDir.mkdirs()) {
                String msg = "Failed creating directory " + analysisDir +'!';
                out.println(msg);
                response.sendError(HttpServletResponse.SC_CONFLICT, msg);
                return;
            }
            StringBuilder cmd = new StringBuilder(256);
            cmd.append(Config.BIN_DIR.trim()).append("xanadu ").
                append(modes[mode]);
            for (String runId : runIdStrings)
                cmd.append(' ').append(Config.OUT_DIR).append(runId).
                        append(File.separator).append(Config.XML_STATS_DIR);
            
            cmd.append(' ').append(Config.ANALYSIS_DIR).append(output);

            logger.info("Executing: " + cmd.toString());
            Command c = new Command(cmd.toString());
            c.execute();
            File outIdx = new File(analysisDir, "index.html");
            if (!outIdx.exists()) {
                String msg = "Failed creating analysis.";
                analysisDir.delete();
                out.println(msg);
                response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED,
                                   msg);
                return;
            }
    %>
            <meta HTTP-EQUIV=REFRESH CONTENT="0;URL=analysis/<%= output %>/index.html">
        </head>
    <body>
    <%
        }
    %>
    </body>
</html>