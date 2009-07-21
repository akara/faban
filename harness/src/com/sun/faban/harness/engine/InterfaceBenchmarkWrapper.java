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
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.Benchmark;

/**
 * This is an interface based benchmark wrapper class.
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
     *
     * @throws java.lang.Exception
     */
    @Override
    void validate() throws Exception {
        benchmark.validate();
    }

    /**
     *
     * @throws java.lang.Exception
     */
    @Override
    void configure() throws Exception {
       benchmark.configure();
    }

    /**
     *
     * @throws java.lang.Exception
     */
    @Override
    void start() throws Exception {
        benchmark.start();
    }

    /**
     *
     * @throws java.lang.Exception
     */
    @Override
    void end() throws Exception {
        benchmark.end();
    }

    /**
     *
     * @throws java.lang.Exception
     */
    @Override
    void postRun() throws Exception {
        // Noop.
    }

    /**
     *
     * @throws java.lang.Exception
     */
    @Override
    void kill() throws Exception {
        benchmark.kill();
    }
}
