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
package com.sun.faban.driver.engine;

import com.sun.faban.driver.util.Random;
import com.sun.faban.driver.DefinitionException;
import com.sun.faban.driver.CycleType;
import java.lang.annotation.Annotation;
import java.io.IOException;

/**
 * Implements the fixed time setting for an operation.
 *
 * @author Akara Sucharitakul
 */
public class FixedTime extends Cycle {

	private static final long serialVersionUID = 1L;
    
	long cycleTime;

    /**
     * Initializes this cycle according to the annotation.
     * @param a The annotation
     * @throws DefinitionException If there is an error in the annotation
     */
	public void init(Annotation a) throws DefinitionException {
        com.sun.faban.driver.FixedTime cycleDef =
                (com.sun.faban.driver.FixedTime) a;
        cycleType = cycleDef.cycleType();
        cycleDeviation = cycleDef.cycleDeviation();
        cycleTime = cycleDef.cycleTime() * 1000000l;

        // Now check parameters for validity.
        if (cycleTime == 0 && cycleType == CycleType.CYCLETIME) {
			throw new DefinitionException("@FixedTime cycle time " +
                    "cannot be 0, use think time instead.");
		}

    }

    /**
     * Returns a delay time for the thread. The initial delay time
     * returned is randomly selected between 0 and the set cycle time.
     * The subsequent times are exactly the cycle time. This is to
     * prevent thundering of operations when using @FixedTime. 
     *
     * @param random The random number generator used
     * @return The delay time
     */
	public long getDelay(Random random) {
        return cycleTime;
    }

    /**
     * Provides the maximum value to be represented inside a histogram.
     *
     * @return The max reasonable delay to be presented in the output histogram.
     */
	public double getHistogramMax() {
        if (cycleTime > 0) {
			return cycleTime * 2;
		}

        // We know it takes very little time to prepare the data to submit.
        // This case can only happen for think time. Giving a histogram
        // max of 2 seconds for data prep should be enough.
        return 2d;
    }

    // How to load myself from a stream
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}
