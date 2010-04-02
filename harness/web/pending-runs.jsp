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
    <%@ page language="java" import="javax.security.auth.Subject,
                                     com.sun.faban.harness.common.Config,
                                     com.sun.faban.harness.engine.RunQ,
                                     com.sun.faban.harness.security.AccessController"%>
    <jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Ramesh Ramachandran"/>
        <meta name="Description" content="Pending Runs"/>
        <title>Pending Runs [<%= Config.FABAN_HOST %>]</title>
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
        <link rel="icon" type="image/gif" href="img/faban.gif">
    </head>
    <body>
        <br>
        <%
            Subject user = usrEnv.getSubject();
            String[][] pending = RunQ.getHandle().listRunQ();
            if (!(AccessController.isSubmitAllowed(user) || AccessController.isManageAllowed(user))) {
        %>
                <br/>
                <br/>
                <br/>
                <b><center>Permission Denied.</center></b>
        <%
            } else if ((pending != null) && (pending.length > 0)) {
                boolean form = false;
                boolean[] killAllowed = new boolean[pending.length];
                for (int i = 0; i < killAllowed.length; i++) {
                    killAllowed[i] = AccessController.isKillAllowed(
                            user, pending[i][1] + '.' + pending[i][0]);
                    if (killAllowed[i])
                        form = true;
                }
                if (form) {
        %>
                <form  method="post" action="delete-runs.jsp">
        <%      } %>
                    <center>
                    <table BORDER=0 CELLPADDING=4 CELLSPACING=3 width="80%" align="center" style="padding:2px; border: 2px solid #cccccc;">
                    <tbody>
                    <tr>
                        <th class="header">Run ID</th>
                        <th class="header">Benchmark</th>
                        <th class="header">Description</th>
                    </tr>
        <%
                final String[] rowType = {"even", "odd"};
                for(int i=0; i < pending.length; i++) {
                    String runqDir = pending[i][1] + "." + pending[i][0];

        %>
                    <tr class="<%=rowType[i % 2]%>">
                        <td class="tablecell" style="text-align: right;">
        <%
                        if (killAllowed[i]) {
        %>
                            <input type="checkbox" name="selected-runs" value="<%=runqDir %>">
        <%
                        }
        %>
                            <%= pending[i][0] %></td>
                        <td class="tablecell"><%= pending[i][1]%></td>
                        <td class="tablecell"><%= pending[i][2]%></td>
                    </tr>
         <%     } %>
                 </tbody>
                 </table>
         <%     if (form) { %>
                 <br>
                 <br>
                 <center>
                 <input type="submit" value="Remove">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                 <input type="reset"></center>
                 </center>
                </form>
        <%      }
            } else {
        %>
                <br/>
                <br/>
                <br/>
                <b><center>There are no pending runs.</center></b>
        <%  } %>
    </body>
</html>
