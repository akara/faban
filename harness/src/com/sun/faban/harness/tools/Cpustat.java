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
 * $Id: Cpustat.java,v 1.5 2008/04/04 22:09:27 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.tools;

import com.sun.faban.common.Command;
import com.sun.faban.harness.agent.CmdAgent;
import com.sun.faban.harness.agent.CmdAgentImpl;
import com.sun.faban.harness.common.Config;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

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

    protected void xferLog() {

        super.xferLog();

        // Do the post-processing after the transfer.
        Command c = null;
        try {

            // Run postprocessor on master
            c = new Command("cpustat-post", outfile);
            c.setOutputFile(Command.STDOUT, postFile);
            CmdAgent masterAgent = (CmdAgent) CmdAgentImpl.getRegistry().
                                   getService(Config.CMD_AGENT);
            masterAgent.execute(c);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error executing " + c, e);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "cpustat postprocessor interrupted.", e);
        }
    }
}
