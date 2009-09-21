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

import com.sun.faban.common.Command;
import com.sun.faban.harness.agent.CmdAgentImpl;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

/**
 * Statit is a wrapper for the statit utility.
 * Currently, it only supports the -x and -y options 
 * <ul>
 * <li> It implements the Tool interface by extending GenericTool
 * </ul>
 *
 * @author Ramesh Ramachandran
 * @see GenericTool
 * @see Tool
 * @deprecated
 */
@Deprecated public class Statit extends GenericTool {

    public void configure(String toolName, List<String> argList, String path,
                          String outDir, String host, String masterhost,
                          CmdAgentImpl cmdAgent, CountDownLatch latch) {

        // Insert the -x option at the beginning.
        argList.add(0, "-x");

        super.configure(toolName, argList, path, outDir, host, masterhost,
                        cmdAgent, latch);
    }

    protected void stop(boolean warn) {

        logger.fine("Stopping statit");
        if (toolStatus == STARTED) {
            Command c = new Command("statit -y");
            c.setStreamHandling(Command.STDOUT, Command.CAPTURE);
            c.setOutputFile(Command.STDOUT, logfile);
            try {
                // Replace tool so that super.stop gets the output from -y instead.
                tool = cmdAgent.execute(c, null);
                // saveToolLogs(tool.getInputStream(), tool.getErrorStream());
                toolStatus = STOPPED;
                // xfer log file to master machine, log any errors
                xferLog();
                logger.fine(toolName + " Stopped ");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error executing command statit -y", e);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE,
                           "Interrupted waiting for command statit -y", e);
            }
        } else if (warn && toolStatus == NOT_STARTED)
            logger.warning("Tool not started but stop called for " + this.toolCmd);
    }
}