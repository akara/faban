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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: Benchmark.java,v 1.1 2006/06/29 18:51:42 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;
import com.sun.faban.harness.common.Run;

/**
 * The methods in this interface are the public face of 
 * all UserEnv configured within Faban. New benchmarks
 * that are added should implement this interface.
 *
 * @author Ramesh Ramachandran
 * @see GenericBenchmark
 */
public interface Benchmark {

	/**
	 * This method is called to configure the specific benchmark run
	 * Tasks done in this method include reading user parameters,
	 * logging them and initializing various local variables.
	 *
	 * @param r Run object that identifies this run
	 * @param par ParamRepository for this run
	 * @return true if configuration was successful, 
	 *         false otherwise (abort run)
	 *
	 */
  	boolean configure(Run r, ParamRepository par) throws Exception;

	/**
	 * This method aborts the current benchmark run and is
	 * called when a user asks for a run to be killed
	 */
  	void kill();

	/**
 	* This method is responsible for starting the benchmark run
 	*/
    void start() throws Exception;
}
