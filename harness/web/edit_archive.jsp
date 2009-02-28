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
 * $Id: edit_archive.jsp,v 1.5 2009/02/28 04:34:16 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
<%@ page language="java" import="com.sun.faban.harness.webclient.ResultAction"
 %>
 <% ResultAction.EditArchiveModel model = (ResultAction.EditArchiveModel)
                                    request.getAttribute("editarchive.model");

 %>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Akara Sucharitakul"/>
        <meta name="Description" content="Edit Archive JSP"/>
        <title>Archive Runs</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
    </head>
    <body>
    <h2><center>Archive Runs to <%=model.head%></center></h2>
    <form name="analyzename" method="post" action="/controller/result_action/archive">
    <table border="1" cellpadding="2" cellspacing="0"
     style="width: 80%; text-align: left; margin-left: auto; margin-right: auto;">
      <tbody>
        <tr>
          <th style="vertical-align: top;">RunId</th>
          <th style="vertical-align: top;">Tags</th>
          <th style="vertical-align: top;">Description</th>
        </tr>
        <% for (int i = 0; i < model.runIds.length; i++) {
            String runId = model.runIds[i];
        %>
            <tr>
              <td style="vertical-align: top;"><%=runId%>
                <input type="hidden" name="select" value="<%=runId%>"/><br>
     <%
            if (model.duplicates.contains(runId)) {
     %>
            <br/><input type="checkbox" name="replace"
                title="Check this box if you want to replace the old archive."
                value="<%=runId%>">Run already archived. Replace?</input>
     <%
            }
     %>
              </td>
              <td style="vertical-align: top;">
                  <textarea name="<%=runId%>_tags"
                       title="Tags associated for run <%=runId%>"
                       rows="2" style="width: 98%;"
                       ><%=model.results[i].tags%></textarea>
              </td>
              <td style="vertical-align: top;">
                <textarea name="<%=runId%>_description"
                       title="Input/modify the description of run <%=runId%>"
                       rows="3" style="width: 98%;"
                       ><%=model.results[i].description%></textarea>
              </td>
            </tr>
       <% } %>
      </tbody>
    </table><br>
     <%for (String dup : model.duplicates){%>
        <input type="hidden" name="duplicates" value="<%=dup%>"/><br>
     <%}%>
    <center><input type="submit" name="process" value="Archive"
            >&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="reset"></center>
    </form>
    </body>
</html>
