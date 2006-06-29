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
 * $Id: pending-runs.jsp,v 1.2 2006/06/29 19:38:44 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
<html>
    <%@ page language="java" import="com.sun.faban.harness.engine.RunQ"%>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Ramesh Ramachandran"/>
        <meta name="Description" content="Pending Runs"/>
        <title>Benchmark results</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
    </head>
    <body>
        <br>
        <% String[][] pending = RunQ.getHandle().listRunQ();
            if((pending != null) && (pending.length > 0)) {
        %>
                <form  method="post" action="delete-runs.jsp">
                    <table cellpadding="2" cellspacing="0" border="1" width="80%" align="center">
                    <tbody>
                    <tr>
                        <th>Run ID</th>
                        <th>Benchmark</th>
                        <th>Description</th>
                    <tr>
        <%
                for(int i=0; i < pending.length; i++) {
                    String runqDir = pending[i][1] + "." + pending[i][0];
        %>
                    <tr>
                        <td><input type="checkbox" name="selected-runs"
                             value=<%=runqDir %>><%= pending[i][0] %></td>
                        <td><%= pending[i][1]%></td>
                        <td><%= pending[i][2]%></td>
                    <tr>
         <%     } %>           
                 </tbody>
                 </table>
                 <br>
                 <br>
                 <center>
                 <input type="submit" value="Remove">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                 <input type="reset"></center>
                 </center>
                </form>
        <%
            }
            else {
        %>
                <br/>
                <br/>
                <br/>
                <b><center>There are no pending runs.</center></b>
        <%  } %>
    </body>
</html>
