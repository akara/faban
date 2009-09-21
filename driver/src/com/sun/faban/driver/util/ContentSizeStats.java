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
 * $Id$
 *
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.util;

import com.sun.faban.driver.CustomMetrics;
import com.sun.faban.driver.Result;

/**
 * The ContentSizeStats collects the content size metrics for the FabanHTTPBench
 * or othe standard Http drivers.
 *
 * @author Akara Sucharitakul
 */
public class ContentSizeStats implements CustomMetrics {

    /** The sum of content sizes. */
    public long[] sumContentSize;

    /**
     * Constructs the ContentSizeStats for the given number of operations.
     * @param opCount The number of operations.
     */
    public ContentSizeStats(int opCount) {
        sumContentSize = new long[opCount];
    }

    /**
     * Aggregates the ContentSizeStats from another source or thread with
     * the current one.
     * @param other The stats from another source
     */
    public void add(CustomMetrics other) {
        ContentSizeStats o = (ContentSizeStats) other;
        for (int i = 0; i < sumContentSize.length; i++)
            sumContentSize[i] += o.sumContentSize[i];
    }

    /**
     * The metrics need to be cloneable and not throw any exceptions.
     * @return The ContentSizeStats clone
     */
    public Object clone() {
        ContentSizeStats o = new ContentSizeStats(sumContentSize.length);
        for (int i = 0; i < sumContentSize.length; i++)
            o.sumContentSize[i] = sumContentSize[i];
        return o;
    }

    /**
     * Obtains the results of this ContentSizeStats as to be reported.
     * @return The result elements for each metric.
     */
    public Element[] getResults() {
        Element[] e = new Element[sumContentSize.length];
        Result r = Result.getInstance();
        String[] opsNames = r.getOpsNames();
        int[] counts = r.getOpsCountSteady();
        for (int i = 0; i < e.length; i++) {
            e[i] = new Element();
            e[i].description = "Average Content Size for " + opsNames[i];
            if (counts[i] != 0)
                e[i].result = String.format("%.2f",
                                sumContentSize[i] / (double) counts[i]);
            else
                e[i].result = "N/A";
        }
        return e;
    }
}
