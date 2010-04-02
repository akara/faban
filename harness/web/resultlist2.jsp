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
                                 com.sun.faban.harness.webclient.RunResult,
                                 com.sun.faban.harness.webclient.TableModel"%>
<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Akara Sucharitakul"/>
        <meta name="Description" content="Results Table"/>
        <title>Benchmark Results [<%= Config.FABAN_HOST %>]</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
<%  TableModel resultTable = RunResult.getResultTable(usrEnv.getSubject());
    int pageSize = 10;
    String tableName = "result_table";
    if(resultTable != null && resultTable.rows() > 0) {
        session.setAttribute(tableName, resultTable);
        int rows = resultTable.rows();

%>
        <script src="scripts/prototype.js" type="text/javascript"></script>
        <script src="scripts/rico.js" type="text/javascript"></script>
        <script src="scripts/ricoDragAndDropCustomDraggable.js" type="text/javascript"></script>
    </head>
<body onload="javascript:bodyOnLoad()">
	<script>
	   var onloads = new Array();
	   function bodyOnLoad() {
	      // new Rico.Effect.Round( null, 'roundNormal' );
	      // new Rico.Effect.Round( null, 'roundCompact', {compact:true} );
	      for ( var i = 0 ; i < onloads.length ; i++ )
	         onloads[i]();
	   }
	   // showMenuContext()
	</script>

<style>
.fixedTable {
   table-layout : fixed;
}

td.cell {
    padding       : 2px 0px 2px 3px;
    margin        : 0px;
    border-bottom : 1px solid #b8b8b8;
    border-right  : 1px solid #b8b8b8;
    height        : 22px;
    overflow      : hidden;
    font-size     : 11px;
    font-family: verdana, arial, helvetica, sans-serif;
    line-height: 12px;
}

.first {
   border-left  : 1px solid #b8b8b8;
}

.tableCellHeader {
   padding          : 2px 0px 2px 3px;
   text-align       : left;
   font-size        : 11px;
   border-top       : 1px solid #b8b8b8;
   border-right     : 1px solid #b8b8b8;
   background-color : #cedebd;
}

.listBox {
   padding-top      : 5px;
   padding-bottom   : 5px;
   background-color : #ffffff;
   border           : 1px solid #8b8b8b;
}
</style>
<script>
   tu = null;
   var opts = {   prefetchBuffer: true,
   				  onscroll :      updateHeader,
                  sortAscendImg:        'img/sort_asc.gif',
                  sortDescendImg:       'img/sort_desc.gif'
              };
   onloads.push( function() { tu = new Rico.LiveGrid( '<%= tableName %>',
                                                       <%= pageSize %>,
                                                       <%= rows %>,
                                                       'livegriddata.jsp', opts);
                             } );

   function updateHeader( liveGrid, offset ) {
      $('bookmark').innerHTML = "Run Results " + (offset+1) + " - " + (offset+liveGrid.metaData.getPageSize()) + " of " +
      liveGrid.metaData.getTotalRows();
      var sortInfo = "";
      /* if (liveGrid.sortCol) {
         sortInfo = "&data_grid_sort_col=" + liveGrid.sortCol + "&data_grid_sort_dir=" + liveGrid.sortDir;
      }
	  $('bookmark').href="/rico/livegrid.page" + "?data_grid_index=" + offset + sortInfo; */
   }


   function myAlert(s) { alert(s)}
</script>

<a id="bookmark" style="margin-bottom:3px;font-size:12px">Run Results 1 - <%= pageSize %> of <%= rows %></a>
<div id="container" >
<table id="<%= tableName %>_header" class="fixedTable" cellspacing="0" cellpadding="0" style="width:730px">
  <tr>
	  <th class="first tableCellHeader" style="width:50px"><%= resultTable.getHeader(0) %></th>
	  <th class="tableCellHeader" style="width:280px"><%= resultTable.getHeader(1) %></th>
	  <th class="tableCellHeader" style="width:60px"><%= resultTable.getHeader(2) %></th>
	  <th class="tableCellHeader" style="width:60px"><%= resultTable.getHeader(3) %></th>
	  <th class="tableCellHeader" style="width:60px"><%= resultTable.getHeader(4) %></th>
	  <th class="tableCellHeader" style="width:80px"><%= resultTable.getHeader(5) %></th>
	  <th class="tableCellHeader" style="width:80px"><%= resultTable.getHeader(6) %></th>
      <th class="tableCellHeader" style="width:60px"><%= resultTable.getHeader(7) %></th>
  </tr>
</table>
<div id="<%= tableName %>_container" style="width:770px">
	<div id="viewPort" style="float:left">
	<table id="<%= tableName %>"
         class="fixedTable"
         cellspacing="0"
	       cellpadding="0"
         style="width:700px; border-left:1px solid #ababab" >

<%      for (int i = 0; i < pageSize * 3; i++) { %>
        <tr>
<%
            if (i < rows) {
                Comparable[] row = resultTable.getRow(i);
%>
	        <td id="runId<%= i %>" class="cell" style="width:50px"><%= row[0] %></td>
	        <td class="cell" style="width:280px"><%= row[1] %></td>
	        <td class="cell" style="width:60px"><%= row[2] %></td>
	        <td class="cell" style="width:60px"><%= row[3] %></td>
	        <td class="cell" style="width:60px"><%= row[4] %></td>
	        <td class="cell" style="width:80px"><%= row[5] %></td>
	        <td class="cell" style="width:80px"><%= row[6] %></td>
	        <td class="cell" style="width:60px"><%= row[7] %></td>
<%          } else { %>
	        <td id="runId<%= i %> class="cell" style="width:50px">&nbsp;</td>
	        <td class="cell" style="width:280px">&nbsp;</td>
	        <td class="cell" style="width:60px">&nbsp;</td>
	        <td class="cell" style="width:60px">&nbsp;</td>
	        <td class="cell" style="width:60px">&nbsp;</td>
	        <td class="cell" style="width:80px">&nbsp;</td>
	        <td class="cell" style="width:80px">&nbsp;</td>
	        <td class="cell" style="width:60px">&nbsp;</td>
<%          } %>
        </tr>
<%      } %>
	</table>
  </div>
</div>
<span>ABC&nbsp;&nbsp;</span>
<span>DEF&nbsp;<br/></span>
<span>GHI&nbsp;<br/></span>
<span>LMN&nbsp;<br/></span>
<div id="dropBox" style="margin-left:8px;margin-bottom:8px;float:left">
   <span class="catHeader">dropped name-list</span>

   <div class="listBox" id="dropZone" style="width:250px;height:140px;overflow:auto;">
   </div>
</div>

<%  } else { %>
    </head>
    <body>
        <br/>
        <center><b>There are no results</b></center>
        <br/>
<%  } %>
    </body>
</html>
