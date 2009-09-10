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
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.tools;

import com.sun.faban.common.Command;
import com.sun.faban.harness.Configure;
import com.sun.faban.harness.Context;
import com.sun.faban.harness.Start;
import com.sun.faban.harness.Stop;
import com.sun.faban.harness.tools.Postprocess;
import com.sun.faban.harness.tools.ToolContext;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

/**
 * ApacheErrorLog implements a tool used for gathering the statistics from a
 * MySQL instance.
 */
public class ApacheErrorLog {

    /** The injected tool context. */
    @Context public ToolContext ctx;
    String errlogFile, beginDate, endDate;
    Command cmd;
    GregorianCalendar calendar = new GregorianCalendar();
    SimpleDateFormat df = new SimpleDateFormat("MMM,dd,HH:mm:ss");

    /**
     * Configures the ApacheErrorLog.
     */
    @Configure public void config() {
        String logsDir = ctx.getServiceProperty("logsDir");
        if (!logsDir.endsWith(File.separator))
            logsDir = logsDir + File.separator;
        errlogFile = logsDir + "error_log";
    }

    /**
     * Starts the ApacheErrorLog.
     */
    @Start public void start() {
        beginDate = df.format(calendar.getTime());
    }

    /**
     * Stops the ApacheErrorLog.
     */
    @Stop public void stop() {
        endDate = df.format(calendar.getTime());
    }

    /**
     * This method is responsible for postprocessing.
     * @throws IOException Error post-processing ApacheErrorLog
     * @throws InterruptedException Interrupted waiting for commands
     */
    @Postprocess
    public void postprocess() throws IOException, InterruptedException {
        ctx.setOutputFile(errlogFile);
        //parse the log file
        Command parseCommand = new Command("apache_trunc_errorlog.sh " +
                beginDate + " " + endDate + " " + ctx.getOutputFile());
        ctx.exec(parseCommand, true);
    }
}