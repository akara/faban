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
package com.sun.faban.harness.webclient;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.security.Acl;
import com.sun.faban.harness.util.FileHelper;
import com.sun.faban.common.Command;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.HashSet;
import java.util.ArrayList;

/**
 * Run Analyzer that handles all the backend tasks to analyze the runs.
 *
 * @author Akara Sucharitakul
 */
public class RunAnalyzer {

    /** Analysis types. */
    public enum Type {

        /** Comparison. */
        COMPARE,

        /** Averaging. */
        AVERAGE;

        /**
         * Returns the string representation of the analysis type.
         * @return The string representation of the enum elements in lower case
         */
        public String toString() {
            return name().toLowerCase();
        }
    };

    private static Logger logger =
            Logger.getLogger(RunAnalyzer.class.getName());

    /**
     * Provides a suggested analysis name for the analysis based on the
     * analysis type and and run ids that are used.
     * The run id can come in the form of bench.seq or
     * host.bench.seq. We try to keep the suggested name
     * as short as possible. We have three formats:
     * <ol>
     * <li>Generic format: type_runId1_runId2...</li>
     * <li>Same benchmark: type-bench_<host.>seq1_<host>.seq2...</li>
     * <li>Same host, same benchmark: type-bench-host_seq1_seq2...</li>
     * </ol>
     * @param type Whether it is a compare or an average
     * @param runIdStrings The run ids to include in the analysis
     * @return The suggested analysis name
     */
    public static String suggestName(Type type, String[] runIdStrings) {
        StringBuilder suggestion = new StringBuilder();
        HashSet<String> benchNameSet = new HashSet<String>();
        HashSet<String> hostNameSet = new HashSet<String>();
        suggestion.append(type);

        RunId[] runIds = new RunId[runIdStrings.length];
        String benchName = null;
        String hostName = null;
        for (int i = 0; i < runIdStrings.length; i++) {
            RunId runId = new RunId(runIdStrings[i]);
            benchName = runId.getBenchName();
            hostName = runId.getHostName();
            benchNameSet.add(benchName);
            hostNameSet.add(hostName);
            runIds[i] = runId;
        }

        if (benchNameSet.size() == 1) {
            suggestion.append('-').append(benchName);
            if (hostNameSet.size() == 1) {
                if (hostName.length() > 0)
                    suggestion.append('-').append(hostName);
                for (RunId runId : runIds)
                    suggestion.append('_').append(runId.getRunSeq());
            } else {
                for (RunId runId : runIds) {
                    suggestion.append('_');
                    hostName = runId.getHostName();
                    if (hostName.length() > 0)
                        suggestion.append(hostName).append('.');
                    suggestion.append(runId.getRunSeq());
                }
            }
        } else {
            for (RunId runId : runIds)
                suggestion.append('_').append(runId);
        }
        return suggestion.toString();

    }

    /**
     * Checks whether the analysis with the given name exists.
     * @param name The analysis name
     * @return true if the analysis exists, false otherwise
     */
    public static boolean exists(String name) {
        File analysisDir = new File(Config.ANALYSIS_DIR + name);
        File resultFile = new File(analysisDir, "index.html");
        if (resultFile.exists()) {
            return true;
        } else if (analysisDir.isDirectory()) {
            FileHelper.recursiveDelete(analysisDir);
            return false;
        } else {
            return false;
        }
    }

    /**
     * Removes the analysis results.
     * @param name The name of the result to remove
     */
    public static void clear(String name) {
        File analysisDir = new File(Config.ANALYSIS_DIR + name);
        if (analysisDir.isDirectory()) {
            FileHelper.recursiveDelete(analysisDir);
        }
    }

    /**
     * Executes the run analysis.
     * @param type The type of the analysis
     * @param runIdStrings The run ids to analyze, in form of String array
     * @param output The name of the analysis results
     * @param user The user name requesting this analysis
     * @throws IOException The analysis failed and results are not generated
     */
    public static void analyze(Type type, String[] runIdStrings,
                               String output, String user)
            throws IOException {
        File analysisDir = new File(Config.ANALYSIS_DIR + output);
        if (!analysisDir.mkdirs()) {
            throw new IOException("Failed creating directory " +
                                    analysisDir +'!');
        }
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(Config.BIN_DIR.trim() + "fenxi");
        cmd.add(type.toString());
        for (String runId : runIdStrings) {
            String inputDir = Config.OUT_DIR + runId;
            if (new File(inputDir, "xanaDB").isDirectory()) {
                cmd.add(inputDir);
                continue;
            }
            inputDir += File.separator + Config.POST_DIR;
            if (new File(inputDir, "xanaDB").isDirectory()) {
                cmd.add(inputDir);
                continue;
            }
            // If we're here, no xanaDB is found.
            throw new IOException("RunId " + runId +
                    " has not been post-processed and cannot be analyzed. " +
                    "Please run fenxi process on the run result first.");
        }

        cmd.add(Config.ANALYSIS_DIR + output);

        // Before we put anything in, we deal with security.
        File metaDir = new File(analysisDir, "META-INF");
        metaDir.mkdir();

        // Merge the ACLs from all the source
        Acl.merge(runIdStrings, output);

        if (user != null)
            FileHelper.writeStringToFile(user, new File(metaDir, "submitter"));

        Command c = new Command(cmd);
        try {
            c.execute();
        } catch (InterruptedException e) {
            String msg = "Analysis interrupted";
            logger.log(Level.SEVERE, msg, e);
            throw new IOException(msg);
        }
        File outIdx = new File(analysisDir, "index.html");
        if (!outIdx.exists()) {
            FileHelper.recursiveDelete(analysisDir);
            throw new IOException("Failed creating analysis.");
        }
    }
}
