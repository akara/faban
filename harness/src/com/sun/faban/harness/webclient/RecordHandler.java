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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Handler for parsing the log and displaying a log record in full
 * detail inclusing the exceptions.
 *
 * @author Akara Sucharitakul
 */
class RecordHandler extends LogParseHandler {

    LogRecordDetail detail = new LogRecordDetail();
    ExceptionRecord exception = new ExceptionRecord();
    StackFrame frame = new StackFrame();
    ArrayList<StackFrame> stackFrames = new ArrayList<StackFrame>();

    /**
     * Handles the records.
     * @param recordId The record id
     * @param request The request object
     * @param out The output stream
     * @param runId The run id
     */
    public RecordHandler(long recordId, HttpServletRequest request, 
                                ServletOutputStream out, String runId) {
        super(request, out, runId);
        begin = recordId;
        end = recordId + 1;
    }

    /**
     * Processes the record.
     */
    public void processRecord() {
        // Noop. All work is done in the superclass.
    }

    /**
     * Process the details.
     * @param qName The element name
     */
    public void processDetail(String qName) {
        if ("millis".equals(qName))
            detail.millis = buffer.toString().trim();
        else if ("sequence".equals(qName))
            detail.sequence = buffer.toString().trim();
        else if ("logger".equals(qName))
            detail.logger = buffer.toString().trim();
        else if ("message".equals(qName))
            exception.message = buffer.toString().trim();
        else if ("class".equals(qName))
            frame.clazz = buffer.toString().trim();
        else if ("method".equals(qName))
            frame.method = buffer.toString().trim();
        else if ("line".equals(qName))
            frame.line = buffer.toString().trim();
        else if ("frame".equals(qName)) {
            stackFrames.add(frame);
            frame = new StackFrame();
        } else if ("exception".equals(qName)) {
            StackFrame[] frameArray =
                    new StackFrame[stackFrames.size()];
            exception.stackFrames = stackFrames.toArray(frameArray);
            stackFrames.clear();
            logRecord.exceptionFlag = true;
        }
    }

    /**
     * Prints the details to the screen.
     * @throws java.io.IOException
     */
    public void printHtml() throws IOException {
        out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
        out.println("<html>");
        out.println("<head><title>LogRecord: RunID " + runId + "</title>");
        out.println("<link rel=\"icon\" type=\"image/gif\" href=\"" +
                    request.getContextPath() + "/img/faban.gif\">");
        out.println("<link rel=\"stylesheet\" type=\"text/css\" " +
                    "href=\"/css/style.css\"/>");
        out.println("</head><body>");
        out.println("<table border=\"0\" cellpadding=\"2\" " +
                "cellspacing=\"0\" style=\"text-align: left; " +
                "width: 50%;\"><tbody>");
        out.println("<tr><th>Record:</th><td>" + logRecord.id + "</td>");
        out.println("<td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>");
        out.println("<th>Level:</th>");
        if ("SEVERE".equals(logRecord.level)) {
            out.print("<td style=\"font-weight: bold; color: " +
                    "rgb(255, 0, 0);\">");
        } else if ("WARNING".equals(logRecord.level)) {
            out.print("<td style=\"font-weight: bold; color: " +
                    "rgb(255, 102, 51);\">");
        } else if ("INFO".equals(logRecord.level)) {
            out.print("<td style=\"font-weight: bold; color: " +
                    "rgb(0, 192, 0);\">");
        } else if ("CONFIG".equals(logRecord.level)) {
            out.print("<td style=\"font-weight: bold;\">");
        } else {
            out.print("<td>");
        }
        out.println(logRecord.level + "</td></tr></table></br>");
        out.println("<span style=\"font-weight: bold;\">Message:</span>");
        out.println("<table border=\"0\" cellpadding=\"4\" cellspacing=\"3\" " +
                    "style=\"padding: 2px; border: 2px solid #cccccc; " +
                    "text-align: left; width: 100%;\">");
        out.print("<tbody><tr class=\"even\"><td>");
        out.print(formatMessage(logRecord.message));
        out.println("</td></tr></tbody></table>");
        out.println("<hr style=\"border: 1px solid #cccccc;\">" +
                    "<h2>Details:</h2>");

        out.println("<table border=\"0\" cellpadding=\"4\" cellspacing=\"3\" " +
                    "style=\"padding: 2px; border: 2px solid #cccccc; " +
                    "text-align: center; width: 100%;\"><tbody><tr>");
        out.println("<th class=\"header\">Host</th>");
        out.println("<th class=\"header\">Sequence</th>");
        out.println("<th class=\"header\">Date</th>");
        out.println("<th class=\"header\">Millis</th></tr><tr class=\"even\">");
        if (logRecord.host == null) {
            out.println("<td class=\"tablecell\">&nbsp;</td>");
        } else {
            int endHostName = logRecord.host.indexOf('.');
            if (endHostName > 0)
                logRecord.host = logRecord.host.substring(0, endHostName);
            out.println("<td  class=\"tablecell\" style=\"text-align: center;\">" +
                    logRecord.host + "</td>");
        }
        out.println("<td class=\"tablecell\">" + detail.sequence + "</td>");
        out.println("<td class=\"tablecell\">" + logRecord.date + "</td>");
        out.println("<td class=\"tablecell\">" + detail.millis + "</td></tr></tbody></table>");

        out.print("<br><span style=\"font-weight: bold;\">Logger:" +
                "</span>&nbsp;");
        out.println(detail.logger);

        out.println("<table border=\"0\" cellpadding=\"4\" cellspacing=\"3\" " +
                    "style=\"padding: 2px; border: 2px solid #cccccc; " +
                    "text-align: center; width: 100%;\"><tbody><tr>");
        out.println("<th class=\"header\">Thread</th>");
        out.println("<th class=\"header\">Class</th>");
        out.println("<th class=\"header\">Method</th></tr><tr class=\"even\">");
        out.println("<td class=\"tablecell\">" + logRecord.thread + "</td>");
        out.println("<td class=\"tablecell\">" + logRecord.clazz + "</td>");
        out.println("<td class=\"tablecell\">" + logRecord.method + "</td></tr></tbody>" +
                "</table>");

        if (logRecord.exceptionFlag) {
            out.println("<hr style=\"border: 1px solid #cccccc;\">" +
                        "<h2>Exception:</h2>");
            out.println("<span style=\"font-weight: bold;\">Message:" +
                    "</span>");
            out.println("<table border=\"0\" cellpadding=\"4\" " +
                        "cellspacing=\"3\" style=\"padding: 2px; border: 2px " +
                        "solid #cccccc; text-align: left; width: 100%;\">");
            out.print("<tbody><tr class=\"even\"><td>");
            out.print(formatMessage(exception.message));
            out.println("</td></tr></tbody></table>");
            out.print("<br><span style=\"font-weight: bold;\">" +
                    "Stack Trace:</span>");
            out.println("<table border=\"0\" cellpadding=\"4\" " +
                        "cellspacing=\"3\" style=\"padding: 2px; border: 2px " +
                        "solid #cccccc; text-align: center; width: 100%;\">" +
                        "<tbody><tr>");
            out.println("<th class=\"header\" style=\"text-align: left;\">" +
                        "Class</th>");
            out.println("<th class=\"header\">Method</th>");
            out.println("<th class=\"header\">Line</th></tr>");
            for (int i = 0; i < exception.stackFrames.length; i++) {
                StackFrame frame = exception.stackFrames[i];
                out.println("<tr class=\"" + ROWCLASS[i % 2] +
                            "\"><td class=\"tablecell\" style=\"text-align: left;\">" +
                            frame.clazz + "</td>");
                if (frame.method == null || frame.method.length() == 0)
                    frame.method="&nbsp;";
                // We got to be careful with method names like <init>
                // They won't display so we need to convert them.
                int ltIdx = frame.method.indexOf('<');
                int gtIdx = frame.method.indexOf('>');
                if (ltIdx != -1 || gtIdx != -1) {
                    StringBuffer mBuffer = new StringBuffer(frame.method);
                    while (ltIdx != -1) {
                        mBuffer.replace(ltIdx, ltIdx + 1, "&lt;");
                        ltIdx = mBuffer.indexOf("<", ltIdx + 4);
                    }

                    // We changed the indexes from the last replace.
                    // We need to search again.
                    gtIdx = mBuffer.indexOf(">");
                    while (gtIdx != -1) {
                        mBuffer.replace(gtIdx, gtIdx + 1, "&gt;");
                        gtIdx = mBuffer.indexOf(">", gtIdx + 4);
                    }
                    frame.method = mBuffer.toString();
                }

                out.println("<td class=\"tablecell\">" + frame.method + "</td>");
                if (frame.line == null || frame.line.length() == 0)
                    frame.line="&nbsp;";
                out.println("<td class=\"tablecell\">" + frame.line + "</td></tr>");
            }
            out.println("</tbody></table>");
        }
        out.println("</body></html>");
    }
}
