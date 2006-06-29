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
 * $Id: new-run.jsp,v 1.2 2006/06/29 19:38:44 akara Exp $
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
<%@ page language="java" import="com.sun.faban.harness.webclient.UserEnv,
                                 com.sun.faban.harness.common.BenchmarkDescription,
                                 java.util.Map"%>
<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
<%
    String user = (String)session.getAttribute("faban.user");
    if (user == null) {
        user = request.getParameter("user");
        session.setAttribute("faban.user", user);
    }
    BenchmarkDescription benchDesc = (BenchmarkDescription)session.getAttribute("faban.benchmark");
    String benchmark = null;
    if (benchDesc == null) {
        Map bms = BenchmarkDescription.getBenchNameMap();
        benchmark = request.getParameter("benchmark");
        benchDesc = (BenchmarkDescription) bms.get(benchmark);
        session.setAttribute("faban.benchmark", benchDesc);
    }
    usrEnv.copyParamRepository(user, benchDesc);
    String url = "benchmarks/" + benchDesc.shortName + '/' + benchDesc.configForm;

    if ((user != null) && (benchDesc != null)) {
%>

<meta HTTP-EQUIV=REFRESH CONTENT="0;URL=<%=url%>">
<link rel="icon" type="image/gif" href="img/faban.gif">
</head>

<% }
   else {
%>
<body>
<form name="bench" method="post" action="select-user.jsp">

  <br/>
  <center><b>Unable to determine user or benchmark... Please Login</b></center>
  <br/>
  <center><input type="submit" value="OK"></center>
</form>
</body>
<% } %>
</html>
