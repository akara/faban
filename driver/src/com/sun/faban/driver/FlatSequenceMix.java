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
 * This annotation interface describes the parameters
 * required when defining a flat sequence mix. The FlatSequenceMix annotation
 * will need to be used in conjunction with the BenchmarkDriver
 * annotation. Otherwise this annotation will be ignored.<p>
 * Note that the mix in the BenchmarkOperation annotation is ignored
 * when a benchmark is specified to use FlatSequenceMix.
 * @author Scott Oaks
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FlatSequenceMix {

    /**
     * An array of OperationSequence annotations describing
     * each annotation sequence.
     */
    OperationSequence[] sequences();

    /**
     * The allowed deviation froom the specified mix, in percent.
     */
    double deviation()    default 2d;

    /**
     *  The actual mix ratio for selecting the sequence. 
     */
    double[] mix();
}
