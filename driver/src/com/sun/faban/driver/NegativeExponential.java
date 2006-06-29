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
 * $Id: NegativeExponential.java,v 1.2 2006/06/29 19:38:36 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * NegativeExponential represents a negative exponential distribution
 * for think or cycle time distribution.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface NegativeExponential {

    /** The type of cycle to be used. This is either think or cycle time. */
    CycleType cycleType() default CycleType.CYCLETIME;

    /** The mean cycle or think time in milliseconds. */
    int cycleMean() default 1000;

    /** The max cycle or think time in milliseconds, defaults to 5xcycleMean. */
    int cycleMax() default -1;

    /** The allowed deviation from the targeted time, in %. */
    double cycleDeviation();
}
