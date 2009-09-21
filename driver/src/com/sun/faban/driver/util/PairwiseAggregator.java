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
package com.sun.faban.driver.util;

import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * The PairwiseAggregator implements the pairwise algorithm discussed by
 * Chan's paper at <a href=
 * "ftp://reports.stanford.edu/pub/cstr/reports/cs/tr/79/773/CS-TR-79-773.pdf"
 * >Updating Formulae and a Pairwise Algorithm for Computing Sample
 * Variances</a>
 *
 * @author akara
 */
public class PairwiseAggregator<T extends PairwiseAggregator.Aggregable<T>> {

    static Logger logger = Logger.getLogger(PairwiseAggregator.class.getName());
    int nodeCount;
    Provider<T> provider;
    int[] extraPairIdxs;
    T[] levelStore;

    /**
     * Creates a PairwiseAggregator.
     * @param nodeCount The number of objects to aggregate
     * @param provider The object provider for aggregation
     */
    public PairwiseAggregator(int nodeCount, Provider<T> provider) {
        this.provider = provider;
        this.nodeCount = nodeCount;
        ArrayList<Integer> extraPairIdxs = new ArrayList<Integer>();
        int idx = 0;
        for (int i = 0; nodeCount > 1; i++) { // Establishing the levels...
            int extra = nodeCount % 2;
            nodeCount /= 2;
            if (extra == 0) {
                extraPairIdxs.add(-1);
            } else {
                if (idx >= nodeCount) {
                    idx = 0;
                }
                extraPairIdxs.add(idx++);
            }
        }
        this.extraPairIdxs = new int[extraPairIdxs.size()];
        for (int i = 0; i < this.extraPairIdxs.length; i++) {
            this.extraPairIdxs[i] = extraPairIdxs.get(i);
        }

        levelStore = (T[]) java.lang.reflect.Array.newInstance(
                provider.getComponentClass(), this.extraPairIdxs.length);
    }

    private void reset() {
        for (int i = 0; i < levelStore.length; i++) {
            if (levelStore[i] != null) {
                provider.recycle(levelStore[i]);
                levelStore[i] = null;
            }
        }
    }


    /**
     * Uses the pairwise algorithm to aggregate all the metrices together.
     * This is to minimize floating point error and get mathematically
     * stable results. It also minimizes memory use for a pairwise
     * algorithm.
     * @return The aggregated metric
     */
    public T collectStats() {
        int[] extraPairIdxs = this.extraPairIdxs.clone();
        T m;
        
        // Debugging/monitoring of the algorithm.
        // The addCount must be nodeCount - 1 in any case.
        int addCount = 0;
        int base = 0;

        reset();

        if (nodeCount == 1) {
            m = provider.getMutableMetrics(0);
            return m;
        } else if (nodeCount % 2 == 1) { // Is it an odd number?
            // Add the odd one on the first addition.
            m = provider.getMutableMetrics(0);
            provider.add(m, 1);
            provider.add(m, 2);
            addCount += 2;
            --extraPairIdxs[0]; // Mark the base oddity as accounted for.
            levelStore[0] = m;
            base = 3;
        }

        // Scan and add each pair.
        for (int i = base; i < nodeCount; i += 2) {
            m = provider.getMutableMetrics(i);
            provider.add(m, i + 1);
            ++addCount;

            // Push the results up in the tree, add as soon as we can add.
            // There will be zero or one entry for each level. As soon
            // as there are two, we need to add. This will ensure minimum
            // life objects being used.
            // One exception, if there are odd number of objects
            // at this level, they should be added at times specified by
            // the extraPairIdxs to spread the extra one around and
            // minimize impact on numeric stability.
            T m0 = m;
            for (int j = 0;; j++) {
                T m1 = levelStore[j];
                if (m1 == null) {
                    levelStore[j] = m0;
                    break;
                } else {
                    --extraPairIdxs[j + 1];
                    m1.add(m0);
                    ++addCount;
                    provider.recycle(m0); // Done using this, return to pool.
                    if (extraPairIdxs[j + 1] == -1) { // It's time to
                        break;   // do the extra one. Don't push up level.
                    } else {
                        levelStore[j] = null;
                    }
                    m0 = m1;
                }
            }
        }

        if (addCount + 1 != nodeCount)
            logger.warning("From " + nodeCount + "nodes, added " + addCount +
                    " times. Should be " + (nodeCount - 1) + " times.");
        return levelStore[levelStore.length - 1];
    }

    /**
     * Provider interface to provide objects to aggregate.
     */
    public static interface Provider<T extends Aggregable<T>> {

        /**
         * Fetches an object that can be used as the aggregation origin.
         * @param idx The object index
         * @return The mutable object
         */
        public T getMutableMetrics(int idx);

        /**
         * Adds to a mutable instance.
         * @param instance The mutable instance
         * @param idx The object index to add
         */
        public void add(T instance, int idx);

        /**
         * Obtains the type of the component this provider provides.
         * @return The class object representing the type.
         */
        public Class<T> getComponentClass();

        /**
         * Recycles an object after use. The implementation my pool recycled
         * objects for reuse or do nothing, just let the garbage collector
         * handle it.
         * @param metrics The object to recycle
         */
        public void recycle(T metrics);
    }

    /**
     * Interface to an aggregable object.
     */
    public static interface Aggregable<T> {

        /**
         * The add method for aggregation, adding the metrics to this object.
         * @param metrics The metrics to add
         */
        public void add(T metrics);
    }
}
