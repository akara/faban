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
                                 com.sun.faban.harness.common.BenchmarkDescription"%>
<html>
    <head>
        <title><%=BenchmarkDescription.getBannerName()%> Administration [<%= Config.FABAN_HOST %>]</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
    </head>
    <body>
        <table BORDER="0" CELLSPACING="5" CELLPADDING="10" WIDTH="100%" BGCOLOR="#FFFFFF" >
            <tr>
                <td ALIGN="CENTER" WIDTH="33%" BGCOLOR="#594FBF"> Sun Microsystems </td>
                <td ALIGN="CENTER" WIDTH="34%" BGCOLOR="#FBE249"><b><%=BenchmarkDescription.getBannerName()%> Administration</b></td>
                <td ALIGN="CENTER" WIDTH="33%" BGCOLOR="#D12124"> Version <%=BenchmarkDescription.getBannerVersion()%> </td>
            </tr>  
        </table>    
        <table BORDER="0" CELLPADDING="0" WIDTH="100%" BGCOLOR="#FFFFFF" >
            <tr>
                <td WIDTH="15%" VALIGN="TOP">
                    <table BORDER="0" WIDTH="100%" BGCOLOR="#FFFFCC" color="#666699" >
                        <tr><td VALIGN="TOP"></td></tr>
                        <tr><td VALIGN="CENTER"><a href="GenericParams.jsp">Schedule Runs</a></td></tr>
                        <tr><td VALIGN="CENTER"><br/><a href="index.html">View Results</a></td></tr>
                        <tr><td VALIGN="CENTER"><br/><a href="index.html">View Pending Runs</a></td></tr>
                        <tr><td VALIGN="CENTER"><br/><a href="index.html">Help</a></td></tr>
                        <tr><td VALIGN="TOP"></td></tr>
                    </table>
                </td>
                <td WIDTH="85%" VALIGN="TOP" BGCOLOR="#FFFFCC"/>
            </tr>
        </table>        
    </body>
</html>
