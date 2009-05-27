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

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.Context;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;


/**
 *
 * @author Sheetal Patil
 */
public class CommandLineTool {

    private static Logger logger =
            Logger.getLogger(CommandLineTool.class.getName());

    @Context public ToolContext ctx;
    Command cmd;
    CommandHandle processRef;
    ArrayList<String> toolCmd;
    String toolName;
    List<String> toolArgs;

    @Configure public void config() {
        toolName = ctx.getToolName();
        toolArgs = ctx.getToolArgs();
        toolCmd = new ArrayList<String>();
        toolCmd.add(toolName);
        if (toolArgs != null)
            toolCmd.addAll(toolArgs);
        cmd = new Command(toolCmd);
        cmd.setOutputFile(Command.STDOUT, ctx.getOutputFile());
        cmd.setSynchronous(false);
        logger.fine(toolName + " Configured with toolCmd " + toolCmd);

    }

    @Start public void start() throws IOException, InterruptedException {
        processRef = ctx.exec(cmd);
        logger.fine(toolName + " Started with Cmd = " + toolCmd);
    }

    @Stop public void stop() throws RemoteException, InterruptedException {
        logger.fine("Stopping tool " + this.toolCmd);
        processRef.destroy();
        processRef.waitFor(10000);
    }

}
