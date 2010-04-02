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
<%@ page language="java" import="com.sun.faban.harness.common.Config,
                                 com.sun.faban.harness.common.BenchmarkDescription"%>
<html>
  <head>
    <title>Welcome to Faban [<%= Config.FABAN_HOST %>]</title>
    <link rel="icon" type="image/gif" href="img/faban.gif">
    <link rel="stylesheet" type="text/css" href="/css/style.css" />
  </head>
  <body>
  <% String bannerName = BenchmarkDescription.getBannerName();
     if (!"Faban".equals(bannerName)) {
         out.println("<br/><br/><br/>");
     }
  %>
  <h2 align="center">Welcome to <%=bannerName%></h2>
  <% // We only show notices if this is not a benchmark integration.
     // The legal notice is not practical if Faban emulates benchmark-specific
     // behavior.
     if ("Faban".equals(bannerName)) {
  %>
        <p><br/><br/></p>
        <p style="margin-left: 100px; margin-right: 100px;"><font size="-2">
        Copyright &copy; 2006-2009 Sun Microsystems, Inc. All rights reserved.
        </font></p>
        <p style="margin-left: 100px; margin-right: 100px;"><font size="-2">
        U.S. Government Rights - Commercial software. Government users are
        subject to the Sun Microsystems, Inc. standard license agreement and
        applicable provisions of the FAR and its supplements.</font></p>
        <p style="margin-left: 100px; margin-right: 100px;"><font size="-2">
        Use is subject to license terms.</font></p>
        <p style="margin-left: 100px; margin-right: 100px;"><font size="-2">
        This distribution may include materials developed by third parties.
        </font></p>
        <p style="margin-left: 100px; margin-right: 100px;"><font size="-2">
        Sun, Sun Microsystems, the Sun logo and Java are trademarks or
        registered trademarks of Sun Microsystems, Inc. in the U.S. and other
        countries.<br/></font></p>
        <p><br/></p>
        <p style="margin-left: 100px; margin-right: 100px;"><font size="-2">
        Copyright &copy; 2006-2009 Sun Microsystems, Inc. Tous droits
        r&eacute;serv&eacute;s.</font></p>
        <p style="margin-left: 100px; margin-right: 100px;"><font size="-2">
        L'utilisation est soumise aux termes du contrat de licence.</font></p>
        <p style="margin-left: 100px; margin-right: 100px;"><font size="-2">
        Cette distribution peut comprendre des composants
        d&eacute;velopp&eacute;s par des tierces parties.</font></p>
        <p style="margin-left: 100px; margin-right: 100px;"><font size="-2">
        Sun, Sun Microsystems, le logo Sun et Java sont des marques de fabrique
        ou des marques d&eacute;pos&eacute;es de Sun Microsystems, Inc. aux
        Etats-Unis et dans d'autres pays.</font><br/>
        </p>
  <%
     }
  %>
  </body>
</html>
