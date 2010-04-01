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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation interface describes the parameters
 * required when defining a benchmark operation.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BenchmarkOperation {

    /** The name of the operation. */
    String name()     default "";

    /**
     * The maximum limit of the percentiles set in the BenchmarkDriver
     * annotation. Overrides max90th if set. Please specify the limit of 0
     * if there is no limit. At least one limit needs to be specified in a
     * driver or a DefinitionException will be thrown.<br>
     * The unit of measure is dependent on the response time unit specified
     * in the BenchmarkDriver annotation.
     *
     * @see BenchmarkDriver#responseTimeUnit()
     */
    double[] percentileLimits()   default {};

    /**
     * The maximum 90th percentile allowed for this operation.
     * The benchmark run will fail if the 90th percentile of this
     * operation exceeds the given limit.<br>
     * The unit of measure is dependent on the response time unit specified
     * in the BenchmarkDriver annotation.
     * 
     * @see BenchmarkDriver#responseTimeUnit()
     */
    double max90th()    default -1;

    /**
     * Sets the timing mode of this operation to manual or automatic.
     * Manual timing is needed if the benchmark is not a client/server
     * benchmark or the transport still does not support automatic
     * timing.
     */
    Timing timing()   default Timing.AUTO;

    /**
     * Whether this operation gets counted toward the final metric.
     * The default is true. 
     */
    boolean countToMetric() default true;
}
