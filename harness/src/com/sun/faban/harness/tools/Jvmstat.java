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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Jvmstat.java,v 1.1 2006/06/29 18:51:43 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.tools;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.agent.CmdAgent;
import com.sun.faban.harness.util.DeployUtil;
import com.sun.faban.common.Command;

/**
 * Jvmstat is a wrapper for the jvmstat utility.
 * <ul>
 * <li> It implements the Tool interface by extending GenericTool
 * </ul>
 *
 * @author Ramesh Ramachandran
 * @see GenericTool
 * @see com.sun.faban.harness.tools.Tool
 */
public class Jvmstat extends GenericTool {

    List argList;

    /**
     * This is the method that should get the arguments to
     * call the tool with.
     */
    public void configure(String tool, List argList, String path, String outDir,
                          String host, String masterhost, CmdAgent cmdAgent) {
        this.argList = argList;
        path = DeployUtil.getJavaHome() + File.separator + "bin" + File.separator;
        tool = "java";
        argList.add(0, "-jar");
        argList.add(1, Config.LIB_DIR + "jvmps.jar");
        argList.add(2, "-v");
        super.configure(tool, argList, path, outDir, host, masterhost, cmdAgent);
    }

    public boolean start(int delay) {

        ArrayList pids = new ArrayList();
        Command cmd = new Command(this.cmd);
        cmd.setStreamHandling(Command.STDOUT, Command.CAPTURE);
        String result = null;
        try {
            tool = cmdAgent.execute(cmd);
            result = new String(tool.fetchOutput(Command.STDOUT));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error starting command " + this.cmd, e);
            return false;
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted starting command " +
                    this.cmd, e);
            return false;
        }

        int startIdx = 0;
        int endIdx = result.indexOf('\n');
        while (endIdx > 0) {
            String line = result.substring(startIdx, endIdx);
            startIdx = endIdx + 1;
            endIdx = result.indexOf('\n', startIdx);
            if (line == null || line.length() == 0)
                continue;
            if (line.indexOf("jvmps") == -1 && line.indexOf("faban") == -1)
                pids.add(line.substring(0, line.indexOf(' ')));
        }

        argList.set(1, Config.LIB_DIR + "jvmstat.jar");
        argList.set(2, pids.get(0));
        for (int i = 1; i < pids.size(); i++)
            argList.add(i + 2, pids.get(i));

        buildCmd(argList);


        // @todo If there are more than one JVM we need to spawn multiple jvmstat

        // get the cmd line params for the tool and append it.
        return super.start(delay);
    }
    
    //  All other methods are inherited from GenericTool
    //  @todo The process.destroy is not killing the jvmstat process.

}