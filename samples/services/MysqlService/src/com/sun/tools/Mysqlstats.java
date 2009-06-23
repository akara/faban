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
 * $Id: Mysqlstats.java,v 1.2 2009/06/23 19:02:13 sheetalpatil Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.tools;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.Context;
import com.sun.faban.harness.tools.Configure;
import com.sun.faban.harness.tools.Start;
import com.sun.faban.harness.tools.Stop;

import com.sun.faban.harness.tools.ToolContext;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Mysqlstats {

    private static Logger logger =
            Logger.getLogger(Mysqlstats.class.getName());

    @Context public ToolContext ctx;
    Command cmd;
    CommandHandle processRef;
    ArrayList<String> toolCmd;
    String toolName;

    @Configure public void config() {
        toolName = ctx.getToolName();
        
        String mysqlHome = ctx.getServiceProperty("serverHome");
        String mysqlUser = ctx.getServiceProperty("user");
        String mysqlPass = ctx.getServiceProperty("password");

        if (mysqlHome.endsWith(File.separator))
            mysqlHome = mysqlHome.substring(0, mysqlHome.length() -
                                              File.separator.length());

        toolCmd = new ArrayList<String>();
        String mysqlCmd = "mysql";
        if (mysqlHome != null)
            mysqlCmd = mysqlHome + File.separator + "bin" + File.separator + mysqlCmd;
        toolCmd.add(mysqlCmd);
		if (mysqlUser != null)
            toolCmd.add("-u" + mysqlUser);
        if (mysqlPass != null)
            toolCmd.add("-p" + mysqlPass);
        toolCmd.add("-B");
        toolCmd.add("-e");
        toolCmd.add("show global status;");
        logger.info("Setting up mysql command: " + toolCmd);
        cmd = new Command(toolCmd);
        cmd.setOutputFile(Command.STDOUT, ctx.getOutputFile());
        cmd.setSynchronous(false);
        logger.info(toolName + " Configured with toolCmd " + toolCmd);

    }

    @Start public void start() throws IOException, InterruptedException {
        processRef = ctx.exec(cmd);
        logger.info(toolName + " Started with Cmd = " + toolCmd);
    }

    @Stop public void stop() {
        try {
            logger.info("Stopping tool " + this.toolCmd);
            processRef.destroy();
            processRef.waitFor(10000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Mysqlstats.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RemoteException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

}

