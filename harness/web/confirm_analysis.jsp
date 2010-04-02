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
                                 com.sun.faban.harness.webclient.ResultAction"%>
<%
    ResultAction.EditAnalysisModel model = (ResultAction.EditAnalysisModel)
             request.getAttribute("editanalysis.model");
 %>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
        <meta name="Author" content="Akara Sucharitakul"/>
        <meta name="Description" content="Results JSP"/>
        <title>Analyze Runs [<%= Config.FABAN_HOST %>]</title>
        <link rel="icon" type="image/gif" href="img/faban.gif">
        <link rel="stylesheet" type="text/css" href="/css/style.css" />
    </head>
    <body>
    <h2><center>Analysis Already Exists</center></h2>
       <form name="replaceOld" method="post" action="analyze">
    <%
            for (String runId : model.runIds) {
    %>
         <input type="hidden" name="select" value="<%= runId %>"/>
    <%
            }
    %>
         <input type="hidden" name="type" value="<%= model.type %>"/>
         <input type="hidden" name="output" value="<%= model.name %>"/>
         <center>
         <input type="submit" name="replace" value="Replace"/>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
         <input type="submit" name="replace" value="Use Old Analysis"/>
         </center>
       </form>
    </body>
</html>