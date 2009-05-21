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
 * $Id: resultlist.jsp,v 1.20 2009/05/21 10:07:32 sheetalpatil Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
    <%@ page language="java" import="com.sun.faban.harness.webclient.RunResult,
                                     com.sun.faban.harness.webclient.TableModel,
                                     com.sun.faban.harness.webclient.TagEngine,
                                     java.util.StringTokenizer,
                                     java.io.File,java.io.*,
                                     java.util.Set,java.util.*,
                                     java.net.URLEncoder,
                                     com.sun.faban.harness.common.Config"%>
    <jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
    <%
    response.setHeader("Cache-Control", "no-cache");
    TableModel resultTable = (TableModel)request.getAttribute("table.model");
    String feedURL = (String)request.getAttribute("feedURL");
    
    int rows;
    if (resultTable != null && (rows = resultTable.rows()) > 0) {
    %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Ramesh Ramachandran"/>
        <meta name="Description" content="Reults JSP"/>
        <title>Benchmark Results [<%= Config.FABAN_HOST %>]</title>
        <link rel="icon" type="image/gif" href="/img/faban.gif"/>
        <link rel="alternate" type="application/atom+xml" title="Atom Feed" href="<%= feedURL %>"/>
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
        <link rel="stylesheet" type="text/css" href="/css/balloontip2.css" />
        <script type="text/javascript" src="/scripts/balloontip2.js"></script>
    </head>
    <body>
        <div style="text-align: right;"><a
             href="<%= feedURL %>"><img
             style="border: 0px solid ; width: 16px; height: 16px;"
             alt="Feed" src="/img/feed.gif"></a></div>

            <form name="processrun" method="post" action="/controller/result_action/take_action">
              <center>
                <input type="submit" name="process" value="Compare">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                <!-- Commented out until FenXi supports averaging again.
                <input type="submit" name="process" value="Average">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                -->
    <%
            boolean allowArchive = false;
            if (Config.repositoryURLs != null &&
                Config.repositoryURLs.length > 0)
                allowArchive = true;

            if (allowArchive) {
    %>
                <input type="submit" name="process" value="Archive">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
    <%
            }
    %>
                <input type="reset">
              </center>
              <br>
              <table BORDER=0 CELLPADDING=4 CELLSPACING=3 width="90%" align="center" style="padding:2px; border: 2px solid #cccccc;">
              <tbody>
              <tr>
                  <th style="font-size: 12px; font-family: 'Times New Roman',Times,serif;" class="header">&nbsp;</th>
    <%      for (int i = 0; i < resultTable.columns(); i++) { %>
                  <th style="font-size: 12px; font-family: 'Times New Roman',Times,serif;" class="header"><%= resultTable.getHeader(i) %></th>
    <%      } %>
              </tr>
    <%
            for(int i = 0; i < rows; i++) {
                Comparable[] row = resultTable.getRow(i);
    %>
            <tr <%if(i % 2 == 0){%>class="even"<%}else{%>class="odd"<% } %>>
                <td style="border-bottom: 1px solid #C1DAD7; border-left: 1px solid #C1DAD7; font-size: 12px; font-family: 'Times New Roman',Times,serif;"><input type="checkbox" name="select" value="<%= row[0] %>"></input></td>
    <%          for (int j = 0; j < row.length; j++) { 
                    if (row[j] == null)
                        row[j] = " ";
                    StringBuilder formattedStr = new StringBuilder();
                    StringTokenizer t = new StringTokenizer(row[j].toString(),"\n ,:;");
                    String val = t.nextToken().trim();
                    while (t.hasMoreTokens()) {
                            formattedStr.append(t.nextToken().trim() + " ");
                    }
                    String mouseover = "onmouseover=\"showtip('" + val + " " + formattedStr.toString()+ "')\" onmouseout=\"hideddrivetip()\"";
                    if(j==0 || j==1 || j==2 || j==5 || row[j].toString().equals("&nbsp;") || row[j].toString().equals("&nbsp")){%>
                       <td style="border-bottom: 1px solid #C1DAD7; border-left: 1px solid #C1DAD7; font-size: 12px;
                        font-family: 'Times New Roman',Times,serif;"><%=row[j]%></td>
                    <%}else{%>
                         <% //if(row[j].toString().length() > 15)
                               //row[j] = row[j].toString().substring(0, 14) + ".....";
                         %>
                         <td style="border-bottom: 1px solid #C1DAD7; border-left: 1px solid #C1DAD7; font-size: 12px; font-family: 'Times New Roman',Times,serif;" <%= mouseover%>><%=val%></td>
    <%                }
                } %>
            </tr>
    <%      } %>
     </tbody>
     </table>
     <br/>
     <br/>
     <center>
     <input type="submit" name="process" value="Compare">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     <!-- Commented out until FenXi supports averaging again.
     <input type="submit" name="action" value="edit_average>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
     -->
    <%
            if (allowArchive) {
    %>
                <input type="submit" name="process" value="Archive">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
    <%
            }
    %>
     <input type="reset">
     </center>
    </form>
    <%
    } else {
    %>
            <br/>
            <center><b>There are no results</b></center>
            <br/>
    <%
    }
    %>
    </body>
</html>
