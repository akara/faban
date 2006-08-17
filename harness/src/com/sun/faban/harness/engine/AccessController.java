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
 * $Id: AccessController.java,v 1.4 2006/08/17 17:30:14 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.common.Config;

import javax.security.auth.Subject;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.Principal;

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

    static class Acl {
        static Logger logger = Logger.getLogger(Acl.class.getName());
        File aclFile;
        long lastModified = 0l;
        HashSet<String> entries = new HashSet<String>();

        Acl(String pathName) {
            aclFile = new File(pathName);
        }

        public void refresh() {
            long modified;
            if (aclFile.isFile() &&
               (modified = aclFile.lastModified()) > lastModified) {
                try {
                    BufferedReader reader = new BufferedReader(
                                            new FileReader(aclFile));
                    entries.clear();
                    String entry;
                    while ((entry = reader.readLine()) != null) {
                        entry = entry.trim();
                        if (entry.length() > 0)
                            entries.add(entry);
                    }
                    reader.close();
                    lastModified = modified;
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error reading acl at " +
                            aclFile.getAbsolutePath(), e);
                }
            }
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }

        public boolean contains(Subject user) {
            Set<Principal> principals = user.getPrincipals();
            for (Principal principal : principals) {
                String name = principal.getName();
                if (entries.contains(name))
                    return true;
            }
            return false;
        }
    }
}
