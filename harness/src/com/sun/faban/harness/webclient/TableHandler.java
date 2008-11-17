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
 * $Id: TableHandler.java,v 1.6 2008/11/17 20:45:03 sheetalpatil Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.common.Config;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletOutputStream;
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

    @Override
    public void processRecord() {
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
            if (headerWritten == false) {
                try {
                    printHeader(null);
                    headerWritten = true;
                } catch (IOException ex) {
                    Logger.getLogger(TableHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            try {
                printRow(logRecord, requestBase);
            } catch (IOException ex) {
                Logger.getLogger(TableHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void processDetail(String qName) {
        if ("exception".equals(qName))
            logRecord.exceptionFlag = true;
    }

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
                naviBuffer.append("<a href=\"" + requestBase + "\">Top</a>\n");
                long prevPage = begin - logBuffer.capacity() / 2l;
                if (prevPage < 0l) {
                    prevPage = 0l;
                }
                naviBuffer.append("<a href=\"" + requestBase + "&startId=" +
                        prevPage + "\">PgUp</a>\n");
            } else {
                naviBuffer.append("Top PgUp ");
            }

            long nextPage = begin + logBuffer.size() / 2l;
            naviBuffer.append("<a href=\"" + requestBase + "&startId=" +
                    nextPage + "\">PgDn</a>\n");

            naviBuffer.append("<a href=\"" + requestBase +
                    "&startId=end#end\">Bottom</a>");
            naviBar = naviBuffer.toString();

            printHeader(naviBar);

            // Write the records.
            int size = logBuffer.size();
            for (int i = 0; i < size; i++) {
                printRow(logBuffer.get(i), requestBase);
            }
        }
        printTrailer(naviBar);
    }
    
    private void printHeader(String naviBar) throws IOException {
        // Write the header.
        out.println("<html>");
        out.print("<head><title>Logs: RunID " + runId);
        out.println("</title>");
        out.println("<link rel=\"icon\" type=\"image/gif\" href=\"" +
                    request.getContextPath() + "/img/faban.gif\">");        
        if (displayEnd && !xmlComplete)
            out.print("<meta http-equiv=\"refresh\" content=\"10\">");
        out.println("</head><body>");
        if (naviBar != null)
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
    }
    
    private void printTrailer(String naviBar) throws IOException {
        // Write the trailer.
        out.println("</tbody></table><a name=\"end\"><hr></a>");
        if (naviBar != null)
            out.println(naviBar);
        out.println("</body></html>");        
    }

    private void printRow(LogRecord record, String requestBase)
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
