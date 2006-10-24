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
 * $Id: RunName.java,v 1.1 2006/10/24 05:24:21 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.common;



/**
 * The RunName class represents a run name and provides standard facilities
 * to decode the run name. The RunName class is immutable.
 *
 * @author Akara Sucharitakul
 */
public class RunName {

    private String host;
    private String benchName;
    private String runId;
    private String runName;


    /**
     * Constructs a run name based on the benchmark name and the run id.
     * @param benchName The benchmark name
     * @param runId The run id
     */
    public RunName(String benchName, String runId) {
        this(null, benchName, runId);
    }

    /**
     * Constructs a run name based on the host name, the benchmark name,
     * and the run id.
     * @param host The host name
     * @param benchName The benchmark name
     * @param runId The run id
     */
    public RunName(String host, String benchName, String runId) {
        this.host = host;
        this.benchName = benchName;
        this.runId = runId;
        StringBuilder runNameBuffer = new StringBuilder();
        if (host != null)
            runNameBuffer.append(host).append('.');
        runNameBuffer.append(benchName).append('.').append(runId);
        this.runName = runNameBuffer.toString();
    }

    /**
     * Parses a string representation of a run name into the RunName object.
     * @param runName The full run name.
     */
    public RunName(String runName) {
        int dotIdx = runName.lastIndexOf('.');
        if (dotIdx == -1)
            throw new IndexOutOfBoundsException("Run name " + runName +
                    "not conforming to spec <host.>benchName.runId!");
        runId = runName.substring(dotIdx + 1);
        int dotIdx2 = runName.lastIndexOf('.', dotIdx - 1);
        benchName = runName.substring(dotIdx2 + 1, dotIdx);
        if (dotIdx2 != -1) // host part
            host = runName.substring(0, dotIdx2);
        this.runName = runName;
    }

    /**
     * Returns the host portion of the run name.
     * @return The host name
     */
    public String getHostName() {
        if (host == null)
            return "";
        return host;
    }

    /**
     * Returns the benchmark name portion of the run name.
     * @return The benchmark name
     */
    public String getBenchName() {
        return benchName;
    }

    /**
     * Returns the id portion of the run name.
     * @return THe run id
     */
    public String getRunId() {
        return runId;
    }

    /**
     * Returns the string representation of RunName in the form <host.>bench.id.
     * @return A string representation of the object.
     */
    public String toString() {
        return runName;
    }
}
