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

import com.sun.faban.harness.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
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
        benchmark = benchmarkClass.newInstance();
        Method[] methods = benchmarkClass.getMethods();
        for (Method method : methods) {
            // Check annotation.
            if (method.getAnnotation(Validate.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (validateMethod == null) {
                    validateMethod = method;
                } else {
                    logger.severe("Error: Multiple @Validate methods.");
                    //throw new Error ("Multiple @Validate methods.");
                }
            }
            if (method.getAnnotation(Configure.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (configureMethod == null) {
                    configureMethod = method;
                } else {
                    logger.severe("Error: Multiple @Configure methods.");
                    //throw new Error ("Multiple @Configure methods.");
                }
            }
            if (method.getAnnotation(PreRun.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (preRunMethod == null) {
                    preRunMethod = method;
                } else {
                    logger.severe("Error: Multiple @PreRun methods.");
                    //throw new Error ("Multiple @PostRun methods.");
                }
            }
            if (method.getAnnotation(StartRun.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (startMethod == null) {
                    startMethod = method;
                } else {
                    logger.severe("Error: Multiple @Start methods.");
                    //throw new Error ("Multiple @Start methods.");
                }
            }
            if (method.getAnnotation(EndRun.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (endMethod == null) {
                    endMethod = method;
                } else {
                    logger.severe("Error: Multiple @End methods.");
                    //throw new Error ("Multiple @End methods.");
                }
            }
            if (method.getAnnotation(PostRun.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (postRunMethod == null) {
                    postRunMethod = method;
                } else {
                    logger.severe("Error: Multiple @PostRun methods.");
                    //throw new Error ("Multiple @PostRun methods.");
                }
            }
            if (method.getAnnotation(KillRun.class) != null) {
                if (!conformsToSpec(method))
                    continue;
                if (killMethod == null) {
                    killMethod = method;
                } else {
                    logger.severe("Error: Multiple @Kill methods.");
                    //throw new Error ("Multiple @Kill methods.");
                }
            }
        }
    }

    private boolean conformsToSpec(Method method) {
            boolean retval= true;
            // Is it a noarg method?
            if (method.getParameterTypes().length > 0) {
                logger.warning("Method has arguments");
                retval = false;
            }
            // Is it a void method?
            if (!method.getReturnType().equals(Void.TYPE)) {
                logger.warning("Method is not of type Void");
                retval = false;
            }
            return retval;
    }
    private void throwSourceException(InvocationTargetException e)
                throws Exception {
            Throwable t = e.getCause();
            if (t instanceof Exception) {
                logger.log(Level.WARNING, t.getMessage(), t);
                throw (Exception) t;
            } else {
                throw e;
            }
    }

    /**
     * Invokes a benchmark's method annotated by @Validate.
     * @throws java.lang.Exception
     */
    void validate() throws Exception {
        if (validateMethod != null){
            try {
                validateMethod.invoke(benchmark,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            }
        }
    }

    /**
     * Invokes a benchmark's method annotated by @Configure.
     * @throws java.lang.Exception
     */
    void configure() throws Exception {
        if (configureMethod != null){
            try {
                configureMethod.invoke(benchmark,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            }
        }
    }

    /**
     * Invokes a benchmark's method annotated by @PreRun.
     * @throws java.lang.Exception
     */
    void preRun() throws Exception {
        if (configureMethod != null){
            try {
                preRunMethod.invoke(benchmark,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            }
        }
    }

    /**
     * Invokes a benchmark's method annotated by @StartRun.
     * @throws java.lang.Exception
     */
    void start() throws Exception {
        if (startMethod != null){
            try {
                startMethod.invoke(benchmark,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            }
        }
    }

    /**
     * Invokes a benchmark's method annotated by @EndRun.
     * @throws java.lang.Exception
     */
    void end() throws Exception {
        if (endMethod != null){
            try {
                endMethod.invoke(benchmark,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            }
        }
    }

    /**
     * Invokes a benchmark's method annotated by @PostRun.
     * @throws java.lang.Exception
     */
    void postRun() throws Exception {
        if (postRunMethod != null){
            try {
                postRunMethod.invoke(benchmark,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            }
        }
    }

    /**
     * Invokes a benchmark's method annotated by @KillRun.
     * @throws java.lang.Exception
     */
    void kill() throws Exception {
        if (killMethod != null){
            try {
                killMethod.invoke(benchmark,new Object[] {});
            } catch (InvocationTargetException e) {
                throwSourceException(e);
            }
        }
    }

}
