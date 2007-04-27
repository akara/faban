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
 * $Id: Engine.java,v 1.5 2007/04/27 21:33:27 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
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

        // Access config NOW, static initializers will config the logging
        // before we instatiate a logger.
        path = Config.DEFAULT_LOG_FILE;

        logger = Logger.getLogger(this.getClass().getName());
        logger.fine("Faban logging to " + path);

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
