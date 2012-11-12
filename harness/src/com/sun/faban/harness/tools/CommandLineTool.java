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
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.tools;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 * This class handles the command line tools. Like, vmstat, mpstat, etc.
 *
 * @author Sheetal Patil
 */
public class CommandLineTool {

    private static Logger logger =
            Logger.getLogger(CommandLineTool.class.getName());

    /** The injected tool context. */
    @Context public ToolContext ctx;

    Command cmd;
    CommandHandle processRef;
    ArrayList<String> toolCmd;
    String toolName;
    String postCmd;
    List<String> toolArgs;
    protected long stopTime;

    /**
     * Configures the command line tool.
     * Tasks done in this method includes building the
     * toolCmd using the params from the tool context.
     */
    @Configure public void config() {
        toolName = ctx.getToolName();
        postCmd = toolName + "-post";
        toolArgs = ctx.getToolArgs();
        toolCmd = new ArrayList<String>();
        toolCmd.add(toolName);
        if (toolArgs != null)
            toolCmd.addAll(toolArgs);
        cmd = new Command(toolCmd);
        cmd.setSynchronous(false);
        logger.fine(toolName + " Configured with toolCmd " + toolCmd);

    }

    /**
     * This method is responsible for starting the command line tool.
     * @throws IOException Error starting the command
     * @throws InterruptedException Interrupted waiting for commands
     */
    @Start public void start() throws IOException, InterruptedException {
        // If we are going to postProcess this command's output, don't set the outputHandle
        if (RunContext.which(postCmd) != null) {
            logger.fine("Setting outputFile for " + toolCmd + " to be " + ctx.getOutputFile());
            cmd.setOutputFile(Command.STDOUT, ctx.getOutputFile());
            cmd.setStreamHandling(Command.STDOUT, Command.CAPTURE);
            processRef = ctx.exec(cmd, false);
        } else {
            processRef = ctx.exec(cmd, true);
        }
        logger.fine(toolName + " Started with Cmd = " + toolCmd);
    }

    /**
     * This method is responsible for stopping the command line tool.
     * @throws IOException Error stopping the command
     * @throws InterruptedException Interrupted waiting for commands
     */
    @Stop public void stop() throws IOException, InterruptedException {
        logger.fine("Stopping tool " + this.toolCmd);
        processRef.destroy();
        processRef.waitFor(10000);
        stopTime = System.currentTimeMillis();
    }

     /**
     * This method is responsible for post-processing. Command line tool
     * post-processing scripts will turn the output file into a xan format.
     * @throws IOException Error post-processing cpustat
     * @throws InterruptedException Interrupted waiting for commands
     */
    @Postprocess public void postprocess()
            throws IOException, InterruptedException {
        // Check if post-processing cmd exists. If not, quietly return
        logger.fine("In postprocess for " + toolName + " postCmd = " + postCmd);
        if (RunContext.which(postCmd) != null) {
            String rawFile = ctx.getOutputFile();
            String postFile = rawFile.replace(".out.", ".xan.");
            postCmd += " " + rawFile;
            ctx.setOutputFile("xan", postFile);
            logger.finer("postCmd = " + postCmd + ", postFile = " + postFile);
            long sleepTime = stopTime + 500 - System.currentTimeMillis();
            if (sleepTime > 0)
                Thread.sleep(sleepTime);
            cmd = new Command(postCmd);
            cmd.setStreamHandling(Command.STDOUT, Command.CAPTURE);
            cmd.setOutputFile(Command.STDOUT, postFile);
            ctx.exec(cmd, false);

            // We want both the raw output and xan output files
            ctx.setOutputFile("raw", rawFile);
        }
    }
    /**/
}
