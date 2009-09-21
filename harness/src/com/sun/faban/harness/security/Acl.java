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
 * $Id$
 *
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.security;

import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.util.FileHelper;

import javax.security.auth.Subject;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import java.io.*;
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
    String resource;
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
                try {
                    RunId runId = new RunId(resource);
                    pathName = Config.OUT_DIR + resource;
                    if (!new File(pathName).isDirectory()) {
                        logger.severe("Requesting " + perm + " ACL for run " +
                                    runId + ": No such run!");
                        remove = true;
                    }
                } catch (IndexOutOfBoundsException e) {
                    // Not a runId, perhaps it is an analysis.
                    pathName = Config.ANALYSIS_DIR + resource;
                    if (!new File(pathName).isDirectory()) {
                        logger.severe("Requesting " + perm + " ACL for " +
                                    "analysis " + resource + ": No such run!");
                        remove = true;
                    }
                }
                pathName += File.separator + "META-INF" + File.separator +
                            perm + ".acl";
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
                acl = new Acl(pathName, resource);
                aclMap.put(pathName, acl);
            }
        }
        acl.refresh();
        return acl;
    }

    private Acl(String pathName, String resource) {
        aclFile = new File(pathName);
        this.resource = resource;
    }

    private synchronized void refresh() {
        long modified;
        if (!aclFile.exists())
            entries.clear();
        if (aclFile.isFile() &&
           (modified = aclFile.lastModified()) > lastModified) {
            try {
                BufferedReader reader = new BufferedReader(
                                        new FileReader(aclFile));
                entries.clear();
                String entry;
                while ((entry = reader.readLine()) != null) {
                    int commentIdx = entry.indexOf('#');
                    if (commentIdx >= 0)
                        entry = entry.substring(0, commentIdx);
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
     * Obtains the resource this ACL represents.
     * @return The resource name
     */
    public String getResource() {
        return resource;
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

    private void save() throws IOException {
        File parentDir = aclFile.getParentFile();
        if (!parentDir.exists())
            parentDir.mkdirs();
        StringBuilder b = new StringBuilder();
        for (String entry : entries)
            b.append(entry).append('\n');
        FileWriter writer = null;
        try {
            writer = new FileWriter(aclFile);
            writer.append(b);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error saving acl at " +
                    aclFile.getAbsolutePath(), e);
        } finally {
            if (writer != null)
                writer.close();
        }
    }

    private static Acl merge(Permission perm, ArrayList<Acl> aclList,
                             String targetResource) {
        Acl acl0 = aclList.get(0);
        if (aclList.size() == 1) {
            FileHelper.copyFile(acl0.aclFile.getAbsolutePath(),
                    Config.ANALYSIS_DIR + targetResource + File.separator +
                    "META-INF" + File.separator + perm + ".acl", false);
        } else {
            for (int i = 1; i < aclList.size(); i++) {
                acl0 = acl0.merge(aclList.get(i), targetResource, perm);
            }
        }
        return acl0;
    }

    private Acl merge(Acl acl2, String resource, Permission perm) {
        HashSet<String> newEntries;
        // Entries of size 0 means allow world
        // So we need to use entries from the other ACL.
        if (entries.size() == 0) {
            newEntries = new HashSet<String>(acl2.entries);
        } else if (acl2.entries.size() == 0) {
            newEntries = new HashSet<String>(entries);
        } else { // If none are 0 entries, we intersect them.
            newEntries = new HashSet<String>();
            for (String entry : entries)
                if (acl2.entries.contains(entry)) {
                    newEntries.add(entry);
                }
        }
        String path = Config.ANALYSIS_DIR + resource +
                File.separator + "META-INF" + File.separator +
                perm + ".acl";

        Acl newAcl = new Acl(path, resource);
        newAcl.entries = newEntries;
        return newAcl;
    }

    /**
     * Merge analysis ACLs from source ACLs.
     * @param resources The resources, usually runs
     * @param targetResource The resource for the analysis
     */
    public static void merge(String[] resources,
                             String targetResource) {

        // We only deal with view and write permissions for these.
        HashMap<Permission, ArrayList<Acl>> permMap =
                                    new HashMap<Permission, ArrayList<Acl>>();

        for (String resource : resources) {
            File base = new File(Config.OUT_DIR + resource, "META-INF");
            if (!base.isDirectory())
                continue;
            File[] aclFiles = base.listFiles();
            for (File aclFile : aclFiles) {
                String fileName = aclFile.getName();
                if (!fileName.endsWith(".acl"))
                    continue;
                String permString  = fileName.substring(0,
                                    fileName.length() - 4);
                Permission perm = Permission.valueOf(permString.toUpperCase());
                Acl acl = getInstance(perm, resource);
                ArrayList<Acl> aclList = permMap.get(perm);
                if (aclList == null) {
                    aclList = new ArrayList<Acl>();
                    permMap.put(perm, aclList);
                }
                aclList.add(acl);
            }
        }

        for (Map.Entry<Permission, ArrayList<Acl>> entry : permMap.entrySet()) {
            Permission perm = entry.getKey();
            ArrayList<Acl> aclList = entry.getValue();
            Acl targetAcl = merge(perm, aclList, targetResource);
            File aclFile = targetAcl.aclFile;
            String path = aclFile.getAbsolutePath();
            try {
                targetAcl.save();
                targetAcl.lastModified = aclFile.lastModified();
                synchronized (aclMap) {
                    aclMap.put(path, targetAcl);
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error saving acl at " +
                        path, e);
            }
        }
    }
}
