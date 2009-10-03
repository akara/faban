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
package com.sun.faban.driver.util;

import com.sun.faban.driver.engine.BenchmarkDefinition;
import com.sun.faban.driver.engine.RunInfo;

import java.util.logging.Logger;

/**
 * Utility class for generating Faban driver DDs for the Faban harness.
 * Set the properties benchmark.config to specify the config file and
 * benchmark.ddfile to specify the output file.
 *
 * @author Akara Sucharitakul
 */
public class DDGenerator {

    /**
     * Reads the annotations and generates a deployment descriptor for the
     * Faban harness.
     * @param args Arguments are ignored.
     */
    public static void main(String[] args) {
        try {
            String definingClassName = RunInfo.getDefiningClassName();
            if (definingClassName != null &&
                    definingClassName.trim().length() > 0)
                BenchmarkDefinition.printFabanDD(definingClassName);
            System.exit(0);
        } catch (Exception e) {
            Logger logger = Logger.getLogger(DDGenerator.class.getName());
            logger.severe(e.getMessage());
            System.exit(1);
        }
    }
}
