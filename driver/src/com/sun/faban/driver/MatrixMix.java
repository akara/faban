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
 * $Id: MatrixMix.java,v 1.1 2006/06/29 18:51:32 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * The MatrixMix  annotation interface describes the parameters
 * required when defining a matrix mix. The MatrixMix annotation
 * will need to be used in conjunction with the BenchmarkDriver
 * annotation. Otherwise this annotation will be ignored.<p>
 * Note that the mix in the BenchmarkOperation annotation is ignored
 * when a benchmark is specified to use MatrixMix.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MatrixMix {

    /**
     * The array of operation names.
     * This is both the x and y title of the matrix.
     */
    String[] operations() default {""};

    /**
     * The matrix rows in the mix, sorted according to the operations list.
     * This will create a mix matrix when combined with the contents of the
     * individual rows.
     */
    Row[] mix();

    /** The allowed deviation of the operation selection. */    
    double deviation()    default 2d;
}
