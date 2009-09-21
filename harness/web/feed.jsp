<?xml version="1.0" encoding="utf-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
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
<%@page contentType="application/atom+xml" pageEncoding="UTF-8"
  language="java" import="com.sun.faban.harness.common.Config,
                          com.sun.faban.harness.webclient.RunResult.FeedRecord,
                          java.util.List,
                          java.util.Date,
                          java.text.SimpleDateFormat,
                          java.net.URL"%>
<%
    StringBuffer reqURL = (StringBuffer) request.getAttribute("request.url");
    URL url = new URL(reqURL.toString());
    String idBase = url.getProtocol() + "://" + url.getHost();
    String id = idBase + url.getPath();
    idBase += "/output/";
    String feedUpdated = (String) request.getAttribute("feed.updated");
    String[] restRequest = (String[]) request.getAttribute("rest.request");
    List<FeedRecord> itemList  = (List<FeedRecord>) request.getAttribute("feed.model");
%>
    <title>Results [<%= Config.FABAN_HOST %>]</title>
    <link rel="self" href="<%= reqURL %>"/>
    <id><%= id %></id>
    <updated><%= feedUpdated %></updated>
    <author>
        <name><%= Config.HARNESS_NAME %></name>
    </author>
    <generator version="<%= Config.HARNESS_VERSION %>"><%= Config.HARNESS_NAME %></generator>
<%
    if (restRequest != null) {
        for (String tag : restRequest) {
%>
    <category term="<%= tag.replace("+", "/") %>"/>
<%
        }
    }
    for (FeedRecord item : itemList) {
%>
    <entry>
        <title><%= item.title %></title>
        <summary><%= item.summary %></summary>
        <id><%= idBase + item.id %></id>
        <updated><%= item.updated %></updated>
        <link href="<%= item.link %>"/>
<%      for (String tag : item.tags) { %>
        <category term="<%= tag %>"/>
<%      } %>
    </entry>
<% } %>
</feed>
