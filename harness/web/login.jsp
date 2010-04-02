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
<html>
<head>
<link rel="icon" type="image/gif" href="img/faban.gif">
<link rel="stylesheet" type="text/css" href="/css/style.css" />
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
<meta name="Author" content="Akara Sucharitakul"/>
<meta name="Description" content="Form to display User login"/>
<%@ page language="java" import="com.sun.faban.harness.webclient.UserEnv,
                                 com.sun.faban.harness.common.BenchmarkDescription,
                                 com.sun.faban.harness.webclient.Authenticator,
                                 java.util.Map,
                                 javax.security.auth.Subject,
                                 javax.security.auth.login.LoginException,
                                 java.security.Principal,
                                 java.util.Set,
                                 com.sun.faban.harness.common.Config"%>

<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
<title>Login [<%= Config.FABAN_HOST %>]</title>
</head>
<%  String bodyHdr = "<body><br/><br/><br/>";
    if (Config.SECURITY_ENABLED) {
        String reloadFrameBody = "<body onLoad=\"top.loginstat.document." +
                "location.reload(); top.menu.document.location.reload()\">" +
                "<br/><br/><br/>";
        String id = null;
        String passwd = null;
        if ("logout".equals(request.getQueryString())) {
            usrEnv.getAuthenticator().logout();
            bodyHdr=reloadFrameBody;
        } else {
            id = request.getParameter("fun");
            passwd = request.getParameter("fp");
        }

        if (id == null || passwd == null ||
            id.length() == 0 || passwd.length() == 0) {
%>
<%= bodyHdr %>
<form name="bench" method="post" action="login.jsp">
  <table cellpadding="0" cellspacing="2" border="0" align="center">
    <tbody>
      <tr>
        <td style="text-align: right;"><%= Config.loginPrompt %></td>
        <td>
          <input type="text" name="fun" title="<%= Config.loginHint %>" size="10">
        </td>
      </tr>
      <tr>
        <td style="text-align: right;"><%= Config.passwordPrompt %></td>
        <td>
          <input type="password" name="fp" title="<%= Config.passwordHint %>" size="10">
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
            Authenticator a = usrEnv.getAuthenticator();
            Subject s = null;
            try {
                s = a.login(id, passwd);
            } catch (LoginException e) {
                out.println(bodyHdr);
                String aMsg = a.getMessage();
                if (aMsg != null) {
%>
<h2><center><%= aMsg %></center></h2>
<%
                }
%>
<h2><center><%= e.getMessage() %></center></h2>
<%
            }
            if (s != null) {
                bodyHdr = reloadFrameBody;
%>
<%= bodyHdr %>
<h2><center>Welcome: <%= id %></center></h2>
<%
            }
        }
    } else {
%>
<%= bodyHdr %>
<h2><center>Security is not enabled!</center></h2>
<%
    }
%>
</body>
</html>
