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
 * $Id$
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
-->
<%@ page language="java" import="com.sun.faban.harness.common.BenchmarkDescription,
                                 com.sun.faban.harness.common.Config"%>
<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
<%
String title = BenchmarkDescription.getBannerName().toUpperCase();
if(title.equals("FABAN"))
    title = " ";
%>
<html>
    <head>
        <title>Faban Banner [<%= Config.FABAN_HOST %>]</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
    </head>
    <body>
        <table BORDER="0" CELLSPACING="0" CELLPADDING="10" WIDTH="100%" BGCOLOR="#FFFFFF" >
            <tr>
                <td ALIGN="left" width=33% BGCOLOR="#395d73" style="color:white; font-size:10px"><img src="img/faban_header.gif" /></td><br>
                VERSION <%=BenchmarkDescription.getBannerVersion()%>
                <td ALIGN="Center" width=33% BGCOLOR="#395d73" style="color:white"><%=title%></td>
                <td ALIGN="right" width=33% BGCOLOR="#395d73" style="color:white">VERSION <%=BenchmarkDescription.getBannerVersion()%></td>
            </tr>
        </table>
    </body>
</html>
