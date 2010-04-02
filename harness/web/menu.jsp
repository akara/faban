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
 * $Id$
 *
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
-->
<%@ page language="java" import="javax.security.auth.Subject,
                                 com.sun.faban.harness.common.Config,
                                 com.sun.faban.harness.security.AccessController"%>
<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
<%
    boolean submitAllowed = false;
    boolean manageAllowed = false;
    boolean rigAllowed = false;
    if (Config.daemonMode != Config.DaemonModes.DISABLED) {
        Subject user = usrEnv.getSubject();
        submitAllowed = AccessController.isSubmitAllowed(user);
        rigAllowed = AccessController.isRigManageAllowed(user);
        manageAllowed = AccessController.isManageAllowed(user);
    }
%>
<html>
    <head>
        <title>Faban Menu [<%= Config.FABAN_HOST %>]</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
    </head>
    <body>
        <table BORDER="0" WIDTH="100%" BGCOLOR="#FFFFFF" color="#666699" >
            <tr><td VALIGN="TOP"></td></tr>
            <tr><td VALIGN="CENTER"><br/>
<% if (submitAllowed) { %>
            <tr><td VALIGN="CENTER"><br/><a href="selectprofile.jsp" target="main">Schedule Run</a></td></tr>
<% } else { %>
            <tr><td VALIGN="CENTER" style="color: rgb(102, 102, 102);"><br/>Schedule Run</td></tr>
<% }
   if (rigAllowed) { %>
            <tr><td VALIGN="CENTER"><br/><a href="suspend-runs.jsp" target="main">Suspend Pending Runs</a></td></tr>
            <tr><td VALIGN="CENTER"><br/><a href="resume-runs.jsp" target="main">Resume Pending Runs</a></td></tr>
<% } else { %>
            <tr><td VALIGN="CENTER" style="color: rgb(102, 102, 102);"><br/>Suspend Pending Runs</td></tr>
            <tr><td VALIGN="CENTER" style="color: rgb(102, 102, 102);"><br/>Resume Pending Runs</td></tr>
<% }
   if (submitAllowed || manageAllowed) { %>
            <tr><td VALIGN="CENTER"><br/><a href="kill-run.jsp" target="main">Kill Current Run</a></td></tr>
<% } else { %>
            <tr><td VALIGN="CENTER" style="color: rgb(102, 102, 102);"><br/>Kill Current Run</td></tr>
<% } %>
            <tr><td VALIGN="CENTER"><br/><a href="/controller/results/list" target="main">View Results</a></td></tr>
<% if (submitAllowed || manageAllowed) { %>
            <tr><td VALIGN="CENTER"><br/><a href="pending-runs.jsp" target="main">View Pending Runs</a></td></tr>
<% } else { %>
            <tr><td VALIGN="CENTER" style="color: rgb(102, 102, 102);"><br/>View Pending Runs</td></tr>
<% }
   if (manageAllowed) { %>
            <tr><td VALIGN="CENTER"><br/><a href="switchprofile.jsp" target="main">Switch Profile</a></td></tr>
<% } else { %>
            <tr><td VALIGN="CENTER" style="color: rgb(102, 102, 102);"><br/>Switch Profile</td></tr>
<% } %>
<% if (Config.targetting == true) { %>
        <% if(usrEnv.getUser() != null ) { %>
            <tr><td VALIGN="CENTER"><br/><a href="/controller/results/targetlist?viewAll=true" target="main">Targets</a></td></tr>
        <%}else{%>
            <tr><td VALIGN="CENTER"><br/><a href="/controller/results/targetlist?viewAll=null" target="main">Targets</a></td></tr>
        <%}%>
<% } %>
            <tr><td VALIGN="CENTER"><br/><a href="http://faban.sunsource.net/1.0/docs/toc.html" target="_blank">Help</a></td></tr>
            <tr><td VALIGN="CENTER"><br/></td></tr>
            <tr><td VALIGN="CENTER">
               <form name="tagsearch" method="get" target="main" action="controller/results/list">
                   Tag Search<br>
                   <input type="text" name="inputtag" title="tag search" size="15"><br>
                   <input type="submit" value="Search">&nbsp;&nbsp;<input type="reset" value="Reset">
               </form>
            </td></tr>
            <tr><td VALIGN="TOP"></td></tr>
        </table>
    </body>
</html>
