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
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
-->
    <%@ page language="java" import="com.sun.faban.harness.webclient.RunResult,
                                     com.sun.faban.common.SortableTableModel,
                                     java.io.File,java.io.*,
                                     java.util.Set,java.util.*,
                                     java.net.URLEncoder,
                                     com.sun.faban.harness.common.Config"%>
    <jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
    <%
    response.setHeader("Cache-Control", "no-cache");
    String[] header = (String[])request.getAttribute("header");
    String[] row = (String[])request.getAttribute("runinfo");
    if (row != null) {
    %>


<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Sheetal Patil"/>
        <title>Run Info Read Only [<%= Config.FABAN_HOST %>]</title>
        <link rel="icon" type="image/gif" href="/img/faban.gif"/>
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
    </head>
    <body>
        <table BORDER=0 CELLPADDING=4 CELLSPACING=3 style="padding:2px; border: 2px solid #cccccc;">
            <tbody>
                <% for (int j = 0; j < row.length; j++) {%>
                <tr <%if (j % 2 == 0) {%>class="even"<%} else {%>class="odd"<% }%>>
                    <td class="tablecell"><%= header[j]%></td>
                    <td class="tablecell" id="the_td<%= j %>"><%=row[j]%></td>
                </tr>
                <% }%>
            </tbody>
        </table>
    <%} %>
    </body>
</html>
