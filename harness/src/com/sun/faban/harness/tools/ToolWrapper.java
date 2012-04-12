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

package com.sun.faban.harness.tools;

import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.FileTransfer;
import com.sun.faban.harness.Configure;
import com.sun.faban.harness.Context;
import com.sun.faban.harness.Start;
import com.sun.faban.harness.Stop;
import com.sun.faban.harness.agent.CmdAgentImpl;
import com.sun.faban.harness.agent.FileAgent;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.util.Invoker;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a wrapper for tool.
 *
 * @author Sheetal Patil
 */
public class ToolWrapper {

    private static Logger logger =
            Logger.getLogger(ToolWrapper.class.getName());

    Object tool;
    Method startMethod;
    Method stopMethod;
    Method configureMethod;
    Method postprocessMethod;

    static final int NOT_STARTED = 0;
    static final int STARTED = 1;
    static final int STOPPED = 2;

    int toolStatus = NOT_STARTED;
    CountDownLatch latch;
    boolean countedDown = false;
    boolean postprocessed = false;
    String outfile;	// Name of stdout,stderr from tool
    CommandHandle outputHandle;
    int outputStream;
    String toolName;
    String path = null; // The path to the tool.
    CmdAgentImpl cmdAgent;

    /** The timer used for scheduling the tools. */
    protected Timer timer;

    ToolContext tc = null;

    private String outDir;
    private String host;

    /**
     * Constructor.
     * @param toolClass The tool class
     * @param ctx The master tool context
     * @throws Exception Error creating the tool
     */
    public ToolWrapper(Class toolClass, MasterToolContext ctx)
            throws Exception {
        Method[] methods = toolClass.getMethods();
        for (Method method : methods) {
            // Check annotation.
            if (method.getAnnotation(Start.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (startMethod == null) {
                    startMethod = method;
                } else {
                    logger.severe(toolName +
                            " Error: Multiple @Start methods.");
                }
            }
            if (method.getAnnotation(Stop.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (stopMethod == null) {
                    stopMethod = method;
                } else {
                    logger.severe(toolName + " Error: Multiple @Stop methods.");
                }
            }
            if (method.getAnnotation(Configure.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (configureMethod == null) {
                    configureMethod = method;
                } else {
                    logger.severe(toolName +
                            " Error: Multiple @Configure methods.");
                }
            }
            if (method.getAnnotation(Postprocess.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (postprocessMethod == null) {
                    postprocessMethod = method;
                } else {
                    logger.severe(toolName +
                            " Error: Multiple @PostProcess methods.");
                }
            }
        }

        Field ctxField = null;
        tc = new ToolContext(ctx.getTool(), ctx.getToolServiceContext(),
                ctx.getToolDescription(), this);
        Field[] fields = toolClass.getFields();
        for (Field field : fields) {
            if (field.getType().equals(ToolContext.class) &&
                                (field.getAnnotation(Context.class) != null)) {
                    if (ctxField == null)
                        ctxField = field;
                    else
                        logger.warning(toolName +
                                ": More than one valid @Context annotation.");
            }
        }
        Invoker.setContextLocation(tc.toolPath);
        try {
            tool = toolClass.newInstance();
            if (ctxField != null)
                ctxField.set(tool, tc);
        } catch (InstantiationException e) {
            Invoker.throwCauseException(e);
        } finally {
            Invoker.setContextLocation(null);
        }
    }

    private void configure() throws Exception {
        Invoker.invoke(tool, configureMethod, tc.toolPath);
        logger.fine("Configured tool " + toolName);
    }

    /**
     * This method is responsible for post-processing.
     * @throws Exception Any exception thrown by the wrapped method
     */
    public void postprocess() throws Exception {
        if (postprocessed)
            return;
        postprocessed = true;
        try {
            if (toolStatus == STOPPED) {
                if (postprocessMethod != null) {
                    logger.fine(toolName + " post-processing.");
                    Invoker.invoke(tool, postprocessMethod, tc.toolPath);
                    logger.fine("Postprocessed tool " + toolName);
                }
                // xfer log file to master machine, log any errors
                xferLog();
                logger.fine("Transfered logs for tool " + toolName);
                logger.info(toolName + " Done ");
            }
        } finally {
            finish();
        }
    }

    /**
     * This method is responsible for starting a tool.
     * @throws Exception Any exception thrown by the wrapped method
     */
    private void start() throws Exception {
        Invoker.invoke(tool, startMethod, tc.toolPath);
        toolStatus = STARTED;
        logger.info("Started tool " + toolName);
    }

    /**
     * This method is responsible for configuring a tool.
     * @param toolName The name of the tool.
     * @param path The path to start the command
     * @param outDir The tool output directory
     * @param host The host the tool should run
     * @param cmdAgent The local command agent
     * @param latch The latch used for identifying tool completion
     * @throws java.lang.Exception If there is any error
     */
    public void configure(String toolName, String path, String outDir,
                          String host, CmdAgentImpl cmdAgent,
                          CountDownLatch latch)
            throws Exception {

        // Prepare the context based on the params.
        this.toolName = toolName;
        this.cmdAgent = cmdAgent;
        this.timer = cmdAgent.getTimer();
        this.latch = latch;

        if (path != null)
            this.path = path;

        this.outDir = outDir;
        this.host = host;

        // Get output logfile name
        this.outfile = outDir + toolName + ".log." + host;
        configure();
    }

    /**
     * This method is responsible for starting a tool with given delay.
     * @param delay The lapse time after which the tool will start
     * @return always return true
     */
    public boolean start(int delay) {
        TimerTask startTask = new TimerTask() {
            public void run() {
                try {
                    start();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, toolName + ": " + ex.getMessage(),
                            ex);
                }
            }
        };
        timer.schedule(startTask, delay * 1000);
        return true;
    }

    /**
     * This method is responsible for starting a tool with given delay and
     * duration.
     * @param delay The lapse time after which the tool will start
     * @param duration The duration the tool is to run
     * @return always return true
     */
    public boolean start(int delay, int duration) {
        if(this.start(delay)) {
            timer.schedule(new StopTask(), (delay + duration) * 1000);
            return true;
        } else {
            return false;
        }
    }

    private class StopTask extends TimerTask {
        public void run() {
            try {
                stop();
                // Schedule it out 0.5 secs just to ensure
                // the other tools' stops are called first.
                timer.schedule(new PostprocessTask(), 500);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, toolName + ": " + ex.getMessage(), ex);
            }
        }
    }

    private class PostprocessTask extends TimerTask {
        public void run() {
            try {
                postprocess();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, toolName + ": " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * This method is responsible for stopping the tool utility.
     * @throws Exception Any exception thrown by the wrapped method
     */
    public void stop() throws Exception {
        stop(true);
    }

    /**
     * This method is responsible for stopping the tool utility.
     * @param warn Whether to warn if the tool already ended.
     * @throws Exception Any exception thrown by the wrapped method
     */
    protected void stop(boolean warn) throws Exception{
        if (toolStatus == STARTED){
            Invoker.setContextLocation(tc.toolPath);
            try {
                stopMethod.invoke(this.tool);
            } finally {
                Invoker.setContextLocation(null);
            }
            // saveToolLogs(tool.getInputStream(), tool.getErrorStream());
                toolStatus = STOPPED;
                logger.info("Stopped tool " + toolName);
        } else if (warn && toolStatus == NOT_STARTED)
            logger.warning("Tool not started but stop called for " + toolName);
    }


    /**
     * Transfers the tool output file back to the master.
     */
    protected void xferLog() {
        String logfile = null;
        try {
            FileTransfer[] transfer;
            if (outputHandle != null) {
                logger.finer("outputHandle is not null. outfile is " + outfile);
                transfer = new FileTransfer[1];
                transfer[0] = outputHandle.fetchOutput(outputStream, outfile);
            } else if (tc.localOutputFiles != null) {
                transfer = new FileTransfer[tc.localOutputFiles.size()];
                logger.finer("tc.localOutputFiles is not null. Size is " + tc.localOutputFiles.size());
                int idx = 0;
                for (Map.Entry<String, String> entry :
                        tc.localOutputFiles.entrySet()) {
                    String key = entry.getKey();
                    String path = entry.getValue();
                    String ext;
                    logger.finer("localOutputFiles path = " + path);
                    if (path.endsWith(".xan") || path.contains(".xan."))
                        ext = ".xan.";
                    else
                        ext = ".log.";
                    if (! new File(path).exists()) {
                        logger.warning(toolName + ": Transfer file " + path +
                                " not found.");
                        continue;
                    }
                    // The multiple files may simply be raw and xan. If so, treat as one
                    String outFile;
                    if ("raw".equals(key) || "xan".equals(key))
                        outFile = outDir + toolName + ext + host;
                    else
                        outFile = outDir + toolName + '-' + key + ext + host;
                    transfer[idx++] = new FileTransfer(path, outFile);
                }
            } else {
                transfer = new FileTransfer[1];
                logfile = tc.getOutputFile();
                logger.finer("single file transfer. logfile is " + logfile);
                if (!new File(logfile).exists()) {
                    logger.warning(toolName + ": Transfer file " + logfile +
                            " not found.");
                    return;
                }
                transfer[0] = new FileTransfer(logfile, outfile);
            }

            String s = Config.FILE_AGENT;
            FileAgent fa = (FileAgent) CmdAgentImpl.getRegistry().getService(s);
            for (FileTransfer t : transfer) {
                logger.fine(toolName + ": Transferring log from " +
                        t.getSource() + " to " + t.getDest());

                // Use FileAgent on master machine to copy log
                if (t != null) {
                    if (fa.push(t) != t.getSize())
                        logger.info(toolName + ": Invalid transfer size");
                }
            }
        } catch (IOException e) {
            if (logfile == null)
                logger.log(Level.INFO, toolName + ": Error transferring " +
                        logfile, e);
            else
                logger.log(Level.INFO, toolName + ": Error transferring " +
                        toolName + " output.", e);
        }
    }

    /**
     * Finishes up the tool and notifies the infrastructure of the tool
     * finishing up.
     */
    protected void finish() {
        if (!countedDown)
            latch.countDown();
        countedDown = true;
    }

    /**
     * This method is responsible for killing the tool utility.
     * @throws Exception Any exception thrown by the wrapped method
     */
    public void kill() throws Exception {
        // For most tools, we try to collect the output no matter what.
        stop(false);
        finish();
    }
    
}
