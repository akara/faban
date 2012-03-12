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
    <%@ page language="java" import="com.sun.faban.harness.webclient.RunResult,
                                     com.sun.faban.common.SortableTableModel,
                                     com.sun.faban.common.SortDirection,
                                     com.sun.faban.harness.webclient.TagEngine,
                                     java.util.StringTokenizer,
                                     java.io.File,java.io.*,
                                     java.util.Set,java.util.*,
                                     java.net.URLEncoder,
                                     com.sun.faban.harness.common.Config"%>
    <jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
    <%
    response.setHeader("Cache-Control", "no-cache");
    SortableTableModel resultTable = (SortableTableModel)request.getAttribute("table.model");
    String feedURL = (String)request.getAttribute("feedURL");
    String tagInSearch = (String)request.getAttribute("tagInSearch");
    String sortDirection = "DESCENDING";
    //String sort = "<img src=/img/sort_desc.gif></img>";
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
                <input type="submit" name="process" value="Delete">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
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
              <table id="ResultTable" BORDER=0 CELLPADDING=4 CELLSPACING=3 width="90%" align="center" style="padding:2px; border: 2px solid #cccccc;">
              <tbody>
              <tr>
                  <th class="header">&nbsp;</th>
    <%      // Last column (column 8) contaiuns the URL to the results.
            for (int i = 0; i < resultTable.columns() - 1; i++) {
                  if(resultTable.getSortDirection() == SortDirection.DESCENDING){
                      sortDirection = "ASCENDING";
                  }else if(resultTable.getSortDirection() == SortDirection.ASCENDING){
                      sortDirection = "DESCENDING";
                  }
                  String sortLink = "/controller/results/list?sortColumn=" + i + "&sortDirection=" + sortDirection.trim();
                  if(tagInSearch != null && tagInSearch.length() > 0)
                      sortLink = "/controller/results/list?inputtag="+ tagInSearch.trim() +"&sortColumn=" + i + "&sortDirection=" + sortDirection.trim();
    %>
                  <th class="header"><a href="<%= sortLink %>" target="main"><%= resultTable.getHeader(i)%></a></th>
    <%      } %>
              </tr>
    <%
            final String[] rowClasses = { "even", "odd" };
            for(int i = 0; i < rows; i++) {
                Comparable[] row = resultTable.getRow(i);
                String rowClass = rowClasses[i % 2];
    %>
            <tr class="<%= rowClass %>" onmouseover="this.className='highlight'"
                 onmouseout="this.className='<%= rowClass %>'">
                <td class="tablecell" ><input type="checkbox" name="select" value="<%= row[0] %>"></input></td>
    <%          String linkURL = row[8].toString();
                String onclick = "";
                if (linkURL != null && linkURL.length() > 0)
                    onclick = "onclick=\"location.href='" + linkURL + "'\"";
                for (int j = 0; j < row.length - 1; j++) { // Again, last is URL
                    String mouseover = " ";                    
                    if(row[j] == null)
                       row[j] = " ";
                    String val = row[j].toString();
                    if(j==0 || j==1 || "&nbsp;".equals(row[j].toString()) || "&nbsp".equals(row[j].toString())){%>
                       <td class="tablecell" <%= onclick %>><%=val%></td>
                    <%} else if (j == 2) { %>                             
                             <td class="tablecell" <%= onclick %> <%= mouseover%>><%=val%></td>
                    <%}else{                      
                         if(row[j] != null) {
                             StringBuilder formattedStr = new StringBuilder();
                             StringTokenizer t = new StringTokenizer(row[j].toString());
                             val = t.nextToken().trim();
                             while (t.hasMoreTokens()) {
                                formattedStr.append(t.nextToken().trim() + " ");
                             }
                             mouseover = "onmouseover=\"showtip('" + val + " " + formattedStr.toString()+ "')\" onmouseout=\"hideddrivetip()\"";
                             %>
                             <%if (j == (row.length - 2)) { // The tag column %>
                                 <% if (row[j].toString().length() > val.length()){ %>
                                    <td class="tablecell" <%= onclick %> <%= mouseover%>><%=val%>.....</td>
                                 <% }else {%>
                                    <td class="tablecell" <%= onclick %>><%=val%></td>
                             <%  }
                             } else {%>
                                <td class="tablecell" <%= onclick %> <%= mouseover%>><%=val%></td>
                             <% }
                         }%>
   <%                }
                } %>
            </tr>
    <%      } %>
     </tbody>
     </table>
     <br/>
     <br/>
     <center>
     <input type="submit" name="process" value="Delete">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
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
