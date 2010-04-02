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
<%@ page import="com.sun.faban.harness.common.Config,
                 java.io.PrintWriter"%>
<%@ page session="true" isErrorPage="true" %>
<html>
<head>
	<title>Error [<%= Config.FABAN_HOST %>]</title>
    <link rel="icon" type="image/gif" href="img/faban.gif">
    <link rel="stylesheet" type="text/css" href="/css/style.css" />
</head>
<body>
<br>
<center>
<table bgcolor="lightgrey" border="0" cellpadding="0" cellspacing="0">
	<tr>
		<td align="right" valign="top">
			<br>
		</td></tr>
		<tr>
			<td>
			<table bgcolor="lightgrey" cellspacing="3">
				<tr>
					<td>&nbsp;</td>
					<td>
						<font face="sans-serif">
						<b>OOPS - An error occurred.</b><br><br>
						<%

 // TODO: JSP error handling is still inadequate. Need to revisit this.
                            Throwable e = (Exception) session.getAttribute("chiba.exception");
                            String message = null;
                            if (e != null)
							    message = e.getMessage();
                            else
                                e = exception;
                                message = e.getMessage();
							if (message!=null && message.length()>0) {
								%>
								<font face="helv" size="+1">
									Message:<br>
									<ul>
										<font color="darkred"><%=message%></font>
									</ul>
								</font>
								<ul>
									<font face="sans-serif">
										<form>
                                            <input type="button" value="Back" onClick="javascript:history.back()">
										</form>
									</font>
								</ul>
								<br>
							<%
							}
							%>

						<% if (!(e instanceof SecurityException)) { %>

							<br><br>
							<b>Stack Trace:</b><br>
                                <pre><% e.printStackTrace(new PrintWriter(out)); %></pre>
						<%
						}
						%>						
						</font>	
					</td>
				</tr>			
			</table>
			</td>
		</tr>
</table>




</body>
</html>
