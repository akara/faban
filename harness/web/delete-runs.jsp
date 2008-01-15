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
 * $Id: delete-runs.jsp,v 1.3 2008/01/15 08:02:53 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
  <meta name="Author" content="Ramesh Ramachandran"/>
  <meta name="Description" content="JSP to Delete selected runs"/>
  <title>Failed</title>
  <link rel="icon" type="image/gif" href=img/faban.gif">
  <%@ page language="java" import="java.io.Reader, com.sun.faban.harness.engine.RunQ,
                                   com.sun.faban.harness.security.AccessController" %>
  <%@ page session="true" %>
  <%@ page errorPage="error.jsp" %>
  <jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>

</head>
<body>
<br/>
<%
    String[] runs = request.getParameterValues("selected-runs");
    if((runs != null) && (runs.length > 0)) {
%>
The following run/s have be removed
<br>
<%
        RunQ runQ = RunQ.getHandle();
        for(String runId : runs) {
            if (AccessController.isKillAllowed(usrEnv.getSubject(), runId)) {
                if (runQ.deleteRun(runId)) {
                    out.println("<b>" + runId + "   Done </b></br>");
                } else {
                    out.println("<b>" + runId + "   Failed </b></br>");
                }
            } else {
                out.print("<b>" + runId + "   Denied </b></br>");
            }
        }
    }
    else {
%>
No Runs selected to remove
<%
    }
%>

<br/>
<br/>
<br/>
<b><%=RunQ.getHandle().getRunDaemonStatus() %></b>
</body>
</html>
