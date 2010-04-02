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
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
<meta name="Author" content="Ramesh Ramachandran"/>
<meta name="Description" content="JSP to setup run.xml for the XForms servlet"/>
<%@ page language="java" import="java.util.Map,
                                 java.util.StringTokenizer,
                                 com.sun.faban.harness.common.BenchmarkDescription,
                                 com.sun.faban.harness.security.AccessController,
                                 com.sun.faban.harness.webclient.UserEnv,
                                 com.sun.faban.harness.util.FileHelper,
                                 com.sun.faban.harness.common.Config,
                                 java.io.File"%>
<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
<title>Please select profile [<%= Config.FABAN_HOST %>]</title>
<%
    String profile = (String)session.getAttribute("faban.profile");
    if (profile == null) {
        profile = request.getParameter("profile");
        session.setAttribute("faban.profile", profile);
    }
    String tags = (String)session.getAttribute("faban.profile.tags");
    if (tags == null) {
        tags = request.getParameter("tags");
        if (profile != null && !"".equals(profile)){
            File profileDir = new File(Config.PROFILES_DIR + profile);
            if(!profileDir.exists())
                profileDir.mkdir();
            File tagsFile = new File(Config.PROFILES_DIR + profile + "/tags");
            if ( (tags != null && !"".equals(tags))) {
                StringBuilder formattedTags = new StringBuilder();
                StringTokenizer t = new StringTokenizer(tags," \n,");
                while (t.hasMoreTokens()) {
                    String nextT = t.nextToken().trim();
                    if( nextT != null && !"".equals(nextT) ){
                        formattedTags.append(nextT + "\n");
                    }
                }
                FileHelper.writeContentToFile(formattedTags.toString(), tagsFile);             
            }
            if(!(tagsFile.length() > 0L))
                    tagsFile.delete();
            if(profileDir.list().length == 0)
                    profileDir.delete();
            session.setAttribute("faban.profile.tags", tags);
        }
    }
    BenchmarkDescription desc = (BenchmarkDescription)
                                        session.getAttribute("faban.benchmark");
    String benchmark = null;
    if (desc == null) {
        Map<String, BenchmarkDescription> bms =
                BenchmarkDescription.getBenchNameMap();
        benchmark = request.getParameter("benchmark");
        desc = bms.get(benchmark);
        session.setAttribute("faban.benchmark", desc);
    }

    if ((profile != null) && (desc != null) && AccessController.
            isSubmitAllowed(usrEnv.getSubject(), desc.shortName)) {

        String templateFile = Config.PROFILES_DIR + profile + File.separator +
                desc.configFileName + "." + desc.shortName;
        File f = new File(templateFile);

        String benchMetaInf = Config.BENCHMARK_DIR + File.separator +
                    desc.shortName + File.separator + "META-INF" +
                    File.separator;

        // String dstFile = Config.TMP_DIR + desc.configFileName;
        if(!f.exists()) // Use the default config file
            templateFile = benchMetaInf + desc.configFileName;

        session.setAttribute("faban.submit.template", templateFile);

        if (desc.configStylesheet != null)
            session.setAttribute("faban.submit.stylesheet", 
                                        benchMetaInf + desc.configStylesheet);

        String url = "bm_submit/" + desc.shortName + '/' + desc.configForm;
%>

<meta HTTP-EQUIV=REFRESH CONTENT="0;URL=<%=url%>">
<link rel="icon" type="image/gif" href="img/faban.gif">
<link rel="stylesheet" type="text/css" href="/css/style.css" />
</head>

<% }
   else {
%>
<body>
<form name="bench" method="post" action="selectprofile.jsp">

  <br/>
  <center><b>Unable to determine profile or benchmark... please select profile</b></center>
  <br/>
  <center><input type="submit" value="OK"></center>
</form>
</body>
<% } %>
</html>
