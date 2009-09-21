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
package com.sun.faban.harness;

/**
 * The methods in this interface are the public face of 
 * all UserEnv configured within Faban. New benchmarks
 * that are added should implement this interface.
 *
 * @author Ramesh Ramachandran
 * @see com.sun.faban.harness.engine.GenericBenchmark
 * @deprecated Please use the benchmark annotations going forward
 */
@Deprecated public interface Benchmark {

    /**
     * Allows benchmark to validate the configuration file. Note that no
     * execution facility is available during validation. This method is just
     * for validation and modifications of the run configuration.
     *
     * @throws Exception if any error occurred.
     * @see com.sun.faban.harness.RunContext#exec(com.sun.faban.common.Command)
     */
    void validate() throws Exception;

	/**
	 * This method is called to configure the specific benchmark run
	 * Tasks done in this method include reading user parameters,
	 * logging them and initializing various local variables.
	 *
     * @throws Exception if any error occurred.
	 */
  	void configure() throws Exception;

	/**
  	 * This method is responsible for starting the benchmark run.
     *
     * @throws Exception if any error occurred.
  	 */
    void start() throws Exception;

    /**
     * This method is responsible for waiting for all commands started and
     * run all postprocessing needed.
     *
     * @throws Exception if any error occurred.
     */
    void end() throws Exception;

    /**
     * This method aborts the current benchmark run and is
     * called when a user asks for a run to be killed.
     *
     * @throws Exception if any error occurred.
     */
     void kill() throws Exception;

}
