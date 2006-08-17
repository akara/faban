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
 * $Id: AccessController.java,v 1.1 2006/08/17 23:22:44 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.security;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.BenchmarkDescription;

import javax.security.auth.Subject;
import java.security.Principal;
import java.io.File;

/**
 * The access controller that gets checked for accessing Faban resources
 * in secure mode. It represents the access rules for accessing Faban.
 * Due to the ever-changing number of Faban resources,
 * we found the java.security.acl package not very suitable for Faban access
 * control and therefore we implement our own simplified version.
 */
public class AccessController {

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
            case MANAGE : allowed = isManageAllowed(user); break;
            case SUBMIT : allowed = isSubmitAllowed(user); break;
            case VIEW   : allowed = isViewAllowed(user);   break;
            case WRITE  : allowed = isWriteAllowed(user);  break;
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
        if (!Config.SECURITY_ENABLED)
            return true;
        boolean allowed = false;
        switch (perm) {
            case MANAGE : allowed = isManageAllowed(user, resource); break;
            case SUBMIT : allowed = isSubmitAllowed(user, resource); break;
            case VIEW   : allowed = isViewAllowed(user, resource);   break;
            case WRITE  : allowed = isWriteAllowed(user, resource);  break;
        }
        return allowed;
    }

    private static boolean isSubmitAllowed(Subject user) {
        if (user == null)
            return false; // You need to at least login.
        for (Acl acl : Acl.getInstances(Permission.SUBMIT)) {
            // An empty acl for submit means everybody allowed.
            if (acl.isEmpty() || acl.contains(user))
                return true;
        }
        return false;
    }

    private static boolean isSubmitAllowed(Subject user, String resource) {
        if (user == null)
            return false;
        Acl acl = Acl.getInstance(Permission.SUBMIT, resource);
        return acl.isEmpty() || acl.contains(user);
    }

    private static boolean isViewAllowed(Subject user) {
        for (Acl acl : Acl.getInstances(Permission.VIEW)) {
            if (acl.isEmpty()) // Public can view, no login needed.
                return true;
            if (user != null && acl.contains(user))
                return true;
        }
        return false;
    }

    private static boolean isViewAllowed(Subject user, String resource) {
        Acl acl = Acl.getInstance(Permission.VIEW, resource);
        return acl.isEmpty() || (user != null && acl.contains(user));
    }

    /**
     * Checks whether the user has one of the rig managing principals
     * defined in harness.xml.
     * @param user The subject to be checked
     * @return True if the user has rig manage rights
     */
    private static boolean isRigManager(Subject user) {
        for (Principal p : user.getPrincipals()) {
            if (Config.PRINCIPALS.contains(p.getName().trim().toLowerCase()))
                return true;
        }
        return false;
    }

    /* The manage and write permissions are not so straightforward and
     * they are optional. By default, if you have submit permissions you
     * have manage permissions. If you have view permissions you have write
     * permissions. But whenever a manage.acl or write.acl exists and is
     * not empty, they will revoke you of the those permissions unless you're
     * on the acl. If you are a rig-managing principal, you automatically
     * have manage rights across the rig.
     * This is a more fine-grained control that sensitive rigs
     * can put in place. I don't expect too frequent use.
     */
    private static boolean isManageAllowed(Subject user) {
        if (user == null)
            return false;
        if (isRigManager(user))
            return true;
        for (String resource: BenchmarkDescription.getBenchDirMap().keySet()) {
            if (isManageResource(user, resource))
                return true;
        }
        return false;
    }

    private static boolean isManageAllowed(Subject user, String resource) {
        return user != null &&
                (isRigManager(user) || isManageResource(user, resource));
    }

    private static boolean isManageResource(Subject user, String resource) {
        Acl acl = Acl.getInstance(Permission.MANAGE, resource);
        if (acl.isEmpty() && isSubmitAllowed(user, resource))
            return true;
        return acl.contains(user);
    }

    private static boolean isWriteAllowed(Subject user) {
        if (user == null)
            return false;
        for (String resource: new File(Config.OUT_DIR).list()) {
            if (isWriteAllowed(user, resource))
                return true;
        }
        return false;
    }

    private static boolean isWriteAllowed(Subject user, String resource) {
        if (user == null)
            return false;
        Acl acl = Acl.getInstance(Permission.WRITE, resource);
        if (acl.isEmpty() && isViewAllowed(user, resource))
            return true;
        return acl.contains(user);
    }
}
