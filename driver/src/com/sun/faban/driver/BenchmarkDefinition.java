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
package com.sun.faban.driver;

import java.lang.annotation.*;

/**
 * The BenchmarkDefinition annotation defines the benchmark. Only one
 * driver will need to provide a benchmark definition, this is usually the
 * first driver and the defining driver of the benchmark. This is where
 * global information about the benchmark is defined.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BenchmarkDefinition {
    /** The name of this benchmark. */
    String name();

    /** The version of this benchmark. */
    String version();

    /**
     * The driver classes. Defaults to the defining driver class which is
     * the class providing the BenchmarkDefinition. The defining driver
     * does not need to be listed and is automatically treated as the
     * first driver. If it is in the list of driver classes, it will
     * assume the number according to it's index in the array.
     */
	Class<?>[] drivers() default { Object.class };

    /**
     * Specifies whether this benchmark uses time or cycles to control the
     * phases in the benchmark run.
     */
    RunControl runControl() default RunControl.TIME;

    /** The name of the resulting metric. */
    String metric()         default "ops/sec";

    /** The name of the benchmark scale. */
    String scaleName()      default "Scale";

    /** The unit of the benchmark scale. */ 
    String scaleUnit()      default "";

    /**
     * The maximum run time of the benchmark, in hours. Can be overridden
     * by the same parameter in the @BenchmarkDriver annotation.
     * Defaults to 6 hrs. This is only used if runControl is CYCLES.
     * Otherwise the benchmark configuration is a better indicator.
     */
    int maxRunTime()        default -1;

    /**
     * If set to true, this benchmark allows pass/fail checks to be done
     * against config parameters in the config file, if they exist. In other
     * words, the config file precedes the annotations for pass/fail check
     * criteria. Default is false which means only the annotations are
     * followed for such checks.
     */
    boolean configPrecedence() default false;
}
