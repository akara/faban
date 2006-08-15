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
 * $Id: AccessController.java,v 1.1 2006/08/15 02:39:02 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.common.Config;

/**
 * The access controller that gets checked for accessing Faban resources
 * in secure mode. Due to the ever-changing number of Faban resources,
 * we found the java.security.acl package not very suitable for Faban access
 * control and therefore we implement our own simplified version.
 */
public class AccessController {

    /* The current implementation does not check authorization.
     * It allows any logged in user to submit and any user to read.
     * TODO: We need to implement the authorization checks.
     */

    /**
     * Checks whether the user is allowed to submit runs for at least one
     * deployed benchmark.
     * @param user The logged in user
     * @return True if submissions are allowed, false otherwise
     */
    public static boolean submitAllowed(String user) {
        if (Config.SECURITY_ENABLED) {
            if (user != null)
                return true;
            else
                return false;
        } else {
            return true;
        }
    }

    /**
     * Checks whether the user is allowed to submit runs for the given
     * benchmark.
     * @param user The logged in user
     * @param benchShortName The benchmark's short name
     * @return True if submissions are allowed, false otherwise
     */
    public static boolean submitAllowed(String user, String benchShortName) {
        return submitAllowed(user);
    }


    /**
     * Checks whether the user is allowed to add comments to a benchmark run.
     * @param user The logged in user
     * @param runId The runId to check the permission for
     * @return True if the user is allowed to write comments to the run.
     */
    public static boolean writeAllowed(String user, String runId) {
        return false;
    }


    /**
     * Checks whtehr the user is allowed to view the benchmark run results.
     * @param user The logged in user
     * @param runId The runId to check the permission for
     * @return True if the user is allowed to view the benchmark run results.
     */
    public boolean readAllowed(String user, String runId) {
        return true;
    }
}
