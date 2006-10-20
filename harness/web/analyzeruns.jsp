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
 * $Id: analyzeruns.jsp,v 1.1 2006/10/20 22:39:08 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Akara Sucharitakul"/>
        <meta name="Description" content="Reults JSP"/>
        <title>Analyze Runs</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
    </head>
    <body>
    <%@ page language="java" import="com.sun.faban.harness.webclient.Result"%>
    <jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
    <%
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
            out.println("Mode " + processString + " not supported.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }        
        
        String[] runNames = request.getParameterValues("select");
        if (runNames.length < 2) {
            if (mode == COMPARE)
                out.println("Select 2 runs to compare.");
            else
                out.println("Select at least 2 runs to average.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        if (mode == COMPARE & runNames.length > 2) {
            out.println("Compare no more than two runs. " +
                        runNames.length + " runs requested.");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String output = request.getParameter("output");
        if (output != null) {
            StringBuilder runList = new StringBuilder();
            StringBuilder suggestion = new StringBuilder();
            suggestion.append(modes[mode]);
            for (String runName : runNames) {
                runList.append(' ').append(runName);
                suggestion.append('_').append(runName);
            }
    %>
    <h2><center><%= processString %></center></h2>
    Runs:<%= runList %>
    <form name="analyzename" method="post" action="analyzeruns.jsp">
    Result name: <input type="text" name="output"
                 title="Enter name of the analysis result."
                 value="<%= suggestion %>" size="40">
    <input type=hidden name=recipient value="liam@htmlhelp.com">
    <center><input type="submit" value="<%= processString%>">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="reset"></center>
    </form>
    <%
        } else {

        }
    %>
    </body>
</html>