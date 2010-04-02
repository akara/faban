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
-->
<%@ page language="java" import="java.util.logging.Logger,
                                 com.sun.faban.harness.common.Config,
                                 com.sun.faban.harness.webclient.RunAnalyzer,
                                 java.io.IOException,
                                 java.util.logging.Level"%>
<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Akara Sucharitakul"/>
        <meta name="Description" content="Results JSP"/>
        <title>Analyze Runs [<%= Config.FABAN_HOST %>]</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
    <%
        Logger logger = Logger.getLogger(this.getClass().getName());

        String processString = request.getParameter("process");
        RunAnalyzer.Type type;
        if ("Compare".equals(processString)) {
            type = RunAnalyzer.Type.COMPARE;
        } else if ("Average".equals(processString)) {
            type = RunAnalyzer.Type.AVERAGE;
        } else {
            String msg = "Type " + processString + " not supported.";
            out.println(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }        
        
        String[] runIds = request.getParameterValues("select");
        if (runIds.length < 2) {
            String msg;
            if (type == RunAnalyzer.Type.COMPARE)
                msg = "Select 2 runs to compare.";
            else
                msg = "Select at least 2 runs to average.";
            out.println(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }

        /* We should not need this based on the latest FenXi. It allows
           comparing more than two runs.
           
        if (type == RunAnalyzer.Type.COMPARE & runIds.length > 2) {
            String msg = "Compare no more than two runs. " +
                         runIds.length + " runs requested.";
            out.println(msg);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, msg);
            return;
        }
        */

        String output = request.getParameter("output");

        boolean analyze = false;
        boolean redirect = false;

        if (output == null) {
            StringBuilder runList = new StringBuilder();

            for (int i = 0; i < runIds.length; i++)
                runList.append(runIds[i]).append(", ");

            runList.setLength(runList.length() - 2); //Strip off the last comma
            String suggestion = RunAnalyzer.suggestName(type, runIds);
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
            for (int i = 0; i < runIds.length; i++) {
    %>
    <input type="hidden" name="select" value="<%= runIds[i] %>"/>
    <%
            }
    %>
    <center><input type="submit" name="process" value="<%= processString%>"
            >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="reset"></center>
    </form>
    <%
        } else if (RunAnalyzer.exists(output)) {
            String replace = request.getParameter("replace");
            if (replace == null) {
    %>
    </head>
    <body>
    <h2><center>Analysis Already Exists</center></h2>
       <form name="replaceOld" method="post" action="analyzeruns.jsp">
    <%
            for (int i = 0; i < runIds.length; i++) {
    %>
         <input type="hidden" name="select" value="<%= runIds[i] %>"/>
    <%
            }
    %>
         <input type="hidden" name="process" value="<%= processString%>"/>
         <input type="hidden" name="output" value="<%= output%>"/>
         <center>
         <input type="submit" name="replace" value="Replace"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
         <input type="submit" name="replace" value="Use Old Analysis"/>
         </center>
       </form>
    </body>
    <%
            } else if (replace.equals("Replace")) {
                analyze = true;
                redirect = true;
            } else {
                redirect = true;
            }
        } else {
            analyze = true;
            redirect = true;
        }

        if (analyze)
            try {
                RunAnalyzer.clear(output);
                RunAnalyzer.analyze(type, runIds, output, usrEnv.getUser());
            } catch (IOException e) {
                String msg = e.getMessage();
                out.println(msg);
                logger.log(Level.SEVERE, msg, e);
                response.sendError(HttpServletResponse.SC_CONFLICT, msg);
            }

        if (redirect) {
    %>
            <meta HTTP-EQUIV=REFRESH CONTENT="0;URL=analysis/<%= output %>/index.html">
        </head>
    <body>
    <%
        }
    %>
    </body>
</html>