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
import com.sun.faban.harness.common.BenchmarkDescription;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a wrapper which checks if the benchmark class is annotation based
 * or interface based.
 * @author Sheetal Patil, Sun Microsystems.
 */
public abstract class BenchmarkWrapper {
    
    private static Logger logger = 
            Logger.getLogger(BenchmarkWrapper.class.getName());

    static BenchmarkWrapper getInstance(BenchmarkDescription desc) {
        Class benchmarkClass = null;
        DeployImageClassLoader loader = DeployImageClassLoader.getInstance(
                "benchmarks", desc.shortName,
                BenchmarkWrapper.class.getClassLoader());
        if (loader != null)
            try {
                benchmarkClass = Class.forName(desc.benchmarkClass,
                        true, loader);


            } catch (ClassNotFoundException e) {
                logger.warning("Cannot find class " + desc.benchmarkClass +
                        " within the Faban Harness.");
//            } catch (Exception e) {
//                logger.log(Level.SEVERE, "Error instantiating " +
//                        desc.benchmarkClass + '.', e);
            }

        // In some cases, the benchmark class is in the Faban package itself.
        if (benchmarkClass == null) {
            logger.info("Trying reading class " + desc.benchmarkClass +
                        " from Faban Harness.");
            try {
                benchmarkClass = Class.forName(desc.benchmarkClass);
            } catch (ClassNotFoundException e) {
                logger.severe("Cannot find class " + desc.benchmarkClass +
                              " within the Faban Harness.");
//            } catch (Exception e) {
//                logger.log(Level.SEVERE, "Error instantiating " +
//                           desc.benchmarkClass + '.', e);
            }
        }
        BenchmarkWrapper wrapper = null;
        try {
        if (Benchmark.class.isAssignableFrom(benchmarkClass)) {
            wrapper = new InterfaceBenchmarkWrapper(benchmarkClass);
        } else {
            wrapper = new AnnotationBenchmarkWrapper(benchmarkClass);
        }
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error creating benchmark wrapper", e);
        }
        return wrapper;
    }
    /**
     * Allows benchmark to validate the configuration file. Note that no
     * execution facility is available during validation. This method is just
     * for validation and modifications of the run configuration.
     *
     * @throws Exception if any error occurred.
     * @see com.sun.faban.harness.RunContext#exec(com.sun.faban.common.Command)
     */
    abstract void validate() throws Exception;

	/**
	 * This method is called to configure the specific benchmark run
	 * Tasks done in this method include reading user parameters,
	 * logging them and initializing various local variables.
     * Configure has access to all remote facilities. Services are
     * not yet started.
	 *
     * @throws Exception if any error occurred.
	 */
  	abstract void configure() throws Exception;

	/**
     * This method if called for preparation of the benchmark run.
     * The preRun is called after all services are available. So
     * it can be used to load/reload data into a database, for example.
     * @throws Exception if any error occurred.
     */
    abstract void preRun() throws Exception;

    /**
  	 * This method is responsible for starting the benchmark run.
     *
     * @throws Exception if any error occurred.
  	 */
    abstract void start() throws Exception;

    /**
     * This method is responsible for waiting for all commands started and
     * run all postprocessing needed.
     *
     * @throws Exception if any error occurred.
     */
    abstract void end() throws Exception;

    /**
     * This method is responsible for running
     * all postprocessing needed.
     *
     * @throws Exception if any error occurred.
     */
    abstract void postRun() throws Exception;

    /**
     * This method aborts the current benchmark run and is
     * called when a user asks for a run to be killed.
     *
     * @throws Exception if any error occurred.
     */
    abstract void kill() throws Exception;

}
