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
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.faban.harness.tools;

import com.sun.faban.common.FileTransfer;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.Context;
import com.sun.faban.harness.agent.CmdAgentImpl;
import com.sun.faban.harness.agent.FileAgent;
import com.sun.faban.harness.common.Config;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    /**
     * Constructor.
     * @param toolClass
     * @param ctx
     * @throws java.lang.Exception
     */
    public ToolWrapper(Class toolClass, MasterToolContext ctx) throws Exception {
        tool = toolClass.newInstance();
        Method[] methods = toolClass.getMethods();
        for (Method method : methods) {
            // Check annotation.
            if (method.getAnnotation(Start.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (startMethod == null) {
                    startMethod = method;
                } else {
                    logger.severe("Error: Multiple @Start methods.");
                }
            }
            if (method.getAnnotation(Stop.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (stopMethod == null) {
                    stopMethod = method;
                } else {
                    logger.severe("Error: Multiple @Stop methods.");
                }
            }
            if (method.getAnnotation(Configure.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (configureMethod == null) {
                    configureMethod = method;
                } else {
                    logger.severe("Error: Multiple @Configure methods.");
                }
            }
            if (method.getAnnotation(Postprocess.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (postprocessMethod == null) {
                    postprocessMethod = method;
                } else {
                    logger.severe("Error: Multiple @PostProcess methods.");
                }
            }
        }

        Field ctxField = null;
        tc = new ToolContext(ctx.getTool(), ctx.getToolServiceContext(), ctx.getToolDescription(), this);
        Field[] fields = toolClass.getFields();
        for (Field field : fields) {
            if (field.getType().equals(ToolContext.class) &&
                                (field.getAnnotation(Context.class) != null)) {
                    if (ctxField == null)
                        ctxField = field;
                    else
                        logger.warning("More than one valid @Context annotation.");
            }
        }
        if (ctxField != null)
            ctxField.set(tool, tc);

    }

    private boolean conformsToSpec(Method method) {
            boolean retval= true;
            // Is it a noarg method?
            if (method.getParameterTypes().length > 0) {
                logger.warning("Method has arguments");
                retval = false;
            }
            // Is it a void method?
            if (!method.getReturnType().equals(Void.TYPE)) {
                logger.warning("Method is not of type Void");
                retval = false;
            }
            return retval;
    }
    private void throwSourceException(InvocationTargetException e)
                throws Exception {
            Throwable t = e.getCause();
            if (t instanceof Exception) {
                logger.log(Level.WARNING, t.getMessage(), t);
                throw (Exception) t;
            } else {
                throw e;
            }
    }

    private void configure() throws Exception {
        if (configureMethod != null){
            try {
                configureMethod.invoke(tool,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            }
        }
    }

    /**
     * This method is responsible for postprocessing.
     * @throws java.lang.Exception
     */
    public void postprocess() throws Exception {
        if (postprocessed)
            return;
        postprocessed = true;
        logger.fine(toolName + " post-processing.");
        try {
            if (toolStatus == STOPPED){
                if (postprocessMethod != null)
                    try {
                        postprocessMethod.invoke(this.tool,new Object[] {});
                    } catch (InvocationTargetException e) {
                        throwSourceException(e);
                    }

                // xfer log file to master machine, log any errors
                xferLog();
                logger.fine(toolName + " Done ");
            }
        } finally {
            finish();
        }
    }

    /**
     * This method is responsible for starting a tool.
     * @throws java.lang.Exception
     */
    private void start() throws Exception {
        if (startMethod != null){
            try {
                startMethod.invoke(tool,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            }
        }
        toolStatus = STARTED;
    }

    /**
     * This method is responsible for configuring a tool.
     * @param toolName
     * @param path
     * @param outDir
     * @param host
     * @param cmdAgent
     * @param latch
     * @throws java.lang.Exception
     */
    public void configure(String toolName, String path, String outDir, String host,
                          CmdAgentImpl cmdAgent, CountDownLatch latch) 
                          throws Exception {

        // Prepare the context based on the params.
        this.toolName = toolName;
        this.cmdAgent = cmdAgent;
        this.timer = cmdAgent.getTimer();
        this.latch = latch;

        if (path != null)
            this.path = path;

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
                    logger.log(Level.SEVERE, null, ex);
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
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    private class PostprocessTask extends TimerTask {
        public void run() {
            try {
                postprocess();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }

    /**
     * This method is responsible for stopping the tool utility.
     * @throws java.lang.Exception
     */
    public void stop() throws Exception {
        stop(true);
    }

    /**
     * This method is responsible for stopping the tool utility.
     * @param warn Whether to warn if the tool already ended.
     * @throws java.lang.Exception
     */
    protected void stop(boolean warn) throws Exception{
        if (toolStatus == STARTED){
            stopMethod.invoke(this.tool,new Object[] {});
                
                // saveToolLogs(tool.getInputStream(), tool.getErrorStream());
                toolStatus = STOPPED;
                logger.fine(toolName + " Stopped ");
        } else if (warn && toolStatus == NOT_STARTED)
            logger.warning("Tool not started but stop called for " + toolName);
    }


    /**
     * Transfers the tool output file back to the master.
     */
    protected void xferLog() {
        String logfile = tc.getOutputFile();
        if (!new File(logfile).exists()) {
            logger.info("Transfer file " + logfile + " not found.");
            return;
        }
        try {
            FileTransfer transfer;
            if (outputHandle != null) {
                transfer = outputHandle.fetchOutput(outputStream, outfile);
            } else {
                transfer = new FileTransfer(logfile, outfile);
            }
            logger.fine("Transferring log from " + logfile + " to " + outfile);

            // Use FileAgent on master machine to copy log
            if (transfer != null) {
                String s = Config.FILE_AGENT;
                FileAgent fa =
                        (FileAgent) CmdAgentImpl.getRegistry().getService(s);
                if (fa.push(transfer) != transfer.getSize())
                    logger.info("Invalid transfer size");
            }
        } catch (IOException e) {
            logger.log(Level.INFO, "Error transferring " + logfile, e);
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
     * @throws java.lang.Exception
     */
    public void kill() throws Exception {
        // For most tools, we try to collect the output no matter what.
        stop(false);
        finish();
    }
    
}
