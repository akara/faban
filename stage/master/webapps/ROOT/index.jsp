<!doctype html public "-//w3c//dtd html 4.0 transitional//en" "http://www.w3.org/TR/REC-html40/strict.dtd">
<%@ page session="false" %>
<html>
    <head>
    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
    <title><%= application.getServerInfo() %></title>
    <style type="text/css">
      <!--
        body {
            color: #000000;
            background-color: #FFFFFF;
            font-family: Arial, "Times New Roman", Times;
            font-size: 16px;
        }

        A:link {
            color: blue
        }

        A:visited {
            color: blue
        }

        td {
            color: #000000;
            font-family: Arial, "Times New Roman", Times;
            font-size: 16px;
        }

        .code {
            color: #000000;
            font-family: "Courier New", Courier;
            font-size: 16px;
        }
      -->
    </style>
</head>

<body>

<!-- Header -->
<table width="100%">
    <tr>
        <td align="left" width="130"><a href="http://jakarta.apache.org/tomcat/index.html"><img src="tomcat.gif" height="92" width="130" border="0" alt="The Mighty Tomcat - MEOW!"></td>
        <td align="left" valign="top">
            <table>
                <tr><td align="left" valign="top"><b><%= application.getServerInfo() %></b></td></tr>
            </table>
        </td>
        <td align="right"><a href="http://jakarta.apache.org/"><img src="jakarta-banner.gif" height="48" width="505" border="0" alt="The Jakarta Project"></a></td>
    </tr>
</table>

<br>

<table>
    <tr>

        <!-- Table of Contents -->
        <td valign="top">
            <table width="100%" border="1" cellspacing="0" cellpadding="3" bordercolor="#000000">
                <tr>
                    <td bgcolor="#D2A41C" bordercolor="#000000" align="left" nowrap>
                        <font face="Verdana" size="+1"><i>Administration</i>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
                    </td>
                </tr>
                <tr>
                    <td bgcolor="#FFDC75" bordercolor="#000000" nowrap>
                        <a href="manager/status">Status</a><br>
                        <a href="admin">Tomcat Administration</a><br>
                        <a href="manager/html">Tomcat Manager</a><br>
                        &nbsp;
                    </td>
                </tr>
            </table>
            <br>
            <table width="100%" border="1" cellspacing="0" cellpadding="3" bordercolor="#000000">
                <tr>
                    <td bgcolor="#D2A41C" bordercolor="#000000" align="left" nowrap>
                        <font face="Verdana" size="+1"><i>Documentation</i>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
                    </td>
                </tr>
                <tr>
                    <td bgcolor="#FFDC75" bordercolor="#000000" nowrap>
                        <a href="RELEASE-NOTES.txt">Release Notes</a><br>
                        <a href="tomcat-docs/changelog.html">Change Log</a><br>
                        <a href="tomcat-docs">Tomcat Documentation</a><br>
                        &nbsp;
                    </td>
                </tr>
            </table>
            <br>
            <table width="100%" border="1" cellspacing="0" cellpadding="3" bordercolor="#000000">
                <tr>
                    <td bgcolor="#D2A41C" bordercolor="#000000" align="left" nowrap>
                        <font face="Verdana" size="+1"><i>Tomcat Online</i>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
                    </td>
                </tr>
                <tr>
                    <td bgcolor="#FFDC75" bordercolor="#000000" nowrap>
                        <a href="http://jakarta.apache.org/tomcat/">Home Page</a><br>
                        <a href="http://jakarta.apache.org/tomcat/bugreport.html">Bug Database</a><br>
                        <a href="http://issues.apache.org/bugzilla/buglist.cgi?bug_status=UNCONFIRMED&amp;bug_status=NEW&amp;bug_status=ASSIGNED&amp;bug_status=REOPENED&amp;bug_status=RESOLVED&amp;resolution=LATER&amp;resolution=REMIND&amp;resolution=---&amp;email1=&amp;emailtype1=substring&amp;emailassigned_to1=1&amp;email2=&amp;emailtype2=substring&amp;emailreporter2=1&amp;bugidtype=include&amp;bug_id=&amp;changedin=&amp;votes=&amp;chfieldfrom=&amp;chfieldto=Now&amp;chfieldvalue=&amp;product=Tomcat+5&amp;short_desc=&amp;short_desc_type=allwordssubstr&amp;long_desc=&amp;long_desc_type=allwordssubstr&amp;bug_file_loc=&amp;bug_file_loc_type=allwordssubstr&amp;keywords=&amp;keywords_type=anywords&amp;field0-0-0=noop&amp;type0-0-0=noop&amp;value0-0-0=&amp;cmdtype=doit&amp;order=%27Importance%27">Open Bugs</a><br>
                        <a href="http://nagoya.apache.org/eyebrowse/SummarizeList?listId=88">Users Mailing List</a><br>
                        <a href="http://nagoya.apache.org/eyebrowse/SummarizeList?listId=46">Developers Mailing List</a><br>
                        <a href="irc://irc.freenode.net/#tomcat">IRC</a><br>
                        &nbsp;
                    </td>
                </tr>
            </table>
            <br>
            <table width="100%" border="1" cellspacing="0" cellpadding="3" bordercolor="#000000">
                <tr>
                    <td bgcolor="#D2A41C" bordercolor="#000000" align="left" nowrap>
                        <font face="Verdana" size="+1"><i>Examples</i>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
                    </td>
                </tr>
                <tr>
                    <td bgcolor="#FFDC75" bordercolor="#000000" nowrap>
                        <a href="jsp-examples/">JSP Examples</a><br>
                        <a href="servlets-examples/">Servlet Examples</a><br>
                        <a href="webdav/">WebDAV capabilities</a><br>
                        &nbsp;
                    </td>
                </tr>
            </table>
            <br>
            <table width="100%" border="1" cellspacing="0" cellpadding="3" bordercolor="#000000">
                <tr>
                    <td bgcolor="#D2A41C" bordercolor="#000000" align="left" nowrap>
                        <font face="Verdana" size="+1"><i>Miscellaneous</i>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>
                    </td>
                </tr>
                <tr>
                    <td bgcolor="#FFDC75" bordercolor="#000000" nowrap>
                        <a href="http://java.sun.com/products/jsp">Sun's Java Server Pages Site</a><br>
                        <a href="http://java.sun.com/products/servlet">Sun's Servlet Site</a><br>
                        &nbsp;
                    </td>
                </tr>
            </table>
        </td>

        <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>

        <!-- Body -->
        <td align="left" valign="top">
            <p><center><b>If you're seeing this page via a web browser, it means you've setup Tomcat successfully. Congratulations!</b></center></p>

            <p>As you may have guessed by now, this is the default Tomcat home page. It can be found on the local filesystem at:
            <blockquote>
                <p class="code">$CATALINA_HOME/webapps/ROOT/index.jsp</p>
            </blockquote>
            </p>

            <p>where "$CATALINA_HOME" is the root of the Tomcat installation directory. If you're seeing this page, and you don't think you should be, then either you're either a user who has arrived at new installation of Tomcat, or you're an administrator who hasn't got his/her setup quite right. Providing the latter is the case, please refer to the <a href="tomcat-docs">Tomcat Documentation</a> for more detailed setup and administration information than is found in the INSTALL file.</p>

            <p><b>NOTE: For security reasons, using the administration webapp
            is restricted to users with role "admin". The manager webapp
            is restricted to users with role "manager".</b>
            Users are defined in <code>$CATALINA_HOME/conf/tomcat-users.xml</code>.</p>

            <p>Included with this release are a host of sample Servlets and JSPs (with associated source code), extensive documentation (including the Servlet 2.4 and JSP 2.0 API JavaDoc), and an introductory guide to developing web applications.</p>

            <p>Tomcat mailing lists are available at the Jakarta project web site:</p>

           <ul>
               <li><b><a href="mailto:tomcat-user-subscribe@jakarta.apache.org">tomcat-user@jakarta.apache.org</a></b> for general questions related to configuring and using Tomcat</li>
               <li><b><a href="mailto:tomcat-dev-subscribe@jakarta.apache.org">tomcat-dev@jakarta.apache.org</a></b> for developers working on Tomcat</li>
           </ul>

            <p>Thanks for using Tomcat!</p>

            <p align="right"><font size=-1><img src="tomcat-power.gif" width="77" height="80"></font><br>
            &nbsp;
            <font size=-1>Copyright &copy; 1999-2004 Apache Software Foundation</font><br>
            <font size=-1>All Rights Reserved</font> <br>
            &nbsp;</p>
            <p align="right">&nbsp;</p>

        </td>

    </tr>
</table>

</body>
</html>
