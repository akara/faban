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
 * $Id: Cpustat.java,v 1.11 2009/06/25 23:13:38 sheetalpatil Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.tools;

import com.sun.faban.common.Command;

import java.io.IOException;

/**
 * Cpustat starts the cpustat tool. Unlike GenericTool, cpustat needs some
 * further postprocessing before the data is useful.
 *
 * @author Akara Sucharitakul
 * @see com.sun.faban.harness.tools.Tool
 */
public class Cpustat extends CommandLineTool{

    @Configure
    public void configure() {
        super.config();
        // The postprocessed output is in .xan.host file
        String rawFile = ctx.getOutputFile();
        ctx.setOutputFile(rawFile.replace(".log.", ".raw."));
    }


    /**
     * This method is responsible for stopping the tool utility.
     */
    @Stop
    public void stop() throws IOException, InterruptedException {
        super.stop();

        String rawFile = ctx.getOutputFile();
        String postFile = rawFile.replace(".raw.", ".xan.");
        ctx.setOutputFile(postFile);

        Thread.sleep(500);

        cmd = new Command("cpustat-post");
        cmd.setStreamHandling(Command.STDOUT, Command.CAPTURE);
        cmd.setOutputFile(Command.STDOUT, postFile);
        ctx.exec(cmd);
    }

    @Postprocess
    public void postprocess() throws IOException, InterruptedException {

    }
}
