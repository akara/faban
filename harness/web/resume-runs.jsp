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
  <meta name="Description" content="JSP to resume the run queue"/>
  <title>Resume Run Queue [<%= Config.FABAN_HOST %>]</title>
  <link rel="icon" type="image/gif" href="img/faban.gif">
  <link rel="stylesheet" type="text/css" href="/css/style.css" />
</head>
<body>
<%
    Logger logger = Logger.getLogger(this.getClass().getName());
    if (AccessController.isRigManageAllowed(usrEnv.getSubject())) {
        logger.info("Audit: Run queue resumed by " + usrEnv.getUser());
        RunQ.getHandle().startRunDaemon();
%>
<br/>
<br/>
<b><%=RunQ.getHandle().getRunDaemonStatus() %></b>
<%  } else {
        logger.severe("Security: Attempted resume-runs by user: " + 
                    usrEnv.getUser() + ". Permission denied!");
%>
<br/><br><h3><center>Permission Denied</center></h3>
<%  } %>
</body>
</html>
