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
 * $Id: Acl.java,v 1.1 2006/08/17 23:22:44 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.security;

import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;

import javax.security.auth.Subject;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Set;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.Principal;

/**
 * Acl is the encapsulation and caching of access control lists. These
 * are represented as *.acl files in various Faban directories. The class
 * provide several static methods for accessing the right ACLs.
 *
 * @author Akara Sucharitakul
 */
public class Acl {
    static Logger logger = Logger.getLogger(Acl.class.getName());
    static HashMap<String, Acl> aclMap = new HashMap<String, Acl>();

    File aclFile;
    long lastModified = 0l;
    HashSet<String> entries = new HashSet<String>();

    /**
     * Obtains all acl instances for a certain permission. This is used to
     * check whether a user has access to such functionality at all.
     * @param perm One of the Faban permissions
     * @return All acl instances reflecting the permission.
     */
    public static Acl[] getInstances(Permission perm) {
        ArrayList<Acl> aclList = null;
        switch (perm) {
            case MANAGE :
            case SUBMIT : // Benchmark permissions
                Set<String> benchNames = BenchmarkDescription.
                        getBenchDirMap().keySet();
                aclList = new ArrayList<Acl>(benchNames.size());
                for (String benchName : benchNames)
                    aclList.add(getInstance(perm, benchName));
                break;

            case VIEW   :
            case WRITE  :
                String[] outputs = new File(Config.OUT_DIR).list();
                aclList = new ArrayList<Acl>(outputs.length);
                for (String outName : outputs)
                    aclList.add(getInstance(perm, outName));
                break;
        }
        Acl[] acls = new Acl[aclList.size()];
        acls = aclList.toArray(acls);
        return acls;
    }

    /**
     * Obtains the acl instance for the given permission and resource.
     * @param perm The permission to check
     * @param resource The resource to check
     * @return The acl instance pertaining to the resource and permission
     */
    public static Acl getInstance(Permission perm, String resource) {

        String pathName = null;
        boolean remove = false;
        switch (perm) {
            case MANAGE :
            case SUBMIT : // These are benchmark permissions
                Set<String> benchNames = BenchmarkDescription.
                                         getBenchDirMap().keySet();
                if (!benchNames.contains(resource)) {
                    logger.severe("Requesting " + perm +
                            " ACL for benchmark " + resource +
                            ": No such benchmark!");
                    remove = true;
                }
                pathName = Config.CONFIG_DIR + resource + File.separator +
                            perm + ".acl";
                break;

            case VIEW   :
            case WRITE  : // These are result permissions
                pathName = Config.OUT_DIR + resource;
                if (!new File(pathName).isDirectory()) {
                    logger.severe("Requesting " + perm + " ACL for run " +
                                resource + ": No such run!");
                    remove = true;
                }
                pathName += "META-INF" + File.separator + perm + ".acl";
                break;
        }
        Acl acl;
        synchronized (aclMap) {
            if (remove) {
                aclMap.remove(pathName);
                return null;
            }
            acl = aclMap.get(pathName);
            if (acl == null) {
                acl = new Acl(pathName);
                aclMap.put(pathName, acl);
            }
        }
        acl.refresh();
        return acl;
    }

    private Acl(String pathName) {
        aclFile = new File(pathName);
    }

    private synchronized void refresh() {
        long modified;
        if (aclFile.isFile() &&
           (modified = aclFile.lastModified()) > lastModified) {
            try {
                BufferedReader reader = new BufferedReader(
                                        new FileReader(aclFile));
                entries.clear();
                String entry;
                while ((entry = reader.readLine()) != null) {
                    entry = entry.trim().toLowerCase();
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

    /**
     * Checks whether the acl is empty. An empty acl usually means everybody
     * is allowed.
     * @return True if the acl is empty, false otherwise
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Checks whether the acl contains an identitity (a principal name) of
     * the given subject.
     * @param user The subject representing the logged on user
     * @return True if the acl contains at least one identity of the user
     */
    public boolean contains(Subject user) {
        Set<Principal> principals = user.getPrincipals();
        for (Principal principal : principals) {
            String name = principal.getName().trim().toLowerCase();
            if (entries.contains(name))
                return true;
        }
        return false;
    }
}
