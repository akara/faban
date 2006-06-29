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
 * $Id: CycleDistribution.java,v 1.1 2006/06/29 18:51:32 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver;

/**
 * CycleDistribution determines the distribution used for obtaining
 * individual think/cycle times. This is used for the setting the
 * cycleDist annotation.
 */
public enum CycleDistribution {

    /**
     * Always use the exact cycleTime set in the BenchmarkDriver annotation.
     */
    FIXED,

    /**
     * Individual cycle time is established from a uniform distribution
     * between delayMin and delayMax. The delayMin is derived as follows:<p>
     * delayMin = cycleTime - (delayMax - cycleTime)<p>
     * This equals to 2 * cycleTime - delayMax.
     * delayMin and delayMax are defined in the BenchmarkDriver annotation.
     */
    UNIFORM,

    /**
     * Individual cycle time is established from a negative exponential
     * distribution using cycleTime set in the BenchmarkDriver annotation as
     * the mean time and delayMax being the cutoff time. Min is always 0.
     */
    NEGATIVE_EXPONENTIAL
}
