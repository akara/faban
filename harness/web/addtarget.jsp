<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<!--
/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://faban.dev.java.net/public/CDDLv1.0.html or
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
<%@ page language="java" import="javax.security.auth.Subject,
                                 com.sun.faban.harness.common.Config,
                                 com.sun.faban.harness.security.AccessController"%>
<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
<%
    response.setHeader("Cache-Control", "no-cache");
    Subject user = usrEnv.getSubject();
    boolean rigAllowed = AccessController.isRigManageAllowed(user);
    boolean manageAllowed = AccessController.isManageAllowed(user);

    String targetname = request.getParameter("targetname");
    String targetowner = usrEnv.getUser();
    String targettags = request.getParameter("targettags");
    String targetmetric = request.getParameter("targetmetric");
    String targetmetricunit = request.getParameter("targetmetricunit");
    String targetcolorred = request.getParameter("targetcolorred");
    String targetcolororange = request.getParameter("targetcolororange");
    String targetcoloryellow = request.getParameter("targetcoloryellow");
%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
        <title>Add/Edit Target Page</title>
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
        <script src="/scripts/validateform.js" type="text/javascript"></script>
    </head>
    <body>
         <% if(rigAllowed == true &&  !"".equals(targetowner) && targetname == null) { %>
            <h3>Add Target</h3>
            <br>
            <form name="targetform" method="post" action="/controller/results/add_edit_target" onsubmit="return checkFields()">
                <input type="hidden" name="flag" title="flag" value="add">
                <input type="hidden" name="viewAll" title="viewAll" value="true">
                <input type="hidden" name="viewMy" title="viewMy" value="true"> 
                <table BORDER=0 CELLPADDING=4 CELLSPACING=3 width="70%" align="left" style="padding:2px; border: 2px solid #cccccc;">
              <tbody>
                <tr>
                        <th class="leftheader">Please enter the target details: </th>
                        <th class="leftheader">&nbsp;</th>
                </tr>
                <tr class="even">
                        <td class="tablecell">Name</td>
                        <td class="tablecell"><input type="text" id="targetname" name="targetname" title="target name" size="25">(ex: foo)</td>
                </tr>
                <tr class="odd">
                        <td class="tablecell">Owner</td>
                        <td class="tablecell"><input type="text" id="targetowner" name="targetowner" title="target owner" size="25" value=<%=targetowner%>></td>
                </tr>
                <tr class="even">
                        <td class="tablecell">Metric</td>
                        <td class="tablecell"><input type="text" id="targetmetric" name="targetmetric" title="target metric" size="25">(ex: 10)</td>
                </tr>
                <tr class="odd">
                        <td class="tablecell">Metric unit</td>
                        <td class="tablecell"><input type="text" id="targetmetricunit" name="targetmetricunit" title="target metric unit" size="25">(ex: ops/sec)</td>
                </tr>
                <tr class="even">
                        <td class="tablecell">Tags</td>
                        <td class="tablecell"><input type="text" id="targettags" name="targettags" title="target tags" size="25">(ex: foo)</td>
                </tr>
                <tr>
                        <th class="leftheader">Please enter the upper limit for the color codes below: </th>
                        <th class="leftheader">&nbsp;</th>
                </tr>
                <tr class="odd">
                        <td class="tablecell">Red %</td>
                        <td class="tablecell"><input style="background-color:red" type="text" id="targetcolorred" name="targetcolorred" title="target color red" size="25" value="60">(ex: 70)</td>
                </tr>
                <tr class="even">
                        <td class="tablecell">Orange %</td>
                        <td class="tablecell"><input style="background-color:orange" type="text" id="targetcolororange" name="targetcolororange" title="target color orange" size="25" value="80">(ex: 80)</td>
                </tr>
                <tr class="odd">
                        <td class="tablecell">Yellow %</td>
                        <td class="tablecell"><input style="background-color:yellow" type="text" id="targetcoloryellow" name="targetcoloryellow" title="target color yellow" size="25" value="100">(ex: 100)</td>
                </tr>
                <tr class="even">
                        <td class="tablecell">* Anything beyond percentage value for yellow is considered as <span style="color:#00cc00">green</span>.</td>
                        <td class="tablecell">&nbsp;</td>
                </tr>
                <tr class="odd">
                        <td class="tablecell"><input type="submit" value="Add">&nbsp;&nbsp;<input type="reset" value="Reset"></td>
                        <td class="tablecell">&nbsp;</td>
                </tr>
                </tbody>
                </table>
            </form>
        <% } else if(rigAllowed == true && !"".equals(targetowner) && targetname != null) {
            targetowner = request.getParameter("targetowner");%>
            <h3>Edit Target</h3>
            <br>
            <form name="targetform" method="post" action="/controller/results/add_edit_target" onsubmit="return checkFields()">
                <input type="hidden" name="flag" title="flag" value="edit">
                <input type="hidden" name="viewAll" title="viewAll" value="true">
                <input type="hidden" name="viewMy" title="viewMy" value="true">
                <input type="hidden" id="targetname" name="targetname" title="target name" size="25" value="<%=targetname%>">
                <table BORDER=0 CELLPADDING=4 CELLSPACING=3 width="70%" align="left" style="padding:2px; border: 2px solid #cccccc;">
                <tbody>
                <tr>
                        <th class="leftheader">Please enter the target details: </th>
                        <th class="leftheader">&nbsp;</th>
                </tr>
                <tr class="even">
                        <td class="tablecell">Name</td>
                        <td class="tablecell"><input disabled=true type="text" title="target name" size="25" value="<%=targetname%>"></td>
                </tr>
                <tr class="odd">
                        <td class="tablecell">Owner</td>
                        <td class="tablecell"><input type="text" id="targetowner" name="targetowner" title="target owner" size="25" value="<%=targetowner%>"></td>
                </tr>                
                <tr class="even">
                        <td class="tablecell">Metric</td>
                        <td class="tablecell"><input type="text" id="targetmetric" name="targetmetric" title="target metric" size="25" value="<%=targetmetric%>">(ex: 10)</td>
                </tr>
                <tr class="odd">
                        <td class="tablecell">Metric unit</td>
                        <td class="tablecell"><input type="text" id="targetmetricunit" name="targetmetricunit" title="target metric unit" size="25" value="<%=targetmetricunit%>">(ex: ops/sec)</td>
                </tr>
                <tr class="even">
                        <td class="tablecell">Tags</td>
                        <td class="tablecell"><input type="text" id="targettags" name="targettags" title="target tags" size="25" value="<%=targettags%>">(ex: foo)</td>
                </tr>
                <tr>
                        <th class="leftheader">Please enter the upper limit for the color codes below: </th>
                        <th class="leftheader">&nbsp;</th>
                </tr>
                <tr class="odd">
                        <td class="tablecell">Red %</td>
                        <td class="tablecell"><input style="background-color:red" type="text" id="targetcolorred" name="targetcolorred" title="target color red" size="25" value="<%=targetcolorred%>">(ex: 70)</td>
                </tr>
                <tr class="even">
                        <td class="tablecell">Orange %</td>
                        <td class="tablecell"><input style="background-color:orange" type="text" id="targetcolororange" name="targetcolororange" title="target color orange" size="25" value="<%=targetcolororange%>">(ex: 80)</td>
                </tr>
                <tr class="odd">
                        <td class="tablecell">Yellow %</td>
                        <td class="tablecell"><input style="background-color:yellow" type="text" id="targetcoloryellow" name="targetcoloryellow" title="target color yellow" size="25" value="<%=targetcoloryellow%>">(ex: 100)</td>
                </tr>
                <tr class="even">
                        <td class="tablecell">* Anything beyond percentage value for yellow is considered as <span style="color:#00cc00">green</span>.</td>
                        <td class="tablecell">&nbsp;</td>
                </tr>
                <tr class="odd">
                        <td class="tablecell"><input type="submit" value="Save">&nbsp;&nbsp;<input type="reset" value="Reset"></td>
                        <td class="tablecell">&nbsp;</td>
                </tr>
                </tbody>
                </table>
            </form>
        <% } else { %>
            Sorry, you might not have logged in or you do not have enough <br>
            privileges to add/edit targets.
        <% } %>
    </body>
</html>
