/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://faban.dev.java.net/public/CDDLv1.0.html or
 * install_dir/license.txt
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
 * InitialDelay tells the driver to wait for a random amount of time up to
 * the specified max before launching the first operation. This is most useful
 * in fixed-cycle drivers (all operations use @FixedTime) to distribute the
 * arrival of the operations across time. Otherwise we will see a
 * thundering-herd effect for each operation which renders the simulation far
 * less useful and realistic. The random time is selected from a uniform
 * distribution.
 *
 * @author Akara Sucharitakul
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface InitialDelay {

    /**
     * The maximum initial delay for any thread, in milliseconds.
     * The actual delay is selected from a uniform distribution between 0
     * and the specified max.
     */
    int max() default 0;
}
