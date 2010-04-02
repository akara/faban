<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN"
   "http://www.w3.org/TR/html4/loose.dtd">
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
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
-->
<!--
/**
 * @author Sheetal Patil
 */
 -->
<%@ page language="java" import="com.sun.faban.harness.common.Config,
                                 com.sun.faban.harness.webclient.ResultAction,
                                 java.util.HashSet"
 %>
 <%  ResultAction.EditArchiveModel model = (ResultAction.EditArchiveModel)
                                    request.getAttribute("archive.model");
     HashSet<String> uploadedRuns = (HashSet<String>) request.getAttribute("uploadedRuns");
     HashSet<String> duplicateSet = (HashSet<String>) request.getAttribute("duplicateRuns");

 %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Archived Runs [<%= Config.FABAN_HOST %>]</title>
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
    </head>
    <body>
        <%if( !(uploadedRuns.isEmpty()) ) {%>
            <h4>Archived Runs to <%=model.head%></h4>
        <%
                for (int i = 0; i < model.runIds.length; i++) {
                    String runId = model.runIds[i];
                    if(uploadedRuns.contains(runId) && !duplicateSet.contains(runId)){
                %>
                        <%=runId%><br>
                <%
                    }
                }
        }
        %>
        <br>
        <%if( !(duplicateSet.isEmpty()) ) {%>
            <h4>These runs might already be archived to <%=model.head%>, please try again!!</h4>
            <%
                for (String runId : model.runIds) {
                    if (duplicateSet.contains(runId)) {
            %>
                    <%=runId%><br>
            <%
                    }
                }
        }
        %>
        <%if(duplicateSet.isEmpty() && uploadedRuns.isEmpty() ) {%>
            <h4>No runs archived to <%=model.head%></h4>
        <%}%>
    </body>
</html>
