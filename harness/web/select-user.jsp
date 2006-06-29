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
 * $Id: select-user.jsp,v 1.2 2006/06/29 19:38:44 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
<html>
<head>
<link rel="icon" type="image/gif" href="img/faban.gif">
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
<meta name="Author" content="Ramesh Ramachandran"/>
<meta name="Description" content="Form to display User login"/>
<%@ page language="java" import="com.sun.faban.harness.webclient.UserEnv,
                                 com.sun.faban.harness.common.BenchmarkDescription,
                                 java.util.Map"%>

<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
<%
    String user = (String)session.getAttribute("faban.user");
    BenchmarkDescription desc =  (BenchmarkDescription) session.getAttribute(
            "faban.benchmark");
    String benchmark = desc == null ? null : desc.name;

    if((user != null) && (benchmark != null)) {
%>
<meta HTTP-EQUIV=REFRESH CONTENT="0;URL=new-run.jsp">
<%
    }
    else {
        String[] users = usrEnv.getUsers();
        Map benchNameMap = BenchmarkDescription.getBenchNameMap();
        int benchCount = benchNameMap.size();
        if (benchCount < 1) {
%>
</head>
<body>
<h3><center>Sorry, Faban could not find or successfully deploy any benchmarks.</center></h3>

<%
        } else {
            String[] benchmarks = new String[benchCount];
            benchmarks = (String[]) benchNameMap.keySet().toArray(benchmarks);
%>


<script>
function updateUser() {
    document.bench.user.value=document.bench.userlist.value
}
</script>

</head>
<body>
<br/>
<br/>
<br/>

<form name="bench" method="post" action="new-run.jsp">
  <table cellpadding="0" cellspacing="2" border="0" align="center">
    <tbody>
      <tr>
        <td>User Name</td>
        <td>
          <input type="text" name="user" size="10"
            <% if(user != null) { %>
              value= <%=user %>
            <% }
               else {
                 if((users != null) && (users.length > 0)) {
            %>
              value= <%=users[0] %>
            <%   }
               }
            %>
          >
          <% if((users != null) && (users.length > 0)) { %>
            <select name="userlist" ONCHANGE="updateUser()">
              <% for(int i = 0; i < users.length; i++) { %>
                <option
                  <% if(((user != null) && users[i].equals(user)) ||
                        ((user == null) && (i == 0))){ %>
                    SELECTED
                  <% } %>
                  ><%= users[i]%>
                </option>
              <% } %>
            </select>
          <% } %>
        </td>
      </tr>
      <% if (benchmark == null)
             benchmark = benchmarks[0];
         if (benchmarks.length > 1) { %>
        <tr>
          <td>Benchmark</td>
          <td>
            <select name="benchmark">
              <% for (int i = 0; i < benchmarks.length; i++) { %>
              <option
                  <% if(benchmarks[i].equals(benchmark)) { %>
                    SELECTED
                  <% } %>
                  ><%= benchmarks[i]%>
              <% } %>
            </select>
          </td>
        </tr>
      <% } %>
    </tbody>
  </table>
  <% if (benchmarks.length == 1) { %>
      <input type="hidden" name="benchmark" value="<%=benchmark %>"></input>
  <% } %>
  <br>
  <br>
  <center><input type="submit" value="Login">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="reset"></center>
</form>

<%          }
        }
%>

</body>
</html>
