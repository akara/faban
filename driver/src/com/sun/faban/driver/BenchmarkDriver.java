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
import java.util.concurrent.TimeUnit;

/**
 * This annotation interface describes the parameters
 * required when defining a benchmark driver.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BenchmarkDriver {

    /** The driver's name, can be ignored for a single-driver benchmark. */
    String name()           default "";

    /** The default metric is "ops/sec". Otherwise specify. */
    String metric()         default "ops/sec";

    /** Unit of operation, in plural. */
    String opsUnit()        default "operations";

    /**
     * The number of threads this driver should launch for each
     * benchmark scale. The actual number of threads launched for this
     * driver is a round(threadsPerScale x scale).
     */
    float threadPerScale()    default 1f;

    /**
     * The percentiles to measure against. Overrides 90th% setting
     * (old style). The percentiles must be numeric strings representing
     * the percentiles of interest. The number must be greater than 0 and less
     * than 100. Decimal digits can be used. The number may optionally have a
     * suffix of 'th', 'st', 'nd', or 'rd' which will be used in the reporting.
     * It can also end with the '%' sign. The following are valid percentiles.
     * <ul>
     *   <li>99</li>
     *   <li>99th</li>
     *   <li>99th%</li>
     *   <li>99.99</li>
     *   <li>99.99th</li>
     *   <li>99.99th%</li>
     * </ul>
     */
    String[] percentiles()       default {};

    /**
     * The time unit used for reporting response times.
     */
    TimeUnit responseTimeUnit() default TimeUnit.SECONDS;
}
