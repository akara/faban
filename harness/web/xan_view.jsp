<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<%--
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
 *  Copyright(c) 2010-2012 Shanti Subramanyam. All Rights Reserved.
 */
 This view displays xan files - both tables and graphs. It uses jqplot
--%>
<%@ page language="java" import="com.sun.faban.harness.webclient.View,
                                 java.util.StringTokenizer,
                                 java.io.File,java.io.*,
                                 java.util.Set,java.util.*,
                                 java.net.URLEncoder,
                                 java.text.*,
                                 com.sun.faban.harness.common.Config"%>
<%
    response.setHeader("Cache-Control", "no-cache");
    View.Xan xan = (View.Xan)request.getAttribute("model");
    Boolean tblOnly = (Boolean)request.getAttribute("tblOnly");
    if (tblOnly == null)
        tblOnly = false;
    String[] rowClasses = {"even", "odd"};
%>

<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title><%= xan.title %></title>
    <link rel="icon" type="image/gif" href="/img/faban.gif"/>
    <link rel="stylesheet" type="text/css" href="/css/style.css" />
    <link rel="stylesheet" type="text/css" href="/css/xanview.css"/>
    <!--[if lt IE 9]><script language="javascript" type="text/javascript" src="/scripts/excanvas.min.js"></script>><![endif]-->
    <script language="javascript" type="text/javascript" src="/scripts/jquery.min.js"></script>
    <script language="javascript" type="text/javascript" src="/scripts/jquery.jqplot.min.js"></script>
    <script type="text/javascript" src="/scripts/jqplot.cursor.min.js"></script>
    <script type="text/javascript" src="/scripts/jqplot.highlighter.min.js"></script>
    <script language="javascript" type="text/javascript" src="/scripts/jqplot.canvasTextRenderer.min.js"></script>
    <script language="javascript" type="text/javascript" src="/scripts/jqplot.canvasAxisLabelRenderer.min.js"></script>

    <script language="javascript" type="text/javascript" src="/scripts/jqplot.dateAxisRenderer.min.js"></script>
    <link rel="stylesheet" type="text/css" href="/css/jquery.jqplot.css" />
    <style>

    .message {
        padding-left: 50px;
        font-size: smaller;
    }
    </style>
  </head>
  <body>
    <h1 class="page_title" align="center"><%= xan.title %></h1>
    <div id="toc">
    <h2><a name="top"></a>Contents</h2>
    <ul>
        <% for (int id = 0; id < xan.sections.size(); id++) {
            View.Section section = xan.sections.get(id);
        %>
        <li><a href="#<%= id %>"><%= section.name %></a></li>
        <% } %>
    </ul>
    </div>
    <br/>
    <% for (int id = 0; id < xan.sections.size(); id++) {
        View.Section section = xan.sections.get(id);
        if (section.link != null){ //link
        %>
            <h2><a name="<%= id %>" href="<%= section.link %>"><%= section.name %></a></h2>
     <% } else {%>
            <h2><a name="<%= id %>"></a><%= section.name %></h2>
    <% }
       if ("line".equalsIgnoreCase(section.display)) {
    %>
        <div id="graph<%= id %>" style="width: 600px; height: 300px; position: relative;">
        </div>
    <% } else { %>
    <table BORDER=0 CELLPADDING=4 CELLSPACING=3 style="padding:2px; border: 2px solid #cccccc;">
        <tr>
        <% if (section.headers != null) {
                for (String header : section.headers) { %>
                    <th class="header"><%= header %></th>
             <% }
           } %>
        </tr>
        <% for (int i = 0; i < section.rows.size(); i++) {
               List<String> row = section.rows.get(i);
        %>
        <tr class="<%= rowClasses[i % 2]%>">
            <%  boolean boldface = false;
                for (String entry : row) {
                   if (entry.startsWith("\\")) { // Escaped start, just remove.
                       entry = entry.substring(1);
                   }
            %>
            <td class="tablecell"><%= entry %></td>
            <% } %>
        </tr>
        <% } %>
    </table>
    <% } %>
    <div class="prevnext">
      <a href="#<%= id - 1 %>">Previous</a> | <a href="#<%= id + 1 %>">Next</a> |
      <a href="#top">Top</a>
    </div>
    <p><br/></p>
    <% } %>

    <script id="source" language="javascript" type="text/javascript">
$(function () {
    <% for (int id = 0; id < xan.sections.size(); id++) {
        View.Section section = xan.sections.get(id);
        int numCols = section.headers.size();
        if (!"line".equalsIgnoreCase(section.display))
            continue;
        // Get data for each series (column). Ignore x-axis, col 0
        for (int col=1; col < numCols; col++) {
    %>
            var data<%= id+1 %><%= col %> = <%= section.json.get(col-1) %>;
        <% } %>
        var xlabel<%= id %> = "<%= section.headers.get(0) %>";
        var dataset = [<%= section.dataName %>];
        var minx = <%= section.min %>;
        var maxx = <%= section.max %>;
        $.jqplot("graph<%= id %>", dataset,  {
            title: "<%= section.name %>",
            axesDefaults: {
                    labelRenderer: $.jqplot.CanvasAxisLabelRenderer
            },
            seriesDefaults: {
                lineWidth: 1.75,
                location: 'e',
                placement: 'outside',
                rendererOptions: {
                    smooth: true
                }
            },
            series: [
            <% for (int j=1; j < numCols; j++) {
                if (j == 1) {
            %>
                    { label: "<%= section.headers.get(j) %>", markerOptions:{show:false} }
                <% } else { %>
                    , { label: "<%= section.headers.get(j) %>", markerOptions:{show:false} }
                <% }
             }%>

            ],
            axes: {
                xaxis: {
                label: xlabel<%= id %>,
                <% if  (section.xIsTime == 1) {
                %>
                    renderer: $.jqplot.DateAxisRenderer,
                    tickOptions: {formatString:'%T'},
                    min: minx,
                    max: maxx,
                    pad: 1.3
                <% } %>
                }
            },
            cursor: {
                show: true, showTooltip: true,
                followMouse: true,
                zoom: true
            },
            legend: {
                show: true
            }
        });
        //Hack to prevent jqplot from changing frame name in Chrome
        if(window.name="y9axis"){
            window.name="display";
        }
    <% } %>
});
</script>
  </body>
</html>
