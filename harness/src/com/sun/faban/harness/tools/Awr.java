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
package com.sun.faban.harness.tools;

/**
 * Runs Oracle's awr tool.
 * @deprecated
 */
@Deprecated public class Awr extends OracleTool {


    /**
     * Obtains the final output file name.
     * @param outDir The output or run directory
     * @param host The host name running the tool
     * @return The output file name
     */
    protected String getOutputFile(String outDir, String host) {
        return super.getOutputFile(outDir, host) + ".log";
    }

    /**
     * Obtains the temporary log file name.
     * @return The log file name
     */
    protected String getLogFile() {
        return super.getLogFile() + ".log";
    }

    /**
     * Creates a sqlplus command to create an awr snapshot.
     * @return The sqlplus command string
     */
    protected String getSnapCommand() {
        return "variable snapid number;\n" +
               "begin\n" +
               "    :snapid := dbms_workload_repository.create_snapshot();\n" +
               "end;\n" +
               "/\n" +
               "print :snapid";
    }

    /**
     * Creates a sqlplus command to create awr reports.
     * @param snapId The id of the first snap
     * @param snapId1 The id of the second snap
     * @param outputFile The output file
     * @return The sqlplus command string
     */
    protected String getReportCommand(String snapId, String snapId1,
                                   String outputFile) {
        return "@?/rdbms/admin/awrrpt\n" +
               "text\n" +
               "\n" +
               snapId + "\n" +
               snapId1 + "\n" +
               outputFile;
    }
}
