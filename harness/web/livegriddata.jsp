<%@ page language="java" import="com.sun.faban.harness.webclient.Result,
                                 com.sun.faban.harness.webclient.TableModel,
                                 com.sun.faban.harness.webclient.SortDirection"%>
<?xml version="1.0" encoding="ISO-8859-1"?>
<ajax-response>
    <response type="object" id=’data_grid_updater'>
        <rows update_ui=’true’ >

<%
    String id = request.getParameter("id");

    String v = request.getParameter("page_size");
    int records = Integer.parseInt(v);
    v = request.getParameter("offset");
    int offset = Integer.parseInt(v);
    
    String sortColumn = request.getParameter("sort_col");

    v = request.getParameter("sort_dir");
    SortDirection direction;
    if (v == null)
        direction = SortDirection.ASCENDING;
    else if (v.startsWith("ASC"))
        direction = SortDirection.ASCENDING;
    else
        direction = SortDirection.DESCENDING;

    TableModel table = (TableModel) session.getAttribute(id);
    if (sortColumn != null)
        table.sort(sortColumn, direction);

    response.setHeader("Content-Type", "text/xml");

    int rows = table.rows();
    if (offset + records > rows)
        records = rows - offset;

    for (int i = offset; i < records; i++) {
        Comparable[] row = table.getRow(i);
%>
            <tr>
<%
        for (int j = 0; j < row.length; j++) {
%>
               <td><%= row[j] %></td>
<%

        }
%>
            </tr>
<%  } %>

        </rows>
    </response>
</ajax-response>