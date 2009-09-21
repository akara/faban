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

import com.sun.faban.harness.common.Config;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.Principal;

/**
 * Submitter is the encapsulation and caching of run submitter maps.
 */
public class Submitter {

    private static Logger logger = Logger.getLogger(Submitter.class.getName());
    private static HashMap<String, Entry> submitterMap =
            new HashMap<String, Entry>();

    /**
     * Checks if a user is submitter.
     * @param user
     * @param resource
     * @return boolean
     */
    public static boolean isSubmitter(Subject user, String resource) {
        String submitter = getSubmitter(resource);
        if (submitter == null)
            return false;

        for (Principal p : user.getPrincipals())
            if (submitter.equalsIgnoreCase(p.getName()))
                return true;
        return false;
    }

    /**
     * Gets the submitter.
     * @param resource
     * @return String
     */
    static String getSubmitter(String resource) {
        File submitterFile;
        Entry entry;
        synchronized (submitterMap) {
            submitterFile = getSubmitterFile(resource);
            if (submitterFile == null) {
                submitterMap.remove(resource);
                logger.severe("SECURITY: Submitter for run " + resource +
                        " not found.");
                return null;
            }

            entry = submitterMap.get(resource);
            long lastUpdate = submitterFile.lastModified();
            if (entry != null) {
                if (entry.lastUpdate >= lastUpdate)
                    return entry.submitter;
            } else {
                entry = new Entry();
                entry.lastUpdate = lastUpdate;
                submitterMap.put(resource, entry);
            }
        }


        synchronized (entry) {
            try {
                BufferedReader r = new BufferedReader(new FileReader(
                                                      submitterFile), 64);
                entry.submitter = r.readLine();
                r.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "SECURITY: Error fetching submitter!", e);
            }
            if (entry.submitter == null)
                synchronized (submitterMap) {
                    submitterMap.remove(resource);
                }
        }
        return entry.submitter;
    }

    private static File getSubmitterFile(String resource) {
        String submitterPath = resource + File.separator + "META-INF" +
                               File.separator + "submitter";
        File submitterFile = new File(Config.OUT_DIR + submitterPath);
        if (submitterFile.isFile())
            return submitterFile;
        submitterFile = new File(Config.RUNQ_DIR + submitterPath);
        if (submitterFile.isFile())
            return submitterFile;
        return null;
    }

    static class Entry {
        String submitter;
        long lastUpdate;
    }


}
