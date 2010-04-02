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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Handler for parsing the log and displaying a log record in a
 * table format showing the essential fields of each log record.
 *
 * @author Akara Sucharitakul
 */
class TableHandler extends LogParseHandler {
    boolean displayEnd = false;
    boolean headerWritten = false;
    String requestBase;
    LogBuffer logBuffer;

    public TableHandler(long start, HttpServletRequest request,
                                ServletOutputStream out, String runId) {        
        super(request, out, runId);
        requestBase = request.getRequestURI() + "?runId=" + runId;
        if (Config.LOG_VIEW_BUFFER_SIZE > 0){
            logBuffer = new LogBuffer(Config.LOG_VIEW_BUFFER_SIZE);
        }
        if (start == -1) {
            displayEnd = true;
        } else if (logBuffer == null){
            begin = start;
            end = Long.MAX_VALUE;
        } else {
            begin = start;
            end = start + logBuffer.capacity();
        }
    }

    /**
     * Processes the records.
     * @throws java.io.IOException
     */
    @Override
    public void processRecord() throws IOException {
        if (logBuffer != null) {
            LogRecord oldLog = logBuffer.add(logRecord);

            // Recycle the old records.
            if (oldLog == null)
                logRecord = new LogRecord();
            else {
                logRecord = oldLog;
                logRecord.clear();
            }
        }else{
            if (!headerWritten) {
                printHeader(null);
                headerWritten = true;
            }
            printRow(begin + recordCount++, logRecord, requestBase);
            logRecord.clear();
        }
    }

    /**
     * Processes the details.
     * @param qName The element name
     */
    public void processDetail(String qName) {
        if ("exception".equals(qName))
            logRecord.exceptionFlag = true;
    }

    /**
     * Prints the table to the screen.
     * @throws java.io.IOException
     */
    public void printHtml()
            throws IOException {
        
        String naviBar = null;
        
        if (logBuffer != null) {

            // Recalculate the real begin based on the parser.
            begin = recordCount - logBuffer.size();
            if (begin < 0l) {
                begin = 0l;
            }

            // Prepare the navigation links
            StringBuilder naviBuffer = new StringBuilder(256);
            if (begin > 0l) {
                naviBuffer.append("<a href=\"").append(requestBase).
                        append("\">Top</a>\n");
                long prevPage = begin - logBuffer.capacity() / 2l;
                if (prevPage < 0l) {
                    prevPage = 0l;
                }
                naviBuffer.append("<a href=\"").append(requestBase).
                        append("&startId=").append(prevPage).
                        append("\">PgUp</a>\n");
            } else {
                naviBuffer.append("Top PgUp ");
            }

            long nextPage = begin + logBuffer.size() / 2l;
            naviBuffer.append("<a href=\"").append(requestBase).
                    append("&startId=").append(nextPage).
                    append("\">PgDn</a>\n");

            naviBuffer.append("<a href=\"").append(requestBase).
                    append("&startId=end#end\">Bottom</a>");
            naviBar = naviBuffer.toString();

            printHeader(naviBar);

            // Write the records.
            int size = logBuffer.size();
            for (int i = 0; i < size; i++) {
                printRow(begin + i, logBuffer.get(i), requestBase);
            }
        }
        printTrailer(naviBar);
    }
    
    private void printHeader(String naviBar) throws IOException {
        // Write the header.
        out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
        out.println("<html>");
        out.print("<head><title>Logs: RunID " + runId);
        out.println("</title>");
        out.println("<link rel=\"stylesheet\" type=\"text/css\" " +
                    "href=\"/css/style.css\" />");
        out.println("<link rel=\"stylesheet\" type=\"text/css\" " +
                    "href=\"/css/balloontip2.css\" />");
        out.println("<script type=\"text/javascript\" " +
                    "src=\"/scripts/balloontip2.js\"></script>");
        out.println("<link rel=\"icon\" type=\"image/gif\" href=\"" +
                    request.getContextPath() + "/img/faban.gif\">");        
        if (displayEnd && !xmlComplete)
            out.print("<meta http-equiv=\"refresh\" content=\"10\">");
        out.println("</head><body>");
        if (naviBar != null)
            out.println(naviBar);
        out.println("<hr style=\"border: 1px solid #cccccc;\">" +
                    "<table border=\"0\" cellpadding=\"4\" " +
                    "cellspacing=\"3\" style=\"padding: 2px; border: " +
                    "2px solid #cccccc;\">");
        out.println("<tbody>");
        out.println("<tr>");
        out.println("<th class=\"header\">Time</th>");
        out.println("<th class=\"header\">Host</th>");
        out.println("<th class=\"header\">Level</th>");
        out.println("<th class=\"header\">Message</th>");
        //out.println("<th class=\"header\">Thread</th>");
        //out.println("<th class=\"header\">Source</th>");
        out.println("</tr>");        
    }
    
    private void printTrailer(String naviBar) throws IOException {
        // Write the trailer.
        out.println("</tbody></table><a name=\"end\">" +
                "<hr style=\"border: 1px solid #cccccc;\"></a>");
        if (naviBar != null)
            out.println(naviBar);
        out.println("</body></html>");        
    }

    private void printRow(long sequence, LogRecord record, String requestBase)
            throws IOException {
        String dt = record.date;
        dt = dt.substring(dt.lastIndexOf("T")+1, dt.length());
        String thread = "Thread: " + record.thread;
        String source = "Source: " + record.clazz + '.' + record.method;       
        String content = thread + "<br/>" + source;
        String msgmouseover = "onmouseover=\"showtip('"+ content +"', '"+ content.length() * 6 +"')\" onmouseout=\"hideddrivetip()\"";
        String datemouseover = "onmouseover=\"showtip('"+ record.date +"')\" onmouseout=\"hideddrivetip()\"";
        out.print("<tr class=\"" + ROWCLASS[(int) (sequence % 2l)] + "\">");
        out.println("<td " + datemouseover + "  class=\"tablecell\">" + dt + "</td>");
        if (record.host == null) {
            out.println("<td class=\"tablecell\">&nbsp;</td>");
        } else {
            int endHostName = record.host.indexOf('.');
            if (endHostName > 0)
                record.host = record.host.substring(0, endHostName);
            out.println("<td class=\"tablecell\" style=\"text-align: center;\">" + record.host +
                    "</td>");
        }
        if ("SEVERE".equals(record.level)) {
            out.print("<td class=\"tablecell\" style=\"text-align: center; font-weight: " +
                    "bold; color: rgb(255, 0, 0);\">");
        } else if ("WARNING".equals(record.level)) {
            out.print("<td class=\"tablecell\" style=\"text-align: center; font-weight: " +
                    "bold; color: rgb(255, 102, 51);\">");
        } else if ("INFO".equals(record.level)) {
            out.print("<td class=\"tablecell\" style=\"text-align: center; font-weight: " +
                    "bold; color: rgb(0, 192, 0);\">");
        } else if ("CONFIG".equals(record.level)) {
            out.print("<td class=\"tablecell\" style=\"text-align: center; font-weight: " +
                    "bold;\">");
        } else {
            out.print("<td class=\"tablecell\" style=\"text-align: center;\">");
        }
        out.print(record.level);
        if (record.exceptionFlag)
            out.println("<font size=\"-1\"><i><br><a href=\"" +
                    requestBase + "&exception=" + record.id +
                    "\">exception</a></i></font></td>");
        else
            out.println("</td>");
        out.println("<td " + msgmouseover + " class=\"tablecell\">" + formatMessage(record.message) + "</td>");
        //out.println("<td class=\"tablecell\" style=\"text-align: center;\">" + record.thread +
        //        "</td>");
        //out.println("<td class=\"tablecell\">" + record.clazz + '.' + record.method + "</td>");
        out.println("</tr>");
    }

    static class LogBuffer {
        LogRecord[] buffer = null;
        int bufPtr = 0;
        boolean wrapped = false;

        public LogBuffer(int size) {
            buffer = new LogRecord[size];
        }

        public LogRecord add(LogRecord record) {
            LogRecord oldRecord = buffer[bufPtr];
            buffer[bufPtr++] = record;
            if (bufPtr >= buffer.length) {
                bufPtr = 0;
                wrapped = true;
            }
            return oldRecord;
        }

        public LogRecord get(int id) {
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
