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
 * $Id: AccessController.java,v 1.3 2006/08/17 06:29:52 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.common.Config;

import javax.security.auth.Subject;

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
     * Checks whether the user has the given permission on one or more
     * resources.
     * @param perm The permission to check
     * @param user The user
     * @return True if action is permitted, false otherwise
     */
    public static boolean isAllowed(Permission perm, Subject user) {
        if (!Config.SECURITY_ENABLED)
            return true;
        boolean allowed = false;
        switch (perm) {
            case MANAGE :
            case SUBMIT : if (user != null) allowed = true; break;
            case VIEW   : allowed = true; break;
            case WRITE  : break;
        }
        return allowed;
    }

    /**
     * Checks whether the user has the given permission on the given resource.
     * @param perm The permission to check
     * @param user The user
     * @param resource The resource to check the permission against
     * @return True if action is permitted, false otherwise
     */
    public static boolean isAllowed(Permission perm, Subject user,
                                    String resource) {
        return isAllowed(perm, user);
    }
}
