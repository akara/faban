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
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
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
        <title>Run Info [<%= Config.FABAN_HOST %>]</title>
        <link rel="icon" type="image/gif" href="/img/faban.gif"/>
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
        <script type="text/javascript">
            function editText(id) {
                var td = document.getElementById(id);
                var content = td.innerHTML;
                td.innerHTML = '<div id="divid"><textarea id="txtarea" class="editing" >'+ content +'</textarea><br><input type="button" id="save" onclick="saveEdit(\'' + id + '\', \'txtarea\')" value="Save" /> <input type="button" id="cancel" onclick="cancelEdit(\'' + id + '\', \'' + content + '\')" value="Cancel" /></div>';
            }

            function saveEdit(td_id, textarea) {
                var td = document.getElementById(td_id);
                td.innerHTML = document.getElementById(textarea).value;
                if(td_id == "Tags"){
                    updateTags(td.innerHTML);
                }
                if(td_id == "Description"){
                    updateDescription(td.innerHTML);
                }

            }

            function cancelEdit(td_id, content) {
                var td = document.getElementById(td_id);
                td.innerHTML = content;
            }

            var updateTagsURL = "/controller/uploader/update_tags_file?tags=";
            function updateTags(content) {
                http.open("GET", updateTagsURL + escape(content) + "&runId=" + escape('<%=row[0]%>'), true);
                http.send(null);
            }

            var updateDescURL = "/controller/uploader/update_run_desc?desc=";
            function updateDescription(content) {
                http.open("GET", updateDescURL + escape(content) + "&runId=" + escape('<%=row[0]%>'), true);
                http.send(null);
            }

            function getHTTPObject() {
                var xmlhttp;
                if (!xmlhttp && typeof XMLHttpRequest != 'undefined') {
                    try {
                        xmlhttp = new XMLHttpRequest();
                    } catch (e) {
                        xmlhttp = false;
                    }
                }
                return xmlhttp;
            }
            var http = getHTTPObject();
        </script>
    </head>
    <body>
        <div id="edit"></div>
        <table BORDER=0 CELLPADDING=4 CELLSPACING=3 style="padding:2px; border: 2px solid #cccccc; width:250px; height:100px;">
            <tbody>
                <% for (int j = 0; j < row.length; j++) {%>
                <tr <%if (j % 2 == 0) {%>class="even"<%} else {%>class="odd"<% }%>>
                    <td style="border-bottom: 1px solid #C1DAD7; border-left: 1px solid #C1DAD7; font-size: 12px; font-family: 'Times New Roman',Times,serif;"><%= header[j]%></td>
                    <%if( header[j].equals("Description") || header[j].equals("Tags")){%>
                        <td id="<%= header[j] %>" ondblclick="editText('<%= header[j] %>');" style="border-bottom: 1px solid #C1DAD7; border-left: 1px solid #C1DAD7; font-size: 12px; font-family: 'Times New Roman',Times,serif;"><%=row[j]%></td>
                    <%}else{%>
                        <td id="<%= header[j] %>" style="border-bottom: 1px solid #C1DAD7; border-left: 1px solid #C1DAD7; font-size: 12px; font-family: 'Times New Roman',Times,serif;"><%=row[j]%></td>
                    <%}%>
                </tr>
                <%      }%>
            </tbody>
        </table>
    <%} %>
    </body>
</html>
