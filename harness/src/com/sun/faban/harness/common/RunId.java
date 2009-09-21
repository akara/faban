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
package com.sun.faban.harness.common;

import java.io.File;


/**
 * The RunId class represents a run name and provides standard facilities
 * to decode the run name. The RunId class is immutable.
 *
 * @author Akara Sucharitakul
 */
public class RunId implements Comparable {

    private String host;
    private String benchName;
    private String runSeq;
    private String runId;


    /**
     * Constructs a run name based on the benchmark name and the run sequence.
     * @param benchName The benchmark name
     * @param runSeq The run id
     */
    public RunId(String benchName, String runSeq) {
        this(null, benchName, runSeq);
    }

    /**
     * Constructs a run name based on the host name, the benchmark name,
     * and the run sequence.
     * @param host The host name
     * @param benchName The benchmark name
     * @param runSeq The run sequence
     */
    public RunId(String host, String benchName, String runSeq) {
        this.host = host;
        this.benchName = benchName;
        this.runSeq = runSeq;
        StringBuilder runIdBuffer = new StringBuilder();
        if (host != null)
            runIdBuffer.append(host).append('.');
        runIdBuffer.append(benchName).append('.').append(runSeq);
        this.runId = runIdBuffer.toString();
    }

    /**
     * Parses a string representation of a runId into the RunId object.
     * @param runId The run id.
     */
    public RunId(String runId) {
        int dotIdx = runId.lastIndexOf('.');
        if (dotIdx == -1)
            throw new IndexOutOfBoundsException("Run id " + runId +
                    "not conforming to spec <host.>benchName.runSeq!");
        runSeq = runId.substring(dotIdx + 1);
        int dotIdx2 = runId.lastIndexOf('.', dotIdx - 1);
        benchName = runId.substring(dotIdx2 + 1, dotIdx);
        if (dotIdx2 != -1) // host part
            host = runId.substring(0, dotIdx2);
        this.runId = runId;
    }

    /**
     * Returns the host portion of the run id.
     * @return The host name
     */
    public String getHostName() {
        if (host == null)
            return "";
        return host;
    }

    /**
     * Returns the benchmark name portion of the run id.
     * @return The benchmark name
     */
    public String getBenchName() {
        return benchName;
    }

    /**
     * Returns the sequence portion of the run id.
     * @return The run sequence
     */
    public String getRunSeq() {
        return runSeq;
    }

    /**
     * Return the result directory associated with this run.
     * @return The result directory for this run
     */
    public File getResultDir() {
        return new File(Config.OUT_DIR, runId);
    }

    /**
     * Returns the string representation of RunId in the form <host.>bench.seq.
     * @return A string representation of the object.
     */
    public String toString() {
        return runId;
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *                            from being compared to this Object.
     */
    public int compareTo(Object o) {
        RunId other = (RunId) o;
        int compare = 0;

        // Host field may be null
        if (host != null) {
            if (other.host != null)
                compare = host.compareTo(other.host);
            else
                compare = host.compareTo("");
        } else if (other.host != null) {
            compare = "".compareTo(other.host);
        }
        
        if (compare == 0)
            compare = benchName.compareTo(other.benchName);
        if (compare == 0)
            compare = compareSeq(other);
        return compare;
    }

    /**
     * Compares the run sequence between one and the other run id. Returns a
     * negative integer, zero, or a positive integer as this sequence is less
     * than, equal to, or greater than the specified run id's sequence.
     * @param o The other run id.
     * @return a negative integer, zero, or a positive integer as this run id
     *         is less than, equal to, or greater than the specified run id.
     */
    public int compareSeq(RunId o) {
        // Split the run sequence into the number and trailing char
        int postIdx = 0;
        for (; postIdx < runSeq.length(); postIdx++) {
            if (Character.isLetter(runSeq.charAt(postIdx)))
                    break;
        }
        String pre = runSeq.substring(0, postIdx);
        char post = runSeq.charAt(postIdx);
        String dup = runSeq.substring(++postIdx);

        String seq = o.runSeq;
        for (postIdx = 0; postIdx < seq.length(); postIdx++) {
            if (Character.isLetter(seq.charAt(postIdx)))
                    break;
        }
        String pre1 = seq.substring(0, postIdx);
        char post1 = seq.charAt(postIdx);
        String dup1 = seq.substring(++postIdx);

        int compare = Integer.parseInt(pre) - Integer.parseInt(pre1);
        if (compare == 0)
            compare = post - post1;
        if (compare == 0)
            compare = dup.compareTo(dup1);

        return compare; 
    }
}
