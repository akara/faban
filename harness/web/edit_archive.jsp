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
<%@ page language="java" import="com.sun.faban.harness.common.Config,
                                 com.sun.faban.harness.webclient.ResultAction"
 %>
 <% ResultAction.EditArchiveModel model = (ResultAction.EditArchiveModel)
                                    request.getAttribute("editarchive.model");

 %>

<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Akara Sucharitakul"/>
        <meta name="Description" content="Edit Archive JSP"/>
        <title>Archive Runs [<%= Config.FABAN_HOST %>]</title>
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
        <link rel="icon" type="image/gif" href="img/faban.gif">
    </head>
    <body>
    <h2><center>Archive Runs to <%=model.head%></center></h2>
    <form name="analyzename" method="post" action="/controller/result_action/archive">
    <table  BORDER=0 CELLPADDING=4 CELLSPACING=3 width="80%" align="center"
            style="padding:2px; border: 2px solid #cccccc;">
      <tbody>
        <tr>
          <th class="header">RunId</th>
          <th class="header">Tags</th>
          <th class="header">Description</th>
        </tr>
        <%
        final String[] rowType = {"even", "odd"};
        for (int i = 0; i < model.runIds.length; i++) {
            String runId = model.runIds[i];
            String tags = null;
            if (model.results[i].tags != null && model.results[i].tags.length > 0) {
                StringBuilder b = new StringBuilder();
                for (String tag : model.results[i].tags) {
                    b.append(tag).append(' ');
                }
                b.setLength(b.length() - 1);
                tags = b.toString();
                b.setLength(0);
            }
        %>
            <tr class="<%=rowType[i % 2]%>">
              <td style="vertical-align: top;" class="tablecell"><%=runId%>
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
              <td style="vertical-align: top;" class="tablecell">
                  <textarea name="<%=runId%>_tags"
                       title="Tags associated for run <%=runId%>"
                       rows="2" style="width: 98%;"
                       ><% if(tags != null && !"".equals(tags)){ %><%=tags%><% } %></textarea>
              </td>
              <td style="vertical-align: top;" class="tablecell">
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
