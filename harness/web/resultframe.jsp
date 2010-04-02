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
                                 java.io.FileReader,
                                 com.sun.faban.harness.common.BenchmarkDescription,
                                 java.io.IOException,
                                 com.sun.faban.harness.webclient.RunResult"%>
<%
    String runId = request.getParameter("runId");
    String resultFile = request.getParameter("result");
    String show = request.getParameter("show");
    String[] statusFileContent = RunResult.readStatus(runId);
    String displayFrame = null;
    if ("logs".equals(show)) {
        if ("STARTED".equals(statusFileContent[0]))
            displayFrame = "LogReader?runId=" + runId + "&startId=end#end";
        else
            displayFrame = "LogReader?runId=" + runId;

    } else {
        displayFrame = "output/" + runId + '/' + resultFile;
    }
%>
<html>
    <head>
        <title><%=BenchmarkDescription.getBannerName()%> Result for Run <%= runId %> [<%= Config.FABAN_HOST %>]</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
    </head>
    <frameset rows="80,*">
        <frame name="navigate" src="resultnavigator.jsp?runId=<%= runId %>" scrolling="no" noresize="noresize" frameborder="0"/>
        <frame name="display" src="<%= displayFrame %>" frameborder="0"/>
        <noframes>
            <p>This page requires frames, but your browser does not support them.</p>
        </noframes>
    </frameset>
</html>