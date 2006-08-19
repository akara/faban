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
 * $Id: suspend-runs.jsp,v 1.3 2006/08/19 03:06:12 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
  <meta name="Author" content="Ramesh Ramachandran"/>
  <meta name="Description" content="JSP to suspend the run queue"/>
  <title>Suspend Run Queue</title>
  <%@ page language="java" import="com.sun.faban.harness.engine.RunQ,
                                   com.sun.faban.harness.security.AccessController,
                                   java.util.logging.Logger"
      session="true" errorPage="error.jsp"%>
  <jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
  <link rel="icon" type="image/gif" href="img/faban.gif">
</head>
<body>
<%
    Logger logger = Logger.getLogger(this.getClass().getName());
    if (AccessController.isRigManageAllowed(usrEnv.getSubject())) {
        logger.info("Audit: Run queue suspended by " + usrEnv.getUser());
        RunQ.getHandle().stopRunDaemon();
%>
<br/>
<br/>
<b><%=RunQ.getHandle().getRunDaemonStatus() %></b>
<%  } else {
        logger.severe("Security: Attempted suspend-runs by user: " +
                    usrEnv.getUser() + ". Permission denied!");
%>
<br/><br><h3><center>Permission Denied</center></h3>
<%  } %>
</body>
</html>
