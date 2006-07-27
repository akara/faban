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
 * $Id: Benchmark.java,v 1.1 2006/07/27 22:34:34 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness;

/**
 * The methods in this interface are the public face of 
 * all UserEnv configured within Faban. New benchmarks
 * that are added should implement this interface.
 *
 * @author Ramesh Ramachandran
 * @see com.sun.faban.harness.engine.GenericBenchmark
 */
public interface Benchmark {

    /**
     * Allows benchmark to validate the configuration file. Note that no
     * remote execution facility is available during validation. Only
     * executions on the master is allowed.
     *
     * @param run The run context for this run.
     * @throws Exception if any error occurred.
     * @see com.sun.faban.harness.RunContext#execute(com.sun.faban.common.Command)
     */
    void validate(RunContext run) throws Exception;

	/**
	 * This method is called to configure the specific benchmark run
	 * Tasks done in this method include reading user parameters,
	 * logging them and initializing various local variables.
	 *
	 * @param run The run context for this run.
     * @throws Exception if any error occurred.
	 */
  	void configure(RunContext run) throws Exception;

	/**
  	 * This method is responsible for starting the benchmark run.
     *
     * @param run The run context for this run.
     * @throws Exception if any error occurred.
  	 */
    void start(RunContext run) throws Exception;

    /**
     * This method is responsible for waiting for all commands started and
     * run all postprocessing needed.
     *
     * @param run The run context for this run.
     * @throws Exception if any error occurred.
     */
    void end(RunContext run) throws Exception;

    /**
     * This method aborts the current benchmark run and is
     * called when a user asks for a run to be killed
     *
     * @param run The run context for this run.
     * @throws Exception if any error occurred.
     */
     void kill(RunContext run) throws Exception;

}
