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
 * $Id: resultlist.jsp,v 1.4 2006/10/20 22:39:08 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Ramesh Ramachandran"/>
        <meta name="Description" content="Reults JSP"/>
        <title>Benchmark results</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
    </head>
    <body>
    <%@ page language="java" import="com.sun.faban.harness.webclient.Result"%>
    <jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
    <% Result[] results = Result.getResults(usrEnv.getSubject());
        if(results != null && results.length > 0) {
    %>
            <form name="analyze" method="post" action="analyzeruns.jsp">
              <center>
                <input type="submit" name="process" value="Compare">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                <input type="submit" name="process" value="Average">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                <input type="reset">
              </center>
              <br>
              <table cellpadding="2" cellspacing="0" border="1" width="90%" align="center">
              <tbody>
              <tr>
                  <th>&nbsp;</th>
                  <th><%= results[0].runId %></th>
                  <th><%= results[0].description %></th>
                  <th><%= results[0].result %></th>
                  <th><%= results[0].scale %></th>
                  <th><%= results[0].metric %></th>
                  <th><%= results[0].status %></th>
                  <th><%= results[0].time %></th>
              <tr>
    <%
            for(int i = 1; i < results.length; i++) {
                Result result = results[i];
    %>
            <tr>
                <td><input type="checkbox" name="select" value="<%= result.runId %>"></input></td>
                <td><%= result.runId %></td>
                <td><%= result.description %></td>
                <td><%= result.result %></td>
                <td><%= result.scale %></td>
                <td><%= result.metric %></td>
                <td><%= result.status %></td>
                <td><%= result.time %></td>
            <tr>
        <%
                }
        %>
     </tbody>
     </table>
     <br/>
     <br/>
     <center>
     <input type="submit" name="process" value="Compare">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     <input type="submit" name="process" value="Average">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     <input type="reset">
     </center>
    </form>
    <%
        }
        else {
            %>
            <br/>
            <center><b>There are no results</b></center>
            <br/>
            <%
        }
    %>
    </body>
</html>
