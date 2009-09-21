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
 * Runs Oracle's statspack tool.
 * @deprecated
 */
@Deprecated public class Statspack extends OracleTool {

    /**
     * Creates a sqlplus command to create an awr snapshot.
     * @return The sqlplus command string
     */
    protected String getSnapCommand() {
        return "variable snapid number;\n" +
               "define top_n_segstat = 10;\n" +
               "begin\n" +
               "    :snapid := statspack.snap(i_snap_level=>7);\n" +
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
        return "@?/rdbms/admin/spreport\n" +
               snapId + "\n" +
               snapId1 + "\n" +
               outputFile;
    }
}
