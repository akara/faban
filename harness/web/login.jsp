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
 * $Id: login.jsp,v 1.1 2006/08/10 01:34:38 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
<html>
<head>
<link rel="icon" type="image/gif" href="img/faban.gif">
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
<meta name="Author" content="Ramesh Ramachandran"/>
<meta name="Description" content="Form to display User login"/>
<%@ page language="java" import="com.sun.faban.harness.webclient.UserEnv,
                                 com.sun.faban.harness.common.BenchmarkDescription,
                                 com.sun.faban.harness.webclient.Authenticator,
                                 java.util.Map,
                                 javax.security.auth.Subject,
                                 javax.security.auth.login.LoginException,
                                 java.security.Principal,
                                 java.util.Set"%>

<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
</head>
<body>
<%  String id = request.getParameter("fun");
    String passwd = request.getParameter("fp");
    if (id == null || passwd == null ||
        id.length() == 0 || passwd.length() == 0) {
%>
<br/>
<br/>
<br/>

<form name="bench" method="post" action="login.jsp">
  <table cellpadding="0" cellspacing="2" border="0" align="center">
    <tbody>
      <tr>
        <td>Login ID</td>
        <td>
          <input type="text" name="fun" size="10">
        </td>
      </tr>
      <tr>
        <td>Password</td>
        <td>
          <input type="password" name="fp" size="10">
        </td>
      </tr>
    </tbody>
  </table>
  <br>
  <br>
  <center><input type="submit" value="Login">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="reset"></center>
  </form>
<%
    } else {
       Authenticator a = new Authenticator(id, passwd);
        Subject s = null;
        try {
            s = a.login();
        } catch (LoginException e) {
%>
<h2><center><%= a.getMessage() %></center></h2>
<h2><center><%= e.getMessage() %></center></h2>
<%
        }
        Set<Principal> principals = s.getPrincipals();
%>
<h2>Welcome:</h2>
<%      for (Principal p : principals)
            out.println(p.getName() + "<br>");
    }
%>

</body>
</html>
