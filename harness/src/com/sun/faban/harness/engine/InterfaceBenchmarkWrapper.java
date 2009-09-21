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
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.Benchmark;

/**
 * Wrapper class for benchmarks implemented using the now deprecated
 * Benchmark interface.
 * @author Sheetal Patil, Sun Microsystems.
 */
public class InterfaceBenchmarkWrapper extends BenchmarkWrapper {

    Benchmark benchmark;

    InterfaceBenchmarkWrapper(Class benchmarkClass) 
            throws InstantiationException, IllegalAccessException {
        Class<Benchmark> c = benchmarkClass.asSubclass(Benchmark.class);
        benchmark = c.newInstance();
    }

    /**
     * Invokes validate on the benchmark class.
     * @throws Exception Indicating an error thrown by the benchmark class
     */
    @Override
    void validate() throws Exception {
        benchmark.validate();
    }

    /**
     * Invokes configure on the benchmark class.
     * @throws Exception Indicating an error thrown by the benchmark class
     */
    @Override
    void configure() throws Exception {
       benchmark.configure();
    }

    /**
     * Noop for the interface wrapper.
     * This method if called for preparation of the benchmark run.
     * The preRun is called after all services are available. So
     * it can be used to load/reload data into a database, for example.
     *
     * @throws Exception if any error occurred.
     */
    @Override
    void preRun() throws Exception {
        // Noop.
    }

    /**
     * Invokes start on the benchmark class.
     * @throws Exception Indicating an error thrown by the benchmark class
     */
    @Override
    void start() throws Exception {
        benchmark.start();
    }

    /**
     * Invokes end on the benchmark class.
     * @throws Exception Indicating an error thrown by the benchmark class
     */
    @Override
    void end() throws Exception {
        benchmark.end();
    }

    /**
     * Noop. The benchmark interface does not have a postRun method.
     * To support postRun, switch to the annotation-based benchmark class. 
     */
    @Override
    void postRun() {
        // Noop.
    }

    /**
     * Invokes kill on the benchmark class.
     * @throws Exception Indicating an error thrown by the benchmark class
     */
    @Override
    void kill() throws Exception {
        benchmark.kill();
    }
}
