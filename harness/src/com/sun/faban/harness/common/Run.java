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
 * $Id: Run.java,v 1.4 2006/10/24 05:24:21 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.common;
import java.io.*;

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
    private BenchmarkDescription benchDesc;
    private String outdir;		// output directory name for this run
    private String runqdir;		// Runq directory name for this run
    private String runID;
    private String runName;
    private boolean inRunQ;

    public Run(String runID, BenchmarkDescription benchDesc) {
        this.benchDesc = benchDesc;
        runName = benchDesc.shortName + "." + runID;
        this.runID = runID;

        runqdir = Config.RUNQ_DIR + runName + File.separator;
        outdir = Config.OUT_DIR + runName + File.separator;

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
    public String getRunID() {
        return runID;
    }

    /**
     * Get the name of this run. 
     *
     */
    public String getRunName() {
        return runName;
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
    public void updateStatus(String status) throws IOException {
        // Update the resultinfo file with Status
        File resultInfo = new File(outdir, Config.RESULT_INFO);
        resultInfo.delete();
        resultInfo.createNewFile();
        FileWriter writer = new FileWriter(resultInfo);
        writer.write(status);
        writer.flush();
        writer.close();
    }
}






