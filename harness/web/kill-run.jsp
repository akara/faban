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
<%@ page language="java" import="com.sun.faban.harness.common.Config,
                                 com.sun.faban.harness.engine.RunQ,
                                 com.sun.faban.harness.security.AccessController,
                                 java.util.logging.Logger"
         session="true" errorPage="error.jsp"%>
<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
  <meta name="Author" content="Ramesh Ramachandran"/>
  <meta name="Description" content="JSP to setup run.xml for the XForms servlet"/>
  <title>Kill Current Run [<%= Config.FABAN_HOST %>]</title>
  <link rel="icon" type="image/gif" href="img/faban.gif">
  <link rel="stylesheet" type="text/css" href="/css/style.css" />
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
            String msg;
            if (AccessController.isKillAllowed(usrEnv.getSubject(), runId)) {
                msg = "Run " + runId + " killed!";
                run = RunQ.getHandle().killCurrentRun(runId, usrEnv.getUser());
                if (run == null)
                    msg = "Run " + runId + " no longer active!";
            } else {
                msg = "Permission Denied";
            }
%>
<br/>
<br/>
<b><%= msg%></b>
<%      } else {
            run = RunQ.getHandle().getCurrentRunId();
            if (AccessController.isKillAllowed(usrEnv.getSubject(), run)) {

%>
<form name="bench" method="post" action="kill-run.jsp">
<input type="hidden" name="confirm" value="<%=System.currentTimeMillis() %>"></input>
<input type="hidden" name="runId" value="<%=run %>"></input>

<br/><br/><center>Are you sure you want to kill run <b><%=run %></b>?<br/>
<br/><br/>Please press the "Kill" button to continue<br>or choose a different
action from the menu on your left.<br/><br/>
<input type="submit" value="Kill"></center>
<%          } else { %>
<br/><br/><h3><center>Sorry, you have no permission killing run
                <%= run %></center></h3>                
<%          }
        }
    }
%>
</body>
</html>
