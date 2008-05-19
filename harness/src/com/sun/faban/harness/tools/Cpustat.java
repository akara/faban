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
 * $Id: Cpustat.java,v 1.6 2008/05/19 22:59:55 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.tools;

import com.sun.faban.common.Command;
import com.sun.faban.common.FileTransfer;
import com.sun.faban.harness.agent.CmdAgent;
import com.sun.faban.harness.agent.CmdAgentImpl;
import com.sun.faban.harness.agent.FileAgent;
import com.sun.faban.harness.common.Config;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.rmi.RemoteException;

/**
 * Cpustat starts the cpustat tool. Unlike GenericTool, cpustat needs some
 * further postprocessing before the data is useful.
 *
 * @author Akara Sucharitakul
 * @see com.sun.faban.harness.tools.Tool
 */
public class Cpustat extends GenericTool {

    String postFile;

    public void configure(String tool, List argList, String path, String outDir,
                          String host, String masterhost, CmdAgent cmdAgent) {
        super.configure(tool, argList, path, outDir, host, masterhost, cmdAgent);

        // The postprocessed output is in .xan.host file
        postFile = outfile.replace(".log.", ".xan.");

        // The raw output will come into the .raw.host file
        outfile = outfile.replace(".log.", ".raw.");
    }


    /**
     * This method is responsible for stopping the tool utility.
     */
    @Override public void stop(boolean warn) {
        super.stop(warn);

        try {
            Thread.sleep(5000); // Ensure we're really beyond steadystate.
            Command c = new Command("cpustat-post", logfile);
            c.execute();

            FileTransfer transfer = tool.fetchOutput(Command.STDOUT, postFile);
            logger.finer("Transferring CPUstat post output to " + postFile);
            if(transfer != null) {
                // Use FileAgent on master machine to copy log
                String s = Config.FILE_AGENT;
                FileAgent fa = (FileAgent) CmdAgentImpl.getRegistry().
                                                            getService(s);
                if (fa.push(transfer) != transfer.getSize())
                    logger.log(Level.SEVERE, "Invalid transfer size");
            }
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "RemoteException post-processing cpustat!",
                    e);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted post-processing cpustat!", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IOException post-processing cpustat!", e);
        }
    }
}
