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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: TableHandler.java,v 1.1 2006/06/29 18:51:45 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletOutputStream;
import java.io.IOException;

/**
 * Handler for parsing the log and displaying a log record in a
 * table format showing the essential fields of each log record.
 *
 * @author Akara Sucharitakul
 */
class TableHandler extends LogReader.LogHandler {

    LogBuffer logBuffer = new LogBuffer(20);
    boolean displayEnd = false;

    public TableHandler(long start) {
        if (start == -1)
            displayEnd = true;
        else {
            begin = start;
            end = start + buffer.capacity();
        }
    }

    public void processRecord() {

        LogReader.LogRecord oldLog = logBuffer.add(logRecord);

        // Recycle the old records.
        if (oldLog == null)
            logRecord = new LogReader.LogRecord();
        else {
            logRecord = oldLog;
            logRecord.clear();
        }
    }

    public void processDetail(String qName) {
        if ("exception".equals(qName))
            logRecord.exceptionFlag = true;
    }

    public void printHtml(HttpServletRequest request,
                          ServletOutputStream out, String runId)
            throws IOException {

        String requestBase = request.getRequestURI() + "?runId=" +
                    runId;

        // Recalculate the real begin based on the parser.
        begin = recordCount - logBuffer.size();
        if (begin < 0l)
            begin = 0l;

        // Prepare the navigation links
        StringBuffer naviBuffer = new StringBuffer(256);
        if (begin > 0l) {
            naviBuffer.append("<a href=\"" + requestBase + "\">Top</a>\n");
            long prevPage = begin - logBuffer.capacity() / 2l;
            if (prevPage < 0l)
                prevPage = 0l;
            naviBuffer.append("<a href=\"" + requestBase + "&startId=" +
                    prevPage + "\">PgUp</a>\n");
        } else {
            naviBuffer.append("Top PgUp ");
        }

        long nextPage = begin + logBuffer.size() / 2l;
        naviBuffer.append("<a href=\"" + requestBase  + "&startId=" +
                nextPage + "\">PgDn</a>\n");

        naviBuffer.append("<a href=\"" + requestBase +
                "&startId=end#end\">Bottom</a>");
        String naviBar = naviBuffer.toString();


        // Write the header.
        out.println("<html>");
        out.print("<head><title>Logs: RunID " + runId);
        out.println("</title>");
        out.println("<link rel=\"icon\" type=\"image/gif\" href=\"" +
                    request.getContextPath() + "/img/faban.gif\">");        
        if (displayEnd && !xmlComplete)
            out.print("<meta http-equiv=\"refresh\" content=\"10\">");
        out.println("</head><body>");
        out.println(naviBar);
        out.println("<hr><table border=\"1\" cellpadding=\"2\" " +
                "cellspacing=\"0\">");
        out.println("<tbody>");
        out.println("<tr>");
        out.println("<th>Time</th>");
        out.println("<th>Host</th>");
        out.println("<th>Level</th>");
        out.println("<th>Message</th>");
        out.println("<th>Thread</th>");
        out.println("<th>Source</th>");
        out.println("</tr>");

        // Write the records.
        int size = logBuffer.size();
        for (int i = 0; i < size; i++)
            printRow(logBuffer.get(i),out, requestBase);

        // Write the trailer.
        out.println("</tbody></table><a name=\"end\"><hr></a>");
        out.println(naviBar);
        out.println("</body></html>");
    }

    private void printRow(LogReader.LogRecord record, ServletOutputStream out,
                          String requestBase)
            throws IOException {
        out.println("<tr>");
        out.println("<td>" + record.date + "</td>");
        if (record.host == null) {
            out.println("<td>&nbsp;</td>");
        } else {
            int endHostName = record.host.indexOf('.');
            if (endHostName > 0)
                record.host = record.host.substring(0, endHostName);
            out.println("<td style=\"text-align: center;\">" + record.host +
                    "</td>");
        }
        if ("SEVERE".equals(record.level)) {
            out.print("<td style=\"text-align: center; font-weight: " +
                    "bold; color: rgb(255, 0, 0);\">");
        } else if ("WARNING".equals(record.level)) {
            out.print("<td style=\"text-align: center; font-weight: " +
                    "bold; color: rgb(255, 102, 51);\">");
        } else if ("INFO".equals(record.level)) {
            out.print("<td style=\"text-align: center; font-weight: " +
                    "bold; color: rgb(0, 192, 0);\">");
        } else if ("CONFIG".equals(record.level)) {
            out.print("<td style=\"text-align: center; font-weight: " +
                    "bold;\">");
        } else {
            out.print("<td style=\"text-align: center;\">");
        }
        out.print(record.level);
        if (record.exceptionFlag)
            out.println("<font size=\"-1\"><i><br><a href=\"" +
                    requestBase + "&exception=" + record.id +
                    "\">exception</a></i></font></td>");
        else
            out.println("</td>");
        out.println("<td>" + record.message + "</td>");
        out.println("<td style=\"text-align: center;\">" + record.thread +
                "</td>");
        out.println("<td>" + record.clazz + '.' + record.method + "</td>");
        out.println("</tr>");
    }

    static class LogBuffer {
        LogReader.LogRecord[] buffer = null;
        int bufPtr = 0;
        boolean wrapped = false;

        public LogBuffer(int size) {
            buffer = new LogReader.LogRecord[size];
        }

        public LogReader.LogRecord add(LogReader.LogRecord record) {
            LogReader.LogRecord oldRecord = buffer[bufPtr];
            buffer[bufPtr++] = record;
            if (bufPtr >= buffer.length) {
                bufPtr = 0;
                wrapped = true;
            }
            return oldRecord;
        }

        public LogReader.LogRecord get(int id) {
            if (wrapped)
                id += bufPtr;
            if (id >= buffer.length)
                id -= buffer.length;
            return buffer[id];
        }

        public int size() {
            if (!wrapped)
                return bufPtr;
            return buffer.length;
        }

        public int capacity() {
            return buffer.length;
        }

        public boolean filled() {
            return wrapped;
        }
    }
}
