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
 * $Id: Run.java,v 1.7 2009/05/28 00:55:25 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.common;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This is the benchmark run object. The run can either be in the
 * RunQ (before it executes) or in the output directory when and after
 * it finishes executing.
 * This class keeps track of the various files in a run.
 * An object of this class is created by the Engine and passed as a
 * a parameter to the various services and benchmark code.
 *
 * @author Ramesh Ramachandran
 */
public class Run implements Serializable {

    public static final int STARTED = 0;
    public static final int RECEIVED = 1;
    public static final int COMPLETED = 2;
    public static final int FAILED = 3;
    public static final int KILLED = 4;

    public static final String[] STATUS_MESSAGE =
            { "STARTED", "RECEIVED", "COMPLETED", "FAILED", "KILLED" };

    private BenchmarkDescription benchDesc;
    private String outdir;		// output directory name for this run
    private String runqdir;		// Runq directory name for this run
    private String runSeq;
    private String runId;
    private boolean inRunQ;

    // Formatter is used to format dates with the status changes.
    private SimpleDateFormat formatter = new SimpleDateFormat(
                              "EEE MMM dd HH:mm:ss z yyyy");


    public Run(String runSeq, BenchmarkDescription benchDesc) {
        this.benchDesc = benchDesc;
        runId = benchDesc.shortName + "." + runSeq;
        this.runSeq = runSeq;

        runqdir = Config.RUNQ_DIR + runId + File.separator;
        outdir = Config.OUT_DIR + runId + File.separator;

        // Check if run is in RunQ
        if((new File(runqdir)).exists())
            inRunQ = true;
        else
            inRunQ = false;
    }

    /**
     * Get the id of the run.
     *
     */
    public String getRunSeq() {
        return runSeq;
    }

    /**
     * Get the name of this run. 
     *
     */
    public String getRunId() {
        return runId;
    }

    /**
     * Obtains the short name of the benchmark run.
     * @return The benchmark's short name
     */     
    public String getBenchmarkName() {
        return benchDesc.shortName;
    }

    /**
     * Get name of benchmark
     */
    public BenchmarkDescription getBenchDesc() {
        return benchDesc;
    }

    /**
     * Get pathname of log for this run
     */
    public String getLog() {
        return(outdir + Config.LOG_FILE);
    }

    /**
     * Get full pathname of ParamRepository for this run
     * This method checks in the run is in the RunQ or output
     * directories, and returns the appropriate path.
     */
    public String getParamFile() {
        if (inRunQ)
            return(runqdir + benchDesc.configFileName);
        else
            return(outdir + benchDesc.configFileName);
    }

    /**
     * Get output directory pathname
     */
    public String getOutDir() {
        return outdir;
    }

    /**
     * Updates the run status in the result info file.
     * @param status The new run status
     * @throws IOException If the update fails
     */
    public void updateStatus(int status) throws IOException {
        // Update the resultinfo file with Status
        File resultInfo = new File(outdir, Config.RESULT_INFO);
        resultInfo.delete();
        resultInfo.createNewFile();
        FileWriter writer = new FileWriter(resultInfo);
        String content = STATUS_MESSAGE[status] + '\t' +
                                formatter.format(new Date());
        writer.write(content);
        writer.flush();
        writer.close();
    }
}






