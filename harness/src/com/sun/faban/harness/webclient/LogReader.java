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
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.common.Config;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A servlet to read the sometimes incomplete log file, complete it, and
 * translate it into readable html format. This servlet does not currntly
 * contain any log filtering options but these can be added in the future.
 *
 * @author Akara Sucharitakul
 */
public class LogReader extends HttpServlet {

    ServletContext ctx;

    public void init() throws ServletException {
        ctx = getServletContext();
    }

    public void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {
        String runId = request.getParameter("runId");
        String startId = request.getParameter("startId");
        String exString = request.getParameter("exception");

        // Check that either startId or exception has to be specified.
        if (startId != null && exString != null)
            throw new ServletException("Either the startId or exception " +
                    "parameter has to be specified, but not both!");

        // Check the input file
        if (runId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "runId parameter not specified");
            return;
        }

        String logFilePath = Config.OUT_DIR + runId +
                File.separator + Config.LOG_FILE;
        InputStream is = new FileInputStream(logFilePath);
        if (is == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "Logfile " + logFilePath + " not found");
            return;
        }

        // Check the start line
        long start = -1l;
        if (startId == null)
            start = 0;
        else if (!"end".equals(startId))
            start = Long.parseLong(startId);

        // Check the exception to display
        boolean showException = false;
        if (exString != null) {
            start = Long.parseLong(exString);
            showException = true;
        }

        ServletOutputStream out = response.getOutputStream();
        LogParseHandler handler = null;

        try {
            SAXParserFactory sFact = SAXParserFactory.newInstance();
            sFact.setFeature("http://xml.org/sax/features/validation", false);
            sFact.setFeature("http://apache.org/xml/features/" +
                    "allow-java-encodings", true);
            sFact.setFeature("http://apache.org/xml/features/nonvalidating/" +
                    "load-dtd-grammar", false);
            sFact.setFeature("http://apache.org/xml/features/nonvalidating/" +
                    "load-external-dtd", false);
            SAXParser parser = sFact.newSAXParser();
            if (!showException)
                handler = new TableHandler(start, request, out, runId);
            else
                handler = new RecordHandler(start, request, out, runId);
            parser.parse(is, handler);
            handler.xmlComplete = true; // If we get here, the XML is good.
        } catch (ParserConfigurationException e) {
            throw new ServletException(e);
        } catch (SAXParseException e) {
            Throwable t = e.getCause();
            // If it is caused by an IOException, we'll just throw it.
            if (t != null && t instanceof IOException)
                throw (IOException) t;
            // Otherwise if the XML enclosure is missing, we'll just ignore.
            handler.xmlComplete = false;
        } catch (SAXException e) {
            throw new ServletException(e);
        }

        response.setContentType("text/html");
        handler.printHtml();
        out.flush();
        out.close();
        response.flushBuffer();
    }
}