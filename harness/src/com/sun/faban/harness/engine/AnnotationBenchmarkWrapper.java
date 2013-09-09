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

import com.sun.faban.harness.*;
import com.sun.faban.harness.util.Invoker;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * This is an annotation based benchmark wrapper class.
 *
 * @author Sheetal Patil, Sun Microsystems.
 */
public class AnnotationBenchmarkWrapper extends BenchmarkWrapper {

    private static Logger logger =
            Logger.getLogger(AnnotationBenchmarkWrapper.class.getName());

    Object benchmark;
    Method validateMethod;
    Method configureMethod;
    Method preRunMethod;
    Method startMethod;
    Method endMethod;
    Method postRunMethod;
    Method killMethod;

    AnnotationBenchmarkWrapper(Class benchmarkClass) throws Exception {
        Method[] methods = benchmarkClass.getMethods();
        String benchmarkClassName = benchmarkClass.getName();
        for (Method method : methods) {
            // Check annotation.
            if (method.getAnnotation(Validate.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (validateMethod == null) {
                    validateMethod = method;
                } else {
                    logger.severe("Error: Multiple @Validate methods in "+benchmarkClassName);
                    //throw new Error ("Multiple @Validate methods.");
                }
            }
            if (method.getAnnotation(Configure.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (configureMethod == null) {
                    configureMethod = method;
                } else {
                    logger.severe("Error: Multiple @Configure methods in "+benchmarkClassName);
                    //throw new Error ("Multiple @Configure methods.");
                }
            }
            if (method.getAnnotation(PreRun.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (preRunMethod == null) {
                    preRunMethod = method;
                } else {
                    logger.severe("Error: Multiple @PreRun methods in "+benchmarkClassName);
                    //throw new Error ("Multiple @PostRun methods.");
                }
            }
            if (method.getAnnotation(StartRun.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (startMethod == null) {
                    startMethod = method;
                } else {
                    logger.severe("Error: Multiple @Start methods in "+benchmarkClassName);
                    //throw new Error ("Multiple @Start methods.");
                }
            }
            if (method.getAnnotation(EndRun.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (endMethod == null) {
                    endMethod = method;
                } else {
                    logger.severe("Error: Multiple @End methods in "+benchmarkClassName);
                    //throw new Error ("Multiple @End methods.");
                }
            }
            if (method.getAnnotation(PostRun.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (postRunMethod == null) {
                    postRunMethod = method;
                } else {
                    logger.severe("Error: Multiple @PostRun methods in "+benchmarkClassName);
                    //throw new Error ("Multiple @PostRun methods.");
                }
            }
            if (method.getAnnotation(KillRun.class) != null) {
                if (!Invoker.isVoidNoArg(method))
                    continue;
                if (killMethod == null) {
                    killMethod = method;
                } else {
                    logger.severe("Error: Multiple @Kill methods in "+benchmarkClassName);
                    //throw new Error ("Multiple @Kill methods.");
                }
            }
            try {
                benchmark = benchmarkClass.newInstance();
            } catch (InstantiationException e) {
                Invoker.throwCauseException(e);
            }
        }
    }

    /**
     * Invokes a benchmark's method annotated by @Validate.
     * @throws java.lang.Exception
     */
    void validate() throws Exception {
        Invoker.invoke(benchmark, validateMethod);
    }

    /**
     * Invokes a benchmark's method annotated by @Configure.
     * @throws java.lang.Exception
     */
    void configure() throws Exception {
        Invoker.invoke(benchmark, configureMethod);
    }

    /**
     * Invokes a benchmark's method annotated by @PreRun.
     * @throws java.lang.Exception
     */
    void preRun() throws Exception {
        Invoker.invoke(benchmark, preRunMethod);
    }

    /**
     * Invokes a benchmark's method annotated by @StartRun.
     * @throws java.lang.Exception
     */
    void start() throws Exception {
        Invoker.invoke(benchmark, startMethod);
    }

    /**
     * Invokes a benchmark's method annotated by @EndRun.
     * @throws java.lang.Exception
     */
    void end() throws Exception {
        Invoker.invoke(benchmark, endMethod);
    }

    /**
     * Invokes a benchmark's method annotated by @PostRun.
     * @throws java.lang.Exception
     */
    void postRun() throws Exception {
        Invoker.invoke(benchmark, postRunMethod);
    }

    /**
     * Invokes a benchmark's method annotated by @KillRun.
     * @throws java.lang.Exception
     */
    void kill() throws Exception {
        Invoker.invoke(benchmark, killMethod);
    }
}
