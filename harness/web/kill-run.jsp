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
 * $Id: kill-run.jsp,v 1.2 2006/06/29 19:38:44 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
  <meta name="Author" content="Ramesh Ramachandran"/>
  <meta name="Description" content="JSP to setup run.xml for the XForms servlet"/>
  <title>Failed</title>
  <%@ page language="java" import="java.io.Reader, com.sun.faban.harness.engine.RunQ" %>
  <%@ page session="true" %>
  <%@ page errorPage="error.jsp" %>
  <link rel="icon" type="image/gif" href="img/faban.gif">
</head>
<body>
<%

    String run = RunQ.getHandle().getCurrentRunId();
    if (run == null) {
%>
<br><br><b>There is no current run!</b>
<%
    } else {
        String confTimeStr = request.getParameter("confirm");
        long lapse = -1l;
        if (confTimeStr != null) {
            lapse = System.currentTimeMillis() - Long.parseLong(confTimeStr);
        }
        if (lapse < 60000 && lapse > 0) { // The confirm must come within 60 sec

            String runId = request.getParameter("runId");
            String msg = "Run " + runId + " killed!";
            run = RunQ.getHandle().killCurrentRun(runId);
            if (run == null)
                msg = "Run " + runId + " no longer active!";
%>
<br/>
<br/>
<b><%= msg%></b>

<%     } else {
           run = RunQ.getHandle().getCurrentRunId();
%>
<form name="bench" method="post" action="kill-run.jsp">
<input type="hidden" name="confirm" value="<%=System.currentTimeMillis() %>"></input>
<input type="hidden" name="runId" value="<%=run %>"></input>

<br><br><center>Are you sure you want to kill run <b><%=run %></b>?<br>
<br><br>Please press the "Kill" button to continue<br>or choose a different
action from the menu on your left.<br><br>
<input type="submit" value="Kill"></center>
<%      }
    }
%>
</body>
</html>
