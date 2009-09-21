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
import com.sun.faban.harness.Configure;
import com.sun.faban.harness.Context;

import com.sun.faban.harness.Start;
import com.sun.faban.harness.Stop;
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
    List<String> toolArgs;

    /**
     * Configures the command line tool.
     * Tasks done in this method includes building the
     * toolCmd using the params from the tool context.
     */
    @Configure public void config() {
        toolName = ctx.getToolName();
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
        processRef = ctx.exec(cmd, true);
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
    }
}
