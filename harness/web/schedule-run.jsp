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
<%@ page language="java" session="true" errorPage="error.jsp"
         import="java.io.Reader, com.sun.faban.harness.engine.RunQ,
                 com.sun.faban.harness.common.Config,
                 java.util.logging.Logger,
                 com.sun.faban.harness.common.BenchmarkDescription,
                 java.security.AccessControlException,
                 com.sun.faban.harness.security.AccessController" %>
<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
  <meta name="Author" content="Ramesh Ramachandran"/>
  <meta name="Description" content="JSP to setup run.xml for the XForms servlet"/>
  <title>Run Scheduled [<%= Config.FABAN_HOST %>]</title>
  <link rel="icon" type="image/gif" href="img/faban.gif">
  <link rel="stylesheet" type="text/css" href="/css/style.css" />
</head>
<body>
<br/><br/><br/>
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
    String profile = (String)session.getAttribute("faban.profile");
    BenchmarkDescription benchmark = (BenchmarkDescription)session.getAttribute(
                                    "faban.benchmark");

    if (!AccessController.isSubmitAllowed(usrEnv.getSubject(), benchmark.shortName)) {
        logger.severe("Security: Attempted schedule-run on benchmark " +
                benchmark.shortName + " by user: " + usrEnv.getUser() +
                ". Permission denied!");
%>
    <center><h2>Permission Denied</h2></center>
<%
    } else {
        usrEnv.saveParamRepository(profile, benchmark, buf);
        // Call runq to get the run id.
        String runId = RunQ.getHandle().addRun(usrEnv.getUser(), profile, benchmark);
%>
<h3>Run scheduled </h3>
Run ID for this run is : <b><a href="/controller/results/location/<%= runId %>"><%= runId %></a></b>
<br/>
<br/>
<b><%=RunQ.getHandle().getRunDaemonStatus() %></b>
<%  }  %>
</body>
</html>
