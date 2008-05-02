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
 * $Id: Wlp2.java,v 1.4 2008/05/02 21:30:03 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.tools;

import com.sun.faban.harness.agent.CmdAgent;
import com.sun.faban.harness.common.Config;

import java.io.File;
import java.util.List;

/**
 * Wlp2 is an Oracle/TPC-E specific tool. So this class does not really belong here.
 * Once we can deploy tools as part of the benchmark, we need to move this class to
 * the appropriate place.
 *
 * @author Akara Sucharitakul
 */
public class Wlp2 extends Statspack {

    private String outDir;
    private String host;
    private String benchName;
    private String wlp2cmd = "";

    /**
     * This is the method that should get the arguments to
     * call the tool with.
     */
    public void configure(String tool, List argList, String path, String outDir,
                          String host, String masterhost, CmdAgent cmdAgent) {
        this.outDir = outDir;
        this.host = host;

        // This is a hack - obtain the benchmark name from the outDir
        int dotIdx = outDir.lastIndexOf('.');
        int benchIdx = outDir.lastIndexOf(File.separator, dotIdx) + 1;
        benchName = outDir.substring(benchIdx, dotIdx);

        super.configure("statspack", argList, path, outDir, host, masterhost, cmdAgent);
    }

    /**
     * Creates a sqlplus command to create awr reports.
     *
     * @param snapId     The id of the first snap
     * @param snapId1    The id of the second snap
     * @param outputFile The output file
     * @return The sqlplus command string
     */
    protected String getReportCommand(String snapId, String snapId1, String outputFile) {

        String script = Config.BENCHMARK_DIR + benchName + File.separator +
                        "lib" + File.separator + "wlp2";

        logger.info("Script path: " + script);

        if (new File(script + ".sql").exists())
            wlp2cmd = "\n@" + script + "\n\n" +
                    snapId + "\n" +
                    snapId1 + "\n" +
                    outputFile + "-wlp2";

        return super.getReportCommand(snapId, snapId1, outputFile) + wlp2cmd;
    }

    protected void xferLog() {
        // Transfer the statspack output first.
        super.xferLog();

        //  Then transfer the wlp2 file.
        xferWlpLog();
    }

    private void xferWlpLog() {
        String logfile = this.logfile + "-wlp2";
        String outfile = outDir + "wlp2.log." + host;
        xferFile(logfile, outfile);
    }
}
