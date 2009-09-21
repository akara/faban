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
import com.sun.faban.common.Utilities;
import com.sun.faban.harness.Configure;
import com.sun.faban.harness.RunContext;
import com.sun.faban.harness.Start;
import com.sun.faban.harness.common.Config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Jvmstat is a wrapper for the jvmstat utility.
 * <ul>
 * <li> It implements the Tool interface by extending GenericTool
 * </ul>
 *
 * @author Ramesh Ramachandran
 * @see com.sun.faban.harness.tools.Tool
 */
public class Jvmstat extends CommandLineTool {


    /**
     * This is the method that should get the arguments to
     * call the tool with.
     */
    @Configure public void configure() {
        super.config();
    }

    /**
     * This method is responsible for starting the tool utility.
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    @Start public void start() throws IOException, InterruptedException {
        ArrayList<String> jvmCmd = new ArrayList<String>();
        ArrayList<String> argList = new ArrayList<String>();
        String path = Utilities.getJavaHome() + File.separator + "bin" + File.separator+ "java";
        String tool = "java";
        argList.add(0, "-jar");
        argList.add(1, Config.LIB_DIR + "jvmps.jar");
        argList.add(2, "-v");
        jvmCmd.add(path);
        jvmCmd.add(tool);
        jvmCmd.addAll(argList);
        ArrayList<String> pids = new ArrayList<String>();
        Command c = new Command(jvmCmd);
        c.setStreamHandling(Command.STDOUT, Command.CAPTURE);
        String result = null;
        processRef = RunContext.exec(c);
        
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

        jvmCmd.add(path);
        jvmCmd.add(tool);
        jvmCmd.addAll(argList);

        // @todo If there are more than one JVM we need to spawn multiple jvmstat

        // get the toolCmd line params for the tool and append it.
        super.start();
    }
    
    //  All other methods are inherited from CommandLineTool
    //  @todo The process.destroy is not killing the jvmstat process.

}