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
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
<!--
    Document   : targetlist
    Created on : Jul 22, 2009, 4:16:14 PM
    Author     : sp208304
-->

    <%@ page language="java" import="com.sun.faban.harness.webclient.RunResult,
                                     com.sun.faban.common.SortableTableModel,
                                     com.sun.faban.common.SortDirection,
                                     com.sun.faban.harness.webclient.TagEngine,
                                     java.util.StringTokenizer,
                                     java.io.File,java.io.*,
                                     java.util.Set,java.util.*,
                                     java.net.URLEncoder,
                                     java.text.*,
                                     com.sun.faban.harness.common.Config"%>
    <jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
    <%
    response.setHeader("Cache-Control", "no-cache");
    String answer = (String)request.getAttribute("answer");
    String viewAll = (String)request.getAttribute("viewAll");
    String viewMy = (String)request.getAttribute("viewMy");
    SortableTableModel targetTable = (SortableTableModel)request.getAttribute("table.model");
    String targetInSearch = (String)request.getAttribute("targetInSearch");
    String sortDirection = "DESCENDING";
    int rows;
    %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Ramesh Ramachandran"/>
        <meta name="Description" content="Reults JSP"/>
        <title>Targets List [<%= Config.FABAN_HOST %>]</title>
        <link rel="icon" type="image/gif" href="/img/faban.gif"/>
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
        <link rel="stylesheet" type="text/css" href="/css/balloontip2.css" />
        <script type="text/javascript" src="/scripts/balloontip2.js"></script>
    </head>
    <body>
            <table CELLSPACING=25>
                <tr>
                    <td ><form name="targetsearch" method="get" target="main" action="/controller/results/targetlist">
                               Target Search
                                <% if(targetInSearch != null){ %>
                               <input type="text" name="inputtarget" title="target search" size="15" value=<%=targetInSearch%>>
                                <% }else{ %>
                                <input type="text" name="inputtarget" title="target search" size="15">
                                <%}%>
                               <input type="submit" value="Search">&nbsp;&nbsp;<input type="reset" value="Reset">
                               <input type="hidden" name="viewAll" value="true">
                               <input type="hidden" name="viewMy" value=<%=viewMy%>>
                           </form>
                    </td>
                     <% if(usrEnv.getUser() != null ) { %>
                        <% if(viewAll.equals("true")) { %>
                              <td><a href="/controller/results/targetlist?viewAll=null&viewMy=<%=viewMy%>" target="main">View All Targets</a></td>
                        <% }else{ %>
                                <% if(!viewMy.equals("disable")) { %>
                                    <td><a href="/controller/results/targetlist?viewAll=true&viewMy=<%=viewMy%>" target="main">View My Targets</a></td>
                                <%}%>
                        <%}%>
                              <td ><a href="/addtarget.jsp" target="main">Add Target</a></td>
                    <%}%>
                </tr>
            </table>
            <hr/>

              <% if(answer != null) { %>
                    <%=answer%>
                    <br><br>
              <% }
    if (targetTable != null && (rows = targetTable.rows()) > 0) {
    %>

              <table BORDER=0 CELLPADDING=4 CELLSPACING=3 width="90%" align="center" style="padding:2px; border: 2px solid #cccccc;">
              <tbody>
              <tr>
    <%      for (int i = 0; i < (targetTable.columns()-3); i++) {
                  if(targetTable.getSortDirection() == SortDirection.DESCENDING){
                      sortDirection = "ASCENDING";
                  }else if(targetTable.getSortDirection() == SortDirection.ASCENDING){
                      sortDirection = "DESCENDING";
                  }
                  String sortLink = "/controller/results/targetlist?viewMy=" + viewMy + "&viewAll="+ viewAll+ "&sortColumn=" + i + "&sortDirection=" + sortDirection.trim();
                  if(targetInSearch != null && targetInSearch.length() > 0)
                      sortLink = "/controller/results/targetlist?viewMy=" + viewMy + "&viewAll="+ viewAll+ "&inputtarget="+ targetInSearch.trim() +"&sortColumn=" + i + "&sortDirection=" + sortDirection.trim();
    %>
                  <th class="header"><a href="<%= sortLink %>" target="main"><%= targetTable.getHeader(i)%></a></th>
    <%      } %>
              </tr>
    <%
            String[] rowClasses = { "even", "odd" };
            for(int i = 0; i < rows; i++) {
                Comparable[] row = targetTable.getRow(i);
    %>
            <tr class="<%= rowClasses[i % 2]%>">

    <%          String sortLink = "";
                if(row[4] != null){
                    String tagsInSearch = row[5].toString();
                    sortLink = "/controller/results/list?inputtag="+ tagsInSearch.trim() +"&sortColumn=" + 4 + "&sortDirection=" + SortDirection.DESCENDING;
                }
                for (int j = 0; j < (row.length-3); j++) {
                    int red, orange, yellow;
                    if(row[6] != null){
                        red = Integer.parseInt(row[6].toString());
                    }else{
                        row[6] = "60";
                        red = Integer.parseInt(row[6].toString());
                    }
                    if(row[7] != null){
                        orange = Integer.parseInt(row[7].toString());
                    }else{
                        row[7] = "70";
                        orange = Integer.parseInt(row[7].toString());
                    }
                    if(row[8] != null){
                        yellow = Integer.parseInt(row[8].toString());
                    }else{
                        row[8] = "100";
                        yellow = Integer.parseInt(row[8].toString());
                    }
                    if(row[j] == null)
                       row[j] = " ";
                    String val = row[j].toString();
                    if(j == 0) {
                        String metric = row[4].toString().substring(0, row[4].toString().lastIndexOf(" ")).trim();
                        String unit = row[4].toString().substring(row[4].toString().lastIndexOf(" "),row[4].toString().length()).trim();
                        String addTargetLink = "/addtarget.jsp?targetname=" + row[0].toString() + "&targetowner=" + row[1].toString() + "&targetmetric=" + metric + "&targetmetricunit=" + unit + "&targettags=" + row[5].toString() + "&targetcolorred=" + row[6].toString() + "&targetcolororange=" + row[7].toString() + "&targetcoloryellow=" + row[8].toString();
                        String deleteTargetLink = "/controller/results/add_edit_target?viewMy=" + viewMy + "&viewAll="+ viewAll+ "&flag=delete&targetname=" + row[0].toString()+ "&targetowner=" + row[1].toString() + "&targetmetric=" + row[4].toString() + "&targettags=" + row[5].toString();
        %>
        <% if(row[1].toString().equalsIgnoreCase(usrEnv.getUser())){ %>
                <td class="tablecell"><%=val%>&nbsp;&nbsp;
                    (<a href="<%= addTargetLink %>" target="main">Edit</a>&nbsp;
                    <a href="<%= deleteTargetLink %>" onclick="return confirm('You are about to delete a target. Are you sure?');" target="main">Delete</a>)
                </td>
        <% }else{ %>
                <td class="tablecell"><%=val%></td>
        <%      }
                   }else if(j == 2){
                        double v = Double.parseDouble(val);
        %>
                        <% if (v == 0d) { // No results %>
                            <td bgcolor="black" style="color: white;" class="tablecell"><%out.print(new DecimalFormat("#").format(Double.parseDouble(val)) + "%"); %></td>
                        <% } else if (v < red) {%>
                            <td bgcolor="red" style="color: white;" class="tablecell"><%out.print(new DecimalFormat("#").format(Double.parseDouble(val)) + "%"); %></td>
                        <% } else if (v >= red && v < orange) { %>
                            <td bgcolor="orange" class="tablecell"><%out.print(new DecimalFormat("#").format(Double.parseDouble(val)) + "%"); %></td>
                        <% } else if (v >= orange && v < yellow) { %>
                            <td bgcolor="yellow" class="tablecell"><%out.print(new DecimalFormat("#").format(Double.parseDouble(val)) + "%"); %></td>
                        <% } else if (v >= yellow) {%>
                            <td bgcolor="#00cc00" class="tablecell"><%out.print(new DecimalFormat("#").format(Double.parseDouble(val)) + "%"); %></td>
                        <% } %>
    <%              } else if(j == 3){
    %>
                        <td class="tablecell"><a href="<%= sortLink %>" target="main"><%=val%></a></td>
    <%              } else { %>
                        <td class="tablecell"><%=val%></td>
    <%              }
                }      %>
            </tr>
    <%      } %>
     </tbody>
     </table>
     <br/>
     <br/>
    <%
    } 
    %>
    </body>
</html>
