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

import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.engine.RunQ;
import com.sun.faban.harness.security.AccessController;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Submitter servlet is used to submit a benchmark run from the CLI.
 *
 * @author Akara Sucharitakul
 */
public class CLIServlet extends HttpServlet {

    static final int TAIL = 0;
    static final int FOLLOW = 1;

    static Logger logger = Logger.getLogger(CLIServlet.class.getName());

    String[] getPathComponents(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();

        StringTokenizer pathTokens = null;
        int tokenCount = 0;
        if (pathInfo != null) {
            pathTokens = new StringTokenizer(pathInfo, "/");
            tokenCount = pathTokens.countTokens();
        }
        String[] comps = new String[tokenCount + 1];
        comps[0] = request.getServletPath();
        int i = 1;
        while (pathTokens != null && pathTokens.hasMoreTokens()) {
            comps[i] = pathTokens.nextToken();
            if (comps[i] != null && comps[i].length() > 0)
                ++i;
        }

        if (i != comps.length) {
            String[] comps0 = new String[i];
            System.arraycopy(comps, 0, comps0, 0, i);
            comps = comps0;
        }
        return comps;
    }

    /**
     * Lists pending runs, obtains status, or show logs of a particular run.<ol>
     * <li>Pending: http://..../pending/</li>
     * <li>Status:  http://..../status/${runid}</li>
     * <li>Logs:    http://..../logs/${runid}</li>
     * <li>Tail Logs: http://..../logs/${runid}/tail</li>
     * <li>Follow Logs: http://..../logs/${runid}/follow</li>
     * <li>Combination of tail and follow, postfix /tail/follow</li>
     * </ol>.
     * @param request The request object
     * @param response The response object
     * @throws ServletException Error executing servlet
     * @throws IOException I/O error
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String[] reqC = getPathComponents(request);
        if ("/status".equals(reqC[0])) {
            sendStatus(reqC, response);
        } else if ("/pending".equals(reqC[0])) {
            sendPending(response);
        } else if ("/logs".equals(reqC[0])) {
            sendLogs(reqC, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Request string " + reqC[0] + " not understood!");
        }
    }

    /**
     * Submits new runs or kills runs. For submission, the POST request must be
     * a multi-part POST. The first parts contain user name and password
     * information (if security is enabled). Each subsequent part contains the
     * run configuration file. The configuration file is not used for kill
     * requests.
     * <br><br>
     * Path to call this servlet is http://.../submit/${benchmark}/${profile}
     * and http://.../kill/${runId}.
     *
     * @param request The mime multi-part post request
     * @param response The response object
     * @throws ServletException Error executing servlet
     * @throws IOException I/O error
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Path to call this servlet is http://...../${benchmark}/${profile}
        // And it is a post request with optional user, password, and
        // all the config files.
        String[] reqC = getPathComponents(request);
        if ("/submit".equals(reqC[0])) {
            doSubmit(reqC, request, response);
        } else if ("/kill".equals(reqC[0])) {
            doKill(reqC, request, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Request string " + reqC[0] + " not understood!");
        }
    }

    private void sendPending(HttpServletResponse response) throws IOException {
        String[] pending = RunQ.listPending();
        if (pending == null) {
            response.sendError(HttpServletResponse.SC_NO_CONTENT,
                    "No pending runs");
        } else {
            Writer w = response.getWriter();
            for (int i = 0; i < pending.length; i++)
                w.write(pending[i] + '\n');
            w.flush();
            w.close();
        }
    }

    private void sendStatus(String[] reqC, HttpServletResponse response)
            throws IOException {
        if (reqC.length < 2) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                        "Missing RunId.");
            return;
        }
        String runId = reqC[1];
        String status = RunResult.getStatus(new RunId(runId));
        if (status == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                        "No such runId: " + runId);
        } else {
            Writer w = response.getWriter();
            w.write(status + '\n');
            w.flush();
            w.close();
        }
    }

    private void sendLogs(String[] reqC, HttpServletResponse response)
            throws ServletException, IOException {
        if (reqC.length < 2) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                                        "Missing RunId.");
            return;
        }
        RunId runId = new RunId(reqC[1]);
        boolean[] options = new boolean[2];
        options[TAIL] = false;
        options[FOLLOW] = false;
        for (int i = 2; i < reqC.length; i++) {
            if ("tail".equals(reqC[i])) {
                options[TAIL] = true;
            } else if ("follow".equals(reqC[i])) {
                options[FOLLOW] = true;
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid option \"" + reqC[i] + "\"."); ;
                return;
            }
        }
        File logFile = new File(Config.OUT_DIR + runId, "log.xml");
        String status = null;
        response.setContentType("text/plain");
        PrintWriter out = response.getWriter();
        while (!logFile.exists()) {
            String[] pending = RunQ.listPending();
            if (pending == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                            "RunId " + runId +" not found");
                return;
            }
            boolean queued = false;
            for (String run : pending) {
                if (run.equals(runId.toString())) {
                    if (status == null) {
                        status = "QUEUED";
                        out.println(status);
                        response.flushBuffer();
                    }
                    queued = true;
                    try {
                        Thread.sleep(1000);  // Check back in one sec.
                    } catch (InterruptedException e) {
                        //Noop, just look it up again.
                    }
                    break;
                }
            }
            if (!queued) { // Either never queued or deleted from queue.
                // Check for 10x, 100ms each to allow for start time.
                for (int i = 0; i < 10; i++) {
                    if (logFile.exists()) {
                        status = "STARTED";
                        break;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING,
                                "Interrupted checking existence of log file.");
                    }
                }

                if (!"STARTED".equals(status)) {
                    if ("QUEUED".equals(status)) { // was queued before
                        status = "DELETED";
                        out.println(status);
                        out.flush();
                        out.close();
                        return;
                    } else { // Never queued or just removed.
                        response.sendError(HttpServletResponse.SC_NOT_FOUND,
                                            "RunId " + runId +" not found");
                        return;
                    }
                }
            }
        }

        LogOutputHandler handler = new LogOutputHandler(response, options);
        InputStream logInput;
        if (options[FOLLOW]) {
            // The XMLInputStream reads streaming XML and does not EOF.
            XMLInputStream input = new XMLInputStream(logFile);
            input.addEOFListener(handler);
            logInput = input;
        } else {
            logInput = new FileInputStream(logFile);
        }
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
            parser.parse(logInput, handler);
            handler.xmlComplete = true; // If we get here, the XML is good.
        } catch (ParserConfigurationException e) {
            throw new ServletException(e);
        } catch (SAXParseException e) {
            Throwable t = e.getCause();
            // If it is caused by an IOException, we'll just throw it.
            if (t != null) {
                if (t instanceof IOException)
                    throw (IOException) t;
                else if (options[FOLLOW])
                    throw new ServletException(t);
            } else if (options[FOLLOW]) {
                throw new ServletException(e);
            }
        } catch (SAXException e) {
            throw new ServletException(e);
        } finally {
            if (options[TAIL] && !options[FOLLOW]) // tail not yet printed
                handler.eof();
        }
    }

    private void doKill(String[] reqC, HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        if (reqC.length < 2) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing RunId.");
            return;
        }
        RunId runId = new RunId(reqC[1]);
        String user = request.getParameter("sun");
        String password = request.getParameter("sp");

        // Check the status of the run
        boolean found = false;
        boolean queued = false;
        String terminateStatus = null;
        RunResult result = RunResult.getInstance(runId);
        if (result != null) {
            found = true;
            if ( "COMPLETED".equals(result.status) ||
                    "FAILED".equals(result.status) ||
                    "KILLED".equals(result.status) ) {
                terminateStatus = result.status;
            }
        } else { // If not found, look in queue
            String[] pending = RunQ.listPending();
            if (pending != null) {
                for (String run : pending) {
                    if (run.equals(runId.toString())) {
                        found = true;
                        queued = true;
                        break;
                    }
                }
            }
        }

        if (found && terminateStatus == null) { // not yet terminated

            // First authenticate the user and make sure he/she is the CLI user.
            boolean hasPermission = true;
            if (Config.SECURITY_ENABLED) {
                if (Config.CLI_SUBMITTER == null ||
                        Config.CLI_SUBMITTER.length() == 0 ||
                        !Config.CLI_SUBMITTER.equals(user)) {
                    hasPermission = false;
                }
                if (Config.SUBMIT_PASSWORD == null ||
                        Config.SUBMIT_PASSWORD.length() == 0 ||
                        !Config.SUBMIT_PASSWORD.equals(password)) {
                    hasPermission = false;
                }
                if (AccessController.isKillAllowed(user, runId.toString())) {
                    hasPermission = false;
                }
            }

            if (hasPermission) {

                // No matter of status, the run may be running by now.
                // So check for active runs first.
                if (RunQ.getHandle().killCurrentRun(runId.toString(), user)
                        != null) {
                    terminateStatus = "KILLING";
                } else { // Or the run may have already terminated...
                    result = RunResult.getInstance(runId);
                    if (result != null) {
                        if ( "COMPLETED".equals(result.status) ||
                                "FAILED".equals(result.status) ||
                                "KILLED".equals(result.status) ) {
                            terminateStatus = result.status;
                        }
                    } else if (queued) { // Or it still is in the queue
                        RunQ.getHandle().deleteRun(runId.toString());
                        terminateStatus = "DELETED";
                    }
                }
            } else {
                if (queued) // Run was removed in the meantime
                    terminateStatus = "DELETED";
                else
                    terminateStatus = "DENIED";
            }
        }

        if (!found) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "No such runId: " + runId);
        } else {
            Writer w = response.getWriter();
            w.write(terminateStatus + '\n');
            w.flush();
            w.close();
        }
    }

    private void doSubmit(String[] reqC, HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {
        if (reqC.length < 3) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Benchmark and profile not provided in request.");
            return;
        }
        // first is the bench name
        BenchmarkDescription desc =
                BenchmarkDescription.getDescription(reqC[1]);
        if (desc == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Benchmark " + reqC[1] + " not deployed.");
            return;
        }
        try {
            String user = null;
            String password = null;
            boolean hasPermission = true;

            ArrayList<String> runIdList = new ArrayList<String>();

            DiskFileUpload fu = new DiskFileUpload();
            // No maximum size
            fu.setSizeMax(-1);
            // maximum size that will be stored in memory
            fu.setSizeThreshold(8192);
            // the location for saving data larger than getSizeThreshold()
            fu.setRepositoryPath(Config.TMP_DIR);

            List fileItems = null;
            try {
                fileItems = fu.parseRequest(request);
            } catch (FileUploadException e) {
                throw new ServletException(e);
            }

            for (Iterator i = fileItems.iterator(); i.hasNext();) {
                FileItem item = (FileItem) i.next();
                String fieldName = item.getFieldName();
                if (item.isFormField()) {
                    if ("sun".equals(fieldName)) {
                        user = item.getString();
                    } else if ("sp".equals(fieldName)) {
                        password = item.getString();
                    }
                    continue;
                }
                if (reqC[2] == null) // No profile
                    break;

                if (desc == null)
                    break;

                if (!"configfile".equals(fieldName))
                    continue;

                if (Config.SECURITY_ENABLED) {
                    if (Config.CLI_SUBMITTER == null ||
                            Config.CLI_SUBMITTER.length() == 0 ||
                            !Config.CLI_SUBMITTER.equals(user)) {
                        hasPermission = false;
                        break;
                    }
                    if (Config.SUBMIT_PASSWORD == null ||
                            Config.SUBMIT_PASSWORD.length() == 0 ||
                            !Config.SUBMIT_PASSWORD.equals(password)) {
                        hasPermission = false;
                        break;
                    }
                }

                String usrDir = Config.PROFILES_DIR + reqC[2];
                File dir = new File(usrDir);
                if(dir.exists()) {
                    if(!dir.isDirectory()) {
                         logger.severe(usrDir +
                                        " should be a directory");
                        dir.delete();
                        logger.fine(dir + " deleted");
                    }
                    else
                        logger.fine("Saving parameter file to" +
                                    usrDir);
                }
                else {
                    logger.fine("Creating new profile directory for " +
                                reqC[2]);
                    if(dir.mkdirs())
                        logger.fine("Created new profile directory " +
                                    usrDir);
                    else
                        logger.severe("Failed to create profile " +
                                      "directory " + usrDir);
                }


                // Save the latest config file into the profile directory
                String dstFile = Config.PROFILES_DIR + reqC[2] +
                        File.separator + desc.configFileName + "." +
                        desc.shortName;

                item.write(new File(dstFile));
                runIdList.add(RunQ.getHandle().addRun(user, reqC[2], desc));
            }

            response.setContentType("text/plain");
            Writer writer = response.getWriter();

            if (!hasPermission) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                writer.write("Permission denied!\n");
            }

            if (runIdList.size() == 0)
                writer.write("No runs submitted.\n");
            for (String newRunId : runIdList) {
                writer.write(newRunId);
            }

            writer.flush();
            writer.close();
        } catch (ServletException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new ServletException(e);
        }
    }

    private static class LogOutputHandler extends LogParseHandler
            implements XMLInputStream.EOFListener {

        private ServletResponse response;
        private PrintWriter writer;
        private boolean[] options;

        LogRecordDetail detail = new LogRecordDetail();
        ExceptionRecord exception = new ExceptionRecord();
        StackFrame frame = new StackFrame();
        ArrayList stackFrames = new ArrayList();
        private CircularBuffer<LogRecord> recordBuffer;

        LogOutputHandler(PrintWriter writer, boolean[] options) {
            super(null, null, null);
            this.writer = writer;
            this.options = options;
            if (options[TAIL])
                recordBuffer = new CircularBuffer<LogRecord>(10);
        }

        LogOutputHandler(ServletResponse response, boolean[] options)
                throws IOException {
            this(response.getWriter(), options);
            this.response = response;
        }

        private void flush() {
            if (response != null)
                try {
                    response.flushBuffer();
                } catch (IOException e) {
                    // Noop. If a client socket closes, we just don't care.
                }
            else
                writer.flush();
        }

        /**
         * The processRecord method allows subclasses to define
         * how a record should be processed.
         *
         * @throws org.xml.sax.SAXException If the processing should stop.
         */
        public void processRecord() throws SAXException {
            if (options[TAIL]) {
                recordBuffer.add(logRecord);
                logRecord = new LogRecord(); // Don't reuse LogRecord if kept
            } else {
                printRecord(logRecord);
            }
        }

        /**
         * Formats a multi-line message into text line breaks
         * for readability.
         *
         * @param message The message to be formatted.
         * @return The new formatted message.
         */
        @Override String formatMessage(String message) {
            int idx = message.indexOf("<br>");
            if (idx == -1) // If there's no <br>, don't even hassle.
                return message;
            StringBuffer msg = new StringBuffer(message);
            String crlf = "\n";
            while (idx != -1) {
                msg.replace(idx, idx + 4, crlf);
                idx = msg.indexOf("<br>", idx + crlf.length());
            }
            return msg.toString();
        }

        /**
         * The processDetail method allows subclasses to process
         * the exceptions not processed by default. This is called
         * from endElement.
         *
         * @param qName The element qName
         * @throws org.xml.sax.SAXException If the processing should stop.
         */
        public void processDetail(String qName) throws SAXException {
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
                frame = new RecordHandler.StackFrame();
            } else if ("exception".equals(qName)) {
                RecordHandler.StackFrame[] frameArray =
                        new RecordHandler.StackFrame[stackFrames.size()];
                exception.stackFrames =
                        (RecordHandler.StackFrame[]) stackFrames.toArray(frameArray);
                stackFrames.clear();
                logRecord.exceptionFlag = true;
                logRecord.exception = exception;
                exception = new ExceptionRecord();
            }
        }

        /**
         * Prints the html result of the parsing to the servlet output.
         */
        public void printHtml() {
            // We never print in html. So this is a noop here.
        }

        /**
         * Gets called if and when eof is hit.
         */
        public void eof() {
            if (options[TAIL]) {
                int size = recordBuffer.size();
                for (int i = 0; i < size; i++)
                    printRecord(recordBuffer.get(i));
                options[TAIL] = false;
                recordBuffer = null;
            }
            flush();
        }

        private void printRecord(LogRecord r) {
            // Print only the time, not the date.
            int timeIdx = r.date.indexOf('T') + 1;
            writer.println(r.date.substring(timeIdx) +
                        ':' + r.level + ':' + formatMessage(r.message));
            if (r.exception != null) {
                writer.println(formatMessage(r.exception.message));
                for (StackFrame s : r.exception.stackFrames) {
                    writer.println("    at " + s.clazz + '.' + s.method +
                                " (" + s.line + ')');
                }
                r.exception = null;
            }
        }
    }

    static class CircularBuffer<E> {

        private int head = 0;
        private boolean wrapped = false;
        private int size = 0;
        private Object[] buffer;

        CircularBuffer(int capacity) {
            buffer = new Object[capacity];
        }

        void add(E object) {
            buffer[head] = object;
            moveHead();
            if (size < buffer.length)
                ++size;
        }

        private void moveHead() {
            ++head;
            if (head >= buffer.length) {
                head = 0;
                wrapped = true;
            }
        }

        E get(int idx) {
            if (wrapped)
                idx += head;
            if (idx >= buffer.length)
                idx -= buffer.length;
            return (E) buffer[idx];
        }

        int size() {
            return size;
        }
    }
}
