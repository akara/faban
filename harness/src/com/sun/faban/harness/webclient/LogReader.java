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
 * $Id: LogReader.java,v 1.2 2006/06/29 19:38:44 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import com.sun.faban.harness.common.Config;

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

        LogHandler handler = null;
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
                handler = new TableHandler(start);
            else
                handler = new RecordHandler(start);
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

        ServletOutputStream out = response.getOutputStream();
        handler.printHtml(request, out, runId);
        out.flush();
        out.close();
        response.flushBuffer();
    }


    /**
     * LogRecord contains all the basic fields used from a logRecord.
     */
    static class LogRecord {
        long id = -1;
        String date;
        String host;
        String level;
        String clazz;
        String method;
        String thread;
        String message;
        boolean exceptionFlag = false;

        public void clear() {
            id = -1;
            date = null;
            host = null;
            level = null;
            clazz = null;
            method = null;
            thread = null;
            message = null;
            exceptionFlag = false;
        }
    }

    /**
     * The superclass of all log handlers provides all basic services
     * to be subclassed by specific handlers or display formatters.
     */
    static abstract class LogHandler extends DefaultHandler {

        long recordCount = 0;
        long begin = 0;
        long end = Long.MAX_VALUE;

        ArrayList stack = new ArrayList();
        StringBuffer buffer = new StringBuffer();
        LogRecord logRecord = new LogRecord();
        boolean xmlComplete = false; // Sets by the caller if parsing complete

        /**
         * Receive notification of the start of an element.
         * <p/>
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the start of
         * each element (such as allocating a new tree node or writing
         * output to a file).</p>
         *
         * @param uri        The Namespace URI, or the empty string if the
         *                   element has no Namespace URI or if Namespace
         *                   processing is not being performed.
         * @param localName  The local name (without prefix), or the
         *                   empty string if Namespace processing is not being
         *                   performed.
         * @param qName      The qualified name (with prefix), or the
         *                   empty string if qualified names are not available.
         * @param attributes The attributes attached to the element.  If
         *                   there are no attributes, it shall be an empty
         *                   Attributes object.
         * @throws org.xml.sax.SAXException Any SAX exception, possibly
         *                                  wrapping another exception.
         * @see org.xml.sax.ContentHandler#startElement
         */
        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {

            // Put the QName into the stack
            stack.add(qName);
        }

        /**
         * Receive notification of the end of an element.
         * <p/>
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the end of
         * each element (such as finalising a tree node or writing
         * output to a file).</p>
         *
         * @param uri       The Namespace URI, or the empty string if the
         *                  element has no Namespace URI or if Namespace
         *                  processing is not being performed.
         * @param localName The local name (without prefix), or the
         *                  empty string if Namespace processing is not being
         *                  performed.
         * @param qName     The qualified name (with prefix), or the
         *                  empty string if qualified names are not available.
         * @throws org.xml.sax.SAXException Any SAX exception, possibly
         *                                  wrapping another exception.
         * @see org.xml.sax.ContentHandler#endElement
         */
        public void endElement(String uri, String localName, String qName)
                throws SAXException {

            int depth = stack.size();

            if (!stack.remove(depth - 1).equals(qName))
                throw new SAXException("endElement mismatch: " + qName);
            if ("record".equals(qName)) {
                logRecord.id = recordCount;
                if (recordCount >= begin)
                    processRecord();
                if (++recordCount >= end)
                    throw new SAXParseException(
                            "End request range, abort processing!", null);
            } else if (recordCount >= begin) {
                Object parent = null;
                if (depth >= 2)
                    parent = stack.get(depth - 2);
                if ("host".equals(qName))
                    logRecord.host = buffer.toString().trim();
                else if ("date".equals(qName))
                    logRecord.date = buffer.toString().trim();
                else if ("level".equals(qName))
                    logRecord.level = buffer.toString().trim();
                else if ("class".equals(qName) && "record".equals(parent))
                    logRecord.clazz = buffer.toString().trim();
                else if ("method".equals(qName) && "record".equals(parent))
                    logRecord.method = buffer.toString().trim();
                else if ("thread".equals(qName))
                    logRecord.thread = buffer.toString().trim();
                else if ("message".equals(qName) && "record".equals(parent)) {
                    logRecord.message = formatMessage(buffer.toString().trim());
                }
                else
                    processDetail(qName);
            }
            buffer.setLength(0);
        }

        /**
         * Formats a multi-line message into html line breaks
         * for readability.
         * @param message The message to be formatted.
         * @return The new formatted message.
         */
        String formatMessage(String message) {
            int idx = message.indexOf('\n');
            if (idx == -1) // If there's no \n, don't even hassle.
                return message;
            StringBuffer msg = new StringBuffer(message);
            String crlf = "<br>";
            while (idx != -1) {
                msg.replace(idx, idx + 1, crlf);
                idx = msg.indexOf("\n", idx + crlf.length());
            }
            return msg.toString();
        }

        /**
         * The processRecord method allows subclasses to define
         * how a record should be processed.
         * @throws SAXException If the processing should stop.
         */
        public abstract void processRecord() throws SAXException;

        /**
         * Prints the html result of the parsing to the servlet output.
         * @param request The servlet request
         * @param out The servlet output stream
         * @param runId The run id
         * @throws IOException Error writing to the servlet output stream
         */
        public abstract void printHtml(HttpServletRequest request,
                              ServletOutputStream out, String runId)
                throws IOException;

        /**
         * The processDetail method allows subclasses to process
         * the exceptions not processed by default. This is called
         * from endElement.
         * @param qName The element qName
         * @throws SAXException If the processing should stop.
         */
        public abstract void processDetail(String qName) throws SAXException;

        /**
         * Receive notification of character data inside an element.
         * <p/>
         * <p>By default, do nothing.  Application writers may override this
         * method to take specific actions for each chunk of character data
         * (such as adding the data to a node or buffer, or printing it to
         * a file).</p>
         *
         * @param ch     The characters.
         * @param start  The start position in the character array.
         * @param length The number of characters to use from the
         *               character array.
         * @throws org.xml.sax.SAXException Any SAX exception, possibly
         *                                  wrapping another exception.
         * @see org.xml.sax.ContentHandler#characters
         */
        public void characters(char ch[], int start, int length)
                throws SAXException {
            if (recordCount >= begin)
                buffer.append(ch, start, length);
        }
    }
}