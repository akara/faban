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
<%@ page language="java" import="java.util.Map,
                                 java.util.HashMap,
                                 java.io.File,
                                 javax.security.auth.Subject,
                                 com.sun.faban.harness.util.FileHelper,
                                 com.sun.faban.harness.common.Config,
                                 com.sun.faban.harness.common.BenchmarkDescription,
                                 com.sun.faban.harness.security.AccessController,
                                 com.sun.faban.harness.webclient.UserEnv"%>

<jsp:useBean id="usrEnv" scope="session" class="com.sun.faban.harness.webclient.UserEnv"/>
<html>
     <head>
         <link rel="icon" type="image/gif" href="img/faban.gif">
         <link rel="stylesheet" type="text/css" href="/css/style.css" />
         <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1"/>
         <meta name="Author" content="Ramesh Ramachandran"/>
         <meta name="Description" content="Form to display profile selection"/>
         <title>Select Profile [<%= Config.FABAN_HOST %>]</title>
<%  String tagsForProfile = "";
    File tagsFile = null;
    String profile = (String)session.getAttribute("faban.profile");
    
    
    BenchmarkDescription desc =  (BenchmarkDescription) session.getAttribute(
            "faban.benchmark");
    String benchmark = desc == null ? null : desc.name;
    Subject user = usrEnv.getSubject();

    if(profile != null && benchmark != null && 
       AccessController.isSubmitAllowed(user, desc.shortName)) {
%>
         <meta HTTP-EQUIV=REFRESH CONTENT="0;URL=new-run.jsp">
<%
    }
    else {
        String[] profiles = usrEnv.getProfiles();
        if(profile == null){
            if((profiles != null) && (profiles.length > 0)) {
                profile = profiles[0];
                tagsFile = new File(Config.PROFILES_DIR + profile + "/tags");
                if(tagsFile.exists() && tagsFile.length()>0){
                    tagsForProfile = FileHelper.readContentFromFile(tagsFile).trim();
                }
            }
        }
        
        Map<String, BenchmarkDescription> benchNameMap =
                BenchmarkDescription.getBenchNameMap();
        // We need to ensure only benchmarks the user is allowed to submit are shown.
        // The benchNameMap is a reference to the cached version. Don't change it.
        // Make copies instead.
        HashMap<String, BenchmarkDescription> allowedBench = 
                new HashMap<String, BenchmarkDescription>(benchNameMap.size());
        for (Map.Entry<String, BenchmarkDescription> entry : benchNameMap.entrySet()) {
            BenchmarkDescription d = entry.getValue();
            if (AccessController.isSubmitAllowed(user, d.shortName))
                allowedBench.put(entry.getKey(), d);
        }
        int benchCount = allowedBench.size();
        if (benchNameMap.size() < 1) {
%>
    </head>
    <body>
<h3><center>Sorry, Faban could not find or successfully deploy any benchmarks.</center></h3>
<%
        } else if (benchCount < 1) {
%>
</head>
<body>
<h3><center>Sorry, you're not allowed to submit any benchmark.</center></h3>
<%
        } else {
            String[] benchmarks = new String[benchCount];
            benchmarks = allowedBench.keySet().toArray(benchmarks);
%>


<script>
var req;
function updateProfile() {
    document.bench.profile.value=document.bench.profilelist.value;
    var url = "/controller/result_action/profile_tag_list?profileselected="+escape(document.bench.profile.value);
    if (typeof XMLHttpRequest != "undefined") {
       req = new XMLHttpRequest();
   } else if (window.ActiveXObject) {
       req = new ActiveXObject("Microsoft.XMLHTTP");
   }
   req.open("GET", url, true);
   req.onreadystatechange = callback;
   req.send(null);
}

function callback() {
    if (req.readyState == 4) {
        if (req.status == 200) {
            //update tags field
            var result = req.responseText;
            document.getElementById("tags").innerHTML=result;
        }
    }
}

</script>

</head>
<body>
<br/>
<br/>
<br/>

<form name="bench" method="post" action="new-run.jsp">
  <table cellpadding="0" cellspacing="2" border="0" align="center">
    <tbody>
      <tr>
        <td>Profile</td>
        <td>
          <% if(profile != null) { %>
            <input type="text" title="Please select profile or enter new profile name"
                name="profile" size="10" value=<%=profile %> >
            <select name="profilelist" title="Please select profile or enter new profile name"
                ONCHANGE="updateProfile()">
              <% for(int i = 0; i < profiles.length; i++) { %>
                <option
                  <% if(((profile != null) && profiles[i].equals(profile)) ||
                        ((profile == null) && (i == 0))){ %>
                    SELECTED
                  <% } %>
                  ><%= profiles[i]%>
                </option>
              <% } %>
            </select>
          <% } else { %>
            <input type="text" title="Please enter new profile name for your runs"
                name="profile" size="10">
          <% } %>
        </td>
      </tr>
      <% if (benchmark == null)
             benchmark = benchmarks[0];
         if (benchmarks.length > 1) { %>
        <tr>
          <td>Benchmark</td>
          <td>
            <select name="benchmark" title="Please select benchmark to run">
              <% for (int i = 0; i < benchmarks.length; i++) { %>
              <option
                  <% if(benchmarks[i].equals(benchmark)) { %>
                    SELECTED
                  <% } %>
                  ><%= benchmarks[i]%>
              <% } %>
            </select>
          </td>
        </tr>
      <% } %>
      <tr>
         <td>Tags for this profile</td>
         <td>
             <textarea id="tags" name="tags" title="Tags associated with profile"
                       rows="2" style="width: 98%;"><%= tagsForProfile%></textarea>
         </td>
       </tr>
    </tbody>
  </table>
  <% if (benchmarks.length == 1) { %>
      <input type="hidden" name="benchmark" value="<%=benchmark %>"></input>
  <% } %>
  <br>
  <br>
  <center><input type="submit" value="Select">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="reset"></center>
</form>

<%          }
        }
%>

</body>
</html>
