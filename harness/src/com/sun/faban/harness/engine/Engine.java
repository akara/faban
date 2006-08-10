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
 * $Id: Engine.java,v 1.3 2006/08/10 01:34:37 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.logging.LogConfig;
import com.sun.faban.harness.logging.LogServer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * This class starts the engine by creating the Registry
 * with which all other instances of remote servers register.
 *
 * @author Ramesh Ramachandran
 */
public class Engine {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static Engine instance;

    private Logger logger;

    // Server which listens on a port to receive log msgs from
    // remote agents.
    private LogServer logServer;

    private RunQ runQ;


    // private String[] commands = { "switchlog", "getlog", "getstats",
    //                                    "resetstats", "reconfig" };

    private Engine() {
    }

    public static void initIfNotInited(ServletContext ctx,
                                       HttpServletRequest request) {
        if (INITIALIZED.compareAndSet(false, true)) {
            instance = new Engine();
            instance.init(ctx, request);
        }
    }

    public static void destroy() {
        if (INITIALIZED.compareAndSet(true, false))
            instance.terminate();
    }

    private void init(ServletContext ctx, HttpServletRequest request) {
        String path = ctx.getRealPath("");
        if(path == null)
            path = ctx.getRealPath(".");

        path = path + File.separator;
        if(path != null)
            System.setProperty("faban.root", path);

        StringBuffer reqUrl = request.getRequestURL();
        int uriLength = reqUrl.length() - request.getRequestURI().length() +
                        request.getContextPath().length();
        reqUrl.setLength(++uriLength);
        System.setProperty("faban.url", reqUrl.toString());

        // Read the real path back from the Config and make it
        // faban.home/logs.
        path = Config.FABAN_HOME + "logs";

        // Redirect log to faban.log.xml
        if(path == null)
            path = "%t";

        path = path + File.separator + "faban.log.xml";

        StringBuffer sb = new StringBuffer();
	    sb.append("\nhandlers = java.util.logging.FileHandler\n");
        sb.append("java.util.logging.FileHandler.pattern = " + path + "\n");
        sb.append("java.util.logging.FileHandler.append = true\n");
        sb.append("java.util.logging.FileHandler.limit = 102400\n");
        sb.append("java.util.logging.FileHandler.formatter = com.sun.faban.harness.logging.XMLFormatter\n");

        try {
            LogManager.getLogManager().readConfiguration(
                    new ByteArrayInputStream(sb.toString().getBytes()));
        } catch(IOException e) { }

        logger = Logger.getLogger(this.getClass().getName());
        logger.fine("Faban servlet initializing Log file = " + path);

        // Instanciate the runq which in turn will start the runDaemon
        runQ = RunQ.getHandle();
        logger.fine("RunQ created");

        try {
            logServer = new LogServer(new LogConfig());
            logServer.start();
        } catch (IOException e) {
            logger.log(Level.SEVERE,  "Error starting log server", e);
        }
    }

    /*
     * This servlet's get call takes the parameters
     * cmd=&lt;command&gt;. Commands are listed in the
     * commands array.
     * @param     request          the servlet request
     * @param     response         the servlet response
     * @exception ServletException if get request fails
     *
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws ServletException {

        String okMsg = "200 OK\n";
        String errorMsg = "400 Error\n";
        String responseString = "";
        boolean responded = false;
        ServletOutputStream responseStream = null;

        try {
            responseStream = response.getOutputStream();
        } catch (IOException e) {
            getServletConfig().getServletContext().log(
                               "Error getting response OutputStream", e);
            throw new ServletException(e);
        }

        int paramCount = 0;

        for (Enumeration paramNames = request.getParameterNames(); paramNames.hasMoreElements(); ) {
            String paramName = (String) paramNames.nextElement();
            ++paramCount;

            String[] paramValues = request.getParameterValues(paramName);

            for (int i = 0; i < paramValues.length; i++)
                if ("cmd".equals(paramName)) {
                }
                else
                    responseString = errorMsg + '\n' +
                                     "Unrecognized get parameter: " +
                                     paramName;
        }

        try {
            if (paramCount == 0) {
                response.setContentType("text/html");
                responseStream.println("<html><head><title>Faban Engine Servlet " +
                    "</title></head><body bgcolor=#ffffff><center>" +
                    "<h2>Faban Engine Servlet</h2></center>" +
                    "<h3>No command specified</h3>" +
                    "To specify command, append '?cmd=&lt;command&gt;' " +
                    "to the current URL in your browser<p>" +
                    "Supported commands:<ul>");

                for (int i = 0; i < commands.length; i++)
                    responseStream.println("<li>" + commands[i]);

                responseStream.println("</ul></pre></body></html>");
            } else if (responseString.length() > 0) {
                if (responded)
                    responseStream.println("");
                responseStream.print(responseString);
            } else
                if (!responded)
                    responseStream.print(okMsg);

                responseStream.close();
        } catch (IOException e) {
            logger.severe("Error " + e);
            logger.log(Level.FINE, "Exception", e);
        }
    }

    */

    private void terminate() {
         try {
             // Remove RunDaemon
             runQ.exit();
             // Shutdown logServer
             logServer.shutdown();
         } catch (Exception e ) {
             logger.severe("Error " + e);
             logger.log(Level.FINE, "Exception", e);
         }
    }
}
