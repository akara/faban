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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Specifies simulated operations performed in the background without user
 * initiation. For example, some clients like POP3 periodically download
 * new mail without the user initiating the download. There are certain
 * restrictions imposed on background operations:
 * <ul>
 * <li>The sequence of background operation execution is fixed</li>
 * <li>The cyclet time, if explicitly specified for background operations must
 *     be FixedTime</li>
 *</ul>
 * However, if an operation is used solely for background simulation but
 * does not specify the timings for the operation, it will use the cycles
 * specified for the operation or the whole driver which can be of any valid
 * cycle type.
 *
 * @author Akara Sucharitakul
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Background {

    /**
     * The list of operation names to be run in the background. The same
     * operation may be run both in the foreground and in the background.
     */
    String[] operations();

    /**
     * The timing cycles for the specified operations. It can only be FixedTime.
     * If only one FixedTime is specified, it will propagate to all the
     * background operations. Otherwise the number of FixedTime elements must
     * match the number of operations. Leaving timings unspecified allows the
     * background operations to use the cycles otherwise applicable to the
     * operation.
     */
    FixedTime[] timings() default {};

    /**
     * Defines the initial delay maximum of the background.
     */
    InitialDelay initialDelay() default @InitialDelay(max=0);
}
