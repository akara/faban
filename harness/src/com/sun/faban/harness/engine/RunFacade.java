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
package com.sun.faban.harness.engine;

import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;

import java.io.File;

/**
 * The RunFacade provides a benchmark with callbacks to get run information.
 *
 * @author Akara Sucharitakul
 */
public class RunFacade {

    private static RunFacade instance;
    private Run run;
    private ParamRepository param;

    private RunFacade(Run run, ParamRepository param) {
        this.run = run;
        this.param = param;
    }

    static RunFacade newInstance(Run run, ParamRepository param) {
        instance = new RunFacade(run, param);
        return instance;
    }

    /**
     * Returns instance of RunFacade.
     * @return RunFacade
     */
    public static RunFacade getInstance() {
        return instance;
    }

    /**
     * Resets the instance.
     */
    static void clearInstance() {
        instance = null;
    }

    /**
     * Obtains the benchmark deployment directory.
     * @return The benchmark deployment directory
     */
    public String getBenchmarkDir() {
        return Config.BENCHMARK_DIR + run.getBenchmarkName() + File.separator;
    }

    /**
     * Obtains the output directory for this run.
     * @return The run output directory
     */
    public String getOutDir() {
        return run.getOutDir();
    }

    /**
     * Obtains the param repository for this run.
     * @return The param repository
     */
    public ParamRepository getParamRepository() {
        return param;
    }

    /**
     * Obtains the parameter/config file path for this run.
     * @return The parameter file path
     */
    public String getParamFile() {
        return run.getParamFile();
    }

    /**
     * Obtains the sequence part of the run id.
     * @return The run id
     */
    public String getRunSeq() {
        return run.getRunSeq();
    }

    /**
     * Obtains the run id in the form benchmark.id.
     * @return The run id
     */
    public String getRunId() {
        return run.getRunId();
    }
}
