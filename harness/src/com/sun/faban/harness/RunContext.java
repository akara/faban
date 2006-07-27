/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://faban.dev.java.net/public/CDDLv1.0.html or
 * install_dir/license.txt
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
 * $Id: RunContext.java,v 1.1 2006/07/27 22:34:34 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;
import com.sun.faban.harness.engine.CmdService;

import java.io.File;
import java.io.IOException;

/**
 * The RunContext provides a benchmark with the environment for it to run.
 * It provides all callbacks into the harness.
 *
 * @author Akara Sucharitakul
 */
public class RunContext {

    private Run run;
    private ParamRepository param;

    public RunContext(Run run, ParamRepository param) {
        this.run = run;
        this.param = param;
    }

    public String getBenchmarkDir() {
        return Config.BENCHMARK_DIR + run.getBenchmarkName() + File.separator;
    }

    public String getOutDir() {
        return run.getOutDir();
    }

    public ParamRepository getParamRepository() {
        return param;
    }

    public CommandHandle execute(Command c)
            throws IOException, InterruptedException {
        return CmdService.getHandle().execute(c);
    }

    public CommandHandle execute(String host, Command c)
            throws IOException, InterruptedException {
        return CmdService.getHandle().execute(host, c);
    }

    public CommandHandle[] execute(String[] hosts, Command c)
            throws IOException, InterruptedException {
        return CmdService.getHandle().execute(hosts, c);
    }

    public String getParamFile() {
        return run.getParamFile();
    }
}
