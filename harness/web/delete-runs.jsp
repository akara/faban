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
 * $Id: delete-runs.jsp,v 1.2 2006/06/29 19:38:44 akara Exp $
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
  <%@ page language="java" import="java.io.Reader, com.sun.faban.harness.engine.RunQ" %>
  <%@ page session="true" %>
  <%@ page errorPage="error.jsp" %>
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
        for(int i = 0; i < runs.length; i++) {
%>
<b><%=    runs[i] %> <%= RunQ.getHandle().deleteRun(runs[i]) ? "   Done " : "   Failed "%></b>
<br>
<%      }
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
