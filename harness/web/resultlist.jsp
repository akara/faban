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
 * $Id: resultlist.jsp,v 1.11 2009/02/17 22:39:55 sheetalpatil Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
    <%@ page language="java" import="com.sun.faban.harness.webclient.Result,
                                     com.sun.faban.harness.webclient.TableModel,
                                     com.sun.faban.harness.webclient.TagEngine,
                                     java.util.concurrent.locks.ReentrantReadWriteLock,
                                     java.util.concurrent.locks.*,
                                     java.io.File,java.io.*,
                                     java.util.Set,java.util.*,
                                     com.sun.faban.harness.common.Config"%>
    <jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
    <%
    response.setHeader("Cache-Control", "no-cache");
    String tag = request.getParameter("inputtag");
    String processString = request.getParameter("action");
    if ("Compare".equals(processString) ||
        "Average".equals(processString)) {
        RequestDispatcher rd = request.getRequestDispatcher("/analyzeruns.jsp");
        rd.forward(request, response);
    } else if ("Archive".equals(processString)) {
        RequestDispatcher rd = request.getRequestDispatcher("/archiveruns.jsp");
        rd.forward(request, response);
    } else {
        TableModel resultTable = null;
        ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
        Lock rlock = rwl.readLock();
        if(tag != null) {
           TagEngine te = new TagEngine();
           File filename = new File(Config.OUT_DIR + "/tagenginefile");
           if (filename.exists()) {
               ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename));
               te = (TagEngine) in.readObject();
               in.close();
           }
           Set<String> answer = null;
           if (rlock.tryLock()) {
              try {
                  answer = te.search(tag.trim());
              } finally {
                  rlock.unlock();
              }
           } 
           resultTable = Result.getTagSearchResultTable(answer,te);
        }else{
           resultTable = Result.getResultTable(usrEnv.getSubject());
        }
        int rows = resultTable.rows();
        if(resultTable != null && rows > 0) {
    %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Ramesh Ramachandran"/>
        <meta name="Description" content="Reults JSP"/>
        <title>Benchmark results</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
    </head>
    <body>
            <form name="processrun" method="post" action="controller/result_action/take_action">
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
              <table cellpadding="2" cellspacing="0" border="1" width="90%" align="center">
              <tbody>
              <tr>
                  <th>&nbsp;</th>
    <%      for (int i = 0; i < resultTable.columns(); i++) { %>
                  <th><%= resultTable.getHeader(i) %></th>
    <%      } %>
              </tr>
    <%
            for(int i = 0; i < rows; i++) {
                Comparable[] row = resultTable.getRow(i);
    %>
            <tr>
                <td><input type="checkbox" name="select" value="<%= row[0] %>"></input></td>
    <%          for (int j = 0; j < row.length; j++) { %>
                <td><%= row[j] %></td>
    <%          } %>
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
        }
        else {
            %>
            <br/>
            <center><b>There are no results</b></center>
            <br/>
            <%
        }
    }
    %>
    </body>
</html>
