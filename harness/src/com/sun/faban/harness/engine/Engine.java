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
package com.sun.faban.harness.engine;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.logging.LogConfig;
import com.sun.faban.harness.logging.LogServer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
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

    /**
     * Initializes the instance if it is not already initialized.
     * @param ctx
     * @param request
     * @throws java.io.IOException
     */
    public static void initIfNotInited(ServletContext ctx,
                                       HttpServletRequest request)
            throws IOException {
        if (INITIALIZED.compareAndSet(false, true)) {
            try {
                instance = new Engine();
                instance.init(ctx, request);
            } catch (IOException e) {
                INITIALIZED.set(false);
                throw e;
            } catch (Throwable t) {
                INITIALIZED.set(false);
                throw new IOException(t.getMessage(), t);
            }
        }
    }

    /**
     * Terminates the instance.
     */
    public static void destroy() {
        if (INITIALIZED.compareAndSet(true, false))
            instance.terminate();
    }

    private void init(ServletContext ctx, HttpServletRequest request)
            throws IOException {
        String path = ctx.getRealPath("");
        if(path == null)
            path = ctx.getRealPath(".");

        path = path + File.separator;
        if(path != null)
            System.setProperty("faban.root", path);

        String fabanHome = ctx.getInitParameter("faban.home");
        if (fabanHome != null)
            System.setProperty("faban.home", fabanHome);

        StringBuffer reqUrl = request.getRequestURL();
        int uriLength = reqUrl.length() - request.getRequestURI().length() +
                        request.getContextPath().length();
        reqUrl.setLength(++uriLength);
        System.setProperty("faban.url", reqUrl.toString());

        // Access config NOW, static initializers will config the logging
        // before we instatiate a logger.
        path = Config.DEFAULT_LOG_FILE;

        logger = Logger.getLogger(this.getClass().getName());
        logger.fine("Faban logging to " + path);

        // Instantiate the runq which in turn will start the runDaemon
        runQ = RunQ.getHandle();
        logger.fine("RunQ created");

        if (Config.daemonMode == Config.DaemonModes.POLLER ||
                Config.daemonMode == Config.DaemonModes.LOCAL) {
            logServer = new LogServer(new LogConfig());

            // Share the thread pool for other uses, too.
            Config.THREADPOOL = logServer.config.threadPool;

            logServer.start();
        }
    }

    private void terminate() {
        // Stop the run daemon.
        try {
            runQ.exit();
        } catch (Throwable t) {
            logger.log(Level.SEVERE, "Error shutting down run daemon", t);
        }

        // Shutdown the log server.
        if (logServer != null)
            try {
                logServer.shutdown();
            }  catch (Throwable t) {
                logger.log(Level.SEVERE, "Error shutting down log server", t);
            }
    }
}
