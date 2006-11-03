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
 * $Id: RunId.java,v 1.3 2006/11/03 09:45:46 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
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
     * than, equal to, or greater than the specified object.<p>
     * <p/>
     * In the foregoing description, the notation
     * <tt>sgn(</tt><i>expression</i><tt>)</tt> designates the mathematical
     * <i>signum</i> function, which is defined to return one of <tt>-1</tt>,
     * <tt>0</tt>, or <tt>1</tt> according to whether the value of <i>expression</i>
     * is negative, zero or positive.
     * <p/>
     * The implementor must ensure <tt>sgn(x.compareTo(y)) ==
     * -sgn(y.compareTo(x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
     * implies that <tt>x.compareTo(y)</tt> must throw an exception iff
     * <tt>y.compareTo(x)</tt> throws an exception.)<p>
     * <p/>
     * The implementor must also ensure that the relation is transitive:
     * <tt>(x.compareTo(y)&gt;0 &amp;&amp; y.compareTo(z)&gt;0)</tt> implies
     * <tt>x.compareTo(z)&gt;0</tt>.<p>
     * <p/>
     * Finally, the implementer must ensure that <tt>x.compareTo(y)==0</tt>
     * implies that <tt>sgn(x.compareTo(z)) == sgn(y.compareTo(z))</tt>, for
     * all <tt>z</tt>.<p>
     * <p/>
     * It is strongly recommended, but <i>not</i> strictly required that
     * <tt>(x.compareTo(y)==0) == (x.equals(y))</tt>.  Generally speaking, any
     * class that implements the <tt>Comparable</tt> interface and violates
     * this condition should clearly indicate this fact.  The recommended
     * language is "Note: this class has a natural ordering that is
     * inconsistent with equals."
     *
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *                            from being compared to this Object.
     */
    public int compareTo(Object o) {
        RunId other = (RunId) o;
        return runId.compareTo(other.runId);
    }
}
