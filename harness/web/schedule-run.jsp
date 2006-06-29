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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: schedule-run.jsp,v 1.1 2006/06/29 18:51:45 akara Exp $
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
  <%@ page language="java" import="java.io.Reader, com.sun.faban.harness.engine.RunQ,
                                   java.util.logging.Logger,
                                   com.sun.faban.harness.common.BenchmarkDescription" %>
  <%@ page session="true" %>
  <%@ page errorPage="error.jsp" %>
  <jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
  <link rel="icon" type="image/gif" href="img/faban.gif">
</head>
<body>
<h3>Run scheduled </h3>
<%
    Logger logger = Logger.getLogger(this.getClass().getName());
    Reader reader = request.getReader();
    int size = request.getContentLength();
    char[] buf = new char[size];
    logger.finer("Length of buffer created is " + size);
    int len = 0;
    while(len < size)
        len += reader.read(buf, len, size - len);

    logger.finer("Length of buffer read is " + len);
    String user = (String)session.getAttribute("faban.user");
    BenchmarkDescription benchmark = (BenchmarkDescription)session.getAttribute(
                                    "faban.benchmark");

    usrEnv.saveParamRepository(user, benchmark, buf);
    // Call runq to get the run id.
    String runId = null;
    runId = RunQ.getHandle().addRun(user, benchmark);
%>
Run ID for this run is : <b><%= runId %></b>
<br/>
<br/>
<b><%=RunQ.getHandle().getRunDaemonStatus() %></b>
</body>
</html>
