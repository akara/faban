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
 * $Id: resultlist.jsp,v 1.6 2006/11/17 01:55:17 akara Exp $
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
    <%@ page language="java" import="com.sun.faban.harness.webclient.Result,
                                     com.sun.faban.harness.webclient.TableModel"%>
    <jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
    <%  TableModel resultTable = Result.getResultTable(usrEnv.getSubject());
        int rows = resultTable.rows();
        if(resultTable != null && rows > 0) {
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
    <%      for (int i = 0; i < resultTable.columns(); i++) { %>
                  <th><%= resultTable.getHeader(i) %></th>
    <%      } %>
              <tr>
    <%
            for(int i = 0; i < rows; i++) {
                Comparable[] row = resultTable.getRow(i);
    %>
            <tr>
                <td><input type="checkbox" name="select" value="<%= row[0] %>"></input></td>
    <%          for (int j = 0; j < row.length; j++) { %>
                <td><%= row[j] %></td>
    <%          } %>
            <tr>
    <%      } %>
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
